# Ruby continuations: source audit (2026-10-20)

Read-only source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Below `lbc/...` and `lbcr/...` are exact paths relative to that root. Static audit only; no build/server run. Current batch: Consuming + both `cons_material` donors. Duplicating is next-batch research.

## Contract and reuse

(user) Real shapeless 2×2/3×3 craft consumes exactly one Ruby and one donor. Preserve source design keys. Owner attribution is mandatory for direct hits, partitions, summons and terrain consequences. No creative/developer bypasses for wands.

Reuse `LbcCraftingRecipe` for native preview/take/shift-click; `LbcWands` for registered items/input/mana/target filtering; `LbcRuby` for finite native ray jobs/lifetime and shared terrain helpers; `LbcStonekivi.queuedWork` for bounded accepted terrain work. Do not create another input, mana, crafting or lifecycle engine. Academy remains `LbcAcademy`.

## Recipes and independent donors

`lbc/function/other/craft/inventory_craft_check.mcfunction:7` and `other/craft_ground.mcfunction:7` accept `struby:1 + cons_material:1`. Result is `lbc/loot_table/tuntijaa_greed_destroy.json`: `consuming_wand`, model `lbc:consuming_wand`, tier_s, staff/raycastspell, lore keys `consuming_wand.lore`, `magic_damage`, `mana_on_activate`. Lore says 5 damage; actual immediate ray does 2 per sample. Do not silently choose lore over code.

`lbc/loot_table/transformables_spells.json` has TWO cons_material entries (lines213,280), each carrot_on_a_stick, max stack64, source count1, spellitem/raycastspell/spell/transformable/craftusable:

- `creeperz:1`: name `creeper_army_spell`, model/source lore in its entry. `lbc/function/workingitems/raycast_spells/select.mcfunction:5` calls `unstable_spellbook/raycast_loop8.mcfunction`: step1, up to201 samples, stops at block or mob and invokes `raycast_spells/creepers.mcfunction`. That summons lifetime6000 `unstablespell`/`rc_5t` marker, consumes one creeperz and sets Distance300. Despite its name, this is a destructive curved-ray area, not a live creeper army: `lbc/function/raycasters_5t.mcfunction` → `other/unstablearea.mcfunction` → `other/unstablearea_rc.mcfunction`; each5ticks launches150 half-block curved samples, erasing with fill ±0.51. No direct damage command; terrain consequences need owner.
- `trap2:1`: name `destroyer_spell`, model `lbc:breaker`, lore `destroyer_spell.lore*`. `raycast_spells/select.mcfunction:19,21` calls `unstable_spellbook/raycast_loop.mcfunction` then consumes one trap2. 201 step1 samples; air fill grows from ±0.5 through ±1,1.5,2,3,4,5 at Distance thresholds5/10/20/40/80/100/200. Nomagic radius20 and stopper radius20 stop continuation. No direct entity damage.

`lbc/function/other/sauvojen_tuntija/crafted_greed{,_inv}.mcfunction` awards result; legacy ground deletes whole item entities and inventory scans all4slots. These mechanics do not override the user 1+1 native craft requirement.

Other branch: `struby + greed_greed` → `lbc/loot_table/tuntijaa_greed_avarice.json`; `other/sauvojen_tuntija/crafted_avarice{,_inv}.mcfunction`. `greed_greed` is independent `lbc/loot_table/greed_greed.json`: Divider, model `lbc:divider`, tier_s, magicwand/spellwithcooldown, lore divider.lore/.1 + craft_with_metal_pipe. `workingitems/divider.mcfunction` clones centered25³ into26 adjacent cubes, sets divider_cd30, costs no mana. Source main/offhand input is `workingitems/spell1_default{,_left}.mcfunction`.

## Consuming normal

`lbc/function/config.mcfunction:79`: consuming_wand_mana_use=2000. Main/offhand dispatch: `lbcr/function/raycast{,_left}.mcfunction:24`; upgraded equivalents line13. Normal `lbc/function/workingitems/awaken_tuntija_wands/consuming_wand.mcfunction` branches to trial on sneak+upgradable (checks SelectedItem even for offhand), otherwise loop; subtracts2000 and invokes common mage/trinket cast hooks.

All following files are in `lbc/function/workingitems/awaken_tuntija_wands/`:

