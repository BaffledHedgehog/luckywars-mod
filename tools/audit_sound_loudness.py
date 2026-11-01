"""Read-only R128/peak audit of a resource pack against local vanilla sound assets."""
import argparse
import concurrent.futures
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess
import contextlib
import tempfile
import wave

import numpy as np
import soundfile as sf


REFERENCE_EVENTS = [
    "block.stone.break", "block.grass.step", "block.wood.step", "entity.player.attack.strong",
    "entity.player.attack.weak", "entity.player.attack.crit", "entity.player.hurt", "entity.zombie.hurt",
    "entity.skeleton.hurt", "entity.arrow.shoot", "entity.arrow.hit", "entity.generic.explode",
    "block.fire.ambient", "entity.experience_orb.pickup", "block.chest.open", "block.chest.close",
    "entity.item.pickup", "ui.button.click", "entity.enderman.teleport", "entity.lightning_bolt.thunder",
    "block.anvil.land", "item.shield.block", "entity.cow.ambient", "entity.player.burp",
    "block.glass.break", "entity.splash_potion.break", "entity.bat.takeoff", "item.trident.throw",
    "entity.ender_pearl.throw", "block.note_block.pling", "block.enchantment_table.use", "block.beacon.power_select",
]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


@contextlib.contextmanager
def client_pcm(path):
    """26.3 ChunkedSampleByteBuf quantizes/clamps before OpenAL applies playback gain."""
    folder = Path(__file__).resolve().parents[1] / "build/verification/sound-normalization-2026-11-01/pcm-temp"
    folder.mkdir(parents=True, exist_ok=True)
    descriptor, name = tempfile.mkstemp(suffix=".wav", prefix="native-", dir=folder)
    import os
    os.close(descriptor)
    temporary = Path(name)
    try:
        with sf.SoundFile(str(path)) as source, wave.open(str(temporary), "wb") as target:
            target.setnchannels(source.channels)
            target.setsampwidth(2)
            target.setframerate(source.samplerate)
            for block in source.blocks(blocksize=65536, dtype="float32", always_2d=True):
                quantized = np.clip(np.trunc(block * np.float32(32767.5) - np.float32(.5)), -32768, 32767).astype("<i2")
                target.writeframesraw(quantized.tobytes())
        yield temporary
    finally:
        temporary.unlink(missing_ok=True)


def measure_pcm(path):
    info = sf.info(str(path))
    energy = peak = 0.0
    count = 0
    with sf.SoundFile(str(path)) as stream:
        for block in stream.blocks(blocksize=65536, dtype="float32", always_2d=True):
            energy += float(np.sum(block.astype(np.float64) ** 2))
            peak = max(peak, float(np.max(np.abs(block))))
            count += block.size
    result = subprocess.run([
        "ffmpeg", "-hide_banner", "-nostats", "-threads", "1", "-filter_threads", "1",
        "-i", str(path), "-af", "loudnorm=I=-23:TP=-1:LRA=11:print_format=json", "-f", "null", "-",
    ], capture_output=True, text=True, encoding="utf-8", errors="replace", check=True)
    blocks = re.findall(r'\{\s*"input_i"[\s\S]*?\}', result.stderr)
    if not blocks:
        raise ValueError(f"No EBU R128 result: {path}")
    loudness = json.loads(blocks[-1])

    def finite(value):
        value = float(value)
        return value if math.isfinite(value) else None

    return dict(seconds=info.duration, channels=info.channels,
                sampleRate=info.samplerate, rmsDbfs=20 * math.log10(math.sqrt(energy / count)) if energy else None,
                samplePeakDbfs=20 * math.log10(peak) if peak else None,
                integratedLufs=finite(loudness["input_i"]), truePeakDbtp=finite(loudness["input_tp"]),
                loudnessRangeLu=finite(loudness["input_lra"]))


def measure(path):
    with client_pcm(path) as native:
        result = measure_pcm(native)
    result["sha256"] = digest(path)
    result["measurementPath"] = "26.3 signed16 client quantizer, then EBU R128"
    return result


