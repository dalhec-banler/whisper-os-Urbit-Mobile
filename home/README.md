# Whisper Home

The quiet home for Whisper OS, as a standalone HOME app. It is the phase-1
implementation of `docs/product/proposal-2026-09/nouns-before-apps.html`:

- **Home**: the clock, a *Next* line (first calendar instance within four hours),
  a *Reach* line (the newest recent message from someone on the reach list),
  then a short list of tools in words. Long-press the clock for Settings.
- **Later** (swipe up): the record across every source: DMs and group activity
  from the moon, Android notifications through a notification listener. Tap
  opens, hold marks done, Play-app noise folds into one line.
- **People** and a **Person** page: everyone the moon knows, with reach and mute
  as facts about the person.
- **Type** (swipe down): one line that resolves to people and apps; cleared, it
  is every app on the phone, hosted and Android alike, with `sendable` as the
  only distinction.

It reads only the controller provider (runtime, hosted-app inventory, web login
code) and the moon's local Eyre. Hosted apps open through the ROM's own
`io.nativeplanet.action.OPEN_URBIT_APP` path, so they get the fixed WebView.

## Build and run on the phone

```bash
cd home
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant io.nativeplanet.home android.permission.READ_CALENDAR
adb shell cmd notification allow_listener io.nativeplanet.home/io.nativeplanet.home.data.WhisperNotificationListener
adb shell cmd package set-home-activity io.nativeplanet.home/.MainActivity
```

To go back to the Launcher3 home: `adb shell cmd package set-home-activity com.android.launcher3/.uioverrides.QuickstepLauncher`.

For a ship on the development machine: `./gradlew assembleDebug -PshipUrl=http://10.0.2.2:8080`
(the emulator sees the host as 10.0.2.2). The controller is then absent and the
app shows the server as unavailable; the web login code has to come from the
controller, so ship data only works on the ROM.

## What the moon answers today

The moon's `%activity` answers the v4 paths (`activity/v4/all`,
`activity/v4/activity`); DMs come from `chat/dm` and
`chat/dm/<ship>/writs/newest/N/light`; people from `contacts/all`. Keep these
in `data/Ship.kt` and nowhere else.

## Not yet

Things (Grove and Kin surfaces), send-an-app, the Clay time scrubber, Next from
Kin plans and from times in messages, and the lock screen. The ROM's
`launcher3-whisper-os-v3-hosted-app-tasks.patch` is still required for
deep links into an already-open hosted app.
