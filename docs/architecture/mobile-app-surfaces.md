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

The phone can now discover and open apps installed on the running moon. That is
the correct foundation, but generic hosted web shells are not enough for a daily
phone. Some Urbit apps have better mobile entrypoints:

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
      "preferredLaunchMode": "local_webview",
      "androidPackage": null,
      "pwaManifestPath": null,
      "mobilePath": "/apps/groups/",
      "recommended": true,
      "hidden": false
    }
  ]
}
```

This desk must not expose keys, `+code` values, cookies, or parent credentials.

## Artemis Role

Artemis remains the parent-side authority for creating and managing `%mobile`
moons. Its next responsibility is to help define the policy for mobile moons:

- create `%mobile` moons
- label and track them
- eventually recommend default desks/apps for those moons
- eventually expose parent-approved mobile app policy

Artemis should not become the phone runtime API. The phone runtime API is still
the controller provider backed by the local moon.

## Near-Term Implementation Steps

1. Keep the verified Docket polling path as the baseline.
2. Add controller support for optional `%nativeplanet-mobile` app metadata.
3. Merge mobile metadata into `hosted-apps.json`.
4. Add known companion-app package detection only after package names are
   verified.
5. Validate Tlon, Terminal, Landscape, Grove, and Kin one at a time.
6. Build a real satellite pill that includes `%nativeplanet-mobile` once the
   desk is useful.

## Non-Goals

- Do not require Lens.
- Do not make Artemis a launcher-facing API.
- Do not fake installed app inventory.
- Do not hide Android app store behavior.
- Do not build parent/satellite state sync before the mobile app surface
  contract is stable.
