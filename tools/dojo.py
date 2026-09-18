#!/usr/bin/env python3
"""Run dojo commands on a ship over HTTP by typing into %herm (the web terminal),
and print what the terminal shows.

Usage: dojo.py <http-base-url> <+code> [--wait SECONDS] [--session NAME] [--raw] "<command>" ["<command>" ...]
       dojo.py <http-base-url> <+code> --poke <app> <mark> '<json>'

  --wait SECONDS   seconds to let each command run before typing the next (default 6)
  --session NAME   open a fresh herm/dill session named NAME and type into that
                   instead of the default one
  --raw            also print each raw channel event (truncated)
  --poke           instead of typing into the dojo, poke <app> with <mark> and a
                   JSON body over the same Eyre channel and print the ack or nack

Works when the ship has no tty (-t) and when the default herm session is wedged.
"""
import json, sys, time, threading, uuid, urllib.request, http.cookiejar

DEFAULT_WAIT = 6.0        # seconds a command gets before the next one is typed
STREAM_OPEN_SLACK = 30.0  # extra seconds on the event-stream socket timeout
STREAM_READ_SLACK = 8.0   # extra seconds to keep reading after the last command
POKE_ACK_WAIT = 8.0       # seconds to wait for a poke ack or nack

def main():
    args = sys.argv[1:]
    base, code = args[0], args[1]
    wait = DEFAULT_WAIT
    session = ""
    raw = "--raw" in args
    rest = [a for a in args[2:] if a != "--raw"]
    poke = None
    while rest and rest[0] in ("--wait", "--session", "--poke"):
        if rest[0] == "--wait": wait = float(rest[1]); rest = rest[2:]
        elif rest[0] == "--session": session = rest[1]; rest = rest[2:]
        else: poke = rest[1:4]; rest = rest[4:]
    cmds = rest
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
    def close():
        n[0] += 1
        try: send([{"id": n[0], "action": "delete"}])
        except Exception: pass
    if poke:
        app, mark, body = poke
        n[0] += 1
        send([{"id": n[0], "action": "poke", "ship": ship, "app": app, "mark": mark, "json": json.loads(body)}])
        try:
            resp = opener.open(urllib.request.Request(chan, headers={"Accept": "text/event-stream"}), timeout=POKE_ACK_WAIT + 5)
            end = time.time() + POKE_ACK_WAIT
            while time.time() < end:
                line = resp.readline()
                if not line: break
                if line.startswith(b"data:"):
                    ev = json.loads(line[5:].decode())
                    if raw: print("RAW " + json.dumps(ev)[:400])
                    if ev.get("response") == "poke":
                        print("ack" if ev.get("ok") else "NACK: " + str(ev.get("err"))[:600]); break
        except Exception as e:
            print("no ack read:", e.__class__.__name__)
        close(); return
    lines = []
    def reader():
        try:
            resp = opener.open(urllib.request.Request(chan, headers={"Accept": "text/event-stream"}), timeout=wait + STREAM_OPEN_SLACK)
            end = time.time() + wait + STREAM_READ_SLACK
            while time.time() < end:
                chunk = resp.readline()
                if not chunk: break
                if chunk.startswith(b"data:"):
                    try:
                        ev = json.loads(chunk[5:].decode())
                    except Exception:
                        continue
                    j = ev.get("json")
                    if raw: lines.append("RAW " + json.dumps(ev)[:400])
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
    close()
    seen = None
    for l in lines:
        if not l.strip() or l == seen:
            continue
        if l.startswith("RAW ") and not raw:
            continue
        print(l); seen = l

if __name__ == "__main__":
    main()
