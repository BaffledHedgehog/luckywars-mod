# Frozen entity action guards

Action input guards are attached to the existing Fabric player callbacks and server packet hooks.

- Frozen players cannot start item use through the main hand/offhand item callback, interact with a block, or interact with or attack an entity.
- Frozen players cannot execute the native use-item, use-item-on-block, interact-entity, block-break, spear-stab, or item-release packet paths. Use-item guards run after vanilla queues the block-change acknowledgment; they resend inventory contents and, for block use, the clicked and adjacent loaded block states within reach. Destroy actions queue their sequence acknowledgment and resend the targeted loaded block state within reach. Releasing an item while frozen stops use and cancels the release. Denials do not load chunks from arbitrary packet coordinates.
- Container click handling is untouched, so inventory rearrangement remains available. Existing observer guards and lobby menu callbacks remain in place.
- A frozen mob's currently active item use is stopped by the freeze lifecycle supplied by `LbcFrost`; no broad damage-source filter is used, so launched projectiles and passive damage effects continue.

Root integration adds two native guards in existing mixins: `PlayerMechanicsMixin`
cancels `Player.attack`, and `CreativeBlockDropsMixin` cancels
`ServerPlayerGameMode.destroyBlock` and clears existing immediate/delayed mining
progress on its next tick. Vanilla abort alone would leave delayed mining active.
No extra mixin class, polling inventory scan, or reflection was added.

The callbacks and packet methods provide coverage for native and custom items that use vanilla interaction routes. Direct custom effects invoked outside those routes need their own frozen-state guard. Mob AI attacks are not canceled here; a damage callback would also cancel already-launched projectiles, so that broader hook is not suitable for the requested input freeze.
