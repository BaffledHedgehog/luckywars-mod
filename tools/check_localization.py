"""Check translation coverage and the generated Polymer pack without a client."""
from pathlib import Path
from collections import Counter
import argparse
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/skywars2/lang"
TEMPLATE = ROOT / "resourcepack_template/assets/skywars2/lang"
DEFAULT_RESOURCE_PACK = Path(r"C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, f"Duplicate translation: {key}"
        result[key] = value
    return result


def load(text):
    return json.loads(text, object_pairs_hook=unique_object)


def placeholders(value):
    result = []
    implicit = 1
    # Original static captions contain bare percentages. With no arguments,
    # Minecraft's TranslatableContents falls back to the unchanged caption.
    # Keep those exact source values; parameterized messages stay strict.
    if not re.search(r"%(?:\d+\$)?s", value):
        return Counter()
    for match in re.finditer(r"%(?:(\d+)\$)?([A-Za-z%]|$)", value):
        token = match.group()
        if token == "%%":
            continue
        assert token.endswith("s"), f"Invalid client placeholder: {token} in {value}"
        index = int(match[1]) if match[1] else implicit
        if not match[1]:
            implicit += 1
        result.append(index)
    return Counter(result)


def check(pack=None, resource_pack=DEFAULT_RESOURCE_PACK):
    # Applied translations are deliberately deleted from mod/template by the user.
    languages = {locale: load((LANG / f"{locale}.json").read_text(encoding="utf-8"))
                 if (LANG / f"{locale}.json").is_file() else {}
                 for locale in ["ru_ru", "en_us"]}
    existing = {locale: {} for locale in languages}
    assert resource_pack.is_dir(), f"Read-only reference resource pack not found: {resource_pack}"
    for locale in languages:
        for file in sorted((resource_pack / "assets").glob(f"*/lang/{locale}.json")):
            # Match the game's last-entry-wins behavior for third-party source files.
            existing[locale].update(json.loads(file.read_text(encoding="utf-8-sig")))
    for locale, entries in languages.items():
        duplicates = entries.keys() & existing[locale].keys()
        assert not duplicates, f"Already translated in resource pack ({locale}): {sorted(duplicates)}"
        template = TEMPLATE / f"{locale}.json"
        if template.exists():
            delta = load(template.read_text(encoding="utf-8"))
            assert not (delta.keys() & existing[locale].keys()), f"Template duplicates resource-pack keys: {locale}"
            assert delta == entries, f"Template differs from new mod translations: {locale}"
    combined = {locale: {**existing[locale], **entries} for locale, entries in languages.items()}
    with zipfile.ZipFile(ROOT.parent / "versions/26.3/server-26.3.jar") as vanilla:
        vanilla_keys = json.loads(vanilla.read("assets/minecraft/lang/en_us.json"))
    reference = languages["en_us"]
    for locale, entries in languages.items():
        for key, value in entries.items():
            assert isinstance(value, str), f"Invalid translation value: {locale}/{key}"
            assert not re.search(r"\?{2,}|\ufffd", value), f"Corrupted translation encoding: {locale}/{key}"
    for key in languages["ru_ru"].keys() | reference.keys():
        assert key in combined["en_us"] and key in combined["ru_ru"], f"Missing locale for new translation: {key}"
        assert placeholders(combined["en_us"][key]) == placeholders(combined["ru_ru"][key]), key

    def require_key(key, source):
        if key in vanilla_keys and key not in combined["en_us"]:
            return
        assert key in combined["en_us"], f"Missing {key} in {source}"
        # English is the native client fallback when the reference pack lacks ru_ru.
        if key in combined["ru_ru"]:
            assert placeholders(combined["en_us"][key]) == placeholders(combined["ru_ru"][key]), key

    for source in (ROOT / "src/main/java").rglob("*.java"):
        code = source.read_text(encoding="utf-8")
        # Dynamic families are exercised by the live menu/event checks.
        for method, key in re.findall(r'\b(text|t|c|translatable|legacyName)\("([^"\n]+)"\s*[,)]', code):
            prefix = {"text": "skywars2.", "t": "skywars2.gui.", "c": "skywars2.common.", "translatable": "", "legacyName": ""}[method]
            resolved = prefix + key
            if method in ("translatable", "legacyName") or resolved.startswith(("skywars2.", "swrg.", "lbc.")):
                require_key(resolved, source.name)
        # Cover key lookup tables/switches without mistaking swrg.spawn entity tags for translations.
        tables = re.findall(r'String\[\]\s+ORIGINAL_(?:KIT_NAME_KEYS|SKILL_KEYS|SKILL_LORE_KEYS)\s*=\s*\{(.*?)\};', code, re.DOTALL)
        table_text = "\n".join(tables)
        keys = re.findall(r'"([A-Za-z0-9_.]+)"', table_text)
        keys += re.findall(r'->\s*"((?:skywars2|swrg|lbc)\.[A-Za-z0-9_.]+)"', code)
        keys += re.findall(r'"(swrg\.lore\.(?:kit|skill)\.[A-Za-z0-9_.]+)"', code)
        for key in keys:
            if not key.endswith("."):
                require_key(key, source.name)

    def visit(value, filename):
        if isinstance(value, dict):
            if "translate" in value and not value["translate"].startswith("minecraft."):
                require_key(value["translate"], filename)
            assert "text" not in value, f"Literal player text in {filename}"
            for child in value.values():
                visit(child, filename)
        elif isinstance(value, list):
            for child in value:
                visit(child, filename)

    loot = ROOT / "src/main/resources/data/swrg/loot_table"
    for folder in ["chests", "game"]:
        for filename in (loot / folder).rglob("*.json"):
            visit(load(filename.read_text(encoding="utf-8")), filename)

    # Generated GUI templates keep structural separators and numbers literal;
    # all language-dependent leaves must use keys from the client pack or delta.
    design_file = ROOT / "src/main/resources/skywars2/gui_design.json"
    design = load(design_file.read_text(encoding="utf-8"))
    assert "localizedStorageComponents" in design, "GUI component import is incomplete"

    def visit_gui(value, source):
        if isinstance(value, dict):
            if "translate" in value:
                require_key(value["translate"], source)
            if "text" in value:
                assert not re.search(r"[^\W\d_]", value["text"]), f"Literal GUI caption in {source}: {value['text']}"
            for child in value.values():
                visit_gui(child, source)
        elif isinstance(value, list):
            for child in value:
                visit_gui(child, source)

    visit_gui(design["localizedStorageComponents"], "prepared GUI storage components")

    if pack:
        with zipfile.ZipFile(pack) as archive:
            assert archive.testzip() is None, "Damaged resource pack"
            for locale, expected in languages.items():
                path = f"assets/skywars2/lang/{locale}.json"
                actual = load(archive.read(path).decode("utf-8")) if path in archive.namelist() else {}
                assert actual == expected, f"Generated pack has different {locale} translations"
            assert "pack.mcmeta" in archive.namelist()
    print(f"PASS localization: new keys ru={len(languages['ru_ru'])}, en={len(reference)}; reference pack, template, placeholders and text coverage"
          + (", generated pack verified" if pack else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", type=Path)
    parser.add_argument("--resource-pack", type=Path, default=DEFAULT_RESOURCE_PACK)
    args = parser.parse_args()
    check(args.pack, args.resource_pack)
