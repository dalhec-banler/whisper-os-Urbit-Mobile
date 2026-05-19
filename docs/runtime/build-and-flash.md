# Build and Flash Guide

Complete instructions for building vere for Android and integrating with GrapheneOS.

## Prerequisites

- Zig 0.15.2+ (cross-compilation toolchain)
- GrapheneOS source tree
- Android SDK/NDK
- ~100GB disk space for ROM build
- Device: Pixel 8 Pro (husky) or compatible

## Step 1: Build vere for Android

```bash
cd /path/to/vere

# Clean previous builds
rm -rf .zig-cache zig-out

# Build for Android ARM64
/path/to/zig build \
  -Dtarget=aarch64-linux-musl \
  -Dandroid=true \
  -Drelease

# Verify binary
readelf -h zig-out/aarch64-linux-musl/urbit | grep -E 'Type|Entry'
# Should show: Entry point address: 0x402XXXXX (above 1GB)
```

## Step 2: Copy to ROM Tree

```bash
cp zig-out/aarch64-linux-musl/urbit \
   /path/to/grapheneos/vendor/nativeplanet/prebuilts/bin/vere
```

## Step 3: Build ROM

```bash
cd /path/to/grapheneos

# Set up environment
source build/envsetup.sh
lunch husky

# Build target files (incremental, ~3-30 min depending on changes)
m vendorbootimage vendorkernelbootimage target-files-package

# Build OTA tools
m otatools-package

# Finalize
./script/finalize.sh

# Generate release (needs signing keys)
export password=""  # or your key passphrase
./script/generate-release.sh husky $BUILD_NUMBER
```

## Step 4: Flash

```bash
BUILD_NUMBER=2026051404  # adjust as needed

cd releases/$BUILD_NUMBER

# Extract install image
unzip release-husky-$BUILD_NUMBER/husky-install-$BUILD_NUMBER.zip

# Reboot to bootloader
adb reboot bootloader

# Flash
cd husky-install-$BUILD_NUMBER
bash flash-all.sh
```

## Step 5: Verify

```bash
# Wait for boot
adb wait-for-device

# Enable root for testing
adb root

# Clear any stale pier
adb shell rm -rf /data/nativeplanet/pier

# Start service
adb shell setprop nativeplanet.vere.enabled 1

# Wait for boot (~60-90 seconds for fresh pier)
sleep 90

# Check status
adb shell getprop init.svc.nativeplanet_vere
# Should output: running

# Check HTTP
adb forward tcp:12321 tcp:12321
curl -X POST http://127.0.0.1:12321 \
  -H "Content-Type: application/json" \
  -d '{"source":{"dojo":"+trouble"},"sink":{"stdout":null}}'
```

## Incremental Builds

For small changes (launcher, init.rc, sepolicy):

```bash
# Just rebuild affected targets
m nativeplanet-vere-launch systemextimage target-files-package

# Then finalize and generate release as above
```

## Troubleshooting

### Service keeps restarting

```bash
# Check launcher log
adb shell cat /data/nativeplanet/logs/nativeplanet-vere-launch.log

# Check vere early log
adb shell cat /data/nativeplanet/logs/vere-early.log

# Check dmesg
adb shell dmesg | grep nativeplanet
```

### SELinux denials

```bash
# Check for AVC denials
adb shell dmesg | grep "avc.*nativeplanet"

# Common issue: kmsg_debug write denial (harmless, can be ignored)
```

### Binary load failure

If vere crashes before main():
1. Verify `image_base = 0x40000000` in build.zig
2. Check entry point with `readelf -h`
3. Should be above 0x40000000

### Pier locked

```bash
# Remove stale lock
adb shell rm /data/nativeplanet/pier/.vere.lock
```
