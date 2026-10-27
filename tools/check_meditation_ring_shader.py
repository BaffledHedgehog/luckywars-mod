"""Compile native item variants and render the analytic warning ring on a GPU."""
from pathlib import Path
import hashlib
import json
import re
import struct
import zipfile

import moderngl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
TEMPLATE = ROOT / 'resourcepack_template'
OUT = ROOT / 'build/verification/meditation-ring'
OUT.mkdir(parents=True, exist_ok=True)
JAR = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3.jar')))
PACK_PATHS = ['assets/minecraft/shaders/core/item.vsh', 'assets/minecraft/shaders/core/item.fsh',
              'assets/lbc/shaders/include/meditation_warning.glsl',
              'assets/lbc/models/item/meditation_warning_ring_green.json']
pack_hashes = {name: hashlib.sha256((RP / name).read_bytes()).hexdigest() for name in PACK_PATHS}


def source(path, original=False):
    for base in ((RP,) if original else (TEMPLATE, RP)):
        p = base / path
        if p.is_file() and p.stat().st_size:
            return p.read_text(encoding='utf-8')
    return JAR.read(path).decode()


def expand(text, original=False):
    def include(match):
        namespace, name = match[1].split(':', 1)
        return expand(source(f'assets/{namespace}/shaders/include/{name}', original), original)
    return re.sub(r'#include\s+<([^>]+)>', include, text).replace('gl_VertexIndex', 'gl_VertexID')


ctx = moderngl.create_standalone_context(require=330)
vs_path = 'assets/minecraft/shaders/core/item.vsh'
fs_path = 'assets/minecraft/shaders/core/item.fsh'
variants = ['', '#define ALPHA_CUTOUT 0.1', '#define OIT_ALPHA_ONLY',
    '#define OIT_ALPHA_ONLY\n#define ALPHA_CUTOUT 0.1',
    '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE',
    '#define GLINT', '#define GLINT\n#define GLINT_SPECIAL',
    '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE\n#define GLINT']
for definitions in variants:
    vs = source(vs_path).replace('#version 330', '#version 330\n' + definitions, 1)
    fs = source(fs_path).replace('#version 330', '#version 330\n' + definitions, 1)
    ctx.program(vertex_shader=expand(vs), fragment_shader=expand(fs)).release()

programs = [ctx.program(vertex_shader=expand(source(vs_path, old), old),
                        fragment_shader=expand(source(fs_path, old), old)) for old in (False, True)]
frame = ctx.simple_framebuffer((384, 384), components=4, dtype='f4')
dtype = np.dtype([('pos', 'f4', 3), ('color', 'f4', 4), ('uv', 'f4', 2),
                  ('overlay', 'i4', 2), ('light', 'i4', 2), ('normal', 'f4', 3)])


def render(program, rgba, angle=90, fade=1, light=1, offset=0, atlas=64, reverse=False, cull=False, additive=False):
    buffers = []
    theta = np.radians(angle)
    model = np.array([[1, 0, 0, 0], [0, np.cos(theta), -np.sin(theta), 0],
                      [0, np.sin(theta), np.cos(theta), 0], [0, 0, 0, 1]], dtype='f4')
    projection = np.diag([1.6, 1.6, .1, 1]).astype('f4')
    for name in program:
        member = program[name]
        if not isinstance(member, moderngl.UniformBlock):
            continue
        member.binding = len(buffers)
        data = bytearray(member.size)
        if name == 'DynamicTransforms':
            data[:64] = model.T.tobytes()
            data[64:128] = np.eye(4, dtype='f4').tobytes()
            struct.pack_into('4f', data, 128, 1, 1, 1, 1)
        elif name == 'Projection':
            data[:64] = projection.T.tobytes()
        elif name == 'Fog':
            struct.pack_into('6f', data, 16, 1000, 2000, 1000, 2000, 2000, 2000)
        elif name == 'Lighting':
            struct.pack_into('3f', data, 0, 0, light, 0)
            struct.pack_into('3f', data, 16, 0, 0, light)
        buffer = ctx.buffer(data)
        buffer.bind_to_uniform_block(member.binding)
        buffers.append(buffer)
    pixels = np.full((atlas, atlas, 4), [17, 25, 33, 255], dtype='u1')
    pixels[16:32, 16:32] = rgba
    textures = [ctx.texture((atlas, atlas), 4, pixels.tobytes()),
                ctx.texture((1, 1), 4, bytes([80, 90, 100, 0])),
                ctx.texture((1, 1), 4, bytes([int(light * 255)] * 3 + [255]))]
    for index, texture in enumerate(textures):
        texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
        texture.use(index)
        if f'Sampler{index}' in program:
            program[f'Sampler{index}'].value = index
    data = np.zeros(offset + 8, dtype=dtype)
    positions = np.array([[-.5, 0, -.5], [-.5, 0, .5], [.5, 0, .5], [.5, 0, -.5]])
    data['pos'][offset:] = np.concatenate([positions, positions[::-1]])
    data['color'] = (fade, fade, fade, 1)
    data['uv'][offset:] = np.tile(np.array([[16.01, 16.01], [16.01, 31.99],
                                          [31.99, 31.99], [31.99, 16.01]]) / atlas, (2, 1))
    data['normal'][offset:offset + 4] = (0, 1, 0)
    data['normal'][offset + 4:] = (0, -1, 0)
    faces = [[0, 1, 2, 2, 3, 0], [4, 5, 6, 6, 7, 4]]
    if reverse:
        faces.reverse()
    vertices = ctx.buffer(data.tobytes())
    indices = ctx.buffer((np.array(faces, dtype='u4').ravel() + offset).tobytes())
    vao = ctx.vertex_array(program, [(vertices, '3f 4f 2f 2i 2i 3f',
        'Position', 'Color', 'UV0', 'UV1', 'UV2', 'Normal')], indices, skip_errors=True)
    frame.use()
    frame.clear(0, 0, 0, 0)
    ctx.disable(moderngl.DEPTH_TEST | moderngl.BLEND | moderngl.CULL_FACE)
    if cull:
        ctx.enable(moderngl.CULL_FACE)
    if additive:
        ctx.enable(moderngl.BLEND)
        ctx.blend_func = (moderngl.ONE, moderngl.ONE)
    vao.render(moderngl.TRIANGLES)
    result = np.frombuffer(frame.read(components=4, dtype='f4'), dtype='f4').reshape(384, 384, 4).copy()
    vao.release()
    vertices.release()
    indices.release()
    for resource in textures + buffers:
        resource.release()
    return result


