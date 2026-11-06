# Seven native passive accessories — qualified, installed (6 November 2026)

Seven existing, ready passive accessories now have canonical server item identities
and the common Polymer client proxy. Their abilities remain owned by the existing
services. No extra tick task, inventory scan, NBT parser or ability implementation
was added. The canonical prototype reader runs once for each item at registration.

| Item | Ready loot table | Native identity |
| --- | --- | --- |
| Sand Amulet | `lbc:ported/sand_amulet` | `skywars2:sand_amulet` |
| Save Platform | `lbc:ported/save_platform` | `skywars2:save_platform` |
| Regeneration Belt | `lbc:ported/regeneration_belt` | `skywars2:regeneration_belt` |
| Mana Belt | `lbc:ported/mana_belt` | `skywars2:mana_belt` |
| Magnet | `lbc:ported/magnet` | `skywars2:magnet` |
| Devil Stomach | `lbc:ported/devil_stomach` | `skywars2:devil_stomach` |
| Infinite Quiver | `lbc:ported/infinite_quiver` | `skywars2:infinite_quiver` |

The Mana Belt public leaf is newly exposed. The other six public leaves already
existed. Original reforge/acquisition references resolve the same canonical source
items. Seven source changes replace only the carrier ID; every other function,
count, weight, quality, condition, nested PublicBukkitValues, name, lore and tier
is preserved. The additional Mana Belt leaf references the canonical definition.
Legacy Balance aliases retain their distinct flags, lore, tiers and identities.
Existing raw-tagged saved stacks remain recognized by unchanged shared consumers.

The common deferred prototype factory supplies identical native /give defaults
and canonical loot components, including vanilla EPIC rarity. A SimplePolymerItem
uses the original command-block client carrier without inheriting block placement.
Every existing model and RU/EN translation was checked read-only in the author RP.
No resource-pack additions or edits are necessary.

## Validation

Test `7644e266` / production `73392944`; exact full hashes and archive paths
are sealed in `build/verification/native-passive-accessories-2026-11-06/qualification.json`.
Seven fresh Java families, eight exact processed resources; 10243 unchanged source
inputs and 282 author working paths guarded. Retained binary linkage audit passes.
No author WIP classes were overlaid; production excludes MapAuthoring/MapDeletion.

Eight actual isolated native matches passed on the exact test archive:

- Mode 1: ordinary-accessories, regeneration-belt, quiver, incoming-artifacts, mana-core.
- Mode 3: ordinary-accessories, incoming-artifacts, mana-core.

The accessory scene checks all seven native default/loot identities, exact
components, Polymer proxies and server codec round-trips. Existing scenes exercise
real weighted reforges, inventory scopes, preserved legacy variants, magnet item
movement, void rescue platform, Devil Stomach refresh/expiry, health regeneration,
Quiver generation and payloads, Sand Amulet incoming/outgoing protection and actual
native Mana Belt gear refresh. Native pickup-stat snapshots were updated for the
new Belt/Quiver identities. Existing suppression and TimeStop rules are unchanged.

Fixtures shut down normally, restore their snapshots and auxiliary files, leave
no live child, retained lock or temporary local map. This does not qualify a
physical client, peak-load TPS, full disk restart or the separate broad rewind gate.

A first compile caught a missing test-only Item type; corrected before packaging.
Candidate 04ee58f2 passed all five mode1 scenes, then exposed an old fixture
assumption: mana-core expected immediate damage admission after native body
replacement in BedWars. Its real 100-tick spectator respawn interval is valid.
The fixture now requires the fresh live body plus that genuine pending phase,
while retaining every exact mana/status persistence assertion. Gameplay timing
was unchanged. Failed evidence remains in the stage; all eight scenes were rerun
on the final archive. No rejected candidate was
installed. Gradle used 512 MiB/one worker; fixtures used 2 GiB and ran only after
memory recovered, one heavy process at a time. Only the confirmed own Serena JDT
was stopped; IDE, TFG and Velocity were preserved.

## Release

The qualified test archive is installed to the stopped main server and both
build/libs archives match their qualified hashes. No main restart and no push.
Receipt: `build/verification/native-passive-accessories-2026-11-06/main-install.json`.
Full IMBA, TMT-last, custom/mixed/type catalog, mode, skill2 and broad rewind gates
remain unchanged. This batch closes seven native item identities, not those gates.
