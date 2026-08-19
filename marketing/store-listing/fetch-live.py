#!/usr/bin/env python3
"""Snapshot the live Play Store listing into `<locale>/live.md`.

Why this exists: the store listing is edited in Play Console, not in this repo, so the
repo drifts from what users actually see. Run this before touching any `listing.md` so
you are diffing your draft against the real live copy, not against a stale memory of it.

    python3 marketing/store-listing/fetch-live.py            # all known locales
    python3 marketing/store-listing/fetch-live.py en-US      # just one

Then: `git diff marketing/store-listing` shows exactly what changed on the store since
the last snapshot. Commit the snapshot so the next drift is visible too.

Scraping caveat: this parses Play's server-rendered HTML, which Google changes without
notice. If a field comes back empty the page shape moved — check the raw HTML that gets
left in the system temp dir on failure rather than trusting a blank result.
"""

import html
import pathlib
import re
import sys
import urllib.request
from datetime import date

PACKAGE = "com.beely.phonicskids"
ROOT = pathlib.Path(__file__).parent

# locale dir -> (hl, gl) query params Play expects
LOCALES = {
    "en-US": ("en_US", "US"),
    "vi-VN": ("vi", "VN"),
}

UA = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120 Safari/537.36"
)


def fetch(hl: str, gl: str) -> str:
    url = f"https://play.google.com/store/apps/details?id={PACKAGE}&hl={hl}&gl={gl}"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8", errors="replace")


def meta(page: str, prop: str) -> str | None:
    m = re.search(rf'<meta[^>]*{re.escape(prop)}[^>]*content="([^"]*)"', page)
    return html.unescape(m.group(1)) if m else None


def long_description(page: str, short: str) -> str | None:
    """The long description sits between the short description and its repeat.

    Play renders the short description as an attribute value, then the long description
    as the element body, then echoes the short description again further down. Anchoring
    on the short description avoids depending on Play's obfuscated class names.

    The `<meta name="description">` tag in <head> ends in `">` too, so it matches the same
    anchor — and because the next echo of the short description is thousands of characters
    away, that decoy is *longer* than the real body. So reject candidates carrying page
    chrome (inline JS, nav links) first, then take the longest of what survives.
    """
    junk = ("AF_initDataKeys", "window.wiz", "google_logo", "<meta", "<script")
    escaped = html.escape(short, quote=True)
    needle = escaped + '">'
    best = None
    pos = page.find(needle)
    while pos >= 0:
        start = pos + len(needle)
        end = page.find(escaped, start)
        raw = page[start : end if end > start else start + 8000]
        if not any(marker in raw for marker in junk):
            body = (
                raw.replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\n", "\n")
                .replace("\\u0026", "&")
            )
            body = re.sub(r"<br\s*/?>", "\n", body)
            body = re.sub(r"<[^>]+>", "", body)
            # The slice can end mid-tag, leaving an unterminated `<div class="…` tail
            # that the paired-delimiter pattern above cannot match.
            body = re.sub(r"<[^>]*$", "", body)
            body = html.unescape(body).strip()
            if best is None or len(body) > len(best):
                best = body
        pos = page.find(needle, pos + 1)
    return best or None


def updated_on(page: str) -> str:
    """Play puts markup between the label and the date, so strip tags before matching."""
    for label in ("Updated on", "Lần cập nhật gần đây nhất"):
        i = page.find(label)
        if i < 0:
            continue
        window = " ".join(re.sub(r"<[^>]+>", " ", page[i : i + 600]).split())
        m = re.search(r"([A-Z][a-z]{2} \d{1,2}, \d{4}|\d{1,2} thg \d{1,2}, \d{4})", window)
        if m:
            return m.group(1)
    return "unknown"


def snapshot(locale: str) -> bool:
    hl, gl = LOCALES[locale]
    page = fetch(hl, gl)

    title = (meta(page, "og:title") or "").split(" - ")[0].strip()
    short = meta(page, 'name="description"') or ""
    body = long_description(page, short) if short else None

    if not (title and short and body):
        dump = pathlib.Path(f"/tmp/play-{locale}.html")
        dump.write_text(page)
        print(f"  !! {locale}: could not parse (title={bool(title)} short={bool(short)} "
              f"long={bool(body)}) — raw HTML saved to {dump}")
        return False

    has_iap = "In-app purchases" in page or "Mua hàng trong ứng dụng" in page
    has_ads = "Contains ads" in page or "Có quảng cáo" in page

    out = ROOT / locale / "live.md"
    out.write_text(
        f"""# LIVE Play Store listing — {locale}

> 🔵 **Auto-generated snapshot of what is live RIGHT NOW. Do not hand-edit.**
> Regenerate with `python3 marketing/store-listing/fetch-live.py {locale}`.
> The editable draft lives in `listing.md` — diff it against this file before
> pasting anything into Play Console.

| | |
|---|---|
| Package | `{PACKAGE}` |
| URL | https://play.google.com/store/apps/details?id={PACKAGE}&hl={hl} |
| Store "Updated on" | {updated_on(page)} |
| Snapshot fetched | {date.today().isoformat()} |
| Badge: in-app purchases | {"✅ shown" if has_iap else "— not shown"} |
| Badge: contains ads | {"⚠️ shown" if has_ads else "✅ not shown"} |

## App title ({len(title)} chars)

```
{title}
```

## Short description ({len(short)} chars)

```
{short}
```

## Long description ({len(body)} chars)

```
{body}
```
""",
        encoding="utf-8",
    )
    print(f"  ✓ {locale}: title={len(title)} short={len(short)} long={len(body)} -> {out}")
    return True


def main() -> int:
    wanted = sys.argv[1:] or list(LOCALES)
    unknown = [locale for locale in wanted if locale not in LOCALES]
    if unknown:
        print(f"Unknown locale(s): {', '.join(unknown)}. Known: {', '.join(LOCALES)}")
        return 2

    print(f"Fetching live listing for {PACKAGE}")
    return 0 if all([snapshot(locale) for locale in wanted]) else 1


if __name__ == "__main__":
    raise SystemExit(main())
