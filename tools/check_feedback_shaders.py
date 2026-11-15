"""Actual GUI/shader blending checks and optional isolated post-pass GPU timings.

Uses the prepared read-only addon staging directory and the active/native assets.
No Minecraft/server launch or author-pack writes. Run one GPU job at a time.
"""
import os
for key in ("OMP_NUM_THREADS","OPENBLAS_NUM_THREADS","MKL_NUM_THREADS"):os.environ[key]="1"
import io,json,math,struct,sys,zipfile,importlib.util,re,hashlib
from pathlib import Path
import numpy as np
import moderngl as gl
from PIL import Image
ROOT=Path.cwd();OUT=ROOT/"build/verification/feedback_2026_11_15"
PACK=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
OVERLAY=OUT/"assets-staging"
spec=importlib.util.spec_from_file_location("limits",ROOT/"tools/check_timestop_shaderc.py");limits=importlib.util.module_from_spec(spec);spec.loader.exec_module(limits);limits.limit_resources()
jar=zipfile.ZipFile(next(p for p in (ROOT/".gradle/loom-cache/minecraftMaven").rglob("*26.3.jar") if not p.name.endswith("-sources.jar")))
DEPENDENCIES={}
def read(name,overlay=True):
    for base in ((OVERLAY,PACK) if overlay else (PACK,)):
        if (base/name).exists():
            data=(base/name).read_bytes();DEPENDENCIES[("candidate:" if base==OVERLAY else "active:")+name]=hashlib.sha256(data).hexdigest();return data
    data=jar.read(name);DEPENDENCIES["native:"+name]=hashlib.sha256(data).hexdigest();return data
def source(name,overlay=True):
    s=read(name,overlay).decode("utf-8")
    def include(m):
        ns,path=m[1].split(":",1) if ":" in m[1] else ("minecraft",m[1])
        return source(f"assets/{ns}/shaders/include/{path}",overlay)
    return re.sub(r'^\s*#(?:include|moj_import)\s*[<"]([^>"]+)[>"]\s*$',include,s,flags=re.M).replace("gl_VertexIndex","gl_VertexID")
ctx=gl.create_standalone_context(require=330)
def program(v,f):return ctx.program(vertex_shader=source(v),fragment_shader=source(f))
def blocks(p,w,h,projection=None,values=None):
    buffers=[]
    for name in p:
        block=p[name]
        if not isinstance(block,gl.UniformBlock):continue
        data=bytearray(block.size);identity=np.eye(4,dtype="f4").tobytes()
        if name=="Projection":data[:64]=identity if projection is None else projection.astype("f4").T.tobytes()
        elif name=="DynamicTransforms":
            data[:64]=identity;data[64:128]=identity;struct.pack_into("4f",data,128,1,1,1,1)
        elif name=="Globals":struct.pack_into("2f",data,32,w,h)
        elif name=="BlitConfig":struct.pack_into("4f",data,0,1,1,1,1)
        elif name=="Fog":
            for n in range(16,len(data)-3,4):struct.pack_into("f",data,n,1_000_000+n*100)
        if values and name in values:
            offset=0
            for v in values[name]:
                n=len(v["value"]);align=16 if n>=3 else 8 if n==2 else 4
                offset=(offset+align-1)//align*align
                struct.pack_into(f"{n}f",data,offset,*v["value"]);offset+=4*n
        block.binding=len(buffers);buffers.append(ctx.buffer(data))
    return buffers
def bind(buffers):
    for i,b in enumerate(buffers):b.bind_to_uniform_block(i)
def texture(w,h,data=None,components=4):
    t=ctx.texture((w,h),components,data);t.filter=(gl.NEAREST,gl.NEAREST);t.repeat_x=t.repeat_y=False;return t
def framebuffer(w,h):
    t=texture(w,h);return t,ctx.framebuffer([t])
def image(fbo,w,h):return np.frombuffer(fbo.read(components=4,alignment=1),dtype="u1").reshape(h,w,4).copy()
def uniforms(p,inputs):
    for i,(name,t) in enumerate(inputs.items()):
        if name in p:p[name].value=i;t.use(i)
