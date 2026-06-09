# 2026-06-08 Flash Verification

ROM: userdebug build 20260528

Device: husky / Pixel 8 Pro

## Pass

- Flash completed from signed factory image.
- Boot completed with `sys.boot_completed=1`.
- `sys.init.updatable_crashing` stayed empty after fresh boot.
- Build is `userdebug`, `ro.debuggable=1`, SELinux enforcing.
- `/system_ext/bin/vere` SHA256:
  `8e95e63cfbaed7141ce87ad3128cbda0b35f7cf869fdad7fc6e011f7be50fd42`
- `nativeplanet_vere` init service includes `oneshot`.
- `nativeplanet-vere-launch` includes stale `.vere.lock` and `.urb/conn.sock` cleanup.
- Dev moon `~namfeb-rossyp-palrum-roclur` boots via init.
- `conn.sock` is created at:
  `/data/nativeplanet/ships/namfeb-rossyp-palrum-roclur/.urb/conn.sock`
- Controller conn polling works through `android.system.Os` in enforcing mode.
- Provider `/runtime` returns:
  - `state=running`
  - `shipName=~namfeb-rossyp-palrum-roclur`
  - `version=4.3-33293b1`
  - `lastError=null`
  - `connSockAvailable=true`
- Launcher debug APK installs and launches without NativePlanet crash entries.
- Reboot auto-start from a healthy running pier passes.
- GroundSeg-compatible Click/conn.sock `|exit` path works:
  - request tested via `%fyrd %base %khan-eval %noun %ted-eval`
  - Hoon tested: `%hood` `%drum-exit`
  - conn.sock response returned `%success`
  - init-managed Vere exited and provider reported `state=stopped`
  - toggling desired state back to `1` restarted the moon and restored `conn.sock`
- Root SIGTERM to the Vere king process exits the init-managed service cleanly:
  - command tested: `kill -TERM <king-pid>`
  - init reported `nativeplanet_vere` exited with status `0`
  - runtime/provider moved to `state=stopped`
  - toggling desired state back to `1` restarted the moon and restored `conn.sock`

## Blocked

- Manual property stop/restart is not safe:
  - `setprop nativeplanet.vere.enabled 0` stops the service without init crash-looping.
  - The stale runtime cleanup runs on the next start.
  - Existing-pier boot can still exit/segfault after a hard stop.
- Unprivileged shell cannot signal `nativeplanet_vere` in enforcing mode.
- Android init `gentle_kill` alone is not enough for product shutdown because init's fallback kill window is far shorter than GroundSeg's long-running graceful wait model.

## Current Product Rule

Use start, reboot auto-start, and the tested conn.sock `|exit` mechanism as verified lifecycle primitives. Do not expose user-facing stop/restart until the controller wraps them in a long-running graceful stop API.

Target shutdown model:

- Preferred: GroundSeg-compatible Click/conn.sock `|exit` path, i.e. `%hood` `%drum-exit`.
- Wait: poll until the ship exits, allowing minutes rather than seconds.
- Fallback: SIGTERM to the king process, which was validated on device.
- Force-stop: init `stop` / process-group kill only as an explicit emergency action.

## Non-Blocking Noise

- `nativeplanet_vere` has `/dev/kmsg_debug` write denials.
- Existing platform components produce unrelated GrapheneOS/Pixel AVC noise.
