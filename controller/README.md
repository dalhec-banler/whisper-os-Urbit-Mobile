# NativePlanet Controller

Android-side service for managing the Urbit runtime.

## Overview

The NativePlanet Controller provides a clean API for Whisper Launcher to:

- Manage runtime lifecycle (start/stop/restart)
- Prepare and validate BootPackages
- Query runtime status and health
- Access runtime logs

## Architecture

```
┌─────────────────────────────────────────┐
│         Whisper Launcher                │
│           (Kotlin App)                  │
├─────────────────────────────────────────┤
│      INativePlanetController            │
│         (Service Interface)             │
├─────────────────────────────────────────┤
│     NativePlanetControllerImpl          │
│       - setprop control                 │
│       - BootPackage file I/O            │
│       - Log file reading                │
│       - Init service state              │
├─────────────────────────────────────────┤
│        Android System                   │
│     - init (nativeplanet_vere)          │
│     - /data/nativeplanet/               │
└─────────────────────────────────────────┘
```

## API Contract

### INativePlanetController

```kotlin
interface INativePlanetController {
    fun runtimeStatus(): RuntimeStatus
    fun activeShip(): String?
    fun prepareBootPackage(pkg: BootPackage): BootPackageResult
    fun startRuntime(): RuntimeStartResult
    fun stopRuntime(): RuntimeStopResult
    fun restartRuntime(): RuntimeStartResult
    fun logs(maxLines: Int): List<RuntimeLogLine>
    fun health(): RuntimeHealth
}
```

### Models

See `src/main/kotlin/io/nativeplanet/controller/` for full model definitions.

## Implementation Notes

### Runtime Control

The controller uses Android system properties to control the init service:

```kotlin
fun startRuntime(): RuntimeStartResult {
    // setprop nativeplanet.vere.enabled 1
}

fun stopRuntime(): RuntimeStopResult {
    // setprop nativeplanet.vere.enabled 0
}
```

### BootPackage Management

The controller writes BootPackage JSON to:

```
/data/nativeplanet/boot-package.json
```

### Log Access

Reads from:

```
/data/nativeplanet/logs/nativeplanet-vere-launch.log
```

### Status Queries

Reads init service state from:

```kotlin
getprop init.svc.nativeplanet_vere  // running, stopped, etc.
```

## Security Constraints

- Do not log keyMaterialRef contents
- Do not accept raw +code in BootPackage
- Do not pass secrets in system properties
- Do not print environment variables
- Validate all BootPackage fields before writing

## Status

- [x] Interface definition
- [x] Model definitions
- [ ] Stub implementation
- [ ] Binder service (if needed)
- [ ] Integration with Whisper Launcher
