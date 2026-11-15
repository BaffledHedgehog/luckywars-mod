"""Actual core-shader compilation/rendering: packed pixels, arbitrary atlas placement, old paths exact."""
import hashlib,json,re,struct,zipfile
from pathlib import Path
import moderngl
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
PACK=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
ADDON=ROOT/"resourcepack_template/updates/tiktok_hd_2026_11_15"
OUT=ROOT/"build/verification/tiktok";OUT.mkdir(parents=True,exist_ok=True)
BASE=json.loads((ADDON/"PHONE_AUTHOR_BASE_SHA256.json").read_text())
CURRENT={name:hashlib.sha256((PACK/name).read_bytes()).hexdigest() for name in BASE}
jar=zipfile.ZipFile(next(p for p in (ROOT/".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob('*.jar') if not p.name.endswith('-sources.jar')))

def source(name,old):
    baseline=ADDON/'PHONE_BASELINE'/Path(name).name
    if old and name.startswith('assets/minecraft/shaders/core/item.') and baseline.exists():
        data=baseline.read_bytes();assert hashlib.sha256(data).hexdigest()==BASE[name];return data.decode()
    if not old and (ADDON/name).exists():return (ADDON/name).read_text(encoding='utf-8')
    if (PACK/name).exists():
        data=(PACK/name).read_bytes();digest=hashlib.sha256(data).hexdigest()
        CURRENT.setdefault(name,digest)
        BASE[name]=digest;return data.decode()
    return jar.read(name).decode()

def expand(text,old):
    return re.sub(r'#include\s+<([^>]+)>',lambda m:expand(source('assets/'+m[1].split(':',1)[0]+'/shaders/include/'+m[1].split(':',1)[1],old),old),text).replace('gl_VertexIndex','gl_VertexID')

ctx=moderngl.create_standalone_context(require=330)
checks=[];programs={}
variants=[{}, {'ALPHA_CUTOUT':.1},{'GLINT':1},{'GLINT':1,'GLINT_SPECIAL':1}, {'GLINT':1,'ALPHA_CUTOUT':.1},
          {'OIT_ALPHA_ONLY':1},{'OIT':1,'OIT_COEFF_COUNT':4,'OIT_WAVELET_RANK':1,'OIT_ACCUMULATE':1},{'OIT':1,'OIT_COEFF_COUNT':4,'OIT_WAVELET_RANK':1,'OIT_ACCUMULATE':1,'GLINT':1}]
raster_variants=range(5)
for old in (True,False):
    for i,defines in enumerate(variants):
        define='\n'+''.join('#define '+k+' '+str(v)+'\n' for k,v in defines.items())
        def shader(ext):return expand(source('assets/minecraft/shaders/core/item.'+ext,old),old).replace('#version 330','#version 330'+define,1)
        programs[old,i]=ctx.program(vertex_shader=shader('vsh'),fragment_shader=shader('fsh'))
checks.append('original and new actual core/item compile/link including native cutout, glint, glint-special and OIT')

def blocks(program):
    values=[]
    for name in program:
        block=program[name]
        if not isinstance(block,moderngl.UniformBlock):continue
        block.binding=len(values);data=bytearray(block.size)
        if name=='Projection':data[:64]=np.eye(4,dtype='f4').tobytes()
        if name=='DynamicTransforms':
            data[:64]=np.eye(4,dtype='f4').tobytes();data[64:128]=np.eye(4,dtype='f4').tobytes()
            struct.pack_into('4f',data,128,1,1,1,1)
        if name=='Lighting':
            struct.pack_into('3f',data,0,0,0,1);struct.pack_into('3f',data,16,0,0,1)
        if name=='Fog':
            for offset in range(16,len(data)-3,4):struct.pack_into('f',data,offset,1000000.+offset*10000)
        buffer=ctx.buffer(data);buffer.bind_to_uniform_block(block.binding);values.append(buffer)
    return values

def draw(program,atlas,quads,size=(96,32),dark=False):
    positions=[];colors=[];uvs=[];indices=[]
    for lo,hi,tint,uv in quads:
        first=len(positions)
        for xy,st in [((lo[0],hi[1]),(uv[0],uv[1])),((lo[0],lo[1]),(uv[0],uv[3])),((hi[0],lo[1]),(uv[2],uv[3])),((hi[0],hi[1]),(uv[2],uv[1]))]:
            positions.append([*xy,0]);colors.append([*(np.array(tint)/255),1]);uvs.append(st)
        indices.extend([first,first+1,first+2,first,first+2,first+3])
    count=len(positions);attributes={'Position':(positions,'3f'),'Color':(colors,'4f'),'UV0':(uvs,'2f'),
            'UV3':(np.zeros((count,2),dtype='f4'),'2f'),'UV1':(np.zeros((count,2),dtype='i4'),'2i'),'UV2':(np.full((count,2),240,dtype='i4'),'2i'),'Normal':(np.tile([0,0,1],(count,1)),'3f')}
    resources=blocks(program);textures=[];buffers=[]
    texture=ctx.texture((atlas.shape[1],atlas.shape[0]),4,atlas.astype('u1').tobytes());texture.filter=(moderngl.NEAREST,moderngl.NEAREST);texture.use(0);textures.append(texture)
    for sampler,unit,color in [('Sampler0',0,None),('Sampler1',1,[255,0,0,0] if dark else [0,0,0,255]),('Sampler2',2,[0,0,0,255] if dark else [255]*4),('GlintSampler',3,[0]*4),('DissolveMaskSampler',4,[0]*4)]:
        if sampler not in program:continue
        program[sampler].value=unit
        if color is not None:
            tex=ctx.texture((16,16),4,bytes(color)*256);tex.use(unit);textures.append(tex)
    content=[]
    for name,(data,fmt) in attributes.items():
        if name not in program:continue
        buf=ctx.buffer(np.asarray(data,dtype='i4' if fmt.endswith('i') else 'f4').tobytes());buffers.append(buf);content.append((buf,fmt,name))
    index=ctx.buffer(np.asarray(indices,dtype='u4').tobytes());buffers.append(index)
    vao=ctx.vertex_array(program,content,index);target=ctx.simple_framebuffer(size,components=4,dtype='f4');target.use();target.clear()
    vao.render();pixels=np.frombuffer(target.read(components=4,dtype='f4'),dtype='f4').reshape(size[1],size[0],4).copy()
    for value in [vao,target,*buffers,*textures,*resources]:value.release()
    assert np.isfinite(pixels).all()
    return pixels

tile=np.asarray(Image.open(ADDON/'assets/skywars2/textures/item/tiktok_pixels.png').convert('RGBA'))
include=(ADDON/'assets/lbc/shaders/include/phone_hd_pixels.glsl').read_text()
palette=np.array([[int(s) for s in p] for p in re.findall(r'uvec3\((\d+)u,(\d+)u,(\d+)u\)',include)],dtype='u1')
assert palette.shape==(256,3)
rng=np.random.default_rng(11315);width,height=480,854;frame=rng.integers(4,248,(height,width),dtype='u1')
for origin in [(0,0),(17,19)]:
    atlas=rng.integers(0,256,(128,128,4),dtype='u1');x,y=origin;atlas[y:y+64,x:x+64]=tile
    uv=[(x+.001)/128,(y+.001)/128,(x+63.999)/128,(y+63.999)/128]
    quads=[]
    for row in range(height):
        for col in range(0,width,3):
            packed=[int(frame[row,min(width-1,col+j)]) for j in range(3)]
            quads.append(([-1+col/(width/2),1-(row+1)/(height/2)],[-1+min(width,col+3)/(width/2),1-row/(height/2)],packed,uv))
    image=draw(programs[False,0],atlas,quads,(width,height),dark=True)[::-1]
    assert np.max(np.abs(image[:,:,:3]-palette[frame]/255.))<.00001,(origin,np.max(np.abs(image[:,:,:3]-palette[frame]/255.)))
    assert np.all(image[:,:,3]==1)
    checks.append('all409920real480p pixels exact under dark lightmap/red hurt overlay, atlas '+str(origin))
    del quads
# All native palette indices, including the brightest white/red/yellow, across the actual item variants.
atlas=np.zeros((128,128,4),dtype='u1');atlas[16:80,16:80]=tile;uv=[16.001/128,16.001/128,79.999/128,79.999/128]
quads=[]
for i in range(0,246,3):
    ids=[min(247,4+i+j) for j in range(3)]
    quads.append(([-1+i/123,-1],[-1+(i+3)/123,1],ids,uv))
for variant in raster_variants:
    image=draw(programs[False,variant],atlas,quads,(246,1),dark=True)
    expected=palette[np.minimum(np.arange(246)+4,247)]/255.
    assert np.max(np.abs(image[0,:,:3]-expected))<.00001,variant
checks.append('bright full native palette stays unlit across every item pipeline variant')

# Scope: ordinary textures/colours, a forged single marker texel, and a damaged corner remain byte-identical.
for case in range(20):
    atlas=rng.integers(0,256,(128,128,4),dtype='u1');atlas[:,:,3]=255
    if case==0:atlas[7:71,5:69]=tile;atlas[70,68,0]=44
    if case==1:atlas[7,5]=tile[0,0]
    tint=[255]*3 if case%3==0 else [int(x) for x in rng.integers(0,248,3)]
    uv=[5.01/128,7.01/128,68.99/128,70.99/128]
    quad=[([-1,-1],[1,1],tint,uv)]
    for variant in raster_variants:
        assert np.array_equal(draw(programs[True,variant],atlas,quad),draw(programs[False,variant],atlas,quad)),(case,variant)
checks.append('100 ordinary/forged-marker actual original/new raster comparisons byte-exact')
for name,digest in CURRENT.items():assert hashlib.sha256((PACK/name).read_bytes()).hexdigest()==digest
(ADDON/'PHONE_AUTHOR_BASE_SHA256.json').write_text(json.dumps(BASE,indent=2)+'\n')
result={'passed':True,'gpu':ctx.info['GL_RENDERER'],'checks':checks,'palettePixels':409920,'packedTintCount':136640,'nativePipeline':'core/item',
        'compiledVariantsPerVersion':len(variants),'ordinaryRasterComparisons':100,'connectedMinecraftClientTested':False,
        'authorInputsUnchangedDuringCheck':len(CURRENT),'authorAlreadyMergedPhoneShader':CURRENT.get('assets/minecraft/shaders/core/item.vsh')!=BASE.get('assets/minecraft/shaders/core/item.vsh'),
        'shaderSha256':hashlib.sha256(include.encode()).hexdigest()}
(OUT/'phone-hd-gpu-results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
for program in programs.values():program.release()
ctx.release()
