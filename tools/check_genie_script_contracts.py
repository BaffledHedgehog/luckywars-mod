#!/usr/bin/env python3
"""Cross-check the cloud verifier against the real Java compiler, including malformed JSON."""
import argparse
import copy
import json
from pathlib import Path
import subprocess
import verify_genie_program as verifier


def program(code=None):
    return {"version": 1, "state": {}, "handlers": {"USE": "main"},
            "functions": {"main": {"parameters": ["event"], "code": code or []}}}


def check(java, classpath, output):
    command = [java, "-Xmx128m", "-cp", classpath, "me.skywars2.game.genie.script.GenieScriptContracts"]
    subprocess.run(command, check=True)
    contract = verifier.parse(subprocess.check_output(command + ["--contract"], encoding="utf-8"))
    cases = []

    def add(name, value, accepted):
        cases.append((name, value if isinstance(value, str) else json.dumps(value, ensure_ascii=True), accepted))

    add("empty handler", program(), True)
    add("unbounded cooperative loop", program([["label", "again"], ["jump", "again"]]), True)
    add("power is the model's decision", program([["const", "damage @e[limit=1] 1000000"],
                                                ["call", "world.command", 1], ["return"]]), True)
    unicode = program()
    unicode["state"] = {"😀": "ё\n\u0001", "\ue000": "\\\"/", "negative_zero": 0}
    add("Unicode canonical order", unicode, True)
    add("exact decimals", json.dumps(program()).replace('"state": {}',
        '"state":{"n":1.2300e2,"z":-0.000,"large":1e4096,"tiny":1e-4096}'), True)
    add("decimal notation thresholds", json.dumps(program()).replace('"state": {}',
        '"state":{"a":1000.00,"b":0.000001,"c":0.0000001,"d":1.230000e-6,"e":-123000e10}'), True)
    compact = json.dumps(program()).replace('"state": {}', '"state":{"values":[' + ",".join(["1e4096"] * 1000) + "]}")
    add("compact exponents remain compact", compact, True)
    assert len(verifier.canonical(verifier.parse(compact))) < 20000, "canonical output expanded compact exponents"
    for op, definition in contract["opcodes"].items():
        if op in {"LOAD", "STORE", "JUMP", "JUMP_IF_FALSE", "CALL"}:
            operand = "event" if op == "LOAD" else "local" if op == "STORE" else "end"
            row = [op, operand]
        elif op == "CONST":
            row = [op, None]
        elif op in {"ARRAY", "OBJECT"}:
            row = [op, 2]
        else:
            row = [op]
        if op == "CALL":
            row = [op, "world.command", 1]
        prefix = [["const", i] for i in range(5)]
        add("opcode " + op, program(prefix + [row, ["label", "end"]]), True)
        if definition["pops"] not in (0,):
            add("underflow " + op, program([row, ["label", "end"]]), False)
    invalid = [
        ("missing label", [["jump", "absent"]]),
        ("duplicate label", [["label", "same"], ["label", "same"]]),
        ("undeclared local", [["load", "missing"]]),
        ("reserved state store", [["const", {}], ["store", "state"]]),
        ("missing host", [["call", "world.absent", 0]]),
        ("arity", [["const", 1], ["const", 2], ["call", "world.command", 2]]),
        ("wrong opcode width", [["const", 1, 2]]),
        ("negative collection", [["array", -1]]),
        ("large collection", [["array", 65537]]),
        ("non-integral collection", [["array", 1.5]]),
        ("unknown opcode", [["execute_java"]]),
        ("bad name", [["store", "not a name"]]),
        ("control-flow merge", [["const", True], ["jump_if_false", "end"], ["const", 1], ["label", "end"]]),
    ]
    for name, code in invalid:
        add(name, program(code), False)
    for name, change in [
        ("extra root field", lambda p: p.update(extra=1)),
        ("no event", lambda p: p.update(handlers={})),
        ("unknown event", lambda p: p.update(handlers={"DEATH": "main"})),
        ("unknown handler", lambda p: p.update(handlers={"USE": "missing"})),
        ("handler arity", lambda p: p["functions"]["main"].update(parameters=[])),
        ("duplicate parameter", lambda p: p["functions"]["main"].update(parameters=["a", "a"])),
        ("reserved parameter", lambda p: p["functions"]["main"].update(parameters=["state"])),
        ("native shadow", lambda p: p["functions"].update({"world.command": {"parameters": [], "code": []}})),
        ("bad version boolean", lambda p: p.update(version=True)),
    ]:
        value = program()
        change(value)
        add(name, value, False)
    raw = json.dumps(program())
    for name, source in [
        ("duplicate key", raw.replace('"version": 1', '"version":1,"version":1')),
        ("unpaired surrogate", raw.replace('"state": {}', '"state":{"x":"\\ud800"}')),
        ("NaN", raw.replace('"state": {}', '"state":{"x":NaN}')),
        ("Infinity", raw.replace('"state": {}', '"state":{"x":Infinity}')),
        ("large exponent", raw.replace('"state": {}', '"state":{"x":1e4097}')),
        ("unstable normalized exponent", raw.replace('"state": {}', '"state":{"x":10000000000000000e4096}')),
        ("trailing document", raw + "{}"),
        ("lenient quotes", raw.replace('"version"', "'version'")),
    ]:
        add(name, source, False)
    recursive = program([["const", 2], ["call", "recursive", 1], ["return"]])
    recursive["functions"]["recursive"] = {"parameters": ["n"], "code": [
        ["load", "n"], ["const", 0], ["le"], ["jump_if_false", "recurse"], ["const", 0], ["return"],
        ["label", "recurse"], ["load", "n"], ["const", 1], ["sub"], ["call", "recursive", 1], ["return"]]}
    add("recursive user function", recursive, True)
    replies = subprocess.run(command + ["--verify-lines"], input="\n".join(row[1] for row in cases) + "\n",
                             text=True, encoding="utf-8", capture_output=True, check=True).stdout.splitlines()
    # Low-priority wrappers may emit their own prefix; only the child emits JSON records.
    replies = [json.loads(line) for line in replies if line.startswith("{")]
    assert len(replies) == len(cases), (len(replies), len(cases))
    for (name, source, expected), native in zip(cases, replies):
        try:
            cloud = verifier.verify(verifier.parse(source), contract)
        except (ValueError, TypeError, KeyError, OverflowError, RecursionError) as error:
            cloud = {"ok": False, "error": str(error)}
        assert native["ok"] == cloud["ok"] == expected, (name, expected, native, cloud)
        if expected:
            assert native["program_sha256"] == cloud["program_sha256"], (name, native, cloud)
    output.mkdir(parents=True, exist_ok=True)
    (output / "contract.json").write_text(verifier.canonical(contract), encoding="utf-8")
    sample = program([["const", "damage @e[limit=1] 1000000"], ["call", "world.command", 1], ["return"]])
    sample["state"] = {"verification_probe": "model-judges-balance-not-json-handler"}
    (output / "program.json").write_text(verifier.canonical(sample), encoding="utf-8")
    receipt = verifier.verify(sample, contract)
    (output / "local-receipt.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    report = {"cases": len(cases), "pass": True, "contract_sha256": verifier.digest(contract),
              "verifier_sha256": __import__("hashlib").sha256(Path(verifier.__file__).read_bytes()).hexdigest()}
    (output / "parity-results.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print("GENIE_CLOUD_VERIFIER_PARITY_PASS " + json.dumps(report))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", required=True)
    parser.add_argument("--classpath", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    check(args.java, args.classpath, args.output)
