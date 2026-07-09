# Fresh-phone E2E test (Product Step 3) — 2026-07-09

**Device:** Pixel 8 Pro (husky), ROM 2026062202
**Outcome:** Provisioning path now works end-to-end and a fresh moon boots and
reaches its sponsor. Two real bugs found (one fixed), and first-boot does not
yet settle. Details below.

## Result summary

- **Pairing + provisioning: WORKING.** Controller `pairWithPlanet` against
  `~hobdem` (`https://hobdem.startram.io`) authenticated, Artemis minted
  `~sabwel-winwen-dozzod-hobdem`, and the controller wrote a valid boot
  package and key file.
- **Bug #1 (FIXED): fresh-provision directory permissions.**
  `ProvisioningManager.ensureDirectory()` created `keys/` and `ships/` with
  `mkdirs()` only, which under the controller's 0077 umask yields `0700`
  (no group access). vere runs as group `shell` and must traverse `keys/`
  to read its key and write into `ships/` to create its pier, so first boot
  died with "Key file not found". Fixed by `Os.chmod(dir, 0770)` after
  create (idempotent). Rebuilt, signed with the release platform key, and
  deployed.
- **Bug #2 (NOT fixed, characterized): vere HTTPS is broken on this Android
  build.** During dawn, vere fetches the Azimuth galaxy table by POSTing a
  256-call `getPoint` batch to `https://roller.urbit.org/v1/azimuth`
  (overridable with `-e`). That endpoint is **alive** (POST-only; a GET 404s,
  which is what made it look dead). The real failure is that vere's embedded-CA
  TLS handshake fails for every HTTPS host on this build — proven by an
  `adb reverse` test where vere's *plain-HTTP* POST reached a host listener
  while every HTTPS attempt returned a generic curl "Error" (the build
  disables curl verbose strings, hiding the specific code). Root shell, so
  not SELinux.
- **Moon boots via a one-time host proxy.** Standing up a plain-HTTP→`roller`
  forwarding proxy on the host (over `adb reverse`, so vere talks plain HTTP
  and the host does the TLS) let dawn complete: galaxy table retrieved,
  sponsor keys for `~hobdem`/`~dem` fetched, pier created, Arvo booted (1770
  jets), Ames came up and resolved the galaxies, conn.sock served, and
  `peel who` returned `995073605278680554` = `~sabwel-winwen-dozzod-hobdem`
  (cross-checked with urbit-ob and the on-device PatpFormatter). The moon
  reached its sponsor and began `kiln: install %base from ~hobdem/%kids`.
- **First boot does NOT settle (open).** The moon then loops roughly every
  60 s: `kiln: downloading update for %base from ~hobdem/%kids` →
  `activated install` → the docket agent queries `/charges/noun`, spider
  crashes (`%arvo-response`, `bail: 2/4`), repeat. It never reaches a stable
  live state. This looks like a satellite.pill `%base` vs `~hobdem/%kids`
  desk-compatibility problem and needs separate investigation.

## What this means

The controller/launcher provisioning stack is now correct on a genuinely
fresh `/data/nativeplanet` (bug #1 was only reachable on a true fresh wipe,
which is exactly the fresh-phone scenario prior non-wiped runs never hit).
The remaining blockers to an autonomous fresh moon on the phone are both
below the controller:

1. **vere TLS on Android** — vere must be able to fetch the Azimuth galaxy
   table over HTTPS on-device (today it can't; a host proxy is a lab-only
   workaround). Fix in the vere fork's curl/CA setup (`_setup_ssl_curl` /
   `_curl_ssl_ctx_cb` in `pkg/vere/main.c`) or bake an Azimuth snapshot.
2. **first-boot %base loop** — reconcile the satellite.pill `%base` with the
   sponsor's `%kids` so the initial install settles.

## Device state left

Idle and stable: no ship running, `nativeplanet.vere.enabled=0`, autostart
off. The `~sabwel-winwen-dozzod-hobdem` pier (~540 MB, past dawn) is preserved
on disk. The prior `~pacbyr-balteb-palrum-roclur` moon is in the host backup
`~/whisper-backups/nativeplanet-full-pre-e2e-20260709.tar.gz` and can be
restored. Key-material audit of the run was clean (no `+code` or key bytes in
logs; the launch wrapper redacts key refs).

## To finish (needs decisions/deeper work)

- Decide phone's ship: keep debugging the hobdem moon's %base loop, or restore
  the working pacbyr moon meanwhile.
- Fix vere HTTPS on Android so dawn works without a host proxy.
- Re-run the checklist in `docs/product/provisioning-mvp-checklist.md`
  §"Product Step 3" once the moon boots to a stable live state.
