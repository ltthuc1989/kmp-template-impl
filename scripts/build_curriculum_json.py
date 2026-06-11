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
# Manifest of words that use an AI-generated image instead of an emoji (no Unicode emoji exists).
# Shared with opw_audio_project/scripts/generate_vocab_images.py. Columns: word,image,hint
VOCAB_IMAGES_CSV = REPO_ROOT / "scripts" / "vocab_images.csv"

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

# Hardcoded displayLetter per CSV `id`, derived 100% từ TOC sách OPW (screenshots).
# Key = CSV id (column 1). Value = displayLetter shown in app.
# Unit title = " ".join(displayLetter for lessons in unit) → match book TOC.
LESSON_DISPLAY_LETTER = {
    # L1 — The Alphabet (26)
    "L1U1_A": "Aa", "L1U1_B": "Bb", "L1U1_C": "Cc",
    "L1U2_D": "Dd", "L1U2_E": "Ee", "L1U2_F": "Ff",
    "L1U3_G": "Gg", "L1U3_H": "Hh", "L1U3_I": "Ii",
    "L1U4_J": "Jj", "L1U4_K": "Kk", "L1U4_L": "Ll",
    "L1U5_M": "Mm", "L1U5_N": "Nn", "L1U5_O": "Oo",
    "L1U6_P": "Pp", "L1U6_Q": "Qq", "L1U6_R": "Rr",
    "L1U7_S": "Ss", "L1U7_T": "Tt", "L1U7_U": "Uu", "L1U7_V": "Vv",
    "L1U8_W": "Ww", "L1U8_X": "Xx", "L1U8_Y": "Yy", "L1U8_Z": "Zz",
    # L2 — Short Vowels (24)
    "L2U1_a": "a", "L2U1_am": "am", "L2U1_an": "an",
    "L2U2_ad_ag": "ad ag", "L2U2_ap": "ap", "L2U2_at": "at",
    "L2U3_e": "e", "L2U3_et": "et", "L2U3_en_ed": "en ed",
    "L2U4_i": "i", "L2U4_ip": "ip", "L2U4_ib_id": "ib id",
    "L2U5_in": "in", "L2U5_ig": "ig", "L2U5_it_ix": "it ix",
    "L2U6_o": "o", "L2U6_ot": "ot", "L2U6_op": "op",
    "L2U7_u": "u", "L2U7_ug": "ug", "L2U7_ud_up": "ud up",
    "L2U8_ut": "ut", "L2U8_ub_um": "ub um", "L2U8_un": "un",
    # L3 — Long Vowels (24)
    "L3U1_a_e": "a_e", "L3U1_ame_ake": "ame ake", "L3U1_ate_ave": "ate ave",
    "L3U2_i_e": "i_e", "L3U2_ime_ike": "ime ike", "L3U2_ive_ine": "ive ine",
    "L3U3_o_e": "o_e", "L3U3_u_e_1": "u_e", "L3U3_u_e_2": "u_e",
    "L3U4_ai": "ai", "L3U4_ay": "ay", "L3U4_ai_ay": "ai ay",
    "L3U5_ee": "ee", "L3U5_ea": "ea", "L3U5_y_ey": "y ey",
    "L3U6_igh": "igh", "L3U6_ie": "ie", "L3U6_y": "y",
    "L3U7_oa": "oa", "L3U7_ow": "ow", "L3U7_oa_ow": "oa ow",
    "L3U8_ue": "ue", "L3U8_ui_ew": "ui ew", "L3U8_oo": "oo",
    # L4 — Blends & Digraphs (24)
    "L4U1_bl_cl": "bl cl", "L4U1_br_cr": "br cr", "L4U1_fl_gl": "fl gl",
    "L4U2_fr_gr": "fr gr", "L4U2_pl_sl": "pl sl", "L4U2_dr_tr": "dr tr",
    "L4U3_sm_sn": "sm sn", "L4U3_sp_sw": "sp sw", "L4U3_st": "st",
    "L4U4_sh": "sh", "L4U4_ch_tch": "ch tch", "L4U4_ph_wh": "ph wh",
    "L4U5_th_voiced": "th", "L4U5_th_unvoiced": "th", "L4U5_ck_qu": "ck qu",
    "L4U6_ng_nk": "ng nk", "L4U6_nd_nt": "nd nt", "L4U6_lt_mp": "lt mp",
    "L4U7_sk_sc": "sk sc", "L4U7_spr_str": "spr str", "L4U7_spl_squ": "spl squ",
    "L4U8_soft_c": "c", "L4U8_soft_g": "g", "L4U8_voiced_s": "s",
    # L5 — Letter Combinations (24)
    "L5U1_ar": "ar", "L5U1_ir_ur": "ir ur", "L5U1_er_or": "er or",
    "L5U2_ou_ow": "ou ow", "L5U2_oi_oy": "oi oy", "L5U2_oo_u": "oo u",
    "L5U3_au_aw": "au aw", "L5U3_all_wa": "all wa", "L5U3_or_oar": "or oar",
    "L5U4_are_air": "are air", "L5U4_ea_ear": "ea ear", "L5U4_ear_eer": "ear eer",
    "L5U5_a_open": "a", "L5U5_e_i_open": "e i", "L5U5_o_u_open": "o u",
    "L5U6_schwa_a": "a", "L5U6_schwa_eiou": "e i o u", "L5U6_schwa_o": "o",
    "L5U7_kn_wr": "kn wr", "L5U7_mb_e": "mb e", "L5U7_rh_st": "rh st",
    "L5U8_ture_sure": "ture sure", "L5U8_tion_sion": "tion sion", "L5U8_ous_ful": "ous ful",
}

