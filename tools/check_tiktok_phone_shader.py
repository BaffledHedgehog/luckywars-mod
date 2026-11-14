"""Actual core-shader compilation/rendering: packed pixels, arbitrary atlas placement, old paths exact."""
import hashlib,json,re,struct,zipfile
from pathlib import Path
import moderngl
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
PACK=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
ADDON=ROOT/"resourcepack_template/updates/tiktok_phone_2026_11_15"
OUT=ROOT/"build/verification/tiktok";OUT.mkdir(parents=True,exist_ok=True)
BASE=json.loads((ADDON/"PHONE_AUTHOR_BASE_SHA256.json").read_text())
CURRENT={name:hashlib.sha256((PACK/name).read_bytes()).hexdigest() for name in BASE}
jar=zipfile.ZipFile(next(p for p in (ROOT/".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob('*.jar') if not p.name.endswith('-sources.jar')))

def source(name,old):
    baseline=ADDON/'PHONE_BASELINE'/Path(name).name
    if old and name.startswith('assets/minecraft/shaders/core/entity.') and baseline.exists():
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
variants=[{}, {'NO_OVERLAY':1},{'EMISSIVE':1},{'PER_FACE_LIGHTING':1},{'NO_CARDINAL_LIGHTING':1},
          {'ALPHA_CUTOUT':.1},{'GLINT':1},{'DISSOLVE':1},{'NO_OVERLAY':1,'EMISSIVE':1,'ALPHA_CUTOUT':.1}]
for old in (True,False):
    for i,defines in enumerate(variants):
        define='\n'+''.join('#define '+k+' '+str(v)+'\n' for k,v in defines.items())
        def shader(ext):return expand(source('assets/minecraft/shaders/core/entity.'+ext,old),old).replace('#version 330','#version 330'+define,1)
        programs[old,i]=ctx.program(vertex_shader=shader('vsh'),fragment_shader=shader('fsh'))
checks.append('original and new actual core compile/link across nine native entity variants')

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

def draw(program,atlas,quads,size=(96,32)):
    positions=[];colors=[];uvs=[];indices=[]
    for lo,hi,tint,uv in quads:
        first=len(positions)
        for xy,st in [((lo[0],hi[1]),(uv[0],uv[1])),((lo[0],lo[1]),(uv[0],uv[3])),((hi[0],lo[1]),(uv[2],uv[3])),((hi[0],hi[1]),(uv[2],uv[1]))]:
            positions.append([*xy,0]);colors.append([*(np.array(tint)/255),1]);uvs.append(st)
        indices.extend([first,first+1,first+2,first,first+2,first+3])
    count=len(positions);attributes={'Position':(positions,'3f'),'Color':(colors,'4f'),'UV0':(uvs,'2f'),
            'UV1':(np.zeros((count,2),dtype='i4'),'2i'),'UV2':(np.full((count,2),240,dtype='i4'),'2i'),'Normal':(np.tile([0,0,1],(count,1)),'3f')}
    resources=blocks(program);textures=[];buffers=[]
    texture=ctx.texture((atlas.shape[1],atlas.shape[0]),4,atlas.astype('u1').tobytes());texture.filter=(moderngl.NEAREST,moderngl.NEAREST);texture.use(0);textures.append(texture)
    for sampler,unit,color in [('Sampler0',0,None),('Sampler1',1,[0,0,0,255]),('Sampler2',2,[255]*4),('GlintSampler',3,[0]*4),('DissolveMaskSampler',4,[0]*4)]:
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
include=(ADDON/'assets/lbc/shaders/include/tiktok_pixels.glsl').read_text()
palette=np.array([[int(s) for s in p] for p in re.findall(r'uvec3\((\d+)u,(\d+)u,(\d+)u\)',include)],dtype='u1')
assert palette.shape==(256,3)
rng=np.random.default_rng(11315);frame=rng.integers(4,248,(112,64),dtype='u1')
for origin in [(0,0),(5,7),(17,11)]:
    atlas=rng.integers(0,256,(32,32,4),dtype='u1');x,y=origin;atlas[y:y+4,x:x+4]=tile
    uv=[(x+.001)/32,(y+.001)/32,(x+3.999)/32,(y+3.999)/32]
    quads=[]
    for row in range(112):
        for col in range(0,64,3):
            packed=[int(frame[row,min(63,col+j)]) for j in range(3)]
            quads.append(([-1+col/32,1-(row+1)/56],[-1+min(64,col+3)/32,1-row/56],packed,uv))
    for variant in [0,1,2,3,4,5,8]:
        image=draw(programs[False,variant],atlas,quads,(64,112))[::-1]
        assert np.max(np.abs(image[:,:,:3]-palette[frame]/255.))<.00001,(origin,variant,np.max(np.abs(image[:,:,:3]-palette[frame]/255.)))
        assert np.all(image[:,:,3]==1)
    checks.append('all7168independent pixels exact, last partial group, arbitrary atlas origin '+str(origin))

# Scope: ordinary textures/colours, a forged single marker texel, and a damaged corner remain byte-identical.
for case in range(20):
    atlas=rng.integers(0,256,(32,32,4),dtype='u1');atlas[:,:,3]=255
    if case==0:atlas[7:11,5:9]=tile;atlas[10,8,0]=44
    if case==1:atlas[7,5]=tile[0,0]
    tint=[255]*3 if case%3==0 else [int(x) for x in rng.integers(0,248,3)]
    uv=[5.01/32,7.01/32,8.99/32,10.99/32]
    quad=[([-1,-1],[1,1],tint,uv)]
    for variant in range(len(variants)):
        assert np.array_equal(draw(programs[True,variant],atlas,quad),draw(programs[False,variant],atlas,quad)),(case,variant)
checks.append('180 ordinary/forged-marker actual original/new raster comparisons byte-exact')
for name,digest in CURRENT.items():assert hashlib.sha256((PACK/name).read_bytes()).hexdigest()==digest
(ADDON/'PHONE_AUTHOR_BASE_SHA256.json').write_text(json.dumps(BASE,indent=2)+'\n')
result={'passed':True,'gpu':ctx.info['GL_RENDERER'],'checks':checks,'palettePixels':7168,'packedTintCount':2464,
        'compiledVariantsPerVersion':len(variants),'ordinaryRasterComparisons':180,'connectedMinecraftClientTested':False,
        'authorInputsUnchangedDuringCheck':len(CURRENT),'authorAlreadyMergedPhoneShader':CURRENT.get('assets/minecraft/shaders/core/entity.vsh')!=BASE.get('assets/minecraft/shaders/core/entity.vsh'),
        'shaderSha256':hashlib.sha256(include.encode()).hexdigest()}
(OUT/'phone-gpu-results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
for program in programs.values():program.release()
ctx.release()
