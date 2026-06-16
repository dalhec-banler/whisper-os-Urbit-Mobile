# Build And Flash Guide

Current target: Pixel 8 Pro / `husky`, Android release `bp4a`, `userdebug`.

This guide is intentionally conservative. A full ROM build is expensive; run cheap module validation first, verify target-files contents before release generation, and do not flash without explicit approval.

## Prerequisites

You need local checkouts of:

- GrapheneOS (appropriate tag for your device)
- Vere fork (this repo's sibling or upstream)
- Launcher (whisper-launcher)

The examples below assume you're in the relevant directory.

## Build Vere64

Use modern Vere. Do not regress to old/32-bit runtime.

```bash
# From your vere checkout
zig build \
  -Dtarget=aarch64-linux-musl \
  -Doptimize=ReleaseFast \
  -Dandroid=true \
  --summary all
```

Output:

```text
zig-out/aarch64-linux-musl/urbit
```

Copy to the GrapheneOS vendor tree only when intentionally updating the ROM prebuilt:

```bash
cp zig-out/aarch64-linux-musl/urbit \
  $GRAPHENEOS/vendor/nativeplanet/prebuilts/bin/vere
```

Record SHA256 and provenance. Do not commit the binary to this repository.

## Cheap Controller Validation

Use this before any full ROM build if controller Java, manifest, or sepolicy source changed.

```bash
# From your GrapheneOS checkout
source build/envsetup.sh
lunch husky bp4a userdebug
m NativePlanetController -j10
```

Expected output ends with:

```text
#### build completed successfully
```

Verify classes when relevant:

```bash
strings out/target/product/husky/system_ext/priv-app/NativePlanetController/NativePlanetController.apk \
  | grep -E 'RuntimeControl|ConnSockClient|NounCodec|RuntimeStatusPoller'
```

## Launcher ROM Prebuilt

The launcher source lives in this repository under `launcher/`. Build it with
Gradle, then copy the APK into the local GrapheneOS vendor tree before building
the ROM.

```bash
# From this repository
( cd launcher && ANDROID_HOME=$ANDROID_HOME ./gradlew assembleDebug )
```

Copy the APK into the active GrapheneOS checkout:

```bash
cp launcher/app/build/outputs/apk/debug/app-debug.apk \
  $GRAPHENEOS/vendor/nativeplanet/prebuilts/apk/NativePlanetLauncher.apk
```

The ROM imports this APK as `NativePlanetLauncher`, installs it under
`system_ext/priv-app`, and signs it with the ROM platform key. Do not commit the
APK binary to this repository.

Cheap module gate:

```bash
# From your GrapheneOS checkout
source build/envsetup.sh
lunch husky bp4a userdebug
m NativePlanetLauncher -j10
```

Expected output includes:

```text
Install: out/target/product/husky/system_ext/priv-app/NativePlanetLauncher/NativePlanetLauncher.apk
#### build completed successfully
```

## Full Target-Files Build

Only run after cheap gates pass and the queued change justifies a ROM build.

```bash
# From your GrapheneOS checkout
source build/envsetup.sh
lunch husky bp4a userdebug
m vendorbootimage vendorkernelbootimage target-files-package -j10
```

Before verification, record the build start time. Reject target-files artifacts older than that time.

Expected target-files path:

```text
out/target/product/husky/obj/PACKAGING/target_files_intermediates/husky-target_files.zip
```

## Target-Files Verification Gates

Verify the exact target-files zip built in this run.

Required checks:

- Timestamp is newer than build start.
- Bootloop fix files exist:
  - `VENDOR_DLKM/etc/init.insmod.husky.cfg`
  - `VENDOR_DLKM/etc/init.insmod.ripcurrent.cfg`
  - `VENDOR_DLKM/etc/init.insmod.shiba.cfg`
- `init.insmod.husky.cfg` contains `bcmdhd4398.ko`.
- `SYSTEM_EXT/priv-app/NativePlanetController/NativePlanetController.apk` exists.
- Controller APK contains expected classes for the change.
- `SYSTEM_EXT/etc/init/nativeplanet-vere.rc` contains expected service options.
- SELinux CIL contains any newly queued rules.
- No source-only documentation file such as `vere.provenance` is installed.

Stop immediately if any gate fails.

## Release Generation

Only after target-files verification passes.

```bash
# From your GrapheneOS checkout
./script/finalize.sh
./script/generate-release.sh husky <BUILD_NUMBER>
```

Verify release artifacts under:

```text
releases/<BUILD_NUMBER>/release-husky-<BUILD_NUMBER>/
```

Do not commit release artifacts.

## Flash

Only flash after explicit approval.

```bash
cd releases/<BUILD_NUMBER>/release-husky-<BUILD_NUMBER>/husky-factory-<BUILD_NUMBER>
./flash-all.sh
```

## Post-Flash Verification

Core:

```bash
adb wait-for-device
adb shell getprop sys.boot_completed
adb shell getprop sys.init.updatable_crashing
adb shell getprop ro.build.type
adb shell getprop ro.debuggable
```

Controller/provider:

```bash
adb shell pidof io.nativeplanet.controller
adb shell content call --uri content://io.nativeplanet.controller --method getStatus
```

Runtime:

```bash
adb shell getprop init.svc.nativeplanet_vere
adb shell pidof vere
adb shell find /data/nativeplanet/ships -name conn.sock
adb shell content call --uri content://io.nativeplanet.controller --method getRuntime
```

conn.sock health:

```bash
adb forward tcp:12321 localfilesystem:/data/nativeplanet/ships/<ship>/.urb/conn.sock
node tools/conn-client.js --port 12321 peel live
node tools/conn-client.js --port 12321 peel who
node tools/conn-client.js --port 12321 peel v
```

Or let the tool find the current pier from the controller provider and create
the adb forward automatically:

```bash
node tools/conn-client.js --adb peel live
node tools/conn-client.js --adb mobile-apps
```

Hosted app merge:

```bash
./tools/smoke-hosted-mobile-apps.sh
```

Expected result after a ROM with the v1 satellite pill and current controller:

```text
PASS: hosted mobile app provider smoke check
  source: docket+nativeplanet-mobile
  desks: groups, grove, kin, landscape, webterm
```

On a fresh satellite pier, `%docket` can lag behind `%nativeplanet-mobile`.
In that case `source: nativeplanet-mobile` is also valid as long as the same
required desks are present and `mobileMetadataAvailable=true`.

Graceful shutdown:

- Preferred product path: controller-owned Click/conn.sock `|exit`.
- Verified test path: `%fyrd %base %khan-eval %noun %ted-eval` with `%hood %drum-exit`.
- Do not use `setprop nativeplanet.vere.enabled 0` as normal user-facing stop behavior.

## Troubleshooting

Check logs:

```bash
adb shell cat /data/nativeplanet/logs/nativeplanet-vere-launch.log
adb shell cat /data/nativeplanet/logs/vere-early.log
adb logcat -d -b all | grep -Ei 'NativePlanet|ConnSock|RuntimeStatus|vere'
adb shell dmesg | grep -i 'avc.*nativeplanet'
```

Non-blocking noise seen during bring-up:

- `/proc/net/route` reads by Vere
- `/dev/kmsg_debug` writes by Vere

Do not broaden SELinux for non-blocking noise.
