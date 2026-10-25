# Sheep ordinary and Academy native port

Implementation prepared on 25 October 2026. Build and runtime qualification are
owned by the integrating agent; this note does not claim those checks passed.
Source audit: [SHEEP_HEDGEHOG_SOURCE.md](SHEEP_HEDGEHOG_SOURCE.md).
Presentation: [SHEEP_RESOURCES.md](SHEEP_RESOURCES.md).

`LbcSheep` registers `skywars2:sheep_wand` and
`skywars2:academy_sheep_wand`, preserving `lbc:sheepwand`, A/X tier and source
custom-data flags. Existing prepared leaves retain the exact authored text.
Configured `sheep_wand_mana_use` defaults to500 only when missing; actual casts
use shared wand admission/spending, cast hooks, roles, Time Stop and antimagic.
There is no Academy mode selector. Native first impact terminates the ray at
one target/wall, as required by the shared port contract. Source sample spacing
is0.75 and the Distance limit is150: terminal source reach is112.5 blocks,
including the initial sample at zero. Ordinary dust is magenta2 particles,
Academy is muted purple1 particle, scale2. Resident chunks only are queried.
Dropped exact succubus potions are explicitly eligible native ray targets.

Polymorph permanently replaces eligible nonplayer entities with a fresh native
8HP sheep named `jeb_`, hidden name, empty death loot and source knockback
resistance20 subject to the shared attribute limits. Players never transform.
Originals are discarded at their actual position without loot/death side effects.
No original snapshot, restoration duration, random color or hotbar removal is
introduced. Friendly mobs remain direct targets, while allied players and
technical roles remain protected. Health at least1000 retains the source
polymorph advancement. Sheep retain caster ownership for inherited consequences
through the shared owner resolver; later actual damage keeps its normal priority.

Academy honors existing `male`/`female` tags. Otherwise members of the imported
`minecraft:mobs` tag get a50% male/female choice, including eligible players.
Male entities create a hole unconditionally; final replacement still excludes
players. Ordinary entities outside that tag can transform without gender/hole.
Male holes do not complete the caster's potion trial.

Potion checks require all three exact integer custom-data flags:
`transformable:1,potion:1,succubus:1`. Source detection runs within2 of each ray
sample and the terminal impact. Dropped detection creates a hole and clears all
dropped items within5, matching the authored branch and the hole's own ongoing
item clearing. Inventory detection excludes the caster as its original
`raycaster` selector does, then clears only matching potion stacks in every
player inventory within5. Both resources can independently create holes.
The ordinary trial immediately readies the actual used stack, including offhand,
and tags its caster `ohno`. No potion-effect inference is involved.

The bespoke hole uses the existing shared shader with its accretion disc enabled.
The shader's14-times lens scaling matches destruction radius `sqrt(552)`
(`23.494680...`); it consumes the full radial region, including the former
obsidian core, as the user requested. The old structure's probabilistic
obsidian/air placement is replaced by no-drop native radial clearing. Container
contents are cleared before block replacement. `BlockAreaWork` retains work
across slices, filters empty sections, operates on resident chunks and rotates
holes fairly within a shared8192-inspection/4ms tick allowance. A completed
region is periodically refreshed; moving bounds retain pending intersection
work and enqueue their newly entered region. Terrain influence retains its
original captured WILD owner for support/void/burning consequences.

Source damage remains10 `outside_border` within5, guarded by `void_resistance`.
Native projectiles and items within5 are discarded. Role and Greed guards apply.
The hole is intentionally WILD, including caster/allies, as the source selectors
are. Pull uses velocity rather than teleport: acceleration0.24 inside destruction
radius, fading0.012 beyond it out to48. Weak pull respects any client movement,
jump, crouch or sprint input using the same native input rule as Duplicating;
strong pull still moves players and can lift them. Motion packets synchronize
player velocity. Each displacement keeps the hole's captured WILD author.

The outside-border DamageSource carries its captured WILD cause before entering
the native player damage method. A dynamically scoped cause alone is too late
for ServerPlayer's earlier friendship admission; the shared captured-source
mechanism preserves both friendly fire and owner attribution without adding
another damage type or resetting hurt timers.

Each hole moves0.05 per tick toward its `ohno` caster, otherwise toward the
furthest loaded eligible member of the source mobs tag, with no added chase
range cap. Disc tilt smoothly leans at most18 degrees toward motion, matching
the shared Void Arcanum level5 presentation. The source sheep ambient sound
retains selector radius80 and minVolume0.5. Source normal tick dispatch is
`tick` -> `tick_not_timestopped:3` -> `raycastselector:9` ->
`raycastselector_rare:24`; `rare` is not a slower clock.

There is no invented lifetime or held-wand requirement. Sheep and holes retire
on owner loss, actual unload, match stop/epoch change or local antimagic.
Time Stop freezes hole updates. Bodies cannot be serialized, and tagged orphan
loads are rejected. Native snapshots expose sheep/hole identities, owner,
position, velocity, radius, disc and pending terrain count for meaningful checks.
No author resource-pack or template changes are required by this implementation.

## Integrated qualification

Build `65a39f13` passed 26 recorded suite results
in two actual mode1 matches (79.34 seconds), including actual
Sheep input/potion trial/promotion, permanent replacement, native shader-hole
physics/terrain, player input and actual match-stop cleanup. Danmaku, ordinary
Acid Academy, player rules, Time Stop, KFC counters and acquisition also passed.
Exact 348 sources / 965 classes /
2818 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-sheep-65a39f13.jar`; evidence: `build/verification/sheep-final/`.
Main remains04396ca4; READY ONLY is the user's existing choice. No author RP
edits or translation fragment are needed. Native fixtures do not qualify real
client pixels, disk restart/unload/disconnect or peak-load performance.