def draw(p,vao,fbo,buffers,inputs):
    fbo.use();ctx.viewport=(0,0,*fbo.size);bind(buffers);uniforms(p,inputs);vao.render(vertices=3)

W,H=640,360
base=np.empty((H,W,4),dtype="u1");base[:]=[64,128,192,255]
original=texture(W,H,base.tobytes())
stamp=program("assets/lbc/shaders/post/weapon_spread.vsh","assets/lbc/shaders/post/weapon_spread.fsh")
resolve=program("assets/lbc/shaders/post/weapon_spread_resolve.vsh","assets/lbc/shaders/post/weapon_spread_resolve.fsh")
gui=program("assets/minecraft/shaders/core/position_tex_color.vsh","assets/minecraft/shaders/core/position_tex_color.fsh")
stampvao=ctx.vertex_array(stamp,[]);resolvevao=ctx.vertex_array(resolve,[])
stampblocks=blocks(stamp,W,H);resolveblocks=blocks(resolve,W,H)
icon=Image.open(io.BytesIO(read("assets/minecraft/textures/gui/sprites/hud/crosshair.png"))).convert("RGBA")
assert icon.size==(15,15),icon.size
icon=texture(15,15,icon.tobytes())
written,writtenfbo=framebuffer(W,H);final,finalfbo=framebuffer(W,H)
rowsource=source("assets/lbc/shaders/include/weapon_spread_data.glsl")
row=int(re.search(r"LBC_WEAPON_SPREAD_ROW\s*=\s*(\d+)",rowsource)[1])
def telemetry(degrees,active=True):
    data=np.zeros((H,W,4),dtype="u1")
    if active:
        fields=[0,0,0,degrees,0,0,0,1,0,0,0,0,1,1,0,0,1,1,0,0,0,1,0]
        for i,v in enumerate(fields):
            a,b,c,d=struct.pack("<f",v);data[row,i*2]=[a,b,201,255];data[row,i*2+1]=[c,d,202,255]
        data[row,56]=[83,80,82,255]
    return texture(W,H,data.tobytes())
def native(scale):
    gw,gh=math.ceil(W/scale),math.ceil(H/scale)
    x,y=math.floor((gw-15)/2),math.floor((gh-15)/2)
    positions=np.array([[x,y,0,0,0,1,1,1,1],[x,y+15,0,0,1,1,1,1,1],[x+15,y+15,0,1,1,1,1,1,1],[x+15,y,0,1,0,1,1,1,1]],dtype="f4")
    buffer=ctx.buffer(positions.tobytes());vao=ctx.vertex_array(gui,[(buffer,"3f 2f 4f","Position","UV0","Color")])
    projection=np.eye(4,dtype="f4");projection[0,0]=2/gw;projection[1,1]=-2/gh;projection[0,3]=-1;projection[1,3]=1
    uniform=blocks(gui,W,H,projection)
    writtenfbo.use();ctx.viewport=(0,0,W,H);bind(uniform);uniforms(gui,{"Sampler0":icon})
    ctx.enable(gl.BLEND);ctx.blend_func=(gl.ONE_MINUS_DST_COLOR,gl.ONE_MINUS_SRC_COLOR,gl.ONE,gl.ZERO)
    vao.render(mode=gl.TRIANGLE_FAN,vertices=4);ctx.disable(gl.BLEND)
    vao.release();buffer.release()
    for b in uniform:b.release()
