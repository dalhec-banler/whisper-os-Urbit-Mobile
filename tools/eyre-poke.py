#!/usr/bin/env python3
"""Poke an agent over Eyre with a JSON-convertible mark, like a web client does.

Usage: eyre-poke.py <http-base-url> <+code> <app> <mark> '<json>'
"""
import json, sys, uuid, urllib.request, http.cookiejar

def main():
    base, code, app, mark, body = sys.argv[1:6]
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    opener.open(urllib.request.Request(base + "/~/login", data=("password=" + code).encode(),
                headers={"Content-Type": "application/x-www-form-urlencoded"}), timeout=20)
    names = [c.name for c in jar if c.name.startswith("urbauth-")]
    if not names: print("login failed"); sys.exit(1)
    ship = names[0][len("urbauth-~"):]
    chan = base + "/~/channel/" + uuid.uuid4().hex
    msgs = [{"id": 1, "action": "poke", "ship": ship, "app": app, "mark": mark, "json": json.loads(body)}]
    req = urllib.request.Request(chan, data=json.dumps(msgs).encode(), method="PUT", headers={"Content-Type": "application/json"})
    try:
        r = opener.open(req, timeout=20); print("poke sent", r.status)
    except urllib.error.HTTPError as e:
        print("poke refused", e.code, e.read()[:200]); return
    # read the ack or nack from the channel stream
    try:
        resp = opener.open(urllib.request.Request(chan, headers={"Accept": "text/event-stream"}), timeout=15)
        import time; end = time.time() + 8
        while time.time() < end:
            line = resp.readline()
            if not line: break
            if line.startswith(b"data:"):
                ev = json.loads(line[5:].decode())
                if ev.get("response") == "poke":
                    print("ack" if ev.get("ok") else "NACK: " + str(ev.get("err"))[:600]); break
    except Exception as e:
        print("no ack read:", e.__class__.__name__)
    try:
        opener.open(urllib.request.Request(chan, data=json.dumps([{"id": 2, "action": "delete"}]).encode(), method="PUT", headers={"Content-Type": "application/json"}), timeout=10)
    except Exception: pass

if __name__ == "__main__":
    main()
