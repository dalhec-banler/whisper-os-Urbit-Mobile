# BootPackage Specification

## Overview

BootPackage refers to two related concepts:

1. **Runtime BootPackage (v0)**: A JSON config file that controls how `nativeplanet-vere-launch` boots vere
2. **Distribution BootPackage (future)**: A signed archive for distributing moon identities

This document covers both.

---

## Runtime BootPackage v0

The runtime BootPackage is a JSON file at `/data/nativeplanet/boot-package.json` that tells the launcher how to boot vere.

### Location

```
/data/nativeplanet/boot-package.json
```

### Schema

```json
{
  "ship": "zod",
  "parent": null,
  "pillPath": "/system_ext/etc/nativeplanet/satellite.pill",
  "pierPath": "/data/nativeplanet/ships/zod",
  "bootMode": "FAKE_TEST",
  "keyMaterialRef": "none",
  "networkConfig": {},
  "delegationConfig": {
    "canReadNotifications": false,
    "canMirrorMessages": false,
    "canRequestPosts": false,
    "canRequestReplies": false,
    "canAccessFiles": false,
    "revocationEpoch": 0
  },
  "createdAtMs": 0,
  "packageVersion": 1
}
```

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| ship | string | Ship name (e.g., "zod", "~sampel-palnet") |
| pillPath | string | Path to satellite pill |
| pierPath | string | Path where pier will be created/resumed |
| bootMode | string | "FAKE_TEST" or "MOON" |
| keyMaterialRef | string | Reference to key material (must be "none" for FAKE_TEST) |
| packageVersion | int | Must be 1 |

### Optional Fields (v0 ignores)

| Field | Type | Description |
|-------|------|-------------|
| parent | string | Parent planet for moon mode |
| networkConfig | object | Network configuration |
| delegationConfig | object | Permission delegation |
| createdAtMs | int | Creation timestamp |

### Boot Modes

| Mode | Description | Support |
|------|-------------|---------|
| FAKE_TEST | Fake ship for development | Yes |
| MOON | Real satellite moon | Yes (device runtime) |

### Path Constraints

```
pillPath must start with: /system_ext/etc/nativeplanet/
pierPath must start with: /data/nativeplanet/ships/
```

No path traversal (`..`) allowed.

### Security Validations

1. Missing BootPackage: service fails, logs error, exits 1
2. Malformed JSON: service fails, logs error, exits 1
3. Missing required field: service fails, logs specific error
4. Invalid pillPath prefix: service fails
5. Invalid pierPath prefix: service fails
6. Path traversal detected: service fails
7. keyMaterialRef not "none" in FAKE_TEST: service fails
8. keyMaterialRef looks like raw secret: service fails

Both `FAKE_TEST` and `MOON` boot modes are accepted. In `MOON` mode the
launch wrapper boots an existing moon pier, or provisions a new one from the
satellite pill on first boot.

### Launcher Behavior

```
If BootPackage missing:
  log: "BootPackage not found"
  exit 1

If BootPackage invalid:
  log: "BootPackage invalid: <reason>"
  exit 1

If pier exists (.urb directory):
  exec vere -t --no-dock <pierPath>

If pier does not exist:
  validate pill exists
  create parent directories
  exec vere -t -F <ship> --no-dock -B <pillPath> -c <pierPath>
```

### Example BootPackage

See `examples/bootpackage.fake.json` for a working example.

### Test Cases

See `docs/runtime/bootpackage-v0-test-cases.md` for full test coverage.

---

## Distribution BootPackage (Future)

A self-contained archive that enables one-tap urbit provisioning on a mobile device.

### Contents

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

### Manifest Schema

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

### Security Model

#### Key Encryption

Identity keys are encrypted with a user-provided passphrase:
- AES-256-GCM encryption
- Argon2id key derivation
- Passphrase never stored

#### Package Signing

BootPackages are signed by the parent planet:
- Ed25519 signature over manifest
- Verifiable without network access
- Prevents tampering

### Generation Flow

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

### Consumption Flow

```
Mobile Device
    │
    ├── Import bootpackage (QR, file, URL)
    ├── Verify signature
    ├── Prompt for passphrase
    ├── Decrypt keys
    ├── Extract to runtime BootPackage JSON
    ├── Boot from satellite pill
    │
    └── Connect to parent for initial sync
```

### Distribution Methods

1. **QR Code** - Encode URL to hosted package
2. **Direct Transfer** - AirDrop, Bluetooth, USB
3. **Cloud Link** - Secure download URL (time-limited)

---

## Security Considerations

- BootPackages may contain sensitive identity material
- Never commit real BootPackages to git
- Use test/fake identities for development
- Production packages should be single-use
- Runtime BootPackage logs keyMaterialRef as "[redacted]"
