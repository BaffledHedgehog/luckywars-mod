# Nuclear mushroom: upward dissolve and twenty-second tail

The author updated the shader: first clear the base and stem upward, then slowly
dissolve the cap, while retaining the explosion's world position. The guide is
read-only at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/blockbench/nuclear_mushroom.txt`,
around line 204. This supersedes the old four-second tail described in
[NUCLEAR_SHADER_PORT.md](NUCLEAR_SHADER_PORT.md).

## Contract

Only the fade carrier uses `[R, 1 + ageSeconds, R * (2 + fadeSeconds)]`.
The main carrier still uses `[R, 1 + ageSeconds, R]`. `effect_state.fsh` decodes
Z/R >= 2.5 as the fade-only duration, bounded by the shader to 0.5..120 seconds;
its default remains twelve seconds. This change explicitly requests twenty.
`nuclear_data.glsl` reserves 35% for the upward base/stem clearing and the rest
for the cap: seven and thirteen seconds for this duration.

The common `NuclearVisuals` service retains the fade for 420 normal ticks after
main removal: 400 ticks of animation plus twenty ticks of delivery margin.
Position, radius and removal age stay fixed. An age-eight, radius-twelve fade
therefore gets `[12,9,264]`, exactly as in the guide. The existing main lifetime
is six active seconds; its natural fade gets `[12,7,264]`.

Natural handoff stages both carriers for one active tick. Early cancellation,
including cancellation during the staged natural handoff, keeps the same fade
carrier and adjusts only its frozen age; the custom duration remains intact.
Slots remain reserved until destruction. Stand pauses the growing main, while
an already-running fade uses the existing normal shader clock; global tick freeze
pauses the retention clock too. Existing dimension/disconnect cleanup and visual
tracking/resynchronization remain unchanged.

All production users already enter this one service: ordinary/academic Exoperso,
Exasperato nuclear payloads (including the academic payload), and the rare
silverfish explosion. There is no independent nuclear visual producer to patch.
No RP files or author templates were changed; the current author RP supplies
the new appearance.

## Verification

The scoped native runner is `tools/smoke_nuclear_fade_20s.py`. It covers shared
Hedgehog, nuclear lifecycle, visual reload, Exoperso, native blast, academic
Exasperato and Time Stop in two actual matches. Native metadata checks assert
the fade-only Z encoding, preserved main scale/position/age, exact 420-tick
quarantine, no cleanup at 90/240/400/419 ticks, natural/early/staged cancellation,
repeat-stop idempotence, Stand and global freeze, and dimension cleanup.

The first run passed the changed visual checks in both matches but rejected the
batch because Hedgehog requires an empty helper region and ran immediately after
the Academy fixture's lightning. Its initial log/results/proof remain under
`build/verification/nuclear-fade-20s/initial-8b593d2c-*`. Run Hedgehog first rather
than weakening its admission or changing gameplay; rerun the same immutable JAR.

Artifact/qualification/install evidence lives in
`build/verification/nuclear-fade-20s/`. These checks inspect native metadata and
carrier lifetimes; they do not claim physical GPU rendering, camera/pixel proof,
or natural TPS measurements.

Final immutable build `8b593d2c` passed 22 recorded results in two actual matches
(36.35 seconds). Normal owned-child shutdown and exact fixture restoration passed.
All 552 sources, 1570 classes and 3234 resources match the tested artifact;
198 author paths and 29 live override JSONs remain byte-exact.
