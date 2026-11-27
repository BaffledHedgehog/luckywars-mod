"""Compile actual bloom rules on GPU and classify authored beam materials."""
from pathlib import Path
import json,zipfile
import numpy as np
import moderngl
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
OVERLAY=ROOT/'resourcepack_template/updates/feedback_2026_11_27'
item=json.loads((OVERLAY/'assets/lbc/items/hedgehog_laser_ray.json').read_text(encoding='utf8'))
assert item['model']['tints'][0]['value']==0xfcfdf9
jar=next((ROOT/'.gradle').rglob('minecraft-merged-d6f9bb6878-26.3-sources.jar'))
with zipfile.ZipFile(jar) as z:
    assert 'Codec.withAlternative(Codec.INT, VECTOR3F' in z.read('net/minecraft/util/ExtraCodecs.java').decode()
paths=['assets/minecraft/textures/item/lightning_bolt_linear.png','assets/lbc/textures/item/hedgehog_laser_ray.png']
colors=[]
for path in paths:
    file=OVERLAY/path if (OVERLAY/path).exists() else PACK/path
    im=Image.open(file).convert('RGBA')
    colors.extend(color for count,color in im.getcolors(im.width*im.height) if color[3]==255)
old=np.asarray(Image.open(PACK/paths[0]).convert('RGBA'));new=np.asarray(Image.open(OVERLAY/paths[0]).convert('RGBA'))
expected=old.copy();mask=(old==[255,252,252,255]).all(axis=2);expected[mask]=[255,252,253,255]
assert np.array_equal(expected,new)
source=(PACK/'assets/lbc/shaders/include/bloom_rules.glsl').read_text(encoding='utf8')
ctx=moderngl.create_standalone_context(require=330)
vertex='#version 330\nvoid main(){vec2 p=vec2((gl_VertexID << 1)&2,gl_VertexID&2);gl_Position=vec4(p*2.-1.,0,1);}'
fragment='#version 330\n'+source+'\nuniform vec3 testColor;out vec4 result;void main(){int i=lbc_bloom_rule(testColor);result=vec4(i>=0?1.:0.,0,0,1);}'
program=ctx.program(vertex_shader=vertex,fragment_shader=fragment);tex=ctx.texture((1,1),4);frame=ctx.framebuffer([tex]);frame.use();vao=ctx.vertex_array(program,[])
for color in set(colors):
    program['testColor'].value=tuple(v/255 for v in color[:3]);vao.render(vertices=3)
    assert frame.read(components=4)[0]==255,color
print('PASS',ctx.info['GL_RENDERER'],'actual bloom GLSL classified',sorted(set(colors)),'lightning changed pixels',int(mask.sum()),'native RGB codec and reserved laser tint verified')
