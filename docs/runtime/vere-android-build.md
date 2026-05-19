# Vere Android Build Configuration

## Build System

Vere uses Zig as its build system, configured in `build.zig`. Android support requires specific flags and modifications.

## Key Build Flags

```bash
zig build \
  -Dtarget=aarch64-linux-musl \  # ARM64 with musl libc
  -Dandroid=true \                # Enable Android-specific code
  -Drelease                       # Optimized release build
```

## Android-Specific Changes

### 1. Image Base Address

In `build.zig`:

```zig
const urbit = b.addExecutable(.{
    .name = cfg.binary_name,
    .root_module = b.createModule(.{
        .target = target,
        .optimize = optimize,
    })
});

// For Android: use a high base address to avoid conflicts
// Android typically uses low addresses (0-256MB) for vDSO/linker
// We move the binary to 0x40000000 (1GB) which is safely in user space
if (cfg.android) {
    urbit.image_base = 0x40000000;
}
```

### 2. wasm3 PIC Flag

In `ext/wasm3/build.zig`:

```zig
const common_flags = [_][]const u8{
    "-std=c99",
    "-Wall",
    "-Wextra",
    "-Wparentheses",
    "-Wundef",
    "-Wpointer-arith",
    "-Wstrict-aliasing=2",
    "-Werror=implicit-function-declaration",
    "-fno-sanitize=all",
    "-fPIC",  // Required for Android image_base relocation
};
```

### 3. Android Defines

When `-Dandroid=true`, the following are defined:

- `__ANDROID__` - General Android detection
- Platform-specific entropy source selection

## Binary Characteristics

The resulting binary is:

- **Type**: EXEC (non-PIE executable)
- **Entry Point**: ~0x40242000 (above 1GB)
- **Linking**: Statically linked with musl libc
- **Size**: ~21MB unstripped

## Verification

```bash
# Check binary type and entry point
readelf -h zig-out/aarch64-linux-musl/urbit

# Expected output:
#   Type:                              EXEC (Executable file)
#   Entry point address:               0x40242000

# Check it's statically linked
file zig-out/aarch64-linux-musl/urbit
# Expected: statically linked
```

## Cross-Compilation Notes

### Host Requirements

- Linux x86_64 host
- Zig 0.15.2 or later
- No Android NDK required (Zig provides toolchain)

### Target Platform

- Android 14+ (API 34+)
- ARM64 (aarch64)
- GrapheneOS or AOSP-based ROM

## Build Output Location

After build:
- Binary: `zig-out/aarch64-linux-musl/urbit`
- Not stripped (debug symbols preserved)

Copy to ROM tree as:
- `vendor/nativeplanet/prebuilts/bin/vere`

## Troubleshooting

### Linker errors about relocations

```
error: ld.lld: relocation R_AARCH64_ABS64 cannot be used against symbol
```

**Fix**: Add `-fPIC` to the affected library's build flags.

### Binary crashes before main()

**Cause**: Load address conflict with Android mappings.

**Fix**: Ensure `image_base = 0x40000000` is set in build.zig.

### Missing symbols at runtime

**Cause**: Dynamic linking attempted.

**Fix**: Ensure static linking with musl (`-Dtarget=aarch64-linux-musl`).
