# Launcher Design Integration

Status: active guide for applying the local design handoff to the real launcher.

The original launcher design handoff remains useful for visual direction, but
parts of it were written before the ROM, controller, and conn.sock integration
were real. Treat the design handoff as visual and interaction input, not as the
runtime contract.

## Current Product Truth

- The launcher reads real provider data from NativePlanet Controller.
- The phone runs a real init-managed moon.
- Lens is deprecated and is not part of launcher health checks.
- Click/conn.sock is the runtime truth path.
- The primary onboarding path is parent hosting URL plus `+code`.
- Manual moon-key import is the advanced fallback.
- QR pairing and stub-controller-only instructions are stale.

## Design Rules To Keep

- Warm dark surface, restrained accents, and calm status presentation.
- Source Serif display type, Inter body type, JetBrains Mono for `@p` and
  runtime metadata.
- Sigils for identity and ship recognition.
- `Pair with planet` as the primary identity action.
- `Use moon key` as secondary recovery/developer path.
- Comet entry as a quiet deferred option.
- No fake success states. If parent pairing is unavailable, say so plainly.

## Design Rules To Defer

- Global SystemUI replacement.
- Custom notification shade outside launcher surfaces.
- Lock screen replacement.
- Native Phone/Messages app surfaces.
- QR boot package pairing.
- AI sheet over arbitrary apps.

## Current Safe UI Work

- Keep primary home focused on identity, runtime, boot validity, and network.
- Keep diagnostics available but avoid raw debug banners on the main surface.
- Route Identity settings `Add identity` into onboarding.
- Show parent-pairing failures in product language, not raw backend codes.
- Keep the manual moon-key path available until Artemis provisioning is proven
  end to end.

## Remaining UI Work Before Daily Carry

- Add a friendlier empty state for no configured ship.
- Add a clearer unavailable state when Artemis is not installed or not updated
  on the parent.
- Add first-run wording that explains parent URL and `+code` without exposing
  secrets in screenshots or logs.
- Add a polished reveal path after successful Artemis pairing.
- Add accessibility checks for text size, contrast, and one-handed use.
