# NativePlanet Controller API Contract v0.1

Draft contract for Whisper Launcher UI integration.

## API Mechanism Recommendation

### Phase 1 (Now - Testing/Dev)
**File-based status + system properties + shell commands**

- Status files in `/data/nativeplanet/` (already partially exists)
- System properties for runtime control (already exists: `nativeplanet.vere.enabled`)
- Shell commands via `adb shell` for testing
- Readable by apps with appropriate permissions

### Phase 2 (Production)
**Content Provider + Broadcast Intents**

- `content://io.nativeplanet.controller/status` for queries
- `io.nativeplanet.STATUS_CHANGED` broadcast for reactive UI
- Bound service for control operations (start/stop/restart)

### Why not Binder service only?
Content Provider allows passive queries without holding service connection. Broadcasts enable reactive UI without polling.

---

## 1. Runtime Status

### Data Shape
```json
{
  "runtime": {
    "state": "running" | "stopped" | "starting" | "stopping" | "error" | "crashed",
    "shipName": "~sampel-palnet" | null,
    "bootMode": "COMET" | "MOON" | "FAKE_TEST" | null,
    "pid": 5633 | null,
    "uptimeMs": 123456 | null,
    "lastStartTime": "2026-05-22T09:14:32Z" | null,
    "lastStopTime": "2026-05-22T08:00:00Z" | null,
    "lastError": {
      "code": "BOOT_PACKAGE_INVALID",
      "message": "missing 'pierPath'",
      "timestamp": "2026-05-22T08:56:58Z"
    } | null,
    "exitCode": 1 | null
  }
}
```

### File Location (Phase 1)
`/data/nativeplanet/runtime-status.json`

---

## 2. Network Status

### Data Shape
```json
{
  "network": {
    "type": "WIFI" | "CELLULAR" | "ETHERNET" | "VPN" | "NONE",
    "interfaceName": "wlan0" | null,
    "stackedInterfaceName": "v4-wlan0" | null,
    "validated": true | false,
    "dnsServers": ["10.0.0.1", "8.8.8.8"],
    "nat64Prefix": "64:ff9b::/96" | null,
    "timestampMs": 1779458230418,
    "resolverAvailable": true | false,
    "resolverPath": "/data/nativeplanet/resolv.conf"
  }
}
```

### File Location (Phase 1)
`/data/nativeplanet/network-state.json`

---

## 3. BootPackage Status

### Data Shape
```json
{
  "bootPackage": {
    "exists": true | false,
    "valid": true | false,
    "packageVersion": 1,
    "bootMode": "MOON" | "COMET" | "FAKE_TEST",
    "ship": "~lislys-hinnyt-dalhec-banler",
    "parent": "~dalhec-banler" | null,
    "pierPath": "/data/nativeplanet/ships/...",
    "pillPath": "/system_ext/etc/nativeplanet/satellite.pill",
    "keyMaterialRef": "file:***REDACTED***" | "none",
    "keyFileExists": true | false,
    "pierExists": true | false,
    "pillExists": true | false,
    "validationErrors": [
      {
        "field": "keyMaterialRef",
        "error": "KEY_FORMAT_UNSUPPORTED",
        "message": "Key is in old vere format, requires regeneration"
      }
    ]
  }
}
```

### File Location (Phase 1)
`/data/nativeplanet/boot-package-status.json`

---

## 4. Controls

### Operations
| Operation | Phase 1 Mechanism | Phase 2 Mechanism |
|-----------|-------------------|-------------------|
| start | `setprop nativeplanet.vere.enabled 1` | Binder `startRuntime()` |
| stop | Provider `stopRuntime` call; controller sends Click-style graceful `%hood` `%drum-exit` over conn.sock, then clears desired-state property after exit | Binder `stopRuntime()` |
| restart | graceful stop + start | Binder `restartRuntime()` |
| pair with planet | Provider `pairWithPlanet` call with hosting URL and `+code`; returns unavailable until parent service exists | Binder `pairWithPlanet()` |
| provision moon | Provider `provisionMoon` call with sanitized JSON request | Binder `provisionMoon()` |
| refresh | re-read status files | Content Provider query |
| clearTestData | `rm -rf /data/nativeplanet/ships/*` | Binder `clearTestData()` (gated) |

### Control Response Shape
```json
{
  "operation": "start",
  "success": true | false,
  "error": {
    "code": "ALREADY_RUNNING" | "BOOT_PACKAGE_MISSING" | "PERMISSION_DENIED",
    "message": "..."
  } | null,
  "newState": "starting" | "stopped" | ...
}
```

