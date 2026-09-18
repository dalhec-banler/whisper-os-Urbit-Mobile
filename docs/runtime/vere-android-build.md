# Building vere for the phone

Status: current, 2026-09-18. Supersedes the patched build this file used to
describe.

Upstream vere on the `develop` branch builds for the phone without patches.
The image-base and `-fPIC` changes the earlier Android build carried are not
needed with the 64-bit loom; the official edge binary runs on the ROM as-is
(see `docs/verification/2026-09-15-device-recheck.md`).

## Recipe

```bash
git clone --branch develop https://github.com/urbit/vere.git
colima start                      # any Linux docker daemon
tools/build-vere-android.sh ./vere ../tools
```

The script runs, inside a `linux/arm64` Debian container with zig 0.15.2:

```bash
zig build -Dtarget=aarch64-linux-musl -Drelease -Dpace=edge -Dvere64=true
```

and copies `zig-out/aarch64-linux-musl/urbit` out as
`vere64-develop-<sha>-linux-aarch64` with its SHA-256.

## Why a container

vere pins zig 0.15.2 in CI. On macOS 26 that zig cannot link its own build
runner against any installed SDK; zig 0.16 links but rejects vere's
`build.zig.zon` files. A Linux container with the pinned zig is the build
that matches upstream.

## Result

- `urbit 5.0 edge (64-bit)`, ELF aarch64, statically linked, about 24 MB.
- The launch wrapper already passes `--lmdb-map-size` and `--loom 32`, which
  the 64-bit runtime needs on the phone.

## Putting it on the phone

The ROM prebuilt is `vendor/nativeplanet/prebuilts/bin/vere` in the
GrapheneOS tree (`docs/runtime/build-and-flash.md`). The system partition is
read-only on the device, so a built binary reaches a running phone only
through a ROM build, or through `adb remount` on a userdebug build for a test.
Record the SHA-256 and the vere commit with the ROM.
