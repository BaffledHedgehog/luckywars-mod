"""Pack three map-colour pixels into each native item tint; preserve the author's phone model.

The input resource pack is read-only. This creates an independent mergeable addon.
No maps, map backgrounds, auxiliary entities or per-frame resource-pack reloads.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
DEFAULT_PACK=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
ADDON=ROOT/"resourcepack_template/updates/tiktok_phone_2026_11_15"
WIDTH,HEIGHT,GROUP=64,112,3


def write(relative,value):
    path=ADDON/relative;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")


def without_phone_shader(source,extension):
    """Re-running after the user merges the addon must not insert it a second time."""
    source=source.replace('\r\n','\n')
    if '#include <lbc:tiktok_pixels.glsl>' not in source:return source
    if extension=='vsh':
        begin=source.index('#include <lbc:tiktok_pixels.glsl>')
        end=source.index('    uvec3 rawTint',begin)
        source=source[:begin]+'void main() {\n'+source[end:]
    else:
        begin=source.index('#define LBC_TIKTOK_FRAGMENT')
        end=source.index('void main() {',begin)
        source=source[:begin]+source[end:]
        fragment='    bool isPhone = tiktokPixels.w != 0u;\n    if (isPhone) rawColor = vec4(lbc_tiktok_colour(tiktokPixels.xyz, tiktokU), 1.0);\n'
        assert source.count(fragment)==1;source=source.replace(fragment,'',1)
        source=source.replace('int alphaMarker = (isTmt || isPhone) ?','int alphaMarker = isTmt ?',1)
        source=source.replace('bool bypassLighting = isPhone || alphaMarker','bool bypassLighting = alphaMarker',1)
        fragment='    if (isPhone) {\n        color = rawColor * vec4(1.0, 1.0, 1.0, ColorModulator.a);\n    } else if (alphaMarker == 254) {'
        assert source.count(fragment)==1;source=source.replace(fragment,'    if (alphaMarker == 254) {',1)
    assert 'tiktokPixels' not in source and 'tiktokU' not in source
    return source


def main():
    parser=argparse.ArgumentParser();parser.add_argument("--pack",type=Path,default=DEFAULT_PACK);args=parser.parse_args()
    pack=args.pack.resolve()
    if ADDON.resolve().is_relative_to(pack):raise ValueError("Output must be outside the author resource pack")
    owned={}
    def original(relative):
        data=(pack/relative).read_bytes();owned[relative]=hashlib.sha256(data).hexdigest();return data
    phone=json.loads(original("assets/lbc/models/item/zaika.json"))
    assert len(phone["elements"])==5 and phone["elements"][0]["to"]==[14,31,2]
    # Read/hash these dependencies without copying/replacing the author's images or mesh.
    for name in ("assets/lbc/items/zaika.json","assets/minecraft/textures/item/zaika.png","assets/minecraft/textures/item/zaika_frame.png"):
        original(name)
    texture=Image.new("RGBA",(4,4))
    for y in range(4):
        for x in range(4):texture.putpixel((x,y),(43,71+x,93+y,211))
    marker=ADDON/"assets/skywars2/textures/item/tiktok_pixels.png";marker.parent.mkdir(parents=True,exist_ok=True);texture.save(marker)
    Image.new("RGBA",(1,1),(0,0,0,255)).save(marker.with_name("tiktok_black.png"))
    # Video is letterboxed within the existing 14x32 screen; neither source ratio nor body transforms change.
    elements=[{"from":[0,-1,2.002],"to":[14,31,2.003],"shade":False,"light_emission":15,
               "faces":{"south":{"uv":[0,0,16,16],"texture":"#blank"}}}]
    pixel=13.7/WIDTH;left=.15;top=15+HEIGHT*pixel/2
    for y in range(HEIGHT):
        for x in range(0,WIDTH,GROUP):
            index=y*((WIDTH+GROUP-1)//GROUP)+x//GROUP
            elements.append({"from":[round(left+x*pixel,8),round(top-(y+1)*pixel,8),2.009],
                             "to":[round(left+min(WIDTH,x+GROUP)*pixel,8),round(top-y*pixel,8),2.01],
                             "shade":False,"light_emission":15,
                             "faces":{"south":{"uv":[0,0,16,16],"texture":"#pixels","tintindex":index}}})
    write("assets/skywars2/models/item/tiktok_screen.json",{"ambientocclusion":False,"gui_light":"front",
          "textures":{"pixels":"skywars2:item/tiktok_pixels","blank":"skywars2:item/tiktok_black","particle":"minecraft:item/zaika"},
          "display":phone["display"],"elements":elements})
    tints=[{"type":"minecraft:custom_model_data","index":i,"default":0} for i in range(len(elements)-1)]
    video={"type":"minecraft:composite","models":[
          {"type":"minecraft:model","model":"lbc:item/zaika"},
          {"type":"minecraft:model","model":"skywars2:item/tiktok_screen","tints":tints}]}
    write("assets/lbc/items/zaika_video.json",{"hand_animation_on_swap":False,"model":{"type":"minecraft:select",
          "property":"minecraft:display_context","cases":[{"when":["firstperson_righthand","firstperson_lefthand"],"model":video}],
          "fallback":{"type":"minecraft:model","model":"lbc:item/zaika"}}})
    jar=ROOT/".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar"
    with zipfile.ZipFile(jar) as archive:colors_source=archive.read("net/minecraft/world/level/material/MapColor.java").decode()
    colours={int(i):int(rgb) for i,rgb in re.findall(r"new MapColor\((\d+), (\d+)\)",colors_source)}
    palette=[[((colours.get(i//4,0)>>shift)&255)*[180,220,255,135][i%4]//255 for shift in (16,8,0)] for i in range(256)]
    palette_entries=",\n".join("    uvec3("+",".join(str(n)+"u" for n in p)+")" for p in palette)
    include='''#ifndef LBC_TIKTOK_PIXELS
#define LBC_TIKTOK_PIXELS
// A 4x4 tile signature including all four corners; exact texelFetch avoids mip/filter ambiguity.
bool lbc_tiktok_tile(sampler2D atlas, vec2 uv, out vec2 local) {
    ivec2 size = textureSize(atlas, 0);
    ivec2 position = clamp(ivec2(floor(uv * vec2(size))), ivec2(0), size - 1);
    ivec4 tag = ivec4(round(texelFetch(atlas, position, 0) * 255.0));
    local = vec2(0.0);
    if (tag.r != 43 || tag.a != 211 || tag.g < 71 || tag.g > 74 || tag.b < 93 || tag.b > 96) return false;
    ivec2 origin = position - ivec2(tag.g - 71, tag.b - 93);
    if (any(lessThan(origin, ivec2(0))) || any(greaterThanEqual(origin + 3, size))) return false;
    if (any(notEqual(ivec4(round(texelFetch(atlas, origin, 0) * 255.0)), ivec4(43,71,93,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(3,0), 0) * 255.0)), ivec4(43,74,93,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(0,3), 0) * 255.0)), ivec4(43,71,96,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(3,3), 0) * 255.0)), ivec4(43,74,96,211)))) return false;
    local = (uv * vec2(size) - vec2(origin)) * 0.25;
    return true;
}
#ifdef LBC_TIKTOK_FRAGMENT
const uvec3 LBC_TIKTOK_PALETTE[256] = uvec3[256](
'''+palette_entries+''');
vec3 lbc_tiktok_colour(uvec3 pixels, float localX) {
    int component = int(clamp(floor(localX * 3.0), 0.0, 2.0));
    return vec3(LBC_TIKTOK_PALETTE[int(pixels[component] & 255u)]) / 255.0;
}
#endif
#endif
'''
    include_path=ADDON/"assets/lbc/shaders/include/tiktok_pixels.glsl";include_path.parent.mkdir(parents=True,exist_ok=True);include_path.write_text(include,encoding="utf-8")
    for extension in ("vsh","fsh"):
        relative="assets/minecraft/shaders/core/entity."+extension;source=without_phone_shader(original(relative).decode("utf-8"),extension)
        baseline=ADDON/'PHONE_BASELINE'/('entity.'+extension);baseline.parent.mkdir(parents=True,exist_ok=True)
        baseline.write_bytes(source.encode('utf-8'));owned[relative]=hashlib.sha256(baseline.read_bytes()).hexdigest()
        assert source.count("void main() {")==1
        if extension=="vsh":
            declarations='''#include <lbc:tiktok_pixels.glsl>
layout(location = 10) flat out uvec4 tiktokPixels;
layout(location = 11) out float tiktokU;
void main() {
    uvec3 phoneTint = uvec3(round(Color.rgb * 255.0));
    vec2 phoneUV;
    bool phoneCarrier = all(lessThanEqual(phoneTint, uvec3(247u))) && lbc_tiktok_tile(Sampler0, UV0, phoneUV);
    tiktokPixels = uvec4(phoneTint, phoneCarrier ? 1u : 0u);
    tiktokU = phoneCarrier ? phoneUV.x : 0.0;
'''
            source=source.replace("void main() {",declarations,1)
        else:
            declarations='''#define LBC_TIKTOK_FRAGMENT
#include <lbc:tiktok_pixels.glsl>
layout(location = 10) flat in uvec4 tiktokPixels;
layout(location = 11) in float tiktokU;
void main() {'''
            source=source.replace("void main() {",declarations,1)
            source=source.replace("    bool isTmt =", "    bool isPhone = tiktokPixels.w != 0u;\n    if (isPhone) rawColor = vec4(lbc_tiktok_colour(tiktokPixels.xyz, tiktokU), 1.0);\n    bool isTmt =",1)
            source=source.replace("int alphaMarker = isTmt ?", "int alphaMarker = (isTmt || isPhone) ?",1)
            source=source.replace("bool bypassLighting = alphaMarker", "bool bypassLighting = isPhone || alphaMarker",1)
            source=source.replace("    if (alphaMarker == 254) {", "    if (isPhone) {\n        color = rawColor * vec4(1.0, 1.0, 1.0, ColorModulator.a);\n    } else if (alphaMarker == 254) {",1)
        destination=ADDON/relative;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_text(source,encoding="utf-8")
    write("PHONE_AUTHOR_BASE_SHA256.json",owned)
    print(json.dumps({"groups":len(tints),"nativePixels":WIDTH*HEIGHT,"faces":len(elements),"authorInputs":len(owned),"output":str(ADDON)}))


if __name__=="__main__":main()
