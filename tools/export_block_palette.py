"""Export the active Polymer block palette for merging into a standalone RP.

Reads the generated ZIP and optional reference RP; never writes to the reference.
The resulting supplement is tied to that server's installed mod/palette set.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import zipfile


def export(pack, reference, output):
    pack, reference, output = pack.resolve(strict=True), reference.resolve(strict=True), output.resolve()
    if output == reference or reference in output.parents or output in reference.parents:
        raise ValueError('Output must be separate from the reference resource pack')
    files = {}
    with zipfile.ZipFile(pack) as archive:
        if archive.testzip() is not None:
            raise ValueError('Invalid generated resource pack')
        metadata = json.loads(archive.read('skywars2/block_palette_allocations.json'))
        files['skywars2/block_palette_allocations.json'] = archive.read('skywars2/block_palette_allocations.json')

        def include(name):
            if PurePosixPath(name).is_absolute() or '..' in PurePosixPath(name).parts:
                raise ValueError('Invalid asset path: ' + name)
            if name in files:
                return
            if name in archive.namelist():
                files[name] = archive.read(name)
            elif (reference / name).is_file() or name.startswith('assets/minecraft/'):
                return
            else:
                raise ValueError('Missing required resource: ' + name)
            if '/models/' in name and name.endswith('.json'):
                model = json.loads(files[name])
                parent = model.get('parent')
                if parent:
                    include(asset(parent, 'models', '.json'))
                for texture in model.get('textures', {}).values():
                    if isinstance(texture, str) and not texture.startswith('#'):
                        include(asset(texture, 'textures', '.png'))

        def asset(identifier, folder, suffix):
            namespace, _, path = identifier.partition(':')
            if not path:
                namespace, path = 'minecraft', namespace
            return f'assets/{namespace}/{folder}/{path}{suffix}'

        for allocation in metadata['allocations']:
            if allocation['client_block'] != 'minecraft:note_block':
                raise ValueError('Unexpected non-note palette allocation')
            include(allocation['merge_into'])
            include(asset(allocation['model'], 'models', '.json'))
    # Validate the entire export before creating or replacing anything.
    for name, data in files.items():
        target = output / name
        if target.exists() and target.read_bytes() != data:
            raise FileExistsError('Existing supplement differs; use a fresh output directory: ' + str(target))
    for name, data in files.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    manifest = {'sourceZipSha256': hashlib.sha256(pack.read_bytes()).hexdigest(),
                'allocations': len(metadata['allocations']),
                'files': {name: hashlib.sha256(data).hexdigest() for name, data in sorted(files.items())},
                'referencePackModified': False}
    (output / 'export-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(manifest))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', required=True, type=Path)
    parser.add_argument('--reference-pack', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    export(args.pack, args.reference_pack, args.output)
