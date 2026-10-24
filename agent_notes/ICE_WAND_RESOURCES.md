# Ice Wand presentation and Academy source audit — 24 October 2026

Source audit and scoped resource preparation; no build or server changes.
Legacy root below is `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Author RP root is the read-only `all_in_one_1.21.8` recorded in RESOURCE_PACK.md.

## Exact item and resources

`loot_table/ice_wand.json`: carrot_on_a_stick, model `lbc:ice_wand`,
tooltip `lbc:tier_s`, unbreakable hidden. Name `ice_wand`, light_purple,
italic false. RU name «Вечная Мерзлота»; EN `Permafrost wand`.
Custom data `{transformable:1,staff:1,magicwand:1,ice_wand:1}`.
Seven lore rows in order: aqua type+staff; aqua literal `5 ` plus
projectile_damage; aqua literal `5 ` plus ice_crystal_damage; aqua score
ice_wand_mana_use + literal ` / ` + score ice_wand_shift_mana_use +
mana_on_activate; three white ice_wand.lore.1/.2/.3. Explicit italic false
on authored components must remain; literal slash is a bare string.
`function/config.mcfunction:94–95` sets the scores to400/200.
Source runtime uses6 projectile and8 crystal damage. User answer: KEEP authored
5/5 description exactly; do not rewrite the presentation to match implementation.

Existing RP assets require reuse, no additions: assets/lbc/items and
assets/lbc/models/item contain ice_wand, ice_crystal_proj, ice_crystal_small,
runic_ice_shard JSON; textures live under assets/minecraft/textures/item.
Runic shard texture is animated with existing PNG.mcmeta. Sounds
`minecraft:lbcsounds.ice_wand` and `minecraft:lbcsounds.ice_wand_shatter` are
registered in assets/minecraft/sounds.json and have original OGG files.
All text keys below already exist in BOTH assets/minecraft/lang/ru_ru.json
and en_us.json. These files are minified: parse JSON for selective inspection.

## Academy wiring and actual trial

`function/other/magic_academy/interacted_2.mcfunction:52` dispatches
can_upgrade/ice_wand; this leaf only prints yellow `upgrade_condition_ice`.
RU condition asks to freeze another player using this wand. Actual success:
workingitems/ice_wand/projectile/impact/damage.mcfunction:14 and
crystal/explode/damage.mcfunction:10 run common can_upgrade AFTER cold/check,
when target is player frozen>=1 and caster currently holds mainhand ice_wand
upgradable:1 without upgrade_available:1. Common can_upgrade_book marks
upgrade_available:1/upgradable:0 and sends existing aqua can_upgrade message.
Native implementation should follow existing actual-used-hand user rulings.

`interacted_upgrade.mcfunction:55` dispatches upgrade/ice_wand, which prints
yellow upgrade_succesful_ice and applies tiers/ss to mainhand. That modifier
sets only tooltip style lbc:tier_ss; generic Academy code owns upgrade flags
and common description. `advancement/true_advancements/legends/academy/ice_wand.json`
uses inventory custom data upgraded:1,transformable:1,staff:1,ice_wand:1.
Existing native Ice Gem trinket unlock references this advancement, but does
not imply a native Ice Wand registration or trial.

Existing upgrade_succesful_ice RU/EN promises player damage
24%% current health and other mobs40%%. Actual shatter/run uses
floor(Health)*0.4 for all targets. User answer: native shatter is WILD, deals24%
current health to players and40% to other mobs. Preserve literal %% and line
breaks in existing text. Crystal block cleanup restores the prior block only
if the cell is still that crystal's own barrier; normal spawn is in air (user).

Scoped native resources prepared: ported/ice_wand.json, academy_ice_wand.json,
runic_ice_shard.json, neutral_ball.json and skywars2 recipe ice_wand_combine.
Existing modern project source loot is copied with native item IDs; Academy
adds the existing green upgraded_item row, SS tooltip and completed upgrade
flags. Source score components remain intact, including neutral_ball5000
activation score; build/catalog integration belongs to the parent task.

## Complete acquisition boundary

Native LbcWands/LbcAcademy have no ice_wand registration at audit time.
Source craft: function/other/craft/ice_wand[_inv].mcfunction plus dispatch
craft_ground:36 and inventory_craft_check:36. Neutral staff + runic_ice_shard
produces BOTH ice_wand AND neutral_ball, grants staffs/neutral criterion ice.
Source inventory function checks only four crafting slots; native existing
user requirement is real exact2x2/3x3 recipes. Source ground branch kills
whole matching stacks: do not copy this defective consumption silently.
`loot_table/transformables_staff_all.json:78` references Ice Wand.

Runic shard source entry lives in loot_table/transformables_accessory.json
(model at567), with name runic_ice_shard and two original lore keys. No native
Java runic_ice_shard match was found. It is NOT merely a decorative donor:
players_10t:6–7 invokes workingitems/iceshard when carried/in hands, gated by
antimagic and native-style skill2 hand-only restriction. Every invocation adds
5cold for Slowness amplifier>=1; adds1cold while cold<10; cold_cd2; freeze/check.
When frozen grants accessories/jaakivi and Resistance V for25seconds. Both
container and weapon checks can invoke separately in the same pass.
This passive and its suspicious duplicate checks are a bounded dependency to
resolve before advertising donor acquisition as fully migrated.

## Remaining-family comparison

The awaken_tuntija_wands/avarice directory is Duplicating, already native and
documented in DUPLICATING_REWORK_PORT.md, not a new unported wand family.
Witch generators remain a substantial separate99-function sins system per
ACQUISITION_AUDIT.md; current defensive sin_greed_active compatibility does
not provide activation/acquisition. Do not bundle sins into Ice Wand work.
