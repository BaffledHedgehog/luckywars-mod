"""Bounded stereo sinusoidal fallback for an unmodified Minecraft client.

Audio sidecars leave TKP3 palette/JPEG/original PCM bytes untouched. The fixed
96ms Vorbis atoms remain shorter than100ms at every allowed semitone pitch, so
88 events every50ms use176 playback voices at ideal cadence. Client frame/audio
dispatch jitter can overlap more and is a separate limitation.
"""
from __future__ import annotations
import math
import os
from pathlib import Path
import struct

import numpy as np

RATE, SIZE, HOP, FPS, PARTIALS = 24000, 2400, 1200, 20, 88
BANK_RATE, BANK_SAMPLES, BANK_AMPLITUDE = 48000, 4608, .8
PHASES, BASES = 16, 111
HEADER = struct.Struct('>4sIII')
RECORD_BYTES = 2 + 4 * PARTIALS
FREQUENCIES = np.fft.rfftfreq(SIZE, 1 / RATE)
EDGES = np.array([20, 250, 500, 1000, 2000, 4000, 8000, 11500])
BANDS = [(FREQUENCIES >= a) & (FREQUENCIES < b) for a, b in zip(EDGES[:-1], EDGES[1:])]
WINDOW = np.sin(np.pi * np.arange(SIZE) / SIZE)
KERNEL = np.sinc(np.arange(-31, 32) / 2) * np.hamming(63)
KERNEL /= KERNEL.sum()


def select_bins(power, count):
    floors = ([4, 4, 5, 6, 8, 9, 6] if count >= 80 else
              [3, 3, 4, 5, 6, 6, 4] if count >= 50 else [1] * 7)
    chosen = []
    for band, floor in zip(BANDS, floors):
        candidates = np.flatnonzero(band)
        chosen.extend(candidates[np.argpartition(power[candidates], -floor)[-floor:]])
    score = power * np.clip(FREQUENCIES / 1000, .15, 8) ** .45
    score[(FREQUENCIES < 20) | (FREQUENCIES >= 11500)] = -1
    score[chosen] = -1
    extra = count - len(chosen)
    chosen.extend(np.argpartition(score, -extra)[-extra:])
    return np.array(chosen)


def side_budget(mid_power, side_power):
    ratio = math.sqrt(side_power / max(mid_power, 1e-30))
    return 0 if ratio < .01 else int(np.clip(round(PARTIALS * ratio / (1 + ratio)), 8, 24))


def encode_frame(stereo, side_count):
    mid = stereo.mean(axis=1)
    side = (stereo[:, 0] - stereo[:, 1]) / 2
    record = bytearray(RECORD_BYTES)
    count = 0
    for channel, signal, budget in ((0, mid, PARTIALS - side_count), (1, side, side_count)):
        if not budget:
            continue
        spectrum = np.fft.rfft(signal * WINDOW)
        for index in select_bins(abs(spectrum) ** 2, budget):
            volume = round(2 * abs(spectrum[index]) / SIZE / BANK_AMPLITUDE * 65535)
            if volume < 2:
                continue
            phase = round(np.angle(spectrum[index]) * PHASES / (2 * np.pi)) % PHASES
            key = (int(index) << 5) | (phase << 1) | channel
            struct.pack_into('>HH', record, 2 + count * 4, key, min(65535, volume))
            count += 1
    struct.pack_into('>H', record, 0, count)
    return record


def prepare_pcm(pcm, frames, destination):
    """One small working window; PCM is mapped, never a float movie in RAM."""
    if not 1 <= frames <= 6000:
        raise ValueError('spectral_frames')
    length = Path(pcm).stat().st_size
    if length % 4:
        raise ValueError('spectral_pcm')
    source = np.memmap(pcm, dtype='<i2', mode='r', shape=(length // 4, 2)) if length else np.zeros((0, 2), dtype='<i2')
    total = frames * 2400  # 48kHz source samples, duration fixed by the video.
    mid_power = side_power = 0.
    for begin in range(0, min(len(source), total), 48000):
        part = source[begin:min(begin + 48000, total)].astype(np.float64)
        mid_power += float(np.sum(((part[:, 0] + part[:, 1]) / 2) ** 2))
        side_power += float(np.sum(((part[:, 0] - part[:, 1]) / 2) ** 2))
    budget = side_budget(mid_power, side_power)
    destination = Path(destination)
    temporary = destination.with_suffix(destination.suffix + '.tmp')
    maximum = 0
    try:
        with temporary.open('wb') as out:
            out.write(HEADER.pack(b'TKA1', frames, FPS, PARTIALS))
            for frame in range(frames):
                # Circular look-ahead closes the existing clip loop. At initial
                # playback only the first50ms fade in; there is no extra AV delay.
                indices = (np.arange(SIZE * 2 + 62) + frame * 2400 - 31) % total
                raw = np.zeros((len(indices), 2), dtype=np.float64)
                valid = indices < len(source)
                raw[valid] = source[indices[valid]] / 32768
                stereo = np.stack([np.convolve(raw[:, c], KERNEL, 'valid')[::2]
                                   for c in range(2)], axis=1)
                record = encode_frame(stereo, budget)
                maximum = max(maximum, struct.unpack_from('>H', record)[0])
                out.write(record)
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)
        if isinstance(source, np.memmap):
            source._mmap.close()
    return {'format': 'TKA1', 'frames': frames, 'fps': FPS, 'maxPartials': maximum,
            'sideBudget': budget, 'bytes': destination.stat().st_size}


def migrate(directory, stage):
    """Read existing prepared PCM; never redownload/reencode/delete originals."""
    directory, stage = Path(directory), Path(stage)
    pcm = stage / 'audio.pcm'
    try:
        with (directory / 'clip.tkp').open('rb') as video, pcm.open('wb') as audio:
            header = struct.unpack('>4sIIIIII', video.read(28))
            if header[0] != b'TKP3' or header[2:] != (20, 480, 854, 48000, 48):
                raise ValueError('spectral_source')
            frames = header[1]
            if not 1 <= frames <= 6000:
                raise ValueError('spectral_frames')
            with (directory / 'clip.idx').open('rb') as index:
                offsets = index.read()
            if len(offsets) != frames * 8:
                raise ValueError('spectral_index')
            for offset, in struct.iter_unpack('>Q', offsets):
                video.seek(offset)
                compressed, jpeg = struct.unpack('>II', video.read(8))
                if not 1 <= compressed <= 480 * 854 + 1024 or not 1 <= jpeg <= 131072:
                    raise ValueError('spectral_record')
                video.seek(compressed + 144, 1)
                samples = video.read(9600)
                if len(samples) != 9600:
                    raise ValueError('spectral_truncated')
                audio.write(samples)
        return prepare_pcm(pcm, frames, stage / 'clip.tka')
    finally:
        pcm.unlink(missing_ok=True)


def bank_atom(base, phase, side):
    frequency = 20 * 2 ** (base / 12)
    n = np.arange(BANK_SAMPLES)
    wave = BANK_AMPLITUDE * np.sin(np.pi * n / BANK_SAMPLES) * np.cos(2 * np.pi * frequency * n / BANK_RATE + 2 * np.pi * phase / PHASES)
    return np.stack([wave, -wave if side else wave], axis=1)
