# TMT inventory/consume/crafting traits and physical drops, 7 November 2026

Component verified, not installed; full TMT remains incomplete.

The catalog admits native Death Compass, Reroll Machine, Steampunk glove,
KFC/Govno and Oreshek consume properties. The two flagless native KFC identities
normalize to their existing consume selectors. Mechanics remain with their
existing owners. The native death tracker preserves TMT's procedural model
instead of overwriting it with the compass model; its scalar recognition no
longer copies the item's complete custom data. Ordinary compass behavior is
unchanged. The protected author Reroll source was not edited or compiled.

Native inventory splitting can copy the currently borrowed private property
view. The shared LivingEntity physical-drop factory now restores the full TMT
view before publishing such a drop. The existing outerCopy helper resolves the
real active source, including split copies sharing its immutable component and
physical item type, and restores its original payload components. Ordinary
items keep their original stack without another copy. Deliberate projectile
source copies keep their private context; no generic ItemStack.copy interception
or global cache was introduced. Vanilla Inventory.dropAll uses this same factory.

The new scene uses a genuine partial native inventory drop inside a property
scope. It verifies both the dropped/remaining full item and a non-default outer
potion component temporarily absent in the projected source. It also verifies
that ordinary producer copies retain their context. Actual native consumption
combines self-reproduction and an attribute reward with one food count/debit;
actual result-slot taking preserves the full mixed reforge catalyst while
spending one donor and 5000 mana. The scheduled native compass resolves the real
remembered target without changing TMT's model or private properties.

Internal test `93c5f4a71eb50ce6c599ac5dfa5b7eea6a6b5be4d800ec36718effe466690ca5`
and production `a1eb520fa1d976f8761af6cdac8aa2f053d802df46a2ac721c164e9f047a2fcc`
carry complete 3f76765b / 9d6f7172. Seven Java families and one mixin registration;
10,339 other inputs and all 282 protected author paths preserved. Main/libs,
author RP and concurrent HUD unchanged. Evidence is sealed in
`build/verification/tmt-inventory-traits-2026-11-07/component-verification.json`.

Nine actual mode-1 matches pass: tmt-inventory-traits, tmt-carriers,
tmt-block-use, totems, tmt-drops, tmt-native-throws, death-compass, tmt-catalog
and reroll-machine. Final reports verify normal shutdown, exact fixture
restoration and no failures, cleanup errors, live child or retained locks.
The source Reroll test runs separately with its original temporary two-stack
loot/actual-player-origin probe. The probe is removed and exact files restored.

Earlier diagnostic reports remain unqualified: block-use cleanup removed members
of the live entity iterator, and the source Reroll test was initially run without
its required probe. Cleanup now snapshots that iterator; the final candidate
passes the source Reroll test with the proper temporary resource setup. These
test repairs do not change either gameplay owner.

830 families / 1,602 variants is not full coverage. This does not add a lethal
player-death scene or exercise every random KFC/Oreshek branch. Remaining native
domains, pools/acquisition and full TMT release stay pending. No physical-client
rendering or peak-TPS claim.
