# Mechanism suppressor source audit

Read-only audit, 21 October 2026. User contract: Attack Helicopter is TECH;
magic suppression must affect neither use nor shots; mechanism suppression affects both.
Only this Markdown was written; no Java/resources/build/server changes.

## Source roots

- Legacy root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data` (paths below beginning `lbc/` or `minecraft/`).
- Native root `E:/servers_mew/skywars2_last/luckywars-mod` (paths beginning `src/`).

## Two types and presentation

The two source stopper types are mechanism `stopper` and magic `stopper_magic`.
`lbc/loot_table/stopper_techno.json:8` uses panda_spawn_egg; lines 13–20 preserve
model `lbc:stopper`, tooltip `lbc:tier_b`, marker entity and tags
`stopper,rc_1sec,marker_2t`. Name translation is `mecha_breaker` (line 29);
lore keys `mecha_breaker.lore.1/.2` (40,45). Magic source is
`lbc/loot_table/stopper_magic.json:13,18`: model/tag `stopper_magic`.
No second mechanism-only variant was found.

## Activation, area and lifetime

- `lbc/function/raycasters_1s.mcfunction:9,11` activates unactivated mechanism/magic markers.
- `lbc/function/other/stopperactivate.mcfunction:1–4` places a lightning rod at
  the marker when air, otherwise one above when the current block is neither air
  nor lightning rod, moves the marker upward accordingly, then adds `activated`.
  `stopperactivate_magic.mcfunction:1–4` is identical.
- `lbc/function/markers_2t1.mcfunction:1` dispatches `stopper` to `other/stopper`.
- `lbc/function/other/stopper.mcfunction:1` removes `#minecraft:mecha` blocks
  in an inclusive +/-15 cube, unless marker has `not_block`; strict replacement.
- Lines 2–5 use **spherical distance <=20**, without team filtering: destroy
  `#minecraft:mecha` entities, cyborg_boss skeletons, entities tagged `mecha`,
  and remove two player attribute modifiers for non-spectators.
- `other/stopper_destroy.mcfunction:1–2`: soul_fire_flame then kill.
  `other/stopper_marker_destroy.mcfunction:1–2`: nuke cleanup if tagged nuke,
  then kill. `other/stopper_player_remove_smth.mcfunction:1–2` removes only
  movement_speed UUID c1cda724-0ea2-460a-98b8-9631eafd2515 and attack_damage
  UUID 585f98b8-a8b8-4e4c-838a-f2e124831040; does not broadly strip all tech.
- No timer/TTL: persistent while carrier rod survives. `other/stopper.mcfunction:6`
  checks activated rod existence. `other/stopperbreaked.mcfunction:1–2` finds
  nearest lightning_rod item, converts through `stopperbreakedasitem:1–2`
  into loot `lbc:stopper_techno`, kills that item and marker.

## Target definitions and Helicopter

`minecraft/tags/entity_type/mecha.json:1` defines hopper/spawner/TNT minecarts,
lightning_bolt, TNT, creeper, `#arrows`, iron_golem, copper_golem.
`minecraft/tags/block/mecha.json:1` is the explicit redstone/mechanism block list,
also nesting doors/trapdoors/fence_gates. Preserve source membership instead of
guessing from block names/materials.

`lbc/function/workingitems/spell1_default.mcfunction:42` and
`spell1_default_left.mcfunction:17` gate Helicopter activation with marker tag
`stopper` at distance <=20, then set coldown25=800.
`workingitems/attackhelicopter.mcfunction:1–2` and
`attackhelicopter_left.mcfunction:1–2` spawn native arrows tagged
`helicopterbullet,groundbreaker`; mechanism suppression therefore also catches
in-flight bullets through `#arrows` membership, even without tag `mecha`.

Magic is distinct: `other/stopper_magic.mcfunction:1,3–6` removes magic blocks,
summactivated mobs, `#magic_entities`, tagged magic entities and checks its rod.
The user contract explicitly excludes Helicopter from this category.

## Native gap and integration boundary

Existing native magic service is `src/main/java/me/skywars2/game/LbcMagic.java`:
registration at 122,204–215; source-compatible magic-beacon presentation at
212–215; magic marker tracking at 420–427; spherical magic query at 398–403.
It accepts only `stopper_magic`, not mechanism markers.

Existing mechanism checks are isolated: `LbcItems.java:289–291` checks `stopper`
for rocket boots using a **cube** of radius20 (explicit Paper compatibility);
`LbcItems.java:1159` accepts either stopper for curse pausing. These do not
implement marker activation, rod lifetime, mechanism block/entity pulses or a
reusable mechanism suppression API. Rocket cube semantics must remain local.

Required cohesive dependency for Helicopter: native mechanism beacon placement,
presentation, saved/lifecycle state and cleanup, sphere20 point/segment queries,
source mechanism block/entity membership and suppression pulses; use and bullet
flight must call this service. Broad porting of every tech weapon/armor feature
is unnecessary. Existing magic beacon lifecycle/index/scheduler patterns are
the relevant implementation precedent, but magic suppression must stay separate.
