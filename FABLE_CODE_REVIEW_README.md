# Fable Code Review Handoff: Whisper OS / NativePlanet Mobile

This file is a condensed, source-of-truth handoff for an outside senior model or engineer doing a hard review of the current NativePlanet Mobile / Whisper OS project.

It is intentionally not a marketing README. It is a review packet: what we are building, where the code lives, what has been proven, what is still risky, and what we want challenged.

No production secrets, moon keys, access codes, signing passwords, or local home-directory paths are included here. If you see a real secret in any repo or doc, treat that as a blocking finding.

## Copy-Paste Prompt For Fable

You are reviewing the Whisper OS / NativePlanet Mobile project. Read this entire handoff first, then inspect the referenced repos and code. Your job is to challenge the plan and code as a senior reviewer: find blockers, security risks, architectural mistakes, stale assumptions, missing tests, and better next steps. Do not use Lens as an integration path. Do not request or expose keys, access codes, signing material, or local machine paths. Return findings with severity, concrete file references, suggested fixes, and a recommended next plan.

## What We Want From Fable

Please perform a full code and architecture review. Be blunt. We are specifically testing whether you can find missing risks, bad assumptions, or better next steps.

Return findings in this shape:

1. Critical blockers: things that could brick, lose data, leak secrets, break boot/update, or make the phone unsafe.
2. High-priority product blockers: things that stop this from being a pleasant second phone.
3. Architecture risks: places where we are going down a path that will hurt us later.
4. Code quality findings: file-level issues with concrete references.
5. Security/privacy findings: especially key handling, WebView, Android permissions, SELinux, logging, and repo hygiene.
6. Suggested next plan: what to do next, in what order, and what not to do yet.

Do not just summarize the project back. Challenge it.

## Product Goal

Build a Pixel 8 Pro-based Urbit phone:

- Android-native launcher behavior, gestures, recents, widgets, app drawer, drag/drop, workspace pages, and polish.
- Urbit-first identity and runtime, but not Android-hostile. The Play Store remains a normal path for Android apps.
- A local phone moon runs under Android init through modern `vere64`.
- The phone exposes real runtime truth through the controller and status provider.
- Urbit apps appear as first-class phone launch targets in a "My Urbit Apps" surface.
- The parent planet should eventually be able to provision/manage the phone moon, recommend or install apps, and coordinate preferences/application state.

The user does not want a toy launcher. They want something that feels like a high-end Android phone with an Urbit-native spine.

## Non-Negotiables

- Lens is deprecated. Do not build new product flows on Lens.
- Click / `conn.sock` is the runtime truth path.
- Lick is future Android capability IPC, not an MVP blocker.
- `vere64` is required. Do not regress to older `vere32` / old runtime assumptions.
- Moon-first. Comets are not the main target right now.
- Do not leak keys, access codes, signing materials, or local machine paths into GitHub.
- Do not solve launcher problems by losing Android-native launcher behavior. Launcher3/Quickstep is now the preferred base.
- Do not start a full ROM build for one speculative non-blocking change.
- Factory/install flashes wipe data. Treat flashing as a deliberate operation, not a background convenience.

## Repository Map

Use these aliases instead of local absolute paths:

- `<WHISPER_OS_REPO>`: canonical product repo, docs, launcher source mirror, runtime docs.
- `<GRAPHENEOS_ROOT>`: local GrapheneOS checkout used to build the ROM and carry active Android source changes.
- `<VERE_ROOT>`: mobile Vere fork with Android build/runtime patches.
- `<ARTEMIS_REPO>`: Urbit-side app used or planned for mobile moon provisioning from the parent planet.

Canonical product docs are in `<WHISPER_OS_REPO>`. Avoid putting product roadmap or controller docs into the Vere runtime fork.

## Current High-Level State

### Proven

