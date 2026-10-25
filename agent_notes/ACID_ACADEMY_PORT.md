# Ordinary Acid Academy port (25 October 2026)

Adds the missing Academy path for the ordinary stacid wand, reusing its native
acid implementation. This is distinct from the already ported awakened
Acidacid and caustic acid staffs.

The ordinary cast retains its configured250-mana cost, random yaw/pitch spread,
normal shot and cast hooks. A paid shot while the caster is burning immediately
readies the actual used upgradable stack. The other hand and unrelated pending
trials are not used as selectors. Normal Academy assigns/promotes this trial;
token-built single-use Academy consumes its interaction on promotion normally.

Promotion creates `skywars2:academy_acid_wand`, original model `lbc:st/acid`,
A-tier background, all seven authored lore rows and the existing eighth
`upgraded_item` row. Test acquisition:
`/loot give @s loot lbc:ported/academy_acid_wand`.

Shift+use freely switches Default/Ignited per hand, including at zero mana,
without paid cast hooks or spawning a charge. Actual casts retain configured250
spend per hand. Both modes launch exactly one existing big native acid charge
along the caster's aim, without the ordinary random spread; mode2 starts with
`on_fire`. Native gravity, acid reactivity, lifetime300, entity passage, shields,
Greed reflection and ownership stay shared. Input respects normal participant,
Time Stop and antimagic guards. Friendly players stay protected while friendly
mobs can receive the impact under existing projectile rules.

Source and presentation audit: [ACID_ACADEMY_RESOURCES](ACID_ACADEMY_RESOURCES.md).
Native checks: [ACID_ACADEMY_CHECKS](ACID_ACADEMY_CHECKS.md).
Remaining Academy scope: [REMAINING_ACADEMY_AUDIT_2026_10_25](REMAINING_ACADEMY_AUDIT_2026_10_25.md).

READY ONLY by existing user choice. No main server replacement/restart or author
RP changes. Existing RP contains every required key and model; no new template.
Build/native server qualification is recorded below once complete.

Burning acid trailing particles use normalized native flight rather than
Entity.getLookAngle with Projectile angles. A stuck charge uses its last
native orientation. Movement and physics are unchanged by this visual fix.

## Integrated qualification

Build `1229b044` passed 28 recorded suite results
in two actual mode1 matches (124.28 seconds), including ordinary/awakened acid, Gaycaster, acid ants and actual retained-projectile match-stop cleanup.
Exact 344 Java sources / 942 classes /
2814 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-acid-academy-1229b044.jar`. Evidence: `build/verification/acid-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
