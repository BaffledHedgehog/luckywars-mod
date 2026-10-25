# Negative mining attributes - 25 October2026

User explicitly chose unbounded `block_break_speed`, `submerged_mining_speed` and `mining_efficiency`; a negative effective mining rate clones the targeted block toward the player while LMB is held, with frequency determined by magnitude. User separately confirmed copying block entities **with their contents**. The root attribute-policy change supplies unbounded attribute values; this mechanic does not edit attribute ranges.

## Exact native trace

Local26.3 sources from `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `world/entity/player/Player.java:getDestroySpeed`: effective tool speed, conditional additive mining efficiency, Haste/fatigue, multiplicative block-break speed, eye-water submerged multiplier, airborne/5.
- `world/level/block/state/BlockBehaviour.java:getDestroyProgress`: native player destroy speed / native block hardness /30 or100 depending correct tool. Native unbreakable hardness-1 returns0.
- `server/level/ServerPlayerGameMode.java:handleBlockBreakAction`: reach and maxY validation, then spawn protection/mayInteract; creative calls the first `destroyAndAck` before normal survival block-action validation. Survival retains `isDestroyingBlock` after negative START. STOP normally moves insufficient progress into delayed destruction, which can never complete at a negative rate. ABORT clears ordinary hold.
- `ServerPlayerGameMode.tick/incrementDestroyProgress`: normal held/delayed progress uses native negative values without ever reaching completion. A client does send START while holding an unbreakable/zero/negative-progress target and ABORT on release/target change; this mechanic relies on those real packet boundaries rather than inventing a new input packet.

## Implementation

`NegativeMiningMixin` appends a custom session only after a native accepted survival START. It then clears native held/delayed state so vanilla cannot run an impossible delayed break. The exact first native `destroyAndAck` call is intercepted only for negative effective progress, adding creative support after native reach/height/spawn/mayInteract checks. Positive and zero creative progress keep vanilla instant breaking. Explicit checks retain the source's tool permission, operator-only-block permission and adventure restrictions.

The session lives on that player's native game-mode object, stores only source block/state, level, epoch and fractional credit, and receives one update per native game-mode tick. Native per-tick destroy progress is recalculated live; its absolute value accumulates until1. Each threshold attempts one copy. Credit saturates at1 and resets after the attempt, so large values never cause burst loops or queued work beyond one copy/tick. Two negative multipliers yielding positive effective progress retain native normal mining; negative efficiency uses the same native formula and only matters when vanilla applies that attribute to an effective tool.

The destination is exactly one neighboring cell in the dominant direction from source center to current player eyes. Only a resident air cell inside the border and outside spawn/interaction protection can receive a copy. An occupied neighbor stalls; it is never overwritten and the same held source never searches farther or grows an unbounded line. As native targeting changes to a nearer created block, a later client START can independently target that block.

The source's exact BlockState is copied, including orientation. A fresh full native block-entity snapshot, components and inventory are loaded into a distinct destination BE with destinationXYZ. This intentionally duplicates chest/container inventory as the user requested; mutable ItemStacks are independently decoded. The source is neither cleared nor mined, no tool durability is consumed by a completed copy, and no block/inventory loot is emitted. Existing MapManager no-spill write flags818 and snapshot load logic are reused. Native shared terrain attribution records possible suffocation/terrain consequences against the copying player.

Guards include actual active match/world/epoch, living eligible participant, reach, resident source/destination chunks, spawn/border/adventure/op/tool permissions, technical/observer roles, active Greed, Frost/stun and Time Stop. Guards that become invalid end the session; there is no deferred replay after protection ends. STOP/ABORT clear the custom hold and cannot create vanilla delayed destruction. Dimension change immediately clears the field. Match stop changes the epoch and the following native tick drops the session; no static registry or scheduled task survives logout/object disposal.

## Integration and focused checks

Register `NegativeMiningMixin` in the existing mixin manifest. Register `NegativeMiningChecks.run(GameController)` with GameChecks. No LbcItems constructor/service/tick/stop changes are needed.

`NegativeMiningChecks` sends real native START/STOP/ABORT actions and steps actual ServerPlayerGameMode ticks. It covers fractional threshold, greater magnitude, exact oriented state, unchanged source, occupied-neighbor stall/no line growth, no STOP/ABORT replay, negative efficiency, actual eye-water negative submerged speed, Time Stop and Greed cancellation, creative negative cloning, and a native chest containing13 diamonds. It compares all copied chest metadata/components exceptXYZ and proves mutable inventory independence by shrinking only the copy. Fixture map/player/attribute state is restored, and no clone-created item drops are permitted.

Compilation and native runtime qualification belong to the integrating root agent. No client-pixel, natural packet-network timing, disk restart or throughput benchmark is claimed.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
