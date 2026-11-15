"""Build the November15 fixes from the current author pack, without modifying it."""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/verification/feedback_2026_11_15/assets-staging"
DEFAULT_PACK = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")


HEADER = """#ifndef LBC_RETICLE_HEADER
#define LBC_RETICLE_HEADER
// Outside TimeStop's last12 pixels. RGB is preserved: this is invisible metadata.
const int LBC_RETICLE_BYTES = 28;
const int LBC_RETICLE_MAGIC[4] = int[4](83,80,82,219);
int lbc_reticle_offset(ivec2 pixel,ivec2 size) {
    int n=size.x-20-pixel.x;
    return (pixel.y==0 || pixel.y==size.y-1) && n>=0 && n<LBC_RETICLE_BYTES ? n : -1;
}
#endif
"""

STAMP = """#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 spreadBounds;
layout(location=1) flat in vec2 spreadStyle;
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    fragColor=texelFetch(SceneSampler,pixel,0);
    int n=lbc_reticle_offset(pixel,textureSize(SceneSampler,0));
    if(n<0)return;
    int value;
    if(n<4)value=LBC_RETICLE_MAGIC[n];
    else {
        int field=(n-4)/4;
        float v=field<4 ? spreadBounds[field] : spreadStyle[field-4];
        value=int((floatBitsToUint(v)>>uint(((n-4)%4)*8))&255u);
    }
    fragColor.a=float(value)/255.0;
}
"""

RESOLVE_VERTEX = """#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat out vec4 reticleBounds;
layout(location=1) flat out vec2 reticleStyle;
layout(location=2) flat out vec2 reticleCenter;
layout(location=3) flat out vec4 nativeRect;
int byteAt(int n,int row,ivec2 size) {
    return int(round(texelFetch(SceneSampler,ivec2(size.x-20-n,row),0).a*255.0));
}
bool header(int row,ivec2 size) {
    for(int i=0;i<4;i++)if(byteAt(i,row,size)!=LBC_RETICLE_MAGIC[i])return false;
    return true;
}
bool nativePixel(ivec2 p,ivec2 size) {
    return all(greaterThanEqual(p,ivec2(0))) && all(lessThan(p,size)) &&
        int(round(texelFetch(SceneSampler,p,0).a*255.0))==251;
}
void main() {
    vec2 uv=vec2((gl_VertexIndex<<1)&2,gl_VertexIndex&2);
    gl_Position=vec4(uv*2.0-1.0,0,1);
    reticleBounds=vec4(0);reticleStyle=vec2(0);nativeRect=vec4(0);
    ivec2 size=textureSize(SceneSampler,0);
    reticleCenter=vec2(size)*.5;
    if(size.x<48 || size.y<2)return;
    int row=header(0,size)?0:size.y-1;
    if(!header(row,size))return;
    float fields[6];
    for(int i=0;i<6;i++) {
        uint bits=0u;
        for(int b=0;b<4;b++)bits|=uint(byteAt(4+i*4+b,row,size))<<uint(b*8);
        fields[i]=uintBitsToFloat(bits);
        if(isnan(fields[i]) || isinf(fields[i]) || fields[i]<0)return;
    }
    if(fields[4]<=0 || fields[4]>64 || fields[5]>256)return;
    // Inspect the actual GUI stroke, including its pixel-snapped center/GUI scale.
    // With F1, a scope or a menu there is no native stroke: retain that behavior.
    ivec2 middle=size/2,found=ivec2(-1);
    for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)
        if(nativePixel(middle+ivec2(x,y),size))found=middle+ivec2(x,y);
    if(found.x<0)return;
    ivec2 lo=found,hi=found;
    for(int i=-128;i<=128;i++)for(int d=-1;d<=1;d++) {
        ivec2 x=ivec2(middle.x+i,middle.y+d),y=ivec2(middle.x+d,middle.y+i);
        if(nativePixel(x,size)){lo.x=min(lo.x,x.x);hi.x=max(hi.x,x.x);}
        if(nativePixel(y,size)){lo.y=min(lo.y,y.y);hi.y=max(hi.y,y.y);}
    }
    nativeRect=vec4(lo,hi+1);
    reticleCenter=(vec2(lo+hi)+1.0)*.5;
    reticleBounds=vec4(fields[0],fields[1],fields[2],fields[3]);
    reticleStyle=vec2(fields[4],fields[5]);
}
"""

