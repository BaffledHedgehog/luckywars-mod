"""Bounded 24-band vocoder bank: eight three-band mixtures, <=8 private sound events/frame."""
import os
for key in ('OPENBLAS_NUM_THREADS','OMP_NUM_THREADS','MKL_NUM_THREADS'):os.environ[key]='1'
import argparse,hashlib,json,subprocess,tempfile,wave
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
ADDON=ROOT/'resourcepack_template/updates/tiktok_hd_2026_11_15'
PACK=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
RATE=24000;SAMPLES=400;WINDOW=2400;BANDS=48


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--variants',type=int,default=4);args=parser.parse_args()
    assert args.variants==4
    output=ADDON/'assets/skywars2/sounds/tiktok/v2';output.mkdir(parents=True,exist_ok=True)
    original=PACK/'assets/skywars2/sounds.json'
    sounds=json.loads(original.read_text()) if original.exists() else {}
    source_sha=hashlib.sha256(original.read_bytes()).hexdigest() if original.exists() else None
    frequencies=np.fft.rfftfreq(WINDOW,1/RATE);edges=np.geomspace(90,10000,BANDS+1)
    ramp=np.ones(SAMPLES);fade=16;ramp[:fade]=np.linspace(0,1,fade);ramp[-fade:]=np.linspace(1,0,fade)
    rng=np.random.default_rng(0x54494b544f4b);bank=[]
    for variant in range(args.variants):
        channels=[]
        for band in range(BANDS):
            white=rng.normal(size=WINDOW);spectrum=np.fft.rfft(white)
            spectrum[(frequencies<edges[band])|(frequencies>=edges[band+1])]=0
            signal=np.fft.irfft(spectrum,n=WINDOW)
            signal=signal[WINDOW//2:WINDOW//2+SAMPLES]
            signal=signal/max(.00001,np.sqrt(np.mean(signal**2)))*.12*ramp;channels.append(signal)
        bank.append(channels)
    entries=0;max_peak=0.
    with tempfile.TemporaryDirectory(prefix='skywars-tiktok-audio-') as directory:
        wav=Path(directory)/'one.wav'
        for group in range(BANDS//3):
            index=0
            for a in range(5):
                for b in range(5-a):
                    c=4-a-b;assert index==a*(11-a)//2+b
                    key=f'tiktok.v2.noise_{group}_{index}';variants=[]
                    for variant in range(args.variants):
                        weights=np.array([a,b,c])/np.linalg.norm([a,b,c])
                        signal=sum(weights[n]*bank[variant][group*3+n] for n in range(3))
                        max_peak=max(max_peak,float(np.max(np.abs(signal))));assert max_peak<1
                        pcm=np.round(signal*32767).astype('<i2')
                        with wave.open(str(wav),'wb') as out:
                            out.setparams((1,2,RATE,SAMPLES,'NONE','not compressed'));out.writeframes(pcm.tobytes())
                        name=f'triad_{group}_{index}_{variant}'
                        subprocess.run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-threads','1','-i',str(wav),
                                        '-c:a','libvorbis','-q:a','3','-threads','1',str(output/(name+'.ogg'))],
                                       stdin=subprocess.DEVNULL,check=True,timeout=15,
                                       creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
                        variants.append({'name':'skywars2:tiktok/v2/'+name,'attenuation_distance':16,'preload':True})
                    sounds[key]={'sounds':variants};entries+=1;index+=1
    (ADDON/'assets/skywars2/sounds.json').write_text(json.dumps(sounds,separators=(',',':'))+'\n')
    result={'events':entries,'clips':entries*args.variants,'bands':BANDS,'groupsPerSlice':BANDS//3,'fps':60,'maxEventsPerSecond':60*(BANDS//3),
            'rate':RATE,'samplesPerClip':SAMPLES,'maxPeak':max_peak,'bytes':sum(p.stat().st_size for p in output.glob('*.ogg')),
            'authorSoundsSha256':source_sha,'originalSoundsPreserved':True}
    (ADDON/'AUDIO_BANK.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))


if __name__=='__main__':main()
