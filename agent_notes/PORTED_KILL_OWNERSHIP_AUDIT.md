# Deferred kill ownership audit: BrBad, mana loss, and ported items

Bounded review of current Fabric implementation (2026-10-16), followed by the narrow direct-owner fixes listed below. No build, server, or resource-pack changes. Scope is concrete delayed/direct damage and environmental consequences in already ported handlers, not a world-wide hazard audit.

## Coverage and gaps

Historical findings below were recorded before the root implementation.
The BrBad, mana-debt, periodic Wither/Poison and owned world-hazard gaps have
since been addressed; current scope and limits are documented in
[INDIRECT_KILLS.md](INDIRECT_KILLS.md). Native validation results are in
[SERVER_TESTS.md](../SERVER_TESTS.md). This bounded audit is not a claim that
every item or all future mechanics have been exhaustively verified.

| Item/path | Status and concrete finding | Actionable hook |
|---|---|---|
| Tiny Teleport / Ruby / Stone | **Covered.** `LbcWands.ray` / `rubyRay`, `placeTerraBlock` call `LbcIndirectKills.levitated`, `supportRemoved` + `finishSupportRemoval`, `stonePlaced` + `blockChanged`; `IndirectKillMixin.die` invokes `dying` before native rewards. | Keeps narrow FALL/VOID, actual support-loss fall, and tracked STONE `IN_WALL`; retain these checks and ArenaBlockMixin cleanup. |
| Tiny Acid | **Covered** for wand burning direct damage and accepted wand poison. `acidImpact` builds `DamageSource(ON_FIRE, projectile, owner)`; accepted `addEffect(poison, owner)` records the exact poison; `PoisonDamageMixin.applyEffectTick` brackets native poison damage. | Keep exact effect-identity and source-specific tick conditions; don't treat arbitrary MAGIC as Acid. |
| BrBad thermite (234) | **Missing.** `LbcClouds.thermite(CloudState)` applies `entity.damageSources().lava()` to targets (`:261`), while `state.cloud.getOwner()` exists. | Pass a LAVA `DamageSource` with cloud direct entity and cloud owner causing entity at this call. Replacement-block-created later lava/fire hazards are broader world consequences, outside this bounded hook. |
| BrBad sulfur/phosphine (235/236), including sulfur acid helper | **Missing delayed attribution.** `sulfur`/`phosphine` apply ownerless WITHER/POISON repeatedly (`:276-287`); `spawnAcid(cloud)` carries cloud owner, but `acidImpact` only records POISON for WAND_ACID (`:399-403`). | Add source-aware accepted effect recording for BrBad POISON/WITHER; wrap native PoisonMobEffect and WitherMobEffect periodic damage, retaining exact effect identity. Also record BrBad sulfur-spawned acid poison when accepted. Existing `LbcColorEffectsMixin.onEffectAdded/onEffectUpdated` receives `Entity source` but currently only invalidates poison state; resolve a cloud source through `AreaEffectCloud.getOwner()`. Preserve MAGIC/WITHER damage types. |
| BrBad fulminate (237) | **Missing.** `LbcClouds.fulminate` creates 2-tick `PrimedTnt(..., null)` though cloud owner is known. | Set TNT owner from cloud; confirm vanilla explosion source resolves that owner for player kill credit. |
| Mana remove potion / Overload death | **Missing for hostile potion owner.** `LbcMana.badPotion` (amp 20) removes 2000 mana; `tickPlayer` runs `tickPotions` before `overload.afterRegen`, where stage 5 can call `LbcOverload.explode` and `p.kill(p.level())`. Potion sentinel arrives via vanilla cloud/status source, but `LbcMana.effectsChanged` stores no actor. | Record accepted amp-20 instance/source during `LbcColorEffectsMixin.onEffectAdded` (cloud source must resolve to its owner), carry attacker→target through the corresponding bad-potion timer, and set native credit before the lethal overload `kill` only if that timer materially caused stage-5 death. Clear on replacement, expiry, cleanse, death. Do not credit ordinary self-debt or ownerless self overload. |
| Overload armor slam | **Fixed here.** `LbcOverloadArmor.impact` now builds the existing OUTSIDE_BORDER damage type with `player` as source and reuses it for targets. | Damage type and radius/damage rules remain native; no zero-damage hit is used to seed ownership. |
| Item dynamite | **Fixed here.** `LbcItems.tickDynamites` now passes the thrown snowball's `LivingEntity` owner to `PrimedTnt`; wand dynamite already constructed TNT with player (`:607`). | Native TNT explosion retains its thrower; null/nonliving owner keeps prior null fallback. |
| Trinket Intercal wither | **Source already supplied.** `LbcTrinkets.damaged` passes `attacker` to both WITHER `addEffect` calls at `:202,204`; the native periodic WITHER tick still needs deferred attribution. | Root-owned generalized effect-source/tick capture should reuse these existing source arguments; no change needed in this class. |
| Frost, Terra, direct wand hits, Overload ray | No additional delayed gap found in reviewed paths: Frost cold/frozen handlers apply control effects and owned temporary ICE without delayed damage; Terra projectile and wand Terra/electro paths construct owner-bearing sources; Overload ray uses owner-bearing OUTSIDE_BORDER. | Global later-hit precedence still applies. Frost hold/restore is not an added suffocation trap in this implementation. |

## Shared contract / regression locations

- `LbcIndirectKills.dying` skips deaths with a non-null `DamageSource.getEntity()`, preserving native direct attribution. `credit` sets `setLastHurtByPlayer` and `setLastHurtByMob` before vanilla loot/XP/kill-score consumers; match rewards consult native kill credit.
- `SkyWars2` AFTER_DAMAGE calls `indirectKills().damaged` only for positive applied damage; newer different-player hits override older deferred ownership. New status/timer causes must enter that order and expire when no longer causal.
- Reuse existing `GameChecks.checkBrBad` (`:3490+`), `checkManaPotions` (`:5017+`), and indirect-kill/zone lethal fixture (`:15224+`) for focused attribution regressions; no broad hazard sweep follows from this audit. Checks were not edited or run for these changes.
