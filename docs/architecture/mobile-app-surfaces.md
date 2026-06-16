# Mobile App Surfaces

Whisper OS should make Urbit apps feel like phone apps while keeping the
running moon as the source of truth.

This document defines the next contract after hosted app launch:

```text
parent planet / Artemis
        ↓
mobile moon desks and Docket metadata
        ↓
controller hosted-app inventory
        ↓
Launcher3 My Urbit Apps and pinned shortcuts
```

## Problem

The phone can now discover apps installed on the running moon. That is the
correct foundation, but app discovery is not the same thing as a launchable
phone surface. A hosted app should only expose an Open action once the moon
provides a route or package that actually renders. Some Urbit apps have better
mobile entrypoints:

- a native Android package
- a PWA or installable web app shell
- a local Vere-hosted app
- a browser fallback

The product needs one place to decide the preferred mobile surface for each app.

## Contract

The controller inventory remains the launcher-facing contract:

```json
{
  "version": 1,
  "source": "docket",
  "timestampMs": 1770000000000,
  "lastPollAttemptMs": 1770000000000,
  "lastError": null,
  "stale": false,
  "apps": [
    {
      "id": "groups",
      "desk": "groups",
      "title": "Tlon",
      "info": "Messaging and communities",
      "tileColor": "#dedede",
      "imageUrl": null,
      "launchMode": "local_webview",
      "basePath": "/apps/groups/",
      "startUrl": "http://127.0.0.1:8080/apps/groups/",
      "androidPackage": null,
      "pwaManifestUrl": null,
      "availability": "glob"
    }
  ]
}
```

`launchMode` is one of:

- `native`
- `pwa`
- `local_webview`
- `browser`

Launcher3 should continue to consume only this normalized inventory. It should
not scry the ship, read `/data/nativeplanet` directly, or know how Artemis
stores its state.

## Source Priority

The controller should merge app metadata in this order:

1. mobile-specific metadata from the running moon, once `%nativeplanet-mobile`
   exists
2. Docket metadata from the running moon
3. known companion-app mappings, such as Tlon's Android package when installed
4. safe fallback to local hosted WebView when a Docket base path exists
5. browser fallback for unsupported entries

Parent-side Artemis should eventually manage the policy for what the mobile
moon should install or prefer, but the phone should still derive the live app
inventory from the running moon.

## First App Targets

### Tlon

Preferred order:

1. native Android app when installed and explicitly selected
2. PWA when a reliable manifest exists
3. local `%groups` hosted WebView

### Terminal / Dojo

Preferred order:

1. local `%webterm` or `%dojo` hosted WebView
2. no native fallback

This is a development and recovery surface, not a consumer default.

### Landscape

Preferred order:

1. local hosted WebView

Landscape remains useful as a fallback launcher for Urbit-side apps.

### Grove

Preferred order:

1. PWA or local app shell
2. local hosted WebView

Grove is the first serious candidate for phone-local file-backed app behavior
because it can store files in the moon's loom.

### Kin

Preferred order:

1. PWA or local app shell
2. local hosted WebView

Kin is a candidate for app installation and app preference flows.

## `%nativeplanet-mobile` Desk

The v1 mobile desk should provide a small read-only API for launcher metadata.

Scry:

```text
/gx/nativeplanet-mobile/apps/json
```

Suggested JSON:

```json
{
  "version": 1,
  "apps": [
    {
      "desk": "groups",
      "preferredLaunchMode": null,
      "androidPackage": null,
      "pwaManifestPath": null,
      "mobilePath": null,
      "recommended": true,
      "hidden": false
    }
  ]
}
```

This desk must not expose keys, `+code` values, cookies, or parent credentials.

The v1 `%nativeplanet-mobile` desk is allowed to report inventory-only entries
with null launch fields. The controller should keep those entries visible so the
OS can show what the moon knows about, but Launcher3 should treat them as
pending and disable pin/open actions. Once a desk serves a verified route, the
metadata can add `preferredLaunchMode` and `mobilePath` for that app.

## Artemis Role

Artemis remains the parent-side authority for creating and managing `%mobile`
moons. Its next responsibility is to help define the policy for mobile moons:

- create `%mobile` moons
- label and track them
- eventually recommend default desks/apps for those moons
- eventually expose parent-approved mobile app policy

Artemis should not become the phone runtime API. The phone runtime API is still
the controller provider backed by the local moon.

## Parent Desk Install Probe

The development tool `tools/install-parent-mobile-desks.sh` can read a parent
ship's Kiln state, resolve the actual publisher for each app desk, and send
Click/Khan `%kiln-install` requests to the connected phone moon.

It is intentionally a dev tool rather than product API:

```bash
NP_PAIRING_URL=https://example.tlon.network ./tools/install-parent-mobile-desks.sh
NP_PAIRING_URL=https://example.tlon.network ./tools/install-parent-mobile-desks.sh --apply
```

The script never prints the parent `+code`. Preview mode reports the publishers
the parent is syncing from. Apply mode asks the phone moon to install those
desks and reports whether they become live or remain held.

The first live test showed the phone moon accepts `%kiln-install` requests over
Click, but the requested desks can remain `held` with hash `0v0`. That means the
next product problem is desk delivery/sync policy, not Launcher3 routing.

## Mobile App Sync Doctor

The development tool `tools/doctor-mobile-app-sync.sh` checks whether the phone
moon has genuinely launchable Urbit app surfaces.

It reports:

- current runtime/provider state
- conn.sock Ames health from `%peel %info`
- phone Kiln pike state
- local `/apps/<desk>/` route availability
- local Docket charge availability
- optional parent comparison when `NP_PAIRING_URL` and `NP_PAIRING_CODE` are
  supplied through an ignored local environment file

The important diagnosis pattern is:

```text
desk is held with hash 0v0
local /apps/<desk>/ returns 404
Ames reports can-send=false or can-scry=false
```

When that pattern appears, the launcher should keep the app visible only as
inventory and disable Open/Pin. The next fix belongs in Urbit networking,
parent/mobile desk policy, or desk delivery, not in the launcher route code.

## Near-Term Implementation Steps

1. Keep the verified Docket polling path as the baseline.
2. Add controller support for optional `%nativeplanet-mobile` app metadata.
3. Merge mobile metadata into `hosted-apps.json`.
4. Add known companion-app package detection only after package names are
   verified.
5. Validate Tlon, Terminal, Landscape, Grove, and Kin launch surfaces one at a
   time before marking them openable.
6. Use `tools/doctor-mobile-app-sync.sh` to distinguish launcher bugs from
   desk delivery or Ames reachability failures.
7. Build a real satellite pill that includes `%nativeplanet-mobile` once the
   desk is useful.

## Non-Goals

- Do not require Lens.
- Do not make Artemis a launcher-facing API.
- Do not fake installed app inventory.
- Do not hide Android app store behavior.
- Do not build parent/satellite state sync before the mobile app surface
  contract is stable.
