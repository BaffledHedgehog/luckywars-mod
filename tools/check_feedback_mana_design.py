"""Read-only source/config closure checks for the November 2 mana/design feedback."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources/skywars2"
presentations = json.loads((RES / "authored_presentations.json").read_text(encoding="utf-8"))
config = json.loads((RES / "source_config_defaults.json").read_text(encoding="utf-8"))
academy = json.loads((RES / "academy_presentations.json").read_text(encoding="utf-8"))
references = set()


def visit(value):
    if isinstance(value, dict):
        if "score" in value and value["score"]["objective"] == "lbc.math":
            references.add(value["score"]["name"])
        for child in value.values():
            visit(child)
    elif isinstance(value, list):
        for child in value:
            visit(child)


visit(presentations)
assert references <= config["values"].keys() | config["derived"].keys(), references - config["values"].keys() - config["derived"].keys()
assert "time_stopped" not in config["values"], "Runtime clocks are not configuration"
assert config["values"]["void_arcanum_mana_tick"] == 11
assert config["values"]["ice_wand_shift_mana_use"] == 200
for rule in config["derived"].values():
    assert rule["source"] in config["values"] and rule["multiplier"] > 0
assert academy["academy_evoker_wand"] == "b"
assert academy["academy_ice_wand"] == "ss"
assert academy["academy_soulcorruption_wand"] == "a"
for name in ("termit", "sulfur", "phosphin", "gremuch"):
    assert presentations[name]["minecraft:tooltip_display"]["hidden_components"] == ["minecraft:potion_contents"]
for name in ("evoker_wand", "ice_wand", "healing_arcanum"):
    assert presentations[name]["minecraft:custom_name"]
    assert presentations[name]["minecraft:lore"]
    assert presentations[name]["minecraft:tooltip_style"].startswith("lbc:tier_")
print(f"PASS: {len(presentations)} source presentations, {len(academy)} Academy pairs, "
      f"{len(references)} tooltip operands covered by {len(config['values'])} settings and {len(config['derived'])} derived prices")
