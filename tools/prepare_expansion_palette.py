"""Extract the finite Expansion source palette, without changing the legacy source.

Only the audited one-command default-state grammar is accepted. Any future NBT,
state or second command fails explicitly instead of silently losing metadata.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=Path("E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/blocks"))
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / "src/main/resources/skywars2/expansion_blocks.json")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    entries = []
    digest = hashlib.sha256()
    for number in range(1, 1107):
        raw = (args.source / f"{number}.mcfunction").read_bytes()
        text = raw.decode("utf-8-sig").strip()
        match = re.fullmatch(r"setblock ~ ~ ~ ((?:minecraft:)?[a-z0-9_]+)(?: strict)?", text)
        if not match:
            raise ValueError(f"Unaudited source grammar in entry {number}: {text!r}")
        name = match[1]
        entries.append(name if ":" in name else "minecraft:" + name)
        digest.update(f"{number}\0".encode() + raw)
    assert len(entries) == 1106
    data = {"source_count": 1106, "source_sha256": digest.hexdigest(),
            "append_missing_vanilla_defaults": True, "blocks": entries}
    encoded = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if args.check:
        assert args.output.read_text(encoding="utf-8") == encoded, "Palette differs from source"
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded, encoding="utf-8", newline="\n")
    print(f"Expansion: {len(entries)} exact source entries; sha256={digest.hexdigest()}")


if __name__ == "__main__":
    main()
