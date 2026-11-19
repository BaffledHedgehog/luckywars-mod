"""Gemini web transport. Credentials are read only from stdin."""

import asyncio
import json
import os
from pathlib import Path
import secrets
import sys
import tempfile


class SessionFailure(Exception):
    def __init__(self, issue):
        super().__init__(issue)
        self.issue = issue


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
    marker = "GENIE_" + secrets.token_hex(8)
    document = Path("session_check.json")
    document.write_text(json.dumps({"marker": marker}), encoding="utf-8")
    try:
        check = await client.generate_content(
            "Read the attached JSON and reply with only the exact value of the marker field.",
            files=[document], model=model, temporary=True,
        )
    except Exception as error:
        raise SessionFailure("ATTACHMENT_FAILED") from error
    # Gemini may format the answer. The unpredictable marker proves it read the attachment.
    if marker not in response_text(check):
        raise SessionFailure("ATTACHMENT_FAILED")
    return {"connected": True}


async def refresh(cookie):
    from curl_cffi.requests import AsyncSession, Cookies
    from gemini_webapi.exceptions import AuthError
    from gemini_webapi.utils import rotate_1psidts

    jar = Cookies()
    for name in ("__Secure-1PSID", "__Secure-1PSIDTS"):
        jar.set(name, cookie[name], domain=".google.com", secure=True)
    try:
        async with AsyncSession(cookies=jar, timeout=35) as session:
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


async def run(cookie):
    from loguru import logger
    logger.remove()
    from gemini_webapi import GeminiClient
    from gemini_webapi.constants import AccountStatus

    client = GeminiClient(cookie["__Secure-1PSID"], cookie["__Secure-1PSIDTS"])
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
        refresh_only = sys.argv[2:] == ["refresh"]
        if refresh_only:
            from loguru import logger
            logger.remove()
        request = refresh(cookie) if refresh_only else run(cookie)
        if refresh_only or not Path("prompt.txt").is_file():
            request = asyncio.wait_for(request, 65)
        print(json.dumps(asyncio.run(request), ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except SessionFailure as error:
        print(json.dumps({"session_error": error.issue}))
    except Exception:
        # Neither local credentials nor remote error bodies should reach process output.
        raise SystemExit(1) from None
