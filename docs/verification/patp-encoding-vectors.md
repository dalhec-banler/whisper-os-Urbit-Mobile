# @p encoding vector verification

Verified 2026-07-08 against the controller's `PatpFormatter.java`. All 3,229
vectors match urbit-ob.

## What was verified

`PatpFormatter.format(BigInteger)` renders an Urbit ship number as its @p
name. It ports `+fein:ob` / `+feis:ob` / `+fe:ob` following the vere jet
(`pkg/noun/jets/e/fein_ob.c`), including the jet's exact constants
(`a=0xffff`, `b=0x10000`, `k=0xffff0000`, the four `+raku:ob` murmur3
seeds) and the "legendary @max19" recombination branch
(`r == a ? r*a + l : l*a + r`).

The standalone class was compiled with the host JDK and its output diffed
against **urbit-ob 5.x** (`ob.patp`) as an independent reference
implementation.

## Vector set (3,229 total)

- Class edges: `0`, `1`, `255` (galaxies); `256`, `65535` (stars);
  `65536`, `65537`, `0xffffffff` (planet bounds); `2^32`, `2^32+1`,
  `2^64-1` (moon bounds).
- Live ships round-tripped from their names: `~palrum-roclur`,
  `~wacpeg-hodpel-palrum-roclur`, `~pacbyr-balteb-palrum-roclur`.
- Moon-of-star / moon-of-galaxy passthrough branch (`lo < 0x10000`):
  e.g. `2^32 + 256`, `255·2^32`.
- Comets exercising the `--` 64-bit group separator: `2^64`,
  `2^64 + 0x10000`, `2^96 + 42`, `2^127 + 123456789`.
- Deterministic pseudo-random sweep (LCG, fixed seed): 1,500 planets,
  1,500 moons, 200 comets.

All 3,229 outputs are byte-identical to urbit-ob. Zero mismatches.

## Notes

- The C jet (`fein_ob.c`), not a hoon reference, is the authority for the
  scramble; the Java port matches it exactly across the whole vector set.
- `RuntimeStatusPoller.formatShipName()` treats the conn.sock-derived name as
  authoritative and logs a warning if it disagrees with `boot-package.json`, on
  the basis that a mismatch means Vere is running a different ship than the one
  provisioned.
- Confirmed on device: `runtime-status.json` reports the ship name derived from
  the live `%who` peel.

## Reproduce

```
npm install urbit-ob
# copy PatpFormatter.java, make class/format() public, compile with Main
# driver reading decimal ship numbers on stdin
node gen.js && java Main < vectors.txt > actual.txt
diff <(sort -u expected.txt) <(sort -u actual.txt)
```