- GrapheneOS-based husky / Pixel 8 Pro ROM boots.
- `nativeplanet_vere` init service can start a real moon.
- Modern `vere64` binary is installed and used.
- Controller can write network state and resolver files.
- Status provider returns real network/runtime data.
- `conn.sock` is enabled and works on Android.
- Controller can poll `conn.sock` using direct `android.system.Os` Unix socket calls.
- Provider reports real runtime state from controller polling.
- Graceful `|exit` through Click/conn.sock has been tested and works as a clean product stop path.
- Launcher3/Quickstep is now the active home launcher, not the earlier Compose toy launcher.
- Launcher3 has a working "My Urbit Apps" entry point and can pin hosted Urbit app shortcuts.
- Tlon and Dojo hosted app entries open a branded local web shell instead of broken Android placeholders.
- A signed ROM candidate with the latest Launcher3 hosted-app changes was generated.

### Not Yet Fully Solved

- Production-grade parent planet -> phone moon provisioning and app sync.
- The mobile moon pill/application set.
- Real Landscape/Docket app inventory and tile metadata on device.
- Real Tlon/Dojo/Grove/Kin local mobile web app entrypoints.
- Full Whisper OS visual skin across Launcher3, quick settings, notification shade, lock screen, and system surfaces.
- Fresh-flash verification of the latest signed ROM candidate.
- Production/user build strategy versus userdebug development strategy.
- Full repo sync discipline between GrapheneOS active source and the product repo.

## Current Runtime Baseline

The current phone has been verified with a running dev moon and provider state similar to:

```text
runtime.state=running
runtime.version=4.3-33293b1
runtime.connSockAvailable=true
```

The current active test moon is intentionally not listed here with any key material. Moon names may appear in device logs and test notes, but keys and access codes must not be committed.

## Conversation History Condensed

This section compresses the working history so a reviewer understands why the system looks the way it does.

### 1. ROM boot and NativePlanet base

Early work focused on getting a GrapheneOS-based ROM to boot reliably with a NativePlanet vendor overlay. A bootloop issue around `init.insmod.*.cfg` was fixed and verified. The native controller service and status provider were added under `vendor/nativeplanet`.

Key lessons:

- Build warmth varies wildly. Some module builds are seconds; full ROM builds are hours.
- Full ROM builds should be batched behind preflight gates.
- Userdebug builds are essential for this development loop.

### 2. Provider/network/controller bring-up

The controller wrote `/data/nativeplanet` state files and the launcher/provider could read real network status. A setgid and SELinux batch fixed the controller's ability to write and the provider's ability to read status files.

Important fixes:

- `/data/nativeplanet` uses setgid so group inheritance works.
- Controller avoids failing `chown` and relies on setgid + chmod.
- SELinux grants were kept narrow.

### 3. Real moon runtime

Disposable dev moons replaced fake ships and comets for most testing. Real moon boot through init was verified. The runtime served HTTP and bound Ames sockets. DNS/resolver and network validation worked.

Important product decision:

- Use throwaway dev moons for testing, but keep the product moon-first. Comets are later.

### 4. Lens deprecation and Click/conn.sock

Lens was identified as deprecated and unreliable for health checks. Click/conn.sock became the runtime truth path. Vere originally disabled `conn.sock` on Android; the Android-specific guard was removed in the Vere fork and a new `vere64` binary was built.

Important fixes:

- Enable `conn.sock` in Vere on Android.
- Add SELinux permission for `nativeplanet_vere` to create `sock_file` under `nativeplanet_data_file`.
- Verify `conn.sock` with jam/cue newt-framed `%peel` requests.

Verified commands through a custom Node client:

- `%peel %live`
- `%peel %who`
- `%peel %info`
- `%peel %v`

### 5. Controller conn.sock polling

The controller gained:

- Minimal Java noun jam/cue support.
- Newt framing.
- Direct Unix socket client.
- Runtime poller that writes `runtime-status.json`.
- Provider endpoint returning runtime truth.

Important implementation lesson:

- Android `LocalSocket` with `FILESYSTEM` namespace failed before native `connect()`.
- Direct `android.system.Os.socket/connect/read/write` worked.
- `platform_apis: true` was required for the controller module, not `sdk_version: "module_current"`.

### 6. Graceful lifecycle

Research against upstream Vere and Native Planet GroundSeg showed the product stop path should mirror GroundSeg:

