# Nuke and Exoperso source audit

Read-only audit of the legacy source and the user's resource pack. This note distinguishes source behavior from the user's target requirements. Nothing here claims the new implementation has been runtime-tested.

## Source paths

Paths are relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- Exasperato upgraded wand mode 4: `lbc/function/workingitems/awaken_tuntija_wands/exasperato/click2_upgraded.mcfunction` selects the mode; `.../click2_upgraded_mode_nuke.mcfunction` searches forward and spawns the falling charge; `.../click2_upgraded_mode_nuke_spawn.mcfunction` creates the `falling_nuke` marker; `.../click2_upgraded_mode_nuke_rocket.mcfunction` advances that marker downward; `lbc/function/raycastselector_other.mcfunction` routes the marker to the rocket tick.
- Impact and 32-point pattern: `lbc/function/workingitems/exopersoexplode.mcfunction` (mode 4) and `exopersoexplode1.mcfunction` (parallel hedgehog-named variant). `exopersoexplode_scheduled.mcfunction` handles delayed advancement bookkeeping. The particle tick is `lbc/function/workingitems/exopersoexplodeparticles.mcfunction`.
- A separate nuclear mushroom implementation is `lbc/function/other/nuke/nuke1_at.mcfunction`, with its own `_2` particle tick in `lbc/function/workingitems/exopersoexplodeparticles_2.mcfunction`. This is separate from the mode-4 32-point impact; `big_nuke_2` is a distinct future item/effect, not another name for this mode-4 pattern.
- Particle dispatch: `lbc/function/tick_not_timestopped.mcfunction` calls `raycastselector`; `raycastselector.mcfunction` routes the raycaster; `raycastselector_player.mcfunction` dispatches `slowparticlesexplode` to `exopersoexplodeparticles` and `slowparticlesexplode_2` to `exopersoexplodeparticles_2`.
- Namespace check: actual `lbcr` functions are files under `lbcr/function/`, for example `raycast_acid.mcfunction`, `raycast_molnia.mcfunction`, and `raycast_tnt.mcfunction`. Mode 4 uses the `lbc` function files above; matching content elsewhere does not change their namespace.

## Mode 4 search and falling charge

The source has two distinct phases. `click2_upgraded_mode_nuke.mcfunction` does the forward search: it checks for non-spectator mobs within 15 blocks and for a non-air block, then recursively advances by 0.7 blocks while its distance score is at most 400. This is the old capped search, roughly 280 blocks of travel.

After that search finds a target, `click2_upgraded_mode_nuke_spawn.mcfunction` creates a falling marker 500 blocks above it. `click2_upgraded_mode_nuke_rocket.mcfunction` is the separate falling-charge tick: it advances downward by 0.25 blocks and recursively calls itself four times per game tick, for 1 block per tick. It checks blocks and tests the current position for impact. On impact it calls `lbc:workingitems/exopersoexplode`; that function's `kill @s[type=!minecraft:player]` removes the calling falling marker after the single impact. The 0.7 step and 400-step cap belong to the earlier search, not the falling charge.

The source's 15-block mob query and 400-step search cap are legacy behavior. The user's target requirement is to search for mobs and blocks without an artificial search-distance limit.

## 32-point impact: exact count and geometry

`exopersoexplode.mcfunction` summons exactly 32 creepers, each with `ExplosionRadius:20`, `Fuse:0`, and `ignited:1b`. The parallel `exopersoexplode1.mcfunction` has the same 32-point pattern with a different custom name. Relative offsets, in source order:

```text
(0,0,0), (5,0,0), (-5,0,0), (0,0,5), (0,0,-5),
(5,0,5), (-5,0,-5), (5,0,-5), (-5,0,5),
(0,5,0), (0,10,0), (0,15,0), (0,20,0),
(0,-5,0), (0,-10,0), (0,-15,0),
(7,5,0), (-7,5,0), (0,5,7), (0,5,-7),
(7,5,7), (-7,5,-7), (7,5,-7), (-7,5,7),
(7,-5,0), (-7,-5,0), (0,-5,7), (0,-5,-7),
(7,-5,7), (-7,-5,-7), (7,-5,-7), (-7,-5,7)
```

This geometry is 9 points at y=0 (center, four axis offsets at 5, and four diagonal offsets at 5); 7 points on the central vertical column (y=+5,+10,+15,+20 and y=-5,-10,-15); 8 points on a radius-7 ring at y=+5; and 8 matching points at y=-5. It is not a uniform cube.

## Impact sounds, emitter, and particle preset

`exopersoexplode.mcfunction` creates the particle marker at impact y-4, with tags `slowraycast`, `slowparticlesexplode`, `player`, `magic` and lifetime 200. It then emits five global sounds at the impact: dragon fireball explode, generic explode, fire charge use, firework large blast, and firework large blast far (volume 100, pitch 0). It emits `minecraft:explosion_emitter` at impact y+10 with spread `(15,10,15)`, speed 0, count 500, force. Finally, it removes the non-player caller, which is the falling charge in mode 4.

`exopersoexplodeparticles.mcfunction` is exactly three particle commands, every marker tick: fire block-marker at marker y-3, count 15, spread `(20,5,15)`; fire at marker y+15, count 20, spread `(3,10,3)`; lava at marker y+35, count 20, spread `(13,8,13)`. Each uses speed 0 and `force`, for 55 particles per tick. Since the marker is at impact y-4, those centers are impact y-7, y+11, and y+31. The separate `_2` tick is a different, larger multi-layer preset attached to `nuke1_at`; it should not be folded into this three-command mode-4 trail.

## Separate `nuke1_at` mushroom behavior

`other/nuke/nuke1_at.mcfunction` creates a `_2` particle marker at impact, places the `lbc:air` template six times at forward distance 25 (yaw 0/60/120/180/240/300, chance 0.5) and six times at distance 60 (same yaws, chance 0.25), and applies Wither amplifier 30 for 20 ticks to non-spectators within 45 blocks. It summons 24 creepers with radius 25 and fuse 1: 12 at horizontal offsets 39 and 12 at offsets 18, with rings at y=0,+40,-40. It stops the nuke sound and removes non-creeper/non-player entities within 14 blocks, except the `_2` particle marker and strict map objects. This is not the mode-4 32-point, radius-20 impact.

## User target requirements

Preserve the user's requested behavior:

> creepers -> TNT; fuse 0 direct native explosions; two finite front charges 50/100 + smart rear; mode 2 trail unchanged; Nuke search no artificial limit.

For the mode-4 impact, keep 32 points, radius 20, the same impact location and count, one impact, and remove the falling charge afterward. Keep the one-block-per-tick descent from 500 above. Search both mobs and blocks without an artificial maximum distance. The zero-fuse requirement is for direct native explosions. Preserve the existing mode-2 trail.

## Resource-pack shader feasibility

The read-only resource pack has `assets/lbc/shaders/post/zone_border.fsh` and `.vsh`, `zone_damage.fsh`, shader includes, and custom core entity/item/particle/text shaders. Minecraft 26.3 snapshot 3 officially adds resource-pack post effects that a server can request by sending effect IDs; the clientbound packet carries a list of IDs, not a dynamic world origin or age uniform: [Minecraft 26.3 Snapshot 3](https://www.minecraft.net/en-us/article/minecraft-26-3-snapshot-3). This makes a custom post effect a possible future visual route on supporting clients, but an impact-centered animated effect would also need world-anchor and timing metadata delivered through a separate client-aware channel and consumed by client code/shader integration. The existing zone-border shader is a local technique reference; it does not establish a ready-made nuclear effect or prove server activation at runtime. Do not change the resource pack now.
