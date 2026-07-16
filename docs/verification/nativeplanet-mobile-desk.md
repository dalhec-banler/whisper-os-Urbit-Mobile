# nativeplanet-mobile metadata desk

Verified 2026-07-16 on a Pixel 8 Pro (husky) device. The `%nativeplanet-mobile`
desk is installed on a running moon and the controller merges its metadata with
docket discovery.

## Why per-moon install instead of a pill

`%nativeplanet-mobile` serves `/gx/nativeplanet-mobile/apps/json` — static
curation the controller merges into the hosted-app list (recommended flags,
placeholder launch-mode hints). Baking it into the Tlon-carrying pill is not
workable on an offline build ship: the Tlon `%groups` desk's `metagrab` agent
needs network to run, and on a `--fake` (networkless) build ship it crash-loops,
which breaks the pill generator's build of the `%groups` source. Installing the
desk onto an already-running, networked moon sidesteps that entirely.

## Install

`tools/install-mobile-metadata-desk.sh` performs the standard desk install over
the moon's `conn.sock` (Click), against the device's current moon:

1. `%kiln-merge` — fork `%nativeplanet-mobile` from `%base`.
2. `%kiln-mount` — mount the desk to the pier.
3. Overlay the three desk files (`sys.kelvin`, `desk.bill`,
   `app/nativeplanet-mobile.hoon`) from
   `satellite-pill/desks/nativeplanet-mobile`.
4. `%kiln-commit`.
5. `%kiln-install`.

The install commits to the moon's clay, so it persists across reboots via event
replay. It is a runtime operation on a single moon; a freshly provisioned moon
does not have it until the tool is run (or until provisioning is taught to run
this same sequence — a follow-up).

## Verified on `~hadwyn-taslyx-dozzod-hobdem`

- Each step returned success over Click; `%kiln-install` reported `%installed`.
- `/gx/nativeplanet-mobile/apps/json` serves.
- The controller's `hosted-apps.json` moved from `source: docket` to
  `source: docket+nativeplanet-mobile`, `mobileMetadataAvailable: true`, with the
  curation applied: Tlon, Terminal, Grove, and Kin marked recommended; Landscape
  not. Tlon continues to open and render as before.

## Follow-up

Teach provisioning to run the same merge/mount/overlay/commit/install sequence
after a fresh moon reaches `running`, so new moons get the desk automatically.
The mechanism is proven; the remaining work is wiring it into the controller and
testing it against a fresh provision.
