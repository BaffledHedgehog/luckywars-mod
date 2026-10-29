# Witch actor source closure ? 29 October 2026

Source-only audit. No Java/resources/RP edits, build, server run or commit.
Root owns integration; chaos_core owns base seven-sin acquisition/activation and
mastery. This note does not implement those systems or mark any actor complete.
Original full25 Chaos pool remains intact and release-gated.

Selector scope correction for this source audit: native unbounded `@e` searches
loaded dimensions globally. A distance or AABB argument binds the search to the
execution world. Below, historical shorthand such as "same-world" for an
unbounded count/existence selector is not an implementation contract; reproduce
each actual source selector's native scope. In particular, no-distance actor
existence/count gates and scheduled dispatch must not become per-world copies.

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
Both namespaces matter: rabbit/snake admission and impact recursion are **lbcr**;
actor helpers/schedulers are **lbc**. Calling only the lbc function graph misses
real Gluttony input. Black snakes belong to **Gluttony**, not Lust.

## Front doors and shared admission

`gluttony_get` produces `lbc:glutton_summons`: three independent one-roll pools,
not a random choice of one item. Whale raw flag bundle is
`{whale:1,magicwand:1,spellwithcooldown:1,gluttony_cooldown:1}`; rabbit is
`{rabbit:1,raycastspell:1,spellwithcooldown:1,gluttony_cooldown:1}`; snake is
`{snake:1,raycastspell:1}`. All are carrot_on_a_stick with authored model/name/lore.
`indolence_claw` is `{magicwand:1,indolence:1,spellwithcooldown:1}`;
`lust_get` is `{magicwand:1,lust_cooldown:1,spellwithcooldown:1}`.

Whale main/offhand input lives in spell1_default/left, requires sin_gluttony,
no stopper_magic marker within20 and no white_whale ghast in the current world.
Rabbit/snake main/offhand input lives in lbcr:raycast/left, requires sin_gluttony,
no stopper_magic20 and no existing grand_rabbit / black_snake respectively.
Neither ray is a one-use scroll: no source item clear/debit in these helpers.
The rabbit cooldown flags feed HUD readiness, not a fabricated private timer.

Indolence mainhand source uses an `unless score @s[tag=sin_indolence,...] ...`
selector embedded in its unless-score guard; offhand directly tests the item but
has no sin_indolence test. These are literal asymmetries, not evidence to invent
normalized admission. Both require indolence_cd not positive and stopper_magic20
absent. Source indolence_cd decrements every players_tick, not every second.
Lust main/offhand requires sin_lust, raw lust_cooldown and stopper_magic20 absent.
Lust's two cooldown counters decrement in players_1s (seconds).

## Indolence fingers, hands and respawn

Acquisition `indolence_get` summons one skeleton at each of up to20 random
nonplayer entity positions, current dimension. Persistent silent bow skeletons,
empty death loot, zero equipment drop chances. Boots/legs use lbc:finger equipment;
chest/head use lbc:void, all unbreakable. Then source invokes lbcr:podchinenie
at **all** indolence_finger skeletons. That helper transfers overlapping nonplayer
mobs to the caster's numeric team1..32 (or leaves unteamed), copies lbcID2 and
team_number and plays villager.trade1/1. It does not create a native owner UUID.
Do not substitute an arbitrary ordinary skeleton alias and omit armor/team work.

`indolence_hand` counts every indolence_finger skeleton in the same world:

| Finger count | Normal markers / angular spread per axis | Native tick lifetime | indolence_cd ticks |
|---|---|---|---|
| 0 |16 / uniform integer -40..40 degrees |60|40|
| 1..2 |8 / -20..20|60|30|
| 3..4 |4 / -10..10|60|20|
| 5..6 |2 / -5..5|60|10|
| 7+ |1 / exact holder rotation|60|5|
| 5..6 sneaking |3 controlled, two with knockb jitter|200|15|
| 7+ sneaking |1 controlled|200|20|

Every branch plays item.firecharge.use MASTER1/pitch0. Normal markers are
slowraycast/invisible_hand/magic/other, initialized from nearest nonspectator
sin_indolence rotation (integer-degree quantization before spread), then active.
Controlled markers are slowraycast/controlled_invisible_hand/magic/other and
read nearest sin_indolence rotation EACH tick; knockb adds independent uniform
integer -60..60 to both quantized axes each tick. They advance local .3/tick.

