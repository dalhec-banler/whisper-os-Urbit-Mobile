# 2026-06-10 Artemis Mobile Provisioning Verification

Parent ship: `~palrum-roclur`

## Pass

- Parent hosting URL responds.
- Artemis app responds at `/apps/artemis/`.
- Authenticated hood scry responds.
- Authenticated Artemis `/moons` channel subscription returns moon facts.
- Phone-side provider and launcher smokes passed before testing.
- Artemis created a new `%mobile` moon for the phone:
  `~bitreb-tindys-palrum-roclur`
- Manual recovery path provisioned the Artemis-created moon through the
  controller `provisionMoon` provider method.
- Phone booted the new moon through init-managed Vere.
- Provider runtime reported:
  - `state=running`
  - `shipName=~bitreb-tindys-palrum-roclur`
  - `version=4.3-33293b1`
  - `connSockAvailable=true`
- Boot package provider reported:
  - `bootMode=MOON`
  - `parent=~palrum-roclur`
  - `pierExists=true`
  - `pillExists=true`
  - `keyFileExists=true`
- Controller provider smoke passed.
- Launcher home smoke passed.

## Issue Found

The direct `pairWithPlanet` path successfully authenticated to the parent and
requested a `%mobile` moon, but returned:

```text
PARENT_MOON_CREATE_TIMEOUT
```

Artemis did create the moon. The controller missed the created moon through the
channel wait path.

## Source Fix

Queued controller fix:

- Keep `/moons` channel subscription as the primary path.
- If the created moon is not observed through the channel, perform an
  authenticated `/~/scry/artemis/mons.json` fallback.
- Select a `%mobile` moon not present in the pre-create moon set.
- Continue into the existing local `provisionMoon` path.

The fix builds with `m NativePlanetController -j10`.

## Not Done

- The fix was not live-pushed to the phone because the local module APK and
  flashed system APK signatures differ.
- Full direct `pairWithPlanet` verification waits for the next ROM build.

## Security Notes

- No `+code` or moon key material was committed.
- No raw key material was written to docs.
- A local ignored backup was made before replacing the previous test moon.