1. Mark desired runtime state stopped.
2. Send Urbit graceful `|exit` through Click/conn.sock.
3. Poll for process exit with a long timeout.
4. Fall back to SIGTERM-to-king if Click fails.
5. Hard init stop only as an emergency force-stop.

Android init hard stop / `setprop nativeplanet.vere.enabled 0` is not a product stop path.

### 7. Artemis and provisioning

Artemis is intended to help create/manage mobile moons from the parent planet. The parent planet flow should eventually ask for:

- Hosting URL.
- Access code.

Then the phone should discover enough to provision/import the moon without manual adb/root work.

The user is comfortable using temporary development codes and cycling them, but production must treat these as sensitive and never log/commit them.

### 8. Compose launcher detour

An earlier Compose launcher was built from design handoff files. It had attractive pieces but behaved like a toy:

- Poor Android gesture integration.
- Weak home/workspace behavior.
- Drag/drop bugs.
- Easy escape back to stock Graphene launcher.
- Missing native launcher features.

Decision: stop trying to rebuild Android launcher behavior from scratch. Use Launcher3/Quickstep as the base and skin/extend it.

### 9. Launcher3 pivot

Launcher3/Quickstep is now the home launcher. Whisper OS work moved into Launcher3:

- App name rebranded to Whisper OS.
- Whisper visual resource overrides started.
- Default workspace changed.
- "My Urbit Apps" activity added.
- Hosted Urbit app WebView shell added.
- Hosted app shortcuts can be pinned to workspace.
- Shortcut title/icon persistence across reload was fixed.
- Duplicate pin detection was fixed.
- Provenance marks were added to app icons.

This is the current preferred direction.

## Current ROM / Release State

A full target-files build completed successfully for the latest Launcher3 hosted-app integration.

Relative artifacts under `<GRAPHENEOS_ROOT>`:

```text
out/target/product/husky/obj/PACKAGING/target_files_intermediates/husky-target_files.zip
releases/2026061101/release-husky-2026061101/husky-factory-2026061101.zip
releases/2026061101/release-husky-2026061101/husky-img-2026061101.zip
releases/2026061101/release-husky-2026061101/husky-install-2026061101.zip
releases/2026061101/release-husky-2026061101/husky-ota_update-2026061101.zip
releases/2026061101/release-husky-2026061101/husky-target_files.zip
```

Important:

- The release artifact has not been flashed yet.
- The phone is currently running live-pushed Launcher3 fixes.
- Factory/install flashing will wipe data. Do not do that casually.
- Consider whether an OTA update path is safe/compatible before flashing if preserving current device state matters.

## Build Commands

From `<GRAPHENEOS_ROOT>`:

```sh
source build/envsetup.sh
lunch husky bp4a userdebug
m NativePlanetController -j10
m Launcher3QuickStep -j10
m vendorbootimage vendorkernelbootimage target-files-package -j10
```

Release generation used:

```sh
source build/envsetup.sh
lunch husky bp4a userdebug
export BUILD_NUMBER=2026061101
script/finalize.sh
password='' ./script/generate-release.sh husky 2026061101
```

The empty `password` environment variable was needed because the local release-key decrypt script otherwise prompted interactively.

## Verification Commands

Runtime/provider:

```sh
adb shell content call --uri content://io.nativeplanet.controller --method getRuntime
adb shell content call --uri content://io.nativeplanet.controller --method getHostedApps
```

Home launcher:

```sh
adb shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN \
  -c android.intent.category.HOME
```

Launcher database inspection:

```sh
adb shell 'sqlite3 /data/user/0/com.android.launcher3/databases/launcher_4_by_5.db \
  "select _id,title,itemType,container,screen,cellX,cellY,length(icon) from favorites order by container,screen,cellY,cellX,_id;"'
```

Fresh log scan:

```sh
adb logcat -d -t 500 | grep -E "AndroidRuntime|FATAL|Whisper|Launcher|NativePlanet"
```

## Key Code Areas To Review

### GrapheneOS / Android vendor overlay

Under `<GRAPHENEOS_ROOT>/vendor/nativeplanet`:

