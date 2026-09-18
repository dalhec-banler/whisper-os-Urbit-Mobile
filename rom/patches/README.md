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

HOME is Whisper Home (`home/`). This patch supplies the hosted-app WebView task
host (`WhisperHostedWebActivity`, reached through
`io.nativeplanet.action.OPEN_URBIT_APP`) and local-host-only cleartext for
Urbit web app shells. Its `My Urbit Apps` surface and Whisper OS styling are
carried along but no longer user-facing.

Apply from the root of a compatible GrapheneOS checkout:

```bash
cd packages/apps/Launcher3
git apply /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v2.patch
```

## `launcher3-whisper-os-v3-hosted-app-tasks.patch`

Follow-on to v2 (apply v2 first). Found on device 2026-09-15 against ROM
`2026062202`: every hosted Urbit app opened into the same standard task, so
after opening one hosted app and going Home, tapping a different hosted app
only brought the old task to the front and showed the previous app. The patch
makes `WhisperHostedWebActivity` a document activity keyed by
`urbit-app://<desk>` (`documentLaunchMode="intoExisting"`, `onNewIntent`), so
each Urbit app is its own task in recents, and pads the content by the IME
inset so the terminal prompt is not hidden under the keyboard.

```bash
cd packages/apps/Launcher3
git apply /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v2.patch
git apply -C1 /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v3-hosted-app-tasks.patch
```

The v3 manifest hunks were written without the full manifest at hand, so their
line numbers are approximate; `-C1` lets `git apply` place them by context.

## `launcher3-whisper-os-v3.patch`

Consolidated patch: v2 plus the hosted-app-tasks fix in one file, regenerated
2026-09-18 from a build tree with both applied (`git diff --binary 2026040800`).
Verified to apply cleanly to a pristine `2026040800` checkout. Prefer this over
the two-step apply:

```bash
cd packages/apps/Launcher3
git apply /path/to/whisper-os-Urbit-Mobile/rom/patches/launcher3-whisper-os-v3.patch
```

The two files above are retained for history; new checkouts should use only
this consolidated patch.
