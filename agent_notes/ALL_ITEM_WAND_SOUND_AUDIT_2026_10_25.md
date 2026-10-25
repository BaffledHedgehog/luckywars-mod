# Active item, wand and Academy sound audit — 25 October 2026

Scope: native ready loot leaves, their active weapons/wands/Academy/arcanums and
tools. Drops, food, potions, armor, trinkets and NPC encounters have a separate
audit owner. This note is a static source audit and correction contract, not a
claim of actual client audio or of exhaustive runtime execution of every item.

Legacy paths below are relative to the read-only
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Native files are under `src/main/java/me/skywars2/game/`. The author RP is the
read-only `all_in_one_1.21.8` specified in [RESOURCE_PACK.md](RESOURCE_PACK.md).
Neither source tree nor author RP has been changed.

## Reproducible inventory

[audit_wand_sounds.py](../tools/audit_wand_sounds.py) reads all legacy functions,
all native `Lbc*.java` sound sites and every narrow `lbc:ported` JSON leaf. Run:

```text
python tools/audit_wand_sounds.py
```

Evidence is written only below ignored
`build/verification/all-item-sounds/wands/`:

- `ready_leaves.json`: every ready leaf and its direct native item IDs, plus
  explicit implementation-family mapping. Empty mapping means outside this
  matrix or pending classification; it does not mean silent or verified.
- `family_matrix.csv`: per-family ready leaves and exact native sound-site
  references. This is an inventory, not an automatic PASS matrix.
- `legacy_playsound.json`: 1,656 non-comment command sites including exact file,
  line, condition/command, event, category, target, coordinates, volume, pitch,
  minimum volume. Old/unported/Spell Library variants remain identifiable.
- `native_calls.json`: 285 native sound-site contexts at the correction snapshot.
  Context includes the actual family instead of claiming that a globally found
  event proves every item's branch uses it. Dynamic/wrapper calls still require
  inspection.
- `custom_assets.json`: 110 concrete custom sound identifiers, event definitions,
  recursively referenced OGG existence and SHA-256. The dynamically constructed
  prefix `laser_mg_` is deliberately not treated as a concrete missing event.
- `summary.json`: 271 ready leaves, 101 implementation families, all271 leaves
  mapped. `review_basis` distinguishes this active-item matrix from the
  [companion drops/passives audit](ALL_ITEM_DROP_SOUND_AUDIT_2026_10_25.md).
  There are0 unclassified leaves. The individual mappings identify owners and
  source-review scope, not271 runtime-qualified items. Later ports may add leaves.
- `vanilla_registry_validation.json`: local26.3 sources JAR SHA,1,983 vanilla
  event IDs including expanded adult Cat/Chicken/Cow/Pig/Wolf registrations,
  and262 concrete native literal sites. There are0 unknown native vanilla
  literals and0 unknown vanilla event IDs in the indexed legacy commands.

No automatic reachability, sound decoding, client loaded-pack inspection or
audio playback occurs. Vanilla literal names are checked against local26.3
`SoundEvents.java`, including dynamic registration variants; this establishes
registry names, not client playback. All custom event files referenced by
concrete native calls inspected here exist in the author RP. Missing legacy
custom definitions `lbcsounds.coin` (Touhou kit menu) and `lbcsounds.zap`
(shock/wisp-book visual) have no concrete native call found; they are not
evidence of broken ready-wand playback.

## Corrected source discrepancies

These production edits were explicitly assigned by the parent agent after the
read-only findings. Joint compilation/runtime checks belong to the parent;
this audit ran neither a build nor a server.

