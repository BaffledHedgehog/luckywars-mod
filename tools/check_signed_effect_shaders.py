import json,re,zipfile,struct
from pathlib import Path
import numpy as np
import moderngl
ROOT=Path(__file__).resolve().parents[1]
RP=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8")
TEMPLATE=ROOT/"resourcepack_template"
JAR=zipfile.ZipFile(next(p for p in (ROOT/".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob("*.jar") if not p.name.endswith("-sources.jar")))
OUT=ROOT/"build/verification/signed-effect-visual-gpu";OUT.mkdir(parents=True,exist_ok=True)
def source(path):
 for base in [TEMPLATE,RP]:
  p=base/path
  if p.is_file():return p.read_text(encoding="utf-8")
 return JAR.read(str(path).replace("\\","/")).decode()
def expand(s):
 def inc(m):
  ns,name=m.group(1).split(":",1)
  return expand(source(Path("assets")/ns/"shaders/include"/name))
 return re.sub(r"#include\s+<([^>]+)>",inc,s).replace("gl_VertexIndex","gl_VertexID")
def shader(path):return expand(source(Path(path)))
ctx=moderngl.create_standalone_context(require=330)
results={"renderer":ctx.info["GL_RENDERER"],"version":ctx.info["GL_VERSION"],"checks":[]}
def check(ok,msg):
 assert ok,msg
 results["checks"].append(msg)
vs="#version 330\n#extension GL_ARB_separate_shader_objects : require\nlayout(location=0) out vec2 texCoord;void main(){vec2 uv=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(uv*2.0-1.0,0,1);texCoord=uv;}"
core=ctx.program(vertex_shader=shader("assets/minecraft/shaders/core/rendertype_outline.vsh"),fragment_shader=shader("assets/minecraft/shaders/core/rendertype_outline.fsh"))
check(True,"combined original plus new outline vertex/fragment shaders compile and link")
outline=source(Path("assets/minecraft/shaders/core/rendertype_outline.vsh"))
check("lbc_bh_slot(Color.rgb)" in outline and "lbc_nuclear_slot(Color.rgb)" in outline,
      "original black-hole/nuclear marker recognition retained")
marker=source(Path("assets/lbc/shaders/include/signed_glow_data.glsl"))
check("FIRST_ROW=50" in marker and "COUNT=8" in marker and "b.r==213 && b.g==117" in marker,
      "new glow marker and rows do not overlap zone/black-hole/nuclear channels")
post=json.loads(source(Path("assets/minecraft/post_effect/entity_outline.json")))
passes=[p["fragment_shader"] for p in post["passes"]]
check(passes.index("lbc:post/zone_damage") < passes.index("lbc:post/signed_glow") < passes.index("minecraft:post/entity_sobel"),
      "glow consumes raw carrier records after existing alpha transfer and before sobel")
check("scene.a" in source(Path("assets/lbc/shaders/post/signed_glow.fsh")),"glow has no effect-history alpha rewrite")
visual=ctx.program(vertex_shader=vs,fragment_shader=shader("assets/lbc/shaders/post/signed_effect.fsh"))
visual["SignedEffectConfig"].binding=0;visual["Globals"].binding=1
config=ctx.buffer(reserve=16);config.bind_to_uniform_block(0)
globals=ctx.buffer(struct.pack("3if3ff2f2i",0,0,0,0,0,0,0,0.02,512,256,0,0));globals.bind_to_uniform_block(1)
tex=ctx.texture((512,256),4,bytes([76,102,128,255])*(512*256));tex.use(0);visual["InSampler"].value=0
frame=ctx.simple_framebuffer((512,256),components=4);frame.use();vao=ctx.vertex_array(visual,[])
for mode,level in [(0,2),(0,3),(1,16),(2,16),(3,16),(4,16)]:
 config.write(struct.pack("4f",mode,level,0,0));vao.render(vertices=3)
 pixels=np.frombuffer(frame.read(components=4),dtype=np.uint8).reshape(256,512,4)
 check(bool(np.isfinite(pixels).all()),f"mode{mode} level{level} renders finite GPU pixels")
 if mode==0 and level==3:check(bool((pixels[:,:,:3]==0).all()),"blindness/darknessIII renders exact black")
 if mode==1:check(float(pixels[:,:,:3].mean())>102,"negative darkness increases brightness")
 if mode==2:check(pixels[128,256,0]>pixels[128,256,2],"negative nightvision reverses scene colors")
glow=ctx.program(vertex_shader=shader("assets/lbc/shaders/post/signed_glow.vsh"),fragment_shader=shader("assets/lbc/shaders/post/signed_glow.fsh"))
check(True,"individual glow vertex/fragment shaders compile and link")
glow["SceneSampler"].value=0;glow["GlowDataSampler"].value=1
scene=ctx.texture((512,256),4,bytes([16,16,16,255])*(512*256));scene.use(0)
gvao=ctx.vertex_array(glow,[])
def render(loglevel):
 data=np.zeros((256,512,4),dtype=np.uint8)
 refs=[0,0,0,loglevel,0,0,0,.5,0,0,0,16]+list(np.eye(4,dtype=np.float32).flatten())
 for i,value in enumerate(refs):
  b=struct.pack("f",value);data[50,i*2,:3]=[b[0],b[1],201];data[50,i*2+1,:3]=[b[2],b[3],202]
 data[50,56,:3]=[71,76,87]
 mask=ctx.texture((512,256),4,data.tobytes());mask.use(1);frame.use();gvao.render(vertices=3)
 pixels=np.frombuffer(frame.read(components=4),dtype=np.uint8).reshape(256,512,4).copy();mask.release();return pixels
small=render(1.0);large=render(5.0);huge=render(31.0)
check(int(large[:,:,:3].sum())>int(small[:,:,:3].sum()),"each carrier level independently grows halo radius/brightness")
check(int(huge[0,0,0])>16,"IntMax-level halo reaches screen corner at fixed shader work")
check(bool((huge[:,:,3]==255).all()),"glow pass preserves original scene alpha")
for path in (TEMPLATE/"assets/lbc/post_effect/signed_effects").glob("*.json"):
 parsed=json.loads(path.read_text());check(len(parsed["passes"])==2,"two-pass chain "+path.stem)
(OUT/"results.json").write_text(json.dumps(results,indent=2)+"\n",encoding="utf-8")
print(json.dumps({"renderer":results["renderer"],"checks":len(results["checks"]),"success":True}))
