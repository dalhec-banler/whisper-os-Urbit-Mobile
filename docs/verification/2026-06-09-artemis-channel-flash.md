# 2026-06-09 Artemis Channel Flash Verification

Device: husky / Pixel 8 Pro

Build type: local `bp4a userdebug`, signed with release keys

## Summary

The ROM-side Artemis pairing update was built, signed, flashed with a no-wipe fastboot update, and verified on device.

This build includes the controller/backend pairing path that talks to Artemis through Urbit channel facts:

- subscribes to `/moons`
- sends the `make-moon` action
- waits for a new `%mobile` moon fact
- provisions the resulting moon through the controller

The Whisper launcher onboarding UI was not bundled into this ROM. It was built and installed separately as a debug APK for UI/provider verification.

## Flash Result

- Fastboot update completed successfully.
- Userdata was preserved.
- WiFi state survived the update.
- Android boot completed.
- `sys.init.updatable_crashing` was empty.
- Build remained `userdebug` with release-key signing.

## Controller Verification

- `NativePlanetController` installed under `system_ext`.
- Controller APK was signed with the expected release platform certificate.
- On-device APK contains the Artemis channel pairing strings:
  - `/moons`
  - `make-moon`
  - `artemis-action`
  - `subscribeArtemisMoons`
- Controller process started after boot.
- Controller wrote network and resolver state.

## Runtime Verification

- Existing moon boot package survived the no-wipe flash.
- Vere restarted through init.
- King and serf processes were running.
- `conn.sock` existed under the pier.
- Runtime status provider returned:
  - `state=running`
  - current moon ship name
  - `version=4.3-33293b1`
  - `connSockAvailable=true`
- Boot package provider returned:
  - `valid=true`
  - `bootMode=MOON`
  - `pierExists=true`
  - `pillExists=true`
  - `keyFileExists=true`

## Network Verification

- Network type: `WIFI`
- Interface: `wlan0`
- Validated: `true`
- DNS: local gateway resolver
- Resolver file available through provider.

## Launcher Verification

- Current launcher debug APK built successfully.
- Launcher installed over the existing app.
- Launcher launched without fatal crash.
- Launcher displayed real provider data:
  - running moon
  - parent ship
  - valid boot package
  - pier/pill/key checks
  - WiFi/resolver state

## Pairing Smoke Test

Provider method `pairWithPlanet` was present and returned structured failures for safe dummy requests:

- non-HTTPS URL: `INVALID_HOST_URL`
- unreachable HTTPS URL: `PARENT_NETWORK_FAILED`

No real parent access code was used in this smoke test, and no new moon was created.

## Known Caveats

- The launcher UI is still a separately installed debug APK.
- A real Artemis pairing run still needs to be tested from the launcher UI with a live parent URL and access code.
- Known non-blocking `nativeplanet_vere` denial for `/dev/kmsg_debug` still appears.
- Shell-only inspection can trigger expected denials when reading protected NativePlanet data paths.
