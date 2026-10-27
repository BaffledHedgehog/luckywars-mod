# Missing-island respawn and nearby chests - 27 October 2026

Author correction (user): «поправлю - если местность впорядке - восстанавливай только сундуки! (блок + заполнение)».

Every genuine remaining-life respawn now restores missing marked chest blocks
and regenerates ordinary category loot within18blocks of the assigned spawn.
The island geometry is restored only when there is no landing surface below
that spawn. Final-life spectator, lobby, loading and late-viewer paths do not
enter this recovery path. Existing spawn assignment and life accounting remain.

The old datapack chain is `lbc:respawn` -> `respawnnovanity1` -> `respawn_1` ->
`respawn_2`. Its recovery copies a21x31x21 region (XZ+/-10,Y-20..+10) from the
pristine `minecraft:white` map with a masked clone, then refills chest markers
within18 through `items/bows/kaname_bow/chest_loot_spawn`. The reference map is
constructed independently from original map placements. Paper's safe-respawn
helper only rerolled a spawn; it did not implement this restoration.

The native game captures nonair source blocks and full block-entity NBT for
each spawn at actual match start, after cage removal and before chest loot is
generated. This replaces the extra reference dimension. Original air is masked:
new construction in previously empty cells survives recovery. The snapshot is
cleared when arena entities are removed and recreated for each actual match.
Only the bounded spawn regions are stored, not a duplicate of the entire map.

Landing detection uses the native motion-blocking heightmap predicate and the
existing `minecraft:solid` tag with collision support. It searches below the
spawn, so an overhead roof does not masquerade as a landing surface.
Recovery is immediate before the respawn teleport; the datapack's shared delayed
schedule is unnecessary. Source cells use native clone ordering: supporting
solids, block entities, attachments, temporary barrier staging without drops,
then native final neighbor notifications. Shared snapshot/NBT restoration is
reused. No invented island template or new loot pool is introduced.

Nearby loot reuses `MapManager.fillChests` with a radius filter. This includes
recreation of missing chest blocks, original default/midle/center categories,
removal of previous contents, actual native generation and synthetic-count
normalization. Far chests and unmarked containers on surviving terrain are left
alone. The supported game mode remains1; unported mode2 is not enabled.

Build903de258 passed28recorded results in two actual mode1matches (60.96seconds).
Each match uses actual vanilla-command deaths and PERFORM_RESPAWN replacement
players for three cases: destroyed island with remaining lives; surviving
landing with a deliberately destroyed chest; zero lives. Checks cover original
nonair states, an unmarked barrel's exact NBT, masked original-air construction,
all three chest categories, normalized counts, an untouched25block distant
chest, unchanged unmarked barrel on surviving terrain, spectator exclusion and
cache cleanup after real match stop. The fixture prepares all candidate islands
and selects an actual assigned participant without changing the production shuffle.
Player rules, Time Stop, effect layers, firearms and FOV regressions also pass.

Two earlier native runs were rejected and retained. The first exposed a nested
test-command result checked before command-queue execution and missing cleanup
acknowledgement; the second fixture incorrectly assumed its first prepared spawn
would be randomly assigned. The final fixture stages damage outside command
execution and prepares every candidate, without replaying failed gameplay actions.
These fixture corrections are independent of the implemented recovery behavior.

Artifact: `build/verification/skywars2-respawn-island-903de258.jar`.
SHA256: `903de258e96301f70c624c7158ded5977994d0442a96fd839c1b7b0abaf0cdf1`.
Evidence, qualification, install and cleanup receipts: `build/verification/respawn-island/`.
All509sources/1448classes/3216processed resources match the final artifact
(fabric metadata matches semantically plus Loom's expected nestedSGUI entry).
All175protected author paths and31live grimoire overrides remain unchanged.
The owned localhost25576child shut down normally; fixture snapshots were restored
exactly. No RP changes or peak-load/physical-client visual claims.

903de258 is installed on the main server after normal shutdown, without restart;
the exact previously installedfe61bfdb is retained as rollback. Two older verified
JARs were retired. Cumulative cleanup:351files/12626780887bytes.
