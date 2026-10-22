"""Run native Phantom Knives input, hit and lifecycle checks on the isolated key-reuse server."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir: Path, stand: bool = False, time_stop: bool = False, overheaven: bool = False) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER != (cycle.PROJECT / "build/verification/key-reuse-server").resolve():
        raise ValueError("--server-dir must name the isolated key-reuse-server directory")
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(f"Isolated server has no {launcher.name}: {cycle.SERVER}")
    properties = dict(line.split("=", 1) for line in (cycle.SERVER / "server.properties").read_text(encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Phantom Knives smoke requires the isolated server on port 25576")
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

        label = "overheaven" if overheaven else "time-stop" if time_stop else "stand" if stand else "phantom-knives"
        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("loot-test-catalog")
        completed.append("Actual loot completion catalog and explicit hidden nested-table execution")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("phantom-knives-menu")
        if stand:
            active.check("stand-oraora-menu")
        if time_stop:
            active.check("time-stop-menu")
            active.check("time-stop-crafting")
        if overheaven:
            active.check("stand-overheaven-menu")
        completed.append("Native Phantom Knives identity, legacy marker, source name, lore, flags, model and tier")
        for match in (1, 2):
            if overheaven:
                active.check("stand-overheaven")
                completed.append(f"Match {match}: persistent Overheaven, five full retained matches, respawn, clock passives and native lightning field")
            if time_stop:
                active.check("time-stop")
                active.check("time-stop-schedules")
                completed.append(f"Match {match}: scoped native time stop, clocks, exact restore, all deferred hits and block/fluid scheduled delay preservation")
            if stand:
                active.check("stand-arrow")
                active.check("stand-oraora")
                completed.append(f"Match {match}: Stand Arrow one accepted hit/one return, source probability branches and ORAORA 100 fists/10s, contact damage, owner and guards")
            active.check("phantom-knives")
            completed.append(f"Match {match}: native and legacy use, source cooldown and geometry, 5/10/20/40/80 counts, native kill attribution, teammate collision, observer and offhand guards, clock slots and mixed hands")
            active.check("phantom-knives-stop-probe")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.check("phantom-knives-cleanup")
            if stand:
                active.check("stand-oraora-cleanup")
            if time_stop:
                active.check("time-stop-cleanup")
            if overheaven:
                active.check("stand-overheaven-cleanup")
            completed.append(f"Match {match}: stop clears pending waves, cooldown and native arrows; old load cannot revive them")
            if match == 1:
                active.prepare_map(1)
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
        passed = True
        print("OVERHEAVEN CHECKS PASSED" if overheaven else "TIME STOP CHECKS PASSED" if time_stop else "STAND CHECKS PASSED" if stand else "PHANTOM KNIVES CHECKS PASSED", flush=True)
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
            (cycle.OUT / ("overheaven-results.json" if overheaven else "time-stop-results.json" if time_stop else "stand-results.json" if stand else "phantom-knives-results.json")).write_text(
                json.dumps({"passed": passed, "completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated key-reuse-server directory only")
    parser.add_argument("--stand", action="store_true", help="Include Stand Arrow and ORAORA checks alongside Phantom regression")
    parser.add_argument("--time-stop", action="store_true", help="Include native clocks, scoped freeze and scheduled tick checks")
    parser.add_argument("--overheaven", action="store_true", help="Include retained Overheaven and lightning field with Stand/time-stop regression")
    args = parser.parse_args()
    run(args.server_dir, args.stand or args.time_stop or args.overheaven, args.time_stop or args.overheaven, args.overheaven)
