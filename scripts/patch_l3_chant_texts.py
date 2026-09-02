#!/usr/bin/env python3
"""Patch chantTexts của Level 3 trong curriculum.json cho khớp tiếng chant.

Vì sao patch tại chỗ mà không chạy build_curriculum_json.py: file JSON đang có phần
chỉnh tay CSV không dựng lại được (chantTexts/stretchedWord L1, `displays` nhiều emoji),
chạy lại nguyên file là mất. Script này chỉ đụng `chantTexts` của lesson Level 3.

VÌ SAO KHÔNG TỰ SUY VẦN NHƯ BẢN L2: cấp 3 gọi nhịp bằng vần thật của TỪNG TỪ
(`tape` → "ape", `bone` → "one"), không phải vần của bài. Công thức của bản L2 đọc
mã `letter` theo khuôn `SHORT-{nguyên âm}-{vần}` nên với mã cấp 3 `A_E-AME-AKE` nó
ra vowel="AME" — sai. Và kể cả viết công thức đúng thì vẫn là CÔNG THỨC THỨ HAI đặt
cạnh công thức bên opw: hai chỗ là hai lần có thể lệch, mà lệch thì loa đọc "ape"
trong khi thẻ hiện chữ khác, không log không crash.

Nên script này ĐỌC vần từ file do opw sinh ra:
    opw: ./venv/bin/python scripts/assemble_chant.py --level 3 --dump-cues
         → output/level_3/_qa/chant_cues.json

Chữ THƯỜNG, không phải chữ hoa như cấp 1-2: bài đang dạy dáng chữ viết tay, dáng đó
chỉ thấy được ở chữ thường (ChantScreen.kt đã hạ chữ thường khi hiện, chú thích ở
đó nói cùng một điều). Cấp 1-2 giữ nguyên chữ hoa — đã ship, và màn hình vẫn hạ.

Khuôn chữ = khuôn tiếng trong 02_chant.mp3:
    tiếng "ape, ape, tape!"  → chữ "ape-ape-tape"
    tiếng "one, one, bone!"  → chữ "one-one-bone"

Cách chạy:
  python3 scripts/patch_l3_chant_texts.py --dry-run
  python3 scripts/patch_l3_chant_texts.py
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CURRICULUM_JSON = REPO_ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"
DEFAULT_CUES = Path(
    "/Volumes/Entertainment/GeminiGenerator/opw_audio_project/"
    "output/level_3/_qa/chant_cues.json"
)
LEVEL = 3


def chant_texts_for(lesson_id: str, words: list, cues: dict) -> list:
    """["ape-ape-tape", …] — vần lấy từ chant_cues.json, KHÔNG suy lại.

    Dùng `cue_display` (vần thô) chứ không phải `cue_tts`: hai thứ khác nhau khi
    một vần phải đổi chính tả để TTS đọc đúng. Cấp 2 đã có tiền lệ — thẻ hiện
    `AN-AN-Fan` trong khi audio đọc "ann".
    """
    entry = cues.get(lesson_id)
    if entry is None:
        raise SystemExit(f"❌ {lesson_id}: không có trong chant_cues.json — "
                         "chạy lại --dump-cues bên opw")
    by_word = {w["word"].lower(): w["cue_display"] for w in entry["words"]}
    out = []
    for item in words:
        word = item["word"]
        cue = by_word.get(word.lower())
        if cue is None:
            raise SystemExit(f"❌ {lesson_id}: từ '{word}' không có vần trong "
                             "chant_cues.json — CSV bên opw lệch curriculum.json")
        out.append(f"{cue.lower()}-{cue.lower()}-{word.lower()}")
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--cues", type=Path, default=DEFAULT_CUES,
                    help="chant_cues.json do assemble_chant.py --dump-cues sinh")
    args = ap.parse_args()

    if not args.cues.exists():
        raise SystemExit(f"❌ chưa có {args.cues}\n"
                         "   chạy: ./venv/bin/python scripts/assemble_chant.py "
                         "--level 3 --dump-cues")
    cues = json.loads(args.cues.read_text(encoding="utf-8"))

    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    changed = 0
    seen = 0
    for level in data["levels"]:
        if level.get("number") != LEVEL:
            continue
        for unit in level["units"]:
            for lesson in unit["lessons"]:
                words = lesson.get("words") or []
                if not words:
                    continue
                seen += 1
                new = chant_texts_for(lesson["id"], words, cues)
                old = lesson.get("chantTexts")
                if old == new:
                    continue
                print(f"{lesson['id']:14s} {old} → {new}")
                lesson["chantTexts"] = new
                changed += 1

    if seen != len(cues):
        print(f"\n⚠️  curriculum.json có {seen} lesson cấp {LEVEL}, "
              f"chant_cues.json có {len(cues)} — hai bên lệch")

    if args.dry_run:
        print(f"\n[dry-run] {changed}/{seen} lesson sẽ đổi")
        return 0

    CURRICULUM_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    print(f"\n✅ {changed}/{seen} lesson đã cập nhật → "
          f"{CURRICULUM_JSON.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
