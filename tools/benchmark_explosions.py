"""Compare synchronous native blast costs on an isolated server; never reports MSPT."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import statistics
import time
import smoke_game_cycle as cycle


CANDIDATES = {
    "moonrise": "Moonrise-Fabric-1.2.0+61c1328.jar",
    "lithium": "lithium-fabric-0.26.2+mc26.3.jar",
}


def run(server_dir: Path, optimizer: str, label: str, rounds: int, disable_cache: bool) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent or not cycle.SERVER.is_relative_to(cycle.OUT.resolve()):
        raise ValueError("Benchmark server must be isolated inside build/verification")
    cycle.require_stopped(cycle.SERVER)
    if not (cycle.SERVER / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Prepare the isolated Fabric server first")
    settings = cycle.SERVER / "config/luckywars/game.json"
    profiles = cycle.SERVER / "config/luckywars/players.json"
    controlled = [settings, profiles] + [cycle.SERVER / "mods" / f for f in CANDIDATES.values()]
    originals = {p: p.read_bytes() if p.exists() else None for p in controlled}
    old_java_options = os.environ.get("JAVA_TOOL_OPTIONS")
    active = None
    started = time.monotonic()
    warmup_rounds = 2
    result = {"optimizer": optimizer, "rounds": rounds, "warmupRounds": warmup_rounds, "explosionCache": not disable_cache,
              "metric": "synchronous instantBlast milliseconds; not MSPT"}
    try:
        os.environ["JAVA_TOOL_OPTIONS"] = (old_java_options or "") + (
            " -Dskywars2.disableExplosionCache=true" if disable_cache else " -Dskywars2.disableExplosionCache=false")
        for f in CANDIDATES.values():
            (cycle.SERVER / "mods" / f).unlink(missing_ok=True)
        if optimizer != "baseline":
            candidate = cycle.OUT / "optimizer-candidates" / CANDIDATES[optimizer]
            shutil.copy2(candidate, cycle.SERVER / "mods" / candidate.name)
            result["optimizerSha256"] = hashlib.sha256(candidate.read_bytes()).hexdigest()
        artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
        shutil.copy2(artifact, cycle.SERVER / "mods/skywars2.jar")
        result["artifactSha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
        config = json.loads(originals[settings] or b"{}")
        config.update(lives=2, health=20, gameMode=1, countdownSeconds=1,
                      stageOneSeconds=3600, stageTwoSeconds=7200, stageOneEvent=0,
                      stageTwoEvent=0, vanillaKits=False, vanillaSkills=False,
                      lbcKits=True, lbcSkills=True, mapId="1", broken=False)
        settings.parent.mkdir(parents=True, exist_ok=True)
        settings.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        for scene in ("dense", "sparse"):
            for index in range(rounds + warmup_rounds):
                # A following marker waits for the complete command, not an old duplicate PASS.
                marker = f"skywars2-benchmark-{scene}-{index}-complete"
                suite = "explosion-benchmark-air" if scene == "sparse" else "explosion-benchmark"
                cursor = active.send(f"sw check {suite}", f"say {marker}")
                active.expect(marker, cursor)
        samples = []
        pattern = re.compile(r"explosion-benchmark sample=(\d+) millis=([\d.]+) remaining=(\d+) shape=([0-9a-f]+) drops=(\d+) scene=(dense|sparse)")
        for line in active.lines:
            m = pattern.search(line)
            if m:
                samples.append(dict(sample=int(m[1]), milliseconds=float(m[2]),
                                    remaining=int(m[3]), shape=m[4], drops=int(m[5]), scene=m[6]))
        if len(samples) != (rounds + warmup_rounds) * 14:
            raise AssertionError(f"Missing measurements: {len(samples)}")
        seen = {"dense": 0, "sparse": 0}
        for sample in samples:
            sample["warmup"] = seen[sample["scene"]] < warmup_rounds * 7
            seen[sample["scene"]] += 1
        scenes = {}
        for scene in ("dense", "sparse"):
            values = [s["milliseconds"] for s in samples if s["scene"] == scene and not s["warmup"]]
            scenes[scene] = dict(medianMs=statistics.median(values), maxMs=max(values), minMs=min(values))
        result.update(samples=samples, scenes=scenes, completed=True)
        print(json.dumps({k: v for k, v in result.items() if k != "samples"}), flush=True)
    finally:
        try:
            if active:
                active.stop()
        finally:
            for p, content in originals.items():
                if content is None:
                    p.unlink(missing_ok=True)
                else:
                    p.write_bytes(content)
            if old_java_options is None:
                os.environ.pop("JAVA_TOOL_OPTIONS", None)
            else:
                os.environ["JAVA_TOOL_OPTIONS"] = old_java_options
            result["elapsedSeconds"] = round(time.monotonic() - started, 2)
            (cycle.OUT / f"{label}-results.json").write_text(
                json.dumps(result, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--optimizer", choices=("baseline", *CANDIDATES), required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--disable-cache", action="store_true", help="Disable only our ray-read cache for same-JAR A/B")
    args = parser.parse_args()
    if not re.fullmatch(r"[a-z0-9_-]+", args.label) or not 1 <= args.rounds <= 10:
        parser.error("Use a safe log label and 1..10 rounds")
    run(args.server_dir, args.optimizer, args.label, args.rounds, args.disable_cache)
