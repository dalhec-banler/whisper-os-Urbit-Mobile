# Satellite Pill Architecture

## Overview

A Satellite Pill is a minimal urbit boot image designed for mobile devices that:
- Boots quickly with minimal resources
- Connects to a parent planet for state sync
- Handles intermittent connectivity gracefully

## Design Goals

1. **Fast cold boot** - Under 30 seconds on mobile hardware
2. **Small footprint** - Minimal desk set for mobile use cases
3. **Parent sync** - State backed up to parent planet
4. **Offline capable** - Core functionality works without network

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
   - Load satellite pill
   - Generate or import identity
   - Connect to parent for initial sync

2. **Subsequent Boots**
   - Load from local pier
   - Sync delta from parent
   - Process offline queue

## Implementation Status

- [ ] Define minimal desk set
- [ ] Create satellite pill builder
- [ ] Parent sync protocol
- [ ] Offline queue mechanism
- [ ] Identity binding to parent

## Next Steps

See [Parent-Satellite Protocol](parent-satellite-protocol.md) for sync details.
