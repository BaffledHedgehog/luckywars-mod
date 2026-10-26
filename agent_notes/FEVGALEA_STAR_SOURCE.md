# Fevgalea Star exact source audit - 27 October 2026

Static source/RP audit for the standalone sword batch, baseline HEAD `fbb26fb`.
Only this new document is written; no Java, original datapack, author RP, builds,
servers, staging or commits are changed by this audit. Read project AGENTS,
agent_notes/AGENTS and [RESOURCE_PACK.md](RESOURCE_PACK.md). Existing shared
producer review describes the baseline `SauvojenStars`, not qualification of
parent's in-progress standalone implementation.

Source root S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
F = `lbc/function/items/swords/fevgalea_star/`; R = `lbcr/function/`.
Every function below is `.mcfunction`; line references are one-based.

## Item and authored presentation

[S/lbc/loot_table/fevgalea_star.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/fevgalea_star.json)
has one roll, one carrot_on_a_stick, count1; model `lbc:fevgalea_star`, tooltip
`lbc:tier_x`, max_damage4000, MAINHAND base_attack_damage ADD_VALUE16 and
base_attack_speed ADD_VALUE-2.2. Thus a default player has17 attack damage and
1.8 attack speed while held; do not confuse attribute modifier16 with total17.
No explicit max_stack_size, enchantment, unbreakable, weapon/tool or consumption
component is added. Preserve exact source components when converting identity.
Name is green/nonitalic translate `fevgalea_star`. Lore has12 rows: aqua
nonitalic `type`+`sword`, then WHITE/nonitalic `fevgalea_star.lore.1` through11.
Custom data exactly `{swordwithability:1,transformable:1,sword:1,magicwand:1,fevgalea_star:1}`.
No extra Legendary/adjective heading exists in this source item.

