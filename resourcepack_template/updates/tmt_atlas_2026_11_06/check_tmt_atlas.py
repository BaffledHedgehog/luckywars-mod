"""Standalone GPU/static checks. Reads author pack; writes only this update directory."""
from pathlib import Path
import hashlib, json, re, struct, zipfile
import moderngl
import numpy as np
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
JAR = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3.jar')))
def source(path, original=False):
    for base in ((RP,) if original else (HERE, RP)):
        p = base / path
        if p.is_file(): return p.read_text(encoding='utf-8')
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
def render(program, rgba, angle=90, fade=1, light=1, offset=0, atlas=64, reverse=False, cull=False, additive=False, tint=None):
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
    yy, xx = np.indices((atlas, atlas))
    pixels = np.stack([(xx * 17) % 256, (yy * 29) % 256, ((xx + yy) * 11) % 256, np.full_like(xx, 255)], axis=2).astype('u1')
    pixels[16:32, 16:32] = rgba
    textures = [ctx.texture((atlas, atlas), 4, pixels.tobytes()),
                ctx.texture((1, 1), 4, bytes([80, 90, 100, 255])),
                ctx.texture((1, 1), 4, bytes([int(light * 255)] * 3 + [255]))]
    for index, texture in enumerate(textures):
        texture.filter = (moderngl.NEAREST, moderngl.NEAREST)
        texture.use(index)
        if f'Sampler{index}' in program:
            program[f'Sampler{index}'].value = index
    data = np.zeros(offset + 8, dtype=dtype)
    positions = np.array([[-.5, 0, -.5], [-.5, 0, .5], [.5, 0, .5], [.5, 0, -.5]])
    data['pos'][offset:] = np.concatenate([positions, positions[::-1]])
    data['color'] = (fade, fade, fade, 1) if tint is None else (*[v / 255 for v in tint], 1)
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

markers = [(255,14,14,253), (43,216,147,253), (255,14,14,251), (0,48,255,250),
           (255,142,50,249), (255,14,14,254), (43,216,147,130), (180,90,30,255),
           (200,100,50,252), (43,216,147,255), (43,216,146,252), (43,216,147,252)]
for marker in markers:
    for tint in [None, (250, 12, 34)]:
        assert np.allclose(render(programs[0], marker, light=.3, tint=tint),
                           render(programs[1], marker, light=.3, tint=tint), atol=1e-6), marker
carrier = (31,197,83,255)
# Sprite alone and tint alone cannot activate TMT.
assert np.allclose(render(programs[0], carrier), render(programs[1], carrier), atol=1e-6)
reference = render(programs[0], carrier, tint=(250,12,34))
assert np.isfinite(reference).all() and np.max(reference[:,:,3]) == 1
for offset in (0,4,12):
    assert np.allclose(reference, render(programs[0], carrier, tint=(250,12,34), offset=offset), atol=1e-6)
assert np.allclose(reference, render(programs[0], carrier, tint=(250,12,34)), atol=1e-6)
assert not np.allclose(reference, render(programs[0], carrier, tint=(250,12,35)), atol=1e-6)
for angle in (90,45,15,-15,-45,-90):
    image = render(programs[0], carrier, tint=(250,12,34), angle=angle)
    assert np.isfinite(image).all() and np.max(image[:,:,3]) == 1
# Seed tint does not colorize or extinguish procedural output at zero channels.
for seed in (0, 1, 255, 256, 65535):
    image=render(programs[0],carrier,tint=(250,seed>>8,seed&255))
    assert np.max(image[:,:,:3]) > .1
# Check every transported seed against independent integer reference and the GPU.
def hash32(x):
    x = ((x ^ (x >> 16)) * 0x7feb352d) & 0xffffffff
    x = ((x ^ (x >> 15)) * 0x846ca68b) & 0xffffffff
    return x ^ (x >> 16)
def resolution_for(seed):
    ticket = hash32(seed ^ 0x93a761bd) % 12758
    return next(res for bound, res in [(7248,16),(9508,32),(11379,64),(12159,128),(12758,256)] if ticket < bound)
resolution_reference = np.array([resolution_for(seed) for seed in range(65536)], dtype='f4').reshape(256,256)
resolution_program = ctx.program(vertex_shader="""#version 330
void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}""", fragment_shader='#version 330\n' + source('assets/lbc/shaders/include/tmt_atlas.glsl') + """
layout(location=0) out float resolution;
void main() { resolution = float(lbc_tmt_resolution(uint(gl_FragCoord.x) + uint(gl_FragCoord.y) * 256u)); }
""")
resolution_frame=ctx.simple_framebuffer((256,256),components=1,dtype='f4')
resolution_frame.use()
ctx.vertex_array(resolution_program,[]).render(vertices=3)
resolution_gpu=np.frombuffer(resolution_frame.read(components=1,dtype='f4'),dtype='f4').reshape(256,256)
assert np.array_equal(resolution_gpu,resolution_reference)
resolution_counts={str(res):int(np.sum(resolution_gpu==res)) for res in (16,32,64,128,256)}
legacy_counts=(7248,2260,1871,780,599)
for res, weight in zip((16,32,64,128,256),legacy_counts):
    assert abs(resolution_counts[str(res)]/65536-weight/12758)<.01
resolution_visuals={}
for res in (16,32,64,128,256):
    seed=next(seed for seed in range(65536) if resolution_for(seed)==res)
    image=render(programs[0],carrier,tint=(250,seed>>8,seed&255),atlas=512)
    colors=np.unique(image[image[:,:,3]>.99,:3],axis=0)
    resolution_visuals[str(res)]={'seed':seed,'distinctOpaqueColors':len(colors)}
    assert len(colors)>32, (res,len(colors))
    if res>=64: assert len(colors)>1024, (res,len(colors))
    if res<=32: assert len(colors)<=res*res, (res,len(colors))
resolution_frame.release()
resolution_program.release()
for p in HERE.rglob('*.json'):
    if p.name not in ('GPU_RESULTS.json',): json.loads(p.read_text())
baseline=json.loads((HERE/'AUTHOR_BASE_SHA256.json').read_text())
assert all(hashlib.sha256((RP/name).read_bytes()).hexdigest()==digest for name,digest in baseline.items())
result=dict(passed=True, renderer=ctx.info['GL_RENDERER'], compiledLinkedVariants=len(variants),
            unchangedOtherMaterials=len(markers)*2, persistentSeedRepeat=True, distinctSeedDifference=True,
            nativeQuadOffsets=[0,4,12], testedAngles=[90,45,15,-15,-45,-90],
            authorShaderHashesUnchanged=True, weightedResolutionSeedCounts=resolution_counts,
            cpuGpuResolutionAgreementSeeds=65536, resolutionVisualSamples=resolution_visuals,
            maximumCollageFetches=8,
            assetSha256={str(p.relative_to(HERE)).replace(chr(92),'/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((HERE/'assets').rglob('*')) if p.is_file()},
            limitation='Standalone GPU with synthetic atlas/native shader includes; no live Minecraft GUI/hand/drop capture or frame-time benchmark.')
(HERE/'GPU_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
