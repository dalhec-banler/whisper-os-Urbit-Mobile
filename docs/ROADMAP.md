# NativePlanet Mobile Roadmap

A mobile-first Urbit runtime for Android, built on GrapheneOS.

## Current Status

**Phase 1: Moon Onboarding** — in progress

The device boots, runs a real Urbit moon, and reports truthful status. Core infrastructure is verified:

- GrapheneOS ROM boots reliably on Pixel 8 Pro
- Vere runs as an init-managed Android service
- Controller polls conn.sock for live runtime status
- Provider exposes real network/runtime/boot-package state to the launcher
- Graceful shutdown through conn.sock works (Click-style `%hood %drum-exit`)
- Reboot persistence restores the running ship
- Launcher can provision a moon from manually entered moon name, parent, and moon key

Next step: Artemis-backed moon provisioning. The phone should ask for the
ship's hosting URL and `+code`, then ask Artemis on the parent ship to create or
hand out a `%mobile` moon. Manual moon-key import stays available as an advanced
fallback.

For detailed verification reports, see [docs/verification/](verification/).

---

## Phase 0: Runtime Base

**Goal:** Android can boot and host modern Vere safely.

**Status:** Complete.

- ARM64 `vere64` binary with compatible load address
- Init-managed `nativeplanet_vere` service
- Satellite pill installed
- conn.sock enabled for runtime health checks
- SELinux policy for runtime operation

---

## Phase 0.5: Runtime Truth

**Goal:** The system tells the truth and manages a moon without manual adb work.

**Status:** Complete for the current MVP baseline.

**Done:**
- Controller conn.sock client with jam/cue/newt support
- Runtime status polling (`%peel %live`, `%who`, `%v`)
- Provider exposes runtime state to launcher
- Graceful stop/start through controller
- Controller provisioning API (key import, boot-package write, start runtime)
- Launcher import flow wired to provisioning
- Fresh-phone end-to-end test

For the active checklist, see [docs/product/provisioning-mvp-checklist.md](product/provisioning-mvp-checklist.md).

---

## Phase 1: Launcher MVP

**Goal:** First shippable launcher surfaces against real backend data.

**Status:** Moon-key import works; Artemis-backed pairing is next.

- Runtime Console showing real status
- Network panel from provider
- Import moon flow wired to controller
- Start/stop controls through graceful shutdown
- No demo fallback unless controller is genuinely unavailable
- First-run setup path when no ship is configured

**Next:**
- Pairing screen asks for hosting URL and `+code`
- Artemis creates or returns a `%mobile` moon
- Phone provisions the returned boot package and starts Vere
- Manual moon-key import remains available as an advanced path

Tlon signup can be linked from onboarding later, but it is not part of the
current MVP.

---

## Phase 2: Daily Launcher

**Goal:** The phone is usable as a daily launcher.

- Whisper home screen
- Sigil grid and app drawer
- Universal search
- Lock screen and notification shade
- Settings

---

## Phase 3: Failure States

**Goal:** The product feels resilient, not demo-like.

- Empty/offline/error states for all surfaces
- Runtime failure states (key invalid, boot failed, conn unavailable)
- Accessibility mode

---

## Phase 4: Modern Urbit Integration

**Goal:** Use modern Urbit control surfaces beyond health checks.

- Replace all Lens assumptions with Click/conn.sock
- Add `%peek` and `%fyrd` support where needed
- Design Lick bridge for future Android capabilities (deferred from MVP)

---

## Phase 5: Polish

**Goal:** Finish the feel without adding noise.

- Sound and haptics
- Animation polish
- Copy review

---

## Phase 6: Hardening and Beta

**Goal:** Prepare for real users.

- Permissions and SELinux audit
- Battery and memory profiling
- Beta with 20 NativePlanet users
- Onboarding completion rate above 85%

---

## Deferred

Not MVP blockers:

- Comet support
- Multi-ship switching
- SystemUI replacement
- Tablet/foldable layouts
- iOS
