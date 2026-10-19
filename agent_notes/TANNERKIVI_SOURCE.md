# Tannerkivi standalone source audit

Scope: read-only audit of the legacy `world/datapacksdisabled/LBC/data/{lbc,lbcr}` datapack. This covers the standalone arcanum marked `tannerkivi:1`; the separate `stonekivi_wand` spell is outside this note.

## Author decision

Latest correction (user): «Вообще что угодно в землю кроме воздуха и неосязаемых
полностью прозрачных блоков (air/cave_air/void_air/light/structure_void)».
This overrides the source block-tag whitelist in the native port. Already-dirt
cells need no write. Native initial scans, palette filters and dirty events
share this same predicate. Historical source-tag descriptions below remain
source evidence, not current port restrictions.

Movement correction: native work now coalesces by chunk section and processes
actual new slabs, not a full-cube cursor that merely skips its intersection.
See [current implementation and budgets](STONEKIVI_PORT.md).

(user): «оставь порог. Пусть 1 тик сработает и выключится».
The native activation threshold remains 500. When the subsequent 15 upkeep
cannot be paid, the first effect pulse still occurs and the hand becomes
inactive. Its accepted terrain work may finish through the budgeted queue.
See [the native port](STONEKIVI_PORT.md); historical source routing below is
retained as evidence rather than a competing specification.

## Item and routing

- `lbc/loot_table/transformables_arcanum.json:400-496` defines a one-count repeating command block with `lbc:tannerkivi` model, tier C tooltip, translated name/lore, mana cost lore, and custom data `{craftusable:1,transformable:1,arcanum:1,arcanumt:1,tannerkivi:1}`. It is one entry in the transformables arcanum loot table (weight 10, quality 10).
- The per-player item selector checks `SelectedItem` custom data for mainhand at `lbc/function/items/arcanums/select.mcfunction:10-11`, and `weapon.offhand` for offhand at `select2.mcfunction:10-11`. `lbc/function/mobs_tick.mcfunction:4` routes each non-spectator, non-time-stopped player to `players_tick`; `lbc/function/players_tick.mcfunction:35-39` polls both slots each game tick whenever that slot contains an `arcanumt:1` item and magic is not suppressed by a non-spectator `nomagic_active` player within 15 blocks or a `stopper_magic` marker within 20 blocks. This is automatic while held; there is no right-click trigger. Suppression skips the selector, pausing mana cost and effects while retaining its active tag. Each selector dispatches to its own handler (`tannerkivi.mcfunction` / `tannerkivi2.mcfunction`). Moving Tannerkivi out of its checked slot clears its corresponding active tag; leaving the broader `arcanumt` slot also invokes `end.mcfunction` / `end2.mcfunction` to clear active state.
- Either handler activates once mana is at least 500, subtracts that activation cost in `tannerskivi_spawn.mcfunction:1-4` / `tannerskivi_spawn2.mcfunction:1-4`, then calls the shared `tannerskivi_tick.mcfunction` while mana is at least 15. Since the handler continues after activation, the first 15-mana tick is charged immediately after the 500 activation cost. `config.mcfunction:11-12` sets activation to 500 and upkeep to 15 per game tick; lines 70-71 derive 300 mana per second for the tooltip. If mana drops below 15, the matching active tag is removed.
- The active tick gives Resistance I for one second. If held in both hands, it additionally gives Resistance I for two seconds. It converts blocks tagged `#minecraft:tannerkivi` to dirt in a 7 x 8 x 7 fill centered around the holder; with the item in both hands it also attempts a 31 x 31 x 31 fill. See `tannerskivi_tick.mcfunction:1-6`.

## Projectile transmutation and advancement

- The active tick searches `#minecraft:projectiles_tannerkivi` within six blocks, excluding tags `swrg.center`, `swrg.default`, `swrg.look`, `swrg.midle`, `swrg.spawn`, `magic_academy`, `transformator`, `danmaku_bomb`, `danmaku_bomb_fast`, `crocodildo`, `crocodildo_hitbox`, `helicopter_boss`, `helicopter_boss_hitbox`, `grimoire_boss_hitbox`, and `grimoire_boss_visual` (`tannerskivi_tick.mcfunction:7-8`). Each eligible projectile is replaced at its current position with dirt, emits a break sound and particles, then is killed (`tannerskivi_tick_transform.mcfunction:1-4`).
- When the arcanum is in both hands, an additional search reaches 20 blocks (`tannerskivi_tick.mcfunction:9`). The six-block and 20-block searches use the same exclusions and transformation.
- Having an eligible projectile within six blocks grants `lbc:true_advancements/arcanums/tannerkivi` (`tannerskivi_tick.mcfunction:7`). The advancement is a visible task with an impossible trigger, parent `kiuaskivi`, toast/chat announcement, and reward function `lbc:swrg_kit_integration/gain/skill` (`advancement/true_advancements/arcanums/tannerkivi.json`). It therefore appears to be granted by the arcanum's nearby projectile check rather than by picking up the item. This is also the SWRG unlock gate for Tannerkivi (`function/swrg_kit_integration/gui/page/10000.mcfunction:14,65,95`).

