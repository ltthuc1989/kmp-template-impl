"""
================================================================
BUILD CONTENT MANIFEST — chia asset thành pack + hash nội dung
================================================================
Sinh core/resource/.../composeResources/files/content_manifest.json:
mỗi asset có hash nội dung, dung lượng, và thuộc pack nào.

Pack là ĐƠN VỊ TẢI, chia theo UNIT chứ không theo level — vì
FREE_UNITS_PER_LEVEL unit đầu của MỌI level là hàng miễn phí và phải
nằm sẵn trong APK để chơi được lúc offline. Level 1 KHÔNG miễn phí:
nó cũng là sản phẩm $5 như L2-L5, chỉ 2 unit đầu là mở.

  core          → ship trong APK: 2 unit đầu mỗi level, sfx/prompt,
                  phoneme/rime/find_*, ảnh vocab, JSON
  L1U3 … L5U8   → tải khi LevelAccess cho phép mở level đó

Story đi theo unit nó khép lại (`unit_number` trong stories/level_N.json),
cả audio narration lẫn ảnh scene.

Hash để đặt đường dẫn CDN bất biến: content/<hash>/<logicalPath>.
Nội dung đổi → hash đổi → đường dẫn đổi → bản cũ trên máy tự hết hiệu
lực. Không cần ETag, không cần revalidate, offline vẫn đúng.

Manifest KHÔNG chứa timestamp: chạy lại mà nội dung không đổi thì file
ra y hệt, nên git diff sạch và biết ngay có gì thực sự thay đổi.

Pack đã publish là NGUỒN CHÂN LÝ nằm trong chính manifest: file của nó
đã bị strip khỏi repo nên quét đĩa không thấy, và chạy lại script sẽ
chép tiếp entry cũ thay vì xoá đi.

Cách chạy:
  python3 scripts/build_content_manifest.py
  python3 scripts/build_content_manifest.py --check   # CI: fail nếu lệch
================================================================
"""

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
RES_FILES = BASE / "core/resource/src/commonMain/composeResources/files"
MANIFEST = RES_FILES / "content_manifest.json"

MONETIZATION_CONFIG = BASE / "core/model/src/commonMain/kotlin/me/ltthuc/kmp/core/model/MonetizationConfig.kt"


def free_units_per_level() -> int:
    """Đọc thẳng từ MonetizationConfig.kt thay vì chép lại số.

    Hai nơi cùng giữ một con số thì sớm muộn cũng lệch, mà lệch ở đây là lỗi câm:
    manifest ship nhầm unit trả tiền vào APK, hoặc tệ hơn là đẩy unit miễn phí ra
    CDN khiến người chưa mua không học được lúc offline.
    """
    match = re.search(r"const\s+val\s+FREE_UNITS_PER_LEVEL\s*=\s*(\d+)", MONETIZATION_CONFIG.read_text())
    if not match:
        raise SystemExit(f"Không đọc được FREE_UNITS_PER_LEVEL trong {MONETIZATION_CONFIG}")
    return int(match.group(1))


CORE_PACK = "core"
HASH_LEN = 10

# Thư mục được đưa vào manifest. sfx/ và JSON không có ở đây: chúng luôn
# nằm trong APK nên không cần hash để tải.
SCANNED_DIRS = ("audio", "images")

UNIT_DIR_RE = re.compile(r"^audio/level_(\d+)/unit_(\d+)/")
STORY_DIR_RE = re.compile(r"^(?:audio|images)/level_(\d+)/stories/([A-Za-z0-9_]+)/")


def load_units() -> dict[tuple[int, int], dict]:
    """(level number, unit number) -> {'id', 'free'}

    Free = đúng luật của decideUnitStatus() trong UnitRepository.kt:

        index < freeUnitsPerLevel   -> mở
        !levelOwned && monetization -> PremiumLocked

    tức chỉ FREE_UNITS_PER_LEVEL unit đầu mỗi level là miễn phí, và luật này áp
    cho MỌI level —
    Level 1 cũng là sản phẩm bán $5 (SubscriptionPlan.LEVEL_1). KHÔNG dùng cờ
    `isPremium` trong curriculum.json: cờ đó chỉ điều khiển trạng thái ComingSoon
    ở màn chọn level, không phải paywall, và lấy nhầm nó thì 6 unit trả tiền của
    Level 1 bị ship vào APK.
    """
    free_units = free_units_per_level()
    curriculum = json.loads((RES_FILES / "curriculum.json").read_text())
    units = {}
    for level in curriculum["levels"]:
        for unit in level["units"]:
            free = unit["orderIndex"] < free_units
            units[(level["number"], unit["number"])] = {"id": unit["id"], "free": free}
    return units


