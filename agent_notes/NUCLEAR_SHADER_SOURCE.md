# Nuclear shader source audit — 21 October 2026

Source read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/blockbench/nuclear_mushroom.txt`, sections «Контракт серверного мода», «Плавное появление и удаление». No RP, production Java, build, or server changes in this audit.

## Carrier contract

One `minecraft:item_display` per explosion, anchored at the epicenter near ground. Paper item with `minecraft:item_model=lbc:nuclear_mushroom`; glowing true; glow color `15180800 + slot` (decimal 15180800..15180815). Expanded by the user to sixteen nuclear slots 0..15; the updated guide was re-read on21 October. Slots must be unique among effects simultaneously visible to a player; a collision overwrites another effect. Black holes use a separate 16-slot pool.

`item_display=none`, `billboard=fixed`, width/height zero, view_range64 (server tracking range also required). Translation zero, both rotations identity. Scale `[R,1+ageSeconds,R]`: Y is age, never physical height. Keep epicenter fixed. Update age every 1–4 ticks with 1–4 tick interpolation, repeatedly forcing interpolation start zero. Newly tracking players must receive current age. R12 matches the large old `_2` cloud: cap approximately104 blocks diameter centered Y+60, top Y+92. Radius does not govern damage.

Full animation lasts70 seconds (1400 ticks); natural fade66..70. Spawn is instant at current stage. Immediate carrier removal starts a four-second client tail with frozen mushroom stage; reserve its slot another80 ticks after removal before reuse. Reload clears client state. Optional controlled fade keeps the carrier alive: Z=R*(2-visibility), interpolate Z fromR to2R for80 ticks while Y keeps age advancing, then remove; stop regular scale writes during that final interpolation. Right rotation must remain identity. A hidden carrier does not flash again when killed. Once all live carriers vanish the tail only has the previous camera rotation; keeping a hidden carrier during the fade preserves camera updates.

CPU implication: a tiny active display list and metadata updates replace repeated particle emission. Preserve shared lifecycle/time-stop policy and cleanup; do not scan all world entities each tick or claim measured speedups without profiling. Sixteen slots are a renderer protocol limit, not permission to change explosion gameplay when overloaded.

## Legacy callsites

Root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

| Source | Legacy visual |
| --- | --- |
| `workingitems/exopersoexplode.mcfunction:1` and `exopersoexplode1.mcfunction:1` | marker at Y-4, tag slowparticlesexplode, lifetime200 ticks |
| `drops/crocodildo/blowjob.mcfunction:1` | marker at epicenter, slowparticlesexplode_2, lifetime400 |
| `other/nuke/nuke1_at.mcfunction:1` | same large marker, lifetime400 |
| `other/nuke/nuke1_at_bruh.mcfunction:9` | same large marker, lifetime400 |
| `raycastselector_player.mcfunction:18–19` | dispatches those tags to workingitems/exopersoexplodeparticles and _2 |

The small legacy particle function has cap Y+35, sigma13/8/13 and column Y+15 sigma3/10/3. The large function has cap Y+60 sigma26/16/26 and column Y+23 sigma6/20/6. R6 is a plausible half-scale small effect but is an implementation choice, not an explicit guide requirement; old offsets and cap heights are not exactly a uniform half-scale. Do not infer shader age from legacy200/400 tick marker lifetime: the guide explicitly specifies70 seconds.

Future nuclear-bomb items and other callers of these legacy marker tags should use the same visual service at actual explosion epicenters. `other/nuke/nuke1.mcfunction:1` dispatches to nuke1_at; `nuke1_bruh.mcfunction:1` dispatches to nuke1_at_bruh. The latter also calls nuke1_at_bruh_2 at up to16 random non-spectator entities, but that subfunction has no mushroom-marker spawn: do not add one per sub-explosion without a separate behavior decision.
