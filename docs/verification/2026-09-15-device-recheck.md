# Device recheck, 2026-09-15

Re-verification of the test phone after the summer, driven from the MacBook
over adb. Nothing on the device was changed except a disposable fake ship in
`/data/local/tmp` (removed afterwards; the upstream binary and pill were left
there for reuse).

## Environment

- Device: Pixel 8 Pro (husky), ROM `2026062202`, userdebug, SELinux enforcing,
  48-bit user address space
- Ship: `~hadwyn-taslyx-dozzod-hobdem` (moon of `~hobdem`), boot mode MOON,
  state `running`, vere `4.3-33293b1`, king RSS ~55 MB, serf ~272 MB
- Launch wrapper already passes `--lmdb-map-size 68719476736 --loom 32`

## Results

| Check | Result |
|---|---|
| `tools/smoke-controller-provider.sh` | PASS (runtime running, boot package valid, WIFI) |
| `tools/smoke-launcher-ui.sh` | PASS (Launcher3 HOME, My Urbit Apps, pinned Landscape/Terminal/Tlon) |
| Provider `getStatus` / `getRuntime` / `getHostedApps` | OK; inventory `docket+nativeplanet-mobile`, five apps |
| Terminal (`%webterm`) | Live dojo; typed text reaches the prompt. Prompt line is hidden under the keyboard (no IME resize) |
| Tlon (`%groups`) | Home surface with real DMs |
| Landscape (`%landscape`) | Tile grid renders |
| Grove (`%grove`) | Opens from My Urbit Apps, file view renders |
| Kin (`%kin`) | Inventory-only, "no launch URL yet", as intended |
| `tools/conn-client.js --adb` | Fails by policy: SELinux denies the `shell` domain on `nativeplanet_data_file`. Use the provider, or root. |
| `tools/smoke-hosted-mobile-apps.sh` | Fails for the same reason (its fyrd is cancelled) |

## Defect: hosted app switching

With one hosted app open, press Home, then tap a different hosted app: the old
app is shown. Both directions reproduced (Terminal -> Tlon, Tlon -> Terminal);
the hosted window token is unchanged and `ActivityTaskManager` logs
`onActivityRestartAttempt` for task #204 (affinity `com.android.launcher3`).
Cause: all hosted apps share one standard task. Workaround on device:
`adb shell am force-stop com.android.launcher3`. Fix prepared as
`rom/patches/launcher3-whisper-os-v3-hosted-app-tasks.patch` (per-desk
document tasks + `onNewIntent` + IME inset padding); needs a ROM rebuild.

## Upstream vere on the phone

The official upstream build `vere64-v5.0-e162414-linux-aarch64` (edge pace,
static musl, 64-bit loom, demand paging) runs on the phone unpatched, as root
and as uid 2000. A fake `~zod` booted from `/data/local/tmp` with the official
`urbit-v4.6.pill` to a live pier: Eyre answered on its port and `conn.sock`
answered `%peel %live` / `%who` through
`adb forward tcp:<port> localfilesystem:/data/local/tmp/ships/zod/.urb/conn.sock`
(allowed there because `/data/local/tmp` is `shell_data_file`). The 32-to-64
loom migration was also exercised on a real comet on the Mac. Open question for
the ROM: adopt the upstream binary as the prebuilt (drops the custom zig Android
build and its image-base patch) after a real moon proves dawn over HTTPS with it.
Note that the emulator's kernel has a 39-bit address space where the default
1 TiB LMDB map fails; the wrapper's explicit map size covers that.