def audit(pack, assets, index_path, output):
    if output.resolve().is_relative_to(pack.resolve()):
        raise ValueError("The author's source resource pack is read-only")
    index = json.loads(index_path.read_text(encoding="utf-8"))["objects"]

    def vanilla_file(key):
        sha = index[key]["hash"]
        return assets / "objects" / sha[:2] / sha

    vanilla = json.loads(vanilla_file("minecraft/sounds.json").read_text(encoding="utf-8"))

    def resolve(name, seen=frozenset()):
        name = name.removeprefix("minecraft:")
        if name in seen:
            raise ValueError("Cyclic vanilla sound event")
        sounds = []
        for entry in vanilla[name]["sounds"]:
            entry = {"name": entry} if isinstance(entry, str) else entry
            if entry.get("type") == "event":
                sounds.extend(resolve(entry["name"], seen | {name}))
            else:
                key = "minecraft/sounds/" + entry["name"].removeprefix("minecraft:") + ".ogg"
                if key in index and vanilla_file(key).exists():
                    sounds.append(key)
        return sounds

    references = {}
    jobs = {}
    for event in REFERENCE_EVENTS:
        if event not in vanilla:
            continue
        keys = sorted(set(resolve(event)))[:2]
        references[event] = keys
        for key in keys:
            jobs[vanilla_file(key)] = "vanilla:" + key
    events = {}
    definitions = {}
    for definition in sorted((pack / "assets").glob("*/sounds.json")):
        namespace = definition.parent.name
        definitions[definition.relative_to(pack).as_posix()] = digest(definition)
        for name, event in json.loads(definition.read_text(encoding="utf-8-sig")).items():
            files = []
            for entry in event["sounds"]:
                entry = {"name": entry} if isinstance(entry, str) else entry
                if entry.get("type") == "event":
                    raise ValueError("Custom nested event requires explicit resolution")
                value = entry["name"]
                ns, relative = value.split(":", 1) if ":" in value else (namespace, value)
                path = pack / "assets" / ns / "sounds" / (relative + ".ogg")
                if not path.exists():
                    raise FileNotFoundError(path)
                files.append(path.relative_to(pack).as_posix())
                jobs[path] = "custom:" + files[-1]
            events[namespace + ":" + name] = dict(definition=event, files=files)
    measured = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        futures = {executor.submit(measure, path): (path, key) for path, key in jobs.items()}
        for future in concurrent.futures.as_completed(futures):
            path, key = futures[future]
            measured[key] = future.result()
            print(f"Measured {len(measured)}/{len(jobs)}: {path.name}", flush=True)
    # Equal event weighting avoids a family with many tiny variants determining the target alone.
    levels, rms = [], []
    for event, keys in references.items():
        values = [measured["vanilla:" + key] for key in keys]
        finite = [v["integratedLufs"] for v in values if v["integratedLufs"] is not None]
        if finite:
            levels.append(float(np.median(finite)))
        finite_rms = [v["rmsDbfs"] for v in values if v["rmsDbfs"] is not None]
        if finite_rms:
            rms.append(float(np.median(finite_rms)))
    report = dict(pack=str(pack), assetIndex=str(index_path), definitionHashes=definitions,
                  measurementPath="26.3 ChunkedSampleByteBuf signed16 conversion before playback gain",
                  referenceEvents=references, targetIntegratedLufs=float(np.median(levels)),
                  shortSoundRmsCeilingDbfs=float(np.median(rms)), truePeakCeilingDbtp=-1.0,
                  events=events, measured=measured)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    print(json.dumps({k:report[k] for k in ["targetIntegratedLufs", "shortSoundRmsCeilingDbfs"]}), flush=True)


def write_server_profiles(report_path, output):
    """Regenerate server gain limits, never sound assets or client resource definitions."""
    report = json.loads(report_path.read_text(encoding="utf-8"))
    pack = Path(report["pack"])
    if output.resolve().is_relative_to(pack.resolve()):
        raise ValueError("The author's source resource pack is read-only")
    entries = {}
    for event, definition in sorted(report["events"].items()):
        if len(definition["files"]) != 1:
            raise ValueError("Mixed sound variants require per-variant gain analysis")
        for sound in definition["definition"]["sounds"]:
            if isinstance(sound, dict) and (sound.get("volume", 1) != 1 or sound.get("attenuation_distance", 16) != 16):
                raise ValueError("Playback metadata changed; review radius/gain projection")
        source = report["measured"]["custom:" + definition["files"][0]]
        if digest(pack / definition["files"][0]) != source["sha256"]:
            raise ValueError("Source sound changed since the measured audit")
        level, target = source["integratedLufs"], report["targetIntegratedLufs"]
        if level is None:
            level, target = source["rmsDbfs"], report["shortSoundRmsCeilingDbfs"]
        gain_db = min(0, target-level if level is not None else 0,
                      report["truePeakCeilingDbtp"]-source["truePeakDbtp"] if source["truePeakDbtp"] is not None else 0)
        if event == "minecraft:lbcsounds.metal_pipe":
            gain_db = 0
        entries[event] = dict(maxVolume=10 ** (gain_db / 20), channels=source["channels"],
                              integratedLufs=source["integratedLufs"], truePeakDbtp=source["truePeakDbtp"], sourceSha256=source["sha256"])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(dict(referenceIntegratedLufs=report["targetIntegratedLufs"],
                                     shortSoundRmsCeilingDbfs=report["shortSoundRmsCeilingDbfs"],
                                     measurement="26.3-style signed16 conversion before playback gain; EBU R128; representative vanilla-event median",
                                     events=entries), indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    parser.add_argument("assets", type=Path)
    parser.add_argument("index", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--server-profiles", type=Path)
    args = parser.parse_args()
    audit(args.pack.resolve(), args.assets.resolve(), args.index.resolve(), args.output.resolve())
    if args.server_profiles:
        write_server_profiles(args.output.resolve(), args.server_profiles.resolve())
