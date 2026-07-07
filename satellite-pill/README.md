# Satellite Pill

The Satellite Pill is the custom Urbit pill for Whisper OS mobile devices.

## Overview

The Satellite Pill is built using `+brass` from `pkg/arvo/gen/pill/brass.hoon`. It contains the base Urbit system plus NativePlanet mobile-specific desks.

## Pill Versions

### Satellite Pill v0 (Legacy Baseline)

- Base: `%base` desk only
- Purpose: Runtime validation, BootPackage testing
- Source: Known-good brass pill renamed to `satellite.pill`

### Satellite Pill v1 (Current)

- Desks: `%base`, `%nativeplanet-mobile`
- Purpose: Mobile-specific agents and marks
- Status: builds with Brass, boots on host, and boots on Android Vere.
  `%nativeplanet-mobile` installs as an essential desk, reports
  `app status: running`, and serves `/apps/json`.
- Current artifact hash:
  `0e6e61a0362180c3954ac8dd8607c132dcbf2a92fc63a6113ac0007163fd204e`.

The generated pill artifact is intentionally ignored by git. Build it locally
and copy it into the ROM prebuilt path when preparing a ROM build. Signed ROM
`2026062201` includes this v1 artifact and has been flashed and verified on the
test phone.

### Satellite Pill v2+ (Future)

- Desks: `%base`, `%nativeplanet-mobile`, `%satellite-sync`, `%parent-delegation`, `%lick-bridge`, `%launcher-api`
- Purpose: Full mobile satellite functionality

## Building

### v0 (Alias)

For v0, use the known-good brass pill:

```bash
cp /path/to/urbit-v4.3.pill satellite.pill
```

### v1+ (Custom Build)

```hoon
:: From a disposable fake ship
|merge %nativeplanet-mobile our %base
|mount %nativeplanet-mobile
```

Overlay the source files into the mounted desk:

```bash
cp satellite-pill/desks/nativeplanet-mobile/desk.bill <pier>/nativeplanet-mobile/desk.bill
cp satellite-pill/desks/nativeplanet-mobile/sys.kelvin <pier>/nativeplanet-mobile/sys.kelvin
cp satellite-pill/desks/nativeplanet-mobile/app/nativeplanet-mobile.hoon <pier>/nativeplanet-mobile/app/nativeplanet-mobile.hoon
```

Commit, install, and export the pill:

```hoon
|commit %nativeplanet-mobile
|install our %nativeplanet-mobile
.satellite +pill/brass %base %nativeplanet-mobile
```

Copy the generated jamfile:

```bash
./satellite-pill/build-satellite-pill.sh v1-copy <pier>
```

Or use the build script:

```bash
./build-satellite-pill.sh v1
```

The Dojo dot sink writes the pill jamfile to
`<pier>/.urb/put/.satellite`; the helper copies it to
`satellite-pill/out/satellite.pill`.

Before attempting a pill build, run the desk smoke check:

```bash
./tools/smoke-satellite-pill.sh
```

## brass.hoon Reference

From `pkg/arvo/gen/pill/brass.hoon`:

- `+brass` builds a pill from a list of desks
- The first desk becomes the pill's base desk
- Remaining desks are installed through Kiln
- Default is `%base` only

## Installation Path

```
/system_ext/etc/nativeplanet/satellite.pill
```

## Security Constraints

The Satellite Pill:

- Must NOT contain user secrets
- Must NOT contain moon keys
- Must NOT contain +codes
- Must NOT contain real BootPackages
- Must NOT contain parent-specific identity material

The pill is generic. Identity is provisioned through the BootPackage at runtime.

## Directory Structure

```
satellite-pill/
  README.md           # This file
  build-satellite-pill.sh  # Build script
  desks/              # Desk source
    nativeplanet-mobile/
    satellite-sync/
    parent-delegation/
    lick-bridge/
    launcher-api/
  out/                # Build output (gitignored)
```
