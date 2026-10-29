# Active Lust / Похоть — native stage, not released

## Author decisions

(user) «Да, разделить режимы» — grounded ordinary Flesh use only swaps places;
airborne ordinary use only creates the dragon. The old inverted NBT condition
could run both branches on the ground.

(user) «Завершать превращение в антимагии» — an existing Lust dragon is retired
when the controller or the dragon enters antimagic. TECH does not affect it.

(user) «Да, защищать цель внутри антимагии» — swap candidates inside antimagic
are excluded even when the caster is outside. Use the shared suppression query
at the target's actual position and dimension, covering beacons and artifacts.

The previously approved current authority holder owns control and future attacks.
An accepted child retains its historical causal author. The rider must be hidden
from other viewers and safe from its own dragon's native contact damage/push.
Time Stop pauses authority actions and timers. Lust's melee HP theft continues
to use only actual direct melee targets, not attributed NPC/spell damage.

All questions stay in CHAT. `PENDING_QUESTIONS.md` is author-owned and untouched.
The full original25 Chaos release gate stays closed. No main installation,
restart or push. Full Vanity must restore world/all players/core state; a local
NBT snapshot is not a substitute.

## Implementation

Raw vanilla callbacks route the existing `lust_cooldown:1` carrier through the
shared two-hand wand dispatcher. Both hands require `sin_lust`; ordinary role,
dimension, input, antimagic and native Time Stop guards apply. Flesh requires
and consumes no mana and consumes no item. Successful casts use the existing
free-wand success callback without paid Mage maximum-mana growth.

Grounded ordinary use keeps the native GLOBAL random `#minecraft:mobs` selector
and the original `!owner`/`!spectator` exclusions, with shared protected-role,
Greed, concealed-polymorph and target antimagic guards. Allies may swap. Position and rotation
exchange through native teleport APIs; each entity keeps its own velocity.
Cross-dimension mob teleport uses the returned native replacement identity and
a zero relative velocity delta, avoiding double addition of its old momentum.

Grounded sneak uses the original feet distance .01..20, enemy policy and target
antimagic protection. The shared HP helper performs the source's two passes:
first add8 base HP per qualifying target to the caster, then subtract8 per
target. Integer base arithmetic, no instant healing and the existing lethal,
void-resistance/Greed and causal damage pipeline remain. Source cooldown30
seconds uses the existing `lust_cd` owner, with no duplicate timer.

Airborne sneak calls the exact original90 native mob factory twenty times.
All original types, equipment, NBT, charged creeper and global luck7 -> index22
remain; no narrowed pool or fallback alias. COMMAND finalization is enabled
exactly for rows without source NBT. Native summon failure does not reroll.
They appear at the source feet (`anchored eyes` did not affect its `~` coords).
Source hidden Resistance V20ticks, Levitation XXI20ticks and cooldown10seconds
are retained with the shared item effect policy.

Airborne ordinary use creates the actual native EnderDragon three blocks along
the caster's look from feet:20HP, persistence, native phase1 and original name,
attributes and tags. The shared `lbcr:podchinenie` bridge handles initial control.
`LustDragonMixin` changes phase steering/movement and excludes the controller
and actual passengers from native head/wing queries. It retains native entity
ticks, multipart poses, health, contacts with other entities and terrain damage;
it does not replace the dragon with a marker or disable AI/multipart updates.
Flight follows the current holder's look at the source2-block step. Native
model yaw includes its180-degree orientation difference. Unresident/outside
world-border destinations produce no movement, with no forced chunk loads.

The rider uses the existing FullInvisibility viewer overlay and passenger
filter. Its own/camera view remains native; other viewers only render the
dragon. Mount/eject/authority handover/retirement refresh the same overlay.
Current holder changes also use shared control/team/counter ownership. The
original five-tick global crouch/no-near-holder retirement remains, with the
new author-approved immediate antimagic retirement. TECH does not stop flight.
`lust_cd_small` uses the existing ten-source-second cooldown owner.

Native actor and child births inherit the existing persistent SpellSource scope.
Living children own their real emitter UUID; accepted projectiles retain their
captured parent's causal record. Native lifecycle hooks handle reload, epoch
retirement and match stop; no second death or player movement engine is added.
The complete producer enables actual raw mode3 acquisition: source qualification,
message/clear1/authority transfer order, real Flesh loot and original advancement.
Only Vanity acquisition remains disconnected pending its full checkpoint.
Offline/cross-restart authority reconciliation is not declared complete here.

The CHAT question about swap-target antimagic is answered and implemented.
Protected entities are excluded from the random candidate pool. If no unprotected
candidate remains, neither participant moves and the use emits no successful
cast bonus. HP theft target protection and dragon retirement remain unchanged.

