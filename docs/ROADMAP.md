# NativePlanet Product Roadmap

Status: source of truth as of 2026-06-08

This roadmap supersedes the older UI-forward launcher roadmap until explicitly revised. The core lesson from bring-up is that the product spine is runtime truth first: controller, conn.sock, provisioning, status provider, and then launcher UI.

## Operating Rules

1. Lens is deprecated and must not be used for health checks or product integration.
2. Click/conn.sock is the runtime truth path.
3. Lick is future Android capability IPC, not required for the current MVP.
4. Use throwaway dev moons for current testing. Do not prioritize comets until moon lifecycle is stable.
5. Never start a full ROM build for a single non-blocking fix.
6. Live-test on userdebug first, patch source second, batch ROM builds only when blocked or release-ready.
7. No key material in logs, screenshots, process args, crash reports, docs, or assistant output.
8. Prefer controller-owned integration. The launcher should read provider data, not poke runtime internals directly.
9. Every roadmap change needs an obstacle or new learning that justifies reconsideration.

## Current Baseline

Proven:

- GrapheneOS-based husky ROM boots reliably.
- Bootloop fix for `init.insmod.*.cfg` is verified.
- `nativeplanet_vere` init service exists and can launch Vere.
- `vere64` is installed and required. Do not regress to old/32-bit runtime.
- Network controller writes `resolv.conf` and `network-state.json`.
- `NativePlanetStatusProvider` exposes real network/provider data.
- A real dev moon, `~namfeb-rossyp-palrum-roclur`, boots on Android.
- `conn.sock` works for `%peel %live`, `%peel %who`, `%peel %v`, and `%peel %info`.
- Launcher can consume provider data and show real network state.

Known queued ROM fixes:

- None for the currently flashed backend happy path. ROM `eng.anoffice.20260528.112643` has the `conn.sock` DAC, SELinux, controller polling, `oneshot`, and stale `.vere.lock` / `.urb/conn.sock` cleanup fixes installed.
- `/proc/net/route` and `/dev/kmsg_debug` denials are non-blocking. Do not patch unless they become real blockers.
- Manual property stop via `setprop nativeplanet.vere.enabled 0` is not a valid user-facing graceful shutdown path. Use it only as a low-level desired-state toggle after the ship has already exited cleanly, or as part of explicit emergency recovery.
- Controller-owned graceful shutdown through conn.sock `%fyrd` / `%hood` `%drum-exit` is source/module verified but not live on the currently flashed ROM until the next flash.

## Phase 0 · Runtime Base

Goal: Android can boot and host modern Vere safely.

Status: mostly complete.

1. Build and install `vere64` for Android ARM64.
2. Keep binary load address compatible with Android mappings.
3. Install `satellite.pill`.
4. Install init-managed `nativeplanet_vere`.
5. Use foreground `-t` mode for init-managed service.
6. Verify bootloop fix and Bluetooth/coex stability.
7. Verify WiFi/DNS resolver path.
8. Verify `conn.sock` can be created by init-managed Vere.

Exit criteria:

- Device boots with no `sys.init.updatable_crashing`.
- `nativeplanet_vere` can start a known-good dev moon.
- HTTP, Ames UDP, and `conn.sock` appear.
- No blocking SELinux denials.

## Phase 0.5 · Runtime Truth And Provisioning Spine

Goal: make the system tell the truth and manage a moon without manual adb-root work.

Status: current priority.

2026-06-08 flashed result:

- ROM `eng.anoffice.20260528.112643` boots as `userdebug` with SELinux enforcing and no `sys.init.updatable_crashing`.
- `vere64` SHA256 on device is `8e95e63cfbaed7141ce87ad3128cbda0b35f7cf869fdad7fc6e011f7be50fd42`.
- NativePlanetController starts and writes real WiFi/DNS state.
- Dev moon `~namfeb-rossyp-palrum-roclur` boots via init after a valid v1 `boot-package.json`.
- `conn.sock` is created by Vere at `.urb/conn.sock` with mode `0666`, and parent pier directories are DAC-traversable.
- Controller Phase 0.5 Java `android.system.Os` transport works in enforcing mode and reports `state=running`, ship `~namfeb-rossyp-palrum-roclur`, version `4.3-33293b1`, and `connSockAvailable=true`.
- Provider `/runtime` returns the same real runtime state. Launcher debug APK installs, launches, and does not crash.
- Reboot auto-start works from a healthy running pier: after reboot, controller/provider return `state=running` and `connSockAvailable=true`.
- Manual `setprop nativeplanet.vere.enabled 0` hard-stop no longer causes an init crash loop because the service is `oneshot`, but it is still unsafe as lifecycle behavior. Treat start-only plus reboot auto-start as verified on the currently flashed ROM. Treat stop/restart UI as pending the next ROM flash with controller-owned graceful shutdown.

