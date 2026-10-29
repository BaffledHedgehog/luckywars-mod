"""Compile native item variants and exercise the isolated polymorph bloom mask on a GPU."""
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path

import moderngl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
TEMPLATE = ROOT / 'resourcepack_template'
OUT = ROOT / 'build/verification/polymorph'
JAR = zipfile.ZipFile(next((ROOT / '.gradle/loom-cache/minecraftMaven').rglob('minecraft-merged-d6f9bb6878-26.3.jar')))


def read(path):
    for base in (TEMPLATE, RP):
        if (base / path).is_file():
            return (base / path).read_text(encoding='utf-8')
    return JAR.read(path).decode()


def expand(text):
    def include(match):
        namespace, name = match[1].split(':', 1)
        return expand(read(f'assets/{namespace}/shaders/include/{name}'))
    return re.sub(r'#include\s+<([^>]+)>', include, text).replace('gl_VertexIndex', 'gl_VertexID')


sources = ['assets/minecraft/post_effect/end_of_frame.json',
           'assets/minecraft/shaders/post/bloom_sample.fsh',
           'assets/minecraft/textures/item/sheepwand.png']
hashes = {path: hashlib.sha256((RP / path).read_bytes()).hexdigest() for path in sources}
ctx = moderngl.create_standalone_context(require=330)
variants = ['', '#define ALPHA_CUTOUT 0.1', '#define OIT_ALPHA_ONLY',
            '#define OIT_ALPHA_ONLY\n#define ALPHA_CUTOUT 0.1',
            '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE',
            '#define GLINT', '#define GLINT\n#define GLINT_SPECIAL',
            '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE\n#define GLINT']
for definitions in variants:
    vs = read('assets/minecraft/shaders/core/item.vsh').replace('#version 330', '#version 330\n' + definitions, 1)
    fs = read('assets/minecraft/shaders/core/item.fsh').replace('#version 330', '#version 330\n' + definitions, 1)
    ctx.program(vertex_shader=expand(vs), fragment_shader=expand(fs)).release()

vs = '#version 330\n#extension GL_ARB_separate_shader_objects : require\nlayout(location=0) out vec2 texCoord;void main(){vec2 uv=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(uv*2-1,0,1);texCoord=uv;}'
program = ctx.program(vertex_shader=vs, fragment_shader=expand(read('assets/lbc/shaders/post/polymorph_bloom_sample.fsh')))
program['BlitConfig'].binding = 0
config = ctx.buffer(struct.pack('4f', 1, 1, 1, 1))
config.bind_to_uniform_block(0)
frame = ctx.simple_framebuffer((16, 16), components=4, dtype='f4')
frame.use()
vao = ctx.vertex_array(program, [])
scene = ctx.texture((16, 16), 4, bytes([223, 20, 255, 255]) * 256)
scene.use(0)
program['DiffuseSampler'].value = 0
outlines = ctx.texture((16, 16), 4, bytes([0, 0, 0, 0]) * 256)
outlines.use(1)
program['PolymorphOutlineSampler'].value = 1
vao.render(vertices=3)
assert np.max(np.frombuffer(frame.read(components=4, dtype='f4'), dtype=np.float32)) < 1e-6, 'pink scene blocks must not enter the new bloom'
outlines.write(bytes([223, 20, 255, 255]) * 256)
vao.render(vertices=3)
pixels = np.frombuffer(frame.read(components=4, dtype='f4'), dtype=np.float32).reshape(-1, 4)
assert np.all(pixels[:, 0] > .87), 'the actual glowing sphere must bloom'
image = np.array(Image.open(TEMPLATE / 'assets/lbc/textures/item/polymorph_sphere.png'))
assert set(np.unique(image[:, :, 3])) == {0, 254}
assert np.all(image[8:24, 8:24, 3] == 254), 'sphere interior has no translucent holes'
post = json.loads((TEMPLATE / 'updates/polymorph/end_of_frame.json').read_text())
new_pass = [p for p in post['passes'] if p.get('fragment_shader') == 'lbc:post/polymorph_bloom_sample']
assert len(new_pass) == 1 and {'sampler_name': 'PolymorphOutline', 'target': 'minecraft:entity_outline'} in new_pass[0]['inputs']
assert all(hashlib.sha256((RP / path).read_bytes()).hexdigest() == value for path, value in hashes.items())
OUT.mkdir(parents=True, exist_ok=True)
(OUT / 'gpu-results.json').write_text(json.dumps({
    'renderer': ctx.info['GL_RENDERER'], 'itemVariantsCompile': len(variants),
    'bloomCompile': True, 'pinkSceneWithoutOutlineExcluded': True,
    'actualOutlineIncluded': True, 'sphereInteriorOpaque': True,
    'packReadOnlyHashes': hashes,
}, indent=2))
print('POLYMORPH GPU PASS', ctx.info['GL_RENDERER'])
