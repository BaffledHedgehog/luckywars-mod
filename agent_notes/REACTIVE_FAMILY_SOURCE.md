# Reactive family: source audit (23 October 2026)

Read-only source audit; no implementation, build, server or resource-pack changes.
Scope: reactive_shish_item, railgun_bow, shishgun, shish_wall_wand.

## Resolved implementation scope (23 October 2026)

The audit below records the original source and its suspicious cases. The later
implementation contract is [REACTIVE_FAMILY_PORT.md](REACTIVE_FAMILY_PORT.md).
Only Reactive Shish and Shish Wall are being enabled; Railgun/Shishgun remain
separate future acquisitions, and no broad pool is enabled by this batch.

User response to the range question (user):
> умный рейкаст без фиксированной дальности

Use the shared resident-chunk, CPU-budgeted ray search, with no fixed distance.
The earlier explicit user instruction for subsequent crafts already authorizes
one donor pair through native inventory/workbench recipes. The held-item gate
follows the existing two-hand usable-item contract, rather than copying the
unsafe main-hand selector. Shish Wall reuses the user's previously established
Wall dismantling refund maximum of 500, replacing the source's 1500 refund.

## Recommendation and required decisions

Reactive rod + automatic-mode improved Wall are a bounded **mechanical** batch.
Reactive_shish also feeds an outward Railgun/Shishgun recipe; that separate branch
can remain gated when qualifying only these scoped ready leaves, provided no
enabled acquisition advertises or emits an unimplemented reward. It is not a
dependency of the Shish Wall cast. Full family crafting closure needs all four.
Do not enable forest rare shish_of_everything or broad pools.
Target Java currently has reactive_shish only as a cleansing exclusion (LbcMagic),
not a functioning rod. Academy Wall is the existing wall:1,upgraded:1 donor.

User clarification required before faithfully porting suspicious source behavior:

- Shish Wall increments Distance AFTER recursive traversal. Its ..999 condition
  consequently does not bound open-air travel; source reaches command recursion
  limits instead. Confirm intended finite range (apparently 1000 quarter-block
  samples, approximately 250 blocks) rather than silently preserving/fixing it.
- The main-hand reactive call uses `unless score @s[SelectedItem=reactive] ...`
  rather than an explicit held-item gate. This is unsafe selector logic; offhand
  checks the flag explicitly. Confirm ordinary item-gated activation in both hands.
- Craft consumption differs: ground functions kill entire ingredient item entities;
  inventory functions decrement EVERY matching slot, not one selected pair.
  Use the existing native one-pair recipe convention only with applicable prior
  authorization or an explicit decision; do not label accidental overconsumption design.
- Direct TNT/creeper and terrain consequences require owner/indirect kill lineage,
  even though legacy source lacks it (agent_notes/AGENTS.md user requirement).

## Reactive rod

`workingitems/reactive_shish`: Levitation 20 ticks, amplifier15; coldownshish=25.
IMPORTANT: players_5t decrements this score once per FIVE ticks: nominal125-tick
cooldown, not25 ticks. At score19 (six decrements, about30 ticks), if magic is
not suppressed within20 blocks, place a3x3 nonpersistent spruce-leaf platform
at player Y-2, overwriting existing blocks. At integer Y>=322 use fixed Y319.
players_tick emits sapling/happy-villager particles and grass-step sound while
score>=21. UI shows raw score with E105, or E104 + translated ready at<=0.
Both default spell hand dispatchers reference the rod; no mana cost is authored.
Reset/death/purify clear the score. Cleansing exclusion must remain functional.

## Shish Wall

Both spell hands require mana>=500 and absence of nearby stopper_magic. Start
at eyes+0.5, then sample every0.25 through #minecraft:airs; emit bricks/sapling
item particles. Entities are not ray terminals. First non-air hit charges500 mana.
No item-specific cooldown is authored. Automatic mode priority: sneaking dismantle;
otherwise airborne horizontal; otherwise sprinting stair; otherwise vertical.
These are state-selected modes, not Academy Wall's stored/cycled mode.

Shared lbcr/raycast_end2_mode_* terminals: vertical7x6 local lateral columns,
horizontal7x7 at another forward0.5, bricks only replace #minecraft:airs;
stair places lbc:stair5x5x5 with yaw-dependent rotation/offset; dismantle removes
5x5x5 #minecraft:bricks, returns that many full bricks and refunds1500 mana if
>=15 removed. No automatic source mana charge on a miss. Hit-created blocks
must not cause duplicate terminal execution.

Craft: reactive_shish:1 + wall:1,upgraded:1 -> shish_wall_wand, both ground
and inventory routes. Railgun is not a dependency of this recipe or cast.

## Railgun and Shishgun dependency

