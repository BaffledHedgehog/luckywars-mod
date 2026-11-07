"""Small source atlas + procedural equipped armor. Author resource pack is read-only."""
from pathlib import Path
import hashlib,io,json,math,zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
RP=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
OUT=ROOT/'resourcepack_template/updates/tmt_equipment_2026_11_07'
JAR=ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3.jar'
def digest(data):return hashlib.sha256(data).hexdigest()
def write(name,data):
    p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_bytes(data if isinstance(data,bytes) else data.encode('utf-8'))

def build():
    originals={};sources={}
    with zipfile.ZipFile(JAR) as jar:
        for name in jar.namelist():
            if '/textures/item/' in name and name.endswith('.png'):sources[name]=jar.read(name)
        for p in sorted((RP/'assets').glob('*/textures/item/**/*.png')):
            name=p.relative_to(RP).as_posix()
            if '/tmt/' in name:continue
            sources[name]=p.read_bytes()
        unique={};entries=[]
        for name,data in sorted(sources.items()):
            # First square frame of animated sprites; no thousands of old output sprites.
            image=Image.open(io.BytesIO(data)).convert('RGBA')
            image=image.crop((0,0,image.width,min(image.height,image.width))).resize((16,16),Image.Resampling.NEAREST)
            pixels=image.tobytes();key=digest(pixels)
            if key in unique or not image.getchannel('A').getbbox():continue
            unique[key]=True;entries.append((name,image))
        side=2**math.ceil(math.log2(max(2,math.ceil(math.sqrt(len(entries)+1)))))
        atlas=Image.new('RGBA',(side*16,side*16));atlas.paste((31,197,83,255),(0,0,16,16))
        for index,(_,image) in enumerate(entries,1):atlas.paste(image,((index%side)*16,(index//side)*16))
        image_out=io.BytesIO();atlas.save(image_out,format='PNG',optimize=True)
        for layer in ['humanoid','humanoid_leggings']:
            write(f'assets/lbc/textures/entity/equipment/{layer}/tmt/procedural.png',image_out.getvalue())
        layers={kind:[{'texture':'lbc:tmt/procedural','dyeable':{'color_when_undyed':0xFA0000}}] for kind in ['humanoid','humanoid_leggings']}
        write('assets/lbc/equipment/tmt/procedural.json',json.dumps({'layers':layers},indent=2)+'\n')
        paths=['assets/minecraft/shaders/core/entity.vsh','assets/minecraft/shaders/core/entity.fsh']
        shader=[]
        for name in paths:
            p=RP/name;data=p.read_bytes() if p.is_file() else jar.read(name)
            originals[name]={'authorFile':p.is_file(),'sha256':digest(data)};shader.append(data.decode('utf-8-sig'))
    vertex,fragment=shader
    assert vertex.count('void main() {')==1 and fragment.count('vec4 rawColor = texture(Sampler0, texCoord0);')==1
    # Entity UVs cover a whole armor skin. No item-quad vertex-index convention here.
    vertex=vertex.replace('void main() {','''uniform sampler2D Sampler0;
layout(location = 8) flat out uvec2 tmtEquipmentSeed;
void main() {
    uvec3 rawTint = uvec3(round(Color.rgb * 255.0));
    bool carrier = rawTint.r == 250u && all(equal(ivec4(round(texelFetch(Sampler0, ivec2(0), 0) * 255.0)), ivec4(31,197,83,255)));
    tmtEquipmentSeed = uvec2(carrier ? 1u : 0u, (rawTint.g << 8u) | rawTint.b);
    vec4 equipmentColor = carrier ? vec4(1.0,1.0,1.0,Color.a) : Color;''')
    # Replace input tint only in the native lighting operations; preserve its alpha.
    for old,new in [('minecraft_mix_light_separate(-light, Color)','minecraft_mix_light_separate(-light, equipmentColor)'),
                    ('minecraft_mix_light_separate(light, Color)','minecraft_mix_light_separate(light, equipmentColor)'),
                    ('vertexColor = Color;','vertexColor = equipmentColor;'),
                    ('Normal, Color);','Normal, equipmentColor);')]:vertex=vertex.replace(old,new)
    fragment=fragment.replace('void main() {','''#include <lbc:tmt_equipment.glsl>
layout(location = 8) flat in uvec2 tmtEquipmentSeed;
void main() {''')
    fragment=fragment.replace('vec4 rawColor = texture(Sampler0, texCoord0);','''vec4 rawColor = texture(Sampler0, texCoord0);
    if (tmtEquipmentSeed.x != 0u) rawColor = lbc_tmt_collage(Sampler0, texCoord0, tmtEquipmentSeed.y);''')
    include=(ROOT/'resourcepack_template/updates/tmt_atlas_2026_11_06/assets/lbc/shaders/include/tmt_atlas.glsl').read_text(encoding='utf-8')
    include=include.replace('sole source is the currently bound ITEM atlas','source is the compact equipment source sheet bound').replace('LBC_TMT_ATLAS_GLSL','LBC_TMT_EQUIPMENT_GLSL').replace('float mask = max(centered.x, centered.y) < 13.0 ? 1.0 : 0.0;','float mask = 1.0;').replace('if (centered.x + centered.y > 22.0) mask = 0.0;','// Armor covers its native faces; the item silhouette mask does not apply.')
    write(paths[0],vertex);write(paths[1],fragment);write('assets/lbc/shaders/include/tmt_equipment.glsl',include)
    write('AUTHOR_BASE_SHA256.json',json.dumps(originals,indent=2)+'\n')
    write('SOURCE_ATLAS.json',json.dumps({'sourceItemTextures':len(entries),'size':[atlas.width,atlas.height],'rgbaGpuBytesPerLayer':atlas.width*atlas.height*4,'pngBytesPerLayer':len(image_out.getvalue()),'sources':[name for name,_ in entries]},indent=2)+'\n')
    write('README.md','''# Procedural TMT equipped armor

Copy this update's assets into your pack, merging entity shaders if your author
hash differs from AUTHOR_BASE_SHA256.json. The author pack was read only.
Both humanoid layers use one equipment asset lbc:tmt/procedural. The armor seed
is independently persisted by the server and transported through dyed_color
0xFA0000 | seed16. Ordinary armor, glint, light, fog and author alpha effects
keep their native branches. Native armor UVs are used, never item vertex indices.

Equipment has no access to the bound item atlas: its sole Sampler0 is its own
equipment texture. This patch therefore supplies a small source sheet of unique
vanilla and author item textures (excluding old TMT outputs), and computes the
collage in the shader. No random output textures are pregenerated. See
SOURCE_ATLAS.json for exact size and sources. Rebuild with
python tools/build_tmt_equipment_patch.py after changing the source item art.

Keep the old 707 equipment assets for saved items until they have been migrated;
new TMT items use the new asset. The item-atlas patch remains independent.
Test all four equipped slots, glint, other players, armor stands and reconnect.
GPU tests do not replace a physical Minecraft rendering test.
''')
    print(json.dumps({'uniqueSourceTextures':len(entries),'size':atlas.size,'pngBytes':len(image_out.getvalue()),'output':str(OUT)}))

if __name__=='__main__':build()
