# PatpFormatter @p scramble — vector verification

**Date:** 2026-07-08
**Component:** `rom/vendor/nativeplanet/controller/.../PatpFormatter.java`
**Result:** PASS — 3,229 / 3,229 vectors match urbit-ob

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

- The initial code-read suspicion that the round-modulus parity and final
  recombination were inverted was **wrong** — the C jet (not hoon.hoon
  read from memory) is the correct reference, and the Java port matches
  it exactly.
- `RuntimeStatusPoller.formatShipName()` now treats the conn.sock-derived
  name as authoritative and logs a loud warning if it disagrees with
  `boot-package.json` (a mismatch means vere is running a different ship
  than provisioned).
- Remaining on-device step: flash/install the rebuilt
  `NativePlanetController.apk` (built 2026-07-07, includes this change)
  and confirm `runtime-status.json` reports
  `~pacbyr-balteb-palrum-roclur` derived from the live `%who` peel.

## Reproduce

```
npm install urbit-ob
# copy PatpFormatter.java, make class/format() public, compile with Main
# driver reading decimal ship numbers on stdin
node gen.js && java Main < vectors.txt > actual.txt
diff <(sort -u expected.txt) <(sort -u actual.txt)
```
