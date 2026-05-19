# Whisper OS

Whisper OS is an open-source GrapheneOS-derived mobile environment for running a local Urbit satellite moon on Android hardware.

The project includes:
- NativePlanet runtime integration for system-level vere
- Satellite Pill architecture
- BootPackage provisioning
- Parent planet ↔ satellite moon protocol
- Whisper Launcher design and implementation
- Android/Urbit bridge work through Lick

Original code in this repository is MIT licensed unless otherwise noted. Third-party components retain their original licenses.

## Project Status

**Runtime Base v1: Complete** (2026-05-18)

- Vere runs natively on Android ARM64
- Full urbit boot from brass pill
- Init-managed service lifecycle
- State-aware pier management

## Architecture

```
┌─────────────────────────────────────────────┐
│              Android Device                  │
├─────────────────────────────────────────────┤
│  Launcher App (Kotlin)                      │
│    └── WebView (Landscape UI)               │
│    └── Lick Bridge (native commands)        │
├─────────────────────────────────────────────┤
│  vere (ARM64 native binary)                 │
│    └── /system_ext/bin/vere                 │
│    └── /data/nativeplanet/pier              │
├─────────────────────────────────────────────┤
│  GrapheneOS (hardened Android base)         │
└─────────────────────────────────────────────┘
```

## Repository Structure

```
docs/
  runtime/          # Vere Android runtime documentation
  architecture/     # System design docs
  product/          # Product specs and onboarding
rom/
  vendor/nativeplanet/  # Android build integration
launcher/           # Android launcher app
tools/              # Build and packaging scripts
secrets/            # Local-only keys (gitignored)
```

## Quick Start

### Build vere for Android

```bash
cd /path/to/vere
zig build -Dtarget=aarch64-linux-musl -Dandroid=true -Drelease
```

### Build ROM with Urbit

```bash
cd /path/to/grapheneos
source build/envsetup.sh
lunch husky
m vendorbootimage vendorkernelbootimage target-files-package
./script/finalize.sh
./script/generate-release.sh husky $BUILD_NUMBER
```

### Flash and Test

```bash
# Flash factory image
cd releases/$BUILD_NUMBER/release-husky-$BUILD_NUMBER
bash flash-all.sh

# Start urbit service
adb shell setprop nativeplanet.vere.enabled 1

# Verify
adb shell getprop init.svc.nativeplanet_vere  # should be "running"
adb forward tcp:12321 tcp:12321
curl -X POST http://127.0.0.1:12321 \
  -H "Content-Type: application/json" \
  -d '{"source":{"dojo":"+trouble"},"sink":{"stdout":null}}'
```

## Documentation

- [Runtime Base v1](docs/runtime/nativeplanet-runtime-base-v1.md) - Core Android runtime
- [Build and Flash](docs/runtime/build-and-flash.md) - Complete build instructions
- [Whisper OS Overview](docs/product/whisper-os-overview.md) - Product vision

## License

MIT License - See [LICENSE](LICENSE) for the full license text.

## Contributing

Contributions are welcome under the MIT License. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Security

See [SECURITY.md](SECURITY.md) for information about what should never be committed to this repository (keys, secrets, piers, etc.).
