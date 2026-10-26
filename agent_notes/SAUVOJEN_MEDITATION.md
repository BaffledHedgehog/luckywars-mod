# Sauvojen combined Academy meditation

Implemented27 October2026 in `SauvojenMeditation.java`; the native entry point is
`SauvojenMeditationChecks.run(GameController)`. The complete parent batch is
qualified; [the port record](SAUVOJEN_ACADEMY.md) contains exact release evidence.

Sources are the read-only legacy `LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/`
tree, `lbcr/function/raycast_terrameditator`, `lbcr/function/placegrass`, the green/red
Gaycaster meditation particle generators and `lbc/predicate/looking_at_scare.json`.
The detailed source ledger is [SAUVOJEN_ADVANCED_SOURCE_2026_10_26.md](SAUVOJEN_ADVANCED_SOURCE_2026_10_26.md).

The service admits the exact live mainhand upgradable combined stack, maintains
620 ticks with the source's 70% fear stalls, and runs all nine source phases.
Readiness changes the participating stack through existing Academy.ready; it does
not award an upgraded replacement. Failure/holding guard/forget/epoch/map-stop
close only the affected trial's helpers. The source's global helper kills are not
retained. Scared persists after failure and resets during fire, as in source.
The unused incomplete blind_check function is not called or treated as an active bug.

Ruby destroys the exact349-cell stepped footprint through25 levels (8725 cells).
Stone uses the source47×19×47 structure through `MapManager.placeMapSlice`, retaining
6964 stone,26115 explicit air and8892 absent cells. The resource must be prepared
by the parent integration as `lbc:stone_cylinder`; a missing packaged template is an explicit integration error, while a nonresident
footprint pauses the phase instead of replacing it with a smaller arena.
Fire clears20181 cells and writes42510 unique lava cells (31×30×31 plus121×121 plane,
with961 overlap). Stone retains all19 native row slices. Ruby/fire preserve every
authored cell and resume1024-write native
quanta under the shared current-tick52ms deadline (54ms target less2ms reserve).
The countdown remains at its phase until writes finish. Air is strict; lava retains
native LiquidBlock.onPlace scheduling and flow. Full terrain writes carry native indirect ownership and support
removal attribution. Persistent world damage intentionally survives trial failure.

Terra produces three radius10 displays per tick and delayed20-tick native-grass rays;
electro draws a ray to every earlier-active web (6 edges for4 nodes,120 for16),
with each damaging ray stopping at its first eligible contact; air emits37 full-height red
columns, four native unprotected radius4 explosions each, and radius7 magic damage;
acid preserves moving radius10..15 damaging rings, falling markers, poison and powder
corruption; fear uses real4×4 interaction entities, ten face models, native sight/block
geometry, floating100-tick helpers and source-style stage translations.

Global/owner Time Stop pauses work. Owner antimagic pauses countdown; local antimagic
removes helpers. Native valid-target/Greed and resident-chunk checks protect shared
roles and avoid terrain acquisition. Direct hazards use captured WILD ownership;
poison, levitation/falls and block hazards retain the same captured cause.
`managed(Entity)` and `cause(Entity)` identify ephemeral native TNT sources for parent
shared damage/indirect/chain integration. `damaged` closes the active victim's trial
for such a native explosion. The parent wires start/tick/stop/forget/loaded/unloaded and damage callbacks.
The central Academy route receives only the owned Sauvojen enum row; prior author edits remain intact.

Checks use actual world blocks, actual actors, native DamageSources/effects and manual
service ticks (beginning a fresh shared time budget for each simulated turn), and log
full ruby/stone/fire CPU duration and worst native work turn. They restore touched terrain and the shared player fixture. They cover
full destructive footprints and explicit structure air, held-stack identity, source
readiness, local-air placement, helpers/cleanup, rejected-damage failure and acid
poison. The full batch passed94 recorded results in two actual mode1 matches;
qualification is linked above. The source terrain completed in1 Ruby turn,1-2 Stone turns and2-3 Fire turns
in the two controlled fixtures. Real native actors,
allied TNT damage/credit, graph geometry and owner cleanup are checked. Concurrent
physical clients, peak TPS, source spreadplayers presentation and complete
stochastic survival remain unobserved.