| Item / trigger | Exact legacy source | Before | Correction |
|---|---|---|---|
| Ordinary and Academy Ender Wand cast, both broadcasts | `function/workingitems/endcast.mcfunction:2,11` | `LbcEnderWiki.ender` sent MASTER volume2/pitch1 with selector radius0, allowing listeners out to native range32 | Radius16 at both calls; private caster volume200 at line71 retained, including original origin coordinates after teleport |
| Tannerkivi converting a physical projectile to dirt | `function/items/arcanums/tannerskivi_tick_transform.mcfunction:1` | `LbcTanner.transmuteProjectile` used BLOCKS instead of source `master` | MASTER rooted-dirt event1/1 at the projectile exact position (parent also corrected prior block-center rounding) |
| Academy Acid Corruption applying delayed death | `function/workingitems/awaken_tuntija_wands/acidcorrupt/mode_titul_end_mob.mcfunction:3–6` | `applyDeath` broadcast four cave sounds near the target | Four private target-player packets, MASTER1/1. `SoundEvents.AMBIENT_CAVE` is already a Holder, as confirmed by existing `LbcItems` use of `.value()`; no extra `Holder.direct` wrapper |
| Barrier Arcanum newly paid main/off hand activation | `function/items/arcanums/barrier_spawn.mcfunction:1`, `barrier_spawn2.mcfunction:1`; inactive-hand gate in `barrier.mcfunction:1`, `barrier2.mcfunction:1` | Native per-hand latches produced no beacon activation sound | MASTER beacon.activate1/1 on each null-to-paid hand latch; no repeated sound on steady upkeep; failed payment emits none. Existing author-requested shared regeneration×5 upkeep stays authoritative; no old activation charge restored |
| Stand clocks actual use | `function/time_stop_powerful.mcfunction:6` (plain `time_stop.mcfunction:6` agrees) | Native MASTER volume10/pitch1, radius0/min0: loud within160 and absent beyond160 | MASTER1/pitch1/radius0/min.5: original volume near origin; far players in same world hear source minimum-volume fallback |
| Time Stop restore trail | `time_stopped_tickentity.mcfunction:4`, `datapack_enable_e:3`, `timestop_ret_particle:1–3` | Native lacked saved unfrozen-player path presentation | Per-world/epoch sparse trail, shared proximity<=1, restored source dust and MASTER impact.1/2/selector16. Normal end emits; match cancellation discards. Details below |
| Ordinary Exahoming paid click | `workingitems/awaken_tuntija_wands/exahoming_click.mcfunction:3` | Native lacked the source click cue | Source-exact `entity.firework_rocket.shoot`, MASTER1/1/radius0/min0 at caster origin, even when ray misses. Academy modes and donor Falling Stars kept separate |

`git diff --check` passed for these five edited files and the inventory utility.
The parent owns meaningful packet/runtime verification. No author RP or
translation/template change is needed for these corrections.

## Time Stop trail implementation and Exahoming registry check

Time Stop's **restore trail** was absent in the native implementation inspected.
`function/time_stopped_tickentity.mcfunction:4` creates a `timestop_particle`
marker at each unfrozen player's position if none exists within1 block. At
restore, `datapack_enable_e.mcfunction:3` dispatches each marker to
`timestop_ret_particle.mcfunction:1–3`, which emits a dust transition, plays
MASTER `entity.lightning_bolt.impact` volume.1/pitch2 to selector distance16,
then kills the marker. The parent explicitly authorized restoring this source
presentation. `LbcTimeStop.tick` now samples unfrozen players before countdown
end, excluding observers, spectator tags, nexus/imprinted. A27-cell spatial
index enforces source shared proximity<=1 across players, without marker
entities, forced chunk loads, arbitrary TTL or count cap. Extension retains
existing points. Normal `end` emits the source white-to-black dust transition
(scale3, count2, offset.3/.6/.3, speed2) and MASTER impact.1/2/selector16 at
each point. `stop` cancellation clears trails first; epoch mismatch discards
stale presentation. Per-player disconnect does not erase already laid world
trail points. Runtime verification remains parent-owned.

