#!/usr/bin/env python3
"""Cloud/local technical verifier for LuckyWars item programs. No wish-balance policy.

Usage: python verify_genie_program.py contract.json program.json
Import verify(program, contract) when producing several candidates in a cloud code tool.
The contract is exported by the actual Java compiler. Never edit it to accept a program.
An ok receipt is useful only with an observed code-execution trace; it is not proof by itself.
"""
from collections import deque
from decimal import Decimal
import base64
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import sys

VERIFIER_VERSION = "luckywars-script-1.2.0"
NAME = re.compile(r"[A-Za-z_][A-Za-z0-9_.:]*\Z")


def require(ok, message):
    if not ok:
        raise ValueError(message)


def pairs(entries):
    result = {}
    for key, value in entries:
        require(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def parse(source):
    require(len(source.encode("utf-16-le")) // 2 <= 4194304, "JSON input exceeds storage bounds")
    return json.loads(source, object_pairs_hook=pairs, parse_int=Decimal, parse_float=Decimal,
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError("non-finite JSON number")))


def identifier(value):
    require(isinstance(value, str) and len(value) <= 128 and NAME.fullmatch(value),
            "invalid identifier")
    return value


def integer(value):
    require(not isinstance(value, bool) and isinstance(value, (int, Decimal)), "integer expected")
    require(value == int(value) and -(2**31) <= value < 2**31, "integer outside native range")
    return int(value)


def text_units(value):
    try:
        return len(value.encode("utf-16-le")) // 2
    except UnicodeError as error:
        raise ValueError("unpaired Unicode surrogate") from error


def verify_structure(archive):
    require(len(archive) <= 262148, "compressed structure exceeds storage bounds")
    try:
        with gzip.GzipFile(fileobj=io.BytesIO(base64.b64decode(archive[4:], validate=True))) as gzip_file:
            data = gzip_file.read(2 * 1024 * 1024 + 1)
    except (OSError, EOFError, ValueError) as error:
        raise ValueError("invalid compressed structure") from error
    require(len(data) <= 2 * 1024 * 1024, "structure exceeds decoded storage bounds")
    source = io.BytesIO(data)
    tags = 0

    def read(fmt):
        length = struct.calcsize(fmt)
        raw = source.read(length)
        require(len(raw) == length, "truncated structure NBT")
        return struct.unpack(fmt, raw)[0]

    def text():
        length = read(">H")
        value = source.read(length)
        require(len(value) == length, "truncated structure text")
        return value.decode("utf-8")

    def payload(kind, depth):
        nonlocal tags
        tags += 1
        require(depth <= 24 and tags <= 100000, "structure NBT exceeds complexity bounds")
        formats = {1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}
        if kind in formats:
            return read(formats[kind])
        if kind == 8:
            return text()
        if kind == 9:
            child, count = read(">B"), read(">i")
            require(0 <= count <= 4096, "structure list exceeds bounds")
            return [payload(child, depth + 1) for _ in range(count)]
        if kind == 10:
            fields = {}
            while child := read(">B"):
                key = text()
                require(key not in fields, "duplicate structure NBT key")
                fields[key] = payload(child, depth + 1)
            return fields
        if kind in (7, 11, 12):
            count = read(">i")
            require(0 <= count <= 4096, "structure array exceeds bounds")
            result = source.read(count * {7: 1, 11: 4, 12: 8}[kind])
            require(len(result) == count * {7: 1, 11: 4, 12: 8}[kind], "truncated structure array")
            return result
        raise ValueError("unsupported structure NBT tag")

    require(read(">B") == 10, "structure root must be an NBT compound")
    text()
    root = payload(10, 0)
    require(not source.read(1), "trailing structure data")
    size = root.get("size")
    require(isinstance(size, list) and len(size) == 3 and all(isinstance(axis, int) and 1 <= axis <= 64 for axis in size),
            "invalid structure dimensions")
    require(root.get("entities", []) == [], "entities are outside the structure API")
    palette = root.get("palette")
    if palette is None and root.get("palettes"):
        palette = root["palettes"][0]
    require(isinstance(palette, list) and 0 < len(palette) <= 4096, "invalid structure palette")
    control_blocks = {"command_block", "chain_command_block", "repeating_command_block", "structure_block", "jigsaw"}
    for entry in palette:
        require(isinstance(entry, dict), "structure palette entry must be a compound")
        name = entry.get("Name", entry.get("id"))
        require(isinstance(name, str) and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+", name), "invalid structure block identifier")
        require(name not in {"minecraft:" + block for block in control_blocks}, "control blocks cannot be placed")
        properties = entry.get("Properties", entry.get("properties", {}))
        require(isinstance(properties, dict) and all(re.fullmatch(r"[a-z0-9_]+", key) and isinstance(value, str)
                and re.fullmatch(r"[a-z0-9_-]+", value) for key, value in properties.items()), "invalid structure block property")
    blocks = root.get("blocks")
    require(isinstance(blocks, list) and 0 < len(blocks) <= 4096, "invalid structure blocks")
    seen = set()
    for block in blocks:
        require(isinstance(block, dict) and "nbt" not in block, "block entity payloads are outside the structure API")
        index, pos = block.get("state"), block.get("pos")
        require(isinstance(index, int) and 0 <= index < len(palette), "invalid structure palette index")
        require(isinstance(pos, list) and len(pos) == 3 and all(isinstance(axis, int) and 0 <= axis < bound
                for axis, bound in zip(pos, size)), "invalid structure block position")
        require(tuple(pos) not in seen, "duplicate structure block position")
        seen.add(tuple(pos))


def check_data(root, limits):
    pending = [(root, 0, False)]
    active = set()
    structures = set()
    nodes = characters = 0
    while pending:
        value, depth, exiting = pending.pop()
        if exiting:
            active.remove(id(value))
            continue
        nodes += 1
        require(nodes <= limits["nodes"] and depth <= limits["depth"], "script data exceeds memory bounds")
        if isinstance(value, (dict, list)):
            require(id(value) not in active, "cyclic script value")
            active.add(id(value))
            pending.append((value, depth, True))
            if isinstance(value, dict):
                for key, child in value.items():
                    require(isinstance(key, str), "JSON object key must be text")
                    characters += text_units(key)
                    pending.append((child, depth + 1, False))
            else:
                pending.extend((child, depth + 1, False) for child in value)
        elif isinstance(value, str):
            characters += text_units(value)
            if value.startswith("nbt:") and value not in structures:
                verify_structure(value)
                structures.add(value)
        elif isinstance(value, (int, Decimal)) and not isinstance(value, bool):
            number = Decimal(value)
            require(number.is_finite(), "non-finite number")
            parts = number.as_tuple()
            require(len(parts.digits) <= limits["number_precision"] and
                    abs(parts.exponent) <= limits["number_scale"], "numeric storage exceeds memory bounds")
            compact_exponent = parts.exponent
            if number:
                for digit in reversed(parts.digits):
                    if digit:
                        break
                    compact_exponent += 1
            else:
                compact_exponent = 0
            require(abs(compact_exponent) <= limits["number_scale"], "numeric storage exceeds memory bounds")
        else:
            require(value is None or isinstance(value, bool), "not a JSON value")
        require(characters <= limits["characters"], "script text exceeds memory bounds")


def canonical(value):
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, str):
        text_units(value)
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    if isinstance(value, dict):
        keys = sorted(value, key=lambda key: key.encode("utf-16-be"))
        return "{" + ",".join(canonical(key) + ":" + canonical(value[key]) for key in keys) + "}"
    if isinstance(value, list):
        return "[" + ",".join(canonical(child) for child in value) + "]"
    number = Decimal(value)
    require(number.is_finite(), "non-finite number")
    if not number:
        return "0"
    # Build a normalized tuple without Decimal.normalize(), whose context can round.
    sign, digits, exponent = number.as_tuple()
    digits = list(digits)
    while digits[-1] == 0:
        digits.pop()
        exponent += 1
    return str(Decimal((sign, tuple(digits), exponent)))


def digest(value):
    return hashlib.sha256(canonical(value).encode("utf-8")).hexdigest()


def compile_function(parameters, source, signatures, contract):
    limits = contract["limits"]
    labels, locals_, code = {}, set(parameters) | {"state"}, []
    for row in source:
        require(isinstance(row, list) and row, "instruction must be a nonempty array")
        op = identifier(row[0]).upper()
        if op == "LABEL":
            require(len(row) == 2, "LABEL requires one identifier")
            label = identifier(row[1])
            require(label not in labels, "duplicate label: " + label)
            labels[label] = len(code)
            continue
        require(op in contract["opcodes"], "unknown instruction: " + op)
        require(len(row) == contract["opcodes"][op]["width"], "wrong instruction arity: " + op)
        name, count = None, 0
        if op in {"LOAD", "STORE", "JUMP", "JUMP_IF_FALSE", "CALL"}:
            name = identifier(row[1])
        if op in {"ARRAY", "OBJECT"}:
            count = integer(row[1])
            require(0 <= count <= limits["collection"], "collection exceeds memory bounds")
        if op == "STORE":
            require(name != "state", "cannot replace state root")
            locals_.add(name)
        if op == "CALL":
            count = integer(row[2])
            require(name in signatures and signatures[name][0] <= count <= signatures[name][1],
                    "unknown call or invalid argument count: " + name)
        code.append((op, name, count))
    code.extend([("CONST", None, 0), ("RETURN", None, 0)])
    for op, name, _ in code:
        if op == "LOAD":
            require(name in locals_, "undeclared variable: " + name)
        if op in {"JUMP", "JUMP_IF_FALSE"}:
            require(name in labels, "unknown label: " + name)
    heights, pending = {}, deque([(0, 0)])
    while pending:
        pc, height = pending.popleft()
        require(0 <= pc < len(code), "jump outside function")
        if pc in heights:
            require(heights[pc] == height, "inconsistent operand stack at control-flow merge")
            continue
        heights[pc] = height
        op, name, count = code[pc]
        definition = contract["opcodes"][op]
        needed = definition["pops"]
        needed = count if needed == "n" else 2 * count if needed == "2n" else integer(needed)
        require(height >= needed, "operand stack underflow at " + str(pc))
        next_height = height - needed + integer(definition["pushes"])
        require(next_height <= limits["stack"], "operand stack exceeds memory bounds")
        if op in {"RETURN", "HALT"}:
            continue
        if op in {"JUMP", "JUMP_IF_FALSE"}:
            pending.append((labels[name], next_height))
        if op != "JUMP":
            pending.append((pc + 1, next_height))


def verify(program, contract):
    require(contract["language"] == "luckywars-item-script" and integer(contract["version"]) == 1,
            "unsupported compiler contract")
    require(contract.get("canonical") == "unicode-utf16-bigdecimal-v2", "unsupported canonical number format")
    limits = contract["limits"]
    check_data(program, limits)
    require(isinstance(program, dict) and set(program) == {"version", "state", "handlers", "functions"},
            "unknown or missing program fields")
    require(integer(program["version"]) == integer(contract["version"]), "unsupported program version")
    require(all(isinstance(program[key], dict) for key in ("state", "handlers", "functions")),
            "program sections must be objects")
    functions = program["functions"]
    require(0 < len(functions) <= limits["functions"], "function table exceeds memory bounds")
    signatures = {key: (integer(value["minimum"]), integer(value["maximum"]))
                  for key, value in contract["api"].items()}
    parameters = {}
    for key, function in functions.items():
        identifier(key)
        require(key not in signatures, "function shadows native API")
        require(isinstance(function, dict) and set(function) == {"parameters", "code"} and
                isinstance(function["parameters"], list) and isinstance(function["code"], list),
                "invalid function fields")
        params = [identifier(value) for value in function["parameters"]]
        require(len(params) == len(set(params)) and "state" not in params, "duplicate or reserved parameter")
        require(len(params) <= limits["parameters"], "function argument table exceeds memory bounds")
        signatures[key] = (len(params), len(params))
        parameters[key] = params
    total = 0
    for key, function in functions.items():
        total += len(function["code"])
        require(total <= limits["instructions"], "program instruction table exceeds memory bounds")
        compile_function(parameters[key], function["code"], signatures, contract)
    require(program["handlers"], "program has no event entry point")
    for event, entry in program["handlers"].items():
        require(event in contract["events"], "unknown item event: " + event)
        identifier(entry)
        require(entry in functions and signatures[entry] == (1, 1), "event handler must accept one event argument")
    return {"ok": True, "verifier": VERIFIER_VERSION, "program_sha256": digest(program),
            "contract_sha256": digest(contract), "functions": len(functions), "instructions": total}


def main():
    try:
        require(len(sys.argv) == 3, "usage: verify_genie_program.py contract.json program.json")
        contract = parse(Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
        program = parse(Path(sys.argv[2]).read_text(encoding="utf-8-sig"))
        print(json.dumps(verify(program, contract), ensure_ascii=True, separators=(",", ":")))
    except (ValueError, TypeError, KeyError, OverflowError, RecursionError) as error:
        print(json.dumps({"ok": False, "verifier": VERIFIER_VERSION, "error": str(error)}, ensure_ascii=True))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
