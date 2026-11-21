"""Audit obsolete LuckyWars resources; applying a reviewed plan is opt-in.

Default operation reads the pack and creates a local cleanup plan. The agent must
not use --apply on the author's pack. A user can run the generated standalone
script: it rechecks hashes/references and backs up original bytes before cleanup.
"""
from __future__ import annotations
import argparse
from contextlib import ExitStack
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACK = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
VANILLA = ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3.jar'
OUTPUT = ROOT/'resourcepack_template/updates/resource_cleanup_2026_11_16'
RETIRED_CHAINS = {f'assets/lbc/post_effect/badapple/{name}.json' for name in
                  ['movie', 'control_0', 'control_1', 'control_2', 'control_0_paused', 'control_1_paused', 'control_2_paused']}
INCLUDES = re.compile(r'^\s*#(?:include|moj_import)\s*[<"]([^>"]+)[>"]', re.M)


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def contained(root, relative):
    path = (root/relative).resolve()
    if not path.is_relative_to(root.resolve()) or path == root.resolve():
        raise ValueError('Path escapes the selected pack')
    return path


def identifier(value, suffix):
    namespace, name = value.split(':', 1) if ':' in value else ('minecraft', value)
    if '..' in Path(name).parts or not re.fullmatch(r'[a-z0-9_.-]+', namespace):
        raise ValueError('Invalid resource identifier')
    return f'assets/{namespace}/shaders/{name}.{suffix}'


def audit(pack, vanilla, server, extra_jars=()):
    pack, vanilla, server = pack.resolve(), vanilla.resolve(), server.resolve()
    with zipfile.ZipFile(server) as runtime:
        movie = runtime.read('me/skywars2/game/items/gadgets/LbcBadApple.class')
        audio = runtime.read('me/skywars2/game/items/phone/TikTokAudio.class')
    movie_retired = (b'lbc:badapple/black' in movie and b'lbc:badapple/white' in movie
                     and b'control_' not in movie and b'badapple/movie' not in movie)
    v1_retired = b'tiktok.v1.' not in audio and b'tiktok/v1/' not in audio
    shaders = {p.relative_to(pack).as_posix():p for p in (pack/'assets').glob('*/shaders/**/*')
               if p.is_file() and p.suffix in {'.vsh','.fsh','.glsl','.csh','.gsh'}}
    chains = {p.relative_to(pack).as_posix():p for p in (pack/'assets').glob('*/post_effect/**/*.json')}
    retired = set(chains)&RETIRED_CHAINS if movie_retired else set()
    roots = {p for p in shaders if '/shaders/core/' in p or p.startswith('assets/sodium/')}
    for name, path in chains.items():
        if name in retired:
            continue
        for entry in json.loads(path.read_text(encoding='utf-8-sig')).get('passes', []):
            for key, suffix in [('vertex_shader','vsh'),('fragment_shader','fsh'),('compute_shader','csh')]:
                if key in entry:
                    roots.add(identifier(entry[key], suffix))
    missing = set(); reached = set()
    with ExitStack() as stack:
        archives = [stack.enter_context(zipfile.ZipFile(p)) for p in [vanilla, *extra_jars]]
        builtins = {name:archive for archive in archives for name in archive.namelist()}
        def walk(name):
            if name in reached:
                return
            reached.add(name)
            path = contained(pack, name)
            if path.exists():
                text = path.read_text(encoding='utf-8-sig')
            elif name in builtins:
                text = builtins[name].read(name).decode('utf-8-sig')
            else:
                missing.add(name); return
            for include in INCLUDES.findall(text):
                if ':' in include:
                    namespace, item = include.split(':', 1)
                    target = f'assets/{namespace}/shaders/include/{item}'
                else:
                    relative = (Path(name).parent/include).as_posix()
                    target = relative if (pack/relative).exists() or relative in builtins else 'assets/minecraft/shaders/include/'+include
                walk(target)
        for name in roots:
            walk(name)
    candidates = []
    def candidate(name, reason):
        path = contained(pack, name)
        candidates.append({'path':name, 'sha256':sha(path), 'bytes':path.stat().st_size, 'reason':reason})
    for name in sorted(set(shaders)-reached):
        if name.startswith(('assets/lbc/', 'assets/minecraft/')):
            candidate(name, 'Unreachable from all retained post chains, native core shaders and recursive includes')
    for name in sorted(retired):
        candidate(name, 'Retired Bad Apple control transport; installed server uses black/white')
    font_name = 'lbc:attribute_hud/glyphs.ttf'
    font = 'assets/lbc/font/attribute_hud/glyphs.ttf'
    font_refs = []
    for path in (pack/'assets').glob('*/font/**/*.json'):
        if font_name in path.read_text(encoding='utf-8-sig'):
            font_refs.append(path.relative_to(pack).as_posix())
    if (pack/font).is_file() and not font_refs:
        candidate(font, 'Retired handwritten TTF; no font provider references it')
    sound_path = pack/'assets/skywars2/sounds.json'
    sounds = json.loads(sound_path.read_text(encoding='utf-8-sig'))
    keys = [key for key in sounds if key.startswith('tiktok.v1.')] if v1_retired else []
    foreign_refs = []
    for path in (pack/'assets').glob('*/sounds.json'):
        definitions = json.loads(path.read_text(encoding='utf-8-sig'))
        for key, value in definitions.items():
            if path == sound_path and key in keys:
                continue
            encoded = json.dumps(value)
            if 'tiktok/v1/' in encoded or 'tiktok.v1.' in encoded:
                foreign_refs.append(path.relative_to(pack).as_posix()+':'+key)
    if foreign_refs:
        keys = []
    elif v1_retired:
        for path in sorted((pack/'assets/skywars2/sounds/tiktok/v1').glob('*.ogg')):
            candidate(path.relative_to(pack).as_posix(), 'Retired v1 bank; no installed runtime or retained sound definition reference')
    return {'packRoot':str(pack), 'vanillaJar':str(vanilla), 'serverJar':str(server), 'serverJarSha256':sha(server),
            'extraJars':[str(p.resolve()) for p in extra_jars],
            'postChains':len(chains), 'shaderFiles':len(shaders), 'missingDependencies':sorted(missing),
            'files':candidates, 'soundEvents':keys, 'soundsSha256':sha(sound_path),
            'blockedSoundReferences':foreign_refs, 'fontReferences':font_refs,
            'keptExternalShaders':sorted(p for p in shaders if p.startswith('assets/sodium/')),
            'bytesToRemove':sum(p['bytes'] for p in candidates), 'authorPackWritten':False}


