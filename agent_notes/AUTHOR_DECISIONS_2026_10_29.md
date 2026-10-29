# Author decisions, 29 October 2026

These are requested targets, not a completion report. They supersede unanswered
questions in PENDING_QUESTIONS.md. New questions belong in chat. That file is
being edited by the author and must not be staged with implementation changes.

## Full Chaos dependencies

(user) Bean completion chance .008 per step; finish exactly once at the moved
position. Do not attribute a Paper regression to this behavior. Each hand casts
its own raw grimoire. Brush still erases any selected block.

(user) Theft uses the actual hit, including allies and mobs. Steal mainhand,
then offhand, then a random mob inventory item. Empty-handed players yield the
original trusi substitute. On total failure preserve the glove.

(user) Drone preserves fixed launch-point reference and becomes FPV: camera in
drone, bounded gradual look steering, Shift releases into straight flight,
controller body stays put, no item use during control, explosion restores camera.
Drone has20 HP, explodes when shot down, spawns further/higher. Singular lightning
item produces exactly one bolt.

## Food

(user) Time Stop pauses all food/status clocks. Shifting and confusion spike
choose exactly one different dimension; include Nexus. Random attribute food
leaves nonpositive bases unchanged and handles the singleton range correctly.
Stand/Hedgehog random statuses respect beacon AND stronger antimagic artifact.
Infinite foods replace the consumed hand. Oreshek special outcomes must not
disable future consumption; retain U+E202 notification.

## Apocalypse and Witch authorities

(user) CyberShish starts flight counter50. Whale clones follow main whale HP.
Lust dragon follows look, hides rider from other viewers, cannot hurt its rider.
Indolence requires the authority in both hands; reward armor uses its own slots.
Actors/control belong to the CURRENT authority holder and transfer with it.

(user) Vanity rewinds the whole match to acquisition: map, all players including
those killed since capture, locations/dimensions/motion, full NBT/inventories,
effects/layers, attributes, counters/tags, core counters and map parameters.
A player-NBT-only checkpoint does not satisfy this.

(user) Offline captured players receive their restored state on next join;
players joining after capture return to spectators. Preserve the original
GLOBAL cantleave cleanup, including preexisting restrictions.

## Suppression

(user) Antimagic prevents Greed heart creation and disables existing hearts
inside its area without deleting them. The holder's active immunity itself
is not suppressed. Hearts are unlimited. REMOVE Greed healing entirely;
other Greed active bonuses/cleanse remain unsuppressed. Lust regeneration stays
nonmagical. Envy copying is antimagic-suppressed. Wrath checks holder and target
antimagic. All source-second powers pause during Time Stop. Lust max-HP theft
uses real melee targets, including sweep hits, and protects targets in antimagic.

The full original25 Chaos transitive release gate remains. No claim that FPV,
Vanity, all authorities or complete reward closure are already implemented.
