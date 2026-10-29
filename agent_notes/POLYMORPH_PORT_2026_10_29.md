# Temporary polymorph and Magnesia — native stage, 29 October 2026

(user) Ordinary wand polymorph lasts10seconds; academy polymorph stays permanent.
An ordinary-wand Chef returns if its sheep survives. A dead sheep does not return
its original. Original wand admission/gender/potion/hole rules and jeb_/KR20
presentation are retained. Noita's separate sheep form uses ordinary sheep stats.

(user) Replace End of Everything's Magnesia fireballs with10 volleys,4 spheres
per volley. Confirmed10tick spacing, XZ offsets independently uniform[-10,10),
spawn Y=selected entity Y+15, independent random3D initial directions,100tick
lifetime, weak bounded homing, through blocks, WILD eligible non-sheep targets.
Native speed.45blocks/tick and total3degree turn/tick are explicit tunable values.
The original global event remains global; role/observer/absolute Greed guards remain.
End49 starts one producer;50–52 are source schedule placeholders;53 follows after
100ticks so all10 volleys launch. No recursive selector/function implementation.

(user) MAG destroys spheres and immediately restores a polymorphed target inside
its area. TECH does not suppress this family. TimeStop pauses both clocks.

`LbcPolymorph` owns the reversible form shared by the ordinary wand and spheres.
The original entity object/UUID remains concealed and suspended, rather than
being reconstructed from an entity type. Native entity tick/despawn and custom
Chef/summon/mobgun work pause. On successful expiry it returns at the sheep's
current position. Permanent academy conversion retains explicit erasure.

A native participant keeps ordinary player movement and camera input; an actual
visible sheep follows position/look without teleporting the player. It cannot
push the controller. Sheep HP is8, movement_speed.23; the player scale is.85
and the ordinary camera remains attached to the player. External hit impulse transfers to
the controller. The sheep remains a real attackable animal. Native damage to
the concealed shell redirects; shared-source duplicate shell/body hits are
deduplicated without disabling intentional repeated hits within one shell.
Death restores original inventory/state before entering ordinary native player
death, preserving the existing lives/respawn owner. No second death engine.

Inventory/equipment and vanilla effects are suspended. Full native attribute-map
copies preserve permanent/transient modifiers. `EffectLayers.copy` explicitly
copies hidden successors omitted by the native copy constructor. Owned-effect
leases and signed absorption ledgers survive. Registered custom status owners
pause before validity cleanup/countdown; raw status counters are hidden and
restored. Cold's physical ice enclosure releases and is rebuilt on return.
Shared action/attack/mining/use/inventory gates block actions, but do not freeze
ordinary locomotion. Normal disconnect restores before native player save;
world/match stop releases forms before ordinary service cleanup.

The resource pack remains read-only. Template model `lbc:polymorph_sphere` is one
billboard plane, opaque emissive alpha254 inside its circular silhouette, with
five actual sheepwand-tip colors and ordered color dithering. A separate actual
display-outline mask supplies bloom, instead of recognizing pink scene blocks.
Installation: [RP instructions](../resourcepack_template/POLYMORPH_SPHERES_README.md).

Final candidate f39de5487ea7b3f04df55d47c7faa74cc9d90d305fea4322d7f191ab7c6861d6
passed17 gameplay suites across2 actual mode1 matches60.10s/58.17s: polymorph,
sheep, apocalypse-events, chef-execution, god-hedgehog, effect-layers,
full-invisibility, quiver, ordinary-summoners, chain-lightning-fixes,
shared-status-hud, food-lifecycle, babylon, signed-effects-math, signed-visuals,
dimension-items, frost. Classic preparation/cleanup also passed.

The new fixture uses natural200/100tick clocks, native TimeStop, same-object
cow/Chef return, native participant move/look, real blocked melee, body HP and
external impulse, hidden effect chains, owned withdrawal, permanent modifiers,
ordinary sheep-death boundary, actual MAG block retirement, actual End49 producer
and40 launches, wall traversal and natural orb expiry. Sphere geometry is exact
swept box clipping. No physical-client/ping/full-apocalypse/peakTPS or native
replacement-player-death roundtrip claim is made by these bounded checks.

GPU: eight native item variants and new bloom program compile/link on RTX4080
SUPER; opaque interior, positive outline emission and zero new emission from a
pink scene without an outline are verified. Original RP hashes unchanged.
Evidence: `build/verification/polymorph/f39de548*` and `gpu-results.json`.
11008 exact source/processed inputs;5210 JAR class/resource entries compared.
199 existing dirty paths,31 live loot overrides and main d9087f0a unchanged.
Both owned children shut down normally, exact snapshots restored, no locks remain.
Rejected earlier runs remain: NBT load reset the sheep spawn position; native
copy lost hidden effect nodes; fixtures retained a returned cow in a later ray
and retired a MAG marker without its migrated block, causing false ray failures.
Each issue was fixed and the final exact artifact rerun. No failures were hidden.

Full original25 Chaos transitive release gate stays CLOSED. No main install,
restart, push, author-question-file edit or old-JAR deletion. Remaining Witch
acquisition/Indolence/Lust/full Vanity work is unchanged; do not narrow the
world+all-player+core Vanity contract to a player-NBT-only checkpoint.

## Immediate HP and all-sheep match follow-up

(user) During temporary polymorph, human HP equals sheep HP. A match must not
finish merely because all or almost all living participants became sheep.

The previous native suite verified the sheep damage but not immediate human HP
in the same call. The old tick-only copy left a short stale-health interval.
Body.setHealth now mirrors every native HP change immediately, including healing
and zero HP. On accepted lethal body damage, the existing finish/restoration and
native player death callback run before hurtServer returns. The normal original
HP restoration precedes the inherited lethal hit, so the already-zero mirrored
HP cannot suppress the native die/life accounting. No death engine was replaced.

GameController victory already counts registered participants with lives and
observer roles, not wands.validTarget or visible mob identities. It needed no
formula change. The expanded natural fixture verifies a partial-sheep interval,
then both remaining native players as sheep for the full200tick lifetime, stable
lives/participant identities, matching HP and successful return without match end.
The separate polymorph-death scenario exercises actual lethal native body damage,
exactly one life/death/attributed kill, the PERFORM_RESPAWN packet and replacement
ServerPlayer, and continued two-player gameplay. This closes the earlier narrow
replacement-player-death proof gap, not every possible death/latency combination.

Candidate59df476169559d7a71de2de38aebd05f86c5ec931d9c25136656386932838b70
passed polymorph, sheep, chef-execution, effect-layers and polymorph-death in an
actual mode1 match (47.41s runner total). Build succeeded offline. Frozen11010
inputs and5211 JAR entries match. Normal owned-child shutdown, exact fixture
snapshots restored, no retained locks;199 author paths/31 live loot overrides and
main d9087f0a unchanged. Evidence: build/verification/polymorph-hp/final-audit.json,
59df4761-native.log,59df4761-results.json and inputs-59df4761.json.
No RP changes or physical-client rendering/network-latency claim. Main release
still waits for the full original25 Chaos transitive chain; no install/restart/push.
