"""Real OpenGL capture-once, final-frame retention, thaw, resize and re-entry checks."""
from pathlib import Path
import json,struct,zipfile
import moderngl
import numpy as np

ROOT=Path(__file__).resolve().parent
ctx=moderngl.create_standalone_context(require=330)
ASSETS=ROOT/'assets/lbc/shaders/post'
native_cache=Path('.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3')
with zipfile.ZipFile(next(p for p in native_cache.glob('*.jar') if not p.name.endswith('-sources.jar'))) as jar:
    globals_source=jar.read('assets/minecraft/shaders/include/globals.glsl').decode()
    plain_vertex=jar.read('assets/minecraft/shaders/core/screenquad.vsh').decode().replace('gl_VertexIndex','gl_VertexID')
def source(name):return (ASSETS/name).read_text().replace('gl_VertexIndex','gl_VertexID').replace('#include <minecraft:globals.glsl>',globals_source)
def program(fragment,vertex=None):
    p=ctx.program(vertex_shader=source(vertex) if vertex else plain_vertex,fragment_shader=source(fragment));return p,ctx.vertex_array(p,[])
world_gate,wgvao=program('time_world_gate.fsh')
capture,cvao=program('time_capture.fsh','time_capture.vsh')
present,pvao=program('time_present.fsh','time_present.vsh')
world,wvao=program('time_world.fsh')
one,ovao=program('time_one.fsh')
gate,gvao=program('time_gate.fsh')
ubo=ctx.buffer(reserve=48);ubo.bind_to_uniform_block(7);world['Globals'].binding=7
checks=[]
def check(ok,label):assert ok,label;checks.append(label)
def texture(size):
    t=ctx.texture(size,4,dtype='f1');t.filter=(moderngl.NEAREST,moderngl.NEAREST);t.write(bytes(size[0]*size[1]*4));return t
def target(t):return ctx.framebuffer([t])
def draw(p,vao,out,**inputs):
    for i,(name,texture_) in enumerate(inputs.items()):p[name+'Sampler'].value=i;texture_.use(i)
    out.use();ctx.viewport=(0,0,*out.size)
    with ctx.query(samples=True) as q:vao.render(vertices=3)
    return q.samples
def read(fbo):return np.frombuffer(fbo.read(components=4,alignment=1),dtype=np.uint8).reshape(fbo.height,fbo.width,4).copy()
rng=np.random.default_rng(2515)
size=(64,36)
ready=texture((1,1));world_control=texture((1,1));previous=texture((1,1));control=texture((1,1))
world_cache=texture(size);screen_cache=texture(size);scene=texture(size);shown=texture(size)
world_fbo=target(world_cache);screen_fbo=target(screen_cache);shown_fbo=target(shown)
first_world=None;first_screen=None
for frame in range(12):
    pixels=rng.integers(0,256,(size[1],size[0],4),dtype=np.uint8);pixels[:,:,3]=255;scene.write(pixels.tobytes())
    draw(world_gate,wgvao,target(world_control),History=ready)
    samples=draw(capture,cvao,world_fbo,Gate=world_control,Scene=scene)
    draw(one,ovao,target(ready))
    ubo.write(struct.pack('3if3ff2f2i',0,0,0,0,0,0,0,0,*size,0,0))
    draw(world,wvao,shown_fbo,Frame=world_cache)
    if frame==0:first_world=read(shown_fbo)
    else:check(np.array_equal(read(shown_fbo),first_world),'world snapshot retained, frame'+str(frame))
    check((samples>0)==(frame==0),'world capture draws only first frame'+str(frame))
    draw(gate,gvao,target(control),Scene=shown,History=previous)
    previous.write(target(control).read(components=4))
    # Model changing bloom/other global effects AFTER the initial player post-effect.
    postprocessed=rng.integers(0,256,(size[1],size[0],4),dtype=np.uint8);postprocessed[:,:,3]=255;scene.write(postprocessed.tobytes())
    samples=draw(capture,cvao,screen_fbo,Gate=control,Scene=scene)
    shown.write(postprocessed.tobytes());draw(present,pvao,shown_fbo,Gate=control,Frame=screen_cache)
    if frame==0:first_screen=read(shown_fbo)
    check(np.array_equal(read(shown_fbo),first_screen),'fully postprocessed final frame retained'+str(frame))
    check((samples>0)==(frame==0),'final frame capture draws only first frame'+str(frame))
for frame in range(3):
    pixels=rng.integers(0,256,(size[1],size[0],4),dtype=np.uint8);pixels[:,:,3]=255;scene.write(pixels.tobytes())
    draw(gate,gvao,target(control),Scene=scene,History=previous);previous.write(target(control).read(components=4))
    check(draw(capture,cvao,screen_fbo,Gate=control,Scene=scene)==0,'inactive capture has zero rasterized samples')
    shown.write(pixels.tobytes());check(draw(present,pvao,shown_fbo,Gate=control,Frame=screen_cache)==0,'inactive present has zero rasterized samples')
    check(np.array_equal(read(shown_fbo),pixels),'thaw immediately preserves current live image')
# Native GameRenderer closes requested-effect persistent targets when it leaves the list.
ready.write(bytes(4));world_cache.write(bytes(size[0]*size[1]*4))
fresh=rng.integers(0,256,(size[1],size[0],4),dtype=np.uint8);fresh[:,:,3]=255;scene.write(fresh.tobytes())
draw(world_gate,wgvao,target(world_control),History=ready);draw(capture,cvao,world_fbo,Gate=world_control,Scene=scene);draw(one,ovao,target(ready))
draw(world,wvao,shown_fbo,Frame=world_cache);check(not np.array_equal(read(shown_fbo),first_world),'next activation gets a fresh world image')
draw(gate,gvao,target(control),Scene=shown,History=previous);previous.write(target(control).read(components=4));draw(capture,cvao,screen_fbo,Gate=control,Scene=scene)
before=read(screen_fbo)
larger=texture((128,72));larger_fbo=target(larger)
draw(present,pvao,larger_fbo,Gate=control,Frame=screen_cache)
check(np.array_equal(read(larger_fbo),np.repeat(np.repeat(before,2,axis=0),2,axis=1)),'window resize rescales the same fixed capture, never a new live frame')
config=json.loads((ROOT/'assets/lbc/post_effect/time_stop_freeze.json').read_text())
check(config['targets']['time_world_frame']=={'width':3840,'height':2160,'persistent':True,'clear_color':0},'native fixed4K persistent target is resize independent')
author=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/post_effect/end_of_frame.json')
original=json.loads(author.read_text(encoding='utf-8'));full=json.loads((ROOT/'assets/minecraft/post_effect/end_of_frame.json').read_text())
check(full['passes'][2:-2]==original['passes'],'all existing author global passes retained byte-for-byte as JSON values')
check(all(full['targets'][k]==v for k,v in original['targets'].items()),'all existing author targets retained')
(ROOT/'GPU_RESULTS.json').write_text(json.dumps({'renderer':ctx.info['GL_RENDERER'],'checks':checks,'success':True},indent=2)+'\n')
print(json.dumps({'success':True,'renderer':ctx.info['GL_RENDERER'],'checks':len(checks)}))
