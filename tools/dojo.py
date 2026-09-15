#!/usr/bin/env python3
"""Run dojo commands on a ship over HTTP by typing into %herm (the web terminal),
and print what the terminal shows.

Usage: dojo.py <http-base-url> <+code> [--wait SECONDS] "<command>" ["<command>" ...]

Works when the ship has no tty (-t) and when the lens dojo session is broken.
"""
import json, sys, time, threading, uuid, urllib.request, http.cookiejar

def main():
    args = sys.argv[1:]
    base, code = args[0], args[1]
    wait = 6.0
    session = ""
    rest = args[2:]
    while rest and rest[0] in ("--wait", "--session"):
        if rest[0] == "--wait": wait = float(rest[1])
        else: session = rest[1]
        rest = rest[2:]
    cmds = [c for c in rest if c != "--raw"]
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    r = opener.open(urllib.request.Request(base + "/~/login", data=("password=" + code).encode(),
                    headers={"Content-Type": "application/x-www-form-urlencoded"}), timeout=20)
    names = [c.name for c in jar if c.name.startswith("urbauth-")]
    if not names:
        print("login failed", r.status); sys.exit(1)
    ship = names[0][len("urbauth-~"):]
    chan = base + "/~/channel/" + uuid.uuid4().hex
    n = [0]
    def send(msgs):
        req = urllib.request.Request(chan, data=json.dumps(msgs).encode(), method="PUT",
                                     headers={"Content-Type": "application/json"})
        try:
            opener.open(req, timeout=20).read()
        except urllib.error.HTTPError as e:
            print("channel PUT failed", e.code, "for", json.dumps(msgs)[:160]); raise
    lines = []
    def reader():
        try:
            resp = opener.open(urllib.request.Request(chan, headers={"Accept": "text/event-stream"}), timeout=wait + 30)
            buf = b""
            end = time.time() + wait + 8
            while time.time() < end:
                chunk = resp.readline()
                if not chunk: break
                if chunk.startswith(b"data:"):
                    try:
                        ev = json.loads(chunk[5:].decode())
                    except Exception:
                        continue
                    j = ev.get("json")
                    if "--raw" in sys.argv: lines.append("RAW " + json.dumps(ev)[:400])
                    def walk(b):
                        if isinstance(b, list):
                            for x in b: walk(x)
                        elif isinstance(b, dict):
                            if "mor" in b: walk(b["mor"])
                            if "lin" in b and isinstance(b["lin"], list): lines.append("".join(b["lin"]))
                            if "klr" in b and isinstance(b["klr"], list):
                                lines.append("".join("".join(seg.get("text", [])) if isinstance(seg, dict) else "" for seg in b["klr"]))
                    walk(j)
        except Exception as e:
            lines.append(f"[stream ended: {e.__class__.__name__}]")
    def belt(b):
        n[0] += 1
        if session:
            return {"id": n[0], "action": "poke", "ship": ship, "app": "herm", "mark": "herm-task", "json": {"session": session, "belt": b}}
        return {"id": n[0], "action": "poke", "ship": ship, "app": "herm", "mark": "belt", "json": b}
    if session:
        # a fresh dill session running the dojo, independent of the default one
        n[0] += 1
        send([{"id": n[0], "action": "poke", "ship": ship, "app": "herm", "mark": "herm-task",
               "json": {"session": session, "open": {"term": "herm", "apps": [{"who": "~" + ship, "app": "dojo"}]}}}])
        time.sleep(1.0)
    # subscribe first so output is not missed
    n[0] += 1
    send([{"id": n[0], "action": "subscribe", "ship": ship, "app": "herm", "path": "/session/" + session + "/view"}])
    t = threading.Thread(target=reader, daemon=True); t.start()
    time.sleep(1.0)
    for cmd in cmds:
        if cmd == "^C":
            send([belt({"mod": {"mod": "ctl", "key": "c"}})]); time.sleep(2); continue
        msgs = [belt({"txt": [ch]}) for ch in cmd] + [belt({"ret": None})]
        send(msgs)
        time.sleep(wait)
    n[0] += 1
    try: send([{"id": n[0], "action": "delete"}])
    except Exception: pass
    seen = None
    for l in lines:
        if l.strip() and l != seen and not l.startswith("RAW ") or l.startswith("RAW ") and "--raw" in sys.argv:
            print(l); seen = l

if __name__ == "__main__":
    main()
