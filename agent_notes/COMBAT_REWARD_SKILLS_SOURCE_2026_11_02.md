# Combat/reward skills source audit — 2 November 2026

Bounded read-only audit. Only this new note was written; no source, RP, runtime, build or git edits. Existing notes were preserved. Prior skills1/12/13 are completed/installed0971bd4f and their resolved questions are outside this audit.

Project: `E:/servers_mew/skywars2_last/luckywars-mod`.
Legacy `L`: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Actual RP inspected read-only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json`.

Recommend **14/16/18**, using owner-local native bow production and exact native kill/death-drop events. Defer7 because its armor-pool contract has substantial independent ambiguities. Root has opened author questions for suppression, Soul Hunter absorption scope and actual-victim loot scope. Nothing below constitutes author approval or runtime qualification.

## Menu and author names

Canonical legacy page/selection: `L/function/swrg_kit_integration/gui/{page,select}/10000.mcfunction`; presentation tables `L/loot_table/gui/page/10000/{6,13,15,17}.json`.

| ID | Slot | Actual RP name/key | Gate | Locked lore key |
|---|---:|---|---|---|
|7|6|Незапятнанная броня / `lbcskill.6`|`lbc:true_advancements/armor/paladin`|`lbc.adv.armor.paladin.lore`|
|14|13|Пулемётчик / `lbcskill.13`|`lbc:true_advancements/bows/minigun`|`lbc.adv.bow.minigun.lore`|
|16|15|Пожиратель душ / `lbcskill.15`|`lbc:true_advancements/hoes/death_skytche`|`lbc.adv.hoe.death_skytche.lore`|
|18|17|Развалился / `lbcskill.17`|`lbc:true_advancements/hoes/socializm`|`lbc.adv.hoe.communism.lore`|

14 uses `lore.lbcskill.13.0` through `.3`;16 uses `lore.lbcskill.15.0` through `.3`;18 uses singular **`lore.lbcskill.17`**. Existing author translations suffice. Preserve original typo spellings in identifiers. Do not expose7 incidentally.

## 14 — Пулемётчик

Source chain:

- `L/advancement/bow_pulling.json`: `minecraft:using_item`, actual `minecraft:bow`, outside Nexus and Imprinted -> `function/items/bows/charge_add.mcfunction`.
- `charge_add` increments `bow_charge` unless global `time_stopped>=1`, grants the source infinite-arrow replenishment on first charge if applicable, and dispatches `swrg_kit_integration/game/skills/bow_charge` while global skills enabled.
- `bow_charge` admits skill14 only with at least one **item id minecraft:arrow** anywhere in Inventory; tipped/spectral-only inventory does not qualify. Custom-data variants of normal arrows do qualify. Calls `pulemet`.
- Pulse factory `pulemet_shoot`; exceptional catch-up `pulemet_shoot_rec`. No skill-local stopper/nomagic/strong-artifact checks.

Exact cadence: let `q=bow_charge`, `s=bow_charge_sub`, `d=20-s`, `threshold=d-1`. If d==0, set d=1 **after** calculating threshold. Compute score remainder `r=q%d`. If r==threshold, shoot once; if r>threshold, recurse: shoot, subtract100 from r, continue while r>threshold. Each shot increments **s**, not q. There is **no clamp for d<0**. Thus after twenty automatic shots the divisor enters zero/negative territory, and after further shots the recursive path can produce increasing pulses per callback. A simple `max(1,20-s)` one-shot cadence is a sensible bounded correction but is not literal source; obtain/record author resolution before replacing this arithmetic. Shared scoreboard scratch values may also be clobbered by nested trinket functions; preserve deliberate behavior, not global scratch collisions.

Counters reset together in `L/function/items/bows.mcfunction:17-18` after ordinary release and in `items/bows/charge_query.mcfunction:3-4` when SelectedItem is no longer `minecraft:bow`. No decay exists. Charge-query is run from player tick while charge>=1. Mainhand-only query is a legacy limitation despite using_item itself accepting a used bow. Native implementation should key a draw to actual player/used-hand/stack identity; clarify offhand policy through established project use rules. `other/twitching_bow` and `other/twitching_crossbow` can set charge30; `other/eatables/kfc_chick/all_up_scores_no_mana` also increments charge. Existing native KFC source counter includes bow_charge; avoid silently divorcing a replacement counter from already-supported counter mutation semantics.

Automatic shot properties:

- Summon **fresh ordinary arrow** at eyes, tags `pule_arrow,killedarrow`, Owner actual shooter UUID. No copying of supplied normal-arrow custom components, tipped effects, bow enchantments, custom-bow identity or critical flag in this factory. `pule_arrow` is removed after initialization; `killedarrow` remains. Native default summoned arrow damage/pickup applies unless modified by explicit traits.
- Invoke `items/bows_get_trinket` per automatic shot before motion initialization. Its8 source trinkets are blaze, ghast, penis_gun, bedrcock, volos, cocktail, fourier, rocket_spell. Use existing trinket service once per successfully published automatic pulse; avoid counting the same shot again through a native release hook.
- Speed `(q+200)*.005` =1+.005q. Source sets speed10 only when q+200>=10000; it therefore exceeds10 at q>1800 until abruptly dropping at q9800. This is a second source discontinuity, not a normal min(10,speed) cap. Clarify if reachability remains after approved cadence limits.
- Motion helper `raycast_vpered_storage` quantizes look-vector components through data-get*10000 and *.0001, then `rand_motion_razbros_storage` independently adds integer-uniform XYZ jitter in `[-max(2000-q,0),+max(2000-q,0)]*.0001`. It does not use native Gaussian inaccuracy or inherit shooter momentum. Model rotation source initially negates player yaw/pitch; native arrow orientation should follow actual motion as existing shooter factories do.
- Each automatic pulse independently has50% chance to clear one normal arrow; this is independent of Infinity/creative ammo handling. Read source normal-arrow gate again if a batch consumes its last arrow; do not manufacture indefinite ammo.
- If levelups globally enabled: fast_charge multiplies quantized motion1.3 then `reduce_motion_to_10x`; strong_charge multiplies damage1.25; strong_charge_1 adds .5; gravitas sets NoGravity. These are shared traits, not ordinary enchantment copying.
- This auto factory **does not** invoke ordinary custom-bow branches in `items/bows`: explosive/double/double_old/onetnt/shulker/shulker_new/railgun/shish_gun/madoka are release-time branches. Actual final release still executes normal bow behavior and resets counters. Automatic pulses should not suppress or repeatedly trigger that final release path.

Native reuse evidence: existing `LbcShooters` owns concrete Arrow factories, source velocity quantization/orientation (`orient`), registered current epochs, narrow active sets and time-stop preparation. Existing `TrinketBowMixin` counts only real successful ProjectileWeaponItem release, `LbcTrinkets` owns trinket behavior, `LbcQuiver`/`QuiverNativeMixin` own exact native custom ammo effects. No existing general native bow-draw queue was found in bounded searches; create a narrow actual-use callback/owner queue instead of selecting global nearby arrows. Keep automatically generated plain ammo distinct from supplied special ammo, retain final-release native arrow ownership/physics, and avoid world scans. Snapshot active draw counters and queues using established checkpoints if work can cross rewind boundaries.

Open14 decisions: MAG/TECH/strong-artifact policy, negative-divisor/recursive cadence correction, source speed discontinuity, intended main/offhand draw support. Already-resolved prior skills need no questions.

## 16 — Пожиратель душ

`L/advancement/player_killed_entity.json` has native player_killed_entity with **no victim-type restriction**, outside Nexus/Imprinted. Rewards `function/items/player_killed_entity` -> globally skills-enabled `swrg_kit_integration/game/skills/player_killed_entity` -> legacy-disabled uniform26 `other/attributes/random_attrs_up`. Thus player victims also qualify for the random roll. Separately `advancement/player_killed_player.json` adds player-victim restriction -> `function/items/player_killed_player` -> `skills/player_killed_player` -> `soul_hunter_eat_player` for skill16. A player kill receives both branches. No source jammer checks. Preserve actual credited native killer and exclude protected match roles rather than relying on a nearby death selector.

Positive random helper uses uniform integer1..26, one permanent BASE mutation. For a row `(scale,step,out)` the exact source result is scoreboard-scaled base, integer addition, then output multiplication; use `LbcAttributeChanges` so quantization/saturation/integer overflow policy stays coherent with existing qualified base changes. Native `LbcRandomStatLevelup` is **not** equivalent:21 choices, two draws, transient modifiers, different amounts. Native `LbcParasitePenalties` is negative26 and also not a drop-in positive table.

| Draw | Attribute | scale | step | out |
|---:|---|---:|---:|---:|
|1|armor|1|1|1|
|2|armor_toughness|1|1|1|
|3|attack_damage|10|1|.1|
|4|attack_knockback|10|1|.1|
|5|attack_speed|5|1|.2|
|6|block_break_speed|10|1|.1|
|7|block_interaction_range|5|1|.2|
|8|burning_time|10|-1|.1|
|9|entity_interaction_range|5|1|.2|
|10|explosion_knockback_resistance|10|1|.1|
|11|fall_damage_multiplier|10|-1|.1|
|12|jump_strength|1000|1|.001|
|13|knockback_resistance|10|10|.1|
|14|luck|1|1|1|
|15|max_health|1|2|1|
|16|mining_efficiency|3|1|.333333333|
|17|movement_efficiency|10|1|.1|
|18|oxygen_bonus|10|1|.1|
|19|safe_fall_distance|5|1|.2|
|20|scale|100|1|.01|
|21|sneaking_speed|10|1|.1|
|22|step_height|10|1|.1|
|23|submerged_mining_speed|20|1|.05|
|24|sweeping_damage_ratio|20|1|.05|
|25|water_movement_efficiency|10|1|.1|
|26|movement_speed|100000|100|.00001|

Source helper calls corresponding `other/attributes/*_update` macros to set base values. Some leaf functions have authored sound/particle/advancement side effects (e.g. armor_toughness emits levelup,100 iron particles and revokes armor_toughnessattr); decide shared feedback deliberately, do not confuse arithmetic equivalence with exact full leaf behavior. Another legacy positive-helper caller is `other/imba/cunny_slab/cunnys/5_plapping.mcfunction`; a shared native positive26 helper is appropriate without expanding this batch to that other power.

Absorption helper `skills/soul_hunter_eat_player` adds **only seven** donor base attributes: armor/toughness/attack_damage quantized*.1, luck integral, knockback_resistance quantized*.001, max_health donor base*.2 stored integral, movement_speed donor base*50 then *.001 (5%). Killer own armor/toughness/attack_damage also quantized*.1, max_health/luck integral, knockback/movement*.001. Attack speed excluded. Actual donor source is `@a[gamemode=!spectator,scores={death=1},limit=1]`: global selector can absorb the wrong simultaneously dead player. Bind exact victim and snapshot its bases before existing death cleanup. RP lore claims all base attributes except attack speed and reduced movement, so **seven actual source fields versus all available bases** is an author question already opened by root. Do not silently infer new maxHP percentage or amount for modern attributes.

Native integration: `SkyWars2` AFTER_DEATH dispatches services then calls `GameController.died`, which clears player/loadout state. Native kill criteria or exact death actor context can bind recipient and victim without searching players. Capture victim base values before mutation/reset; apply one random draw per actual credited kill and one player absorption once. Follow `LbcIndirectKills` for actual credit/authority (including deliberately supported indirect kills) rather than only source.getEntity. Real native drop/removal that skips death must not invent kill rewards.

## 18 — Развалился

Same globally enabled player_killed_entity advancement path above: skill18 adds tag `dupesssr`, schedules `skills/loot_dupe` in1t. `loot_dupe` dispatches each nonspectator tagged player at their position then removes the tag globally. `loot_dupe1` scans **all loaded Item entities** with Age0 and Age1, no distance, victim, owner or dimension-local loot attribution beyond command execution dimension. Each selected original calls `loot_dupe_at`: summon fresh stone ItemEntity at exact original position, copy only its **Item stack** (full item data/count), remove temporary tag. Original Age/motion/pickup delay/health/ownership are not copied. XP is never selected. Dropped equipment, loot-table items and player inventory item drops are included if selected by age; there is no special-item exclusion here.

Multiple killers can repeatedly duplicate the same original/newborn stacks. First pass creates new Age0 children; subsequent owners can select them; Age1 second pass also includes a distinct birth cohort. This is global incidental duplication, not a reliable twice-only victim reward. Root has asked author to confirm actual-victim-only policy. Once confirmed, capture the exact victim's actual native death ItemEntity publications and duplicate each original stack once, preserving complete components/count. Do not reroll loot tables (changes rolls/conditions/custom stacks), multiply XP, include unrelated nearby items, or double duplicate-generated children. Native death inventory/equipment and custom mob drops should route through same causal death scope where appropriate; hook actual spawn success and narrow owner identity rather than global Age scans. A bounded source list/queue is preferable to world polling. Account for keepInventory, mob loot rules, cursed/special native drops, canceled publication and no-death discard paths; preserve their source/native rules.

`SyringeMobAccess` already injects `dropAllDeathLoot`; `OpAnvilDropMixin` observes `spawnAtLocation`, illustrating existing precise native hooks. AFTER_DEATH alone may be too late to surround all native loot publication, so verify pinned drop/event ordering before selecting scope. Keep scope stack/nesting-safe for chain kills; mark duplicates or publish outside capture to prevent recursive multiplication. Source delayed1t suggests separating capture from later duplication if time-stop/lifecycle policy demands it; do not retain stale player bodies across respawn/rewind.

## 7 — deferred Незапятнанная броня

Read-only source roots `skills/armored_change_inv`, `armored_damaged`, `armored_damaged_alt`, `armored_damaged_yes`, `armored_damaged_yes_toughness`, `armored_damaged_{1..4}`, `armored_damaged_toughness_{1..4}` and both storage macros. Dispatch on inventory_changed and damaged. No source jammer checks.

Inventory routine reads explicit worn attribute-modifier armor amounts*10; any piece>2 produces infinite ResistanceV; otherwise **clears all Resistance** indiscriminately. Damage routine first inspects toughness*40; if any positive, consumes toughness, otherwise armor*20. `blocked_damage` is registered `minecraft.custom:damage_resisted` in load.mcfunction219, not raw HP loss. Both branches integer-divide it by4 and subtract that share from each eligible worn piece; redistribution repeatedly drains eligible pieces for absent pieces. Values<=2 become0. Toughness result*.025, armor*.05. Authored lore claims immunity while armor intact, armor points lost on hits and toughness withstands2.5x more. Exact raw math, worn explicit/default components, native operations/multiple entries, rounding, resistance lease ownership and pool overflow need separate clarification; do not expose before resolved.

There is no `LbcErosionProtection` class in the inspected tree. Actual `LbcErosionSuppression` parks Protection enchantments on erosion stacks during beacon suppression, with lifecycle cleanup and stack-identity tracking; it is not a finite armor/toughness depletion pool. Its event/lifecycle patterns can be reused, its mechanics cannot substitute for7. Existing inventory skill service supplies equipment mutation callbacks and pause queues; avoid idle inventory scanning. Preserve unrelated armor components and other owners' Resistance effects when7 is eventually designed.

## Suppression

All four skill-local source chains omit MAG/TECH/strong-artifact checks. Global skills admission, legacy-disabled dispatch, dimension restrictions from native advancements and time-stop handling still apply. Neighboring trinket, bow or attribute powers may have their own guards; they do not resolve these skill policies. Root is handling author decisions for selected14/16/18. TMT and all other prior agreed gates remain outside scope.
