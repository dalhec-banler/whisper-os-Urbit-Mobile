# Whisper OS Overview

## Vision

Whisper OS is a mobile-first sovereign computing platform that puts users in complete control of their digital identity, data, and communications.

Built on urbit running natively on Android, Whisper OS provides:
- **True ownership** - Your data lives on your device, not in the cloud
- **Encrypted by default** - End-to-end encryption for all communications
- **Interoperable** - Connect with the broader urbit network
- **Open source** - Fully auditable, no black boxes

## Target Users

### Phase 1: Technical Early Adopters
- Existing urbit users wanting mobile access
- Privacy-conscious developers
- Self-sovereignty advocates

### Phase 2: Privacy-Conscious Mainstream
- Journalists and activists
- Business users needing secure comms
- Anyone tired of surveillance capitalism

## Core Features

### Messaging
- Encrypted peer-to-peer chat
- Group conversations
- Offline message queuing
- Sync with desktop urbit

### Identity
- Self-sovereign identity (@p)
- No phone number required
- Portable across devices
- Optional satellite/moon mode

### Data
- Local-first storage
- Optional parent planet backup
- No cloud dependency
- Full export capability

## Technical Foundation

```
┌─────────────────────────────────────┐
│         Whisper Launcher            │
│   (Kotlin native Android app)       │
├─────────────────────────────────────┤
│         Landscape UI                │
│   (WebView + native bridges)        │
├─────────────────────────────────────┤
│         Urbit Runtime               │
│   (vere on Android ARM64)           │
├─────────────────────────────────────┤
│         GrapheneOS                  │
│   (hardened Android base)           │
├─────────────────────────────────────┤
│         Pixel Hardware              │
│   (Titan M2 security chip)          │
└─────────────────────────────────────┘
```

## Development Phases

### Phase 1: Runtime Foundation ✅
- Vere running on Android ARM64
- Init service lifecycle
- Basic pier management

### Phase 2: Satellite System (Current)
- Satellite pill for fast mobile boot
- Parent-satellite sync protocol
- BootPackage provisioning

### Phase 3: Launcher App
- Native Android launcher
- WebView for Landscape UI
- Lick bridge for native features

### Phase 4: Polish & Distribution
- UX refinement
- Beta program
- ROM distribution

## Why GrapheneOS?

- Hardened Android with security focus
- Verified boot chain
- No Google services dependency
- Active security maintenance
- Pixel hardware support (Titan M2)

## Differentiation

| Feature | Whisper OS | Signal | WhatsApp |
|---------|------------|--------|----------|
| Self-hosted | ✅ | ❌ | ❌ |
| No phone number | ✅ | ❌ | ❌ |
| Open source | ✅ | Partial | ❌ |
| Decentralized | ✅ | ❌ | ❌ |
| Offline capable | ✅ | Limited | ❌ |
| Data portability | ✅ | Limited | ❌ |

## Getting Involved

See [CONTRIBUTING.md](../../../CONTRIBUTING.md) for how to participate.

## License

MIT License - See [LICENSE](../../../LICENSE).
