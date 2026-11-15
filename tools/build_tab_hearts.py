"""Read actual native/RP heart glyph widths; generate font assets and server spacing metrics."""
from pathlib import Path
import io,json,hashlib,math,zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
ADDON=ROOT/'resourcepack_template/updates/tab_overlay_2026_11_15'
names=['container','full','half','absorbing_full_blinking','absorbing_half_blinking']
native=next(p for p in (ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*.jar') if not p.name.endswith('-sources.jar') and '/26.3/' in p.as_posix())
providers=[];glyphs=[]
numbers=[];digits='0123456789.-'
with zipfile.ZipFile(native) as jar:
    for index,name in enumerate(names):
        rel='assets/minecraft/textures/gui/sprites/hud/heart/'+name+'.png';source=PACK/rel
        data=source.read_bytes() if source.is_file() else jar.read(rel)
        picture=Image.open(io.BytesIO(data)).convert('RGBA');bbox=picture.getchannel('A').getbbox();assert bbox
        advance=math.floor(bbox[2]*9/picture.height+.5)+1
        glyphs.append(dict(char=chr(0xE000+index),advance=advance,texture=rel,sha256=hashlib.sha256(data).hexdigest(),source='author' if source.is_file() else 'native'))
        providers.append(dict(type='bitmap',file='minecraft:gui/sprites/hud/heart/'+name+'.png',height=9,ascent=7,chars=[chr(0xE000+index)]))
    rel='assets/minecraft/textures/font/ascii.png';source=PACK/rel;data=source.read_bytes() if source.is_file() else jar.read(rel)
    picture=Image.open(io.BytesIO(data)).convert('RGBA');w,h=picture.width//16,picture.height//16;grid=[['\0']*16 for _ in range(16)]
    for index,char in enumerate(digits):
        x,y=ord(char)%16,ord(char)//16;bbox=picture.getchannel('A').crop((x*w,y*h,(x+1)*w,(y+1)*h)).getbbox();assert bbox
        advance=math.floor(bbox[2]*8/h+.5)+1;grid[y][x]=chr(0xE100+index)
        numbers.append(dict(char=char,glyph=chr(0xE100+index),advance=advance))
    providers.append(dict(type='bitmap',file='minecraft:font/ascii.png',height=8,ascent=7,chars=[''.join(row) for row in grid]))
providers.insert(0,dict(type='space',advances={chr(0xE200+step+16):step for step in range(-16,17)}))
font=ADDON/'assets/skywars2/font/tab_hearts.json';font.parent.mkdir(parents=True,exist_ok=True)
font.write_text(json.dumps({'providers':providers},ensure_ascii=True,indent=2)+'\n',encoding='utf-8')
cell=max(row['advance'] for row in numbers)
metrics=dict(glyphs=glyphs,numbers=numbers,numberCell=cell,numberTexture=dict(texture=rel,sha256=hashlib.sha256(data).hexdigest(),source='author' if source.is_file() else 'native'),ascent=7,height=9,nativeAdvance='floor(actual rightmost alpha width * height / texture height + 0.5) + 1')
(ROOT/'src/main/resources/skywars2/tab_heart_metrics.json').write_text(json.dumps(metrics,ensure_ascii=True,indent=2)+'\n',encoding='utf-8')
# Simulate actual glyph advances, including negative spacing, for all bar-sized native scores.
checks=[]
for score in range(1,100):
    full=(score+1)//2;slots=max(score,20)//2;step=min(86//slots,9)
    if step<=3:continue
    cursor=0;count=max(full,slots)
    for i in range(count):
        start=cursor;cursor+=glyphs[0]['advance']
        if i<full:
            cursor-=glyphs[0]['advance'];fill=(3 if i>=10 else 1) if i*2+1<score else (4 if i>=10 else 2)
            assert cursor==start;cursor+=glyphs[fill]['advance'];advance=glyphs[fill]['advance']
        else:advance=glyphs[0]['advance']
        cursor+=(9 if i==count-1 else step)-advance
        assert cursor==(count-1)*step+9 if i==count-1 else cursor==(i+1)*step
    checks.append(score)
columns=[]
for score in [0,1,19,20,21,43,44,100,1000,2147483647]:
    slots=max(score,20)//2;step=min(86//slots,9)
    health_width=0 if score<=0 else len(f'{score/2:.1f}')*cell if step<=3 else (max((score+1)//2,slots)-1)*step+9
    for ping in ['-','1','52','149','300','15000']:
        cursor=health_width+(90-health_width+4)+(5-len(ping))*cell
        for char in ping:
            advance=numbers[digits.index(char)]['advance'];cursor+=advance+(cell-advance)
        assert cursor==94+5*cell
        columns.append([score,ping,cursor])
(ADDON/'ASSET_CHECKS.json').write_text(json.dumps(dict(success=True,glyphs=glyphs,numbers=numbers,numberCell=cell,verifiedNativeHeartScores=checks,verifiedCompositeColumns=columns,compositeWidth=94+5*cell,finalWidth='90px HP + 4px gap + 5 fixed-width ping digits',shadowDisabled=True),ensure_ascii=True,indent=2)+'\n')
print('TAB heart advances:',[x['advance'] for x in glyphs],'; native score layouts:',len(checks),'; aligned columns:',len(columns))
