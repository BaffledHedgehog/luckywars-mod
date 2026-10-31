# Nuclear fade with original total duration ? 31 October 2026

The current user requirement preserves the TOTAL mushroom plus fade lifetime of the original datapack. This supersedes the old fixed shader total and the intermediate six-second main plus twenty-second tail. Resource pack files were read only.

Actual source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

- `workingitems/exopersoexplode.mcfunction:1` and `workingitems/exopersoexplode1.mcfunction:1`: `slowparticlesexplode`, marker lifetime200 ticks.
- `other/nuke/nuke1_at.mcfunction:1`, `other/nuke/nuke1_at_bruh.mcfunction:9`, and `drops/crocodildo/blowjob.mcfunction:1`: `slowparticlesexplode_2`, marker lifetime400 ticks.
- `tick_not_timestopped.mcfunction:3` executes the slow marker selector every active tick. `raycastselector.mcfunction:1` increments Lifetime by one; lines12?13 remove the marker at its configured lifetime. These are source tick budgets, not shader animation ages.

The actual current guide was read at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/blockbench/nuclear_mushroom.txt`, including lines204 onward. Fade Z encodes `R * (2 + seconds)`, not geometric depth. Default12 seconds uses a bottom-to-top dissolve:35% stem,65% cap. Its example20-second tail requires400 ticks plus delivery slack; keeping the carrier alone does not slow the shader.

The shared visual now preserves each source family:

| Family | Main | Encoded fade | Total visible | Carrier after main removal |
| --- | --- | --- | --- | --- |
| Small Exoperso |120 ticks /6s |80 ticks /4s |200 ticks /10s |100 ticks |
| Large nuclear/Nuke/crocodildo |160 ticks /8s |240 ticks /12s |400 ticks /20s |260 ticks |

Main lifetime equals source total minus fade ticks. Fade scale is `[R, 1 + mainAge/20, R*(2 + fadeTicks/20)]`. At R12 natural small handoff is `[12,7,72]`, large `[12,9,168]`. The extra20 retention ticks are an invisible delivery margin and do not extend the shader's visible duration. Early cancellation freezes its actual age and retains that family's configured fade; repeated Stop does not extend its deadline. Per-client16 slots, independent main/fade holders, dimension cleanup, normal GameTime tail and Stand-paused main clocks remain the existing owners.

Existing five-argument `show` is the large400-tick family; the overload accepts only the two source totals200/400. The shared Exasperato/Exoperso burst must select200 for Exoperso and400 for academic nuclear. Other existing production callers use the large family. The resource pack, damage, terrain, main installation and protected199 snapshot were not changed by this subtask.

Native fixture remains `nuclear-visual` in the bounded nuclear methods of `GameChecks.java`; it checks metadata/model/glow/transform, same-slot handoff, frozen encoded scale, per-client nearest16, overlap, exact100/260 retention boundaries, both200/400 total-duration identities, repeated Stop, global freeze, Stand pause, dimension cleanup and antimagic cancellation. Source changes prepared here; build and actual native suite execution belong to the integrating agent. No GPU pixel, camera delivery or in-client frame-rate claim.
