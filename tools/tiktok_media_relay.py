"""Bounded HTTPS delivery of pre-rendered phone clips on the Velocity host."""

import argparse
import hashlib
import hmac
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
import re
import shutil
import ssl
import tempfile
import threading
import time
import urllib.parse
import urllib.request


MAX_CLIP = 64 * 1024 * 1024
MAX_CACHE = 384 * 1024 * 1024
MAX_LEASE = 600
PREFIX = "/luckywars-media/v1/"
UPLOAD = re.compile(PREFIX + r"([0-9a-f]{64})")
DOWNLOAD = re.compile(PREFIX + r"([0-9a-f]{64})/([0-9]{1,12})/([0-9a-f]{64})")
SOURCE = re.compile(PREFIX + r"source/([0-9a-f]{64})")
CDN_HOSTS = ("tiktokcdn.com", "tiktokcdn-us.com", "tiktokcdn-eu.com", "tiktokcdn-in.com", "tiktokv.com", "muscdn.com")


def valid_source(url):
    if not isinstance(url, str) or len(url) > 4096 or not re.fullmatch(r"[\x21-\x7e]+", url):
        return False
    try:
        parsed = urllib.parse.urlsplit(url)
        host = parsed.hostname or ""
    except ValueError:
        return False
    return (parsed.scheme == "https" and parsed.netloc.lower() == host and not parsed.fragment
            and any(host == domain or host.endswith("." + domain) for domain in CDN_HOSTS)
            and re.fullmatch(r"/video/tos/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+/?", parsed.path) is not None)


class CdnRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        if not valid_source(newurl):
            raise ValueError("cdn_redirect")
        return super().redirect_request(request, fp, code, message, headers, newurl)


def signature(key, digest, expires):
    return hmac.new(key, f"{digest}/{expires}".encode("ascii"), hashlib.sha256).hexdigest()


class Relay(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, directory, key, tls=None):
        super().__init__(address, Request)
        self.directory = directory
        self.key = key
        self.tls = tls
        self.incoming = threading.Lock()
        self.concurrent = threading.BoundedSemaphore(8)
        self.next_prune = 0
        self.sources = {}
        self.sources_lock = threading.Lock()

    def process_request(self, request, address):
        if not self.concurrent.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, address)
        except BaseException:
            self.concurrent.release()
            raise

    def process_request_thread(self, request, address):
        try:
            request.settimeout(12)
            if self.tls is not None:
                try:
                    request = self.tls.wrap_socket(request, server_side=True)
                except OSError:
                    self.shutdown_request(request)
                    return
            super().process_request_thread(request, address)
        finally:
            self.concurrent.release()

    def service_actions(self):
        now = time.monotonic()
        with self.sources_lock:
            for identity, entry in list(self.sources.items()):
                if entry[1] <= time.time():
                    del self.sources[identity]
        if now >= self.next_prune and self.incoming.acquire(blocking=False):
            try:
                self.make_room(0)
                self.next_prune = now + 30
            finally:
                self.incoming.release()

    def clip(self, digest):
        return self.directory / (digest + ".clip")

    def materialize(self, identity):
        target = self.clip(identity)
        if target.is_file() and not target.is_symlink():
            return True
        with self.sources_lock:
            source = self.sources.get(identity)
        if source is None or source[1] <= time.time():
            return False
        with source[2]:
            if target.is_file() and not target.is_symlink():
                return True
            temporary = None
            try:
                request = urllib.request.Request(source[0], headers={"User-Agent": "Mozilla/5.0", "Accept": "video/*,*/*"})
                with urllib.request.build_opener(CdnRedirect()).open(request, timeout=12) as response:
                    if not valid_source(response.url):
                        return False
                    size = int(response.headers.get("Content-Length", "0"))
                    if size > MAX_CLIP:
                        return False
                    deadline, received = time.monotonic() + 60, 0
                    with tempfile.NamedTemporaryFile(dir=self.directory, prefix=".source-", delete=False) as output:
                        temporary = Path(output.name)
                        while True:
                            block = response.read(65536)
                            if not block:
                                break
                            received += len(block)
                            if received > MAX_CLIP or time.monotonic() > deadline:
                                return False
                            output.write(block)
                    if not received:
                        return False
                with self.incoming:
                    self.make_room(received)
                    temporary.replace(target)
                return True
            except (OSError, ValueError):
                return False
            finally:
                if temporary is not None:
                    temporary.unlink(missing_ok=True)

    def make_room(self, size):
        now = time.time()
        remaining = []
        for path in self.directory.glob("*.clip"):
            if not re.fullmatch(r"[0-9a-f]{64}\.clip", path.name) or path.is_symlink():
                continue
            try:
                stored = path.stat()
                if now - stored.st_mtime > MAX_LEASE:
                    path.unlink(missing_ok=True)
                else:
                    remaining.append((stored.st_mtime, stored.st_size, path))
            except FileNotFoundError:
                pass
        total = sum(length for _, length, _ in remaining)
        for _, length, path in sorted(remaining):
            if total + size <= MAX_CACHE:
                break
            path.unlink(missing_ok=True)
            total -= length


