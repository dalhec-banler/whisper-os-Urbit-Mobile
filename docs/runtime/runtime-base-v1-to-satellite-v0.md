# Runtime Base v1 to Satellite v0 Transition

Status: historical transition note from the May bring-up. It is useful context, but it predates the current controller/provider/conn.sock architecture. Use [../ROADMAP.md](../ROADMAP.md) and [build-and-flash.md](build-and-flash.md) for current behavior.

## Overview

This document describes the changes from Runtime Base v1 (hardcoded configuration) to Satellite v0 (BootPackage-driven configuration).

## What Changed

### Pill Path

| Runtime Base v1 | Satellite v0 |
|-----------------|--------------|
| `/system_ext/etc/nativeplanet/urbit-v4.3.pill` | `/system_ext/etc/nativeplanet/satellite.pill` |

### Pier Path

| Runtime Base v1 | Satellite v0 |
|-----------------|--------------|
| `/data/nativeplanet/pier` | `/data/nativeplanet/ships/<ship>` |

### Configuration

| Runtime Base v1 | Satellite v0 |
|-----------------|--------------|
| Hardcoded in C launcher | `/data/nativeplanet/boot-package.json` |
| Always uses "zod" | Ship name from BootPackage |
| Always uses same pill | Pill path from BootPackage |
| Single pier location | Pier path from BootPackage |

### Android.bp Changes

| v1 | v0 |
|----|-----|
| `nativeplanet-pill` module | `nativeplanet-satellite-pill` module |
| `prebuilts/pill/urbit-v4.3.pill` | `prebuilts/pill/satellite.pill` |
| Installs as `urbit-v4.3.pill` | Installs as `satellite.pill` |

### init.rc Changes

| v1 | v0 |
|----|-----|
| `mkdir /data/nativeplanet` | `mkdir /data/nativeplanet` |
| `mkdir /data/nativeplanet/logs` | `mkdir /data/nativeplanet/logs` |
| | `mkdir /data/nativeplanet/ships` (new) |

### Launcher Behavior

**Runtime Base v1:**
```c
if (pier_exists("/data/nativeplanet/pier")) {
    execv(vere, {"-t", "--no-dock", pier_path});
} else {
    execv(vere, {"-t", "-F", "zod", "--no-dock", "-B", pill, "-c", pier_path});
}
```

**Satellite v0:**
```c
BootPackage pkg = read_bootpackage("/data/nativeplanet/boot-package.json");
if (!pkg.valid) {
    log_error("BootPackage invalid");
    exit(1);
}

if (pier_exists(pkg.pierPath)) {
    execv(vere, {"-t", "--no-dock", pkg.pierPath});
} else {
    execv(vere, {"-t", "-F", pkg.ship, "--no-dock", "-B", pkg.pillPath, "-c", pkg.pierPath});
}
```

## Security Additions

Satellite v0 adds:

1. **Path allowlist validation**
   - pillPath must start with `/system_ext/etc/nativeplanet/`
   - pierPath must start with `/data/nativeplanet/ships/`

2. **Path traversal rejection**
   - `..` rejected in all paths

3. **Control character rejection**
   - Characters < 0x20 or 0x7f rejected

4. **Ship name validation**
   - Only lowercase a-z, 0-9, -, ~ allowed

5. **Key material validation**
   - keyMaterialRef must be "none" for FAKE_TEST
   - Hex strings > 50 chars rejected
   - Raw secrets detected and rejected

6. **Boot mode restriction**
   - MOON mode not supported in v0

7. **Logging safety**
   - keyMaterialRef always logged as "[redacted]"

## Backward Compatibility

Satellite v0 is NOT backward compatible with Runtime Base v1:

- v1 required no BootPackage; v0 requires one
- v1 pier at `/data/nativeplanet/pier` won't be found
- v1 pill name `urbit-v4.3.pill` not used

### Migration Path

To migrate an existing v1 installation:

```bash
# Stop service
adb shell setprop nativeplanet.vere.enabled 0

# Move pier to new location
adb shell mkdir -p /data/nativeplanet/ships
adb shell mv /data/nativeplanet/pier /data/nativeplanet/ships/zod

# Create BootPackage
adb push bootpackage.fake.json /data/nativeplanet/boot-package.json

# Fix permissions
adb shell restorecon_recursive /data/nativeplanet

# Start service
adb shell setprop nativeplanet.vere.enabled 1
```

## Verification

After transition, verify:

```bash
# Service running
adb shell getprop init.svc.nativeplanet_vere  # should be "running"

# Pier exists at new location
adb shell ls -la /data/nativeplanet/ships/zod/.urb

# Logs show successful boot
adb shell cat /data/nativeplanet/logs/nativeplanet-vere-launch.log

# No AVC denials
adb shell dmesg | grep -i "avc.*nativeplanet"

# Runtime healthy
adb forward tcp:12321 tcp:12321
curl -X POST http://127.0.0.1:12321 \
  -H "Content-Type: application/json" \
  -d '{"source":{"dojo":"+trouble"},"sink":{"stdout":null}}'
```

## Failure Modes

| Condition | v1 Behavior | v0 Behavior |
|-----------|-------------|-------------|
| Missing pill | Service fails | Service fails with clear log |
| Missing pier | Creates new pier | Creates new pier if BootPackage valid |
| Missing BootPackage | N/A | Service fails: "BootPackage not found" |
| Invalid BootPackage | N/A | Service fails: "BootPackage invalid: <reason>" |
| Path traversal | N/A | Service fails: "invalid characters" |
| Wrong path prefix | N/A | Service fails: "must start with..." |

## Rollback

If Satellite v0 fails, rollback to Runtime Base v1:

1. Revert rom/vendor/nativeplanet/ to v1 state
2. Rebuild ROM
3. Flash ROM
4. Remove BootPackage: `adb shell rm /data/nativeplanet/boot-package.json`
5. Start service: `adb shell setprop nativeplanet.vere.enabled 1`

Runtime Base v1 does not require a BootPackage and will work without one.

## Next Steps

- Gate validation on device
- Integration with NativePlanet Controller
- MOON boot mode implementation (v1)
