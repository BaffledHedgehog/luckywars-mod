#!/usr/bin/env python3
"""Exercise real NBT/Anvil conversion only in retained build/verification copies."""
from __future__ import annotations
from datetime import datetime, timezone
import gzip
import hashlib
import io
import json
import math
from pathlib import Path
import struct
import subprocess
import sys
from unittest.mock import patch
import uuid
import zlib
import nbtlib as n
import crop_overworld_height as crop

ROOT = Path(__file__).resolve().parents[1]


def packed(values):
    result = [0] * math.ceil(len(values) / 7)
    for i, value in enumerate(values): result[i // 7] |= value << (i % 7 * 9)
    return n.LongArray(result)


def unpack(values):
    return [(int(values[i // 7]) >> (i % 7 * 9)) & 511 for i in range(256)]


def fixture(x=0, z=0):
    heights = ([0, 1, 63, 64, 65, 127, 128, 383, 384] * 29)[:256]
    sections = []
    for y in [-4, -1, 0, 3, 4, 19]:
        states = n.Compound({'palette': n.List[n.Compound]([n.Compound({'Name': n.String('minecraft:stone')})])})
        if y in (-1, 4):
            data = [0] * 256; data[2] = 1 << 4  # Local x1/y0/z2, native 4-bit padded palette.
            states = n.Compound({'palette': n.List[n.Compound]([
                n.Compound({'Name': n.String('minecraft:air')}),
                n.Compound({'Name': n.String('minecraft:chest'), 'Properties': n.Compound({
                    'facing': n.String('north'), 'type': n.String('single'), 'waterlogged': n.String('false')})})]),
                'data': n.LongArray(data)})
        sections.append(n.Compound({'Y': n.Byte(y), 'block_states': states,
            'biomes': n.Compound({'palette': n.List[n.String]([n.String('minecraft:plains')])}),
            'BlockLight': n.ByteArray([(i % 256) - 128 for i in range(2048)])}))
    sections.append(n.Compound({'Y': n.Byte(20), 'SkyLight': n.ByteArray([-1] * 2048)}))
    def chest(y, count):
        return n.Compound({'id': n.String('minecraft:chest'), 'x': n.Int(x * 16 + 1), 'y': n.Int(y), 'z': n.Int(z * 16 + 2),
            'Items': n.List[n.Compound]([n.Compound({'Slot': n.Byte(4), 'id': n.String('minecraft:diamond'), 'count': n.Int(count),
                'components': n.Compound({'minecraft:custom_name': n.String('{"text":"positive survives"}'),
                    'minecraft:custom_data': n.Compound({'opaque': n.IntArray([1, -2, 2147483647])})})})]),
            'LootTableSeed': n.Long(1234567890123)})
    def tick(y, item): return n.Compound({'i': n.String(item), 'x': n.Int(1), 'y': n.Int(y), 'z': n.Int(2), 't': n.Int(38), 'p': n.Int(-2)})
    return n.File({'DataVersion': n.Int(4325), 'xPos': n.Int(x), 'zPos': n.Int(z), 'yPos': n.Int(-4),
        'Status': n.String('minecraft:full'), 'LastUpdate': n.Long(12345), 'InhabitedTime': n.Long(67890),
        'sections': n.List[n.Compound](sections), 'block_entities': n.List[n.Compound]([chest(-16, 7), chest(64, 13)]),
        'block_ticks': n.List[n.Compound]([tick(-1, 'minecraft:stone'), tick(64, 'minecraft:chest')]),
        'fluid_ticks': n.List[n.Compound]([tick(-64, 'minecraft:water'), tick(319, 'minecraft:lava')]),
        'Heightmaps': n.Compound({'WORLD_SURFACE': packed(heights), 'MOTION_BLOCKING': packed(list(reversed(heights)))}),
        'PostProcessing': n.List[n.List[n.Short]]([n.List[n.Short]([n.Short(i * 17), n.Short(4095)]) for i in range(24)]),
        'isLightOn': n.Byte(1), 'blending_data': n.Compound({'min_section': n.Int(-4)}),
        'below_zero_retrogen': n.Compound({'target_status': n.String('minecraft:full')}),
        'opaque_positive_metadata': n.Compound({'data': n.ByteArray([-128, 0, 127]), 'keep': n.String('unchanged')})})


def parse(raw): return n.File.parse(io.BytesIO(raw))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def raises(kind, action):
    try: action()
    except kind: return
    raise AssertionError('Expected ' + kind.__name__)


def write_region(path, entries):
    path.parent.mkdir(parents=True, exist_ok=True)
    header = bytearray(8192); records = [bytes(4096)]; sector = 3
    rx, rz = map(int, path.stem.split('.')[1:])
    for index, mode, chunk in entries:
        plain = crop.encoded(chunk); kind = mode & 127
        data = gzip.compress(plain, mtime=55) if kind == 1 else zlib.compress(plain) if kind == 2 else plain
        if mode & 128:
            (path.parent / f'c.{rx * 32 + index % 32}.{rz * 32 + index // 32}.mcc').write_bytes(data)
            record = struct.pack('>I', 1) + bytes([mode])
        else: record = struct.pack('>I', len(data) + 1) + bytes([mode]) + data
        count = math.ceil(len(record) / 4096)
        header[index * 4:index * 4 + 4] = ((sector << 8) | count).to_bytes(4, 'big')
        header[4096 + index * 4:4100 + index * 4] = (1700000000 + index).to_bytes(4, 'big')
        records.append(record + bytes(count * 4096 - len(record))); sector += count
    path.write_bytes(bytes(header) + b''.join(records))


def read_region(path, index):
    raw = path.read_bytes(); location = int.from_bytes(raw[index * 4:index * 4 + 4], 'big'); offset = location >> 8
    length = int.from_bytes(raw[offset * 4096:offset * 4096 + 4], 'big'); mode = raw[offset * 4096 + 4]
    rx, rz = map(int, path.stem.split('.')[1:])
    data = (path.parent / f'c.{rx * 32 + index % 32}.{rz * 32 + index // 32}.mcc').read_bytes() if mode & 128 else raw[offset * 4096 + 5:offset * 4096 + 4 + length]
    plain = gzip.decompress(data) if mode & 127 == 1 else zlib.decompress(data) if mode & 127 == 2 else data
    return parse(plain), mode


def check_chunk(before, after):
    assert int(after['yPos']) == 0 and all(int(s['Y']) >= 0 for s in after['sections'])
    assert crop.positive_image(before) == crop.positive_image(after)
    for key in ['block_entities', 'block_ticks', 'fluid_ticks']:
        assert crop.encoded({'v': n.List[n.Compound]([v for v in before[key] if int(v['y']) >= 0])}) == crop.encoded({'v': after[key]})
    assert int(after['block_entities'][0]['Items'][0]['count']) == 13
    assert after['PostProcessing'] == before['PostProcessing'][4:]
    assert len(after['PostProcessing']) == 20 and after['PostProcessing'][0] == before['PostProcessing'][4]
    for name in before['Heightmaps']:
        assert unpack(after['Heightmaps'][name]) == [max(0, value - 64) for value in unpack(before['Heightmaps'][name])]
    assert int(after['isLightOn']) == 0 and 'blending_data' not in after and 'below_zero_retrogen' not in after


def lock_probe(world):
    code = """import pathlib,sys
sys.path.insert(0,sys.argv[1]);import crop_overworld_height as c
try:
 with c.session_lock(pathlib.Path(sys.argv[2])):pass
except (OSError,ValueError):sys.exit(0)
sys.exit(3)
"""
    result = subprocess.run([sys.executable, '-c', code, str(ROOT / 'tools'), str(world)], capture_output=True, timeout=10)
    assert result.returncode == 0, 'A second compatible native-style process acquired the held session lock'


def check():
    output = ROOT / 'build/verification/overworld-height-crop-2026-11-27' / (datetime.now(timezone.utc).strftime('%H%M%S') + '-' + uuid.uuid4().hex[:8])
    output.mkdir(parents=True); checks = []
    before = fixture(); after = parse(crop.encoded(before)); assert crop.crop_chunk(after); check_chunk(before, after)
    first = crop.encoded(after); assert not crop.crop_chunk(after) and crop.encoded(after) == first
    (output / 'before.nbt').write_bytes(crop.encoded(before)); (output / 'after.nbt').write_bytes(first)
    checks.append('actual NBT sections/container/ticks/heightmaps/PostProcessing and idempotency')
    legacy_lights=parse(first)
    legacy_lights['sections'].append(n.Compound({'Y':n.Byte(-1),
        'SkyLight':n.ByteArray([-1]*2048),'starlight.skylight_state':n.Int(2),
        'starlight.blocklight_state':n.Int(0)}))
    light_bytes=crop.encoded(legacy_lights)
    assert not crop.crop_chunk(legacy_lights) and crop.encoded(legacy_lights)==light_bytes
    light_source=output/'starlight/source/r.0.0.mca';light_dest=output/'starlight/converted/r.0.0.mca'
    write_region(light_source,[(0,2,legacy_lights)])
    light_result=crop.convert_region(light_source,light_dest)
    light_reloaded,_=read_region(light_dest,0)
    assert light_result['chunks_changed']==0 and crop.encoded(light_reloaded)==light_bytes
    bad_light=parse(light_bytes);bad_light['sections'][-1]['biomes']=n.Compound({'palette':n.List[n.String]([n.String('minecraft:plains')])})
    raises(ValueError,lambda:crop.crop_chunk(bad_light))
    checks.append('legacy Starlight boundary lighting retained byte-identically; actual negative terrain/biomes still rejected')
    for mode in [1, 2, 3, 129, 130, 131]:
        case = output / ('compression-' + str(mode)); source = case / 'source/r.-2.3.mca'; dest = case / 'converted/r.-2.3.mca'
        chunk = fixture(-64, 96); write_region(source, [(0, mode, chunk), (1023, mode, fixture(-33, 127))])
        original = sha(source); result = crop.convert_region(source, dest)
        assert sha(source) == original and result['chunks_changed'] == 2
        assert source.read_bytes()[4096:8192] == dest.read_bytes()[4096:8192]
        for index in [0, 1023]:
            converted, actual_mode = read_region(dest, index); assert actual_mode == mode
            check_chunk(chunk if index == 0 else fixture(-33, 127), converted)
        next_file = case / 'second/r.-2.3.mca'; result2 = crop.convert_region(dest, next_file)
        assert result2['chunks_changed'] == 0 and sha(next_file) == sha(dest)
        for name, hashes in result['external_sha256'].items():
            assert sha(source.parent / name) == hashes['before_sha256'] and sha(dest.parent / name) == hashes['after_sha256']
            assert (next_file.parent / name).read_bytes() == (dest.parent / name).read_bytes()
        checks.append('MCA compression ' + str(mode) + ', negative region coords, timestamp/sidecar preservation and byte idempotency')
        raw=source.read_bytes()
        locations=[int.from_bytes(raw[i*4:i*4+4],'big') for i in range(1024)]
        last_offset=max(value>>8 for value in locations if value)
        record_end=last_offset*4096+4+int.from_bytes(raw[last_offset*4096:last_offset*4096+4],'big')
        assert record_end<len(raw)
        trimmed=case/'trimmed-source'/source.name;trimmed.parent.mkdir()
        trimmed.write_bytes(raw[:record_end]);trimmed_before=sha(trimmed)
        for companion in source.parent.glob('*.mcc'):
            (trimmed.parent/companion.name).write_bytes(companion.read_bytes())
        trimmed_output=case/'trimmed-converted'/source.name
        crop.convert_region(trimmed,trimmed_output)
        assert sha(trimmed)==trimmed_before and sha(trimmed_output)==sha(dest)
        truncated=case/'truncated-source'/source.name;truncated.parent.mkdir()
        truncated.write_bytes(raw[:record_end-1])
        for companion in source.parent.glob('*.mcc'):
            (truncated.parent/companion.name).write_bytes(companion.read_bytes())
        raises(ValueError,lambda:crop.convert_region(truncated,case/'bad-truncated'/source.name))
    checks.append('native-compatible missing last-sector padding accepted for all six encodings; genuinely truncated payloads rejected')
    bad = fixture(); bad['yPos'] = n.Int(-3); raises(ValueError, lambda: crop.crop_chunk(bad))
    bad = fixture(); bad['PostProcessing'] = n.List[n.List[n.Short]](bad['PostProcessing'][:-1]); raises(ValueError, lambda: crop.crop_chunk(bad))
    bad = fixture(); bad['yPos'] = n.Int(0); raises(ValueError, lambda: crop.crop_chunk(bad))
    raises(ValueError, lambda: crop.heightmap(n.LongArray([0])))
    raises(ValueError, lambda: crop.heightmap(packed([511] * 256)))
    source = output / 'invalid/r.0.0.mca'; write_region(source, [(0, 2, fixture())]); raw = bytearray(source.read_bytes())
    raw[4:8] = raw[:4]; source.write_bytes(raw); raises(ValueError, lambda: crop.convert_region(source, output / 'bad-overlap/r.0.0.mca'))
    write_region(source, [(0, 2, fixture())]); raw = bytearray(source.read_bytes()); raw[3 * 4096 + 4] = 4; source.write_bytes(raw)
    raises(ValueError, lambda: crop.convert_region(source, output / 'bad-compression/r.0.0.mca'))
    write_region(source, [(0, 130, fixture())]); raw = bytearray(source.read_bytes()); raw[3 * 4096:3 * 4096 + 4] = struct.pack('>I', 2); source.write_bytes(raw)
    raises(ValueError, lambda: crop.convert_region(source, output / 'bad-external-stub/r.0.0.mca'))
    checks.append('reject unknown dimensions, malformed PostProcessing/heightmaps, partial crops, overlap and unsupported LZ4')
    world = output / 'world'; region = world / 'region'; region.mkdir(parents=True); (world / 'session.lock').write_bytes(b'\xe2\x98\x83')
    write_region(region / 'r.0.0.mca', [(0, 130, fixture())]); source_hash = sha(region / 'r.0.0.mca'); external_hash = sha(region / 'c.0.0.mcc')
    real_convert = crop.convert_region; real_replace = crop.os.replace; probes = []
    def checked_convert(*args): lock_probe(world); probes.append('stage'); return real_convert(*args)
    def checked_replace(*args): lock_probe(world); probes.append('apply'); return real_replace(*args)
    stage = output / 'staged'
    with patch.object(crop, 'convert_region', checked_convert), patch.object(sys, 'argv', ['crop', '--world', str(world), '--output', str(stage)]): crop.main()
    assert sha(region / 'r.0.0.mca') == source_hash and sha(region / 'c.0.0.mcc') == external_hash
    entry = json.loads((stage / 'conversion.json').read_text())['regions'][0]
    (region / 'c.0.0.mcc').write_bytes(b'changed after staging')
    raises(RuntimeError, lambda: crop.verify_sources(region, [entry])); assert sha(region / 'r.0.0.mca') == source_hash
    # Restore this world's exact original source, then exercise backed-up atomic apply.
    write_region(region / 'r.0.0.mca', [(0, 130, fixture())])
    backup = output / 'backup'; applied = output / 'applied'
    with patch.object(crop, 'convert_region', checked_convert), patch.object(crop.os, 'replace', checked_replace), patch.object(sys, 'argv', ['crop', '--world', str(world), '--output', str(applied), '--apply', '--backup', str(backup)]): crop.main()
    assert sha(backup / 'r.0.0.mca') == source_hash and sha(backup / 'c.0.0.mcc') == external_hash
    converted, mode = read_region(region / 'r.0.0.mca', 0); check_chunk(fixture(), converted); assert mode == 130
    assert probes.count('stage') == 2 and probes.count('apply') == 2
    checks.append('held cross-process compatible session lock during stage/apply, no default source mutation, external hash preflight, exact backup and atomic apply')
    tampered = output / 'tamper-world'; (tampered / 'region').mkdir(parents=True); (tampered / 'session.lock').write_bytes(b'\xe2\x98\x83')
    write_region(tampered / 'region/r.0.0.mca', [(0, 130, fixture())])
    write_region(tampered / 'region/r.1.0.mca', [(0, 2, fixture(32, 0))])
    untouched = sha(tampered / 'region/r.0.0.mca'); replacements = []
    def tamper_after_stage(source, dest):
        result = real_convert(source, dest)
        if source.name == 'r.1.0.mca': (tampered / 'region/c.0.0.mcc').write_bytes(b'changed during staging')
        return result
    with patch.object(crop, 'convert_region', tamper_after_stage), patch.object(crop.os, 'replace', lambda *args: replacements.append(args)), patch.object(sys, 'argv', ['crop', '--world', str(tampered), '--output', str(output / 'tamper-output'), '--apply', '--backup', str(output / 'tamper-backup')]):
        raises(RuntimeError, crop.main)
    assert replacements == [] and not (output / 'tamper-backup').exists() and sha(tampered / 'region/r.0.0.mca') == untouched
    with patch.object(sys, 'argv', ['crop', '--world', str(world), '--output', str(output / 'same-path'), '--apply', '--backup', str(output / 'same-path')]):
        raises(SystemExit, crop.main)
    checks.append('all-file companion preflight aborts before first backup/replace; backup/output path alias rejected')
    report = {'passed': True, 'checks': checks, 'fixtures': str(output)}
    (output / 'checks.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2)); return output


if __name__ == '__main__': check()
