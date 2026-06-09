# NativePlanet Mobile Onboarding

Status: moon-first MVP flow.

The first-run product path is for an existing Urbit user with a hosted parent
planet. The phone asks for the parent hosting URL and `+code`, then uses Artemis
on the parent to create a `%mobile` moon for the phone.

Tlon signup and new-user identity creation are deferred.

## Primary Flow: Pair With Planet

1. User flashes or receives a NativePlanet Mobile phone.
2. User completes enough Android setup to join WiFi.
3. Launcher opens to onboarding.
4. User enters:
   - parent ship hosting URL
   - parent `+code`
5. Controller authenticates to the parent ship through Eyre.
6. Controller verifies Artemis is installed.
7. Controller asks Artemis for a `%mobile` moon.
8. Controller provisions the returned moon boot package locally.
9. Vere starts under Android init.
10. Launcher lands on truthful runtime status for the running moon.

## Fallback Flow: Manual Moon Key

Manual import remains available for developers and recovery:

1. User creates a moon from a parent ship manually.
2. User enters:
   - moon ship name
   - parent ship name
   - current moon boot key
3. Controller validates the key format.
4. Controller writes a local boot package and key file.
5. Controller starts the runtime.

This path is advanced and should not be the main user story.

## Screens

### Welcome

Primary action: **Pair with planet**

Secondary action: **Use moon key**

Deferred action: **Start as comet**

### Pair With Planet

Fields:

- Hosting URL
- `+code`

Behavior:

- The `+code` field is secret.
- The `+code` is cleared from UI state after submit.
- Errors come from the controller.
- If Artemis is missing, the UI explains that the parent needs Artemis.
- If Artemis exists but mobile provisioning is not implemented in this build,
  the UI should say so plainly and offer manual moon-key import.

### Manual Moon Key

Fields:

- Moon name
- Parent name
- Moon boot key

Behavior:

- The key is never displayed after submit.
- Validation errors are shown without echoing key material.
- Success navigates to the reveal/runtime flow.

### Runtime Ready

The launcher shows:

- ship name
- parent
- runtime state
- network state
- boot package validity
- conn.sock availability

Runtime stop/restart controls stay hidden until graceful lifecycle management is
safe enough for normal users.

## Error Handling

| Error | User Meaning |
| --- | --- |
| `INVALID_HOST_URL` | The hosting URL is missing or not HTTPS |
| `MISSING_ACCESS_CODE` | The `+code` field is empty |
| `PARENT_AUTH_FAILED` | The hosting URL or `+code` did not log in |
| `PARENT_NETWORK_FAILED` | The phone could not reach the parent |
| `PARENT_SERVICE_UNAVAILABLE` | Login worked, but Artemis is not installed |
| `PARENT_PROTOCOL_UNSUPPORTED` | Artemis exists, but this phone build cannot request a mobile moon yet |

## Deferred

- Tlon signup from the first-run flow
- Comet-first onboarding
- QR boot packages
- Multi-ship switching
- Planet key import
