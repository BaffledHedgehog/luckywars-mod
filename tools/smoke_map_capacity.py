"""Catalog capacity, native menu selection and16/24-player quick start qualification in two actual mode1 matches on the isolated native fixture.

Only this runner's child server may be started/stopped. A timeout never triggers a restart
or forced kill. Bytes are restored only after a clean child exit and released world lock.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import sys
import time
import zipfile
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parent))
import smoke_game_cycle as cycle


SUITES = ("map-capacity-match", "chests")
PROBES = ()
CLEANUP = ()


class NativeServer(cycle.Server):
    def stop(self):
        # Do not use the base helper's kill-on-shutdown-timeout fallback.
        if self.process.poll() is None:
            self.send("stop")
            self.process.wait(timeout=180)
        if self.process.returncode != 0:
            raise RuntimeError(f"Fixture child exited abnormally: {self.process.returncode}")
        self.log.close()


def next_server_tick(server):
    def game_time():
        cursor = server.send("time query gametime")
        line = server.expect("The game time is ", cursor, timeout=10)
        return int(re.search(r"The game time is (\d+)", server.lines[line]).group(1))
    previous = game_time()
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        time.sleep(.025)
        if game_time() != previous:
            return
    raise AssertionError("Native game clock did not advance between independent fixtures")


def run(server_dir: Path, focus=None, keep_going=False, artifact=None):
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Only build/verification/key-reuse-server is authorized")
    cycle.SERVER = allowed
    props = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if props.get("server-port") != "25576":
        raise ValueError("The isolated fixture must use port25576")
    world = (allowed / props.get("level-name", "world")).resolve()
    world.relative_to(allowed)
    if not (allowed / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Isolated Fabric launcher is absent")
    candidate = (artifact or cycle.PROJECT / "build/libs/skywars2-0.1.0.jar").resolve()
    candidate.relative_to((cycle.PROJECT / "build").resolve())
    if not candidate.is_file():
        raise FileNotFoundError(candidate)
    planned = tuple(focus) if focus else SUITES
    needs_classic = bool(set(planned) & {"classic-melee", "devil-sword", "jump-artifacts", "traveller", "popit", "ordinary-accessories", "inventory-hazards", "passive-consumables", "pending-combat", "pending-swords", "incoming-artifacts", "reactive-artifacts", "fragment-knowledge", "moisei-boots", "regeneration-belt", "quiver", "dimension-items"})
    # Reject typo/unregistered commands before modifying or launching the fixture.
    dispatcher = (cycle.PROJECT / "src/main/java/me/skywars2/game/GameChecks.java").read_text(encoding="utf-8")
    registered = set(re.findall(r'case\s+"([^"]+)"', dispatcher))
    required = set(planned) | {"bots", "map-capacity", "map-capacity-menu", "map-capacity-crowd", "map-capacity-crowd-24", "map-capacity-cleanup"}
    if set(planned) & {"late-swords", "late-swords-stop-probe"}:
        required.update(("late-swords-prepare", "late-swords-preparation-cleanup"))
    if needs_classic:
        required.update(("classic-melee-menu", "classic-melee-lobby-cleanup"))
    if not focus:
        required.update(PROBES + CLEANUP)
    if missing := sorted(required - registered):
        raise ValueError(f"Native suites are not registered: {missing}")

    paths = (allowed / "codex-native-fixture.lock", allowed / "codex-map-capacity.lock")
    descriptors = []
    try:
        for path in paths:
            descriptor = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            descriptors.append((path, descriptor))
            os.write(descriptor, str(os.getpid()).encode("ascii"))
    except BaseException:
        for path, descriptor in reversed(descriptors):
            os.close(descriptor)
            path.unlink(missing_ok=True)
        raise

    active = None
    backups = {}
    completed, preparations, setups, failures, completed_matches = [], [], [], [], []
    classic_setup = False
    match_active = False
    cleanup_errors = []
    source_sha = installed_sha = None
    recovery_archive = None
    success = False
    normal_stop = False
    restored = False
    started = time.monotonic()
    keep_going = keep_going or not focus

    def check(match, suite):
        next_server_tick(active)
        cursor = active.send("sw check " + suite)
        try:
            acknowledgement = "map-capacity-crowd" if suite == "map-capacity-crowd-24" else suite
            line = active.expect("[check] PASS " + acknowledgement, cursor, timeout=180)
            if suite == "map-capacity-crowd-24" and "players=24" not in active.lines[line]:
                raise AssertionError("Native24-player setup acknowledged a different crowd size")
        except AssertionError as error:
            failures.append({"match": match, "suite": suite, "error": str(error)})
            # An observed native FAIL permits independent fixtures to continue. An
            # observation timeout or exited server is fatal; never restart to erase it.
            if not keep_going or "[check] FAIL" not in str(error):
                raise
            return False
        record = {"match": match, "suite": suite}
        target = setups if suite in ("classic-melee-menu", "classic-melee-lobby-cleanup") else preparations if suite in ("late-swords-prepare", "late-swords-preparation-cleanup") else completed
        target.append(record)
        return True

    def prepared_check(match, suite):
        # Warm tickets in their own command; check() observes a REAL native tick
        # before the next command. No source/manual world ticking or combat retries.
        try:
            if not check(match, "late-swords-prepare"):
                return False
            return check(match, suite)
        finally:
            check(match, "late-swords-preparation-cleanup")

    try:
        cycle.require_stopped(allowed)
        with socket.socket() as probe:
            if probe.connect_ex(("127.0.0.1", 25576)) == 0:
                raise RuntimeError("Isolated fixture port25576 is already accepting connections")
        settings_path = allowed / "config/luckywars/game.json"
        profiles_path = allowed / "config/luckywars/players.json"
        installed = allowed / "mods/skywars2.jar"
        player_paths = []
        for name in ("TestAlpha", "TestBeta", *(f"TestMap{i:02}" for i in range(3,25))):
            raw = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
            raw[6] = (raw[6] & 0x0f) | 0x30
            raw[8] = (raw[8] & 0x3f) | 0x80
            identity = str(uuid.UUID(bytes=bytes(raw)))
            player_paths.extend((world / "playerdata" / (identity + ".dat"), world / "playerdata" / (identity + ".dat_old"),
                                 world / "stats" / (identity + ".json"), world / "advancements" / (identity + ".json")))
        for path in (settings_path, profiles_path, world / "data/scoreboard.dat",
                     world / "data/scoreboard.dat_old", installed, *player_paths):
            backups[path] = path.read_bytes() if path.exists() else None
        # Durable recovery survives a Python interruption or a child shutdown timeout.
        # Successful byte restoration removes this temporary old-JAR copy.
        recovery_archive = cycle.OUT / f"map-capacity-recovery-{time.time_ns()}-{os.getpid()}.zip"
        with zipfile.ZipFile(recovery_archive, "x", compression=zipfile.ZIP_STORED) as archive:
            manifest = []
            for path, original in backups.items():
                name = path.relative_to(allowed).as_posix()
                manifest.append({"path": name, "existed": original is not None,
                                 "sha256": hashlib.sha256(original).hexdigest() if original is not None else None})
                if original is not None:
                    archive.writestr("bytes/" + name, original)
            archive.writestr("manifest.json", json.dumps(manifest, indent=2) + "\n")
        source_sha = hashlib.sha256(candidate.read_bytes()).hexdigest()
        # The world-lock and port checks above are mandatory before this only JAR copy.
        installed.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(candidate, installed)
        installed_sha = hashlib.sha256(installed.read_bytes()).hexdigest()
        if installed_sha != source_sha:
            raise AssertionError("Installed candidate differs from the exact selected artifact")
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=3, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=1200, stageTwoSeconds=1800, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=False, lbcSkills=False,
                        lbcLevelups=False, lbcLevelupLimit=4, mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = NativeServer("map-capacity-gameplay")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "map-capacity", "map-capacity-menu"):
            if not check(0, suite):
                raise AssertionError("Required native menu/capacity setup failed")
        active.expect("Selected map ready: 54", timeout=180)
        active.prepare_map(23)
        if needs_classic:
            classic_setup = True
            if not check(0, "classic-melee-menu"):
                raise AssertionError("Required scoped native third participant setup failed")
        for match in ((1,) if focus else (1, 2)):
            cursor = active.send("sw map 1" + (" broken" if match == 2 else ""))
            active.expect("Selected map ready: 1 broken=" + ("true" if match == 2 else "false"), cursor, timeout=180)
            check(match, "map-capacity-crowd" if match == 1 else "map-capacity-crowd-24")
            check(match, "map-capacity")
            cursor = active.send("sw quick")
            active.expect("-> PLAYING", cursor, timeout=180)
            match_active = True
            for suite in planned:
                # These are disposable native fixture items, never production players.
                for entity_type in ("minecraft:item", "minecraft:experience_orb"):
                    active.send(f"execute in minecraft:overworld run kill @e[type={entity_type}]")
                (prepared_check if suite in ("late-swords", "late-swords-stop-probe") else check)(match, suite)
            if not focus:
                for suite in PROBES:
                    check(match, suite)
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor, timeout=180)
            match_active = False
            if not focus:
                for suite in CLEANUP:
                    check(match, suite)
            check(match, "map-capacity-cleanup")
            completed_matches.append(match)
        if classic_setup:
            check(0, "classic-melee-lobby-cleanup")
            classic_setup = False
        if failures:
            raise AssertionError(f"Qualification rejected: {len(failures)} recorded native suite failure(s)")
        success = True
    finally:
        original_error = sys.exc_info()[1]
        if original_error is not None and not failures:
            failures.append({"suite": "runner", "error": str(original_error)})
        try:
            if active is not None:
                if classic_setup and not match_active and active.process.poll() is None:
                    check(0, "classic-melee-lobby-cleanup")
                    classic_setup = False
                active.stop()
            normal_stop = True
            cycle.require_stopped(allowed)
            # Each exact byte snapshot is restored only after normal owned-child shutdown.
            for path, original in backups.items():
                try:
                    if original is None:
                        path.unlink(missing_ok=True)
                    else:
                        path.parent.mkdir(parents=True, exist_ok=True)
                        path.write_bytes(original)
                    if (path.read_bytes() if path.exists() else None) != original:
                        raise AssertionError(f"Restored bytes differ: {path}")
                except BaseException as error:
                    cleanup_errors.append(str(error))
            restored = not cleanup_errors
            if restored and recovery_archive is not None:
                recovery_archive.unlink()
        except BaseException as error:
            cleanup_errors.append(str(error))
        live_child = active is not None and active.process.poll() is None
        for path, descriptor in reversed(descriptors):
            try:
                os.close(descriptor)
                # Retain ownership markers if a shutdown timed out with our child alive.
                if not live_child:
                    path.unlink(missing_ok=True)
            except BaseException as error:
                cleanup_errors.append(str(error))
        result = {
            "success": success and normal_stop and restored and not cleanup_errors,
            "qualified": success and not focus and completed_matches == [1, 2]
                         and not preparations and not setups
                         and normal_stop and restored and not cleanup_errors,
            "scope": "native-mode1-map-capacity",
            "completedMatches": completed_matches, "completed": completed,
            "preparations": preparations, "setups": setups,
            "crowdContract": "Fourteen/twenty-two additional real vanilla players created only in the ready isolated lobby. All24 UUID playerdata/stats/advancement files are snapshotted and restored only after normal owned-child shutdown; normal crowd disconnect follows each actual match.",
            "preparationContract": "No terrain injection or source/manual world ticking; catalog/menu and actual16/24-player normal/broken quick-start matches use native map loading.",
            "checkFailures": failures, "cleanupErrors": cleanup_errors,
            "artifactSha256": installed_sha, "sourceArtifactSha256": source_sha,
            "candidateArtifact": str(candidate), "serverDirectory": str(allowed), "serverPort": 25576,
            "normalChildShutdown": normal_stop, "exactSnapshotsRestored": restored,
            "recoveryArchive": str(recovery_archive) if recovery_archive is not None and recovery_archive.exists() else None,
            "liveChildPid": active.process.pid if live_child else None,
            "retainedLocks": [str(path) for path, _ in descriptors] if live_child else [],
            "measurementLimit": "Two actual mode1 quick-start matches:16-player normal and24-player broken map64, catalog counts1..25 both variants and actual formerly blocked GUI map23/map54 selection. No physical-client or peakTPS claim. No physical client pixels, arbitrary-map/global-fastest-route, network latency or peakTPS claim.",
            "elapsedSeconds": round(time.monotonic() - started, 2),
        }
        (cycle.OUT / "map-capacity-gameplay-results.json").write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8")
        if original_error is None and cleanup_errors:
            raise RuntimeError("Fixture cleanup failed: " + "; ".join(cleanup_errors))
    print("MAP CAPACITY " + ("FOCUS CHECKS PASSED" if focus else "QUALIFICATION PASSED"), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, help="Exact candidate JAR under this project's build directory")
    parser.add_argument("--focus", nargs="+", help="One-match debugging only; never marks qualification true")
    parser.add_argument("--keep-going", action="store_true", help="Collect independent native FAIL results in focus mode")
    args = parser.parse_args()
    run(args.server_dir, args.focus, args.keep_going, args.artifact)
