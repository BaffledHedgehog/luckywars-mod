# Boba map rotation and Hagalaz — native checks passed, not released

User decisions in chat:

- Hagalaz permanently removes Chef, including a knocked Chef.
- Wait for the entire original Chaos reward chain before updating the main mod.
- Both anti-magic beacon and stronger artifact prevent Boba use.
- Rotate respawn anchors and their pristine island restoration reference with Boba.

## Boba implementation

[Raw consumer/source details](BOBA_CONSUMER_2026_10_31.md). The full source cube is
320×320×320, Y=0..319, X/Z centered on the command origin. Within source integer
coordinate bounds ±16000, the origin is (0,160,0); outside it follows the caster.
The source `RotateMapService.java` in the old Paper plugin maps blocks as
`(x,y,z) -> (x,319-(z-zStart),zStart+y)`. Block facing/axis/NBT directions are
preserved, not rotated. Native entities use the separate continuous pivot at
Y=160, Z=zStart+160, retaining their yaw and pitch.

`MapManager.rotateMap` owns global admission, existing chunk tickets and warning.
`MapRotation` is accepted work in the existing Stonekivi terrain dispatcher. It
first copies native section palettes and full block-entity NBT, then applies the
immutable image through native block writes and existing MapLighting publication.
There are no 32-million-object cell records, off-thread world reads, helper mobs,
new function support or second map scheduler. Uniform-air source/destination
sections are skipped, with clipped unaligned edges checked explicitly. Changed
native cells retain source causality; same-state containers load saved NBT too.
Accepted work drains before Vanity capture, so the point cannot save half a flip.
Match stop/rewind of post-point work cancels through its existing boundary.

Source inventory-order `/clear` occurs before engine busy rejection, for either
hand. Shared main-hand input may also process the offhand, as with existing wands.
Modes 3/4 retain their separate native target teleport and original loot/particles.
Use is blocked by user-confirmed beacon/artifact policy and shared role/stasis
admission. Already accepted terrain work is not given a new mid-flight gate.

The selected vehicle/passenger graph is captured once, detached, transformed once
per body and reattached with native riding calls. Current momentum survives both
player and mob position changes. Native displacement attribution remains shared.
Current spawn/observer anchors and immutable pristine island cells transform too;
block entity coordinates are rewritten for their new cell. Participant respawn
anchors follow the map. Existing MatchCoreCheckpoint now retains this map-owned
anchor/reference state, so full rewind restores it without a separate job adapter.

## Hagalaz

`LbcRuneGlobals.clearEntities` retains the existing six cleanup passes and exact
maploader/lobby whitelist precedence. After admission it first calls the existing
`wtf.erase` owner; ordinary living entities still use inherited kills. Chef's
registry entries and chunk tickets retire through their native owner. This is
permanent removal, not another knock. Terrain work and mapstat remain unchanged.

## Evidence

Runtime/check offline build passed (`build -x sourcesJar`). Final archive:
`build/verification/boba-hagalaz-chain/chain-b51226c7.jar`, SHA-256
`b51226c7c44646cad723df339979bb994551f95f72eab2e126d1ddc72e4016de`.

Four focused groups passed in four fresh native mode1 matches in one child:
`boba`, `chef-execution`, `imba-runes-globals`, `vanity-rollback`.

Boba executes two complete 32,768,000-cell transforms: aligned fixed origin and
shifted unaligned origin. It checks actual native input/busy/debit behavior, both
MAG sources and hands, source block facing/chest contents/new BE position, clipped
boundaries, native actor pivot/momentum/riding graph, every pristine-island cell's
transpose and actual full-world/core/actor rewind between cases. Modes3/4 execute
both hands, source inventory debit, exact target coordinates/look/momentum and
real original loot actors; each adds 94 source items. Accepted construction must
block partial capture. Actual Chef checks cover active/knocked erasure, strict
whitelist protection, maploader override, inherited ordinary kill and ticket cleanup.
Existing sacrifice/nuclear/Stand/End/delayed-death/stasis assertions remain.

In this sparse native fixture each flip changed 56,506 cells. Aligned case:
811,008 actual cell comparisons, 363.429ms total, max work step47.8108ms. Unaligned:
1,875,696 comparisons, 285.8191ms total, max step50.0811ms. Logical coverage is
32,768,000 in each case; skipped palette cells are not misreported as comparisons.
These are bounded fixture measurements, not peak-TPS/dense-cube/client claims.

Initial `a297fb2e` failure receipts are retained. Its Boba fixture left a second
token in the other hand, legitimately causing a second source debit; its Chef
fixture prematurely marked the target as a protected map object before summoning.
Those setups were corrected without removing their original assertions.
`final-verification.json` confirms9,536 frozen inputs,196 unchanged author paths,
31 untouched live overrides, unchanged main JAR d9087f0a, normal child shutdown,
exact fixture snapshots and no retained locks. No RP writes, main install/restart
or push. Focused runner results are not relabeled full-release qualification.

## Remaining complete-chain work

A confirmed reachable dependency still needs porting:
Chaos evil-luckyblock trap -> native enemy DeathLootTable `lbc:luckyblock` ->
high-quality lucky items -> `barriersnottraps` -> `grimoire_dungeon` drop ->
Shadow Citadel rooms ->1000HP grimoire boss ->`grimoire_boss` loot ->Uni Wand.
The dungeon token is currently emitted by canonical item definition
`item_defs/lbc/drop_not_trap__77de94f9f93e` (barriersnottraps lines140/158), but
no native dungeon/boss/Uni Wand consumer exists. Uni Wand also needs original
grimoire-composition crafting and custom mana/tier/slot admission; existing
library spell owners cover effects, not that carrier. Worldgen resources alone
do not implement room triggers or the boss. Do not close the release chain based
only on table resolution or Boba's passing result. Main update remains gated by
the user's explicit complete-chain decision.
