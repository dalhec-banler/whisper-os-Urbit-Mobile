# Claude Code Instructions

Project-specific rules for AI assistants working on NativePlanet Mobile.

## Architecture

- **Click/conn.sock** is the runtime truth path. Use `%peel` for status, `%fyrd` for operations.
- **Lens is deprecated.** Do not use it for health checks or product integration.
- **Lick** is future Android capability IPC, not required for MVP.
- **Controller owns runtime.** The launcher reads provider data; it does not poke pier internals directly.

## Development Rules

- Use throwaway dev moons for testing. Do not prioritize comets until moon lifecycle is stable.
- Never start a full ROM build for a single non-blocking fix. Batch ROM builds.
- Live-test on userdebug first, patch source second.
- Graceful shutdown must follow the GroundSeg/Click pattern: conn.sock `%hood %drum-exit`, wait for exit, then clear desired-state. Do not use `setprop` hard-stop as normal behavior.

## Security

- No key material in logs, screenshots, process args, crash reports, docs, or assistant output.
- Never commit `*.key`, `*.pill`, `*.jam`, boot packages with real identities, or pier data.
- Treat `secrets/` as local-only. Do not read or modify files there except README/.gitignore.

## Repository Ownership

- **whisper-os-Urbit-Mobile**: Product docs, roadmap, controller contracts, launcher source, ROM overlay source, verification reports.
- **vere fork**: Runtime source changes only. No product docs, no roadmap, no controller specs.
- **Local GrapheneOS checkout**: Build workspace. Not project memory.

## Documentation Style

- Write for humans, not for assistant session recovery.
- Keep timestamped verification/research notes in `docs/verification/` or `docs/research/`.
- Do not embed session dumps or "flashed result" blocks in specs or roadmaps.
- Avoid checkbox trackers in markdown; use GitHub issues for task tracking.

## When Editing Roadmap

- Every roadmap change needs an obstacle or new learning that justifies reconsideration.
- Link to verification docs rather than embedding test results.
- Keep phase descriptions concise.
