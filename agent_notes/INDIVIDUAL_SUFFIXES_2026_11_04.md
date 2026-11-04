# Individual overhead suffixes and native time-sprint experiment

Status: QUALIFIED AND INSTALLED, 4 November 2026. Test JAR `a47f6a1e`,
production JAR `5ce7d4f0`; the main server was not stopped or restarted.

## Author rule

User, 4 November 2026, answering the vanilla same-line team-suffix limitation:

> видимость невидимых союзников не нужна и в целом не используется так что можно ей пренебречь

Use separate client teams for individual suffixes in the main overhead name line.
The server's canonical gameplay teams and player profiles must remain unchanged.
Friendly invisible ally visibility is explicitly expendable. Existing FPV self
hiding is a separate feature and must remain intact.

## Implementation

`LbcNameplates` publishes immutable canonical team/player/selection images on
actual team mutations, cosmetic selection, join, disconnect and whole rewind.
Each connection receives a singleton cosmetic team per online player, including
players with suffix zero. Native prefix, suffix, colour and flags are retained;
the existing `LbcCosmetics.suffix()` adds the selected source glyph/style.
Visibility and collision are normalized for each viewer's actual team relation.
Native teams retain non-player entries. Ordered collision-checked synthetic IDs
preserve ordinary TAB grouping without changing GameProfiles, skins or list order.
The pinned 26.3 packet codec supports UTF strings beyond the old team command's
16-character input limit.

Packet projection supports all five native team packet methods and bundles.
Snapshots are captured on the server thread; the send path does not read worlds,
profiles, NBT or the live scoreboard. Unchanged ordinary packets take a fast path.
`FpvDroneMixin.Presentation` composes the existing FPV projection first, followed
by the nameplate projection. Foreign `sw2fpv` teams retain priority, including a
suffix refresh during control and restoration on exit.

No extra nameplate entities, resource-pack edits, BELOW_NAME objective or second
gameplay team system are introduced. Per-connection projection is transient and
rebuilt from canonical state after rewind; it is not another persistent snapshot.

## Disabled genuine time experiment

`/skywars check time-fragment-native` is an explicit operator test, not a usable
item. It requests 20 real native ticks with `requestGameToSprint(20)` and checks
actual Fabric/match/world clocks, native entity/projectile gravity and movement,
effect duration, scheduled block/fluid changes, connection ticks and packet count.
It requires an ordinary stable match, no TimeStop, no map/restart/rewind transition,
no existing sprint/freeze/step and a small resident fixture with grounded players.
It restores the scoped player, causal, terrain, scheduled-tick and ticket state.

Native sprint runs the whole server faster; it is not O(1), not a timer-only
approximation and not limited to a single player's area. Client input remains
ordinary client input. A small passing fixture cannot qualify peak TPS, large
maps, long jumps, physical-client presentation or the public fragment items.
Those items stay disabled. TMT stays last; modes 2/4, skill 2 and the broad public
rewind release gates are unchanged.

## Evidence

Stage: `build/verification/individual-suffixes-2026-11-04/`.
The baseline protects the author's 275 existing modified/deleted paths and the
previously installed `0e57163a` archive. Final `a47f6a1e` passes `nameplates-native`,
`nameplates-rewind` and `time-fragment-native` in three actual isolated matches.
The native experiment observed 20 server/Fabric/match/world ticks, 74 outgoing
packets and 40 connection ticks for two participants in 116.5972 ms. This is one
small scene, not a peak-TPS or long-jump result.

Rejected candidates and their complete logs remain in the same stage. The first
exposed unnecessary wrapping of ordinary teleport packets and duplicate native
non-player membership removal. Ordinary packets now pass unchanged, while native
membership edges are deduplicated against the recipient's client projection.
The second exposed a fixture-only grounding assumption; the headless fixture now
verifies solid support and supplies an ordinary native client ground-status packet.
Both rejected runs shut down normally and restored all scoped fixture snapshots.
One later mixed regression run was also rejected because its classic fixture
occupied the extra player sessions required by `vanity-core`. Its full evidence
was retained; splitting the roster-sensitive suite into its own process passed.
No production code changed for that fixture separation.

No physical client, true user late join or actual respawn is claimed by the new
fixture. It checks a genuinely replaced native listener, loaded/teleport ACKs,
all five team packet modes, bundles, reconstructed vanilla client scoreboards,
all 29 source selections plus zero, canonical team identity/metadata/members,
non-player membership, viewer-relative visibility/collision, real FPV attach and
release, and actual whole-coordinator restore. Existing regressions provide
separate compatibility coverage.

Final qualification contains 12 groups in 12 actual isolated matches: nine mode 1
and three mode 3. Besides the three new suites, it includes `vanity-core`,
`cosmetics-native`, `cases-cosmetics-rewind`, `fpv-drone`, `fragment-knowledge`,
`team-colors`, plus BedWars `nameplates-native`, `bedwars-shop-role` and
`bedwars-lifecycle`. All selected final logs are free of check failures and child
crashes; children shut down normally, exact snapshots were restored, temporary
maps were removed and no fixture locks remained.

Nine Java source families and only `skywars2.mixins.json` were freshly overlaid
onto the qualified note-palette archives. All archive entries were verified;
10,045 other source/resource inputs and all 275 author paths were guarded.
The linkage audit checked 3,178 retained classes and 64 fresh classes without
unresolved removed-member references. Both `build/libs` variants are updated.
`main-install.json` verifies installation to `E:/servers_mew/skywars2_last` with
the old `0e57163a` archive retained as rollback and `restarted: false`.

Exact machine evidence: `qualification.json`, `package-proof.json`,
`linkage-proof.json`, the four selected native result files, `main-install.json`
and `local-commit.json` in the stage directory. No remote push was requested.
