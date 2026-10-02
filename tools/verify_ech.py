#!/usr/bin/env python3
"""Verification commands for the ECH WebView bridge and packaged native libraries."""

from __future__ import annotations

import argparse
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
BRIDGE_SOURCE = (
    PROJECT_ROOT
    / "app/src/main/java/io/github/daisukikaffuchino/han1meviewer"
    / "logic/network/ech/EchWebBridgeJs.kt"
)
JNI_SYMBOL = (
    b"Java_io_github_daisukikaffuchino_han1meviewer_"
    b"logic_network_ech_HyEchH3_h3Fetch"
)


def extract_bridge_template() -> str:
    source = BRIDGE_SOURCE.read_text(encoding="utf-8")
    match = re.search(r'private val TEMPLATE = """(.*?)"""\.trimIndent\(\)', source, re.S)
    if not match:
        raise SystemExit("Unable to extract the ECH bridge JavaScript template")
    return match.group(1).replace("__ECH_PROTECTED__", '["hanime1.me"]')


def verify_bridge() -> None:
    script = extract_bridge_template()
    with tempfile.TemporaryDirectory() as temporary:
        path = Path(temporary) / "ech_bridge.js"
        path.write_text(script, encoding="utf-8")
        subprocess.run(["node", "--check", str(path)], check=True)

        harness = Path(temporary) / "harness.js"
        harness.write_text(
            """
const fs = require('fs');
let calls = [];
global.window = global;
global.location = { href: 'https://hanime1.me/login' };
global.document = {
  addEventListener() {},
  querySelector() { return null; }
};
global.EchBridge = {
  log() {},
  send(id, method, url, headers, body, page) {
    calls.push({ method, url });
    window.__echBridgeResolve(id, JSON.stringify({
      ok: true, status: 200, url, headers: {}, bodyB64: ''
    }));
  },
  postForm() {}
};
global.fetch = async () => new Response('{}', { status: 200 });
global.XMLHttpRequest = function () {};
global.XMLHttpRequest.prototype = { open() {}, setRequestHeader() {}, send() {}, abort() {} };
eval(fs.readFileSync(process.argv[2], 'utf8'));
(async () => {
  await window.fetch('https://hanime1.me/api', { method: 'POST', body: 'a=1' });
  if (calls.length !== 1 || calls[0].method !== 'POST') process.exit(2);
  await window.fetch('https://example.com/api', { method: 'POST', body: 'a=1' });
  console.log('ECH WebView bridge behavior OK');
})().catch(error => { console.error(error); process.exit(1); });
""",
            encoding="utf-8",
        )
        subprocess.run(["node", str(harness), str(path)], check=True)


def default_apk() -> Path:
    candidates = sorted(
        (PROJECT_ROOT / "app/build/outputs/apk/debug").glob("*.apk"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )
    if not candidates:
        raise SystemExit("No debug APK found")
    return candidates[0]


def verify_apk(path: str | None) -> None:
    apk = Path(path).resolve() if path else default_apk()
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if "lib/arm64-v8a/libchino.so" not in names:
            raise SystemExit("APK is missing lib/arm64-v8a/libchino.so")
        if "lib/arm64-v8a/libconscrypt_jni.so" not in names:
            raise SystemExit("APK is missing lib/arm64-v8a/libconscrypt_jni.so")
        if any(name.endswith("/libhn1_h3.so") for name in names):
            raise SystemExit("APK still contains a separate libhn1_h3.so")
        if any("libquiche" in name for name in names):
            raise SystemExit("APK contains an unexpected libquiche shared library")
        chino = archive.read("lib/arm64-v8a/libchino.so")
        if JNI_SYMBOL not in chino:
            raise SystemExit("libchino.so does not contain the HyEchH3 JNI symbol")
    print(f"APK verification OK: {apk}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("bridge", help="verify WebView bridge JavaScript behavior")
    apk = commands.add_parser("apk", help="verify packaged ECH native libraries")
    apk.add_argument("path", nargs="?", help="APK path; defaults to the latest debug APK")
    args = parser.parse_args()

    if args.command == "bridge":
        verify_bridge()
    else:
        verify_apk(args.path)


if __name__ == "__main__":
    main()
