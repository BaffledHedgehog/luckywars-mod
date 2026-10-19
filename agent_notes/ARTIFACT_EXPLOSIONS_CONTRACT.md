# Mystical artifact type 2: anti-explosion contract

Pre-implementation research. The current bounded port is described in
[ARTIFACT_EXPLOSIONS_PORT.md](ARTIFACT_EXPLOSIONS_PORT.md), with runtime evidence
in [SERVER_TESTS.md](../SERVER_TESTS.md). Recommendations below retain the audit context.

This note pins down the behavior of legacy `mystical:1,type:2` and the native Minecraft 26.3 removal semantics relevant to a bounded port. Legacy source is read-only at `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC`; target code is read-only for this research.

## Legacy dispatch and gates

- `data/lbc/function/items/changed_inventory/sel.mcfunction:66,70,78` adds `notnt_active` when the player has an item matching `minecraft:custom_data~{mystical:1,type:2}` in either `container.*` or `weapon.*`; it removes the tag only when neither group has a match. `weapon.*` includes mainhand and offhand. Armor is not searched by these checks.
- `data/lbc/function/players_2t.mcfunction:8` runs `lbc:other/artifact/notnt` at every player bearing `notnt_active`. `data/lbc/function/mobs_2t.mcfunction` dispatches `players_2t` for `@s[type=minecraft:player,gamemode=!spectator]` on the two-tick dispatcher, with no additional time/stage gate. Thus the source excludes spectator-mode holders. The `notnt` branch itself has no `stopper_magic`, `stopper`, or `time_stopped` suppression gate. Do not import the arrow-only `time_stopped` rule into this artifact.
- The only active `notnt_active` references found in the LBC functions are those two inventory state changes and the `players_2t` dispatch. The `legacy_tick_5t_player` reference is a separate `mystical_old,type:5` call to `notnt_old`, not another `notnt_active` selector.

## Effect per active holder

`data/lbc/function/other/artifact/notnt.mcfunction` runs three selectors, in order:

1. `@e[type=#minecraft:unstable,distance=..24]`: emit `minecraft:explosion` at each selected entity's position, zero offsets, speed 1, count 1, `normal` mode.
2. The same selector: execute `playsound minecraft:entity.generic.explode master @a ~ ~ ~ 1 1` at each entity. The source uses category `master`, volume 1, pitch 1, and targets `@a`; vanilla `playsound` with its default `minVolume=0` sends only to players in the same dimension within 16 blocks for volume 1. The target `LbcMagic.java:456-459` already preserves this audience rule for its native sound packet: it sends to `level.players()` with squared distance `<=256`.
3. `@e[distance=..24,tag=nuke]`: run `lbc:other/stoppernuke` as and at each nuke marker.
4. `@e[type=#minecraft:unstable,distance=..24]`: run vanilla `kill` on every selected entity.

Distance is a spherical selector distance (24 blocks from the holder), not a cubic radius. Each eligible holder independently executes these commands sequentially. The first holder that selects an unstable entity emits its particle/sound and kills it; subsequent holder selectors cannot select that entity again because vanilla `@e` filters by `isAlive`. Nearby overlapping holders therefore do not duplicate effects for a successfully killed entity.

`data/minecraft/tags/entity_type/unstable.json` has `replace:false` and exactly these values: `minecraft:dragon_fireball`, `minecraft:end_crystal`, `minecraft:fireball`, `minecraft:firework_rocket`, `minecraft:small_fireball`, `minecraft:tnt`, `minecraft:tnt_minecart`, `minecraft:wither_skull`, `creeper`, `wind_charge`, and `breeze_wind_charge`. The final three unqualified values resolve in the `minecraft` namespace. This is a custom legacy tag, not a native vanilla tag. Do not reduce it to TNT or just projectiles. An equivalent Java predicate should enumerate the full list and the target data resource should not accidentally replace unrelated vanilla tag values if the chosen tag mechanism supports additive values.

## Nuke branch boundary

