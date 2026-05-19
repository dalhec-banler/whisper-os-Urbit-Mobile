# BootPackage Specification

## Overview

A BootPackage is a self-contained archive that enables one-tap urbit provisioning on a mobile device. It contains everything needed to boot a new satellite without manual configuration.

## Contents

```
bootpackage-<ship>.zip
├── manifest.json         # Package metadata
├── satellite.pill        # Minimal boot pill
├── identity/
│   ├── ship.txt          # Ship name (@p)
│   ├── life.txt          # Key revision
│   └── keys.jam          # Encrypted key material
├── parent/
│   ├── planet.txt        # Parent planet @p
│   └── endpoint.txt      # Parent sync endpoint
└── config/
    └── defaults.json     # Initial configuration
```

## Manifest Schema

```json
{
  "version": 1,
  "created": "2026-05-19T00:00:00Z",
  "ship": "~sampel-palnet",
  "parent": "~zod",
  "pill_hash": "0x...",
  "expires": "2026-06-19T00:00:00Z"
}
```

## Security Model

### Key Encryption

Identity keys are encrypted with a user-provided passphrase:
- AES-256-GCM encryption
- Argon2id key derivation
- Passphrase never stored

### Package Signing

BootPackages are signed by the parent planet:
- Ed25519 signature over manifest
- Verifiable without network access
- Prevents tampering

## Generation Flow

```
Parent Planet
    │
    ├── spawn moon/comet identity
    ├── generate satellite keys
    ├── create satellite pill
    ├── encrypt keys with user passphrase
    ├── sign manifest
    │
    └── Output: bootpackage-<ship>.zip
```

## Consumption Flow

```
Mobile Device
    │
    ├── Import bootpackage (QR, file, URL)
    ├── Verify signature
    ├── Prompt for passphrase
    ├── Decrypt keys
    ├── Boot from satellite pill
    │
    └── Connect to parent for initial sync
```

## Distribution Methods

1. **QR Code** - Encode URL to hosted package
2. **Direct Transfer** - AirDrop, Bluetooth, USB
3. **Cloud Link** - Secure download URL (time-limited)

## Implementation Status

- [ ] Package format specification
- [ ] Key encryption scheme
- [ ] Signing implementation
- [ ] QR encoding/decoding
- [ ] Launcher import flow

## Security Considerations

- BootPackages contain sensitive identity material
- Never commit real BootPackages to git
- Use test/fake identities for development
- Production packages should be single-use
