"""Compare equal-budget stereo spectral selectors on real speech/music.

Offline diagnostics only. No server, resource-pack, playback or account access.
The hybrid uses fixed band-limited noise atoms, not the original residual PCM.
All variants keep the reference gain and the same 88-event ceiling per frame.
"""
import os
for _name in ('OPENBLAS_NUM_THREADS', 'OMP_NUM_THREADS', 'MKL_NUM_THREADS'):
    os.environ[_name] = '1'

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import soundfile as sf
import importlib.util

import check_tiktok_audio_reconstruction as codec

_spec = importlib.util.spec_from_file_location('skywars_spectral', codec.ROOT / 'src/main/resources/skywars2/tiktok/spectral.py')
_spectral = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_spectral)

EDGES = np.array([20, 250, 500, 1000, 2000, 4000, 8000, 11500])
FREQUENCIES = np.fft.rfftfreq(codec.SIZE, 1 / codec.RATE)
BANDS = [(FREQUENCIES >= low) & (FREQUENCIES < high)
         for low, high in zip(EDGES[:-1], EDGES[1:])]


def select_bins(power, count, balanced):
    eligible = power.copy()
    eligible[(FREQUENCIES < 20) | (FREQUENCIES >= 11500)] = 0
    if not balanced:
        return np.argpartition(eligible, -count)[-count:]
    return _spectral.select_bins(power, count)


def noise_atom(low, high, seed):
    # A bank of these fixed atoms is feasible; no source phase is hidden here.
    rng = np.random.default_rng(seed)
    spectrum = np.zeros(len(FREQUENCIES), complex)
    band = (FREQUENCIES >= low) & (FREQUENCIES < high)
    spectrum[band] = np.exp(2j * np.pi * rng.random(np.count_nonzero(band)))
    return band, spectrum


def reconstruct(signal, count, balanced, noise_bands=()):
    output = np.zeros(len(signal) + codec.SIZE * 2)
    original_power = np.zeros(len(BANDS))
    selected_power = np.zeros(len(BANDS))
    events = 0
    atoms = [[noise_atom(low, high, 80000 + band_index * 16 + variant)
              for variant in range(16)]
             for band_index, (low, high) in enumerate(noise_bands)]
    for frame_index, (begin, source) in enumerate(codec.frames(signal)):
        power = abs(source) ** 2
        chosen = select_bins(power, count, balanced)
        spectrum = np.zeros_like(source)
        phases = np.rint(np.angle(source[chosen]) * 16 / (2 * np.pi)) * (2 * np.pi / 16)
        spectrum[chosen] = abs(source[chosen]) * np.exp(1j * phases)
        events += int(np.count_nonzero(abs(spectrum) > 1e-8))
        for band_index, variants in enumerate(atoms):
            band, atom = variants[frame_index % len(variants)]
            residual = np.maximum(0, power - abs(spectrum) ** 2)
            gain = np.sqrt(residual[band].sum() / max(np.sum(abs(atom) ** 2), 1e-30))
            spectrum += gain * atom
            events += int(gain > 1e-8)
        for index, band in enumerate(BANDS):
            original_power[index] += power[band].sum()
            selected_power[index] += np.sum(abs(spectrum[band]) ** 2)
        grain = np.fft.irfft(spectrum, n=codec.SIZE) * codec.WINDOW
        output[begin:begin + codec.SIZE] += grain
    return output[codec.HOP:codec.HOP + len(signal)], {
        'events': events,
        'bandEnergyRatios': (selected_power / np.maximum(original_power, 1e-30)).tolist(),
    }


def compare(reference, destination):
    stereo, rate = sf.read(reference, always_2d=True)
    assert rate == codec.RATE and stereo.shape[1] == 2
    assert 10 <= len(stereo) / rate <= 30
    assert np.isfinite(stereo).all()
    mid = stereo.mean(axis=1)
    side = (stereo[:, 0] - stereo[:, 1]) / 2
    relative_side_rms = np.sqrt(np.sum(side ** 2) / max(np.sum(mid ** 2), 1e-30))
    side_budget = _spectral.side_budget(float(np.sum(mid ** 2)), float(np.sum(side ** 2)))
    mid_budget = 88 - side_budget
    destination.mkdir(parents=True, exist_ok=True)
    sf.write(destination / 'original.wav', stereo, rate, subtype='FLOAT')
    # The baseline is the exact previous selector, including DC and sub-70Hz,
    # so that a subjective comparison does not silently change its definition.
    left, lm = codec.reconstruct(mid, 64, None, 16)
    right, rm = codec.reconstruct(side, 24, None, 16)
    baseline = np.stack([left + right, left - right], axis=1)
    sf.write(destination / 'previous_88.wav', baseline, rate, subtype='FLOAT')
    reports = []
    noise_mid = (
         ((2000, 3000), (3000, 4500), (4500, 6000), (6000, 8000),
          (8000, 10000), (10000, 11500)) if side_budget else
         ((1500, 2200), (2200, 3000), (3000, 4000), (4000, 5000),
          (5000, 6500), (6500, 8000), (8000, 10000), (10000, 11500)))
    noise_side = ((2000, 5000), (5000, 11500)) if side_budget else ()
    variants = [
        ('balanced_88', mid_budget, side_budget, (), ()),
        ('balanced_noise_88', mid_budget - len(noise_mid), side_budget - len(noise_side),
         noise_mid, noise_side),
    ]
    for name, mid_count, side_count, mid_noise, side_noise in variants:
        a, ma = reconstruct(mid, mid_count, True, mid_noise)
        if side_count:
            b, mb = reconstruct(side, side_count, True, side_noise)
        else:
            b = np.zeros_like(a)
            mb = {'events': 0, 'bandEnergyRatios': [0.] * len(BANDS)}
        decoded = np.stack([a + b, a - b], axis=1)
        assert np.isfinite(decoded).all()
        # No separate normalization/compressor/treble boost hides the tradeoff.
        sf.write(destination / (name + '.wav'), decoded, rate, subtype='FLOAT')
        reports.append({'file': name + '.wav', 'midPartials': mid_count,
                        'sidePartials': side_count, 'noiseAtoms': len(mid_noise) + len(side_noise),
                        'eventCeilingPerSecond': 88 * rate / codec.HOP,
                        'maxOverlappingVoicesIdeal': 176,
                        'mid': ma, 'side': mb, **codec.quality(stereo, decoded)})
    result = {'referenceSha256': hashlib.sha256(reference.read_bytes()).hexdigest(),
              'durationSeconds': len(stereo) / rate, 'sampleRate': rate,
              'sourceStereoCorrelation': float(np.corrcoef(stereo.T)[0, 1]),
              'relativeSideRms': float(relative_side_rms),
              'bandsHz': EDGES.tolist(), 'baseline': codec.quality(stereo, baseline),
              'variants': reports,
              'scope': 'Ideal offline timing only; no native bank/transport implementation. '
                       'Noise approximates spectral energy, not the missing waveform.'}
    (destination / 'results.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'directory': str(destination), 'duration': result['durationSeconds'],
                      'variants': [{'file': r['file'], 'peak': r['peak'], 'snrDb': r['snrDb']}
                                   for r in reports]}), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    from check_timestop_shaderc import limit_resources
    limit_resources()
    compare(args.reference, args.output)


if __name__ == '__main__':
    main()
