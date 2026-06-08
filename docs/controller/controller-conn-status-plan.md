# Controller Runtime-Status Polling via conn.sock

Status: implemented in the ROM overlay source. Early drafts assumed
`android.net.LocalSocket`; device testing showed that path fails before
`connect()`. The current implementation uses `android.system.Os` with
`AF_UNIX` filesystem sockets, explicit read/write loops, and socket timeouts.

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
                        │ (system_app)         │
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

## Implementation

### Current Approach: Java With `android.system.Os`

The controller uses Android's low-level `android.system.Os` APIs directly:

```kotlin
val fd = Os.socket(AF_UNIX, SOCK_STREAM, 0)
val address = UnixSocketAddress.createFileSystem(sockPath)
Os.setsockoptTimeval(fd, SOL_SOCKET, SO_RCVTIMEO, timeout)
Os.setsockoptTimeval(fd, SOL_SOCKET, SO_SNDTIMEO, timeout)
Os.connect(fd, address)
```

`ConnSockClient.java` handles Newt framing, partial reads/writes, normalized
error codes, and cleanup. `NounCodec.java` implements the small jam/cue subset
needed for `%peel` and `%fyrd` requests.

### Rejected Approach: `android.net.LocalSocket`

`LocalSocket` with `Namespace.FILESYSTEM` looked like the simplest option, but
on-device tracing showed it closed the socket before making the native
`connect()` syscall. `nc -U` and direct native sockets worked against the same
path, so the controller moved to `android.system.Os`.

### Future Fallback: Tiny Native Helper

Small C binary that wraps conn protocol:

```
/system/bin/conn-query <sock-path> <command>
# stdout: JSON response
# exit 0 = success, 1 = conn failed, 2 = protocol error
```

Controller spawns process, reads stdout.

**Pros:** Reuses vere's jam/cue, simple IPC
**Cons:** Extra binary, process spawn overhead

### Future Fallback: Controller JNI

Link jam/cue from vere's libnoun into a JNI library.

```kotlin
external fun jamNoun(noun: ByteArray): ByteArray
external fun cueNoun(bytes: ByteArray): ByteArray
```

**Pros:** Fast, native performance
**Cons:** JNI complexity, library management

## Implementation Tasks

### Phase 1: Core Protocol (Controller)

1. [x] Add `android.system.Os` conn.sock client to Controller
2. [x] Port minimal jam/cue to Java (atoms + cells only, no jets)
3. [x] Implement newt framing encode/decode
4. [x] Add %peel %live health check
5. [x] Write runtime-status.json on state change

### Phase 2: Full Status (Controller)

6. [x] Add %peel %who for ship identity
7. [x] Add %peel %v for version
8. [x] Add %peel %info for metrics (lower frequency)
9. [ ] Parse $mass tree for event number, ports
10. [x] Add PID detection via /proc scan

### Phase 3: Provider Integration

11. [x] Update NativePlanetStatusProvider to read runtime-status.json
12. [x] Map JSON fields to existing provider columns
13. [x] Add fields for version, lastSuccessfulPoll, connSockAvailable
14. [ ] Test launcher queries after next ROM flash

### Phase 4: Error Handling

15. [x] Classify conn failures (refused, timeout, protocol)
16. [x] Populate lastError field
17. [x] Add slower polling when stopped
18. [x] Handle pier restart detection

## SELinux Considerations

Current policy includes:
- `nativeplanet_vere` creating `sock_file` in the data dir
- `system_app` atomic-write permissions for runtime status files

The controller runs as `system_app` in this package layout.

## Testing Plan

1. Unit test jam/cue with known noun vectors
2. Unit test newt framing encode/decode
3. Integration test against live conn.sock (adb forward)
4. End-to-end test: Controller → JSON → Provider → Launcher

## Files to Create/Modify

```
rom/vendor/nativeplanet/controller/src/main/java/io/nativeplanet/controller/
├── ConnSockClient.java                 # AF_UNIX socket + newt framing
├── NounCodec.java                      # Minimal jam/cue
├── RuntimeStatusPoller.java            # Polling loop + JSON writer
├── RuntimeControl.java                 # Start/stop lifecycle control
├── NativePlanetControllerService.java  # Poller integration
└── NativePlanetStatusProvider.java     # Read JSON, expose status
```

## Open Questions

1. Should %info parsing extract specific fields or store raw?
2. Polling interval configurable via system property?
3. Should status JSON include boot-package echo for launcher convenience?
4. Need tombstone/crash detection separate from conn polling?
