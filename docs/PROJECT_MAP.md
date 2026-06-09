# Project Map

This document defines where project state belongs. Use it to prevent NativePlanet Mobile work from drifting across local checkouts.

## Repositories And Workspaces

### `dalhec-banler/whisper-os-Urbit-Mobile`

Project home.

Owns:

- Product roadmap and current project memory
- Controller/provider contracts
- ROM overlay source under `rom/vendor/nativeplanet`
- Launcher source under `launcher`
- Verification reports
- Research notes
- Safe examples and test fixtures

Does not own:

- build outputs
- target files
- factory or OTA zips
- APK outputs
- pills
- keys
- piers
- real boot packages

### `dalhec-banler/vere`

Runtime fork.

Owns:

- Vere source changes that could plausibly live upstream
- Android runtime/build patches
- conn.sock enablement patches
- runtime-level logging or loom fixes

Does not own:

- launcher code
- ROM controller Java code
- product roadmap
- NativePlanet provisioning flow docs
- device verification reports, except when directly tied to a runtime patch

### Local GrapheneOS Checkout

Build workspace. Clone GrapheneOS and check out the appropriate tag.

Owns:

- active Android build tree
- generated `out/`
- generated releases
- temporary source edits before they are copied back into this repo

Does not own:

- long-term project memory
- roadmap
- private source-of-truth docs

### Local Launcher Checkout

Current working app checkout. The source should be periodically copied into this repo's `launcher/` directory, excluding build output and local machine config.

## Current Source Of Truth

Use these in order:

1. [ROADMAP.md](ROADMAP.md)
2. [verification/2026-06-08-flash-verification.md](verification/2026-06-08-flash-verification.md)
3. [research/2026-06-08-graceful-shutdown-research.md](research/2026-06-08-graceful-shutdown-research.md)
4. [controller/controller-api-contract.md](controller/controller-api-contract.md)

Historical May docs live in `docs/archive/2026-05/` and must not be treated as current.

## Current Technical Direction

- Click/conn.sock is the runtime truth path.
- Lens is deprecated and should not be used for health checks.
- Lick is future Android capability IPC, not MVP lifecycle/status.
- Launcher reads provider data. It should not poke pier internals directly.
- Controller owns provisioning, runtime status, and lifecycle operations.
- Graceful shutdown should follow Native Planet GroundSeg's Click `|exit` pattern and allow minutes, not seconds.
- Urbit MCP is development tooling for parent/distro ships, not a phone runtime dependency. See [runtime/urbit-mcp.md](runtime/urbit-mcp.md).

## Commit Hygiene

Before committing:

```bash
git status --short
git diff --stat
git diff --check
```

Reject the commit if it includes:

- `*.key`, `*.pill`, `*.zip`, `*.img`, `*.apk`
- `out/`, `build/`, `.gradle/`, `local.properties`
- `/data/nativeplanet` data
- screenshots containing keys or tickets

## Sync Checklist

When the local GrapheneOS tree changes:

1. Copy source-only `vendor/nativeplanet` changes into `rom/vendor/nativeplanet`.
2. Exclude `prebuilts/bin/*`, `prebuilts/pill/*.pill`, `out/`, and release artifacts.
3. Update roadmap or verification docs if behavior changed.
4. Build `NativePlanetController` module if controller code changed.

When launcher changes:

1. Copy `whisper-launcher/` into `launcher/`.
2. Exclude `.gradle`, `app/build`, root `build`, and `local.properties`.
3. Run the launcher build locally before committing when feasible.
