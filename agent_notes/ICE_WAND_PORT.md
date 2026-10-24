# Permafrost / Ice Wand native port

Source (read only): `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/ice_wand/` (28 functions).

The native service is `src/main/java/me/skywars2/game/LbcIceWand.java`. Shared item registration, costs, Frost transitions, damage allegiance exceptions and verification belong to the integration change. This note records the core contract; it is not a qualification claim.

## User rulings relayed by the parent task, 24 October 2026

Author answers (user):

> Сделать 5/5 по описанию

> WILD, 24% игрокам / 40% мобам

> так он вроде и не может в блоках появиться. Там типо блоки ледяных кристаллов создаются которые потом раскалываются. Ну пусть будет прежний блок восстанавливать

- Projectile and crystal damage are **5 / 5**, following the item description, superseding source functions' 6 FREEZE / 8 ARROW.
- Academy Shift shatters **all** frozen eligible entities within 50, including caster and allies (WILD). Direct ARROW uses **24% current player HP / 40% current mob HP**. Splash is 10 FREEZE within 4, excluding only the caster and protected server roles.
- Restore the previous native block snapshot only while the owned barrier remains. Crystal placement requires an AIRS candidate cell; crystals cannot spawn inside solid blocks.

## Source and native behavior

Ordinary and Academy normal casts launch an owned native Snowball at velocity 1.5: source .5 vector multiplied by three. The ten-million-X motion refresh workaround is omitted. Its paired native item display uses `lbc:ice_crystal_proj`, translation Y -.04, initialized at actual carrier coordinates with full pose before publication. Native first contact triggers exactly one radius-three impact, native FREEZE with the NO_KNOCKBACK tag, +10 shared cold unless already frozen, and one crystal attempt. Snowball's own zero/Blaze hit is not called.

Crystal support order is DOWN, UP, EAST, WEST, SOUTH, NORTH; only if no adjacent support exists does the distance-two search move the candidate one cell towards support. Actual native wall impacts offset the placement origin .01 towards the hit face's air side, keeping the damage center at the real collision point. The model `lbc:ice_crystal_small` preserves source rotations. The paired Interaction is 1.2 by 1.2 at center Y-.6. The barrier owns a detached MapManager snapshot. Model/hitbox ownership is established before native publication callbacks.

Crystal expiry is 400 active ticks. Any player attack, another eligible non-grounded projectile within 1.5 of the source probe, or Shift of the owning caster within 50 triggers radius-three ARROW/cold explosion at the Interaction origin (display Y-.6), as retained by the source execute-as chain. Ice shots do not trigger crystals. Caster and allied players are excluded from ordinary impact/crystal damage; friendly mobs remain eligible. Explosions prime nearby crystals to two ticks, preserving the source chain cadence. Academy shatter clears shared ice/Slowness before the native source damage sequence. Ordinary hurt cooldown remains native.

Actual ice impacts/crystal explosions can complete an ordinary trial only for the original initiating hand and identical live ItemStack still held there with the trial flag. Epoch, match ending/stop, participant death/forget, antimagic at actor position and native unload cancel actors and restore an owned barrier if still present. Time Stop pauses native flight and crystal lifetime. Flight checks its entire resident DDA segment before native Snowball physics and does not force-load chunks.

## Integration interfaces

`register()`, constructor `(GameController)`, `use(owner,hand,stack,academy)`, `loaded`, `unloaded`, `tick`, `forget`, `stop`, and `attack(ServerPlayer,Entity)` (managed Interaction only). Package fixtures: `count`, `snapshot` (crystals), `shotSnapshot`, `spawn`, `spawnCrystal`, `impact`, `explode`, `shatter`. Shared Frost supplies `iceHit(target,owner)`, `isFrozen(target)`, `shatter(target)`. Shared allegiance recognizes `LbcIceWand.WildSource`; native `ImpactSource` overrides only `DamageTypeTags.NO_KNOCKBACK`. Shared magic projectile guards include IceShot; native ArrowAccess exposes in-ground state for exact projectile eligibility.

No old server/resource-pack write, broad pool enablement, build, deployment or visual/client qualification is implied by this implementation note.

## Qualification

Qualified build `04c4694d` passed 42 recorded suite results in two actual matches (148.04s). Exact 296 sources / 800 classes / 2786 resources match the tested artifact. Installed stopped main; rollback `58a78a13`, server not started. Native boundary fixtures include manual ticks, no client pixel/actual disk restart/unload/disconnect or peak-load claim. Evidence: `build/verification/ice-wand-final/`.
