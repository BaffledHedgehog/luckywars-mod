"""Offscreen GPU regression for native scoreboard rectangles and staged vertex bases."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
from zipfile import ZipFile

import moderngl
import numpy as np

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--pack', type=Path, required=True)
parser.add_argument('--overlay', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
args.output.mkdir(parents=True, exist_ok=True)
paths = ['assets/minecraft/shaders/core/gui.vsh', 'assets/minecraft/shaders/core/gui.fsh']
sha = lambda data: hashlib.sha256(data).hexdigest()
watched = {path: sha((args.pack / path).read_bytes()) for path in paths}
native_path = root / '.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar'
with ZipFile(native_path) as archive:
    native = {name: archive.read('net/minecraft/client/' + name).decode() for name in [
        'gui/Hud.java', 'gui/GuiGraphicsExtractor.java', 'renderer/state/gui/ColoredRectangleRenderState.java',
        'gui/render/GuiRenderer.java', 'renderer/StagedVertexBuffer.java',
    ]}
assert 'int height = entriesCount * 9;' in native['gui/Hud.java']
assert 'int bottom = graphics.guiHeight() / 2 + height / 3;' in native['gui/Hud.java']
assert 'graphics.fill(left - 2, headerY - 1, right, bottom, backgroundColor);' in native['gui/Hud.java']
assert 'graphics.fill(left - 2, headerY - 9 - 1, right, headerY - 1, headerBackgroundColor);' in native['gui/Hud.java']
assert 'if (x0 < x1)' in native['gui/GuiGraphicsExtractor.java']
assert 'if (y0 < y1)' in native['gui/GuiGraphicsExtractor.java']
assert 'draw.vertexOffset = Mth.roundToward(nextVertexOffset, draw.format.getVertexSize());' in native['renderer/StagedVertexBuffer.java']
assert 'int baseVertex = draw.vertexOffset / draw.format.getVertexSize();' in native['renderer/StagedVertexBuffer.java']
assert 'executeInfo.baseVertex()' in native['gui/render/GuiRenderer.java']
assert 'this.x0(), this.y0()' in native['renderer/state/gui/ColoredRectangleRenderState.java']

vertex = (args.pack / paths[0]).read_text(encoding='utf-8').replace('gl_VertexIndex', 'gl_VertexID')
baseline = (args.pack / paths[1]).read_text(encoding='utf-8')
patched = (args.overlay / paths[1]).read_text(encoding='utf-8')
reference = baseline[:baseline.index('void main()')] + '''void main() {
    if(vertexColor.a==0.0)discard;
    fragColor=vertexColor*ColorModulator;
}
'''
ctx = moderngl.create_standalone_context(require=330)
programs = {name: ctx.program(vertex_shader=vertex, fragment_shader=fragment)
            for name, fragment in [('reference', reference), ('baseline', baseline), ('patched', patched)]}
dynamic = np.zeros(40, dtype='f4')
dynamic[:16] = np.identity(4, dtype='f4').flatten(order='F')
dynamic[16:32] = np.identity(4, dtype='f4').flatten(order='F')
dynamic[32:36] = 1
ubo = ctx.buffer(dynamic.tobytes())
projection = ctx.buffer(reserve=64)
for program in programs.values():
    program['DynamicTransforms'].binding = 0
    program['Projection'].binding = 1
ubo.bind_to_uniform_block(0)
projection.bind_to_uniform_block(1)
dtype = np.dtype([('position', '<f4', (3,)), ('color', 'u1', (4,))])
assert dtype.itemsize == 16  # Native POSITION_COLOR; text quad stride28 occupies112bytes.
clear = (31 / 255, 73 / 255, 119 / 255, 1)
rows = []

def render(program, rectangle, color, base, size, scale, fbo):
    width, height = size
    gui_width, gui_height = width / scale, height / scale
    matrix = np.identity(4, dtype='f4')
    matrix[0, 0] = 2 / gui_width
    matrix[1, 1] = -2 / gui_height
    matrix[0, 3] = -1
    matrix[1, 3] = 1
    projection.write(matrix.flatten(order='F').tobytes())
    left, top, right, bottom = rectangle
    # GuiGraphicsExtractor.fill reverses bounds, then ColoredRectangleRenderState
    # emits x0/y0, x0/y1, x1/y1, x1/y0. Indices include the native baseVertex.
    data = np.zeros(base + 4, dtype=dtype)
    data['position'][base:] = [(right, bottom, 0), (right, top, 0), (left, top, 0), (left, bottom, 0)]
    data['color'][base:] = color
    vertices = ctx.buffer(data.tobytes())
    indices = ctx.buffer((np.array([0, 1, 2, 2, 3, 0], dtype='u4') + base).tobytes())
    vao = ctx.vertex_array(program, [(vertices, '3f 4f1', 'Position', 'Color')], index_buffer=indices)
    try:
        fbo.use()
        fbo.clear(*clear)
        ctx.viewport = (0, 0, width, height)
        vao.render(mode=moderngl.TRIANGLES)
        x0 = max(0, int(left * scale) - 2)
        y0 = max(0, height - int(np.ceil(bottom * scale)) - 2)
        x1 = min(width, int(np.ceil(right * scale)) + 2)
        y1 = min(height, height - int(top * scale) + 2)
        return np.frombuffer(fbo.read(viewport=(x0, y0, x1 - x0, y1 - y0), components=4, alignment=1), dtype='u1').copy()
    finally:
        vao.release(); indices.release(); vertices.release()

try:
    for size, scale in [((640, 360), 1), ((853, 479), 2), ((1365, 767), 3), ((1919, 1079), 4)]:
        fbo = ctx.simple_framebuffer(size, components=4)
        try:
            width = int(np.ceil(size[0] / scale)); height = int(np.ceil(size[1] / scale))
            cases = []
            for count, text_width in [(1, 0), (1, 32), (7, 90), (15, 180)]:
                bottom = height // 2 + count * 9 // 3
                header_y = bottom - count * 9
                for kind, top, lower in [('body', header_y - 1, bottom), ('title', header_y - 10, header_y - 1)]:
                    cases.append((f'{kind}-{count}-{text_width}', (width - text_width - 5, top, width - 1, lower), (0, 0, 0), True))
            cases += [
                ('ordinary-menu', (30, 35, 120, 70), (0, 0, 0), False),
                ('right-panel-wrong-y', (width - 45, 20, width - 1, 30), (0, 0, 0), False),
                ('right-panel-wrong-height', (width - 45, height // 2 - 7, width - 1, height // 2 + 7), (0, 0, 0), False),
                ('colored-sidebar', (width - 45, height // 2 - 7, width - 1, height // 2 + 3), (17, 51, 93), False),
            ]
            #0/7/14/21 follow0/1/2/3 preceding native28byte font quads.4checks another aligned base.
            for base in [0, 4, 7, 14, 21]:
                for alpha in [1, 77, 102, 255]:
                    for name, rectangle, rgb, remove in cases:
                        native_image = render(programs['reference'], rectangle, (*rgb, alpha), base, size, scale, fbo)
                        old_image = render(programs['baseline'], rectangle, (*rgb, alpha), base, size, scale, fbo)
                        new_image = render(programs['patched'], rectangle, (*rgb, alpha), base, size, scale, fbo)
                        blank = np.tile(np.array([31, 73, 119, 255], dtype='u1'), native_image.size // 4)
                        assert np.any(native_image != blank), ('reference rectangle clipped', size, scale, name)
                        expected = blank if remove else native_image
                        rows.append({'size': size, 'guiScale': scale, 'baseVertex': base, 'case': name, 'alpha': alpha,
                                     'sidebar': remove, 'baselinePassed': np.array_equal(old_image, expected),
                                     'patchedPassed': np.array_equal(new_image, expected)})
        finally:
            fbo.release()
finally:
    for program in programs.values(): program.release()
    ubo.release(); projection.release()
assert watched == {path: sha((args.pack / path).read_bytes()) for path in paths}, 'Author pack was modified'
failures = [row for row in rows if not row['patchedPassed']]
baseline_failures = [row for row in rows if not row['baselinePassed']]
result = {'passed': not failures and bool(baseline_failures), 'renderer': ctx.info['GL_RENDERER'],
          'cases': len(rows), 'patchedFailures': failures, 'baselineFailures': len(baseline_failures),
          'baselineFailureVertexPhases': sorted({row['baseVertex'] % 4 for row in baseline_failures}),
          'authorPackUnchanged': True, 'authorBaseSha256': watched,
          'nativeContracts': {name: sha(text.encode()) for name, text in native.items()},
          'nativeAllocationExample': {'precedingFontQuadBytes': 4 * 28, 'positionColorStride': 16, 'baseVertex': 7},
          'physicalClientReloadObserved': False, 'rows': rows}
(args.output / 'GPU_RESULTS.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({key: value for key, value in result.items() if key not in ('rows', 'nativeContracts', 'authorBaseSha256')}))
assert result['passed'], f'{len(failures)} patched GPU cases failed; baseline failures={len(baseline_failures)}'