Exahoming's ordinary legacy click uses `entity.firework_rocket.shoot` in
`workingitems/awaken_tuntija_wands/exahoming_click.mcfunction:3` and
`exahoming_click_lib.mcfunction:2`. Initially this was suspected to be a typo
because it was absent in the author RP. The requested local26.3 registry audit
disproved that suspicion: `SoundEvents.java:632` explicitly registers
`FIREWORK_ROCKET_SHOOT` with that exact event ID. The parent-authorized fix
therefore restores source-exact `.shoot`1/1, rather than substituting `.launch`.
Native `LbcHoming`'s pre-existing launch3/1 belongs to **Falling Stars**, and
sculk-catalyst bloom1/1 belongs to Academy Curse. They do not replace the
ordinary click trigger. No native literal missing vanilla event remains after
the registry check. Real client playback remains unqualified.

## Family comparison matrix

The generated CSV provides exact leaves/native lines, including both Academy
and ordinary names where ready leaves exist. This table records the stage and
source context reviewed, with deliberately limited conclusions. `Present`
means the stated explicit sound branch exists; it is not runtime qualification
of every mode, chance predicate or listener.

| Family / native implementation | Source stage reviewed | Observation |
|---|---|---|
| `LbcWands`, tiny Fire/Blind/Air/Stone/Teleport/Ruby/Terra/Acid/Electro | `workingitems/sauvojen_staffs/*`, tiny casts and ray feedback | Explicit firecharge, squid, stone/amethyst, thunder, cure and twinkle branches present; MASTER helper. Shared ownerless trace fallback retains native sound path; no package-wide silence |
| ordinary/Academy Fire | `sauvojen_staffs/fire_preloop*` | Firecharge1/1 volley,1/.5 big, lava1/.1 concentrated present; tiny Fire1/2 remains distinct |
| ordinary/Academy Acid | `sauvojen_staffs/acid_preloop*`, `LbcClouds` | Cure1/2 on actual casts present; spread/accurate charge sound not inferred merely from other acid families |
| `LbcIceWand` | `workingitems/ice_wand/*` | Custom ice-wand1/.7 and shatter2/1, freeze-hurt and glass/deepslate impact branches present |
| `LbcCorruption` | `workingitems/corrupter.mcfunction`, `_upgraded` | Wither ambient1/1.2 present; old-file volume2 is not the current ordinary contract |
| `LbcAcidCorruption` | `awaken_tuntija_wands/acidcorrupt/*` | Cure cast present; four target-only delayed-death cave packets corrected above |
| `LbcAwakenedAcid` + `LbcClouds` | `awaken_tuntija_wands/acidacid/*` | Item-break rifle feedback and shared cure/extinguish/shield branches present; class alone is not the whole projectile effect chain |
| `LbcErecto` | `awaken_tuntija_wands/erecto/*` | Rotate trident3/1.3, sniper/ray feedback and random trace twinkles present |
| `LbcTesla` + `LbcWands` | `workingitems/lightningwand*` | Bend twinkle.1/2 and shared ray traces/cast/impact branches present |
| `LbcPrism` | `workingitems/prisma*` | Moving ordinary trident.1/0 and Academy chance amethyst.3/1 present; shield block separately present |
| `LbcGaycaster`, `LbcGaycasterAcademy` | `workingitems/gaycaster/*` including `meditate/*` | Laser shot+trident, custom hit voices, beacon+laser meditation and four element feedback branches present; modes have distinct volumes |
| `LbcCausticOmega` + `LbcClouds` | `workingitems/omegaflag.mcfunction:4`, acid-cloud effects | Omega distant Y100000 growl100000/pitch2 retained; not an accidental giant radius to normalize |
| `LbcAdvancedSnow` | `workingitems/advancedteleport*`, `snow_wand*` | Teleport source/destination sounds and custom snow2/.8 present |
| `LbcEnderWiki` | `workingitems/endcast.mcfunction`, `badwiki*` | Ender radius corrected; Bad Wiki private book page volume10 remains private and present |
| `LbcNeutral` | neutral cast and feather branches | Private caster wither-shot1/1 preserved; feather dragon-flap1/2 is separate and present |
| `LbcBloodConfuse` | `bloodmagic*`, `foxuscast*` | Dragon growl1/2; four illusioner branches with correct ordinary/default or explicit pitch0 present |
| `LbcEvokerBass` | `evocator*`, `basscast*`, combo functions | Evoker prepare1/2; double bass2/0; combo blast+twinkle1/1.5 present |
| `LbcLifestealer` | lifestealer casts/`other/artifact/vampirism.mcfunction:8` | Cast cat hiss1/2 and upgraded/lifesteal1.5/2 branch present. Ready aliases are `lifestealer`, `lifestealer_upgraded` |
| `LbcBlaze` | fallingstars/fireblaze mode functions | Blaze hurt/ambient/shoot, trade, firecharge and lava mode branches present; explicit source-defined families kept distinct |
| `LbcSummons` | `workingitems/summoner/*` | Initial vex/wither, cultist acquired-spell sounds, stranger beacon1.5/2 and fox+arrow movement feedback present |
| `LbcBolts` | `awaken_tuntija_wands/boltbolt/*`, infinite bolt, lightning bolts | Thunder, distant blast+anvil, raw thunder19/0 and signed snow/dragon-fireball burst branches present |
| `LbcConsuming` | `consuming_wand_loop:6`, `_upgraded_mode_avarice:3` | Selector25/min.01 wither.01/1 and selector25/min1 deepslate.2/1 already correct; do not repeat SLR radius/min-volume bug here |
| `LbcDuplicating` | `workingitems/divider.mcfunction:31–34`, avarice/dublicating | Four anvil4/pitches0,.2,.5,.13; placing.05/1 and bee.1/0 selector30 present |
| `LbcHoming` | exahoming and true-falling-star branches | Ordinary source shoot1/1 restored; Stars launch3/1 and Curse bloom already present. Local26.3 registry confirms both shoot and launch events |
| `LbcPsycho` | `workingitems/psychowand/*`, psychoblind | Target-only phantom/behind/scare packets preserved; offsets within hearing range make source min1 immaterial there. Radius10/30 scream branches distinct |
| `LbcAir` | `sauvojen_staffs/air_preloop*` | Explosion1/1.3 and source moss.2/.05 feedback present |
| `LbcBlind` | `sauvojen_staffs/blind_preloop*` | Default/phantom/rain/gatling and target feedback sound branches present |
| `LbcStone` | `sauvojen_staffs/stone_preloop*` | Cast amethyst1/1, ray/spike1/.1 and five stone-place pitches present |
| `LbcTeleport` | `sauvojen_staffs/teleport_*` | Cast thunder1/2; trial twinkle1/1 + thunder10/1 present |
| `LbcRuby` | `sauvojen_staffs/ruby_preloop*`, `ruby_ray`, `ruby_hole` | Ordinary amethyst1/1 versus ray/hole1/.1 preserved |
| `LbcTerra` | `sauvojen_staffs/terra_preloop*`, `true_terra_sword_hit` | Per-kind amethyst/block/beacon distinctions present; beacon.5/2 for true-sword hit |
| `LbcElectro` | `sauvojen_staffs/electro_*`, yellow-bolt/cyberkatana | Cast1/2; tiny/ordinary source differences retained; trace twinkle+rooted dirt.03/2; shield and triple cyberkatana burst present |
| `LbcExasperato`, `LbcExoperso` | exasperato mode tree, `exoperso*`, `exopersoexplode*` | Custom Exoperso at caster Y+20 volume15/pitch1 or2; blast sound bundle delegated through `exopersoBurst`, not absent just because class lacks five individual calls |
| `LbcBedrock` | `awaken_tuntija_wands/bedrock/*` | Mode place and room-break three pitches retained through local sound wrapper |
| `LbcWall` | wall and Academy wall functions | Place1/1 and break1/2 present |
| `LbcStonekivi` | `awaken_tuntija_wands/stonekivi/*` | Place1/1 and double deepslate-break4/7 volume modes present; source deprecated sun functions are not ready mode contracts |
| `LbcSheep` | sheep replacement and hole source | Extinguish1/2 replacement, sheep ambient1/1 selector80/min.5 present |
| `LbcDanmaku` | `items/wands/danmaku_staff/shoot:6`, `shoot_homing:6`, `click_bomb:9` | Cartography.5/2 and custom bomb2/1 present; parent separately owns grimoire continuation |
| `LbcSoulless` | soulcorruption/metasola field functions | Field feedback, heal, lava, wool, beacon and ravager branches present; material throw uses native snowball sound |
| `LbcVoidArcanum` | `items/arcanums/void_curse_spawn*.mcfunction:1` | Evoker cast1/1 present |
| `LbcDefensiveArcanums` | healing + barrier source | Healing XP pickup1/1 present; barrier per-hand activation corrected |
| `LbcTanner` | `items/arcanums/tannerskivi_*` | Stone activation1/0 present; conversion category corrected |
| `LbcKiuas` | `items/arcanums/kiuaskivi_spawn*`, `5true/sun_*` | Kiuas firecharge1/0; Sun4/0 and fire ambient3/0 retained |
| `LbcReactive` | `workingitems/reactive_shish_particles.mcfunction:3` | Grass-step MASTER1/0 present |
| `LbcShooters` | KFC/Phantom/Woodificator/Razorpine/transformables/SLR | Explicit cast/impact/melee mode-feedback present; woodificator roots.5/1 local branch exists; SLR previous fix retained. Projectile/native inherited effects not fabricated as extra authored sounds |
| `LbcGovnomet` | Govnomet cast/impact | Gravel cast1/2 and impact1/0; shield block branch present |
| `LbcVexSword` | `vexsword_arrow*`, `vex_ulta:6–15` | Arrow1/2 and ten cave5/1 packets retained; minion arrow/explosion branches present |
| `LbcFrost` | ice-sword effects | Snow-fall1/0 and zombie-villager-cure1/0 present |
| `LbcHalloweenWind` | `other/event/halloween/vex_wand/*` | Wind burst cast pitch1 or2 and impact1/1 present |
| `LbcAk47`, `LbcMagnum`, `LbcMobgun`, `LbcLaserMinigun` | weapon source paths in prior [weapon audit](WEAPON_SOUND_FIX_2026_10_25.md) | Prior qualified gun checks remain distinct evidence; do not declare all items qualified from these |
| `LbcFartus` | fartus casts/projectiles | Snowball throw, cartography branch, target-private oluh and banana feedback present |
| `LbcWtf` | firtusnadristailo/chef source | Custom chef random voices, craft icum1/1 and fountain grass.5/2 present. Fountain source explicitly uses `block`, so native BLOCKS is correct |
| `LbcHohlopitek` | Hohlopitek ray/impact | Pig ambient/death/hurt3/1 at origin plus custom bambam3/1 at impact Y+20 present |
| `LbcSoulhunter` | soulhunter moves/craft | Phantom1/1.3, zombie-door3/.8, wind, stamina wool and craft enchant1/0 present |
| `LbcStand`, `LbcTimeStop` | `items/oraora/click`, projectile damage, `time_stop_powerful` | ORAORA10/1 and strong hit1/1 present; clocks activation and sparse restore trail corrected above |
| `LbcBabylon`, `LbcMagic` | native gate and suppression/beacon transitions | No class-local new cast sound inferred from vanilla entities; shared Magic explosion/extinguish MASTER1/1 transition packets already implemented |
| `LbcHomes` | Magic Mirror teleport | Native actual-use origin enderman1/1 present; home-drop source audit belongs to the separate drops owner |
| `LbcIceMaterials` | Runic Ice Shard / Neutral Ball passive items | Class contains no explicit sound. Shard delegates to `LbcFrost.runicShard`; these passive proxies are not vanilla throw/use triggers |

