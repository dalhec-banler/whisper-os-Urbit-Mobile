# ROM Patch Sets

Patch sets in this directory are source-only changes that apply to upstream
Android or GrapheneOS projects outside `vendor/nativeplanet`.

## `launcher3-whisper-os-v2.patch`

Applies the current Whisper OS Launcher3/Quickstep integration.

Generated against the GrapheneOS `2026040800` tag of `packages/apps/Launcher3`
with:

```bash
cd packages/apps/Launcher3
git diff --binary 2026040800 > /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v2.patch
```

Regenerate it the same way whenever Launcher3 source changes so the product
repo stays in sync with the build tree.

This patch keeps native Android launcher behavior as the product shell:

- native HOME role through Launcher3/Quickstep
- Android gestures, recents, drag/drop, app drawer, widgets, and workspace logic
- Whisper OS colors and provenance styling
- a first-party `My Urbit Apps` surface
- local-host-only cleartext for Urbit web app shells

It intentionally does not include the earlier custom `WhisperHomeActivity`
prototype. That prototype bypassed native Launcher3 behavior and should not be
revived as the HOME shell.

Apply from the root of a compatible GrapheneOS checkout:

```bash
cd packages/apps/Launcher3
git apply /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v2.patch
```
