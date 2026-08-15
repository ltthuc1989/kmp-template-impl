"""
================================================================
OPTIMIZE APP AUDIO — re-encode bản phân phối trong app
================================================================
Encode MP3 trong core/resource/.../composeResources/files/ xuống
40kbps mono 22.05kHz — bitrate đã chốt bằng tai 2026-08-15.

KHÁC với scripts/optimize_audio.py bên opw_audio_project: script kia
encode ĐÈ LÊN MASTER. Script này chỉ đụng bản copy trong app; master
ở opw_audio_project/output/ giữ nguyên chất lượng gốc.

- Bỏ qua file đã ≤ SKIP_ABOVE_BITRATE (nhóm chant vốn đã 32k) — encode
  lại chỉ làm tệ thêm mà không giảm được size.
- Bỏ qua chime UI ngắn (sfx/*.mp3): tổng 28KB, không đáng đổi chất lượng.
- In-place atomic: ghi .tmp rồi rename, hỏng giữa chừng không mất file gốc.

Cách chạy:
  python3 scripts/optimize_app_audio.py --dry-run
  python3 scripts/optimize_app_audio.py
================================================================
"""

import argparse
import os
import subprocess
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
RES_FILES = BASE / "core/resource/src/commonMain/composeResources/files"

# Thư mục được phép encode. sfx/*.mp3 (click/correct/lesson_complete) cố tình
# không nằm trong đây — chime ngắn, tổng 28KB, giữ nguyên chất lượng.
TARGETS = [
    RES_FILES / "audio",
    RES_FILES / "sfx/prompts",
    RES_FILES / "sfx/voice",
]

TARGET_BITRATE = "40k"
TARGET_SAMPLE_RATE = "22050"
TARGET_CHANNELS = "1"

# File đã ở dưới ngưỡng này thì bỏ qua (đã tối ưu từ trước).
SKIP_AT_OR_BELOW_BPS = 48_000


def bitrate_of(path: Path) -> int:
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=bit_rate", "-of", "csv=p=0", str(path)],
        capture_output=True,
        text=True,
    ).stdout.strip()
    return int(out) if out.isdigit() else 0


def encode(path: Path) -> None:
    tmp = path.with_suffix(".tmp.mp3")
    subprocess.run(
        [
            "ffmpeg", "-v", "error", "-y", "-i", str(path),
            "-c:a", "libmp3lame",
            "-b:a", TARGET_BITRATE,
            "-ac", TARGET_CHANNELS,
            "-ar", TARGET_SAMPLE_RATE,
            str(tmp),
        ],
        check=True,
    )
    os.replace(tmp, path)


def main() -> int:
    parser = argparse.ArgumentParser(description="Re-encode app audio → 40k mono 22.05kHz")
    parser.add_argument("--dry-run", action="store_true", help="Chỉ liệt kê, không sửa file")
    args = parser.parse_args()

    files = sorted(p for root in TARGETS for p in root.rglob("*.mp3"))
    if not files:
        print(f"Không tìm thấy MP3 nào dưới {RES_FILES}", file=sys.stderr)
        return 1

    before_total = after_total = 0
    encoded = skipped = failed = 0

    for path in files:
        size_before = path.stat().st_size
        before_total += size_before

        if bitrate_of(path) <= SKIP_AT_OR_BELOW_BPS:
            skipped += 1
            after_total += size_before
            continue

        if args.dry_run:
            encoded += 1
            after_total += size_before
            continue

        try:
            encode(path)
        except subprocess.CalledProcessError as err:
            print(f"LỖI {path.relative_to(RES_FILES)}: {err}", file=sys.stderr)
            failed += 1
            after_total += size_before
            continue

        encoded += 1
        after_total += path.stat().st_size

    mb = 1024 * 1024
    verb = "sẽ encode" if args.dry_run else "đã encode"
    print(f"{verb}: {encoded} file | bỏ qua: {skipped} | lỗi: {failed}")
    print(f"trước: {before_total / mb:.1f} MB")
    if not args.dry_run:
        saved = before_total - after_total
        print(f"sau:   {after_total / mb:.1f} MB  (giảm {saved / mb:.1f} MB)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
