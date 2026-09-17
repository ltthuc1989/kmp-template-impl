#!/usr/bin/env python3
"""
Mô phỏng game Fill Letter kiểu MỚI — khuyết VẦN thay vì chữ đầu — trên toàn bộ curriculum,
để soát từng từ trước khi đụng code.

    python3 scripts/build_fill_chunk_preview.py            # in bảng ra terminal
    python3 scripts/build_fill_chunk_preview.py --json out.json
    python3 scripts/build_fill_chunk_preview.py --html <scratchpad>/fill_chunk_review.html   # trang soát

Luật đã chốt 2026-09-17 và đã vào app: game/filletter/FillChunk.kt. Sửa bên nào thì sửa cả bên kia.

Luật đang mô phỏng (bản đề xuất, chưa chốt):
  1. Đáp án = đúng phần bài dạy nằm trong từ
       L1 chữ cái (fox → fo_) · L2 nguyên âm hoặc vần cuối · L3 tổ hợp nguyên âm / vần magic-e /
       magic-e tách (home → h_m_) · L4 chữ hồng của bảng tách split · L5 tạm dò chuỗi (chưa có split)
  2. Nhiễu = vần CÙNG LOẠI trong unit → thiếu thì lùi unit trước (gần nhất trước, qua cả level)
     → vẫn thiếu mới lấy unit sau (gắn cờ "chưa học")
  3. Bỏ trùng theo mặt chữ (th hữu thanh/vô thanh, y /iː/ /aɪ/ chỉ hiện một thẻ)
  4. L3: vần BAO (a_e unit 1, i_e unit 2) không làm đáp án — cùng quyết định Bubble Pop 2026-08-31

Port tay từ Kotlin — sửa luật bên app thì sửa cả đây:
  step/common/PhonicsPatterns.kt  parsePatterns
  step/common/BlendParts.kt       blendParts
  game/common/WordPatternMatch.kt wordHasPattern
  step/common/ClusterBlend.kt     patternForChunk
"""
import argparse
import json
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CURRICULUM = ROOT / "core/resource/src/commonMain/composeResources/files/curriculum.json"

VOWELS = "aeiou"
MIN_WORD_LEN = 3

# L5 chưa có bảng tách: phép dò chuỗi ra SAI vị trí ở những từ này (soát tay 2026-09-17).
# Chính là bằng chứng L5 phải có cột split như L4 trước khi bật game.
L5_KNOWN_WRONG = {
    "tiger": "phải khuyết i (ti·ger), dò ra e",
    "uniform": "phải khuyết u đầu từ, dò ra o",
    "panda": "phải khuyết a CUỐI (âm ơ), dò ra a đầu (âm a ngắn)",
    "pencil": "phải khuyết i (âm ơ), dò ra e",
    "lemon": "phải khuyết o (âm ơ), dò ra e",
    "surprise": "phải khuyết u (sur·prise), dò ra e câm cuối",
}
CHOICES = 4

# ---------------------------------------------------------------- port từ Kotlin

def parse_patterns(letter: str, level: int) -> list[str]:
    raw = [s for s in letter.strip().lower().split("-") if s]
    if not raw:
        return []
    if level <= 2:
        return raw[2:]
    segs = [s for s in raw if not s.isdigit()]
    if not segs:
        return []
    if "_" in segs[0]:
        return segs[1:] or [segs[0]]
    return segs


VOWEL_TEAMS = ["igh", "ai", "ay", "ee", "ea", "ie", "oa", "ow", "ue", "ui", "ew", "oo", "ey"]
TWO_SYLLABLE = {
    "candy": ["c", "a", "n", "d", "y"],
    "happy": ["h", "a", "p", "y"],
    "money": ["m", "o", "n", "ey"],
    "yellow": ["y", "e", "l", "ow"],
    "pillow": ["p", "i", "l", "ow"],
    "elbow": ["e", "l", "b", "ow"],
    "window": ["w", "i", "n", "d", "ow"],
    "tuesday": ["t", "ue", "s", "d", "ay"],
}


