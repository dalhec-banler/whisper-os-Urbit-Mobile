# Fresh-phone E2E test (Product Step 3) — 2026-07-09

**Device:** Pixel 8 Pro (husky), ROM 2026062202, controller rebuilt 2026-07-07
**Outcome:** BLOCKED at the pairing step. Parent authentication failed; the
rest of the fresh-provision chain could not run. Device restored to the
working moon afterward.

## What ran

1. **Backup.** Full `/data/nativeplanet` (2.36 GB, 743 entries, live pier
   event log intact) streamed to
   `~/whisper-backups/nativeplanet-full-pre-e2e-20260709.tar.gz` before any
   destructive step.
2. **Graceful stop.** Controller `stopRuntime` → `STOP_REQUESTED`; init
   service reached `stopped`, `runtime-status.json` reported `stopped` with
   no error. (Graceful conn.sock shutdown path confirmed working.)
3. **Wipe.** `/data/nativeplanet/*` cleared; directory + SELinux label
   preserved. Controller correctly reported `state=stopped`,
   `lastError="no boot-package"`.
4. **Pairing — BLOCKED.** See below.
5. **Restore.** Original moon restored from backup and brought back to
   `running` (see "Restore" below).

## Blocker: parent authentication to ~hobdem

Pairing target: `https://hobdem.startram.io`, parent `~hobdem`,
provisioning `+code` supplied by the operator.

`/~/host` returns `~hobdem` and `/apps/artemis/` answers, so the ship and
Artemis are reachable. But **login is rejected**:

| Probe                                   | Result |
|-----------------------------------------|--------|
| `POST /~/login` with supplied `+code`   | HTTP 400, base session cookie set |
| `POST /~/login` with a known-wrong code | HTTP 400, base session cookie set (same) |
| `GET /~/scry/hood/kiln/pikes.json`      | HTTP 403 (unauthenticated) |
| `GET /~/scry/hood/our.json`             | HTTP 403 |

A correct Urbit `+code` login returns 204 + `urbauth` cookie and unlocks
scries. Both the supplied code and a deliberately wrong code produce the
identical 400 + 403 pattern, so the supplied code is **not being accepted
by ~hobdem**. Most likely the code has rotated/is stale, or it is for a
different ship than the one hosted at that URL.

The controller's `ParentPairingManager.pairWithPlanet` uses exactly this
path — `POST /~/login` then `confirmAuthenticated` via the
`hood/kiln/pikes.json` scry requiring HTTP 200 — so it would (correctly)
return `PARENT_AUTH_FAILED`. Driving the flow was not attempted because it
cannot succeed until a working credential is available. **No security gate
was bypassed.**

### Secondary observations (for the ship owner, not blockers)

- Artemis `/moons` streamed moon **seed material** to a session carrying a
  *failed-login* cookie during probing. A fully unauthenticated session
  (no cookie at all) got HTTP 204 on subscribe and **zero** seed records,
  so this is not an open-to-the-world leak, but Artemis treating a
  failed-login session as authorized enough to receive seeds is worth a
  look on the parent.
- If a StarTram-fronted parent ever blocks `hood/kiln` scries for an
  otherwise-valid session, `confirmAuthenticated` would false-negative.
  Pairing is verified working against Tlon hosting (`palrum-roclur`); the
  StarTram auth-confirm path is unverified. Latent robustness note only.

## Restore (and a bug found + fixed)

Restoring the backup surfaced a real ownership bug in the manual restore
procedure (not in product code): a blanket `chown -R system:shell` on the
restored tree broke boot. The moon runs as user **shell**; its pier and
`logs/` are natively `shell:shell`, while only the controller-written
metadata (`keys/`, `*.json`, the `ships/` container) is `system:shell`.
Forcing everything to `system` made the launch wrapper unable to open its
log (silent 99 ms `exit 1`) and vere unable to open its own pier.

Correct ownership, taken from the backup's preserved per-path owners:

- `ships/<ship>/**`, `logs/**` → `shell:shell`
- `ships/` container, `keys/**`, `*.json`, `resolv.conf`, top dir → `system:shell`

After applying that and `restorecon -RF`, a clean reboot auto-started the
moon: `~pacbyr-balteb-palrum-roclur`, `running`, `connSockAvailable=true`,
`lastError=null`. conn.sock `peel who` returned ship id
`10805379437197648590`, which decodes to `~pacbyr-balteb-palrum-roclur`
(identical from urbit-ob and the on-device PatpFormatter) — the derived-@p
live path validated end-to-end.

## Key-material audit — PASS

28,344 lines of logcat captured across the whole session plus all
on-device `/data/nativeplanet/logs`: **zero** occurrences of the `+code`,
no `password=` values, no PEM/seed key material. The launch wrapper
redacts key references (`keyMaterialRef: [redacted]`). The staged `+code`
file used for probing was `shred`-removed.

## To resume the fresh-provision test

1. Operator supplies a **current** `+code` for the parent (from `+code`
   in the parent's dojo, or `|code %reset` then read it), and the exact
   hosting URL that authenticates.
2. Re-run steps 1–3 above (backup, stop, wipe).
3. Drive `pairWithPlanet` (UI or controller) and complete the checklist
   items in `docs/product/provisioning-mvp-checklist.md` §"Product Step 3".
