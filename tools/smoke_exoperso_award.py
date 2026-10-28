"""Verify the Exoperso native advancement from two player kills in one real burst.

Runs only against a stopped, isolated Fabric server. Carpet supplies a third
real fake player and drives the wand through its native item-use path.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


ADVANCEMENT = "lbc:true_advancements/staffs/exoperso"


def await_probe(server, command: str, marker: str, timeout: float = 8) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        cursor = server.send(command)
        with server.condition:
            while time.monotonic() < deadline:
                if any(marker in line for line in server.lines[cursor:]):
                    return
                if server.process.poll() is not None:
                    raise AssertionError("Server exited before the Exoperso advancement could be checked")
                server.condition.wait(min(.25, deadline - time.monotonic()))
                # Reissue the conditional probe after a short quiet window.
                if not any(marker in line for line in server.lines[cursor:]):
                    break
    raise AssertionError(f"Probe {marker} did not pass; last output: {server.lines[-15:]}")


def run(server_dir: Path) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("--server-dir must name an isolated server directory")
    if not (cycle.SERVER / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError(f"Isolated server has no fabric-server-launch.jar: {cycle.SERVER}")
    cycle.require_stopped(cycle.SERVER)

    settings_path = cycle.SERVER / "config/luckywars/game.json"
    profiles_path = cycle.SERVER / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    if not artifact.is_file():
        raise FileNotFoundError(f"Build the native mod first: {artifact}")
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()

    active = None
    forced_fixture = False
    started = time.monotonic()
    completed = []
    label = "exoperso-award"
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=1, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=False, lbcSkills=False,
                        lbcLevelups=False, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        gamma_cursor = active.send("player TestGamma spawn")
        active.expect("TestGamma[local] logged in", gamma_cursor, timeout=90)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor, timeout=180)

        # Use a fixed arena coordinate; /data cannot query player NBT. The full
        # fixture remains comfortably inside the game's +/-500 horizontal guard.
        x, z = 300, 300
        active.send(f"execute in minecraft:overworld run forceload add {x - 2} {z - 3} {x + 34} {z + 3}")
        forced_fixture = True
        await_probe(active, f"execute in minecraft:overworld if loaded {x - 2} 259 {z - 3} "
                    f"if loaded {x + 34} 259 {z + 3} run say EXOPERSO_FIXTURE_LOADED",
                    "EXOPERSO_FIXTURE_LOADED", timeout=30)
        commands = [
            f"execute in minecraft:overworld run fill {x - 2} 259 {z - 3} {x + 34} 259 {z + 3} minecraft:stone",
            f"execute in minecraft:overworld run fill {x + 30} 260 {z - 1} {x + 30} 264 {z + 1} minecraft:stone",
            f"execute in minecraft:overworld run tp TestAlpha {x + .5} 260 {z + .5}",
            f"execute in minecraft:overworld run tp TestBeta {x + 29.5} 260 {z - 2.5}",
            f"execute in minecraft:overworld run tp TestGamma {x + 29.5} 260 {z + 2.5}",
            "clear TestAlpha",
            "clear TestBeta",
            "clear TestGamma",
            f"advancement revoke TestAlpha only {ADVANCEMENT}",
            "loot replace entity TestAlpha weapon.mainhand loot lbc:ported/exoperso_wand",
            "scoreboard players set TestAlpha manamax 100000",
            "scoreboard players set TestAlpha manaregen 0",
            "scoreboard players set TestAlpha mana 30000",
            "player TestAlpha look east",
            "player TestAlpha use once",
        ]
        cast_cursor = active.send(*commands)
        active.expect("TestAlpha", cast_cursor, timeout=20)
        # The ordinary charge captures aim at tick 319, launches at 321, and
        # reaches the wall at x+30 after the native flight starts.
        active.expect("-> ENDING", cast_cursor, timeout=90)
        await_probe(active,
                    f'execute if entity @a[name=TestAlpha,advancements={{{ADVANCEMENT}=true}}] run say EXOPERSO_AWARD_OK',
                    "EXOPERSO_AWARD_OK", timeout=8)
        completed.append("One native Exoperso wall-collision series killed both other enrolled players and granted the real advancement")

        active.expect("LOADING -> LOBBY", cast_cursor, timeout=90)
        active.prepare_map(1)
        active.send(f"execute in minecraft:overworld run forceload remove {x - 2} {z - 3} {x + 34} {z + 3}")
        forced_fixture = False
        active.stop()
        active = None
        print("EXOPERSO NATIVE AWARD CHECK PASSED", flush=True)
    finally:
        try:
            if active is not None:
                if forced_fixture:
                    active.send(f"execute in minecraft:overworld run forceload remove {x - 2} {z - 3} {x + 34} {z + 3}")
                try:
                    cursor = active.send("sw stop")
                    active.expect("LOADING -> LOBBY", cursor, timeout=30)
                    active.prepare_map(1)
                except Exception:
                    pass
                active.stop()
        finally:
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(original)
            (cycle.OUT / f"{label}-results.json").write_text(
                json.dumps({"completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    args = parser.parse_args()
    run(args.server_dir)
