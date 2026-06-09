# Whisper Launcher

Native Android launcher shell for NativePlanet Mobile.

Current status:

- Kotlin/Compose debug scaffold exists.
- Runtime Console reads `NativePlanetStatusProvider`.
- Provider fallback/demo mode exists for development.
- Onboarding screens are scaffolded.
- Real provisioning/start/stop should go through controller provider calls, not direct pier access.

Build locally:

```bash
cd launcher
./gradlew assembleDebug
```

Output APKs are build artifacts and must not be committed.

Source is periodically synced from the working whisper-launcher checkout.
