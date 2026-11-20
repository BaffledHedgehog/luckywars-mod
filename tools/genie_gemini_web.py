"""Gemini web transport. Credentials are read only from stdin."""

import asyncio
import json
import os
from pathlib import Path
import secrets
import sys
import tempfile
from urllib.parse import urlsplit


class SessionFailure(Exception):
    def __init__(self, issue):
        super().__init__(issue)
        self.issue = issue


def request_issue(error):
    if isinstance(error, SessionFailure):
        return error.issue
    if isinstance(error, ImportError):
        return "DEPENDENCY_UNAVAILABLE"
    if isinstance(error, TimeoutError):
        return "NETWORK_TIMEOUT"

    from curl_cffi.requests.exceptions import RequestException, Timeout as CurlTimeout
    from gemini_webapi.exceptions import AuthError, TemporarilyBlockedError
    from gemini_webapi.exceptions import TimeoutError as GeminiTimeout

    if isinstance(error, AuthError):
        return "UNAUTHENTICATED"
    if isinstance(error, TemporarilyBlockedError):
        return "ACCESS_TEMPORARILY_UNAVAILABLE"
    if isinstance(error, (CurlTimeout, GeminiTimeout)):
        return "NETWORK_TIMEOUT"
    if not isinstance(error, RequestException):
        return None
    response = error.response
    if response is not None:
        status = response.status_code
        if status == 401:
            return "UNAUTHENTICATED"
        if status == 429:
            return "ACCESS_TEMPORARILY_UNAVAILABLE"
        if 300 <= status < 400:
            # A proxy may reject the redirected host and leave the original response on the error.
            try:
                target = urlsplit(response.headers.get("location", ""))
                path = target.path.casefold()
                if target.scheme == "https" and target.hostname == "accounts.google.com" and (
                    path == "/servicelogin" or path == "/signin" or path.startswith(("/signin/", "/v3/signin/"))
                ):
                    return "UNAUTHENTICATED"
            except ValueError:
                pass
    return "NETWORK_ERROR"


def response_text(output):
    raw = output.text.strip()
    if raw.startswith(('```json\n', '```\n')) and raw.endswith('```'):
        return raw.split('\n', 1)[1][:-3].strip()
    return raw


async def wish(client, model):
    from verify_genie_program import canonical, parse, verify

    contract = parse(Path("contract.json").read_text(encoding="utf-8"))
    prompt = Path("prompt.txt").read_text(encoding="utf-8")
    chat = client.start_chat(model=model)
    proposal = await chat.send_message(
        prompt + """

For this web session, use the server verifier tool through the following protocol.
In your first reply send either {"decision":"reject"} or the proposed grant JSON
with decision, item, answer, twist and program. Omit receipt until the server
validates the exact proposal and sends the verifier output back to this chat.
If validation fails, correct the proposal and return the complete corrected JSON.
After a verified grant, reply with only {"decision":"grant","receipt":<the exact receipt>}.
""",
        files=[Path(name) for name in ("game_knowledge.json", "contract.json", "verify_genie_program.py")],
        temporary=True,
    )
    for attempt in range(3):
        raw = response_text(proposal)
        try:
            reply = parse(raw)
            if reply == {"decision": "reject"}:
                return {"reply": raw}
            if set(reply) != {"decision", "item", "answer", "twist", "program"} or reply["decision"] != "grant":
                raise ValueError("Expected the five proposed grant fields, without a receipt.")
            receipt = verify(reply["program"], contract)
        except (ValueError, TypeError, KeyError, OverflowError, RecursionError) as error:
            if attempt == 2:
                raise
            proposal = await chat.send_message(
                "The verifier rejected the proposal. Repair the JSON and try again. Error: "
                + json.dumps(str(error)[:500], ensure_ascii=True),
                temporary=True,
            )
            continue

        confirmation = await chat.send_message(
            "The verifier accepted the exact program. Reply with just this JSON as the final confirmation:\n"
            + json.dumps({"decision": "grant", "receipt": receipt}),
            temporary=True,
        )
        if parse(response_text(confirmation)) != {"decision": "grant", "receipt": receipt}:
            raise ValueError("Gemini did not confirm the verified program.")
        reply["receipt"] = receipt
        return {"reply": canonical(reply), "receipt": receipt}
    raise ValueError("Gemini did not provide a valid program.")


