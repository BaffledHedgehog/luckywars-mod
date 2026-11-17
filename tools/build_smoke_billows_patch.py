"""Prepare short-lived native smoke billows; never write the installed author pack.

Only four fully transparent flash texels gain invisible coordinate metadata.
The native flash art, ordinary fireworks and all other particle branches stay intact.
"""
import argparse
import hashlib
from io import BytesIO
import json
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ADDON = ROOT / "resourcepack_template/updates/smoke_billows_2026_11_17"
JAR = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3.jar"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    args = parser.parse_args()
    pack = args.pack.resolve()
    if pack == ADDON.resolve() or pack in ADDON.resolve().parents:
        raise ValueError("Output must be outside the author pack")
    inputs = {}
    archive = zipfile.ZipFile(JAR)

    def read(name):
        is_author = (pack / name).is_file()
        raw = (pack / name).read_bytes() if is_author else archive.read(name)
        inputs[name] = {"source": "author" if is_author else "native", "sha256": hashlib.sha256(raw).hexdigest()}
        return raw

    def write(name, value):
        path = ADDON / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(value.encode("utf-8") if isinstance(value, str) else value)

    def replace(text, old, new):
        if text.count(old) != 1:
            raise ValueError(f"Author shader changed; expected one {old!r}")
        return text.replace(old, new)

    definition = json.loads(read("assets/minecraft/particles/flash.json"))
    if definition != {"textures": ["minecraft:flash"]}:
        raise ValueError("Recheck the changed flash sprite definition before preparing metadata")
    name = "assets/minecraft/textures/particle/flash.png"
    image = Image.open(BytesIO(read(name))).convert("RGBA")
    corners = [(0, 0), (image.width-1, 0), (image.width-1, image.height-1), (0, image.height-1)]
    for index, (x, y) in enumerate(corners):
        # Preserve alpha exactly. The adjacent bilinear footprint must remain
        # below the native .1 cutout even at full vertex opacity.
        if image.getpixel((x,y))[3] != 0:
            raise ValueError("Do not replace a visible flash corner")
        for dx in (0, 1 if x == 0 else -1):
            for dy in (0, 1 if y == 0 else -1):
                if image.getpixel((x+dx, y+dy))[3] >= 25:
                    raise ValueError("The artist changed a flash corner; do not overwrite visible pixels")
        image.putpixel((x, y), (83, 77, 241+index, 0))
    output = BytesIO();image.save(output, format="PNG");write(name, output.getvalue())
    metadata_name=name+'.mcmeta'
    if (pack/metadata_name).is_file():
        metadata=json.loads(read(metadata_name))
    elif metadata_name in archive.namelist():
        metadata=json.loads(read(metadata_name))
    else:
        metadata={};inputs[metadata_name]={"source":"absent","sha256":None}
    if 'animation' in metadata:
        raise ValueError("Recheck animated flash frames before assigning corner metadata")
    # AUTO/CUTOUT solidifies transparent RGB even at mip0 in26.3. MEAN keeps
    # hidden coordinate bytes; ordinary flash coverage and visible RGB are intact.
    metadata.setdefault('texture',{})['mipmap_strategy']='mean'
    write(metadata_name,json.dumps(metadata,indent=2)+'\n')

    name = "assets/minecraft/shaders/core/particle.vsh"
    text = read(name).decode("utf-8")
    text = replace(text, "uniform sampler2D Sampler2;", "uniform sampler2D Sampler0;\nuniform sampler2D Sampler2;")
    text = replace(text, "layout(location = 5) out vec2 particleCoord;", "layout(location = 5) out vec2 particleCoord;\nlayout(location = 6) out vec3 smokeWorldRay;")
    text = replace(text, "void main() {", """// Recover corners from this one marked sprite, not the shared VBO's vertex phase.
// Native indexed draws may start at ANY baseVertex after another vertex format.
bool lbc_smoke_corner(out vec2 uv) {
    ivec2 size=textureSize(Sampler0,0);
    vec2 boundary=UV0*vec2(size);
    ivec2 pixel=ivec2(round(boundary));
    uv=vec2(.5);
    if(any(greaterThan(abs(boundary-vec2(pixel)),vec2(.02))))return false;
    const ivec2 offsets[4]=ivec2[](ivec2(0,0),ivec2(-1,0),ivec2(-1,-1),ivec2(0,-1));
    const vec2 coords[4]=vec2[](vec2(0,0),vec2(1,0),vec2(1,1),vec2(0,1));
    for(int i=0;i<4;i++) {
        ivec2 at=pixel+offsets[i];
        if(any(lessThan(at,ivec2(0)))||any(greaterThanEqual(at,size)))continue;
        ivec4 tag=ivec4(round(texelFetch(Sampler0,at,0)*255.0));
        if(all(equal(tag,ivec4(83,77,241+i,0)))){uv=coords[i];return true;}
    }
    return false;
}

void main() {""")
    needle = """        customParticle=3;
        // Native QuadParticleRenderState corners are(+,-),(+,+),(-,+),(-,-)."""
    text = replace(text, needle, """        vec2 nativeUV;
        if(lbc_smoke_corner(nativeUV)) {
            customParticle=4;
            particleCoord=nativeUV;
            vec2 side=vec2(nativeUV.x*2.0-1.0,1.0-nativeUV.y*2.0);
            // OverlayParticle encodes its own local age in alpha before extraction:
            // alpha=.725-age*.125. Midpoint decoding bounds quantization error.
            // No server/client clock or network-latency-dependent birth timestamp.
            float age=(.725-(round(Color.a*255.0)+.5)/255.0)*8.0;
            float nativeSize=7.1*sin((age-1.0)*.25*3.14159265359);
            float grow=clamp((age-1.0)*1.5,0.0,1.0);
            float fade=1.0-smoothstep(3.0,4.0,age);
            vec4 view=ModelViewMat*vec4(Position,1.0);
            view.xy-=side*nativeSize;
            view.xy+=side*vec2(8.0,5.2)*(float(checkColor.b)/127.0)*grow*(.8+.2*fade);
            view.xyz+=mat3(ModelViewMat)*vec3(0.0,max(0.0,age-1.0)*.07,0.0);
            smokeWorldRay=transpose(mat3(ModelViewMat))*view.xyz;
            pre_Position=ProjMat*view;
            if(age<1.0||age>=4.0)pre_Position=vec4(2.0,2.0,2.0,1.0);
            vertexColor=vec4(vec3(.46),fade);
        } else {
        // Compatibility with an older server's potion-particle smoke carrier.
        customParticle=3;
        // Native QuadParticleRenderState corners are(+,-),(+,+),(-,+),(-,-).""")
    text = replace(text, """        vertexColor=vec4(vec3(.48),1.0);
    }""", """        vertexColor=vec4(vec3(.48),1.0);
        }
    }""")
    text = replace(text, "vec4 pre_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);",
                   "vec4 pre_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n    smokeWorldRay=Position;")
    write(name, text)

    name = "assets/minecraft/shaders/core/particle.fsh"
    text = read(name).decode("utf-8")
    text = replace(text, "#include <minecraft:fog.glsl>", "#include <minecraft:fog.glsl>\n#include <minecraft:globals.glsl>")
    text = replace(text, "layout(location = 5) in vec2 particleCoord;", "layout(location = 5) in vec2 particleCoord;\nlayout(location = 6) in vec3 smokeWorldRay;")
    text = replace(text, """    if (color.a < 0.1) {""", """    if(customParticle==4) {
        vec2 p=particleCoord*2.0-1.0;
        float edge=.84+.05*sin(p.x*9.0+p.y*5.0)+.03*sin(p.y*13.0-p.x*6.0);
        float coverage=1.0-smoothstep(edge-.08,edge,length(p));
        // Equal, opaque interiors do not reveal seams where billboards overlap.
        // Only the outer rim and the final native tick fade into the next puff.
        // Shared world-oriented shading: every puff on this view ray has the
        // same colour, so depth order cannot draw a patchwork of visible seams.
        vec3 camera=vec3(CameraBlockPos)-CameraOffset;
        vec3 q=normalize(smokeWorldRay)*18.0+camera*.12+GameTime*24000.0*vec3(.009,.004,-.006);
        float billow=sin(dot(q,vec3(1.17,.93,1.42))+.4*sin(dot(q,vec3(-1.3,2.2,.7))));
        float shade=.46+.035*billow;
        color=vec4(vec3(shade),coverage*vertexColor.a)*ColorModulator;
    }
    if (color.a < 0.1) {""")
    write(name, text)
    for name, entry in inputs.items():
        if entry["source"] == "author" and hashlib.sha256((pack / name).read_bytes()).hexdigest() != entry["sha256"]:
            raise AssertionError("Author input changed during preparation: " + name)
    write("AUTHOR_BASE_SHA256.json", json.dumps(inputs, indent=2) + "\n")
    archive.close()
    print(json.dumps({"addon": str(ADDON), "authorPackUnchanged": True, "transparentMetadataTexels": 4}))


if __name__ == "__main__":
    main()
