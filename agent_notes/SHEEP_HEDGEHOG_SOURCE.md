# Sheep and Hedgehog: static source audit, 25 October 2026

Read-only audit. No implementation/runtime qualification. Only this note was written. Main remains READY ONLY. Rules read: project AGENTS.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. Source root below is `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`; every relative anchor is against that root.

Recommendation: choose ordinary + Academy **Sheep together**, retaining permanent sheep and unconditional male holes as the user has now confirmed. It is a smaller coherent family than Hedgehog. It still requires a complete bespoke sheep-hole branch, not merely a transformation wand. Hedgehog also requires five helper behaviors, trial wall mobs, seven dropped spikes and the god boss chain; a wand-only implementation would be incomplete.

## Dispatch and cost

`lbc/function/items/spells.mcfunction:5,11` selects raycastspell main/offhand; `lbc/function/workingitems/spell.mcfunction:2-3` and `spell_left.mcfunction:1-2` split upgraded flags. Numeric dispatch is `lbcr/function/raycast.mcfunction:10-11`, `raycast_left.mcfunction:10-11`, `raycast_upgraded.mcfunction:8-9`, `raycast_upgraded_left.mcfunction:8-9`: Sheep is **8**, Hedgehog **9**. Antimagic player/stopper gates apply. Actual source is neighboring `lbcr`, despite flag records living under `lbc`.

| Family | Ordinary | Academy |
|---|---|---|
| Sheep | Configured500 (`lbc/function/config.mcfunction:35`); `raycast_loop8cd:1-4` | Same500, no selectable modes (`raycast_loop8cd_upgraded:1-4`) |
| Hedgehog | Configured2500 (`config:34`), `raycast_loop9cd:1-7` |1000 admission and debit; Shift five-mode selector, actual debit only nonsneak (`raycast_loop9cd_upgraded:1-7`, `_2:1-6`) |

All paid branches invoke mage set and cast-wand hooks. Academy mode selection must follow the newer user contract: free and allowed at any mana, overriding legacy1000 admission. Academy Sheep offhand dispatch actually calls the ordinary upgraded wrapper, not the separate `_left` file. Ordinary offhand trial checks SelectedItem, a source hand bug to avoid in native used-stack progress.

## Sheep actual payload and trial

`lbcr/function/raycast_loop8.mcfunction:1-4` and `_upgraded:1-4` sample at0.75, Distance through150, while local block is air. They call the impact scan repeatedly; transforming a mob does not terminate the ray. Ordinary magenta dust, Academy muted purple.

`raycast_end8.mcfunction:1` selects nonitem nonmagicalsheep noncaster entity in the sample cell, excluded `#minecraft:notforsheep`. That tag (`minecraft/tags/entity_type/notforsheep.json:4`) contains **player only**. `raycast_end8_mob.mcfunction:1-7` summons a fresh vanilla sheep named jeb_, magicalsheep tag, empty loot, knockback resistance20; checks original Health>=1000 for advancement; particles/sound; teleports original to29999999/19999999/29999999 and kills it. No original entity snapshot, color choice, timed restoration or restoration handler exists in this chain. Antimagic `lbc/function/other/artifact/nomagic.mcfunction:5-10` destroys magicalsheep. This agrees with the actual author lore: mob becomes magical sheep, explicitly excluding players. Earlier agent claims about temporary/random-color transformations and player hotbar/death lore were mistaken and are withdrawn.

Academy `raycast_end8_upgraded.mcfunction:1` permits players in the initial selector because `notforsheep_2.json` is empty. But `raycast_end8_mob_check.mcfunction:1-6` eventually calls ordinary transform only for `type=!#minecraft:notforsheep`, still excluding players. Existing male/female tags are honored; otherwise a #minecraft:mobs entity gets male at chance50, then remaining untagged gets female. A **male mob unconditionally creates a sheep hole** at line5, then still transforms at line6. This does not check consumed potion/effects. Players are not transformed by line6.

Both impact handlers also check dropped items within2 or player inventory within2 for exact custom data `{transformable:1,potion:1,succubus:1}` (`raycast_end8:2-3`, upgraded same lines). Dropped potion path creates hole and kills **all item entities within5** (`raycast_end8_mob_sheep_blackhole:1-3`). Player-inventory path creates hole and clears matching potions from all players within5 (`..._player:1-3`). No potion-effect inspection. Each checks executing entity's SelectedItem for upgradable Sheep; if true, `...blackhole2:1-2` marks ready and adds ohno to that entity. The potion branches retain caster execution, while Academy male-mob branch executes as victim; therefore male-mob holes normally do not ready the caster.

