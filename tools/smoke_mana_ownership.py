"""Run mana potion ownership against a real Carpet fake player on an isolated server."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir: Path) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("--server-dir must name an isolated server directory")
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(f"Isolated server has no {launcher.name}: {cycle.SERVER}")
    cycle.require_stopped(cycle.SERVER)

    settings_path = cycle.SERVER / "config/luckywars/game.json"
    profiles_path = cycle.SERVER / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    active = None
    started = time.monotonic()
    completed = []
    label = "mana-carpet-kill"
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("player ManaVictim spawn")
        active.expect("ManaVictim[local] logged in", cursor, timeout=90)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor, timeout=120)
        check_cursor = active.check("mana-carpet-kill")
        # Carpet disconnects the fake player after the native death path; its reason
        # should identify the death, while the server console records the global chat.
        active.expect("ManaVictim lost connection:", check_cursor, timeout=30)
        death_lines = [line for line in active.lines[check_cursor:]
                       if "System chat:" in line
                       and ("death.attack." in line or ("ManaVictim" in line
                            and "joined the game" not in line and "left the game" not in line))]
        if not death_lines:
            raise AssertionError("No native global death message was recorded for ManaVictim")
        if any("ManaVictim lost connection: Killed" in line for line in active.lines[check_cursor:]):
            raise AssertionError("Carpet used its direct Killed disconnect instead of the native death path")
        completed.append("Actual mana splash kill of a Carpet fake player credits its thrower, awards normal kill rewards and emits native death output")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=120)
        active.stop()
        active = None
        print("MANA CARPET KILL CHECK PASSED", flush=True)
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
