#!/usr/bin/env python3
"""Print what has to happen at publish time for a release to force an update.

There is nothing to build in. Play's `inAppUpdatePriority` is per-release metadata
on the track, and the app that reads it is the one already installed on the device —
so the AAB being uploaded cannot influence whether users are forced onto it. The
only lever is set after upload, and only through the Publishing API: the Play
Console UI has no field for it.

Run with no arguments; it reads the version being shipped from libs.versions.toml.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
TOML = ROOT / "gradle" / "libs.versions.toml"
CONTROLLER = (
    ROOT / "composeApp/src/androidMain/kotlin/me/ltthuc/kmp/update/InAppUpdateController.kt"
)


def version() -> tuple[str, str]:
    text = TOML.read_text()
    name = re.search(r'^versionName\s*=\s*"([^"]*)"', text, re.M)
    code = re.search(r'^versionCode\s*=\s*"([^"]*)"', text, re.M)
    if not (name and code):
        sys.exit("could not read the version from libs.versions.toml")
    return name.group(1), code.group(1)


def threshold() -> str:
    if not CONTROLLER.is_file():
        return "4"
    found = re.search(r"IMMEDIATE_PRIORITY\s*=\s*(\d+)", CONTROLLER.read_text())
    return found.group(1) if found else "4"


def main() -> int:
    name, code = version()
    need = threshold()

    print(f"""
FORCE UPDATE — publish-time steps for {name} (versionCode {code})

The AAB carries none of this. `inAppUpdatePriority` lives on the Play track
release, and the app that acts on it is the one already installed, so this must be
set after the bundle is uploaded.

1. Upload the AAB to the track as usual, but do NOT roll it out yet.

2. Set the priority through the Publishing API — the Console has no field for it:

     edits.tracks.update
       track:    production        (or the track you are shipping)
       releases: [ {{
         versionCodes:        ["{code}"],
         inAppUpdatePriority: {need},
         status:              "completed"
       }} ]

   {need} is what InAppUpdateController treats as force-worthy; anything lower
   leaves users on the quiet flexible flow.

3. Roll out.

WHO ACTUALLY GETS FORCED
Only devices whose installed build contains InAppUpdateController. Users on any
version older than the first release that shipped it read no priority at all and
see nothing — they update on their own schedule, or not at all.

NO AUTOMATION HERE
This repo has no service account, fastlane, or CI publish path, so the call above
is manual. Wiring one is the only way to stop this being a step someone forgets.
""".strip())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
