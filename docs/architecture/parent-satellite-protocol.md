# Parent-Satellite Protocol

Status: superseded by [delegation.md](delegation.md) (2026-09-15) for message and group sync; kept for the offline-queue and backup ideas. Not implemented. The current verified path is a throwaway dev moon running locally on Android with controller/provider/conn.sock status. Parent sync and Lick integration remain future work.

The Parent-Satellite Protocol defines how a mobile satellite syncs state with its parent planet, handles offline operation, and manages identity.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐
│  Parent Planet  │◄───────►│    Satellite    │
│   (always-on)   │  Ames   │    (mobile)     │
├─────────────────┤         ├─────────────────┤
│ Full state      │         │ Minimal state   │
│ Message history │ ──sync──► Recent messages │
│ All desks       │         │ Core desks only │
│ Backup storage  │◄─backup─│ Local changes   │
└─────────────────┘         └─────────────────┘
```

## Sync Operations

### 1. Initial Sync

On first connection after boot:

```hoon
:: Request state delta since last sync
[%sync-request since=@da]

:: Response with compressed delta
[%sync-response delta=(list event) checkpoint=@da]
```

### 2. Incremental Sync

During active connection:

```hoon
:: Real-time event forwarding
[%event-forward event=* source=@p]

:: Acknowledgment
[%event-ack id=@ud]
```

### 3. Offline Queue

When offline, satellite queues outbound:

```hoon
:: Queue structure
=|  queue=(list [id=@ud event=* timestamp=@da])

:: On reconnect, flush queue
[%queue-flush events=(list *)]
```

## Message Handling

### Inbound Messages

1. Parent receives message for satellite
2. Parent forwards via satellite sync channel
3. Satellite ACKs receipt
4. Parent retains until ACK (offline buffer)

### Outbound Messages

1. Satellite queues message locally
2. On sync, sends via parent
3. Parent forwards to network
4. Satellite removes from queue on ACK

## Identity Model

### Satellite Identity Options

1. **Moon** - Derived from parent, shares reputation
2. **Comet** - Independent, no parent dependency
3. **Planet** - Full independence (advanced)

### Key Management

- Satellite holds own signing keys
- Parent holds backup of encrypted keys
- Key rotation coordinated with parent

## Connection States

```
┌──────────┐
│ Offline  │◄──────────────────────┐
└────┬─────┘                       │
     │ network available           │ network lost
     ▼                             │
┌──────────┐                       │
│Connecting│───────────────────────┤
└────┬─────┘ timeout/error         │
     │ connected                   │
     ▼                             │
┌──────────┐                       │
│ Syncing  │───────────────────────┤
└────┬─────┘ error                 │
     │ sync complete               │
     ▼                             │
┌──────────┐                       │
│  Active  │───────────────────────┘
└──────────┘
```

## Lick Integration

The Lick vane provides native Android bridge for:
- Network state notifications
- Push notification triggers
- Background sync scheduling

See [Lick Android Bridge](lick-android-bridge.md).

## Security Considerations

- All sync traffic over encrypted Ames
- Parent cannot read satellite private data
- Satellite can operate fully offline
- No third-party dependencies for sync
