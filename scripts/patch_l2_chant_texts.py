#!/usr/bin/env python3
"""Patch chantTexts của Level 2 trong curriculum.json cho khớp tiếng chant.

Vì sao patch tại chỗ mà không chạy build_curriculum_json.py: file JSON đang có phần
chỉnh tay CSV không dựng lại được (chantTexts/stretchedWord L1, `displays` nhiều emoji),
chạy lại nguyên file là mất. Script này chỉ đụng `chantTexts` của các lesson Level 2.

Khuôn chữ = khuôn tiếng trong 02_chant.mp3 (opw prompts.template_chant_word_family):
    lesson có vần   "SHORT-A-AN"      → tiếng "an, an, fan!"   → chữ "AN-AN-Fan"
    lesson 2 vần    "SHORT-A-AD-AG"   → tiếng "ag, ag, bag!"   → chữ "AG-AG-Bag"
    lesson nguyên âm "SHORT-A"        → tiếng "ahh, ahh, cat!" → chữ "A-A-Cat"

Cách chạy:
  python3 scripts/patch_l2_chant_texts.py --dry-run
  python3 scripts/patch_l2_chant_texts.py
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CURRICULUM_JSON = REPO_ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"


def chant_texts_for(letter: str, words: list) -> list:
    segments = letter.upper().split("-")
    vowel = segments[1] if len(segments) > 1 else segments[0]
    rimes = segments[2:]
    out = []
    for entry in words:
        word = entry["word"]
        cue = next(
            (r for r in rimes if word.lower().endswith(r.lower()) and len(word) > len(r)),
            vowel,
        )
        out.append(f"{cue}-{cue}-{word.capitalize()}")
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    changed = 0
    for level in data["levels"]:
        if level.get("number") != 2:
            continue
        for unit in level["units"]:
            for lesson in unit["lessons"]:
                words = lesson.get("words") or []
                if not words:
                    continue
                new = chant_texts_for(lesson["letter"], words)
                old = lesson.get("chantTexts")
                if old == new:
                    continue
                print(f"{lesson['id']:14s} {old} → {new}")
                lesson["chantTexts"] = new
                changed += 1

    if args.dry_run:
        print(f"\n[dry-run] {changed} lesson sẽ đổi")
        return 0

    CURRICULUM_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    print(f"\n✅ {changed} lesson đã cập nhật → {CURRICULUM_JSON.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
