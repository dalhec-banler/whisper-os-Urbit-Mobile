# ROM build checklist for the Linux machine

Status: current, 2026-09-18. What to carry from a Mac session into the
GrapheneOS tree and how to rebuild with the least work.

The ROM builds only on x86_64 Linux inside a GrapheneOS checkout. Everything
else in this repo (the home app, the desks, the runtime binary) can be
sideloaded onto a userdebug phone from a Mac, and a phone set up that way is
the reference for what the next ROM must contain. This page is the handoff.

## What the ROM must carry after 2026-09-18

| Piece | Source | Where it goes in the GrapheneOS tree |
|---|---|---|
| vere, 64-bit edge | `tools/build-vere-android.sh` output, `vere64-develop-<sha>-linux-aarch64` | `vendor/nativeplanet/prebuilts/bin/vere` |
| Whisper Home | `home/`, `./gradlew assembleRelease` (unsigned is fine: `android_app_import` re-signs with the platform key) | `vendor/nativeplanet/prebuilts/apk/WhisperHome.apk`, priv-app, HOME category |
| Launcher3 task host | `rom/patches/launcher3-whisper-os-v3.patch` (consolidated; supersedes v2 + v3-hosted-app-tasks) | `packages/apps/Launcher3` |
| Controller | `rom/vendor/nativeplanet/controller/` | `vendor/nativeplanet/controller` |
| Satellite pill and desks | `satellite-pill/` | `vendor/nativeplanet/prebuilts/etc/nativeplanet/satellite.pill` |

Record the vere commit and SHA-256 with the build (the tree keeps this in
`vendor/nativeplanet/prebuilts/bin/vere.provenance`). The mirror desk
(`satellite-pill/desks/nativeplanet-mobile`) is still installed per moon by
`tools/install-mobile-metadata-desk.sh`; baking it into the pill is open.

On the Linux machine vere builds directly on the host — the container in
`tools/build-vere-android.sh` is only a macOS workaround. Fetch zig 0.15.2 for
x86_64-linux and run the same
`zig build -Dtarget=aarch64-linux-musl -Drelease -Dpace=edge -Dvere64=true`
from a real clone of `urbit/vere` `develop` (a git worktree fails: vere's
`build.zig` opens `.git/logs/HEAD`, which is not a directory in a worktree).

Whisper Home's HOME intent-filter carries `android:priority="1"`. Launcher3
stays in the image as the hosted-app task host and still declares HOME; at
equal priority Android's role controller resolves no default home at all and
first boot lands in a chooser.

## Incremental build

If the `out/` tree from the last ROM is still there, only the changed modules
rebuild. Expect tens of minutes, not hours.

```bash
cd $GRAPHENEOS
source build/envsetup.sh
lunch husky bp4a userdebug

cp /path/to/vere64-develop-<sha>-linux-aarch64 vendor/nativeplanet/prebuilts/bin/vere
cp /path/to/WhisperHome.apk vendor/nativeplanet/prebuilts/apk/WhisperHome.apk
git -C packages/apps/Launcher3 apply /path/to/rom/patches/launcher3-whisper-os-v3.patch

m NativePlanetController Launcher3QuickStep -j"$(nproc)"   # cheap check first
m -j"$(nproc)"                                              # the image
script/release.sh husky                                     # signed OTA and factory images
```

A patch that fails to apply means the Launcher3 tree already has it, or the
GrapheneOS tag moved; `git -C packages/apps/Launcher3 log --oneline -3` says
which.

## Clean build

Only when `out/` is gone. Needs about 64 GB of RAM and 300 GB of disk, and
several hours. The GrapheneOS build docs are the authority for the checkout;
the NativePlanet steps are the same as above after `lunch`.

## Flash and verify

`docs/runtime/build-and-flash.md` has the flash sequence. After the phone
boots, run the three smoke tests from the Mac:

```bash
tools/smoke-controller-provider.sh
tools/smoke-launcher-ui.sh
tools/smoke-hosted-mobile-apps.sh
```

Then pair through Whisper Home's setup, install the mirror desk with
`tools/install-mobile-metadata-desk.sh`, and confirm Settings reads
"acting as <planet>".

## Sideloading instead of flashing

On a userdebug phone every piece except the Launcher3 patch and the pill can
be replaced without a ROM build:

```bash
adb root && adb remount
adb push vere64-develop-<sha>-linux-aarch64 /system_ext/bin/vere.new
adb shell 'chmod 755 /system_ext/bin/vere.new && chown root:shell /system_ext/bin/vere.new \
  && mv /system_ext/bin/vere.new /system_ext/bin/vere && restorecon /system_ext/bin/vere'
adb install -r home/app/build/outputs/apk/debug/app-debug.apk
```

Stop the ship through the controller before swapping vere and start it after.

Known limit, 2026-09-18: a pier the 4.3 build created does not survive the
32-to-64 loom migration on the 5.0 edge binary. It bails in `u3j_ream`
during `u3_migrate_d` and the ship stays stopped. A pier booted fresh under
the new binary works (2026-09-15 device recheck). So the new runtime goes
with a fresh moon, not over an existing pier; the phone keeps
`/system_ext/bin/vere.5.0-edge-e162414` beside the 4.3 `vere` for that.
