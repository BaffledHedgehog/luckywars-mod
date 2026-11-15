"""Render real prepared phone frames with the exact active/native world-text shader.

Read-only author pack; one hidden GL context and three bounded frames, no game/server launch.
"""
import os
for key in ("OPENBLAS_NUM_THREADS", "OMP_NUM_THREADS", "MKL_NUM_THREADS"):
    os.environ[key] = "1"
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
PACK = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
OUT = ROOT / "build/verification/tiktok"
JAR = next(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob("*.jar") if not p.name.endswith("-sources.jar"))
dependencies = {}

with zipfile.ZipFile(JAR) as jar:
    def source(name):
        path = PACK / name
        data = path.read_bytes() if path.exists() else jar.read(name)
        dependencies[name] = hashlib.sha256(data).hexdigest()
        return data.decode("utf-8")

    def expand(text):
        return re.sub(r"#include\s+<([^>]+)>", lambda m: expand(source("assets/" + m[1].split(":", 1)[0] + "/shaders/include/" + m[1].split(":", 1)[1])), text).replace("gl_VertexIndex", "gl_VertexID")

    vertex = expand(source("assets/minecraft/shaders/core/text.vsh"))
    fragment = expand(source("assets/minecraft/shaders/core/text.fsh"))

ctx = moderngl.create_standalone_context(require=330)
programs = []
for definitions in ("", "#define OIT_ALPHA_ONLY 1\n", "#define OIT 1\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE 1\n"):
    shaders = [s.replace("#version 330", "#version 330\n" + definitions, 1) for s in (vertex, fragment)]
    programs.append(ctx.program(vertex_shader=shaders[0], fragment_shader=shaders[1]))
program = programs[0]
resources = []
for name in program:
    block = program[name]
    if not isinstance(block, moderngl.UniformBlock):
        continue
    block.binding = len(resources)
    data = bytearray(block.size)
    if name == "Projection":
        data[:64] = np.eye(4, dtype="f4").tobytes()
    if name == "DynamicTransforms":
        data[:64] = np.eye(4, dtype="f4").tobytes()
        data[64:128] = np.eye(4, dtype="f4").tobytes()
        struct.pack_into("4f", data, 128, 1, 1, 1, 1)
    if name == "Fog":
        for offset in range(16, len(data) - 3, 4):
            struct.pack_into("f", data, offset, 1_000_000 + offset * 10_000)
    buffer = ctx.buffer(data)
    buffer.bind_to_uniform_block(block.binding)
    resources.append(buffer)

attributes = {
    "Position": (np.array([[-1, 1, 0], [-1, -1, 0], [1, -1, 0], [1, 1, 0]], dtype="f4"), "3f"),
    "Color": (np.ones((4, 4), dtype="f4"), "4f"),
    "UV0": (np.array([[0, 0], [0, 1], [1, 1], [1, 0]], dtype="f4"), "2f"),
    "UV2": (np.full((4, 2), 240, dtype="i4"), "2i"),
}
buffers, content = [], []
for name, (array, fmt) in attributes.items():
    if name in program:
        buffer = ctx.buffer(array.tobytes())
        buffers.append(buffer)
        content.append((buffer, fmt, name))
index = ctx.buffer(np.array([0, 1, 2, 0, 2, 3], dtype="u4").tobytes())
vao = ctx.vertex_array(program, content, index)
lightmap = ctx.texture((16, 16), 4, bytes([255] * 4) * 256)
lightmap.use(2)
program["Sampler0"].value = 0
if "Sampler2" in program:
    program["Sampler2"].value = 2

clip = OUT / "hd-reference/clip.tkp"
offsets = [v[0] for v in struct.iter_unpack(">Q", (OUT / "hd-reference/clip.idx").read_bytes())]
checks = []
with clip.open("rb") as inp:
    magic, frames, fps, width, height, rate, bands = struct.unpack(">4sIIIIII", inp.read(28))
    assert (magic, fps, rate, bands) == (b"TKP3", 20, 48000, 48)
    for number in (0, frames // 2, frames - 1):
        inp.seek(offsets[number])
        compressed, length = struct.unpack(">II", inp.read(8))
        inp.seek(compressed + 144 + 9600, 1)
        pixels = np.asarray(Image.open(io.BytesIO(inp.read(length))).convert("RGBA"))
        assert pixels.shape == (1280, 720, 4)
        texture = ctx.texture((720, 1280), 4, pixels.tobytes())
        texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
        texture.use(0)
        target = ctx.simple_framebuffer((720, 1280), components=4, dtype="f4")
        target.use()
        target.clear()
        vao.render()
        result = np.frombuffer(target.read(components=4, dtype="f4"), dtype="f4").reshape(1280, 720, 4)[::-1]
        error = float(np.max(np.abs(result - pixels / 255)))
        assert error < 0.00001 and np.isfinite(result).all(), (number, error)
        checks.append({"frame": number, "pixels": 720 * 1280, "maximumError": error})
        texture.release()
        target.release()

for resource in (vao, index, lightmap, *buffers, *resources, *programs):
    resource.release()
ctx.release()
report = {"passed": True, "pipeline": "actual core/text WORLD_TEXT", "nativeVariantsCompiled": 3,
          "fullbrightLightmap": [240, 240], "actualPreparedFrames": checks,
          "shaderDependenciesSha256": dependencies, "authorPackWritten": False,
          "scope": "GPU shader/texture pixels; native FaceInfo geometry, STB decode, sound and transport are separately tested. Not a connected Minecraft client capture."}
(OUT / "client-shader-results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"passed": True, "frames": len(checks), "pixels": sum(v["pixels"] for v in checks), "variants": 3}))
