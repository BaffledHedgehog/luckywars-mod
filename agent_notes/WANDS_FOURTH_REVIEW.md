# Tiny Acid: focused Java review

Reviewed the new Tiny Acid registration/dispatch in `LbcWands`, and wand-only changes in `LbcClouds`, against `WANDS_FOURTH_SOURCE.md` and Minecraft 26.3 native bytecode. Read-only review; no build or runtime PASS is claimed.

## Contract matches

- `TINY_ACID` is registered under the existing item framework with old key/model; enum cost is 63. `cast` charges once per hand and retains the shared Mage bonus and trinket hook. No Creative/developer-specific exception was added.
- `ray()` walks at most 101 points in 0.35 increments, ends on first entity or first wall, and dispatches a single terminal. Wall endpoint uses the previous free point, matching the established ordinary single-terminal rule. `rayEnd(ACID)` creates one volley; the source wrapper + `acid_end` + preloop cure sounds are preserved as one cast-origin sound and two endpoint sounds.
- `wandAcid` creates six projectiles. Yaw `nextInt(361)-180` gives every integer angle from -180 through 180; pitch `nextInt(31)-90` gives -90 through -60. Six origins are offset by `0.01` on world Y, matching source summon points. The source enters the acid-end helper at the ray endpoint, sets pitch to -90, and summons those local offsets; native command `positioned` resets the anchor to feet, so this port should not add another eye-height offset. Each uses native `shoot(..., .5f, 0)` and the existing acid type, not a new movement loop.
- `ThrowableProjectile.tick` in the local 26.3 merged jar calls `applyGravity`, `applyInertia`, `ProjectileUtil.getHitResultOnMoveVector`, moves to the hit location, then invokes the hit path. Its inherited `Projectile.canHitEntity` excludes entities that cannot be hit and owner passengers; the override adds spectator/gameplay and wand-only owner/allied guards. Wand effect and shield-target loops apply the same owner/allied/observer eligibility. Nearby burning entities remain an ignition source without an ally filter, matching the source behavior root selected.
- Acid ages through the existing persistent `ACID_AGE` attachment and 300-tick cap. `LbcItems.tick()` advances the cloud tracker; load/unload hooks register and unregister projectiles, and `stop()` clears tracking and discards tracked entities. Each wand projectile receives the current epoch tag. `onHit` invokes the shared acid effect and discards it. This reuses the native acid lifecycle without changing non-wand acid: `WAND_ACID` branches alone add owner/team targeting and attributed wand effects/damage.

## Native API checks

- `ThrowableProjectile.tick` / `Projectile.canHitEntity`: local `minecraft-merged.jar` bytecode confirms native gravity, drag, swept movement collision, and inherited owner/passenger rejection before the custom predicate.
- `LivingEntity.addEffect(MobEffectInstance, Entity)`: the native overload records the supplied source when adding the effect; wand poison passes the projectile owner.
- `Entity.hurtServer(ServerLevel, DamageSource, float)`: wand fire-acid damage supplies an `ON_FIRE` `DamageSource` with projectile as direct entity and owner as causing entity; non-wand acid still calls the existing vanilla on-fire source.
- Registration, one-time age advancement, stop/unload cleanup, and epoch gating are reviewed in `LbcClouds`/`LbcItems`; no new tracker or long-lived marker loop was introduced.

No confirmed semantic bug found in this focused review. This does not replace the root's planned build or in-server checks.

## References

- Project: `src/main/java/me/skywars2/game/LbcWands.java`, `LbcClouds.java`, `LbcItems.java`, `SkyWars2.java`, `GameController.java`.
- Native 26.3 merged jar: `ThrowableProjectile.tick`, `Projectile.canHitEntity`, `ProjectileUtil.getHitResultOnMoveVector`, `LivingEntity.addEffect(MobEffectInstance, Entity)`.
- Source contract: `agent_notes/WANDS_FOURTH_SOURCE.md`.
