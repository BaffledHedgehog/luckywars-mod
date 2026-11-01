"""Read-only pack/native-contract audit and offscreen GPU visual repair proof; no game/server."""
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
PATCH = ROOT / 'resourcepack_template/updates/visual_repair_2026_11_01'
OUT = ROOT / 'build/verification/combat-visual-flight-2026-11-01'
OUT.mkdir(parents=True, exist_ok=True)
JAR = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3.jar')))
SOURCES = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3-sources.jar')))
watched = [p for p in (RP / 'assets').rglob('*') if p.is_file() and '/shaders/' in p.as_posix()]
watched += [RP / path for path in json.loads((PATCH / 'source_manifest.json').read_text())['sourceSha256']]
before = {str(p.relative_to(RP)): hashlib.sha256(p.read_bytes()).hexdigest() for p in watched}
print('Renderer source hashes captured', flush=True)
checks = []


def check(ok, message):
    assert ok, message
    checks.append(message)


def source(path, baseline=False):
    for base in ((RP,) if baseline else (PATCH, RP)):
        file = base / path
        if file.is_file():
            return file.read_text(encoding='utf-8')
    return JAR.read(path).decode()


def expand(text):
    def include(match):
        namespace, name = match[1].split(':', 1)
        return expand(source(f'assets/{namespace}/shaders/include/{name}'))
    return re.sub(r'#include\s+<([^>]+)>', include, text).replace('gl_VertexIndex', 'gl_VertexID')


def invalid_targets(graph, allowed):
    known = set(graph.get('targets', {})) | allowed
    return sorted({row['target'] for p in graph['passes'] for row in p.get('inputs', []) if row['target'] not in known}
                  | {p['output'] for p in graph['passes'] if p['output'] not in known})


native = SOURCES.read('net/minecraft/client/renderer/LevelTargetBundle.java').decode()
check('MAIN_TARGETS = Set.of(MAIN_TARGET_ID)' in native and 'OUTLINE_TARGETS = Set.of(MAIN_TARGET_ID, ENTITY_OUTLINE_TARGET_ID)' in native,
      'native 26.3 MAIN/OUTLINE target accessibility contract inspected')
main = {'minecraft:main'}
outline = main | {'minecraft:entity_outline'}
old_end = json.loads(source('assets/minecraft/post_effect/end_of_frame.json', True))
end = json.loads(source('assets/minecraft/post_effect/end_of_frame.json'))
old_outline = json.loads(source('assets/minecraft/post_effect/entity_outline.json', True))
new_outline = json.loads(source('assets/minecraft/post_effect/entity_outline.json'))
check(invalid_targets(old_end, main) == ['minecraft:entity_outline'], 'reproduced original end_of_frame forbidden target that rejects entire chain')
check(not invalid_targets(end, main), 'repaired end_of_frame uses only native accessible main and its own local targets')
check(not invalid_targets(new_outline, outline), 'polymorph bloom now executes inside legally accessible outline chain')
old_passes = [p for p in new_outline['passes'] if p['fragment_shader'] not in {
    'lbc:post/polymorph_outline_bloom_sample', 'lbc:post/polymorph_outline_bloom'}
    and p['output'] not in {'polymorph_bloom_h', 'polymorph_bloom_v', 'polymorph_bloom_scene'}
    and not any(i['target'] == 'polymorph_bloom_scene' for i in p.get('inputs', []))]
check(old_passes == old_outline['passes'], 'all existing outline/zone/signed-glow/spread passes retained unchanged and ordered')
check(end['passes'][3]['fragment_shader'] == 'minecraft:post/bloom_sample', 'ordinary cyan laser bloom again uses author palette sampler')
native_pipeline = SOURCES.read('net/minecraft/client/renderer/RenderPipelines.java').decode()
check('new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)' in native_pipeline, 'native 26.3 reverse-Z particle comparison inspected')

ctx = moderngl.create_standalone_context(require=330)
print('GPU context ready: ' + ctx.info['GL_RENDERER'], flush=True)


def program(vertex, fragment, definitions=''):
    def shader(path):
        return expand(source(path).replace('#version 330', '#version 330\n' + definitions, 1))
    return ctx.program(vertex_shader=shader(vertex), fragment_shader=shader(fragment))