# Words that don't appear anywhere in the old curriculum.json. Filled after first pass.
EMOJI_FALLBACK = {
    "apple": "🍎",
    "ant": "🐜",
    "cat": "🐱",
    "dog": "🐶",
    "doll": "🪆",
    "egg": "🥚",
    "elephant": "🐘",
    "fish": "🐟",
    "lion": "🦁",
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
    "fan": "🪭", "man": "🧑", "pan": "🍳", "can": "🥫",
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
    "car": "🚗", "farm": "🏡", "park": "🏞️", "star": "⭐",
    "bird": "🐦", "girl": "👧", "nurse": "👩‍⚕️", "purple": "🟣",
    "sister": "👭", "doctor": "👨‍⚕️", "tractor": "🚜",
    "house": "🏠", "cow": "🐄", "brown": "🟫",
    "coin": "🪙", "soil": "🪴", "toy": "🧸", "boy": "👦",
    "book": "📚", "foot": "🦶", "bush": "🌿", "pull": "🤜",
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
    "lamb": "🐑", "comb": "🪮", "glove": "🧤", "live": "🎙️",
    "rhino": "🦏", "rhubarb": "🌱", "whistle": "🎽", "castle": "🏰",
    "picture": "🖼️", "nature": "🌿", "treasure": "💎", "measure": "📏",
    "station": "🚉", "competition": "🏆", "television": "📺", "excursion": "🗺️",
    "famous": "⭐", "dangerous": "☢️", "beautiful": "🌸", "helpful": "🙋",
}

def generate_chant_texts(level: int, stretched_word: str, words: list) -> list:
    """Pre-generate 4 chant texts per lesson. User can hand-tune in JSON later.

    L1: prefix-style — split stretched_word at last "-" → prefix; output [prefix-{Word}] x 4.
        e.g. "AAA-pple" + [apple,ax,ant,alligator]
          → ["AAA-Apple","AAA-Ax","AAA-Ant","AAA-Alligator"]

    L2-L5: blending-style — each word → uppercase letters joined by "-".
        e.g. "cat" → "C-A-T", "tape" → "T-A-P-E", "black" → "B-L-A-C-K"
    """
    if level == 1:
        if "-" in stretched_word:
            prefix = stretched_word.rsplit("-", 1)[0]
        else:
            prefix = stretched_word
        return [f"{prefix}-{w['word'].capitalize()}" for w in words]
    return ["-".join(ch.upper() for ch in w["word"] if ch != " ") for w in words]


