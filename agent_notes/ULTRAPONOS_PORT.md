# Ultraponos native drop

Source and Paper distinctions: [NEXT_DROPS_AFTER_SKY_SOURCE.md](NEXT_DROPS_AFTER_SKY_SOURCE.md).
Public acquisition: `lbc:ported/ultraponos_drop`, native item
`skywars2:ultraponos_drop`. Original repeating-command-block presentation,
trap model/tier, translated name/lore and ultraponos/drop flags are retained.
Whole Lucky Block reward pools remain gated. RP/template unchanged.

The six-block `lbc:ponos` template is used directly at floor(token XYZ), with
native block states, explicit air and actual five-slot hopper contents. Native
template replacement semantics remain; no container-copy shortcut or invented
structure is used. Only resident footprint/neighbour chunks are admitted.
Placement registers WILD causal terrain effects through the existing service.

Four native silent/invulnerable/no-AI/no-gravity persistent creepers appear at
block-centered X/Z and blockY offsets -1.5,-2.5,+.5,+1.5. Their radii are6/7/8/9,
fuse290, invisible288, translated yellow nonitalic megadrist and source
govno_creeper tag. Native fuse, explosion/mobGriefing and potion-cloud/TNT
continuations reuse the existing creeper service, including author, unload,
epoch, Time Stop and quiet stop. No TNT protection setting is added to a mob
explosion. WILD admits thrower/allies while retaining observer/service safety.
Only this subtype gains WILD; existing ordinary creeper-drop rules are unchanged.

Skill9 choice (user,23 October2026): "8 взрывов, как в датапаке".
When LBC skills are enabled and the nearest eligible player has skill9,
four stronger creepers with radii12/14/16/18 precede the four ordinary ones.
This preserves the datapack's additional stronger series; Paper's replacement
of the ordinary four is not used. Pickup target and actual thrower do not
replace the source's nearest-player skill choice.

One compact emitter replaces the marker/scoreboard loop. User refinement
(23 October2026) replaces five-item bursts with one existing `lbc:govno` roll
on each active tick1..280. The item originates at block-centered X/Z andY+1.05,
just above the hopper rim, with native velocityY=.8 and independent uniform
X/Z in[-.2,.2]. The source marker centerY+1.5 still anchors the govnoq item-entity
count in spherical radius15: <=200 permits an emission. The authored200
condition is not an invented hard item cap; stack units are not counted.
The scan uses a local query on emission and a prebuilt custom-data
predicate. Missing source columns and dimension Time Stop pause the emitter;
no chunk loading/tickets or global item scan is introduced. Mechanism suppression
permanently cancels the emitter, preserving the source mecha-marker deletion;
already ignited creepers remain. Magic suppression does not cancel this machine.

Particles also follow the author's refinement: the same brown-wool block
particles originate at the hopper-rim point, directed upward. One batched native
ALTERNATIVE particle packet per nearby viewer emits60 per tick, preserving the
old300-per-five-ticks particle rate with zero spawn-area offsets and positive
Y input speed. The native client adds its normal terrain-particle jitter/gravity;
the DEFAULT Gaussian velocity mode would incorrectly send half downward.
This uses26.3's existing packet options, without a client mod or new particle type.

All four existing govno branches retain native components. The cookie branch
uses actual `skywars2:govno_eaten` food and its existing reward continuation;
the relics remain usable in native govnoq crafting. Source flags copied to
ordinary items remain recognized. Shared dispatcher owns fake/stale/stage checks,
one unit debit and same-tick replay protection. Native merge interception preserves
different token authors without an extra tick/world scan. Stop invalidates the
emitter and shared actors; the placed ordinary structure is not restored.

## Validation

Build91d85205 passed an offline build and two actual isolated mode1 matches:
12 recorded suite results in68.14s.242sources/665classes/2710resources exactly
match the tested artifacts. Synchronous fixtures cover source presentation,
public loot, native structure/inventory, normal/mega actors, cadence, conditional
entity count, no-load/fake/epoch/pause, suppression, WILD roles, actual native
delayed explosion/credit, edible/crafting rewards and actual match-stop cleanup.
No real-client sound/particle rendering, actual disk restart/unload/disconnect,
full natural trajectories or peak-capacity benchmark is claimed.

The final native tests validate all280 single-item emissions with exact spawn/
velocity bounds, and the actual particle packet's brown-wool type, point origin,
positive-Y ALTERNATIVE mode and batching. Native290-fuse explosion and causal
kill credit, WILD entity-write terrain context, actual food use/native crafting,
normal creeper/KFC/UI regressions and actual stop cleanup passed in both matches.
The original structure NBT is byte-identical to the source file.

Independent review found missing WILD propagation in shared explosion/entity
terrain-write contexts. Both unions now include this creep subtype; final tests
cover allied hazard permission/credit. Initial API-only compilation attempts
used pre26.3 brown wool/dye constants and were corrected to color collections.
An earlier12-result run passed before the author's item/particle refinements;
its evidence is retained separately and did not qualify the installed artifact.
Known fake-player Polymer packet-context warning recurred; no final suite failed.

Evidence: `build/verification/ultraponos-final/`; runner:
`tools/smoke_ultraponos.py`. Installed to main while stopped; rollback20210f2b
retained. The agent did not start main. The three user-owned Reroll/AcidAnt/
HelicopterEncounterChecks files remain untouched and excluded from the commit.
