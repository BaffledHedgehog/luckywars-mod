"""Run native /tick query against controlled AcidProjectile populations.

Requires an existing isolated server under build/verification. This tool does
not build, copy jars, or touch the main/legacy server.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import platform
import re
import secrets
import shutil
import socket
import time
import smoke_game_cycle as cycle

ARENA = "minecraft:overworld"
ACID = "skywars2:acid_projectile"
TAG = "skywars2.perf"
TARGET_TAG = "skywars2.perf_target"
COUNTS = (0, 100, 200, 300)


def set_property(text, key, value):
    lines, prefix, found = text.splitlines(), key + "=", False
    out = []
    for line in lines:
        if line.startswith(prefix):
            out.append(prefix + str(value))
            found = True
        else:
            out.append(line)
    if not found:
        out.append(prefix + str(value))
    return "\n".join(out) + "\n"


def properties(text):
    return {k.strip(): v.strip() for line in text.splitlines()
            if line and not line.startswith(("#", "!")) and "=" in line
            for k, v in [line.split("=", 1)]}


def free_loopback_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        if hasattr(socket, "SO_EXCLUSIVEADDRUSE"):
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def positions(kind, count):
    for i in range(count):
        if kind == "sparse":
            yield (i % 20) * 5 - 47.5, 280, (i // 20) * 5 - 35
        else:
            yield (i % 12) * .8 - 4.4, 279 + (i // 144) * .5, ((i // 12) % 12) * .8 - 4.4


def make_pack(pack, namespace, targets):
    functions = pack / "data" / namespace / "function"
    functions.mkdir(parents=True)
    (pack / "pack.mcmeta").write_text(json.dumps({"pack": {
        "min_format": 121, "max_format": 121,
        "description": "Temporary projectile benchmark probes"
    }}) + "\n", encoding="utf-8")
    objective = "perf_" + namespace.rsplit("_", 1)[-1]
    target_spawns = "".join(
        f'execute in {ARENA} run summon minecraft:cow {(i % 4) * 1.2 - 1.8:.2f} 277.10 {(i // 4) * 1.2 - 4.2:.2f} '
        f'{{NoAI:1b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:["{TARGET_TAG}"]}}\n'
        for i in range(targets))
    (functions / "prepare.mcfunction").write_text(
        f"scoreboard objectives add {objective} dummy\n"
        f"execute in {ARENA} run forceload add -64 -64 64 64\n"
        + target_spawns +
        f"say BENCH_READY {namespace}\n", encoding="utf-8")
    (functions / "cleanup.mcfunction").write_text(
        f"execute in {ARENA} run kill @e[type={ACID},tag={TAG}]\n"
        f"execute in {ARENA} run kill @e[tag={TARGET_TAG}]\n"
        f"execute in {ARENA} run forceload remove -64 -64 64 64\n"
        f"scoreboard objectives remove {objective}\n"
        f"say BENCH_CLEAN {namespace}\n", encoding="utf-8")
    for layout in ("sparse", "dense"):
        for count in COUNTS:
            label = f"{layout}_{count}"
            spawn = [f"execute in {ARENA} run kill @e[type={ACID},tag={TAG}]"]
            if targets:
                spawn.append(f"execute in {ARENA} run effect clear @e[tag={TARGET_TAG}]")
            tags = json.dumps([TAG, "skywars2.acid", "magic", "raycastacid"], separators=(",", ":"))
            for x, y, z in positions(layout, count):
                spawn.append(f"execute in {ARENA} run summon {ACID} {x:.3f} {y:.3f} {z:.3f} "
                             f"{{NoGravity:1b,Motion:[0.03d,0.0d,0.0d],Tags:{tags}}}")
            spawn += [
                f"scoreboard players set #actual {objective} 0",
                f"execute in {ARENA} as @e[type={ACID},tag={TAG}] run scoreboard players add #actual {objective} 1",
                f"execute unless score #actual {objective} matches {count} run say BENCH_COUNT_ERROR {label}",
                f"schedule function {namespace}:measure_{label} 120t",
                f"say BENCH_SPAWNED {label} {count}",
            ]
            (functions / f"spawn_{label}.mcfunction").write_text("\n".join(spawn) + "\n", encoding="utf-8")
            measure = [
                f"scoreboard players set #actual {objective} 0",
                f"execute in {ARENA} as @e[type={ACID},tag={TAG}] run scoreboard players add #actual {objective} 1",
                f"execute unless score #actual {objective} matches {count} run say BENCH_COUNT_ERROR {label}",
                f"execute if score #actual {objective} matches {count} run say BENCH_COUNT_OK {label} {count}",
                f"say BENCH_TICK_QUERY_BEGIN {label}",
                f"say BENCH_TICK_QUERY_END {label}",
            ]
            if targets and count and layout == "dense":
                selector = f'@e[type=minecraft:cow,tag={TARGET_TAG},nbt={{active_effects:[{{id:"minecraft:poison"}}]}}]'
                measure.insert(0, f"execute in {ARENA} unless entity {selector} run say BENCH_TARGET_ERROR {label}")
                measure.insert(0, f"execute in {ARENA} if entity {selector} run say BENCH_TARGET_OK {label}")
            (functions / f"measure_{label}.mcfunction").write_text("\n".join(measure) + "\n", encoding="utf-8")


def run(server_dir, targets=0):
    server_dir = server_dir.resolve()
    allowed = cycle.OUT.resolve()
    try:
        server_dir.relative_to(allowed)
    except ValueError as error:
        raise ValueError(f"--server-dir must be an isolated child of {allowed}") from error
    if server_dir == allowed or not server_dir.is_dir():
        raise ValueError("Refusing the verification root; the isolated server must already exist")
    jar = server_dir / "mods" / "skywars2.jar"
    if not (server_dir / "fabric-server-launch.jar").is_file() or not jar.is_file():
        raise FileNotFoundError("Prepare the isolated launcher and baseline mod jar first; no copying/building is done")
    props_path = server_dir / "server.properties"
    config_path = server_dir / "config" / "luckywars" / "game.json"
    if not props_path.is_file() or not config_path.is_file():
        raise FileNotFoundError("Isolated server.properties and game.json are required")
    lock = allowed / ".projectile-benchmark.lock"
    try:
        lock_fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError as error:
        raise RuntimeError("Another projectile benchmark is already running") from error

    original_props = original_config = original_players = None
    players_path = server_dir / "config" / "luckywars" / "players.json"
    mutated = False
    pack = None
    active = None
    result = {"serverDir": str(server_dir), "jarSha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
              "tickQuery": "native /tick query after 120 server ticks per scenario",
              "targets": targets, "trajectory": "NoGravity, initial velocity (0.03,0,0), native drag",
              "clients": "two fake ServerPlayer connections, no real network clients",
              "cpu": {"platform": platform.platform(), "processor": platform.processor(),
                      "logicalCpus": os.cpu_count()}, "scenarios": []}
    try:
        original_props, original_config = props_path.read_bytes(), config_path.read_bytes()
        original_players = players_path.read_bytes() if players_path.exists() else None
        props = properties(original_props.decode("utf-8"))
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
            probe.settimeout(1)
            if probe.connect_ex(("127.0.0.1", int(props.get("server-port", 25565)))) == 0:
                raise RuntimeError("Stop the isolated server before benchmarking its world")
        world = (server_dir / props.get("level-name", "world")).resolve()
        world.relative_to(server_dir)
        datapacks = world / "datapacks"
        datapacks.mkdir(parents=True, exist_ok=True)
        namespace = "skywars2_perf_" + secrets.token_hex(3)
        pack = datapacks / namespace
        new_props = set_property(original_props.decode("utf-8"), "server-port", free_loopback_port())
        new_props = set_property(new_props, "server-ip", "127.0.0.1")
        new_props = set_property(new_props, "online-mode", "false")
        config = json.loads(original_config)
        config.update(mapId="1", countdownSeconds=1, stageOneSeconds=600, stageTwoSeconds=1200,
                      endingSeconds=1, vanillaKits=False, vanillaSkills=False,
                      lbcKits=False, lbcSkills=False, broken=False)
        mutated = True
        props_path.write_text(new_props, encoding="utf-8")
        config_path.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        result["serverProperties"] = {k: props.get(k) for k in ("view-distance", "simulation-distance")}
        make_pack(pack, namespace, targets)
        cycle.SERVER = server_dir
        label_prefix = "projectile-benchmark-targets" if targets else "projectile-benchmark"
        active = cycle.Server(label_prefix)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor, timeout=120)
        active.send("gamemode creative TestAlpha", "gamemode creative TestBeta",
                    f"execute in {ARENA} run tp TestAlpha 0 281 50",
                    f"execute in {ARENA} run tp TestBeta 4 281 50",
                    f"function {namespace}:prepare")
        active.expect(f"BENCH_READY {namespace}", cursor, timeout=30)
        for layout in (("dense",) if targets else ("sparse", "dense")):
            for count in COUNTS:
                label = f"{layout}_{count}"
                cursor = active.send(f"function {namespace}:spawn_{label}")
                active.expect(f"BENCH_SPAWNED {label} {count}", cursor, timeout=60)
                query_cursor = active.expect(f"BENCH_TICK_QUERY_BEGIN {label}", cursor, timeout=180)
                query_end = active.expect(f"BENCH_TICK_QUERY_END {label}", query_cursor, timeout=30)
                setup_output = active.lines[cursor:query_end + 1]
                if any("BENCH_COUNT_ERROR" in line or "BENCH_TARGET_ERROR" in line for line in setup_output):
                    raise AssertionError(f"Projectile population changed in {label}")
                if not any(f"BENCH_COUNT_OK {label} {count}" in line for line in setup_output):
                    raise AssertionError(f"Missing population validation in {label}")
                if targets and count and not any(f"BENCH_TARGET_OK {label}" in line for line in setup_output):
                    raise AssertionError(f"Acid aura did not poison a live target in {label}")
                # Function sources suppress command feedback. Query from the console only after
                # the scheduled marker proves 120 real ticks elapsed (the ring holds 100 ticks).
                query_cursor = active.send("tick query")
                query_end = active.expect("Percentiles: P50:", query_cursor, timeout=30)
                window = active.lines[query_cursor:query_end + 1]
                rendered = "\n".join(window)
                average = re.search(r"Average time per tick: ([\d.]+)ms", rendered)
                percentiles = re.search(r"P50: ([\d.]+)ms P95: ([\d.]+)ms P99: ([\d.]+)ms\. Sample: (\d+)", rendered)
                if not average or not percentiles or int(percentiles[4]) != 100:
                    raise AssertionError(f"Missing native 100-tick measurements: {rendered}")
                metrics = dict(zip(("p50", "p95", "p99"), map(float, percentiles.group(1, 2, 3))))
                metrics["average"] = float(average[1])
                result["scenarios"].append({"name": label, "layout": layout, "entityCount": count,
                                            "mspt": metrics, "nativeTickQueryOutput": window})
                print(label, metrics, flush=True)
        cursor = active.send(f"function {namespace}:cleanup")
        active.expect(f"BENCH_CLEAN {namespace}", cursor, timeout=30)
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=120)
        active.stop()
        active = None
    finally:
        if active is not None:
            try:
                active.send(f"function {namespace}:cleanup")
            finally:
                active.stop()
        if mutated and original_props is not None:
            props_path.write_bytes(original_props)
        if mutated and original_config is not None:
            config_path.write_bytes(original_config)
        if mutated:
            if original_players is not None:
                players_path.write_bytes(original_players)
            else:
                players_path.unlink(missing_ok=True)
        if pack is not None and pack.exists():
            pack.resolve().relative_to(allowed)
            shutil.rmtree(pack)
        os.close(lock_fd)
        lock.unlink(missing_ok=True)
        label_prefix = "projectile-benchmark-targets" if targets else "projectile-benchmark"
        result["rawLog"] = str(cycle.OUT / (label_prefix + ".log"))
        log = Path(result["rawLog"])
        result["rawLogSha256"] = hashlib.sha256(log.read_bytes()).hexdigest() if log.exists() else None
        (cycle.OUT / (label_prefix + "-results.json")).write_text(json.dumps(result, indent=2) + "\n",
                                                                    encoding="utf-8")
    print(f"Projectile benchmark complete: {cycle.OUT / (label_prefix + '-results.json')}", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Existing isolated instance inside build/verification")
    parser.add_argument("--targets", type=int, choices=(0, 32), default=0,
                        help="Add 32 persistent living targets below a dense cloud and verify actual poison")
    args = parser.parse_args()
    run(args.server_dir, args.targets)
