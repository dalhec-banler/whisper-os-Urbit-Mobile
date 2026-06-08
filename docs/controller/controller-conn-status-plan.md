# Controller Runtime-Status Polling via conn.sock

## Overview

Replace deprecated Lens-based health checks with conn.sock %peel queries. The Controller polls the running pier and writes status to a JSON file that the StatusProvider exposes to the launcher.

## Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│  Launcher App   │────▶│ NativePlanetStatus   │────▶│ runtime-status  │
│  (read-only)    │     │ Provider (query)     │     │ .json           │
└─────────────────┘     └──────────────────────┘     └────────┬────────┘
                                                              │ writes
                        ┌──────────────────────┐              │
                        │ NativePlanetController│─────────────┘
                        │ (system_server)      │
                        └──────────┬───────────┘
                                   │ Unix socket
                        ┌──────────▼───────────┐
                        │ <pier>/.urb/conn.sock│
                        └──────────────────────┘
```

## Data Flow

1. Controller reads `/data/nativeplanet/boot-package.json` to locate pier
2. Controller connects to `<pier>/.urb/conn.sock`
3. Controller sends %peel requests, decodes responses
4. Controller writes `/data/nativeplanet/runtime-status.json`
5. StatusProvider reads JSON on query, returns to launcher

## Protocol Implementation

### Newt Framing
```
[0x00][4-byte LE length][jammed noun payload]
```

### Request Format
```
[request-id=@ud %peel path]
```

### Commands

| Command | Path | Response | Frequency |
|---------|------|----------|-----------|
| health | `[%live ~]` | `[rid ~ %.y/%.n]` | 5s |
| identity | `[%who ~]` | `[rid ~ @p]` | on-boot |
| version | `[%v ~]` | `[rid ~ @t]` | on-boot |
| metrics | `[%info ~]` | `[rid ~ $mass]` | 60s |

### Response Handling
- `[rid ~ value]` = success (unit with value)
- `[rid ~]` = success (null/none)
- Connection refused = pier not running
- Timeout = pier unresponsive

## Status JSON Schema

```json
{
  "timestamp": 1716825600000,
  "runtime": {
    "state": "running",
    "shipName": "~namfeb-rossyp-palrum-roclur",
    "shipType": "moon",
    "version": "4.3-33293b1",
    "pid": 4364,
    "eventNumber": 4490,
    "uptime": 3600
  },
  "network": {
    "amesPort": 0,
    "httpPort": 8080,
    "connSockAvailable": true
  },
  "lastError": null,
  "lastSuccessfulPoll": 1716825600000
}
```

### State Values
- `running` - conn.sock responds, %live = %.y
- `stopped` - conn.sock doesn't exist or connection refused
- `starting` - conn.sock exists but %live = %.n or timeout
- `error` - conn.sock responds with error or malformed data

## Implementation Options

### Option A: Pure Java (Preferred)

Android API 26+ supports Unix domain sockets via `java.net.UnixDomainSocketAddress` (API 33+) or `android.net.LocalSocket` (older).

```kotlin
// Controller polling loop
class ConnSockClient(private val sockPath: String) {
    private val socket = LocalSocket()

    fun connect() {
        socket.connect(LocalSocketAddress(sockPath, LocalSocketAddress.Namespace.FILESYSTEM))
    }

    fun sendPeel(cmd: String): ByteArray {
        val request = buildPeelRequest(cmd)
        val framed = newtEncode(jam(request))
        socket.outputStream.write(framed)
        return newtDecode(socket.inputStream)
    }
}
```

**Pros:** No JNI, pure Kotlin, easier maintenance
**Cons:** Need to implement jam/cue in Kotlin (or use existing lib)

### Option B: Tiny Native Helper

Small C binary that wraps conn protocol:

```
/system/bin/conn-query <sock-path> <command>
# stdout: JSON response
# exit 0 = success, 1 = conn failed, 2 = protocol error
```

Controller spawns process, reads stdout.

**Pros:** Reuses vere's jam/cue, simple IPC
**Cons:** Extra binary, process spawn overhead

### Option C: Controller JNI

Link jam/cue from vere's libnoun into a JNI library.

```kotlin
external fun jamNoun(noun: ByteArray): ByteArray
external fun cueNoun(bytes: ByteArray): ByteArray
```

**Pros:** Fast, native performance
**Cons:** JNI complexity, library management

## Recommended Approach

**Start with Option A (Pure Java)** because:
1. `android.net.LocalSocket` works with filesystem paths
2. Jam/cue implementation is ~200 lines (port from nockjs)
3. No additional SELinux rules for native binaries
4. Polling is infrequent (5s), performance not critical

If jam/cue proves complex, fall back to **Option B** (native helper).

## Implementation Tasks

### Phase 1: Core Protocol (Controller)

1. [ ] Add `LocalSocket` conn.sock client to Controller
2. [ ] Port minimal jam/cue to Kotlin (atoms + cells only, no jets)
3. [ ] Implement newt framing encode/decode
4. [ ] Add %peel %live health check
5. [ ] Write runtime-status.json on state change

### Phase 2: Full Status (Controller)

6. [ ] Add %peel %who for ship identity
7. [ ] Add %peel %v for version
8. [ ] Add %peel %info for metrics (lower frequency)
9. [ ] Parse $mass tree for event number, ports
10. [ ] Add PID detection via /proc scan

### Phase 3: Provider Integration

11. [ ] Update NativePlanetStatusProvider to read runtime-status.json
12. [ ] Map JSON fields to existing provider columns
13. [ ] Add new columns for extended metrics
14. [ ] Test launcher queries

### Phase 4: Error Handling

15. [ ] Classify conn failures (refused, timeout, protocol)
16. [ ] Populate lastError field
17. [ ] Add retry logic with backoff
18. [ ] Handle pier restart detection

## SELinux Considerations

Current policy already allows:
- `nativeplanet_vere` creating `sock_file` in data dir
- Controller (system_server) needs to connect to that socket

May need:
```
allow system_server nativeplanet_data_file:sock_file { read write connectto };
```

Defer SELinux changes until implementation confirms requirements.

## Testing Plan

1. Unit test jam/cue with known noun vectors
2. Unit test newt framing encode/decode
3. Integration test against live conn.sock (adb forward)
4. End-to-end test: Controller → JSON → Provider → Launcher

## Files to Create/Modify

```
frameworks/base/services/core/java/com/android/server/nativeplanet/
├── ConnSockClient.kt           # Unix socket + newt framing
├── NounCodec.kt                # Minimal jam/cue
├── RuntimeStatusPoller.kt      # Polling loop + JSON writer
└── NativePlanetController.java # Add poller integration

frameworks/base/core/java/android/provider/
└── NativePlanetStatusProvider.java  # Read JSON, expose columns
```

## Open Questions

1. Should %info parsing extract specific fields or store raw?
2. Polling interval configurable via system property?
3. Should status JSON include boot-package echo for launcher convenience?
4. Need tombstone/crash detection separate from conn polling?