cases=[]
for scale in (1,2,3,4,6):
    for degrees in (0,.1,.5,10):
        data=telemetry(degrees)
        draw(stamp,stampvao,writtenfbo,stampblocks,{"SceneSampler":original,"SpreadDataSampler":data})
        native(scale);before=image(writtenfbo,W,H);mask=before[:,:,3]==251; yy,xx=np.indices((H,W));mask&=(abs(xx-W/2)<128)&(abs(yy-H/2)<128)
        assert mask.any(),("native GUI tag missing",scale,degrees,np.unique(before[:,:,3]))
        draw(resolve,resolvevao,finalfbo,resolveblocks,{"SceneSampler":written})
        after=image(finalfbo,W,H);ys,xs=np.where(mask);cx=(xs.min()+xs.max()+1)/2;cy=(ys.min()+ys.max()+1)/2
        rect=np.zeros((H,W),dtype=bool);rect[int(math.floor(cy))-1:int(math.floor(cy))+1,int(math.floor(cx))-1:int(math.floor(cx))+1]=True
        inverse=255-base[:,:,:3]
        if not np.array_equal(after[:,:,:3][rect],inverse[rect]):
            raw=[round(float(written.read()[((H-1)*W+(W-20-n))*4+3])) for n in range(28)]
            print('DEBUG native bbox',xs.min(),xs.max(),ys.min(),ys.max(),'header',raw,'center',cx,cy,'pixels',after[:,:,:3][rect].tolist())
            for value in ['vec4(reticleCenter,reticleStyle)','nativeRect']:
                debug=ctx.program(vertex_shader=source('assets/lbc/shaders/post/weapon_spread_resolve.vsh'),fragment_shader='#version 330\n#extension GL_ARB_separate_shader_objects : require\nlayout(location=0) flat in vec4 reticleBounds;layout(location=1) flat in vec2 reticleStyle;layout(location=2) flat in vec2 reticleCenter;layout(location=3) flat in vec4 nativeRect;layout(location=0) out vec4 fragColor;void main(){fragColor='+value+';}')
                debugtex=ctx.texture((1,1),4,dtype='f4');debugfbo=ctx.framebuffer([debugtex]);debugvao=ctx.vertex_array(debug,[])
                draw(debug,debugvao,debugfbo,blocks(debug,W,H),{'SceneSampler':written});print('DEBUG',value,np.frombuffer(debugtex.read(),dtype='f4'))
            raise AssertionError(('center erased',scale,degrees))
        if degrees==0:
            expected=base.copy();expected[:,:,:3][rect]=inverse[rect]
            assert np.array_equal(after[:,:,:3],expected[:,:,:3]),("zero spread is not exactly2x2",scale)
        elif degrees==10:
            assert np.array_equal(after[:,:,:3][mask],inverse[mask]),("overlap double inversion",scale)
        else:
            yy,xx=np.indices((H,W));reach=max(1,math.tan(math.radians(degrees))*W/2)
            outside=(xx+.5<cx-reach)|(xx+.5>=cx+reach)|(yy+.5<cy-reach)|(yy+.5>=cy+reach)
            assert np.array_equal(after[:,:,:3][outside],base[:,:,:3][outside]),("native tails not removed",scale,degrees)
        cases.append({"guiScale":scale,"spreadDegrees":degrees,"passed":True});data.release()
# Native crosshair must remain alone without telemetry, and F1 must hide both.
for active,visible in ((False,True),(True,False)):
    data=telemetry(3,active);draw(stamp,stampvao,writtenfbo,stampblocks,{"SceneSampler":original,"SpreadDataSampler":data})
    if visible:native(4)
    before=image(writtenfbo,W,H);draw(resolve,resolvevao,finalfbo,resolveblocks,{"SceneSampler":written});after=image(finalfbo,W,H)
    assert np.array_equal(before[:,:,:3],after[:,:,:3]),("inactive/F1 changed image",active,visible)
    data.release()
