"""Compile actual entity pipelines and render deterministic armor seeds on a GPU."""
from pathlib import Path
import ast,hashlib,json,re,struct,zipfile
import moderngl
import numpy as np

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
RP=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
JAR=zipfile.ZipFile(ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3.jar')
def source(name,original=False):
    for base in ((RP,) if original else (HERE,RP)):
        p=base/name
        if p.is_file():return p.read_text(encoding='utf-8-sig')
    return JAR.read(name).decode()
def expand(text,original=False):
    def include(match):
        namespace,name=match[1].split(':',1)
        return expand(source(f'assets/{namespace}/shaders/include/{name}',original),original)
    return re.sub(r'#include\s+<([^>]+)>',include,text).replace('gl_VertexIndex','gl_VertexID')
ctx=moderngl.create_standalone_context(require=330)
vs='assets/minecraft/shaders/core/entity.vsh';fs='assets/minecraft/shaders/core/entity.fsh'
variants=['','#define ALPHA_CUTOUT 0.1','#define NO_OVERLAY\n#define ALPHA_CUTOUT 0.1',
          '#define NO_CARDINAL_LIGHTING','#define PER_FACE_LIGHTING','#define EMISSIVE',
          '#define GLINT\n#define NO_OVERLAY\n#define ALPHA_CUTOUT 0.1',
          '#define OIT_ALPHA_ONLY','#define OIT_ALPHA_ONLY\n#define ALPHA_CUTOUT 0.1',
          '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE',
          '#define OIT\n#define OIT_COEFF_COUNT 4\n#define OIT_WAVELET_RANK 1\n#define OIT_ACCUMULATE\n#define GLINT']
for variant in variants:
    ctx.program(vertex_shader=expand(source(vs).replace('#version 330','#version 330\n'+variant,1)),
                fragment_shader=expand(source(fs).replace('#version 330','#version 330\n'+variant,1))).release()
programs=[ctx.program(vertex_shader=expand(source(vs,old),old),fragment_shader=expand(source(fs,old),old)) for old in (False,True)]
frame=ctx.simple_framebuffer((384,384),components=4,dtype='f4')
dtype=np.dtype([('pos','f4',3),('color','f4',4),('uv','f4',2),('overlay','i4',2),('light','i4',2),('normal','f4',3)])
# Share the established native uniform/mesh harness; only the equipment texture
# marker and native full-skin UV range differ from the item-atlas test.
item_check=HERE.parent/'tmt_atlas_2026_11_06/check_tmt_atlas.py'
original=item_check.read_text(encoding='utf-8');tree=ast.parse(original)
function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='render')
render_source=ast.get_source_segment(original,function)
render_source=render_source.replace('padding_rgb=None):','padding_rgb=None, equipment=False):')
render_source=render_source.replace('pixels[16:32, 16:32] = rgba','pixels[16:32, 16:32] = rgba\n    if equipment: pixels[0,0] = (31,197,83,255)')
render_source=render_source.replace('16.01','0.01').replace('31.99','(atlas-0.01)')
exec(compile(render_source,str(item_check),'exec'))
for rgba in [(180,90,30,255),(255,14,14,254),(43,216,147,130),(31,197,83,255)]:
    for tint in [None,(250,12,34),(125,99,203)]:
        for light in [.3,1]:
            assert np.allclose(render(programs[0],rgba,tint=tint,light=light),render(programs[1],rgba,tint=tint,light=light),atol=1e-6)
reference=render(programs[0],(180,90,30,255),tint=(250,12,34),equipment=True)
assert np.isfinite(reference).all() and reference[:,:,3].max()==1
assert np.allclose(reference,render(programs[0],(180,90,30,255),tint=(250,12,34),equipment=True),atol=1e-6)
assert not np.allclose(reference,render(programs[0],(180,90,30,255),tint=(250,12,35),equipment=True))
for offset in [0,4,12]:
    assert np.allclose(reference,render(programs[0],(180,90,30,255),tint=(250,12,34),equipment=True,offset=offset),atol=1e-6)
for seed in [0,1,255,256,65535]:
    output=render(programs[0],(180,90,30,255),tint=(250,seed>>8,seed&255),equipment=True)
    assert np.isfinite(output).all() and output[:,:,3].max()==1
for light in [.3,1]:
    output=render(programs[0],(180,90,30,255),tint=(250,12,34),equipment=True,light=light)
    assert np.isfinite(output).all()
result={'passed':True,'entityPipelineVariants':len(variants),'ordinaryRenderComparisons':24,
        'repeatSeedIdentical':True,'distinctSeedVaries':True,'nativeArmorUvsNotVertexIndex':True,
        'seedEndpointsFinite':True,'gpu':ctx.info.get('GL_RENDERER'),
        'authorPackReadOnly':True,'physicalMinecraftRenderingTested':False,
        'assets':{p.relative_to(HERE).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (HERE/'assets').rglob('*') if p.is_file()}}
(HERE/'GPU_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='assets'}))
