# NativePlanet Runtime Base v1

**Achieved:** 2026-05-18

## Summary

Urbit vere runtime successfully ported to Android ARM64, running as a native init-managed service.

## Root Cause (Original Crash)

The ARM64 vere binary was loading at address `0x1000000` (16MB), which conflicted with Android kernel mappings (vDSO, linker, etc.) that occupy low memory addresses. The binary crashed before `main()` could execute.

## Solution

### 1. Binary Load Address Fix

Set `image_base = 0x40000000` (1GB) in `build.zig` for Android builds:

```zig
if (cfg.android) {
    urbit.image_base = 0x40000000;
}
```

### 2. PIC Compilation for wasm3

Added `-fPIC` to `ext/wasm3/build.zig` common_flags to support the relocated base address.

### 3. State-Aware Launcher

Created `nativeplanet-vere-launch` native wrapper that:
- Detects if pier exists (checks for `.urb` directory)
- Creates new pier with `-c` flag on first boot
- Resumes existing pier without `-c` on subsequent boots
- Runs in foreground (`-t`) for init service management

## Launcher Logic

```c
if (pier_exists()) {
    // Resume existing pier
    execv(vere, {"-t", "--no-dock", pier_path});
} else {
    // Create new fake ~zod pier
    execv(vere, {"-t", "-F", "zod", "--no-dock", "-B", pill, "-c", pier_path});
}
```

**Key insight:** Use `-t` (terminal/foreground) not `-d` (daemon) for init-managed services. Daemon mode forks and exits parent, confusing Android init.

## Binary Verification

```
$ readelf -h zig-out/aarch64-linux-musl/urbit | grep -E 'Type|Entry'
  Type:                              EXEC (Executable file)
  Entry point address:               0x40242000
```

## Test Results

| Test | Result |
|------|--------|
| Fresh pier creates successfully | PASS |
| Existing pier boots without -c | PASS |
| Service starts/stops cleanly | PASS |
| +trouble healthy after restart | PASS |
| No bootloop | PASS |

## Service Control

```bash
# Start urbit
adb shell setprop nativeplanet.vere.enabled 1

# Stop urbit
adb shell setprop nativeplanet.vere.enabled 0

# Check status
adb shell getprop init.svc.nativeplanet_vere
```

## HTTP Access

After boot, urbit serves on:
- `127.0.0.1:12321` (loopback)
- `0.0.0.0:8080` (public)

```bash
adb forward tcp:12321 tcp:12321
curl -X POST http://127.0.0.1:12321 \
  -H "Content-Type: application/json" \
  -d '{"source":{"dojo":"+trouble"},"sink":{"stdout":null}}'
```

## Files Modified

| File | Change |
|------|--------|
| `vere/build.zig` | `urbit.image_base = 0x40000000` for Android |
| `vere/ext/wasm3/build.zig` | `-fPIC` in common_flags |
| `vendor/nativeplanet/src/nativeplanet-vere-launch.c` | State-aware launcher |
| `vendor/nativeplanet/init/nativeplanet-vere.rc` | Init service using launcher |
