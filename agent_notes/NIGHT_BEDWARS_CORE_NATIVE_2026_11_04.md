# Native BedWars core integration - 4 November 2026

Prepared against root's post-overnight baseline `a4de834c` / qualified installed `223c424b`. This agent changed ONLY `GameController.java`, `LbcTeamColors.java` and this note, plus ignored evidence. No build/server/commit, RP/template/shared-loadout/arena/auxiliary edits or user questions. Root owns NIGHT_QUESTIONS and all272 protected author paths. Mode2/4 remain gated, TMT LAST. Source defaults for questions6/7/9 are provisional, not author approval.

## Controller and checkpoint contract

`GameController.bedwars()` returns one final `LbcBedwars`, constructed after `LbcItems`. Core checkpoint stores the root's `LbcBedwars.Checkpoint` and calls `bedwars.restore(image)` after restoring authoritative Participant/core values. The scalar contains root-owned topology values, assignments, entitlement/pending timers/setup and closes shop windows on restore; native bodies/terrain/teams remain existing whole-coordinator owners. No second lives registry, private body snapshot or private rewind was added.

New package-private read-only `activeMembersForBedwars()` returns an immutable list of Participant UUIDs with lives>0 and !disconnected, INCLUDING awaiting native/BedWars respawns. Root's presence/winner logic consumes this, rather than solo living-player count.

`boolean bedwarsRespawnReady(UUID)` waits for actual alive replacement native body, current source entitlement, active mode3/match and the existing Participant.awaitingRespawn flag. It clears that flag, removes observer view if any, performs normal native reset, restores pre-reset native XP state and adds30 LEVELS, rejoins canonical team, teleports exact shared team spawn, applies `lbc.applyBedwarsRespawn`, clears source title and synchronizes inventory/XP. Root supplies the exact source LBC kit/skill/trinket loadout API; ordinary `lbc.apply(player,false)` was explicitly rejected because it excludes trinkets but regrants challenge/levelups/randomstat. No vanilla kit, levelup, challenge, island reconstruction or Stand re-award occurs in this BedWars-specific callback.

`void bedwarsCancelRespawn(UUID)` retires only that enrolled Participant: awaiting=false,lives0, canonical team departure and observer state after actual native body exists. Developer remains protected. A still-dead body is not healed/revived by this cancellation; its later native respawn callback takes the ordinary eliminated path.

Root service should remove a0timer only when bedwarsRespawnReady returns true; failed readiness retains accepted pending work. Root owns100tick counter and initial/remaining translated death HUD. Initial core start never invents a timer service.

## Initial start, tick and existing mode1 behavior

Start admission uses a private static-final `BEDWARS_READY=false` switch. Root flips only after all chain compilation/static integration gates are satisfied. No mode2/4 change was made. Existing menus/config can retain source values without admitting unsupported gameplay.

Initial mode3 begin validates family and calls root prepare(ready), requiring one spawn assignment for every enrolled UUID. It accepts multiple players sharing a team spawn rather than imposing one spawn PER PLAYER. Source-balanced/preselected teams and topology validation remain root's prepare owner. A rejected topology uses ordinary cancelled-start/lobby cleanup and a server log; no new RP prose/key was added.

No mode3 cage generation/removal, island snapshots or chest fill/refill is invoked at initial begin/startPlaying or existing renewal completion paths. Countdown calls BedWars.tick every unfrozen tick, waits for root's bounded beacon/arena setup `startReady()`, then counts the source60ticks before initial survival/loadouts. It does not run solo cage anchoring or create an extra countdown title. Common initial native kit/skill/LBC resource/startup hooks remain existing owners. `events.start/tick` is retained for vanilla skill state; ordinary stage1/stage2 admission is explicitly bypassed, so no configured solo stage reward/event is scheduled. Bossbar stays hidden for mode3.

Playing mode3 advances the root BedWars owner and uses winnerTeam (-1 pending/multiple,0draw,1..16winner) instead of solo count. Existing source five-dispatch completion delay remains provisional; no claim it is five seconds. Shared TimeStop pauses BedWars/countdown/gameTicks/victory countdown/border time while existing items.tick still advances the actual TimeStop engine. Developer suppresses auto-finish. Mode1's original branch remains intact.

## Death and actual native respawn

Existing Vanity death hook still has first authority. VIP retains its native death priority. When ordinary bed entitlement applies, no Participant life is decremented and Indolence finger is not needlessly consumed; root BedWars.death accepts its100tick pending record. Existing generic death cleanup, attribution and kill/profile rewards still execute once. Developer remains on the existing path. If no bed applies, configured residual lives and VIP/Indolence recovery keep their existing native logic (pending question7 default); there is no unconditional boolean final-death replacement.

A native respawn while BedWars.pending holds the new body as actual GameType.SPECTATOR, with Participant.awaitingRespawn STILL TRUE, preserving native team. It does not add a Spectators viewer, whose ordinary detach changes mode to Adventure and resets view state. Source entities/abilities see a spectator, and ordinary participant input/damage remains denied by awaitingRespawn. The clock's ready callback alone admits survival. Bed lost during this interval cancels entitlement through the callback above. Ordinary no-bed residual-life recovery skips solo island rebuilding on a BedWars map but otherwise preserves existing behavior.

Unrelated late operators are not silently enrolled with invented solo BedWars teams. Existing enrolled returns require a source team and no active BedWars pending record. Root must qualify operator rejoin/team changes with actual canonical participant topology.