### Pair With Planet Request

This is the preferred user-facing onboarding path. The launcher sends the
parent ship's hosting URL and `+code` to the controller. The controller
authenticates to the parent and uses Artemis as the parent-side moon authority.
Artemis creates or returns a `%mobile` moon, and the controller provisions it
locally using the same storage path as manual moon import.

Provider call:

```text
content://io.nativeplanet.controller call method=pairWithPlanet extras.json=<request>
```

Request JSON:

```json
{
  "hostUrl": "https://parent.example",
  "accessCode": "<+code>"
}
```

Current behavior:

1. Validate the hosting URL and `+code`.
2. Authenticate to the parent ship's Eyre login endpoint.
3. Confirm the authenticated session with a known scry.
4. Check that Artemis is installed at `/apps/artemis/`.
5. Subscribe to Artemis `/moons` over the Urbit channel API.
6. Read the first `/moons` fact to capture the existing moon list.
7. Poke Artemis over the same channel to create a `%mobile` moon.
8. Read the next `/moons` facts until the new mobile moon appears.
9. Provision the returned moon locally and request runtime start.

Successful Eyre login without Artemis:

```json
{
  "accepted": false,
  "code": "PARENT_SERVICE_UNAVAILABLE",
  "message": "Planet login worked. Artemis is not installed on the planet yet."
}
```

Successful Eyre login with Artemis installed, before the required Artemis
channel facts are available:

```json
{
  "accepted": false,
  "code": "PARENT_PROTOCOL_UNSUPPORTED",
  "message": "Artemis is installed, but its mobile provisioning channel is not available yet."
}
```

Other expected failure codes:

| Code | Meaning |
|------|---------|
| `INVALID_HOST_URL` | Hosting URL is missing or is not HTTPS |
| `MISSING_ACCESS_CODE` | `+code` was empty |
| `PARENT_AUTH_FAILED` | Eyre login did not return an authenticated session cookie |
| `PARENT_NETWORK_FAILED` | Parent hosting URL could not be reached |
| `PARENT_SERVICE_UNAVAILABLE` | Login worked, but Artemis is not installed on the parent |
| `PARENT_PROTOCOL_UNSUPPORTED` | Artemis exists, but the required mobile provisioning surface is missing |
| `PARENT_MOON_CREATE_FAILED` | Artemis did not accept the mobile moon request |
| `PARENT_MOON_CREATE_TIMEOUT` | Artemis did not return a new mobile moon in time |

The `+code` must never be returned, logged, screenshotted, or written to
diagnostics.

### Provision Moon Request

Provider call:

```text
content://io.nativeplanet.controller call method=provisionMoon extras.json=<request>
```

Request JSON:

```json
{
  "bootMode": "MOON",
  "ship": "~sample-moon-parent",
  "parent": "~parent-planet",
  "keyMaterial": "0w...3i5",
  "replaceExisting": true
}
```

Response JSON:

```json
{
  "accepted": true,
  "code": "PROVISIONED_START_REQUESTED",
  "message": null,
  "bootPackage": {
    "exists": true,
    "valid": true,
    "packageVersion": 1,
    "bootMode": "MOON",
    "ship": "sample-moon-parent",
    "parent": "~parent-planet",
    "pierPath": "/data/nativeplanet/ships/sample-moon-parent",
    "pierExists": false,
    "pillPath": "/system_ext/etc/nativeplanet/satellite.pill",
    "pillExists": true,
    "keyFileExists": true,
    "validationErrors": []
  }
}
```

Raw key material must never be returned, logged, screenshotted, or written to diagnostics.

---

## 5. Diagnostics

### Data Shape
```json
{
  "diagnostics": {
    "controllerLogs": [
      {"timestamp": "...", "level": "INFO", "message": "..."},
      ...
    ],
    "launcherLogs": [
      {"timestamp": "2026-05-22 09:06:10", "level": "INFO", "message": "BootPackage loaded: ship=zod"},
      ...
    ],
    "recentErrors": [
      {"source": "launcher", "timestamp": "...", "message": "BootPackage invalid: missing 'pierPath'"}
    ],
    "avcDenials": [
      {"timestamp": "...", "context": "nativeplanet_vere", "permission": "write", "target": "kmsg_debug"}
    ],
    "resolverContents": "# Generated by NativePlanet Controller\nnameserver 10.0.0.1\n",
    "networkStateRaw": "{ ... raw JSON ... }"
  }
}
```

