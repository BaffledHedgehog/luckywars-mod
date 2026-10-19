# Kiuaskivi and Sun Arcanum — legacy source audit

Scope: read-only audit of `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`. Statements below describe datapack source behavior; no live server run was used to validate it.

## Author decisions for the native port

Sun damage (user): «Сохранить: урон только с двумя Sun».
Kiuaskivi branches (user): «Сохранить обе независимые руки».
The native port keeps per-hand maintenance and per-paid-hand 5% rolls.
Damage attribution uses the real arcanum holder under the general author
contract; the old nearest-player selector is not a required behavior.
See [native behavior and verification links](FIRE_KIUAS_PORT.md).

## Kiuaskivi

- `items/arcanums/select.mcfunction:7-8` dispatches a mainhand item marked `custom_data.kiuaskivi:1` to `kiuaskivi`; `select2.mcfunction:7-8` does the same for offhand into `kiuaskivi2`. Each hand has its own active player tag (`kiuaskiviactive` / `kiuaskiviactive2`) and calls its own spawn branch when inactive (`kiuaskivi.mcfunction:1-3`, `kiuaskivi2.mcfunction:1-3`). Spawn deducts 1000 mana and adds that branch's active tag (`kiuaskivi_spawn.mcfunction:1-4`, `kiuaskivi_spawn2.mcfunction:1-4`). Config sets 10 mana per tick and 1000 activation (`config.mcfunction:14-15`).
- Every active-hand branch invokes the same `kiuaskivi_tick`: fire resistance 30 seconds; fill a player-centered 3×4×3 region, first replacing `#minecraft:kiuaskivi_to_air` with air using `strict`, then replacing `#minecraft:kiuaskivi_to_fire` with fire; particle; deduct 10 mana (`kiuaskivi_tick.mcfunction:1-6`). The 5% `big_fire_fill` check is inside this shared tick and requires Kiuaskivi in both hands (`:4`), so each active branch that runs it gets its own roll; big fill is ±15 (`big_fire_fill.mcfunction:1-2`).
- Doublehand behavior is conditional on mana at each branch check. The mainhand wrapper runs before the offhand wrapper, and activation is followed immediately by a maintenance check/tick in each wrapper. So the offhand activation condition sees mana remaining after the entire mainhand branch (including its 10-mana tick if affordable); its own tick then sees the post-activation balance. Both hands can run work only when those sequential checks pass. Each active hand is charged separately; this is not a single 1000+10 shared activation cost.
- The air replacement tag contains water, snow, ice, water/powder-snow cauldrons and cobweb (`minecraft/tags/block/kiuaskivi_to_air.json:1-11`). The fire tag includes mineable/axe, mineable/hoe, replaceable, flowers, saplings, wool, campfires and wool carpets (`.../kiuaskivi_to_fire.json:1-13`); the broad mineable tags include solid blocks. The second fill sees the air produced by the first. Native 26.3 #minecraft:replaceable includes air, so the second fill converts that air to fire too.

## Sun Arcanum

