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
    for locale in ("ru_ru", "en_us"):
        existing = {}
        for source in sorted((PACK / "assets").glob(f"*/lang/{locale}.json")):
            existing.update(json.loads(source.read_text(encoding="utf-8-sig")))
        target = ROOT / f"src/main/resources/assets/skywars2/lang/{locale}.json"
        entries = json.loads(target.read_text(encoding="utf-8"))
        before = len(entries)
        entries.update(added.get(locale, {}))
        entries = {key: value for key, value in entries.items() if key not in existing}
        encoded = json.dumps(entries, ensure_ascii=False, indent=2) + "\n"
        target.write_text(encoded, encoding="utf-8")
        template = ROOT / f"resourcepack_template/assets/skywars2/lang/{locale}.json"
        # Never recreate a deleted, already-applied template unless new keys are needed.
        if template.exists() or entries:
            template.parent.mkdir(parents=True, exist_ok=True)
            template.write_text(encoded, encoding="utf-8")
        print(f"{locale}: {before} previous mod keys, {len(entries)} missing pack keys remain")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--additions", type=Path)
    sync(parser.parse_args().additions)
