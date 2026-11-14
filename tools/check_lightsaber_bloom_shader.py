"""Render authored saber models through actual item/bloom shaders; keep the source pack read-only."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import zipfile

import moderngl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ADDON = ROOT / "resourcepack_template/updates/lightsaber_bloom_2026_11_14"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--pack", type=Path, required=True)
parser.add_argument("--output", type=Path, default=ADDON / "GPU_RESULTS.json")
args = parser.parse_args()
PACK = args.pack.resolve(strict=True)
JAR = zipfile.ZipFile(next((ROOT / ".gradle").rglob("minecraft-merged-d6f9bb6878-26.3.jar")))
FS = "assets/minecraft/shaders/core/item.fsh"
VS = "assets/minecraft/shaders/core/item.vsh"
BLOOM = "assets/minecraft/shaders/post/bloom_sample.fsh"


def source(name, old=False):
    for base in ((PACK,) if old else (ADDON, PACK)):
        if (base / name).is_file():
            return (base / name).read_text(encoding="utf-8")
    return JAR.read(name).decode()


def expand(text, old=False):
    return re.sub(r"#include\s+<([^>]+)>", lambda m: expand(source(
        "assets/" + m[1].replace(":", "/shaders/include/", 1), old), old), text
    ).replace("gl_VertexIndex", "gl_VertexID")


ctx = moderngl.create_standalone_context(require=330)
screen_vs = """#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location=0) out vec2 texCoord;
void main(){texCoord=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(texCoord*2-1,0,1);}
"""


def item_program(old=False, defines=""):
    return ctx.program(vertex_shader=expand(source(VS, old).replace(
        "#version 330", "#version 330\n" + defines, 1), old),
        fragment_shader=expand(source(FS, old).replace(
            "#version 330", "#version 330\n" + defines, 1), old))


variants = ["", "#define ALPHA_CUTOUT 0.1", "#define GLINT", "#define GLINT\n#define GLINT_SPECIAL",
    "#define OIT_ALPHA_ONLY", "#define OIT_ALPHA_ONLY\n#define ALPHA_CUTOUT 0.1",
    "#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE",
    "#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE\n#define GLINT"]
for defines in variants:
    item_program(defines=defines).release()
items = {(old, foil): item_program(old, "#define GLINT" if foil else "")
         for old in (False, True) for foil in (False, True)}
extractors = {old: ctx.program(vertex_shader=screen_vs, fragment_shader=expand(source(BLOOM, old), old))
              for old in (False, True)}


def uniforms(program, angle=0, light=1, glint=1):
    buffers = []
    theta = np.radians(angle)
    matrix = np.array([[np.cos(theta), 0, np.sin(theta), 0], [0, 1, 0, 0],
                       [-np.sin(theta), 0, np.cos(theta), 0], [0, 0, 0, 1]], dtype="f4")
    for name in program:
        member = program[name]
        if not isinstance(member, moderngl.UniformBlock):
            continue
        member.binding = len(buffers)
        raw = bytearray(member.size)
        if name == "DynamicTransforms":
            raw[:64] = matrix.T.tobytes()
            raw[64:128] = np.eye(4, dtype="f4").T.tobytes()
            struct.pack_into("4f", raw, 128, 1, 1, 1, 1)
        elif name == "Projection":
            raw[:64] = np.diag([1, 1, .1, 1]).astype("f4").tobytes()
        elif name == "Fog":
            struct.pack_into("6f", raw, 16, 1000, 2000, 1000, 2000, 2000, 2000)
        elif name == "Lighting":
            struct.pack_into("3f", raw, 0, light, light, light)
            struct.pack_into("3f", raw, 16, -light, light, -light)
        elif name == "Globals":
            struct.pack_into("f", raw, 12, glint)
            struct.pack_into("2f", raw, 32, 192, 192)
        elif name == "BlitConfig":
            struct.pack_into("4f", raw, 0, 1, 1, 1, 1)
        buffer = ctx.buffer(raw)
        buffer.bind_to_uniform_block(member.binding)
        buffers.append(buffer)
    return buffers


def extract(rgba, old=False, dtype="f1"):
    image = (np.round(np.clip(rgba, 0, 1) * 255).astype("u1") if dtype == "f1"
             else rgba.astype("f2" if dtype == "f2" else "f4"))
    texture = ctx.texture((image.shape[1], image.shape[0]), 4, image.tobytes(), dtype=dtype)
    texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
    texture.use(0)
    p = extractors[old]
    p["DiffuseSampler"].value = 0
    buffers = uniforms(p)
    frame = ctx.simple_framebuffer(texture.size, components=4, dtype="f4")
    frame.use()
    vao = ctx.vertex_array(p, [])
    vao.render(vertices=3)
    result = np.frombuffer(frame.read(components=4, dtype="f4"), dtype="f4").reshape(*image.shape[:2], 4).copy()
    for resource in [vao, frame, texture, *buffers]:
        resource.release()
    return result


VERTEX = np.dtype([("pos", "f4", 3), ("color", "f4", 4), ("uv", "f4", 2),
                   ("overlay", "i4", 2), ("light", "i4", 2), ("normal", "f4", 3)])
FACES = {"north": ([0, 1, 0], [1, 1, 0], [1, 0, 0], [0, 0, 0]),
         "south": ([1, 1, 1], [0, 1, 1], [0, 0, 1], [1, 0, 1]),
         "west": ([0, 1, 1], [0, 1, 0], [0, 0, 0], [0, 0, 1]),
         "east": ([1, 1, 0], [1, 1, 1], [1, 0, 1], [1, 0, 0]),
         "up": ([0, 1, 0], [0, 1, 1], [1, 1, 1], [1, 1, 0]),
         "down": ([0, 0, 1], [0, 0, 0], [1, 0, 0], [1, 0, 1])}
NORMALS = dict(north=(0, 0, -1), south=(0, 0, 1), west=(-1, 0, 0), east=(1, 0, 0), up=(0, 1, 0), down=(0, -1, 0))


def render(model_name, old=False, foil=False, angle=0, light=1, solid=None):
    model = json.loads(source(f"assets/lbc/models/item/{model_name}.json"))
    data, indices = [], []
    for element in model["elements"]:
        low, high = np.array(element["from"]), np.array(element["to"])
        for face, definition in element["faces"].items():
            uv = definition["uv"]
            coords = [(uv[0], uv[1]), (uv[2], uv[1]), (uv[2], uv[3]), (uv[0], uv[3])]
            start = len(data)
            for corner, tex in zip(FACES[face], coords):
                pos = (low + np.array(corner) * (high - low) - [8, 7.25, 8]) * .04
                data.append((pos, (1, 1, 1, 1), np.array(tex) / 16, (0, 0), (0, 0), NORMALS[face]))
            indices.extend(start + n for n in (0, 1, 2, 2, 3, 0))
    vertices = ctx.buffer(np.array(data, dtype=VERTEX).tobytes())
    ib = ctx.buffer(np.array(indices, dtype="u4").tobytes())
    p = items[old, foil]
    buffers = uniforms(p, angle, light)
    image = Image.open(PACK / f"assets/minecraft/textures/item/{model_name}.png").convert("RGBA")
    if solid is not None:
        image = Image.new("RGBA", image.size, solid)
    textures = [ctx.texture(image.size, 4, image.tobytes()),
                ctx.texture((1, 1), 4, bytes([0, 0, 0, 255])),
                ctx.texture((1, 1), 4, bytes([round(light * 255)] * 3 + [255])),
                ctx.texture((1, 1), 4, bytes([120, 40, 210, 200]))]
    for i, (name, texture) in enumerate(zip(("Sampler0", "Sampler1", "Sampler2", "GlintSampler"), textures)):
        texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
        texture.use(i)
        if name in p:
            p[name].value = i
    vao = ctx.vertex_array(p, [(vertices, "3f 4f 2f 2i 2i 3f", "Position", "Color", "UV0", "UV1", "UV2", "Normal")], ib, skip_errors=True)
    frame = ctx.simple_framebuffer((192, 192), components=4, dtype="f4")
    frame.use()
    frame.clear()
    ctx.disable(moderngl.BLEND | moderngl.CULL_FACE)
    ctx.enable(moderngl.DEPTH_TEST)
    vao.render(moderngl.TRIANGLES)
    result = np.frombuffer(frame.read(components=4, dtype="f4"), dtype="f4").reshape(192, 192, 4).copy()
    ctx.disable(moderngl.DEPTH_TEST)
    for resource in [vao, frame, vertices, ib, *textures, *buffers]:
        resource.release()
    return result


# Source models, original pixels and actual shader variants, in dark/bright light and all yaw quadrants.
models = []
for model in ("lightsaber_jedi", "lightsaber_sith"):
    for light in (0, .3, 1):
        for angle in (0, 45, 90, 180):
            ordinary, enchanted = [render(model, foil=foil, angle=angle, light=light) for foil in (False, True)]
            for dtype in ("f1", "f2", "f4"):
                emissions = [extract(a, dtype=dtype) for a in (ordinary, enchanted)]
                counts = [int(np.count_nonzero(e[:, :, :3].max(axis=2) > .1)) for e in emissions]
                assert min(counts) > 20, (model, light, angle, dtype, counts)
            models.append(dict(model=model, light=light, yaw=angle, baseBloomPixels=counts[0], enchantedBloomPixels=counts[1]))

# A reproducible failure with the unmodified shaders, not a test which can pass both versions.
old_blue = render("lightsaber_jedi", old=True)
assert not np.any(extract(old_blue, old=True, dtype="f2")[:, :, :3])
old_foil = render("lightsaber_jedi", old=True, foil=True)
assert not np.any(extract(old_foil, old=True)[:, :, :3])

# Only alpha254 emission changes in the item shader. Hilt, ordinary materials and foil remain exact.
controls = [(180, 90, 30, 255), (43, 216, 147, 130), (0, 48, 255, 250),
            (255, 14, 14, 253), (17, 19, 23, 255), (255, 190, 45, 255)]
for rgba in controls:
    for foil in (False, True):
        assert np.array_equal(render("lightsaber_jedi", old=True, foil=foil, solid=rgba),
                              render("lightsaber_jedi", foil=foil, solid=rgba)), (rgba, foil)

# Main-scene extraction adds no newly glowing vanilla block or skin samples, including author overrides.
names = [n for n in JAR.namelist() if n.endswith(".png") and n.startswith((
    "assets/minecraft/textures/block/", "assets/minecraft/textures/entity/player/"))]
total = newly_glowing = 0
for start in range(0, len(names), 50):
    colors = []
    for name in names[start:start + 50]:
        raw = (PACK / name).read_bytes() if (PACK / name).exists() else JAR.read(name)
        pixels = np.array(Image.open(io.BytesIO(raw)).convert("RGBA")).reshape(-1, 4)
        colors.append(np.unique(pixels[pixels[:, 3] > 0, :3], axis=0))
    rgb = np.concatenate(colors)
    rgb = np.concatenate([np.floor(rgb.astype("f4") * level) for level in (.2, .4, .6, .8, 1)])
    total += len(rgb)
    width = 1024
    rgba = np.ones(((len(rgb) + width - 1) // width * width, 4), dtype="f4")
    rgba[:len(rgb), :3] = rgb / 255
    rgba = rgba.reshape(-1, width, 4)
    before, after = [extract(rgba, old=old).reshape(-1, 4)[:len(rgb), :3].max(axis=1) > .001 for old in (True, False)]
    newly_glowing += int(np.count_nonzero(after & ~before))
assert newly_glowing == 0, newly_glowing
base = json.loads((ADDON / "AUTHOR_BASE_SHA256.json").read_text())
assert all(hashlib.sha256((PACK / name).read_bytes()).hexdigest() == sha for name, sha in base.items())
result = dict(passed=True, renderer=ctx.info["GL_RENDERER"], compiledItemVariants=len(variants),
    actualModels=models, bufferFormats=["RGBA8", "RGBA16F", "RGBA32F"],
    oldBlueHalfFloatFailureReproduced=True, oldEnchantedBlueFailureReproduced=True,
    unchangedNonEmissionMaterials=len(controls), vanillaBlockAndSkinTextures=len(names),
    sceneColorSamples=total, newlyGlowingSceneSamples=newly_glowing, authorAssetsUntouched=True,
    limitations="Standalone GPU render of actual models/textures/shaders; no physical in-game client capture or full OIT compositor simulation.")
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps({k: v for k, v in result.items() if k != "actualModels"}))