Ordinary hand tick sets Distance3 and executes three source samples at current,
+.33,+.66. All recursive frames then observe shared Distance0 on unwind and
execute their own local+1 teleport; the outermost frame runs last, so final
travel is **1.0** (intermediate teleport destinations1.66 then1.33 then1.0). Samples emit
purple dust [0.188,0.035,0.192], scale3, count1 FORCE separately to sin_indolence
and sin_vanity viewers. Each sample selects #minecraft:mobs in native dx=dy=dz0
plus distance<=15, excluding spectator and sin_indolence ONLY, and deals ARROW3
attributed to nearest sin_indolence. No enemy/team exclusion: allies and other
players remain eligible (subject shared approved role/Greed rules). Collision
with any non-#airs invokes fill from +.3 to-.3 AIR **destroy**, then kills marker.
Controlled hand uses the same damage/block behavior. No invented explosions.

`indolence_respawn`: teleport to random same-world finger; replace four armor
slots with exact author leather armor, bow+64arrows+claw. Head uses finger,
other pieces assasin. Source chest/legs/feet modifier entries incorrectly say
slot=head; preserve/document rather than silently repair. Set kill_one_skeleton,
remove indolence_active; global REPLACE scheduled helper5ticks selects all tagged
players, removes tag and kills nearest finger. Separate hand-only helper gives
just the claw. Root/base death interception must invoke the actual respawn edge;
these helpers are not a second vanilla death/respawn implementation.

## Gluttony rabbits

lbcr rabbit search: increments Distance, checks native dx/dy/dz0 #mobs within15
excluding raycaster/spectator, independently checks nonair block at previous
local step, emits cloud count1 offsets.1 speed.2, recurses local+.5 while Distance
<=300 AND no stopper_magic20 at next sample. Impact summons native rabbit at that
position, sets Distance400. Entity AND block tests can both fire at one sample;
source has no return between them. Native Type99 killer rabbit, persistent,
follow_range100 and movement_speed1; no replacement health/damage/owner-team
components. The initial source ray has no podchinenie edge.

Every20tick mobs_1s callback, EACH grand_rabbit with no stopper_magic20 counts
same-world grand_rabbits and reproduces at its position when count<=99. New
rabbits get the same native Type99 payload, and the cap is100 current-world
actors (sequential source selectors; no made-up per-owner cap). Normal native
killer-rabbit AI/attacks/loot remain actual AI, not a simulated periodic bite.
No source actor TTL. MAG production admission and clone gating are explicit;
these rabbits have no magic tag, unlike snake markers. Don't assume stopper or
nomagic physically deletes the rabbit from this helper alone.

## Gluttony black snakes and sculk field

lbcr snake search uses the same bounded .5-step entity+block/jammer recursion rules as rabbit,
but dark dust [0.001,0,0.001] scale4 and impact produces black_snake/slowraycast/
magic/other marker with **NO data.lifetime**. This is a persistent head, not TTL20.

Each native ray tick first fills a7x7x7 sculk box replacing #tannerkivi, then a
3x3x3 unconditional strict sculk box. Independent chance6 (0.06) can emit a
fragment if same-world total black_snake marker count<=78 (maximum79 after one
spawn). Fragment is black_snake/slowraycast/fragm/magic/other with lifetime20,
uniform yaw -360..360 and pitch -90..90, then actived. Fragments advance1/tick;
if quantized Y is outside0..319 they kill themselves (the following tp remains
literal source order). Every head/fragment selects #mobs<=5 excluding ONLY
sin_gluttony and deals GENERIC .5, with no source team/spectator/Greed test in
that local damage helper. Capture author for project credit without inventing
an enemy-only filter or globally relaxing shared Greed/role policy.

Heads additionally: two independent chance1 draws set shared snake_storona to1
then2 (later success wins); persistent shared previous value otherwise. Y<=10
teleports up30/pitch-5; Y>=310 down30/pitch+5. Side1/2 advances .5 and changes yaw
+2/-2. It fills9x9x9 strict sculk plus15x15x15 #tannerkivi replacement, then
chance0_25 (0.0025) faces eligible nonspectator/non-strict #mobs excluding
sin_gluttony and advances another .5. Source selector lacks limit=1 here:
multiple candidates may fail native facing resolution; don't silently select
nearest unless that behavior change is separately resolved.

Global tick1s, while ANY head/fragment exists in that dimension, checks every
#mob excluding spectator/sin_gluttony/sin_greed_active outside sealed realms.
Eight exact sculk probes at X/Z +/-.3 with Y-.5 orY+1 invoke snake_damage_entity.
Each successful probe repeats the same visible effects: Blindness10s/amp1,
Darkness20s/2, Hunger30s/4, MiningFatigue40s/8, Nausea50s/16, Poison60s/32,
Slowness70s/64, Unluck80s/127, Weakness90s/127, Wither100s/127. Apply current
ItemEffectLevels caps, native layers and causal effect tracking. This is sculk
contact anywhere in the world while a snake exists, not just near the head.

