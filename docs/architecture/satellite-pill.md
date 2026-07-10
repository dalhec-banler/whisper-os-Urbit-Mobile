# Satellite Pill Architecture

## Overview

A Satellite Pill is a minimal urbit boot image designed for mobile devices that:
- Boots quickly with minimal resources
- Connects to a parent planet for state sync
- Handles intermittent connectivity gracefully

## Installation Path

```
/system_ext/etc/nativeplanet/satellite.pill
```

## Pill Versions

### Satellite Pill v0 (Current)

For v0, the Satellite Pill is an alias for the known-good brass pill:

```bash
cp urbit-v4.3.pill satellite.pill
```

This validates the runtime infrastructure before building custom pills.

### Satellite Pill v1 (Device Verified)

Built from `+brass` with:
- `%base` desk (core urbit)
- `%nativeplanet-mobile` desk (mobile-specific agents)

A fresh moon provisioned from Satellite Pill v1 boots to a stable,
network-live ship on device. See
[../verification/fresh-moon-boot.md](../verification/fresh-moon-boot.md) for
the verification record.

### Satellite Pill v2+ (Future)

Full mobile satellite with:
- `%base`
- `%nativeplanet-mobile`
- `%satellite-sync`
- `%parent-delegation`
- `%lick-bridge`
- `%launcher-api`

## Building with brass.hoon

The Satellite Pill is built using `+brass` from `pkg/arvo/gen/pill/brass.hoon`.

```hoon
:: Generate pill from desk list
:: First desk becomes base, others installed via Kiln
.satellite +pill/brass %base %nativeplanet-mobile
```

Key facts about `+brass`:
- Builds a pill from a list of desks
- First desk becomes the pill's base desk
- Remaining desks are installed through Kiln
- Default is `%base` only
- The Dojo dot sink writes the generated pill jamfile to
  `<pier>/.urb/put/.satellite`

See `satellite-pill/README.md` for build instructions.

## Design Goals

1. **Fast cold boot** - Under 30 seconds on mobile hardware
2. **Small footprint** - Minimal desk set for mobile use cases
3. **Parent sync** - State backed up to parent planet
4. **Offline capable** - Core functionality works without network
5. **Generic** - No user secrets, identity provisioned at runtime

## Pill Contents

### Minimal Desk Set

```
%base        - Core urbit (required)
%landscape   - Basic UI framework
%groups      - Messaging (optional, sync from parent)
%talk        - Encrypted chat
```

### Excluded from Satellite

```
%bitcoin     - Not needed on mobile
%webterm     - Terminal via parent
%studio      - Development tools
```

## Parent-Satellite Relationship

```
Parent Planet (desktop/server)
    │
    ├── Full urbit with all desks
    ├── Persistent storage
    ├── Always-on connectivity
    │
    └── Satellites[]
            │
            └── Mobile Satellite
                ├── Minimal desk set
                ├── Syncs messages/state
                └── Offline queue
```

## Boot Sequence

1. **First Boot**
   - Load satellite pill from `/system_ext/etc/nativeplanet/satellite.pill`
   - Read BootPackage from `/data/nativeplanet/boot-package.json`
   - Create pier at specified `pierPath`
   - Generate fake identity (v0) or import moon identity (v1+)

2. **Subsequent Boots**
   - Detect existing pier (`.urb` directory)
   - Boot from local pier without `-c`
   - Connect to parent for delta sync (v1+)
   - Process offline queue (v1+)

## Security Constraints

The Satellite Pill:
- Must NOT contain user secrets
- Must NOT contain moon keys
- Must NOT contain +codes
- Must NOT contain real BootPackages
- Must NOT contain parent-specific identity material

The pill is generic. Identity is provisioned through the BootPackage at runtime.

## Current Status

v0 validated the runtime and BootPackage path with the known-good brass pill.
v1 is now built with `%nativeplanet-mobile`, shipped in the ROM prebuilt path,
and boot-verified on device: a fresh moon from Satellite Pill v1 comes up
network-live and auto-starts across reboots. See
[../verification/fresh-moon-boot.md](../verification/fresh-moon-boot.md).

v2+ adds parent sync, delegation, Lick bridge, and launcher API desks.

## Next Steps

See:
- [Parent-Satellite Protocol](parent-satellite-protocol.md) for sync details
- [BootPackage](bootpackage.md) for runtime configuration
- `satellite-pill/README.md` for build instructions
