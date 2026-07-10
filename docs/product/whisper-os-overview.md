# Whisper OS Overview

Whisper OS is a mobile-first NativePlanet environment for running a local Urbit moon on Android hardware.

The near-term product is not a generic WebView wrapper. The current spine is:

```text
Whisper Launcher
  -> NativePlanetStatusProvider
  -> NativePlanetController
  -> conn.sock / Click-style runtime control
  -> vere64 running a moon
```

## Product Goal

Give a user a phone that can host and manage a local Urbit identity with a quiet, trustworthy launcher UI and a controller that tells the truth about runtime, network, provisioning, and diagnostics.

## Current MVP Scope

In scope:

- local dev moon boot
- WiFi/mobile resolver state
- controller-owned runtime status
- provider API for launcher
- launcher Runtime Console and onboarding shell
- import/provisioning flow
- graceful start/stop/restart through controller

Out of scope for MVP:

- Lens-based health checks
- direct launcher access to pier internals
- production parent/satellite sync
- Lick-based Android capability IPC
- Groundwire comet support
- global SystemUI replacement

## Current Technical Foundation

Verified:

- GrapheneOS-derived ROM on husky
- `vere64` ARM64 runtime
- init-managed `nativeplanet_vere`
- the provisioned moon boots network-live on device and auto-starts across reboots
- `conn.sock` runtime control plane
- controller status polling via `android.system.Os`
- controller-owned graceful shutdown via conn.sock
- launcher reads provider data
- launcher start/stop wired to provider control calls

## Development Phases

### Phase 0: Runtime Base

Mostly complete. Android can boot and host modern Vere safely.

### Phase 0.5: Runtime Truth And Provisioning Spine

Complete. The controller owns runtime truth, provisioning, lifecycle operations, and structured errors.

### Phase 1: Launcher MVP On Real Truth

Build polished launcher surfaces against provider data:

- Runtime Console
- onboarding/import
- identity settings
- network/runtime/boot-package diagnostics

### Phase 2: Moon Lifecycle

Production-grade moon import, backup reminders, restart persistence, and safe graceful shutdown.

### Phase 3: Mobile Network Behavior

WiFi/mobile handoff, Helium classification, NAT64, resolver resilience, and power behavior.

### Phase 4: Android/Urbit Capabilities

Lick, Hark notifications, Android intents/sensors, and app-level IPC.

## Principles

- Runtime truth first, UI second.
- Click/conn.sock replaces Lens for health and control.
- Lick is future capability IPC, not MVP lifecycle.
- Launcher reads provider data; controller owns privileged behavior.
- Never expose raw key material through provider or logs.
- Use throwaway dev moons for testing until lifecycle is stable.

## Getting Involved

Start with [../ROADMAP.md](../ROADMAP.md) and [../PROJECT_MAP.md](../PROJECT_MAP.md).
