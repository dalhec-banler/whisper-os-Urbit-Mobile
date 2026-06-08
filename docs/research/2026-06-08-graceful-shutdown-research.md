# 2026-06-08 Graceful Shutdown Research

## Summary

NativePlanet Mobile should not use `setprop nativeplanet.vere.enabled 0` as a normal stop path. That path asks Android init to stop the service and can become a hard process-group kill. It is useful only as an emergency force-stop.

The product stop path should mirror Native Planet GroundSeg:

1. Mark desired runtime state as stopped so watchdog/restart logic does not immediately bring the ship back.
2. Send Urbit's graceful `|exit` through Click/conn.sock.
3. Poll for process exit and allow minutes, not seconds.
4. Fall back to SIGTERM-to-king if Click/conn.sock is unavailable.
5. Fall back to init hard stop only for explicit force-stop.

## Upstream Vere

Local source confirms the king process installs signal handling for graceful shutdown:

- `pkg/vere/king.c`: `_king_sign_init()` registers `SIGTERM` and the nearby source comment says it gracefully shuts down on SIGTERM.
- `pkg/vere/king.c`: `_king_sign_cb()` calls `u3_king_exit()` for `SIGTERM`.
- `pkg/vere/lord.c`: `u3_writ_exit()` writes `%exit` to the serf.
- `pkg/vere/mars.c`: `%exit` transitions mars into exit state.

## Native Planet GroundSeg

GroundSeg is the relevant Native Planet server-device manager. Its docs describe GroundSeg as Native Planet's open-source app for managing Urbit piers and related services on dedicated home servers.

GroundSeg's product shutdown path is Click-aware:

- `goseg/click/exit.go`: `BarExit()` creates `exit.hoon` and runs Click.
- The Hoon is:
  `=/  m  (strand ,vase)  ;<  our=@p  bind:m  get-our  ;<  ~  bind:m  (poke [our %hood] %drum-exit !>(~))  (pure:m !>('success'))`
- `goseg/routines/docker.go`: `GracefulShipExit()` calls `click.BarExit(patp)` for running ships, then polls until the container is no longer `Up`.
- `goseg/handler/urbit.go`: `WaitComplete()` polls every 500ms and gives ship operations up to 10 minutes.

GroundSeg still has Docker stop fallbacks, but those are fallbacks after the Urbit-aware path fails or for non-ship containers.

## Android Device Test

Device state before test:

- ROM: `eng.anoffice.20260528.112643`
- dev moon: `~namfeb-rossyp-palrum-roclur`
- `nativeplanet_vere`: running
- king PID: `3236`
- serf PID: `3386`

Test:

```sh
adb root
adb shell 'kill -TERM 3236'
```

Observed:

- Unprivileged shell signal was denied by SELinux, as expected.
- Root SIGTERM to king made init report `nativeplanet_vere` exited with status `0`.
- Runtime provider transitioned to `state=stopped`, `connSockAvailable=false`.
- `setprop nativeplanet.vere.enabled 0; setprop nativeplanet.vere.enabled 1` restarted the moon.
- After restart, PIDs returned, `.urb/conn.sock` returned, and provider reported `state=running`, `connSockAvailable=true`.

Second test:

```sh
adb forward tcp:12321 localfilesystem:/data/nativeplanet/ships/namfeb-rossyp-palrum-roclur/.urb/conn.sock
node /tmp/conn-test/fyrd-client.js 12321 \
  "=/  m  (strand ,vase)  ;<  our=@p  bind:m  get-our  ;<  ~  bind:m  (poke [our %hood] %drum-exit !>(~))  (pure:m !>('success'))"
```

Observed:

- `%fyrd %base %khan-eval %noun %ted-eval` successfully ran a harmless Hoon thread first.
- The GroundSeg `|exit` Hoon then returned `%avow ... %success`.
- The init-managed Vere process exited cleanly and provider reported `state=stopped`.
- Toggling desired state off/on restarted the dev moon, recreated `.urb/conn.sock`, and restored provider `state=running`.

## Implementation Implications

Controller work:

- Add a controller-owned graceful stop action; keep direct runtime internals out of the launcher.
- Implement the GroundSeg `|exit` Hoon via conn.sock by extending `NounCodec`/`ConnSockClient` from `%peel` to `%fyrd`.
- Wait/poll for stop completion with a long timeout, using GroundSeg's 10-minute model as the reference.
- Add a structured state such as `stopping`.
- Add structured failure codes such as `STOP_CLICK_FAILED`, `STOP_SIGNAL_DENIED`, `STOP_TIMEOUT`, and `FORCE_STOP_REQUIRED`.
- If implementing SIGTERM fallback from `system_app`, add only the narrow process signaling SELinux rule needed for `system_app -> nativeplanet_vere`.

Implemented in source after this research:

- `NounCodec.buildFyrdKhanEvalRequest()`
- `ConnSockClient.sendKhanEval()`
- `ConnSockClient.requestGracefulExit()`
- `RuntimeControl.startRuntime()`
- `RuntimeControl.stopRuntimeAsync()`
- provider `call()` methods `startRuntime` and `stopRuntime`

Validation:

- `m NativePlanetController -j10` passed on 2026-06-08.
- Built APK contains `RuntimeControl`, `%khan-eval`, and `%drum-exit`.
- The implementation is not live on the device until the next ROM flash.

Launcher work:

- Hide or disable user-facing stop/restart until controller graceful stop exists.
- When enabled, show stop/restart as long-running operations with progress/state, not instant toggles.

Do not:

- Do not expose `setprop nativeplanet.vere.enabled 0` as normal UI behavior.
- Do not rely on Android init `gentle_kill`; Android's built-in fallback kill window is too short for a real Urbit shutdown.
- Do not use Lens for shutdown or health.
