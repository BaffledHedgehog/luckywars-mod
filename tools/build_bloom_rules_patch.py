"""Prepare a bloom-material addon; the author's installed pack is read-only."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PACK = Path(r"C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
ADDON = ROOT / "resourcepack_template/updates/bloom_rules_2026_11_16"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK)
    args = parser.parse_args()
    base_hashes = {}

    def load(relative):
        raw = (args.pack / relative).read_bytes()
        base_hashes[relative] = hashlib.sha256(raw).hexdigest()
        return raw.decode("utf-8")

    def write(relative, text):
        destination = ADDON / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(text, encoding="utf-8", newline="\n")

    def replace_once(text, old, new):
        if text.count(old) != 1:
            raise ValueError(f"Author shader changed: expected exactly one {old!r}")
        return text.replace(old, new)

    for name in ("item", "entity", "terrain", "particle"):
        relative = f"assets/minecraft/shaders/core/{name}.fsh"
        text = load(relative)
        if "#include <lbc:bloom_rules.glsl>" in text:
            raise ValueError(f"Bloom framework is already installed in {relative}; use its editor instead")
        text = replace_once(text, "#include <minecraft:oit.glsl>",
                            "#include <minecraft:oit.glsl>\n#include <lbc:bloom_rules.glsl>")
        if name == "item":
            old = "fragColor = calculateFinalColor(color, bypassLighting, bypassFog, alphaMarker == 254);"
            extra = """
    #ifndef OIT
    // Item cutout writes this explicit material tag; PNG alpha255 never opts in.
    fragColor = lbc_bloom_unmarked(fragColor);
    if (alphaMarker == 254 && lbc_bloom_rule(rawColor.rgb) >= 0) fragColor.a = 254.0 / 255.0;
    #endif"""
        elif name == "entity":
            old = "fragColor = calculateFinalColor(color, bypassLighting, bypassFog);"
            extra = """
    #ifndef OIT
    // Fullbright textures explicitly opt in with alpha254; ordinary skins do not.
    fragColor = lbc_bloom_unmarked(fragColor);
    if (alphaMarker == 254 && lbc_bloom_rule(rawColor.rgb) >= 0) fragColor.a = 254.0 / 255.0;
    #endif"""
        else:
            old = "fragColor = calculateFinalColor(color);" if name == "terrain" else "fragColor = calculateFinalColor(color, laser);"
            extra = """
    #ifndef OIT
    // The material tag is owned by item/entity emission, never terrain/particles.
    fragColor = lbc_bloom_unmarked(fragColor);
    #endif"""
        text = replace_once(text, old, old + extra)
        write(relative, text)

    relative = "assets/minecraft/post_effect/end_of_frame.json"
    chain = json.loads(load(relative))
    changed = 0
    for render_pass in chain["passes"]:
        if render_pass["fragment_shader"] == "minecraft:post/bloom":
            render_pass["fragment_shader"] = "lbc:post/material_bloom"
            changed += 1
    if changed != 2:
        raise ValueError("Expected exactly the two scene-bloom blur/composite passes")
    write(relative, json.dumps(chain, ensure_ascii=False, indent=2) + "\n")
    for relative in ("assets/minecraft/shaders/post/bloom_sample.fsh",):
        load(relative)
    (ADDON / "AUTHOR_BASE_SHA256.json").write_text(json.dumps(base_hashes, indent=2) + "\n")
    print(f"Prepared {len(base_hashes)} existing-file replacements; author pack unchanged")


if __name__ == "__main__":
    main()
