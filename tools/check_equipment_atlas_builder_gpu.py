"""Render the existing armor collage shader against a generated, tightly packed atlas."""
import argparse
import hashlib
import json
from pathlib import Path
import moderngl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("atlas", type=Path)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
source = (ROOT / "resourcepack_template/updates/tmt_equipment_2026_11_07/assets/lbc/shaders/include/tmt_equipment.glsl").read_text(encoding="utf-8")
ctx = moderngl.create_standalone_context(require=330)
with Image.open(args.atlas).convert("RGBA") as image:
    dimensions = image.size
    assert image.getpixel((0, 0)) == (31, 197, 83, 255)
    assert max(dimensions) <= ctx.info["GL_MAX_TEXTURE_SIZE"]
    texture = ctx.texture(dimensions, 4, image.tobytes())
texture.repeat_x = texture.repeat_y = False
texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
texture.use(0)
program = ctx.program(vertex_shader='''#version 330
out vec2 quad;
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    quad = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}''', fragment_shader='''#version 330
uniform sampler2D atlas;
uniform uint seed;
in vec2 quad;
out vec4 fragColor;
''' + source + '''
void main() {
    vec4 signature = texelFetch(atlas, ivec2(0), 0);
    if (!lbc_tmt_carrier(signature, 250u) || lbc_tmt_carrier(signature, 249u)) {
        fragColor = vec4(0.0); return;
    }
    fragColor = lbc_tmt_collage(atlas, quad, seed);
}''')
program["atlas"].value = 0
frame = ctx.simple_framebuffer((128, 128), components=4, dtype="f4")
vao = ctx.vertex_array(program, [])


def render(seed):
    frame.use()
    frame.clear()
    program["seed"].value = seed
    vao.render(vertices=3)
    pixels = np.frombuffer(frame.read(components=4, dtype="f4"), dtype=np.float32)
    assert np.isfinite(pixels).all()
    pixels = pixels.reshape((128, 128, 4))
    assert np.all(pixels[:, :, 3] == 1)
    assert np.all((pixels >= 0) & (pixels <= 1))
    return pixels.tobytes()


seeds = [0, 1, 255, 256, 65535, 42, 43, 1024]
outputs = {seed: hashlib.sha256(render(seed)).hexdigest() for seed in seeds}
assert hashlib.sha256(render(42)).hexdigest() == outputs[42]
assert len(set(outputs.values())) == len(seeds)
result = {
    "passed": True, "atlas": str(args.atlas), "size": dimensions,
    "atlasSha256": hashlib.sha256(args.atlas.read_bytes()).hexdigest(),
    "shaderSha256": hashlib.sha256(source.encode("utf-8")).hexdigest(),
    "gpu": ctx.info["GL_RENDERER"], "gpuMaxTextureSize": ctx.info["GL_MAX_TEXTURE_SIZE"],
    "seedRenders": outputs, "repeatIdentical": True, "distinctSeedsVary": True,
    "nonPowerOfTwoTexture": any(n & (n - 1) for n in dimensions),
    "headerAndCarrierDetection": True, "physicalMinecraftRenderingTested": False,
}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result))
vao.release()
frame.release()
program.release()
texture.release()
ctx.release()
