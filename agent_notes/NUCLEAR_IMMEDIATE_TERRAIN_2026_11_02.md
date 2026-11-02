# Shared nuclear terrain and immediate status damage — 2 November 2026

Implementation ownership: LbcNukeEvent, nuclear-only LbcExasperato/LbcLegacyEffects branches,
and the authorized LbcHedgehog sneeze producer/queue/checkpoint branch,
NuclearTerrain, NuclearBlastChecks, and the narrow native-explosion discovery/ownership hooks
in HazardPropagationMixins. LbcAcademy, actual resource packs, cosmic chain-world explosions,
and ordinary Exasperato/85-TNT/electro/encounter payloads were not edited.

## Cause and behavior

The old pointBurst queued a visual, twelve overlapping 48-cube air template placements with
integrity processors, wither/cleanup operations, and only then a deferred burst. effect_nuking
consumed every tick while the owner remained alive, publishing independent large jobs long
before the first lethal damage reached them. Exasperato/Exoperso likewise queued their first
native damage wave behind other work.

Point nuclear activation now publishes its accepted job, emits the existing instant visual,
and executes a native power25 wave at the detonation center in the activation call. This is
a deliberate relocation of the first inner18/+X wave, as requested during integration, not
a new damage type or additional wave. The remaining23 native source offsets retain their
power25; bruh retains power16 and its first source inner18/+X wave. The repeating body status
uses pointStatusBurst: an identity lease joins that body's existing pending job. Its scoreboard
still consumes one duration unit per active tick, and no global limit applies to independent
bombs, ordinary pointBurst calls, or EOE. Publication and score debit precede lethal native
callbacks; no lucky-effect marker is re-added after death cleanup. Leases retire with their
own completed/cancelled job. Accepted nuclear consequences outlive author death/disconnect.

Exasperato/Exoperso and the rare silverfish sneeze retain the source32 power20 offsets. The center wave executes synchronously,
then the common terrain operation, then31 remaining native waves under the existing CPU queue.
Queued count is therefore31 immediately after activation in both services. The fish still uses
a null native explosion actor so its own native exposure is included, with its original
captured damage source identifying the narrow terrain-suppression scope. Its blast checkpoint
also copies and restores the shared terrain cursor. The existing shader show calls,
parameters, durations, and fade service remain unchanged.

## Terrain and numeric inference

Read-only source evidence: effects/effect_nuking.mcfunction; other/nuke/nuke1_at.mcfunction;
other/nuke/nuke1_at_bruh.mcfunction and nuke1_at_bruh_2.mcfunction;
workingitems/exopersoexplode.mcfunction in the original disabled LBC datapack. Point source
uses 48-cubes at horizontal radii25/60, i.e. outer horizontal reach84, plus native creeper
waves at cardinal radii39/18 with Y0/+40/-40, power25 (bruh16). Wand source has32 power20
center/5/7 offsets and vertical offsets -15..20.

The requested replacement has one shared ellipsoid profile per payload: point horizontal84,
vertical65; wand horizontal40, vertical45 with centerY+2.5. These are approximate envelopes,
not claims of exact cube-union equivalence. The direction-coherent deterministic boundary
varies .88..1.12 of the profile, with a dense geometric core through .78. Surviving blocks
are selected deterministically using native block/fluid explosion resistance; unbreakable
native hardness stays intact. The probability curve (scale96 with radial reduction) is an
implementation inference because the legacy integrity cubes ignored resistance entirely;
there is no corresponding legacy numeric resistance curve to preserve.

NuclearTerrain.Shape scans each resident nonempty section once, at most4096 candidate cells
per invocation, skipping empty sections and unavailable chunks. Point legacy-air policy uses strict native LbcDropBlocks writes. Wand/sneeze instead retain one
native ServerExplosion application object per logical job and invoke only its private
interactWithBlocks(admittedPositions) through the existing Blast mixin invoker. Native
onExplosionHit callbacks preserve TNT priming, block/container loot and block reactivity once.
No explode(), block discovery, damage, game-event or packet/sound method runs for terrain slices.
Original TNT/MOB drop-decay and mobGriefing rules apply to that object. Per-slice source bindings
retain Academy/Exoperso chain trials and sneeze child lineage. The restored logical job lazily
recreates one application object from rebound sources, rather than retaining stale body links.
All writes retain ordinary engine lighting/block-change hooks and source trapChanges scope
(one per slice, with shared support candidates), without per-voxel NBT. It does not load chunks or allocate cube snapshots.
The one logical terrain operation is sliced by existing queue budgets. Dense worst-case maps
still require work proportional to their changed cells; no map-size or queue cap was added.

Native damage-wave block discovery returns an empty list only inside an exact-emitter scope.
This prevents32 repeated terrain traversals without changing unrelated nested chain reactions.
Damage, difficulty scaling, exposure, armor, resistance, absorption, cooldown delta comparison,
knockback, and native death protection/totems still execute through native ServerExplosion.
Later waves observe actual blocks after the terrain operation; surviving walls remain the
primary protection. No unconditional through-wall damage or cooldown/bypass override exists.
The point source now passes its actual managed marker as native direct source. Its already
captured NuclearDamage attribution/death message is preserved, and role/Greed filtering runs
before native damage. Ordinary explosion ownership behavior is unchanged.

## Review and qualification boundary

NuclearBlastChecks.run/tick supplies asynchronous native-body visibility warmup; tests the
actual synchronous point status call and duplicate-body coalescing, two real hand totems plus
ordinary native increasing-wave cooldown deltas, actual wall exposure before/after removal,
and the shared terrain dense-core/noise/hardness/read-budget properties. A native application
probe also verifies one reused application object, no terrain-slice damage, one native chained
TNT retaining the actual author/family, and exactly11 chest diamonds published once. The fixture uses a
small owned terrain frame and cancels the full point terrain before it can mutate the map.
It does not prove a physical client view or peak server TPS. It proves native lethal damage
in the producer call using a published mob, not an actual participant death/respawn; root
integration may add that broader lifecycle probe in its isolated snapshot fixture.

No build, native run, artifact installation, commit, server stop/restart, or actual-RP write
was performed by this worker. Scoped git diff --check passed. Root owns GameChecks dispatch,
tick wiring, editing-fixture packaging exclusions, compilation, and native qualification.
Exasperato blast checkpoints copy the nuclear terrain cursor and accepted causal record;
restore resumes its remaining cells and native waves. NukeEvent retains its existing requirement
to finish pending event jobs before a checkpoint.
