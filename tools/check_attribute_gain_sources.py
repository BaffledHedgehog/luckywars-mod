"""Compare the shared positive attribute arithmetic with the actual legacy leaf functions."""
from pathlib import Path
from decimal import Decimal
import argparse
import re


def verify(project: Path, source: Path) -> int:
    root = source / "function/other/attributes"
    dispatcher = (root / "random_attrs_up.mcfunction").read_text(encoding="utf-8")
    assert "random value 1..26" in dispatcher, "source uniform draw bounds"
    rows = re.findall(r"matches (\d+) run function lbc:other/attributes/(\w+)", dispatcher)
    assert [int(i) for i, _ in rows] == list(range(1, 27)), "complete uniform source pool"
    native = (project / "src/main/java/me/skywars2/game/LbcAttributeGains.java").read_text(encoding="utf-8")
    assert "nextInt(26)+1" in native, "native uniform draw bounds"
    cases = re.findall(r"case (\d+) -> LbcAttributeChanges.change\(player, Attributes\.(\w+), (\d+), (-?\d+), ([.\d]+)\);", native)
    assert len(cases) == 26, "complete native pool without duplicate/unrecognized cases"
    actual = {int(i): (name.lower(), int(scale), int(step), Decimal(out)) for i, name, scale, step, out in cases}
    for index, name in rows:
        leaf = (root / (name + ".mcfunction")).read_text(encoding="utf-8")
        attr, scale = re.search(r"attribute @s minecraft:(\w+) base get (\d+)", leaf).groups()
        operation, step = re.search(r"scoreboard players (add|remove) tmp lbc.math (\d+)", leaf).groups()
        out = re.search(r"storage lbcmath tmp double ([.\d]+) run", leaf).group(1)
        expected = (attr, int(scale), int(step) * (-1 if operation == "remove" else 1), Decimal(out))
        assert actual[int(index)] == expected, (index, name, actual[int(index)], expected)
    return len(rows)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=Path("E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc"))
    args = parser.parse_args()
    print(f"Matched {verify(Path(__file__).resolve().parents[1], args.source)} source attribute gains")
