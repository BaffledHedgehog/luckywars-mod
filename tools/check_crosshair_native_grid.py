"""Compare the spread pass plus native INVERT blending against Minecraft's GUI grid."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import struct
from zipfile import ZipFile

from check_timestop_shaderc import limit_resources

import moderngl as gl
import numpy as np
from PIL import Image


def main():
    limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, required=True)
    parser.add_argument('--overlay', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    jar_path = next((root / '.gradle/loom-cache/minecraftMaven').rglob('*26.3-sources.jar'))
    studio = gl.create_standalone_context(require=330)
    settings_path = 'assets/lbc/shaders/include/reticle_gui_settings.glsl'
    sprite_path = 'assets/minecraft/textures/gui/sprites/hud/crosshair.png'
    author_hashes = json.loads((args.overlay / 'AUTHOR_BASE_SHA256.json').read_text())
    background = (33, 105, 217)
    inverted = tuple(255 - value for value in background)
    cases = []

    with ZipFile(jar_path.with_name(jar_path.name.replace('-sources', ''))) as jar:
        def asset(name, overlay=True):
            for base in ((args.overlay, args.pack) if overlay else (args.pack,)):
                path = base / name
                if path.is_file():
                    return path.read_bytes()
            return jar.read(name)

        def shader(namespace, name, settings=None, overlay=True):
            path = f'assets/{namespace}/shaders/{name}'
            raw = asset(path, overlay).decode()
            if path == settings_path and settings is not None:
                maximum, unicode = settings
                raw = (f'const int LBC_GUI_SCALE = {maximum};\n'
                       f'const bool LBC_FORCE_UNICODE = {str(unicode).lower()};\n')

            def expand(match):
                ns, loc = match[1].split(':', 1)
                return shader(ns, 'include/' + loc, settings, overlay)

            return re.sub(r'^\s*#include\s+<([^>]+)>\s*$', expand, raw, flags=re.M).replace(
                'gl_VertexIndex', 'gl_VertexID')

        def program(namespace, name, settings=None, overlay=True):
            return studio.program(vertex_shader=shader(namespace, name + '.vsh', settings, overlay),
                                  fragment_shader=shader(namespace, name + '.fsh', settings, overlay))

        with ZipFile(jar_path) as sources:
            renderer = sources.read('net/minecraft/client/renderer/GameRenderer.java').decode()
            render = renderer[renderer.index('public void render()'):renderer.index('private void preparePostEffects')]
            assert render.index('this.applyPostEffects();') < render.index('this.guiRenderer.render();')

        sprite = Image.open(io.BytesIO(asset(sprite_path))).convert('RGBA')
        assert sprite.tobytes() == Image.open(io.BytesIO(jar.read(sprite_path))).convert('RGBA').tobytes()
        atlas_image = Image.new('RGBA', (32, 32))
        atlas_image.paste(sprite, (5, 9))
        atlas = studio.texture(atlas_image.size, 4, atlas_image.tobytes())
        atlas.filter = (gl.NEAREST, gl.NEAREST)
        gui = program('minecraft', 'core/position_tex_color')
        vanilla = studio.program(vertex_shader=jar.read('assets/minecraft/shaders/core/position_tex_color.vsh').decode(),
                                 fragment_shader=jar.read('assets/minecraft/shaders/core/position_tex_color.fsh').decode())
        assert asset('assets/minecraft/shaders/core/position_tex_color.fsh') == asset(
            'assets/minecraft/shaders/core/position_tex_color.fsh', False)
        stamp = program('lbc', 'post/weapon_spread')
        old = program('lbc', 'post/weapon_spread_resolve', overlay=False)
        dynamic = studio.buffer(np.concatenate((np.eye(4).flatten(), np.eye(4).flatten(), [1,1,1,1,0,0,0,0])).astype('f4'))
        projection = studio.buffer(reserve=64)
        globals_buffer = studio.buffer(reserve=48)

        def draw_gui(gpu, fbo, size, scale, left, top, base_vertex):
            w, h = size
            matrix = np.eye(4, dtype='f4')
            matrix[0, 0], matrix[1, 1] = 2 * scale / w, -2 * scale / h
            matrix[0, 3], matrix[1, 3] = -1, 1
            projection.write(matrix.T.tobytes())
            globals_buffer.write(struct.pack('2f', w, h), offset=32)
            for index, (name, buffer) in enumerate((('DynamicTransforms', dynamic), ('Projection', projection),
                                                    ('Globals', globals_buffer))):
                if name in gpu:
                    gpu[name].binding = index
                    buffer.bind_to_uniform_block(index)
            atlas.use(0)
            gpu['Sampler0'].value = 0
            vertices = np.zeros((base_vertex + 4, 9), dtype='f4')
            vertices[base_vertex:] = [(left, top, 0, 5/32, 9/32, 1, 1, 1, 1),
                                      (left, top+15, 0, 5/32, 24/32, 1, 1, 1, 1),
                                      (left+15, top+15, 0, 20/32, 24/32, 1, 1, 1, 1),
                                      (left+15, top, 0, 20/32, 9/32, 1, 1, 1, 1)]
            vbo = studio.buffer(vertices)
            ibo = studio.buffer(np.array([0,1,2,2,3,0], dtype='u4') + base_vertex)
            vao = studio.vertex_array(gpu, [(vbo, '3f 2f 4f', 'Position', 'UV0', 'Color')], index_buffer=ibo)
            fbo.use()
            studio.enable(gl.BLEND)
            studio.blend_func = (gl.ONE_MINUS_DST_COLOR, gl.ONE_MINUS_SRC_COLOR, gl.ONE, gl.ZERO)
            vao.render(mode=gl.TRIANGLES)
            studio.disable(gl.BLEND)
            vao.release()
            ibo.release()
            vbo.release()

        def project(size, angle, row):
            # Replay the input of the real stamp shader, including the mirrored header row.
            w, h = size
            data = np.zeros((h, w, 4), dtype='u1')
            if angle is not None:
                fov_y = 1.4
                fields = [0,0,0,angle,0,0,0,1,0,0,0,0,fov_y*h/w,fov_y,0,0,1,1,0,0,0,1,0]
                for i, value in enumerate(fields):
                    a, b, c, d = struct.pack('<f', value)
                    data[row, 2*i:2*i+2] = [(a,b,201,255),(c,d,202,255)]
                data[row, 56] = (83,80,82,255)
            return studio.texture(size, 4, data.tobytes())

        def full_pass(gpu, fbo, **inputs):
            fbo.use()
            for unit, (name, texture) in enumerate(inputs.items()):
                texture.use(unit)
                gpu[name].value = unit
            vao = studio.vertex_array(gpu, [])
            vao.render(vertices=3)
            vao.release()

        def picture(frame, w, h):
            return np.frombuffer(frame.read(components=3, alignment=1), dtype='u1').reshape(h, w, 3)

        detected_baseline_bug = False
        settings_source = asset(settings_path).decode()
        chosen_max = int(re.search(r'LBC_GUI_SCALE = (\d+);', settings_source)[1])
        chosen_unicode = 'LBC_FORCE_UNICODE = true;' in settings_source
        for (w, h), maximum, unicode in (
            ((640, 360), 1, False), ((853, 479), 3, False), ((853, 480), 2, False),
            ((1280, 721), 3, False), ((1920, 1080), chosen_max, chosen_unicode),
            ((1919, 1079), 3, False), ((1919, 1079), 4, False),
            ((2560, 1441), 0, False), ((1280, 961), 3, True), ((640, 359), 0, True),
        ):
            active = program('lbc', 'post/weapon_spread_resolve', (maximum, unicode))
            scale = 1
            while scale != maximum and w // (scale+1) >= 320 and h // (scale+1) >= 240:
                scale += 1
            if unicode and scale % 2:
                scale += 1
            left = ((w + scale - 1) // scale - 15) // 2
            top = ((h + scale - 1) // scale - 15) // 2
            cx, cy = (left + 7)*scale, h - (top + 8)*scale
            yy, xx = np.ogrid[:h, :w]
            lx, ly = xx-cx, yy-cy
            native_image = studio.simple_framebuffer((w, h), components=4)
            scene = studio.texture((w, h), 4, bytes((*background,255))*(w*h))
            stamped = studio.texture((w, h), 4)
            stamp_frame = studio.framebuffer([stamped])

            for angle in (None, 0, .07, .38, 1.6, 8.0):
                phase = 3 if angle is None else (5 if angle < 1 else 14)
                row = 58 if phase == 5 else h - 59
                record = project((w,h), angle, row)
                full_pass(stamp, stamp_frame, SceneSampler=scene, SpreadDataSampler=record)
                full_pass(active, native_image, SceneSampler=stamped)
                draw_gui(gui, native_image, (w,h), scale, left, top, phase)
                actual = picture(native_image, w, h)

                if angle is None:
                    native_image.clear(*(value/255 for value in background), 1)
                    draw_gui(vanilla, native_image, (w,h), scale, left, top, phase)
                    assert np.array_equal(actual, picture(native_image, w, h)), ('native', w,h,scale,phase)
                else:
                    # Compare a full frame to an independent logical-cell oracle.
                    reach = int(np.floor(np.tan(np.radians(angle))*1.4*h/(2*scale)+.5))
                    x, y = np.abs(lx//scale), np.abs(ly//scale)
                    mask = ((lx >= 0) & (lx < max(2,scale)) & (ly >= 0) & (ly < max(2,scale)))
                    mask |= ((y == 0) & (x >= max(1,reach-3)) & (x <= reach))
                    mask |= ((x == 0) & (y >= max(1,reach-3)) & (y <= reach))
                    assert np.all(actual[mask] == inverted) and np.all(actual[~mask] == background), (
                        'reticle', w,h,scale,phase,angle, int(mask.sum()),
                        int(np.count_nonzero(np.any(actual != background, axis=2))))
                    if (w,h,scale,angle) == (1920,1080,3,0):
                        full_pass(old, native_image, SceneSampler=stamped)
                        draw_gui(gui, native_image, (w,h), scale, left, top, phase)
                        detected_baseline_bug = not np.array_equal(actual, picture(native_image, w, h))
                cases.append({'size':[w,h], 'guiScale':scale, 'baseVertex':phase, 'degrees':angle})
                record.release()
            native_image.release()
            stamp_frame.release()
            stamped.release()
            scene.release()
            active.release()

        assert detected_baseline_bug, 'Negative control failed to reproduce the old guessed-grid mismatch'
        for name, digest in author_hashes.items():
            original = args.pack / name
            current = hashlib.sha256(original.read_bytes()).hexdigest() if original.is_file() else None
            assert current == digest, name
        result = {'success':True, 'renderer':studio.info['GL_RENDERER'], 'cases':cases,
                  'nativeSpriteAndGuiBlend':True, 'oldGridMismatchObserved':detected_baseline_bug,
                  'authorPackUnchanged':True, 'physicalMinecraftFrameObserved':False}
        args.output.mkdir(parents=True, exist_ok=True)
        (args.output/'GPU_RESULTS.json').write_text(json.dumps(result, indent=2)+'\n')
        print(f"PASS {len(cases)} native GUI + spread GPU cases on {result['renderer']}")


if __name__ == '__main__':
    main()
