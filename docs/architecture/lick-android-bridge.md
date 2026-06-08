# Lick Android Bridge

Status: future architecture, not current MVP.

Current MVP integration uses `conn.sock` / Click-style `%peel` and `%fyrd` requests for runtime truth and lifecycle operations. The launcher reads Android `ContentProvider` data exposed by `NativePlanetController`; it does not connect directly to Lick or pier internals.

Use this document as Phase 4 planning material for Android capability IPC, such as notifications, sensors, intents, and app-specific native integrations.

## Overview

Lick is an urbit vane that provides a bridge between the urbit runtime and native Android functionality. On Whisper OS, it enables the launcher app to communicate bidirectionally with the urbit.

## Architecture

```
┌─────────────────────────────────────────────┐
│              Launcher App (Kotlin)          │
├─────────────────────────────────────────────┤
│  LickBridge.kt                              │
│    ├── sendToUrbit(mark, data)              │
│    └── onUrbitMessage(callback)             │
├─────────────────────────────────────────────┤
│  Unix Domain Socket / Named Pipe            │
│    /data/nativeplanet/pier/.urb/dev/lick    │
├─────────────────────────────────────────────┤
│  vere (Lick vane)                           │
│    ├── %lick-open                           │
│    ├── %lick-send                           │
│    └── %lick-close                          │
├─────────────────────────────────────────────┤
│  Gall Agent (e.g., %whisper-controller)     │
└─────────────────────────────────────────────┘
```

## Lick Protocol

### Opening a Channel

From Gall agent:
```hoon
[%pass /lick %arvo %l [%spin %my-channel]]
```

### Sending Data

To native:
```hoon
[%pass /lick %arvo %l [%spit %my-channel data=*]]
```

### Receiving Data

Card from Lick:
```hoon
[%lick %soak name=@tas data=*]
```

## Android Implementation

### LickBridge.kt

```kotlin
class LickBridge(private val pierPath: String) {
    private val socketPath = "$pierPath/.urb/dev/lick"
    
    fun connect(channel: String): LickChannel {
        // Connect to unix socket
        // Send channel open request
        // Return channel handle
    }
    
    fun send(channel: LickChannel, mark: String, data: ByteArray) {
        // Serialize and send to urbit
    }
    
    fun setMessageHandler(handler: (String, ByteArray) -> Unit) {
        // Register callback for incoming messages
    }
}
```

### Message Format

```
┌─────────┬─────────┬──────────┬──────────┐
│ Length  │ Channel │   Mark   │   Data   │
│ 4 bytes │ varies  │  varies  │  varies  │
└─────────┴─────────┴──────────┴──────────┘
```

## Use Cases

### 1. Notification Forwarding

Android → Urbit:
```kotlin
lick.send("notifications", "push-token", token.toByteArray())
```

### 2. Background Sync Trigger

Android → Urbit:
```kotlin
lick.send("sync", "trigger", "")
```

### 3. UI State Updates

Urbit → Android:
```hoon
[%pass /lick %arvo %l [%spit %ui [%state-update !>(state)]]]
```

### 4. Hardware Access

Android → Urbit:
```kotlin
// Camera, GPS, sensors via Lick bridge
lick.send("hardware", "location", locationJson.toByteArray())
```

## Security Model

- Lick socket only accessible to shell user
- SELinux policy restricts socket access
- No network exposure of Lick channel
- Data validation on both sides

## Implementation Status

- [x] Lick vane in vere (upstream)
- [ ] Android socket client
- [ ] Kotlin bridge library
- [ ] Example Gall agent
- [ ] Documentation for agent authors

## Testing

```bash
# Check if Lick device exists
adb shell ls -la /data/nativeplanet/pier/.urb/dev/

# Manual socket test
adb shell nc -U /data/nativeplanet/pier/.urb/dev/lick
```
