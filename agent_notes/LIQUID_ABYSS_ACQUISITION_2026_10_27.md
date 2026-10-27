# Liquid Abyss acquisition boundary - 27 October 2026

Read-only acquisition audit after parent baseline `97ee6ba`. This note changes no production, resources, source pool, or existing audit. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Native project root: `E:/servers_mew/skywars2_last/luckywars-mod`.

## Decision

A complete existing normal-play producer is present: ordinary Consuming Wand, marked for its Academy trial, casts the deterministic 28-Void-Man trial; each native Skeleton loads its configured `lbc:voidman` death loot. Native26.3 still directly reads `DeathLootTable`. No missing preset hook or production producer change was found by this code audit.

**This is code-path evidence, not new runtime acquisition qualification.** Existing `GameChecks.checkConsumingTrial` proves native actors, payment, attributes and promotion; its code never kills a trial mob or inspects the loot override. The next fixture must prove an accepted native death actually creates Liquid Abyss. A manual `rollTable`, an independently constructed Skeleton, or JSON presence does not supply that proof. Do not enable `random_glack_1`, God Claw, or another unresolved whole source pool to qualify this item.

## Exact route and admission

1. `LbcCraftingRecipe` already has `Kind.CONSUMING` (`consuming_combine`, `consuming_wand`): Ruby/Academy Ruby/`struby:1` plus one native Creeper Army/Destroyer or `cons_material:1` donor, through existing native crafting. Result uses `lbc:ported/consuming_wand`. Ingredient recognition is at lines221-224 and result selection252; this audit does not requalify all upstream donor acquisition pools. The original plain Consuming leaf has no `upgradable` flag.
2. Existing actual Academy NPC interaction (`LbcAcademy.interact`, lines182-236) recognizes the ordinary consuming identity/flag. First unmarked interaction sets `upgradable:1`, writes the inventory and tells `upgrade_condition_consuming`; the source condition helper itself only tells that condition. No mob kill, target or random chance is required to arm the trial. Existing native Academy acquisition/placement remains the producer of these NPCs; a fixture NPC only isolates this already integrated interaction boundary.
3. Real native item input goes through `LbcWands.use`/`cast`, then the dedicated Consuming branch525-526. `use` enforces the offhand option; a mainhand action also attempts its offhand independently when enabled. `cast` rejects `actionsBlocked` before dispatch. Native consuming `use`151-170 checks living/nonremoved/current-map, shared gameplay admission, not observer/ENDING, dimension/owner Time Stop, frost and both owner/eye magic suppression, and prepared resources. Shared gameplay admission remains authoritative; no extra Creative bypass is inserted.
4. `LbcConsuming.use`163-168 reads the live `consuming_wand_mana_use` math score (default2000 from `LbcWands.Spell`). `trySpendWand` requires the real required amount unless the existing full Overload armor exception applies, then subtracts that amount. No independent free-creative route exists here. Ordinary + shift + `LbcAcademy.trial(stack)` (`upgradable==1 && upgraded!=1`) selects `trial(owner)`. Otherwise ordinary mode1 is its normal consuming beam. The trial has **no aim target, hit requirement, chance, or preset lottery**.
5. `trial`451-466 queues meditation at floor(owner eye position)+(-23,-16,-23), then deterministically attempts all28 preset spawns at that original eye position plus the ordered offsets. Multiplicities: four each of(0,1,0),(0,2,0),(0,3,0); six each of(1,0,0),(0,0,1); four of(0,0,0). Every entry names the sole `void_man` preset. Only already-resident spawn cells are admitted. Native `load`, position, current epoch, trial tag, and ownership-map insertion precede `addFreshEntity`; insertion failure removes ownership and discards only that failed mob.
6. Darkness400ticks amp3 and Blindness400ticks amp3, hidden, are applied to the caster. `academy.ready` immediately sets the actual used stack `upgrade_available:1, upgradable:0`; **survival/killing the28 is not a further upgrade condition**. A subsequent mainhand NPC interaction transmutes to Academy Consuming, tierX, mode1, source upgrade lore. `wandCasted` runs once after the paid trial and retains existing full-Mage max-mana/trinket behavior.

Rejection before debit leaves the resource unchanged. After debit there is no transaction rollback for a skipped/unaccepted spawn or delayed meditation work: source commands are independent. `trial` rechecks valid/prepared before execution; there is no intervening asynchronous callback in the normal use path. No stack consumption/durability cost is added.

Academy ordinary casting (mode1 default,2 Silence,3 The End,4 Avarice), mode cycling (shift, before debit), explicit-origin `emitAt` (only2/3/4), Sauvojen adapters, and consuming grimoires do **not** call this preset producer. An already upgraded Consuming stack cannot initiate its ordinary trial. `rg` found no second production `trial` caller besides ordinary `use`; direct fixture invocation is not acquisition evidence.

## Configured Void Man and migration

