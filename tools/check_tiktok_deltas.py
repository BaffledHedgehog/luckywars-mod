"""Measure full-palette 480p deltas on a real clip and test GPU history; no client transport claims."""
import os
for key in ('OPENBLAS_NUM_THREADS','OMP_NUM_THREADS','MKL_NUM_THREADS'):os.environ[key]='1'
import argparse,hashlib,importlib.util,json,re,shutil,struct,subprocess,sys,tempfile,zipfile,zlib
from pathlib import Path
import numpy as np
import moderngl

sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/verification/tiktok-deltas';OUT.mkdir(parents=True,exist_ok=True)
WIDTH,HEIGHT,FPS=480,854,20
FRAGMENT='''#version 330
uniform sampler2D Previous;
uniform sampler2D Patch;
out vec4 color;
void main(){ivec2 p=ivec2(gl_FragCoord.xy);vec4 update=texelFetch(Patch,p,0);
color=update.a>.5?vec4(update.rgb,1):texelFetch(Previous,p,0);}
'''
VERTEX='''#version 330
void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2-1,0,1);}
'''

def read_frame(pipe):
    size=WIDTH*HEIGHT*3;data=bytearray()
    while len(data)<size:
        chunk=pipe.read(size-len(data))
        if not chunk:break
        data.extend(chunk)
    if not data:return None
    if len(data)!=size:raise RuntimeError('truncated local video frame')
    return np.frombuffer(data,dtype=np.uint8).reshape(-1,3).astype(np.uint16)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--original',type=Path,required=True);args=parser.parse_args()
    source=args.original.resolve(strict=True)
    assert source.is_file() and source.stat().st_size<=64*1024*1024
    spec=importlib.util.spec_from_file_location('tiktok_worker',ROOT/'src/main/resources/skywars2/tiktok/worker.py')
    worker=importlib.util.module_from_spec(spec);spec.loader.exec_module(worker)
    jar=next((ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*26.3-sources.jar'))
    with zipfile.ZipFile(jar) as archive:
        text=archive.read('net/minecraft/world/level/material/MapColor.java').decode()
        post=archive.read('net/minecraft/client/renderer/PostChainConfig.java').decode()
    assert 'optionalFieldOf("persistent", false)' in post
    colors={int(i):int(v) for i,v in re.findall(r'new MapColor\((\d+), (\d+)\)',text)}
    palette=np.array([[((colors.get(i//4,0)>>shift)&255)*[180,220,255,135][i%4]//255 for shift in (16,8,0)] for i in range(256)],dtype=np.uint8)
    lut=worker.palette_lut(palette)
    ctx=moderngl.create_standalone_context(require=330)
    program=ctx.program(vertex_shader=VERTEX,fragment_shader=FRAGMENT)
    quad=ctx.vertex_array(program,[])
    images=[ctx.texture((WIDTH,HEIGHT),4,dtype='f1') for _ in range(2)]
    buffers=[ctx.framebuffer([image]) for image in images]
    patch_texture=ctx.texture((WIDTH,HEIGHT),4,dtype='f1')
    for image in images+[patch_texture]:image.filter=(moderngl.NEAREST,moderngl.NEAREST)
    previous=np.zeros(WIDTH*HEIGHT,dtype=np.uint8)
    rgba=np.zeros((HEIGHT,WIDTH,4),dtype=np.uint8);rgba[:,:,3]=255;images[0].write(rgba.tobytes())
    frames=[];turn=0
    with tempfile.TemporaryDirectory(prefix='real-clip-',dir=OUT) as temporary:
        original=Path(temporary)/'original.mp4';shutil.copyfile(source,original)
        original_sha=hashlib.sha256(original.read_bytes()).hexdigest()
        command=['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-threads','1','-i',str(original),
                 '-filter_threads','1','-vf',f'fps={FPS},scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=decrease,pad={WIDTH}:{HEIGHT}:(ow-iw)/2:(oh-ih)/2:black',
                 '-t','2','-threads','1','-f','rawvideo','-pix_fmt','rgb24','pipe:1']
        process=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,
                                 creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        try:
            while (pixels:=read_frame(process.stdout)) is not None:
                packed=((pixels[:,0]>>3)<<10)|((pixels[:,1]>>3)<<5)|(pixels[:,2]>>3)
                current=lut[packed];changed=current!=previous
                # Transport-independent candidate: mask+values, or bytewise XOR, whichever compresses better.
                mask=np.packbits(changed,bitorder='little').tobytes();values=current[changed].tobytes()
                sparse=zlib.compress(mask+values,6);xor=zlib.compress(np.bitwise_xor(current,previous).tobytes(),6)
                recovered=previous.copy();raw=zlib.decompress(sparse)
                decoded=np.unpackbits(np.frombuffer(raw[:len(mask)],dtype=np.uint8),bitorder='little')[:len(current)].astype(bool)
                recovered[decoded]=np.frombuffer(raw[len(mask):],dtype=np.uint8)
                assert np.array_equal(recovered,current)
                assert np.array_equal(np.bitwise_xor(np.frombuffer(zlib.decompress(xor),dtype=np.uint8),previous),current)
                rgb=palette[current].reshape(HEIGHT,WIDTH,3)
                update=np.zeros((HEIGHT,WIDTH,4),dtype=np.uint8);update[:,:,:3]=rgb;update[:,:,3]=changed.reshape(HEIGHT,WIDTH)*255
                patch_texture.write(update.tobytes());images[turn].use(0);patch_texture.use(1)
                program['Previous'].value=0;program['Patch'].value=1;buffers[1-turn].use();ctx.viewport=(0,0,WIDTH,HEIGHT)
                quad.render(mode=moderngl.TRIANGLES,vertices=3)
                actual=np.frombuffer(buffers[1-turn].read(components=4,alignment=1),dtype=np.uint8).reshape(HEIGHT,WIDTH,4)
                assert np.array_equal(actual[:,:,:3],rgb) and np.all(actual[:,:,3]==255)
                turn=1-turn
                full=len(zlib.compress(current.tobytes(),6))
                frames.append(dict(changedPixels=int(changed.sum()),fullCompressedBytes=full,
                                   xorCompressedBytes=len(xor),sparseCompressedBytes=len(sparse),
                                   selectedBytes=min(full,len(xor),len(sparse))))
                previous=current.copy()
            assert process.wait(timeout=30)==0 and len(frames)>=2
        finally:
            process.stdout.close()
            if process.poll() is None:process.terminate();process.wait(timeout=10)
    # A genuinely empty patch must leave every previous texel intact, not clear it.
    patch_texture.write(bytes(WIDTH*HEIGHT*4));images[turn].use(0);patch_texture.use(1);buffers[1-turn].use();quad.render(mode=moderngl.TRIANGLES,vertices=3)
    assert buffers[1-turn].read(components=4,alignment=1)==buffers[turn].read(components=4,alignment=1)
    result=dict(passed=True,width=WIDTH,height=HEIGHT,fps=FPS,frames=len(frames),paletteCodes=248,
                opaquePaletteColours=244,paletteReduced=False,realOriginalSha256=original_sha,gpu=ctx.info['GL_RENDERER'],
                fullMeanBytesPerFrame=round(sum(f['fullCompressedBytes'] for f in frames)/len(frames)),
                adaptiveMeanBytesPerFrame=round(sum(f['selectedBytes'] for f in frames)/len(frames)),
                meanChangedFraction=sum(f['changedPixels'] for f in frames)/(len(frames)*WIDTH*HEIGHT),
                gpuHistoryFramesExact=len(frames),emptyPatchPreserved=True,nativePersistentTargetExists=True,
                nativeDataIngressImplemented=False,connectedMinecraftClientTested=False,
                notes='GPU history and lossless delta math only. zlib sizes are measurements, not a vanilla-client transport implementation.',samples=frames)
    (OUT/'results.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    (OUT/'history.fsh').write_text(FRAGMENT,encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='samples'}))
    quad.release();program.release();patch_texture.release()
    for obj in buffers+images:obj.release()
    ctx.release()

if __name__=='__main__':main()
