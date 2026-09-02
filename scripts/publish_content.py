"""
================================================================
PUBLISH CONTENT — đưa pack ra khỏi APK, lên CDN
================================================================
Bốn việc, theo đúng thứ tự an toàn:

  1. Dựng cây CDN   <out>/content/<hash>/<đường dẫn logic>
  2. (--upload) Đẩy cây đó lên bucket
  3. Đánh dấu pack là đã đưa ra ngoài trong content_manifest.json
  4. (--strip) Xoá file khỏi composeResources để nó rời khỏi APK

Thứ tự này quan trọng: bước 3 làm app chuyển sang tải từ CDN, nên file
phải có mặt trên CDN TRƯỚC. Đảo lại là các bài đó câm ngay. Vì vậy
manifest mới chỉ được ghi xuống đĩa SAU khi upload xong — upload hỏng
thì manifest giữ nguyên bản cũ, chạy lại là được, không để lại nửa vời.

Không dùng --upload thì bước 3 vẫn chạy, và anh phải tự đẩy lên bucket
NGAY, trước khi build bản app nào.

Đường dẫn chứa hash nội dung nên bất biến — publish lại bản audio đã
sửa sẽ ghi ra đường dẫn MỚI, bản cũ vẫn nằm đó phục vụ các máy chưa
cập nhật app. Không bao giờ ghi đè, không cần purge cache CDN.

Cách chạy — CDN giả để test, không cần tài khoản nào:
  python3 scripts/publish_content.py --packs L1U3 --out /tmp/cdn
  python3 -m http.server 8000 --directory /tmp/cdn
  # local.properties: CONTENT_CDN_BASE_URL=http://10.0.2.2:8000/content

Đẩy lên bucket thật (bucket phải cho đọc công khai prefix content/):
  python3 scripts/publish_content.py --packs L1U3 --out build/cdn --upload --strip

  --upload không kèm giá trị thì lấy bucket suy từ CONTENT_CDN_BASE_URL
  trong composeApp/build.gradle.kts. Trỏ chỗ khác: --upload gs://bucket/content

Sửa/thêm một vài file trong pack ĐÃ publish: đặt file vào đúng đường dẫn
logic cũ rồi chạy lại đúng lệnh trên — chỉ file đổi nội dung mới sinh hash
mới và được đẩy lên. Gỡ hẳn nội dung đã publish thì dùng
build_content_manifest.py --drop (xoá file khỏi đĩa là không đủ).
================================================================
"""

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

# Compose Resources copies composeResources/ into these generated dirs, and its copy task does
# NOT delete files that disappeared from the source. Strip a pack without clearing them and the
# next build happily packages the removed audio again: the APK stays exactly as big as before,
# the app never fetches anything, and nothing warns you. Measured 2026-08-16 — an APK still
# carrying 762 MP3s when only 341 were left on disk.
STALE_ASSET_DIRS = (
    "core/resource/build/generated/assets",
    "core/resource/build/intermediates/assets",
)

BASE = Path(__file__).resolve().parent.parent
RES_FILES = BASE / "core/resource/src/commonMain/composeResources/files"
MANIFEST = RES_FILES / "content_manifest.json"
BUILD_MANIFEST = BASE / "scripts/build_content_manifest.py"
GRADLE_CONFIG = BASE / "composeApp/build.gradle.kts"


def run_manifest(externalized: set[str]) -> dict:
    subprocess.run(
        [sys.executable, str(BUILD_MANIFEST), "--externalize", ",".join(sorted(externalized))],
        check=True,
        stdout=subprocess.DEVNULL,
    )
    return json.loads(MANIFEST.read_text())


def default_upload_target() -> str:
    """gs://... suy từ CONTENT_CDN_BASE_URL trong build.gradle.kts.

    Không chép lại tên bucket vào đây: hai nơi giữ một địa chỉ thì sớm muộn cũng lệch, mà lệch
    ở đây là đẩy nội dung lên đúng một bucket app không hề đọc.
    """
    match = re.search(r'"CONTENT_CDN_BASE_URL",\s*\n\s*"([^"]+)"', GRADLE_CONFIG.read_text())
    if not match:
        raise SystemExit(f"Không đọc được CONTENT_CDN_BASE_URL trong {GRADLE_CONFIG}")
    url = match.group(1)
    bucket = re.match(r"https://storage\.googleapis\.com/([^/]+)/(.+)$", url)
    if not bucket:
        raise SystemExit(f"CONTENT_CDN_BASE_URL không phải Cloud Storage ({url}) — truyền --upload gs://...")
    return f"gs://{bucket.group(1)}/{bucket.group(2)}"


