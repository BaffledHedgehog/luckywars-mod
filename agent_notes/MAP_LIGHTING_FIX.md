# Map loading lighting synchronization

User report (26 October 2026): some players see dark chunks after loading a map.
Whether F3+A repairs the observed client has not been tested (user). The native
checks below establish server light and packet correctness; they do not claim
observation of that client's pixels.

## Cause and behavior

The loader formerly declared the map ready immediately after its final block
slice, before queued native lighting necessarily finished. Vanilla light-change
broadcasts also target only the tracked border of a player's view. An interior
viewer can therefore miss an authoritative light correction during bulk edits.
The exact cached official 26.3 source and synchronization review are recorded in
[the source audit](MAP_LIGHTING_SOURCE_2026_10_26.md).

`MapLighting.Update` captures affected chunks before writes, including a
one-chunk propagation margin. Native chunk send dependencies prevent premature
initial chunk/light snapshots. Per-resident-chunk native POST_UPDATE barriers
wait for sky/block propagation and publication. Map readiness and loading
tickets remain held until those barriers and final light packets complete.
Only already completed futures are joined; the game thread never waits for an
unfinished lighting task.

Full native light packets go to all actual tracked viewers, including interior
chunks. Pending initial viewers receive settled light with their gated initial
chunk packet. Resends use at most 16 chunks / 1.5 ms per tick and skip absent
chunks and untracked recipients. This does not generate remote chunks, rebuild
the whole world's lighting, resend block entities or alter block-write flags.
Late writes invalidate barriers and the final resend queue. Cancellation and
shutdown release send dependencies.

Removing the lobby overlay before play, and repairing externally edited lobby
cells outside a map operation, use the same eventual authoritative refresh.
Their ordinary block changes remain immediate. The loader's existing terrain,
container, bounds-journal, fluid suspension and success/failure handling remain.

## Verification and release

Offline Gradle build succeeded. The focused native fixture and the full runner
`tools/smoke_map_lighting.py --server-dir build/verification/key-reuse-server`
passed. The full run recorded 49 successful results across two actual mode1
matches in 89.19 seconds, with normal map1, map35 and broken-map1 loading.

The native fixture repeatedly opens/closes a roof and adds/removes glowstone
across a chunk boundary. It checks actual skylight/blocklight, packet section
masks and nibble values, an interior tracked viewer, an excluded viewer,
initial-send gating, cancellation, non-generation of an absent remote chunk and
restoration. It runs in play and lobby and after broken-map loading. Existing
fluid/weather loading/lobby transitions, operator/player rules, Sauvojen,
projectiles, Time Stop, scopes/SLR and actual match-stop cleanup also pass.

Evidence: `build/verification/map-lighting/`; tested artifact:
`build/verification/skywars2-map-lighting-ff96bdee.jar`.
Exact 437 Java sources / 1213 classes / 3140 resources match that tested JAR.
Immutable qualification: `qualification-ff96bdee.json`. Installer11424 adopted
this release, superseding the pending Sauvojen build. Its `main-install.json`
currently records waiting for normal main shutdown/world unlock (port24566,
RCON off); main disk remains1a8a3825. No forced stop/restart was performed.
Keep newff96bdee, priorc9e36258 and installed1a8a3825. Obsoleteb2c91dcf was
removed after qualification: cumulative324 old JARs /11549691952 bytes.
All175 protected author paths and31 live grimoire override files are unchanged.
Physical reconnect, actual Minecraft client-rendered frames and peak TPS are
outside these checks. No author resource-pack or template changes are required.
