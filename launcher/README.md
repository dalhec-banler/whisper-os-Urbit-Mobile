# Whisper Launcher

Native Android launcher application for Whisper OS.

## Overview

The Whisper Launcher is a Kotlin Android application that provides:
- User onboarding and identity setup
- WebView container for Landscape UI
- Native bridge to urbit via Lick
- Service lifecycle management
- Push notification handling

## Architecture

```
┌─────────────────────────────────────┐
│         WhisperApplication          │
├─────────────────────────────────────┤
│  MainActivity                       │
│    ├── OnboardingFragment           │
│    ├── LandscapeWebViewFragment     │
│    └── SettingsFragment             │
├─────────────────────────────────────┤
│  Services                           │
│    ├── UrbitServiceController       │
│    ├── LickBridgeService            │
│    └── NotificationService          │
├─────────────────────────────────────┤
│  Data                               │
│    ├── UrbitStateRepository         │
│    └── PreferencesManager           │
└─────────────────────────────────────┘
```

## Key Components

### UrbitServiceController
Manages urbit service via system properties:
```kotlin
fun startUrbit() = setSystemProperty("nativeplanet.vere.enabled", "1")
fun stopUrbit() = setSystemProperty("nativeplanet.vere.enabled", "0")
```

### LickBridge
Communicates with urbit via Lick vane:
```kotlin
class LickBridge(pierPath: String) {
    fun connect(channel: String): LickChannel
    fun send(channel: LickChannel, mark: String, data: ByteArray)
    fun setMessageHandler(handler: MessageHandler)
}
```

### LandscapeWebView
WebView configured for Landscape UI:
```kotlin
class LandscapeWebView : WebView {
    // JavaScript bridge for native features
    // Custom URL handling for urbit links
    // Offline caching support
}
```

## Development

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8+
- Android SDK 34+

### Building
```bash
./gradlew assembleDebug
```

### Testing on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Design Handoff

See [design-handoff/](design-handoff/) for:
- Figma links
- UI specifications
- Interaction patterns
- Asset exports

## Status

- [ ] Project setup
- [ ] Onboarding flow
- [ ] WebView container
- [ ] Lick bridge
- [ ] Service control
- [ ] Notifications
- [ ] Settings
