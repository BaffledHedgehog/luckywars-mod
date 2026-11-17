"""Native GUI geometry regression; candidate is diagnostic, not an installable reticle patch."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
from zipfile import ZipFile

import moderngl as gl
import numpy as np
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--pack', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
root = Path(__file__).resolve().parents[1]
base = root / '.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3'
vertex_path = 'assets/minecraft/shaders/core/position_tex_color.vsh'
fragment_path = 'assets/minecraft/shaders/core/position_tex_color.fsh'
sha = lambda data: hashlib.sha256(data).hexdigest()
watched = {name: sha((args.pack / name).read_bytes()) for name in (vertex_path, fragment_path)}
old_vertex = (args.pack / vertex_path).read_text(encoding='utf-8')
old_fragment = (args.pack / fragment_path).read_text(encoding='utf-8')
with ZipFile(base / 'minecraft-merged-d6f9bb6878-26.3.jar') as jar:
    vanilla_vertex = jar.read(vertex_path).decode()
    vanilla_fragment = jar.read(fragment_path).decode()
    sprite = Image.open(io.BytesIO(jar.read('assets/minecraft/textures/gui/sprites/hud/crosshair.png'))).convert('RGBA')
    assert sprite.size == (15, 15)
with ZipFile(base / 'minecraft-merged-d6f9bb6878-26.3-sources.jar') as jar:
    sources = {name: jar.read(name).decode() for name in [
        'net/minecraft/client/gui/Hud.java', 'net/minecraft/client/renderer/GameRenderer.java',
        'net/minecraft/server/level/ClientInformation.java', 'com/mojang/renderpearl/backend/opengl/GlCommandEncoder.java',
        'com/mojang/renderpearl/backend/vulkan/VulkanGpuSurface.java', 'net/minecraft/client/renderer/RenderPipelines.java',
    ]}
assert '(graphics.guiWidth() - 15) / 2, (graphics.guiHeight() - 15) / 2, 15, 15' in sources['net/minecraft/client/gui/Hud.java']
renderer = sources['net/minecraft/client/renderer/GameRenderer.java']
render = renderer[renderer.index('public void render()'):renderer.index('private void preparePostEffects')]
assert render.index('this.applyPostEffects();') < render.index('this.guiRenderer.render();')
assert 'blitFrameBuffers(this.drawFbo, 0,' in sources['com/mojang/renderpearl/backend/opengl/GlCommandEncoder.java']
assert 'vkCmdBlitImage(' in sources['com/mojang/renderpearl/backend/vulkan/VulkanGpuSurface.java']
assert 'guiScale' not in sources['net/minecraft/server/level/ClientInformation.java']

# WIP: fixes recognition and restores native positions. DO NOT install it alone:
# the existing post pass still cancels a forced physical-size crosshair mask.
candidate = old_vertex[:old_vertex.index('void main()')] + '''void main() {
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
wip = args.output / 'NOT_FOR_INSTALL' / vertex_path
wip.parent.mkdir(parents=True, exist_ok=True)
wip.write_text(candidate, encoding='utf-8', newline='\n')
ctx = gl.create_standalone_context(require=330)
programs = {name: ctx.program(vertex_shader=v.replace('gl_VertexIndex', 'gl_VertexID'), fragment_shader=f)
            for name, v, f in [('vanilla', vanilla_vertex, vanilla_fragment), ('baseline', old_vertex, old_fragment),
                               ('candidate', candidate, old_fragment)]}
dynamic = np.zeros(40, dtype='f4')
dynamic[:16] = np.eye(4, dtype='f4').flatten(order='F')
dynamic[16:32] = np.eye(4, dtype='f4').flatten(order='F')
dynamic[32:36] = 1
ubos = [ctx.buffer(dynamic.tobytes()), ctx.buffer(reserve=64), ctx.buffer(reserve=48)]
for program in programs.values():
    for index, name in enumerate(('DynamicTransforms', 'Projection', 'Globals')):
        if name in program: program[name].binding = index
    program['Sampler0'].value = 0
for index, buffer in enumerate(ubos): buffer.bind_to_uniform_block(index)
atlas = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
atlas.paste(sprite, (64, 32))
texture = ctx.texture(atlas.size, 4, atlas.tobytes())
texture.filter = (gl.NEAREST, gl.NEAREST)
texture.use(0)
dtype = np.dtype([('position', '<f4', (3,)), ('uv', '<f4', (2,)), ('color', 'u1', (4,))])
assert dtype.itemsize == 24
rows = []

def draw(program, frame, scale, vertex_base):
    width, height = frame.size
    gui_w, gui_h = width / scale, height / scale
    x = (int(np.ceil(gui_w)) - 15) // 2
    y = (int(np.ceil(gui_h)) - 15) // 2
    matrix = np.eye(4, dtype='f4')
    matrix[0, 0] = 2 / gui_w; matrix[1, 1] = -2 / gui_h
    matrix[0, 3] = -1; matrix[1, 3] = 1
    ubos[1].write(matrix.flatten(order='F').tobytes())
    global_data = bytearray(48); struct.pack_into('2f', global_data, 32, width, height); ubos[2].write(global_data)
    data = np.zeros(vertex_base + 4, dtype=dtype)
    data['position'][vertex_base:] = [(x,y,0),(x,y+15,0),(x+15,y+15,0),(x+15,y,0)]
    data['uv'][vertex_base:] = [(64/256,32/256),(64/256,47/256),(79/256,47/256),(79/256,32/256)]
    data['color'][vertex_base:] = (255, 255, 255, 255)
    vbo = ctx.buffer(data.tobytes()); ibo = ctx.buffer((np.array([0,1,2,2,3,0],dtype='u4')+vertex_base).tobytes())
    vao = ctx.vertex_array(program, [(vbo,'3f 2f 4f1','Position','UV0','Color')], index_buffer=ibo)
    try:
        frame.use(); ctx.viewport=(0,0,width,height); frame.clear(64/255,128/255,192/255,1)
        ctx.enable(gl.BLEND); ctx.blend_func=(gl.ONE_MINUS_DST_COLOR,gl.ONE_MINUS_SRC_COLOR,gl.ONE,gl.ZERO)
        vao.render(mode=gl.TRIANGLES); ctx.disable(gl.BLEND)
        radius=15*scale+4;left=max(0,width//2-radius);bottom=max(0,height//2-radius)
        return frame.read(viewport=(left,bottom,min(width-left,radius*2),min(height-bottom,radius*2)),components=4,alignment=1)
    finally:
        vao.release();ibo.release();vbo.release()

try:
    for size, scales in [((640,360),(1,2)),((853,479),(1,2)),((1920,1080),(1,2,3,4)),((1919,1079),(1,2,3,4)),((2560,1440),(1,2,3,4,5,6))]:
        frame=ctx.simple_framebuffer(size,components=4)
        try:
            for scale in scales:
                previous=None
                for vertex_base in (0,3,5,14):
                    golden=draw(programs['vanilla'],frame,scale,vertex_base)
                    old=draw(programs['baseline'],frame,scale,vertex_base)
                    actual=draw(programs['candidate'],frame,scale,vertex_base)
                    rows.append({'size':size,'guiScale':scale,'baseVertex':vertex_base,
                                 'baselineMatchesVanilla':old==golden,'candidateMatchesVanilla':actual==golden,
                                 'baselineChangesWithVertexBase':previous is not None and old!=previous})
                    previous=old
        finally:frame.release()
finally:
    texture.release()
    for program in programs.values():program.release()
    for buffer in ubos:buffer.release()
assert watched == {name:sha((args.pack/name).read_bytes()) for name in watched}, 'Author pack changed'
result={'scope':'Native GUI stage only; active spread XOR integration unresolved','task14Complete':False,
        'candidateGuiGeometryPassed':all(row['candidateMatchesVanilla'] for row in rows),
        'cases':len(rows),'baselineMismatchCases':sum(not row['baselineMatchesVanilla'] for row in rows),
        'baselinePhaseChanges':sum(row['baselineChangesWithVertexBase'] for row in rows),
        'renderer':ctx.info['GL_RENDERER'],'authorPackUnchanged':True,'authorBaseSha256':watched,
        'nativeSources':{name:sha(source.encode()) for name,source in sources.items()},
        'postRunsBeforeGui':True,'finalPresentUsesHardwareBlit':True,'guiScaleAbsentFromClientInformation':True,
        'candidateInstallable':False,'rows':rows}
(args.output/'GUI_STAGE_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({key:value for key,value in result.items() if key not in ('rows','authorBaseSha256','nativeSources')}))
assert result['candidateGuiGeometryPassed'] and result['baselineMismatchCases']>0 and result['baselinePhaseChanges']>0
