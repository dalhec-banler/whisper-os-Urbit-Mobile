# Grove runs as a hosted app

Verified 2026-07-17 on a Pixel 8 Pro (husky) device. Grove (a file-storage app,
`~palrum-roclur`/`groundwire.dev`) installs from its publisher over Ames and
launches as a hosted app alongside Tlon, Landscape, and Terminal.

## Install path

Grove is distributed ship-to-ship, so it installs the standard way:

```
|install ~palrum-roclur %grove
```

The controller issues the equivalent `%kiln-install` poke over conn.sock. The
moon subscribes and syncs the desk (agents + UI) from `~palrum-roclur` over
Ames. When the publisher is online the desk arrives and reaches `zest: live`.

This path matters for a second reason: Grove's UI bundles are larger than 256KB
(`index.js` is ~267KB), and the device runtime's clay **mount** drops files over
256KB — so hand-committing the UI into a mounted desk does not work. Syncing the
published desk over Ames is not subject to that mount limit, so the full UI
arrives intact. Ship-to-ship distribution is the correct mechanism here.

## Controller fix: launch `site`-served apps

Grove serves its own UI from the desk (docket `site+/apps/grove`) via a
file-serving agent, rather than shipping a downloadable glob like Tlon. The
hosted-app poller only recognized `glob` docket hrefs as launchable; a `site`
href fell through to a non-launchable `browser` entry with no base path, so
Grove showed as inventory-only.

`HostedAppsPoller.parseHref` now handles the `site` type: a site app is
launchable in the local WebView at `/apps/<desk>/` (docket's site convention).

## Verified on `~hadwyn-taslyx-dozzod-hobdem`

- `|install ~palrum-roclur %grove` synced the desk to `zest: live`.
- Grove's UI serves: `GET /apps/grove/` is HTTP 200, `index.js` is HTTP 200
  (~267KB), `index.css` HTTP 200, service worker HTTP 200; title "Grove".
- The controller lists Grove as `local_webview` with base path `/apps/grove/`,
  alongside Tlon, Landscape, and Terminal.
- After a device reboot the moon auto-starts and Grove still serves; the desk
  persists via event replay.

## Note for the grove repo

The `grove-ui` Vite config sets asset filenames to `[name].[hash]`
(dot-separated), which produces names like `index.Diipi0XB.js`. Clay cannot hold
dots inside path segments, so a clay-served (`site+`) build drops every asset.
The published `~palrum-roclur` desk uses clean, hyphen/plain names and works. A
dash-separated config (`[name]-[hash]`, with dotted asset names sanitized) makes
a locally built desk clay-safe as well.
