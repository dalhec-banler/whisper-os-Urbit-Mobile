> Archived 2026-05. Superseded by [docs/controller/controller-api-contract.md](../../controller/controller-api-contract.md); Lick is covered by [docs/architecture/lick-android-bridge.md](../../architecture/lick-android-bridge.md).

# Launcher-Controller Contract

## Overview

This document defines the interface contract between the Whisper Launcher (Android app) and the Controller (Gall agent running in urbit).

## Communication Channel

- **Transport**: Lick vane over Unix domain socket
- **Location**: `/data/nativeplanet/pier/.urb/dev/whisper`
- **Format**: JSON over length-prefixed frames

## Message Types

### Launcher → Controller

#### `boot-status`
Request current boot state.
```json
{
  "type": "boot-status",
  "id": "uuid"
}
```

#### `service-control`
Start/stop urbit service.
```json
{
  "type": "service-control",
  "action": "start" | "stop" | "restart",
  "id": "uuid"
}
```

#### `sync-request`
Trigger parent sync.
```json
{
  "type": "sync-request",
  "id": "uuid"
}
```

#### `notification-token`
Register push notification token.
```json
{
  "type": "notification-token",
  "token": "firebase-token-string",
  "id": "uuid"
}
```

#### `webview-ready`
Signal WebView is ready for content.
```json
{
  "type": "webview-ready",
  "viewport": {"width": 1080, "height": 1920},
  "id": "uuid"
}
```

### Controller → Launcher

#### `boot-state`
Current boot state response.
```json
{
  "type": "boot-state",
  "state": "booting" | "running" | "stopped" | "error",
  "ship": "~sampel-palnet",
  "uptime": 3600,
  "id": "uuid"
}
```

#### `sync-state`
Parent sync status.
```json
{
  "type": "sync-state",
  "connected": true,
  "lastSync": "2026-05-19T00:00:00Z",
  "pendingOut": 3,
  "pendingIn": 0,
  "id": "uuid"
}
```

#### `notification`
Push notification to display.
```json
{
  "type": "notification",
  "title": "New message",
  "body": "~zod: Hello!",
  "channel": "messages",
  "data": {"thread": "~zod/thread-id"},
  "id": "uuid"
}
```

#### `navigate`
Request WebView navigation.
```json
{
  "type": "navigate",
  "url": "/apps/groups/~zod/channel",
  "id": "uuid"
}
```

#### `error`
Error response.
```json
{
  "type": "error",
  "code": "SYNC_FAILED",
  "message": "Parent unreachable",
  "id": "uuid"
}
```

## State Machine

### Boot States

```
┌─────────┐
│ stopped │◄───────────────────────┐
└────┬────┘                        │
     │ start                       │ stop
     ▼                             │
┌─────────┐                        │
│ booting │────────────────────────┤
└────┬────┘ error                  │
     │ ready                       │
     ▼                             │
┌─────────┐                        │
│ running │────────────────────────┘
└─────────┘
```

### Sync States

```
┌────────────┐
│ disconnected│◄──────────────────┐
└─────┬──────┘                    │
      │ connect                   │ disconnect
      ▼                           │
┌────────────┐                    │
│ connecting │────────────────────┤
└─────┬──────┘ error              │
      │ connected                 │
      ▼                           │
┌────────────┐                    │
│  syncing   │────────────────────┤
└─────┬──────┘ error              │
      │ complete                  │
      ▼                           │
┌────────────┐                    │
│   synced   │────────────────────┘
└────────────┘
```

## Error Codes

| Code | Description |
|------|-------------|
| `BOOT_FAILED` | Urbit failed to boot |
| `PIER_LOCKED` | Pier locked by another process |
| `SYNC_FAILED` | Parent sync failed |
| `NETWORK_ERROR` | Network unavailable |
| `AUTH_FAILED` | Authentication failed |
| `INVALID_MESSAGE` | Malformed message |

## Timeouts

| Operation | Timeout |
|-----------|---------|
| Boot | 120s |
| Sync | 30s |
| Message send | 10s |
| Heartbeat | 30s |

## Heartbeat

Launcher sends heartbeat every 30s:
```json
{"type": "heartbeat", "id": "uuid"}
```

Controller responds:
```json
{"type": "heartbeat-ack", "id": "uuid"}
```

Missing 3 consecutive heartbeats = assume controller dead.

## Implementation Notes

### Launcher Responsibilities
- Manage urbit service lifecycle via setprop
- Maintain Lick connection
- Handle WebView lifecycle
- Display notifications
- Manage network state

### Controller Responsibilities
- Track urbit state
- Manage parent sync
- Queue notifications
- Coordinate with Landscape agents