### File Locations (Phase 1)
- Controller logs: logcat filter `NativePlanetController`
- Launcher logs: `/data/nativeplanet/logs/nativeplanet-vere-launch.log`
- AVC denials: `dmesg | grep nativeplanet`
- Resolver: `/data/nativeplanet/resolv.conf`
- Network state: `/data/nativeplanet/network-state.json`

---

## 6. Error Model

### Error Codes

| Code | Severity | User Message | Recovery Action |
|------|----------|--------------|-----------------|
| `NO_BOOT_PACKAGE` | blocking | "No ship configured" | Create/select boot package |
| `BOOT_PACKAGE_INVALID` | blocking | "Ship configuration invalid" | Fix or recreate boot package |
| `BOOT_PACKAGE_PARSE_ERROR` | blocking | "Could not read configuration" | Check file format |
| `MISSING_KEY_FILE` | blocking | "Ship key not found" | Import key file |
| `KEY_FORMAT_UNSUPPORTED` | blocking | "Key needs update" | Regenerate from parent planet |
| `MISSING_PILL` | blocking | "Boot image not found" | System image issue |
| `MISSING_PIER` | info | "Ship data not found, will create" | Normal for new ships |
| `RESOLVER_UNAVAILABLE` | warning | "DNS not configured" | Check controller service |
| `NETWORK_UNAVAILABLE` | warning | "No network connection" | Connect to WiFi/cellular |
| `NETWORK_NOT_VALIDATED` | warning | "Network not ready" | Wait or reconnect |
| `RUNTIME_CRASHED` | error | "Ship stopped unexpectedly" | Check logs, restart |
| `RUNTIME_EXIT_ERROR` | error | "Ship failed to start" | Check boot package, logs |
| `PERMISSION_DENIED` | blocking | "Cannot access ship data" | Check file permissions |
| `SELINUX_DENIAL` | warning | "Security policy issue" | Check SELinux config |

### Error Shape
```json
{
  "error": {
    "code": "BOOT_PACKAGE_INVALID",
    "severity": "blocking" | "error" | "warning" | "info",
    "userMessage": "Ship configuration invalid",
    "technicalMessage": "missing 'pierPath' field",
    "source": "launcher" | "controller" | "runtime" | "system",
    "timestamp": "2026-05-22T08:56:58Z",
    "recoveryAction": "FIX_BOOT_PACKAGE",
    "recoveryHint": "Edit boot-package.json and add pierPath field"
  }
}
```

---

## Combined Status Endpoint

### Full Status Shape (for UI refresh)
```json
{
  "version": "0.1",
  "timestampMs": 1779460000000,
  "runtime": { ... },
  "network": { ... },
  "bootPackage": { ... },
  "errors": [ ... ],
  "diagnosticsAvailable": true
}
```

### File Location (Phase 1)
`/data/nativeplanet/status.json` - combined status file, refreshed by controller

---

## Implementation Priority

### Must Have (Phase 1 - File-based)
1. `runtime-status.json` - basic state, ship name, last error
2. `network-state.json` - already exists, minor extensions
3. `boot-package-status.json` - validation status
4. start/stop via system properties

### Should Have (Phase 1.5)
1. `status.json` - combined endpoint
2. Launcher log parsing for errors
3. Error code classification

### Nice to Have (Phase 2)
1. Content Provider
2. Broadcast intents
3. Bound service for controls
4. Structured diagnostics

---

## File Permissions (Phase 1)

| File | Owner | Mode | Readable By |
|------|-------|------|-------------|
| `/data/nativeplanet/status.json` | system:shell | 0640 | shell group apps |
| `/data/nativeplanet/runtime-status.json` | system:shell | 0640 | shell group apps |
| `/data/nativeplanet/network-state.json` | system:shell | 0640 | shell group apps |
| `/data/nativeplanet/boot-package-status.json` | system:shell | 0640 | shell group apps |
| `/data/nativeplanet/resolv.conf` | system:shell | 0640 | shell group apps |
| `/data/nativeplanet/logs/*` | shell:shell | 0600 | shell only |

Note: Whisper Launcher will need to run as shell group or use a privileged helper.

---

## Next Steps

1. Implement `runtime-status.json` writer in controller
2. Implement `boot-package-status.json` generator
3. Extend `network-state.json` with resolver fields
4. Create combined `status.json` endpoint
5. UI can poll these files or use FileObserver for changes
6. After UI design files received, map UI elements to these data shapes
