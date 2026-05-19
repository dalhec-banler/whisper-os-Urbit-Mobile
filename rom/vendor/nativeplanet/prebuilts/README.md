# Prebuilts Directory

This directory contains prebuilt binaries for the NativePlanet integration.

## Structure

```
prebuilts/
├── bin/
│   └── vere           # ARM64 vere binary (gitignored)
└── pill/
    └── urbit-v4.3.pill  # Boot pill (gitignored)
```

## Building vere

```bash
cd /path/to/vere
zig build -Dtarget=aarch64-linux-musl -Dandroid=true -Drelease
cp zig-out/aarch64-linux-musl/urbit prebuilts/bin/vere
```

## Obtaining Pills

Pills can be obtained from:
- https://bootstrap.urbit.org/
- Build from urbit source

## Why Gitignored

These files are large binaries (20MB+ for vere, 40MB+ for pills) that:
- Change infrequently
- Can be built from source
- Are better distributed via releases

For CI/CD, download or build these at build time.
