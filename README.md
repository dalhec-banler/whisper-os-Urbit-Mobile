# Whisper OS / NativePlanet Mobile

Mobile-first Urbit runtime integration for Android, built on a GrapheneOS-derived ROM.

This repository is the project home for NativePlanet Mobile work: roadmap, ROM overlay source, controller/provider contracts, launcher source, verification reports, and research notes. Runtime changes that belong upstream live in the Vere fork; build outputs, keys, pills, piers, and release artifacts do not belong here.

## Current Status

Source of truth: [docs/ROADMAP.md](docs/ROADMAP.md)

As of 2026-06-08:

- Pixel 8 Pro / husky ROM boots as userdebug with SELinux enforcing.
- `vere64` runs as an init-managed Android service.
- Dev moon `~namfeb-rossyp-palrum-roclur` boots through `nativeplanet_vere`.
- `conn.sock` works and replaces Lens for runtime health.
- `NativePlanetController` polls conn.sock and exposes provider status to the launcher.
- Launcher debug app consumes provider data and launches without crashing.
- Reboot auto-start from a healthy pier is verified.
- GroundSeg-compatible graceful shutdown through Click/conn.sock `%hood %drum-exit` is verified on device and queued in controller source.

## Repository Layout

```text
docs/
  ROADMAP.md                 Current source-of-truth roadmap
  controller/                Provider and conn.sock controller contracts
  verification/              Flash/test reports
  research/                  Research notes and upstream findings
  architecture/              Product architecture notes
  archive/                   Historical docs that are no longer current

rom/vendor/nativeplanet/     Source overlay for GrapheneOS vendor/nativeplanet
launcher/                    Whisper Launcher Android project
controller/                  Historical placeholder; controller now lives under rom/vendor/nativeplanet/controller
examples/                    Safe fixtures only
satellite-pill/              Pill build notes/scripts, not pill binaries
tools/                       Helper scripts
secrets/                     Local-only, gitignored
```

## What Belongs Where

- This repo: project docs, ROM overlay source, launcher source, controller API contracts, verification reports.
- `dalhec-banler/vere`: upstream runtime changes only, such as Android build/runtime patches.
- Local GrapheneOS checkout: build workspace and generated artifacts, not project memory.
- Local device/workstation: keys, boot packages, piers, target files, factory images, APK build outputs.

See [docs/PROJECT_MAP.md](docs/PROJECT_MAP.md) for the full ownership map.

## Current Architecture

```text
Launcher
  |
  | ContentProvider calls
  v
NativePlanetController
  |-- Android network observer -> /data/nativeplanet/network-state.json
  |-- runtime poller ----------> /data/nativeplanet/runtime-status.json
  |-- provider ----------------> content://io.nativeplanet.controller/*
  |
  | conn.sock / newt / jam+cued nouns
  v
vere64 / Urbit moon
```

Runtime truth comes from conn.sock:

- `%peel %live`
- `%peel %who`
- `%peel %v`
- `%peel %info`
- `%fyrd %base %khan-eval` for Click-style operations

Lens is deprecated and must not be used for product health checks. Lick remains future Android capability IPC, not the current MVP control plane.

## Build Pointers

Build details move quickly, so prefer the current guide:

- [docs/runtime/build-and-flash.md](docs/runtime/build-and-flash.md)

Useful commands (from your GrapheneOS checkout):

```bash
source build/envsetup.sh
lunch husky bp4a userdebug
m NativePlanetController -j10
m vendorbootimage vendorkernelbootimage target-files-package -j10
```

## Security

Read [SECURITY.md](SECURITY.md) before committing.

Never commit:

- Urbit keys, tickets, boot packages with real identities, or pier data
- ROM signing keys or AVB keys
- pills, target files, OTA/factory images, APKs, or build outputs
- logs or screenshots containing secrets

## License

Original code in this repository is MIT licensed unless otherwise noted. Third-party components retain their original licenses.