## Parent runtime verification contract

Use actual item/service triggers and outbound packets. Near listener, far
listener and second-world listener must be distinguished; event existence alone
cannot qualify delivery.

1. Ender ordinary/Academy, each supported actual hand: near15 receives two
   volume2/pitch1 MASTER broadcasts; at17/31 none; caster retains extra private
   volume200. Sound coordinates are original execution origin, including the
   post-teleport command, matching legacy command context.
2. Tanner physical projectile conversion: conversion still writes dirt and
   discards the projectile; observed ROOTED_DIRT_BREAK packet uses MASTER1/1.
3. Acid Corruption actual delayed-death application to a player: exactly four
   target packets at1/1; attacker and nearby bystander receive none. Nonplayer
   application does not broadcast cave sounds. Existing Greed/rejection guards
   and delayed-death owner attribution still apply.
4. Barrier initial main, steady upkeep, add offhand, steady dual, remove/readd
   main, insufficient mana, reset/reacquire: counts1/0/1/0/1/0/1. Initial dual
   counts2. Every sound MASTER1/1; upkeep paid once per tick for either/both.
   Swapping equal barrier items without an observed empty hand keeps its latch
   and does not invent an extra activation. Removal while paused is still
   observed; time/suppression guards must not emit paid-activation sounds.