def blend_parts(word: str):
    """[(label, [spans], kind)] — span = (start, end_exclusive)."""
    w = word.lower()
    if w in TWO_SYLLABLE:
        out, cur = [], 0
        for label in TWO_SYLLABLE[w]:
            doubled = cur + 1 < len(w) and len(label) == 1 and w[cur] == label[0] and w[cur + 1] == label[0]
            ln = 2 if doubled else len(label)
            kind = "Vowel" if any(c in VOWELS for c in label) or (label == "y" and cur != 0) else "Letter"
            out.append((label, [(cur, cur + ln)], kind))
            cur += ln
        return out
    nuc = _nucleus(w)
    if nuc is None:
        return []
    pieces = []
    onset_end = nuc[1][0][0]
    if onset_end > 0:
        pieces.append((w[:onset_end], [(0, onset_end)], "Onset"))
    pieces.append(nuc)
    coda_start = nuc[1][0][1]
    coda_end = nuc[1][-1][0] if len(nuc[1]) > 1 else len(w)
    if coda_start < coda_end:
        pieces.append((w[coda_start:coda_end], [(coda_start, coda_end)], "Coda"))
    return pieces


def _nucleus(w):
    if len(w) >= 4 and w.endswith("e"):
        last = len(w) - 1
        v = next((i for i in range(last - 2, -1, -1) if w[i] in VOWELS), None)
        if v is not None:
            between = w[v + 1:last]
            if between and not any(c in VOWELS for c in between):
                return (f"{w[v]}_e", [(v, v + 1), (last, last + 1)], "Vowel")
    for team in VOWEL_TEAMS:
        i = w.find(team)
        if i >= 0:
            return (team, [(i, i + len(team))], "Vowel")
    y = w.rfind("y")
    if y > 0:
        return ("y", [(y, y + 1)], "Vowel")
    v = next((i for i, c in enumerate(w) if c in VOWELS), None)
    if v is not None:
        return (w[v], [(v, v + 1)], "Vowel")
    return None


def is_magic_e_rime(p: str) -> bool:
    return "_" not in p and len(p) == 3 and p[0] in VOWELS and p[1] not in VOWELS and p[2] == "e"


def word_has_pattern(word: str, pattern: str) -> bool:
    w, p = word.strip().lower(), pattern.strip().lower()
    if not w or not p:
        return False
    if is_magic_e_rime(p):
        return w.endswith(p)
    return any(k == "Vowel" and lab == p for lab, _, k in blend_parts(w))


# ---------------------------------------------------------------- luật mới

def role_of(label: str, level: int) -> str:
    """Loại vần — nhiễu chỉ lấy cùng loại. Tách magic-e là HÌNH DẠNG khác nên loại riêng."""
    if level == 1:
        return "letter"
    if level == 2:
        return "vowel" if len(label) == 1 else "rime"
    if level == 3:
        if "_" in label:
            return "split"
        return "rime" if is_magic_e_rime(label) else "vowel"
    if level == 4:
        return "cluster"
    return "combo"


def lesson_labels(lesson, level):
    """Các vần một lesson đóng góp vào kho nhiễu: [(label, role)]."""
    if level == 1:
        lab = lesson["letter"].strip().lower()
        roles = ["letter"]
        return [(lab, r) for r in roles]
    if level == 2:
        pats = parse_patterns(lesson["letter"], 2)
        if not pats:
            return [(lesson["displayLetter"].strip().lower(), "vowel")]
        return [(p, "rime") for p in pats]
    if level in (3, 4):
        return [(p, role_of(p, level)) for p in parse_patterns(lesson["letter"], level)]
    # L5: parsePatterns trả rác ("open", "schwa") — dùng displayLetter.
    return [(p, "combo") for p in lesson["displayLetter"].strip().lower().split() if p]


def vowel_letters_of_l1(lesson):
    """Chữ nguyên âm cấp 1 dạy ĐÚNG âm ngắn của cấp 2 (ahh ehh…) → tính là nguyên âm đã học."""
    lab = lesson["letter"].strip().lower()
    return [(lab, "vowel")] if lab in VOWELS else []


def is_long(label: str) -> bool:
    """Nhóm độ dài: 1–2 chữ ngắn, 3–4 chữ dài; gạch nối magic-e không tính. 4 thẻ phải cùng nhóm."""
    return sum(1 for c in label if c != "_") >= 3


def word_index_map(word: str):
    """Vị trí trong chuỗi bỏ dấu cách → vị trí trong từ gốc."""
    return [i for i, c in enumerate(word) if c != " "]