- `controller/`
- `init/nativeplanet-vere.rc`
- `src/nativeplanet-vere-launch.c`
- `sepolicy/private/*.te`
- `prebuilts/bin/vere`
- `prebuilts/pill/*`

Review focus:

- SELinux scope and overbroad grants.
- Key material handling and file permissions.
- Atomic writes and data durability.
- Runtime start/stop semantics.
- Controller/provider permission boundaries.
- Whether provisioning APIs are safe for production.

### Launcher3 / Quickstep

Under `<GRAPHENEOS_ROOT>/packages/apps/Launcher3`:

- `AndroidManifest.xml`
- `quickstep/AndroidManifest.xml`
- `res/values/strings.xml`
- `res/values/config.xml`
- `res/xml/default_workspace_4x5.xml`
- `src/com/android/launcher3/WhisperHostedAppsActivity.java`
- `src/com/android/launcher3/WhisperHostedWebActivity.java`
- `src/com/android/launcher3/WhisperAppProvenance.java`
- `src/com/android/launcher3/BubbleTextView.java`
- `src/com/android/launcher3/model/LoaderCursor.java`
- `src/com/android/launcher3/model/WorkspaceItemProcessor.kt`
- `src/com/android/launcher3/util/ItemInflater.kt`

Review focus:

- Shortcut pinning correctness.
- Launcher DB reads and same-process provider constraints.
- Icon persistence and duplicate detection.
- WebView security settings.
- Intent handling and URL validation.
- Whether `ITEM_TYPE_SHORTCUT` support introduces restore/import regressions.
- Whether the provenance icon drawing is robust and not too invasive.

### Vere Android fork

Under `<VERE_ROOT>`:

- `pkg/vere/main.c`
- `pkg/vere/io/conn.c`
- Android build patches.
- Android logging additions.
- `build.zig` and subpackage build files.

Review focus:

- `conn.sock` Android enablement risk.
- `vere64` verification and prevention of old runtime regression.
- Logging: no secret material.
- Android-specific defaults for LMDB/loom/memory.
- Graceful shutdown handling.

### Product repo

Under `<WHISPER_OS_REPO>`:

- `docs/ROADMAP.md`
- `docs/architecture/hosted-urbit-apps.md`
- `docs/architecture/artemis-mobile-provisioning.md`
- `docs/research/2026-06-08-graceful-shutdown-research.md`
- `docs/verification/2026-06-11-launcher3-hosted-apps.md`
- `launcher/` legacy Compose app source and design assets.

Review focus:

- Whether docs accurately reflect code.
- Whether stale Compose launcher code should be archived, deleted, or kept as design reference.
- Whether the product repo should vendor/sync Launcher3 changes or point to the GrapheneOS checkout.
- Repo hygiene: no local paths, no secrets, no AI scratchpad tone in public docs.

### Artemis / parent planet app

Under `<ARTEMIS_REPO>`:

- Review current mobile moon provisioning support.
- Review how it exposes or can expose mobile moon creation/import metadata.
- Review whether Docket/Landscape app inventory can be surfaced for the phone.
- Review whether Grove/Kin/Tlon/Dojo can be installed or presented cleanly.

## Current Hosted App Model

The phone has a controller-provided hosted-app inventory. Seeded dev entries include:

- Tlon
- Dojo
- App Store

The target product model:

- "My Urbit Apps" lives next to Play Store / App Store in the Android launcher.
- It should eventually show all apps from the ship's Landscape/Docket inventory.
- Apps can have different launch modes:
  - native Android package
  - PWA
  - local WebView against the local Vere HTTP service
  - external browser fallback
- Users can pin Urbit apps to Android homescreens.
- Urbit app icons should eventually use existing Urbit tiles/metadata, not placeholder letter icons.

Review question:

Is the current hosted-app model pointing at the right Urbit data source? If not, what should be the authoritative source and protocol?

## UX Direction

The product is no longer trying to replace Android launcher mechanics with a custom Compose launcher.

Current direction:

- Use Launcher3/Quickstep for Android-native launcher behavior.
- Skin and extend it for Whisper OS.
- Add first-class Urbit surfaces into the launcher.
- Keep Android status bar for now.
- Later skin status bar, notification shade, quick settings, and lock screen where practical.

