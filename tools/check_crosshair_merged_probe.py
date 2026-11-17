"""Locally render the merged GUI diagnostic without replacing unrelated pack effects.

Only writes ignored verification output. This is not an installable reticle fix.
"""
import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import re
import struct
from zipfile import ZipFile

import moderngl as gl
import numpy as np
from PIL import Image


def main():
    root = Path(__file__).resolve().parents[1]
    spec = importlib.util.spec_from_file_location('shader_limits', root / 'tools/check_timestop_shaderc.py')
    limits = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(limits)
    limits.limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    vertex = 'assets/minecraft/shaders/core/position_tex_color.vsh'
    fragment = 'assets/minecraft/shaders/core/position_tex_color.fsh'
    resolve = 'assets/lbc/shaders/post/weapon_spread_resolve.fsh'
    original = {name: (args.pack / name).read_bytes() for name in (vertex, fragment, resolve)}
    sha = lambda data: hashlib.sha256(data).hexdigest()
    base = root / '.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3'
    with ZipFile(base / 'minecraft-merged-d6f9bb6878-26.3.jar') as jar:
        vanilla_vertex = jar.read(vertex).decode()
        vanilla_fragment = jar.read(fragment).decode()
        icon = Image.open(io.BytesIO(jar.read('assets/minecraft/textures/gui/sprites/hud/crosshair.png'))).convert('RGBA')
    with ZipFile(base / 'minecraft-merged-d6f9bb6878-26.3-sources.jar') as jar:
        renderer = jar.read('net/minecraft/client/renderer/GameRenderer.java').decode()
        hud = jar.read('net/minecraft/client/gui/Hud.java').decode()
        native_renderer = renderer[renderer.index('public void render()'):renderer.index('private void preparePostEffects')]
        assert native_renderer.index('this.applyPostEffects();') < native_renderer.index('this.guiRenderer.render();')
        assert '(graphics.guiWidth() - 15) / 2, (graphics.guiHeight() - 15) / 2, 15, 15' in hud
    old_vertex = original[vertex].decode()
    # Keep every declaration/helper and the COMPLETE artist fragment. Only the
    # recognized crosshair's geometry changes: use native Position, not baseVertex.
    merged_vertex = old_vertex[:old_vertex.index('void main()')] + '''void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    texCoord0=UV0;
    vertexColor=Color;
    centeredCrosshairScale=vec2(0.0);
    if(abs(ProjMat[3][3]-1.0)<0.0001) {
        vec2 viewport=2.0/abs(vec2(ProjMat[0][0],ProjMat[1][1]));
        vec2 origin=floor((ceil(viewport-0.001)-15.0)*0.5);
        vec2 corner=(Position.xy-origin)/15.0;
        if(all(lessThan(abs(corner-round(corner)),vec2(0.0001))) &&
           all(greaterThanEqual(corner,vec2(0))) && all(lessThanEqual(corner,vec2(1)))) {
            ivec2 spriteOrigin=ivec2(round(UV0*vec2(textureSize(Sampler0,0))-corner*15.0));
            if(vanillaCrosshair(spriteOrigin))centeredCrosshairScale=ScreenSize/viewport;
        }
    }
}
'''
    old_resolve = original[resolve].decode()
    marker = '    fragColor=scene;'
    assert old_resolve.count(marker) == 1
    merged_resolve = old_resolve.replace(marker, marker + '''
    // LOCAL DIAGNOSTIC ONLY: native GUI must invert this after post-processing.
    if(all(lessThan(abs(gl_FragCoord.xy-vec2(size)*.5),vec2(48)))) {
        fragColor=vec4(1.0,0.0,1.0,1.0);return;
    }''')
    merged = {vertex: merged_vertex.encode(), fragment: original[fragment], resolve: merged_resolve.encode()}
    for name, data in merged.items():
        path = args.output / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    for name, data in original.items():
        path = args.output / 'restore' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def expand(source):
        def include(match):
            ns, name = match[1].split(':', 1)
            return expand((args.pack / f'assets/{ns}/shaders/include/{name}').read_text())
        return re.sub(r'^\s*#include\s+<([^>]+)>\s*$', include, source, flags=re.M).replace('gl_VertexIndex', 'gl_VertexID')

    ctx = gl.create_standalone_context(require=330)
    programs = {name: ctx.program(vertex_shader=v.replace('gl_VertexIndex', 'gl_VertexID'), fragment_shader=f)
                for name, v, f in [('native', vanilla_vertex, vanilla_fragment),
                                   ('baseline', old_vertex, original[fragment].decode()),
                                   ('merged', merged_vertex, original[fragment].decode())]}
    dynamic = np.zeros(40, dtype='f4')
    dynamic[:16] = np.eye(4, dtype='f4').flatten(order='F')
    dynamic[16:32] = np.eye(4, dtype='f4').flatten(order='F')
    dynamic[32:36] = 1
    ubos = [ctx.buffer(dynamic.tobytes()), ctx.buffer(reserve=64), ctx.buffer(reserve=48)]
    for p in programs.values():
        for index, name in enumerate(('DynamicTransforms', 'Projection', 'Globals')):
            if name in p:
                p[name].binding = index
        p['Sampler0'].value = 0
    for index, buffer in enumerate(ubos):
        buffer.bind_to_uniform_block(index)
    atlas = Image.new('RGBA', (256, 256))
    atlas.paste(icon, (64, 32))
    tex = ctx.texture(atlas.size, 4, atlas.tobytes())
    tex.filter = (gl.NEAREST, gl.NEAREST)
    dtype = np.dtype([('position', '<f4', (3,)), ('uv', '<f4', (2,)), ('color', 'u1', (4,))])
    assert dtype.itemsize == 24  # Native POSITION_TEX_COLOR layout, including RGBA8.

    def draw(p, fbo, scale, base_vertex, *, at=None, blend=False, clear=True):
        width, height = fbo.size
        gw, gh = width / scale, height / scale
        x, y = ((int(np.ceil(gw)) - 15) // 2, (int(np.ceil(gh)) - 15) // 2) if at is None else at
        matrix = np.eye(4, dtype='f4')
        matrix[0, 0] = 2 / gw
        matrix[1, 1] = -2 / gh
        matrix[0, 3], matrix[1, 3] = -1, 1
        ubos[1].write(matrix.flatten(order='F').tobytes())
        globals_data = bytearray(48)
        struct.pack_into('f', globals_data, 28, .0123)
        struct.pack_into('2f', globals_data, 32, width, height)
        ubos[2].write(globals_data)
        data = np.zeros(base_vertex + 4, dtype=dtype)
        data['position'][base_vertex:] = [(x,y,0),(x,y+15,0),(x+15,y+15,0),(x+15,y,0)]
        data['uv'][base_vertex:] = [(64/256,32/256),(64/256,47/256),(79/256,47/256),(79/256,32/256)]
        data['color'][base_vertex:] = (255,255,255,255)
        vbo = ctx.buffer(data.tobytes())
        ibo = ctx.buffer((np.array([0,1,2,2,3,0], dtype='u4') + base_vertex).tobytes())
        vao = ctx.vertex_array(p, [(vbo, '3f 2f 4f1', 'Position', 'UV0', 'Color')], index_buffer=ibo)
        try:
            fbo.use()
            ctx.viewport = (0, 0, width, height)
            if clear:
                fbo.clear(64/255,128/255,192/255,1)
            tex.use(0)
            if blend:
                ctx.enable(gl.BLEND)
                ctx.blend_func = (gl.ONE_MINUS_DST_COLOR,gl.ONE_MINUS_SRC_COLOR,gl.ONE,gl.ZERO)
            vao.render(mode=gl.TRIANGLES)
            ctx.disable(gl.BLEND)
            # Small readback for the whole drawn sprite; no full-resolution screenshots.
            left, bottom = max(0, int(x*scale)-2), max(0, int(height-(y+15)*scale)-2)
            w, h = min(width-left, int(15*scale)+4), min(height-bottom, int(15*scale)+4)
            return fbo.read(viewport=(left,bottom,w,h), components=4, alignment=1)
        finally:
            vao.release()
            ibo.release()
            vbo.release()

    rows, color_cases = [], []
    frames = [((640,360),(1,2)),((853,479),(1,2)),((1920,1080),(1,2,3,4)),
              ((1919,1079),(1,2,3,4)),((2560,1440),(1,2,3,4,5,6))]
    try:
        for size, scales in frames:
            fbo = ctx.simple_framebuffer(size, components=4)
            try:
                for scale in scales:
                    for phase in (0,3,5,14):
                        native = draw(programs['native'],fbo,scale,phase,blend=True)
                        actual = draw(programs['merged'],fbo,scale,phase,blend=True)
                        rows.append({'size':size,'guiScale':scale,'baseVertex':phase,'exactNative':native==actual})
            finally:
                fbo.release()
        fbo = ctx.simple_framebuffer((640,360), components=4)
        try:
            for rgb in [(78,92,36),(21,25,10),(170,2,2),(74,3,15),(255,255,255),(14,87,211)]:
                for alpha in (0,64,255):
                    atlas.paste((*rgb,alpha),(64,32,79,47))
                    tex.write(atlas.tobytes())
                    for phase in (0,3,5,14):
                        old = draw(programs['baseline'],fbo,2,phase,at=(20,20))
                        actual = draw(programs['merged'],fbo,2,phase,at=(20,20))
                        color_cases.append({'rgb':rgb,'alpha':alpha,'baseVertex':phase,'exactPrevious':old==actual})
            atlas.paste(icon,(64,32))
            tex.write(atlas.tobytes())
            scene = ctx.texture(fbo.size,4,bytes([64,128,192,255])*(640*360))
            scene.filter = (gl.NEAREST,gl.NEAREST)
            post = ctx.program(vertex_shader=expand((args.pack/'assets/lbc/shaders/post/weapon_spread_resolve.vsh').read_text()),
                               fragment_shader=expand(merged_resolve))
            post['SceneSampler'].value = 0
            vao = ctx.vertex_array(post,[])
            try:
                scene.use(0)
                fbo.use()
                ctx.viewport = (0,0,*fbo.size)
                vao.render(vertices=3)
                result = draw(programs['merged'],fbo,2,3,blend=True,clear=False)
                pixels = np.frombuffer(result,dtype='u1').reshape(-1,4)
                inverted = int(np.all(pixels[:,:3] == (0,255,0),axis=1).sum())
                native_pixels = int((np.asarray(icon)[:,:,3]>0).sum())*4
                assert inverted == native_pixels, (inverted,native_pixels)
                order = {'nativePostThenGui':True,'magentaProbeGreenCrosshairPixels':inverted,'nativeExpectedPixels':native_pixels}
            finally:
                vao.release()
                post.release()
                scene.release()
        finally:
            fbo.release()
    finally:
        tex.release()
        for p in programs.values():
            p.release()
        for buffer in ubos:
            buffer.release()
    assert all(row['exactNative'] for row in rows)
    assert all(row['exactPrevious'] for row in color_cases)
    assert all((args.pack/name).read_bytes()==data for name,data in original.items()), 'Author pack changed'
    result = {'success':True,'scope':'Merged diagnostic, actual GPU stages in native source draw order; not a complete reticle fix',
              'installable':False,'physicalMinecraftFrameObserved':False,'renderer':ctx.info['GL_RENDERER'],
              'fragmentByteIdentical':merged[fragment]==original[fragment],
              'nativeGuiCases':len(rows),'preservedEffectCases':len(color_cases),'order':order,
              'authorPackUnchanged':True,'base':{n:sha(d) for n,d in original.items()},
              'merged':{n:sha(d) for n,d in merged.items()},'guiCases':rows,'effectCases':color_cases}
    (args.output/'GPU_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('base','merged','guiCases','effectCases')}))


if __name__ == '__main__':
    main()
