# 2026-06-10 Artemis Mobile Provisioning Verification

Parent ship: `~palrum-roclur`

Latest verified ROM package: `2026061002`

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
- The `2026061002` OTA preserved WiFi, `/data/nativeplanet`, and the existing
  runtime state across update.
- The installed controller includes the Artemis scry fallback path and accepts
  current Artemis `%uw` mobile moon seeds.
- Direct `pairWithPlanet` against `~palrum-roclur` succeeded without manual
  file pushing.
- Artemis created and provisioned a fresh `%mobile` moon:
  `~wacpeg-hodpel-palrum-roclur`
- Provider runtime after direct pairing reported:
  - `state=running`
  - `shipName=~wacpeg-hodpel-palrum-roclur`
  - `version=4.3-33293b1`
  - `connSockAvailable=true`
- Boot package provider after direct pairing reported:
  - `bootMode=MOON`
  - `parent=~palrum-roclur`
  - `pierExists=true`
  - `pillExists=true`
  - `keyFileExists=true`
- New moon `conn.sock` exists and controller polling connects repeatedly.
- Provider smoke passed after direct pairing.
- Launcher home smoke passed after direct pairing.
- Plain reboot persistence passed: WiFi came back validated and
  `~wacpeg-hodpel-palrum-roclur` auto-started with `connSockAvailable=true`.
- Provider and launcher smokes passed again after reboot.

## Issues Found And Resolved

The direct `pairWithPlanet` path successfully authenticated to the parent and
requested a `%mobile` moon, but returned:

```text
PARENT_MOON_CREATE_TIMEOUT
```

Artemis did create the moon. The controller missed the created moon through the
channel wait path.

Resolution:

- Keep `/moons` channel subscription as the primary path.
- If the created moon is not observed through the channel, perform an
  authenticated `/~/scry/artemis/mons.json` fallback.
- Select a `%mobile` moon not present in the pre-create moon set.
- Continue into the existing local `provisionMoon` path.

The fallback fix shipped in the verified `2026061002` package.

A second issue appeared after the fallback landed: Artemis emits valid modern
`%uw` moon seeds whose suffix varies, while the phone validator only accepted
the older observed `0w3i5` suffix. The controller now accepts the modern mobile
moon seed tail shape used by Artemis.

## Remaining

- Full launcher-driven pairing still needs a hands-on UI pass. The provider
  path is verified; the product UI path should exercise the same controller
  method.
- Fresh-phone end-to-end testing from a cleared `/data/nativeplanet` state is
  still outstanding.
- Known non-blocking `nativeplanet_vere` `/dev/kmsg_debug` AVC noise remains.

## Security Notes

- No `+code` or moon key material was committed.
- No raw key material was written to docs.
- Local ignored backups were made before replacing previous test moons.