## Team identity, results and cleanup

`LbcTeamColors.beginBedwars(List<ServerPlayer>,Map<UUID,Integer>)` validates complete source1..16 assignments, saves original lobby memberships through existing Assignment owner, and lets multiple real players share canonical numeric teams. No mutually-hostile solo fallback or suffixed team is created for BedWars. Rejoin resolves the source numeric team after native object replacement. The BedWars flag joins the existing typed team checkpoint; stop restores lobby preferences and clears the flag. No friendly-fire metadata toggle is added; root's exception-aware shared harm policy owns safety, preserving explicit item rulings.

Core captures team result and actual living winning teammates BEFORE BedWars.stop or Balance legacy reset. Source-default profile reward is nearest actual LIVING winning teammate only:100coins,one win,one random existing key. All living winning teammates receive source win message/creative inspection state; no extra profile awards. When every winner is pending, it announces a TEAM result and does not call it a draw or manufacture a profile recipient. Existing translated `swrg.title.player_win`/`player_win2` composition is retained.

Actual RP verification caught dormant TeamInitCommand's `Team Gays`/etc key construction: NONE of those16 strings exists in the current author RP. Native all-pending team names instead use exact existing `team_aqua`, `team_black`, `team_blue`, `team_dark_aqua`, `team_dark_blue`, `team_dark_gray`, `team_dark_green`, `team_dark_purple`, `team_dark_red`, `team_gold`, `team_gray`, `team_green`, `team_light_purple`, `team_red`, `team_white`, `team_yellow`. No key alias or RP mutation. Exact current RU/EN/RPR values and file hashes are in ignored core/static_integration.json.

BedWars.stop runs at match finish, lobby restore/cancel and shutdown before item/team teardown. Root stop must clear owned have_bed tags and auxiliary bodies/timers/windows. Full restore calls scalar restore instead of stop, preserving captured pending rights through the coordinated replay.

## Root completion / qualification boundary

Core files are released for root compilation and fixture work. Source APIs referenced are root's actual on-disk LbcBedwars methods and the agreed new LbcLoadouts.applyBedwarsRespawn. Core itself was not compiled or run here. `git diff --check` passed. Ignored static proof: `build/verification/bedwars-native-2026-11-04/core/static_integration.json`.

Root retains GameController ownership after release for the accepted Supernova renewal continuation: derive renewal capacity from active team count, prepareRenewal returns new shared spawns without changing native teams/lives/rights, wait for async source setup before releasing players, update Participant.spawn from that returned map. My edits only suppress inappropriate solo cage/chest work on the existing completion path; no accepted Supernova intent is cancelled and no old/new graph is hardcoded. Balance's original legacy transition remains its existing separate policy; mode detection already excludes balanceLegacy.

Required real tests: complete startup/setup60tick and source initial loadouts;2teams with shared spawns;real native death/body replacement/100tick hold/bedbreak cancellation;VIP/Vanity/Indolence/configured residual lives;all-pending team win/no fabricated recipient;disconnect/operator return/lobby preference restore;actual TimeStop pauses;nonempty coordinated rewind with team/bed/pending state;source/local map and accepted Supernova replacement;actual mode1 regression. Runtime/artifact/client/performance qualification is root-owned and not claimed by this note.


## Source 7b material-height preflight correction

Parent native candidate9d8f7876 source7b preflight failed before a match: original48-high upper templates startY288 and raw extent335, but their material fits vanilla bounds. Scoped MapManager change computes materialMinY/materialMaxY in the existing background split block scan, using one boolean material-state union over every palette variant. Air/cave_air/void_air and STRUCTURE_VOID do not count; any possible nonair material variant does. Existing native block-state decoding is reused. Empty-material templates use min>max sentinel and pass material-height preflight. Realmaterial min/max plus placementorigin use long arithmetic and must fit actual world getMinY/getMaxY; no world/dimension limits changed.

PreparedTemplate private record is now `(Vec3i size, List<TemplateSlice> slices, int materialMinY, int materialMaxY)`, constructed only by splitTemplate. Raw size/slices/block positions/entities/journal bounds remain unchanged. Out-of-height AIR padding is not clipped or rewritten; native placement retains its existing behavior, and all nonair material remains subject to validation. No added world scan or gameplay tick owner. fail(Component) now logs the actual translatable key+deep argument array before normal completion (literal fallback logs text), so untranslated load_failed cannot hide the concrete preflight reason in server logs.

Read-only NBT proof `build/verification/bedwars-native-2026-11-04/material-height-static.json`: source/native SHA256 equal. swrg:7b-5up5 localmaterialY0..7 => placed288..295, checksum3dfed83b6c527235ae93f3a7eb53e156cdc7ba5a3837fff24a6ad061378cfa67; swrg:7b-9up5 local0..12 =>288..300, checksumd8ba82ce1fbfd6678e7ac6934551cfb4fa6345cd09d5bcedb788a0ca8d6e4a03. Both raw48cube, all localY>=32 nonmaterial. Structure resources unchanged. MapManager SHA256 before scoped edit d3b83892172d3c9d5c17fae8f86cf7cc9f0be96fd4ba74fc3fa377018ccc5dab; releasedafter5a7f21fbe5f9e72184317ba913fc68e0c95da38dbac57c4ac353527ef5deece1. git diff --check passed. No build/server/commit performed; native source7b qualification remains root-owned.
