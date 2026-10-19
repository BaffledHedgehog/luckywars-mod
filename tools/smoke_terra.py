"""Run the regular Terra wand checks on a disposable, isolated server copy."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir: Path, variants: tuple[str, ...] = ("regular",), swords: bool = False,
        crafting: bool = False, electro_crafting: bool = False, electro_gear: bool = False, razor: bool = False, pine: bool = False,
        electroblocker: bool = False, exasperato: bool = False, exasperato_academy: bool = False, exasperato_crafting: bool = False, exoperso: bool = False, exoperso_blast: bool = False, fire: bool = False, stone: bool = False, blind: bool = False, air: bool = False, teleport: bool = False, ruby: bool = False) -> None:
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
    label = ("exoperso-blast" if exoperso_blast else "exoperso" if exoperso else "ruby-wand" if ruby else "teleport-wand" if teleport else "air-wand" if air else "blind-wand" if blind else "stone-wand" if stone else "fire-wand" if fire else "exasperato-academy" if exasperato_academy else "exasperato-crafting" if exasperato_crafting else "exasperato" if exasperato else "electroblocker" if electroblocker else "pinegun" if pine else "electro-gear" if electro_gear else "electro-crafting" if electro_crafting else "terra-crafting" if crafting else "terra-razor" if razor else "terra-swords" if swords
             else "terra-both" if len(variants) > 1 else f"terra-{variants[0]}")
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=10 if exoperso_blast else 2, health=20, day=1, gameMode=1, countdownSeconds=1,
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
        if swords:
            active.check("terra-sword-menu")
            completed.append("Terra sword loot payloads and Polymer item restoration")
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            active.check("terra-sword")
            completed.append("Terra sword native use, melee/arrow damage, swept hits and suppression")
            active.check("terra-beam")
            completed.append("True Terra swept interval damage, hurt cooldown bypass, visual-edge collision, stacking and attribution")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.prepare_map(1)
            active.check("terra-sword-cleanup")
            completed.append("Terra sword projectiles and virtual displays clear after match stop")
        elif crafting or electro_crafting or electro_gear or razor or pine or electroblocker or exasperato or exasperato_academy or exasperato_crafting or exoperso or exoperso_blast or fire or stone or blind or air or teleport or ruby:
            if pine:
                active.check("pinegun-menu")
                completed.append("Pinegun loot bundle and Polymer item restoration")
            if electro_gear:
                active.check("electro-gear-menu")
                completed.append("Cyberkatana and Electro Shield loot payloads and Polymer item restoration")
            if exoperso:
                active.check("exoperso-menu")
                completed.append("Ordinary and Academy Exoperso source loot, native IDs, source model/lore, Tier A/S and 5000 mana allowance")
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            if stone:
                active.check("stone-wand")
                completed.append("Stone wand source loot, native flight, Academy modes, trap attribution and target guards")
            if blind:
                active.check("blind-wand")
                completed.append("Blind wand source loot, native dispatch, Academy modes, mana and charge queues")
            if air:
                active.check("air-wand")
                completed.append("Air source payload, smart lifetime, Academy promotion, TNT impacts and credited blast damage beyond 80 blocks")
            if teleport:
                active.check("teleport-wand")
                completed.append("Teleport loot and cost, native sweep/lightning/levitation, Academy modes, inverse container move and arrow cloud")
            if ruby:
                active.check("ruby-wand")
                completed.append("Ruby source loot and cost, offhand/mana/mode controls, native drilling and Academy rays/targets")
            if fire:
                active.check("fire-wand")
                completed.append("Native fire wand volley, Academy modes/trial, mana, impacts, ownership and protected targets")
            if electroblocker:
                active.check("electroblocker")
                completed.append("Electroblocker native crafting, Polymer presentation, passive ownership and electric aura")
            if exasperato:
                active.check("exasperato")
                completed.append("Exasperato native TNT/lightning payload, collision, ownership, team filter and lifecycle")
            if exasperato_academy:
                active.check("exasperato")
                completed.append("Base Exasperato native payload, collision, ownership and lifecycle regression")
                active.check("exasperato-academy")
                completed.append("Academy Exasperato upgraded loot, hand/mana restrictions, charge lifetimes and electro-ray payload")
            if exasperato_crafting:
                active.check("exasperato-crafting")
                completed.append("Native Exasperato crafting from ordinary/Academy Air and Exoperso donor, previews, takes and Polymer restoration")
            if exoperso:
                active.check("exoperso")
                completed.append("Exoperso 4999/5000 mana gates and debits, Overload allowance bypass, five-cast same-hand Academy trial, windup/aim/flight and native wall/entity collision queue")
                active.check("exasperato-crafting")
                completed.append("Native ordinary and Academy Exoperso donors craft Exasperato in 2x2 with preview and take conservation")
            if exoperso_blast:
                active.check("exoperso-blast")
                completed.append("Real Exoperso collision drains all 32 shared TNT callbacks, verifies owner damage attribution, and clears series sources")
            if crafting:
                active.check("terra-crafting")
                completed.append("Native Terra 2x2/3x3 recipes, preview safety, normal/shift takes and conservation")
            if electro_crafting or electro_gear:
                active.check("electro-crafting")
                completed.append("Native Electro wand 2x2/3x3 recipe, legacy donor and preview/take restrictions")
            if electro_gear:
                active.check("electro-gear")
                completed.append("Native katana and shield recipes plus Electro gear combat checks")
            if razor:
                active.check("terra-razor")
                completed.append("Terra Razor RMB-only dispatch, LMB rejection, offhand, swept hit attribution, TTL and guards")
            if pine:
                active.check("pinegun")
                completed.append("Pinegun loot, native volleys, ammo, held-pair multiplier and restrictions")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.prepare_map(1)
            if stone:
                active.check("stone-wand-cleanup")
                completed.append("Stone charge cleanup after match stop")
            if blind:
                active.check("blind-wand-cleanup")
                completed.append("Blind charges and queued modes clear after match stop")
            if air:
                active.check("air-wand-cleanup")
                completed.append("Air charges and owned TNT clear after match stop")
            if teleport:
                active.check("teleport-wand-cleanup")
                completed.append("Teleport charges and arrow clouds clear after match stop")
            if ruby:
                active.check("ruby-wand-cleanup")
                completed.append("Ruby charges clear after match stop")
            if fire:
                active.check("fire-wand-cleanup")
                completed.append("Native fire projectiles clear after match stop")
            if razor:
                active.check("terra-razor-cleanup")
                completed.append("Terra Razor projectiles and display tails clear after match stop")
            if pine:
                active.check("pinegun-cleanup")
                completed.append("Pinegun burst and native arrows clear after match stop")
            if exasperato or exasperato_academy:
                active.check("exasperato-cleanup")
                completed.append("Exasperato charges and native payload clear after match stop")
            if exoperso or exoperso_blast:
                active.check("exoperso-cleanup")
                completed.append("Exoperso charges, shared blast queue and burst visual state clear after match stop")
            if exasperato:
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                active.check("exasperato-second-match")
                completed.append("Exasperato cast and shared lifetime work in the following match")
                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check("exasperato-cleanup")
                completed.append("Second-match Exasperato charge clears on match stop")
            if blind:
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                active.check("blind-wand-resume")
                completed.append("Blind wand service accepts casts in the following match after stop")
                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check("blind-wand-cleanup")
                completed.append("Following-match Blind charges and Gatling queues clear on stop")
            if air:
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                active.check("air-wand-resume")
                completed.append("Air wand service accepts casts in the following match after stop")
                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check("air-wand-cleanup")
                completed.append("Following-match Air charges clear on match stop")
            if teleport:
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                active.check("teleport-wand-resume")
                completed.append("Teleport service accepts casts in the following match after stop")
                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check("teleport-wand-cleanup")
                completed.append("Following-match Teleport charge clears on match stop")
            if ruby:
                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                active.check("ruby-wand-resume")
                completed.append("Ruby service accepts casts in the following match after stop")
                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check("ruby-wand-cleanup")
                completed.append("Following-match Ruby charge clears on match stop")
        else:
            for variant in variants:
                active.check(f"terra-{variant}-menu")
                completed.append(f"{variant} Terra loot payload, mana cost and Polymer item restoration")

                cursor = active.send("sw start")
                active.expect("-> PLAYING", cursor)
                if variant == variants[0]:
                    active.check("terra-visual")
                    completed.append("Terra display prediction, aligned particle trail, native freeze/step gate, clipped impacts and two-tick retirement")
                active.check(f"terra-{variant}")
                completed.append(f"{variant} Terra swept projectile, targets, walls, pause, suppression and lifecycle")

                cursor = active.send("sw stop")
                active.expect("LOADING -> LOBBY", cursor)
                active.prepare_map(1)
                active.check(f"terra-{variant}-cleanup")
                completed.append(f"{variant} Terra projectiles and virtual displays clear after match stop")
        active.stop()
        active = None
        print("EXOPERSO BLAST CHECKS PASSED" if exoperso_blast else "EXOPERSO CHECKS PASSED" if exoperso else "RUBY WAND CHECKS PASSED" if ruby else "TELEPORT WAND CHECKS PASSED" if teleport else "AIR WAND CHECKS PASSED" if air else "BLIND WAND CHECKS PASSED" if blind else "STONE WAND CHECKS PASSED" if stone else "FIRE WAND CHECKS PASSED" if fire else "EXASPERATO ACADEMY CHECKS PASSED" if exasperato_academy else "EXASPERATO CRAFTING CHECKS PASSED" if exasperato_crafting else "EXASPERATO CHECKS PASSED" if exasperato else "ELECTROBLOCKER CHECKS PASSED" if electroblocker else "PINEGUN CHECKS PASSED" if pine else "ELECTRO GEAR CHECKS PASSED" if electro_gear else "ELECTRO CRAFTING CHECKS PASSED" if electro_crafting else "TERRA SWORD CHECKS PASSED" if swords else f"TERRA CHECKS PASSED: {', '.join(variants)}", flush=True)
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
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--blind", action="store_true", help="Run ordinary and Academy Blind wand checks")
    modes.add_argument("--air", action="store_true", help="Run ordinary and Academy Air wand checks")
    modes.add_argument("--teleport", action="store_true", help="Run ordinary and Academy Teleport wand checks")
    modes.add_argument("--ruby", action="store_true", help="Run ordinary and Academy Ruby wand checks across two matches")
    modes.add_argument("--stone", action="store_true", help="Run ordinary and Academy stone wand checks")
    modes.add_argument("--fire", action="store_true", help="Run ordinary and Academy fire wand checks")
    modes.add_argument("--academy", action="store_true", help="Run only Academy Terra checks")
    modes.add_argument("--both", action="store_true", help="Run regular and Academy Terra checks")
    modes.add_argument("--swords", action="store_true", help="Run regular and improved Terra sword checks")
    modes.add_argument("--crafting", action="store_true", help="Run only native Terra crafting checks")
    modes.add_argument("--electro-crafting", action="store_true", help="Run only native Electro crafting checks")
    modes.add_argument("--electro-gear", action="store_true", help="Run Electro weapon and shield crafting/combat checks")
    modes.add_argument("--electroblocker", action="store_true", help="Run Electroblocker crafting and passive projectile checks")
    modes.add_argument("--exasperato", action="store_true", help="Run Exasperato native payload and collision checks")
    modes.add_argument("--exasperato-crafting", action="store_true", help="Run only native Exasperato crafting checks")
    modes.add_argument("--exasperato-academy", action="store_true", help="Run base and Academy Exasperato native match checks")
    modes.add_argument("--exoperso", action="store_true", help="Run only native ordinary and Academy Exoperso resource, mana, trial, flight, collision, crafting and cleanup checks")
    modes.add_argument("--exoperso-blast", action="store_true", help="Drain one real 32-step Exoperso burst and verify damage attribution and series cleanup")
    modes.add_argument("--razor", action="store_true", help="Run only Terra Razor native combat checks")
    modes.add_argument("--pine", action="store_true", help="Run only native Pinegun combat checks")
    args = parser.parse_args()
    run(args.server_dir, ("regular", "academy") if args.both else
        ("academy",) if args.academy else ("regular",), swords=args.swords,
        crafting=args.crafting, electro_crafting=args.electro_crafting, electro_gear=args.electro_gear, razor=args.razor, pine=args.pine,
        electroblocker=args.electroblocker, exasperato=args.exasperato,
        exasperato_academy=args.exasperato_academy, exasperato_crafting=args.exasperato_crafting, exoperso=args.exoperso, exoperso_blast=args.exoperso_blast, fire=args.fire, stone=args.stone, blind=args.blind, air=args.air,
        teleport=args.teleport, ruby=args.ruby)