def apply(plan_path):
    plan = json.loads(plan_path.read_text(encoding='utf-8'))
    pack = Path(plan['packRoot']).resolve()
    fresh = audit(pack, Path(plan['vanillaJar']), Path(plan['serverJar']), [Path(p) for p in plan.get('extraJars',[])])
    if fresh['missingDependencies']:
        raise ValueError('Dependencies changed: generate a fresh plan')
    eligible = {p['path']:p['sha256'] for p in fresh['files']}
    if any(eligible.get(p['path']) != p['sha256'] for p in plan['files']):
        raise ValueError('A file changed or gained a reference: generate a fresh plan')
    if plan['soundEvents'] and (fresh['soundsSha256'] != plan['soundsSha256'] or set(fresh['soundEvents']) != set(plan['soundEvents'])):
        raise ValueError('Sound definitions changed: generate a fresh plan')
    paths = [p['path'] for p in plan['files']]
    if plan['soundEvents']:
        paths.append('assets/skywars2/sounds.json')
    backup_dir = pack.parent/'luckywars-cleanup-backups'; backup_dir.mkdir(exist_ok=True)
    backup = backup_dir/(pack.name+'-'+time.strftime('%Y%m%d-%H%M%S')+'-'+str(os.getpid())+'.zip')
    with zipfile.ZipFile(backup, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
        for name in paths:
            archive.write(contained(pack, name), name)
    with zipfile.ZipFile(backup) as archive:
        assert archive.testzip() is None
    changed = []
    try:
        if plan['soundEvents']:
            target = contained(pack, 'assets/skywars2/sounds.json')
            sounds = json.loads(target.read_text(encoding='utf-8-sig'))
            for key in plan['soundEvents']:
                del sounds[key]
            temp = target.with_suffix('.json.cleanup-tmp')
            temp.write_text(json.dumps(sounds, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
            os.replace(temp, target); changed.append('assets/skywars2/sounds.json')
        for row in plan['files']:
            contained(pack, row['path']).unlink(); changed.append(row['path'])
    except BaseException:
        with zipfile.ZipFile(backup) as archive:
            for name in changed:
                target = contained(pack, name); target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
        raise
    print(json.dumps({'removedFiles':len(plan['files']), 'removedSoundEvents':len(plan['soundEvents']), 'backup':str(backup)}, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, default=PACK)
    parser.add_argument('--vanilla-jar', type=Path, default=VANILLA)
    parser.add_argument('--server-jar', type=Path, default=ROOT.parent/'mods/skywars2.jar')
    parser.add_argument('--output', type=Path, default=OUTPUT)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--plan', type=Path, default=Path(__file__).with_name('CLEANUP_PLAN.json'))
    args = parser.parse_args()
    if args.apply:
        apply(args.plan); return
    from check_timestop_shaderc import limit_resources
    limit_resources()
    if not args.output.resolve().is_relative_to((ROOT/'resourcepack_template').resolve()):
        raise ValueError('Audit output must stay under project resourcepack_template')
    extra_jars = sorted((args.pack.parent.parent/'mods').glob('sodium-*.jar'))
    plan = audit(args.pack, args.vanilla_jar, args.server_jar, extra_jars)
    if plan['missingDependencies']:
        raise ValueError('Missing shader dependencies: '+str(plan['missingDependencies']))
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output/'CLEANUP_PLAN.json').write_text(json.dumps(plan, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    shutil.copyfile(__file__, args.output/'cleanup_resourcepack.py')
    print(json.dumps({key:plan[key] for key in ['postChains','shaderFiles','bytesToRemove','missingDependencies','fontReferences']}, ensure_ascii=False))
    print('Planned files='+str(len(plan['files']))+' sound events='+str(len(plan['soundEvents']))+'; author pack unchanged.')


if __name__ == '__main__':
    main()
