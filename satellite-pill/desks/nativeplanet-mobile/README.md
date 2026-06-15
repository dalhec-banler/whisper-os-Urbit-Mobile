# `%nativeplanet-mobile` Desk

Draft source desk for the mobile app-surface contract.

This desk is intended for Satellite Pill v1. It should be generic: no ship
identity, moon key, `+code`, cookie, parent URL, or user-specific preference
belongs in the pill.

## API

When installed, the desk should expose:

```text
/~/scry/nativeplanet-mobile/apps.json
```

The response is a small launcher metadata document. The controller may merge it
over Docket metadata when present, while preserving Docket as the fallback.

## Status

The Gall app passes a host `urbit eval` parse/type smoke and installs on a
disposable local ship when the desk is initialized from `%base`.

`desk.docket-0` is intentionally omitted for now. The fake-ship base used for
smoke testing does not include a `%docket-0` mark, so Docket metadata belongs in
a later packaging pass with the correct mark layer.

The desk has not yet been built into `satellite.pill`.
