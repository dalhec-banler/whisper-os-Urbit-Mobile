# 2026-06-15 Hosted App Auth Verification

## Summary

Build `2026061505` verifies local hosted Urbit apps on the phone without using Lens.

The launcher opens hosted apps through local Eyre on `127.0.0.1:8080`. The controller fetches the current Eyre web login code over `conn.sock` and the Launcher3 hosted web shell establishes a local session before loading the app URL.

## Device Checks

- OTA from `2026061504` to `2026061505` completed successfully through Android update engine.
- User data was preserved.
- Wi-Fi remained connected after reboot.
- `NativePlanetController` started.
- `Launcher3` started.
- Vere started for the existing moon.
- Runtime provider returned:
  - `state=running`
  - `connSockAvailable=true`
  - `version=4.3-33293b1`
- Hosted app provider returned docket data for:
  - Landscape
  - Terminal / Webterm
  - Tlon / Groups

## Hosted App Checks

- Controller `getWebLoginCode` returned successfully; the code was not logged or committed.
- Authenticated local Eyre requests returned HTTP 200 for:
  - `/apps/landscape/`
  - `/apps/groups/`
  - `/apps/webterm/`
- `WhisperHostedWebActivity` opened Landscape without a login bounce.
- `WhisperHostedAppsActivity` displayed all hosted apps with real ship status.
- Pinning Terminal from `My Urbit Apps` added it to the Launcher3 home screen.
- Opening the pinned Terminal shortcut launched authenticated Webterm.

## Notes

- The old `127.0.0.1:12321` path remains unsuitable for hosted apps on this build.
- The product path is `127.0.0.1:8080` plus controller-provided Eyre auth.
- Known non-blocking AVCs remain for generic launcher property reads and Vere `/dev/kmsg_debug` writes.