Promotion: `lbc/function/other/magic_academy/can_upgrade_book.mcfunction:1-6` sets upgrade_available1/upgradable0 in mainhand. Sheep upgrade file `upgrade/sheepwand.mcfunction:1-2` applies X tier; Hedgehog equivalent applies S tier. Native must retain actual used-stack trial ownership, persistence and indirect kill attribution under existing user requirements.

## Bespoke sheep hole dependency

`lbc/function/raycastselector_rare.mcfunction:24` routes sheep_black_hole marker to `other/sheep_black_hole/tick.mcfunction:1-8`:

- Place `lbc:black_hole` structure each update at offset-23 on each axis, integrity0.05. Template dependency must be inspected for exact terrain mutation before claiming parity.
- Sheep sound R80. Pull entities at3..20 one block toward an arbitrary selected sheep-hole marker, excluding spectator/Greed/strict-map-object.
- Damage10 outside_border within5 except void_resistance>=1; kill projectiles and items within5. Damage is not explicitly credited in source and must inherit caster in native.
- Move0.05 toward any ohno player; otherwise toward furthest qualifying mob. No lifetime or local destruction condition in this handler. Global source cleanup and match cleanup need explicit native ownership.

This is distinct from existing black-hole/void/duplicating services; existing generic capability is not evidence that this authored variant is ported. Actual author Academy text explicitly describes male creatures creating a black hole. This matches the unconditional male-mob branch; no consumed-potion requirement applies to that Academy branch.

## Hedgehog complete dependency chain

`raycast_loop9.mcfunction:1-5` advances1 through200; mob-cell or solid-block collision spawns one stationary NoAI silverfish (`raycast_end9:1-7`), named BaffledHedgehog, owner ID/team, simpleTimer8. It shoots toward nearest enemy within15 (`lbc/function/other/hedgehogarrow:1-4`); `other/raycastarrow:1-14` creates piercing100 physical arrow at speed2 with helper UUID Owner. Native credit must trace back through helper to caster.

Academy five modes: Default, Healer, Tank, Far/Sniper, Laser. `raycast_loop9cd_upgraded_2_loop:1-8` reads per-hand mode then dispatches named mode loops. Each ray steps0.6 through400 and spawns helper on mob/block hit. `raycast_end9_mode_*:2-7` supplies caster ID/team; Default/Healer/Sniper start timer8, Laser has no timer, Tank has normal AI,100HP,15attack,KB resistance1000/attack KB1. `podchinenie_hedgehog:2-36` joins source team.

Live ticking is `lbc/function/mobs_tick_lbc:6` → `other/silverfish_tick:1-6`. Timer resets40 at0 and decrements (`lbc/function/timer:1-2`). Default emits4 arrow-damage beam over15 (`other/baffledwall/tick_loop_pl:1-4`, `tick_loop_dmg:1-2`); Healer instant_health0 over15 (`tick_loop_pl_heal`), Sniper20 arrow damage over90 with chance20 per eligible update (`tick_loop_pl_sniper`, `tick_loop_dmg_big`), Laser0.02 generic over9 every update (`tick_loop_pl_laser`). Target helpers preserve same owner/team exclusions; source Default/Sniper additionally require owner nearby (17/95 respectively), healer selects allies. Native teammate rules and protected-role filters remain required.

Ordinary trial: `raycast_loop9cd:4` sneaking with mainhand upgradable flag invokes `raycast_loop9cd_upgrade`. Lines1-5 summon **5 columns ×13 fish =65**, at forward1 and lateral1,2,-1,-2,0, base slow falling. Each has30HP,attack10,armor/toughness10,follow200,KB100; named grand_baffled_wall, loot lbc:hedgehog_wall. Lines9-10 globally count all baffled_wall silverfish;>=150 invokes `megahedgehog_spawn`. Line12 immediately readies wand even without boss threshold. Ordinary shot still occurs and2500 still spent.>=40 nearby ordinary/Academy helpers grants separate staff advancement, not this promotion trial.

`lbcr/function/megahedgehog_spawn:1-9` kills **every silverfish**, then spawns persistent god_hedgehog,scale16,health/max2147483647,armor/toughness20, loot lbc:god, special sounds/titles. That demands full `lbc/function/other/god_hedgehog/` graph, not the existing Silence special-case fixture alone. Wall combat graph is `other/baffledwall/tick*`; wall loot (`lbc/loot_table/hedgehog_wall.json`) yields seven executable spike snowballs: confuse, blind, boom, standart [source spelling], wither, poison, impulse. Produced loot must have native behavior/acquisition before enabling trial. `silverfish_tick:7` also has an extraordinarily rare exopersoexplode hook; choice on preserving this global silverfish quirk should be explicit rather than silently invented.

