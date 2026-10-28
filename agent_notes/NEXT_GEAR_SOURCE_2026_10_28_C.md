# Next complete gear source audit — 28 October 2026

Implementation and author decisions: [Reactive, Techno and MK2 port](TECHNO_ARMOR_PORT_2026_10_28.md).
The original audit below is historical source evidence; its open questions and
WILD policy are superseded by the author's recorded answers in that contract.

Read-only audit of current Java/resources before historical notes. No gameplay,
build, server, RP, author override or commit changes. This is source evidence,
not runtime qualification. Legacy paths below are relative to
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

## Recommendation

Choose complete **Steampunk/Reactive + Techno + Techno MK2 armor and same-slot
cyberpunk fusion**. This contains real missing runtime mechanics and closes the
inherited flags. Do not claim completion after electric immunity or catalog work.
Multiplying and Randchanted armor are finite native-component qualification
work, but have no separate source ticking family behavior to implement.

Current native Java has full mixed Techno/MK2 electrical protection in
`LbcDamage.allowDamage`, and mechanism-presence recognition in `LbcMagic`.
`LbcBolts` already owns normal slot and Ultra reforge routing. Searches for
`reactivefuel`/`technomk2shield` find only KFC reset objective names, not armor
consumers. `LbcReactive` is the rod/wall ray consumer, not reactive armor.

## Exact active source consumers and order

- `lbc/function/items/changed_inventory/sel.mcfunction`: equipped flags select
  reactive helmet, reactive feet excluding MK2, Techno head/feet and generic
  Techno tick. Four Techno flags with fewer than four MK2 flags activate ordinary
  set bonus; four MK2 disable it. Actual MK2 leaves also contain techno/reactive.
- `players_2t.mcfunction`: reactive helmet clears blindness, darkness, nausea
  every two ticks WITHOUT stopper gate. `armor/reactive_helmet.mcfunction`.
  Reactive boots every two ticks WITH stopper gate call `other/reactive_boots`.
- `other/reactive_boots.mcfunction`: clear levitation when coldownshish<=20;
  airborne sneak invokes reactive_boots2; ground recharge toward10, twice with
  reactive legs; reactive chest raises ceiling20 and legs double that rate.
  `other/reactive_boots2`: slow falling200ticks amplifier0, cloud, count four
  reactive slots, positive fuel invokes `other/reactiveboots` or `_mega`.
  These give levitation20ticks amplifier15 or30, source sound/flame, debit1.
- `players_tick.mcfunction`: stopper-gated `armor/techno_sel`: Techno helmet
  `armor/powerhelmettick` sends effect particles at all original mobs/nonplayer
  and other nonspectator players; MK2 feet run boots/tick.
- `players_5t`: Techno feet soul-fire particles, stopper gated.
- `players_1s`: MK2 head night vision300ticks amplifier0 WITHOUT stopper gate;
  Techno head stopper-gated `armor/powerhelmet`, which renders player direction
  particles through powerhelmet_particle (distance1.., positioned Y+3).
  This is LIVE, not a dead helper: both periodic head visual paths matter.
- `players_10t`: ordinary full Techno wearer in literal water, stopper gated,
  `armor/techno_set_bonus`: original #minecraft:mobs within10 also in literal
  water -> `techno_set_bonus1`:6 cactus damage credited to wearer, electric
  sparks/firework twinkle. Source aliases select at target position.
- `items/damaged.mcfunction`: all-four MK2 and shield score0, stopper gated ->
  `armor/set_bonus/technomk2/damaged`: deactivate sound, dust30, score100.
  Ordinary active Techno -> `lbcr:raycast_preloop10_setbonus`: three nearby
  electrical ray markers, lifetime15, shield_blockable/mecha/player, copied
  owner lbcID2 and team. Follow existing shared electrical ray service; do not
  replace this retaliation with just water shock. Generic lbcr ray continuation
  is a further mandatory closure before implementation.
- MK2 `boots/tick`: airborne sneak positive fuel starts flight unless flying;
  grounded nonflight regen; stopped_flight runs after_flight; full MK2 runs
  shield tick. Shield tick decrements positive score, activates sound when new
  value1, and at0 grants Resistance20ticks amplifier4 with dust.
- MK2 flight subtree `armor/technomk2/boots/flight/*`: begin resets subfuel,
  marks flight, clears stopped flag, schedule first tick2; then every tick run
  pitch -90..-75 up, -74..74 hover,75..90 down; subfuel>=40 debits1 then resets;
  ground or zero fuel stops. Up increments subfuel10, hover1, down0. Up without
  MK2 legs uses Levitation20ticks amplifier21; with MK2 legs derives gravity
  from pitch. Hover uses source gravity IDs and speedy score. MK2 chest changes
  particle origin. Rocket sound counter and all sound/particle helpers belong
  to closure. Ground regen increments subfuel1 or2 with reactive legs; each20
  adds fuel to10, or20 with reactive chest. Stop clears both owned gravity IDs,
  levitation/slow falling, adds stopped_flight, sounds; after_flight tests air
  below1..4 and grants slow falling20ticks amplifier2, cloud/sound.
