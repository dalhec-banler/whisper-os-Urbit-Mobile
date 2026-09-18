# tools

Developer scripts. Run from the repo root unless the header says otherwise.

Three of these overlap in purpose; use the canonical one:

- `conn-client.js` is canonical for talking to a ship over `conn.sock` (newt framing, jam/cue; `--adb` forwards the device pier's socket).
- `fake-ship.py` is canonical for local fake ships: boots vere under a pty and drives the dojo (`start`, `send`, `run`, `ctrl`, `clear`, `tail`, `stop`).
- `dojo.py` is canonical for herm-over-Eyre: types dojo commands into `%herm` over HTTP and prints the terminal; `--poke app mark json` sends a JSON poke and prints the ack. `eyre-poke.py` was folded into this mode and removed.

| Tool | Purpose |
| --- | --- |
| `build-vere-android.sh` | Build vere for aarch64-linux-musl with zig; `VERE_SRC` and `ZIG_PATH` or two positionals. |
| `check-repo-hygiene.sh` | Grep tracked and untracked files for home paths, owner labels, login codes, moon keys. Exit 1 on a hit. |
| `conn-client.js` | conn.sock client: peek, poke and scry a running ship from node. |
| `doctor-mobile-app-sync.sh` | Compare the phone moon's installed desks against the parent and the controller's hosted-app list; reads `secrets/urbit/pairing.env`. |
| `dojo.py` | Dojo over Eyre via `%herm`; `--wait`, `--session`, `--raw`, `--poke`. Works with `-t` ships and a wedged default session. |
| `fake-harness.sh` | Build the delegation test harness from scratch: fake ~zod, its moon, and ~bud, with Tlon, Artemis and the mobile desk installed. Needs `NP_TOOLS_DIR`, `NP_ARTEMIS_DESK`, `VERE` or their repo-relative defaults. |
| `fake-ship.py` | Run one fake ship under a pty and drive its dojo from the shell. `VERE` selects the binary. |
| `install-mobile-metadata-desk.sh` | Install `%nativeplanet-mobile` onto a running moon over adb (the per-moon path; it cannot be baked into the pill). |
| `install-parent-mobile-desks.sh` | Ask the parent planet to publish the mobile desk set to the phone moon; dry-run by default, `--apply` to do it. |
| `package-rom.sh` | Build and package the GrapheneOS ROM with the NativePlanet vendor tree. |
| `probe-parent-pairing.sh` | Log in to the parent planet with the pairing env and report what it exposes; `--code-stdin` reads the code from stdin. |
| `smoke-controller-provider.sh` | Query the controller's status content provider over adb and assert the expected runtime and network state. |
| `smoke-hosted-mobile-apps.sh` | Assert the controller's hosted-app list carries the expected sources. |
| `smoke-launcher-ui.sh` | Dump the launcher UI over adb and assert the home screen and safe labels are present. |
| `smoke-satellite-pill.sh` | Typecheck the `%nativeplanet-mobile` Gall app under `urbit eval`; `URBIT_BIN` selects the binary. |