## Resources and source

`lust_get.json` is migrated by the existing26.3 loot schema conversion and
explicitly packaged. Source carrot carrier, tier_ss, model `lbc:metamorph_flesh`,
name, four lore rows and all custom flags are retained. The actual read-only
author pack already contains both model definitions and all RU/EN keys; no new
template asset/key or author resource-pack edit is needed.

Exact receipts are in `build/verification/witch-lust/source-inputs.json`,
`mob-pool.json` and `rp-reuse.json`. Source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`:

- `function/workingitems/witch_gens/lust_flesh*.mcfunction`
- `function/workingitems/witch_gens/lust_dragon*.mcfunction`
- `function/workingitems/witch_gens/lust*.mcfunction`
- `function/raycast_vpered_slow_dragon.mcfunction`
- `function/other/random_mob_spawn.mcfunction`
- `loot_table/lust_get.json`

Exact26.3 native EnderDragon, summon and teleport implementations were read from
the local Loom source JAR, not approximated from old mappings or executed as
legacy command strings.

## Native verification

Candidate SHA256:
`68f6f7f16f268f04bded984122c1a9440ae6e188a53a4fcb949c112ee437d9be`.
Offline Gradle build passed. Its11047 frozen Java/resource/class/build inputs and
5237 packaged processed entries match (only Fabric nested-JAR metadata is
normalized). Candidate native focused and combined receipts are archived by SHA.

The Lust fixture passed actual raw both-hand authority admission, real swap and
both velocities, source HP transfer/ally exclusions/no heal/cooldown, ALL90 native
factories including payloads, exact twenty luck-modified draws, native dragon
mount/HP/phase/pose/look packets, external/self viewer policy, native head damage
to another creature, current-holder/team handover, immutable accepted child cause,
actual Time Stop, MAG retirement/TECH preservation, source crouch retirement and
natural source-second cooldown expiration. Updated acquisition and direct-melee
HP regression suites passed as well.

An initial12-suite order passed eleven but the existing Indolence fixture failed
its combined ordinary-flight elapsed-time/cooldown assertion. Without any source
or artifact change, Indolence passed alone and after Envy; the failure is retained
in `68f6f7f1-combined-*`, not relabeled as a clean run. A final combined run using
the prior stage's suite order with Lust inserted passed ALL12 suites in one
actual mode1 match in97.64seconds: witch-gluttony, witch-acquisition,
witch-indolence, witch-lust, witch-progress, witch-wrath, witch-passives,
witch-envy, time-stop, mage-nyauwu, indolence-rebirth and polymorph. That clean
receipt is archived separately as `68f6f7f1-final-*`; `final-audit.json` records
its native stage success, normal child exit and exact fixture restoration.
The earlier order-dependent elapsed-time failure was not reproduced in either
narrower control or the final combined order; no unrelated runtime code or old
assertion was weakened to obtain a pass.

Fixture startup retains the preexisting Polymer missing-packet-context diagnostic
also present in the preceding acquisition baseline; this stage does not claim to
fix it. The Gluttony fixture's large terrain snapshot setup pause is test overhead,
not a measured production TPS limit.

No physical client rendering, network latency, arbitrary-map, long-distance
cross-chunk dragon flight, all90 long-running AI combinations, offline/cross-restart
reconciliation or full25-trap Chaos release claim is made. Ordinary dragon death
animation is still native; only explicit retirement immediately discards it.

All199 protected author paths,31 live loot overrides and installed main JAR must
match the pre-stage baseline. The final receipt records normal child shutdown,
exact fixture snapshot restoration, no retained locks and no main deployment.

## Swap target protection follow-up

Follow-up artifact SHA256:
`0343e64cc74b9dbfab2c1d2c4a4a58ca32e62b79a47f6bc172d759bf7e9a4a95`.
Offline build and the full focused `witch-lust` scenario passed in one native
mode1 match (28.57seconds including setup). The new regression places the caster
24 blocks from the jammer and its sole candidate8 blocks from it, proving target
protection independently of caster admission. Rejected use preserves both
positions, both momenta and mana. Removing the actual jammer permits the same
raw input to swap immediately; all previous Lust fixture modes still pass.

The11047 frozen inputs and5237 packaged entries match this exact tested artifact.
199 author paths/31 live overrides/main d9087f0a remain unchanged; normal child
shutdown, exact fixture restoration and no retained locks are recorded in
`build/verification/witch-lust-swap/final-audit.json`. No additional RP assets,
main installation, restart, push or question-file edit. This focused follow-up
does not relabel the earlier12-suite artifact or open the full Chaos release gate.
