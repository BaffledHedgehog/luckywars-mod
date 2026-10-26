# Operator live settings and explicit return to a match

Operator-only /settings retains its native gamemaster permission and validation,
but reading/writing is allowed throughout the match stages. The three event
flags also persist at every stage. Suggestions now include the previously
omitted LBC toggles/limits and gameMode. Existing configuration consumers retain
their application timing; settings do not retroactively regrant kits or reset
all current players. Map/broken changes outside a ready lobby validate and save
the next selection without starting a paste over the active arena. Lobby map
selection retains its ordinary asynchronous preparation.

/gamemode0/1/2 (with the usual space after gamemode) are vanilla-client-readable
literal aliases, including optional native player targets. Word modes remain.
Aliases invoke the same native GameModeCommand.setMode loop and permission gate.
That exact command boundary can restore an observer, an eliminated/disconnected
participant or an authorized late target during an active playing phase.
Ordinary ServerPlayer.setGameMode and noncommand enforcement do not enroll.
Mode3 remains spectator and never restores participation.

Return clears viewer camera/effect/inventory copies, invisibility, noclip and
viewer flight/immunity; installs requested mode abilities even when adventure
was already selected; restores at least one life, eligible participant flags,
respawn/spawn, ordinary respawn loadouts and boss-bar membership. It does not
award another match entry. An already active participant changing mode retains
items and remaining lives. Spectator tick cannot undo a successful return.

LbcTeamColors retains a match-scoped departure snapshot before disconnect
restores original external membership. Rejoin restores the match team; a deleted
owned temporary team is reconstructed with its metadata. External deliberate
memberships remain external, and late participants get an empty solo team when
needed. Stop restores original membership and clears snapshots/owned empty teams;
lobby departures do not create an unbounded history.

Native OperatorSettingsChecks covers all settings, all stages/events, persistent
reload/invalid inputs, permission rejection, map/broken no-paste behavior,
0/1/2 and named modes, same adventure, actual viewer camera/mirrored inventory,
GameController leave/join, late enrollment, existing item/life preservation and
deleted team reconstruction. The normal game-cycle runner now expects accepted
midmatch health edits and restores the original configured value.

Pending signed effect preparation is preserved but its three mixins are
unregistered in this gameplay release. The native constructor remains0..255,
explicitly checked. No unapproved extended formulas are published. When the user
answers the forty effect questions, enable the completed policies and update the
explicit temporary unsigned-effect release guard in OperatorSettingsChecks.

Buildb2c91dcf passed34 recorded results in two fresh mode1 matches (54.29s), with
exact409 sources /1147 classes /3139 resources matching the tested JAR. Includes
team colors/original stop restoration, player rules, friendly projectiles,
Time Stop, directional knockback, rifle accuracy/zoom and real-stop cleanup.
Protected author changes/deletions and live grimoire overrides are preserved.
Installer11640 installedb2c91dcf after normal main shutdown/world unlock, retained
875fed3a rollback and did not restart main. Immutable qualification, live receipt
and cleanup evidence: build/verification/operator-controls-final/. Ready artifact:
build/verification/skywars2-operator-controls-b2c91dcf.jar. Retainb2c91dcf,875fed3a
and6e648580; retired1265875f. Cumulative cleanup321 JARs /11432160601 bytes.
Physical client reconnect, rendered frames, full main restart and peak TPS are
not directly observed; native lifecycle methods and commands are exercised.