- `consuming_wand_loop.mcfunction`: increment Distance; erase3³ via strict fill air including unbreakables; 2 arrow damage per sample to up to2 overlapping mobs excluding spectator/raycaster, attributed to caster. Step1 while Distance<=50 =51 samples from Distance0; recursion gated by nomagic16/stopper20. It traverses walls.
- `consuming_wand_loop_randomcloud.mcfunction`: each sample farther7fromcaster creates a marker, NOT chance-based. lifetime100, slowraycast/consumption_part/rare/magic.
- `consuming_wand_loop_randomcloud_get.mcfunction`: random yaw AND pitch -180..180; copies caster ID/team, active tag.
- `consumption_part.mcfunction`: inert through Lifetime59; Lifetime>=60 invokes `consumption_part_da.mcfunction` on every tick. Selector increments Lifetime first, dispatches then expires at100 (`lbc/function/raycastselector.mcfunction`), so source can execute active ages60..100 inclusive.
- `consumption_part_da.mcfunction`: erase3³, particles; invokes `lbcr:function/damagewithid10` (actual path `lbcr/function/damagewithid10.mcfunction`) if overlapping mob. Helper hits up to3 for2arrow, excludes ID-matched owner and team. Move0.4 toward an arbitrary eligible mob within8 (no nearest sort); otherwise preserve random heading. Trigger uses distance15 + dx/dy/dz0; helper distance10 + dx/dy/dz0. No terrain collision stop.

## Consuming Academy

`lbc/function/other/magic_academy/can_upgrade/consuming_wand.mcfunction` uses `upgrade_condition_consuming`; upgrade file uses `upgrade_succesful_consuming` and tiers/x. `consuming_wand_upgrade.mcfunction` places meditation at(-23,-16,-23), summons28 invisible hostile skeleton void men (40HP,10attack, void armor, voidman loot), applies darkness/blindness20s and immediately calls common can_upgrade. No survival condition in this function. Attribution of summoned consequences required.

Upgraded `consuming_wand_upgraded{,_left}.mcfunction`: sneak cycles4modes and tells source keys; no debit on cycle but outer2000 gate remains. Non-sneak uses `consuming_wand_upgraded_pre.mcfunction`, reads correct hand mode, resets Distance0, dispatches and debits2000 for EVERY mode with same mage/trinket hooks.

| Mode | Exact file suffix after consuming_wand_upgraded_ | Behavior |
|---|---|---|
| Default | mode_default.mcfunction | normal loop |
| Silence | mode_silence.mcfunction | levitation3s amp30, slowfall60s; stopsound ALL players; eight silence templates; kill nonplayers within48 except strict_map_object/god_hedgehog; god hedgehog can retaliate10billion damage; other nonspectator players within48 take20 outside_border unless void_resistance |
| The End | mode_the_end_pre.mcfunction / mode_the_end.mcfunction | starts8ahead; five rays yaw0,+20,-20,-10,+10; each201samples step2; air11x11 template per sample;6arrow to up to2 within5 excluding caster/spectator; independent chance5 nether/end portal placements4behind; nomagic16/stopper20 gates |
| Avarice | mode_avarice.mcfunction / mode_avarice_end.mcfunction | step0.7 up to201samples, first mob/block terminates; block impact backs0.7. Teleports ALL nonplayers in dimension except core_marker/strict_map_object to impact, then20generic to mobs within3 except caster/strict. No radius on teleport. |

Source template NBT sizes/record counts: `lbc/structure/meditation.nbt`47³/54435; air11x11.nbt11³/1331; silence_1..8.nbt each48³, records58774/60580/60580/62420/57004/58774/58774/60580. Silence placements tile96³ around cast (-48..47), with sparse source records preserved.

Author decisions, 20 October 2026 (user): Silence preserves broad ordinary nonplayer deletion. Replace the eight template pieces with direct erasure of every block in a sphere of radius 48. Avarice preserves dimension-wide ordinary nonplayer teleport, including items, projectiles and allied mobs. Technical NPC/observer protections remain mandatory. Source lacks owner on Silence/Avarice damage; this is a migration gap, not desired behavior. The old template sizes above describe the source, not the new sphere implementation.

## Duplicating next-batch findings

Cost `lbc/function/config.mcfunction:80`=1000 both normal/all Academy modes. Result model/name `lbc:dublicating_wand`/`dublicating_wand`, tier_a; spelling intentional. Exact implementation folder `lbc/function/workingitems/awaken_tuntija_wands/avarice/`.

`click2.mcfunction` creates lifetime200 moving duplicate marker7ahead, copies owner/team. `tick.mcfunction` moves1/tick and creates lifetime3 random clone seeds; `rand_rot.mcfunction` picks random rotation and ±4pos; `rand_rot2.mcfunction` performs line/cube clones and clears3³. Trial `click2_upgrade*.mcfunction` creates90 lifetime30 duplicate markers, randomizes and immediately marks ready. Academy keys upgrade_condition_dublicating/upgrade_succesful_dublicating, tiers/ss.

Modes in `click2_upgraded.mcfunction`: default; expand (marker14ahead lifetime100, speed2, larger clones); divide (step0.4 up to251, impact copies9³ into26neighbors); black_hole (marker8ahead lifetime300, speed0.3, r7obsidian template integrity0.65, pulls entities0.35,0.01 outside_border to up to5 mobs within4 excluding owner/team/voidresistance). Expand `rand_rot2_2.mcfunction` explicitly clones Nether/End/nexus blocks INTO overworld and force-loads ±20 source regions through rand_from_dim helpers. Needs author decision before port; current batch excludes this implementation.
