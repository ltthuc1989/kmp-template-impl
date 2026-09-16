#!/usr/bin/env python3
"""Đưa bảng tách bước 0 của Level 4 vào curriculum.json, và đổi 5 mã `letter` sang mã sạch.

Vì sao patch tại chỗ mà không chạy build_curriculum_json.py: file JSON đang có phần chỉnh
tay CSV không dựng lại được (chantTexts/stretchedWord L1, `displays` nhiều emoji, ảnh từ
vựng). Script này chỉ đụng lesson của Level 4:
  - `letter`: TH-voiced→TH-1, TH-unvoiced→TH-2, SOFT-C→C, SOFT-G→G, VOICED-S→S
    (mã có từ mô tả làm parsePatterns đẻ pattern rác "voiced"/"soft" — soát 2026-08-31)
  - mỗi từ thêm `split` (danh sách mảnh) + `patternIndex` (mảnh mang pattern), đọc từ cột
    split1..split4 của opw `data/level_4/phonics.csv`. Ô CSV: `*bl|ack`, `mo|*th|er` —
    dấu `*` đánh dấu mảnh pattern.
  - `chantTexts` của bước 1 = cả từ lặp ba lần: `black-black-black` (luật 2026-09-15; trước
    đó là mảnh bước 0 `bl-ack-black`). Tiếng sinh bằng opw `assemble_chant.py --level 4 --parts`.

Nguồn duy nhất là CSV bên opw — app KHÔNG suy tách bằng thuật toán (xem BlendSplit.kt).

Cách chạy:
  python3 scripts/patch_l4_blend_split.py --dry-run
  python3 scripts/patch_l4_blend_split.py
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CURRICULUM_JSON = REPO_ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"
OPW_CSV = Path("/Volumes/Entertainment/GeminiGenerator/opw_audio_project/data/level_4/phonics.csv")

RENAME = {"TH-voiced": "TH-1", "TH-unvoiced": "TH-2", "SOFT-C": "C", "SOFT-G": "G", "VOICED-S": "S"}


def parse_split(cell: str) -> tuple[list[str], int]:
    chunks = cell.split("|")
    starred = [i for i, c in enumerate(chunks) if c.startswith("*")]
    if len(starred) != 1 or any(not c for c in chunks):
        raise ValueError(f"ô tách hỏng: {cell!r}")
    return [c.lstrip("*") for c in chunks], starred[0]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    rows = {r["id"]: r for r in csv.DictReader(OPW_CSV.open(encoding="utf-8"))}
    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    lv4 = next(l for l in data["levels"] if l["id"] == "L4")

    n_letter = n_words = n_chant = 0
    for unit in lv4["units"]:
        for lesson in unit["lessons"]:
            row = rows[lesson["id"]]
            new_letter = RENAME.get(lesson["letter"], lesson["letter"])
            if new_letter != row["letter"]:
                sys.exit(f"{lesson['id']}: mã JSON '{lesson['letter']}'→'{new_letter}' không khớp CSV '{row['letter']}'")
            if lesson["letter"] != new_letter:
                lesson["letter"] = new_letter
                n_letter += 1
            csv_words = [row[f"word{i}"] for i in range(1, 5)]
            for i, w in enumerate(lesson["words"], start=1):
                if w["word"] != csv_words[i - 1]:
                    sys.exit(f"{lesson['id']}: từ {i} JSON '{w['word']}' ≠ CSV '{csv_words[i-1]}'")
                chunks, pidx = parse_split(row[f"split{i}"])
                if "".join(chunks) != w["word"].replace(" ", "").lower():
                    sys.exit(f"{w['word']}: mảnh ghép lại không bằng từ")
                w["split"], w["patternIndex"] = chunks, pidx
                n_words += 1
            # Thẻ chant i là từ thứ chantOrder[i]. Màn chant tách token theo dấu "-", nên ba
            # lần lặp nối bằng "-" để mỗi lần sáng đúng một nhịp.
            order = lesson.get("chantOrder") or list(range(len(lesson["words"])))
            chant = ["-".join([lesson["words"][i]["word"]] * 3) for i in order]
            if lesson.get("chantTexts") != chant:
                lesson["chantTexts"] = chant
                n_chant += 1

    print(f"đổi {n_letter} mã letter, gắn split cho {n_words} từ, đổi chantTexts {n_chant} bài")
    if args.dry_run:
        print("[dry-run] không ghi")
        return 0
    CURRICULUM_JSON.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"đã ghi {CURRICULUM_JSON.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