def load_story_units() -> dict[str, tuple[int, int]]:
    """story id -> (level number, unit number it follows)"""
    stories = {}
    for path in sorted((RES_FILES / "stories").glob("level_*.json")):
        level = int(path.stem.split("_")[1])
        for story in json.loads(path.read_text()):
            stories[story["id"]] = (level, story["unit_number"])
    return stories


def pack_for(logical_path: str, units: dict, story_units: dict) -> str:
    """Pack chứa asset này. Không nhận ra thì cho vào core — thà ship thừa
    còn hơn để một file lặng lẽ không thuộc pack nào rồi thiếu lúc chạy."""
    match = UNIT_DIR_RE.match(logical_path)
    if match:
        key = (int(match.group(1)), int(match.group(2)))
        unit = units.get(key)
        return CORE_PACK if not unit or unit["free"] else unit["id"]

    match = STORY_DIR_RE.match(logical_path)
    if match:
        key = story_units.get(match.group(2))
        unit = units.get(key) if key else None
        return CORE_PACK if not unit or unit["free"] else unit["id"]

    return CORE_PACK


def current_manifest() -> dict:
    return json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}


def current_externalized() -> set[str]:
    """Pack đang được đánh dấu đã đưa ra ngoài trong manifest hiện có."""
    packs = current_manifest().get("packs", {})
    return {name for name, info in packs.items() if not info.get("bundled", True)}


def published_assets() -> dict[str, dict]:
    """Asset của các pack ĐÃ publish, đọc từ manifest hiện có.

    Pack publish xong thì `publish_content.py --strip` xoá file khỏi repo — nội dung chỉ còn
    trên CDN và trong chính manifest này. Script quét đĩa, nên chạy lại sau khi strip sẽ không
    thấy file đâu và xoá sạch entry của chúng. Luật tra cứu bên app là "không có trong assets
    → nằm trong APK", nên mất entry = mọi bài đã bán của pack đó câm tiếng, không lỗi, không
    warning. Vì vậy manifest là NGUỒN CHÂN LÝ của pack đã publish và được chép tiếp sang bản
    mới, chứ không sinh lại từ đĩa.
    """
    return current_manifest().get("assets", {})


def content_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()[:HASH_LEN]


def build(externalized: set[str], drop: tuple[str, ...] = ()) -> dict:
    units = load_units()
    story_units = load_story_units()

    assets = {}
    packs: dict[str, dict[str, int]] = {}

    files = sorted(
        path
        for directory in SCANNED_DIRS
        for path in (RES_FILES / directory).rglob("*")
        if path.is_file() and not path.name.startswith(".")
    )

    for path in files:
        logical = path.relative_to(RES_FILES).as_posix()
        pack = pack_for(logical, units, story_units)
        size = path.stat().st_size

        # Chỉ ghi asset của pack ĐÃ ĐƯA RA NGOÀI. Luật tra cứu bên app là "có trong
        # assets → tải về; không có → nằm trong APK", nên ghi một pack vào đây trong khi
        # CDN chưa có file là làm đứt ngay các bài đó. Pack chưa externalize vẫn được
        # tính vào bảng `packs` để biết tách ra sẽ tiết kiệm bao nhiêu.
        if pack in externalized:
            assets[logical] = {"hash": content_hash(path), "bytes": size, "pack": pack}

        entry = packs.setdefault(pack, {"files": 0, "bytes": 0, "bundled": pack not in externalized})
        entry["files"] += 1
        entry["bytes"] += size

    carried = carry_published(assets, packs, externalized, drop)

    return {
        "version": 1,
        "hashLength": HASH_LEN,
        "packs": dict(sorted(packs.items())),
        "assets": dict(sorted(assets.items())),
    }, carried


