#!/usr/bin/env python3
"""Convert Oxford Phonics Words CSV files into curriculum.json for Grabee.

Reads:
    /Volumes/Entertainment/GeminiGenerator/opw_audio_project/data/level_{1..5}/phonics.csv
Writes:
    core/resource/src/commonMain/composeResources/files/curriculum.json

Reuses emoji map from the existing curriculum.json (words[].emoji + vocabulary[].emoji)
and supplements with EMOJI_FALLBACK below for words not present in the old file.
"""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = Path("/Volumes/Entertainment/GeminiGenerator/opw_audio_project/data")
CURRICULUM_JSON = REPO_ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"

LEVEL_META = {
    1: {"title": "The Alphabet", "ageRange": "3-5", "isPremium": False},
    2: {"title": "Short Vowels", "ageRange": "4-6", "isPremium": True},
    3: {"title": "Long Vowels", "ageRange": "5-7", "isPremium": True},
    4: {"title": "Blends & Digraphs", "ageRange": "5-8", "isPremium": True},
    5: {"title": "Letter Combinations", "ageRange": "6-8", "isPremium": True},
}

THEME_CHIP = {
    # L2 — short vowel families
    "L2U1": "short a", "L2U2": "short a", "L2U3": "short e",
    "L2U4": "short i", "L2U5": "short i", "L2U6": "short o",
    "L2U7": "short u", "L2U8": "short u",
    # L3 — long vowel families
    "L3U1": "long a", "L3U2": "long i", "L3U3": "long o & long u",
    "L3U4": "long a", "L3U5": "long e", "L3U6": "long i",
    "L3U7": "long o", "L3U8": "long u",
    # L4 — special groupings
    "L4U5": "voiced & unvoiced",
    "L4U8": "soft c · soft g · voiced s",
    # L5 — schwa & silent letters
    "L5U6": "schwa",
    "L5U7": "silent letters",
}

# Hand-curated overrides for letter codes whose default lowercasing isn't right.
DISPLAY_LETTER_OVERRIDE = {
    "TH-voiced": "th",
    "TH-unvoiced": "th",
    "SOFT-C": "c",
    "SOFT-G": "g",
    "VOICED-S": "s",
    "U_E-1": "u_e",
    "U_E-2": "u_e",
    "A-OPEN": "a",
    "E-I-OPEN": "e i",
    "O-U-OPEN": "o u",
    "SCHWA-A": "a",
    "SCHWA-EIOU": "e i o u",
    "SCHWA-O": "o",
    "MB-E": "mb e",
}

