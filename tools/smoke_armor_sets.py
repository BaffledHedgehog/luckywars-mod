"""Paladin, Shitty and Erosion armor set qualification in two actual mode1 matches on the isolated native fixture.

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
    "phantom-armor", "assassin-armor", "armor-sets", "ordinary-accessories", "native-gear", "passive-consumables",
    "signed-effects-math", "effect-layers", "time-stop", "time-stop-contacts",
    "danmaku", "void-arcanum", "minigun-visual", "rifle-main-hand", "projectile-self-hits",
)
PROBES = ()
CLEANUP = ()
LOBBY_SUITES = ()
LIVES_SCENARIO = False
FIXTURE_LIVES = 3
EXTRA_FIXTURE_PLAYERS = ()


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


def run(server_dir: Path, focus=None, keep_going=False, artifact=None, fixture_port=25576, match_groups=None):
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
    if fixture_port not in (25576, 25577):
        raise ValueError("Only isolated fixture ports25576/25577 are authorized")
    planned = tuple(focus) if focus else SUITES
    if match_groups is not None:
        match_groups = tuple(tuple(group) for group in match_groups)
        if not focus or not match_groups or any(not group for group in match_groups) or tuple(s for group in match_groups for s in group) != planned:
            raise ValueError("Explicit focus match groups must preserve every planned suite, in order")
        match_plans = tuple(enumerate(match_groups, 1))
    else:
        match_plans = tuple((match, planned) for match in ((1,) if focus else (1, 2)))
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
        required.update(LOBBY_SUITES)
        if LIVES_SCENARIO:
            required.add("lives")
    if missing := sorted(required - registered):
        raise ValueError(f"Native suites are not registered: {missing}")

    paths = (allowed / "codex-native-fixture.lock", allowed / "codex-armor-sets.lock")
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
            if probe.connect_ex(("127.0.0.1", fixture_port)) == 0:
                raise RuntimeError(f"Isolated fixture port{fixture_port} is already accepting connections")
        settings_path = allowed / "config/luckywars/game.json"
        profiles_path = allowed / "config/luckywars/players.json"
        installed = allowed / "mods/skywars2.jar"
        fixture_player_paths = []
        extra_players = ("TestDelta",) if "vanity-core" in planned else ()
        for fixture_name in dict.fromkeys(("TestAlpha", "TestBeta", "TestGamma", *EXTRA_FIXTURE_PLAYERS, *extra_players)):
            fixture_bytes = bytearray(hashlib.md5(("OfflinePlayer:" + fixture_name).encode()).digest())
            fixture_bytes[6] = (fixture_bytes[6] & 0x0f) | 0x30
            fixture_bytes[8] = (fixture_bytes[8] & 0x3f) | 0x80
            fixture_id = str(uuid.UUID(bytes=bytes(fixture_bytes)))
            fixture_player_paths.extend((world / "playerdata" / (fixture_id + ".dat"),
                                         world / "playerdata" / (fixture_id + ".dat_old"),
                                         world / "stats" / (fixture_id + ".json"),
                                         world / "advancements" / (fixture_id + ".json"),
                                         world / "players/data" / (fixture_id + ".dat"),
                                         world / "players/data" / (fixture_id + ".dat_old"),
                                         world / "players/stats" / (fixture_id + ".json"),
                                         world / "players/advancements" / (fixture_id + ".json")))
        for path in (allowed / "server.properties", settings_path, profiles_path, allowed / "usercache.json", world / "data/scoreboard.dat",
                     world / "data/scoreboard.dat_old", world / "data/minecraft/scoreboard.dat",
                     world / "data/minecraft/scoreboard.dat_old", installed, *fixture_player_paths):
            backups[path] = path.read_bytes() if path.exists() else None
        # Durable recovery survives a Python interruption or a child shutdown timeout.
        # Successful byte restoration removes this temporary old-JAR copy.
        recovery_archive = cycle.OUT / f"armor-sets-recovery-{time.time_ns()}-{os.getpid()}.zip"
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
        settings.update(lives=FIXTURE_LIVES, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=1200, stageTwoSeconds=1800, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        if fixture_port != 25576:
            # Another independent local server may occupy the default test port.
            # Change only our stopped fixture and include exact bytes in recovery.
            property_path = allowed / "server.properties"
            raw = backups[property_path]
            assert raw is not None and b"server-port=25576" in raw
            property_path.write_bytes(raw.replace(b"server-port=25576", f"server-port={fixture_port}".encode()))
        active = NativeServer("armor-sets-gameplay")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "loot-test-catalog"):
            if not check(0, suite):
                raise AssertionError("Required native player/loot fixture setup failed")
        if needs_classic:
            classic_setup = True
            if not check(0, "classic-melee-menu"):
                raise AssertionError("Required scoped native third participant setup failed")
        for match, match_suites in match_plans:
            if match > 1:
                active.prepare_map(1)
            for suite in LOBBY_SUITES:
                check(match, suite)
            for suite in ("kfc-menu", "frost-menu", "slr-menu"):
                check(match, suite)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            match_active = True
            for suite in match_suites:
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
        if LIVES_SCENARIO and not focus:
            # Finished maps remain for inspection; explicitly request fresh spawn markers for the extra scenario.
            active.prepare_map(1)
            # The real remaining-life scenario creates/replaces the player through PERFORM_RESPAWN.
            cursor = active.send("sw check lives")
            match_active = True
            active.expect("[check] PASS map-world-respawn", cursor, timeout=180)
            active.expect("[check] PASS lives:", cursor, timeout=180)
            # Other suites may retain a third participant: one elimination need not end this match.
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor, timeout=180)
            match_active = False
            completed.extend(({"match": 3, "suite": "map-world-respawn"}, {"match": 3, "suite": "lives"}))
            check(3, "map-world")
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
                         and not preparations
                         and normal_stop and restored and not cleanup_errors,
            "scope": "native-mode1-armor-sets",
            "completedMatches": completed_matches, "completed": completed,
            "matchGroups": [{"match": match, "suites": list(suites)} for match, suites in match_plans],
            "preparations": preparations, "setups": setups,
            "thirdParticipantContract": "Created only in ready lobby via native vanilla player factory; owned Gamma disconnects normally after final match. Preexisting Gamma is retained. Exact UUID playerdata/stats/advancement bytes join the normal post-exit restoration snapshots.",
            "preparationContract": "Armor checks use registered native participants, scoped entities and the actual shared item scheduler; neighboring suites own their usual scoped preparation.",
            "checkFailures": failures, "cleanupErrors": cleanup_errors,
            "artifactSha256": installed_sha, "sourceArtifactSha256": source_sha,
            "candidateArtifact": str(candidate), "serverDirectory": str(allowed), "serverPort": fixture_port,
            "normalChildShutdown": normal_stop, "exactSnapshotsRestored": restored,
            "recoveryArchive": str(recovery_archive) if recovery_archive is not None and recovery_archive.exists() else None,
            "liveChildPid": active.process.pid if live_child else None,
            "retainedLocks": [str(path) for path, _ in descriptors] if live_child else [],
            "measurementLimit": f"{len(completed_matches)} actual mode1 matches completed; exact planned scope is recorded in matchGroups and observed results in completed/checkFailures. No physical client rendering, arbitrary-map, network latency or peakTPS claim.",
            "elapsedSeconds": round(time.monotonic() - started, 2),
        }
        (cycle.OUT / "armor-sets-gameplay-results.json").write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8")
        if original_error is None and cleanup_errors:
            raise RuntimeError("Fixture cleanup failed: " + "; ".join(cleanup_errors))
    print("ARMOR SETS " + ("FOCUS CHECKS PASSED" if focus else "QUALIFICATION PASSED"), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, help="Exact candidate JAR under this project's build directory")
    parser.add_argument("--focus", nargs="+", help="One-match debugging only; never marks qualification true")
    parser.add_argument("--keep-going", action="store_true", help="Collect independent native FAIL results in focus mode")
    args = parser.parse_args()
    run(args.server_dir, args.focus, args.keep_going, args.artifact)