Author RP, read-only:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/`.
The item JSON points to `lbc:item/fevgalea_star`; model is vanilla `item/handheld`
with `minecraft:item/fevgalea_star` layer0. Its PNG is32x32. Existing RU/EN
name, all11 lore keys, `craft_with_fevgalea`, and both advancement keys are
present. Existing spellings, doubled `%%`, colors and translations are authored
content; no new key/model/texture/template is needed for this sword.
RU name is `Февгалея Звёздная`; EN name is `Fevgalea Star`.
EN lore11 explicitly says15%% target max health,100%% target attack power,
5000%% target speed; the misleading old function names do not override it.

## Acquisition and crafting

`lbc/function/other/craft_ground:42`: executing dropped `true_terra:1` item
requires a distinct `truefallingstars:1` item entity at distance0.001..1, limit1.
`other/craft/fevgalea_star:1..5` spawns the source loot at execution origin,
then green dust color[.129,.82,.43932],scale1 atY+1, count100/spread1/speed1
(force), enchantment_table.use MASTER volume1/pitch1; it KILLS one matching
item entity of each ingredient within1. This removes each selected whole
stack, not one item from a stack.

`other/craft/inventory_craft_check:42` requires both flags in the player's
2x2 crafting slots and `return run`s `fevgalea_star_inv`. Its lines1..3 spawn
the same item/cues at the player. Lines5..12 apply `lbc:count_reduce` to EVERY
matching slot0..3 for each flag. The modifier is set_count-1/addtrue: duplicate
ingredient slots can consume more than one of either ingredient for one output.
No3x3 positional recipe is authored here. The intended ingredient pair is
True Terra + True Falling Stars; normalization to the established native1+1
crafting contract should be recorded as native policy, not misreported as
literal source whole-stack/all-slot behavior. No extra advancement grant,
charge initialization or mana debit occurs in either craft function.

## Actual input and sequential dispatch

`items/spells:7` routes selected MAINHAND magicwand through `workingitems/spell1`;
that wrapper resets Distance and marks caster/raycaster. `spell1_default:78`
calls F/click_select. The offhand dispatcher exists globally, but
`workingitems/spell1_default_left` contains NO Fevgalea branch. Source alone
therefore establishes MAINHAND ability input, not an offhand implementation.
Shared user-authorized used-hand behavior, if applied, is an explicit native
adaptation. Mainhand flag checks are source bugs only when a preexisting user
contract authorizes actual used-stack correction.

F/click_select consists of FOUR sequential executes, without return/else:

| Line | Current condition | Helper | Cost and result |
|---|---|---|---|
|1|sneak, charge<=6, mana>=5000|click_uncharged_shift|mana-5000; self life field|
|2|not sneak, charge<=0, mana>=1000|click_uncharged|mana-1000; rain projectile|
|3|sneak, charge>=7|click_charged_shift|charge-7; radius20 dance|
|4|not sneak, charge>=1|click_charged|charge-1; owner-Health beam|

Predicates are reevaluated after each helper. For stable sneak state they
remain mutually exclusive: life field does not add charges, rain does not add
charges, and the last two rows require opposite sneak states. E.g. charge7
Shift emits only dance and leaves0; charge1 nonsneak emits only beam and leaves0.
No fallthrough rain after spending the last charge, because rain is earlier.
No general mana gate on the charged branches. Costs are raw scoreboard debit,
with NO Mage manamax bonus, trinket cast_wand event, wandCasted, Academy trial,
mode switch or item consumption anywhere in these helpers.
The outer `items/spells:2` temporarily adds10,000,000 mana for full Overload;
its final cleanup removes that temporary allowance, revokes the interaction
advancement, resets RC_Raycast and removes caster. It has no global cast_wand
or Mage callback. `spell1_default` additionally checks the independent full
Nyauwu armor effect at its tail; that is not a wand-cast bonus. Current native
LbcMana.trySpendWand itself only checks full Overload permission and debits
mana; it does NOT grant Mage/trinket bonuses. It is therefore suitable for
this item under the already-approved native Overload rule, without separately
calling wandCasted.

Life field (`click_uncharged_shift:1..12`) emits HEART100 atY+1 spread4,
then three normal particles at owner position, each1000/spread15/speed0:
dust[1,.616,.616]scale1, FIREWORK, END_ROD. It grants resistance140ticks amp4,
instant_health1tick amp99, adds7 to `void_resistance`, then debits5000 mana.
It neither adds nor spends charge. Exact native EffectCommands gives
instantaneous effects the argument as ticks, so authored duration1 is one
native tick, not20 ticks. This is checked through the real native effect.
`players_1s:26` calls `other/effect_void_resistance`: belowY0 it teleports holder
toY320; subtracts1counter/second and grants the existing void-resistance effect
advancement. Repeated fields ADD seven seconds; do not replace the counter.

Dance (`click_charged_shift:1..7`) emits normal1000 dust[.961,1,.616]scale1,
1000 FIREWORK and1000 END_ROD, spread15; trident.riptide_3 MASTER3/pitch1.5.
It stores owner team_number into `*tempt lbcID2`, then visits ALL #minecraft:mobs
at spherical distance0.01..20 with predicate!same_team. `damage_100hp:1..2`
reads EACH target's current Health*.5 to INT and damages OUTSIDE_BORDER:
amount=floor(current target Health*.5), not50% max health and not raw floating
half. The helper omits spectator and void_resistance guards; shared technical
roles/Greed remain required native user contracts. All same-team entities,
including allied mobs, are excluded by the source predicate. Finally charge-7.

Beam (`click_charged:1..3`) resets Distance, anchors at OWNER EYES, calls
click_uncharged_shift_rc, then removes1charge even when ray hits nothing.
The ray increments Distance BEFORE sampling and recursively advances1 while
Distance<=120:121 samples at offsets0..120. At EVERY sample: ELECTRIC_SPARK10,
spread1.5/speed2(force); independent15% amethyst_cluster.fall MASTER1/pitch2;
if any noncaster/nonspectator #minecraft:mobs lies within4, its damage helper
reads executing PLAYER Health to INT, then damages ALL such mobs within4 by
that amount OUTSIDE_BORDER. This is spherical radius4, not selector-cell
collision, and DOES NOT filter teams: friendly players/mobs can be hit.
Caster is excluded by tag, not by zero-distance condition. The Health amount
is reread at each contact sample before visiting that sample's targets.
After payload, a block outside #minecraft:airs sets Distance1000 and ends the
ray. Recursion also checks nomagic player radius16 and stopper_magic radius20.
Repeated samples use the imported outside_border bypasses_cooldown tag.

ACADEMY DIFFERENCE: the same ray helper invoked as a Sauvojen item_display
has no Health. Native execute-store result callback on failure supplies0,
not ambient storage17. Existing `SauvojenStarChecks` independently verifies
this after the outer command execution context closes. Preserve Academy
mode5's zero-damage beam; standalone player beam needs explicit Health input.

## Hit, charge and advancements

`lbc/advancement/gowno.json` rewards `items/swords` on accepted, unblocked
player_hurt_entity damage excluding #minecraft:arrow_only and interaction
victims, outside Nexus/Imprinted. `items/swords:57` checks selected mainhand
fevgalea_star and calls F/hit WITHOUT its own nomagic/stopper guards. `hit:1..2`
adds1 to player `fevgalea_charge`, then anchors at eyes and invokes hit_shoot.
Source hook is accepted non-arrow damage, not an intrinsic melee-only predicate.
Parent explicitly preserves this wide source hook, including all original
Nexus/Imprinted/interaction/arrow-tag exclusions, while excluding the own star
carrier/focus source from newly attributed percentage/beam damage. Existing
family-specific melee-only choices for other swords do not alter this source.
Percentage/beam helpers use unowned /damage so cannot recurse into owner hit
hook; adding required native ownership must not create extra charge/shot
recursion. Explicit accepted native attack admission or producer guard is needed.

`load:168` defines the dummy objective. No passive decay/cap/stack-local charge
exists. `leave_death:321` and `other/purify:29` reset0; KFC chicken
`other/eatables/kfc_chick/all_up_scores_no_mana:88` adds1. Existing native
LbcKfcFood already includes this counter. `ui/charging:8` shows the shared
player score when weapon.* has the flag: existing sword glyph, literalx,
score color#41BBE0; no denominator. Stack swapping must not reset player charge.

`lbc/advancement/true_advancements/swords/fevgalea_star.json` is a CHALLENGE
under `.../piston_sword`, visible/toast/chattrue. Icon golden_sword with
lbc:fevgalea_star model. Trigger player_hurt_entity requires a nonspectator
player with MAINHAND customdata subset transformable1,sword1,fevgalea_star1.
It contains NO victim-type-player restriction despite translated lore saying
another player. No charge threshold, craft grant, paid-use grant or explicit
function-based award exists. Reuse imported trigger/component translation,
not a new achievement for every right-click.

## Full reusable rain/charge closure

All projectiles are invisible marker carriers, not item displays; no additional
model or TNT helper appears. Hit_shoot chooses uniformly1..3, creates one
star_charge/flavour marker lifetime40 at source pose, copies Rotation/ownerID/
team and marks active. Rain spawn creates one star_rain lifetime100 similarly.
All three carriers include magic/player/slowraycast/shield_blockable tags.
`raycastselector:1,8,12..13` increments lifetime, runs payload, then kills at
lifetime>=configured threshold: final active tick still executes payload.
Player dispatcher31..33 selects charge, rain and drop closures.

| Producer | Per active tick | Contact and lifetime |
|---|---|---|
|star_charge_select|10 samples,9 gaps*.2, final displacement1.8|40 ticks; each sample emits9 colored local trail points; three local collision strips|
|star_rain_tick|independent50% spawn child BEFORE moving forward1|100 ticks; no block-stop test on parent|
|star_raindrop_tick|11 samples,10 gaps*.33, final displacement3.3|25 ticks; terrain terminates after sampled payload; entity contact does not consume|

Standalone click_uncharged anchors at OWNER EYES then local UP15 before rain
spawn. Academy marker eye-height0 means local UP15 from drone origin. Local up
is rotation-dependent; do not replace with worldY+15. Child `random_rot_down`
yaw is uniform integer[-180000,180000]*.001; pitch[70000,90000]*.001 (70..90).

The audited source uses #minecraft:mobs throughout. No `mobs_hhn` tag file or
reference exists in the inspected legacy LBC data, and no `friend` selector
is present in this closure. Do not substitute a guessed alternate tag or
add a source team exclusion where the specific helper has none. Native
technical-role/Greed filtering remains an independent established contract.

Charge three strips are local(0,0,0),(.6,0,-.25),(-.6,0,-.25). Source
`dx=.3,dy=0,dz=.3` selector volume is1.3x1x1.3. Helpers limit3 targets PER
strip and sample. Source wrapper marks owner AND same-team PLAYERS to exclude;
it does not mark allied nonplayer mobs. Shared projectileTarget has the same
player-alliance distinction. Flavor3 uses one exclusion tag for both owner/team;
flavors1/2 use two tags, with equivalent player eligibility. Target attributes
are reevaluated for each damage attempt:

|Flavour/color|Actual amount|Damage type|
|---|---|---|
|1 dust[.945,.494,.071]|floor(MAX_HEALTH)*.15|out_of_world|
|2 dust[.714,.945,.071]|floor(ATTACK_DAMAGE); missing attribute yields failed-store0|out_of_world|
|3 dust[.071,.698,.945]|floor(MOVEMENT_SPEED*50)|out_of_world|

`damage_with_tmp_void1` suppresses void_resistance>=1 except a source selector
accident involving active Greed. Native universal Greed immunity overrides
that accidental bypass; do not make Greed vulnerable. Neither function names
`10health`/`10speed` nor lore paraphrases establish different formulas.
Nine trail local positions are0 and left/right .2,.4,.6,.75 with forward
-.07,-.15,-.2,-.25; color scale1, one normal particle each. Block #airs check
occurs AFTER payload; no contact consume. At Distance10, raycast_end_with_tp
resets Distance0 and teleports to the final sample. No extra .2 movement.

Each drop sample emits normal END_ROD1 speed.25 and forced dust[.796,1,.984]
scale1;5% firework_rocket.launch MASTER.2/pitch2. A1x1x1 selector cell calls
`dmg_bypl_3nocd`, which excludes owner/same-team PLAYERS, limits2 other targets
and deals3 ARROW by an owner-ID player/marker. A fallback marker is attributed
if no owner player was resolved; native preserves original caster instead.
ARROW explicitly belongs to SOURCE and current native bypasses_cooldown tags;
no manual iframe reset is required, and saying ordinary arrow cooldown remains
would be wrong. Source out_of_world is absent from that tag; cached26.3 vanilla
bypasses_cooldown is empty, so do not infer repeated void damage bypasses it.

## Existing SauvojenStars reuse assessment and bounded decisions

Baseline producer already has exact rain100/drop25/charge40, localup15, final
sample displacements1.8/3.3, three strips/nine trail points, native integer
attribute formulas, parent terrain penetration, child collision and owner/team
player exclusions. Its shared role/Greed/loaded/epoch/TimeStop/suppression/shield
checks implement existing native user contracts. Baseline drop comment about
ordinary /damage without iframe reset is incomplete: imported ARROW tag is what
provides no cooldown. Runtime formula and failedHealth fixtures are existing
qualification evidence; this audit itself executes no tests.

Required standalone additions are identity/full source components, craft,
actual-owner Health beam with required attribution and the established native
ally policy, four input routes, accepted-hit
counter/shot, shared charge HUD/reset, and correct advancement behavior. No
source evidence requires a new boss, dimension, recipe ingredient, projectile
model or shader. Source beam has no ally exclusion, but parent retains the already-approved
general protection for allied PLAYERS pending an optional WILD-beam preference
in PENDING_QUESTIONS; this is not permission to change the established default.
This ally-protection selection is the parent's application of an existing
approved default, not a newly attributed user decision about this sword.
No new blocking author decision was established by this audit. Source quirks about absent offhand dispatch and excessive craft consumption
are stated above; apply only already-authorized native policies and record
any remaining preference in PENDING_QUESTIONS rather than asking asleep author.

## Exact inspected helper inventory

| F helper | Lines | SHA-256 |
|---|---:|---|
| `click_charged` | 3 | `42045056df2cf4d25b2c8eb1b831011c56ab84e01f293dde97cfa3ed50ae7de8` |
| `click_charged_shift` | 7 | `f2f2e039457854bdd187ba1e5e4f88a3496831d53bb7e51a337fd8f4ba35e6f1` |
| `click_select` | 4 | `ff06314a0c41d2bf750322f72d3aaa4dd0b7d1e4beea3da84f2395ed1a5db29e` |
| `click_uncharged` | 2 | `d2f02d89f459e22c2f5bb37ef754426144203745ae9ab8a9e4c173317b94199d` |
| `click_uncharged_shift` | 10 | `1b031b04497c8b1cfe4af1e5f9990d87df7211f1e68b49b0e275ff3528dc2f1f` |
| `click_uncharged_shift_damage` | 2 | `f424f47c54587f2d0f013469f6bba27d17894c3cdbde730f2edfa452da171a85` |
| `click_uncharged_shift_rc` | 6 | `4aa8355d5412780ed5691bfca86d8ea2198051fb703e91aca3049c666522915c` |
| `damage_100hp` | 2 | `d71f3c3f3a1946b465486eef13fec9735376af2c67a7a1410e358a151aa1c79a` |
| `hit` | 2 | `4f5bf7426258df34f2fd6dc675479c9a64cf76d9838ab353707e7564ed842b65` |
| `hit_shoot` | 8 | `2fee4fd746fd049c6348d62db70381b339f2e164a67a798d5324b71d8cfb9860` |
| `rain_projectile_spawn` | 5 | `4e6365871d4d3593669fac21e38563ba8c0678d5942e4f1478e779c54b36b8f6` |
| `star_charge_1` | 12 | `c33a38b234608fb36e3863a15ca65df33fe61403fd629ef79183e5c60ab516ea` |
| `star_charge_2` | 12 | `2dd1ada341fe67acbbeca2a2050021341fd67edd6185b075f659000366b74b8e` |
| `star_charge_3` | 12 | `3cc79c28b1ad2f056964637bcabc6987077251f3cfa3b6070a5bb67a3b762fb8` |
| `star_charge_select` | 7 | `c1bf92b94c00fcafe3cc6b2918582d97246ac37857883d0d83796883cb769c72` |
| `star_rain_tick` | 2 | `f4da8d0fe5542df0a04a6a3c962147e9edfe77c0bc8584036ca11efba31b3017` |
| `star_rain_waterdrops_spawn` | 5 | `ae4c1eda121b2af037436b396e2d8caa48b76a8db05458dcb8e9f6ed5ecec194` |
| `star_raindrop_tick` | 8 | `9964afcfe19e6f8ade312e4c424e5e680a4d3f0f0755601b64d10ea4269eb855` |

## Read-only author RP fingerprints

| Relative author RP path | SHA-256 |
|---|---|
| `assets/lbc/items/fevgalea_star.json` | `a16f02505a6c4e6b6810b1ae4db23730bcdb2bd20fcf25675a0df0b625d5f2f9` |
| `assets/lbc/models/item/fevgalea_star.json` | `96ab984df52eb87c6b56726eb6dfcca6de241d8725e6271e7312d598254dac1f` |
| `assets/minecraft/textures/item/fevgalea_star.png` | `898e365b7d36503c91c0131ff2cc9aee7c43820fcdc808b8ca85e329177183d4` |
| `assets/minecraft/lang/ru_ru.json` | `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc` |
| `assets/minecraft/lang/en_us.json` | `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7` |
