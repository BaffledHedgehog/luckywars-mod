"""Separate spectral reconstruction error from native-client dispatch jitter.

Uses the existing local PCM reference only: no account access, sound playback,
server launch or author-pack writes. Square-root periodic Hann analysis/synthesis
has an exact50% overlap-add control before any spectral truncation or timing loss.
Reference: Julius O. Smith, Spectral Audio Signal Processing, WOLA chapters.
"""
import os
for name in ('OPENBLAS_NUM_THREADS','OMP_NUM_THREADS','MKL_NUM_THREADS'):os.environ[name]='1'
import argparse,hashlib,importlib.util,json,math
from pathlib import Path
import numpy as np
import soundfile as sf

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/verification/tiktok-audio-reconstruction'
RATE=24000;SIZE=2400;HOP=SIZE//2
WINDOW=np.sqrt(np.hanning(SIZE+1)[:-1])

def frames(signal):
    padded=np.pad(signal,(HOP,SIZE))
    for begin in range(0,len(signal)+HOP,HOP):
        yield begin,np.fft.rfft(padded[begin:begin+SIZE]*WINDOW)

def reconstruct(signal,partials=None,fps=None,phase_steps=None):
    output=np.zeros(len(signal)+SIZE*2)
    frame_count=events=0;energy_kept=energy_total=0.
    for begin,spectrum in frames(signal):
        power=np.abs(spectrum)**2;energy_total+=float(power.sum())
        if partials:
            eligible=power.copy();eligible[0]=0;eligible[-1]=0
            selected=np.argpartition(eligible,-partials)[-partials:]
            keep=np.zeros(len(spectrum),bool);keep[selected]=True
            spectrum=np.where(keep,spectrum,0)
        energy_kept+=float(np.sum(abs(spectrum)**2))
        if phase_steps:
            phase=np.rint(np.angle(spectrum)*phase_steps/(2*np.pi))*2*np.pi/phase_steps
            spectrum=abs(spectrum)*np.exp(1j*phase)
        grain=np.fft.irfft(spectrum,n=SIZE)*WINDOW
        # A packet's sounds are scheduled together on the client main thread.
        scheduled=begin if fps is None else round(math.ceil(begin/RATE*fps-1e-9)/fps*RATE)
        output[scheduled:scheduled+SIZE]+=grain
        frame_count+=1;events+=int(np.count_nonzero(abs(spectrum)>1e-8))
    return output[HOP:HOP+len(signal)],{'frames':frame_count,'events':events,'spectralEnergyRetained':energy_kept/max(energy_total,1e-30)}

def quality(reference,actual):
    error=actual-reference;power=float(np.mean(reference**2))
    return {'snrDb':float(10*np.log10(max(power,1e-30)/max(float(np.mean(error**2)),1e-30))),
            'rms':float(np.sqrt(np.mean(actual**2))),'peak':float(np.max(abs(actual))),
            'maxAbsoluteError':float(np.max(abs(error)))}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--reference',type=Path,default=ROOT/'build/verification/feedback_2026_11_15/audio/original_long.wav');args=parser.parse_args()
    spec=importlib.util.spec_from_file_location('limits',ROOT/'tools/check_timestop_shaderc.py');limits=importlib.util.module_from_spec(spec);spec.loader.exec_module(limits);limits.limit_resources()
    OUT.mkdir(parents=True,exist_ok=True)
    signal,rate=sf.read(args.reference);assert rate==RATE and signal.ndim==1 and 10<len(signal)/RATE<31
    assert np.max(abs(WINDOW[:HOP]**2+WINDOW[HOP:]**2-1))<1e-12
    # A positive reconstruction control must pass on non-bin-centered tones AND
    # broadband noise before subjective tests can say anything about the codec.
    rng=np.random.default_rng(3401);time=np.arange(RATE*2)/RATE
    controls=[np.sin(2*np.pi*233.13*time)*.3,rng.normal(0,.1,len(time)),signal]
    control_errors=[]
    for control in controls:
        actual,_=reconstruct(control);q=quality(control,actual);assert q['maxAbsoluteError']<1e-12,q;control_errors.append(q)
    reports=[]
    for name,partials,fps,steps in [('spectral_48_ideal',48,None,16),('spectral_48_32fps',48,32,16),
                                   ('spectral_48_144fps',48,144,16),('spectral_48_400fps',48,400,16),
                                   ('spectral_96_ideal',96,None,16),('full_spectrum_32fps',None,32,None)]:
        actual,counts=reconstruct(signal,partials,fps,steps);q=quality(signal,actual)
        # No individual RMS normalization that hides synthesis gain errors.
        sf.write(OUT/(name+'.wav'),actual,RATE,subtype='FLOAT')
        reports.append({'file':name+'.wav','partials':partials,'fps':fps,'phaseSteps':steps,**counts,**q})
    result={'referenceSha256':hashlib.sha256(args.reference.read_bytes()).hexdigest(),'durationSeconds':len(signal)/RATE,
            'sampleRate':RATE,'blockSamples':SIZE,'hopSamples':HOP,'positiveReconstructionControls':control_errors,
            'results':reports,'scope':'Offline exact-basis diagnostic; no native asset bank/transport implementation.',
            'dspReference':'https://www.dsprelated.com/freebooks/sasp/Weighted_Overlap_Add.html'}
    (OUT/'results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))

if __name__=='__main__':main()
