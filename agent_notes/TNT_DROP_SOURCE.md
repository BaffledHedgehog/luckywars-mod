# 13-TNT special drop - source audit (22 October 2026)

Read-only audit; no build/runtime qualification. Read project AGENTS.md and agent_notes/AGENTS.md. Main jar69de8601 and original RP/server remain untouched. Author requirements: preserve real causal kill owner, shield reflection transfers ownership, protect allied players/service roles. No new author question was found for this plain trap.

## Exact legacy payload

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

- `function/drops/specialdrops.mcfunction`: checks bastion, end_city, treasure, village, bonus_chest in this order, then `{explode:1}` calls `lbc:drops/tnts`. These are independent checks. Shared dispatcher must run every applicable supported effect in source order, debit one unit once.
- `function/drops/tnts.mcfunction`: exactly13 native TNT at exact token position+(0,.1,0), fuse70. Motion Y=1 throughout. Ordered X/Z: `(0,0),(0,.1),(0,-.1),(.1,0),(.1,.1),(.1,-.1),(-.1,0),(-.1,.1),(-.1,-.1),(.2,0),(-.2,0),(0,.2),(0,-.2)`. No groundbam, passengers, custom explosion power, sounds or tags. Vanilla movement/fuse/explosion applies.
- `loot_table/traps.json`: repeating_command_block count1 weight10 quality-10; tooltip_style `lbc:tier_ff`, item_model `lbc:drop_trap`; name `[{translate:"trap_tnt",italic:false,color:"yellow"}]`; one lore row `{translate:"trap_desc",italic:false,color:"white"}`; custom_data `{explode:1,drop:1}`. `imba/traps.json` has weight0 and `{explode:1,drop:1,imba:1}` without ordinary presentation; do not substitute its payload.

## Active resource pack (read-only)

Actual root: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (the historical RESOURCE_PACK.md path includes `(1)`; use the current root).

`assets/minecraft/lang/{ru_ru,en_us}.json` existing keys:

- `trap_tnt`: RU Ловушка: Фонтан динамита; EN Trap: Dynamite Fountain.
- `trap_desc`: RU Активный Дроп. Для активации выбросте предмет.; EN Active Drop. Throw the item to activate.

`assets/lbc/items/drop_trap.json` selects `lbc:item/drop_trap`; `assets/lbc/models/item/drop_trap.json` uses parent `minecraft:item/generated`, layer0 `minecraft:item/drop_trap`. Reuse all existing keys/model/tooltip; no RP additions needed.

## Native causal ownership and reuse

Local authoritative sources jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.

- `net/minecraft/world/entity/item/ItemEntity.java`: `getOwner()` resolves private EntityReference thrower; `setThrower(Entity)` sets that reference. There is no getThrower method. NBT `Thrower` is causal; NBT `Owner` is separate pickup-target UUID (`setTarget`), used for pickup restriction/merge compatibility, never kill ownership. Ownerless world tokens must stay ownerless; nearest player is not causal evidence.
- `PrimedTnt.java`: constructor takes LivingEntity owner, persisted as lowercase `owner`; `getOwner()` resolves EntityReference; no native setter. `EntityReference.java` retains a reference only while not removed; then resolves its UUID across dimensions, returning null while missing/removed. Retained subclass source (existing Helicopter pattern) can preserve causality after native removal; any active native/reassigned owner must take precedence.
- `LbcIndirectKills.playerSource(Entity)` already unwraps TNT.getOwner and supported summon owners. Reuse this for attribution, but note it does not itself unwrap ItemEntity: resolve token.getOwner first.
- `LbcElectro.assignOwner` sets Projectile owner directly; non-Projectile TNT gets `reflectedOwners` map only. `reflectedOwner(tnt)` must take precedence for TNT attribution. `LbcDamage.allowDamage` already uses this map to apply projectileTarget for reflected damage; native TNT.getOwner alone does not reflect this transfer.

## Actual protection path

`LbcItems.allowDamage` runs LbcDamage guards, player gameplay damage guard, LbcWands filters and encounter services. Generic `LbcWands.allowDamage` returns true for plain non-Projectile TNT after its other specialized branches: service-specific TNT role/team filtering is necessary.

`LbcWands.validTarget` excludes dead/removed/spectator entities and tags spectator, rcdenyalways, sauvojen_tuntija, strict_map_object, magic_academy, transformator, plus inactive player roles. `projectileTarget` protects allied players while allowing friendly mobs; `target` also excludes friendly summons/mobs. Select the intended existing helper explicitly; ownerless TNT still needs validTarget protection without inventing an owner.

Global TNT reduction already exists: `mixin/LbcDamageMixin.java` inner TntProtection wraps Player.hurtServer's Avatar call after difficulty, before armor/effects. ServerPlayer raw amount calls `LbcDamage.protectedTntDamage`: any explosion whose direct entity is PrimedTnt gets multiplier `(5-clamp(tntProtection,0,5))/5`, except tag `skywars2.tnt.unprotected`. Default config4 gives20% damage; config5 suppresses zero-damage hurt reactions entirely. New fuse70 TNT must retain this path, without UNPROTECTED_TNT; no per-service second reduction. Vanilla block explosion remains unchanged.

## Native insertion, merge and chain follow-up

`LivingEntity.drop` calls `createItemStackToDrop`; that constructs ItemEntity, calls `setThrower(this)` when thrownFromHand, then returns before `level.addFreshEntity`. Normal player throws therefore already expose causal getOwner at ENTITY_LOAD. Capture the actual reference at load to survive owner disconnect/removal before deferred processing. A missing thrower cannot be reconstructed from nearest player. Parent decision: preserve already emitted physical TNT after death/disconnect; losing owner availability is not grounds to cancel it.

Native ItemEntity merge checks equal pickup target and item/components, not thrower. It mutates survivor stack/pickupDelay/age and discards empty donor; survivor keeps its original thrower. No existing general item causal helper was found in LbcIndirectKills; call playerSource on token.getOwner, with a retained reference where required. Mixed-owner native merging is a causal limit unless special tokens are guarded against merging or processed before it.

`HazardPropagationMixins.ChainTnt` wraps TntBlock.wasExploded/addFreshEntity: only parents tagged LbcExasperato.EXPLOSIVE propagate that tag, epoch and academy/exoperso blast inheritance. Ordinary trap TNT is outside this branch. Native TntBlock creates chain TNT with explosion.getIndirectSourceEntity; native owner persists in lowercase owner, but service-specific marker/epoch/protection inheritance needs a deliberate narrow hook if chains must belong to the new service. Existing indirectKills.worldChanges wraps explosion block effects for hazards; it does not automatically add trap-service tags to chain TNT.