## White whale, clones and fog

Whale spawn at caster-eyes local+10: actual persistent Ghast, white_whale and
lucky_mob tags; Health**5400**, MAX_HEALTH**54000**, FOLLOW_RANGE200,
ExplosionPower30, translated white_whale nonitalic name. Exact difference in
health/max is not a typo to repair. lbcr:podchinenie at spawn assigns numeric
team/counters; initial motion is look direction*7, quantized via source marker.
Native ghast AI/fireballs remain native. Three global whale sounds play MASTER
at (0,1000000,0), volume1 pitch1 minimumVolume1, not just ordinary local sound.

Each live whale and clone's mobs_tick_lbc calls whale_tick: fills11x11x11 AIR
strict, kills native ItemEntities in positive dx/dy/dz6 region from its feet,
emits white_wool block_marker count10 offsets6 FORCE, and performs three
independent chance1 sound draws. This is real terrain destruction/native item
removal with shared provenance; not visual-only fog. Global TimeStop bypasses
this callback through tick_not_timestopped; source also has an explicit time
score check in mobs_tick_lbc.

Every20ticks main whale floors Health to a scratch score. Health3001..5000:
chance5 drives velocity7 toward random eligible #mob; Health3001..4000:
independent chance10 spawns fog toward random target. Both exclude whale/clone,
spectator/sin_gluttony/sin_wrath/strict_map_object. These main-whale helpers do
NOT add an explicit stopper_magic guard. At<=3000 and not cloned, spawn THREE
native Ghasts (persistent/lucky_mob/white_whale_clone, HP/MAX15000, follow200,
ExplosionPower30/name), mark original cloned, teleport original to (0,150,0),
set velocity(0,10,0), and play all three clone sounds globally. No source TTL.
No podchinenie is called for clones: do not claim automatic source team transfer.

Every20ticks each clone dies if no same-world white_whale exists. Source then
tries a Health synchronization command (ambiguity below). Independent chance3
movement7 and chance3 fog are each gated by stopper_magic20; target excludes
white whale/clone/spectator/sin_gluttony/strict but notably DOES NOT exclude
sin_wrath. Clone per-tick terrain and sound loops still execute separately.

Fog: native marker slowraycast/whale_fog/magic/other, data.lifetime100, initial
rotation/position copied by entity-target tp from producer, then actived. Each
tick advances1, writes *tempt from its team_number, emits light_gray_wool marker
particles count10 offsets2 FORCE; kills all entity types within2 unless source
exclusions apply: white_whale, clone, fog, sin_gluttony, sin_wrath, spectator,
sin_greed_active, strict_map_object, same_team or positive void_resistance.
This is a native /kill consequence, not damage or a TNT/creeper explosion carrier.
Nonair invokes fog_rc_explode: count1000 particles offsets10 and same kills
within5. **It does not kill fog marker or stop movement**: a blocked fog can
repeat impact each subsequent tick until its100tick lifetime. No fabricated
one-shot impact/real explosion. No source fog team/counter inheritance command.

## Lust actors and related helpers

Snakes are not Lust. Lust's own raw item chooses four physical ground/sneak
branches: nonsneak ground swaps with random same-world #mob excluding spectator
and temporary owner tag (not enemy-only); nonsneak air when small cooldown not
positive issues hidden ResistanceV20ticks, summons native EnderDragon local+3
(HP/MAX20,follow200,Phase1,persistent/lucky_mob/lust_dragon/name), invokes
podchinenie at spawn, velocity look*2 and mounts caster, sets lust_cd_small10
seconds. No artificial dragon stand-in. Every5ticks lucky-mob dispatcher kills
it (tp0,-1000,0 thenkill) if ANY nonspectator sin_lust is sneaking or no such
holder within20; otherwise requests holder-directed motion*2 and refreshes
nearest holder hidden ResistanceV20ticks. Literal rotation helper ambiguity below.

Sneak air when lust_cd<=0: ResistanceV20ticks, TWENTY independent calls to actual
other/random_mob_spawn (uniform1..90, final_luck_modifier7 overrides outcome22
Phase4 dragon), LevitationXXI20ticks, lust_cd10seconds. Native 90-preset ordinary
summons include source equipped mobs, Wither, charged Creeper and Warden Brain
payload; these are actual random mobs, not forbidden creeper blast carriers.
Existing Mobgun selects114 outcomes and launches them with its own motion/payload;
it is NOT an equivalent90-way spawn helper without a factored exact producer.

