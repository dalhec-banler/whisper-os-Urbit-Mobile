# Android Management API MCP

Status: reviewed, not part of the current NativePlanet Mobile toolchain.

Google provides a remote MCP server for the Android Management API at:

```text
https://androidmanagement.googleapis.com/mcp
```

The service is currently documented as a preview feature. It is meant for
Android Enterprise / EMM workflows: querying managed enterprises, devices,
policies, applications, and web apps.

## Available Public Tools

The unauthenticated `tools/list` response currently advertises:

- `get_application`
- `get_device`
- `get_enterprise`
- `get_policy`
- `get_web_app`
- `list_devices`
- `list_enterprises`
- `list_policies`
- `list_web_apps`

## Why It Is Not Our Current Android MCP

NativePlanet Mobile development needs:

- ADB device control
- logcat and dmesg inspection
- package install and launch
- screenshots and UI testing
- Gradle builds
- GrapheneOS ROM module/full builds
- filesystem checks on `/data/nativeplanet`
- SELinux denial inspection

The Android Management API MCP does not provide those local development
surfaces. It is a cloud fleet-management API for enrolled enterprise devices.

## When It Might Matter

It may become useful later if NativePlanet Mobile needs managed-device or
enterprise fleet features, such as:

- checking a managed fleet's device status,
- auditing policy compliance,
- managing Android Enterprise policies,
- querying managed app state.

That is separate from building and testing the custom GrapheneOS-based phone
runtime.

## Current Recommendation

Do not add this MCP server to AI coding agents / MCP clients for the MVP phone build loop.

Keep using:

- local shell and ADB for Android device work,
- GrapheneOS build commands for ROM work,
- Urbit MCP on a dev/distro ship for Artemis and parent-side Urbit work.

If we later need Android Enterprise management, configure this server with a
dedicated Google Cloud identity and least-privilege IAM roles. Do not store
OAuth credentials in the repo.
