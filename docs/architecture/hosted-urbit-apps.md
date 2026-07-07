# Hosted Urbit Apps

Whisper OS should treat Urbit apps as first-class phone apps without pretending
they are Android packages.

## Product Shape

The home screen has a first-party **My Urbit Apps** entry near the Android app
store. Opening it shows the apps available on the running ship, using the same
metadata Landscape uses for its tiles when available.

Users can:

- open an Urbit app from the ship-backed app surface
- pin an Urbit app tile to any normal Android home screen
- remove a pinned tile without uninstalling the app from the ship
- distinguish Urbit apps from system, store, and sandboxed Android apps by
  Whisper OS provenance styling

## Launch Model

An Urbit app inventory entry is not necessarily a URL. The launcher should
resolve the best available launch target for each app:

1. **Native Android app** when the app has a preferred installed package.
   Example: Tlon may have both an Urbit web surface and a Google Play app.
2. **PWA / app shell** when the Urbit app provides a manifest and installable
   web-app metadata. Grove and Kin are expected to be important here.
3. **Local hosted WebView** for normal Landscape/Grove/Kin apps running through
   local Vere. This should feel like an app, not like opening a browser URL.
4. **External browser** only as a fallback for debugging or unsupported content.

The product goal is that most Urbit apps feel like phone apps even when they are
served by the local ship. Users should not need to see or manage
`127.0.0.1` URLs during normal use.

## Source of Truth

The running moon is the source of truth.

Primary metadata should come from app distribution data already used by Urbit:

- `/desk/docket-0` for tile title, color, image, base/site, version, and glob
  source
- `%docket` / Landscape state for installed app inventory
- Grove and Kin as candidate installation/sync paths for mobile web apps

The controller should adapt that ship state into a launcher-friendly inventory:

```json
{
  "version": "0.1",
  "ship": "~example-moon",
  "updatedAtMs": 1770000000000,
  "apps": [
    {
      "id": "tlon",
      "title": "Tlon",
      "desk": "groups",
      "basePath": "/apps/groups",
      "tileColor": "#111111",
      "imageUrl": null,
      "source": "docket",
      "launchMode": "native|pwa|local_webview|browser",
      "androidPackage": "network.tlon",
      "startUrl": "http://127.0.0.1:8080/apps/groups",
      "pwaManifestUrl": null,
      "installed": true,
      "pinned": false
    }
  ]
}
```

Controller-owned file:

```text
/data/nativeplanet/hosted-apps.json
```

Launcher3 must not read this file directly. It should ask the controller
provider for the inventory:

```text
content://io.nativeplanet.controller
method: getHostedApps
```

This keeps `/data/nativeplanet` private to the runtime/controller side and
avoids widening SELinux rules for Launcher3. The provider returns `{"apps":[]}`
when no inventory exists yet.

## Current Implementation

Launcher3 has a first-party `My Urbit Apps` surface. It supports these launch
modes:

- `native`: open an installed Android package, for apps like Tlon when the
  native app is preferred.
- `pwa`: open the app in the Whisper hosted WebView shell using PWA metadata.
- `local_webview`: open a local Vere-hosted app in the Whisper hosted WebView
  shell without exposing the URL as normal user chrome.
- `browser`: open an external browser as a fallback only.

The controller treats `%nativeplanet-mobile` as the authority for whether an
app is launchable on mobile. Docket/Glob data can prove that an app exists and
provide tile metadata, but it is not enough by itself to expose an Open action.

For local web launches, the controller probes the requested local Eyre path
before publishing `local_webview`, `pwa`, or `browser` launch metadata. Routes
that return server errors remain visible as inventory-only app entries. This
keeps the launcher honest: users can see that Grove, Kin, Landscape, Terminal,
or Tlon are present without being sent into a broken `500 hosed` screen.

On the current Android runtime, hosted web routes are served by local Eyre on
`127.0.0.1:8080`. The runtime control port is not a valid hosted web origin.
The controller should probe Eyre routes through port `8080` and then publish
only app-like launch metadata, not raw local URLs, to Launcher3.

The hosted WebView shell is private to Launcher3, full-screen, and app-like. It
keeps Android status/navigation gestures available, blocks arbitrary external
navigation, and preserves WebView storage for web-app sessions.

## First Validation Apps

Use apps already expected on the moon as the first real validation targets:

- Tlon
- Landscape
- Terminal
- Grove

Tlon should prefer a native Android package when installed, then fall back to a
PWA/browser route. The local `%groups` WebView path is valid on the current
moon and can be used as the immediate web fallback. Landscape and Terminal are
also valid local WebView routes. Grove is discovered by mobile metadata, and
its PWA/install behavior is the next validation target.

These validate the end-to-end loop before Grove/Kin installation UI is added.
Do not seed these as fake launcher inventory. They should appear only after the
controller can discover real Docket/Landscape/mobile metadata from the running
ship.

## Launcher Responsibilities

Launcher3 should:

- render the My Urbit Apps surface as an OS-level launcher feature, not as the
  old NativePlanet app menu
- read the controller-provided hosted app index through the status provider
- provide pin-to-home behavior using Launcher3's normal workspace model
- resolve app launches through native package, PWA/app-shell, local WebView, or
  browser fallback in that order
- keep hosted launches full-screen and app-like when possible, without exposing
  raw local URLs as the normal UX
- show honest empty/offline states when the ship is stopped or the inventory is
  unavailable

## Controller Responsibilities

The controller should:

- discover installed Urbit apps from the running ship
- normalize Docket/Landscape metadata into `hosted-apps.json`
- expose hosted app inventory through `getHostedApps`
- enrich inventory entries with preferred launch targets when known
- suppress local web launch targets when the route does not probe healthy
- detect installed native Android packages for apps with native companions
- avoid logging secrets, cookies, `+code`, or key material
- expose the inventory through the status provider once stable

## Non-goals For The First Pass

- Do not replace the Android app store.
- Do not fake an app inventory when the ship cannot provide one.
- Do not depend on Lens.
- Do not require comets for validation.
- Do not make the browser address bar the default user experience for hosted
  Urbit apps.

## References

- [Urbit Docket File](https://docs.urbit.org/build-on-urbit/userspace/dist/docket)
- [Urbit Software Distribution Guide](https://docs.urbit.org/build-on-urbit/userspace/dist/software-distribution)
