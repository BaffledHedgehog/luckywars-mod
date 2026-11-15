"""Generate the compact stereo tone bank into a merge-only RP addition."""
import os
for key in ('OPENBLAS_NUM_THREADS', 'OMP_NUM_THREADS', 'MKL_NUM_THREADS'):
    os.environ[key] = '1'
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import soundfile as sf
import numpy as np
from check_timestop_shaderc import limit_resources

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT/'resourcepack_template/updates/tiktok_audio_2026_11_16')
    args = parser.parse_args()
    limit_resources()
    output = args.output.resolve()
    if not output.is_relative_to((ROOT/'resourcepack_template').resolve()):
        raise ValueError('Output must be inside the project resourcepack_template')
    spec = importlib.util.spec_from_file_location('spectral', ROOT/'src/main/resources/skywars2/tiktok/spectral.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    folder = output/'assets/skywars2/sounds/tiktok/v3'
    folder.mkdir(parents=True, exist_ok=True)
    sounds = {}; manifest = {}; peak = 0.; channel_error = 0.
    for base in range(module.BASES):
        for phase in range(module.PHASES):
            for side in range(2):
                name = f'tone_{base}_{phase}_{side}'
                path = folder/(name+'.ogg')
                wave = module.bank_atom(base, phase, side)
                sf.write(path, wave, module.BANK_RATE, format='OGG', subtype='VORBIS',compression_level=0.)
                decoded, rate = sf.read(path, always_2d=True)
                assert rate == module.BANK_RATE and decoded.shape == wave.shape
                assert np.isfinite(decoded).all()
                peak = max(peak, float(abs(decoded).max()))
                channel_error = max(channel_error, float(abs(decoded[:, 0] - decoded[:, 1] * (-1 if side else 1)).max()))
                sounds['tiktok.v3.'+name] = {'sounds':[{'name':'skywars2:tiktok/v3/'+name, 'preload':True}]}
                manifest[str(path.relative_to(output)).replace('\\','/')] = hashlib.sha256(path.read_bytes()).hexdigest()
    assert peak < 1 and channel_error < 1e-4, (peak,channel_error)
    sound_json = output/'assets/skywars2/sounds.json'
    sound_json.write_text(json.dumps(sounds,separators=(',',':'))+'\n', encoding='utf-8')
    manifest[str(sound_json.relative_to(output)).replace('\\','/')] = hashlib.sha256(sound_json.read_bytes()).hexdigest()
    slowest_pitch = 2**(-1/24)
    maximum_duration = module.BANK_SAMPLES/module.BANK_RATE/slowest_pitch
    assert maximum_duration < .1
    result = {'files':len(sounds), 'sampleRate':module.BANK_RATE, 'stereoFrames':module.BANK_SAMPLES,
              'compressedBytes':sum(path.stat().st_size for path in folder.glob('*.ogg')),
              'decodedPcm16Bytes':len(sounds)*module.BANK_SAMPLES*4,
              'maxDurationSeconds':maximum_duration, 'maxOverlappingVoicesIdeal':176,
              'maxEventsPerSecond':1760, 'maximumDecodedPeak':peak,
              'maximumMidSideChannelError':channel_error,
              'authorPackWritten':False, 'soundsJsonMergeOnly':True,
              'scope':'Actual Vorbis decoding/phase/signed stereo asset checks. '
                      'Ideal cadence uses176voices; client dispatch jitter can overlap more. '
                      'Native/play tests are still required.'}
    (output/'AUDIO_BANK.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    (output/'export-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result),flush=True)


if __name__ == '__main__':
    main()
