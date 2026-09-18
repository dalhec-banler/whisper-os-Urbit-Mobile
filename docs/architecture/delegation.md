# Delegation: one identity, two ships

Status: design, 2026-09-15. Supersedes the sketch in
`parent-satellite-protocol.md` for the parts it covers. Grounded in the current
Urbit kernel and the Tlon desks as of September 2026 (sources at the end).

## The constraint everything follows from

Since urbit/urbit PR #6047 (January 2023) `++team:title` returns true only for
the ship itself. Moons are ordinary foreign ships to every Gall agent. Tlon's
`%chat`, `%groups` and `%activity` gate every client-facing poke and
subscription on `from-self` (`=(our src):bowl`), and none of them mention moons
at all. A moon that pokes the parent's `%chat` gets a nack; a moon that watches
`/v4` on the parent's `%activity` is refused.

So the phone cannot act as the planet by talking to the planet's apps. It can
only act as the planet by asking a relay on the planet, which re-issues the
action locally so that `src.bowl` is the planet. That relay is the whole
feature.

## What we build

Two agents. The parent-side one lives in the Artemis desk, because Artemis
already knows which moons exist and which are `%mobile`. The moon-side one is
the `%nativeplanet-mobile` agent that the satellite pill already ships.

### `%satellite` on the parent (Artemis desk)

Auth on every poke and watch:

```hoon
?>  ?&  (moon:title our.bowl src.bowl)
        =(%mobile rol:(~(got by mons:artemis) src.bowl))
    ==
```

`moon:title` is pure arithmetic (`&(=(%earl (clan who)) =(her (^sein who)))`);
the role check reuses Artemis state, so the allowlist is "moons Artemis minted
as phones" and nothing else.

Outbound, parent to moon. On init the relay subscribes locally, as the planet,
to the paths Tlon reserves for self:

- `[%pass /chat %agent [our %chat] %watch /v4]` for DM and club writs
- `[%pass /activity %agent [our %activity] %watch /v6]` and `/v6/unreads`
- `[%pass /groups %agent [our %groups] %watch /v3/groups]`

and re-gives each fact on moon-facing paths `/moon/chat`, `/moon/activity`,
`/moon/groups`. When a moon watches, the first fact is a snapshot from local
scries (`/x/v4/dm`, `/x/v4/dm/<ship>/writs/newest/50`, `/x/v3/groups`,
`/x/v6/activity/unreads`). On reconnect the moon sends its last-seen time and
the relay answers from the `/changes/<since>` scries all three agents expose for
exactly this.

Inbound, moon to parent. The moon pokes the relay; the relay re-pokes the local
app so the action is the planet's:

- send a DM: `satellite-dm+[ship action:dm:v7]` becomes
  `[%pass /fwd/dm %agent [our %chat] %poke chat-dm-action-2+...]` with the
  author overwritten to `our.bowl`. `%chat` runs its normal `di-proxy` and the
  peer receives the message from the planet. One identity.
- accept or decline a DM: `chat-dm-rsvp`.
- read state: `activity-action` `%read`, so unreads converge on both ships.
- join, knock, leave a group: `group-foreign-2` and `group-action-5`.

Nacks from the local pokes go back to the moon on `/moon/acks` so the phone can
say a send failed.

### `%nativeplanet-mobile` on the moon

Gains a mirror. It learns its parent from the boot package (the controller
pokes it once with `%pair parent`), watches the three relay paths, keeps DMs,
groups and unreads in state, resubscribes with its last-seen time on every
kick, and exposes the mirror to the phone over local Eyre:

- `/x/dms`, `/x/dm/<ship>/writs/newest/<n>`, `/x/groups`, `/x/unreads`
- pokes from the phone: `%send-dm`, `%read`, `%join-group`

Outbound pokes queue while the parent is unreachable. One static wire per
purpose, because Ames orders messages only within a flow; Tlon's own
`/proxy/diff` rule.

### Groups

Two honest options, and the recommendation is to do both, in order.

1. **Now: the moon joins the same groups as itself.** The relay tells the moon
   which groups the planet is in; the moon's own `%groups` joins each one
   (open groups directly, private ones after the planet invites its moon, which
   only works where the planet is an admin, otherwise the moon knocks). Tlon's
   app on the phone then shows the same groups. Posts written on the phone are
   authored by the moon. That is the "match the parent's groups" option at
   setup, and it works with no new UI.