1. Implement controller conn.sock client.
2. Implement minimal jam/cue/newt support for `%peel`.
3. Poll `%peel %live` for health.
4. Poll `%peel %who` for identity.
5. Poll `%peel %v` for runtime version.
6. Defer `%peel %info` to lower-frequency metrics after Phase 1 of polling is stable.
7. Write `/data/nativeplanet/runtime-status.json`.
8. Extend provider `/runtime` and `/status` to expose runtime truth.
9. Classify runtime states:
   - `uninitialized`
   - `starting`
   - `running`
   - `stopped`
   - `error`
   - `crashed`
10. Implement process/PID detection as a supporting signal, not the primary truth.
11. Implement boot-package validator:
   - package version
   - boot mode
   - ship
   - parent for moons
   - pier path
   - pill path
   - key file presence
   - modern key format sanity
12. Implement basic provisioning API:
   - receive/import key material
   - write key file with safe permissions
   - write `boot-package.json`
   - create/prepare directories
   - start runtime
   - report structured failure
13. Implement real start/stop/restart controls through the controller. Start is proven; stop/restart source exists and must follow the GroundSeg/Click pattern:
   - set desired runtime state to stopped before initiating shutdown
   - prefer Click/conn.sock `|exit`, implemented as a `%hood` `%drum-exit` poke
   - wait up to 10 minutes for the ship to exit, matching GroundSeg's maintenance wait model
   - use SIGTERM-to-king as a validated fallback path
   - reserve init `stop` / SIGKILL-style behavior for explicit force-stop only
14. Keep status provider read-only to launcher.
15. Maintain a queued-ROM-fixes ledger.

Exit criteria:

- Launcher sees `runtime.state=running` from real provider data.
- Launcher sees the correct ship and version from conn.sock-derived data.
- A dev moon can be imported/provisioned without manual adb file pushing.
- Start/reboot auto-start work through controller-owned APIs.
- Stop/restart use graceful runtime shutdown and do not corrupt or strand the pier.
- Failures have structured, user-actionable codes.

### Graceful Shutdown Findings

2026-06-08 research and device test:

- Upstream Vere installs a SIGTERM handler in the king process and routes it through `u3_king_exit()`.
- Native Planet GroundSeg uses Click for product shutdown: it writes a small `exit.hoon`, executes it with Click, pokes `%hood` with `%drum-exit`, and waits until Docker no longer reports the ship as `Up`.
- GroundSeg's `WaitComplete()` gives ship operations up to 10 minutes, polling every 500ms. Treat graceful stop as a long-running runtime transition, not a quick service toggle.
- Live Android test on the dev moon: root `kill -TERM <king-pid>` made `nativeplanet_vere` exit with status `0`; controller status recovered to `stopped`; toggling desired state back to `1` restarted the moon, recreated `conn.sock`, and provider returned `state=running`.
- Live Android test through conn.sock: `%fyrd %base %khan-eval %noun %ted-eval` successfully ran the same `%hood` `%drum-exit` Hoon used by GroundSeg's `BarExit()`. Vere exited cleanly, provider reported `state=stopped`, and a property off/on restart recovered the moon and `conn.sock`.
- Unprivileged shell cannot signal `nativeplanet_vere`; production controller signaling will need a narrow SELinux/process rule if SIGTERM fallback is implemented in system_app.
- Controller source now includes the first graceful-stop implementation:
  - `NounCodec.buildFyrdKhanEvalRequest()`
  - `ConnSockClient.sendKhanEval()`
  - `ConnSockClient.requestGracefulExit()`
  - `RuntimeControl.startRuntime()`
  - `RuntimeControl.stopRuntimeAsync()`
  - provider `call()` methods: `startRuntime`, `stopRuntime`
- `m NativePlanetController -j10` passed on 2026-06-08; built APK size `53,662` bytes and contains `RuntimeControl`, `%khan-eval`, and `%drum-exit`.

## Phase 1 · Launcher MVP On Real Truth

Goal: build the first shippable launcher surfaces against honest backend data.