particle=program("assets/minecraft/shaders/core/particle.vsh","assets/minecraft/shaders/core/particle.fsh")
projection=np.zeros((4,4),dtype="f4");f=1/math.tan(math.radians(70)/2)
projection[0,0]=f;projection[1,1]=f;projection[2,2]=-(100+.1)/(100-.1);projection[2,3]=-(2*100*.1)/(100-.1);projection[3,2]=-1
particle_blocks=blocks(particle,512,512,projection)
positions=ctx.buffer(np.array([[.1,-.1,-5],[.1,.1,-5],[-.1,.1,-5],[-.1,-.1,-5]],dtype="f4").tobytes())
uvs=ctx.buffer(np.array([[1,1],[1,0],[0,0],[0,1]],dtype="f4").tobytes())
lights=ctx.buffer(np.full((4,2),240,dtype="i4").tobytes())
sprite=texture(1,1,bytes([230,51,230,255]));lightmap=texture(16,16,bytes([255]*16*16*4))
puff=texture(512,512);puff_depth=ctx.depth_renderbuffer((512,512));puff_fbo=ctx.framebuffer([puff],puff_depth)
def puff_frame(size,ordinary=False,occluded=False):
    color=np.array([1,1,1,1] if ordinary else [83/255,77/255,size/255,1],dtype="f4")
    colors=ctx.buffer(np.tile(color,(4,1)).tobytes())
    vao=ctx.vertex_array(particle,[(positions,"3f","Position"),(uvs,"2f","UV0"),(colors,"4f","Color"),(lights,"2i","UV2")])
    puff_fbo.use();ctx.viewport=(0,0,512,512);puff_fbo.clear(0,0,0,0,depth=1)
    ctx.enable(gl.DEPTH_TEST);ctx.depth_func="<"
    if occluded:
        cover=ctx.program(vertex_shader="#version 330\nvoid main(){vec2 uv=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(uv*2-1,.90,1);}",
                          fragment_shader="#version 330\nout vec4 fragColor;void main(){if(gl_FragCoord.x>=256)discard;fragColor=vec4(.8,0,0,1);}")
        covervao=ctx.vertex_array(cover,[]);covervao.render(vertices=3);covervao.release();cover.release()
    bind(particle_blocks);uniforms(particle,{"Sampler0":sprite,"Sampler2":lightmap})
    ctx.enable(gl.BLEND);ctx.blend_func=(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA,gl.ONE,gl.ONE_MINUS_SRC_ALPHA)
    vao.render(mode=gl.TRIANGLE_FAN,vertices=4);ctx.disable(gl.BLEND);ctx.disable(gl.DEPTH_TEST)
    result=image(puff_fbo,512,512);vao.release();colors.release();return result
small=puff_frame(2);large=puff_frame(127);normal=puff_frame(127,ordinary=True);covered=puff_frame(127,occluded=True)
small_count=int(np.count_nonzero(small[:,:,3]));large_count=int(np.count_nonzero(large[:,:,3]));ordinary_count=int(np.count_nonzero(normal[:,:,3]))
assert small_count>0 and large_count>20*small_count and large_count<512*512*.8,("puff size/growth",small_count,large_count)
assert len(np.unique(large[:,:,3]))>30,"soft smoke edges"
assert ordinary_count<large_count/20,"ordinary particles must retain native size"
assert np.all(covered[:,:256,0]==204) and np.all(covered[:,:256,1:3]==0),"smoke must remain behind nearer opaque geometry"
assert np.count_nonzero(covered[:,256:,3])>100,"smoke must render in the unoccluded half"
for defines in ("#define OIT_ALPHA_ONLY 1\n","#define OIT 1\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE 1\n"):
    v=source("assets/minecraft/shaders/core/particle.vsh").replace("#version 330","#version 330\n"+defines,1)
    f=source("assets/minecraft/shaders/core/particle.fsh").replace("#version 330","#version 330\n"+defines,1)
    ctx.program(vertex_shader=v,fragment_shader=f).release()
smoke_result={"passed":True,"smallPixelCount":small_count,"largePixelCount":large_count,"ordinaryPixelCount":ordinary_count,
              "softEdges":True,"nativeDepthOcclusion":True,"ordinaryParticleControl":True,"shaderVariants":3}
report={"passed":True,"renderer":ctx.info["GL_RENDERER"],"actualGuiShader":True,"actualNativeInvertBlend":True,"actualVanillaSprite":True,
        "actualWorldTelemetryAndFinalResolve":True,"cases":cases,"noTelemetryAndF1Controls":True,"smokeParticle":smoke_result,"shaderDependenciesSha256":dict(DEPENDENCIES)}
