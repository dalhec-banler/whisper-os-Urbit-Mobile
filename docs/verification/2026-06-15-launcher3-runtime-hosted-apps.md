# 2026-06-15 Launcher3 Runtime And Hosted Apps Verification

## Summary

The current Pixel 8 Pro userdebug test phone is running the Launcher3/Quickstep
Whisper OS shell, not the earlier custom launcher prototype.

This preserves native Android home, recents, app drawer, drag/drop, widgets, and
gesture behavior while adding first-party NativePlanet runtime surfaces.

Current verified build: `2026061504`.

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
- The hosted-apps provider discovers real installed app metadata from Docket
  through Click/conn.sock.
- The current inventory includes Landscape, Terminal, and Tlon.
- `My Urbit Apps` shows pinned state for apps already on the workspace and does
  not offer duplicate pins.
- Pinning Landscape from `My Urbit Apps` returns to the home workspace and
  creates a normal Launcher3 workspace item.
- Reopening `My Urbit Apps` refreshes pinned state on resume, so newly pinned
  apps immediately show as pinned.
- Tapping the pinned Landscape icon opens the Whisper-hosted app shell without
  the Android broken-page placeholder.
- The hosted web shell shows an honest fallback message when the moon does not
  provide a usable mobile app surface.
- No Launcher3 or NativePlanet `AndroidRuntime` crashes were observed during the
  post-flash verification pass.

## Source State

- Launcher3/Quickstep patchset:
  `rom/patches/launcher3-whisper-os.patch`
- Hosted app inventory provider:
  `rom/vendor/nativeplanet/controller/src/main/java/io/nativeplanet/controller/NativePlanetStatusProvider.java`

## Next

- Build the mobile desk/moon pill so hosted apps expose phone-ready entrypoints.
- Wire Docket tile images into Launcher3 icons instead of generated letter
  glyphs where possible.
- Add richer hosted app actions: pin, unpin, open locally, and open in browser.
- Continue Whisper OS skinning for deeper system surfaces: notification shade,
  quick settings, lock screen, and status bar treatment.
