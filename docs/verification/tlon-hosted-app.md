# Tlon runs as a hosted app

Verified 2026-07-15 on a Pixel 8 Pro (husky) device. A provisioned moon serves
the full Tlon Messenger PWA, and the launcher lists it as a hosted app.

## Summary

The satellite pill previously in use was a stripped, minimal base that carried
only system desks. Tlon needs the `%groups` desk plus `%landscape` (which
provides `%docket`), and those were absent, so Tlon could not run at all.

The stock `urbit-v4.3.pill` already contains `%base`, `%groups` (Tlon),
`%landscape` (docket), and `%webterm`, all at a zuse kelvin (409–410) that the
runtime boots. Switching the satellite pill to this fuller base makes a
provisioned moon come up with Tlon already installed and tiled. No custom desk
source or pill rebuild from tloncorp was needed.

## What was verified

On `~hadwyn-taslyx-dozzod-hobdem` (provisioned through the controller against
its parent):

- The moon boots from the pill and reaches a live, running state. Dawn,
  including the Azimuth galaxy-table fetch, completes over HTTPS on device with
  no host tooling.
- The `docket` charges scry returns a live "Tlon" charge for the `groups` desk.
- The Tlon docket glob (a `glob-http` reference) downloads over HTTPS, and the
  moon's Eyre serves the app: `GET /apps/groups/` returns the Tlon PWA shell
  (HTTP 200), the main JS bundle serves (HTTP 200, ~7.1 MB,
  `application/javascript`), and the web manifest reports `"name": "Tlon"`.
- The moon survives a device reboot: it auto-starts back to running with no
  double-boot or replay abort, and the Tlon PWA still serves afterward.
- The controller's hosted-app inventory lists Tlon (`groups`, title "Tlon")
  alongside Landscape and Terminal, each as a `local_webview` surface, with the
  Tlon tile icon cached.

## Controller fix

The hosted-app poller scried the `docket` and `nativeplanet-mobile` sources
through a `%gu` agent-liveness guard that itself failed as a strand scry,
blocking discovery. It now soft-scries each source with `+mole`, so a source
that is not yet installed returns empty and the poll retries cleanly instead of
crashing the strand. This both fixes discovery and preserves the fresh-boot
safety the guard was meant to provide.

## Notes and follow-ups

- The Tlon-enabled pill is larger (~175 MB vs the stripped ~12 MB) because Tlon
  is a large app; any pill carrying it is comparable in size.
- `%nativeplanet-mobile` (the launcher metadata desk) is not in this stock pill.
  The launcher discovers apps through docket regardless, and the poller degrades
  gracefully when that desk is absent. A follow-up is to bake
  `%nativeplanet-mobile` back into the pill alongside the Tlon desks.