def uniforms(prog, size, projection=None):
    buffers = []
    for name in prog:
        member = prog[name]
        if not isinstance(member, moderngl.UniformBlock):
            continue
        member.binding = len(buffers)
        data = bytearray(member.size)
        if name == 'DynamicTransforms':
            data[:64] = np.eye(4, dtype='f4').tobytes()
            data[64:128] = np.eye(4, dtype='f4').tobytes()
            struct.pack_into('4f', data, 128, 1, 1, 1, 1)
        elif name == 'Projection':
            data[:64] = (np.eye(4, dtype='f4') if projection is None else projection).T.tobytes()
        elif name == 'Globals':
            struct.pack_into('2f', data, 32, *size)
        elif name == 'Fog':
            struct.pack_into('6f', data, 16, 1000, 2000, 1000, 2000, 2000, 2000)
        elif name == 'BlitConfig':
            struct.pack_into('4f', data, 0, 1, 1, 1, 1)
            if member.size >= 24:
                struct.pack_into('2f', data, 16, 0, 1)
        buffer = ctx.buffer(data)
        buffer.bind_to_uniform_block(member.binding)
        buffers.append(buffer)
    return buffers


screen = 'assets/minecraft/shaders/core/screenquad.vsh'
size = (256, 128)
frame = ctx.simple_framebuffer(size, components=4)


def post_render(prog, textures):
    blocks = uniforms(prog, size)
    for index, (name, texture) in enumerate(textures.items()):
        if name in prog:
            texture.use(index)
            prog[name].value = index
    frame.use()
    ctx.disable(moderngl.DEPTH_TEST | moderngl.BLEND | moderngl.CULL_FACE)
    vao = ctx.vertex_array(prog, [])
    vao.render(vertices=3)
    result = np.frombuffer(frame.read(components=4), dtype='u1').reshape(size[1], size[0], 4).copy()
    vao.release()
    for block in blocks:
        block.release()
    return result


def texture(array):
    tex = ctx.texture((array.shape[1], array.shape[0]), 4, array.tobytes())
    tex.filter = (moderngl.NEAREST, moderngl.NEAREST)
    return tex


yy, xx = np.indices((size[1], size[0]))
scene_data = np.stack([30 + xx % 130, 40 + yy % 110, 90 + xx % 90, (xx + yy * 3) % 256], axis=-1).astype('u1')
scene = texture(scene_data)
mask_data = np.zeros_like(scene_data)
mask_data[60:68, 124:132] = [223, 20, 255, 255]
mask_data[2, :57] = [66, 72, 100, 0]  # an invisible black-hole record is not sphere emission
mask_data[25:30, 25:30] = [255, 100, 40, 255]  # unrelated glowing entity
mask = texture(mask_data)
sample = program(screen, 'assets/lbc/shaders/post/polymorph_outline_bloom_sample.fsh')
sampled = post_render(sample, {'PolymorphOutlineSampler': mask})
check(np.array_equal(sampled[60:68, 124:132], mask_data[60:68, 124:132]), 'actual GPU sphere emission mask preserves authored outline palette')
check(np.count_nonzero(sampled[:, :, 3]) == 64, 'GPU sphere bloom ignores terrain, ordinary glow and invisible black-hole records')
composite = program(screen, 'assets/lbc/shaders/post/polymorph_outline_bloom.fsh')
combined = post_render(composite, {'DiffuseSampler': scene, 'BloomSampler': texture(sampled)})
check(np.array_equal(combined[:, :, 3], scene_data[:, :, 3]), 'GPU polymorph composite preserves all 256 alpha bytes including effect transport')
check(int(combined[55:73, 120:136, :3].sum()) > int(scene_data[55:73, 120:136, :3].sum()), 'GPU sphere outline produces visible bloom')
laser_palette = program(screen, 'assets/minecraft/shaders/post/bloom_sample.fsh')
cyan = np.zeros_like(scene_data)
cyan[62:66, 80:176] = [51, 220, 255, 255]
emission = post_render(laser_palette, {'DiffuseSampler': texture(cyan)})
check(np.array_equal(emission[63, 100], cyan[63, 100]), 'GPU original laser cyan palette is accepted once end_of_frame loads')
print('Graph/alpha/sphere/laser checks passed', flush=True)

# Exercise the unchanged real black-hole tracer with a native-format live object record.
fields = [0., 0., -5., .8, 0., -5., 0., .8, -5., 0., 0., -4.2]
f = 1 / np.tan(np.radians(70) / 2)
projection = np.array([[f / 2, 0, 0, 0], [0, f, 0, 0], [0, 0, .05 / 100, .05], [0, 0, -1, 0]], dtype='f4')
fields += np.linalg.inv(projection).T.flatten().tolist()
record = np.zeros((68, 171, 4), dtype='u1')
for index, value in enumerate(fields):
    a, b, c, d = struct.pack('<f', value)
    record[2, index * 2] = [a, b, 201, 255]
    record[2, index * 2 + 1] = [c, d, 202, 255]
