"""Real headless browser, local intercepted For You response, synthetic cookies only."""
import asyncio
from contextlib import asynccontextmanager
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import types
from unittest.mock import patch

sys.dont_write_bytecode = True
from playwright.async_api import async_playwright as real_playwright

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("worker", ROOT / "src/main/resources/skywars2/tiktok/worker.py")
worker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(worker)


async def run():
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
    asyncio.run(run())
