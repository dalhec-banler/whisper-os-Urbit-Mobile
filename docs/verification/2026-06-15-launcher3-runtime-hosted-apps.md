# 2026-06-15 Launcher3 Runtime And Hosted Apps Verification

## Summary

The current Pixel 8 Pro userdebug test phone is running the Launcher3/Quickstep
Whisper OS shell, not the earlier custom launcher prototype.

This preserves native Android home, recents, app drawer, drag/drop, widgets, and
gesture behavior while adding first-party NativePlanet runtime surfaces.

## Verified

- HOME resolves to `com.android.launcher3/.uioverrides.QuickstepLauncher`.
- The phone boots after reboot with a Palrum child moon running through
  `nativeplanet_vere`.
- The controller provider reports:
  - `runtime.state=running`
  - `version=4.3-33293b1`
  - `connSockAvailable=true`
  - valid moon boot package
  - WiFi validated through `wlan0`
- `My Urbit Apps` opens as a Launcher3 activity.
- The hosted-apps status strip respects the Android status bar inset.
- The hosted-apps provider returns `{"apps":[]}` when no inventory exists.
- The hosted-apps screen shows an honest empty state instead of fake Tlon/Dojo
  tiles.
- Local HTTP guesses such as `/apps`, `/apps/dojo`, and `/apps/groups` currently
  return `500 hosed`; hosted app inventory must come from ship metadata rather
  than hardcoded URL guesses.

## Source State

- Launcher3/Quickstep patchset:
  `rom/patches/launcher3-whisper-os.patch`
- Hosted app inventory provider:
  `rom/vendor/nativeplanet/controller/src/main/java/io/nativeplanet/controller/NativePlanetStatusProvider.java`

## Next

- Discover installed app metadata from the running ship through Click/conn.sock.
- Normalize Docket/Landscape metadata into `/data/nativeplanet/hosted-apps.json`.
- Use Tlon and Dojo as first real validation apps once their metadata and mobile
  entrypoints are discovered from the ship.
- Keep the launcher empty state honest until that inventory exists.