record[2, 56] = [66, 72, 100, 255]
record[2, 58] = list(struct.pack('<f', 1.0))
depth = ctx.texture(size, 1, np.zeros((size[1], size[0]), dtype='f4').tobytes(), dtype='f4')
black_hole = program('assets/lbc/shaders/post/black_hole_image_fast.vsh', 'assets/lbc/shaders/post/black_hole_image_fast.fsh')
traced = post_render(black_hole, {'HoleDataSampler': texture(record), 'SceneSampler': scene, 'SceneDepthSampler': depth})
check(int(traced[64, 128, :3].sum()) < 8, 'GPU actual unchanged black-hole tracer renders the central opaque horizon')
check(np.count_nonzero(np.any(traced[:, :, :3] != scene_data[:, :, :3], axis=-1)) > 100, 'GPU actual black-hole tracer lenses scene pixels from canonical carrier fields')
program('assets/lbc/shaders/post/black_hole_image_many.vsh', 'assets/lbc/shaders/post/black_hole_image.fsh').release()
check(True, 'both unchanged fast/many black-hole shader paths compile and link')
print('Black-hole render checks passed', flush=True)

# Draw the ACTUAL radar vertex/fragment shaders against a nearer wall under reverse depth.
particle_v = 'assets/minecraft/shaders/core/particle.vsh'
particle_f = 'assets/minecraft/shaders/core/particle.fsh'
for definitions in ['', '#define OIT_ALPHA_ONLY', '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE']:
    program(particle_v, particle_f, definitions).release()
check(True, 'repaired normal/OIT alpha-only/OIT accumulate particle shaders compile and link')


def radar_render(baseline, green):
    prog = ctx.program(vertex_shader=expand(source(particle_v, baseline)), fragment_shader=expand(source(particle_f)))
    blocks = uniforms(prog, (32, 32))
    tex = ctx.texture((1, 1), 4, bytes([255] * 4))
    tex.use(0); prog['Sampler0'].value = 0
    tex.use(1); prog['Sampler2'].value = 1
    verts = np.zeros(4, dtype=[('p', 'f4', 3), ('uv', 'f4', 2), ('c', 'f4', 4), ('l', 'i4', 2)])
    verts['p'] = [[-.8, -.8, 0], [.8, -.8, 0], [.8, .8, 0], [-.8, .8, 0]]
    verts['c'] = [0, green / 255, 0, 1]
    buf = ctx.buffer(verts.tobytes()); idx = ctx.buffer(np.array([0, 1, 2, 2, 3, 0], dtype='u4').tobytes())
    vao = ctx.vertex_array(prog, [(buf, '3f 2f 4f 2i', 'Position', 'UV0', 'Color', 'UV2')], idx)
    target = ctx.simple_framebuffer((32, 32), components=4)
    target.use(); target.clear(0, 0, 0, 0, depth=.7)
    ctx.enable(moderngl.DEPTH_TEST); ctx.depth_func = '>='
    vao.render()
    count = np.count_nonzero(np.frombuffer(target.read(components=4), dtype='u1').reshape(32, 32, 4)[:, :, 3])
    ctx.disable(moderngl.DEPTH_TEST)
    for obj in [*blocks, vao, idx, buf, target, tex, prog]:
        obj.release()
    return int(count)


for marker in (1, 2):
    check(radar_render(True, marker) == 0 and radar_render(False, marker) > 0,
          f'GPU radar marker{marker} is hidden by wall before repair and visible through wall after reverse-Z repair')
check(radar_render(False, 3) == 0, 'ordinary non-marker particles retain world occlusion')
after = {str(p.relative_to(RP)): hashlib.sha256(p.read_bytes()).hexdigest() for p in watched}
check(before == after, 'all inspected actual resource pack renderer sources unchanged')
result = {'success': True, 'renderer': ctx.info['GL_RENDERER'], 'checks': checks,
          'packRendererFilesReadOnly': len(before), 'physicalMinecraftClientRender': False,
          'baselineForbiddenEndOfFrameTargets': invalid_targets(old_end, main)}
(OUT / 'visual-repair-gpu.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'success': True, 'checks': len(checks), 'renderer': result['renderer']}))
