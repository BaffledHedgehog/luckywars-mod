# Witch authority ownership at native lifecycle boundaries

## Contract and scope

(user) Actors/control belong to the CURRENT authority holder and transfer with it.
Source thresholds, GLOBAL nonspectator incumbent selection and all successful
claim producers remain the existing shared acquisition rules. In particular,
an offline incumbent must not become a new qualification obstacle.

(user) Full Vanity must rewind the whole match: world/map, all players including
killed/offline/newcomer cases, native and private state, locations/dimensions,
motion, effects/layers, attributes, counters and core/map parameters. A player
NBT replay does not satisfy that requirement.

This stage fixes a concrete prerequisite: an offline player's old native tags
could survive an authority handoff and restore duplicate powers on reconnect.
It does NOT implement or enable the full Vanity checkpoint. Its acquisition
remains disconnected. The full original25 Chaos release gate stays CLOSED.
No main installation/restart/push, RP changes or pending-question-file edits.

## Implementation

`LbcWitchClaims` now records the current UUID for each successful claimed mode
in its existing match-scoped service. It still reads qualification/progress from
the actual source counters and native selectors; there is no new progress or
cooldown ledger. The existing source message/debit/reward sequence is unchanged.
Multiple different authorities may belong to the same UUID independently.

The actual GameController join/leave/death/respawn boundaries reuse that owner:

- Before native join powers can act, saved tags are reconciled against the
  current match's claims. A lost authority is removed; an untouched authority is
  retained by UUID, including a replacement ServerPlayer after ordinary death.
- Existing live native tags can be adopted at actual eligible leave/death
  boundaries for compatibility with native scripts/operator-granted tags. Login
  NBT never establishes a new claim. A conflicting existing owner is not replaced.
- Removing a current online holder's native authority tag revokes its reconnect
  claim on the next shared tick. This seven-entry check is lifecycle bookkeeping,
  not a second ability timer and not source-progress advancement during Time Stop.
- Match stop clears the session claims as well as online tags. Offline old NBT
  is rejected on its next login, including old Greed/Indolence active flags.
  A new server starts in its ordinary lobby with no claims from the old match.
- Spectator enrollment, eliminated/disconnected lives, authorized gamemode
  reentry, team restoration and native player death remain the existing core's
  responsibilities. Possessing an authority never enrolls or revives a player.

The shared passive owner also removes only its own Greed damage/knockback
modifier identity and active flag on reconciliation. A native autosave may
contain those permanent modifiers; loading it after the authority has moved
must not retain them. Eligible current holders regain their bonus through the
existing heartbeat and actual surviving-heart rules, without replaying effects
or constructing another immunity engine. Other attributes are not reset here.

Native Fabric source confirms JOIN runs at the return of `placeNewPlayer` and
LEAVE at the head of `PlayerList.remove`, before ordinary native saving. Normal
disconnect therefore already has passive cleanup before save. The stale-modifier
test deliberately loads a genuine earlier active autosave, as after an unclean
departure; it does not incorrectly attribute this case to ordinary disconnect.

## Verification

The native `witch-lifecycle` fixture creates only its owned TestGamma session,
uses the authorized native gamemode command, claims Wrath/Lust/Greed with the
real packaged root/token and source counters, and lets the actual Greed heartbeat
install its bonus using a native heart. It then:

1. Captures an actual native autosave containing the bonus, disconnects through
   the real listener and verifies old authority tags in the compressed file.
2. Transfers Wrath and Greed while the old holder is offline at the source minimum,
   preserving Lust; loads that exact earlier autosave at native reconnect.
3. Checks lost tags/Greed bonuses are gone, untouched Lust survives and the new
   session remains a spectator until authorized reentry.
4. Removes a live authority tag and observes its reconnect claim revoked.
5. Executes native lethal damage and client respawn, checking the replacement
   object retains untouched ownership without reviving its eliminated participant.
6. Stops claims while the saved holder is offline and checks its next native login
   has no old authority/active flags. Two original participants keep playing.

The fixture restores the two existing participants and removes only its owned
extra participant. The runner restores TestGamma's native data/stat/advancement
files and configuration only after normal child exit. It never writes main
server player data. One initial fixture failed because a nested gamemode command
was queued in the outer command context; waiting for the actual next tick fixed
the fixture, with the production command path unchanged.

Final artifact SHA256:
`b123f108ab72e5e93c76a800efaa1b7606b86899e0b8f3796bc02b6647ba1660`.
The11050 frozen Java/resource/class/build inputs and5239 packaged processed
entries match that exact artifact. Offline build and focused lifecycle/acquisition/
Indolence-rebirth checks passed. A final combined run passed ALL13 gameplay suites
in one actual mode1 match in102.70seconds: witch-gluttony, witch-acquisition,
witch-indolence, witch-lust, witch-lifecycle, witch-progress, witch-wrath,
witch-passives, witch-envy, time-stop, mage-nyauwu, indolence-rebirth and polymorph.

Exact native results,11 source receipts and protected file audit are recorded in
`build/verification/witch-authority-lifecycle/`. All199 author paths,31 live loot
overrides and installed main d9087f0a are unchanged. Normal child shutdown,
exact fixture-file restoration, no failures/cleanup errors and no retained locks
are recorded in `final-audit.json`. Large Gluttony fixture setup work is not a
measurement of production TPS; the preexisting Polymer packet-context diagnostic
at fixture startup is outside this stage and is not claimed fixed.

## Limits and next work

This is match-scoped ownership reconciliation, not persistent resumption of a
running match after a server restart. It does not replay private subsystem queues,
restore terrain/entities, rewind native player statistics or implement a Vanity
world/core snapshot. Future full Vanity restoration must also restore these
canonical claim UUIDs and reconcile restored players through the same owner.
No physical-client, arbitrary-map, network-latency or peak-TPS claim is made.
