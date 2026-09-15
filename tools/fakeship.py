#!/usr/bin/env python3
"""Run a ship under a pseudo-terminal and drive its dojo from the command line.

  fakeship.py start <pier-dir> [vere args...]      # spawns a detached daemon
  fakeship.py send  <pier-dir> "<text>"            # types text + Enter into the dojo
  fakeship.py ctrl  <pier-dir> c                   # sends a control key (ctrl-c, ctrl-d ...)
  fakeship.py tail  <pier-dir> [N]                 # last N lines of terminal output
  fakeship.py stop  <pier-dir>                     # ctrl-d (graceful exit)

The daemon keeps <pier-dir>.tty.log (all terminal output) and <pier-dir>.cmd
(a FIFO for input). VERE points at the binary (default: tools/vere64-edge).
"""
import os, sys, time, pty, select, subprocess, errno, re, signal

VERE = os.environ.get("VERE", "/Users/austinnelsen/Desktop/Urbit Development/tools/vere64-edge")

def paths(pier):
    pier = os.path.abspath(pier.rstrip("/"))
    return pier, pier + ".tty.log", pier + ".cmd"

def daemon(pier, args):
    pier, log, fifo = paths(pier)
    if not os.path.exists(fifo): os.mkfifo(fifo)
    master, slave = pty.openpty()
    proc = subprocess.Popen([VERE] + args, stdin=slave, stdout=slave, stderr=slave, preexec_fn=os.setsid, close_fds=True)
    os.close(slave)
    cmd = os.open(fifo, os.O_RDONLY | os.O_NONBLOCK)
    ansi = re.compile(rb"\x1b\[[0-9;?]*[A-Za-z]|\r")
    with open(log, "ab", buffering=0) as out:
        while proc.poll() is None:
            r, _, _ = select.select([master, cmd], [], [], 1.0)
            if master in r:
                try:
                    data = os.read(master, 65536)
                except OSError as e:
                    if e.errno == errno.EIO: break
                    raise
                if data: out.write(ansi.sub(b"", data))
            if cmd in r:
                data = os.read(cmd, 65536)
                if data: os.write(master, data)
                else:
                    os.close(cmd); cmd = os.open(fifo, os.O_RDONLY | os.O_NONBLOCK)
    try: os.unlink(fifo)
    except OSError: pass

def main():
    op = sys.argv[1]; pier = sys.argv[2]
    p, log, fifo = paths(pier)
    if op == "start":
        if os.fork() == 0:
            os.setsid()
            if os.fork() == 0:
                daemon(p, sys.argv[3:])
            os._exit(0)
        time.sleep(0.5); print("started", p); return
    if op == "send":
        with open(fifo, "wb") as f: f.write(sys.argv[3].encode() + b"\r"); return
    if op == "ctrl":
        with open(fifo, "wb") as f: f.write(bytes([ord(sys.argv[3].lower()) - 96])); return
    if op == "stop":
        with open(fifo, "wb") as f: f.write(b"\x04"); return
    if op == "clear":
        with open(fifo, "wb") as f: f.write(b"\x7f" * 400); return
    if op == "run":
        # clear any stale input, type one command, wait until the dojo prompt is idle; print what appeared
        timeout = float(sys.argv[4]) if len(sys.argv) > 4 else 180.0
        with open(fifo, "wb") as f: f.write(b"\x7f" * 400)
        time.sleep(1.5)
        before = os.path.getsize(log) if os.path.exists(log) else 0
        with open(fifo, "wb") as f: f.write(sys.argv[3].encode() + b"\r")
        t0 = time.time(); last = None; stable = 0
        while time.time() - t0 < timeout:
            time.sleep(1.0)
            data = open(log, "rb").read()[before:]
            txt = data.decode("utf8", "replace")
            lastline = txt.rstrip("\n").splitlines()[-1] if txt.strip() else ""
            idle = re.search(r"~[a-z-]+:dojo> ?$", lastline) is not None
            if idle and txt == last: stable += 1
            else: stable = 0
            last = txt
            if idle and stable >= 2: break
        out = [l for l in last.splitlines() if l.strip() and not set(l.strip()) <= set("|/-\\")]
        print("\n".join(out[-60:])); return
    if op == "tail":
        n = int(sys.argv[3]) if len(sys.argv) > 3 else 20
        lines = open(log, "rb").read().decode("utf8", "replace").splitlines()
        print("\n".join(l for l in lines[-n:] if l.strip())); return
    print(__doc__)

if __name__ == "__main__":
    main()
