# Canned Food (konserva) - complete finite source preparation 2026-10-27

New owned note only. Legacy/native/RP static read-only audit; no implementation, build, test, server, git or RP changes. Source paths below relative to `world/datapacksdisabled/LBC/data/lbc`. Source text is not observed historical runtime; native signed effects/item caps, roles, Time Stop, owner lifecycle and passive-under-stun policy remain authoritative.

## Complete independent next family

Current Canned Food has a complete finite11-outcome inventory consumer and an original lucky-items leaf. Its normal LuckyWars acquisition is not live in the current mod; see the [packaging and routing audit](CANNED_FOOD_ACQUISITION_2026_10_27.md). It has no Tin Can output or helmet dependency in this source version. Whole konserva flag/name search finds only the current loot leaf, two periodic probes, helper and metadata index. No old flag/old leaf, recipe, advancement, hidden material output, armor slot mutation, helmet passive or special task was found. Do not add invented Tin Can items or enable a whole unresolved pool.

Native production searches for konserva/canned/tin aliases found the exact existing vanilla loot resource, but no matching gameplay input/consumer. Shared native KFC/Colors/Traveller/Classic Melee services implement other food effects/counters, not this11-way producer. This is a real missing entrypoint with reusable effects/food_count state; no Belt/Quiver answers are needed.

## Exact periodic admission and source order

`tick2s:1-2 -> tick2smobs:6 -> players_2s:2-3`: every40 scheduled game ticks, ordinary nonspectator player outside nexus/imprinted. Two **independent** lines each `execute at @s[scores={foodlvl=..19}]`: first container.* konserva1, second weapon.* konserva1. Selected mainhand belongs to both, offhand to weapon only. A backpack copy plus offhand copy also admits both. Do not deduplicate or trigger on cursor/armor alone. Source foodlvl is a stored FOOD criterion (`load:22`), updated by native player ticks rather than lazily on read. The port rechecks live physical FoodData before each probe; its existing foodlvl DUMMY compatibility ledger stays separate. See the [native timing audit](CANNED_FOOD_TIMING_2026_10_27.md) for the explicit historical stale-score distinction and same-amplifier instant-effect merging.

No legacy, skill2, #lbcskill, stopper, stopper_magic, beacon, nomagic_active or mana gate occurs in the probes/helper. No RMB, private cooldown or equipment action. Native Time Stop remains common authoritative pause; this passive must not accidentally inherit active-use stun rejection. No target area/ally damage, terrain or custom projectile is involved.

`other/konserva:1-23` exact action sequence:

1. Grant Saturation duration1 amp100, show_particles=true (literal false hideParticles argument).
2. Seven independent item particles, cooked_beef/cooked_porkchop/cooked_rabbit/cooked_salmon/cooked_mutton/potato/baked_potato, in that order; each `anchored eyes positioned ^ ^ ^0.3`, spread.1/.1/.1, speed0 count2 NORMAL. Total14 particles. These are visuals, not material item outputs.
3. Burp `minecraft:entity.player.burp` MASTER @a, captured ownerXYZ, volume1 pitch1, minVolume default0. Native command sound range/dimension audience, not broadcast to every client. Particle NORMAL uses vanilla near audience.
4. Uniform random integer1..11, apply the matching exact effect below.
5. Clear ONE matching konserva from full `/clear` command scope; then food_count +=1 unconditionally. No return/failure guard surrounds those last commands. Clear can consume earlier armor/craft/cursor match when triggering stack is in later slots; preserve exact command scope/order rather than removing only the trigger reference.

| Draw | Exact effect | Native duration / amplifier / visible |
|---|---|---|
|1|absorption60s amp4|1200t /4 /yes|
|2|fire_resistance60s amp0|1200t /0 /yes|
|3|haste30s amp1|600t /1 /yes|
|4|instant_health1 amp1|1 instantaneous tick /1 /yes|
|5|invisibility30s amp1 true|600t /1 /NO particles|
|6|luck10s defaultamp0|200t /0 /yes|
|7|night_vision120s amp0|2400t /0 /yes|
|8|regeneration10s amp1|200t /1 /yes|
|9|resistance20s amp1|400t /1 /yes|
|10|strength10s defaultamp0|200t /0 /yes|
|11|water_breathing30s defaultamp0|600t /0 /yes|

Native26.3 `EffectCommands.computeDurationInTicks:170-178`: instantaneous effects take the provided number directly, not seconds*20. Thus initial Saturation is **1 tick**, not20; outcome4 also1 tick. `SaturationMobEffect:13-15` feeds amplification+1 with saturation modifier1.0 through native FoodData caps. Use exact native effect/layer helpers and existing signed/cap semantics; do not implement a20-tick amp100 pulse or direct ad-hoc food refill. Saturation/effect update timing matters for second probe and is testable against real native effect controls; source script alone is not runtime proof of when FOOD scoreboard refresh occurs.

## Shared counter, reset and side effects

`load:139` food_count dummy; helper:23 increments once per admitted invocation, not per stack unit or actual accepted buff. Existing shared consumers include witch_gens/gluttony threshold64 (rank comparison, tellraw); Canned Food does not implement or grant that whole skill itself. `resets:74` and witch_gens/reset:2 reset counter, existing Vanity snapshots/restores it. No new can-specific timer/HUD/persistent layer needs serialization. Native `LbcColors:235-236`, `LbcClassicMelee:45`, `LbcTraveller:54` already use real food_count state; `LbcMana:184` resets it. Reuse that counter rather than new item-private count. No source food_count cap; normal scoreboard integer semantics remain distinct from native effect/item caps.

