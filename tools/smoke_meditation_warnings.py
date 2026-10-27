"""Meditation warning displays, complete web collision and Gaycaster qualification in two actual mode1 matches on the isolated native fixture.

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


SUITES = (
    "sauvojen-meditation", "gaycaster", "minigun-visual", "time-stop",
    "time-stop-contacts", "tps-compensation", "effect-layers", "speed-fov-transitions",
)
PROBES = ("gaycaster-stop-probe",)
CLEANUP = ("gaycaster-cleanup",)


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
    required = set(planned) | {"bots", "loot-test-catalog", "kfc-menu", "frost-menu", "slr-menu"}
    if set(planned) & {"late-swords", "late-swords-stop-probe"}:
        required.update(("late-swords-prepare", "late-swords-preparation-cleanup"))
    if needs_classic:
        required.update(("classic-melee-menu", "classic-melee-lobby-cleanup"))
    if not focus:
        required.update(PROBES + CLEANUP)
    if missing := sorted(required - registered):
        raise ValueError(f"Native suites are not registered: {missing}")

    paths = (allowed / "codex-native-fixture.lock", allowed / "codex-meditation-warnings.lock")
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
            active.expect("[check] PASS " + suite, cursor, timeout=180)
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
        gamma_bytes = bytearray(hashlib.md5(b"OfflinePlayer:TestGamma").digest())
        gamma_bytes[6] = (gamma_bytes[6] & 0x0f) | 0x30
        gamma_bytes[8] = (gamma_bytes[8] & 0x3f) | 0x80
        gamma_id = str(uuid.UUID(bytes=bytes(gamma_bytes)))
        gamma_paths = (world / "playerdata" / (gamma_id + ".dat"), world / "playerdata" / (gamma_id + ".dat_old"),
                       world / "stats" / (gamma_id + ".json"), world / "advancements" / (gamma_id + ".json"))
        for path in (settings_path, allowed / "config/luckywars/core.json", profiles_path, world / "data/scoreboard.dat",
                     world / "data/scoreboard.dat_old", installed, *gamma_paths):
            backups[path] = path.read_bytes() if path.exists() else None
        # Durable recovery survives a Python interruption or a child shutdown timeout.
        # Successful byte restoration removes this temporary old-JAR copy.
        recovery_archive = cycle.OUT / f"meditation-warnings-recovery-{time.time_ns()}-{os.getpid()}.zip"
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
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = NativeServer("meditation-warnings-gameplay")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "loot-test-catalog"):
            if not check(0, suite):
                raise AssertionError("Required native player/loot fixture setup failed")
        if needs_classic:
            classic_setup = True
            if not check(0, "classic-melee-menu"):
                raise AssertionError("Required scoped native third participant setup failed")
        for match in ((1,) if focus else (1, 2)):
            if match == 2:
                active.prepare_map(1)
            for suite in ("kfc-menu", "frost-menu", "slr-menu"):
                check(match, suite)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
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
            "scope": "native-mode1-meditation-warnings",
            "completedMatches": completed_matches, "completed": completed,
            "preparations": preparations, "setups": setups,
            "thirdParticipantContract": "Created only in ready lobby via native vanilla player factory; owned Gamma disconnects normally after final match. Preexisting Gamma is retained. Exact UUID playerdata/stats/advancement bytes join the normal post-exit restoration snapshots.",
            "preparationContract": "Sauvojen fixtures snapshot and restore their own resident terrain; the meditation fixture warms only its complete source footprint.",
            "checkFailures": failures, "cleanupErrors": cleanup_errors,
            "artifactSha256": installed_sha, "sourceArtifactSha256": source_sha,
            "candidateArtifact": str(candidate), "serverDirectory": str(allowed), "serverPort": 25576,
            "normalChildShutdown": normal_stop, "exactSnapshotsRestored": restored,
            "recoveryArchive": str(recovery_archive) if recovery_archive is not None and recovery_archive.exists() else None,
            "liveChildPid": active.process.pid if live_child else None,
            "retainedLocks": [str(path) for path, _ in descriptors] if live_child else [],
            "measurementLimit": "Two actual mode1 matches: native meditation warning geometry/lifetime and complete web collision, Gaycaster ordinary/Academy and Polymer minigun plus shared TimeStop/TPS/effect/FOV regressions. No physical client pixels, arbitrary-map/global-fastest-route, network latency or peakTPS claim.",
            "elapsedSeconds": round(time.monotonic() - started, 2),
        }
        (cycle.OUT / "meditation-warnings-gameplay-results.json").write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8")
        if original_error is None and cleanup_errors:
            raise RuntimeError("Fixture cleanup failed: " + "; ".join(cleanup_errors))
    print("MEDITATION WARNINGS " + ("FOCUS CHECKS PASSED" if focus else "QUALIFICATION PASSED"), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, help="Exact candidate JAR under this project's build directory")
    parser.add_argument("--focus", nargs="+", help="One-match debugging only; never marks qualification true")
    parser.add_argument("--keep-going", action="store_true", help="Collect independent native FAIL results in focus mode")
    args = parser.parse_args()
    run(args.server_dir, args.focus, args.keep_going, args.artifact)