def chunk_for_word(lesson, w, level, umbrella):
    """(label, spans, note) hoặc (None, None, lý do loại)."""
    word = w["word"]
    lw = word.lower()
    if level == 1:
        lab = lesson["letter"].strip().lower()
        i = lw.find(lab)
        if i < 0:
            return None, None, f"không thấy chữ '{lab}' trong từ"
        return lab, [(i, i + 1)], None
    if level == 2:
        pats = parse_patterns(lesson["letter"], 2)
        if not pats:
            v = lesson["displayLetter"].strip().lower()
            i = lw.find(v)
            if i < 0:
                return None, None, f"không thấy nguyên âm '{v}'"
            return v, [(i, i + 1)], None
        hit = sorted([p for p in pats if lw.endswith(p)], key=len, reverse=True)
        if not hit:
            return None, None, f"từ không kết thúc bằng {'/'.join(pats)}"
        p = hit[0]
        return p, [(len(lw) - len(p), len(lw))], None
    if level == 3:
        pats = [p for p in parse_patterns(lesson["letter"], 3) if p not in umbrella]
        if not pats:
            return None, None, f"vần bao {'/'.join(parse_patterns(lesson['letter'], 3))} — bỏ như Bubble Pop"
        for p in sorted(pats, key=len, reverse=True):
            if not word_has_pattern(lw, p):
                continue
            if is_magic_e_rime(p):
                return p, [(len(lw) - 3, len(lw))], None
            for lab, spans, kind in blend_parts(lw):
                if kind == "Vowel" and lab == p:
                    return p, spans, None
        return None, None, f"blendParts không ra {'/'.join(pats)}"
    if level == 4:
        split, pi = w.get("split"), w.get("patternIndex")
        if not split or pi is None or pi >= len(split):
            return None, None, "thiếu bảng tách split"
        if "".join(split).lower() != lw.replace(" ", ""):
            return None, None, "split ghép lại không ra từ"
        pats = parse_patterns(lesson["letter"], 4)
        chunk = split[pi].lower()
        pat = next((p for p in sorted(pats, key=len, reverse=True) if p in chunk), None)
        if pat is None:
            return None, None, f"mảnh '{chunk}' không chứa {'/'.join(pats)}"
        off = sum(len(c) for c in split[:pi]) + chunk.find(pat)
        idx = word_index_map(word)
        return pat, [(idx[off], idx[off + len(pat) - 1] + 1)], None
    # L5 — tạm: vần dài nhất xuất hiện đầu tiên. KHÔNG phải luật ship, chỉ để thấy chỗ vỡ.
    pats = [p for p in lesson["displayLetter"].strip().lower().split() if p]
    for p in sorted(pats, key=len, reverse=True):
        i = lw.find(p)
        if i >= 0:
            return p, [(i, i + len(p))], "tạm (chưa có split)"
    return None, None, f"không thấy {'/'.join(pats)}"


def apply(word: str, spans, label: str) -> str:
    """Điền label vào chỗ trống. Tách magic-e: ký tự đầu vào ô 1, 'e' vào ô 2."""
    parts = label.split("_") if len(spans) == 2 else [label]
    if len(parts) != len(spans):
        return None
    out, cur = [], 0
    for (s, e), part in zip(spans, parts):
        out.append(word[cur:s])
        out.append(part)
        cur = e
    out.append(word[cur:])
    return "".join(out)


def blank_segments(word: str, spans):
    """[{t: text}|{blank: true}] để trang vẽ ô trống."""
    segs, cur = [], 0
    for s, e in spans:
        if s > cur:
            segs.append({"t": word[cur:s]})
        segs.append({"blank": True, "n": e - s})
        cur = e
    if cur < len(word):
        segs.append({"t": word[cur:]})
    return segs


