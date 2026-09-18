# Delegation on the real chain, 2026-09-18

First run of [delegation](../architecture/delegation.md) against real ships:
the `%satellite` relay on the star `~hobdem`, the mirror agent on the test
phone's moon, and Whisper Home on the phone. Nothing on the star was changed
except the `artemis` desk install; the dev moon carried the relay by Clay.

## Environment

- Device: Pixel 8 Pro (husky), ROM `2026062202`, Whisper Home debug build
  installed over adb and holding HOME
- Ship: `~hadwyn-taslyx-dozzod-hobdem` (moon of `~hobdem`), boot mode MOON,
  state `running`
- Parent: `~hobdem`, Artemis installed, the moon held with role `%mobile`
- Relay source: `artemis/desk/app/satellite.hoon` committed into the dev
  moon's mounted `artemis` desk, then `|install <dev-moon> %artemis` on the star

## Results

| Check | Result |
|---|---|
| Relay reaches the star | PASS: `|install` from the dev moon; `/~/scry/satellite/moons.json` 404 before, 200 after |
| Mirror agent on the moon | PASS: `tools/install-mobile-metadata-desk.sh`; `/gx/nativeplanet-mobile/mirror/json` answers |
| Pairing | PASS: `%noun [%pair ~hobdem]` over conn.sock; `moons.json` lists the moon |
| Mirror snapshot | PASS: carries the star's DM with `~dalhec-banler` |
| Whisper Home identity | PASS: shows "acting as ~hobdem" |
| Send a DM from the Person page | PASS: writ lands in the star's `%chat` authored `~hobdem`; the relay's live fact reaches the mirror; the page shows "now" |
| `refresh` poke | PASS: snapshot re-sent; the mirror asks for one when a fact names an unseen thread |
| Groups phase A, closed group | PASS: `invite-moon` through the relay, then the moon's own `group-join` (Settings, "Groups · same as ~hobdem") |
| Groups phase A, open group | PASS: direct join from the same list |

## Defect: relay reload

Reloading the relay after a `|commit` crashed `on-load` with
`%watch-not-unique`: the relay re-issued its `%chat` `/v4` watch on a wire it
already held. Fixed the same day by watching only when the wire is absent from
`wex.bowl`. Reinstall verified on the star.

## Gotchas

- The dev moon pier runs only on the 4.3 `tools/urbit` binary in daemon mode
  (`-t`); the 64-bit runtime refuses its stale snapshot.
- The star's herm session types but shows nothing. Verify with scries over
  Eyre instead.
- `tools/conn-client.js eval` prints a cord as one big integer; decode it as
  little-endian bytes, then UTF-8. Python needs
  `sys.set_int_max_str_digits(0)` once the snapshot carries groups.

Still open: the setup flow doing the pairing poke and the mirror install
itself, group activity through the relay, and the activity and groups watch
paths listed under Later in the design.
