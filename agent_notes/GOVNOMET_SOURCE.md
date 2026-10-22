# Govnomet — bounded source audit, 21 October 2026

Current implementation and resolved author decisions: [NEXT_SHOOTER_PORT](NEXT_SHOOTER_PORT.md).
The bounded source findings below are historical source observations; they do not override that contract.

Read-only source audit; no implementation or runtime qualification. Applicable
AGENTS.md, agent_notes/AGENTS.md and ARCHITECTURE.md read. Source root below is
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data` (D).

## Identity and activation

- D/lbc/loot_table/transformables_shooter_all.json: inline carrot-on-a-stick,
  model `lbc:govnomet`, tier D, name `shit_cannon`, flags
  `{govnoq:1,transformable:1,shooter:1,magicwand:1,craftusable:1,govnomet:1}`.
  Same item also appears inline in loliluckyblock.json. No standalone loot ID.
- Preserve source lore/name/author/event formatting. Read-only RP
  `assets/minecraft/lang/ru_ru.json`: name «Говномёт», lore «Стреляет говном»,
  `shitgun.lore` «Стоит объединить с Электрокатаной!» (existing crafting family).
- D/lbc/function/load.mcfunction:8 defines carrot use RC_Raycast;
  players_tick.mcfunction:19 calls items/spells when positive. The
  interaction_rightclickable advancement also rewards items/spells.
- items/spells.mcfunction:7/13 dispatches magicwand main/offhand; own
  nomagic_active and any nonspectator nomagic_active player within 20 block firing.
  Offhand additionally requires global lefthand != 1. Thus one legacy input may
  fire both matching held copies. RC_Raycast resets after dispatch. No separate
  weapon-specific handinput function exists in the traced graph.
- workingitems/spell1[_left] -> spell1_default[_left] -> govno, via govnomet flag.
  Main line46/offhand line23 additionally reject marker tag stopper within20.
  No mana check, subtraction, inventory/ammo consumption or cooldown in this graph.
- govno selects govno_govnomen only with #lbcskill=1 and player lbc.skill=9;
  all other cases select govno_default. Skill support should use existing skill
  state, not infer variant from govnoq.

## Projectile

- govno_default/govno_govnomen each spawn ONE marker at eyes+0.5, lifetime50,
  tags slowraycast, shieldblockable, pieceofshit[_mega], player. Both copy owner
  lbcID2/team_number and rotation; launch sound gravel.place volume1 pitch2.
- Normal govnoselect adds independent integer yaw[-20,20], pitch[-10,10].
  Mega has no spread. Legacy rotation lookup uses nearest player, a source flaw.
- tick_not_timestopped.mcfunction:3 -> raycastselector increments Lifetime,
  player dispatcher lines16/17 -> lbcr:raycast_gavno[_mega]. At end lifetime50
  raycastselector_kills kills marker. Time-stop pauses the normal tick path.
- Each flight tick samples five points at 0.5 spacing (0..2 forward), then
  raycast_end_with_tp resets Distance and teleports marker to last sample.
  Thus 2 blocks/tick, nominal 100-block flight plus spawn offset; dust brown
  [0.388,0.251,0.137] one per sample normal, four mega.
- Block collision is any non-#minecraft:airs at sample, impact at previous
  half-step. Entity selector is #minecraft:mobs, dx=dy=dz=0 and distance<=15,
  not spectator; this is a small selector box, NOT a 15-block splash sphere.
  Entity prebranch temporarily tags owner nodmg and teammate players nodmg1,
  then impacts at first eligible entity. Cleanup removes tags globally.
- Projectile has NO magic or mecha tag: stopper_magic's tag=magic destruction
  does not remove it in flight. shieldblockable spelling has no other .mcfunction
  consumer found (normal shield family uses different spellings).

## Impacts and damage

- lbcr/raycast_gavno1: replaces impact block with dirt; gravel.place volume1
  pitch0; nausea15s amplifier0 to eligible players in selector box, wither1s
  amplifier10 to eligible #mobs in box; kills marker. No direct /damage.
- lbcr/raycast_gavno_mega1: resistance1s amplifier3, hidden particles, to ALL
  nonspectator players radius2; exactly TEN dirt falling blocks (Time1,
  DropItem1, HurtEntities1, FallHurtAmount2, FallHurtMax1000); each random XYZ
  offset[-0.5,0.5] through universal_pos_razb_0_5; instant silent creeper
  Fuse0, ExplosionRadius1, ignited1; same sound/nausea, wither3s amplifier10;
  kills marker. Native explosion/falling damage is additional, not fixed damage.
- Wither amp10 uses minimum native tick interval; actual accepted damage depends
  native effect timing/resistance/invulnerability, so do not describe it as a
  fixed DPS without runtime evidence. Source supplies no attribution for status,
  creeper or falling blocks: port must inherit owner (user contract).
- Both impacts grant shooter/shit_cannon advancement when tagged govno_creeper
  overlaps selector box/distance4, to first nodmg player. The advancement is
  impossible-trigger/manual grant. Block path does not establish nodmg tags.

## Boundaries, reuse, decisions to review

`swrg_kit_integration/items/govno_poprigun*` is a DIFFERENT bouncing kit weapon
(lifetime600), NOT this weapon's projectile. Its shared mega1_1 variant uses TNT
instead of creeper and lacks cannon's nausea/wither/marker kill. Do not import
that behavior accidentally. Generic govnoq already participates in existing
damage/crafting/rewards; it is not the weapon identity or an ammo requirement.

Reuse LbcWands input/target/firstHit/shield helpers, LbcShooters native shot
lifecycle, LbcIndirectKills owner/status/falling/explosion tracking, match epoch
cleanup and time-stop controls. Preserve current ally-player/NPC/spectator guards;
friendly mob projectile hits follow current shared contract. Do not create a
second target or projectile framework. Inline source importer/pool handling needs
care: broad unfinished shooter pools stay outside this bounded port.

Questions/suspicious legacy defects for author review before implementation:

1. Magic suppression at cast despite shooter presentation; in-flight marker is
   neither magic nor mecha. Choose intended category explicitly.
2. shieldblockable appears a typo with no datapack consumer. Should shields stop it?
3. Owner/team exclusions occur only entity prebranch; block impact can affect
   shooter/allies. Port must apply shared protection, not preserve this accident.
4. Mega resistance affects enemies too; falling blocks/explosion have no native
   owner protection. Decide whether universal local resistance is intentional.
5. Mega skill9 branch versus normal spread should be preserved if skill exists;
   nearest-player rotation and tiny-box entity effects are implementation defects
   requiring explicit geometry choice rather than silently making radius15 AoE.
6. Nonair collision/dirt replacement can overwrite blocks; use existing journaling
   and protection rules. No source block loot is requested by setblock.

Essential source files: workingitems/{govno,govno_default,govno_govnomen,
govnoselect,spell1,spell1_left,spell1_default,spell1_default_left}.mcfunction;
items/spells.mcfunction; raycastselector{,_player,_kills}.mcfunction;
lbcr/function/raycast_gavno*.mcfunction; lbcr/function/raycast_end_with_tp.mcfunction;
lbc/function/universal_pos_razb_0_5.mcfunction; the two inline loot tables;
lbc/advancement/true_advancements/shooter/shit_cannon.json.
