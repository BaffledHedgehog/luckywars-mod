# Shulker armor bounded source review — 27 October 2026

Read-only source research; no build, server action, original-server edit or RP edit.
Read project `AGENTS.md`, this folder's `AGENTS.md` and `RESOURCE_PACK.md`.
Paths below use `S = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`,
`P = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java`, and
`R = C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Line references are to inspected source, not evidence of a running port.

## Author correction (user, 27 October 2026)

> антимагический глушитель так же должен глушить шлем и снятие левитации поножами, это я глэк в легаси забыл так сделать

Target behavior therefore adds Magic Stopper checks to the helmet and leggings
as well. The table below records the original omissions, not the desired port.

## Complete worn behavior

| Piece / combination | Exact source and behavior |
| --- | --- |
| Helmet | `S/function/players_10s.mcfunction:2` calls `workingitems/shulkershell.mcfunction:1-6`. `tick10s.mcfunction:1,3` really schedules **200 ticks**, excluding spectator, Nexus and Imprinted. Bullet spawns at wearer +Y3, initial Motion=(0,5,0), Steps=40; Owner copied from wearer; Target is nearest eligible player through `@p`, excluding wearer and same team. **No direct stopper gate**. Source lore says random player, but selector is nearest. |
| Chest | `S/function/items/damaged.mcfunction:7`: chance20 predicate, chest `shulkshell:1`, no `stopper_magic` marker within20. `armor/shulker_teleport.mcfunction:1-6` gives reverse-portal particles / enderman teleport sound, spreadplayers centered on wearer with maxRange20, then portal particles / same sound. Nether ceiling120; ordinary dimension uses normal ceiling. No cooldown. |
| Leggings | `S/function/players_2t.mcfunction:11` clears **all Levitation**, no magic-stopper check. `tick2t.mcfunction:1,3` -> `mobs_2t.mcfunction:3` proves actual **2-tick** scheduler and spectator/Nexus/Imprinted exclusions. Tag added/removed by `items/changed_inventory/sel.mcfunction:72,80`. Global Time Stop clears the scheduler (`time_stop.mcfunction:19`); this particular leg hook has no individual `time_stopped` check. |
| Boots | No additional active hook found. Original item attributes provide +1 knockback resistance. |
| Four pieces | `items/changed_inventory/sel.mcfunction:41-44`: remove old `shulker_armor` movement modifier, count armor slots and set/remove full-set tag. `players_tick.mcfunction:16` invokes `armor/set_bonus/shulker4.mcfunction:1-3` each tick, unless stopper_magic marker within20. Gives ResistanceII for20ticks, adds **-10 add_multiplied_total movement speed**, and calls noarrows. |
| Full-set movement | `players_tick.mcfunction:6` calls `items/armor` when jump1>=1; `load.mcfunction:3` defines jump1 as **minecraft.custom:minecraft.jump**. `items/armor.mcfunction:7-8` spreads at **local ^ ^ ^5**, maxRange3; Nether ceiling120; same stopper_magic gate. `items/armor.mcfunction:18` resets jump1. **Real jump, not F/offhand swap.** Corrects the historical shorthand in `NEXT_ENCOUNTER_SOURCE_2026_10_27.md:68`. |

`S/function/other/artifact/noarrows.mcfunction:1-3` applies cloud20, speed0.3 and fire-extinguish sound at every entity in **#minecraft:arrows within16**, then kills them. No owner/team, embedded/in-flight or projectile-age exclusion; no broad projectile group, no shulker-bullet destruction. This helper itself has no magic gate; the full-set caller supplies one. `mobs_tick.mcfunction:4` excludes individual time_stopped players from the ordinary full-set tick branch.

Chest trigger is **minecraft:entity_hurt_player** (`S/advancement/cocksuckers.json:4`), with only spectator/Nexus/Imprinted exclusions and no damage-value or blocked condition; reward calls items/damaged at line46 and that function revokes at line1. Consequently a replacement must preserve the native criterion, including shield/environmental/fatal trigger semantics, rather than requiring positive final HP loss or melee damage.

## Acquisition and presentation leaves

Four original leaves are `S/loot_table/neko_set_shulker/{helmet,chestplate,leggings,boots}.json`. All are leather armor, durability900, toughness3, original `lbc:armors/shulker_*` model and `lbc:shulker` equipment asset, name #8942B3/nonitalic. Armor values3/8/6/3. Boots add1 knockback resistance. Tooltip tiers B/B/C/A. No enchantment function. Custom data is `transformable:1`, the respective slot flag, and `shulkshell:1` (`helmet/chestplate:115`, `leggings/boots:108`). Preserve complete original component trees, modifier identifiers and lore; do not reconstruct formatting from this summary.

`neko_set_shulker.json:4-20` chooses one equally weighted piece; `_pre.json:4-8` rolls five selections; `_full.json:4-35` returns all four pieces. All seven leaf/root files existed in the new mod's source resource tree when inspected; this is not proof of JAR packaging. The parent agent separately checked `build.gradle` and actual installed a4683fe2 JAR: only the one-piece root and four slot leaves were packaged, while `_pre` and `_full` were absent. Adding those two exact roots is required for the complete tested reward leaves. Original continuations:

- `S/function/ultra_reforge.mcfunction:29` and native `src/main/java/me/skywars2/game/LbcBolts.java:105` route shulkshell into the one-piece table.
- `S/loot_table/transformables_{helmet,chestplate,leggings,boots}.json:32` select the appropriate piece.
- `S/loot_table/nekolucky_items.json:4,8-9` chooses `_pre` at weight10; `6_sets.json:4,8-9` has six full-set selections, Shulker weight10. These are references, not proof that their entire parent acquisition families are ready.
- Separate legacy helmet appears at `S/loot_table/legacy_luckyblock.json:3512`, with only `shulkshell:1` custom data. Worn hooks must match the flag, not demand transformable/slot flags. Armor advancements are `S/advancement/true_advancements/armor/{shulker,shulker_full}.json` and their existing unlock modifiers remain separate progression leaves.

RP read-only verification: `R/assets/minecraft/lang/{ru_ru,en_us}.json:128-130,793-802` contain existing names, piece lore and four set-bonus lines. English line801 explicitly says jump; no F addition is required. For all four pieces, `R/assets/lbc/items/armors/shulker_*.json:1` -> `assets/lbc/models/item/armors/shulker_*.json:1` -> `assets/minecraft/textures/item/armors/shulker_*.png`; all referenced textures exist. `R/assets/lbc/equipment/shulker.json:1` resolves existing `assets/lbc/textures/entity/equipment/humanoid/shulker.png` and `humanoid_leggings/shulker_leggings.png`. Existing tooltip sprites are present. No new RP asset or duplicate translation key is indicated by this review. This is source/resource resolution, not a visual client check.

## Extensions, suspicious behavior and native reuse

Bounded Java searches of original Paper and Ignite sources found only Paper armor-tag recomputation: `P/me/luckywars/swrg/SwrgNativeService.java:2983,2998,3008`; no added Shulker ability was found. This negative search is not a claim about every embedded resource or runtime path.

Potential source defects to handle explicitly: helmet uses a lingering nearest `shell` tag rather than retaining the newly spawned entity reference; failure to find a target still creates a bullet. Its target is nearest despite random-player lore. Chest's later particle/sound commands retain the function execution position after spreadplayers; the current port intentionally preserves both effects at that original position. Repeated movement modifier addition can fail because the identifier already exists; removal occurs on inventory change rather than every tick, and also on `S/function/leave_death.mcfunction:427`. Keep a single native modifier and remove it when full-set admission ends, without creating a stacking modifier or removing unrelated speed effects. Full Resistance can persist up to20ticks after suppression/removal because source grants a duration.

Best shared reuse: `LbcArmorSets` owns equipped-only admission and active scheduling; existing native jump hooks in `LbcJumpArtifacts`/`LbcOverloadArmor` avoid packet/F inventions. `IncomingArtifactCriterionMixin.java:21-24` already observes the exact ENTITY_HURT_PLAYER criterion; `LbcIncomingArtifacts.java:74-84` preserves pre-death admission. `LbcMagic.java:653-705` already owns indexed arrow interception and erasure; extend holder eligibility for an admitted Shulker set with its own suppression gate while retaining artifact policy. `LbcSurfaceTeleport.java:15-33` is the shared finite resident-column spreadplayers-style search. Existing `LbcCreatureDrops` has managed native Shulker bullets and shared attribution/lifecycle; armor targeting must remain enemy-only, unlike the wild trap's target policy (`LbcCreatureDrops.java:156`). Do not turn failed targeting into anonymous/unowned damage.

Port work may proceed concurrently in production files; this note records source contract only and does not certify the port, build, installation or runtime tests.