Sneak ground: native #mobs at .01..20 excluding lust_dragon/spectator/same_team;
for EACH target position, execute max_health_up_eight AS THE CASTER, then AS each
target max_health_down_eight; lust_cd30seconds. Source scratch base-health reads
quantize to integer. Ordinary lust_damage (upstream base combat callback owned
by chaos_core) similarly selects .01..6 HurtTime10 entities, raises caster base
MAX_HEALTH2 per victim and lowers each victim2. Target base<=0 tries kill unless
void_resistance positive or sin_greed_active. Do not turn this into healing,
flat current-health damage or a separate max-health ledger. Existing native
attributes/ItemEffectLevels/causal effect machinery are reusable primitives.

## Reuse and missing production work

Existing reusable building blocks: LbcIndirectKills immutable accepted causes,
trapChanges and native child/source attribution; LbcDamage typed ARROW/GENERIC,
LbcWands native role and spell source targeting, LbcMagic boundary predicates,
TimeStop native entity freeze and accepted damage replay, LbcDropBlocks native
fills/destroy semantics, TagValueInput/native entity payload loading,
actual rabbit/Ghast/dragon vanilla AI, ItemEffectLevels/effect layers, native
attributes, existing shared motion/teleport momentum, and current actual server
phase clocks. No source actor consumer or raw rabbit/snake/whale/Indolence/Lust
front door was found in production Lbc classes by focused flag/tag search.

Missing are concrete actor owners, not aliases: fingers+handover/respawn actor
edge, hand projectiles/control/destroy; rabbit native AI reproduction; snake
head/fragment terrain/contact curse; whale/clone native AI/terrain/sounds/fog;
Lust dragon/riding steering and factored exact90-mob spawn helper. Root should
supply shared input/hurt/death/lifecycle/projectile inheritance hooks rather
than run runtime functions or duplicate existing managers. A future service must
capture caster UUID/order/epoch when accepted, inherit that cause through native
arrows/fireballs/children/terrain/dropped rewards, keep family selector-specific
team semantics, and preserve delayed credit after author departure. Native
source references nearest CURRENT sin holder in several helpers; historical
cast credit versus current-holder steering is a policy question below. No
blanket enemy-only rule or blanket WILD erasure of explicit team exclusions.

MAG rules are source-local: launch guards20, rabbit reproduction guard20,
clone movement/fog guards20; magic markers are killed by stopper_magic20 and
nomagic artifact25, also susceptible to shared Danmaku bomb magic deletion.
Main/clone Ghasts, native rabbits/fingers/dragons lack magic tags. Their native
AI/physical terrain tick is not automatically suppressed by those marker-only
tests. Existing project participant/observer/current-epoch/sealed realm/TimeStop
policy remains shared and must be stated separately from legacy selectors.

## Literal source ambiguities recorded, no author questions sent

1. whale_clone_second sets Health from the entire main whale entity, with no
source `Health` path; do not assert source successfully copies numeric HP.
2. raycast_vpered_slow_dragon sets Rotation from the entire holder entity, with
no source `Rotation` path. Preserve source evidence; actual command result and
intended corrected steering require a recorded decision/native qualification.
3. Fog never copies team_number; reading an unset marker score may leave shared
*tempt unchanged. Clones likewise never call podchinenie. Captured author/team
is required for credit but must not silently claim these legacy filters worked
with newly inherited team numbers.
4. Black-snake facing lacks limit=1; hand mainhand/offhand sin guards differ;
sequential entity+block impact can double spawn. Do not erase these differences.
5. Hands point to nearest current sin holder each tick/damage, unlike immutable
accepted cast credit. Keep steering selection distinct from project attribution.
6. Head snake has no TTL; reproduction limits are dimension-global and source
kills/terrain can affect arbitrary eligible entities. No performance, whole-chain
readiness,100-actor load or physical-client render claim is justified here.

## Read-only author render assets

