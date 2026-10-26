# Ordinary Sauvojen Tuntija

Source: `items/wands/sauvojen_wand/{cast,cast_2,flying_wand_tick}.mcfunction`
and the nine `workingitems/sauvojen_staffs/` producers in the legacy LBC pack.
[Presentation audit](SAUVOJEN_PRESENTATION_SOURCE_2026_10_26.md) and
[Academy source audit](SAUVOJEN_ADVANCED_SOURCE_2026_10_26.md) retain exact references.

## Implemented scope

`lbc:ported/sauvojen_wand` creates the native `skywars2:sauvojen_wand` with
the original name, four lore rows, flags, tier A and `lbc:sauvojen_wand` model.
Existing author RP resources suffice; no template copies or author RP edits.
The shared paid input spends configured 2500 mana per successful hand and runs
the existing Mage/trinket callback once. Both hands work independently.
Existing native and legacy-flagged ordinary stacks dispatch through the same path.

Each cast uniformly selects one flying elemental wand at eye position plus
look direction times two. It retains the exact source row-major scale2.5 matrix,
witch trail, target range150 and movement0.1 away from its owner's eyes per tick.
The nearest eligible enemy determines aim; absent enemies preserve the previous
rotation, initially zero. Source `minecraft:airs` allows fluids and other
authored passable blocks. A solid center retires ordinary drones.
The first working lifetime is2; retirement happens at200 before firing.

| Element | Shot phase | Child lifetime | Source-specific payload |
| --- | --- | --- | --- |
| Ruby | age mod60 =1 |50|Shared1.8/tick drilling charge|
| Terra | mod20 =1 |200|Shared1.8/tick ordinary Terra|
| Electro | mod20 =1 |35/20/25/30|Four independently owned source arcs|
| Air | mod60 =1 |120|0.8/tick smoke charge; three native strength4 blasts|
| Acid | mod20 =1 |300|Unshielded no-gravity acid, constant speed1.6, native reactions|
| Teleport | mod80 =1 |60|1.2/tick persistent trail; Levitation41 for20ticks|
| Stone | mod60 =1 |80|1.6/tick; strict stone cube from floor(point-1.5) to floor(point+1.5)|
| Fire | mod5 =1 |native|Owned small fireball with source initial speed2.5|
| Blind | mod10 =1 |100|Shared1.8/tick blindness contact charge|

Flying factories take explicit origin/direction and local lifetime. They neither
move/rotate the caster nor invoke paid item use. Ordinary staff behavior remains
on its existing path. Air uses native explosions without world-spawned instant
TNT/creeper helpers. Stone suffocation and levitation inherit the source author.
Item-issued effects use the existing signed-effect grant caps.

The random-library outcome242 delegates to this same producer service. Its old
duplicate display loop and ambient entity-lifetime capture context are removed.
Consumed random input does not claim the combined staff advancement; actual staff
casts award matching `wand1` through `wand9` of the existing achievement.

Time Stop pauses producer motion, aim and age. Antimagic removes the producer.
Owner removal, dimension changes, stale epochs, carrier unload and actual match
stop retire captured state and viewers. Owned manual visuals use the existing
client-world-reset tracking repair. Already-paid producers continue after
their original item moves; the captured stack never writes back into inventory.

## Remaining combined Academy work

The combined Academy version and destructive multistage meditation remain gated.
Neither an Academy leaf nor a fake upgrade route is published. Legacy stacks
with `upgraded` or `upgradable` are rejected before payment until that work lands.
Broad enclosing source pools and live grimoire overrides remain untouched.

The user approved a sequential cycle of all ten upgraded ruby attacks (user).
That replaces its accidental odd-only legacy counter behavior when the combined
Academy implementation is enabled. This ordinary release does not claim it.
The inactive incomplete `blind_check` helper is not an active trial defect.

## Validation

Build `c9e36258` passed46 recorded results in two actual mode1 matches (59.25s).
Exact435 Java sources /1208 classes /3140 resources match the tested JAR.
Nine distinct emissions, actual Air/Stone/Teleport impacts, acid native movement
and hostile raised-shield passage, source presentation/advancement, paid hand
input, targeting, suppression, Time Stop, tracking and actual-stop cleanup pass.
Shared elemental/all-grimoire factories, signed math, player/projectile rules,
rifle accuracy/scopes/SLR and artifact regressions also pass. The read-only item
effect checker covers128 grants and three permitted exact status snapshots.
Native float sine-lookup tolerances are limited to converted electrical angles;
actual launch positions and velocity vectors retain strict numeric checks.

Evidence and immutable qualification:
`build/verification/sauvojen/qualification-c9e36258.json`.
Ready artifact: `build/verification/skywars2-sauvojen-c9e36258.jar`.
Installer11424 waits for normal main shutdown/world unlock (port24566, RCON off).
Its live receipt is `build/verification/sauvojen/main-install.json`; main disk
remains1a8a3825 until that receipt confirms installation. The main JVM was not
stopped or restarted. Preserve current1a8a3825 and rollbackb2c91dcf.
Obsolete875fed3a retired: cumulative323 JARs /11510486943 bytes removed.

All174 preexisting author edits/deletions and31 live grimoire override files
remain unchanged. Author RP was read only. Actual client pixels, physical
reconnect/full restart and peak TPS are not observed by these native fixtures.
