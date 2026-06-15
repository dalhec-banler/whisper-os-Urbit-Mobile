# Provisioning MVP Checklist

Status: active tracker as of 2026-06-15

This checklist tracks the next real product step after the verified ROM/runtime
baseline: a user should be able to pair the phone with a parent planet, have
Artemis create a `%mobile` moon, have the controller provision it, and land on
truthful runtime status without manual adb-root file pushing. Manual moon-key
import remains the advanced fallback.

## Verified Baseline

- [x] Full ROM flash, not live overlay, boots cleanly on husky.
- [x] `vere64` runs under init-managed `nativeplanet_vere`.
- [x] Controller writes real WiFi/DNS state.
- [x] Provider exposes real network, boot package, and runtime status.
- [x] `conn.sock` polling works in enforcing mode through controller.
- [x] Dev moon `~milweg-dapseg-palrum-roclur` boots from a valid v1 boot package.
- [x] Provider `stopRuntime` performs graceful conn.sock shutdown.
- [x] Provider `startRuntime` brings the moon back.
- [x] Reboot persistence restores the running moon.
- [x] Launcher is bundled in the ROM and renders real provider data.
- [x] Hosted Urbit apps open through authenticated local WebView sessions.

## Product Step 1: Controller Provisioning API

- [x] Define final provider/control method names for provisioning.
- [x] Define provisioning request JSON shape.
- [x] Define provisioning response JSON shape with structured error codes.
- [x] Implement key import without logging raw key material.
- [x] Validate modern moon key format before writing files.
- [x] Derive or validate ship name from key material.
- [x] Validate parent ship for moon boot packages.
- [x] Write key file with safe owner, group, mode, and SELinux label.
- [x] Write v1 `boot-package.json` atomically.
- [x] Create or verify pier directory layout.
- [x] Validate pill path and readability.
- [x] Return boot-package validation through provider.
- [x] Start runtime after successful provisioning.
- [x] Surface provisioning failures as actionable codes.
- [x] Add module-level build gate for controller changes.
- [x] Add adb/provider-level provisioning smoke test.
  - `tools/smoke-controller-provider.sh` verifies runtime, boot package,
    network, and the safe empty-request guard for `provisionMoon`.

## Product Step 2: Launcher Import Flow

- [x] Replace stub import action with real provider provisioning call.
- [x] Add paste/import key input path for throwaway dev moons.
- [x] Parse ship and parent fields without exposing the key after submit.
- [x] Show validation errors from controller, not launcher guesses.
- [x] Show provisioning progress states.
- [x] Start runtime from controller response.
- [x] Navigate to reveal screen using real provisioned identity.
- [x] Land on Runtime Console with real provider state.
- [x] Ensure demo/fallback banner only appears when controller is unavailable.
- [x] Build launcher APK.
- [x] Install launcher APK on device.
- [x] Bundle launcher APK in ROM and verify it after no-wipe flash.
- [x] Add runtime home entry into onboarding.
- [x] Add hosting URL and `+code` pairing screen.
- [x] Add Identity settings entry back into onboarding for adding another identity.
- [x] Humanize parent-pairing failures instead of showing raw backend codes.
- [ ] Verify import UI with a throwaway moon.

## Product Step 3: Fresh-Phone End-To-End Test

- [ ] Start from a factory-flashed or cleared `/data/nativeplanet` device.
- [ ] Complete Android setup enough for WiFi and launcher use.
- [ ] Open NativePlanet launcher.
- [ ] Import a throwaway moon only through UI/controller.
- [ ] Confirm `boot-package.json` and key file are written correctly.
- [ ] Confirm runtime reaches `running`.
- [ ] Confirm launcher shows correct ship, parent, network, and runtime state.
- [ ] Test graceful stop from launcher.
- [ ] Test start from launcher.
- [ ] Test restart from launcher if implemented.
- [ ] Reboot phone and verify moon auto-starts.
- [ ] Confirm no raw key material appears in logcat, docs, screenshots, or process args.
- [ ] Record final verification report.

## Product Step 4: Documentation And Source Sync

- [x] Update controller API contract with current Artemis-backed pairing direction.
- [x] Update onboarding product doc to match moon-first import flow.
- [x] Document Artemis mobile provisioning architecture.
- [x] Record current Artemis parent probe without secrets.
- [x] Sync source-only GrapheneOS `vendor/nativeplanet` changes into this repo.
- [x] Preserve Launcher3/Quickstep changes as `rom/patches/launcher3-whisper-os.patch`.
- [ ] Update verification report after fresh-phone test.
- [ ] Sync companion launcher source changes into this repo once the dirty UI branch is reviewed.
- [ ] Run git hygiene checks before commit.

