"""Measure native tick times for 0/200/300 regular or Academy Terra charges."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir, academy=False):
    server_dir = server_dir.resolve()
    server_dir.relative_to(cycle.OUT.resolve())
    if not (server_dir / "fabric-server-launch.jar").is_file():
        raise ValueError("An existing isolated Fabric server under build/verification is required")
    cycle.require_stopped(server_dir)
    cycle.SERVER = server_dir
    config_path = server_dir / "config/luckywars/game.json"
    players_path = server_dir / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None for path in (config_path, players_path)}
    jar = server_dir / "mods/skywars2.jar"
    shutil.copyfile(cycle.PROJECT / "build/libs/skywars2-0.1.0.jar", jar)
    kind = "academy" if academy else "regular"
    prefix = "terra-academy-load" if academy else "terra-load"
    speed = 7.5 if academy else 1.8
    result = dict(artifactSha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
                  cpu=platform.processor(), logicalCpus=os.cpu_count(), variant=kind, scenarios=[],
                  conditions=f"Straight {speed}-block/tick {kind} flight, models and particles enabled; "
                             "two fake connections at launch, one forward target, no dense collisions. "
                             "Test-only loaded corridor, no real client rendering/network benchmark.")
    active = None
    started = time.monotonic()
    label = "terra-academy-benchmark" if academy else "terra-benchmark"
    try:
        config = json.loads(backups[config_path] or b"{}")
        config.update(mapId="1", gameMode=1, lives=2, countdownSeconds=1, endingSeconds=1,
                      stageOneSeconds=600, stageTwoSeconds=1200, stageOneEvent=0, stageTwoEvent=0,
                      vanillaKits=False, vanillaSkills=False, lbcKits=False, lbcSkills=False,
                      lbcLevelups=False, noBorders=True, broken=False)
        config_path.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check(f"{prefix}-prepare")
        time.sleep(3)  # Exclude fixture loading/registration from the 100-tick measurement window.
        for count in (0, 200, 300):
            active.check(f"{prefix}-{count}")
            time.sleep(8)  # Normal 20 TPS ticks, not /tick sprint.
            active.check(f"{prefix}-count-{count}")
            cursor = active.send("tick query")
            end = active.expect("Percentiles: P50:", cursor, timeout=30)
            output = "\n".join(active.lines[cursor:end + 1])
            average = re.search(r"Average time per tick: ([\d.]+)ms", output)
            percentiles = re.search(r"P50: ([\d.]+)ms P95: ([\d.]+)ms P99: ([\d.]+)ms\. Sample: (\d+)", output)
            if not average or not percentiles or int(percentiles[4]) != 100:
                raise AssertionError(f"Expected native 100-tick measurement: {output}")
            metrics = dict(zip(("p50", "p95", "p99"), map(float, percentiles.group(1, 2, 3))))
            metrics["average"] = float(average[1])
            result["scenarios"].append(dict(projectiles=count, mspt=metrics, nativeOutput=output))
            print(count, metrics, flush=True)
        active.check(f"{prefix}-cleanup")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
    finally:
        try:
            if active is not None:
                try:
                    if active.process.poll() is None:
                        active.check(f"{prefix}-cleanup")
                finally:
                    active.stop()
        finally:
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(original)
            result["elapsedSeconds"] = round(time.monotonic() - started, 2)
            (cycle.OUT / f"{label}-results.json").write_text(
                json.dumps(result, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--academy", action="store_true", help="Benchmark Academy Terra instead of regular Terra")
    args = parser.parse_args()
    run(args.server_dir, academy=args.academy)
