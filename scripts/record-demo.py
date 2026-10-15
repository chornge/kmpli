#!/usr/bin/env python3
"""Record a real asciinema cast of the kmpli TUI.

Drives the actual release binary through a pseudo-terminal and captures the real byte
stream, so the demo always reflects what the program does rather than a mock-up.

Usage:  python3 scripts/record-demo.py [path-to-binary] [out.cast]
"""
import json
import os
import pty
import select
import struct
import subprocess
import sys
import tempfile
import termios
import time
import fcntl

COLS, ROWS = 88, 22  # tight enough for a README hero image without much dead space

# (seconds to wait before sending, bytes to send, why)
DOWN, UP, ENTER = b"\x1b[B", b"\x1b[A", b"\r"
SCRIPT = [
    (2.2, b"",            "let the first prompt render"),
    (1.4, DOWN,           "show arrow-key navigation"),
    (0.9, UP,             "and that it moves back"),
    (1.0, ENTER,          "choose: use a template"),
    (1.6, DOWN,           "browse templates"),
    (0.7, DOWN,           ""),
    (1.0, ENTER,          "pick the library template"),
    (1.4, b"MyKmpApp",    "type a project name"),
    (0.9, ENTER,          ""),
    (1.4, b"com.acme.kmp", "type a package id"),
    (0.9, ENTER,          ""),
    (1.4, ENTER,          "include sample tests"),
    (1.8, ENTER,          "confirm and generate"),
]


def main():
    binary = sys.argv[1] if len(sys.argv) > 1 else "composeApp/build/bin/macosX64/releaseExecutable/kmpli.kexe"
    out = sys.argv[2] if len(sys.argv) > 2 else "demo.cast"
    binary = os.path.abspath(binary)

    workdir = tempfile.mkdtemp(prefix="kmpli-demo-")
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", ROWS, COLS, 0, 0))

    env = dict(os.environ, TERM="xterm-256color", COLUMNS=str(COLS), LINES=str(ROWS))
    proc = subprocess.Popen(
        [binary], stdin=slave, stdout=slave, stderr=slave, cwd=workdir, env=env, close_fds=True
    )
    os.close(slave)

    start = time.time()
    frames = []

    def drain(until):
        """Read whatever the program emits until `until`, timestamping each chunk."""
        while time.time() < until:
            r, _, _ = select.select([master], [], [], 0.05)
            if not r:
                continue
            try:
                data = os.read(master, 65536)
            except OSError:
                return False
            if not data:
                return False
            frames.append([round(time.time() - start, 3), "o", data.decode("utf-8", "replace")])
        return True

    for delay, keys, _why in SCRIPT:
        if not drain(time.time() + delay):
            break
        if keys:
            os.write(master, keys)

    drain(time.time() + 6.0)  # capture the generation output
    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        proc.kill()
    os.close(master)

    header = {
        "version": 2,
        "width": COLS,
        "height": ROWS,
        "timestamp": int(start),
        "env": {"SHELL": "/bin/sh", "TERM": "xterm-256color"},
    }
    with open(out, "w") as f:
        f.write(json.dumps(header) + "\n")
        for fr in frames:
            f.write(json.dumps(fr) + "\n")

    print(f"wrote {out}: {len(frames)} frames, {frames[-1][0] if frames else 0}s, workdir {workdir}")


if __name__ == "__main__":
    main()
