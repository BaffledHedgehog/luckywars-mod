#!/usr/bin/env python3
"""Reproducible inventory and strict template audit for lbc_kit_game."""
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JOBS = ROOT / "agent_notes/inventory/research_jobs.json"
OUT = ROOT / "agent_notes/inventory/lbc_kit_game_review_audit.json"

def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def main() -> None:
    job = json.loads(JOBS.read_text(encoding="utf-8"))["lbc_kit_game"]
    rows = []
    exact_groups: dict[str, list[str]] = defaultdict(list)
    block_rows = []
    command_counts = Counter()
    flagged = Counter()
    for rel in job["sources"]:
        path = ROOT / rel
        raw = path.read_bytes()
        text = raw.decode("utf-8-sig")
        lines = [s.strip() for s in text.splitlines() if s.strip() and not s.lstrip().startswith("#")]
        group = sha256("\n".join(lines).encode("utf-8"))
        exact_groups[group].append(rel)
        row = {"source": rel, "sha256": sha256(raw), "bytes": len(raw),
               "commands": len(lines), "ends_with_newline": raw.endswith(b"\n"),
               "exact_template": group}
        if "/skills/blocks/" in rel and rel.endswith(".py"):
            row["kind"] = "non_runtime_generator"
            row["commands"] = 0
            row["generator_lines"] = len(text.splitlines())
        elif "/skills/blocks/" in rel and rel.endswith(".mcfunction"):
            row["kind"] = "generated_block"
            if len(lines) == 1 and lines[0].startswith("setblock ~ ~ ~ "):
                state = lines[0][len("setblock ~ ~ ~ "):]
                row["blockstate"] = state
                block_rows.append({"source": rel, "blockstate": state})
            else:
                row["kind"] = "exception_or_generator"
                flagged["blocks_nonuniform_or_generator"] += 1
        elif rel.endswith(".mcfunction"):
            row["kind"] = "function"
            command_counts.update(re.match(r"[a-zA-Z]+", line).group(0) if re.match(r"[a-zA-Z]+", line) else "<macro>" for line in lines)
            for label, pattern in {
                "storage": r"\bstorage\b", "macro": r"\$\$|\$[A-Za-z]",
                "execute_store_if_entity": r"execute store (?:result|success).*\bif entity\b",
                "execute_store_if_items": r"execute store (?:result|success).*\bif items\b",
                "return": r"\breturn\b", "schedule": r"\bschedule\b",
                "as_context": r"\bas\b", "at_context": r"\bat\b",
                "clear_or_consume": r"\bclear\b|\bitem replace\b",
                "random": r"\brandom (?:value|roll)\b|random_chance",
            }.items():
                n = sum(bool(re.search(pattern, line)) for line in lines)
                if n:
                    row[label] = n
                    flagged[label] += 1
        rows.append(row)

    block_ids = sorted(int(Path(r["source"]).stem) for r in block_rows)
    block_states = Counter(r["blockstate"] for r in block_rows)
    normalized_groups = defaultdict(list)
    for rel in job["sources"]:
        if "/skills/blocks/" not in rel or not rel.endswith(".mcfunction"):
            continue
        name = Path(rel).stem
        text = (ROOT / rel).read_text(encoding="utf-8-sig").strip()
        if name.isdigit() and text.startswith("setblock ~ ~ ~ "):
            normalized_groups[text].append(rel)
        else:
            normalized_groups["<non-numeric-or-non-setblock>"].append(rel)

    generator_path = ROOT / "world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/blocks/blockgen.py"
    generator_tree = __import__("ast").parse(generator_path.read_text(encoding="utf-8-sig"))
    generator_blocks = __import__("ast").literal_eval(generator_tree.body[0].value)
    observed_by_id = {int(Path(r["source"]).stem): r["blockstate"] for r in block_rows}
    generator_differences = [{"id": i + 1, "generator": state, "observed_source": observed_by_id.get(i + 1),
                              "equivalent_unqualified_minecraft_id": state == (observed_by_id.get(i + 1) or "").removeprefix("minecraft:")}
                             for i, state in enumerate(generator_blocks)
                             if state != (observed_by_id.get(i + 1) or "").removeprefix("minecraft:")]
    data = {
        "job": "lbc_kit_game", "job_source_count": len(job["sources"]),
        "unique_source_paths": len(set(job["sources"])),
        "sources": rows,
        "exact_source_groups": [{"sha256_normalized_commands": k, "count": len(v), "sources": v}
                                for k, v in sorted(exact_groups.items(), key=lambda x: (-len(x[1]), x[0]))],
        "strict_numeric_block_audit": {
            "numeric_single_setblock_count": len(block_rows),
            "min_id": min(block_ids) if block_ids else None,
            "max_id": max(block_ids) if block_ids else None,
            "missing_ids": sorted(set(range(min(block_ids), max(block_ids) + 1)) - set(block_ids)) if block_ids else [],
            "duplicate_ids": [n for n, c in Counter(block_ids).items() if c > 1],
            "blockstate_variant_count": len(block_states),
            "blockstate_counts": dict(sorted(block_states.items())),
            "generator_block_count": len(generator_blocks),
            "generator_vs_observed_differences": generator_differences,
            "generated_or_exception_files": [r["source"] for r in rows if r.get("kind") == "exception_or_generator"],
            "normalized_template_groups": [{"normalized_command": k, "count": len(v), "sources": v}
                                            for k, v in sorted(normalized_groups.items(), key=lambda x: (-len(x[1]), x[0]))],
        },
        "function_command_head_counts": dict(command_counts),
        "function_feature_source_counts": dict(flagged),
    }
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(OUT.relative_to(ROOT)), "sources": len(rows),
                      "exact_groups": len(exact_groups),
                      "block_audit": data["strict_numeric_block_audit"] | {"blockstate_counts": "omitted"}}, ensure_ascii=False))

if __name__ == "__main__":
    main()
