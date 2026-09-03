#!/usr/bin/env python3
"""Extract the three Play Console fields out of `<locale>/listing.md` into `<locale>/paste.txt`.

Why this exists: `listing.md` is a working document — draft copy interleaved with rationale
tables, risk notes and verification sources. None of that goes in Play Console, and hunting
for the right fenced block while copy-pasting into a browser is how stale or half-copied
text reaches the store. `paste.txt` holds only what a human pastes, with the character
counts already checked against Play's limits.

    python3 marketing/store-listing/make-paste.py            # every shippable locale
    python3 marketing/store-listing/make-paste.py vi-VN      # just one

Locales whose `listing.md` is flagged STALE are skipped — seven of them were written for
Mốc 1 and never updated, and generating a tidy paste-ready file for copy nobody vetted is
exactly how stale text reaches the store.

Never hand-edit `paste.txt` — edit `listing.md` and re-run, otherwise the two drift and the
document that explains the copy stops describing the copy that actually shipped.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).parent

# Play Console field limits. Exceeding these is rejected at paste time, so fail loudly here
# instead of letting someone discover it in the browser with the tab already open.
LIMITS = {"App title": 30, "Short description": 80, "Long description": 4000}

# Each field is the first fenced block after its `## <name>` heading. Matching on the heading
# rather than block order means adding a code block elsewhere in the document cannot silently
# shift which text gets published.
HEADING = re.compile(r"^##\s+(App title|Short description|Long description)\b.*$", re.M)
FENCE = re.compile(r"```[^\n]*\n(.*?)```", re.S)

# The stale locales carry this marker in their own header. Honour it rather than keeping a
# hard-coded allowlist here, so un-staling a locale is a one-line edit in that locale's file.
STALE = re.compile(r"^>?\s*🛑?\s*\*\*STALE", re.M)


def extract(md: str) -> dict[str, str]:
    fields = {}
    for m in HEADING.finditer(md):
        block = FENCE.search(md, m.end())
        if not block:
            raise SystemExit(f"No fenced block after heading: {m.group(0)!r}")
        fields[m.group(1)] = block.group(1).strip("\n")
    missing = set(LIMITS) - set(fields)
    if missing:
        raise SystemExit(f"Missing section(s): {', '.join(sorted(missing))}")
    return fields


def render(locale: str, fields: dict[str, str]) -> str:
    out = [
        f"Play Console → Store listing → {locale}",
        "GENERATED from listing.md by make-paste.py — do not hand-edit.",
        "",
    ]
    for name, limit in LIMITS.items():
        # Play counts characters, not bytes; Vietnamese is stored NFC here so len() matches
        # what the Console's own counter shows.
        text = fields[name]
        out += [
            "=" * 72,
            f"{name.upper()}  ({len(text)}/{limit} chars)",
            "=" * 72,
            "",
            text,
            "",
        ]
    return "\n".join(out)


def main(argv: list[str]) -> int:
    locales = argv[1:] or sorted(p.parent.name for p in ROOT.glob("*/listing.md"))
    for locale in locales:
        src = ROOT / locale / "listing.md"
        if not src.exists():
            print(f"skip {locale}: no listing.md")
            continue
        md = src.read_text()
        if STALE.search(md):
            print(f"skip {locale}: listing.md is flagged STALE")
            continue
        fields = extract(md)
        over = [
            f"{n} is {len(fields[n])}/{lim}"
            for n, lim in LIMITS.items()
            if len(fields[n]) > lim
        ]
        if over:
            raise SystemExit(f"{locale}: over Play's limit — {'; '.join(over)}")
        dst = ROOT / locale / "paste.txt"
        dst.write_text(render(locale, fields))
        counts = "  ".join(f"{n.split()[0].lower()} {len(fields[n])}/{lim}" for n, lim in LIMITS.items())
        print(f"{locale}: {dst.relative_to(ROOT.parent.parent)}   {counts}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
