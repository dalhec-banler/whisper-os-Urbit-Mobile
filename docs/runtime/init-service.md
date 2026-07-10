# Init Service Configuration

Status: historical baseline with current caveats.

The current service in `rom/vendor/nativeplanet/init/nativeplanet-vere.rc` includes later changes such as setgid data directories and `oneshot`. Do not treat `setprop nativeplanet.vere.enabled 0` as a normal user-facing stop path. Product stop/restart should go through controller-owned graceful shutdown via conn.sock / Click-style `%hood` `%drum-exit`, then update the property after the ship exits.

## Service Definition

Location: `vendor/nativeplanet/init/nativeplanet-vere.rc`

```rc
on post-fs-data
    mkdir /data/nativeplanet 0700 shell shell
    mkdir /data/nativeplanet/logs 0700 shell shell
    mkdir /data/nativeplanet/ships 0700 shell shell
    restorecon_recursive /data/nativeplanet

service nativeplanet_vere /system_ext/bin/nativeplanet-vere-launch
    class late_start
    user shell
    group shell inet
    seclabel u:r:nativeplanet_vere:s0
    disabled
    stdio_to_kmsg

on property:nativeplanet.vere.enabled=1
    start nativeplanet_vere

on property:nativeplanet.vere.enabled=0
    stop nativeplanet_vere
```

## Service Properties

| Property | Value | Description |
|----------|-------|-------------|
| `class` | `late_start` | Start after main boot services |
| `user` | `shell` | Run as shell user (not root) |
| `group` | `shell inet` | Shell group + network access |
| `seclabel` | `u:r:nativeplanet_vere:s0` | SELinux domain |
| `disabled` | - | Don't start automatically |
| `stdio_to_kmsg` | - | Log stdout/stderr to kernel log |

## Control via Properties

```bash
# Start service
setprop nativeplanet.vere.enabled 1

# Stop service
setprop nativeplanet.vere.enabled 0

# Check status
getprop init.svc.nativeplanet_vere
# Values: stopped, starting, running, restarting
```

## Data Directories

Created on `post-fs-data`:

| Path | Mode | Owner | Purpose |
|------|------|-------|---------|
| `/data/nativeplanet` | 0700 | shell:shell | Root data directory |
| `/data/nativeplanet/logs` | 0700 | shell:shell | Launcher/vere logs |
| `/data/nativeplanet/ships` | 0700 | shell:shell | Pier container (Satellite v0) |
| `/data/nativeplanet/boot-package.json` | created by controller | shell:shell | Boot configuration |

## BootPackage Requirement (Satellite v0)

The service requires a valid BootPackage at `/data/nativeplanet/boot-package.json`.

Without a BootPackage, the service will fail with:
```
[ERROR] BootPackage not found: /data/nativeplanet/boot-package.json
```

See [BootPackage](../architecture/bootpackage.md) for schema details.

## Launcher Wrapper

The service runs `nativeplanet-vere-launch` instead of `vere` directly because:

1. **BootPackage parsing**: Reads config from JSON file
2. **State detection**: Checks if pier exists before choosing boot mode
3. **Path validation**: Enforces security allowlist
4. **Foreground mode**: Uses `-t` flag for init compatibility
5. **Logging**: Writes startup state and errors to log file

## Debugging

```bash
# View service logs
adb shell dmesg | grep nativeplanet_vere

# View launcher decisions
adb shell cat /data/nativeplanet/logs/nativeplanet-vere-launch.log

# Check if BootPackage is valid
adb shell cat /data/nativeplanet/boot-package.json

# Check pier exists
adb shell ls -la /data/nativeplanet/ships/<ship>/.urb

# Force restart
adb shell setprop nativeplanet.vere.enabled 0
sleep 5
adb shell setprop nativeplanet.vere.enabled 1
```

## Service Lifecycle (Satellite v0)

```
Boot
  │
  ├─► post-fs-data: create directories
  │
  ├─► (property trigger: enabled=1)
  │       │
  │       └─► start nativeplanet_vere
  │               │
  │               └─► nativeplanet-vere-launch
  │                       │
  │                       ├─► read BootPackage
  │                       │       │
  │                       │       ├─► missing? → log error, exit 1
  │                       │       │
  │                       │       └─► invalid? → log reason, exit 1
  │                       │
  │                       ├─► pier exists? → vere -t --no-dock <pierPath>
  │                       │
  │                       └─► no pier? → vere -t -F <ship> -B <pillPath> -c <pierPath>
  │
  └─► (property trigger: enabled=0)
          │
          └─► stop nativeplanet_vere → SIGTERM → SIGKILL
```

## Failure Modes

| Condition | Behavior | Log Message |
|-----------|----------|-------------|
| Missing BootPackage | Exit 1, service stops | "BootPackage not found" |
| Invalid JSON | Exit 1, service stops | "BootPackage invalid: <reason>" |
| Missing pill | Exit 1, service stops | "Pill not found" |
| Path traversal | Exit 1, service stops | "invalid characters" |
| Wrong path prefix | Exit 1, service stops | "must start with..." |
| MOON mode | Exit 1, service stops | "MOON not supported in v0" |

## Auto-Start on Boot

Currently disabled by default. To enable auto-start, add:

```rc
on property:sys.boot_completed=1
    setprop nativeplanet.vere.enabled 1
```

**Note:** The Planet Link launcher and controller now own runtime lifecycle and
prepare the BootPackage, and the controller derives the ship's `@p` on-device
from the runtime-reported ship id over `conn.sock`. Auto-start on boot is
therefore safe: the provisioned moon boots network-live and resumes across
reboots with no host-side tooling.
