# 2026-06-15 Satellite Pill Fresh Moon Test

## Result

Satellite pill v1 successfully boots a fresh moon pier and installs the
`%nativeplanet-mobile` desk.

## What Passed

- The live system pill at `/system_ext/etc/nativeplanet/satellite.pill` matches
  the v1 pill hash:
  `29d442529dcb44adc99d7762c1af2620b995ddecc1afe3a06d8f9df8b6ac82dd`
- A pre-v1 test pier was preserved before the fresh-pier run.
- The controller graceful-stop path stopped the running moon cleanly.
- Init created a new moon pier from the existing boot package and the v1 pill.
- `conn.sock` came up under the fresh pier.
- Controller/provider smoke passed after the fresh boot.
- Direct Click/conn.sock probes passed:
  - `%peel %live`
  - `%peel %v`
  - `%fyrd %base %khan-eval` scry of
    `/gx/nativeplanet-mobile/apps/json`
- The mobile app metadata response included:
  - Tlon / `%groups`
  - Terminal / `%webterm`
  - Landscape
  - Grove
  - Kin

## Current Limitation

The installed controller APK on the test device predates the
`HostedAppsPoller` merge path. The built controller APK contains
`HostedAppsPoller` and the `docket+nativeplanet-mobile` merge logic, but it is
signed with a different platform key from the installed ROM.

Do not live-push the newer controller APK over the installed privileged app.
The provider-side merge should be verified after the next properly signed ROM
flash.

## Tooling Added

`tools/conn-client.js` is a small dependency-free development client for
newt-framed jam/cue requests over `conn.sock`. It can create an adb forward from
the current controller boot package:

```bash
node tools/conn-client.js --adb peel live
node tools/conn-client.js --adb mobile-apps
```

