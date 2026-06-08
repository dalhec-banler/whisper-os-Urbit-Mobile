# Phase 1 conn.sock Status Report

**Status: IMPLEMENTED AND MODULE-BUILD VERIFIED**

Note: this report was originally written during the first Phase 1 pass. It has
been updated to reflect the current ROM overlay implementation, which uses
`android.system.Os` direct AF_UNIX sockets instead of `android.net.LocalSocket`.

## Summary

Phase 1 conn.sock runtime-status polling implementation is complete and module
build validation has passed.

## Files Changed

### Controller (vendor/nativeplanet/controller)

1. **NounCodec.java** (NEW)
   - Minimal jam/cue implementation (~280 lines)
   - Atom/Cell noun types
   - Newt framing encode/decode
   - %peel request/response builders

2. **ConnSockClient.java** (NEW)
   - `android.system.Os` Unix domain socket client
   - Try-with-resources for cleanup
   - StatusResult for failure classification
   - pollStatus() convenience method

3. **RuntimeStatusPoller.java** (NEW)
   - HandlerThread-based background poller
   - Reads pier path from boot-package.json
   - Polls conn.sock every 5s (30s when stopped)
   - Writes runtime-status.json atomically (temp file + rename)
   - chmod 0640 for group-readable

4. **NativePlanetControllerService.java** (MODIFIED)
   - Added RuntimeStatusPoller field
   - Added start/stop calls in onCreate/onDestroy

5. **NativePlanetStatusProvider.java** (MODIFIED)
   - Updated getRuntimeStatus() to parse new schema
   - Added JSON validation with fallback
   - Extracts nested runtime object
   - Added version, lastSuccessfulPoll, connSockAvailable fields

### Launcher (whisper-launcher)

6. **RuntimeStatus.kt** (MODIFIED)
   - Added: version: String? = null
   - Added: lastSuccessfulPoll: Long? = null
   - Added: connSockAvailable: Boolean = false
   - Default values for backwards compatibility

7. **ProviderNativePlanetClient.kt** (MODIFIED)
   - Updated parsing for new fields
   - lastError handles both object and string formats

### Test Fixtures

8. **test-fixtures/noun-codec-fixtures.md** (NEW)
   - Jam/cue test vectors from working JS implementation
   - Cord encoding examples
   - Response parsing validation checklist

## Static Review Findings

### Security
- ✅ No key material logged or exposed
- ✅ keyMaterialRef properly redacted to keyFileExists boolean
- ✅ Socket path derived only from pierPath in boot-package.json

### Resource Management
- ✅ AF_UNIX FileDescriptor uses try-with-resources
- ✅ closeQuietly() handles cleanup
- ✅ HandlerThread.quitSafely() on stop

### Protocol
- ✅ Newt frame length is little-endian
- ✅ Request IDs via AtomicLong
- ✅ Failures classified (connectionFailed vs protocolError)

### Robustness
- ✅ Atomic file writes (temp file + rename)
- ✅ chmod 0640 maintained
- ✅ Provider fallback on missing/malformed JSON
- ✅ Launcher model has default values

### Code Fix Applied
- RuntimeStatusPoller.writeFile(): Changed from direct write to atomic temp+rename pattern

## SELinux Source Ledger

Confirmed in source at line 14 of `vendor/nativeplanet/sepolicy/private/nativeplanet_vere.te`:
```
allow nativeplanet_vere nativeplanet_data_file:sock_file create_file_perms;
```

Not added (not proven necessary):
- `/proc/net/route` read permission (cosmetic denial, doesn't block boot)

## Build Status

Command used:
```bash
cd /home/anoffice/grapheneos-2026040800
bash --norc -c '
  source build/envsetup.sh
  lunch husky bp4a userdebug
  m NativePlanetController -j10
'
```

Module build has passed with the current Android.bp configuration
(`platform_apis: true`) and the controller APK contains `NounCodec`,
`ConnSockClient`, `RuntimeStatusPoller`, and `RuntimeControl`.

## Device State

- Moon ~namfeb-rossyp-palrum-roclur running
- conn.sock exists and working
- SELinux overlay with sock_file fix active
- Current flashed device state may lag this source until the next ROM flash.

## Live APK Testing Safety

Live APK push remains unsafe when signatures differ from the flashed ROM. Use a
batched ROM build/flash for controller verification.

Before pushing to /system_ext:
1. Build must complete successfully
2. APK must be signed with platform key (automatic in build)
3. Need to verify signature matches existing installed APK
4. Consider stopping Controller service before push

Safer alternative: Include in next batched ROM build.

## Queued ROM Changes

1. SELinux sock_file rule (already in source)
2. Controller conn.sock polling (this PR, pending build)

## Recommended Next Action

1. Wait for build to complete
2. If build passes, verify APK in out/target/product/husky/system_ext/priv-app/
3. User decision: live push vs wait for ROM build
4. If live push approved, follow signature verification steps
