# NativePlanet Controller Provider API

**Authority:** `content://io.nativeplanet.controller`

**Permission:** `io.nativeplanet.permission.READ_STATUS`
- DEBUG ONLY: Currently `protectionLevel="normal"`
- TODO production: Change to `signature|privileged`

## Access Method

Use `ContentResolver.call()`:

```kotlin
val result: Bundle? = contentResolver.call(
    Uri.parse("content://io.nativeplanet.controller"),
    method,   // "getStatus", "getNetwork", "getRuntime", "getBootPackage", "getDiagnostics"
    null,     // arg (unused)
    null      // extras (unused)
)
val json: String? = result?.getString("json")
```

**Method names:**
| Method | Returns |
|--------|---------|
| `getStatus` | Combined status |
| `getNetwork` | Network state |
| `getRuntime` | Runtime status |
| `getBootPackage` | Boot package status |
| `getHostedApps` | Hosted Urbit app inventory |
| `getHostedAppIcon` | Cached Docket tile image bytes |
| `getDiagnostics` | Diagnostics summary |

**Bundle keys returned:**
- `"json"` → JSON string response
- `"bytes"` → raw bytes (only `getHostedAppIcon`; absent when no icon cached)

---

## Endpoint: method `getHostedAppIcon`

Returns the raw tile image bytes for a hosted app, cached by
`HostedAppsPoller` from the app's Docket/mobile `imageUrl` (TLS or local
Eyre sources only, 512 KB cap). Pass the app `id` as the `arg` parameter.
The id must match `[a-z0-9-]{1,64}`; anything else returns an empty
Bundle. Image format is whatever the ship serves — callers must handle
decode failure (e.g. SVG) and fall back to a generated glyph.

Each entry in `getHostedApps` carries `"iconCached": true|false` so
clients can skip the call when no image exists.

---

## Response Shapes

Every `get*` method returns the section object unwrapped (`getNetwork` returns the
`network` object itself). The shapes, the control-response shape, and the
`pairWithPlanet` / `provisionMoon` request and response JSON are defined once in
[docs/controller/controller-api-contract.md](../../../../docs/controller/controller-api-contract.md).

---

## Error Handling

If the provider returns malformed JSON or throws, clients should:
1. Catch exception
2. Fall back to stub/demo data
3. Show "Controller unavailable" UI state

The launcher's `ProviderNativePlanetClient` handles all failure modes gracefully.