## Terrain tags, Academy, and port implementation

- The standalone tick refers to `#minecraft:tannerkivi`; its source definition is `data/minecraft/tags/block/tannerkivi.json`, containing `#minecraft:solid`, `#minecraft:wool_carpets`, `#minecraft:flower_pots`, `#minecraft:beds`, lava, water, powder snow, and cobweb. The companion `tannerkivi_no_dirt` tag includes this tag and dirt, but the standalone fill itself strictly replaces only `#minecraft:tannerkivi`. The separate `tannerskivi_tick_item.mcfunction` has a one-block dirt fill against the same tag, but no caller was found in the allowed trees. `lbc/function/tick_not_timestopped.mcfunction:1` polls item entities, while `lbc/function/items_tick.mcfunction:1-4` only dispatches to `items_tick_act` when the item entity has tag `working`; no allowed source call reaches Tannerkivi's item helper. Thus no dropped-item Tannerkivi conversion cadence/filter is evidenced here.
- No standalone Academy item definition or Academy-specific Tannerkivi route was found under `data/lbc` or `data/lbcr`. The shared projectile selector explicitly excludes `magic_academy` tagged projectiles. This note does not infer Academy item behavior from the separate wand.
- Both hand slots can independently activate the shared effect: if Tannerkivi is in both hands, each selector has its own active tag and calls the shared tick. The source therefore charges 1,000 mana to start both routes and can charge 30 mana per tick; it also executes the large fill twice and repeats nearby projectile scans. The extra 20-block projectile scan only runs in the dual-hand case. The port retains two independent upkeep charges while processing aura effects once, and uses a 2,048-position/3ms work budget rather than a 31-cube scan every tick.
- `data/minecraft/tags/entity_type/projectiles_tannerkivi.json` is broader than projectile subclasses: besides arrows/fireballs/etc. it includes falling blocks, end crystals, fangs, XP entities, area clouds, primed TNT, minecart variants, boats, and wind charges; it also names item/block displays and markers. The source selector applies the listed exception tags, then the transform function kills the selected entity after placing dirt at its location. Port accepts native `Projectile` subclasses as well as physical entities named in this tag, filters visual/technical marker/display proxies, and accepts owner/allied projectiles.
- Port code is `src/main/java/me/skywars2/game/LbcTanner.java`: native Polymer repeating-command-block identity and legacy `tannerkivi:1` matching, automatic held-hand tick, strict mana admission, Resistance (20/40 ticks based on hands emitting that tick), dirt conversion with indirect-kill attribution, source advancement, and loaded-chunk-only scanning. Initial area scans and movement fringe scans are queued and bounded; dirty in-area block updates and chunk-load callbacks feed the same queue. Dirty writes are prioritized before scan cursors; already-paid pulse scans continue after hand deactivation, without ongoing aura effects. No dropped-item aura exists. Activation spends 500; on that same tick, it spends the 15 upkeep if available and remains active. If the 500 succeeds but 15 does not, that first effect pulse still occurs, then aura effects stop.
- Source writes dirt directly with `/fill` and `/setblock`, so replaced blocks do not drop items. Port wraps dirt writes with `LbcIndirectKills.trapChanges` to preserve attribution for later suffocation/fall effects.

## Exact legacy references

Paths below are relative to `world/datapacksdisabled/LBC/data/` unless stated otherwise:

- `lbc/loot_table/transformables_arcanum.json:400-496`
- `lbc/function/config.mcfunction:11-12,70-71`
- `lbc/function/items/arcanums/select.mcfunction:10-11`
- `lbc/function/items/arcanums/select2.mcfunction:10-11`
- `lbc/function/players_tick.mcfunction:35-39`
- `lbc/function/mobs_tick.mcfunction:4`
- `lbc/function/items/arcanums/tannerkivi.mcfunction:1-3`
- `lbc/function/items/arcanums/tannerkivi2.mcfunction:1-3`
- `lbc/function/items/arcanums/tannerskivi_spawn.mcfunction:1-4`
- `lbc/function/items/arcanums/tannerskivi_spawn2.mcfunction:1-4`
- `lbc/function/items/arcanums/tannerskivi_tick.mcfunction:1-9`
- `lbc/function/items/arcanums/tannerskivi_tick_transform.mcfunction:1-4`
- `lbc/function/items/arcanums/tannerskivi_tick_item.mcfunction:1-2`
- `lbc/function/items_tick.mcfunction:1-4`
- `lbc/function/tick_not_timestopped.mcfunction:1`
- `lbc/advancement/true_advancements/arcanums/tannerkivi.json:1-27`
- `lbc/function/swrg_kit_integration/gui/page/10000.mcfunction:14,65,95`
- `data/minecraft/tags/block/tannerkivi.json`
- `data/minecraft/tags/block/tannerkivi_no_dirt.json`
- `data/minecraft/tags/entity_type/projectiles_tannerkivi.json`

