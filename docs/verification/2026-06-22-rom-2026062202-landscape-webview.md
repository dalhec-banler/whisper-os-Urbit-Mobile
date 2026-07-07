# 2026-06-22 ROM 2026062202 Landscape WebView Verification

## Summary

ROM `2026062202` fixes hosted Urbit app launching through the correct local
Eyre origin. The controller now probes hosted app web routes through local Eyre
on port `8080`, not the runtime control port.

## Build

- Device: Pixel 8 Pro / husky
- Variant: `bp4a userdebug`
- Build number: `2026062202`
- Flash mode: no-wipe image update
- Satellite Pill v1 SHA256:
  `0e6e61a0362180c3954ac8dd8607c132dcbf2a92fc63a6113ac0007163fd204e`

## Verified

- Device boots with `sys.boot_completed=1`.
- `sys.init.updatable_crashing` is empty.
- Vere starts automatically after reboot.
- Controller starts automatically after reboot.
- Runtime provider reports the moon as `running`.
- conn.sock polling reports `connSockAvailable=true`.
- Hosted app provider smoke test passes.
- Controller provider smoke test passes.
- Launcher3 smoke test passes.
- No NativePlanet, Vere, Launcher3, or `system_app` SELinux denials were found
  in the verification sweep.
- No NativePlanet or Launcher3 fatal crash logs were found in the verification
  sweep.

## Hosted App Results

The provider publishes these app entries from real ship metadata:

| App | Provider State | Local Route |
| --- | --- | --- |
| Landscape | `local_webview` | `/apps/landscape/` |
| Tlon | `local_webview` | `/apps/groups/` |
| Terminal | `local_webview` | `/apps/webterm/` |
| Grove | inventory-only | `/apps/grove/` route reaches Eyre login |
| Kin | inventory-only | `/apps/kin/` route reaches Eyre login |

Unauthenticated route probes for the hosted web apps return an Eyre login
redirect. This is expected. A browser opened to Landscape lands on the local
Urbit login redirect for `/apps/landscape/`.

## Follow-Up

Grove and Kin are discovered from mobile metadata, but the controller does not
publish them as openable apps yet. The next Urbit-side task is to finish their
mobile/PWA launch surfaces so they can be installed and pinned like Tlon and
Landscape.