class Request(BaseHTTPRequestHandler):
    def do_POST(self):
        matched = SOURCE.fullmatch(self.path)
        if not matched:
            return self.empty(404)
        supplied = self.headers.get("Authorization", "").encode("utf-8")
        if not hmac.compare_digest(supplied, b"Bearer " + self.server.key):
            return self.empty(403)
        length = self.headers.get("Content-Length", "")
        if self.headers.get("Transfer-Encoding") or not re.fullmatch(r"[0-9]{1,5}", length) or not 0 < int(length) <= 8192:
            return self.empty(413)
        try:
            body = json.loads(self.rfile.read(int(length)))
            if not isinstance(body, dict) or set(body) != {"url", "expiresAt"}:
                return self.empty(400)
            url, expires = body["url"], body["expiresAt"]
            if (not valid_source(url) or type(expires) is not int or not time.time() < expires <= time.time() + MAX_LEASE
                    or hashlib.sha256(url.encode("utf-8")).hexdigest() != matched[1]):
                return self.empty(400)
        except (ValueError, TypeError):
            return self.empty(400)
        with self.server.sources_lock:
            for identity, source in list(self.server.sources.items()):
                if source[1] <= time.time():
                    del self.server.sources[identity]
            if matched[1] not in self.server.sources and len(self.server.sources) >= 128:
                return self.empty(429)
            old = self.server.sources.get(matched[1])
            self.server.sources[matched[1]] = (url, expires, old[2] if old else threading.Lock())
        return self.empty(201)

    def log_message(self, _format, *_args):
        # Signed paths and upload credentials stay out of access logs.
        pass

    def empty(self, code):
        self.send_response(code)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_PUT(self):
        matched = UPLOAD.fullmatch(self.path)
        if not matched:
            return self.empty(404)
        supplied = self.headers.get("Authorization", "").encode("utf-8")
        if not hmac.compare_digest(supplied, b"Bearer " + self.server.key):
            return self.empty(403)
        if self.headers.get("Transfer-Encoding"):
            return self.empty(400)
        raw_size = self.headers.get("Content-Length", "")
        if not re.fullmatch(r"[0-9]{1,9}", raw_size) or not 0 < int(raw_size) <= MAX_CLIP:
            return self.empty(413)
        if not self.server.incoming.acquire(blocking=False):
            return self.empty(429)
        target = self.server.clip(matched[1])
        size = int(raw_size)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=self.server.directory, prefix=".incoming-", delete=False) as out:
                temporary = Path(out.name)
                checksum = hashlib.sha256()
                deadline = time.monotonic() + 75
                while size:
                    if time.monotonic() >= deadline:
                        return self.empty(408)
                    block = self.rfile.read(min(size, 65536))
                    if not block:
                        return self.empty(400)
                    checksum.update(block)
                    out.write(block)
                    size -= len(block)
                if not hmac.compare_digest(checksum.hexdigest(), matched[1]):
                    return self.empty(422)
            self.server.make_room(int(raw_size))
            temporary.replace(target)
            self.empty(201)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
            self.server.incoming.release()

    def do_GET(self):
        matched = DOWNLOAD.fullmatch(self.path)
        if not matched:
            return self.empty(404)
        expires = int(matched[2])
        now = int(time.time())
        if not now < expires <= now + MAX_LEASE or not hmac.compare_digest(
                signature(self.server.key, matched[1], expires), matched[3]):
            return self.empty(403)
        target = self.server.clip(matched[1])
        if target.is_symlink():
            return self.empty(404)
        if not target.exists() and not self.server.materialize(matched[1]):
            return self.empty(404)
        try:
            stream = target.open("rb")
        except FileNotFoundError:
            return self.empty(404)
        with stream:
            self.send_response(200)
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Length", str(os.fstat(stream.fileno()).st_size))
            self.send_header("Cache-Control", "private, no-store")
            self.end_headers()
            shutil.copyfileobj(stream, self.wfile, length=65536)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", required=True, type=Path)
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8443)
    parser.add_argument("--certificate", type=Path)
    parser.add_argument("--private-key", type=Path)
    args = parser.parse_args()
    token = os.environ.get("SKYWARS2_MEDIA_RELAY_KEY", "")
    if not re.fullmatch(r"[0-9a-f]{64}", token):
        parser.error("SKYWARS2_MEDIA_RELAY_KEY must be a 64-character lowercase hex key")
    if bool(args.certificate) != bool(args.private_key):
        parser.error("the certificate and private key must be supplied together")
    if not args.certificate and args.bind != "127.0.0.1":
        parser.error("public listeners require a TLS certificate")
    args.directory.mkdir(parents=True, exist_ok=True, mode=0o700)
    tls = None
    if args.certificate:
        tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        tls.minimum_version = ssl.TLSVersion.TLSv1_2
        tls.load_cert_chain(args.certificate, args.private_key)
    with Relay((args.bind, args.port), args.directory, token.encode("ascii"), tls) as relay:
        relay.serve_forever()


if __name__ == "__main__":
    main()
