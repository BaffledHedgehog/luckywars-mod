"""Extract only the six authored TMT literal arrays; never execute functions."""
import argparse
import hashlib
import json
from pathlib import Path


def extract(root):
    tables = {}
    receipts = {}
    for source, target, size in [("nbttags", "tags", 1049), ("names", "names", 11682),
                                 ("hex", "hex", 16), ("sound", "sounds", 1735),
                                 ("attribute", "attributes", 32), ("enchantment", "enchantments", 42)]:
        path = root / f"set_storage_{source}.mcfunction"
        raw = path.read_bytes()
        values = []
        for line in raw.decode("utf-8-sig").splitlines():
            if not line.strip() or line.startswith("#"):
                continue
            table = {"nbttags": "tags", "sound": "sound", "names": "names",
                     "hex": "hex", "attribute": "attribute", "enchantment": "enchantment"}[source]
            prefix = f"data modify storage lbc:tags {table} append value "
            if not line.startswith(prefix):
                raise ValueError(f"Unexpected nonliteral source: {path}: {line}")
            values.append(json.loads(line[len(prefix):]))
        assert len(values) == size, (source, len(values))
        tables[target] = values
        receipts[path.name] = hashlib.sha256(raw).hexdigest()
    basics = (root / "set_basics.mcfunction").read_text(encoding="utf-8-sig").splitlines()[0]
    literal = basics.split(" set value ", 1)[1]
    tables["baseFlags"] = {key: int(value) for key, value in
                           (part.split(":") for part in literal.strip("{}").split(","))}
    return {"source": "LBC/data/lbc/function/drops/tmt", "sha256": receipts, **tables}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    data = extract(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
