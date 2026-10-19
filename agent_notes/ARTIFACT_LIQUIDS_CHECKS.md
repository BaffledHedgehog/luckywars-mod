# Mystical artifact type 1:3 checks

Height-fix verification on 15 October 2026: build and focused smoke passed
in **54.97 seconds, one match, four groups**, SHA-256
`f877ba3d66533b92c9d3a032a4e981e4b40713b33fcef39d9752cb019252b30e`.
The fractional-height checks below use native falling entities positioned
directly at the relevant Y coordinates, with gravity disabled in the fixture.
They verify hardening placement, not a separate simulation of native gravity.
The prepared JAR includes prior stages; the main installed JAR is unchanged.
Current evidence and artifact path: [SERVER_TESTS.md](../SERVER_TESTS.md).

This record describes the focused operator checks added for the liquid and
falling-block artifact port. The checks are authored in
`src/main/java/me/skywars2/game/GameChecks.java` and selected through
`tools/smoke_lbc_loadouts.py --artifact-liquids-only`. They require an isolated
verification server and are not run automatically.

## Coverage

The menu check grants `lbc:ported/mystical_artifact_liquids`, verifies one
artifact with custom data `mystical:1,type:3`, source lore and tooltip style,
and checks the Polymer representation.

The active check registers a native type-3 item and exercises existing lava,
water and powder snow against the port's discrete sphere centered on
`player.blockPosition()`: axial distance 16 is inside, distance 17 is outside,
the old cube corner at `(12, 12, 12)` is outside, and `(9, 9, 9)` is inside.
It queues a source fluid that is replaced before the pulse, checks an
unsupported cauldron state, and moves the holder one block so a position with
squared distance 260 becomes covered at squared distance 245. Isolated native fluid ticks exercise water spreading into a new
air cell that completes a 2x2 water patch, then AGE-3 frosted-ice melting into
water; each new water position is checked in the pending queue before the next
aura pulse hardens it. The 2x2 fixture gives every converted ice block two
frosted neighbors so native neighbor updates do not melt the result. Native
inventory checks cover main inventory, offhand, cursor,
2×2 and 3×3 crafting inputs, and drops. The source conversion function names all 24
conversion inputs (sand, gravel, red sand, three anvils, sixteen concrete
powders, stone bricks, and netherite block) plus unknown dirt. Every falling
entity is expected to be removed; listed states place their converted block
at the nearest Y cell (`floor(entityY + 0.5)`, with native X/Z floor), while unknown dirt does not place a block.
Fractional-height probes cover a 1e-9 downward shift, both sides of the
half-block thresholds and their exact boundaries. Each probe asserts the
converted sandstone cell and that both adjacent vertical cells remain air;
negative fractional Z also retains the native floor behavior.
Vertical probes check the inclusive radius 16 and exclusion at 16.01, then
move a native falling entity into range.

The active fixtures also cover a live Polymer beacon veto and resume, a type-1
artifact and `time_stopped` as non-vetoes, and exclusion of a Creative
developer observer. The cleanup check expects the arena unload to clear the
type-3 holder, liquid aura, pending fluid queue, and tracked falling index.
Changed fixture blocks are restored, and holder inventories, positions,
modes, gravity, observer/developer state, and active-check entities are
cleaned up. A final falling entity and queued fluid are deliberately left for
the match-stop cleanup check.

Falling fixtures are encoded through native `BlockState.CODEC` and checked
after load, before testing the ability. Minecraft 26.3 does not decode the
old `{Name:...}` state format; silently falling back to sand would make the
conversion checks invalid. The boundary and observer probes reuse the same
fixture helper.

A controlled direct chunk write confirms stationary pulses do not scan the
whole sphere again. Calling the chunk-load handler then rediscovers and
hardens that water. This is a simulated callback on a resident full chunk,
not proof of an actual chunk unload/reload cycle. Pulse timing is wired in
the production tick; the focused fixtures invoke the effect directly.

## Limits

These are live-server checks, not isolated unit tests. They test block changes
made through the level block API; they do not claim coverage for direct
`LevelChunk.setBlockState` bypasses. Unloaded-chunk boundary behavior and
direct chunk-write reconciliation need their own fixtures if those paths are
changed. The focused smoke selection contains four operator groups and one match cycle; the
full LBC runner contains 45 operator groups and 16 match cycles.

The source behavior and requirements are documented in
[`ARTIFACT_LIQUIDS_NEXT.md`](ARTIFACT_LIQUIDS_NEXT.md). Passing these checks is
runtime evidence for only the asserted fixtures, not proof of every source
datapack edge case.
