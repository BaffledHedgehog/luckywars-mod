"""Render the whole vanilla phone mesh with actual core/item shaders, not one tile.

No server, launcher or author-pack writes. Includes subpixel minification and
exact-palette controls; cannot replace a connected-client visual playtest.
"""
import os
for key in ('OPENBLAS_NUM_THREADS','OMP_NUM_THREADS','MKL_NUM_THREADS'):os.environ[key]='1'
import hashlib,importlib.util,json,re,struct,sys,zipfile,zlib
from pathlib import Path
import moderngl as gl
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/verification/feedback_2026_11_15'
PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
ADDON=OUT/'assets-staging'
spec=importlib.util.spec_from_file_location('limits',ROOT/'tools/check_timestop_shaderc.py')
limits=importlib.util.module_from_spec(spec);spec.loader.exec_module(limits);limits.limit_resources()
jar=zipfile.ZipFile(next(p for p in (ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*26.3.jar') if not p.name.endswith('-sources.jar')))
hashes={}
def read(name,candidate):
    for directory in ((ADDON,PACK) if candidate else (PACK,)):
        if (directory/name).exists():
            data=(directory/name).read_bytes();hashes[('candidate:' if directory==ADDON else 'active:')+name]=hashlib.sha256(data).hexdigest();return data.decode()
    data=jar.read(name);hashes['native:'+name]=hashlib.sha256(data).hexdigest();return data.decode()
def source(name,candidate):
    def include(m):
        ns,path=m[1].split(':',1) if ':' in m[1] else ('minecraft',m[1])
        return source(f'assets/{ns}/shaders/include/{path}',candidate)
    return re.sub(r'^\s*#(?:include|moj_import)\s*[<"]([^>"\n]+)[>"]\s*$',include,read(name,candidate),flags=re.M).replace('gl_VertexIndex','gl_VertexID')
ctx=gl.create_standalone_context(require=330)
tile=np.array(Image.open(ADDON/'assets/skywars2/textures/item/tiktok_pixels.png').convert('RGBA'))
old_tile=np.array(Image.open(PACK/'assets/skywars2/textures/item/tiktok_pixels.png').convert('RGBA'))
assert tile.shape==(64,64,4) and np.all(tile[:,:,3]==255)
assert np.array_equal(tile[:,:,:3],old_tile[:,:,:3]),'render-layer change must preserve every RGB marker'
atlas=ctx.texture((64,64),4,tile.tobytes());atlas.filter=(gl.NEAREST,gl.NEAREST)
white=ctx.texture((256,256),4,bytes([255])*(256*256*4))
include=read('assets/lbc/shaders/include/phone_hd_pixels.glsl',True)
palette=np.array(re.findall(r'uvec3\((\d+)u,(\d+)u,(\d+)u\)',include),dtype='u1')
assert palette.shape==(256,3)
W,H=480,854;N=W*H//3
x=np.tile(np.arange(W//3),H);y=np.repeat(np.arange(H),W//3)
positions=np.empty((N,4,3),dtype='f4')
for corner,(dx,dy) in enumerate(((0,0),(0,1),(1,1),(1,0))):
    positions[:,corner,0]=-1+(x+dx)*2/(W//3)
    positions[:,corner,1]=1-(y+dy)*2/H
    positions[:,corner,2]=0
indices=(np.arange(N,dtype='u4')[:,None]*4+np.array([0,1,2,0,2,3],dtype='u4')).tobytes()
uv=np.tile(np.array([[.00001,.00001],[.00001,.99999],[.99999,.99999],[.99999,.00001]],dtype='f4'),(N,1))
reference=ROOT/'build/verification/tiktok/hd-reference'
offsets=np.fromfile(reference/'clip.idx',dtype='>u8')
with (reference/'clip.tkp').open('rb') as f:
    f.seek(int(offsets[len(offsets)//2]));length,jpeg=struct.unpack('>II',f.read(8));pixels=np.frombuffer(zlib.decompress(f.read(length)),dtype='u1').reshape(H,W)
checker=np.tile(np.array([34,119,34],dtype='u1'),(H,W//3))
results=[]

def render(candidate,pixels,size,shift=0,composite=False,defines=''):
    atlas.write((tile if candidate else old_tile).tobytes())
    shader=lambda ext:source('assets/minecraft/shaders/core/item.'+ext,candidate).replace('#version 330','#version 330\n'+defines,1)
    program=ctx.program(vertex_shader=shader('vsh'),fragment_shader=shader('fsh'))
    resources=[];attrs=[];buffers=[]
    transform=np.eye(4,dtype='f4');transform[0,3]=shift*2/size[0]
    local_positions=positions;local_uv=uv;local_atlas=atlas
    colors=np.ones((N,4,4),dtype='f4');colors[:,:,:3]=pixels.reshape(N,3)[:,None,:]/255.
    if composite:
        body=np.array(Image.open(PACK/'assets/minecraft/textures/item/zaika.png').convert('RGBA'))
        sheet=np.zeros((2112,2048,4),dtype='u1');sheet[:2048]=body;sheet[2048:2112,:64]=tile;sheet[2048:2112,64:128,3]=255
        local_atlas=ctx.texture((2048,2112),4,sheet.tobytes());local_atlas.filter=(gl.NEAREST,gl.NEAREST)
        local_positions=positions.copy();video_height=13.7*H/W
        local_positions[:,:,0]=(local_positions[:,:,0]+1)*13.7/2+.15
        local_positions[:,:,1]=(local_positions[:,:,1]+1)*video_height/2+29.5-video_height
        local_positions[:,:,2]=2.2
        plane=lambda top,z:np.array([[[0,top,z],[0,-1,z],[14,-1,z],[14,top,z]]],dtype='f4')
        local_positions=np.concatenate([plane(31,2),plane(29.55,2.1),local_positions])
        local_uv=uv.copy();local_uv[:,0]*=64/2048;local_uv[:,1]=(local_uv[:,1]*64+2048)/2112
        body_uv=np.array([[[0,0],[0,2048/2112],[7.19531/16,2048/2112],[7.19531/16,0]]],dtype='f4')
        black_uv=np.array([[[65/2048,2049/2112],[65/2048,2111/2112],[127/2048,2111/2112],[127/2048,2049/2112]]],dtype='f4')
        local_uv=np.concatenate([body_uv,black_uv,local_uv.reshape(N,4,2)]).reshape(-1,2)
        colors=np.concatenate([np.ones((2,4,4),dtype='f4'),colors])
        transform=np.array([[2/14,0,0,-1],[0,2/32,0,-30/32],[0,0,.1,0],[0,0,0,1]],dtype='f4')
    for name in program:
        block=program[name]
        if not isinstance(block,gl.UniformBlock):continue
        data=bytearray(block.size);identity=np.eye(4,dtype='f4').tobytes()
        if name=='Projection':data[:64]=transform.T.tobytes()
        elif name=='DynamicTransforms':data[:64]=identity;data[64:128]=identity;struct.pack_into('4f',data,128,1,1,1,1)
        elif name=='Lighting':struct.pack_into('3f',data,0,0,0,1);struct.pack_into('3f',data,16,0,0,1)
        elif name=='Fog':
            for i in range(16,len(data)-3,4):struct.pack_into('f',data,i,1e8+i*100)
        block.binding=len(buffers);b=ctx.buffer(data);b.bind_to_uniform_block(block.binding);buffers.append(b)
    count=len(local_positions)*4
    attributes={'Position':(local_positions,'3f'),'Color':(colors,'4f'),'UV0':(local_uv,'2f'),
                'UV3':(np.zeros((count,2),dtype='f4'),'2f'),'UV1':(np.zeros((count,2),dtype='i4'),'2i'),
                'UV2':(np.full((count,2),240,dtype='i4'),'2i'),'Normal':(np.tile(np.array([0,0,1],dtype='f4'),(count,1)),'3f')}
    for name,(values,fmt) in attributes.items():
        if name in program:b=ctx.buffer(values.tobytes());resources.append(b);attrs.append((b,fmt,name))
    ib=ctx.buffer((np.arange(len(local_positions),dtype='u4')[:,None]*4+np.array([0,1,2,0,2,3],dtype='u4')).tobytes());vao=ctx.vertex_array(program,attrs,ib)
    target=ctx.texture(size,4,dtype='f4');depth=ctx.depth_renderbuffer(size);fbo=ctx.framebuffer([target],depth);fbo.use();ctx.viewport=(0,0,*size);fbo.clear(0,0,0,0,depth=0)
    ctx.enable(gl.DEPTH_TEST);ctx.depth_func='>'
    for i,t in enumerate((local_atlas,white,white)):
        if 'Sampler'+str(i) in program:program['Sampler'+str(i)].value=i;t.use(i)
    with ctx.query(time=True) as timer:vao.render()
    image=np.frombuffer(fbo.read(components=4,dtype='f4'),dtype='f4').reshape(size[1],size[0],4)[::-1].copy()
    gpu_ms=timer.elapsed/1e6
    for resource in [vao,ib,fbo,target,depth,*resources,*buffers,program]:resource.release()
    if composite:local_atlas.release()
    return image,gpu_ms

actual,ms=render(True,pixels,(W,H))
error=float(np.max(np.abs(actual[:,:,:3]-palette[pixels]/255.)))
assert error<.00001,error
results.append({'case':'entire real prepared480x854 frame','palettePixels':W*H,'quads':N,'maximumError':error,'gpuMs':ms})
cutout,_=render(True,pixels,(W,H),defines='#define ALPHA_CUTOUT 0.1\n')
assert np.array_equal(cutout,actual),'opaque/cutout path changes video pixels'
variants=['','#define ALPHA_CUTOUT 0.1','#define GLINT 1','#define GLINT 1\n#define GLINT_SPECIAL 1',
          '#define GLINT 1\n#define ALPHA_CUTOUT 0.1','#define OIT_ALPHA_ONLY 1',
          '#define OIT 1\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE 1',
          '#define OIT 1\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE 1\n#define GLINT 1']
for defines in variants:
    shader=lambda ext:source('assets/minecraft/shaders/core/item.'+ext,True).replace('#version 330','#version 330\n'+defines+'\n',1)
    pipeline=ctx.program(vertex_shader=shader('vsh'),fragment_shader=shader('fsh'));pipeline.release()
results.append({'case':'actual native item pipeline variants','compiledVariants':len(variants),'opaqueCutoutPixelsExact':True})
Image.fromarray(np.rint(actual*255).astype('u1')).save(OUT/'whole-phone-candidate.png')
composed,_=render(True,pixels,(560,1280),composite=True)
Image.fromarray(np.rint(np.clip(composed,0,1)*255).astype('u1')).save(OUT/'phone-body-statusbar-candidate.png')
# In the actual original texture, time/battery live in the first~80/2048 rows.
# The full GPU composition must preserve those pixels instead of painting black.
assert np.count_nonzero(composed[:50,:,:3]>.3)>100,'original status bar disappeared'
assert np.mean(composed[100:1100,20:540,3])>.99,'coplanar pixel/plate holes in whole-phone composition'
results.append({'case':'whole original phone/body/status bar +136640 pixel quads','resolution':[560,1280],'depthTest':True,'nativeOpaqueItemShader':True})
reference_color=palette[np.array([34,119,34])].mean(axis=0)/255
for width in (160,240,320):
    height=round(H*width/W);measure={}
    for candidate in (False,True):
        errors=[]
        for shift in (.0,.23,.57):
            image,ms=render(candidate,checker,(width,height),shift)
            errors.append(float(np.mean((image[2:-2,2:-2,:3]-reference_color)**2)))
        measure['candidate' if candidate else 'active']=float(np.mean(errors))
    # The synthetic3-subpixel stripe has a constant box-filtered reference.
    assert measure['candidate']<measure['active']*.55,measure
    results.append({'case':'subpixel minification','width':width,**measure})
model=ADDON/'assets/skywars2/models/item/tiktok_screen.json'
with model.open() as f:prefix=f.read(8000)
assert re.search(r'2\.199\s*\]',prefix) and re.search(r'2\.2\s*\]',prefix)
assert re.search(r'2\.1\s*\]',prefix)
assert re.search(r'29\.5\s*,\s*2\.2',prefix),'video top raised without resampling/cropping'
assert re.search(r'29\.55\s*,\s*2\.1',prefix),'black letterbox must leave original time/battery bar visible'
with zipfile.ZipFile(next((ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*26.3-sources.jar'))) as native:
    bakery=native.read('net/minecraft/client/resources/model/cuboid/FaceBakery.java').decode()
    material=native.read('net/minecraft/client/resources/model/geometry/BakedQuad.java').decode()
    assert '.computeTransparency(' in bakery and 'transparency.hasTranslucent() ? Sheets.translucent' in material
results.append({'case':'native opaque material selection','oldMarkerAlpha':int(old_tile[0,0,3]),'newMarkerAlpha':255,
                'nativeMaterialUsesSpriteTransparency':True,'videoTop':29.5,'letterboxTop':29.55,'originalFaceTop':31})
report={'passed':True,'renderer':ctx.info['GL_RENDERER'],'checks':results,'shaderDependenciesSha256':hashes,
        'scope':'Whole native-tint mesh, fullbright exact palette and fractional minification. Not a connected client capture; remaining vertical/perspective aliasing requires playtest.'}
(OUT/'phone-feedback-gpu.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'passed':True,'checks':results}))
atlas.release();white.release();ctx.release();jar.close()
