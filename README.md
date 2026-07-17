# Whisper OS / NativePlanet Mobile

Mobile-first Urbit runtime integration for Android, built on a GrapheneOS-derived ROM.

This repository is the project home for NativePlanet Mobile work: roadmap, ROM overlay source, controller/provider contracts, launcher source, verification reports, and research notes. Runtime changes that belong upstream live in the Vere fork; build outputs, keys, pills, piers, and release artifacts do not belong here.

## Current Status

Source of truth: [docs/ROADMAP.md](docs/ROADMAP.md)

Current baseline:

- Pixel 8 Pro / husky ROM boots as userdebug with SELinux enforcing.
- `vere` runs as an init-managed Android service (`nativeplanet_vere`).
- A moon provisioned from a parent planet boots and reaches its sponsor over
  Ames. A freshly provisioned moon now boots end-to-end on device — including
  the Azimuth galaxy-table fetch over HTTPS — with no host-side tooling.
- `conn.sock` is the runtime truth path and replaces Lens for runtime health.
- `NativePlanetController` polls conn.sock, derives the ship's `@p` from the
  runtime-reported ship id, and exposes provider status to the launcher.
- Whisper OS uses Launcher3/Quickstep as HOME, with native Android gestures,
  recents, drag/drop, app drawer, widgets, and workspace behavior preserved.
- Launcher3 includes a first-party My Urbit Apps surface for Urbit-hosted apps.
  The onboarding app ships as "Planet Link".
- Hosted apps run full-screen in a hosted WebView with no launcher chrome, so an
  Urbit app behaves like any other app on the device. Tlon Messenger (a glob PWA)
  works end to end, including creating and sending a new direct message; Grove (a
  `site`-served file app installed ship-to-ship) launches and serves its UI. Both
  run against the moon, alongside Landscape and Terminal.
- Reboot auto-start from a healthy pier is verified, including for a
  freshly provisioned moon and its installed hosted apps.
- GroundSeg-compatible graceful shutdown through Click/conn.sock
  `%hood %drum-exit` is verified on device and implemented in the controller.

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
rom/patches/                 Patch sets for GrapheneOS projects outside vendor/nativeplanet
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
