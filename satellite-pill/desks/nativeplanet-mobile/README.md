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

Source draft only. The Gall app passes a host `urbit eval` parse/type smoke,
but the desk has not yet been installed on a ship or built into
`satellite.pill`.
