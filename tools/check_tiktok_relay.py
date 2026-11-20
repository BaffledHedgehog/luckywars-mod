"""Run one native fixture against an isolated, ephemeral copy of the real TLS media relay."""

import argparse
import importlib.util
import os
from pathlib import Path
import secrets
import shutil
import ssl
import subprocess
import tempfile
import threading


def configure(directory, openssl, keytool, ffmpeg):
    private_key = directory / "key.pem"
    certificate = directory / "certificate.pem"
    store = directory / "trust.p12"
    subprocess.run(
        [openssl, "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
         "-subj", "/CN=127.0.0.1", "-addext", "subjectAltName=IP:127.0.0.1",
         "-addext", "extendedKeyUsage=serverAuth", "-keyout", private_key, "-out", certificate],
        check=True, capture_output=True,
    )
    subprocess.run(
        [keytool, "-importcert", "-noprompt", "-alias", "loopback-media-relay", "-file", certificate,
         "-keystore", store, "-storetype", "PKCS12", "-storepass", "fixture-only"],
        check=True, capture_output=True,
    )
    subprocess.run(
        [ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", "-f", "lavfi", "-i",
         "color=c=0xe02020:s=180x320:r=15:d=0.6", "-f", "lavfi", "-i",
         "aevalsrc=0.25*sin(2*PI*440*t)|0.25*sin(2*PI*880*t):s=44100:d=0.6",
         "-c:v", "libx264", "-pix_fmt", "yuv420p", "-threads", "1", "-c:a", "aac",
         "-movflags", "+faststart", directory / "original.mp4"],
        check=True,
    )
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    tls.minimum_version = ssl.TLSVersion.TLSv1_2
    tls.load_cert_chain(certificate, private_key)
    return store, tls


def open_relay(module, directory, key, tls):
    cache = directory / "cache"
    cache.mkdir()
    receipts = directory / "puts"
    receipts.write_text("0\n", encoding="ascii")

    class RecordedRequest(module.Request):
        def send_response(self, code, message=None):
            if self.command == "PUT" and code == 201:
                self.server.successful_puts += 1
                staging = directory / ".puts"
                staging.write_text(f"{self.server.successful_puts}\n", encoding="ascii")
                staging.replace(receipts)
            super().send_response(code, message)

    relay = module.Relay(("127.0.0.1", 0), cache, key.encode("ascii"), tls)
    relay.RequestHandlerClass = RecordedRequest
    relay.successful_puts = 0
    return relay


def main():
    project = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--openssl", default=shutil.which("openssl") or "C:/Program Files/Git/usr/bin/openssl.exe")
    parser.add_argument("--keytool", default=shutil.which("keytool") or project.parent / "runtime/jdk-25/bin/keytool.exe")
    parser.add_argument("--ffmpeg", default=shutil.which("ffmpeg"))
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not command or not args.ffmpeg:
        parser.error("supply FFmpeg and the existing isolated native fixture command after --")
    spec = importlib.util.spec_from_file_location("local_tiktok_media_relay", project / "tools/tiktok_media_relay.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    work = project / "build/verification/tiktok-relay-fixture"
    work.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="run-", dir=work) as location:
        directory = Path(location)
        store, tls = configure(directory, args.openssl, args.keytool, args.ffmpeg)
        key = secrets.token_hex(32)
        with open_relay(module, directory, key, tls) as relay:
            origin = f"https://127.0.0.1:{relay.server_port}"
            (directory / "origin").write_text(origin, encoding="ascii")
            env = os.environ.copy()
            env["SKYWARS2_MEDIA_RELAY_URI"] = origin
            env["SKYWARS2_MEDIA_RELAY_KEY"] = key
            options = f'"-Dskywars2.tiktok.relay.fixture={directory}" "-Djavax.net.ssl.trustStore={store}"'
            options += " -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=fixture-only"
            env["JAVA_TOOL_OPTIONS"] = " ".join(filter(None, (env.get("JAVA_TOOL_OPTIONS"), options)))
            thread = threading.Thread(target=relay.serve_forever, name="fixture-loopback-relay", daemon=True)
            thread.start()
            try:
                return subprocess.run(command, cwd=project, env=env, check=False).returncode
            finally:
                relay.shutdown()
                thread.join()


if __name__ == "__main__":
    raise SystemExit(main())
