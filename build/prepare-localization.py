from pathlib import Path
import json
import importlib.util

root = Path(__file__).resolve().parents[1]
merged = {"ru_ru": {}, "en_us": {}}
for filename in ["runtime-translations.json", "gui-translations.json", "map-translations.json"]:
    fragment = json.loads((root / "build" / filename).read_text(encoding="utf-8"))
    for locale, entries in fragment.items():
        for key, value in entries.items():
            assert key not in merged[locale] or merged[locale][key] == value, (key, locale)
            merged[locale][key] = value

legacy = Path(r"E:\servers_mew\cringe1_21_4_skywars\plugins\luckywars\resourcepack_lang")
for locale, entries in merged.items():
    old = json.loads((legacy / f"{locale}.json").read_text(encoding="utf-8"))
    for key in ["golden_pumpkin", "golden_pumpkin.lore"]:
        entries[key] = old[key]
    entries.update({
        "skywars2.item.lucky_block.name": "Лаки-блок" if locale == "ru_ru" else "Lucky Block",
        "skywars2.item.lucky_block.credits.1": "Авторы: FaffledHedgehog, Scuferis," if locale == "ru_ru" else "Made by: FaffledHedgehog, Scuferis,",
        "skywars2.item.lucky_block.credits.2": "Swagrumm9690_, JastSh2sh",
    })
    target = root / f"src/main/resources/assets/skywars2/lang/{locale}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(dict(sorted(entries.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
assert merged["ru_ru"].keys() == merged["en_us"].keys()

spec = importlib.util.spec_from_file_location("importer", root / "tools/import_game_resources.py")
importer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(importer)
for file in (root / "src/main/resources/data/swrg/loot_table/chests").glob("*_lb.json"):
    # The legacy schema migration must run on the original, not on native 26.3 JSON.
    original = importer.LOOT / "chests" / file.name
    obj = json.loads(original.read_text(encoding="utf-8"))
    importer.migrate_loot_schema(obj)
    file.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

server = root.parent
config = server / "config/polymer/resource-pack.json"
obj = json.loads(config.read_text(encoding="utf-8"))
obj["include_zips"] = [x for x in obj["include_zips"] if x != "world/resources.zip" or (server / x).is_file()]
config.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
config = server / "config/polymer/auto-host.json"
obj = json.loads(config.read_text(encoding="utf-8"))
obj["enabled"] = True
obj["message"] = {"translate": "multiplayer.texturePrompt.line1"}
config.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
print(f"Merged {len(merged['ru_ru'])} keys per locale; configured optional Polymer autohost")
