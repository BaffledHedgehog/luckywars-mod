#!/usr/bin/env python3
"""Offline, staged Anvil conversion from -64..319 to0..319. Requires nbtlib.

Default mode writes converted region files below --output without changing the
world. --apply additionally requires a backup directory and a released native
session.lock. Positive-height terrain/entity payloads are checked before writing.
"""
from __future__ import annotations
import argparse
from contextlib import contextmanager
import gzip
import hashlib
import io
import json
import math
import os
from pathlib import Path
import shutil
import struct
import zlib
import nbtlib

# Paper/Starlight keeps its lighting cache beside the vanilla boundary lights.
# These keys do not contain terrain, biomes, entities or scheduled block work.
LIGHT_SECTION_KEYS=frozenset({'Y','BlockLight','SkyLight',
                              'starlight.skylight_state','starlight.blocklight_state'})


def encoded(value):
    out=io.BytesIO();nbtlib.File(value).write(out);return out.getvalue()


def heightmap(values, old_height=384, new_height=320, shift=64):
    old_bits=math.ceil(math.log2(old_height+1));new_bits=math.ceil(math.log2(new_height+1))
    per_old=64//old_bits;per_new=64//new_bits
    if len(values)!=math.ceil(256/per_old):raise ValueError('Unexpected packed heightmap length')
    result=[0]*math.ceil(256/per_new)
    for i in range(256):
        value=(int(values[i//per_old])>>(i%per_old*old_bits))&((1<<old_bits)-1)
        if value>old_height:raise ValueError('Heightmap value exceeds the original world height')
        value=max(0,min(new_height,value-shift))
        result[i//per_new]|=value<<(i%per_new*new_bits)
    return nbtlib.LongArray([v-(1<<64) if v>=1<<63 else v for v in result])


def positive_image(chunk):
    image={k:v for k,v in chunk.items() if k not in {
        'sections','Heightmaps','yPos','PostProcessing','isLightOn','block_entities',
        'block_ticks','fluid_ticks','blending_data','below_zero_retrogen'}}
    image['sections']=nbtlib.List[nbtlib.Compound]([s for s in chunk.get('sections',[]) if int(s['Y'])>=0])
    if 'PostProcessing' in chunk:
        pending=chunk['PostProcessing']
        image['PostProcessing']=type(pending)(pending[4:] if int(chunk.get('yPos',0))==-4 else pending)
    for key in ('block_entities','block_ticks','fluid_ticks'):
        if key in chunk:image[key]=nbtlib.List[nbtlib.Compound]([v for v in chunk[key] if int(v.get('y',0))>=0])
    return encoded(image)


def crop_chunk(chunk):
    bottom=int(chunk.get('yPos',-999))
    if bottom==0:
        # Native lighting may keep a light-only section at -1 after relighting.
        if any(int(s['Y'])<0 and set(s)-LIGHT_SECTION_KEYS for s in chunk.get('sections',[])):
            raise ValueError('Already-cropped chunk still contains negative terrain')
        if any(int(v.get('y',0))<0 for key in ('block_entities','block_ticks','fluid_ticks') for v in chunk.get(key,[])):
            raise ValueError('Already-cropped chunk still contains negative positioned data')
        if 'PostProcessing' in chunk and len(chunk['PostProcessing']) not in (0,20):
            raise ValueError('Already-cropped PostProcessing section count is not20')
        return False
    if bottom!=-4:raise ValueError(f'Unexpected chunk bottom section {bottom}; refusing to guess')
    before=positive_image(chunk)
    chunk['sections']=nbtlib.List[nbtlib.Compound]([s for s in chunk.get('sections',[]) if int(s['Y'])>=0])
    chunk['yPos']=nbtlib.Int(0)
    for key in ('block_entities','block_ticks','fluid_ticks'):
        if key in chunk:chunk[key]=nbtlib.List[nbtlib.Compound]([v for v in chunk[key] if int(v.get('y',0))>=0])
    for name,values in list(chunk.get('Heightmaps',{}).items()):chunk['Heightmaps'][name]=heightmap(values)
    if 'PostProcessing' in chunk:
        pending=chunk['PostProcessing']
        if len(pending) not in (0,24):raise ValueError('Unexpected PostProcessing section count')
        if pending:chunk['PostProcessing']=type(pending)(pending[4:])
    chunk['isLightOn']=nbtlib.Byte(0)
    chunk.pop('blending_data',None);chunk.pop('below_zero_retrogen',None)
    if positive_image(chunk)!=before:raise AssertionError('Positive-height payload changed')
    return True


def convert_region(source:Path, output:Path):
    raw=source.read_bytes()
    if len(raw)<8192:raise ValueError(f'Invalid region header length: {source.name}')
    # Native RegionFile reads complete chunk streams from a partial last sector
    # and restores its padding on close. Never invent missing header/payload bytes.
    padded_length=math.ceil(len(raw)/4096)*4096
    header=bytearray(raw[:8192]);parts=[];sector=2;changed=0;external={};external_hashes={};allocated={0,1}
    rx,rz=map(int,source.stem.split('.')[1:])
    for index in range(1024):
        location=int.from_bytes(raw[index*4:index*4+4],'big');offset,count=location>>8,location&255
        if not offset:
            if count:raise ValueError('Nonempty sector count without a chunk offset')
            continue
        if offset<2 or count==0 or (offset+count)*4096>padded_length:raise ValueError('Invalid chunk allocation')
        sectors_used=set(range(offset,offset+count))
        if allocated&sectors_used:raise ValueError('Overlapping chunk allocations')
        allocated.update(sectors_used)
        if offset*4096+5>len(raw):raise ValueError('Truncated chunk header')
        length=int.from_bytes(raw[offset*4096:offset*4096+4],'big')
        if not 1<=length<=count*4096-4:raise ValueError('Invalid chunk length')
        if offset*4096+4+length>len(raw):raise ValueError('Truncated chunk payload')
        mode=raw[offset*4096+4];compression=mode&127
        if compression not in (1,2,3):raise ValueError(f'Unsupported compression {compression}; source kept')
        companion=f'c.{rx*32+index%32}.{rz*32+index//32}.mcc'
        if mode&128 and length!=1:raise ValueError('External chunk record must have length1')
        data=(source.parent/companion).read_bytes() if mode&128 else raw[offset*4096+5:offset*4096+4+length]
        external_before=hashlib.sha256(data).hexdigest() if mode&128 else None
        unpacked=gzip.decompress(data) if compression==1 else zlib.decompress(data) if compression==2 else data
        chunk=nbtlib.File.parse(io.BytesIO(unpacked))
        if crop_chunk(chunk):
            buf=io.BytesIO();chunk.write(buf);unpacked=buf.getvalue();changed+=1
            # Parse the emitted bytes again; no serializer-only success claim.
            check=nbtlib.File.parse(io.BytesIO(unpacked))
            if int(check['yPos'])!=0 or positive_image(check)!=positive_image(chunk):raise AssertionError('NBT roundtrip mismatch')
            data=gzip.compress(unpacked,mtime=0) if compression==1 else zlib.compress(unpacked) if compression==2 else unpacked
        if mode&128:
            external[companion]=data;record=struct.pack('>I',1)+bytes([mode])
            external_hashes[companion]={'before_sha256':external_before,'after_sha256':hashlib.sha256(data).hexdigest()}
        else:record=struct.pack('>I',len(data)+1)+bytes([mode])+data
        sectors=math.ceil(len(record)/4096)
        if sectors>255:raise ValueError('Chunk requires external storage; refusing implicit conversion')
        header[index*4:index*4+4]=((sector<<8)|sectors).to_bytes(4,'big')
        parts.append(record+bytes(sectors*4096-len(record)));sector+=sectors
    output.parent.mkdir(parents=True,exist_ok=True)
    for name,data in external.items():
        dest=output.parent/name
        if dest.exists() and dest.read_bytes()!=data:raise ValueError('Conflicting external chunk output')
    output.write_bytes(bytes(header)+b''.join(parts))
    for name,data in external.items():
        dest=output.parent/name
        dest.write_bytes(data)
    return {'file':source.name,'chunks_changed':changed,'before_sha256':hashlib.sha256(raw).hexdigest(),
            'after_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'external':sorted(external),
            'external_sha256':external_hashes}


@contextmanager
def session_lock(world):
    path=world/'session.lock'
    if not path.is_file():raise ValueError('Existing native session.lock is required for an offline conversion')
    with path.open('r+b') as stream:
        if os.name=='nt':
            import msvcrt
            msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
            try:yield
            finally:
                stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
        else:
            import fcntl
            # Java FileChannel.tryLock uses fcntl record locks, not the separate flock namespace.
            fcntl.lockf(stream,fcntl.LOCK_EX|fcntl.LOCK_NB)
            try:yield
            finally:fcntl.lockf(stream,fcntl.LOCK_UN)


def unlocked(world):
    with session_lock(world):pass


def verify_sources(region,entries):
    """Preflight EVERY source/companion before any backup or replacement starts."""
    for entry in entries:
        if hashlib.sha256((region/entry['file']).read_bytes()).hexdigest()!=entry['before_sha256']:
            raise RuntimeError('Source region changed after preparation')
        for name,hashes in entry['external_sha256'].items():
            if hashlib.sha256((region/name).read_bytes()).hexdigest()!=hashes['before_sha256']:
                raise RuntimeError('Source external chunk changed after preparation')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--world',type=Path,required=True);parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--apply',action='store_true');parser.add_argument('--backup',type=Path)
    args=parser.parse_args();world=args.world.resolve();out=args.output.resolve()
    if args.apply and args.backup is None:parser.error('--apply requires --backup')
    if out==world or out.is_relative_to(world):parser.error('Output must be outside the source world')
    backup=args.backup.resolve() if args.backup is not None else None
    if backup is not None and (backup==world or backup.is_relative_to(world)):
        parser.error('Backup must be outside the source world')
    if backup is not None and (backup==out or backup.is_relative_to(out) or out.is_relative_to(backup)):
        parser.error('Backup and output directories must be separate trees')
    # The compatible native lock remains held during staging, preflight, backup and apply.
    with session_lock(world):
        modern=world/'dimensions/minecraft/overworld/region';region=modern if modern.is_dir() else world/'region'
        if not region.is_dir():parser.error('No Overworld region directory')
        if out.exists() and any(out.iterdir()):parser.error('Output must be empty')
        if args.apply and backup.exists() and any(backup.iterdir()):parser.error('Backup directory must be empty')
        out.mkdir(parents=True,exist_ok=True);entries=[]
        if args.apply and out.stat().st_dev!=region.stat().st_dev:
            parser.error('Output must share the region filesystem for atomic replacement')
        for source in sorted(region.glob('r.*.*.mca')):entries.append(convert_region(source,out/source.name))
        verify_sources(region,entries)
        report={'world':str(world),'region':str(region),'regions':entries,'applied':False}
        (out/'conversion.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        if args.apply:
            backup.mkdir(parents=True,exist_ok=True)
            for entry in entries:
                source=region/entry['file'];shutil.copy2(source,backup/source.name)
                for name in entry['external']:shutil.copy2(region/name,backup/name)
            # Also catches an external non-server writer during backup before the first replace.
            verify_sources(region,entries)
            for entry in entries:
                for name in entry['external']:os.replace(out/name,region/name)
                os.replace(out/entry['file'],region/entry['file'])
            report['applied']=True;(out/'conversion.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'regions':len(entries),'chunks_changed':sum(e['chunks_changed'] for e in entries),'applied':report['applied']}))


if __name__=='__main__':main()