Source `workingitems/awaken_tuntija_wands/consuming_wand_upgrade.mcfunction:2-29` has28 identical skeleton SNBT payloads; source lines31-33 give the two caster effects and `can_upgrade`. `consuming_presets.json` stores that payload once, inserts `id:minecraft:skeleton`, preserves28 offsets, and declares original data version4440. `LbcConsuming.prepare`104-127 uses the server's real ENTITY DataFixer from4440 to the current runtime data version, then `Skeleton.load(TagValueInput.create(..., registryAccess, copiedMigratedTag))`. It does not interpret arbitrary handwritten modernized fields itself. Async preparation errors are logged and `ready()` remains false. Existing source note `CONSUMING_RESOURCES.md` describes the importer; this audit independently inspected current payload and loader.

Whole payload: Skeleton; `clawed_entity`; bold black nonitalic obfuscated translatable `void_man` name, name not permanently visible; Silent=true; PersistenceRequired=true; current/max health40; attack damage10. Feet leather boots, chest leather chestplate, head leather helmet, each with `equippable.asset_id=lbc:void`; helmet unbreakable. No leggings or held weapon is specified. Four source head/chest/legs/feet drop chances0. Invisibility amp1, duration20,000,000, particles hidden. `DeathLootTable:lbc:voidman`, no custom loot seed. Preserve defaults where the source omits an attribute, equipment component, AI flag, or effect icon flag. The `clawed_entity` tag is a God Claw reprocessing exclusion in source, not a separate scripted combat attack.

Cached native26.3 `net/minecraft/world/entity/Mob.java` uses field `lootTable: Optional<ResourceKey<LootTable>>`; readAdditionalSaveData405 reads `DeathLootTable` with `LootTable.KEY_CODEC`,406 reads seed, getLootTable417-418 returns this override before the entity default, and dropFromLootTable411-413 clears the override only **after** delegating to native loot generation. The contemporary API is `Mob.getLootTable()`; no assumed `customLootTable` field/setter or lower-case `death_loot_table` migration is needed. Native `Mob` also reads persistence396 and native drop chances397. Actual runtime assertion of the post-DFU loot key is still required.

## Death, loot, ownership and protection

`LivingEntity.die`1466-1469 calls `dropAllDeathLoot` during the accepted native death. `shouldDropLoot`588-589 requires not baby and `GameRules.MOB_DROPS`; native normal Skeleton is not a baby. `dropAllDeathLoot`1504-1512 then calls overridden loot generation, custom loot, equipment and XP. Native1541-1572 resolves the actual reloadable loot table; its ENTITY context carries THIS_ENTITY, death position, DAMAGE_SOURCE, attacking/direct entities, and when applicable last damage player+luck. Output is native `spawnAtLocation`, not a compatibility manual-roll replacement. Native source cause and player-kill attribution must therefore be asserted on a real accepted hit. Normal Creative attacker behavior is not overridden; native invulnerability/role/Greed gates are retained. A kill command/discard cannot substitute for this boundary.

Whole source `voidman.json` has exactly two pools:

- Guaranteed one `minecraft:repeating_command_block`: `lbc:tier_ff`, model `lbc:liquidabyss`, dark-purple nonitalic translatable name `liquid_abyss`, one dark-purple nonitalic `liquid_abyss.lore`, custom data exactly `{liquidvoid:1}`. No invented native item registration, charge or extra passive flag.
- Separate one-roll reference to `lbc:sauvojen_tuntija_blind`, weight10/quality8, random_chance0.1. The first guaranteed pool does not depend on this chance. Keep the entire original second pool and leaf; fixture success must not require a random Blind drop.

Native resources have the26.3 schema migration (`modifier`/`type`, singular `condition`) while preserving these contents. Build whitelist254-255 already includes both dependency leaves. `CONSUMING_RESOURCES.md` records existing RP model `liquidabyss`, model `st/blind`, and void equipment definitions; this audit makes no new physical-pixel/RP deployment claim. An additional `random_glack_1:83` source reference is not needed or qualified here.

Trial actors are tagged `skywars2.consuming.trial_actor`; ownership map registration before native add prevents the loaded callback from discarding a legitimate new actor. `LbcItems.loaded/unloaded/tick/forget/stop` invokes consuming lifecycle methods. Orphan/reloaded stale actors are discarded, and epoch/match/owner departure cleanup deliberately discards remaining live trial actors without awarding death loot. `unloaded` removes ownership before discard. Death loot has already been generated synchronously before normal death removal; do not use cleanup discard as the death test.

`canTrialTarget`/`canTrialProjectileTarget` allow the trial's caster as its hostile target but protect caster allies and technical/native roles through shared target policies (friendly mobs differ between ordinary/projectile policy as already defined). `LbcIndirectKills.playerSource` resolves a trial Skeleton or its projectile to its original caster for downstream attribution. `LbcGreed.active` prevents accepted damage through existing native admission; do not bypass it merely to prove acquisition. Native `wands.validRoleTarget` protects observer/spectator/Academy/strict technical actors; no new private creative exemption or role is supplied. Syringe's noncancelling death-loot mixin only restores its tracked buff when present and does not replace the trial table.