2. **Later: mirror groups through the relay** like DMs, so channel posts from
   the phone are authored by the planet too. That requires the phone to have its
   own channel UI and at that point Tlon's desk leaves the moon entirely, which
   is also the end state the design proposal wants.

DMs cannot take option 1: a DM to the planet never reaches the moon, so DMs
go through the relay from day one, and the phone gets a conversation view in
Whisper Home (the Person page grows a compose line).

## The phone side

Whisper Home's Ship layer reads the mirror instead of `%chat` directly, which is
a path change, not a redesign. Reach, Next, Later, and the Person page keep their
contracts. The Person page gains a compose line that pokes `%send-dm`.

## Setup flow

Pairing already talks to Artemis on the parent. It gains two steps: the
controller pokes the moon's `%nativeplanet-mobile` with the parent name, and
offers "Join the same groups as your planet" with the list from the relay,
default on.

## Development harness

A fake `~zod` on the Mac as the parent, a fake moon of it as the phone, both
with the Tlon desks (copied from the comet's mounted `%groups` desk), Artemis
plus `%satellite` on `~zod`, the mobile desk on the moon. Fake ships DM each
other fine. The emulator's Whisper Home points at the moon. Nothing touches a
real identity until the relay is proven end to end.

## Deploying to a real parent

Done once on 2026-09-18 for the test phone; this is the path.

1. The relay travels by Clay. Copy `artemis/desk/app/satellite.hoon` and the
   `desk.bill` that lists `%satellite` into the dev moon's mounted `artemis`
   desk and `|commit %artemis` there. The dev moon pier only runs on the 4.3
   `tools/urbit` binary in daemon mode (`-t`); the 64-bit runtime refuses its
   stale snapshot.
2. On the parent: `|install <dev-moon> %artemis`. The parent's web terminal
   types but shows nothing, so verify with
   `GET /~/scry/satellite/moons.json` (404 before, 200 after).
3. On the phone: `tools/install-mobile-metadata-desk.sh` puts the mirror
   agent on the moon; confirm with a scry of
   `/gx/nativeplanet-mobile/mirror/json`. Pair by poking
   `%nativeplanet-mobile` with `%noun [%pair ~parent]` over conn.sock. The
   parent's `moons.json` then lists the moon, and the moon's mirror carries
   the parent's DMs. Artemis must already hold the moon with role `%mobile`
   (`/~/scry/artemis/mons.json`).
4. `conn-client eval` prints a long cord as one big integer: decode it as
   little-endian bytes, then UTF-8 (Python needs
   `sys.set_int_max_str_digits(0)` once the snapshot carries groups).
5. Groups, phase A: the snapshot's `groups` is the planet's
   `groups/light`. To join one the planet hosts, poke the mirror with
   `{"invite-moon": "<flag>"}`; the relay issues a `group-action-4` invite
   for the moon, and the moon's own `%groups` takes a `group-join`
   `{"flag", "join-all": true}` with the token it now holds. Other groups
   join directly (open) or leave a knock pending (closed).

## Pitfalls to design around

- Mark drift. Compile against the tlon-apps commit installed on the parent;
  `%chat` negotiates protocol versions and skew crashes pokes. Vendor
  `sur/chat.hoon` and `sur/groups.hoon`.
- Author spoofing. The relay always overwrites the memo author; the moon's
  claim is never trusted.
- Kicks. Tlon kicks `/v4` subscribers on migrations; the relay and the mirror
  both resubscribe.
- Snapshot size. Page DM history with `newest/<n>`; never replay the firehose
  on reboot, use `/changes/<since>`.
- Key cycling. `|moon-cycle-keys` or `|moon-breach` on the parent invalidates
  the moon's keys; the allowlist survives, the moon reboots.
- Tlon's open PR #6329 (`%vouch`) adds host-side relay plumbing for bot moons
  that keeps the moon as a distinct author. Not what we want, but retest after
  it lands.

## Sources

- urbit/urbit `sys/zuse.hoon` `++team` / `++moon:title`; PR #6047.
- tloncorp/tlon-apps `desk/app/chat.hoon` (`++from-self`, `++poke`,
  `++di-proxy`, `++di-watch`), `desk/app/groups.hoon`, `desk/app/activity.hoon`.
- docs.urbit.org app-school (pokes, subscriptions), remote scry, Jael and moons,
  dojo tools (`|moon*`, `|sync`).
- urbit/urbit issue #1870, the 2019 statement of this exact problem.