def build(seed: int = 7):
    data = json.loads(CURRICULUM.read_text())
    rng = random.Random(seed)
    vocab = set()
    units = []  # thứ tự toàn curriculum
    for L in data["levels"]:
        for u in L["units"]:
            units.append((L, u))
            for les in u["lessons"]:
                for w in les["words"]:
                    vocab.add(w["word"].lower())

    # Kho vần của từng unit: [(label, role, lessonId)]
    unit_labels = []
    for L, u in units:
        lv = L["number"]
        labs = []
        for les in u["lessons"]:
            for lab, role in lesson_labels(les, lv):
                labs.append((lab, role, les["id"]))
            if lv == 1:
                for lab, role in vowel_letters_of_l1(les):
                    labs.append((lab, role, les["id"]))
        unit_labels.append(labs)

    out_levels = {}
    for ui, (L, u) in enumerate(units):
        lv = L["number"]
        all_words = [w["word"] for les in u["lessons"] for w in les["words"]]
        umbrella = set()
        if lv == 3:
            pats = {p for les in u["lessons"] for p in parse_patterns(les["letter"], 3)}
            umbrella = {p for p in pats if all(word_has_pattern(x, p) for x in all_words)}

        words_out = []
        n = 0
        for les in u["lessons"]:
            for w in les["words"]:
                n += 1
                code = f"{u['id']}-{n:02d}"
                word = w["word"]
                disp = next((d for d in w.get("displays", []) if d["type"] == "image"), None)
                row = {
                    "code": code, "word": word, "lesson": les["id"],
                    "emoji": w.get("emoji"), "image": disp["path"] if disp else None,
                }
                if len(word) < MIN_WORD_LEN:
                    row["excluded"] = f"từ ngắn hơn {MIN_WORD_LEN} chữ (game hiện tại đã loại)"
                    words_out.append(row)
                    continue
                label, spans, note = chunk_for_word(les, w, lv, umbrella)
                if label is None:
                    row["excluded"] = note
                    words_out.append(row)
                    continue
                role = role_of(label, lv)
                # Gom nhiễu: unit mình → lùi dần → tiến dần
                tiers = []
                seen = {label}

                long_answer = is_long(label)

                def take(idx, tag, accept):
                    got = []
                    for lab, r, _ in unit_labels[idx]:
                        if is_long(lab) == long_answer and accept(r) and lab not in seen:
                            seen.add(lab)
                            got.append(lab)
                    if got:
                        U = units[idx][1]["id"]
                        tiers.append({"src": U, "tag": tag, "labels": got})
                    return len(got)

                def sweep(accept, other_kind=False):
                    got = 0
                    order = [(ui, "unit")] + [(j, "prev") for j in range(ui - 1, -1, -1)] \
                        + [(j, "next") for j in range(ui + 1, len(units))]
                    for j, tag in order:
                        if have + got >= CHOICES - 1:
                            break
                        n = take(j, tag, accept)
                        if n and other_kind:
                            tiers[-1]["otherKind"] = True
                        got += n
                    return got

                # Lượt 1: cùng loại + cùng nhóm độ dài. Lượt 2: mọi loại liền khối + cùng nhóm độ dài.
                have = 0
                have += sweep(lambda r: r == role)
                if have < CHOICES - 1 and role != "split":
                    have += sweep(lambda r: r != "split", other_kind=True)

                # Chọn 3: hết tầng này mới sang tầng sau, trong tầng thì xáo.
                picked = []
                for t in tiers:
                    pool = t["labels"][:]
                    rng.shuffle(pool)
                    for lab in pool:
                        if len(picked) < CHOICES - 1:
                            picked.append((lab, t["src"], t["tag"], t.get("otherKind", False)))
                options = [{"label": label, "correct": True}] + [
                    {"label": lab, "src": src, "tag": tag, "otherKind": ok} for lab, src, tag, ok in picked
                ]
                rng.shuffle(options)

                # Nhiễu nào ghép ra một từ KHÁC có trong app
                collisions = []
                for t in tiers:
                    for lab in t["labels"]:
                        filled = apply(word.lower(), spans, lab)
                        if filled and filled != word.lower() and filled in vocab:
                            collisions.append({"label": lab, "makes": filled})
                for o in options:
                    if not o.get("correct"):
                        filled = apply(word.lower(), spans, o["label"])
                        o["makes"] = filled
                        o["vocab"] = bool(filled and filled in vocab)
                        o["fits"] = filled is not None

                flags = []
                if spans[0][0] == 0 and spans[-1][1] == len(word):
                    flags.append({"kind": "crit", "text": "khuyết cả từ — không còn chữ nào để đoán"})
                if lv == 5 and word.lower() in L5_KNOWN_WRONG:
                    flags.append({"kind": "crit", "text": L5_KNOWN_WRONG[word.lower()]})
                if sum(1 for x in all_words if x.lower() == word.lower()) > 1:
                    flags.append({"kind": "warn", "text": "từ có 2 lần trong unit — vòng có thể lặp"})
                if any(t.get("otherKind") for t in tiers):
                    flags.append({"kind": "info", "text": "không có thẻ cùng loại cùng độ dài — lấy thẻ khác loại"})
                if any(t["tag"] == "next" for t in tiers):
                    src = next(t["src"] for t in tiers if t["tag"] == "next")
                    flags.append({"kind": "warn", "text": f"không có unit trước đủ vần — lấy {src} (chưa học)"})
                row.update({
                    "flags": flags,
                    "answer": label, "role": role, "segments": blank_segments(word, spans),
                    "tiers": tiers, "options": options, "collisions": collisions,
                    "note": note, "short": have < CHOICES - 1,
                })
                words_out.append(row)

        pool_tiers = []
        roles_in_unit = []
        for lab, r, _ in unit_labels[ui]:
            if lv == 1 and r == "vowel":
                continue
            if lab not in [x[0] for x in roles_in_unit]:
                roles_in_unit.append((lab, r))
        out_levels.setdefault(L["id"], {"id": L["id"], "title": L["title"], "units": []})["units"].append({
            "id": u["id"], "title": u["title"],
            "labels": [{"label": lab, "role": r} for lab, r in roles_in_unit],
            "umbrella": sorted(umbrella),
            "words": words_out,
        })
    return list(out_levels.values())


