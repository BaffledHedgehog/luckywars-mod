# Three mechanical tools ? native verified,1 November2026

Source audit: [next port](NEXT_PORT_2026_11_01_AUDIT.md),
[breaker history](STEAMPUNK_BREAKER_2026_11_01.md),
[glove source](STEAMPUNK_GLOVE_2026_11_01.md).

## Current user contract

> ? ?????? ????????? ????????? ??? ??????? - ?? ????? ???????????? ???????????? ????? ????? ??????? ????? ??? ????

This supersedes the earlier durability/repair decisions. The breaker rolls25%
once per successful native main-hand block destruction or melee-contact branch,
including Unbreakable tools. Repair, standalone durability loss and armor damage
never trigger it. Shared destroy work cannot recursively trigger player input.
The original17 overlapping3-cubes coalesce; native drops/container contents and
causal terrain ownership use shared Stonekivi/IndirectKills infrastructure.
Accepted construction settles before Vanity capture; TECH cancels pending work.

Glove consumes the existing Moisei sprint clock after that owner ticks; it does
not create a second increment. Original fixed native modifier identities,200-tick
ramp,5 stopped-second recharge, switching/sneak behavior remain. TECH cancels
owned modifiers/tag, including after switching away. MAG does not suppress it.

Hook uses direct player velocity and native motion packets, no pig, mounting,
position corrections or camera rotation. It uses the shared exact ray queue and
TECH admission, a fixed contact point, source steering/damping, SHIFT retirement,
initial120 seconds after death and unlimited reuse until reset. The actual hand
loses one durability only after a valid contact. Sustained anchor/motion/order
lives in a persistent native player attachment; the existing player/body/core
owners save it, without a new private checkpoint engine. Boba transforms anchor
and vector; actual pending teleport/dimension/epoch/death/logout ends travel.
MAG beacon/artifact does not suppress this mechanical tool despite its old
magicwand presentation flag. Landing effects use OwnedEffects layers.

Three SimplePolymerItem types retain the old carriers/models/name/lore. Actual
kit tables reference three canonical ported leaves; all selection metadata and
sibling entries remain exact, checked against HEAD. No RP/template edits needed.

## Verification receipts (provisional)

All artifacts/failed receipts are retained under
`build/verification/steampunk-tools-2026-11-01/`.
Initial compile found obsolete ChunkPos(BlockPos) API; corrected to native chunk
coordinates. compileJava/processResources then passed; archives preserve native
bundled-jar metadata and verify every processed resource and compiled class.

6871ee59: four native regressions passed (Moisei, raw ray tools, technical armor,
full production Vanity); three new suites rejected. Actual spectator exclusion
was missing in breaker admission and is now fixed. Glove fixture needed a native
scheduler warmup; hook fixture had a pending real teleport, correctly retiring
its anchor. No weakening of the genuine-teleport retirement rule was made.

d9a9b2c8: full200-tick glove +25 retained ticks +5 stopped-second recharge passed.
Breaker actual Unbreakable block/melee inputs, probability, exclusions, geometry
and native drops passed up to a fixture assertion comparing mana across natural
regeneration; comparison moved to the synchronous action boundary. Hook retirement
diagnostics confirmed pendingTeleport=true with no TECH/role problem; reused the
existing acknowledged native ScheduledClient fixture. These failed receipts do
not qualify the batch as a whole. Children exited normally and snapshots restored.

No physical-client behavior or loaded-server peakTPS measurement is claimed.
Main JAR remains6763758b until the complete three-tool batch is qualified.

## Final native qualification

036b29de (fullSHA in candidate/qualification receipt) passes all seven groups on
ONE immutable artifact in seven fresh native mode1 matches: steampunk-breaker,
steampunk-glove, steampunk-hook, moisei-boots, raw-raycast-spells, techno-armor,
vanity-rollback. The native child exited normally; launch/config/player snapshots
and additional level/ticket bytes restored exactly; no retained child/world locks.
Elapsed runner time: 133.01 seconds.

Later failed receipts503ea206/6d11f450/31764e9a remain available. Raw hook fixture
construction originally transmuted only component patches, losing the registered
item's default flag: corrected to actual old-carrier metadata and its real Fabric
input callback. Hook now checks isFrozen directly as well as runsNormally, so an
immediate /tick freeze blocks same-tick use. No blanket delay or weakening of
TECH/genuine teleport/Time Stop gates was introduced.

9556 frozen inputs,2340 compiled classes and7156 processed resources are verified.
224 pre-existing author paths and31 live override files remain exact. Kit selection
metadata and exact migrated source modifiers have a separate structural receipt.
Final archive includes all prior installed fixes. No runtime legacy functions,
RP/template/PENDING changes, main stop/restart or remote push.

The breaker trigger is the final ACTION rule above; old durability/repair wording
in historical source notes must not be treated as the current contract.
