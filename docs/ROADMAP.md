# NativePlanet Mobile Roadmap

A mobile-first Urbit runtime for Android, built on GrapheneOS.

## Current Status

**Phase 1: Moon Onboarding And Hosted Apps** — in progress

The device boots, runs a real Urbit moon, and reports truthful status. Core infrastructure is verified:

- GrapheneOS ROM boots reliably on Pixel 8 Pro
- Vere runs as an init-managed Android service
- Controller polls conn.sock for live runtime status
- Provider exposes real network/runtime/boot-package state to the launcher
- Graceful shutdown through conn.sock works (Click-style `%hood %drum-exit`)
- Reboot persistence restores the running ship
- Launcher can provision a moon from manually entered moon name, parent, and moon key
- Controller can pair with a parent ship through Artemis and provision a
  `%mobile` moon from the returned boot fields
- Whisper OS uses the platform Launcher3/Quickstep launcher as HOME, with
  NativePlanet runtime surfaces integrated as OS-level launcher features instead
  of a standalone app icon
- A freshly provisioned moon boots to a stable, network-live ship on device and
  auto-starts across reboots, with no host-side tooling. This closed two
  device-only issues: the Vere binary's HTTPS stack (the dawn Azimuth
  galaxy-table fetch failed the TLS handshake until the fork was rebuilt with the
  current toolchain) and a controller poller that scried a not-yet-running
  `%docket` agent, crashing the ship's strand every poll and stalling the initial
  `%base` install
- `My Urbit Apps` discovers installed Urbit apps from the running moon through
  Click/conn.sock and Docket, and opens them in a hosted WebView. **Tlon
  Messenger** opens and renders — its ~7 MB PWA glob downloads over HTTPS and the
  app loads against the moon (full in-app messaging is still being verified).
  **Grove** (a file-storage app) installs ship-to-ship from its publisher and
  runs as a launchable, `site`-served app. Landscape and Terminal launch the same
  way
- The satellite pill carries a full base (`%groups`/Tlon, `%landscape`/docket,
  `%webterm`), so a provisioned moon comes up with Tlon already installed
- The controller supplies the mobile app curation (recommended apps and
  launch-mode hints) on every moon: it prefers a ship-side `%nativeplanet-mobile`
  desk when present and falls back to a bundled default otherwise
- Signed ROM `2026062202` verifies the baked controller, Launcher3, and the
  hosted-app provider path on device without wiping data

Hosted-app launch policy: the controller does not expose a local WebView launch
for an app unless its route probes healthy on local Eyre. Glob apps (Tlon) and
`site`-served apps (Grove) are both launchable; an app that only reports
inventory stays inventory-only rather than opening a broken page.

Next: finish the remaining app surfaces (Kin) and richer per-app mobile launch
metadata. Manual moon-key import stays available as an advanced fallback.

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
- Fresh-phone end-to-end test: a freshly provisioned moon boots to a stable,
  network-live ship and auto-starts across reboots
  (see [docs/verification/fresh-moon-boot.md](verification/fresh-moon-boot.md))

---

## Phase 1: Launcher MVP

**Goal:** First shippable launcher surfaces against real backend data.

**Status:** Moon-key import and Artemis-backed parent provisioning work.
Launcher work is now based on
Launcher3/Quickstep so Android gestures, recents, drag/drop, app drawer, and
home-screen behavior stay production-grade.

- Runtime Console showing real status
- Network panel from provider
- Import moon flow wired to controller
- Launcher3/Quickstep branded as Whisper OS and verified as the active HOME role
- Launcher3/Quickstep changes preserved as `rom/patches/launcher3-whisper-os-v2.patch`
- Start/stop controls through graceful shutdown
- No demo fallback unless controller is genuinely unavailable
- First-run setup path when no ship is configured
- Pairing screen asks for hosting URL and `+code`
- Identity settings can route back into onboarding to add another identity
- NativePlanet standalone launcher icon removed from the user-facing app drawer
- First-party My Urbit Apps surface exists in Launcher3
- Hosted app launch modes exist for native Android apps, PWA/app-shell,
  local Vere WebView, and browser fallback
- Hosted app inventory is exposed through the controller provider instead of
  direct Launcher3 access to `/data/nativeplanet`

**Next:**
- Keep Artemis-backed parent provisioning current and use manual moon-key
  import as the advanced fallback.
- Find the final mobile entrypoints for Tlon, Dojo, Grove, and Kin. Until a
  route is verified, it must appear as inventory only, not as an openable app.
- Use Grove and Kin as candidate paths for installing or syncing Urbit web apps
  after the first mobile app surfaces are stable.
- Add richer Launcher3 actions for hosted apps: pin, unpin, open locally, and
  open in browser.
- Wire Docket tile images into Launcher3 icons when available.

Tlon signup can be linked from onboarding later, but it is not part of the
current MVP.

---

## Phase 2: Daily Launcher

**Goal:** The phone is usable as a daily launcher.

- Whisper-skinned Launcher3 home screen
- App provenance styling for system, store, sandboxed, and Urbit apps
- My Urbit Apps surface with pin-to-home behavior
- Universal search
- Recents, long-press menus, and drag/drop kept aligned with native Android
- Notification shade and quick settings skinning
- Settings surfaces for runtime, identity, and hosted apps

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
