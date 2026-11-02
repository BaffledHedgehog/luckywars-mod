Final author answers and qualification supersede this provisional handoff: [closure](BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md).

# Native shulker bow implementation handoff — 2026-11-02

Owned files: `src/main/java/me/skywars2/game/LbcShulkerBow.java`, `src/main/java/me/skywars2/mixin/ShulkerBowMixin.java`.

Frozen for root integration. No Gradle/build, native/server execution, git, resource-pack, or runtime file changes were performed. Source signatures were read from the existing local Minecraft 26.3 source jar. Execution qualification remains root/test-owner work.

## Contract and integration

- Constructor `LbcShulkerBow(GameController)`; `augment(ServerPlayer,ItemStack,List<Projectile>)` transforms exact successful native arrows only for raw `shulker_new:1`. MAG at owner suppresses augmentation and leaves the original native arrow. Failed replacement publication retains the original arrow. No trinket shot/RNG calls.
- Native `ShulkerBullet(EntityTypes.SHULKER_BULLET,level)` primary has no target. Owner is exact real shooter; motion and orientation come from the accepted actual arrow. Native movement, gravity, deflection, collision and visual identity remain native. No hidden arrow entity exists.
- Register mixins: `ShulkerBowMixin`, `$ArrowAccess`, `$SpectralAccess`, `$BulletAccess`.
- Root owns LbcItems initialization/accessor/lifecycle (`loaded`, `unloaded`, `tick`, `forget`, `stop`), finalized-family trinkets hook, config and clone aggregation. New service provides `cloneImage` and `cloneRecognizes`.
- Mixin invokes generic trinket `beforeProjectile` at native Shulker tick HEAD, before bow-specific hook. It invokes `bedrockBeforeNativeMove` immediately before native ProjectileUtil movement collision, after native steering. Root's existing Projectile.onHit TAIL delivers generic trinket hits; this mixin does not duplicate hit callback.
- Root must add Quiver helpers: `shulkerLightTrail(ServerLevel,Vec3,LbcIndirectKills.Owner)` delegates existing full 200-tick light trail, and `acceptCarrier(LivingEntity,MobEffectInstance,ServerPlayer nullable,LbcIndirectKills.Owner)` accepts preserved special-ammo effects without potion sentinel transport.

## Typed state and preservation

Full accepted ammo and weapon stacks are copied once. Typed state stores native baseDamage/critical/pierce/spectral duration/fire/pickup/cause/team/epoch/contacts; entity native state retains position/motion/noGravity. Native save/load uses ValueOutput/ValueInput with ItemStack codecs; cloning deep copies stacks and contact history. Missing typed state on recognized bow tags fails closed. Native child target reference/direction/flight steps remain native serializer state.

A successful hit emits one damage event, native levitation, preserved potion contents/duration scale, spectral glow, weapon enchantment damage/postattack/knockback, source fire, and arrow-kill criterion. Piercing contact history suppresses repeats, and native terminal destruction is bypassed only while primary piercing budget remains. Native block hit runs weapon enchantments and drops exact ALLOWED ammo; CREATIVE_ONLY/DISALLOWED do not drop. This adapts recoverable arrow ammo to a native shulker endpoint that destroys instead of sticking in a block.

TECH at released bullet position or crossed motion segment deletes managed bullets. MAG only suppresses launch transformation; coexisting traits and existing Quiver markers keep their own service suppression. Frozen/time stopped carriers do not progress. Resident-chunk guards avoid chunk loading. Unrelated armor/drop bullets have no typed state and retain native damage/targeting/lifecycle.

## Source closure and decisions held for author

Original chain read: `workingitems/shulkerbow_new`, `projectiles/shulker_bullet`, `projectiles/shulker_bullet_generate`; existing LbcQuiver owns light trail endpoint and marker lifecycle. Light primary emits one 200-tick trail per running tick. Trapper emits one native child each running tick, copies source authority, targets nearest eligible mobs within100, and never propagates trail or parent trinket traits to children.

**Release held pending author damage decision:** prototype currently uses `max(4, native arrow velocity/baseDamage/critical/enchant damage)` in a single hit, retaining native shulker levitation. Root has asked the author whether four fixed damage or upgraded arrow damage should govern; do not claim this prototype policy is approved.

**Genuine source trapper defect, held pending author decision:** source summons a default stationary shulker bullet and only writes Target. Native default fields are Steps0/Dirnull/target deltas0/motion0, so it never selects a direction and remains stationary. Prototype uses native targeted constructor plus native selectNextMoveDirection from parent position, providing homing without custom AI. Root has asked the author whether to correct the defect.

Potential custom-stack composition: source `shulker_new` and `madoka_bow` can coexist if manually authored. Root should route generic accepted-hit Madoka callback from the managed single-damage success branch if Madoka marking is generalized to this carrier; ordinary canonical Shulker loot does not carry Madoka.
