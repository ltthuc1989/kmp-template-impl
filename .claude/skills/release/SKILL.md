---
name: release
description: Bump the app version and build the signed release AAB for ABC Phonics Kids. Use when the user asks to "tăng version", "bump version", "build release", "ra bản mới", "làm bản release", or wants an AAB for Play Console. Also drafts Play release notes (EN+VI) via --notes / --notes-only when the user asks for "release notes", "release description", "viết description".
---

# Release — bump version, build the AAB

Two steps. Do them, report the file. Do not run tests, detekt, or a git-status
review unless the user asks — they have their own commands for that, and this
skill exists to be fast.

**Args**: none for a normal patch bump. `--to <versionCode>` to jump to a specific
code. `--no-bump` to build the current version unchanged. `--force-update` to also
print what publishing must do for this release to force an update. `--notes` to
also draft Play release notes after the build. `--notes-only` to draft just the
notes — no bump, no build.

## 1. Bump

```bash
python3 .claude/skills/release/scripts/bump_version.py
```

The script owns the rule `versionName = 0.0.<versionCode>` and refuses to run from
an inconsistent state. Never hand-edit those two lines. Skip this step on `--no-bump`.

## 2. Build

```bash
./gradlew :composeApp:bundleRelease
```

Several minutes — release runs R8 with `isMinifyEnabled` and `isShrinkResources`.
Add `:composeApp:assembleRelease` only if the user also wants an APK to sideload.

## 3. Report

Give the path, the size, and the version read back out of the artifact:

```bash
ls -la composeApp/build/outputs/bundle/release/*.aab
AAPT=$(ls ~/Library/Android/sdk/build-tools/*/aapt2 | tail -1)
"$AAPT" dump badging composeApp/build/outputs/apk/release/composeApp-release.apk \
  | grep -E "^package|debuggable"
```

That last command needs an APK, so run it only when one was built. Expect the
version to match the bump and **no** `debuggable` line — a `debuggable` line means
the `billing` variant got built instead, which Play rejects on upload. The two
variants share an `applicationId` and a signing key, so the filename is the only
other thing telling them apart.

## `--force-update`

```bash
python3 .claude/skills/release/scripts/force_update_notes.py
```

Run it after the build and show the output verbatim. Say plainly that the flag
changed nothing in the AAB, because it cannot: `inAppUpdatePriority` is per-release
metadata on the Play track, read by the build already on the device, so no bundle
can decide whether users are forced onto it. The flag exists to stop the one manual
step from being forgotten, not to bake anything in.

## `--notes` / `--notes-only`

Draft the "Release notes" text for the Play Console release page. This is writing,
not a script — do it yourself:

1. Find what changed: `git log --oneline` since the previous `release version` /
   bump commit. If the release ships new curriculum content, read
   `core/resource/src/commonMain/composeResources/files/curriculum.json` for the
   level/unit names instead of guessing.
2. Write for parents, not engineers. Lead with the headline feature, then short
   `•` bullets. No versionCode, no internal jargon (packs, R8, flags).
3. Output one paste-ready block in Play's language-tag format, both languages:

   ```
   <en-US>
   ...
   </en-US>
   <vi>
   ...
   </vi>
   ```

   Hard limit **500 characters per language** — count before presenting. EN + VI
   only (project decision: store scope is EN+VN, not 9 locales). Remind the user
   the `<vi>` tag only works if the listing already has Vietnamese added;
   otherwise they keep just the `<en-US>` block.

## Not this skill's job

No commit, no tag, no Play upload, no iOS build (that target does not compile at
HEAD). Mention them only if the user asks.

## One thing worth saying out loud when it applies

If the release carries new audio or images, offer `--force-update`. Content ships
inside the APK's manifest, so users who never update never see it — and a release
published at the default priority 0 reaches them only whenever they happen to
update on their own.
