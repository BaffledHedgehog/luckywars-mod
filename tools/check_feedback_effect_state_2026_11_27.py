"""Offscreen GPU regression of effect-state dropout and removal; no game or pack writes."""
import importlib.util
from pathlib import Path
import struct
import numpy as np
import moderngl

ROOT = Path(__file__).resolve().parents[1]
PACK = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
OVERLAY = ROOT / 'resourcepack_template/updates/feedback_2026_11_27'
JAR = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/libraries/com/mojang/minecraft/26.3/minecraft-26.3-client.jar')
spec = importlib.util.spec_from_file_location('effect_lifecycle', PACK / 'tools/effect_lifecycle.py')
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
original = m.shader_sources
def sources(jar, name, suffixes=('vsh','fsh')):
    # The production include tree is read from the installed pack; override
    # only the candidate state shader before the helper expands includes.
    if name != 'assets/lbc/shaders/post/effect_state': return original(jar, name, suffixes)
    import re
    def expand(src):
        def include(match):
            ns, name = match[1].split(':')
            path = PACK / f'assets/{ns}/shaders/include/{name}'
            return expand(path.read_text() if path.exists() else jar.read(f'assets/{ns}/shaders/include/{name}').decode())
        return re.sub(r'#include\s+<([^>]+)>', include, src)
    return [expand((OVERLAY / f'{name}.{suffix}').read_text()) for suffix in suffixes]
m.shader_sources = sources
ctx=moderngl.create_standalone_context(require=330)
buffer=ctx.buffer(reserve=48);buffer.bind_to_uniform_block(0)
lifecycle=m.EffectLifecycle(ctx,JAR,buffer,0)
pixels=np.zeros((8,1024,4),dtype='u1')
points=np.array([[0,0,-10],[2,0,-10],[0,2,-10],[0,0,-8]],dtype='f4')
m.write_packet_record(pixels,0,points.flatten(),2)
m.write_packet_record(pixels,m.PACKET_HOLE_VIEW,np.eye(4,dtype='f4').flatten(),2)
live=ctx.texture((1024,8),4,pixels.tobytes());missing=ctx.texture((1024,8),4,bytes(pixels.nbytes))
def step(texture,second):
    lifecycle.update(texture,second)
    row=lifecycle.records()[2]
    return struct.unpack('<f',row[58].tobytes())[0],int(row[56,3])
step(live,1);assert step(live,1.25)[0] >= .99
assert step(missing,1.30)[0] >= .99
assert step(live,1.34)[0] >= .99
assert step(missing,1.40)[0] >= .99
size,_=step(missing,1.49);assert 0<size<1
assert step(missing,1.72)==(0.0,0)
print('PASS',ctx.info['GL_RENDERER'],'short missing frame retained; removal faded after 0.10s grace and 0.20s collapse')

def packet(kind, ratio, age=0):
    data=np.zeros((8,1024,4),dtype='u1')
    pts=np.array([[0,0,-10],[2,0,-10],[0,1+age,-10],[0,0,-10+2*ratio]],dtype='f4')
    m.write_packet_record(data,16*13,pts.flatten(),kind)
    m.write_packet_record(data,m.PACKET_HOLE_VIEW+17,np.eye(4,dtype='f4').flatten(),kind)
    return ctx.texture((1024,8),4,data.tobytes())

def number(row,column):return struct.unpack('<f',row[column].tobytes())[0]
def nuclear_step(texture,second):
    lifecycle.update(texture,second);row=lifecycle.records()[18]
    return number(row,77),number(row,57),number(row,73),number(row,76),int(row[56,3])
for total,fade in [(10,4),(20,12)]:
    lifecycle.clear();main=packet(4,2+total);fader=packet(5,2+fade)
    nuclear_step(main,100)
    before=None
    for i in range(1,total+1):
        state=nuclear_step(main,100+i)
        assert abs(state[0]-i)<.002 and abs(state[2]-i)<.002,(total,i,state)
        if i==total-fade+1: assert 0<state[1]<1 and state[3]>0,(total,state)
        if i==total-fade+2:before=state
    assert state[1]==0 and state[4]==0,(total,state)
    # Recreate an in-progress fade and swap to the server fade carrier.
    lifecycle.clear();nuclear_step(main,200)
    for i in range(1,total-fade+3): before=nuclear_step(main,200+i)
    after=nuclear_step(fader,200+total-fade+2.1)
    assert 0<=before[1]-after[1]<.05 and after[3]>=before[3],(total,before,after)
    main.release();fader.release()
print('PASS stalled nuclear main: 10/20 second deadlines and seamless 4/12 second fade handoff')

lifecycle.clear();main=packet(4,12)  # 10-second lifetime
nuclear_step(main,300)
assert abs(nuclear_step(main,301)[0]-1)<.002
assert abs(nuclear_step(main,302)[0]-2)<.002
assert abs(nuclear_step(main,300.5)[0]-2)<.002, 'server rewind restarted the effect'
assert abs(nuclear_step(main,301.5)[0]-3)<.002
assert abs(nuclear_step(main,302.5)[0]-4)<.002
# Large forward correction preserves state rather than destroying/restarting it.
assert abs(nuclear_step(main,310)[0]-4)<.002
assert abs(nuclear_step(main,311)[0]-5)<.002
for i in range(1,6): state=nuclear_step(main,311+i)
assert state[1]==0 and state[4]==0,state
main.release()
print('PASS nuclear history survives backward server time correction and large forward correction')

# Simulate repeated server time rewinds with authoritative, increasing age.
lifecycle.clear();clock=500.0
for age in range(0,21):
    main=packet(4,22,age)
    state=nuclear_step(main,clock);main.release()
    assert state[0]>=age-.002,(age,state)
    if age<20:
        main=packet(4,22,age);nuclear_step(main,clock+.9);main.release()
    clock+=.1  # next server packet rewinds client time by 0.8 seconds
assert state[1]==0 and state[4]==0,state
print("PASS repeated backward packet corrections with rising server age do not extend deadline")