Railgun release dispatch checks MAIN selected railgun_bow and stopper within20.
Gives Resistance20 ticks amplifier4. Range steps: charge>=20 ->charge*4,
otherwise floor(charge/2), capped300; initial eye offset2,1-block steps. Kills
nearest arrow within6; transfers potion effects unless special variants300..302.
Each sample summons immediate radius1 creeper except variant302. Special arrows
include homing300, extra explosion301, plus101 TNT,102 fangs,111 kiuas,
103 Erecto sphere,112 light trail,113 trap trail; this is a substantial dependency
tree, not a single plain explosive bow. Traversal honors nearby stopper. Source
also grants railgun advancement for speedy>=300. Tiny Electro + non-Railgun bow
crafts Railgun (ground/inventory).

Shishgun release dispatch: MAIN selected shish_gun, no stopper_magic or stopper
within20. Gives Resistance20 ticks amplifier4, kills arrows within2. Range is
floor(charge/4), cap700; execute only>=6, initial eye offset3,1-block steps.
Each sample summons fuse0 TNT plus marker tags slowraycast/explodein/magic/
mecha/instant lifetime4; spruce-leaf particles and three azalea sounds per step.
The instant selector at Lifetime3 calls other/shishfill: four overlapping volumes
replace #minecraft:tannerkivi with spruce_leaves (5x13x5,11x7x5,5x7x11,7x10x7).
This marker is delayed terrain conversion, not another explosion. Damage remains
the native TNT blast; no separate numeric direct damage is authored.
No authored item cooldown/mana. Railgun + reactive rod crafts Shishgun, both routes.

## Exact item presentation

All name/lore components below have italic=false; preserve line order.

- reactive_shish_item: carrot_on_a_stick, model minecraft:spruce_sapling,
  tooltip lbc:tier_c. Name translate reactive_shish dark_green. Lore
  reactive_shish.lore and reactive_shish.lore.1 green, then
  craft_with_upgraded_wall_wand.1 and .2 gold. Flags
  `{craftusable:1,magicwand:1,reactive_shish:1,spellwithcooldown:1}`.
- railgun: bow, model lbc:railgun_bow, tooltip lbc:tier_b. Name railgun_bow
  white. Lore joined type+bow.lbc aqua; railgun_bow.lore and .lore.1 white;
  joined literal Stendari green + ` Beta Server Harem event` dark_gray.
  Flags `{craftusable:1,transformable:1,bow:1,railgun_bow:1}`.
- shishgun: bow, model lbc:shishgun_bow, tooltip lbc:tier_s, Infinity1.
  Name shish_bow green bold=true. Lore joined type+bow.lbc aqua, then
  shish_bow.lore gold. Flags `{craftusable:1,transformable:1,bow:1,shish_gun:1}`.
- shish_wall_wand: carrot_on_a_stick, model lbc:shish_wall_wand, tooltip
  lbc:tier_b. Name shish_wall_wand dark_green. Lore joined type+staff aqua;
  score shish_wall_wand_mana_use/lbc.math + mana_on_activate aqua;
  shish_wall_wand.lore.1 through .5 white, .6 gray. Flags
  `{craftusable:1,transformable:1,staff:1,magicwand:1,shish_wall_wand:1}`.

## Native reuse and source links

Reuse LbcWands hand admission, mana, suppression and role guards; LbcWall
brick placement/dismantling/stair template, resident checks and trap/worldChanges
lineage; LbcCraftingRecipe donor matching and native consumption; existing shared
cooldown HUD/Time Stop lifecycle; shared native explosive credit infrastructure
if the dependency batch is authorized. LbcWall.cast itself has different ray
range/mode selection and must not be reused unchanged for Shish Wall.

Source root: [LBC data](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data).
Primary exact paths under data/lbc/function: workingitems/reactive_shish.mcfunction,
workingitems/reactive_shish_particles.mcfunction, other/reactive_shish_place.mcfunction,
players_5t.mcfunction, players_tick.mcfunction, workingitems/spell1_default.mcfunction,
workingitems/spell1_default_left.mcfunction, workingitems/shish_wall_wand/{cast,loop,check,end}.mcfunction,
workingitems/railgun_bow.mcfunction, workingitems/shish_bow.mcfunction, items/bows.mcfunction,
other/craft_ground.mcfunction, other/craft/inventory_craft_check.mcfunction.
Terminal sources under data/lbcr/function: railgun*.mcfunction, shishgun*.mcfunction,
raycast_end2_mode_{vertical,horizontal,stair,break}.mcfunction.
Presentation comes directly from data/lbc/loot_table/{reactive_shish_item,railgun,shishgun,shish_wall_wand}.json.
The source-only audit does not establish real-client model rendering or runtime qualification.
Read-only RP item JSON inspection confirms existing lbc:item/shish_wall_wand,
and both bow models use using_item + use_duration scale0.05, thresholds0.65/0.9,
with base, _1 and _2 models (fallback while using is _1). Railgun also declares
oversized_in_gui=true. No RP edits are necessary merely to reuse these items.
