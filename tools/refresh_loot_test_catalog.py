"""Expose original IDs that have a finished public port, without admitting unrelated legacy tables."""
import json
from pathlib import Path


def main():
    project = Path(__file__).resolve().parents[1]
    target = project / "src/main/resources/skywars2/loot_test_catalog.json"
    catalog = json.loads(target.read_text(encoding="utf-8-sig"))
    tables = set(catalog["tables"]) - set(catalog.get("generated_roots", []))
    generated = set()
    root = project / "src/main/resources/data/lbc/loot_table"
    for port in (root / "ported").rglob("*.json"):
        relative = port.relative_to(root / "ported")
        if (root / relative).is_file():
            generated.add("lbc:" + relative.as_posix()[:-5])
    catalog["generated_roots"] = sorted(generated)
    catalog["tables"] = sorted(tables | generated)
    target.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Public original IDs with finished ports: {len(generated)}; catalog entries: {len(catalog['tables'])}")


if __name__ == "__main__":
    main()