RESOLVE_FRAGMENT = """#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 reticleBounds;
layout(location=1) flat in vec2 reticleStyle;
layout(location=2) flat in vec2 reticleCenter;
layout(location=3) flat in vec4 nativeRect;
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy),size=textureSize(SceneSampler,0);
    vec4 scene=texelFetch(SceneSampler,pixel,0);
    bool nativeTag=int(round(scene.a*255.0))==251 && all(lessThan(abs(gl_FragCoord.xy-vec2(size)*.5),vec2(128)));
    bool nativeStroke=nativeTag && all(greaterThanEqual(gl_FragCoord.xy,nativeRect.xy)) && all(lessThan(gl_FragCoord.xy,nativeRect.zw));
    fragColor=scene;
    if(nativeTag || lbc_reticle_offset(pixel,size)>=0)fragColor.a=1.0;
    if(reticleStyle.x<=0)return;
    vec2 delta=gl_FragCoord.xy-reticleCenter;
    vec4 bounds=max(reticleBounds,vec4(1.0));
    float thickness=min(reticleStyle.x,min(min(bounds.x,bounds.y),min(bounds.z,bounds.w))*2.0);
    float lengthPx=reticleStyle.y;
    bool horizontal=delta.y>=-thickness*.5 && delta.y<thickness*.5 &&
        ((delta.x<0 && -delta.x<=bounds.x && -delta.x>=max(0.0,bounds.x-lengthPx)) ||
         (delta.x>=0 && delta.x<bounds.y && delta.x>=max(0.0,bounds.y-lengthPx)));
    bool vertical=delta.x>=-thickness*.5 && delta.x<thickness*.5 &&
        ((delta.y<0 && -delta.y<=bounds.z && -delta.y>=max(0.0,bounds.z-lengthPx)) ||
         (delta.y>=0 && delta.y<bounds.w && delta.y>=max(0.0,bounds.w-lengthPx)));
    bool within=delta.x>=-bounds.x && delta.x<bounds.y && delta.y>=-bounds.z && delta.y<bounds.w;
    // Exactly2x2 physical pixels even at zero spread; never a disappearing crosshair.
    vec2 dotOrigin=floor(reticleCenter)-vec2(1);
    bool centerDot=all(greaterThanEqual(gl_FragCoord.xy,dotOrigin)) &&
        all(lessThan(gl_FragCoord.xy,dotOrigin+2.0));
    bool desired=horizontal || vertical || centerDot || (nativeStroke && within);
    // The scene already contains the GUI inversion. Undo only unwanted native
    // pixels and invert only newly added pixels. Overlap is inverted just once.
    if(nativeStroke!=desired)fragColor.rgb=vec3(1.0)-scene.rgb;
}
"""


