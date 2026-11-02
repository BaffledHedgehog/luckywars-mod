Final author answers and qualification supersede this provisional handoff: [closure](BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md).

# Native minigun crossbow — implementation frozen 2026-11-02

Owned files: `LbcMinigunCrossbow.java`, `MinigunCrossbowChecks.java`. No central registration, mixin, config, build, server, git, RP or pending-state changes by this owner. Root owns shared successful-family callback and fixture registration. No compilation or native execution claimed.

Original functions under `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function`: `items/crossbows.mcfunction` dispatches `fast` and `fast_old` only outside TECH suppression; `workingitems/fastbow.mcfunction` reloads/debits one, exempts infinite-arrow holders, then increments `adv_game_minigun_shooted` and awards `lbc:true_advancements/bows/minigun` at320. `fastbow_old.mcfunction` has unconditional debit and no achievement/infinite exception. Those old/current policy differences are retained; explicit user ammo/weapon preservation overrides destructive whole-weapon/generic-arrow replacement.

Pinned locally against `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar` before implementation:

- `CrossbowItem.performShooting`: empties CHARGED_PROJECTILES before `ProjectileWeaponItem.shoot`, never empties afterward. Therefore root calls `LbcMinigunCrossbow.afterShot(GameController,ServerPlayer,InteractionHand,ItemStack,List<Projectile>)` at shared native shoot return, once per successfully published final family. Pass the actual live weapon, not a snapshot.
- `ProjectileWeaponItem.draw`: native projectile count comes from public `EnchantmentHelper.processProjectileCount(ServerLevel,ItemStack,LivingEntity,int)`; this same native effect API sizes reloads. No manual multishot table.
- `Player.getProjectile`: native supported-held (offhand first), then all-supported inventory order; creative fallback fabricates a generic arrow. Reload reuses native held helper/predicates/inventory order while omitting that fallback, so no physical ammo means no reload, including creative.
- `Inventory.clearOrCountMatchingItems(predicate,true,0,craftSlots)`: read-only counting includes inventory/equipment/offhand, supplied craft slots and carried cursor. Same source-holder policy as existing SLR native port. Craft/cursor are holder scope, not ordinary native ammo-selection scope.
- `Inventory.removeItem(ItemStack)` uses identity, including equipment, so final selected offhand ammo is removed exactly.

An overload adds final `ItemStack firedWeapon` pre-shot snapshot. Root should use it when available so a terminal breaking shot still increments the source counter while the actual empty weapon remains broken and cannot reload. The five-argument form delegates using the live weapon as its identity snapshot.

Only CHARGED_PROJECTILES is changed on the actual weapon. Reload copies every selected ammo component with `copyWithCount(1)`; debit touches exactly the selected reference once. Native tipped/spectral/custom arrows work; rockets work when held as allowed by native crossbow selection. Nothing mutates the already-fired family or invokes trinkets. Native shot damage/durability remains native. TECH skips the reload function and its counter, preserving the already-published vanilla shot. No periodic tick, inventory poll, NBT world scan or persistent service state.

`MinigunCrossbowChecks.run(GameController)` is a prepared authoring fixture using actual `owner.gameMode.useItem`. Cases cover repeated ordinary/custom/tipped/spectral native shots and exact pickup components, all non-charge weapon components, native durability, offhand weapon/ammo, multishot charge family/final stack/depletion, craft/inventory infinite holders, source-specific old policy, held custom rocket, no-ammo counter319→320 actual advancement, and TECH native shot without reload/counter. It restores fixture inventory via existing snapshot, its scoreboard/advancement/crossbow stat, suppression actor and created actors. Root may register group `minigun-crossbow`. These checks have not been run in this frozen stage.
