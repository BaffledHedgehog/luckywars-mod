# Lightning friendship review ? 21 October 2026

Bounded read-only production audit; only this note is written. User requirement supplied to this audit: Chain Lightning selects allies (players and mobs) and self; every actual `minecraft:lightning_bolt` entity ignores friendship. Keep role, stage, service, magic-suppression and lifecycle guards. Native damage cooldown is unchanged. Earlier AGENTS lightning team rules are superseded only within this requirement.

## Blocking paths

| Path | Current gate | Minimal recommendation |
| --- | --- | --- |
| `src/main/java/me/skywars2/game/LbcBolts.java:236` | `target` calls ordinary wands target, excluding owner and friendship. | Leave ordinary targeting unchanged. Add chain-specific eligibility using `wands.validTarget`, same level, suppression/nomagic and existing custom-effect guard. |
| `src/main/java/me/skywars2/game/LbcBolts.java:253` | `chainTarget` delegates ordinary target. | Use chain-specific eligibility for initial acquisition (330-331), retained targets (549), and propagation (558-560). Both existing calls to `targets` prefilter allies/self, so merely changing chainTarget is insufficient. |
| `src/main/java/me/skywars2/game/LbcElectro.java:136` | Ordinary managed bolt uses projectileTarget; egg/SELF tag exceptions vary. | Route every bolt to shared role-safe lightning eligibility, including owner. |
| `src/main/java/me/skywars2/mixin/KfcLightningMixin.java:27` | Independent Cultus AND mobgun-owner projectileTarget check. mobgun native owner resolves any bolt cause (`LbcMobgun.java:236`), so this impacts ordinary managed bolts too. | Both conjuncts must use shared lightning eligibility; changing only Electro wrapper cannot work. |
| `src/main/java/me/skywars2/game/LbcKfcRewards.java:186` | Final owned direct-source damage invokes projectileTarget. | Lightning direct-source branch uses same eligibility; other Cultus damage unchanged. |
| `src/main/java/me/skywars2/game/LbcKfcRewards.java:190` | Cultus bolt targeting invokes projectileTarget. | Same shared bolt eligibility. |
| `src/main/java/me/skywars2/game/LbcDamage.java:111` | Friendly-fire exception recognizes conduction, meditation and egg only. | Include direct source instanceof LightningBolt, preserving existing exceptions. |
| `src/main/java/me/skywars2/mixin/LbcDamageMixin.java:33` | Native ServerPlayer canHarmPlayer wrapper relies on preceding exception. | Shared exception must reach this wrapper; do not bypass victim roles or stage checks. |
| `src/main/java/me/skywars2/game/LbcDamage.java:128` | Reflected-owner projectileTarget can reject allied lightning. | Use lightning eligibility for actual bolt direct source only. |

`LbcWands.java:1152` includeAllies still excludes owner. Therefore `target(owner,target,true)` alone does not implement self. `validTarget` (1156) retains alive, removed, spectator, rcdenyalways, sauvojen_tuntija, strict_map_object, magic_academy, transformator and player gameplay-role checks. Use that shared predicate for self as well.

## Attribution and ignition

Local native source inspected in `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `net/minecraft/world/entity/Entity.java:2915`: thunderHit increments fire and potentially ignites before `hurtServer`; damage at 2921 is ownerless `damageSources().lightningBolt()` (5 damage).
- `net/minecraft/server/level/ServerPlayer.java:982`: attributed player source is checked through canHarmPlayer at 987 before super damage and ALLOW_DAMAGE. Native canHarmPlayer also includes level PvP setting at 999; current electric wrapper bypasses the complete call. Preserve project convention unless user separately asks to distinguish PvP from friendship.
- `net/minecraft/world/entity/LightningBolt.java:137`: native strike calls entity.thunderHit. Cause is separate and is not automatically inserted into damage.

Ensure all Entity.thunderHit damage gets actual bolt as direct entity even for weather/unmanaged/channeling bolts. Keep vanilla lightning damage type for native weather/Cultus, managed electric type for managed bolts, and existing actual player owner where present. Otherwise global `instanceof LightningBolt` checks cannot recognize native weather strikes. Do not fabricate a player owner for weather.

`ElectroNativeMixins.java:44-48` currently tracks block-fire only for managed bolts with cause. Generalize lifetime/block-write tracking to every actual attributed bolt, using cause or resolved authoritative owner. `LbcIndirectKills.java:255` currently sets Owner.friendlyFire only for eggLightning; set it for all LightningBolt world writes. Otherwise delayed fire blocks hit friendly players through `allowEnvironmentDamage` at 286-291.

`ElectroNativeMixins.java:69-70` records direct ignition with egg-only flag; `KfcLightningMixin.java:40` records it with default false. Both wrappers may process the same native impact and overwrite burning ownership. Every lightning impact must consistently record friendlyFire=true, with authoritative owner preserved. Record ignition even when immediate hurt is rejected by native hurt cooldown: native fire has already been applied.

The current friendlyFire environment path uses `wands.target(owner,target,true)` and an owner==target early allowance (`LbcIndirectKills.java:290`). Shared role-safe eligibility should replace the lightning-specific self allowance if adding a distinct lightning policy; do not broadly alter self-owned nonlightning hazards without scope evidence.

## Global events and guards

`SkyWars2.java:141-145` ALLOW_DAMAGE delegates to `LbcItems.allowDamage` (673-677). It preserves observer-attacker gate, active victim-stage gate and final wands/Kfc checks. `GameController.allowDamage` (540) requires playing, participant lives, connected state, no pending respawn and no observer. Keep these.

`LbcWands.allowDamage` (1190) contains summon-owner, blaze and consuming/trial gates before projectile checks. Lightning is not Projectile and ordinary player-owned bolt currently falls through after those unrelated owner branches. Do not add a broad early return bypassing those systems unless role-safe lightning handling deliberately replaces them.

`GameController.damaged` (595-599) is post-damage notification, not friendship rejection; it keeps match and active-attacker checks. No change needed.

## Scope and review limits

No Java, resources, build files, native cooldown tags, invulnerability timers or server state changed by this audit. Test recommendations: same-team player, owner, allied summoned mob, hostile control and protected role; ordinary managed bolt, egg, Cultus, cause-bearing unmanaged bolt and ownerless weather; immediate impact plus ignition/block-fire delayed damage and attributed death. Check Chain initial cast, retained target and next generation separately.
