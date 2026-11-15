#!/usr/bin/env python3
"""Replace the applied AttributeHud TTF with Minecraft's exact bitmap glyphs.

Reads the author pack; writes only a reviewable JSON delta under this project's
resourcepack_template. Existing icon providers, padding and row offsets survive.
No TTF, PNG, custom digit patterns or resampled textures are generated.
"""
from __future__ import annotations
import argparse
import copy
import hashlib
import io
import json
from pathlib import Path
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PACK = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
JAR = ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3.jar'
OUTPUT = ROOT/'resourcepack_template/updates/attribute_hud_vanilla_2026_11_16'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def build(pack: Path, jar: Path, output: Path):
    if not output.resolve().is_relative_to((ROOT/'resourcepack_template').resolve()):
        raise ValueError('Output must stay inside the project resourcepack_template')
    with zipfile.ZipFile(jar) as archive:
        providers = json.loads(archive.read('assets/minecraft/font/include/default.json'))['providers']
        ascii_font = next(p for p in providers if p.get('file') == 'minecraft:font/ascii.png')
        spaces = json.loads(archive.read('assets/minecraft/font/include/space.json'))['providers']
        raw = archive.read('assets/minecraft/textures/font/ascii.png')
    override = pack/'assets/minecraft/textures/font/ascii.png'
    if override.exists() and override.read_bytes() != raw:
        raise ValueError('The pack overrides vanilla ascii.png; inspect it before choosing a vanilla texture reference')
    image = Image.open(io.BytesIO(raw)).convert('RGBA')
    height = ascii_font.get('height', 8)
    cell_width = image.width//len(ascii_font['chars'][0])
    cell_height = image.height//len(ascii_font['chars'])
    advances = {}; glyphs = {}
    for y, line in enumerate(ascii_font['chars']):
        for x, char in enumerate(line):
            if char == '\0':
                continue
            glyph = image.crop((x*cell_width, y*cell_height, (x+1)*cell_width, (y+1)*cell_height))
            bounds = glyph.getchannel('A').getbbox()
            advances[char] = int((bounds[2] if bounds else 0)*height/cell_height+.5)+1
            glyphs[char] = digest(glyph.tobytes())
    # Native HUD cursor calculations assume these exact vanilla advances. If the
    # target version changes, fail instead of silently breaking column alignment.
    assert all(advances[c] == 6 for c in '0123456789-+')
    assert advances['.'] == 2
    source = pack/'assets/lbc/font/attribute_hud'
    rows = sorted(source.glob('row_*.json'))
    if not rows:
        raise ValueError('No applied HUD row fonts found; no icon layout to preserve')
    before = {}; manifest = {}; details = []
    for path in rows:
        original = path.read_bytes(); before[str(path)] = digest(original)
        config = json.loads(original)
        result = copy.deepcopy(config); rewritten = []
        for provider in result['providers']:
            if provider.get('type') != 'ttf' or provider.get('file') != 'lbc:attribute_hud/glyphs.ttf':
                rewritten.append(provider)
                continue
            shift = provider.get('shift', [0, 0])
            if shift[0] != 0 or provider.get('size') != 10:
                raise ValueError(f'Unexpected applied TTF transform in {path.name}')
            native = copy.deepcopy(ascii_font)
            native['height'] = height
            native['ascent'] -= int(shift[1])
            assert native['ascent'] <= height
            rewritten.extend(copy.deepcopy(spaces))
            rewritten.append(native)
            details.append({'font': path.name, 'verticalShift': shift[1],
                            'nativeAscent': native['ascent'], 'nativeHeight': height})
        result['providers'] = rewritten
        if result == config:
            continue
        preserved = [p for p in config['providers'] if p.get('type') != 'ttf']
        assert result['providers'][:len(preserved)] == preserved, 'Icon/padding providers changed'
        relative = Path('assets/lbc/font/attribute_hud')/path.name
        target = output/relative; target.parent.mkdir(parents=True, exist_ok=True)
        encoded = (json.dumps(result, ensure_ascii=True, indent=2)+'\n').encode('utf-8')
        target.write_bytes(encoded); manifest[relative.as_posix()] = digest(encoded)
    if not manifest:
        print('Applied HUD fonts already use the native bitmap; no delta written.')
        return
    report = {'sourceJar': str(jar), 'sourceJarFontDefinition': 'assets/minecraft/font/include/default.json',
              'vanillaAsciiSha256': digest(raw), 'generatedRows': len(details), 'rows': details,
              'nativeAdvances': {c: advances[c] for c in '0123456789-+.NaInfinity'},
              'nativeGlyphPixels': {c: glyphs[c] for c in '0123456789-+.NaInfinity'},
              'iconAndPaddingProvidersUnchanged': True, 'authorSourcesUnchanged': True,
              'writesTextures': False, 'writesTtf': False, 'serverRebuildRequired': False,
              'sourceSha256': before}
    assert all(digest(Path(p).read_bytes()) == value for p, value in before.items())
    (output/'FONT_CHECKS.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    (output/'export-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ['generatedRows','iconAndPaddingProvidersUnchanged','authorSourcesUnchanged','writesTextures','writesTtf','serverRebuildRequired']}))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resourcepack-root', type=Path, default=PACK)
    parser.add_argument('--vanilla-jar', type=Path, default=JAR)
    parser.add_argument('--output', type=Path, default=OUTPUT)
    args = parser.parse_args()
    from check_timestop_shaderc import limit_resources
    limit_resources()
    build(args.resourcepack_root, args.vanilla_jar, args.output)


if __name__ == '__main__':
    main()
