# Higher electricity: Erecto ordinary and Academy

The Tesla + Electro continuation is Jibjun Tssengi Akke (`erecto_wand`).
Sosueneruga is the Electro donor, not the resulting staff. See the exact
[resource audit](ERECTO_RESOURCES.md) for source presentation and existing RP.

## User rulings (24 October 2026)

- Sniper admission / total debit: "РЎРѕС…СЂР°РЅРёС‚СЊ 1100 / 5000" (user), including
  negative mana after paying the additional 3900.
- Sniper erosion and termination: "РЎРѕС…СЂР°РЅРёС‚СЊ РїСЂРѕР±РёРІР°РЅРёРµ Рё С€Р°РЅСЃ 20%" (user).
- Sphere: "РЎРѕС…СЂР°РЅРёС‚СЊ РІСЃС‘" (user): sixteen short discharges every five ticks
  for fifteen seconds; those discharges pass through walls.
- Trial: "Р”Р°, СЂРµР°Р»СЊРЅРѕРµ РїРѕРїР°РґР°РЅРёРµ РґР°Р»СЊС€Рµ 70" (user). Accepted side-arc hits
  farther than seventy blocks from their own launch qualify; age alone does not.
- Existing user performance priority allows brief 18-19 TPS rather than severe
  spell latency. Finite short rays execute inline; moving charges advance every
  game tick. No new work backlog or arbitrary capacity discard is introduced.

## Source and native implementation

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Primary functions are `lbc/function/workingitems/awaken_tuntija_wands/erecto/`:
`click`, `click2`, `click_upgraded`, `click_upgraded_2`, its mode branches,
`central_tick`, `central_tick_end`, `arc_tick`, `arc_tick_end`,
`arc_tick_get_rot`, `arc_tick_get_rot_reset`, and the
`click_upgraded_2_mode_{sphere_tick5t,sniper_charge,ray_tick}` branches.
Source ray scheduling is in `lbc/function/players_tick.mcfunction`; its raw
counter is `ray`, source HUD E117. Shared damage/refraction helpers are under
`lbcr/function/`, including `raycast_molnia_phantom`.

Ordinary and Academy default launch one central charge and eight signed side
arcs. Source lifetimes are 200 ticks. Central flight is 2 blocks/tick, side
flight 2.25, with native per-sample one-damage intersections. Each volley owns
its focus, resolved by resident block DDA plus native entity boxes. Source
distance-dependent curvature and one-time turn toward that focus remain; another
cast cannot overwrite it. One shared Electro trace set handles Time Stop,
owner/death/epoch cleanup, reflection and Curse. Friendly mobs can be physically
hit; allied players are protected. This source family has no default/sniper
shield-blockable flag. Conductive impacts deal eight damage within fifteen
blocks using the native conduction type and source family target rules.

Academy has four modes; Shift+PKM cycles them for free even with negative mana.
Both hands use the existing native dispatcher and pay independently. Admission
is configured `erecto_wand_mana_use` 1100. Modes:

- Default: the same central/eight-side volley.
- Ray: adds forty releases to the player's raw `ray` counter. One finite
  fifty-block ray releases each tick, including after unequip; repeated casts
  accumulate. Native sample damage is 0.5; intersected targets receive Glowing I
  and Slowness CI for one second even if damage itself is rejected, as in the
  source. E117 displays the raw remaining count. KFC's
  exact legacy `ray +1` now adds a live release, preserving the counter's units.
  Purify, forget and match stop clear pending releases; Time Stop pauses them.
- Sphere: resident first-hit placement, offset one block toward the caster.
  Runs for 300 ticks and emits sixteen eight-tick phantom traces every five
  ticks. Those traces move two blocks/tick, retain source random refraction,
  particles, shield clipping and conductivity, and pass solid walls (user).
  Native lifetime expiry limits simultaneously active arcs without dropping
  scheduled waves. Time Stop pauses sphere age and emission.
- Sniper: admits at 1100, debits 5000 total (user), moves eighteen blocks/tick,
  source twenty damage per native intersecting sample, 200-tick lifetime.
  Source recoil, one-second Slowness CI, dust/sculk particles and sounds remain.
  At a solid block it damages conductively, erodes the local block box with
  native container drops, and has twenty percent chance to end (user); otherwise
  it continues. Captured terrain attribution survives later erosion and changes
  when reflection transfers the projectile. No legacy force-load churn is
  reproduced. Resident unloaded boundaries terminate flight. Looking straight
  down retains source free aim assistance after the admission check, without
  creating a shot; target search follows the existing enemy-only homing policy.

All direct higher-electricity damage uses native ELECTRO_BOLT with the actual
current owner, direct electric knockback and native cooldown/armor policies.
Conduction uses ELECTRO_CONDUCTION. The actual used stack/hand receives the
seventy-block trial result, and reflection cannot award the original caster a
different player's hit. Mana activation is blocked by antimagic; already
released electricity continues. No extra projectile physics is added.

## Craft, Academy and presentation

Native exact-two-ingredient recipe: Tesla (`lightningwand:1`) plus Electro
(`stelectro:1`), ordinary or Academy donor. It works in both native 2x2 and 3x3
menus, rejects extras, produces one ordinary staff, and consumes one of each
donor. Shift-click into a full inventory leaves ingredients untouched.

Ready scoped leaves after qualification: `lbc:ported/erecto_wand` and
`lbc:ported/academy_erecto_wand`. Ordinary source S / three lore lines and
Academy SS / one added green upgraded line retain name, animated model, flags
and style. Existing Academy admission, real side-hit trial and promotion are
native. All needed author translations/models already exist; no RP/template
changes or broad pool enablement are needed.

The unreachable `erecto_sniper_bruh` helper/admin branches are outside ordinary
and Academy input scope. The separate Yellow Lightning/Mom encounter is still
pending; this port closes only the Tesla + Electro continuation craft.

## Verification

Qualified build `0faca9ac`: 30 recorded suite results in two actual mode1 matches (146.69s). Exact 287 source files, 769 classes and 2775 resources match the tested artifact. Installed stopped main with rollback `8040ad37`; main not started. Evidence: `build/verification/erecto-final/`. Native Erecto fixture covers presentation, both native
crafting menus, both-hand cost, direct friendly-mob damage/allied-player guard,
reflection, conductivity, independent focus and curve, accumulating ray/HUD/KFC,
antimagic versus released electricity, actual Time Stop, sniper admission/debit,
real erosion and both random termination outcomes, free aim assistance, actual
fifteen-second sphere lifetime, wall-passing phantom arcs, actual used-hand
trial and native SS promotion. Separate probes exercise real match stop.

No real client-pixel validation, disk restart/unload/disconnect or peak-capacity
performance claim is made by these boundary fixtures.
