# 2026-06-11 Launcher3 Hosted Apps Verification

## Summary

Whisper OS now uses the Launcher3 / Quickstep workspace instead of the earlier custom launcher surface. This preserves native Android home, recents, app drawer, drag/drop, and gesture behavior while adding first-party Urbit app entry points.

## Verified

- The default HOME activity resolves to `com.android.launcher3/.uioverrides.QuickstepLauncher`.
- `Whisper OS` branding is installed on the Launcher3 package.
- `My Urbit Apps` is exposed as a first-party Launcher3 activity.
- Hosted app inventory loads from the controller provider.
- Pinned hosted apps survive Launcher cold restart with their own title and icon.
- Tapping a pinned hosted app opens the Whisper-hosted web shell.
- The hosted web shell no longer shows the default Android broken-page placeholder.
- `My Urbit Apps` shows pinned state for apps already on the workspace and avoids duplicate pins.
- Android recents / Quickstep opens without Launcher crashes.
- Runtime provider reports the moon running with conn.sock available.

## Current Test Inventory

- `Tlon` from `%groups`
- `Dojo` from `%dojo`
- `App Store` from `%landscape`

`Tlon` and `Dojo` are pinned on the test workspace. `App Store` remains available in the inventory.

## Remaining Polish

- The hosted web shell close button is functional but visually too blunt.
- Hosted app tile icons are generated letter tiles until Docket/Landscape tile images are wired through.
- The current live workspace has been manually repaired during testing; fresh-flash layout source now places `My Urbit Apps` on the workspace, but a full fresh flash/reset should confirm it.
- Urbit app entrypoints still depend on the moon exposing mobile-ready web paths.
- System surfaces still need deeper Whisper OS skinning: notification shade, quick settings, lock screen, and status bar treatment.

