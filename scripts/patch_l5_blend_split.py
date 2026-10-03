#!/usr/bin/env python3
"""Đưa bảng tách bước 0 của Level 5 vào curriculum.json, đổi 6 mã `letter`, gắn chữ CÂM.

Sinh đôi của `patch_l4_blend_split.py` (đọc docstring file đó trước). Ba việc, chỉ đụng
lesson của Level 5:

  - `letter`: A-OPEN→A-1, E-I-OPEN→E-I-1, O-U-OPEN→O-U-1, SCHWA-A→A-2,
    SCHWA-EIOU→E-I-O-U-2, SCHWA-O→O-2. Mã cũ có từ mô tả (`OPEN`, `SCHWA`) nên
    `parsePatterns` đẻ pattern rác `open`/`schwa` — đúng lỗi #1 của bản soát Level 4.
    Parser hai phía đã bỏ đoạn TOÀN SỐ ở mọi vị trí, nên `A-1` ra `[a]`, `E-I-O-U-2` ra
    `[e, i, o, u]`, khớp `displayLetter`.
  - mỗi từ thêm `split` + `patternIndex`, đọc từ cột split1..split4 của opw
    `data/level_5/phonics.csv` (`c|*ar`, `pand|*a`, `whi|*st|le`).
  - mỗi từ thêm `silent`: CHỈ SỐ ký tự câm trong từ, suy từ cột `silent` của CSV
    (dạng `kn:k|wr:w`). Sách OPW5 in chữ câm màu hồng NHẠT ở panel "Listen and learn"
    (`k n`, `w r`, `m b`, `s t`, `e` của glove) — đó là bài học của unit 7, không phải
    trang trí. Để theo TỪNG TỪ (chỉ số ký tự) thay vì theo lesson vì màn hình tô theo ký
    tự, và vì `wordsJson` là cột JSON sẵn có nên không phải đổi schema Room.
  - `chantTexts` bước 1 = cả từ lặp ba lần (`car-car-car`), cùng luật cấp 4 (2026-09-15).

Nguồn duy nhất là CSV bên opw — app KHÔNG suy tách bằng thuật toán (xem BlendSplit.kt).
Bảng tách dựng theo bản scan sách ở `_archive/opw5_book_ref/`.

Cách chạy:
  python3 scripts/patch_l5_blend_split.py --dry-run
  python3 scripts/patch_l5_blend_split.py
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CURRICULUM_JSON = REPO_ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"
OPW_CSV = Path("/Volumes/Entertainment/GeminiGenerator/opw_audio_project/data/level_5/phonics.csv")

RENAME = {
    "A-OPEN": "A-1",
    "E-I-OPEN": "E-I-1",
    "O-U-OPEN": "O-U-1",
    "SCHWA-A": "A-2",
    "SCHWA-EIOU": "E-I-O-U-2",
    "SCHWA-O": "O-2",
    # 2026-09-29, user chốt: `glove` `live` là bài `ve`, không phải `e` câm đứng một mình.
    # Tách đổi theo (`glo|*ve`, `li|*ve`) nên thẻ dạy có tiếng /v/ thật, còn chữ `e` chỉ
    # nhạt đi. Sách in thẻ là chữ `e` đơn — đây là chỗ đi khác sách có chủ đích.
    "MB-E": "MB-VE",
}


def parse_split(cell: str) -> tuple[list[str], int]:
    chunks = cell.split("|")
    starred = [i for i, c in enumerate(chunks) if c.startswith("*")]
    if len(starred) != 1 or any(not c for c in chunks):
        raise ValueError(f"ô tách hỏng: {cell!r}")
    return [c.lstrip("*") for c in chunks], starred[0]


def parse_silent(cell: str) -> dict[str, str]:
    """`kn:k|wr:w` → {pattern: chữ câm}."""
    out: dict[str, str] = {}
    for item in (cell or "").strip().split("|"):
        if not item:
            continue
        pat, chars = item.split(":", 1)
        out[pat] = chars
    return out


def silent_indices(word: str, chunks: list[str], pattern_index: int,
                   silent_by_pattern: dict[str, str]) -> list[int]:
    """Chỉ số (trong TỪ, tính cả dấu cách) của những ký tự câm.

    Chữ câm luôn nằm trong mảnh pattern, nên tìm trong đúng mảnh đó rồi cộng vị trí bắt
    đầu của mảnh. Tìm trong cả từ là sai: `rhubarb` có `b` ở hai chỗ, `comb` có `m`
    trong `mb` lẫn không.
    """
    if not silent_by_pattern:
        return []
    chunk = chunks[pattern_index].lower()
    chars = next((v for p, v in silent_by_pattern.items() if p in chunk), None)
    if chars is None:
        return []
    start = sum(len(c) for c in chunks[:pattern_index])
    out = []
    for ch in chars:
        at = chunk.find(ch)
        if at < 0:
            raise ValueError(f"{word}: chữ câm '{ch}' không có trong mảnh '{chunk}'")
        out.append(start + at)
    return sorted(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    rows = {r["id"]: r for r in csv.DictReader(OPW_CSV.open(encoding="utf-8"))}
    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    lv5 = next(l for l in data["levels"] if l["id"] == "L5")

    n_letter = n_words = n_chant = n_silent = 0
    for unit in lv5["units"]:
        for lesson in unit["lessons"]:
            row = rows[lesson["id"]]
            new_letter = RENAME.get(lesson["letter"], lesson["letter"])
            if new_letter != row["letter"]:
                sys.exit(f"{lesson['id']}: mã JSON '{lesson['letter']}'→'{new_letter}' "
                         f"không khớp CSV '{row['letter']}'")
            if lesson["letter"] != new_letter:
                # `displayLetter` phải đi CÙNG: golden test hai phía khoá luật
                # `parsePatterns(letter) == displayLetter.split(' ')`. Đổi mã mà quên nhãn
                # thì test đỏ — nhưng nếu ai đó tắt test, app sẽ hiện nhãn cũ mà đọc mã mới.
                display = lesson.get("displayLetter")
                if display:
                    lesson["displayLetter"] = new_letter.lower().replace("-", " ")
                lesson["letter"] = new_letter
                n_letter += 1
            silent_by_pattern = parse_silent(row.get("silent", ""))
            csv_words = [row[f"word{i}"] for i in range(1, 5)]
            for i, w in enumerate(lesson["words"], start=1):
                if w["word"] != csv_words[i - 1]:
                    sys.exit(f"{lesson['id']}: từ {i} JSON '{w['word']}' ≠ CSV '{csv_words[i-1]}'")
                chunks, pidx = parse_split(row[f"split{i}"])
                if "".join(chunks).lower() != w["word"].replace(" ", "").lower():
                    sys.exit(f"{w['word']}: mảnh ghép lại không bằng từ")
                w["split"], w["patternIndex"] = chunks, pidx
                n_words += 1
                sil = silent_indices(w["word"], chunks, pidx, silent_by_pattern)
                if sil:
                    w["silent"] = sil
                    n_silent += 1
                else:
                    w.pop("silent", None)
            order = lesson.get("chantOrder") or list(range(len(lesson["words"])))
            chant = ["-".join([lesson["words"][i]["word"]] * 3) for i in order]
            if lesson.get("chantTexts") != chant:
                lesson["chantTexts"] = chant
                n_chant += 1

    print(f"đổi {n_letter} mã letter, gắn split cho {n_words} từ, "
          f"chữ câm cho {n_silent} từ, đổi chantTexts {n_chant} bài")
    if args.dry_run:
        print("[dry-run] không ghi")
        return 0
    CURRICULUM_JSON.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"đã ghi {CURRICULUM_JSON.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
