# Satellite Pill

The Satellite Pill is the custom Urbit pill for Whisper OS mobile devices.

## Overview

The Satellite Pill is built using `+brass` from `pkg/arvo/gen/pill/brass.hoon`. It contains the base Urbit system plus NativePlanet mobile-specific desks.

## Pill Versions

### Satellite Pill v0 (Current)

- Base: `%base` desk only
- Purpose: Runtime validation, BootPackage testing
- Source: Known-good brass pill renamed to `satellite.pill`

### Satellite Pill v1 (Planned)

- Desks: `%base`, `%nativeplanet-mobile`
- Purpose: Mobile-specific agents and marks
- Status: `%nativeplanet-mobile` source draft exists under
  `desks/nativeplanet-mobile/`; the Gall app passes a host `urbit eval`
  parse/type smoke, but it has not yet been pill-built.

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

```bash
# From an Urbit ship with the required desks installed
|merge %base our %base
|merge %nativeplanet-mobile our %nativeplanet-mobile

# Generate pill
.brass/pill +brass %base %nativeplanet-mobile
```

Or use the build script:

```bash
./build-satellite-pill.sh v1
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