def display_letter(csv_id: str) -> str:
    """Lookup hardcoded displayLetter by CSV id. Hard-fails on unknown id —
    prevents silent drift if CSV adds new rows the script hasn't been updated for."""
    try:
        return LESSON_DISPLAY_LETTER[csv_id]
    except KeyError:
        raise SystemExit(
            f"Missing LESSON_DISPLAY_LETTER for {csv_id!r}. "
            f"Add it (derived from book TOC) before running."
        )


def load_emoji_from_old_curriculum() -> dict[str, str]:
    """Read emoji map from existing curriculum.json. Handles both shapes:
    - Old shape: unit.words[].{text,emoji} + word.vocabulary[].{text,emoji}
    - New shape: unit.lessons[].words[].{word,emoji}"""
    if not CURRICULUM_JSON.exists():
        return {}
    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    mp: dict[str, str] = {}
    for level in data.get("levels", []):
        for unit in level.get("units", []):
            # Old shape
            for word in unit.get("words", []):
                if word.get("emoji"):
                    mp[word["text"].lower()] = word["emoji"]
                for v in word.get("vocabulary", []):
                    if v.get("emoji"):
                        mp[v["text"].lower()] = v["emoji"]
            # New shape
            for lesson in unit.get("lessons", []):
                for w in lesson.get("words", []):
                    if w.get("emoji"):
                        mp[w["word"].lower()] = w["emoji"]
    return mp


def emoji_for(word: str, old: dict[str, str]) -> Optional[str]:
    key = word.lower()
    return old.get(key) or EMOJI_FALLBACK.get(key)


def load_vocab_images() -> dict[str, str]:
    """word(lower) -> image stem, from vocab_images.csv. Empty if manifest missing."""
    mp: dict[str, str] = {}
    if not VOCAB_IMAGES_CSV.exists():
        return mp
    with VOCAB_IMAGES_CSV.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            w = (row.get("word") or "").strip().lower()
            if w:
                mp[w] = (row.get("image") or w).strip()
    return mp


def apply_image_display(entry: dict, vocab_images: dict[str, str]) -> bool:
    """If `entry`'s word has a vocab image, set displays = [image, emoji-fallback].
    Image first = preferred by WordDisplayView; emoji kept as a load-failure fallback.
    Returns True if a display was applied."""
    img = vocab_images.get(entry["word"].lower())
    if not img:
        return False
    displays = [{"type": "image", "path": f"files/images/vocab/{img}.webp"}]
    if entry.get("emoji"):
        displays.append({"type": "emoji", "char": entry["emoji"]})
    entry["displays"] = displays
    return True


def apply_images_only() -> int:
    """Patch the EXISTING curriculum.json in place — inject image displays for manifest
    words without doing a full regen (which would drop manual displays like apple's)."""
    vocab_images = load_vocab_images()
    if not vocab_images:
        print(f"No manifest at {VOCAB_IMAGES_CSV}", file=sys.stderr)
        return 1
    data = json.loads(CURRICULUM_JSON.read_text(encoding="utf-8"))
    count = 0
    for lvl in data.get("levels", []):
        for unit in lvl.get("units", []):
            for lesson in unit.get("lessons", []):
                for word in lesson.get("words", []):
                    if apply_image_display(word, vocab_images):
                        count += 1
    CURRICULUM_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    print(f"Applied image displays to {count} word occurrences ({len(vocab_images)} manifest words)")
    return 0


def main() -> int:
    if "--apply-images-only" in sys.argv:
        return apply_images_only()

    old_emoji = load_emoji_from_old_curriculum()
    vocab_images = load_vocab_images()
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
                    apply_image_display(entry, vocab_images)
                    words.append(entry)
                chant_texts = generate_chant_texts(lvl, row["stretched_word"], words)
                lessons.append({
                    "id": row["id"],
                    "letter": row["letter"],
                    "displayLetter": display_letter(row["id"]),
                    "soundSpelling": row["sound_spelling"],
                    "sentence": row["sentence"],
                    "stretchedWord": row["stretched_word"],
                    "orderIndex": idx,
                    "words": words,
                    "chantTexts": chant_texts,
                    "chantOrder": [0, 1, 2, 3],
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