## Native fixture boundary and limits

Use the **registered singleton** `game.items().consuming()` and existing `ready()`, `summonSnapshot()`, package `owner(Entity)`, and `currentEpoch(entity)`; no production seam is required. Follow actual hand/Academy interaction and `wands.use`; snapshots are observation, not a spawn substitute. Existing `GameChecks.checkConsumingTrial`10834ff already asserts28 actors, source40HP/attack10/invisibility, actual offhand2000 payment, hostile-caster/protected-ally AI, ready stack and promotion, then cleans by discard. It does not inspect native death loot or all cosmetic components.

Additional qualification should inspect the real spawned mob's `getLootTable()` before any kill, loaded attributes/equipment/name flags, then perform a real accepted native player-attributed damage/death under MOB_DROPS=true and inspect newly spawned native ItemEntity stacks. Verify exact Liquid Abyss count/components and that unrelated preexisting drops remain unchanged; do not overwrite or roll the table to manufacture success. Preserve actor/score/food/effects/participant attribution state, player inventory/hand/shift/offhand config, gamerule, resident tickets, meditation terrain and introduced entities on every exit. No synchronous world ticks, manufactured callbacks, RNG-derived expected liquid result, or enabling an unresolved pool is necessary.

If this real boundary fails: first distinguish migrated `Mob.getLootTable()` loss from native reloadable table/schema failure, MOB_DROPS/native rejected hit, suppression/Greed or actor lifecycle cleanup. Only a concrete failed key would justify explicitly assigning the migrated native loot override during the **same complete trial producer**, preserving all28 positions, payload, payment, trial readiness, rollback boundaries and lifecycle ownership. A partial alternate mob/manual item grant is not a fix. No such code defect has been established here.

No build, native run, server action, Git command, old-note edit, production edit, or resource write was performed for this audit. Native source line numbers refer to the cached26.3 source jar, not Minecraft21.4 source. Hashes below identify the exact inspected bytes; no claim is made that this task runtime-qualified the loot/death chain.

## SHA-256 evidence

Legacy paths are relative to the source root above. Native Java/resource paths are relative to the project; cached native entries are uncompressed source-jar entry bytes.

| Scope/path | SHA-256 |
| --- | --- |
| Legacy `function/workingitems/awaken_tuntija_wands/consuming_wand.mcfunction` | `dfa31dccd684f4da3e2ebbcec4b6938f72ffa0e28b8ae7bc19d74c633313fb75` |
| Legacy `function/workingitems/awaken_tuntija_wands/consuming_wand_upgrade.mcfunction` | `10a0bfba640a18cbd2b56038a2336fefe74fbf435b1124b722466feef8da382f` |
| Legacy `function/other/magic_academy/can_upgrade/consuming_wand.mcfunction` | `4a0c22af579cf4952a938f0f762649f3886d2f1ddc95992c9484a4668f18edc8` |
| Legacy `function/other/magic_academy/upgrade/consuming_wand.mcfunction` | `7cdbd93310cb48891e4bdc6229183cd16f7981b78769b13453799c96fbafef53` |
| Legacy `loot_table/voidman.json` | `3eb5b1ad6c981fbbeea0247ea573a4bf1a87c370d6a6f66c7e7f18e084bc1b4a` |
| Legacy `loot_table/sauvojen_tuntija_blind.json` | `5ddcadee2c580b503897f5d32acd886c26e57a849417e90a7d08df0b03cf062f` |
| Native `src/main/java/me/skywars2/game/LbcConsuming.java` | `2c74b2ef065025179e60076e89df3d7c34bc9bed8c1b12a0c938e77d13203c3c` |
| Native `src/main/java/me/skywars2/game/LbcAcademy.java` | `fa6253f7ff2cc83fe0b2450adfb36a011163c9410d1ce4d95a0b4a5d48db4937` |
| Native `src/main/java/me/skywars2/game/LbcCraftingRecipe.java` | `8765657e6dda97a97a49cdd4d470f6e626c6fb7f191d6ab4f6e3094aa9f7fc17` |
| Native `src/main/resources/skywars2/consuming_presets.json` | `46165d162912a1f5adfd2afca4b661f5d95c1d48a3ff1ed708f4a4c7b33c6bc8` |
| Native `src/main/resources/data/lbc/loot_table/voidman.json` | `83f438aef4962f16cbf97c82bf113b153e70cd9888ef360d0b0ed367b25189c9` |
| Native `src/main/resources/data/lbc/loot_table/sauvojen_tuntija_blind.json` | `52a84c42c23e8a80b4cb53aa095b9b9bc196ee1add2e4db3b549131a3df53a03` |
| Cached26.3 `net/minecraft/world/entity/Mob.java` | `9e37897a91b8c8be0acfc077272e62b40bceef665c57a2ccf848ffee8e2b8bfa` |
| Cached26.3 `net/minecraft/world/entity/LivingEntity.java` | `3e9277fb24df1fe4995797a750dcce2ae27abbdcace87f62ef785b43de34f34f` |
