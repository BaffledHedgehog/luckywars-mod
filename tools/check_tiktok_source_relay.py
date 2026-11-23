"""Real HTTP source leases and old PUT/GET; external CDN bodies use a deterministic fixture."""
import concurrent.futures
import hashlib
import http.client
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import threading
import time
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("media_relay", ROOT / "tools/tiktok_media_relay.py")
relay = importlib.util.module_from_spec(spec)
spec.loader.exec_module(relay)


def main():
    key = b"fixture-only-key"
    url = "https://v16-webapp.tiktokcdn-us.com/video/tos/test/clip/?expire=1999999999"
    identity = hashlib.sha256(url.encode()).hexdigest()
    content = b"fixture MP4 body" * 64
    requests = []

    class Body(io.BytesIO):
        headers = {"Content-Length": str(len(content))}
        url = "https://v16-webapp.tiktokcdn-us.com/video/tos/test/clip/?expire=1999999999"

    class Origin:
        def open(self, request, **options):
            requests.append(request)
            time.sleep(.03)
            return Body(content)

    with tempfile.TemporaryDirectory(prefix="phone-relay-") as temporary:
        with relay.Relay(("127.0.0.1", 0), Path(temporary), key) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()

            def call(method, path, body=None, authorized=False):
                connection = http.client.HTTPConnection("127.0.0.1", server.server_port, timeout=5)
                try:
                    headers = {"Authorization": "Bearer " + key.decode()} if authorized else {}
                    connection.request(method, path, body=body, headers=headers)
                    response = connection.getresponse()
                    return response.status, response.read()
                finally:
                    connection.close()

            try:
                lease = int(time.time()) + 300
                source_path = relay.PREFIX + "source/" + identity
                body = json.dumps({"url": url, "expiresAt": lease}).encode()
                assert call("POST", source_path, body)[0] == 403
                with patch.object(relay.urllib.request, "build_opener", return_value=Origin()):
                    assert call("POST", source_path, body, True)[0] == 201
                    assert requests == [] and not server.clip(identity).exists(), "registration must never fetch video"
                    signed = relay.PREFIX + f"{identity}/{lease}/{relay.signature(key, identity, lease)}"
                    with concurrent.futures.ThreadPoolExecutor(2) as clients:
                        results = list(clients.map(lambda _: call("GET", signed), range(2)))
                    assert results == [(200, content), (200, content)] and len(requests) == 1
                    assert all(name.lower() not in ("cookie", "authorization") for name in requests[0].headers)
                    assert call("GET", signed)[1] == content and len(requests) == 1
                assert call("GET", signed[:-1] + ("a" if signed[-1] != "a" else "b"))[0] == 403
                for address in ("https://[", "http://example.com/video/tos/a", "https://tiktokcdn.com.evil.test/video/tos/a", "https://user@tiktokcdn.com/video/tos/a"):
                    invalid = json.dumps({"url": address, "expiresAt": lease})
                    assert call("POST", source_path, invalid, True)[0] == 400
                assert call("POST", source_path, b"x" * 8193, True)[0] == 413
                expired = json.dumps({"url": url, "expiresAt": int(time.time()) - 1})
                assert call("POST", source_path, expired, True)[0] == 400
                old_body = b"prepared old client media"
                digest = hashlib.sha256(old_body).hexdigest()
                assert call("PUT", relay.PREFIX + digest, old_body, True)[0] == 201
                old_signed = relay.PREFIX + f"{digest}/{lease}/{relay.signature(key, digest, lease)}"
                assert call("GET", old_signed) == (200, old_body)
                print("PASS source relay: authenticated metadata only, lazy single CDN fetch, exact parallel/cache bytes, bounded URLs/lease/body, unchanged PUT/signed GET; no external requests")
            finally:
                server.shutdown()
                thread.join()


if __name__ == "__main__":
    main()