User expectations:

- Pixel 8 Pro target.
- Avoid UI under the front camera area.
- Standard Android gestures must work.
- Drag/drop, multi-page workspace, app drawer, recents, and long-press menus should feel native.
- Urbit apps should feel first-class, not buried inside a weird standalone app.

## Known Product Gaps

### Must fix before "carry as second phone"

- Fresh-flash latest ROM candidate or safely OTA it.
- Verify WiFi/setup/runtime after flash without manual root.
- Ensure parent-hosted provisioning works through the UI.
- Ensure running moon persists across reboot.
- Ensure graceful shutdown/start works through product controls.
- Ensure My Urbit Apps is stable and useful.
- Ensure Tlon/Dojo or equivalent basic Urbit surfaces are actually usable.
- Make Launcher3 visual skin feel coherent enough for daily use.

### Must fix before production

- Production key handling.
- Production bootloader/signing/update story.
- User build vs userdebug split.
- No adb/root dependency.
- No permissive policy or live overlays.
- No debug-only shortcuts for provisioning.
- Clear data migration/backup story for moon pier and keys.
- Security review of WebView and hosted app launch modes.
- Review of Android permissions and exported activities/providers.

## Known Technical Risks

- Launcher3 changes are active in GrapheneOS source and may not be fully synced into the product repo.
- Compose launcher code still exists in the product repo and may confuse future work.
- Factory flashes wipe data and can lose the current dev state if backups are not explicit.
- WebView app shell may become a security boundary; treat hostile content and local privileged endpoints carefully.
- Controller currently has powerful access to runtime state and files. Its public IPC contract must stay narrow.
- `conn.sock` gives real control-plane access. Any bridge to UI must be permissioned carefully.
- Artemis/mobile provisioning is the next major cross-ship trust boundary.

## Specific Questions For Fable

1. Is Launcher3/Quickstep the right base, or should another Android launcher base be used?
2. Are the Launcher3 shortcut changes safe enough, or did we patch the wrong layer?
3. Should "My Urbit Apps" be an Activity, a Launcher3 system surface, or another primitive?
4. What is the cleanest path to Docket/Landscape app inventory on the phone?
5. How should Tlon/Dojo/Grove/Kin be represented: local WebView, PWA, native wrapper, or something else?
6. What is the minimum viable "moon pill" for a phone moon?
7. How should the parent planet and phone moon coordinate apps/preferences/posts?
8. Is the controller/provider IPC contract too broad or too narrow?
9. Are the SELinux rules minimal and production-shaped?
10. What must be changed before moving from userdebug to a production-like user build?
11. What is the safest data-preserving update path for this device right now?
12. How should repo organization be improved so GrapheneOS source changes are not orphaned?
13. What code should be deleted or archived to reduce confusion?
14. Where are we likely kidding ourselves?

## Review Output Format Requested

Use severity labels:

- `BLOCKER`
- `HIGH`
- `MEDIUM`
- `LOW`
- `NIT`

For each finding include:

```text
Severity:
Area:
File(s):
Issue:
Why it matters:
Suggested fix:
How to verify:
```

Then provide:

```text
Recommended next 5 steps:
1.
2.
3.
4.
5.
```

## Things Not To Do During Review

- Do not paste or request real moon keys or access codes.
- Do not recommend Lens.
- Do not recommend `vere32` or old runtime paths.
- Do not assume the Compose launcher is still the product launcher.
- Do not flash or wipe the device without explicit user approval.
- Do not commit generated build outputs.
- Do not add local absolute paths to public docs.

## Current Best Next Step Before More Feature Work

My current recommendation is:

1. Review the Launcher3 changes hard.
2. Decide whether to OTA/flash the `2026061101` candidate or keep testing live-pushed Launcher3.
3. Back up `/data/nativeplanet` before any wipe-risk operation.
4. Verify fresh boot with current moon and My Urbit Apps.
5. Move next into parent planet provisioning + Docket/Landscape app inventory.

That said, this handoff exists because we want Fable to challenge that plan.