def upload(tree: Path, target: str) -> bool:
    """rsync cây CDN lên bucket. Không -d: đường dẫn theo hash là bất biến, bản cũ phải ở lại
    phục vụ máy chưa cập nhật app."""
    print(f"Đang đẩy {tree} → {target}")
    command = ["gsutil", "-m", "rsync", "-r", str(tree), target]
    result = subprocess.run(command)
    if result.returncode != 0:
        print(f"\nUpload hỏng ({' '.join(command)}) — manifest giữ nguyên bản cũ.", file=sys.stderr)
        return False
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Publish content packs to a CDN tree")
    parser.add_argument("--packs", required=True, help="Pack id, cách nhau bằng dấu phẩy (vd L1U3,L1U4)")
    parser.add_argument("--out", required=True, help="Thư mục web root; file ghi vào <out>/content/")
    parser.add_argument(
        "--strip",
        action="store_true",
        help="Xoá file khỏi composeResources sau khi dựng cây CDN (git giữ bản gốc)",
    )
    parser.add_argument(
        "--upload",
        nargs="?",
        const="default",
        default=None,
        metavar="gs://bucket/prefix",
        help="Đẩy cây CDN lên bucket TRƯỚC khi ghi manifest. Không kèm giá trị thì suy từ "
             "CONTENT_CDN_BASE_URL trong composeApp/build.gradle.kts.",
    )
    args = parser.parse_args()

    target = None
    if args.upload:
        target = default_upload_target() if args.upload == "default" else args.upload

    wanted = {p.strip() for p in args.packs.split(",") if p.strip()}

    # Pack đang bundled -> hỏi manifest xem pack nào đã đưa ra ngoài từ trước, để không
    # vô tình kéo chúng về lại.
    current = json.loads(MANIFEST.read_text()).get("packs", {})
    already = {name for name, info in current.items() if not info.get("bundled", True)}

    unknown = wanted - current.keys()
    if unknown:
        print(f"Không có pack: {', '.join(sorted(unknown))}", file=sys.stderr)
        return 1

    # Manifest mới được tính ngay, nhưng KHÔNG để lại trên đĩa cho tới khi file đã lên CDN:
    # app đọc manifest để biết cái gì tải về, nên manifest trỏ CDN trước khi CDN có file là các
    # bài đó câm. Giữ bản cũ ở đây rồi ghi đè ở cuối là cách rẻ nhất để thứ tự sai không xảy ra.
    previous_manifest = MANIFEST.read_text()
    manifest = run_manifest(already | wanted)
    pending_manifest = MANIFEST.read_text()
    if target:
        MANIFEST.write_text(previous_manifest)

    out_root = Path(args.out) / "content"
    copied = skipped = 0
    published_bytes = 0

    for logical, asset in manifest["assets"].items():
        if asset["pack"] not in wanted:
            continue
        src = RES_FILES / logical
        if not src.is_file():
            # File đã bị strip ở lần publish trước — cây CDN đã có nó rồi.
            skipped += 1
            continue
        dst = out_root / asset["hash"] / logical
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        copied += 1
        published_bytes += asset["bytes"]

    mb = 1024 * 1024
    print(f"Đã dựng {copied} file ({published_bytes / mb:.1f} MB) vào {out_root}")
    if skipped:
        print(f"  bỏ qua {skipped} file đã strip từ trước")

    if target:
        if not upload(out_root, target):
            return 1
        MANIFEST.write_text(pending_manifest)
        print(f"Đã cập nhật {MANIFEST.relative_to(BASE)} sau khi upload xong")
    else:
        print(
            "\n⚠️  Chưa upload: manifest ĐÃ trỏ sang CDN rồi. Đẩy cây trên lên bucket NGAY, "
            "trước khi build bản app nào — hoặc chạy lại kèm --upload.",
        )

    if args.strip:
        removed = 0
        for logical, asset in manifest["assets"].items():
            if asset["pack"] not in wanted:
                continue
            src = RES_FILES / logical
            if src.is_file():
                src.unlink()
                removed += 1
        # Dọn thư mục rỗng còn lại
        for directory in sorted(RES_FILES.rglob("*"), key=lambda p: -len(p.parts)):
            if directory.is_dir() and not any(directory.iterdir()):
                directory.rmdir()
        cleared = 0
        for rel in STALE_ASSET_DIRS:
            directory = BASE / rel
            if directory.exists():
                shutil.rmtree(directory)
                cleared += 1

        print(f"Đã gỡ {removed} file khỏi composeResources")
        if cleared:
            print(f"Đã xoá {cleared} thư mục asset sinh sẵn (Compose Resources không tự dọn file đã xoá)")
        print("→ build lại để APK nhỏ đi")

    print("\nPack đã đưa ra ngoài:", ", ".join(sorted(already | wanted)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
