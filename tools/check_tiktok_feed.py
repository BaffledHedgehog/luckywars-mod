"""Local For You and CDN metadata fixtures with synthetic cookies only."""
import asyncio
from contextlib import asynccontextmanager, nullcontext
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import sys
import types
from unittest.mock import patch

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("worker", ROOT / "src/main/resources/skywars2/tiktok/worker.py")
worker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(worker)


def check_cdn_source():
    now = 1800000000
    clip = "22222222"
    cookie = "session-fixture-only-8ca5bd"
    cookies = [{"name": "sessionid", "value": cookie, "domain": ".tiktok.com", "path": "/", "secure": True}]
    origin = "https://v16-webapp.tiktokcdn-us.com/video/tos/test-region/test-clip/"
    url = origin + f"?expire={now + 7200}&x-signature=localA%2BB%2F5%3D&mime_type=video_mp4&tk=tt_chain_token"
    content = b"\0\0\0\x18ftypmp42fixture"

    def response(address=url, headers=None, history=()):
        return types.SimpleNamespace(
            status_code=200, history=history, headers={"Content-Length": str(len(content))},
            request=types.SimpleNamespace(url=address, headers=headers or {}),
            iter_content=lambda _: iter((content,)),
        )

    def selected(result):
        return worker.selected_cdn_source(clip, result, cookies, now)

    good = {"clipId": clip, "url": url, "expiresAt": now + 3600}
    assert selected(response(headers={"User-Agent": "fixture-client"})) == good
    assert selected(response(history=(object(),))) == good
    assert selected(response(origin + f"?x-expires={now + 42}"))["expiresAt"] == now + 42
    for invalid in (
        response(url.replace("https:", "http:")),
        response(url.replace("https://", "https://viewer@")),
        response(url.replace(".com/", ".com:443/")),
        response(url.replace(".com/", ".com.other.invalid/")),
        response(url.replace("v16-webapp.tiktokcdn-us.com", "www.tiktok.com")),
        response(url.replace("/test-clip/", "/%2e%2e/")),
        response(url.replace(str(now + 7200), str(now))),
        response(origin + "?x-signature=missing_expiration"),
        response(url + "&expires=1800008000"),
        response(url + "&sessionid=unknown-account-value"),
        response(url + "&user_agent=private-agent"),
        response(url + "&ft=" + cookie),
        response(url + "&ft=embedded%3Dcredential"),
        response(url + "&ft=embedded%253Dcredential"),
        response(url + "#private"),
        response(headers={"Cookie": "sessionid=fixture-only"}),
        response(headers={"Authorization": "fixture-only"}),
    ):
        assert selected(invalid) is None

    stage_parent = ROOT / "build/verification/tiktok"
    stage_parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=stage_parent) as scratch:
        stage = Path(scratch)
        request = types.SimpleNamespace(cookies=types.SimpleNamespace(set=lambda *args, **kwargs: None),
                                        get=lambda *args, **kwargs: nullcontext(response()))
        fake_requests = types.SimpleNamespace(Session=lambda: nullcontext(request))
        with patch.dict(sys.modules, {"requests": fake_requests}), patch.object(worker.time, "time", return_value=now):
            original, source = worker.download({"id": clip, "media": [url], "cookies": cookies},
                                               stage / "unused.json", stage)
        assert original.read_bytes() == content and source == good
        target = stage / "source.json"
        worker.atomic_json(target, source, private=True)
        assert json.loads(target.read_text(encoding="utf-8")) == good
        if sys.platform != "win32":
            assert target.stat().st_mode & 0o777 == 0o600
    print(json.dumps({"ok": True, "selectedClipAndExpiry": True, "credentialFree": True, "remoteRequests": 0}))


async def run():
    from playwright.async_api import async_playwright as real_playwright

    stage = ROOT / "build/verification/tiktok"
    stage.mkdir(parents=True, exist_ok=True)
    cookies = stage / "synthetic-cookies.json"
    cookies.write_text(json.dumps([{"domain": ".tiktok.com", "name": "sessionid", "value": "offline-fixture-only"}]))
    requests, launches = [], []
    authenticated = False
    async with real_playwright() as api:
        async def launch(**kwargs):
            browser = await api.chromium.launch(**kwargs)
            launches.append(kwargs.get("channel", "playwright-chromium"))

            async def new_context(**options):
                context = await browser.new_context(**options)

                async def route(request_route):
                    nonlocal authenticated
                    request = request_route.request
                    requests.append(request.url)
                    if "/api/recommend/item_list" in request.url:
                        authenticated = "sessionid=offline-fixture-only" in request.headers.get("cookie", "")
                        await request_route.fulfill(content_type="application/json", body=json.dumps({"itemList": [
                            {"id": "44444444", "video": {"ok": True}, "author": None},
                            {"id": "11111111", "video": {"ok": True}, "author": {"uniqueId": "already_seen"}},
                            {"id": "33333333", "video": {"ok": True}, "author": {"uniqueId": "bad/path"}},
                            {"id": "22222222", "video": {"ok": True}, "author": {"uniqueId": "fixture_author"}}
                        ]}))
                    elif request.url == "https://www.tiktok.com/foryou":
                        await request_route.fulfill(content_type="text/html", body='<script>fetch("/api/recommend/item_list/?count=5")</script>')
                    else:
                        await request_route.abort()

                await context.route("**/*", route)
                return context
            return types.SimpleNamespace(new_context=new_context, close=browser.close)

        @asynccontextmanager
        async def fake_playwright():
            yield types.SimpleNamespace(chromium=types.SimpleNamespace(launch=launch))

        try:
            with patch("playwright.async_api.async_playwright", fake_playwright):
                result = await worker.recommendations(cookies, {"11111111"})
            assert result["id"] == "22222222" and result["url"] == "https://www.tiktok.com/@fixture_author/video/22222222"
            assert "Chrome" in result["user_agent"]
            assert authenticated and len(requests) == 2
            proof = {"ok": True, "realBrowser": launches, "interceptedPageRequests": len(requests), "remotePageRequests": 0,
                     "authenticatedCookieApplied": authenticated, "excludedVideoSkipped": True, "invalidAuthorSkipped": True,
                     "actualAccountTest": False, "workerSha256": hashlib.sha256(Path(spec.origin).read_bytes()).hexdigest()}
            (stage / "feed-browser-results.json").write_text(json.dumps(proof, indent=2))
            print(json.dumps(proof))
        finally:
            cookies.unlink(missing_ok=True)


if __name__ == "__main__":
    if sys.argv[1:] == ["--offline-source"]:
        check_cdn_source()
    else:
        asyncio.run(run())
