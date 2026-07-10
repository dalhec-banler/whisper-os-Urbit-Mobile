# Hosted app surface validation

Verified 2026-07-07. Per-app status for the phone's hosted Urbit app surfaces:
Tlon, Terminal, Landscape, Grove, and Kin.

## Environment

- Device: Pixel 8 Pro (husky), signed ROM `2026062202`, userdebug
- Ship: `~pacbyr-balteb-palrum-roclur` (moon of `~palrum-roclur`), state
  `running`, vere `4.3-33293b1`, conn.sock available
- Inventory source: `docket+nativeplanet-mobile`, `mobileMetadataAvailable=true`,
  `stale=false`
- Driven over adb (`WhisperHostedAppsActivity` + input injection), screenshots
  reviewed at each step

## Results

| App | Desk | Launch mode | Result |
|-----|------|-------------|--------|
| Landscape | `%landscape` | `local_webview` | Pass. Opens authenticated; the Landscape grid renders real tiles with no login page. |
| Terminal | `%webterm` | `local_webview` | Pass. Live dojo prompt renders. |
| Tlon | `%groups` | `local_webview` | Pass. Home surface renders; the empty state is expected (the moon has no groups joined). |
| Grove | `%grove` | (none) | Inventory-only, as intended. Shows "moon metadata, no launch URL yet" with no fake launch. |
| Kin | `%kin` | (none) | Inventory-only, as intended. Same honest state. |

## Behaviors verified

- Hosted WebView sessions authenticate via controller-brokered cookie; no
  login page and no web login code visible anywhere in UI.
- No `127.0.0.1` URLs exposed in any surface; chrome shows title + `%desk`
  with `BROWSER` / `CLOSE` actions only.
- Tlon task survives backgrounding (HOME, then relaunch): WebView resumes
  in place with session intact, no reload to login.
- No `AndroidRuntime`/`FATAL` entries in logcat across all launches.
- `My Urbit Apps` list shows correct affordances per app state:
  `Open`/`Unpin` for launchable apps, `No URL`/`Pin` for inventory-only.

## Known issues

- Grove and Kin rows render a `Browser` action even though they have no launch
  URL and an empty `website` field. The button is a dead control in that state
  and should be hidden or disabled until a URL exists.

## Remaining

- Grove and Kin need real mobile launch metadata (`%nativeplanet-mobile`
  entrypoints) before they can leave inventory-only state.
- Docket tile images are not yet wired into Launcher3 icons; inventory
  currently exposes `tileColor` for all apps but `imageUrl` only for Tlon,
  so icon wiring needs tile-color glyph fallback.
