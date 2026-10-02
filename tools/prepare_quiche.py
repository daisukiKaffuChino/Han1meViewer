#!/usr/bin/env python3
"""Download, verify, extract, and patch quiche for the H3 ECH transport."""

from __future__ import annotations

import hashlib
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request
from pathlib import Path


QUICHE_VERSION = "0.22.0"
QUICHE_URL = f"https://static.crates.io/crates/quiche/quiche-{QUICHE_VERSION}.crate"
QUICHE_SHA256 = "28E5A763FECB47867BD3720F69EC87031FF42FDA1DC88BE2CB5FBB3A558FA5E4"


def apply_patch(path: Path, old: str, new: str, count: int = 1) -> None:
    source = path.read_text(encoding="utf-8")
    actual = source.count(old)
    if actual != count:
        raise SystemExit(
            f"quiche patch expected {count} matches in {path.name}, found {actual}: {old[:60]!r}"
        )
    path.write_text(source.replace(old, new, 1), encoding="utf-8")


def patch_quiche(root: Path) -> None:
    tls = root / "src" / "tls" / "mod.rs"
    lib = root / "src" / "lib.rs"

    apply_patch(
        tls,
        "    fn SSL_set_quic_use_legacy_codepoint(ssl: *mut SSL, use_legacy: c_int);",
        """    fn SSL_set_quic_use_legacy_codepoint(ssl: *mut SSL, use_legacy: c_int);

    fn SSL_set1_ech_config_list(
        ssl: *mut SSL, ech_config_list: *const u8, ech_config_list_len: usize,
    ) -> c_int;

    fn SSL_get0_ech_name_override(
        ssl: *const SSL, out_name: *mut *const c_char, out_name_len: *mut usize,
    );

    fn SSL_get0_ech_retry_configs(
        ssl: *const SSL, out_retry_configs: *mut *const u8, out_retry_configs_len: *mut usize,
    );""",
    )

    apply_patch(
        tls,
        "    pub fn set_quic_transport_params(&mut self, buf: &[u8]) -> Result<()> {",
        """    pub fn set_ech_config_list(&mut self, ech_config_list: &[u8]) -> Result<()> {
        let rc = unsafe {
            SSL_set1_ech_config_list(
                self.as_mut_ptr(),
                ech_config_list.as_ptr(),
                ech_config_list.len(),
            )
        };
        self.map_result_ssl(rc)
    }

    pub fn ech_name_override(&self) -> Option<String> {
        let mut ptr: *const c_char = std::ptr::null();
        let mut len: usize = 0;
        unsafe { SSL_get0_ech_name_override(self.as_ptr(), &mut ptr, &mut len) };
        if len == 0 || ptr.is_null() {
            return None;
        }
        let bytes = unsafe { std::slice::from_raw_parts(ptr as *const u8, len) };
        Some(String::from_utf8_lossy(bytes).to_string())
    }

    pub fn ech_retry_configs(&self) -> Vec<u8> {
        let mut ptr: *const u8 = std::ptr::null();
        let mut len: usize = 0;
        unsafe { SSL_get0_ech_retry_configs(self.as_ptr(), &mut ptr, &mut len) };
        if len == 0 || ptr.is_null() {
            return Vec::new();
        }
        unsafe { std::slice::from_raw_parts(ptr, len).to_vec() }
    }

    pub fn set_quic_transport_params(&mut self, buf: &[u8]) -> Result<()> {""",
    )

    apply_patch(
        lib,
        "    tls_ctx: tls::Context,",
        "    tls_ctx: tls::Context,\n\n    ech_config_list: Option<Vec<u8>>,",
    )
    apply_patch(lib, "            tls_ctx,", "            tls_ctx,\n            ech_config_list: None,")
    apply_patch(
        lib,
        "    fn with_tls_ctx(version: u32, tls_ctx: tls::Context) -> Result<Config> {",
        """    pub fn set_ech_config_list(&mut self, ech_config_list: &[u8]) {
        self.ech_config_list = Some(ech_config_list.to_vec());
    }

    fn with_tls_ctx(version: u32, tls_ctx: tls::Context) -> Result<Config> {""",
    )
    apply_patch(
        lib,
        "        let tls = config.tls_ctx.new_handshake()?;",
        """        let mut tls = config.tls_ctx.new_handshake()?;

        if let Some(ref ech) = config.ech_config_list {
            tls.set_ech_config_list(ech)?;
        }""",
    )
    apply_patch(
        lib,
        "    pub fn server_name(&self) -> Option<&str> {",
        """    pub fn ech_name_override(&self) -> Option<String> {
        self.handshake.ech_name_override()
    }

    pub fn ech_retry_configs(&self) -> Vec<u8> {
        self.handshake.ech_retry_configs()
    }

    pub fn server_name(&self) -> Option<&str> {""",
    )


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    vendor_root = project_root / "native-h3" / "vendor"
    quiche_root = vendor_root / "quiche"
    vendor_root.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as temporary:
        archive = Path(temporary) / f"quiche-{QUICHE_VERSION}.crate"
        cached = Path(archive)
        try:
            subprocess.run(
                [
                    "curl",
                    "-fsSL",
                    "--retry",
                    "3",
                    "--connect-timeout",
                    "20",
                    "--max-time",
                    "180",
                    QUICHE_URL,
                    "-o",
                    str(cached),
                ],
                check=True,
            )
        except (FileNotFoundError, subprocess.CalledProcessError):
            with urllib.request.urlopen(QUICHE_URL, timeout=180) as response:
                with cached.open("wb") as output:
                    shutil.copyfileobj(response, output)
        digest = hashlib.sha256(archive.read_bytes()).hexdigest().upper()
        if digest != QUICHE_SHA256:
            raise SystemExit(f"quiche checksum mismatch: {digest}")

        extracted = Path(temporary) / "extracted"
        with tarfile.open(archive, "r:gz") as tar:
            tar.extractall(extracted)
        source = extracted / f"quiche-{QUICHE_VERSION}"
        if not source.is_dir():
            raise SystemExit("quiche archive did not contain the expected root")
        if quiche_root.exists():
            shutil.rmtree(quiche_root)
        shutil.copytree(source, quiche_root)

    patch_quiche(quiche_root)
    (vendor_root / ".patched").write_text(QUICHE_SHA256, encoding="ascii")
    print(f"Prepared quiche {QUICHE_VERSION} at {quiche_root}")


if __name__ == "__main__":
    main()
