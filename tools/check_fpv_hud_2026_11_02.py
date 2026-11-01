"""Compile and render the FPV HUD offscreen; never writes the author's resource pack."""
from pathlib import Path
import hashlib
import json
import re
import struct
import zipfile

import moderngl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
PATCH = ROOT / 'resourcepack_template/updates/fpv_hud_2026_11_02'
OUT = ROOT / 'build/verification/fpv-control-2026-11-02'
OUT.mkdir(parents=True, exist_ok=True)
JAR = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3.jar')))
SOURCES = zipfile.ZipFile(next((ROOT / '.gradle').rglob('minecraft-merged-d6f9bb6878-26.3-sources.jar')))
watched = [RP / 'assets/lbc/shaders/post/zoom.fsh', RP / 'assets/lbc/post_effect/zoom_magnum.json',
           RP / 'assets/minecraft/post_effect/end_of_frame.json', RP / 'assets/minecraft/post_effect/entity_outline.json']
before = {str(p.relative_to(RP)): hashlib.sha256(p.read_bytes()).hexdigest() for p in watched}
checks = []


def check(condition, message):
    assert condition, message
    checks.append(message)


def source(path):
    for base in (PATCH, RP):
        p = base / path
        if p.is_file():
            return p.read_text(encoding='utf-8')
    return JAR.read(path).decode()


def expand(text):
    def include(match):
        namespace, name = match[1].split(':', 1)
        return expand(source(f'assets/{namespace}/shaders/include/{name}'))
    return re.sub(r'#include\s+<([^>]+)>', include, text).replace('gl_VertexIndex', 'gl_VertexID')


native = SOURCES.read('net/minecraft/client/renderer/LevelTargetBundle.java').decode()
check('MAIN_TARGETS = Set.of(MAIN_TARGET_ID)' in native, 'native 26.3 post effects only expose main externally')
graph = json.loads(source('assets/lbc/post_effect/fpv_drone.json'))
allowed = set(graph['targets']) | {'minecraft:main'}
check(all(p['output'] in allowed and all(i['target'] in allowed for i in p['inputs']) for p in graph['passes']),
      'FPV graph uses main and its local target only; no inaccessible entity_outline target')
check(len(graph['passes']) == 2 and all(not t.get('persistent', False) for t in graph['targets'].values()),
      'two legal passes with one nonpersistent full-resolution target')
check(not (RP / 'assets/lbc/post_effect/fpv_drone.json').exists()
      and not (RP / 'assets/lbc/shaders/post/fpv_drone.fsh').exists(), 'new FPV resource IDs do not overwrite existing author files')
ctx = moderngl.create_standalone_context(require=330)
vertex = expand(source('assets/minecraft/shaders/core/screenquad.vsh'))
fragment = expand(source('assets/lbc/shaders/post/fpv_drone.fsh'))
hud = ctx.program(vertex_shader=vertex, fragment_shader=fragment)
blit = ctx.program(vertex_shader=vertex, fragment_shader=expand(source('assets/minecraft/shaders/post/blit.fsh')))
check(True, 'native GLSL 330 screenquad/FPV and final blit shaders compile/link on GPU')
hud['InSampler'].value = 0
blit['InSampler'].value = 0
blit['BlitConfig'].binding = 0
config = ctx.buffer(struct.pack('4f', 1, 1, 1, 1))
config.bind_to_uniform_block(0)
vao = ctx.vertex_array(hud, [])
copy_vao = ctx.vertex_array(blit, [])