# Words that don't appear anywhere in the old curriculum.json. Filled after first pass.
EMOJI_FALLBACK = {
    "ax": "🪓",
    "alligator": "🐊",
    "computer": "💻",
    "elbow": "💪",
    "envelope": "✉️",
    "fork": "🍴",
    "gorilla": "🦍",
    "goat": "🐐",
    "gift": "🎁",
    "hot dog": "🌭",
    "insect": "🐛",
    "ink": "🖋️",
    "igloo": "🛖",
    "iguana": "🦎",
    "jet": "✈️",
    "jam": "🍓",
    "juice": "🧃",
    "jacket": "🧥",
    "kangaroo": "🦘",
    "key": "🔑",
    "kite": "🪁",
    "leaf": "🍃",
    "lemon": "🍋",
    "lamp": "💡",
    "milk": "🥛",
    "money": "💰",
    "mouse": "🐭",
    "monkey": "🐵",
    "nut": "🥜",
    "net": "🥅",
    "nest": "🪺",
    "nose": "👃",
    "octopus": "🐙",
    "ox": "🐂",
    "olive": "🫒",
    "ostrich": "🦤",
    "peach": "🍑",
    "pen": "🖊️",
    "panda": "🐼",
    "pineapple": "🍍",
    "queen": "👸",
    "quiz": "❓",
    "quilt": "🛏️",
    "question": "❓",
    "rabbit": "🐰",
    "rose": "🌹",
    "rice": "🍚",
    "robot": "🤖",
    "seal": "🦭",
    "sun": "☀️",
    "soap": "🧼",
    "socks": "🧦",
    "turtle": "🐢",
    "tent": "⛺",
    "tiger": "🐯",
    "teacher": "👩‍🏫",
    "umbrella": "☂️",
    "up": "⬆️",
    "uncle": "👨",
    "umpire": "🧑‍⚖️",
    "van": "🚐",
    "vet": "🩺",
    "vest": "🦺",
    "violin": "🎻",
    "wolf": "🐺",
    "web": "🕸️",
    "water": "💧",
    "watch": "⌚",
    "fox": "🦊",
    "box": "📦",
    "six": "6️⃣",
    "wax": "🕯️",
    "yo-yo": "🪀",
    "yak": "🐃",
    "yogurt": "🍦",
    "yacht": "⛵",
    "zipper": "🤐",
    "zero": "0️⃣",
    "zoo": "🦁",
    "zebra": "🦓",
    # L2 short-vowel words
    "ram": "🐏", "yam": "🍠", "dam": "🏞️",
    "fan": "🌀", "man": "🧑", "pan": "🍳", "can": "🥫",
    "dad": "👨", "pad": "📒", "bag": "👜", "rag": "🧹",
    "cap": "🧢", "map": "🗺️", "nap": "😴", "tap": "🚰",
    "bat": "🦇", "rat": "🐀", "hat": "👒", "mat": "🟫",
    "vet": "🩺", "ten": "🔟",
    "jet": "✈️", "wet": "💦", "pet": "🐶",
    "hen": "🐔", "red": "🟥", "bed": "🛏️",
    "hip": "🍑", "zip": "🤐", "in": "📥",
    "lip": "💋", "tip": "📌", "sip": "🥤", "rip": "✂️",
    "bib": "👶", "rib": "🍖", "kid": "👶", "lid": "🥫",
    "pin": "📍", "fin": "🐟", "bin": "🗑️", "win": "🏆",
    "fig": "🍐", "wig": "💇", "big": "📏", "dig": "⛏️",
    "pit": "🕳️", "hit": "🥊", "mix": "🌀",
    "log": "🪵", "rod": "🎣",
    "pot": "🍯", "hot": "🔥", "cot": "🛏️", "dot": "🔵",
    "top": "🎩", "mop": "🧹", "hop": "🐰", "pop": "💥",
    "jug": "🫙", "hug": "🤗",
    "bug": "🐛", "rug": "🟫", "mug": "☕",
    "bud": "🌱", "mud": "💧", "pup": "🐶", "cup": "☕",
    "hut": "🛖", "cut": "✂️", "but": "❗",
    "cub": "🐻", "tub": "🛁", "gum": "🍬", "hum": "🎵",
    "bun": "🥯", "run": "🏃", "fun": "🎉",
    # L3 long-vowel words
    "tape": "📼", "cape": "🦸", "cane": "🦯", "mane": "🦁",
    "game": "🎮", "cake": "🎂", "name": "🪪", "lake": "🏞️",
    "gate": "🚪", "wave": "🌊", "skate": "⛸️", "cave": "🕳️",
    "kite": "🪁", "pine": "🌲", "ripe": "🍒", "fine": "👌",
    "lime": "🍈", "bike": "🚲", "time": "⏰", "hike": "🥾",
    "five": "5️⃣", "nine": "9️⃣", "dive": "🏊", "line": "📏",
    "home": "🏠", "bone": "🦴", "cone": "🍦", "rope": "🪢",
    "cube": "🟦", "mute": "🔇", "cute": "🥰", "mule": "🫏",
    "tube": "🧴", "june": "📆", "tune": "🎶", "rule": "📜",
    "rain": "🌧️", "nail": "🔨", "tail": "🐈", "wait": "⏳",
    "bay": "🏝️", "day": "📅", "say": "💬", "pay": "💵",
    "sail": "⛵", "mail": "📬", "hay": "🌾", "may": "🌼",
    "bee": "🐝", "feet": "🦶", "seed": "🌱", "jeep": "🚙",
    "leaf": "🍃", "eat": "🍽️", "sea": "🌊", "meat": "🍖",
    "candy": "🍬", "happy": "😄",
    "light": "💡", "night": "🌙", "high": "📈", "right": "✅",
    "pie": "🥧", "tie": "👔", "lie": "🤥", "die": "🎲",
    "spy": "🕵️", "sky": "☁️", "cry": "😢", "my": "👇",
    "boat": "⛵", "coat": "🧥", "road": "🛣️",
    "bow": "🎀", "row": "🚣", "yellow": "💛", "pillow": "🛏️",
    "toad": "🐸", "window": "🪟",
    "blue": "🔵", "glue": "🧴", "clue": "🔍", "tuesday": "📅",
    "fruit": "🍇", "suit": "🤵", "new": "🆕", "dew": "💧",
    "moon": "🌙", "food": "🍽️", "boot": "🥾",
    # L4 blends & digraphs
    "black": "⬛", "blanket": "🛏️", "clock": "🕐", "club": "🃏",
    "broom": "🧹", "bride": "👰", "crab": "🦀", "crocodile": "🐊",
    "fly": "🪰", "flag": "🚩", "globe": "🌍", "glass": "🥃",
    "frog": "🐸", "friday": "📅", "green": "🟢", "grass": "🌿",
    "plate": "🍽️", "play": "🎮", "slide": "🛝", "sleep": "😴",
    "drum": "🥁", "dress": "👗", "truck": "🚚", "tree": "🌳",
    "smile": "😊", "smoke": "💨", "snake": "🐍", "snow": "❄️",
    "spoon": "🥄", "spot": "🔘", "swing": "🎢", "swim": "🏊",
    "stop": "🛑", "test": "📝", "stamp": "📮", "fast": "💨",
    "shell": "🐚", "ship": "🚢", "brush": "🪥",
    "chick": "🐤", "lunch": "🍱", "catch": "🤲",
    "phone": "📱", "dolphin": "🐬", "whale": "🐋", "white": "⬜",
    "this": "👉", "that": "👈", "mother": "👩", "father": "👨",
    "three": "3️⃣", "teeth": "🦷", "think": "💭", "bath": "🛁",
    "duck": "🦆", "rocket": "🚀",
    "king": "👑", "long": "📏", "bank": "🏦", "pink": "🩷",
    "wind": "🌬️", "hand": "✋", "paint": "🎨",
    "belt": "🧷", "adult": "🧑", "camp": "🏕️",
    "skunk": "🦨", "desk": "📋", "scale": "⚖️", "school": "🏫",
    "spray": "🚿", "spring": "🌸", "string": "🧶", "strong": "💪",
    "splash": "💦", "splint": "🩹", "squid": "🦑", "square": "🟦",
    "city": "🏙️", "ice cream": "🍦", "cell phone": "📱",
    "giraffe": "🦒", "orange": "🍊", "giant": "🗿", "cage": "🪺",
    "jeans": "👖", "cheese": "🧀", "legs": "🦵",
    # L5 letter combinations
    "car": "🚗", "farm": "🚜", "park": "🏞️", "star": "⭐",
    "bird": "🐦", "girl": "👧", "nurse": "👩‍⚕️", "purple": "🟣",
    "sister": "👭", "doctor": "👨‍⚕️", "tractor": "🚜",
    "house": "🏠", "cow": "🐄", "brown": "🟫",
    "coin": "🪙", "soil": "🪴", "toy": "🧸", "boy": "👦",
    "book": "📚", "foot": "🦶", "bush": "🌳", "pull": "🤜",
    "sauce": "🥣", "august": "📅", "prawn": "🦐", "draw": "✏️",
    "ball": "⚽", "tall": "📏", "walk": "🚶",
    "horse": "🐴", "roar": "🦁", "board": "🪵",
    "share": "🤝", "chair": "🪑", "hair": "💇",
    "bread": "🍞", "head": "🗣️", "bear": "🐻", "pear": "🍐",
    "ear": "👂", "clear": "💎", "deer": "🦌", "cheer": "📣",
    "acorn": "🌰", "baby": "👶", "elevator": "🛗", "lady": "👩",
    "he": "👨", "she": "👩", "child": "🧒",
    "cold": "🥶", "hotel": "🏨", "uniform": "👔", "music": "🎵",
    "banana": "🍌",
    "chicken": "🐔", "pencil": "✏️", "surprise": "🎁",
    "love": "❤️", "son": "👦", "honey": "🍯",
    "knife": "🔪", "knee": "🦵", "write": "✍️", "wrong": "❌",
    "lamb": "🐑", "comb": "💇", "glove": "🧤", "live": "🎙️",
    "rhino": "🦏", "rhubarb": "🌱", "whistle": "🎽", "castle": "🏰",
    "picture": "🖼️", "nature": "🌿", "treasure": "💎", "measure": "📏",
    "station": "🚉", "competition": "🏆", "television": "📺", "excursion": "🗺️",
    "famous": "⭐", "dangerous": "☢️", "beautiful": "🌸", "helpful": "🙋",
}

