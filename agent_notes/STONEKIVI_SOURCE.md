# Stone + Tannerkivi source audit

Scope: read-only audit of the legacy LBC datapack at `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`. The result is the crafted `stonekivi_wand` (`loot_table/tuntijaa_stone_tanner.json`); its ingredient donor is the standalone Tannerkivi (`custom_data {tannerkivi:1}`), and Stone is `ststone:1`.

## Author decisions and port

Comets (user): «сохрани вообщё всё, кроме технических (жителя на бедварсе то есть)».
The port retains item, projectile and allied entity positions, while excluding
service entities and observers. The caster stays excluded as in the source.
See [the native port](STONEKIVI_PORT.md) for corrected first-hit dispatch,
used-hand trial, native crafting and CPU-budgeted placement. The source's
historical broad proximity hit tests below describe legacy code, not the
native hitbox collision used by the port.

## Entry points and cost

- Ground crafting dispatch: `lbc/function/other/craft_ground.mcfunction` checks `ststone:1` plus nearby item `tannerkivi:1`, then calls `other/sauvojen_tuntija/crafted_tannerstone`. That helper drops `loot lbc:tuntijaa_stone_tanner`, then kills the nearest Tannerkivi item and the crafting marker. Inventory crafting routes to `crafted_tannerstone_inv`, which drops the same result and decrements one item from each matching ingredient slot. Relevant refs: `craft_ground.mcfunction:20`; `crafted_tannerstone.mcfunction:1-5`; `crafted_tannerstone_inv.mcfunction:1-13`.
- The wand costs **2500 mana per cast**, set in `lbc/function/config.mcfunction:89`, debited by both `stonekivi/click.mcfunction:1-5` and `stonekivi/click_upgraded_pre.mcfunction:10-14`. `tannerkivi` donor activation is **500 mana**, then **15 mana/tick** (`config.mcfunction:11-12`; `items/arcanums/tannerkivi.mcfunction:1-3`; `tannerskivi_tick.mcfunction:6`). These are activation/usage costs, not shop prices.
- Base result item and its dynamic mana lore/model/tags: `lbc/loot_table/tuntijaa_stone_tanner.json:1-82` (tier A, model `lbc:stonekivi`, custom data includes `transformable:1,staff:1,raycastspell:1,stonekivi_wand:1`).

## Cast and modes

- Runtime handlers: `lbc/function/workingitems/awaken_tuntija_wands/stonekivi/`. Unupgraded `click.mcfunction` directly starts default `loop`, then charges. Upgraded `click_upgraded.mcfunction` routes sneak-click to four-mode cycling and ordinary click to `click_upgraded_pre`; `_left` uses left-hand cycling and reads the offhand item's mode. The pre-handler reads mode from selected item/offhand and dispatches 1 default, 2 earth, 3 sun, 4 comets; cost is still 2500.
- All ray loops advance by `^ ^ ^0.5`, stop on a non-air block or nearby mob, and suppress continued travel when a nearby player is `nomagic_active` or a `stopper_magic` marker is within 20. Default `loop.mcfunction` range is 300 half-block steps (150 blocks), with mob detection radius 15; it places five centered sphere layers: `stone_sphere_r10` (21?), `iron_sphere_r8` (17?), `obsidian_sphere_r6` (13?), `deepslate_sphere_r4` (9?), `bedrock_sphere_r2` (5?), (`end.mcfunction:1-10`).
- Earth mode (`loop_earth.mcfunction`) reaches 500 steps (250 blocks), ends at a block/mob within 10, and places `lbc:earth` at `~-22 ~-22 ~-22` (`loop_earth_end.mcfunction:1-6`). The legacy NBT is 43x43x43 with 41,147 stored blocks, so the negative 22 offset centers the 43-wide structure around impact.
- Sun mode (`loop_sun.mcfunction`) reaches 500 steps and uses lava/magma particles; on mob hit it shifts to motion-blocking ground and calls `loop_sun_end`. The live path sets storage `command="magmify"`, then immediately returns through `loop_sun_end_nerfed1` (whose macro body is `$$(command)`), so the large `lbc:sun/sun-*` template sequence below is dead code. Macro command dispatch runs the registered legacy Paper `/magmify` implementation. That service captures the 26-connected non-air structure at the impact position and transforms it; successful completion can add lava on exposed faces and impacts surface entities for 40 damage with strong slowness/knockback; the Java service uses the executing command entity as the damage source. See legacy `plugins/luckywars/src/main/java/me/luckywars/MagmifyService.java` and `agent_notes/COMMAND_IMPLEMENTATIONS.md` (Asynchronous map transformations).
- Comets mode (`comet.mcfunction`) reaches 500 steps, hit radius 10, places `bedrock_sphere_r2` centered at impact (`comet_end.mcfunction:1-8`). The source also contains the default mode upgrade prompt: at impact, if selected wand has `upgradable:1`, player within 10 and target block is non-air, it invokes Academy `can_upgrade` (`end.mcfunction:9`).
- Academy check function only prints `upgrade_condition_stonekivi`; upgrade prints `upgrade_succesful_stonekivi` and applies `lbc:tiers/x` to main hand (`other/magic_academy/can_upgrade/stonekivi_wand.mcfunction:1`; `other/magic_academy/upgrade/stonekivi_wand.mcfunction:1-2`). Mode cycle helpers are generic `other/magic_academy/change_mode_4{,_left}`.

## Tannerkivi donor behavior (brief)

`items/arcanums/tannerkivi.mcfunction` starts an active state after 500 mana, charges 15/tick while active, and drops active tag when below tick cost. `tannerskivi_tick.mcfunction` grants resistance, fills a 7×8×7 region with dirt replacing `#minecraft:tannerkivi`; holding Tannerkivi in both hands expands this to a 31×31×31 fill. It also turns nearby tagged projectile/entity types into dirt and kills them. Block tag membership is `minecraft/tags/block/tannerkivi.json`: `#minecraft:solid`, carpets, pots, beds, lava/water/powder snow/cobweb. `tannerkivi_no_dirt.json` swaps base block tag to `solid_no_dirt`.

## Source issues / author clarification candidates

- **Sun mode has an active destructive transformation with dead template fallback.** The macro command is `magmify`; the live Java-backed behavior is connected non-air map transformation plus possible lava and entity damage. The handcrafted `lbc:sun/sun-*` placements after `return run` are unreachable. Preserve the active behavior unless the author explicitly wants to restore the old large template build; do not mistake it for a harmless nerfed marker path.
- **Academy condition text is the only stone-specific gate.** The generic `other/magic_academy/interacted_2.mcfunction` dispatches this handler for the selected wand; this handler only prints `upgrade_condition_stonekivi`. No stone-specific validation or payment appears in this datapack; inspect the translation text before choosing whether the port needs any additional gate.
- **Default impact upgrade trigger checks mainhand only**, whereas upgraded handler explicitly supports offhand. If left/offhand casts are intended to upgrade too, `end.mcfunction:9` is a source-side limitation.
- **Comet entity-hit placement fans out.** `comet_end.mcfunction:1` runs the sphere placement at every entity in radius 15 with only `tag=!raycaster`; it can include allies, items, markers and other non-mob entities and has no limit. Line 2 also places at the ray context. Confirm whether this fan-out is intentional; it can stamp multiple spheres per hit plus the ray impact sphere.
- **Cast admission is external to the handler.** Both cast handlers subtract mana unconditionally and do not test sufficient mana themselves. Generic spell dispatch is expected to gate on mana; preserve/verify that edge before migrating.

