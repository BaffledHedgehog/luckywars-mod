# Complete Glass armor — 28 October 2026

Source closure and exact original assets: [source audit](GLASS_ARMOR_SOURCE_2026_10_28.md). Runtime qualification is recorded below after the native run; source descriptions alone are not proof of working behavior.

## Author decisions (user)

> ничем не глушить - считается что это тупо осколки брони разлетаются. Однако, надо переработать урон от неё и немного занёрфить. Сдлелай, чтобы урон от брони могли получать только мобы которые не прикрыты блоками (логика типа от осколков защищают укрытия), и сделай, чтобы урон от осколков был в разы меньше чем сейчас когда стеклянная броня не доломалась хотя бы до 2 едениц брони на элементе

> Менять только защиту и твёрдость

> урон от осколков не должен триггерить повторные срабатывания брони к слову!

These override the old whole-stack loot replacement and unrestricted through-wall burst. Both physical wear and shards ignore MAG, TECH, personal noMagic and action stuns. Ordinary match/role/dimension/Time Stop admission still applies.

## Implementation

LbcGlassArmor joins the existing native EntityHurtPlayerTrigger bridge, including its pre-hurt eligibility capture for fatal events. No second generic damage callback is introduced. Shield-blocked and absorbed criteria count; rejected attempts do not. The source checks any raw glass:1 stack in the four equipped armor slots, not held/inventory items and not a mandatory full set.

Each equipped piece changes armor and toughness to floor(armor*1000)*0.00095, using the original item armor amount rather than the wearer's total armor. Preserve modifier identifiers, operations, equipment groups and display settings. All other components, unrelated attributes, custom data, durability, enchantments and player naming remain. Unused legacy scratch scores, fresh-item resets and the unrelated unused 90-percent item modifiers are not reproduced.

Read the post-wear armor values A_i. Retain original raw shard calculation R=4*N-floor(sum(floor(0.17*A_i))/N), with its integer floor/overflow behavior; skip R<=0 and N=0. The author-requested nerf multiplies positive R by min(1,2/mean(A_i)), using1 when the mean is<=2. A fresh full set40->38 therefore deals10*(2/38), approximately0.5263158HP before the victim's ordinary defenses. At2armor per piece the factor is1. Single/partial sets retain their original raw thresholds.

The wearer is the local native ARROW source, without a helper projectile or global attacker tag. Preserve spherical feet distance .001..7, ordinary hostile target/role/Greed protection and player/mob friendships. Snapshot the burst origin and center. Vanilla ServerExplosion.getSeenPercent uses actual COLLIDER block shapes to sample target exposure: a full stone or glass wall blocks all shards; partial stone/slab cover proportionally reduces damage. The broad query includes the exact positive-Y7 boundary before the precise spherical predicate.

The later author clarification forbids shard-triggered armor reactivation. A dedicated ShardDamage class retains the ordinary ARROW damage type and local wearer attribution. The Glass criterion rejects this source before any armor wear or secondary burst. The shared attribution wrapper exposes the original damage identity, including nested captured sources, so wrapping cannot bypass the exclusion. Ordinary arrows still activate Glass normally, including shield-blocked arrows. Other incoming artifacts and armor callbacks are not globally disabled. No queue, delayed shard state, ticking service or retaliation loop remains.


## Acquisition and presentation

All four original leaves, equal-weight random root, five-draw pre table and four-distinct-piece full table are public ready entries. Add only the previously missing pre/full packaging. The existing normal slot-reforge and Ultra Glass-family rule remain authoritative. Do not enable unrelated Lucky Block pools. The original full-set PvP goal remains the existing imported advancement, without a duplicate award path.

Original leather bases, durability150, armor40/toughness40, white translated names, three aqua lore rows, per-slot IDs, lbc:armors/glass_* models, lbc:glass equipment and tier_b tooltip are unchanged. The source audit read-verifies RU/EN/RPR keys and original RP assets. No RP/template update is required. The down_armor loot files remain historical source; the approved attribute-only runtime does not call them.

## Qualification

Build76ff129e passed26recorded results in two actual mode1matches (103.02s): Glass, Nanobots, armor sets, Shulker, incoming artifacts, attribute policy, signed effects/layers and Time Stop. The owned native child stopped normally and exact fixture snapshots were restored. Exact552sources/1570classes/3234resources and build.gradle match the tested JAR;198preexisting author paths and29live override JSONs are unchanged. No source-server/RP writes. Native shielded/unshielded and doubly captured shard damage never wears or reactivates target Glass, while ordinary native ARROW positive controls still activate it. No new fatal PvP or physical-client/naturalTPS claim. Main baseline7350302f is the verified installed preceding batch. Installation follows the existing stopped-only worker; the exact receipt in build/verification/glass-armor/main-install.json distinguishes installed from waiting, without forced stop/restart.

The initial557d0e9e native run passed the shared neighboring suites in both matches, but the new Glass suite stopped on its partial-cover scene prerequisite. Vanilla sampled a grounded cow as fully covered by the one-block wall. Raising only that partial-cover witness0.5block makes its native sample rays straddle the obstacle; full-wall tests and exposure assertions remain unchanged. Initial proof/results/log are retained. The subsequent author no-reentry decision supersedes the initial iterative retaliation implementation before final qualification.
