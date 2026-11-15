"""Generate a real 480x854 native phone screen and patch the 26.3 ITEM shader. Author RP is read-only."""
import argparse, hashlib, json, re, zipfile
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
DEFAULT_PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
ADDON=ROOT/'resourcepack_template/updates/tiktok_hd_2026_11_15'
WIDTH,HEIGHT,GROUP=480,854,3

def write(name,value):
    p=ADDON/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--pack',type=Path,default=DEFAULT_PACK);args=parser.parse_args();pack=args.pack.resolve()
    if ADDON.resolve().is_relative_to(pack):raise ValueError('Output must be outside the author resource pack')
    owned={}
    def original(relative):
        data=(pack/relative).read_bytes();owned[relative]=hashlib.sha256(data).hexdigest();return data
    phone=json.loads(original('assets/lbc/models/item/zaika.json'))
    for name in ['assets/lbc/items/zaika.json','assets/minecraft/textures/item/zaika.png','assets/minecraft/textures/item/zaika_frame.png']:original(name)
    marker=ADDON/'assets/skywars2/textures/item/tiktok_pixels.png';marker.parent.mkdir(parents=True,exist_ok=True)
    image=Image.new('RGBA',(64,64))
    for y in range(64):
        for x in range(64):image.putpixel((x,y),(43,32+x,128+y,211))
    image.save(marker);Image.new('RGBA',(1,1),(0,0,0,255)).save(marker.with_name('tiktok_black.png'))
    model_path=ADDON/'assets/skywars2/models/item/tiktok_screen.json';model_path.parent.mkdir(parents=True,exist_ok=True)
    properties={'ambientocclusion':False,'gui_light':'front','textures':{'pixels':'skywars2:item/tiktok_pixels','blank':'skywars2:item/tiktok_black','particle':'minecraft:item/zaika'},'display':phone['display']}
    pixel=13.7/WIDTH;left=.15;top=15+HEIGHT*pixel/2;groups=((WIDTH+GROUP-1)//GROUP)*HEIGHT
    # Stream the static mesh rather than retaining 136640 nested dictionaries in RAM.
    with model_path.open('w',encoding='utf-8') as out:
        out.write(json.dumps(properties,separators=(',',':'))[:-1]+',"elements":[')
        out.write(json.dumps({'from':[0,-1,2.002],'to':[14,31,2.003],'shade':False,'light_emission':15,'faces':{'south':{'uv':[0,0,16,16],'texture':'#blank'}}},separators=(',',':')))
        for y in range(HEIGHT):
            for x in range(0,WIDTH,GROUP):
                index=y*((WIDTH+GROUP-1)//GROUP)+x//GROUP
                element={'from':[round(left+x*pixel,8),round(top-(y+1)*pixel,8),2.009],'to':[round(left+min(WIDTH,x+GROUP)*pixel,8),round(top-y*pixel,8),2.01],'shade':False,'light_emission':15,'faces':{'south':{'uv':[0,0,16,16],'texture':'#pixels','tintindex':index}}}
                out.write(','+json.dumps(element,separators=(',',':')))
        out.write(']}\n')
    definition=ADDON/'assets/lbc/items/zaika_video.json';definition.parent.mkdir(parents=True,exist_ok=True)
    with definition.open('w',encoding='utf-8') as out:
        out.write('{"hand_animation_on_swap":false,"model":{"type":"minecraft:select","property":"minecraft:display_context","cases":[{"when":["firstperson_righthand","firstperson_lefthand"],"model":{"type":"minecraft:composite","models":[{"type":"minecraft:model","model":"lbc:item/zaika"},{"type":"minecraft:model","model":"skywars2:item/tiktok_screen","tints":[')
        for i in range(groups):
            if i:out.write(',')
            out.write(json.dumps({'type':'minecraft:custom_model_data','index':i,'default':0},separators=(',',':')))
        out.write(']}]}}],"fallback":{"type":"minecraft:model","model":"lbc:item/zaika"}}}\n')
    jar=ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar'
    with zipfile.ZipFile(jar) as archive:source=archive.read('net/minecraft/world/level/material/MapColor.java').decode()
    colors={int(i):int(rgb) for i,rgb in re.findall(r'new MapColor\((\d+), (\d+)\)',source)}
    palette=[[((colors.get(i//4,0)>>shift)&255)*[180,220,255,135][i%4]//255 for shift in (16,8,0)] for i in range(256)]
    palette_entries=',\n'.join('uvec3('+','.join(str(n)+'u' for n in row)+')' for row in palette)
    include='''#ifndef LBC_PHONE_HD_PIXELS
#define LBC_PHONE_HD_PIXELS
bool lbc_tiktok_tile(sampler2D atlas, vec2 uv, out vec2 local) {
    ivec2 size = textureSize(atlas, 0);
    ivec2 p = clamp(ivec2(floor(uv * vec2(size))), ivec2(0), size - 1);
    ivec4 tag = ivec4(round(texelFetch(atlas, p, 0) * 255.0));
    local = vec2(0);
    if (tag.r != 43 || tag.a != 211 || tag.g < 32 || tag.g > 95 || tag.b < 128 || tag.b > 191) return false;
    ivec2 origin = p - ivec2(tag.g - 32, tag.b - 128);
    if (any(lessThan(origin, ivec2(0))) || any(greaterThanEqual(origin + 63, size))) return false;
    if (any(notEqual(ivec4(round(texelFetch(atlas, origin, 0) * 255.0)), ivec4(43,32,128,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(63,0), 0) * 255.0)), ivec4(43,95,128,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(0,63), 0) * 255.0)), ivec4(43,32,191,211))) ||
        any(notEqual(ivec4(round(texelFetch(atlas, origin + ivec2(63,63), 0) * 255.0)), ivec4(43,95,191,211)))) return false;
    local = (uv * vec2(size) - vec2(origin)) / 64.0;
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
    inc=ADDON/'assets/lbc/shaders/include/phone_hd_pixels.glsl';inc.parent.mkdir(parents=True,exist_ok=True);inc.write_text(include,encoding='utf-8')
    for extension in ['vsh','fsh']:
        relative='assets/minecraft/shaders/core/item.'+extension;raw=original(relative);source=raw.decode('utf-8').replace('\r\n','\n')
        baseline=ADDON/'PHONE_BASELINE'/('item.'+extension);baseline.parent.mkdir(parents=True,exist_ok=True);baseline.write_bytes(raw)
        if extension=='vsh':
            assert source.count('layout(location = 14) flat out uvec2 tmtTintSeed;')==1
            source=source.replace('layout(location = 14) flat out uvec2 tmtTintSeed;','layout(location = 14) flat out uvec3 tmtTintSeed;')
            source=source.replace('void main() {','#include <lbc:phone_hd_pixels.glsl>\nvoid main() {',1)
            old='    tmtTintSeed = uvec2(tmtTint.r, (tmtTint.g << 8u) | tmtTint.b);';assert source.count(old)==1
            source=source.replace(old,'''    tmtTintSeed = uvec3(tmtTint.r, (tmtTint.g << 8u) | tmtTint.b, 0u);
    vec2 phoneLocal;
    if (all(lessThanEqual(tmtTint, uvec3(247u))) && lbc_tiktok_tile(Sampler0, UV0, phoneLocal)) {
        tmtTintSeed.z = 1u;
        meditationWarningFade = phoneLocal.x;
    }''')
        else:
            assert source.count('layout(location = 14) flat in uvec2 tmtTintSeed;')==1
            source=source.replace('layout(location = 14) flat in uvec2 tmtTintSeed;','layout(location = 14) flat in uvec3 tmtTintSeed;')
            source=source.replace('void main() {','''#define LBC_TIKTOK_FRAGMENT
#include <lbc:phone_hd_pixels.glsl>
void main() {
    if (tmtTintSeed.z != 0u) {
        vec3 rgb = lbc_tiktok_colour(uvec3(tmtTintSeed.x, tmtTintSeed.y >> 8u, tmtTintSeed.y & 255u), meditationWarningFade);
        vec4 phone = vec4(rgb, ColorModulator.a);
        #ifdef OIT_ALPHA_ONLY
        executeAlphaOnlyPhase(gl_FragCoord.z, phone.a);
        #else
        fragColor = calculateFinalColor(phone, true, true, true);
        #endif
        return;
    }''',1)
        destination=ADDON/relative;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_text(source,encoding='utf-8')
    write('PHONE_AUTHOR_BASE_SHA256.json',owned)
    print(json.dumps({'groups':groups,'nativePixels':WIDTH*HEIGHT,'faces':groups+1,'authorInputs':len(owned),'output':str(ADDON)}))
if __name__=='__main__':main()