The `tag=nuke` branch calls `other/stoppernuke`, which removes the local netherite-block/beacon construction, clears the block at y+2, schedules `other/stopnuke` one second later, and kills the marker. This interacts with `other/stopper_marker_destroy`, `other/nuke/nuke`, and delayed `nuke1` work. The target has no migrated nuke/time engine or cleanup lifecycle. Keep this branch explicitly out of a bounded artifact port; do not silently cancel or delete nuke markers while their world-block/timer cleanup is absent.

## What target classes already provide

- `SkyWars2.java:64-71` connects Fabric entity load/unload to `LbcItems.entityLoaded/entityUnloaded`. `LbcItems.java:438-479` fans events to `LbcMagic`, `LbcClouds`, and `LbcTrinkets`, and indexes only entities each subsystem owns. `LbcItems.java:481-513` tracks artifact-created `PrimedTnt` in `dynamites`; this set intentionally does not cover arbitrary vanilla TNT, minecarts, creepers, crystals, or all other unstable types. `LbcItems.tick()` runs the subsystem ticks (`:672+`) and `stop()` clears owned entities (`:720+`).
- `LbcClouds.java:145-177` accepts only recognized chemical clouds/potions/projectiles after `mayRun`: correct arena level, observer exclusion, playing/owner `canUseGameplayItems`, or current-epoch/debug countdown handling. Its `ENTITY_UNLOAD` hook removes its own entities. Its fulminate cloud creates native `PrimedTnt` at `:266+`.
- `LbcTrinkets.java:136-154` tracks only its custom tagged projectiles, validating arena level, owner use/observer status, current item epoch, and debug item-use window. Its stop hook discards its own tracked projectile snapshot.
- `LbcMagic`'s arrow index is the closest performance pattern: entity load/unload registration, tracked-set cleanup, and a strategy that avoids scanning all world entities for every holder (`LbcMagic.java:368-416`). Its `time_stopped` exclusion belongs specifically to arrows.

Those are lifecycle/performance patterns, not a ready-made anti-explosion eligibility policy. Source behavior gates the *holder* by inventory tag and spectator mode; target subsystem `mayRun` gates *owned spawned entities* by game stage/owner/current epoch. A type-2 holder does not own nearby TNT or creepers. Preserve the holder gate and use target gameplay rules deliberately; do not require an explosive's owner or custom epoch tag, since that would miss environmental entities and entities fired by others.

## Native kill contract and consequences

Minecraft 26.3 source in `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar` shows `Entity.kill(ServerLevel)` calls `remove(RemovalReason.KILLED)` and emits `GameEvent.ENTITY_DIE`; `Entity.discard()` calls `remove(RemovalReason.DISCARDED)`. `LivingEntity.kill(ServerLevel)` overrides this by applying `genericKill` damage at `Float.MAX_VALUE`, so it follows living-entity death handling rather than direct base removal. Vanilla entity selectors such as `@e` add an `Entity::isAlive` predicate; a native query intended to reproduce `@e[...]` must exclude a living entity that has entered its death state even if it has not yet been fully removed/unloaded. In particular, forced-killed creepers can remain in death handling/animation and must not be selected again just because `isRemoved()` is false. The legacy command is vanilla `/kill` through Paper; no Paper 1.21.11 source jar was present for an independent Paper patch audit, so the comparison here is to the native command/entity API contract.

Subclass implications verified in that 26.3 source jar:

- `EndCrystal.kill` calls `onDestroyedBy` before `super.kill`; `onDestroyedBy` notifies `EnderDragonFight` if one is present, which updates crystal/respawn state and notifies a live dragon. Its ordinary `hurt` path creates the crystal's block explosion only when the damage source is not itself an explosion. The artifact's `/kill` therefore does not invoke the ordinary crystal-destruction explosion path, though dragon-fight callbacks still occur in an End fight.
- Creeper inherits `LivingEntity.kill`. Its explosive behavior is in its fuse/tick path (`explodeCreeper`), not a `kill` override; forced kill is not the same as waiting for its fuse. It enters living-entity death handling and is no longer `isAlive` before final removal, so the selector's default alive predicate prevents repeated kill-selection during that interval. Living-entity death handling can still run, so kill is not equivalent to `discard` for every side effect.
- `PrimedTnt`, `MinecartTNT`, firework rockets, fireballs, wind charges, and wither skulls have fuse/hit/tick explosion paths, while base kill removes the entity without invoking those paths. `FireworkRocketEntity` normally deals explosion damage and discards itself from its own explode method; kill bypasses that method. `MinecartTNT` explosion is in fuse/damage/collision/fall paths, not a `kill` override.
- Vanilla `/kill` should therefore be represented by `entity.kill(serverLevel)` for faithful native semantics, not by calling each entity's explosion method. `discard()` is a different removal reason and skips LivingEntity's kill/damage behavior and the base kill game event. Check the target mappings/API for the precise `kill(ServerLevel)` signature before implementation.

Legacy `kill @e[type=#minecraft:unstable,...]` also means normal entity death/removal semantics apply to the entire custom tag. It is not a promise that every entity is silently discarded. In particular, the tag's creeper member is a living entity and should go through the native kill path.

## Bounded integration recommendation

Implement a small owner-driven subsystem in the existing `LbcItems` lifecycle, with no global all-entity scan and no new general framework:

1. Refresh the type-2 holder state on the same inventory-change path that refreshes other ported artifact state. Match `mystical:1,type:2` in precisely source-equivalent `container` and `weapon` ranges; use a set of active eligible holders or the existing periodic item refresh mechanism.
2. Run holders at the legacy two-tick cadence, requiring a live player, the match level, and source-equivalent non-spectator eligibility. Apply the target's gameplay/debug stage policy consistently with already ported item behavior, but do not add stopper/time-stop gates unsupported by this source function.
3. For each holder, query the custom unstable set locally with an AABB broad phase and the selector-equivalent alive predicate, then enforce `distanceToSqr <= 24*24`. Emit the exact particle and sound at each selected entity before calling native `kill(serverLevel)`. Keep entity iteration bounded to loaded nearby entities and tolerate death-state changes/unload during callbacks; use the established load/unload hooks only if a set-indexed strategy proves useful.
4. Leave `tag=nuke` / `stoppernuke` out until its timer and block cleanup engine exists. Do not add a `minecraft:unstable` data tag and assume it already exists in target resources; either supply the exact additive tag or use a single explicit entity-type predicate.

This is a source-contract recommendation, not an assertion that type 2 logic is already ported. Existing artifact resource import (`ARTIFACT_EXPLOSIONS_RESOURCES.md`) imports the item payload/type marker only; this audit found no target `notnt` behavior in `LbcItems`, `LbcClouds`, or `LbcTrinkets`.

## Source references

- Legacy: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/sel.mcfunction:66,70,78`
- Legacy: `.../data/lbc/function/players_2t.mcfunction:8`; `.../data/lbc/function/mobs_2t.mcfunction` (two-tick dispatcher, `gamemode=!spectator`)
- Legacy: `.../data/lbc/function/other/artifact/notnt.mcfunction`; `.../data/minecraft/tags/entity_type/unstable.json`
- Legacy nuke dependency: `.../data/lbc/function/other/stoppernuke.mcfunction`, `stopnuke.mcfunction`, `stopper_marker_destroy.mcfunction`, `other/nuke/nuke.mcfunction`, `other/nuke/nuke1.mcfunction`
- Target: `src/main/java/me/skywars2/SkyWars2.java:64-71`; `src/main/java/me/skywars2/game/LbcItems.java:438-513,672-731`; `LbcClouds.java:145-177,266-272`; `LbcTrinkets.java:136-154`; `LbcMagic.java:368-416,456-459`
- Native source jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, files `net/minecraft/world/entity/Entity.java:417-424`, `LivingEntity.java:320-323`, `EndCrystal.java:111-122`, `Creeper.java:230-238`, `PrimedTnt.java:127-...`, `MinecartTNT.java:77-...`, `FireworkRocketEntity.java:192-197`.