def carry_published(assets: dict, packs: dict, externalized: set[str], drop: tuple[str, ...]) -> int:
    """Chép tiếp entry của pack đã publish mà file không còn trên đĩa. Xem [published_assets].

    [drop] là lối ra duy nhất: file đã publish rồi strip thì trên đĩa không còn, nên "xoá khỏi
    đĩa" KHÔNG phải cách gỡ nó khỏi manifest — không có bước này thì entry cũ được chép tiếp
    mãi mãi và app cứ tải về một file chẳng bài nào dùng.
    """
    carried = 0
    dropped = set()
    for logical, asset in published_assets().items():
        pack = asset["pack"]
        if any(logical.startswith(prefix) for prefix in drop):
            dropped.add(logical)
            continue
        if pack not in externalized or logical in assets or (RES_FILES / logical).is_file():
            continue
        assets[logical] = asset
        entry = packs.setdefault(pack, {"files": 0, "bytes": 0, "bundled": False})
        entry["files"] += 1
        entry["bytes"] += asset["bytes"]
        carried += 1

    # Pack khai là đã publish nhưng không có file nào — trên đĩa lẫn trong manifest cũ. Gần như
    # chắc chắn gõ sai tên pack; im lặng cho qua là ship một manifest thiếu cả pack.
    empty = sorted(name for name in externalized if not packs.get(name, {}).get("files"))
    if empty:
        raise SystemExit(f"Pack khai đã publish nhưng rỗng: {', '.join(empty)}")

    # Tiền tố --drop không khớp gì = gõ sai đường dẫn. Bỏ qua im lặng thì người chạy tưởng đã
    # gỡ xong, mà manifest vẫn y nguyên.
    unmatched = [prefix for prefix in drop if not any(d.startswith(prefix) for d in dropped)]
    if unmatched:
        raise SystemExit(f"--drop không khớp asset nào: {', '.join(unmatched)}")
    if dropped:
        print(f"  (gỡ {len(dropped)} asset đã publish khỏi manifest — file trên CDN vẫn nằm đó)")
    return carried


def main() -> int:
    parser = argparse.ArgumentParser(description="Sinh content_manifest.json")
    parser.add_argument("--check", action="store_true", help="So với file hiện có, lệch thì fail")
    parser.add_argument(
        "--externalize",
        default="keep",
        help="Pack đã publish lên CDN và gỡ khỏi APK, cách nhau bằng dấu phẩy "
             "(vd L2U3,L2U4). Mặc định `keep` = giữ nguyên danh sách trong manifest. "
             "`none` = kéo mọi thứ về lại APK (chỉ đúng khi file còn đủ trong repo).",
    )
    parser.add_argument(
        "--drop",
        default="",
        help="Tiền tố đường dẫn logic cần GỠ khỏi manifest, cách nhau bằng dấu phẩy "
             "(vd audio/level_2/unit_05/L2U05_OLD_word/). Dùng khi nội dung đã publish bị bỏ "
             "hẳn — xoá file khỏi đĩa là không đủ, xem [carry_published].",
    )
    args = parser.parse_args()

    drop = tuple(d.strip() for d in args.drop.split(",") if d.strip())

    # Mặc định là `keep`, không phải rỗng: rỗng nghĩa là "chưa publish gì cả", mà chạy như thế
    # sau khi đã strip là xoá sạch entry của pack đã bán — xem [published_assets].
    if args.externalize == "keep":
        externalized = current_externalized()
    elif args.externalize == "none":
        externalized = set()
    else:
        externalized = {p.strip() for p in args.externalize.split(",") if p.strip()}

    manifest, carried = build(externalized, drop)
    rendered = json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"

    if args.check:
        current = MANIFEST.read_text() if MANIFEST.exists() else ""
        if current != rendered:
            print("content_manifest.json lệch với asset trên đĩa — chạy lại script", file=sys.stderr)
            return 1
        print("content_manifest.json khớp")
        return 0

    MANIFEST.write_text(rendered)

    mb = 1024 * 1024
    print(f"{len(manifest['assets'])} asset tải về → {MANIFEST.relative_to(BASE)}")
    if carried:
        print(f"  (giữ nguyên {carried} asset của pack đã publish, file không còn trong repo)")
    for pack, info in manifest["packs"].items():
        label = "trong APK" if info["bundled"] else "tải về"
        print(f"  {pack:8s} {info['files']:4d} file  {info['bytes'] / mb:6.1f} MB  ({label})")
    external = sum(v["bytes"] for v in manifest["packs"].values() if not v["bundled"])
    pending = sum(v["bytes"] for k, v in manifest["packs"].items() if v["bundled"] and k != CORE_PACK)
    print(f"  → đã tách khỏi APK: {external / mb:.1f} MB")
    if pending:
        print(f"  → tách được thêm:   {pending / mb:.1f} MB (chạy scripts/publish_content.py)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
