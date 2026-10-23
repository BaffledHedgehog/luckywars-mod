# Advanced Ender / Snow wand families

The ordinary and Academy variants close through four scoped item entrances.
Final native qualification is recorded below; source files and author resource
pack are read only.

## Source and presentation

Source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
The live Advanced Ender path is `lbcr/function/raycast_loop3*` and
`raycast_end3*`, dispatched from `raycast`/`raycast_left` and upgraded variants.
Snow is `lbc/function/workingitems/snow_wand/`, including `upgraded/`; its
five-tick cloud callback is `lbc/function/raycasters_5t` and its one-second
freeze processing is `mobs_1s` -> `other/freeze/cooldown`.

[Importer](../tools/import_lbc_advanced_snow.py) extracts complete ordinary entries
from `transformables_staff.json` and applies original Academy modifiers. Advanced
Ender keeps tier A -> S, `lbc:advancedenderwand`, nine original lore lines plus
Academy addition; Snow keeps tier D -> B, `lbc:snow_wand`, four lore lines plus
Academy addition. All 22 required RU/EN keys, two models and the existing Snow
sound were checked in the author's pack. No RP edits or duplicate template keys.

Scoped leaves: `lbc:ported/advanced_ender_wand`,
`lbc:ported/academy_advanced_ender_wand`, `lbc:ported/snow_wand`,
`lbc:ported/academy_snow_wand`. General staff/reward pools remain gated.

## Advanced Ender

Configured cost 3000. Ordinary casts seek the first solid block or suitable living
hitbox, teleport the caster to a centered block position before the wall, and
give native Instant Damage I for one tick (6 HP). The original deliberate
head-in-wall trial remains: the used ordinary stack becomes ready if the block
above the landing is not in the source `airs` tag. The existing source distance
advancement keeps its approximately 90-100-block window.

Academy removes self damage, grants visible Slow Falling I for 20 ticks, and has
three modes: default landing, highest MOTION_BLOCKING surface over the impact,
and the first two-air opening after traversing a wall. Wall clip ignores living
targets. Mode switching is free even with negative mana (user). A failed highest
surface at Y<=0 now keeps the original position and refunds the configured price,
fixing the old self-teleport/no-rollback and hardcoded3000 refund. The source's
message X is retained. The actually used second-hand stack receives trial credit.

Rays reuse `LbcExasperato`'s existing resident DDA, CPU budget, suppression,
epoch and cancellation. Position-aware block rules reuse `LbcTerra.wallHit`;
no second job framework or recursive command chain. Loaded terrain/eligible
living geometry determines the extent, including living entities above build
height, rather than the old artificial step cap. Source portal trails and
ordinary/upgraded wall offsets remain. The source instantaneous ray has no
shield-blockable marker; no projectile shield rule is added.

## Snow

Configured cost 1500. Ordinary casts create one native AreaEffectCloud two blocks
above the caster: radius4, Duration250, native default wait20, falling_dust snow.
Native ticking, metadata, client particles and wait+duration expiry remain in
Minecraft. Original Snow sound and burst of300 snowflakes remain. Academy aims
at terrain and creates the same cloud at impact+3Y. The former recursive no-hit
loop now ends at resident chunk/geometry boundaries through the shared ray queue.
Original upgraded Snow omits the mage/trinket cast hook; that difference is
preserved, while ordinary Snow and both Advanced Enders retain their hooks.

Every5 ticks, each cloud adds2 cold and sets cooldown2 to suitable entities within
5 blocks, with the original initial native Slowness multiplier. Only the existing
`LbcFrost.second` performs effect tiers/full freeze. This avoids fourfold effect/
particle work. Frozen targets are not refreshed by cloud pulses. Shared Frost
still owns ice snapshots, native effects, cleansing, HUD, KFC units, time pause,
restoration and thaw. Friendly mobs can receive the projectile/cloud effect;
caster, allied players and technical roles remain protected (user).

The Frost attachment also keeps the existing immutable applying-player cause
(UUID, application order and projectile policy). Deferred effects
and initial encasement use the existing indirect-kill owner; source survives
index unload/load. Timer/effect refresh keeps that application order, so later accepted player hits
retain precedence; an actually newer cold pulse can retake authorship (user). No new
damage type or cause framework is introduced. Unowned old saves remain valid.

Trial requires three suitable frozen entities in the cloud at once and updates
only its actually used stack while that same stack remains in the same hand.
Original nearby-enemy-player advancement is retained. Clouds use native owner,
existing item epoch and role/unload/forget/stop cleanup; an antimagic area removes
the cloud. Source cloud lifetime is not replaced with an invented cap.

## Verification

Build `1196e521` passed 26 recorded suite results in two actual mode1 matches
(51.56 seconds), together with Ender/Bad Wiki, Neutral, Frost, Academy Exasperato,
Reactive, Academy acquisition and KFC regressions. Runner:
[smoke_advanced_snow.py](../tools/smoke_advanced_snow.py). It guarded its directory/
port/lock, stopped its isolated server, and restored configuration, profiles,
scoreboard and JAR without cleanup errors.

The new native fixture checks complete source items, cost and independent hands,
free mode switching/insufficient mana/time admission, head-in-wall trial, default/
highest/clip modes in positive and negative directions, a341-block ray and a
living target above build height, configured refund for a low surface, empty
vertical miss, actual native self damage, native cloud owner/position/radius/
duration/wait/particle and natural expiry boundary, friendly-mob and protected
player/NPC/radius behavior, cold2/cooldown2 and one-second freeze boundary, three-
frozen-entity offhand trial, no frozen refresh, initial Slowness multiplier,
aimed cloud placement, direct unload/antimagic cleanup, native cold-blindness fall
credit after index restoration, later-hit precedence after a subsequent effect
refresh, newer cold-pulse precedence, actual successive-tick Academy promotion,
actual three-server-tick Time Stop of native cloud age and quiet match stop.

Native damage, effect and expiry boundaries include manual calls/tickCount setup;
this is not full natural flight/expiry elapsed time, real-client pixels, actual
disk restart/unload/disconnect or peak-capacity qualification. Per-tick explosion
benchmark timings are not new performance claims for this batch. The only final
ERROR output is the previously known fake-player Polymer packet-context pair.
All final suites passed. Exact source/class/resource byte equality was checked:
274 Java sources, 736 classes, 2755 resources. Full artifact SHA256:
`1196e5215ac15c3371ff6ce9658ad318c883302e22f96f070eb236532a2170fe`.

Installed to stopped main with rollback
`build/verification/main-rollback/skywars2-before-advanced-snow-3f17da51.jar`.
Main was not started. Evidence and install/qualification manifests are in
`build/verification/advanced-snow-final/` (ignored local output). Three preexisting
user Java files retain their baseline hashes and are excluded from the commit.

One old Nuke regression expected a removed rocket to disappear immediately from
the service index. Since the earlier native-unload iterator fix, GameController
drains ENTITY_UNLOAD at the safe tick boundary. The diagnostic established actual
rocket removed=true, live rockets0, mushroom1, pending blasts31, native TNT0. The
fixture now checks live entities rather than treating a pending removed reference
as an active rocket. Production unload timing and explosion behavior are unchanged.

A second historical regression race came from querying newly inserted Chaos
lightning in chunks whose block data had loaded but native entity visibility was
not ready. The Academy fixture now acquires only its own force-load leases, waits
for native `areEntitiesLoaded`, and releases leases in finally. This is test setup
only; production spell/chunk loading is unchanged.
