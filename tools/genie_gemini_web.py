"""Gemini web transport. Credentials are read only from stdin."""

import asyncio
import json
import os
from pathlib import Path
import sys
import tempfile


def response_text(output):
    raw = output.text.strip()
    if raw.startswith(('```json\n', '```\n')) and raw.endswith('```'):
        return raw.split('\n', 1)[1][:-3].strip()
    return raw


async def wish(client):
    from verify_genie_program import canonical, parse, verify

    contract = parse(Path("contract.json").read_text(encoding="utf-8"))
    prompt = Path("prompt.txt").read_text(encoding="utf-8")
    chat = client.start_chat(model="gemini-pro")
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


async def run(cookie):
    from loguru import logger
    logger.remove()
    from gemini_webapi import GeminiClient

    client = GeminiClient(cookie["__Secure-1PSID"], cookie["__Secure-1PSIDTS"])
    try:
        await client.init(timeout=35, watchdog_timeout=35, auto_refresh=False)
        if Path("prompt.txt").is_file():
            return await wish(client)
        check = await client.generate_content("Reply with exactly the letters OK.", model="gemini-pro", temporary=True)
        if check.text.strip() != "OK":
            raise ValueError("Gemini session check failed.")
        return {"connected": True}
    finally:
        await client.close()


def main():
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    sys.dont_write_bytecode = True
    sys.path.insert(1, sys.argv[1])
    with tempfile.TemporaryDirectory(prefix="gemini-session-", dir=".") as cache:
        os.environ["GEMINI_COOKIE_PATH"] = str(Path(cache).resolve())
        print(json.dumps(asyncio.run(asyncio.wait_for(run(json.load(sys.stdin)), 165)), ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Neither local credentials nor remote error bodies should reach process output.
        raise SystemExit(1) from None
