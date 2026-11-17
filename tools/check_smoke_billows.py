"""Render native four-tick smoke quads and ordinary fireworks on a real GPU."""
import argparse
import hashlib
import importlib.util
from io import BytesIO
import json
import math
from pathlib import Path
import re
import struct
import zipfile

import moderngl as gl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("shader_limits", ROOT / "tools/check_timestop_shaderc.py")
limits = importlib.util.module_from_spec(spec);spec.loader.exec_module(limits)


def main():
    limits.limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--overlay", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--loader-report", type=Path, required=True)
    args = parser.parse_args();args.output.mkdir(parents=True, exist_ok=True)
    (args.output/'GPU_RESULTS.json').unlink(missing_ok=True)
    emitter_path=ROOT/'src/main/java/me/skywars2/game/SmokeVisuals.java'
    emitter_bytes=emitter_path.read_bytes()
    count_match=re.search(rb'static final int PUFFS_PER_TICK\s*=\s*(\d+);',emitter_bytes)
    assert count_match is not None
    emitter_count=int(count_match[1])
    native_dir = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3"
    jar = zipfile.ZipFile(native_dir / "minecraft-merged-d6f9bb6878-26.3.jar")
    sources = zipfile.ZipFile(native_dir / "minecraft-merged-d6f9bb6878-26.3-sources.jar")
    watched = {}
    loader=json.loads(args.loader_report.read_text())
    assert loader['passed'] and loader['retainedMarkers']==4 and loader['autoNegativeControlRetainedMarkers']==0
    for name,digest in loader['inputs'].items():assert hashlib.sha256((args.overlay/name).read_bytes()).hexdigest()==digest
    metadata=json.loads((args.overlay/'assets/minecraft/textures/particle/flash.png.mcmeta').read_text())
    assert metadata['texture']['mipmap_strategy']=='mean'

    def source(name, patched=True):
        for base in ((args.overlay, args.pack) if patched else (args.pack,)):
            path = base / name
            if path.is_file():
                data = path.read_bytes()
                if base == args.pack:watched[name] = hashlib.sha256(data).hexdigest()
                break
        else:data = jar.read(name)
        if name.endswith(".png"):return data
        def include(match):
            ns, path = match[1].split(":", 1)
            return source(f"assets/{ns}/shaders/include/{path}", patched)
        return re.sub(r'^\s*#(?:include|moj_import)\s*[<"]([^>"]+)[>"]\s*$', include, data.decode(), flags=re.M).replace("gl_VertexIndex", "gl_VertexID")

    # Check the native client contract, not an assumed lifetime/quad order.
    flash = sources.read("net/minecraft/client/particle/FireworkParticles.java").decode()
    quad = sources.read("net/minecraft/client/renderer/state/level/QuadParticleRenderState.java").decode()
    stage = sources.read("net/minecraft/client/renderer/StagedVertexBuffer.java").decode()
    assert "this.lifetime = 4;" in flash
    assert "0.6F - (this.age + partialTickTime - 1.0F) * 0.25F * 0.5F" in flash
    assert "7.1F * Mth.sin((this.age + a - 1.0F) * 0.25F * (float) Math.PI)" in flash
    for side in ("1.0F, -1.0F", "1.0F, 1.0F", "-1.0F, 1.0F", "-1.0F, -1.0F"):assert side in quad
    assert "Mth.roundToward(nextVertexOffset, draw.format.getVertexSize())" in stage
    atlas_manager=sources.read('net/minecraft/client/resources/model/sprite/AtlasManager.java').decode()
    assert 'new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_PARTICLES, AtlasIds.PARTICLES, false)' in atlas_manager
    assert 'this.config.createMipmaps ? maxMipmapLevels : 0' in atlas_manager
    sprite_loader=sources.read('net/minecraft/client/renderer/texture/atlas/SpriteResourceLoader.java').decode()
    assert 'textureInfo = metadata.getSection(TextureMetadataSection.TYPE);' in sprite_loader
    marker=sources.read("net/minecraft/client/particle/BlockMarker.java").decode()
    assert "this.lifetime = 80;" in marker and "return 0.5F;" in marker
    legacy_root=Path('E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function')
    legacy_inputs={}
    for relative,needle in [('projectiles/smoke.mcfunction','~ ~2.5 ~ 7 4 7 0 100 force'),
                            ('projectiles/prein.mcfunction','run function lbc:projectiles/smoke'),
                            ('projectiles/pre.mcfunction','inGround:1b'),
                            ('projectilestick.mcfunction','run function lbc:projectiles/pre'),
                            ('tick_not_timestopped.mcfunction','run function lbc:projectilestick')]:
        data=(legacy_root/relative).read_bytes();assert needle in data.decode();legacy_inputs[relative]=hashlib.sha256(data).hexdigest()
    listener=sources.read("net/minecraft/client/multiplayer/ClientPacketListener.java").decode()
    assert "this.random.nextGaussian() * packet.xDist()" in listener
    ctx = gl.create_standalone_context(require=330)
    programs = {patched: ctx.program(vertex_shader=source("assets/minecraft/shaders/core/particle.vsh", patched),
                                     fragment_shader=source("assets/minecraft/shaders/core/particle.fsh", patched))
                for patched in (False, True)}
    ctx.enable(gl.BLEND);ctx.blend_func = gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA
    ctx.disable(gl.CULL_FACE)
    light = ctx.texture((16, 16), 4, bytes([255]*16*16*4));light.use(1)
    uniform_buffers = []
    for patched, program in programs.items():
        for name in program:
            block = program[name]
            if not isinstance(block, gl.UniformBlock):continue
            data = bytearray(block.size)
            if name == "DynamicTransforms":
                data[:64] = np.eye(4,dtype='f4').tobytes();data[64:128] = np.eye(4,dtype='f4').tobytes()
                struct.pack_into("4f",data,128,1,1,1,1)
            elif name == "Fog":
                for offset in range(16,block.size-3,4):struct.pack_into("f",data,offset,1000000+offset*100)
            block.binding = len(uniform_buffers)
            uniform_buffers.append((name, ctx.buffer(data), block.binding))
        program['Sampler0'].value = 0;program['Sampler2'].value = 1
    original = np.array(Image.open(BytesIO(source("assets/minecraft/textures/particle/flash.png", False))).convert('RGBA'))
    marked = np.array(Image.open(BytesIO(source("assets/minecraft/textures/particle/flash.png"))).convert('RGBA'))
    wool = np.array(Image.open(BytesIO(source("assets/minecraft/textures/block/gray_wool.png",False))).convert('RGBA'))
    assert np.array_equal(original[:,:,3],marked[:,:,3])
    changed = np.any(original != marked, axis=2);assert changed.sum() == 4 and np.all(original[changed,3] == 0)
    atlas_textures = {}
    atlas_sizes = [(256,128),(512,512)]
    cases = [];timings=[]
    dtype = np.dtype([('p','<f4',(3,)),('uv','<f4',(2,)),('c','u1',(4,)),('light','<i2',(2,))])
    assert dtype.itemsize == 28
    side = np.array([[1,-1],[1,1],[-1,1],[-1,-1]],dtype='f4')
    width,height = 512,320
    frame = ctx.simple_framebuffer((width,height),components=4)
    near,far = .05,512.
    projection = np.zeros((4,4),dtype='f4');f = 1/math.tan(math.radians(70)/2)
    projection[0,0]=f*height/width;projection[1,1]=f
    projection[2,2]=(far+near)/(far-near);projection[2,3]=2*near*far/(far-near);projection[3,2]=-1

    def atlas(patched, size=(256,128), origin=(45,27), linear=False, material='flash'):
        key=(patched,size,origin,linear,material)
        if key not in atlas_textures:
            data=np.zeros((size[1],size[0],4),dtype='u1')
            sprite=wool if material=='wool' else marked if patched else original
            data[origin[1]:origin[1]+sprite.shape[0],origin[0]:origin[0]+sprite.shape[1]]=sprite
            t=ctx.texture(size,4,data.tobytes());t.filter=(gl.LINEAR,gl.LINEAR) if linear else (gl.NEAREST,gl.NEAREST)
            t.repeat_x=t.repeat_y=False;atlas_textures[key]=t
        return atlas_textures[key]

    def vertices(puffs, base, yaw=0, pitch=0, atlas_size=(256,128), origin=(45,27), material='flash'):
        # Native camera quaternion is the inverse of the view's rotation.
        y,p=np.radians([yaw,pitch]);cy,sy,cp,sp=np.cos(y),np.sin(y),np.cos(p),np.sin(p)
        rotation=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]],dtype='f4')@np.array([[1,0,0],[0,cp,-sp],[0,sp,cp]],dtype='f4')
        data=np.zeros(base+len(puffs)*4,dtype=dtype)
        u0,v0=origin[0]/atlas_size[0],origin[1]/atlas_size[1]
        sprite=wool if material=='wool' else original
        u1,v1=(origin[0]+sprite.shape[1])/atlas_size[0],(origin[1]+sprite.shape[0])/atlas_size[1]
        for index,(center,age,size,rgb) in enumerate(puffs):
            age32=np.float32(age)
            alpha=np.float32(.6)-np.float32(age32-np.float32(1))*np.float32(.25)*np.float32(.5)
            native_size=np.float32(7.1)*np.sin(np.float32(age32-np.float32(1))*np.float32(.25)*np.float32(np.pi))
            if material=='wool':native_size,alpha=.5,1
            q=data[base+index*4:base+(index+1)*4]
            offset=np.column_stack((side*native_size,np.zeros(4)))@rotation
            q['p']=np.array(center)+offset
            q['uv']=[(u1,v1),(u1,v0),(u0,v0),(u0,v1)]
            q['c']=[*([83,77,size] if rgb is None else rgb),max(0,min(255,int(np.floor(alpha*255))))]
            q['light']=[240,240]
        return data,rotation

    def render(puffs, patched=True, base=0, yaw=0, pitch=0, atlas_size=(256,128), origin=(45,27), linear=False, depth=None, background=(20,40,60), time_ticks=0, material='flash', measure=False):
        if not puffs:
            pixels=np.full((height,width,4),255,dtype='u1');pixels[:,:,:3]=background
            return pixels
        data,rotation=vertices(puffs,base,yaw,pitch,atlas_size,origin,material)
        model=np.eye(4,dtype='f4');model[:3,:3]=rotation
        for name,buffer,binding in uniform_buffers:
            if name=='DynamicTransforms':buffer.write(model.flatten(order='F').tobytes(),offset=0)
            elif name=='Projection':buffer.write(projection.flatten(order='F').tobytes())
            elif name=='Globals':buffer.write(struct.pack('f',(time_ticks%24000)/24000),offset=28)
            buffer.bind_to_uniform_block(binding)
        program=programs[patched];atlas(patched,atlas_size,origin,linear,material).use(0);light.use(1)
        vbo=ctx.buffer(data.tobytes())
        indices=np.concatenate([np.array([0,1,2,2,3,0],dtype='u4')+base+i*4 for i in range(len(puffs))])
        ibo=ctx.buffer(indices.tobytes())
        vao=ctx.vertex_array(program,[(vbo,'3f 2f 4f1 2i2','Position','UV0','Color','UV2')],index_buffer=ibo)
        frame.use();ctx.viewport=(0,0,width,height)
        if depth is None:ctx.disable(gl.DEPTH_TEST)
        else:ctx.enable(gl.DEPTH_TEST);ctx.depth_func='>='
        frame.clear(*(c/255 for c in background),1,depth=0 if depth is None else depth)
        query=ctx.query(time=True) if measure else None
        if query is None:vao.render(mode=gl.TRIANGLES)
        else:
            with query:vao.render(mode=gl.TRIANGLES)
        pixels=np.frombuffer(frame.read(components=4),dtype='u1').reshape(height,width,4).copy()
        if query is not None:
            elapsed=int(query.elapsed);assert 0<elapsed<1000000000
            timings.append({'material':material,'puffs':len(puffs),'gpuNanoseconds':elapsed})
        vao.release();ibo.release();vbo.release()
        return pixels

    # Real sprite signature lookup must work at all corners, atlas edges and VBO phases.
    for age in (.25,1,1.5,2,2.5,3,3.5,3.99,4):
        for size in (3,32,64,127):
            for yaw,pitch in ((0,0),(37,28)):
                center=np.array([0.,0.,-12.])
                y,p=np.radians([yaw,pitch]);cy,sy,cp,sp=np.cos(y),np.sin(y),np.cos(p),np.sin(p)
                rot=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cp,-sp],[0,sp,cp]])
                center=rot.T@center
                images=[render([(center,age,size,None)],base=phase,yaw=yaw,pitch=pitch) for phase in (0,1,2,3)]
                assert all(np.array_equal(images[0],im) for im in images[1:]), ('VBO phase',age,size,yaw,pitch)
                changed=np.any(images[0][:,:,:3] != [20,40,60],axis=2)
                if age<1 or age>=4:assert not np.any(changed), ('lifetime',age)
                if age==2 and size==127:
                    alternate=render([(center,age,size,None)],yaw=yaw,pitch=pitch,background=(200,180,160))
                    assert np.any(np.all(images[0][:,:,:3]==alternate[:,:,:3],axis=2)), 'No opaque core'
                cases.append({'case':'native-age-geometry','age':age,'size':size,'camera':[yaw,pitch],'all4BasePhasesIdentical':True})
    for atlas_size,origin in [((256,128),(0,0)),((256,128),(224,96)),((512,512),(173,291))]:
        images=[render([([0,0,-12],2,127,None)],base=phase,atlas_size=atlas_size,origin=origin) for phase in range(4)]
        assert all(np.array_equal(images[0],im) for im in images[1:])
        alternate=render([([0,0,-12],2,127,None)],atlas_size=atlas_size,origin=origin,background=(200,180,160))
        assert np.any(np.all(images[0][:,:,:3]==alternate[:,:,:3],axis=2))
        cases.append({'case':'atlas-boundaries','atlas':atlas_size,'origin':origin})

    # Four alpha-zero metadata texels must not alter ordinary native flashes.
    for linear in (False,True):
        for age in (.25,1.5,2.5,3.5):
            for rgb in ([255,255,255],[255,0,0],[71,190,230],[0,1,0],[0,2,0]):
                a=render([([0,0,-12],age,127,rgb)],False,linear=linear)
                b=render([([0,0,-12],age,127,rgb)],True,linear=linear)
                assert np.array_equal(a,b), ('ordinary flash/laser changed',linear,age,rgb,int(np.abs(a.astype('i4')-b).max()))
                cases.append({'case':'ordinary-particle-pixel-identical','linear':linear,'age':age,'rgb':rgb})

    # Native reverse-Z depth still hides puffs behind geometry; no screen-fog trick.
    z=-5;clip=projection@np.array([0,0,z,1]);wall_depth=(clip[2]/clip[3]+1)*.5
    behind=render([([0,0,-8],2,127,None)],depth=wall_depth)
    front=render([([0,0,-4],2,127,None)],depth=wall_depth)
    assert np.all(behind[:,:,:3]==[20,40,60]) and np.any(front[:,:,:3]!=[20,40,60])
    cases.append({'case':'native-reverse-z-occlusion'})

    # Equal interiors must be independent of insertion order, including blending.
    pair=[([-1,0,-10],2,127,None),([1,0,-12],2.5,127,None)]
    a=render(pair);b=render(pair[::-1]);difference=np.abs(a.astype('i4')-b.astype('i4'))
    assert difference[:,:,:3].max()<=1, ('overlap seam',difference.max())
    alternate=render(pair,background=(200,180,160))
    opaque=np.all(a[:,:,:3]==alternate[:,:,:3],axis=2);assert opaque.sum()>10000
    cases.append({'case':'opaque-overlap-no-seam','maximumRgbOrderDifference':int(difference[:,:,:3].max()),'opaquePixels':int(opaque.sum())})
    Image.fromarray(np.flipud(a)).save(args.output/'opaque-overlap.png')

    cloud_rows=[];new_masks=[]
    for seed in range(12):
        rng=np.random.default_rng(seed);puffs=[]
        for age in (1.5,2.5,3.5):
            for center in rng.normal(size=(emitter_count,3))*[7,4,7]+[0,0,-18]:puffs.append((center,age,127,None))
        image=render(puffs,measure=seed<4)
        alternate=render(puffs,background=(200,180,160))
        yy,xx=np.mgrid[:height,:width];core=(xx-width/2)**2+(yy-height/2)**2<24**2
        opaque=np.all(np.abs(image[:,:,:3].astype('i4')-alternate[:,:,:3].astype('i4'))<=1,axis=2)
        new_masks.append(opaque)
        coverage=float(opaque[core].mean());cloud_rows.append({'seed':seed,'centralOpaqueCoverage':coverage})
        if seed in (0,5):Image.fromarray(np.flipud(image)).save(args.output/f'mature-cloud-{seed}.png')
    assert min(r['centralOpaqueCoverage'] for r in cloud_rows)>.95, cloud_rows
    cases.append({'case':'mature-source-spread','nativePuffs':3*emitter_count,'sigma':[7,4,7],'seeds':cloud_rows})
    old_masks=[]
    for seed in range(4):
        rng=np.random.default_rng(seed);legacy=[(center,0,0,[255,255,255]) for center in rng.normal(size=(8000,3))*[7,4,7]+[0,0,-18]]
        a=render(legacy,False,material='wool',measure=True);b=render(legacy,False,material='wool',background=(200,180,160))
        old_masks.append(np.all(np.abs(a[:,:,:3].astype('i4')-b[:,:,:3].astype('i4'))<=1,axis=2))
        if seed==0:Image.fromarray(np.flipud(a)).save(args.output/'legacy-function-block-marker-reference.png')
    old_profile=np.mean([m[height//2-20:height//2+20] for m in old_masks],axis=(0,1))
    new_profile=np.mean([m[height//2-20:height//2+20] for m in new_masks[:4]],axis=(0,1))
    old_dense=int((old_profile>=.95).sum());new_dense=int((new_profile>=.95).sum())
    assert new_dense>=old_dense,('New smoke is smaller than the legacy-function reference',new_dense,old_dense)
    cases.append({'case':'legacy-function-optical-reference','oldBodies':8000,'newVisibleBodies':3*emitter_count,
                  'legacy95PercentDenseWidthPixels':old_dense,'new95PercentDenseWidthPixels':new_dense,
                  'scope':'Legacy function interpreted by native26.3 BlockMarker; no original Paper client frame claim'})
    a=render([([0,0,-1],2,127,None)]);b=render([([0,0,-1],2,127,None)],time_ticks=200)
    assert np.ptp(a[:,:,:3])>12 and np.any(a[:,:,:3]!=b[:,:,:3]), 'Billow shading is flat or static'
    cases.append({'case':'coherent-moving-billow-shading','grayscaleRange':int(np.ptp(a[:,:,:3]))})
    # Use the actual growth law and short native histories, rather than spawning
    # an already complete full-size cloud as a visual "proof".
    radius,density=.15,.1;rng=np.random.default_rng(20261117);births=[];growth=[];previews=[]
    for tick in range(61):
        radius+=(7.5-radius)*.06;density+=(1-density)*.07
        size=max(1,min(127,int(math.floor(radius/7.5*127+.5))))
        count=max(1,int(math.ceil(emitter_count*density*density)))
        for center in rng.normal(size=(count,3))*[radius*7/7.5,radius*4/7.5,radius*7/7.5]+[0,radius/3-2.5,-25]:
            births.append((tick,center,size))
        if tick in (1,5,10,20,60):
            puffs=[(center,tick+.5-born,size,None) for born,center,size in births if 0<=tick+.5-born<4]
            pixels=render(puffs,time_ticks=tick)
            changed=np.any(pixels[:,:,:3]!=[20,40,60],axis=2)
            growth.append({'tick':tick,'radiusDriver':radius,'density':density,'drawnPuffs':len(puffs),'coveredPixels':int(changed.sum())})
            preview=Image.fromarray(np.flipud(pixels));preview.save(args.output/f'growth-{tick}.png');previews.append(preview)
    assert growth[0]['coveredPixels']<growth[2]['coveredPixels']<growth[-1]['coveredPixels'],growth
    assert growth[0]['coveredPixels']<width*height*.02 and growth[-1]['coveredPixels']>width*height*.5,growth
    cases.append({'case':'gradual-growth-from-real-emission-law','frames':growth})
    montage=Image.new('RGB',(width*len(previews),height))
    for index,preview in enumerate(previews):montage.paste(preview,(index*width,0))
    montage.save(args.output/'growth-sequence.png')
    for name,digest in watched.items():assert hashlib.sha256((args.pack/name).read_bytes()).hexdigest()==digest
    for name,digest in legacy_inputs.items():assert hashlib.sha256((legacy_root/name).read_bytes()).hexdigest()==digest
    assert emitter_path.read_bytes()==emitter_bytes
    result={'passed':True,'renderer':ctx.info['GL_RENDERER'],'cases':cases,'authorPackUnchanged':True,
            'nativeSourceContractVerified':True,'legacyFunctions':legacy_inputs,'physicalClientReloadObserved':False,
            'gpuDrawTimings':{'viewport':[width,height],'drawOnlyNotClientFps':True,'samples':timings},
            'emitterProfile':{'countPerTick':emitter_count,'sourceSha256':hashlib.sha256(emitter_bytes).hexdigest()},
            'nativeLoader':loader,'particleAtlasMipLevel':0,
            'scope':'Actual particle vertex/fragment GPU draws with native packed vertices, indexed bases, native local flash ages, linear/nearest atlas, reverse-Z and blended multi-puff scenes. OIT execution remains a separate check.'}
    (args.output/'GPU_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'passed':True,'cases':len(cases),'renderer':result['renderer'],'cloudMinOpaqueCoverage':min(r['centralOpaqueCoverage'] for r in cloud_rows)}))
    sources.close();jar.close();ctx.release()


if __name__=='__main__':main()
