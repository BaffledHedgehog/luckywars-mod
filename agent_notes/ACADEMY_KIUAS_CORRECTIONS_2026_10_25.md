# Token title and Kiuaskivi support (25 October 2026)

Legacy Academy geometry was decoded directly from read-only
`world/datapacksdisabled/LBC/data/lbc/structure/magic_academy.nbt` in the
original server. Native template title origin is(6,2,10), with identity display
transformation, fixed billboard and yaw-540. Blue concrete occupies
x4..7/y2..3/z10; the room side atz9 is air, whilez11 is a solid exterior wall.
The short single-use title alone now uses(6,3,9.97): one block higher, centred
vertically on that panel, and.03blocks toward the room to avoid a coplanar face.
Ordinary Academy retains its exact source title position and text/style.
`AcademySingleUseChecks` verifies both native origins and the token's clear air
position, retaining existing promotion/ownership/stop suites. Client pixels still
require a real-client check. Author RP/template language files were not edited.

The pure `LbcKiuas.kiuResult(BlockState)` classification remains unchanged.
A positional overload admits new FIRE only when the cell below is any nonair
block, or a block on one of the other five faces is flammable. It uses the
Fabric default fire registry, including vanilla entries, and rejects waterlogged
side support as native fire does. All reads require already resident chunks.
If ignition is rejected, the source first-stage TO_AIR conversion still applies
(water clears); other original materials remain unchanged. Existing fire is not
retrospectively removed. Native fire scheduling and original attribution remain.

The aura's small/large conversions, curse and shared `LbcBlaze` Kiuas conversion
all call the positional overload. A native neighbor change queues adjacent aura
cells too, so previously rejected empty space can ignite when support appears.
The same bounded workset/budget still applies. Sun and lava conversion are unchanged.

New integration suite: `kiuas-support` → `KiuasSupportChecks.run(game)`.
It tests every face with stone/wood, unsupported air/wood/water, nonair fluid
below, waterlogged side rejection, actual aura ignition/reactivity, curse and
shared Blaze conversion. It restores resident terrain, scheduled block ticks and
native player state. Existing suites remain `academy-single-use`,
`academy-single-use-stop-probe`, `academy-single-use-cleanup`, `kiuas`,
`kiuas-crafting`, `kiuas-cleanup`.

Status: source/checks prepared and frozen for parent build/runtime qualification.
No build, server, git, author RP or template action was performed by this subtask.
