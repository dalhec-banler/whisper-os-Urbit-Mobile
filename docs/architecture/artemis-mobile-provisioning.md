# Artemis Mobile Provisioning

Status: current parent-assisted provisioning direction.

Artemis should be the parent-side moon authority for NativePlanet Mobile. It
already creates moons, labels them by role, stores the current boot key, and
has a `%mobile` role that maps directly to phone moons.

## Product Flow

1. User enters their parent ship hosting URL and `+code` on the phone.
2. The controller authenticates to the parent ship through Eyre.
3. The controller verifies Artemis is installed on the parent.
4. The controller asks Artemis to create a `%mobile` moon.
5. Artemis returns the moon name, parent ship, and current boot key.
6. The controller provisions that boot package locally and starts Vere.
7. The launcher shows the running phone moon using provider/runtime data.

Manual moon-key import remains the advanced fallback.

## Artemis Capabilities Already Present

Artemis currently provides:

- `[%make-moon nam=@t rol=role]`
- role values including `%mobile`
- a `/moons` subscription fact with each moon's `who`, `nam`, `rol`, and `sed`
- JSON conversion for `sed` as `%uw`, which is the boot key format Android
  provisioning already accepts

The existing Artemis frontend uses the Urbit HTTP channel API to poke
`%artemis-action` and subscribe to `/moons`.

## Recommended Protocol

For MVP, keep this in Artemis rather than creating a separate parent app.

There are two viable integration paths:

1. **Controller speaks the Urbit channel API.**
   - No Artemis changes required.
   - Android sends a poke to `app=artemis`, `mark=artemis-action`.
   - Android subscribes to `/moons` and selects the newly created `%mobile`
     moon.
   - More Java code, but it uses the same public API as the Artemis UI.

2. **Artemis adds a small mobile HTTP endpoint.**
   - Simpler Android code.
   - Endpoint can create a `%mobile` moon and return exactly the boot package
     fields the phone needs.
   - Requires a small Gall HTTP mutation path in Artemis.

Preferred direction: add the smallest clean Artemis mobile endpoint if it can
be implemented without distorting Artemis. Otherwise, implement the channel
client in the controller.

## Security Rules

- The `+code` is never logged, returned, screenshotted, or written to disk.
- The boot key is only written to the controller-owned key file.
- Artemis should avoid logging newly created moon boot keys in production.
- The phone should request `%mobile` moons by default.
- Parent pairing must not pretend success until the phone has a valid local
  boot package and runtime start has been requested.

## Controller States

Current controller behavior:

- Validates HTTPS hosting URL and non-empty `+code`.
- Authenticates to Eyre.
- Confirms the session with a known scry.
- Checks `/apps/artemis/`.
- Returns `PARENT_PROTOCOL_UNSUPPORTED` when Artemis exists but the mobile moon
  request is not implemented yet.

Next controller behavior:

- Request/create a `%mobile` moon from Artemis.
- Parse the returned moon identity and boot key.
- Call the existing local `provisionMoon` path.
- Return `PROVISIONED_START_REQUESTED` only after local provisioning succeeds.
