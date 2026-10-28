# Complete Assassin armor вЂ” 28 October 2026

This continuation ports all four original Assassin pieces and their full-set
ability. Existing vanilla equipment, source attributes, durability, rarity,
models, translated names/lore and original reforge weights are unchanged.
No resource-pack update is required; the author's actual pack remains read-only.

## Source and behavior

Legacy references are relative to
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/items/changed_inventory/sel.mcfunction:46вЂ“48` detects four worn
`assasin:1` pieces; `function/players_tick.mcfunction:18` runs
`function/armor/assasin_set_bonus.mcfunction` each active tick outside Magic
Stopper. Whole-source review found no additional Assassin attack/craft engine.
Witch Indolence's similarly skinned equipment lacks this flag and remains a
different producer.

The complete outfit refreshes hidden Invisibility II for100ticks, clears
Darkness and Blindness, and emits the exact authored black-concrete-powder
block-marker decoys. There are16positions, each repeated at heights1.6 and0.6;
each emission retains count3, spread0.2, speed0 and forced delivery. The
asymmetrical source positions are intentional preservation, not a circular-grid
replacement. Only enemy player viewers receive these decoys; the wearer,
teammates, other dimensions and technical/observer roles do not. Visual audience
checks do not borrow damage immunity: Greed does not hide someone else's decoy.

The shared `LbcArmorSets` scheduler owns this behavior. Its existing five-tick
Erosion and twenty-tick Paladin/Shitty phases retain their cadence. Fixed worn
slots are read directly; there is no new inventory scanner, per-player task,
actor, timer, cached equipment or persistent modifier. Equipped passives continue
under ordinary stun; shared role, dimension, Time Stop and match epoch guards
remain. Magic Stopper prevents new refresh/decoys, leaving a previously granted
Invisibility effect to expire naturally. Equipment removal likewise stops the
refresh without removing unrelated ordinary effects. Mechanism suppression and
the personal no-magic aura are not extra source admission gates.

## Acquisition and existing advancement

Five existing packaged source tables cover the four leaves and one-piece root.
The two previously unpackaged original `_pre` and `_full` roots are now included:
`_pre` makes five independent one-piece draws with replacement; `_full` provides
one of each of the four pieces. All seven are exposed in the ready `/loot`
catalog. Normal armor reforge already selects the exact original family at
source branch10; Ultra reforge already rerolls the original family. No broad
unported armor/Lucky Block pool is enabled by this patch.

The original Assassin goal and loadout gate already exist. The native
`player_killed_entity` advancement retains its full-equipped/PvP predicates,
parent, icon, translations and reward. This patch does not replace the criterion
with a synthetic grant. Registration/type are checked; an actual fatal PvP event
is not claimed as new verification.

## Evidence and limits

`AssassinArmorChecks` exercises actual source loot, all four weighted normal
reforge branches, four native Ultra reforge operations,0вЂ“4worn pieces versus
backpack copies, native effects/cleansing, exact32ordered particle packets and
native teammate exclusion. It checks actual MAG/TECH markers, source no-magic
and stun distinctions, role/dimension/ENDING guards, Time Stop and three real
scheduled ticks. Fixture equipment, connections, advancement criteria and shared
phase are restored. The source `_pre` test admits duplicates rather than assuming
five random draws necessarily contain every piece.

`tools/smoke_armor_sets.py` also runs the previous three armor sets, ordinary
accessories, native gear, passive consumables, signed effects, independent layers
and Time Stop/contact regressions in two actual mode1matches. Normal child stop
and exact settings/profile/world-scoreboard/mod snapshot restoration are
required for qualification. No physical-client screenshot, fatal PvP, arbitrary
network conditions or peakTPS measurement is claimed.

Evidence is in `build/verification/assassin-armor/`. Its source/resource proof
archives28hashes, verifies unchanged source root/pre/full JSON semantics and
existing item/model definitions plus RU/EN translation keys. Initial compilation
found renamed26.3native classes/accessors and copied fixture imports/helper names;
these were corrected before the successful build and native run. No failed
candidate was installed.

Final build `78fc5140` passed26recorded results in two actual matches
(104.69seconds), plus two scoped third-player setup/cleanup checks.
Exact535sources,1517classes and3228processed resources match the tested JAR.
All198protected author paths and29live grimoire override JSONs remain unchanged.
Normal child shutdown and exact snapshots restored. The exact ready artifact is
`build/verification/skywars2-assassin-armor-78fc5140.jar`; qualification and installation receipts are in this scope.

Automatic installer11752 waits for the main server's ordinary shutdown and
world unlock before replacing current`ccac368f`; no forced stop or restart.
Receipt status at handoff: `waiting-for-normal-shutdown`. Retain78fc5140ready,
ccac368fcurrent/rollback,746e62aafallback and the separate network-audit tool.
Obsolete154cc3a7was removed after exact path/hash and retained-artifact checks,
freeing40,740,830bytes; cleanup receipt is archived. This is a qualified queued
release, not a claim that the running process already loaded it.