RES = ROOT / "core/resource/src/commonMain/composeResources"
TEMPLATE = Path(__file__).resolve().parent / "fill_chunk_preview_template.html"


def add_stats(levels):
    for L in levels:
        words = [w for u in L["units"] for w in u["words"]]
        live = [w for w in words if "excluded" not in w]
        L["stats"] = {
            "total": len(words),
            "playable": len(live),
            "excluded": len(words) - len(live),
            "prev": sum(1 for w in live if any(t["tag"] == "prev" for t in w["tiers"])),
            "next": sum(1 for w in live if any(t["tag"] == "next" for t in w["tiers"])),
            "collide": sum(1 for w in live if w["collisions"]),
            "crit": sum(1 for w in live if any(f["kind"] == "crit" for f in w["flags"])),
        }


def write_html(levels, out: Path):
    import base64
    images = {}
    for L in levels:
        for u in L["units"]:
            for w in u["words"]:
                path = w.get("image")
                if path and path not in images and (RES / path).exists():
                    b64 = base64.b64encode((RES / path).read_bytes()).decode()
                    images[path] = f"data:image/webp;base64,{b64}"
    html = TEMPLATE.read_text()
    html = html.replace("__DATA__", json.dumps(levels, ensure_ascii=False))
    html = html.replace("__IMAGES__", json.dumps(images))
    out.write_text(html)
    print(f"wrote {out} ({out.stat().st_size // 1024} KB, {len(images)} ảnh)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--json")
    ap.add_argument("--html")
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()
    levels = build(args.seed)
    add_stats(levels)
    if args.html:
        write_html(levels, Path(args.html))
        for L in levels:
            print(L["id"], L["stats"])
        return
    if args.json:
        Path(args.json).write_text(json.dumps(levels, ensure_ascii=False, indent=1))
    for L in levels:
        print(f"===== {L['id']} {L['title']}")
        for u in L["units"]:
            labs = " ".join(f"{x['label']}({x['role'][0]})" for x in u["labels"])
            print(f"  -- {u['id']}  [{labs}]" + (f"  umbrella={u['umbrella']}" if u["umbrella"] else ""))
            for w in u["words"]:
                if "excluded" in w:
                    print(f"     {w['code']} {w['word']:<12} LOẠI: {w['excluded']}")
                    continue
                blank = "".join(s.get("t", "▢" if s.get("blank") else "") for s in w["segments"])
                opts = " ".join(("*" if o.get("correct") else "") + o["label"]
                                + ("" if o.get("correct") or o.get("tag") == "unit" else f"<{o['tag']}:{o['src']}>")
                                + ("!" + o["makes"] if o.get("vocab") else "")
                                for o in w["options"])
                col = " ".join(f"{c['label']}→{c['makes']}" for c in w["collisions"])
                flags = (" SHORT" if w["short"] else "") + (f" ~{w['note']}" if w["note"] else "")
                print(f"     {w['code']} {w['word']:<12} {blank:<12} {opts}{flags}" + (f"   [trùng: {col}]" if col else ""))


if __name__ == "__main__":
    main()
