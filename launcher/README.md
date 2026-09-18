# Planet Link

Superseded by [`home/`](../home/README.md), which holds HOME. This Compose app is retained for the Artemis pairing and onboarding screens; it is not HOME.

Current status:

- Kotlin/Compose debug scaffold exists.
- Runtime Console reads `NativePlanetStatusProvider`.
- Provider fallback/demo mode exists for development.
- Onboarding screens are scaffolded.
- Real provisioning/start/stop should go through controller provider calls, not direct pier access.
- The on-device ship @p name is derived by the controller from the runtime-reported ship id.

Build locally:

```bash
cd launcher
./gradlew assembleDebug
```

Output APKs are build artifacts and must not be committed.
