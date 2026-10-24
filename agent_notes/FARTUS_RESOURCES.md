# Fartus Nadristailo presentation audit ? 24 October 2026

Source root (read only): `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Author pack (read only): the exact path in [RESOURCE_PACK.md](RESOURCE_PACK.md).

## Scoped leaves

`lbc:ported/fartus_wand` uses native `skywars2:fartus_wand`; `lbc:ported/academy_fartus_wand` uses `skywars2:academy_fartus_wand`. They preserve the complete source modifier payloads. Only the item identity and current loot API keys change (`functions` -> `modifier`, `function` -> `type`, omitted redundant weight1). The source count remains1.

Ordinary source: `loot_table/fartus_nadristailo.json`. Yellow nonitalic translate name `fartus_nadristailo`, model `lbc:fartus`, tier `lbc:tier_s`. Six lore rows retain exact nesting, order, styles and literal credit: aqua type/staff; aqua dynamic mana score +suffix; white lore1; yellow lore2; aqua lore3; green `Tinkist00` plus dark_gray ` Halloween 2025 Legendary Drop Request`. Source custom data remains `{transformable:1,staff:1,magicwand:1,fartus_nadristailo:1,swordwithability:1}`. Source has no enchantments or attributes on this ordinary item.

Academy synthesis follows `function/other/magic_academy/interacted_upgrade.mcfunction`, `item_modifier/add_upgrade_desc.json`, `function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction`, `item_modifier/tiers/x.json`, then `item_modifier/fartus_upgrade.json`. Retained name is the ordinary yellow name; tier is **X**, model `lbc:palma`. Final lore has six rows: aqua type/sword; white evolved1; aqua evolved2; aqua evolved3; yellow evolved4; exact original credit. The generic green upgraded_item row is replaced by fartus_upgrade replace_all, so it is correctly absent in the final item. Generic upgraded/mode flags are1, completed trial upgradable0/upgrade_available1 are retained. Transient trial score/prev are not fixed to a synthetic outcome in the ready leaf.

Exact initial mainhand modifiers: attack_damage `minecraft:base_attack_damage` +19 add_value; attack_speed `minecraft:base_attack_speed` -3.5 add_value; entity_interaction_range `a8uvy89ryvmg89dmybv8gdfrgg` +10 add_value. All source IDs, slots and operations are preserved by set_attributes replace=true. No enchantment is added. `item_modifier/kakashki_fartus_damage_buff.json` replaces these same attributes using storage damagebuff at runtime; that is behavior, not an initial resource value.

## Recipe and trial provenance

`function/other/craft/inventory_craft_check.mcfunction` and `craft_ground.mcfunction` match `stterra:1` plus `wtf_item:1`; donor tables are `loot_table/sauvojen_tuntija_terra.json` (Terra wand) and `loot_table/wtf_item.json` (banana label). Result is original `lbc:fartus_nadristailo`, not a random broader pool. `function/other/craft/fartus_nadristailo_inv.mcfunction` reduces matching donor stacks by1 in the four player crafting slots. Ground counterpart removes its consumed entities.

Trial: `function/workingitems/firtusnadristailo/cast_2.mcfunction` rolls1..2 once per cast and invokes `check_upgrade` only for upgradable mainhand. `check_upgrade.mcfunction` and `check_upgrade_set.mcfunction` recognize AABB or BBAA, using custom_data score/prev, then `other/magic_academy/can_upgrade` gives readiness. Existing `upgrade_condition_fartus_nadristailo` and `upgrade_succesful_fartus_nadristailo` supply the source explanations. The native implementation should follow the actual initiating hand/stack contract rather than copying the old SelectedItem-only limitation.

## Author assets verified

All item wrappers exist in assets/lbc/items and point to matching assets/lbc/models/item JSON: fartus -> fartus; palma -> palma; palm_small -> palma1; banana -> banana; banan -> banan. palma and palm_small specify oversized_in_gui=true. No animated range/select subframes are used by those wrappers. Palma and palma1 are distinct 32-element meshes using the same original256x256 minecraft:item/palma texture; the main sword must retain palma, not palm_small. Fartus uses handheld_rod parent and original16x16 minecraft:item/fartus texture.

Ordinary banana and Academy homing source projectiles use `lbc:banana` (9-element mesh; original32x32 minecraft:item/banana texture). Label source `cast_etiketka_shoot.mcfunction` uses `lbc:banan` (generated flat original128x128 minecraft:item/banan texture), fixed display, scale0.5, source left_rotation [1,0,0,1]. Banana wrapper/model retains its fixed [0,-76.5,0] display rotation. No texture synthesis, substitutions or RP edits are needed. These are static file/geometry checks; no graphical client pixel claim.

Existing author language files assets/minecraft/lang/{ru_ru,en_us,rpr}.json contain all ordinary/evolved name/lore keys, type/staff/sword/mana suffix and both Academy explanation keys. Keep the original keys and values; no language/template duplication is needed. English name is `Fartus Nadristailo`; original author language files remain unchanged.

No source server, author RP, template, generators, shared registry or acquisition pools were edited. Native item registration and runtime decoding/tests remain the main implementation task; no builds or commits were performed in this resource subtask.


## Banana-label scoped donor leaf ? material ability audit

Added only `lbc:ported/wtf_item`, native ID `skywars2:wtf_item`, from exact `loot_table/wtf_item.json`. Structural verification compared every modifier payload to source after only functions/function API-key conversion; all match. It retains tierB, `lbc:banan`, aqua nonitalic name `wtf_item`, three exact lore rows (white `wtf_item.lore`; green Tinkist00 +dark_gray literal ` No Nut November 2023 Event`; yellow `razorpine.lore.1`) and `{wtf_item:1,craftusable:1}`. No enchantment, attribute or count override exists in source; keep default stack behavior. Author ru_ru/en_us/rpr contain all three translated presentation keys. Repeating-command-block proxy must be nonplaceable. The existing banan wrapper/generated model and128x128 original texture were verified above.

**Original audit finding, now resolved by the qualified native Chef rework:** this is not an inert material. Actual source ability exists:

- `function/items/swords.mcfunction:56` dispatches mainhand custom_data wtf_item1 on sword-hit processing.
- `function/items/swords/wtf_item/hit.mcfunction` clears one matching label from inventory, stores caster team in *tempt lbcID2, selects a non-spectator other-team entity with HurtTime10 within0.01..7, and calls spawn.
- `function/items/swords/wtf_item/spawn.mcfunction` creates fixed `lbc:shef` display10 blocks behind that target, scale3, visible translated shef name, rare+slowraycast tags, and tags target shef_hated.
- `function/raycastselector_rare.mcfunction:26` dispatches `function/items/swords/wtf_item/tick.mcfunction`: chance0_5 random sound; disappears when no global shef_hated remains; faces a hated entity and moves0.1; at distance<=5 deals10 cactus through shef display. The source global selectors/absent owned lifecycle are implementation concerns, not permission to silently call the item inert.
- `function/items/swords/wtf_item/random_sound.mcfunction` chooses six author sounds lbcsounds.oluh_1..6, volume2 pitch1; sibling random_sound_low uses volume1 and targets self and is also used by Fartus label-hit effects.
- Acquisition also exists in `function/other/eatables/banana.mcfunction:7` through chance15 -> lbc:wtf_item. Broad acquisition is outside this scoped leaf.

The bounded donor leaf supplies exact presentation for native craft registration, but full ready-catalog qualification must await implementation or an explicit author ruling about the attack ability. No broader pool was enabled.

Terra recipe source predicates check only stterra1 and wtf_item1, with no upgraded exclusion: Academy Terra is accepted and output is ordinary Fartus. Native exact2x2/3x3 craft is the root implementation scope.

Academy activation binding: author `fartus_nadristailo_evolved.lore.3` says Right Click/PKM. `function/items/spells.mcfunction:7,13` -> spell1/spell1_left -> `workingitems/spell1_upgraded.mcfunction:41` / `spell1_upgraded_left.mcfunction:40` -> cast_upgraded when charge>=3. Thus source uses shared spell/right-click cast input; F would conflict with source presentation. Preserve the original lore.


## Chef model and sound assets ? static verification

Author `assets/lbc/items/shef.json` maps directly to `lbc:item/shef`; model `assets/lbc/models/item/shef.json` has minecraft:item/generated parent and layer0 minecraft:item/shef. Original `assets/minecraft/textures/item/shef.png` exists at448x601. No animated select/range subframes, no replacements or generated bitmap assets are needed. Source display explicitly uses fixed transform and scale3; source model has no custom display overrides. Translated shef key exists in author ru_ru/en_us/rpr. Exact entity target/lifecycle policy remains root's pending author ruling; resource verification does not claim behavioral qualification.

Verified current author `assets/minecraft/sounds.json` mappings and every referenced OGG file for minecraft:lbcsounds.oluh_1 through oluh_6, minecraft:lbcsounds.bananoponos and minecraft:lbcsounds.icum. Each maps to matching `assets/minecraft/sounds/lbcsounds/<name>.ogg`, with stream=false. All eight custom event resources exist. The six oluh events have category master, as does icum; bananoponos omits the category field. Source references: `items/swords/wtf_item/random_sound[ _low].mcfunction` (six oluh variants), `workingitems/firtusnadristailo/banan_impact_success.mcfunction:7` (bananoponos, master volume1 pitch1), `other/craft/fartus_nadristailo[_inv].mcfunction:3` (icum, master volume1 pitch1).

Other distinct vanilla source sounds: `workingitems/firtusnadristailo/cast_2.mcfunction:5` entity.snowball.throw (master1/1); `cast_etiketka_shoot.mcfunction:6` ui.cartography_table.take_result (master0.5/2); `piss_particle.mcfunction:3` block.grass.step (block0.5/2). These are vanilla sound events and require no new author assets. Thus the audited union contains eight custom events plus three vanilla events; six Chef oluh variants plus the three direct Fartus throw/label/impact sounds account for nine ability events when craft/particle ambiance is excluded. Static presence/mapping checks do not claim listening or graphical-client validation.


## Integrated runtime qualification (24 October 2026)

Build `b0dfd5ca` passed74 recorded suite results in two actual mode1 matches (168.90s), including Fartus/Chef and the existing shared regressions. Exact 311 sources/857 classes/2797 resources match the tested installed JAR. Main installed while stopped, rollback `0fb48f1a` retained, server not started. Evidence: `build/verification/fartus-final/`. Native synchronous tick boundaries are explicit simulations; no elapsed ten-second wait, real client pixels, actual disk restart/disconnect or general-world/load guarantee is claimed. This integrated result supersedes the earlier pending-status statements by the individual source/check agents.

The old display-model audit is historical: the author explicitly replaced it with the real native Mannequin described in CHEF_REWORK.md. All three scoped leaves are ready; the native rework qualifies the Label attack ability. Broad acquisition remains separate.