Status: scaffolded, not product-complete.

1. Theme/tokens from design files.
2. Runtime Console home.
3. Network panel from real provider.
4. Runtime panel from real `runtime-status.json`.
5. Boot package panel from real validator.
6. Diagnostics panel with structured errors.
7. Welcome screen.
8. Pair with planet screen, stubbed until pairing protocol exists.
9. Import moon screen wired to controller provisioning.
10. Reveal screen with real ship identity.
11. Backup/reminder screen for non-debug flows.
12. Settings -> Identity root.
13. Settings -> add/import identity sheet.
14. Basic start/stop/restart controls.
15. Demo/fallback banner only when controller is genuinely unavailable.

Exit criteria:

- Fresh install can provision a throwaway dev moon through UI/controller.
- User lands on a truthful runtime console.
- No manual adb-root steps required for happy-path dev moon import.
- No secret material is logged or displayed.

## Phase 2 · Daily Launcher Surfaces

Goal: complete the daily-use launcher shell.

1. Whisper home idle/day.
2. Sigil grid.
3. Alphabetical app drawer.
4. Universal search.
5. Launcher-local lock screen.
6. Launcher-local notification shade.
7. Settings root.
8. Quick settings.
9. Install card.
10. Provenance cues for Urbit / Play / Web / Sys / Dev.
11. App rows, cards, and long-press affordances.

Exit criteria:

- The phone is usable as a launcher for daily flows.
- P0 gestures hit 60fps on Pixel 6 floor hardware.
- UI follows calm rules and design tokens.

## Phase 3 · Depth And Failure States

Goal: make the product feel resilient rather than demo-like.

1. Empty states for all primary surfaces.
2. Offline states:
   - immediate offline
   - 4-hour unreachable
   - 24-hour unreachable
   - hard error
3. Runtime failure states:
   - key invalid
   - key old format
   - pill missing
   - boot failed
   - conn unavailable
   - crashed after boot
4. AI thinking / timeout / fallback states.
5. Accessibility mode:
   - type scale
   - contrast boost
   - color-blind provenance fallback
6. Focus mode.
7. Folders.
8. Recents.
9. Long-press menu.
10. Widget picker.
11. In-app chrome.
12. Stream timeline.

Exit criteria:

- Known failures are understandable from UI.
- No common backend failure leaves the user in a fake or blank state.

## Phase 4 · Modern Urbit Integration

Goal: use modern Urbit control surfaces beyond simple health checks.

1. Replace all old Lens assumptions with Click/conn.sock.
2. Add safe `%peek` support where needed.
3. Add `%fyrd` thread execution where needed.
4. Define non-Lens Ames/Urbit health checks.
5. Build a small reusable conn client library.
6. Decide what belongs in controller versus launcher.
7. Design Lick bridge for future Android capabilities:
   - notifications
   - intents
   - sensors
   - files
   - contacts, only after permissions review
8. Keep Lick out of MVP unless it directly unblocks a P0 feature.

Exit criteria:

- Runtime health and identity are Click-based.
- Any Urbit command surface used by the product has a documented request/response contract.

## Phase 5 · Sound, Haptics, And Polish

Goal: finish the feel of the product without adding noise.

1. Single soft tick when AI finishes thinking.
2. Low haptic for important response tier.
3. No notification chimes by default.
4. Lock/unlock haptic pulse.
5. Animation polish.
6. Copy review.
7. Visual QA against reference screens.

Exit criteria:

- Product feels calm and intentional.
- No red colors, emojis, Material chrome, or noisy system behaviors.

## Phase 6 · Hardening And Beta

Goal: prepare for real users.

1. System-app permissions audit.
2. SELinux audit.
3. Battery profiling, target under 2% drain/day for launcher/controller overhead.
4. Memory profiling, target under 80MB resident for launcher where feasible.
5. Runtime battery review for Vere under realistic sync conditions.
6. Rotation and configuration edge cases.
7. Work profile behavior.
8. Play app compatibility.
9. Beta with 20 NativePlanet users.
10. Onboarding completion metric above 85%.
11. Final ROM/launcher release review.

Exit criteria:

- Beta users can onboard and use the phone without developer intervention.
- Release artifacts are reproducible and verified.

## Deferred

These are real but not MVP blockers:

1. Groundwire comet support.
2. Multi-ship switching.
3. True global SystemUI replacement.
4. Tablet and foldable layouts.
5. Wear OS.
6. iOS.
