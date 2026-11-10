"""Link actual native shader variants and render private radar/ordinary controls on a GPU."""
from pathlib import Path
import ast
import hashlib
import json
import re
import struct
import zipfile

import moderngl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / 'resourcepack_template/updates/radar_displays_2026_11_08'
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
JAR = zipfile.ZipFile(Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-merged.jar')


def source(path, original=False):
    for base in ((RP,) if original else (PATCH, RP)):
        file = base / path
        if file.is_file():
            return file.read_text(encoding='utf-8')
    return JAR.read(path).decode()


def expand(text, original=False):
    def include(match):
        namespace, name = match[1].split(':', 1)
        return expand(source(f'assets/{namespace}/shaders/include/{name}', original), original)
    return re.sub(r'#include\s+<([^>]+)>', include, text).replace('gl_VertexIndex', 'gl_VertexID')


# Reuse the native item quad/UBO fixture; it does not execute the ring-specific tests.
fixture = (ROOT / 'tools/check_meditation_ring_shader.py').read_text(encoding='utf-8')
render = next(node for node in ast.parse(fixture).body if isinstance(node, ast.FunctionDef) and node.name == 'render')
render_source = ast.get_source_segment(fixture, render).replace("data['color'] = (fade, fade, fade, 1)", "data['color'] = (*tint, 1)")
exec(compile(render_source, '<shared-native-item-quad>', 'exec'))
ctx = moderngl.create_standalone_context(require=330)
frame = ctx.simple_framebuffer((384, 384), components=4, dtype='f4')
dtype = np.dtype([('pos', 'f4', 3), ('color', 'f4', 4), ('uv', 'f4', 2),
                  ('overlay', 'i4', 2), ('light', 'i4', 2), ('normal', 'f4', 3)])
vs_path, fs_path = [f'assets/minecraft/shaders/core/item.{extension}' for extension in ('vsh', 'fsh')]
variants = ['', '#define ALPHA_CUTOUT 0.1', '#define OIT_ALPHA_ONLY',
            '#define OIT_ALPHA_ONLY\n#define ALPHA_CUTOUT 0.1',
            '#define GLINT', '#define GLINT\n#define GLINT_SPECIAL']
for variant in ('OIT_ACCUMULATE', 'OIT_RESOLVE'):
    variants.append(f'#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define {variant}')
for definitions in variants:
    shaders = [expand(source(path).replace('#version 330', '#version 330\n' + definitions, 1)) for path in (vs_path, fs_path)]
    ctx.program(vertex_shader=shaders[0], fragment_shader=shaders[1]).release()
programs = [ctx.program(vertex_shader=expand(source(vs_path, old), old), fragment_shader=expand(source(fs_path, old), old)) for old in (False, True)]
signature = (17, 19, 23, 253)
for color, expected in [((27, 229, 199), (20, 209, 255)), ((27, 229, 200), (255, 170, 0))]:
    tint = np.array(color) / 255
    reference = render(programs[0], signature)
    opaque = reference[:, :, 3] > .99
    assert opaque.sum() > 10000 and np.allclose(reference[opaque, :3], np.array(expected) / 255, atol=1e-6)
    assert reference[192, 192, 3] > .99 and reference[42, 42, 3] == 0
    for atlas in (64, 256):
        for offset in (0, 4, 12):
            assert np.allclose(render(programs[0], signature, atlas=atlas, offset=offset, light=0), reference, atol=1e-6)
controls = 0
for color in ((255, 255, 255), (27, 229, 198), (27, 229, 199), (27, 229, 200)):
    tint = np.array(color) / 255
    for texture in ((70, 80, 90, 255), (17, 19, 23, 252), (180, 90, 30, 255)):
        for light in (0, .7):
            assert np.allclose(render(programs[0], texture, light=light), render(programs[1], texture, light=light), atol=1e-6)
            controls += 1
baseline = json.loads((PATCH / 'AUTHOR_BASE_SHA256.json').read_text(encoding='utf-8'))
# The manifest records the actual author paths without modifying the author pack.
for path, digest in baseline["sha256"].items():
    if isinstance(digest, str) and len(digest) == 64:
        assert hashlib.sha256((RP / path).read_bytes()).hexdigest() == digest, path
result = dict(passed=True, renderer=ctx.info['GL_RENDERER'], linkedVariants=len(variants),
              ordinaryControls=controls, privateColors=2, atlasSizes=[64, 256], nativeQuadOffsets=[0, 4, 12])
(PATCH / 'GPU_RESULTS.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps(result))
