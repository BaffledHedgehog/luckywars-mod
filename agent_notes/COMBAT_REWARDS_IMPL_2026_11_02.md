# Combat reward implementation — 2 November 2026

Implementation ownership: `LbcCombatRewards.java`, `CombatRewardMixin.java`, and this note only. Root owns service wiring, catalogs, shared death entry, validation and release. No builds, tests, installations, resource pack edits or git operations were run by this implementation agent.

## Author policy supplied by root

Both skills16/18 are unjammed, including strong artifacts, with original unlocks, shared global skill and role admission retained. Skill16 draws the shared exact uniform26 positive permanent base gain on every credited living kill, including a player victim. Player kills additionally absorb full positive surplus base attributes over ordinary native player defaults. MAX_HEALTH uses the same clamped configured health baseline as actual reset, `max(1,min(1000,config.health))`. Final author reply includes ATTACK_SPEED. Previous HP20% and movement5% factors are removed. Equipment, modifiers, negative differences, and ordinary baseline amounts never enter the transfer.

Skill18 copies actual successfully published death item stacks from the exact victim's nested native death context. It never rerolls loot, selects nearby/global age cohorts, or multiplies XP. Stack copies preserve complete item components and counts; counts larger than a native maximum are split into valid stacks. ItemEntity motion/ownership/age are newly native defaults, matching the source's item-stack copy semantics; the stack's own metadata survives.

## Native paths inspected

Pinned source archive: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.

`LivingEntity.die` calls `dropAllDeathLoot` and then `createWitherRose`. `ServerPlayer.die` overrides the whole path and directly calls inherited `dropAllDeathLoot`; its native `Player.dropEquipment` calls `inventory.dropAll` after vanishing curse removal unless KEEP_INVENTORY. Both paths therefore share native equipment/inventory/loot-table/custom-loot publication. Native wither rose and custom AFTER_DEATH publications can occur outside `dropAllDeathLoot`, so the outer die wrapper also supplies the context. Nested same-victim wrappers reuse it; another victim receives a separate frame. Successful `ServerLevel.addFreshEntity` publications are captured by identity, with actual stack copies delayed until the outer context returns so RETURN callbacks finish component/count adjustments.

`DefaultAttributes.getSupplier(EntityTypes.PLAYER)` supplies actual native player baseline values. Materialized attributes absent from that supplier fall back to `Attribute.getDefaultValue()`, exactly as `GameController.resetAttributeValues` does. Configured overrides match actual reset: MAX_HEALTH is clamped configured health and ATTACK_SPEED is100000 under pvpMode1. All available attribute registry holders are inspected only on an actual player kill/direct absorption call.

## Wiring contract

Constructor: `new LbcCombatRewards(game)`; owner getter: `game.items().combatRewards()`.

Root calls `beforeDeath(victim,source)` sequentially after existing `indirectKills().dying`, preserving shared inherited and native old-attacker credit. Explicit self damage cannot take an older foreign player's credit. Actual-body identity prevents duplicate reward callbacks; alive/removed bodies are released from deduplication on `tick()`. The final implementation uses IdentityHashMap: native Entity.equals compares numeric IDs, and respawn deliberately reuses an ID for a different body.

Register `CombatRewardMixin$Death`, `$DeathLoot`, `$Published`. These supply `withDeathLoot(...)` and successful `published(...)`; there is no independent unordered death HEAD attribution callback.

Call `tick()` with the ordinary service ticks and `clear()` on match boundaries and rewind invalidation. Pending rewards identify the player UUID and match epoch and resolve the actual current online same-level body. Paused rewards remain queued until shared Time Stop admission resumes. Ordinary skill16 rewards are immediate, before victim cleanup; ordinary skill18 copies publish immediately after the final outer death scope returns. Only paused rewards queue. Admission is reserved on actual death, so subsequent selection changes or ENDING do not cancel the final kill's already accepted reward. Tick drains a snapshot so nested publication/death callbacks can append new pending rewards safely. `pendingCount()` is package-local and counts paused batches rather than stacks.

Direct validation helpers: `absorb(killer,victim,boolean includeAttackSpeed)`, native `baseline(attribute,int configuredHealth)`, and actual configured `baseline(attribute,GameConfig)`. `includeAttackSpeed(boolean)` selects policy; final default is true. Shared positive roll2 emits source levelup sound and100 iron-block particles.

## Qualification and integration concerns

This note records implementation, not runtime qualification. Root/test agent own compilation and native behavioral proof. Root required immediate unpaused copies and reserved death admission to preserve last-kill rewards through ENDING; this is implemented. Root will clear transient paused rewards on rewind as permitted for short tasks; new-match epoch invalidation separately prevents cross-match replay.