- `advancement/remove_boots.json` calls technomk2/take_off -> flight/stop.
  `armor/technomk2/chestplate/tick.mcfunction` is empty, no invented ability.

## Acquisition and complete bounded table set

Steampunk leaves helmet/chestplate/leggings/boots/boots_spectral; wrappers
`neko_set_steampunk`, `_andboots`, `_pre`, `_full`. Techno four leaves;
`neko_set_techno`, `_pre`, `_full`. MK2 four leaves; `neko_set_technomk2`,
`_full`. Check `kit/steampunk` as an existing kit consumer, not authorization
to rewrite kit design. Keep exact attributes, models, equippable assets and
overlapping flags. Several leaves/wrappers already explicitly packaged in
build.gradle, but pre/full coverage needs checking; native acquisition must
exercise actual normal/Ultra tables, not fabricate matching test gear.

`other/craft/inventory_craft_check` lines25..28 accepts same-slot reactive
Steampunk asset plus Techno asset and calls cyberpunk_{slot}_inv. Matching
`other/craft/cyberpunk_{slot}` ground functions consume the pair. Use existing
native ResultSlot/ground crafting framework, consume one each with surplus
retained. The source count_reduce loops can consume multiple matching grid
slots; do not reproduce accidental overconsumption. `crafted_techno_armor`
spawns Techno full wrapper. Existing Hermes+Reactive flying-boots recipe is a
separate inherited acquisition consumer to audit before declaring closure.

Current rocket movement is `LbcItems` RocketState/rocketEquipment: recognizes
explosive_feets/Paper PDC and has its own glider/jump ledger. Do not conflate
it with reactivefuel. No native Wyvern/Nitro service identified by text search;
`nitro` occurs in reset objective list only. Verify actual movement owner before
adding another velocity/gravity owner.

## Decisions needed before implementation

1. Technology gates: should TECH suppress all new armor abilities, MAG none,
   preserving already granted effects? Original stopper explicitly gates most
   abilities but does not gate reactive helmet cleansing or MK2 night vision.
   A blanket gate would change those source exceptions. Existing electrical
   immunity is already implemented; clarify whether it remains unconditional.
2. Source defects: MK2 downward flight without MK2 legs adds gravity_fix2=0.1,
   then calls hover_1 unconditionally from lbc.math.tmp which was only updated
   in the MK2-legs branch. This can reuse stale cross-call storage and duplicate
   ID. Ask whether intended non-MK2 descent is fixed+0.1 while MK2 uses pitch.
3. Source stop indiscriminately clears other levitation/slow falling. Native
   implementation should own layers and remove its contributions only; preserve
   shared rod/other movement owners. Resolve whether source postflight slowfall
   should continue after unequip/jammer, rather than clearing unrelated effects.
4. Original water shock targets original mob tag indiscriminately; use current
   shared team/protected-role admission per AGENTS, not old friendly-fire bugs.
   Confirm hurt-retaliation admitted criterion semantics via shared hurt bridge.

## Static families, to avoid false runtime scope

Multiplying: four golden pieces, native armor AND toughness
ADD_MULTIPLIED_BASE amounts head2/chest4/legs3/feet1, exact unique source IDs.
No Java timer/bonus is implied. Default component replacement means their
contribution depends on actual attribute base and other equipment; do not turn
this into additive armor or a whole-set multiplier. `set_multiplying` chooses
ONE equal-weight slot, not four-piece full set. Normal transformables slot
tables and Ultra set wrapper already route it. Armor PvP advancement requires
all four raw multiplying flags; kit unlock/UI notifications already reference it.

Randchanted: four iron pieces use native enchant_with_levels uniform1..30,
not independent integer enchant rolls or custom enchanting engine. Root
set_randchanted chooses one slot. Original weapon/tool branches are embedded in
transformables_sword/pickaxe/axe/shovel/bow/hoe (verify actual hoe branch; TMT
name alone is not an item definition). Existing ported/random_enchanted tool
leaves also exist. Armor achievement requires four raw flags, parent Multiplying;
weapon achievements separately retain their source conditions. Ultra source
armor rule excludes sword/pickaxe/bow/axe ONLY; do not silently add shovel/hoe.

Current resources across these AND recently qualified Mage use entry modifier
arrays and child type fields. Never infer a schema defect by comparing old
1.21.4 functions/function. Native26.3 codec or actual owned-child loot roll must
settle it. No encoding rewrite is recommended from this audit.

Preserve approved source knockback budget5, imba provenance exceptions and
PunchVI import boundary; native integer enchant system remains authoritative.
These armor sources introduce no new knockback rebalancing exception.
