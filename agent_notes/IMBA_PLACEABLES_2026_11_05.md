# Native portals, crystal and mystical tree — 5 November 2026

The author confirmed source behavior: none of the four inputs is suppressed.
Mystical-tree progression is also unjammed. The ordinary native explosions of
its terminal creeper and the End Crystal retain common TECH protection.

## Source and implementation

Legacy sources are read-only under
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/raycastselector_instant` routes the three marker entries to
`workingitems/imba/end_portal`, `nether_portal`, `mystical_tree_tick`.
Portal commands place the existing exact structures at offsets(-2,0,-2) and
(-1.5,-1,0), then remove their roots. Native placement reuses the existing
`LbcImbaTerrainUtilities` finite queue and clone/checkpoint owner; old Bastion,
Dirter and Funny Debris behavior is retained. End has21 explicit cells
(12 eyed frames and9 portals); Nether has20(14 obsidian and6 axis-X portals).
Undefined End-template corners remain untouched. New portals do not inherit
Bastion's unrelated stone sound or particle burst.

Tree source: `mystical_tree_tick`, `mystical_tree_move`,
`mystical_tree_move_macros`, `mystical_tree_end`. Each logical step independently
stops with1% probability or outside source height12..308. Otherwise it selects
one of six cardinal directions and distance1..10, strictly fills oak wood
including both endpoints, then moves. The crown is a strict5-cube of persistent
azalea leaves; the center becomes a chest with the original lazy
`lbc:infinity_upgraded_luck_noadv` table. Its26 draws from `lbc:luckyblock`, each
with count64, are preserved using current modifier spelling. The terminal
creeper retains its y-1.2 placement, native1000-tick fuse/radius20, no gravity,
invulnerability, persistence and1000 ticks of particle-free invisibility.

`LbcMysticalTree` stores typed pose/RNG/cursors and uses the shared fair terrain
queue and owned writes. Chunk preparation is asynchronous; callbacks do not
force-load terrain. Whole-coordinator replay restores the same native root and
accepted segment, then continues the same logical RNG decisions. Finite work
may use different publication quanta with different server load; verification
compares logical continuation and exact completed terrain. Unloaded native
roots remain pending; no restart-persistence or extreme-height parity claim.

Free End Crystal reuses the common `LbcSpecialEggs.Egg` native spawn path,
including ordinary stone placement without vanilla crystal's substrate rule.
The existing pre-publication adoption hook binds its causal creator. Canonical
item functions, counts, names, lore, styles, models and EntityData are retained;
saved old carriers use those same input owners. Existing source RP assets are
reused; no author RP/template changes.

## Exact qualification

Test `b65f2053ac6e497eb4f5af93dc9fe91d05f23722d0c4f40fed3ec630e405900c`, production `7ed5ca0c21c02b3e7c96ceaf4c254c3e337a19b0351ec076e59fab8e8395c436`.
Eight isolated native matches passed: modes1/3 each run `imba-placeables` and
`imba-placeables-rewind`; mode1 additionally runs `imba-utilities`,
`imba-followup-rewind`, `special-eggs`, `clone-skills`. Checks include both real
jammer controls, source prototype/Polymer roundtrip, used-hand debit, exact
portal states/offsets, native free crystal, source walk/crown/chest/creeper,
native TimeStop, actual x2*x4 tree composition and same-body pending replay.
All fixture JVMs stop normally and file/ticket/auxiliary snapshots are restored.

Only10 owned Java families and11 resource inputs overlay the qualified TNT
baseline. All archive entries and retained-class linkage are checked;
10130 other source inputs and275 author paths are guarded. Production excludes
MapAuthoring/MapDeletion. Main and both build/libs JARs are updated through the
safe installer without stopping/starting main. Full IMBA/TMT-last/modes2,4/
skill2/broad public rewind gates are unchanged. No physical-client, portal-trip,
peak-TPS or disk-restart-persistence qualification is claimed.
Evidence: `build/verification/imba-placeables-2026-11-05/qualification.json`.