- The held-item dispatch is mainhand `sun` and offhand `sun2` (`items/arcanums/5true/select.mcfunction:1-2`, `select2.mcfunction:1-2`). Activation costs 1500; active maintenance costs 100 each 5-tick selector pass, i.e. configured 400 mana/sec (`config.mcfunction:16-17,76-77`; `sun.mcfunction:1-3`; `sun2.mcfunction:1-3`; `sun_spawn.mcfunction:1-4`; `sun_spawn_left.mcfunction:1-4`). As with Kiuaskivi, active state is tracked separately per hand, and mana is shared.
- Both wrappers call `sun_tick` when active. The offhand wrapper does **not** call the existing `sun_tick_left`; that function is present but no caller was found in the datapack. The current `sun_tick` grants fire resistance 120 seconds, emits flame particles, places template `lbc:the_sun` at `~-13 ~-13 ~-13`, plays sound, deducts 100 mana, and processes projectiles (`sun_tick.mcfunction:1-8`).
- Direct damage occurs only on line 3 of `sun_tick`: it first requires the `sun_arcanum:1` item marker in **both mainhand and offhand**, then targets `@e[distance=0.1..15,type=#mobs,tag=!spectator,predicate=!lbc:same_team]` and applies 10 `on_fire` damage. Thus offhand-only Sun can activate and run the general tick effects but cannot pass the damage hand check. With both hand branches active, both wrappers can call this same tick, so the same eligible mobs can receive the damage command twice in a pass if both sequential mana checks succeed. Damage source is `@p[gamemode=!spectator,tag=sun_active,distance=..1]`, i.e. nearest eligible active-mainhand Sun player within 1 block, not explicitly the tick's `@s`; attribution/behavior under overlapping users is not established from the command alone.
- `sun_tick_left.mcfunction:1-5` duplicates the effects, template placement, projectile processing and cost but omits the damage command. It is currently orphaned according to a reference search; whether it was intended as offhand-only behavior is uncertain.
- Projectile processing selects `@e[type=#minecraft:projectiles,distance=8..25]` and at each selected projectile runs `setblock ~ ~ ~ minecraft:lava` followed by `kill @s` (`sun_tick.mcfunction:8`, `projectile_boom.mcfunction:1-2`). The datapack tag explicitly includes `#arrows`, dragon fireball, egg, wither skull, ender pearl, falling block, fireball, firework rocket, fishing bobber, llama spit, splash/lingering potions, shulker bullet, small fireball, snowball, trident, `tnt`, wind charge and breeze wind charge (`minecraft/tags/entity_type/projectiles.json:1-22`; `#arrows` delegates to Minecraft's arrow tag). The tag file does not list raycasters, marker/display entities, boats, experience entities, area-effect clouds, or TNT minecarts; those appear in the separate `projectiles_double` tag, which Sun does not use.

## Item source and Sun template

- Kiuaskivi is an entry in `loot_table/transformables_arcanum.json` and is created as a repeating command block with Kiuaskivi model/name and custom data `{craftusable:1,transformable:1,arcanum:1,arcanumt:1,kiuaskivi:1}` (entry around lines 299-392). `loot_table/transformables_arcanum_all.json` delegates to this table; the corresponding transformables selection table is therefore the source of standalone Kiuaskivi. Sun is separate: `loot_table/tuntijaa_fire_kiuas.json` creates a repeating command block with item model `lbc:the_sun` and custom data `{transformable:1,arcanum:1,arcanum5t:1,magicwand:1,sun_arcanum:1}` (lines 7-95). `loot_table/transformables_arcanum_all.json` points to that Fire/Kiuas table as a possible roll; crafting dispatches that table through the Kiuas-wand recipe functions.
- Parsed legacy `structure/the_sun.nbt`: size `[27,27,27]`; palette has 3 entries: orange stained glass, air, and lava level 0. It has 6,683 block records: 21 orange stained glass, 1,368 lava, and 5,294 explicit air entries; `entities` is empty and there are no block-entity NBT records. Source file is the single template `structure/the_sun.nbt`; the many `structure/sun/sun-*.nbt` files are separate and are not referenced by `sun_tick`.

## Unresolved behavior questions for porting

1. Is doublehand activation intentionally two independent 1500/100 mana branches, with duplicate Sun template/projectile passes and potentially two 10-damage hits, or should paired hands share one activation/tick?
2. Should offhand-only Sun keep the current effects and projectile conversion without damage, or was the unused `sun_tick_left` intended for that branch?
3. Should damage be credited to the executing player, or should the source's nearest tagged-player selector behavior (including overlap ambiguity) be preserved?
4. Are the broad Kiuaskivi fire replacements (mineable blocks including solids) intentional, and should doublehand multiply the 5% large-fill roll as source currently does?