Checked author pack `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/
26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Existing lbc item definitions
whale, witch_rabbit, snake and equipment finger, void, assasin all exist. Both
RU and EN contain white_whale, lust_dragon, witch_whale, witch_rabbit, witch_snake.
All 16 translation keys extracted from the three original acquisition
tables also exist in both languages. Item models: `lbc:invisible_hand`, `lbc:metamorph_flesh`, `lbc:snake`, `lbc:whale`, `lbc:witch_rabbit`.
All six minecraft sound events lbcsounds.whale_1..3 and whale_clone_1..3 exist.
Actor visuals otherwise use actual vanilla skeleton/rabbit/Ghast/dragon, dust
and block_marker wool particles. No RP edit or substitute render asset proposed.

## Immutable source receipt

Manifest below hashes ORIGINAL file bytes; aggregate is SHA256 of lexically
sorted relative UTF8 path + NUL + its32byte SHA256. It includes actor graph,
transitive explicit helpers, input/dispatcher/TimeStop/jammer boundaries, source
loot and chance/team predicates. Existing mastery-owned source is evidence only.

Files: 105; aggregate SHA256 `6ca626c6c1dc377e7cb925102a0c0a58dae1a30d69fdd283c925bfa193dd4cca`.

```text
d081be87d3841977d6067f2a3d2e48fabb79c4af828a8ad8c5608a6386ff2638  lbc/function/mobs_1s.mcfunction
6747b78fe0ae363996e49e30e5bf960778959cdc09ee4d296a16d96bf76b3c90  lbc/function/mobs_5t.mcfunction
973a64a0910312c7c787eeada912655840b4e1c422597d3a1e524666da5b5086  lbc/function/mobs_5t_lbc.mcfunction
70fa3e4075cb14b3f9607c59b06fcdfaa23f6cd101d1b3b72e7fa49a8dc5d9c8  lbc/function/mobs_tick.mcfunction
8ac1f41c7a55f79acd6ed35ae188c0f5c14ece9479f8ea628b33a7dfabd1d2f2  lbc/function/mobs_tick_lbc.mcfunction
c7c581c0dcd49e5d87cf355e3e52d70b73c73ba2f1d805bf42dca16584240f10  lbc/function/other/artifact/nomagic.mcfunction
77d404c48ee7d0f8f6a73f5ea229a6368331c48af07bebdd6a13f76dc5eea81f  lbc/function/other/attributes/max_health_down_eight.mcfunction
b287b59b6637bb0c03ede2fb082f855927ee47324c824cfb38107aa8a0587b95  lbc/function/other/attributes/max_health_down_two.mcfunction
e849d246f9fbfa9d373590164009b6560de7575e25c6dc4a91ce518d2d96e990  lbc/function/other/attributes/max_health_up.mcfunction
7d577c9027a121485cf67198bf8223d0ee62e3db7de77e1fa70bc0c2bd4f9d70  lbc/function/other/attributes/max_health_up_eight.mcfunction
cb90d2d566aed9974c1a269be92b7ffa87a9c5037fe856ed90b259f6b99c9862  lbc/function/other/attributes/max_health_update.mcfunction
7ba668709d50b27a176028cd942565c417d73b793650a9722faa3130604c9a43  lbc/function/other/random_mob_spawn.mcfunction
bb0718cecabf2ba51601cb2d8f660b2b44811b735a210a1afd8ecb942dec9342  lbc/function/other/stopper_magic.mcfunction
89e2e98d87fcec4b489a6991e24a844df75f67b57a1f4107d9637e064d2efdba  lbc/function/other/stopper_magic_destroy.mcfunction
4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905  lbc/function/players_1s.mcfunction
851491714a1ed2831411a19a829d362e051fdc4b2d5b3db581318e01d383c483  lbc/function/players_tick.mcfunction
2fc7db076033ec892384fba5050761204bf4a44875ec699d7155f0c01f273b73  lbc/function/raycast_vpered_big.mcfunction
1b476fffa01c09d288c0fa698a206c8cc5f2c1727e5a12efed75c2750742a23c  lbc/function/raycast_vpered_slow_dragon.mcfunction
7b4951e7c1ac5aae5fd606e2d8ea53db0bbd8f9125fd77cd1ff4b0836f0c62b6  lbc/function/raycastselector.mcfunction
b9b983d5c0f1f8b2a8df8cd9012b3cba88d90f974801cb54c093461de7b0e070  lbc/function/raycastselector_kills.mcfunction
ee7896f9f8b1c7003ce732487f7aa6f0b75b07edffad6eb307fa1ba269679dfe  lbc/function/raycastselector_other.mcfunction
da799ec32ca58bb89f77cd22e84e85ecae6e9eebccce5e032851aa9bc799d1b4  lbc/function/tick1s.mcfunction
f40cde21c484d4bb283a3d4d26d48a151d19e2ff066a39836ad4c0dfd3051496  lbc/function/tick5t.mcfunction
55faddfdca07e3a486d0f826e9593969ae5fa05260cb2392c162569efa8cf5cc  lbc/function/tick_not_timestopped.mcfunction
f96b0af185d52fd8a18a6f15c1d9ecefd7678e725d0449673b3e39f9793f9773  lbc/function/workingitems/spell1_default.mcfunction
1fc3c74097023eafe4a7aedd15fb1c45e53efee3c6f8308efbd2e5207be83fe7  lbc/function/workingitems/spell1_default_left.mcfunction
1b919802474d7c91293c86f32cf77dc93a28d7f15761548df056df6d4c38b6d9  lbc/function/workingitems/witch_gens/gluttony.mcfunction
62ea262494f48d4181296623560815fb5f341e568982c7eb345e2ff7c12a98c0  lbc/function/workingitems/witch_gens/gluttony_get.mcfunction
5f7df344594fdc855fcc8c2944b74db59cce083f6b2aea55d322819267ab55a7  lbc/function/workingitems/witch_gens/grand_rabbit_spawn.mcfunction
566b2b83e8c69a38635346dfc0197682dd45a7772d2acc1b849c2ab398504f34  lbc/function/workingitems/witch_gens/indolence.mcfunction
59e931869deb825322d4acf16d9b04727cf6e47eb5294ee658203339a1511652  lbc/function/workingitems/witch_gens/indolence_get.mcfunction
80a909aa77dcd67ce512228e6284809d49871c9fd938cda21bf860a6ea565213  lbc/function/workingitems/witch_gens/indolence_hand.mcfunction
2023f22258dcdd352b053aa734525d144ad90d7d8196e414ae1c248dbe8e5897  lbc/function/workingitems/witch_gens/indolence_hand_0.mcfunction
e88f4da88c8173ed36fd3016498ac5d9139d8abb86df3901dab4f1aebb737785  lbc/function/workingitems/witch_gens/indolence_hand_1-2.mcfunction
d2af29b3407a3bbc5936e830ec53ebab863073ecf6bdb3cfa4f672a74e39f081  lbc/function/workingitems/witch_gens/indolence_hand_3-4.mcfunction
78afc9f7bb76ab4e4d990c46178c874bdb6b5642345275423f9acba356301a71  lbc/function/workingitems/witch_gens/indolence_hand_5-6.mcfunction
9347001eacdbcaab2153a07327ca4a8ffa6cd57ffe80d9f799ae7cdef98dc8e1  lbc/function/workingitems/witch_gens/indolence_hand_5-6_sneak.mcfunction
c883becd461217f1c1b4b22e939055a1692492755353cb8b9bb176e65c527f65  lbc/function/workingitems/witch_gens/indolence_hand_7-.mcfunction
548c65698c5a36d1d32c4260dbfae674463181189e7d26bf298b7a916393b45e  lbc/function/workingitems/witch_gens/indolence_hand_7-_sneak.mcfunction
f1efd27d298ea997a836a79de4558eaca9b05ed00f2c23ab8f34701fe8137368  lbc/function/workingitems/witch_gens/indolence_hand_controllable_rc.mcfunction
ec35ca3d6e2163f76639c39fad5cd9240d61c3daeb8c70dce62bbcb595f37bc5  lbc/function/workingitems/witch_gens/indolence_hand_controllable_rc_razbros.mcfunction
f0b908467f711a2b42216577b8a426b632e8a689b0f30353ce6b27b88180bcf1  lbc/function/workingitems/witch_gens/indolence_hand_destroy.mcfunction
01c64c44cd66574e6c6e35b80193e70ccb81357de8575392f85e33285426a97f  lbc/function/workingitems/witch_gens/indolence_hand_get_rot.mcfunction
cf88189f4eb443f8fe9afb68d535eabc5f1f961daed9059d484b29b6af3c1889  lbc/function/workingitems/witch_gens/indolence_hand_get_rot_with_razb.mcfunction
988cbe1e32261f3403da987b69dc2ee04653b4c7c12fe0505264807a60fb0e3f  lbc/function/workingitems/witch_gens/indolence_hand_get_rot_with_razb_2.mcfunction
91bd034d877633ddee3205c7bf24bd2639532bbaee62025a0fb51d9b2d4503c2  lbc/function/workingitems/witch_gens/indolence_hand_get_rot_with_razb_3.mcfunction
e09587c1e776013bb81ed77ce60a50c49b398243114ca7a4edb4e0006dcef526  lbc/function/workingitems/witch_gens/indolence_hand_get_rot_with_razb_4.mcfunction
e2436cd086785f49e6b6ef9a8ec3c5dbbdb5a5847668e68c6220856c8370979e  lbc/function/workingitems/witch_gens/indolence_hand_rc.mcfunction
30ad8f609b4c4b422c1b9ac96475caa4b345e19bfec8bdf40d31f928dea55fb6  lbc/function/workingitems/witch_gens/indolence_hand_rc_recur.mcfunction
4ef58d81ddf9784fb4978066cd407caa46398a93fd14822d30d3198ab7d896cf  lbc/function/workingitems/witch_gens/indolence_respawn.mcfunction
3f54f6b8a7e675ec2780a8211fdcfb163baa2c513a282ca894608ac55a932a01  lbc/function/workingitems/witch_gens/indolence_respawn_cheduled.mcfunction
d9bb871f1a8f2d073bbdb42fee4340392ca4240a73693416b77d10cfd2a8d633  lbc/function/workingitems/witch_gens/indolence_respawn_cheduled_as.mcfunction
f0bec635a42a852a681d4cfd07aa8c13106d11644b22a87d8e4c7cd88f4710e0  lbc/function/workingitems/witch_gens/indolence_respawn_hand_only.mcfunction
e81ec7a89d8a3d85cbd1170530b6c7f5aa41b161d4bca4a785792f294332d926  lbc/function/workingitems/witch_gens/lust.mcfunction
df3bd727fcbeb59e5dbed451e2fad8e59bc8f6044b8d524ea6be6269fbf4c50c  lbc/function/workingitems/witch_gens/lust_damage.mcfunction
568d6d14565135e6fa8a43231ca11c1062d3370bd722935963f4732fd3e7272a  lbc/function/workingitems/witch_gens/lust_damage_2.mcfunction
74baa6768f9b33cb1ab2a501137808fc1c862adbf2d575dd97e2a04d5b9a9d17  lbc/function/workingitems/witch_gens/lust_damage_2_many.mcfunction
a46e347fa1369dd2d1096ee0534ddb6d07fb0f0da9d646e6accd2bb4f5e5daaf  lbc/function/workingitems/witch_gens/lust_dragon_5t.mcfunction
1e28434410eb99f393f8db91e6a495a17e438d23e47826d4fb98d27b8e2ba213  lbc/function/workingitems/witch_gens/lust_dragon_kill.mcfunction
9970a55d040cdfed9a238b8af18c8125ca241685718cef515bc03a02449f5a40  lbc/function/workingitems/witch_gens/lust_flesh.mcfunction
8ca04d728f897017019abbffd63d29a28281858cc806c46fdc2630e16b0a5b98  lbc/function/workingitems/witch_gens/lust_flesh_not_sneak.mcfunction
c93e738fec9dbfc78cfa55d9bdd410530a9442d007ca299dbb81eb9540c316ac  lbc/function/workingitems/witch_gens/lust_flesh_not_sneak_air.mcfunction
372e4d08e260c01e32f555129743502706022cb6cb7855c3b53f63caebabba41  lbc/function/workingitems/witch_gens/lust_flesh_not_sneak_ground.mcfunction
70851b59c6de1cce7943b01ff3fe543229fd42f6cbe954385c4927736ab9b75e  lbc/function/workingitems/witch_gens/lust_flesh_sneak.mcfunction
fa990f1e7337267d50513d568f33b5fe97a7090091ebe98a0e3e24bb14b267b1  lbc/function/workingitems/witch_gens/lust_flesh_sneak_air.mcfunction
044d00d40139850ff181d5b738e5aa7844c994ee717923d73452bea39cbb1653  lbc/function/workingitems/witch_gens/lust_flesh_sneak_ground.mcfunction
da7d944fbf1264a6a69ca531db83fa4cdfe98831dd777453aeca7ac476f9b815  lbc/function/workingitems/witch_gens/lust_flesh_sneak_ground_many.mcfunction
59585082b879a1fb49d5715be5c83bc5df7a46507e2afb07ed3b12be9ae7c444  lbc/function/workingitems/witch_gens/lust_get.mcfunction
462511f91361ea9337f22f564bae2117038b3655ff432b1e90edb1fad5cff971  lbc/function/workingitems/witch_gens/no_gens.mcfunction
10270e78e9cfbc233dba158fff91b099b359ea99519e87cb0da05af01c00bca6  lbc/function/workingitems/witch_gens/snake_big_tick_rc.mcfunction
e881f6aa51384824f170c6f668a8b7f7b66345aff7b8baaf971c8ad386880591  lbc/function/workingitems/witch_gens/snake_damage.mcfunction
0ee9fa058f50301a2b588bac94f99e88dc7c0231926f93dbaa855ea8d507ae98  lbc/function/workingitems/witch_gens/snake_damage_entity.mcfunction
27c26a612dc5685e78e94657ddf86fc9763e6a1fb36ebdde137523322dd689c9  lbc/function/workingitems/witch_gens/snake_tick.mcfunction
bf5b21e946d28540f8ba48c6612014caf2de89a142ab3ab4a52076509ca7a88e  lbc/function/workingitems/witch_gens/snake_tick_if.mcfunction
017615b2588f0ffbd71a9643f365fe9c9903d4171263da8b910877f5ff657c73  lbc/function/workingitems/witch_gens/snake_tick_if_2.mcfunction
4726866e997a60f4a50e6643f50be746fb8b8cbc90fed9c3d0fe32bc71739b2e  lbc/function/workingitems/witch_gens/snake_tick_if_at.mcfunction
d0c82e9a64fb48a52c09e3b81a8d9b966c0d35ffea9ea8b26ff9a708afe2ec51  lbc/function/workingitems/witch_gens/snake_tick_tp.mcfunction
c117a42cd989da906ed87859a09fb0685f01e57c4ac46ef753b0597d64635a67  lbc/function/workingitems/witch_gens/whale_clone_random_sound.mcfunction
1682f2cc3782827b7e82ce4e68f1262deae939781a0072a307444e37c4f526b9  lbc/function/workingitems/witch_gens/whale_clone_second.mcfunction
132f58d6dceb0f175020655d9a544d1abe484964f9147ff8aaf8a1ffec74f18d  lbc/function/workingitems/witch_gens/whale_fog_rc.mcfunction
92d4862c8b9f81d2abbf8b42565583ab93d3928071ada21454fac695fd631b7c  lbc/function/workingitems/witch_gens/whale_fog_rc_explode.mcfunction
fdb3c0525aebbca7d025968f45612cc5353b7b84949047530fb7d54a25223399  lbc/function/workingitems/witch_gens/whale_random_sound.mcfunction
a07d56a5f26109c4e91be00df5f070aeb56e7e3fa6008f4e5b5f75b43f192fca  lbc/function/workingitems/witch_gens/whale_run_away.mcfunction
751ad7624ae266a8c2744998a39c9aa31d1cc4f7d800f0d0e6544c864e47e9ca  lbc/function/workingitems/witch_gens/whale_second.mcfunction
71aaff259c94872c0ffd956c80b8ba48080985d280b2dd2c8039ce2b8ac0b9c1  lbc/function/workingitems/witch_gens/whale_spawn.mcfunction
e9ee65d0202c7685838207ba6389a6ce55f3c92d495320388b4837724719601a  lbc/function/workingitems/witch_gens/whale_spawn_fog.mcfunction
1844a08518e04fdd6d60c3e82a111f1f88837c38822aae30db34d9001032cd0d  lbc/function/workingitems/witch_gens/whale_tick.mcfunction
4164ef835a33bcfa29c813e5bc4fb0549b2bdbd806cb566117a98c96b89b2bf4  lbc/function/workingitems/witch_gens/yes_gens.mcfunction
799b7add27de7eb92d62ecf7194769535f5624778f234860fca975f1583f0198  lbc/loot_table/glutton_summons.json
ef225696c6cbc216e1acff00a4e6cb8909d499960247ee78714712f83ed14141  lbc/loot_table/indolence_claw.json
05d1581dbdd7afa48bbd7567427d7ec825017695a6beadb7a1c6874b569d2169  lbc/loot_table/lust_get.json
6e4e199ce55f34eb8c46b2383a8f9ea472780d0d459c796b9681931f550fd3d0  lbc/predicate/chance0_25.json
a3944d3cac7a486c3344c8c3a89b2edb77a36d2d9e9013d611db1ec0d828b8b4  lbc/predicate/chance1.json
8ac1cc3ce1736d9d5f2300a2ba82f0c6c7164e30a8d0bca71707955ae350b50a  lbc/predicate/chance10.json
a21f7cffa54162f81e7197cbdc36e67c6df5977c5a0ea3b2974a5140ee1b6fb6  lbc/predicate/chance3.json
55e0f9cf6aac6ee4eee01f58f52960dee5f82576d6e376e3e74a75c9258cbd46  lbc/predicate/chance5.json
7b271659363adb98096923c5f62ed7ee24a16b583a7abec0254dd4556785f01a  lbc/predicate/chance6.json
db24246d2c53902d7e5ee09839db69d9d2ea4d781a4c589ce266b44cf502a7a4  lbc/predicate/same_team.json
d23cedfdcbf2cbe2679fbb46e0defa12a370ddfe41c589302c3aacc8814ac7db  lbcr/function/podchinenie.mcfunction
c8ec5488730a00149c0e876b46d6c07885266e189dffc9858e191d35d08f0a85  lbcr/function/raycast.mcfunction
0d1d633680ea529eae292676906ef1f4d151c41cc02d25917f8e8db7b74f7c87  lbcr/function/raycast_left.mcfunction
25d13b07fb24abf2a1679f59fb85a316e257c0ca449fd8e5d31ac062ab4d98a8  lbcr/function/raycast_witch_rabbit.mcfunction
75e340e714d6ea9428f90c5a4f465515fc1b34dae286287c058915550007edc4  lbcr/function/raycast_witch_rabbit_spawn.mcfunction
a0e252edc35d64716704499ac053cac53db1bdbeef4866dface6c654ff52f481  lbcr/function/raycast_witch_snake.mcfunction
1255c218e5476169b1b5836b6401a75c38cfbba19bef5ee61ff7ab7207d7521c  lbcr/function/raycast_witch_snake_spawn.mcfunction
```
