"""Prepare the staff's material markers and signed halo in a separate RP addon.

The author pack is read-only. PNG RGB is copied exactly; only two material-alpha
bytes are tagged, using the same alpha254 contract as the existing artist editor.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ADDON = ROOT / "resourcepack_template/updates/music_staff_bloom_2026_11_17"
TEXTURE = "assets/minecraft/textures/item/wand_52.png"
MODEL = "assets/lbc/models/item/music_staff/staff.json"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    args = parser.parse_args()
    pack = args.pack.resolve()
    if pack == ADDON.resolve() or pack in ADDON.resolve().parents:
        raise ValueError("The output must be outside the author pack")
    hashes = {}

    def read(relative):
        raw = (pack / relative).read_bytes()
        hashes[relative] = hashlib.sha256(raw).hexdigest()
        return raw

    def write(relative, raw):
        path = ADDON / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(raw.encode("utf-8") if isinstance(raw, str) else raw)

    def replace(text, old, new):
        if text.count(old) != 1:
            raise ValueError(f"Author shader changed: expected exactly one {old!r}")
        return text.replace(old, new)

    model = json.loads(read(MODEL))
    if model["textures"]["2"] != "minecraft:item/wand_52":
        raise ValueError("The staff no longer uses the recorded material texture")
    read(TEXTURE)
    texture = Image.open(pack / TEXTURE).convert("RGBA")
    pixels = list(texture.get_flattened_data())
    colors = {(255, 0, 64): 102, (29, 28, 32): 159}
    for rgb, expected in colors.items():
        if sum(pixel == (*rgb, 255) for pixel in pixels) != expected:
            raise ValueError(f"Artist texture changed: recheck {rgb} before tagging")
    # No repainting or resampling: the alpha byte is material metadata, not opacity.
    texture.putdata([(*p[:3], 254) if p[:3] in colors and p[3] == 255 else p for p in pixels])
    output = ADDON / TEXTURE
    output.parent.mkdir(parents=True, exist_ok=True)
    texture.save(output)

    relative = "assets/lbc/shaders/include/bloom_rules.glsl"
    include = read(relative).decode("utf-8")
    rules = json.loads(read("bloom_rules.json"))
    if any(tuple(rule["source"]) in colors for rule in rules):
        raise ValueError("Staff rules already exist; recheck instead of duplicating them")
    rules.extend([
        {"source": [255, 0, 64], "halo": [255, 14, 14], "strength": 1.5, "core": "white"},
        {"source": [29, 28, 32], "halo": [0, 0, 0], "strength": 2.0, "core": "white"},
    ])
    editor_path = "edit_bloom.py"
    read(editor_path)
    spec = importlib.util.spec_from_file_location("artist_bloom_editor", pack / editor_path)
    editor = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(editor)
    # generated() reads its include and returns text; it never saves the author file.
    include = editor.generated(rules)
    include = replace(include, "vec4 lbc_bloom_unmarked(vec4 value) {", """// A zero-RGB halo is subtractive. The existing editor's black swatch selects it.
// Its weight travels in the separate bloom target's otherwise unused alpha.
bool lbc_bloom_dark(LbcBloomRule rule) {
    return all(equal(rule.halo, ivec3(0)));
}
vec4 lbc_bloom_unmarked(vec4 value) {""")
    write(relative, include)
    write("bloom_rules.json", json.dumps(rules, ensure_ascii=False, indent=2) + "\n")

    relative = "assets/minecraft/shaders/post/bloom_sample.fsh"
    text = read(relative).decode("utf-8")
    text = replace(text,
        "fragColor = vec4(vec3(rule.halo) / 255.0 * clamp(rule.strength, 0.0, 4.0) / 4.0, 1.0);",
        """float weight = clamp(rule.strength, 0.0, 4.0) / 4.0;
    // RGB carries light; alpha carries a black halo. Unmarked pixels stay zero.
    fragColor = vec4(vec3(rule.halo) / 255.0 * weight, lbc_bloom_dark(rule) ? weight : 0.0);""")
    write(relative, text)

    relative = "assets/lbc/shaders/post/material_bloom.fsh"
    text = read(relative).decode("utf-8")
    text = replace(text, "vec3 materialBlur(", "vec4 materialBlur(")
    text = replace(text, "vec3 result = texture(image, coord).rgb *", "vec4 result = texture(image, coord) *")
    text = replace(text, "(texture(image, coord - offset).rgb + texture(image, coord + offset).rgb)",
                   "(texture(image, coord - offset) + texture(image, coord + offset))")
    text = replace(text, "fragColor = vec4(materialBlur(DiffuseSampler, texCoord, BlurDir), 1.0);",
                   "fragColor = materialBlur(DiffuseSampler, texCoord, BlurDir);")
    text = replace(text, """vec3 halo = clamp(4.0 * materialBlur(BloomSampler, texCoord, BlurDir), 0.0, 1.0);
        fragColor = vec4(1.0 - (1.0 - core) * (1.0 - halo), scene.a);""", """vec4 halo = clamp(4.0 * materialBlur(BloomSampler, texCoord, BlurDir), 0.0, 1.0);
        vec3 combined = 1.0 - (1.0 - core) * (1.0 - halo.rgb);
        // Keep explicitly white material cores white, including under a dark halo.
        // Scene alpha is unrelated camera/TimeStop data and must remain untouched.
        if (index < 0 || LBC_BLOOM_RULES[index].core != LBC_CORE_WHITE) combined *= 1.0 - halo.a;
        fragColor = vec4(combined, scene.a);""")
    write(relative, text)
    for relative in ("assets/minecraft/post_effect/end_of_frame.json",
                     "assets/minecraft/shaders/core/item.fsh", "assets/minecraft/shaders/core/entity.fsh",
                     "assets/minecraft/shaders/core/terrain.fsh", "assets/minecraft/shaders/core/particle.fsh"):
        read(relative)
    for relative, digest in hashes.items():
        if hashlib.sha256((pack / relative).read_bytes()).hexdigest() != digest:
            raise AssertionError(f"Author input changed during generation: {relative}")
    write("AUTHOR_BASE_SHA256.json", json.dumps(hashes, indent=2) + "\n")
    print(json.dumps({"addon": str(ADDON), "authorPackUnchanged": True,
                      "materialPixels": sum(colors.values()), "existingRules": len(rules) - 2}))


if __name__ == "__main__":
    main()