5. Clocks actual `useClocks`: own/near<=16 MASTER1/1; at17,160,200 same-world
   MASTER.5/1 positioned2 blocks from listener toward source; no other-world
   packet. Rejected/cooldown/observer uses produce no cast sound. Stop duration
   and native pause/restore behavior must retain their existing regression.
6. Sparse restore trail: clocks owner at point0, then .5 away, then2 away,
   stepping existing `tick` produces two points; normal `end` emits two
   impact.1/2 packets (MASTER) at point0/point2 to near listeners, none beyond16.
   Frozen players do not add points; a second unfrozen player's nearby path
   shares the same proximity index. Exact1 distance is suppressed. Extension
   retains points; `stop`/epoch cancellation emits none and leaks no points into
   a later stop. The other-dimension trail must remain separate.
7. Ordinary Exahoming paid actual use, each supported hand: source shoot1/1
   MASTER at caster origin, near15 listener receives,17 receives none, including
   an air/miss ray. Insufficient mana/rejected use emits none. Academy mode
   switches and casts must not acquire this ordinary-only click cue. Falling
   Stars must retain its separate launch3/1 at hit position.

The generator and static diff checks do not substitute for these tests, and
packets do not prove client speaker playback.

## Joint parent qualification

Buildccb40b6c passed46 recorded results in two actual mode1 matches (88.93s).
Native packet captures cover the corrected Ender/Exahoming/Tanner/Acid Corruption/
Barrier/Clock/Helicopter cues, with the prior SLR/Magnum/spawn regressions.
Actual grimoire use, ordinary/Academy Danmaku, Ender/Wiki, defensive arcanums,
Corruption, helicopter encounter, Ultraponos, Tanner, Time Stop/contacts and
Academy acquisition passed, including three actual match-stop cleanup families.
Exact355 sources/988 classes/2819 resources match the tested artifact. All271
ported leaves, including the new grimoire JSON, are present in the packaged JAR.
The only error lines are the known Polymer packet-context diagnostic while
creating fake bots; no new runtime errors occurred. Real client audio and every
conditional mode's runtime are not independently observed.

Ready: `build/verification/skywars2-all-item-sounds-ccb40b6c.jar`.
Evidence: `build/verification/all-item-sounds-final/qualification.json` and
`tools/smoke_all_item_sounds.py`. The isolated server stopped, settings/profiles/
scoreboard/JAR restored and fixture locks released. Five pre-existing user
file hashes and applied-template deletions are preserved. READY ONLY: main
remains04396ca4; no installation/stop/restart or author RP/template change.
Obsolete Hole Erosion54941d0e archive was removed after qualification; previous
Weapon Sound593a39df and main fallbacks remain. Cumulative cleanup290 JARs/
10,231,414,603bytes/9.53GiB. The native packet fixture covers selected source
boundaries; inventory classification is broader than runtime/client playback.
