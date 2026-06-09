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

The controller speaks the same Urbit channel API used by the Artemis frontend:

1. Subscribe to Artemis `/moons` over `/~/channel/<id>`.
2. Read the first `%artemis-update` fact to capture the existing moon set.
3. PUT a channel poke to `app=artemis`, `mark=artemis-action`.
4. Poke body: `{ "make-moon": { "nam": "...", "rol": "mobile" } }`.
5. Read the next `/moons` fact until a new `%mobile` moon appears.
6. Use that moon's `who` and `sed` fields for local provisioning.

This avoids a separate phone-specific parent app and keeps Artemis' existing
poke API as the product contract.

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
- Request/create a `%mobile` moon from Artemis.
- Parse the returned moon identity and boot key.
- Call the existing local `provisionMoon` path.
- Return `PROVISIONED_START_REQUESTED` only after local provisioning succeeds.
