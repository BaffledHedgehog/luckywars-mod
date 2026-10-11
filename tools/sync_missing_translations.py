"""Keep mod/template language files limited to keys absent from the read-only pack.

Optional additions JSON: {"ru_ru": {key: value}, "en_us": {key: value}}.
Existing pack values remain authoritative; this script never writes into the pack.
"""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parents[1]
PACK = Path(r"C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8")


def sync(additions=None):
    added = json.loads(additions.read_text(encoding="utf-8")) if additions else {}
    assert PACK.is_dir(), "Read-only reference pack is missing"
    design = json.loads((ROOT / "src/main/resources/skywars2/gui_design.json").read_text(encoding="utf-8"))
    map_keys = {source["translationKey"] for page in design["pages"] for slot in page["slots"]
                for source in slot["sources"] if "sourceMapId" in source and "translationKey" in source}
    source_keys = set()
    def collect_keys(value):
        if isinstance(value, dict):
            if "translate" in value:
                source_keys.add(value["translate"])
            for child in value.values():
                collect_keys(child)
        elif isinstance(value, list):
            for child in value:
                collect_keys(child)
    collect_keys(design["localizedStorageComponents"])
    for locale in ("ru_ru", "en_us"):
        existing = {}
        for source in sorted((PACK / "assets").glob(f"*/lang/{locale}.json")):
            existing.update(json.loads(source.read_text(encoding="utf-8-sig")))
        target = ROOT / f"src/main/resources/assets/skywars2/lang/{locale}.json"
        template = ROOT / f"resourcepack_template/assets/skywars2/lang/{locale}.json"
        source = target if target.is_file() else template
        entries = json.loads(source.read_text(encoding="utf-8")) if source.is_file() else {}
        before = len(entries)
        entries.update(added.get(locale, {}))
        entries = {key: value for key, value in entries.items() if key not in existing}
        # Map captions used to be generated twice, by page/slot and by map ID.
        # The prepared design is the only consumer; retain just its chosen keys.
        entries = {key: value for key, value in entries.items()
                   if not key.startswith(("swrg.gui.map.", "skywars2.gui.map.")) or key in map_keys}
        entries = {key: value for key, value in entries.items()
                   if not key.startswith("skywars2.gui.source.") or key in source_keys}
        encoded = json.dumps(entries, ensure_ascii=False, indent=2) + "\n"
        if target.exists() or entries:
            target.write_text(encoded, encoding="utf-8")
        # Never recreate a deleted, already-applied template unless new keys are needed.
        if entries:
            template.parent.mkdir(parents=True, exist_ok=True)
            template.write_text(encoded, encoding="utf-8")
        elif template.exists():
            template.unlink()
        print(f"{locale}: {before} previous mod keys, {len(entries)} missing pack keys remain")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--additions", type=Path)
    sync(parser.parse_args().additions)
