# 2026-06-22 Urbit Hosted Apps And Pill Verification

Device: husky / Pixel 8 Pro

## Problem

The running moon exposed Docket and `%nativeplanet-mobile` inventory for Grove,
Kin, Landscape, Terminal, and Tlon, but the local Eyre app routes returned
`500 hosed`.

The older Satellite Pill v1 artifact also contained stale
`%nativeplanet-mobile` metadata that claimed those desks were launchable through
`/apps/...` paths. That made Android surface broken Open actions.

## Fixes

- Controller hosted-app polling now treats `%nativeplanet-mobile` as
  authoritative for mobile launchability.
- The controller clears Docket-derived launch paths before applying mobile
  metadata.
- Local web launches are published only after the requested local Eyre path
  probes healthy.
- Apps with unavailable local routes remain visible as inventory-only entries.
- `tools/conn-client.js` now supports direct filesystem `conn.sock` connections
  for local pill-build verification.
- `tools/smoke-hosted-mobile-apps.sh` now accepts inventory-only app entries
  and fails if the provider exposes a launch mode without a URL/path or a
  server-erroring local route.
- Satellite Pill v1 was rebuilt from the corrected `%nativeplanet-mobile` desk.

## Artifact

New Satellite Pill v1 SHA256:

```text
0e6e61a0362180c3954ac8dd8607c132dcbf2a92fc63a6113ac0007163fd204e
```

The rebuilt artifact is included in signed ROM `2026062201`, which has been
flashed and verified on the test phone.

## Verification

- `%nativeplanet-mobile` desk typecheck passes under `urbit eval`.
- A disposable fake ship booted from the rebuilt pill.
- The rebuilt pill installed `%nativeplanet-mobile` during bootstrap.
- Direct `conn.sock` query against the fresh fake ship returned five apps:
  `groups`, `grove`, `kin`, `landscape`, and `webterm`.
- All five app entries returned inventory-only mobile metadata:
  `preferredLaunchMode=null`, `mobilePath=null`, `pwaManifestPath=null`, and
  `androidPackage=null`.
- Live phone controller/provider smoke test passed.
- Live phone hosted-app provider smoke test passed.
- Live phone Launcher3 smoke test passed.
- Signed ROM `2026062201` flashed with a no-wipe fastboot update.
- Device booted on build `2026062201` with `sys.boot_completed=1`.
- `/system_ext/etc/nativeplanet/satellite.pill` on device matches the rebuilt
  v1 SHA256 above.
- Runtime provider reports `state=running`, `connSockAvailable=true`, and the
  existing moon boot package remains valid after flash.
- Hosted-app provider reports `source=docket+nativeplanet-mobile`,
  `mobileMetadataAvailable=true`, `lastError=null`, and five inventory-only app
  entries.
- Repository hygiene check passed.

## Current Product State

`My Urbit Apps` can safely show the real app inventory without sending the user
into broken hosed routes. The remaining Urbit-side product work is to provide
real mobile entrypoints for Tlon, Terminal, Grove, Kin, and related apps through
the parent/moon app setup.
