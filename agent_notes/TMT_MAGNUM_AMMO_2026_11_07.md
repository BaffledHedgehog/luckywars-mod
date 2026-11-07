# TMT Magnum ammunition intake — 7 November 2026

Component verified internally; not installed. Full TMT remains unreleased.

All six existing native Magnum rounds are one ammunition lottery family with
six payload variants. Its domain is selected-mode ammunition in the native
inventory/crafting search, not a second bullet effect or a new held spell.
The original native round markers and complete source components are retained.

LbcMagnum.use delegates its existing matching predicate to LbcTmtInventory.matches,
which reads immutable private property slots without projecting or changing the
ammo stack. The original clearOrCountMatchingItems call still spends exactly
one physical unit before the initial shot and four scheduled continuations.
Inventory, crafting and cursor search order, mode guards, cooldown, suppression,
native slot cleanup and burst retention remain owned by vanilla/the existing
Magnum engine. Multiple matching private properties do not add consumption.
Mode reading now uses the cached scalar reader rather than copying full NBT.
No additional per-tick inventory search, ammo owner, queue or retained cache.

TmtMagnumAmmoChecks runs thirty real mode-by-carrier combinations, using legal
count1 for native unstackable transports and count2 for snowball/apple. Outer
round markers are deliberately zero: matching must reach the private source.
Each accepted burst makes one initial shot plus four real LbcItems scheduled
shots; full remaining components/private siblings survive without ACTIVE.
Wrong modes, empty slots, inventory-before-craft-before-cursor, offhand,
sneak cycling during cooldown, ordinary MAG, TECH and canonical TimeStop input/
accepted-queue pause are checked. No direct Magnum.tick call substitutes for
native scheduling. Actual gameMode input also covers a held Magnum/round TMT:
count1 carrot consumes itself and cancels the queue next frame; legal snowball
count3 spends one ammo unit plus the ordinary native carrier throw, retains1,
and completes its four continuations with the original held object.

Test e588e673 and production f9debc4f carry the full qualified206f0d5c/2d41f522
parent. Eight suites pass: tmt-magnum-ammo, magnum, tmt-ammo, tmt-native-use,
tmt-inventory-traits, tmt-catalog, uniform-item-pools and typed-uniform-item-pools.
Normal child shutdown, exact fixture restoration and absence of native failures,
cleanup errors, live child PID or retained locks are verified. Exact SHA256s and
reports: build/verification/tmt-magnum-ammo-2026-11-07/component-verification.json.

Four Java families, zero resources; 10,469 other guarded inputs and all282 author
paths preserved. Author HUD/four WIP sources were not edited or compiled.
One512MiB compiler and one2GiB fixture ran sequentially and exited. Main/libs
and author RP remain unchanged; no peak-TPS or physical-client claim.

Fresh catalog963families/1827variants/106pending native-domain rows does not
measure full completion. Three legacy owners and remaining charged/loose/native
domains, acquisition, atlas integration and coherent release remain pending.
BadApple's next boundary can be much smaller than a generic retained dispatcher:
recognized is its only item-data read, the existing whole-body actor already
owns the complete lifecycle, and a private source projection adds no payload
semantics. Preserve that claim contract and qualify the real body/playback path.
