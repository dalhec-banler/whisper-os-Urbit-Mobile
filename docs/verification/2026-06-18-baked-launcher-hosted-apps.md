# 2026-06-18 Baked Launcher And Hosted Apps Verification

ROM: userdebug release `2026061802`

Device: husky / Pixel 8 Pro

Note: the hosted-app route and Satellite Pill findings in this report were
superseded by the 2026-06-22 Urbit hosted-app verification. The signed ROM
validated here still booted and ran correctly, but the pill artifact later
needed a rebuild and the controller now suppresses local web launch actions
unless the route probes healthy.

## Pass

- Built `vendorbootimage`, `vendorkernelbootimage`, and `target-files-package`.
- Generated signed release artifacts for `2026061802`.
- Flashed the signed image zip with `fastboot update` without wiping data.
- Boot completed with `sys.boot_completed=1`.
- Active slot after flash: `_b`.
- Build remains `userdebug` with `release-keys`.
- Runtime provider reports:
  - `state=running`
  - `shipName=~pacbyr-balteb-palrum-roclur`
  - `version=4.3-33293b1`
  - `lastError=null`
  - `connSockAvailable=true`
- Controller/provider smoke test passes:
  - runtime running
  - boot package valid
  - WiFi network state available
  - empty provisioning request guard present
- Hosted-app provider smoke test passes after `adb root` for direct conn access:
  - source: `docket+nativeplanet-mobile`
  - desks: `groups`, `grove`, `kin`, `landscape`, `webterm`
  - provider app count: 5
  - direct conn app count: 5
- Launcher3 smoke test passes:
  - default HOME is `com.android.launcher3/.uioverrides.QuickstepLauncher`
  - `My Urbit Apps` visible
  - pinned hosted app visible
  - hosted provider includes Landscape, Terminal, and Tlon
- `WhisperHostedWebActivity` launches from explicit app metadata.
- Hosted WebView fallback is Whisper-styled and does not crash when the moon
  does not return a usable app surface.
- No fatal Android runtime crashes found in logcat after flash and hosted app
  launch.

## Baked Fixes Verified

- `NativePlanetController.apk` contains the local Eyre origin
  `http://127.0.0.1:12321`.
- `Launcher3QuickStep.apk` contains the local Eyre origin
  `http://127.0.0.1:12321`.
- No stale `127.0.0.1:8080` path remains in the baked controller or Launcher3
  app-launch code.
- This ROM preserved declared hosted launch paths even when the current moon
  returned `500 hosed`; this behavior was later tightened so broken routes are
  inventory-only.
- Satellite Pill v1 hash in this ROM:
  `29d442529dcb44adc99d7762c1af2620b995ddecc1afe3a06d8f9df8b6ac82dd`.

## Known Non-Blocking Noise

- `nativeplanet_vere` still has `/dev/kmsg_debug` write denials.
- `adbd` and shell may hit `nativeplanet_data_file` search denials during
  developer-only direct filesystem probes. Product access goes through the
  controller/provider path.
- Direct conn smoke tests require `adb root` after a fresh flash because adbd
  restarts non-root.

## Remaining Product Gap

The local Urbit app routes are discovered and launchable, but the current moon
still returns `500 hosed` for tested routes such as `/apps/webterm/`,
`/apps/groups/`, `/apps/grove/`, and `/apps/kin/`. Android now handles this as
an honest hosted-app fallback. The next product task is to make the mobile pill
or parent-managed app setup provide real usable mobile entrypoints.
