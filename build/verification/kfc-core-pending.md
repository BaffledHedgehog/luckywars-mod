# KFC gun core: pending implementation

Prepared 20 October 2026. The patch contains only this task's hunks in LbcShooters.java, LbcBlaze.java and LbcIndirectKills.java. Those hunks have been removed from tracked Java; all preexisting edits were preserved. No build or server was started for this patch.

Apply from the project root when the complete KFC feature is ready:

```powershell
git apply --check build/verification/kfc-core-pending.patch
git apply build/verification/kfc-core-pending.patch
```

## Status after integration

Applied and superseded by the complete verified KFC build `4902c01a`.
Do not reapply the adjacent historical patch. Both documented lifecycle gaps
were fixed before the two native KFC matches and shared regression passed.
Current entrypoint: `agent_notes/KFC_PORT.md`; proof: `kfc-verification.json`.

## Integration APIs

- `LbcShooters.KFC_GUN`: native Polymer carrot-on-a-stick, ID `skywars2:kfc_gun`, item model `lbc:kfcgun`.
- `LbcShooters.isKfcGun(ItemStack)`: native item or legacy `kfcgun:1`. Add this predicate to the existing LbcWands shooter dispatch. LbcShooters.use routes it to the KFC handler.
- `LbcShooters.damaged(LivingEntity, DamageSource)`: call from the existing LbcItems accepted-damage callback. Alive tracked chicken at HP <=20 creates a shared Blaze turret at Y+1 and is discarded. There is no collision attack.
- Existing LbcItems shooter load/unload/tick/forget/stop calls handle chickens and the shared-hand cooldown automatically.
- `LbcShooters.owner(Entity)` plugs into LbcIndirectKills' existing summoned-owner resolution; fireballs use existing LbcWands.launchFire owner/fire attribution.
- Package-private `chickenCount()` and `kfcCooldownRemaining(ServerPlayer)` support later checks.
- `LbcBlaze.chickenTurret(ServerPlayer, Vec3)` reuses the shared candidate snapshot, nearest-target guards, owner checks, suppression, one-second clock, transformOrigins, and launchFire. The Turret record gains an explicit speed; existing Blaze speed .7 stays .7, KFC gets 1.6 and three pulses.

## Behavior represented

Launches a native baby burning chicken with 30 HP, visual fire, empty death loot, long egg timer, and rounded native motion 6*look. Cooldown20 is shared between hands. No ammo/mana spending or creative bypass. Uses active/canFire/offhand/frost/time/antimagic guards. Tracks only loaded owned chickens; no per-tick entity scans or raycasts. Native extinguishing (`!isOnFire`) removes tracked state before rolling `lbc:kfc_yumyumyummy` exactly once at its position.

Common team/NPC/nomagic target protections are retained per root guidance; legacy KFC selected targets without ownership/team knowledge. Shared one-second clock means three pulses occur on the next three clock boundaries, as in legacy raycasters_1s; elapsed wall-clock lifetime is 41..60 ticks unless paused.

## Before shipping

1. Finish edible behavior and the pending user decisions; do not expose only the gun as complete KFC.
2. Register/remap native loot and dispatch, and call damaged. Update ARCHITECTURE.md to describe responsibilities.
3. Compile and run meaningful input, accepted-damage, extinguishing, owner/fire, and cleanup checks. No compilation has been performed for this patch.
4. Strengthen unloaded chicken lifecycle before shipping: the prepared implementation removes loaded references and restores tagged chickens on load, but uses ordinary saved Chicken entities. A chicken unloaded across owner cleanup can retain its tags and later reload. Follow the existing native summon subclass `shouldBeSaved() == false` pattern or use an existing owner-generation lifecycle guard, and test chunk unload/reload after owner death/forget.
5. Shared Blaze turrets currently inherit the existing owner's validity-at-pulse cleanup. Verify owner death/respawn cleanup cannot reactivate a pending turret if the owner is valid again before the next pulse; add immediate owner cancellation through the shared turret owner if needed.

Reference: agent_notes/KFC_SOURCE.md. Exact source path is recorded there. The patch intentionally touches no food, resources, random pools, architecture, registration dispatch, build configuration, or tests.
