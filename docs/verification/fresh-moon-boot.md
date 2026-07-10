# Fresh moon boots stably end-to-end

Verified 2026-07-10 on a Pixel 8 Pro (husky) userdebug device running the ROM
`2026062202` base with a rebuilt controller and Vere.

A freshly provisioned moon boots, reaches its sponsor over Ames, settles into a
stable network-live ship, and auto-starts to `running` across reboots under the
controller, with no host-side tooling and no manual steps. Getting there
required fixing two device-only problems.

## 1. Vere's HTTPS stack was broken in the deployed binary

During dawn, Vere fetches the Azimuth galaxy table by POSTing a 256-call
`getPoint` batch to `https://roller.urbit.org/v1/azimuth`. That endpoint is
live and POST-only, so a plain GET returns 404 and can look dead. The real
problem was that the previously deployed binary failed the TLS handshake for
every HTTPS host. An `adb reverse` test isolated it: Vere's plain-HTTP POST
reached a host listener while every HTTPS attempt returned a generic curl error
(the Android build compiles curl without verbose error strings, which hid the
specific code). It was not DNS — Ames resolved dozens of galaxy hostnames — not
the clock, not the embedded CA bundle (which validates roller's Let's
Encrypt / ISRG chain), and not SELinux (it failed as root too).

Rebuilding the Vere fork from current source with the pinned toolchain (zig
0.15.2, `-Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast -Dandroid=true`)
produces a binary that completes dawn over real HTTPS on device. `dawn.c` also
now logs the numeric CURLcode to make any future transport failure diagnosable.

## 2. The apparent "%kids install loop" was the controller crashing the ship

A fresh moon appeared to loop forever on `kiln: install %base from
~hobdem/%kids`, with the docket agent crashing spider (`%arvo-response`) every
few seconds. The cause was the controller: `HostedAppsPoller` runs a khan-eval
that scries `/gx/docket/charges/noun` on every poll, and on a fresh moon
`%docket` is not yet running while `%base` installs. Scrying a not-running
agent bails and crashes the strand; the repeated crashes destabilized kiln and
stalled the initial install. Booting the same pier with the controller not
polling it produced zero crashes, and the ship went straight to stable and
network-live.

`HostedAppsPoller` now guards each scry with a `%gu` agent-liveness check
(`(scry ? /gu/docket)`, `(scry ? /gu/nativeplanet-mobile)`) and skips the scry
until the agent is live.

A related provisioning fix landed alongside: the controller created the `keys/`
and `ships/` directories at mode 0700 under its umask, which locked the
shell-group runtime out of a truly fresh `/data/nativeplanet`; those
directories are now created group-accessible.

## Results

- The moon runs stably under active controller polling. The kernel log shows no
  `arvo-response` / spider crashes, where the same window previously produced
  roughly one crash every fifty seconds.
- The hosted-apps poll completes cleanly while docket is still coming up,
  without crashing the ship.
- After a full reboot with no manual step, the moon auto-starts to `running`
  with the correct derived ship name and no error.
- Fresh dawn against real `https://roller.urbit.org` completes with no proxy:
  galaxy table, sponsor keys, and a created pier.

## Follow-ups

- Provider permission hardening (`READ_STATUS` to `signature|privileged`) is the
  last MVP hardening step.
- Dawn depends on `roller.urbit.org` for the Azimuth snapshot, which is upstream
  Vere behavior; the `-e` flag can point it at any `getPoint`-compatible
  endpoint if that ever changes.