ring = (43, 216, 147, 252)
reference = render(programs[0], ring)
assert reference[192, 192, 3] == 0 and reference[42, 42, 3] == 0
assert np.allclose(reference[192, 314], np.array([43 / 255, 216 / 255, 147 / 255, .55]), atol=1e-6)
expected_area = np.pi * (.25 - 1 / 9) * (384 * .8) ** 2
assert abs(np.sum(reference[:, :, 3] / .55) / expected_area - 1) < .01
for offset in (0, 4, 12):
    for atlas in (64, 256):
        for reverse in (False, True):
            assert np.allclose(render(programs[0], ring, offset=offset, atlas=atlas, reverse=reverse), reference, atol=1e-6)
for angle in (90, 45, 15, -15, -45, -90):
    image = render(programs[0], ring, angle=angle)
    assert np.isfinite(image).all() and np.max(image[:, :, 3]) > .54
    assert np.allclose(image, render(programs[0], ring, angle=angle, cull=True), atol=1e-6)
    ratio = np.sum(image[:, :, 3]) / np.sum(reference[:, :, 3])
    assert abs(ratio - abs(np.sin(np.radians(angle)))) < .015
assert np.allclose(render(programs[0], ring, light=0), reference, atol=1e-6)
assert np.allclose(render(programs[0], ring, additive=True), reference, atol=1e-6)
faded = render(programs[0], ring, fade=.25)
assert np.allclose(faded[:, :, 3], reference[:, :, 3] * .25, atol=1e-6)
markers = [(255, 14, 14, 253), (43, 216, 147, 253), (255, 14, 14, 251),
           (0, 48, 255, 250), (255, 142, 50, 249), (255, 14, 14, 254),
           (43, 216, 147, 130), (180, 90, 30, 255), (200, 100, 50, 252),
           (43, 216, 147, 255), (43, 216, 146, 252)]
for marker in markers:
    assert np.allclose(render(programs[0], marker, light=.3),
                       render(programs[1], marker, light=.3), atol=1e-6), marker
model = json.loads(source('assets/lbc/models/item/meditation_warning_ring_green.json'))
assert len(model['elements']) == 1 and set(model['elements'][0]['faces']) == {'up', 'down'}
result = dict(passed=True, renderer=ctx.info['GL_RENDERER'], itemShaderVariants=len(variants),
              annulusAreaError=float(abs(np.sum(reference[:, :, 3] / .55) / expected_area - 1)),
              modelElements=1, previousElements=96, nativeQuadOffsets=[0, 4, 12],
              doubleFaceSingleContribution=True, cullAndNoCullBothSides=True,
              atlasIndependent=True, uniformFullbright=True, fadePreserved=True,
              unchangedOtherMaterials=len(markers),
              limitation='Actual item shaders on standalone GPU; no physical in-game client capture.')
result['actualRpUnchanged'] = all(hashlib.sha256((RP / name).read_bytes()).hexdigest() == digest
                                for name, digest in pack_hashes.items())
assert result['actualRpUnchanged'], 'Author pack changed during verification; inspect before merging.'
(OUT / 'gpu-results.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
