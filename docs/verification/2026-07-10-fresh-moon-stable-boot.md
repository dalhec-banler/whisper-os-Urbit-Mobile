# Fresh moon boots stably end-to-end — 2026-07-10

**Device:** Pixel 8 Pro (husky), ROM 2026062202 base + rebuilt controller/vere
**Outcome:** RESOLVED. A freshly provisioned moon
(`~sabwel-winwen-dozzod-hobdem`, sponsor `~hobdem`) now boots, reaches its
sponsor, settles to a stable live ship, and **auto-starts to `running`
across reboots** under the controller — no host proxy, no manual steps.

## Two root causes found and fixed

### 1. vere HTTPS/TLS broken in the deployed binary

During dawn, vere fetches the Azimuth galaxy table by POSTing a 256-call
`getPoint` batch to `https://roller.urbit.org/v1/azimuth` (that endpoint is
alive; it is POST-only, so a GET 404s and made it look dead). The deployed
binary (built 2026-05-28) failed the TLS handshake for *every* HTTPS host —
proven by an `adb reverse` test where vere's plain-HTTP POST reached a host
listener while HTTPS returned a generic curl "Error" (the build disables
curl verbose strings). Not DNS (ames resolved 50+ galaxy hostnames), not
the clock, not the CA bundle (the embedded 2024-07-02 bundle validates
roller's Let's-Encrypt/ISRG chain), not SELinux (failed as root too).

**Fix:** rebuilding this vere fork from current source with zig 0.15.2
(`-Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast -Dandroid=true`)
produces a binary that completes dawn over real HTTPS on-device. The old
binary simply had broken TLS. Also added numeric-CURLcode logging to
`dawn.c` for future diagnosis. Deployed to `/system_ext/bin/vere`
(re-signed context restored).

### 2. The "%kids install loop" was the controller crashing the ship

A fresh moon appeared to loop forever on `kiln: install %base from
~hobdem/%kids`, with the docket agent crashing spider
(`%arvo-response`, `bail 2/4`) every ~50s. The real cause: the controller's
`HostedAppsPoller` runs a khan-eval that scries `/gx/docket/charges/noun`
every poll. On a fresh moon, `%docket` is not yet running while `%base`
installs, and scrying a not-running agent **bails and crashes the strand**.
The repeated spider crashes destabilized kiln, stalling the initial %base
install in a retry loop. Booting the same pier with the controller not
polling it produced **zero crashes, zero loop** — the ship went straight to
stable and network-live.

**Fix:** `HostedAppsPoller` now guards each scry with a `%gu` agent-liveness
check (`(scry ? /gu/docket)` / `/gu/nativeplanet-mobile`) and skips the
crashing scry until the agent is live. Rebuilt, signed with the release
platform key, deployed.

## Also fixed en route

- `ProvisioningManager.ensureDirectory()` created `keys/`/`ships/` at 0700
  under the controller umask, locking out the shell-group runtime on a
  truly fresh `/data/nativeplanet` (committed earlier, e884b67).
- Operational note: running vere manually via `adb root` writes pier files
  as `root:root`; the init service runs vere as `shell`, so the pier must be
  `chown -R shell:shell` afterward or boot fails in ~80 ms.

## Verification

- Moon `running` and stable for >3 min under active controller polling;
  kernel log shows **0** `arvo-response`/`spider crashed` events (was ~1 per
  50 s before).
- `hosted-apps.json` poll completes gracefully (no ship crash) while docket
  is still coming up.
- **Reboot persistence:** after a full reboot with no manual step, the moon
  auto-started to `running`, `shipName=~sabwel-winwen-dozzod-hobdem`
  (derived via PatpFormatter), `lastError=null`, 0 crashes.
- Fresh dawn over real `https://roller.urbit.org` (no proxy) completes:
  galaxy table, sponsor keys for `~hobdem`/`~dem`, pier created.

## Remaining

- Provider permission hardening (READ_STATUS → signature|privileged), last.
- The stale `roller.urbit.org` dependency for dawn is upstream vere design;
  fine as long as that endpoint lives.
