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


def current_externalized() -> set[str]:
    """Pack đang được đánh dấu đã đưa ra ngoài trong manifest hiện có."""
    if not MANIFEST.exists():
        return set()
    packs = json.loads(MANIFEST.read_text()).get("packs", {})
    return {name for name, info in packs.items() if not info.get("bundled", True)}


def content_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()[:HASH_LEN]


def build(externalized: set[str]) -> dict:
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

    return {
        "version": 1,
        "hashLength": HASH_LEN,
        "packs": dict(sorted(packs.items())),
        "assets": assets,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Sinh content_manifest.json")
    parser.add_argument("--check", action="store_true", help="So với file hiện có, lệch thì fail")
    parser.add_argument(
        "--externalize",
        default="",
        help="Pack đã publish lên CDN và gỡ khỏi APK, cách nhau bằng dấu phẩy "
             "(vd L2U3,L2U4). Mặc định rỗng = mọi thứ vẫn nằm trong APK. "
             "Giữ nguyên danh sách hiện có: --externalize keep",
    )
    args = parser.parse_args()

    if args.externalize == "keep":
        externalized = current_externalized()
    else:
        externalized = {p.strip() for p in args.externalize.split(",") if p.strip()}

    manifest = build(externalized)
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