LETTER_RE = re.compile(r"[A-Z]")

def display_letter(raw: str) -> str:
    if raw in DISPLAY_LETTER_OVERRIDE:
        return DISPLAY_LETTER_OVERRIDE[raw]
    if re.fullmatch(r"[A-Z]", raw):
        return f"{raw}{raw.lower()}"
    s = raw
    for prefix in ("SHORT-", "LONG-"):
        if s.startswith(prefix):
            s = s[len(prefix):]
            break
    return s.lower().replace("-", " ")


def load_emoji_from_old_curriculum() -> dict[str, str]:
    if not CURRICULUM_JSON.exists():
        return {}
    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    mp: dict[str, str] = {}
    for level in data.get("levels", []):
        for unit in level.get("units", []):
            for word in unit.get("words", []):
                if word.get("emoji"):
                    mp[word["text"].lower()] = word["emoji"]
                for v in word.get("vocabulary", []):
                    if v.get("emoji"):
                        mp[v["text"].lower()] = v["emoji"]
    return mp


def emoji_for(word: str, old: dict[str, str]) -> Optional[str]:
    key = word.lower()
    return old.get(key) or EMOJI_FALLBACK.get(key)


def main() -> int:
    old_emoji = load_emoji_from_old_curriculum()

    levels = []
    missing: list[str] = []
    for lvl in (1, 2, 3, 4, 5):
        csv_path = DATA_DIR / f"level_{lvl}" / "phonics.csv"
        if not csv_path.exists():
            print(f"missing CSV: {csv_path}", file=sys.stderr)
            return 1
        meta = LEVEL_META[lvl]
        with csv_path.open(encoding="utf-8") as f:
            rows = list(csv.DictReader(f))
        units_by_num: dict[int, list] = {}
        for row in rows:
            unum = int(row["unit"])
            units_by_num.setdefault(unum, []).append(row)

        units = []
        for unum in sorted(units_by_num):
            unit_id = f"L{lvl}U{unum}"
            unit_rows = units_by_num[unum]
            lessons = []
            for idx, row in enumerate(unit_rows):
                words = []
                for k in ("word1", "word2", "word3", "word4"):
                    w = row[k].strip()
                    e = emoji_for(w, old_emoji)
                    if e is None:
                        missing.append(w)
                    entry = {"word": w}
                    if e is not None:
                        entry["emoji"] = e
                    words.append(entry)
                lessons.append({
                    "id": row["id"],
                    "letter": row["letter"],
                    "displayLetter": display_letter(row["letter"]),
                    "soundSpelling": row["sound_spelling"],
                    "sentence": row["sentence"],
                    "stretchedWord": row["stretched_word"],
                    "orderIndex": idx,
                    "words": words,
                })
            unit_title = " ".join(le["displayLetter"] for le in lessons)
            units.append({
                "id": unit_id,
                "number": unum,
                "title": unit_title,
                "themeChip": THEME_CHIP.get(unit_id),
                "orderIndex": unum - 1,
                "lessons": lessons,
            })

        levels.append({
            "id": f"L{lvl}",
            "number": lvl,
            "title": meta["title"],
            "isPremium": meta["isPremium"],
            "ageRange": meta["ageRange"],
            "orderIndex": lvl - 1,
            "units": units,
        })

    total_units = sum(len(l["units"]) for l in levels)
    total_lessons = sum(len(u["lessons"]) for l in levels for u in l["units"])
    total_words = sum(len(le["words"]) for l in levels for u in l["units"] for le in u["lessons"])
    print(f"Levels: {len(levels)}, Units: {total_units}, Lessons: {total_lessons}, Words: {total_words}")

    if missing:
        unique_missing = sorted(set(w.lower() for w in missing))
        print(f"⚠️  Missing emojis ({len(unique_missing)} unique):")
        for w in unique_missing:
            print(f"    {w!r}: '?',")

    CURRICULUM_JSON.write_text(
        json.dumps({"levels": levels}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {CURRICULUM_JSON.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
