# Verification History

A dated record of on-device verification milestones. Each entry ties a ROM
build or change to the behavior confirmed on the Pixel 8 Pro (husky) userdebug
device. Current, standalone verification references live alongside this file:

- [fresh-moon-boot.md](fresh-moon-boot.md) — a freshly provisioned moon boots to
  a stable, network-live ship and survives reboot.
- [patp-encoding-vectors.md](patp-encoding-vectors.md) — `@p` ship-name encoding
  conformance vectors.
- [hosted-app-surfaces.md](hosted-app-surfaces.md) — per-app hosted-surface
  status.
- [tlon-hosted-app.md](tlon-hosted-app.md) — Tlon Messenger runs as a hosted app
  from a provisioned moon.
- [nativeplanet-mobile-desk.md](nativeplanet-mobile-desk.md) — the mobile metadata
  desk installed on a running moon and merged by the controller.

## Timeline

- **2026-06-08** — First flash-verification pass. Start, reboot auto-start, and
  the conn.sock lifecycle primitives confirmed on device.
- **2026-06-09** — Artemis parent-pairing path flashed (no-wipe) and verified:
  the controller pairs with a parent through Artemis channel facts. The same
  day, the NativePlanet launcher was bundled as a privileged `system_ext` app
  and verified after a no-wipe update.
- **2026-06-10** — Artemis-backed mobile provisioning verified end to end on ROM
  `2026061002`: pair with a parent, have Artemis create a `%mobile` moon, and
  provision it through the controller. The parent exposes moon inventory over an
  `artemis` `/moons` channel with a `mons.json` scry fallback.
- **2026-06-11** — Whisper OS moved to the Launcher3 / Quickstep workspace as
  HOME, preserving native Android behavior and adding first-party Urbit app
  entry points.
- **2026-06-15** — Hosted Urbit apps open through local Eyre (`127.0.0.1:8080`)
  with the web login code fetched over conn.sock, no Lens (ROM `2026061505`).
  Satellite Pill v1 boots a fresh moon pier and installs `%nativeplanet-mobile`.
- **2026-06-22** — ROM `2026062201` bakes the hosted-app launch-guard safety
  fix. ROM `2026062202` corrects hosted-app launching through the local Eyre
  origin and rebuilds the Satellite Pill v1 artifact.
- **2026-07-07** — Hosted-app surface validation across Landscape, Terminal, and
  Tlon. See [hosted-app-surfaces.md](hosted-app-surfaces.md).
- **2026-07-08** — `@p` ship-name encoding verified against reference vectors.
  See [patp-encoding-vectors.md](patp-encoding-vectors.md).
- **2026-07-10** — A freshly provisioned moon boots to a stable, network-live
  ship on device with no host tooling and auto-starts across reboots. See
  [fresh-moon-boot.md](fresh-moon-boot.md).