## Confirmed author text and target choices

Actual author RP `assets/minecraft/lang/ru_ru.json` values, verified by the root agent with UTF-8 decoding:

- `sheep_wand.lore.1`: «Превращает любого моба в волшебную овцу. (Не игрока)»
- `sheep_wand.lore.2`: «Делает нечто с зельем суккуба.»
- `upgrade_succesful_sheepwand`: «Теперь, если пол существа мужской, жезл взорвёт его эякулят, создав чёрную дыру!»

These text values contain no temporary duration, random-color selection, hotbar-loss or player-death promise. Prior agent inference of such lore was incorrect. Permanent sheep and unconditional male holes agree with the source and are confirmed by the user.

User target relayed by the root agent: preserve permanent sheep and male holes; implement a new sheep hole with shader and disc, radial terrain consumption in the original area, velocity pull stronger inside radius23.5 and weaker beyond while respecting player input, and tilt toward motion. These are authorized target changes to the legacy template/teleport-pull presentation, not claims that those changes already exist in code. Exact source geometry and mechanics below remain historical evidence for implementation.

Hedgehog remains deferred. Its65 hostile wall fish, immediate readiness, global150 boss threshold/all-silverfish wipe, seven spike drops and full boss are the complete source dependency scope.

## Follow-up: decoded structure geometry and entity tag

Read-only gzip/NBT decoding of `lbc/structure/black_hole.nbt` found size47×47×47, palette0 obsidian/palette1 air,54435 block records, no entities. With source placement offset(-23,-23,-23), the exact centered integer-lattice geometry is:

| Region | Exact offsets | Count | Radius |
|---|---|---:|---:|
| Solid obsidian core | dx²+dy²+dz²<=72 |2553 |sqrt72=8.485281 |
| Air consumption shell |72<dx²+dy²+dz²<=552 |51882 |outer sqrt552=23.494680 |
| Outside sphere | d²>552 | no records | unchanged |

Bounds are[-23,23] on each outer axis,[-8,8] for core. Counts independently equal the complete lattice spheres at those thresholds; no internal missing positions. It is neither a47³ air cube nor an empty black center. Source integrity0.05 independently selects template placements, approximately127.65 obsidian and2594.1 air records/update in expectation; repeated updates accumulate a solid inner core and clear outer shell. Lens geometry for a faithful visual can therefore use authored core radius8.485 and outer consumption23.495, subject to the user's explicit choice to replace structure placement with native radial consumption/shader. Those visual radii do not replace the distinct gameplay pullR20/damageR5.

`minecraft/tags/entity_type/mobs.json` is a datapack-authored tag under minecraft namespace, not a vanilla built-in living-entity class. It includes player at line45 and mannequin at89, but not armor_stand. Ordinary Sheep target (`raycast_end8:1`) does **not** require this tag: it excludes only notforsheep(player), item, magicalsheep and raycaster, so armor stands and other nonliving entities can enter kill-and-replace. Marker is excluded only by the preliminary loop trigger, not by the inner target selector. Academy sex assignment and male-hole branch do require #minecraft:mobs (`raycast_end8_mob_check:1,2,5`); armorstands can still reach ordinary conversion at line6 without sex/hole. Exact chance50 predicate is random_chance0.5 (`lbc/predicate/chance50.json:2-3`). Native eligibility should be an explicit implementation of the chosen author semantics, not accidental reuse of Java LivingEntity if broad source eligibility is intended.

## Integrated qualification

Build `65a39f13` passed 26 recorded suite results
in two actual mode1 matches (79.34 seconds), including actual
Sheep input/potion trial/promotion, permanent replacement, native shader-hole
physics/terrain, player input and actual match-stop cleanup. Danmaku, ordinary
Acid Academy, player rules, Time Stop, KFC counters and acquisition also passed.
Exact 348 sources / 965 classes /
2818 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-sheep-65a39f13.jar`; evidence: `build/verification/sheep-final/`.
Main remains04396ca4; READY ONLY is the user's existing choice. No author RP
edits or translation fragment are needed. Native fixtures do not qualify real
client pixels, disk restart/unload/disconnect or peak-load performance.
