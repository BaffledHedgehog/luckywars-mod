"""Run restored vanilla damage cooldown, bow, Blind and native lightning regressions."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir: Path) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER != (cycle.PROJECT / "build/verification/key-reuse-server").resolve():
        raise ValueError("--server-dir must name the isolated key-reuse-server directory")
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(f"Isolated server has no {launcher.name}: {cycle.SERVER}")
    properties = dict(line.split("=", 1) for line in (cycle.SERVER / "server.properties").read_text(encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Damage cooldown smoke requires the isolated server on port 25576")
    cycle.require_stopped(cycle.SERVER)

    settings_path = cycle.SERVER / "config/luckywars/game.json"
    profiles_path = cycle.SERVER / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    if not artifact.is_file():
        raise FileNotFoundError(f"Build the project artifact first: {artifact}")
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    active = None
    started = time.monotonic()
    completed = []
    passed = False
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, offhandWands=True, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("damage-cooldown")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("electric-damage-menu")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("damage-cooldown")
        completed.append("All fourteen restored vanilla damage types bypass cooldown at runtime; actual repeated native hurt calls apply full damage with ownership, while native and managed lightning reject repeats")
        completed.append("Two actual BowItem arrow impacts and two ordinary native Blind charge collisions damage the same victim in one tick without resetting timers")
        active.check("wand-physics")
        completed.append("Managed and vanilla lightning on 1000HP cows and golems match damage and invulnerability over 20 and 40 ticks; projectile physics guards remain active")
        active.check("blind-wand")
        completed.append("Blind wand payment, modes, ownership, pause, collision and input guards remain active")
        active.check("electro-gear")
        active.check("electric-damage")
        completed.append("Electrical armor, owner, ally and observer damage guards remain active")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("electric-damage-cleanup")
        active.check("blind-wand-cleanup")
        passed = True
        print("DAMAGE COOLDOWN CHECKS PASSED", flush=True)
    finally:
        try:
            if active is not None:
                active.stop()
        finally:
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(original)
            (cycle.OUT / "damage-cooldown-results.json").write_text(
                json.dumps({"passed": passed, "completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated key-reuse-server directory only")
    run(parser.parse_args().server_dir)