def main():
    global OUT
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK)
    parser.add_argument("--output", type=Path, default=OUT)
    args = parser.parse_args()
    OUT=args.output.resolve()
    pack = args.pack.resolve()
    if OUT.resolve().is_relative_to(pack):
        raise ValueError("Author pack must remain read-only")
    original = {}

    def read(relative):
        data = (pack / relative).read_bytes()
        original[relative] = hashlib.sha256(data).hexdigest()
        return data.decode("utf-8").replace("\r\n", "\n")

    def write(relative, text):
        path = OUT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")

    write("assets/lbc/shaders/include/reticle_header.glsl", HEADER)
    write("assets/lbc/shaders/post/weapon_spread.fsh", STAMP)
    vertex = read("assets/lbc/shaders/post/weapon_spread.vsh")
    assert vertex.count("degrees<=0.0")==1 or vertex.count("degrees<0.0")==1
    write("assets/lbc/shaders/post/weapon_spread.vsh", vertex.replace("degrees<=0.0", "degrees<0.0"))
    write("assets/lbc/shaders/post/weapon_spread_resolve.vsh", RESOLVE_VERTEX)
    write("assets/lbc/shaders/post/weapon_spread_resolve.fsh", RESOLVE_FRAGMENT)
    relative = "assets/minecraft/shaders/core/position_tex_color.fsh"
    source = read(relative)
    old = "fragColor=vertexColor*ColorModulator;"
    assert source.count(old)==1 or source.count("251.0/255.0")==1
    # Native INVERT uses RGB factors, while output alpha replaces destination
    # alpha (ONE/ZERO). Tagging alpha does not change the native crosshair RGB.
    source = source.replace(old, "fragColor=vec4((vertexColor*ColorModulator).rgb,251.0/255.0);")
    write(relative, source)
    relative = "assets/minecraft/post_effect/end_of_frame.json"
    chain = json.loads(read(relative))
    chain["passes"]=[p for p in chain["passes"] if "weapon_spread_resolve" not in str(p)
                     and not (p.get("fragment_shader")=="minecraft:post/blit" and any(i.get("target")=="reticle_scene" for i in p.get("inputs",[])))]
    chain["targets"]["reticle_scene"] = {}
    # Consume GUI alpha tags before bloom/nuclear/black-hole passes can replace
    # scene alpha. TimeStop's independent last12-pixel header is preserved.
    at = 0
    chain["passes"][at:at] = [
        {"vertex_shader":"lbc:post/weapon_spread_resolve","fragment_shader":"lbc:post/weapon_spread_resolve",
         "inputs":[{"sampler_name":"Scene","target":"minecraft:main"}],"output":"reticle_scene"},
        {"vertex_shader":"minecraft:core/screenquad","fragment_shader":"minecraft:post/blit",
         "inputs":[{"sampler_name":"In","target":"reticle_scene"}],"output":"minecraft:main",
         "uniforms":{"BlitConfig":[{"name":"ColorModulate","type":"vec4","value":[1,1,1,1]}]}}
    ]
    write(relative, json.dumps(chain, indent=2)+"\n")
    relative = "assets/minecraft/post_effect/entity_outline.json"
    chain = json.loads(read(relative))
    obsolete = [p for p in chain["passes"] if p.get("fragment_shader")=="lbc:post/smoke_volume"]
    if obsolete:
        assert len(obsolete)==1
        replacement=next(i["target"] for i in obsolete[0]["inputs"] if i["sampler_name"]=="Scene")
        old_target=obsolete[0]["output"]
        chain["passes"]=[p for p in chain["passes"] if p not in obsolete]
        chain["targets"].pop(old_target,None)
        for p in chain["passes"]:
            for i in p.get("inputs",[]):
                if i.get("target")==old_target:i["target"]=replacement
    write(relative,json.dumps(chain,indent=2)+"\n")
    relative = "assets/minecraft/shaders/core/particle.vsh"
    source = read(relative)
    marker = "    switch(customParticle){"
    assert source.count(marker)==1
    source = source.replace(marker, """    if(checkColor.r==83 && checkColor.g==77 && checkColor.b>=1 && checkColor.b<=127) {
        customParticle=3;
        // Native QuadParticleRenderState corners are(+,-),(+,+),(-,+),(-,-).
        // Expand in camera space, retaining native positions, drift and depth.
        vec2 side=vec2(corner<2 ? 1.0 : -1.0,corner==1||corner==2 ? 1.0 : -1.0);
        particleCoord=side*.5+.5;
        vec4 view=ModelViewMat*vec4(Position,1.0);
        view.xy+=side*(.12+2.3*float(checkColor.b)/127.0);
        pre_Position=ProjMat*view;
        vertexColor=vec4(vec3(.48),1.0);
    }
""" + marker) if "customParticle=3;" not in source else source
    write(relative, source)
    relative = "assets/minecraft/shaders/core/particle.fsh"
    source = read(relative)
    marker = "    if (color.a < 0.1) {"
    assert source.count(marker)==1
    source = source.replace(marker, """    if(customParticle==3) {
        // A soft2D billow, not a fullscreen fog volume or a solid square.
        vec2 p=particleCoord*2.0-1.0;
        float edge=.79+.075*sin(p.x*9.0+p.y*5.0)+.045*sin(p.y*13.0-p.x*6.0);
        float coverage=1.0-smoothstep(edge-.34,edge,length(p));
        float shade=.45+.055*sin(p.x*7.0-p.y*8.0);
        color=vec4(vec3(shade),coverage*.9)*ColorModulator;
    }
""" + marker) if "if(customParticle==3)" not in source else source
    write(relative, source)
    relative = "assets/lbc/shaders/include/phone_hd_pixels.glsl"
    source = read(relative)
    # Native FaceBakery classifies the source sprite before the fragment shader
    # turns it opaque. Alpha211 needlessly sent136640quads through translucent
    # sorting/OIT. Accept both markers while the new sprite uses alpha255.
    if "tag.a != 255" not in source:
        source=source.replace("tag.a != 211", "(tag.a != 211 && tag.a != 255)")
    source,count=re.subn(r"(ivec4\(43,\d+,\d+),211\)",r"\1,tag.a)",source)
    assert count==4 or source.count(",tag.a)")==4
    old = """    int component = int(clamp(floor(localX * 3.0), 0.0, 2.0));
    return vec3(LBC_TIKTOK_PALETTE[int(pixels[component] & 255u)]) / 255.0;"""
    assert source.count(old)==1 or source.count("float width=max(fwidth(localX*3.0)")==1
    source = source.replace(old, """    // Integrate the physical pixel footprint instead of point-sampling three
    // sharp subpixels. Keep all native palette colors; no reduced palette.
    float x=clamp(localX*3.0,0.0,3.0);
    float width=max(fwidth(localX*3.0),.0001);
    float lo=x-width*.5,hi=x+width*.5,total=0.0;
    vec3 rgb=vec3(0);
    for(int i=0;i<3;i++) {
        float weight=max(0.0,min(hi,float(i+1))-max(lo,float(i)));
        rgb+=vec3(LBC_TIKTOK_PALETTE[int(pixels[i]&255u)])*weight;
        total+=weight;
    }
    return rgb/(255.0*max(total,.00001));""")
    write(relative, source)
    from PIL import Image
    relative="assets/skywars2/textures/item/tiktok_pixels.png"
    raw=(pack/relative).read_bytes();original[relative]=hashlib.sha256(raw).hexdigest()
    with Image.open(pack/relative) as marker:
        marker=marker.convert("RGBA");marker.putalpha(255)
        path=OUT/relative;path.parent.mkdir(parents=True,exist_ok=True);marker.save(path)
    relative="assets/lbc/textures/block/consumed_block.png"
    raw=(pack/relative).read_bytes()
    original[relative]=hashlib.sha256(raw).hexdigest()
    with Image.open(pack/relative) as image:
        if image.size==(1,1):
            # Identical uniform RGBA, but no longer forces the entire block atlas
            # down from mip4 to mip0. No appearance/color change.
            path=OUT/relative;path.parent.mkdir(parents=True,exist_ok=True)
            Image.new("RGBA",(16,16),image.convert("RGBA").getpixel((0,0))).save(path)
        else:
            path=OUT/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
    # Increase separation from both the original phone face and the letterbox.
    # Stream the existing mesh: do not materialize136640 nested dictionaries.
    relative = "assets/skywars2/models/item/tiktok_screen.json"
    path = pack / relative
    with path.open("rb") as inp:
        original[relative] = hashlib.file_digest(inp,"sha256").hexdigest()
    dest = OUT / relative
    dest.parent.mkdir(parents=True,exist_ok=True)
    depths={"2.002":"2.099","2.003":"2.1","2.009":"2.199","2.01":"2.2"}
    depth_changes=0
    def shift_depth(text):
        nonlocal depth_changes
        # Change only Z in a numeric XYZ triple, never substrings of an X/Y value.
        def vector(m):
            y=float(m[4]);z=m[6]
            if z in ("2.009","2.01"):
                # Keep480x854 and its aspect, move its top to29.5. The original
                # status bar (time/battery) occupies the uncovered29.55..31.
                y+=29.5-(15+854*(13.7/480)/2)
            elif z=="2.003":y=29.55
            return m[1]+m[2]+m[3]+format(y,".8f").rstrip("0").rstrip(".")+m[5]+depths[z]+m[7]
        result,count=re.subn(r"(\[\s*)(-?\d+(?:\.\d+)?)(\s*,\s*)(-?\d+(?:\.\d+)?)(\s*,\s*)(2\.002|2\.003|2\.009|2\.01)(\s*\])",
                             vector,text)
        depth_changes+=count
        return result
    with path.open(encoding="utf-8") as inp,dest.open("w",encoding="utf-8",newline="\n") as output:
        carry = ""
        while chunk := inp.read(1024*1024):
            text = carry+chunk
            boundaries=list(re.finditer(r"\},\s*\{",text))
            if not boundaries:
                carry=text
                continue
            cut=boundaries[-1].start()+1
            output.write(shift_depth(text[:cut]))
            carry=text[cut:]
        output.write(shift_depth(carry))
    # Author editors may pretty-print this85MB mesh. Both formatting styles
    # must actually move every face; a byte-identical copy is not a depth fix.
    with path.open(encoding="utf-8") as inp:prefix=inp.read(16000)
    assert depth_changes==2*(480*854//3+1) or (depth_changes==0 and re.search(r"29\.5\s*,\s*2\.2",prefix)),depth_changes
    write("AUTHOR_BASE_SHA256.json", json.dumps(original, indent=2)+"\n")
    files = {}
    for path in (OUT/"assets").rglob("*"):
        if path.is_file():
            with path.open("rb") as stream:
                files[path.relative_to(OUT).as_posix()]=hashlib.file_digest(stream,"sha256").hexdigest()
    write("export-manifest.json", json.dumps({"files":files,"authorPackWritten":False}, indent=2)+"\n")
    print(json.dumps({"files":len(files),"output":str(OUT),"authorPackWritten":False}))


if __name__ == "__main__":
    main()
