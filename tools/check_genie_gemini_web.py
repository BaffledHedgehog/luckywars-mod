#!/usr/bin/env python3
"""Exercise Gemini's stateless repair/confirmation with the real program verifier."""
import asyncio
import copy
import json
import os
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

import genie_gemini_web as worker
from verify_genie_program import verify


class UsageLimitExceededError(Exception):
    pass


CONTRACT = {
    "language": "luckywars-item-script", "version": 1, "canonical": "unicode-utf16-bigdecimal-v2",
    "events": ["USE"], "api": {},
    "opcodes": {"CONST": {"width": 2, "pops": 0, "pushes": 1},
                "RETURN": {"width": 1, "pops": 1, "pushes": 0}},
    "limits": {"nodes": 10000, "depth": 64, "characters": 10000, "number_precision": 100,
               "number_scale": 100, "functions": 8, "parameters": 8, "instructions": 100,
               "collection": 100, "stack": 64},
}
PROGRAM = {"version": 1, "state": {}, "handlers": {"USE": "main"},
           "functions": {"main": {"parameters": ["event"], "code": [["CONST", True], ["RETURN"]]}}}
GRANT = {"decision": "grant", "item": {"icon": "minecraft:stick", "name": "Fixture", "description": "Fixture"},
         "answer": "Done", "twist": "Fixture", "program": PROGRAM}


class Client:
    def __init__(self, replies):
        self.replies = iter(replies)
        self.calls = []

    async def generate_content(self, message, **options):
        self.calls.append((message, options))
        reply = next(self.replies)
        if isinstance(reply, Exception):
            raise reply
        return types.SimpleNamespace(text=json.dumps(reply))


class GeminiProtocolChecks(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.previous = Path.cwd()
        os.chdir(self.directory.name)
        Path("contract.json").write_text(json.dumps(CONTRACT), encoding="utf-8")
        Path("prompt.txt").write_text("Original wish and constructor context", encoding="utf-8")
        Path("game_knowledge.json").write_text("{}", encoding="utf-8")
        Path("verify_genie_program.py").write_text("fixture attachment", encoding="utf-8")
        module = types.ModuleType("gemini_webapi.exceptions")
        module.UsageLimitExceededError = UsageLimitExceededError
        self.modules = patch.dict(sys.modules, {"gemini_webapi.exceptions": module})
        self.modules.start()
        self.receipt = verify(PROGRAM, CONTRACT)
        self.confirm = {"decision": "grant", "program_sha256": self.receipt["program_sha256"]}

    async def asyncTearDown(self):
        self.modules.stop()
        os.chdir(self.previous)
        self.directory.cleanup()

    async def test_grant_has_real_matching_receipt(self):
        client = Client([GRANT, self.confirm])
        reply = await worker.wish(client, "model")
        self.assertEqual(reply["receipt"], self.receipt)
        self.assertEqual(json.loads(reply["reply"])["receipt"], self.receipt)
        self.assertTrue(all(options["temporary"] and options["model"] == "model" for _, options in client.calls))

    async def test_accept_is_repaired_without_silently_accepting_alias(self):
        wrong = dict(GRANT, decision="accept")
        client = Client([wrong, GRANT, self.confirm])
        await worker.wish(client, "model")
        message, options = client.calls[1]
        self.assertIn("Original wish and constructor context", message)
        self.assertIn('decision must be the literal', message)
        self.assertIn('"accept"', message)
        self.assertEqual(len(options["files"]), 3)

    async def test_bad_program_gets_actual_verifier_error_and_full_context(self):
        bad = copy.deepcopy(GRANT)
        bad["program"]["functions"]["main"]["code"] = [["RETURN"]]
        client = Client([bad, GRANT, self.confirm])
        await worker.wish(client, "model")
        self.assertIn("operand stack underflow", client.calls[1][0])
        self.assertIn("Original wish and constructor context", client.calls[1][0])

    async def test_invalid_decision_never_grants(self):
        client = Client([dict(GRANT, decision="accept")] * 3)
        with self.assertRaises(worker.SessionFailure) as failure:
            await worker.wish(client, "model")
        self.assertEqual(failure.exception.issue, "INVALID_PROPOSAL")

    async def test_wrong_confirmation_never_grants(self):
        client = Client([GRANT, {"decision": "grant", "program_sha256": "wrong"}, {"decision": "grant", "program_sha256": "wrong2"}])
        with self.assertRaises(worker.SessionFailure) as failure:
            await worker.wish(client, "model")
        self.assertEqual(failure.exception.issue, "INVALID_CONFIRMATION")
        self.assertIn(self.receipt["program_sha256"], client.calls[1][0])
        self.assertIn('"functions"', client.calls[1][0])

    async def test_semantic_review_can_reject_a_syntactically_valid_grant(self):
        client = Client([GRANT, {"decision": "reject"}])
        result = await worker.wish(client, "model")
        self.assertEqual(json.loads(result["reply"]), {"decision": "reject"})
        self.assertNotIn("receipt", result)
        self.assertIn("Original wish and constructor context", client.calls[1][0])
        self.assertIn("NOT gameplay correctness", client.calls[1][0])
        self.assertEqual(len(client.calls[1][1]["files"]), 3)

    async def test_semantic_repair_gets_its_own_verified_hash(self):
        repaired = copy.deepcopy(GRANT)
        repaired["program"]["functions"]["main"]["code"] = [["CONST", False], ["RETURN"]]
        expected = verify(repaired["program"], CONTRACT)
        client = Client([GRANT, repaired, {"decision": "grant", "program_sha256": expected["program_sha256"]}])
        result = await worker.wish(client, "model")
        self.assertEqual(result["receipt"], expected)
        self.assertNotEqual(expected["program_sha256"], self.receipt["program_sha256"])

    async def test_rejection_and_quota_do_not_retry_or_forge_grant(self):
        client = Client([{"decision": "reject"}])
        self.assertEqual(json.loads((await worker.wish(client, "model"))["reply"]), {"decision": "reject"})
        self.assertEqual(len(client.calls), 1)
        client = Client([UsageLimitExceededError()])
        with self.assertRaises(UsageLimitExceededError):
            await worker.wish(client, "model")
        self.assertEqual(len(client.calls), 1)


if __name__ == "__main__":
    unittest.main()
