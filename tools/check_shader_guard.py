"""GPU-check shader guard geometry and the installed/overlay vertex shaders.

This is an isolated OpenGL model test, not a Minecraft client reload.
"""
import json, re, struct, zipfile
from pathlib import Path
import moderngl as gl
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
OVER=ROOT/'resourcepack_template/updates/shader_guard_2026_11_23'
OUT=ROOT/'build/verification/shader-guard-2026-11-23'
OUT.mkdir(parents=True,exist_ok=True)
jar=zipfile.ZipFile(next(p for p in (ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*26.3.jar') if not p.name.endswith('-sources.jar')))
def source(name, patched=True):
    for base in ((OVER,PACK) if patched else (PACK,)):
        p=base/name
        if p.is_file(): data=p.read_text(encoding='utf-8'); break
    else: data=jar.read(name).decode()
    if not patched:
        data=re.sub(r'    // Private shader-off warning: clip only the reserved marker\.\n    if \([^\n]+\) \{\n        gl_Position = vec4\(2\.0, 2\.0, 2\.0, 1\.0\);\n        return;\n    \}\n\n','',data)
    def include(m):
        ns,path=m[1].split(':',1) if ':' in m[1] else ('minecraft',m[1])
        return source(f'assets/{ns}/shaders/include/{path}',patched)
    return re.sub(r'^\s*#(?:include|moj_import)\s*[<\"]([^>\"]+)[>\"]\s*$',include,data,flags=re.M).replace('gl_VertexIndex','gl_VertexID')

model=json.loads((OVER/'assets/lbc/models/item/shader_guard.json').read_text())
assert len(model['elements'])==300
assert json.loads((OVER/'assets/lbc/items/shader_guard.json').read_text())['model']['tints'][0]['value']==0xFE01FB
image=np.array(Image.open(OVER/'assets/lbc/textures/item/shader_guard.png').convert('RGBA'))
assert np.all(image==[0,0,1,255])
vertices=[]
axes={'east':(0,1),'west':(0,-1),'up':(1,1),'down':(1,-1),'south':(2,1),'north':(2,-1)}
for e in model['elements']:
    assert all(-16<=v<=32 for v in e['from']+e['to'])
    face,desc=next(iter(e['faces'].items())); assert desc['uv']==[8,8,8,8] and desc['tintindex']==0
    axis,normal=axes[face]; others=[i for i in range(3) if i!=axis]
    lo=(np.array(e['from'])-8)/4; hi=(np.array(e['to'])-8)/4
    points=[]
    for a,b in [(0,0),(1,0),(1,1),(0,1)]:
        v=lo.copy(); v[axis]=hi[axis] if normal>0 else lo[axis]; v[others[0]]=hi[others[0]] if a else lo[others[0]]; v[others[1]]=hi[others[1]] if b else lo[others[1]]; points.append(v)
    cross=np.cross(points[1]-points[0],points[2]-points[0])
    if cross[axis]*normal<0: points=[points[0],points[3],points[2],points[1]]
    for i in (0,1,2,0,2,3): vertices.append([*points[i],254/255,1/255,251/255,1,.5,.5])
ctx=gl.create_standalone_context(require=330)
ctx.enable(gl.CULL_FACE); ctx.front_face='ccw'; ctx.cull_face='back'
frag='#version 330\nout vec4 outColor; void main(){outColor=vec4(0,0,0,1);}'
programs={}
for kind in ('item','entity','text'):
    for patched in (False,True):
        programs[kind,patched]=ctx.program(vertex_shader=source(f'assets/minecraft/shaders/core/{kind}.vsh',patched),fragment_shader=frag)
texture=ctx.texture((16,16),4,image.tobytes()); texture.filter=(gl.NEAREST,gl.NEAREST); texture.use(0)
width,height=160,90
output=ctx.texture((width,height),4); fbo=ctx.framebuffer([output]); fbo.use(); ctx.viewport=(0,0,width,height)
vertex=ctx.buffer(np.array(vertices,dtype='f4').tobytes())
# 90-degree perspective with the ordinary 0.05-block near plane.
near,far=.05,100.; aspect=width/height
projection=np.array([[1/aspect,0,0,0],[0,1,0,0],[0,0,-(far+near)/(far-near),-2*far*near/(far-near)],[0,0,-1,0]],dtype='f4')
def render(patched,camera,kind='item', marker=True, alpha=255):
    program=programs[kind,patched]
    if 'Sampler0' in program: program['Sampler0'].value=0
    buffers=[]
    view=np.eye(4,dtype='f4'); view[2,3]=-camera
    for name in program:
        block=program[name]
        if not isinstance(block,gl.UniformBlock): continue
        data=bytearray(block.size)
        if name=='DynamicTransforms':
            data[:64]=view.T.tobytes(); data[64:128]=np.eye(4,dtype='f4').tobytes(); struct.pack_into('4f',data,128,1,1,1,1)
        elif name=='Projection': data[:64]=projection.T.tobytes()
        block.binding=len(buffers); buf=ctx.buffer(data); buf.bind_to_uniform_block(len(buffers)); buffers.append(buf)
    fields=[]; fmt=[]
    for n,f in [('Position','3f'),('Color','4f'),('UV0','2f')]:
        if n in program: fields.append(n); fmt.append(f)
        else: fmt.append({'3f':'12x','4f':'16x','2f':'8x'}[f])
    data=np.array(vertices,dtype='f4')
    if not marker: data[:,3]=253/255
    data[:,6]=alpha/255
    case_vertex=ctx.buffer(data.tobytes())
    vao=ctx.vertex_array(program,[(case_vertex,' '.join(fmt),*fields)])
    fbo.clear(.6,.7,.8,1); vao.render()
    pixels=np.frombuffer(fbo.read(components=4),dtype='u1').reshape(height,width,4).copy()
    vao.release(); case_vertex.release()
    for b in buffers:b.release()
    return pixels
results=[]
for camera in (0,.05,.4,1,2,3.5,4,-.05,-.4,-1,-3.5,-4):
    disabled=render(False,camera); active=render(True,camera)
    coverage=float(np.mean(np.all(disabled[:,:,:3]==0,axis=2)))
    hidden=float(np.mean(np.all(active[:,:,:3]==[153,178,204],axis=2)))
    print(camera,coverage,hidden)
    assert coverage==1 and hidden==1,(camera,coverage,hidden)
    results.append({'camera_z':camera,'disabled_black_fraction':coverage,'enabled_background_fraction':hidden})
    if camera in (0,3.5,-3.5):
        Image.fromarray(np.flipud(disabled)).save(OUT/f'disabled-{camera}.png'); Image.fromarray(np.flipud(active)).save(OUT/f'enabled-{camera}.png')
for kind in ('item','entity'):
    assert np.all(render(True,0,kind,False)[:,:,:3]==0), f'{kind}: unrelated tint clipped'
    assert np.all(render(True,0,kind,True)[:,:,:3]==[153,178,204]), f'{kind}: marker visible'
texture.write(bytes([1,0,1,255])*256)
assert np.all(render(True,0,'item')[:,:,:3]==0), 'different texture clipped'
texture.write(image.tobytes())
for active,alpha,background in [(False,181,False),(True,181,True),(True,180,False)]:
    pixels=render(active,0,'text',True,alpha)[:,:,:3]
    expected=[153,178,204] if background else [0,0,0]
    assert np.all(pixels==expected), ('text',active,alpha)
results.append({'case':'item/entity unrelated tint and text alpha selectivity','passed':True})
(OUT/'results.json').write_text(json.dumps({'cases':results,'limitations':'OpenGL geometry test; no Minecraft client, text glyphs, wall depth, interpolation or real camera observed.'},indent=2))