def scene(size, detailed=False):
    width, height = size
    yy, xx = np.indices((height, width))
    data = np.empty((height, width, 4), dtype='u1')
    if not detailed:
        data[:, :, :3] = [34, 43, 52]
    else:
        horizon = height * .43
        sky = yy >= horizon
        t = np.clip((yy - horizon) / (height - horizon), 0, 1)
        data[:, :, 0] = np.where(sky, 95 - 58 * t, 27 + (xx // 45 + yy // 45) % 2 * 5)
        data[:, :, 1] = np.where(sky, 143 - 63 * t, 46 + (xx // 45 + yy // 45) % 2 * 7)
        data[:, :, 2] = np.where(sky, 170 - 60 * t, 45 + (xx // 45 + yy // 45) % 2 * 6)
        # Neutral synthetic flight-view background, not a Minecraft screenshot.
        island = (xx > width * .43) & (xx < width * .57) & (yy > height * .36) & (yy < height * .43)
        data[island, :3] = [69, 89, 54]
    data[:, :, 3] = (xx * 13 + yy * 7) % 256
    return data


def render(data):
    size = (data.shape[1], data.shape[0])
    input_tex = ctx.texture(size, 4, data.tobytes())
    input_tex.filter = (moderngl.NEAREST, moderngl.NEAREST)
    stage = ctx.texture(size, 4)
    stage.filter = (moderngl.NEAREST, moderngl.NEAREST)
    first = ctx.framebuffer(color_attachments=[stage])
    final = ctx.simple_framebuffer(size, components=4)
    ctx.disable(moderngl.BLEND | moderngl.DEPTH_TEST | moderngl.CULL_FACE)
    first.use(); input_tex.use(0); vao.render(vertices=3)
    intermediate = np.frombuffer(first.read(components=4), dtype='u1').reshape(data.shape).copy()
    final.use(); stage.use(0); copy_vao.render(vertices=3)
    result = np.frombuffer(final.read(components=4), dtype='u1').reshape(data.shape).copy()
    for obj in (final, first, stage, input_tex):
        obj.release()
    return intermediate, result


cases = []
for size in [(160, 90), (320, 180), (800, 600), (853, 481), (1280, 720), (1920, 1080), (2560, 1080), (3440, 1440), (900, 1600)]:
    data = scene(size)
    intermediate, actual = render(data)
    width, height = size
    center = (np.arange(width) + .5 >= width * .2) & (np.arange(width) + .5 <= width * .8)
    changed = np.any(actual[:, :, :3] != data[:, :, :3], axis=-1)
    check(np.array_equal(actual[:, center], data[:, center]), f'{width}x{height}: central60% entirely unchanged at every row')
    check(np.array_equal(actual[:, :, 3], data[:, :, 3]), f'{width}x{height}: all256 original alpha values preserved exactly')
    check(np.array_equal(intermediate, actual), f'{width}x{height}: native final blit preserves HUD output bytes')
    check(np.count_nonzero(changed[:, :width // 5]) > 10 and np.count_nonzero(changed[:, -width // 5:]) > 10,
          f'{width}x{height}: both peripheral decorations remain visible')
    check(float(changed.mean()) < .04, f'{width}x{height}: sparse decoration affects under4% of scene pixels')
    mirror_error = int(np.abs(actual[:, :, :3].astype('i2') - actual[:, ::-1, :3].astype('i2')).max())
    check(mirror_error <= 1, f'{width}x{height}: symmetric side geometry within one quantized color byte')
    cases.append({'width': width, 'height': height, 'changedPixels': int(changed.sum()), 'changedFraction': float(changed.mean())})

preview_size = (1920, 1080)
data = scene(preview_size, detailed=True)
_, preview = render(data)
# PNG is an actual offscreen shader render over a synthetic background, not a game capture.
Image.fromarray(preview[::-1, :, :3]).save(OUT / 'fpv-hud-preview-1920x1080.png')
Image.fromarray(preview[::-1, :240, :3]).resize((480, 2160), Image.Resampling.NEAREST).save(OUT / 'fpv-hud-left-detail.png')
check(before == {str(p.relative_to(RP)): hashlib.sha256(p.read_bytes()).hexdigest() for p in watched},
      'inspected actual Magnum and existing post-chain files remain byte-identical')
result = {'success': True, 'renderer': ctx.info['GL_RENDERER'], 'checks': checks, 'viewports': cases,
          'sourceHashesReadOnly': before, 'physicalMinecraftClientRender': False,
          'previewBackground': 'synthetic flight-view colors; actual offscreen GPU shader output'}
(OUT / 'fpv-hud-gpu.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'success': True, 'checks': len(checks), 'viewports': len(cases), 'renderer': result['renderer']}))
