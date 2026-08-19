#!/usr/bin/env python3
"""Bump the app version in gradle/libs.versions.toml.

The project keeps one invariant, set deliberately after a release broke it:
`versionName` is `0.0.<versionCode>`. One number to reason about, and a build's
name tells you its code without opening anything. This script is the only thing
that should ever write those two lines, so the pair cannot drift again.

    python3 bump_version.py --dry-run    # show what would change
    python3 bump_version.py              # apply
    python3 bump_version.py --to 12      # jump to a specific versionCode

Exits non-zero and changes nothing if the file's current values already violate
the invariant — that is a decision for a human, not something to silently "fix".
"""

import argparse
import pathlib
import re
import sys

TOML = pathlib.Path(__file__).resolve().parents[4] / "gradle" / "libs.versions.toml"

NAME_RE = re.compile(r'^versionName\s*=\s*"([^"]*)"', re.M)
CODE_RE = re.compile(r'^versionCode\s*=\s*"([^"]*)"', re.M)


def read() -> tuple[str, int, str]:
    if not TOML.is_file():
        sys.exit(f"not found: {TOML}")
    text = TOML.read_text()
    name = NAME_RE.search(text)
    code = CODE_RE.search(text)
    if not (name and code):
        sys.exit("versionName / versionCode not found in libs.versions.toml")
    try:
        return name.group(1), int(code.group(1)), text
    except ValueError:
        sys.exit(f"versionCode is not an integer: {code.group(1)!r}")


def check_invariant(name: str, code: int) -> None:
    expected = f"0.0.{code}"
    if name != expected:
        sys.exit(
            f"versionName {name!r} does not match versionCode {code} "
            f"(expected {expected!r}).\n"
            "Refusing to bump from an inconsistent state — fix the pair by hand, "
            "or pass --to to set both explicitly."
        )


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--dry-run", action="store_true", help="print the change, write nothing")
    ap.add_argument("--to", type=int, metavar="CODE", help="target versionCode instead of +1")
    args = ap.parse_args()

    name, code, text = read()
    if args.to is None:
        check_invariant(name, code)
        new_code = code + 1
    else:
        if args.to <= code:
            sys.exit(f"--to {args.to} is not above the current versionCode {code}; Play rejects that")
        new_code = args.to
    new_name = f"0.0.{new_code}"

    print(f"versionName  {name}  ->  {new_name}")
    print(f"versionCode  {code}  ->  {new_code}")

    if args.dry_run:
        print("\n(dry run — nothing written)")
        return 0

    text = NAME_RE.sub(f'versionName = "{new_name}"', text, count=1)
    text = CODE_RE.sub(f'versionCode = "{new_code}"', text, count=1)
    TOML.write_text(text)

    # Read back rather than trust the substitution: this file gates every release.
    after_name, after_code, _ = read()
    if (after_name, after_code) != (new_name, new_code):
        sys.exit(f"write-back check failed: file now says {after_name} / {after_code}")
    print(f"\nwritten to {TOML}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