## Product Step 5: Artemis-Backed Parent Provisioning

- [x] Identify Artemis as the parent-side moon authority.
- [x] Confirm Artemis has a `%mobile` role.
- [x] Confirm Artemis exposes moon boot keys as `%uw` JSON for its frontend.
- [x] Update controller probe from a hypothetical parent service to Artemis.
- [x] Decide whether Android should use the Urbit channel API or a small Artemis mobile HTTP endpoint.
- [x] Implement the Urbit channel poke for Artemis `%mobile` moon creation.
- [x] Return the created moon's ship, parent, and boot key to the controller.
- [x] Chain the Artemis response into existing local `provisionMoon`.
- [x] Add a parent-pairing smoke test that does not print the `+code` or boot key.
- [x] Confirm parent login, webterm, and hood scry work with a real parent URL and access code.
- [x] Confirm phone-side Artemis contract matches the current Artemis GitHub source.
- [x] Confirm the parent ship is serving the updated Artemis desk.
- [x] Confirm Artemis can create a new `%mobile` moon for the phone.
- [x] Provision an Artemis-created `%mobile` moon through the controller fallback path.
- [x] Patch controller source to fall back to Artemis scry when the channel fact is missed.
- [x] Include the Artemis scry fallback in a signed no-wipe OTA.
- [x] Patch controller validation for current Artemis `%uw` mobile moon seed suffixes.
- [x] Verify direct `pairWithPlanet` end-to-end with a throwaway `%mobile` moon.
  - Verified with `~wacpeg-hodpel-palrum-roclur` from `~palrum-roclur`.
  - Provider runtime reached `running` with `connSockAvailable=true`.
  - Provider and launcher smokes passed after pairing.
  - Plain reboot persistence passed.
- [x] Re-confirm current parent Artemis deployment after latest Artemis update.
  - Non-mutating probe passed on 2026-06-15.
  - Existing `%mobile` moon inventory exposes `who`, `nam`, `rol`, `pub`,
    `sec`, `lif`, `rif`, `sed`, `dat`, and `tag`.

## Product Step 6: Hosted Urbit Apps

- [x] Decide Launcher3/Quickstep is the product HOME shell.
- [x] Add first-party `My Urbit Apps` Launcher3 surface.
- [x] Discover installed Urbit app inventory from the running moon through
  Click/conn.sock and Docket metadata.
- [x] Expose hosted app inventory through the controller provider.
- [x] Remove fake hosted app fallback tiles.
- [x] Pin hosted Urbit apps to the normal Launcher3 workspace.
- [x] Refresh pinned state when returning to `My Urbit Apps`.
- [x] Open pinned hosted apps in the Whisper-hosted WebView shell.
- [x] Avoid Android broken-page placeholder for unsupported mobile surfaces.
- [x] Add hosted app unpin and open-in-browser actions.
  - Included in the 2026-06-15 no-wipe OTA.
- [x] Authenticate hosted app WebView launches without exposing web login codes.
- [x] Verify Landscape, Tlon/Groups, and Terminal/Webterm open from local Vere.
- [x] Pin hosted apps to the Launcher3 workspace and relaunch them after reboot.
- [x] Define the mobile app surface contract for native/PWA/local-web fallback.
- [x] Add source support for known companion-app detection when installed.
  - Current hook: `%groups` / Tlon can prefer the native Android package if
    present; otherwise it keeps using the verified local WebView path.
  - `NativePlanetController` module build passed; not flashed yet.
- [x] Add draft `%nativeplanet-mobile` desk source for launcher metadata.
  - Source exists under `satellite-pill/desks/nativeplanet-mobile/`.
  - Gall app passes host `urbit eval` parse/type smoke.
  - Not pill-built or installed on a ship yet.
- [ ] Build the mobile desk/moon pill so hosted apps expose phone-ready
  entrypoints.
- [ ] Wire Docket tile images into Launcher3 icons where available.
- [ ] Validate Tlon, Terminal, Landscape, Grove, and Kin as real phone surfaces.

## Not In This Step

- [ ] Groundwire comet support.
- [ ] Multi-ship switching.
- [ ] Lick bridge.
- [ ] Global SystemUI replacement.
- [ ] Parent/satellite sync beyond initial Artemis moon provisioning.
