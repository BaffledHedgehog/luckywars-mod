# Cloned fishing hook primary ledger ? 2 November 2026

Narrow implementation only; no build, native process or runtime qualification.

Pinned native source inspected directly from
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`,
`net/minecraft/world/entity/projectile/FishingHook.java`.

`setOwner` calls private `updateOwnerInfo(this)` (lines527?535). Removal and client
removal call the same method with null (lines516?523). That method's only mutation
is assigning the resolved player's `fishing` field. Native `shouldStopFishing`
(lines254?266) checks held rod, ability to interact and distance; it does not
require that this hook be the player's primary ledger entry.

`CloneFishingLedgerMixin` cancels that exact private method at HEAD only for a hook
carrying `skywars2.cloned_fishing`. Both primary-ledger claiming and clearing are
suppressed on cloned hooks. Native owner resolution, actual fishing physics,
retrieval and ordinary hook removal remain native. Untagged original hooks retain
normal primary-ledger behavior. One narrow mixin registration was added.

The root clone factory must tag before owner/native load operations, preserve the
original primary ledger if a constructor runs before tagging, and restore the tag
after native NBT replaces source tags. This mixin does not create bodies or modify
that root helper. Existing CloneSkillChecks fishing creation/removal assertions
remain the runtime qualification path.
