# 2026-06-15 Mobile App Surface Progress

## Summary

After verifying authenticated hosted Urbit apps on build `2026061505`, the next
mobile app-surface work began.

## Verified On Device

- `NativePlanetController` provider smoke passes.
- Runtime state is `running`.
- Boot package is valid.
- Network state is WiFi.
- Launcher3 / Quickstep is the default HOME.
- `My Urbit Apps` is visible on the workspace.
- At least one pinned hosted Urbit app is visible on the workspace.
- Hosted app provider returns real Docket inventory for:
  - Landscape
  - Terminal
  - Tlon

## Source Progress

- Added the mobile app surface contract:
  `docs/architecture/mobile-app-surfaces.md`.
- Added source support for installed companion-app detection in the controller.
  The first hook lets `%groups` / Tlon prefer a native Android package when it
  is actually installed; otherwise it keeps the verified local WebView path.
- Added draft `%nativeplanet-mobile` desk source under:
  `satellite-pill/desks/nativeplanet-mobile/`.
- Added `tools/smoke-satellite-pill.sh`.
- Updated `tools/smoke-launcher-ui.sh` for the Launcher3/Quickstep product shell.
- Removed the draft `desk.docket-0` from `%nativeplanet-mobile` for now.
  The disposable ship used for desk smoke testing did not include the
  `%docket-0` mark, so Docket metadata needs a later packaging pass with the
  correct mark layer.

## Validation

- `NativePlanetController` module build passed after the companion-app detection
  change.
- `tools/smoke-controller-provider.sh` passed.
- `tools/smoke-launcher-ui.sh` passed.
- `tools/smoke-satellite-pill.sh` passed.
- `tools/check-repo-hygiene.sh` passed.
- `%nativeplanet-mobile` installed on a disposable local fake ship initialized
  from `%base`.
- Gall reported `%nativeplanet-mobile` as running.
- Authenticated HTTP scry passed:
  `/~/scry/nativeplanet-mobile/apps.json`.
- Click/conn.sock Khan eval passed with:
  `/gx/nativeplanet-mobile/apps/json`.
- The scry returned the expected metadata for Landscape, Terminal, Tlon,
  Grove, and Kin.
- `NativePlanetController` module build passed after adding controller-side
  `%nativeplanet-mobile` metadata merge support.
- Satellite Pill v1 built with:
  `.satellite +pill/brass %base %nativeplanet-mobile`.
- The generated pill booted on a fresh disposable fake ship.
- `%nativeplanet-mobile` installed from the pill as an essential desk.
- `+vats %nativeplanet-mobile` reported `app status: running`.
- Direct Gall scry returned the expected `/apps/json` metadata.
- Android package build for `nativeplanet-satellite-pill` passed.
- A fresh target-files build contains the v1 `satellite.pill` at
  `SYSTEM_EXT/etc/nativeplanet/satellite.pill`.
- The Pixel booted a disposable fake ship from the v1 pill with Android Vere.
- Android boot logs showed `%nativeplanet-mobile` installing during bootstrap,
  `conn.sock` coming up, and Eyre binding on the alternate test port.
- On the connected userdebug device, `/system_ext/etc/nativeplanet/satellite.pill`
  was updated through adb remount to the v1 pill hash.
- After reboot, WiFi reconnected, the real moon returned to `state=running`,
  and launcher/provider smoke checks passed.
- A disposable Android boot using the production pill path
  `/system_ext/etc/nativeplanet/satellite.pill` also installed
  `%nativeplanet-mobile` and brought up `conn.sock`.

## Remaining

- Generate signed flashable release images when signing credentials are
  available, or use the unsigned dev image zip on an unlocked test device.
- Flash a ROM containing Satellite Pill v1 without relying on adb remount.
- Verify fresh moon provisioning through the production BootPackage path.
- Validate Tlon, Terminal, Landscape, Grove, and Kin as mobile app surfaces.