(OUT/"crosshair-gpu.json").write_text(json.dumps(report,indent=2)+"\n")
print("PASS actual GUI/core shaders+native INVERT blending+world stamp/end resolve:20 cases, zero2x2, overlap, clipped native tails, all5 GUI scales, inactive/F1")
print("PASS actual smoke particle shaders: native depth, soft edges, size growth, ordinary particle control and3 OIT variants")
if "--profile" in sys.argv:
    import time,statistics
    width,height=1920,1080
    measured=[]
    for chain_name in ("assets/minecraft/post_effect/entity_outline.json","assets/minecraft/post_effect/end_of_frame.json"):
        chain=json.loads(read(chain_name,False))
        targets={}
        scene=np.empty((height,width,4),dtype="u1");scene[:]=[64,80,96,255]
        targets["minecraft:main"]=(texture(width,height,scene.tobytes()),None)
        targets["minecraft:main"]=(targets["minecraft:main"][0],ctx.framebuffer([targets["minecraft:main"][0]]))
        outlined=texture(width,height,bytes(width*height*4))
        targets["minecraft:entity_outline"]=(outlined,ctx.framebuffer([outlined]))
        for name,definition in chain.get("targets",{}).items():
            definition=definition or {}
            w,h=definition.get("width",width),definition.get("height",height)
            t=texture(w,h,bytes(w*h*4));targets[name]=(t,ctx.framebuffer([t]))
        depth=ctx.texture((width,height),1,np.zeros((height,width),dtype="f4").tobytes(),dtype="f4")
        depth.filter=(gl.NEAREST,gl.NEAREST)
        for number,entry in enumerate(chain["passes"]):
            def shader_path(identifier,ext):
                ns,path=identifier.split(":",1) if ":" in identifier else ("minecraft",identifier)
                return f"assets/{ns}/shaders/{path}.{ext}"
            p=ctx.program(vertex_shader=source(shader_path(entry["vertex_shader"],"vsh"),False),
                          fragment_shader=source(shader_path(entry["fragment_shader"],"fsh"),False))
            vao=ctx.vertex_array(p,[])
            buffers=blocks(p,width,height,values=entry.get("uniforms"))
            inputs={}
            for item in entry.get("inputs",[]):
                if item.get("use_depth_buffer"):t=depth
                else:t=targets[item["target"]][0]
                t.filter=(gl.LINEAR,gl.LINEAR) if item.get("bilinear") else (gl.NEAREST,gl.NEAREST)
                inputs[item["sampler_name"]+"Sampler"]=t
            target=targets[entry["output"]][1]
            draw(p,vao,target,buffers,inputs);ctx.finish()
            times=[]
            for sample in range(3):
                with ctx.query(time=True) as query:draw(p,vao,target,buffers,inputs)
                times.append(query.elapsed/1e6);time.sleep(.005)
            measured.append({"chain":chain_name,"pass":number,"fragment":entry["fragment_shader"],"output":entry["output"],
                             "outputSize":target.size,"medianGpuMs":statistics.median(times),"samplesGpuMs":times})
            for b in buffers:b.release()
            vao.release();p.release()
        for t,fbo in targets.values():fbo.release();t.release()
        depth.release()
    results={"renderer":ctx.info["GL_RENDERER"],"resolution":[width,height],"passes":measured,
             "sumMedianMs":sum(p["medianGpuMs"] for p in measured),
             "shaderDependenciesSha256":dict(DEPENDENCIES),
             "scope":"Isolated actual active RP postprocessing, three GPU timer-query samples/pass after warmup, no effect markers or world geometry. This does not measure the complete Minecraft frame/CPU cost."}
    (OUT/"active-post-gpu-profile.json").write_text(json.dumps(results,indent=2)+"\n")
    print("GPU POST PROFILE",json.dumps(sorted(measured,key=lambda p:p["medianGpuMs"],reverse=True)[:8],indent=2))
ctx.release();jar.close()
