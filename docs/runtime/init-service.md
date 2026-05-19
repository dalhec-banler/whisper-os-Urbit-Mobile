# Init Service Configuration

## Service Definition

Location: `vendor/nativeplanet/init/nativeplanet-vere.rc`

```rc
on post-fs-data
    mkdir /data/nativeplanet 0700 shell shell
    mkdir /data/nativeplanet/logs 0700 shell shell
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
| `/data/nativeplanet/pier` | created by vere | shell:shell | Urbit pier |

## Launcher Wrapper

The service runs `nativeplanet-vere-launch` instead of `vere` directly because:

1. **State detection**: Checks if pier exists before choosing boot mode
2. **Foreground mode**: Uses `-t` flag for init compatibility
3. **Logging**: Writes startup state to log file

## Debugging

```bash
# View service logs
adb shell dmesg | grep nativeplanet_vere

# View launcher decisions
adb shell cat /data/nativeplanet/logs/nativeplanet-vere-launch.log

# View vere early boot
adb shell cat /data/nativeplanet/logs/vere-early.log

# Force restart
adb shell setprop nativeplanet.vere.enabled 0
sleep 5
adb shell setprop nativeplanet.vere.enabled 1
```

## Service Lifecycle

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
  │                       ├─► pier exists? → vere -t --no-dock /pier
  │                       │
  │                       └─► no pier? → vere -t -F zod -B pill -c /pier
  │
  └─► (property trigger: enabled=0)
          │
          └─► stop nativeplanet_vere → SIGTERM → SIGKILL
```

## Auto-Start on Boot

Currently disabled by default. To enable auto-start, add:

```rc
on property:sys.boot_completed=1
    setprop nativeplanet.vere.enabled 1
```

**Note:** Not recommended until launcher app is ready to manage lifecycle.
