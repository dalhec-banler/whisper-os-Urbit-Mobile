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

## Validation

- `NativePlanetController` module build passed after the companion-app detection
  change.
- `tools/smoke-controller-provider.sh` passed.
- `tools/smoke-launcher-ui.sh` passed.
- `tools/smoke-satellite-pill.sh` passed.
- `tools/check-repo-hygiene.sh` passed.

## Remaining

- Install `%nativeplanet-mobile` on a dev ship and verify
  `/~/scry/nativeplanet-mobile/apps.json`.
- Add controller merge support for `%nativeplanet-mobile` metadata after the
  desk has been validated on a ship.
- Build a real Satellite Pill v1 containing `%nativeplanet-mobile`.
- Validate Tlon, Terminal, Landscape, Grove, and Kin as mobile app surfaces.