Effect owner is executor/self. No invented killer, damage source, creator UUID or external task attribution. Effects remain normal native living effects after the can is consumed; item removal/held change does not cancel them. Owner death/match reset follows shared effect lifecycle. No scheduled particles or aura persists.

## Exact normal-play leaf and presentation

`loot_table/lucky_items.json:1182-1235`, pool0 entry74 (zero based): minecraft:repeating_command_block, weight10 quality7, **count16**; components item_model lbc:konserva and tooltip_style lbc:tier_d; custom_data `{konserva:1}` only. Name konserva light_purple italicfalse; lore konserva.lore.1/.2 in order, light_purple italicfalse. No accessory/transformable/food/instantitem/magicwand/equippable tag, no edible component. Keep original vanilla identity and model; do not register a replacement edible carrier.

Exact same leaf is present in raw native `src/main/resources/data/lbc/loot_table/lucky_items.json:1182-1235`, but that whole table is excluded from the packaged mod. Source ordinary luckyblock:9, luckyblock_without_rec:9 and luckyblock_64:8 reference lbc:lucky_items; lucky_items_1 references the same pool. Those roots and the LuckyWars mode are not live yet. A source-exact scoped `lbc:ported/canned_food` leaf stages the full consumer for operators and native qualification only; it does not restore ordinary acquisition. Do not enable the unresolved102-entry pool or claim the whole item complete until its normal route closes. The [acquisition audit](CANNED_FOOD_ACQUISITION_2026_10_27.md) supersedes the earlier resource-presence inference.

Author RP English strings:
- `konserva`: Canned food
- `konserva.lore.1`: Automatically used when hungry
- `konserva.lore.2`: Gives a random buff.

Existing author model/style/language are reused. There is no can-to-helmet presentation promise in these strings or helper; if another source version had one, it is not this audited contract. Physical pixels/audio remain unobserved.

## Implementation/qualification boundary

Suggested integration: a cheap inventory flag snapshot marks container and hand scopes; shared40tick phase performs source-ordered hunger/probe checks, calls one complete helper each admission, uses original random11 mapping and native effects, emits all14 original item particles and ranged burp, clears1 via exact command-equivalent scope, increments shared food_count. Preserve two probe phases and self owner; no private debounce, mana debit, consumption-on-click or arbitrary stack/effect cap. Qualify real instantaneous timing/FOOD criterion phase and two-probe cases with independent native controls. No unresolved author choice is needed to add material outputs or Tin Can helmets, because source has none.

## SHA-256 ledger

```text
function/other/konserva.mcfunction bd9ecdd5d2428c6ec98bac836c7ec3c289fcb42530fd08046920109e8829324b
function/players_2s.mcfunction abdb09f5ca07c52fa5adf335311d011d8ef3f643242d17d41905db521d9a1a5e
function/tick2s.mcfunction 011f1b6e3c4e250d94f368ca5e4377af41971bc4142c56dffe65576def91fb70
function/tick2smobs.mcfunction 4585f01a50f0dc194b3e6ccfbeb7ed90a1a33eb6c0117ef7e2eefb1a7c2a095c
function/load.mcfunction b2b4c8d0f4fd6d8a9464262368188d4a26d71bf29f7f931b423658c7a763f6e1
function/resets.mcfunction c10ba440615050e16d2fcb55d6a8032c289ba0cc462da3c29c1c9de59f65d9cc
loot_table/lucky_items.json a9b73cc64f482231beb27f0ae07328180bdcb358fa6cb9cb0867369d792bbcb2
loot_table/luckyblock.json cf05aa79ddf00e034588ec942243e84ae804168e5f58039cde3f0f73fb07dc6a
loot_table/luckyblock_without_rec.json 30abce67d5c6c08decea78f392ad3d7efaa6721db72a0bff13b8a402f783007c
loot_table/luckyblock_64.json ab473d451962ad95732b3aa0e6c4545ab18ecbec3a01cf4e0a3520cc141d5e8c
src/main/resources/data/lbc/loot_table/lucky_items.json a9b73cc64f482231beb27f0ae07328180bdcb358fa6cb9cb0867369d792bbcb2
src/main/java/me/skywars2/game/LbcColors.java 102249dcaf82eaee7bfcdc11d18ab85bafcb6b7e925c65233e02ca876562127b
src/main/java/me/skywars2/game/LbcMana.java 00ee030048c873b588a2c77c8932aea72ddd10bc434090b7f073c6d4f2944692
RP/assets/minecraft/lang/en_us.json 7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7
RP/assets/minecraft/lang/ru_ru.json 9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc
RP/assets/lbc/items/konserva.json 647e8971f0824fa48dc4ab6d59b8ab87b9757ad922bca7f4b90ad1db7396a0b8
RP/assets/lbc/models/item/konserva.json e1e6c97fbcf9c9e69058af0cd89f4e65ab2ac62846ca00e5cac0829581ec41ab
```

Frozen source preparation only; no implementation or native qualification claimed.
