# Native solo team colors — user2, 25 October 2026

Source: read-only original server
`world/datapacksdisabled/swarogskywars/data/swrg/function/core/create_teams.mcfunction`
and `game/start/team_join.mcfunction`. The source creates numeric teams1..32,
colors1..16 aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green,
dark_purple, dark_red, gold, gray, green, light_purple, red, white, yellow,
then repeats for17..32. It disables seeing friendly invisibles and explicitly
keeps death messages visible. Friendly fire, collision and name-tag visibility
retain vanilla defaults. Source team_join assigns one random unteamed player
to the numeric team associated with each spawn marker.

The current GameController supports mode1 only. Its ready participant order and
shuffled spawn list now also assign one distinct native scoreboard team per
participant before countdown. This gives real players native name/tab color;
AttributeHud already consumes native team color for sidebar routing. No custom
tab-name override, resource pack edit or new team gameplay mode is introduced.
The runtime API is Minecraft26.3 `Optional<TeamColor>`, verified against cached
Minecraft sources rather than the older ChatFormatting setter.

LbcTeamColors registers/reuses the32 numeric teams at server startup and match
assignment. Existing numeric teams keep identity, memberships and all noncolor
metadata; only their source palette color is applied. New teams receive source
metadata. A numeric team already occupied by any member is not reused for a
solo player: a separate temporary team receives the corresponding palette
color. Players beyond32 similarly receive distinct teams. Equal repeated
colors never make players allied; native team identity remains authoritative.

Participants' original custom/numeric/absent memberships are saved and restored
on disconnect, match cancellation/lobby restoration and server shutdown. A
later external reassignment is preserved. A removed original team is not
silently recreated. Only empty temporary fallback teams owned by this service
are removed; source numeric teams remain registered. Match memberships persist
through respawn and the winner presentation, then end at lobby restoration.
This is a runtime restoration contract, not crash/disk persistence coverage.

TeamColorChecks.run verifies the32 exact native colors and actual live mode1
participants' distinct teams and formatted names. Native isolated scoreboard
fixtures cover occupied numeric teams, source metadata, preservation of custom
membership, disconnect/stop restoration, fallback cleanup and newer external
assignments. stopProbe/cleanup verify membership restoration across the actual
GameController stop. Root integration must register these three commands in
GameChecks. Checks were prepared and statically reviewed only: no build,
server launch, client pixels or runtime PASS is claimed by this task.

## Combined integration qualification

Root built `17daf0d4` and ran114 recorded suite results in two actual mode1
matches, including these native checks and actual stop cleanup. See
[combined qualification](GAMEPLAY_FIXES_2026_10_25.md) for artifact identity,
ready-only deployment state and verification limits.
