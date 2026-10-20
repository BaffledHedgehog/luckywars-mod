"""Run native Infinite Bolt and native bolt-material checks on an isolated server copy."""
from pathlib import Path
import argparse
import hashlib
import io
import json
import shutil
import time
import zipfile

import smoke_game_cycle as cycle



def enable_empty_roll_probe(active, ready_marker: str) -> None:
    """Wait for a function that only exists in the newly published resource manager."""
    cursor = active.send('datapack enable "file/codex_infbolt_empty_roll_probe.zip" last')
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        active.send("function codex_probe:infbolt_ready")
        with active.condition:
            for line in active.lines[cursor:]:
                if ready_marker in line:
                    return
                if "Failed to execute reload" in line or "[check] FAIL" in line:
                    raise AssertionError(line)
            if active.process.poll() is not None:
                raise AssertionError("Server exited while enabling the empty-roll probe")
            active.condition.wait(min(0.5, max(0, deadline - time.monotonic())))
    raise AssertionError(f"Empty-roll probe resources were not published: {active.lines[-8:]}")


def run(server_dir: Path) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("--server-dir must name an isolated server directory")
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(f"Isolated server has no {launcher.name}: {cycle.SERVER}")
    cycle.require_stopped(cycle.SERVER)

    properties = dict(line.split("=", 1) for line in (cycle.SERVER / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    world = (cycle.SERVER / properties.get("level-name", "world")).resolve()
    world.relative_to(cycle.SERVER)
    empty_pack = world / "datapacks/codex_infbolt_empty_roll_probe.zip"
    if empty_pack.exists():
        raise FileExistsError(f"Reserved temporary smoke datapack already exists: {empty_pack}")
    settings_path = cycle.SERVER / "config/luckywars/game.json"
    profiles_path = cycle.SERVER / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path, empty_pack)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    if not artifact.is_file():
        raise FileNotFoundError(f"Build the released project artifact before running smoke checks: {artifact}")
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    active = None
    started = time.monotonic()
    completed = []
    try:
        # Override one real category with guaranteed empty loot, without modifying source resources.
        pack_metadata = json.loads((cycle.PROJECT / "src/main/resources/data/pack.mcmeta").read_text(encoding="utf-8"))
        pack_metadata["pack"]["description"] = "Temporary Infinite Bolt empty-result integration probe"
        ready_marker = f"CODEX_INFBOLT_EMPTY_PACK_READY_{time.time_ns()}"
        pack_bytes = io.BytesIO()
        with zipfile.ZipFile(pack_bytes, "w") as archive:
            archive.writestr("pack.mcmeta", json.dumps(pack_metadata))
            archive.writestr("data/lbc/loot_table/transformables_pickaxe.json", json.dumps({"pools": []}))
            archive.writestr("data/codex_probe/function/infbolt_ready.mcfunction", f"say {ready_marker}\n")
        empty_pack.parent.mkdir(parents=True, exist_ok=True)
        empty_pack.write_bytes(pack_bytes.getvalue())
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("infbolt")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        # A prior run persists this pack in level.dat's disabled list. Explicit enable is required.
        # "Enabling data pack" is emitted before reload; the unique function proves publication.
        enable_empty_roll_probe(active, ready_marker)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("infbolt-menu")
        completed.append("Original Infinite Bolt and native lightning egg source payload and Polymer presentation")
        active.check("infbolt-egg")
        completed.append("Native spawn egg consumes one; owned electrical lightning harms allies and protects NPC, ordinary wand lightning still protects allies")
        active.check("infbolt-crafting")
        completed.append("Native and legacy Teleport/bolt material pairs, exact 2x2/3x3 consumption and extra rejection")
        active.check("infbolt-empty")
        completed.append("Real empty loot preserves complete normal and ultra reforge inputs and finishes queued jobs")
        active.check("infbolt")
        completed.append("Free/offhand/observer/time-stop, full-stack queue, no Academy upgrade, unsupported input preservation and conditional64-arrow consumption")
        active.check("infbolt-stop-probe")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("infbolt-cleanup")
        active.prepare_map(1)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("infbolt")
        completed.append("Infbolt runtime checks resume in the second match")
        active.check("infbolt-stop-probe")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("infbolt-cleanup")
        print("INFBOLT CHECKS PASSED", flush=True)
    finally:
        try:
            if active is not None:
                try:
                    if active.process.poll() is None:
                        active.send('datapack disable "file/codex_infbolt_empty_roll_probe.zip"')
                finally:
                    active.stop()
        finally:
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(original)
            (cycle.OUT / "infbolt-results.json").write_text(
                json.dumps({"completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    run(parser.parse_args().server_dir)
