"""Check the two instant Bad Apple post effects on a GPU, including alpha and zero history."""
import json
from pathlib import Path
import struct
import moderngl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ADDON = ROOT / "resourcepack_template/updates/badapple_visuals_2026_11_14"
ctx = moderngl.create_standalone_context(require=330)
vertex = """#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location=0) out vec2 texCoord;
void main(){texCoord=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(texCoord*2-1,0,1);}
"""
fragment = (ADDON / "assets/lbc/shaders/post/badapple_monochrome.fsh").read_text()
program = ctx.program(vertex_shader=vertex, fragment_shader=fragment)
program["SceneSampler"].value = 0
program["BadAppleConfig"].binding = 0
vao = ctx.vertex_array(program, [])
data = np.random.default_rng(713).random((64, 64, 4), dtype=np.float32)
data[0, :6] = [(1, 0, 0, 1), (0, 1, 0, 1), (0, 0, 1, 1), (0, 0, 0, .2), (1, 1, 1, .7), (.5, .5, .5, 1)]
texture = ctx.texture((64, 64), 4, data.tobytes(), dtype="f4")
texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
texture.use(0)
frame = ctx.simple_framebuffer((64, 64), components=4, dtype="f4")
frame.use()
buffer = ctx.buffer(reserve=16)
buffer.bind_to_uniform_block(0)
expected = np.clip(data[:, :, :3] @ np.array([.299, .587, .114], dtype="f4"), 0, 1)
results = {}
for effect in ("white", "black", "white", "black"):
    config = json.loads((ADDON / f"assets/lbc/post_effect/badapple/{effect}.json").read_text())
    assert len(config["passes"]) == 2 and set(config["targets"]) == {"monochrome"}
    assert not any("persistent" in value for value in config["targets"].values())
    assert config["passes"][0]["inputs"] == [{"sampler_name": "Scene", "target": "minecraft:main"}]
    assert config["passes"][0]["output"] == "monochrome" and config["passes"][1]["output"] == "minecraft:main"
    invert = config["passes"][0]["uniforms"]["BadAppleConfig"][0]["value"]
    assert isinstance(invert, (int, float)), "Native float uniform codec requires a scalar"
    assert invert == (1.0 if effect == "black" else 0.0)
    buffer.write(struct.pack("4f", invert, 0, 0, 0))
    vao.render(vertices=3)
    image = np.frombuffer(frame.read(components=4, dtype="f4"), dtype="f4").reshape(64, 64, 4)
    gray = 1 - expected if invert else expected
    assert np.allclose(image[:, :, :3], gray[:, :, None], atol=2e-6)
    assert np.array_equal(image[:, :, 3], data[:, :, 3])
    if effect in results:
        assert np.array_equal(image, results[effect]), "No cross-frame history or transition is permitted"
    results[effect] = image.copy()
assert np.allclose(results["white"][:, :, :3] + results["black"][:, :, :3], 1, atol=2e-6)
result = dict(passed=True, renderer=ctx.info["GL_RENDERER"], effects=2,
    sceneSamples=4096, alphaPreserved=True, grayscaleAndExactInverse=True,
    repeatedSwitchesImmediate=True, noPersistentTargets=True,
    limitation="Standalone GPU post-shader test; physical client and world-paint frame rate are separate checks.")
(ADDON / "GPU_RESULTS.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result))