async def probe_attachment(client, model):
    from gemini_webapi.exceptions import UsageLimitExceededError

    marker = "GENIE_" + secrets.token_hex(8)
    document = Path("session_check.json")
    document.write_text(json.dumps({"marker": marker}), encoding="utf-8")
    try:
        check = await client.generate_content(
            "Read the attached JSON and reply with only the exact value of the marker field.",
            files=[document], model=model, temporary=True,
        )
    except UsageLimitExceededError:
        raise
    except Exception as error:
        raise SessionFailure(request_issue(error) or "ATTACHMENT_FAILED") from error
    # Gemini may format the answer. The unpredictable marker proves it read the attachment.
    if marker not in response_text(check):
        raise SessionFailure("ATTACHMENT_FAILED")
    return {"connected": True}


async def refresh(cookie, proxy):
    from curl_cffi.requests import AsyncSession, Cookies
    from gemini_webapi.exceptions import AuthError
    from gemini_webapi.utils import rotate_1psidts

    jar = Cookies()
    for name in ("__Secure-1PSID", "__Secure-1PSIDTS"):
        jar.set(name, cookie[name], domain=".google.com", secure=True)
    try:
        async with AsyncSession(cookies=jar, proxy=proxy, timeout=35) as session:
            timestamp = await rotate_1psidts(session)
    except AuthError as error:
        raise SessionFailure("UNAUTHENTICATED") from error
    return {"timestamp": timestamp}


def select_pro_model(client):
    for name in ("gemini-carbon", "gemini-pro"):
        try:
            model = client.resolve_model(name)
        except ValueError:
            continue
        if model.is_available:
            return model
    raise SessionFailure("PRO_UNAVAILABLE")


async def run(cookie, proxy):
    from curl_cffi.requests import Cookies
    from loguru import logger
    logger.remove()
    from gemini_webapi import GeminiClient
    from gemini_webapi.constants import AccountStatus
    from gemini_webapi.exceptions import UsageLimitExceededError

    client = GeminiClient(cookie["__Secure-1PSID"], cookie["__Secure-1PSIDTS"], proxy=proxy)
    if context := cookie.get("cookies"):
        session = Cookies()
        for row in context:
            name = row["name"]
            domain = row["domain"]
            # curl_cffi strips the domain from __Host- cookies when rebuilding the jar.
            if name.startswith("__Host-") or domain.lstrip(".") not in ("google.com", "gemini.google.com"):
                continue
            session.set(name, row["value"], domain=domain, path=row["path"], secure=row["secure"])
        client.cookies = session
    try:
        await client.init(timeout=35, watchdog_timeout=35, auto_refresh=False)
        if client.account_status == AccountStatus.UNAUTHENTICATED:
            raise SessionFailure("UNAUTHENTICATED")
        if client.account_status == AccountStatus.LOCATION_REJECTED:
            raise SessionFailure("LOCATION_REJECTED")
        if client.account_status in (AccountStatus.TOS_PENDING, AccountStatus.TOS_OUT_OF_DATE):
            raise SessionFailure("TERMS_REQUIRED")
        if client.account_status == AccountStatus.ACCOUNT_UNTRUSTED:
            raise SessionFailure("ACCOUNT_UNTRUSTED")
        if client.account_status == AccountStatus.ACCESS_TEMPORARILY_UNAVAILABLE:
            raise SessionFailure("ACCESS_TEMPORARILY_UNAVAILABLE")
        if client.account_status != AccountStatus.AVAILABLE:
            raise SessionFailure("ACCESS_RESTRICTED")
        model = select_pro_model(client)
        if Path("prompt.txt").is_file():
            # Keep the interactive turn alive while Gemini is working. The Java
            # owner interrupts this dedicated process on logout/death/match reset.
            client.client.timeout = 0
            client.timeout = float("inf")
            client.watchdog_timeout = float("inf")
            return await wish(client, model)
        return await probe_attachment(client, model)
    except UsageLimitExceededError as error:
        raise SessionFailure("QUOTA_EXHAUSTED") from error
    finally:
        await client.close()


def main():
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    sys.dont_write_bytecode = True
    sys.path.insert(1, sys.argv[1])
    with tempfile.TemporaryDirectory(prefix="gemini-session-", dir=".") as cache:
        os.environ["GEMINI_COOKIE_PATH"] = str(Path(cache).resolve())
        cookie = json.load(sys.stdin)
        proxy = os.environ.get("SKYWARS2_GENIE_GEMINI_WEB_PROXY") or None
        refresh_only = sys.argv[2:] == ["refresh"]
        if refresh_only:
            from loguru import logger
            logger.remove()
        request = refresh(cookie, proxy) if refresh_only else run(cookie, proxy)
        if refresh_only or not Path("prompt.txt").is_file():
            request = asyncio.wait_for(request, 65)
        print(json.dumps(asyncio.run(request), ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        issue = request_issue(error)
        if issue is None:
            # Neither local credentials nor remote error bodies should reach process output.
            raise SystemExit(1) from None
        print(json.dumps({"session_error": issue}))
