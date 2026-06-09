# 2026-06-09 Bundled Launcher Flash Verification

Device: husky / Pixel 8 Pro

Build type: local `bp4a userdebug`, signed with release keys

## Summary

The ROM was rebuilt, signed, and flashed with the NativePlanet launcher bundled as a privileged `system_ext` app. The no-wipe fastboot update preserved userdata, the existing moon, and WiFi state.

This verifies the launcher is no longer only a debug APK installed after flashing.

## Build Verification

- Target files included `NativePlanetController`.
- Target files included `NativePlanetLauncher`.
- Both APKs were listed as platform-signed in `META/apkcerts.txt`.
- Signed release artifacts were generated successfully.

## Flash Result

- No-wipe fastboot update completed successfully.
- Android boot completed.
- Build remained `userdebug`.
- `sys.init.updatable_crashing` was empty.
- Userdata was preserved.

## Runtime Verification

- Existing moon boot package survived the update.
- Vere restarted through init.
- King and serf processes were running.
- `conn.sock` existed under the pier.
- Controller connected to `conn.sock` through `android.system.Os`.
- Provider `/runtime` returned:
  - `state=running`
  - current moon ship name
  - `version=4.3-33293b1`
  - `connSockAvailable=true`

## Network Verification

- WiFi was connected and validated.
- Resolver state was available through the provider.
- DNS pointed at the local gateway resolver.

## Launcher Verification

- `io.nativeplanet.launcher` was installed from `system_ext/priv-app`.
- Launcher opened from the ROM-bundled package without a crash.
- Home screen displayed:
  - current moon
  - parent ship
  - `RUNNING`
  - valid boot package
  - pier, pill, and key checks
  - WiFi and resolver status
- `Add identity` opened onboarding.
- Onboarding showed:
  - `Pair with planet`
  - `Use moon key`
  - comet fallback
- Pairing screen asked for the parent hosting URL and `+code`, with manual moon-key import available as a fallback.

## Pairing Smoke Test

- Provider method `pairWithPlanet` was callable from the ROM-bundled controller.
- A safe dummy `+code` against the parent hosting URL returned `PARENT_AUTH_FAILED`.
- No real access code was used and no new moon was created.

## Known Caveats

- Real Artemis pairing still needs an end-to-end UI run with a live parent URL and access code.
- After reboot, provider runtime status can briefly report `stopped` before the next conn.sock poll observes the running ship. A source fix is queued to keep the poller on the fast interval whenever a boot package exists.
- Known non-blocking `nativeplanet_vere` denial for `/dev/kmsg_debug` still appears.
