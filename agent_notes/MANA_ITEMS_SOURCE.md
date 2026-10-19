# Mana stages, potions, and Overload Crux: source audit

Scope: read-only audit of `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC`. Findings below describe source behavior; they do not establish that this disabled datapack is enabled or currently runs on any server.

## Tick path and reverse mana

`lbc:players_tick` calls `lbc:other/manaregen` once per player tick when legacy mode is off, `nomagic` is absent, and no nearby `stopper_magic` marker blocks the path (`data/lbc/function/players_tick.mcfunction`, line 33). Thus ordinary mana regeneration and negative-mana stages are evaluated per game tick, while the `reverse_mana` duration itself is decremented once per second in `players_1s` (line 21). In `manaregen`, the order is:

1. If `mana <= 0` and `obtained_crux` is absent, spawn a Crux and add `obtained_crux`.
2. If the player has `obtained_crux`, remove that tag when `mana >= manamax`, but only when `manamax > 0`.
3. Recompute `manaregen_visual` from `manaregen`, then apply belt/mage-armor/boost modifiers.
4. If `reverse_mana >= 1`, subtract the resulting regeneration amount from mana.
5. If mana is below max and regen is positive, call the positive regen routine; negative regen also calls that routine. Finally, if mana is negative, evaluate negative-mana penalties.

Sources: `data/lbc/function/other/manaregen.mcfunction` lines 1–16; caller `data/lbc/function/players_tick.mcfunction` line 33; duration decrement `data/lbc/function/players_1s.mcfunction` line 21.

The displayed “Reverse Mana” potion advancement is an `impossible` criterion entry, not a consumable item implementation. It is granted every five seconds while `reverse_mana >= 1` (`players_5s.mcfunction` line 5), has a parent in the potions advancement tree, and is revoked to zero by `other/purify.mcfunction` line 44. Its actual effect is the mana-regeneration inversion in `manaregen`: while duration is positive, the current regen amount is subtracted each tick; it does not directly set a vanilla potion effect. Other code can add duration (e.g. `mode_chuma_end` adds 30; `players_1s` consumes one), so 30 is 30 seconds. Sources: `data/lbc/advancement/true_advancements/potions/effect_reverse_mana.json`; `data/lbc/function/players_5s.mcfunction:5`; `data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/mode_chuma_end.mcfunction:1`; `data/lbc/function/other/purify.mcfunction:44`.

## Negative-mana stage effects

The dispatcher `other/negative_mana.mcfunction` is executed at the end of `manaregen` only for `mana <= -1`. For players without the Overload helmet tag, the bands are mutually exclusive:

| Mana | Effects, amplifier values are zero-based |
|---|---|
| -1…-5,000 | Slowness II (amplifier 1), refreshed for 1 second. |
| -5,001…-10,000 | Slowness III (amp 2), Mining Fatigue I, Weakness I; each 1 second. |
| -10,001…-15,000 | Slowness VII (amp 6), Mining Fatigue III (amp 2), Nausea 4 seconds, Weakness II (amp 1), Hunger I; durations otherwise 1 second. |
| -15,001…-20,000 | Slowness, Mining Fatigue, and Weakness amp 127 (all 1 second); Nausea 6 seconds; Hunger III (amp 2, 1 second). |
| <= -20,001 | Same as prior band, plus Blindness 2 seconds and Hunger VII (amp 6, 1 second); also computes a damage check and may fire the tentacle explosion. |

Sources: `data/lbc/function/other/negative_mana.mcfunction` lines 1–9; `negative_mana_1.mcfunction` through `negative_mana_5.mcfunction`.

Armor branches are additional overlays, not replacements for all stage effects. If `overload_helmet_active`, stages 1–4 are suppressed entirely; at `mana <= -20,001`, a helmet-specific stage-5 damage/explosion formula runs. If `overload_chestplate_active`, Resistance is refreshed for 1 second at amp 0/1/2/3/4 across the same five mana bands. The stage dispatcher then separately grants the `oh_no_mana` advancement only when mana, `manaregen_visual`, and `manamax` are all <= -1. Sources: `negative_mana.mcfunction` lines 1–9; `negative_mana_resist.mcfunction` lines 1–5; `negative_mana_5_helmet.mcfunction`.

## Stage-5 damage, floor/zero-max edge cases

For the non-helmet stage 5, the source reads max health into integer score `tmp`, health into `hplvl`, computes `tmp1 = mana / manamax` with scoreboard integer division, subtracts that quotient from max health, multiplies the result by `0.1` via storage conversion, then compares the resulting integer score against current health. If `tmp >= hplvl`, it invokes `explode`; otherwise it passes that damage amount to the tentacle ray routine. Helmet stage 5 uses the same integer quotient but the scale is `0.045`, and compares to current health. The actual formulas are therefore coarse integer arithmetic before scaling. Minecraft 26.3 `OperationArgument` implements scoreboard `/=` by checking for divisor zero (throws divide-by-zero command error) and then calling `Mth.floorDiv(a,b)`, which delegates to `Math.floorDiv`; it is floor division, not truncation toward zero. For example, `-1 / 10000` becomes `-1`. Keep this exact arithmetic in any native port. Source snapshot: `agent_notes/inventory/minecraft_operation_26_3_bytecode.txt` around lines 260–267 and `minecraft_mth_26_3_bytecode.txt` around lines 275–279.

Both stage-5 functions divide by `manamax` without a local positive/nonzero guard. This is reachable if the player has negative mana with `manamax == 0`; on Minecraft 26.3, scoreboard `/=` raises a divide-by-zero command error and does not perform the quotient. The dispatcher does not gate stage 5 by positive max mana. The Crux-rearm test does gate on `manamax > 0`, but that does not protect the stage-5 division. Treat this as a real edge case in a native port. Damage threshold is `computed damage >= current health`, not `>`; equality explodes/kills. Sources: `negative_mana_5.mcfunction` lines 8–15; `negative_mana_5_helmet.mcfunction` lines 2–8; `negative_mana/explode.mcfunction`; `damage_with_tmp_void.mcfunction` at the datapack root (two branches select regular vs full-set tentacles).

## Crux acquisition, use, cleanup, and re-arm

Crux loot item is a carrot-on-a-stick with model `lbc:overload_crux`, max stack size 99, and custom data `{craftusable:1,magicwand:1,overload_crux:1}` (`data/lbc/loot_table/overload_crux.json`). Config sets its cost to 7,500 mana (`data/lbc/function/config.mcfunction:110`). Automatic acquisition is `mana <= 0 && !obtained_crux`; the grant spawns the item and adds `obtained_crux` (`other/manaregen.mcfunction:1`; `other/overload_crux/give.mcfunction:1–4`). The tag is re-armed only at `mana >= manamax` with positive `manamax` (`manaregen.mcfunction:2`). Therefore a player at exactly zero gets the Crux; a player who cannot reach a positive max-mana score never re-arms through this path.

Use is real and wired: while the Crux is the selected item, `spell1_default.mcfunction` calls `other/overload_crux/use` unless a nearby magic stopper exists. It has no `mana >= cost` guard, so it can push mana below zero. Use subtracts 7,500, plays two sounds, casts the full-set tentacle attack five times, then conditionally replaces each equipped diamond armor piece with the corresponding Overload loot item (head/chest/legs/feet checked against the matching diamond item). It consumes one Crux from inventory at the end. Sources: `data/lbc/function/workingitems/spell1_default.mcfunction` final line; `other/overload_crux/use.mcfunction` lines 1–18; `config.mcfunction:110`.

Activation does not add the `overload_helmet_active` / chestplate tags itself in this function; those are determined by the normal armor equip/item-state machinery elsewhere. Do not infer that equipping an output loot item necessarily activates a tag without auditing that armor registration path.

## Craft path: source definition vs reachable path

The Crux loot item advertises `craftusable:1`; the generic inventory crafting hook runs whenever any such item is in `player.crafting.*` (`players_tick.mcfunction:61`). However, a search of the inventory crafting dispatcher and craft-ground dispatcher found no rule that consumes or checks `overload_crux`. The file `other/craft/overload_armor.mcfunction` exists, but no caller/reference to it was found. It appears intended to convert dropped diamond armor into Overload armor; it contains mismatched predicates: chestplate/helmet loot-spawn checks test for `diamond_boots`, even though the following kill lines remove diamond chestplate/helmet. As present, it is not a demonstrated reachable Crux craft recipe and is internally suspect. Sources: `data/lbc/loot_table/overload_crux.json`; `data/lbc/function/players_tick.mcfunction:61`; `data/lbc/function/other/craft_ground.mcfunction`; `data/lbc/function/other/craft/overload_armor.mcfunction`.

## Raid Omen amplifiers 17–20: timer contract

Amplifier 16 remains the existing mana-boost path: `arrows2` adds its remaining duration to `effect_boost_mana`; `other/manaregen` applies it to `manaregen_visual` by multiplying by 3 then `/= 2`, then decrements the timer once per eligible mana-regeneration tick. Amplifiers 17–20 are the four custom mana-status inputs in `data/lbc/function/projectiles/arrows2.mcfunction`. The target entity’s active `minecraft:raid_omen` amplifier selects the timer and `dur` is copied from that active effect’s remaining native duration:

| Amplifier | Timer score | Per-tick helper effect | Duration score consumed |
|---:|---|---|---:|
| 17 | `effect_add_manamax` | `manamax += 2500` | 1 |
| 18 | `effect_add_manaregen` | `manaregen += 5` | 1 |
| 19 | `effect_add_mana` | `mana += 10000` | 1 |
| 20 | `effect_remove_mana` | call `other/mana_potions/bad` | 1 |

Each of the first three helper functions applies its listed change, grants its potion advancement, subtracts 1 from its corresponding timer score, and re-adds `have_lucky_effects`. `effect_remove_mana` instead calls the “bad mana” helper, subtracts 1, and re-adds the tag. The bad helper always subtracts 2,000 mana and independently has an 8/10 chance to subtract 1,000 max mana and a 6/10 chance to subtract 1 regen. These are repeated status pulses, not one-time application totals.

Order per normal game tick: `tick_not_timestopped` visits mobs via `mobs_tick`; `mobs_tick` first converts active Raid Omen through `projectiles/arrows2` and clears the native Raid Omen, then calls `effects/tick` for custom status timers, then calls player mana regeneration (`players_tick`). Within `effects/tick`, mana add runs first, max-mana add second, regen add third, and mana removal fourth. Therefore a 17/18/19/20 tick can apply its pulse before that same tick’s `manaregen` call. The order is explicit in `effects/tick.mcfunction` lines 23–26. Native Raid Omen duration is stored in ticks; a duration of 3,600 gives 3,600 custom pulses (180 seconds at 20 ticks/s), and 9,600 gives 9,600 pulses (480 seconds), absent interruption or score limits. Because each pulse decrements the duration score, all are expected to repeat every tick until exhausted.

Area-effect-cloud/native-short-duration handling is special: `arrows2` rewrites a remaining active-effect duration exactly equal to 2 as 1 before adding it to the custom timer. This makes a cloud’s native two-tick effect contribute one custom pulse on that visit; it is not a conversion to seconds. Re-entry/reapplication contributes the newly observed duration additively to the existing timer scores. The active native effect is cleared after mapping so the same remaining native duration is not repeatedly imported outside a fresh application. Sources: `projectiles/arrows2.mcfunction` lines 1–10 and final clear; `mobs_tick.mcfunction` lines 1–3; `tick_not_timestopped.mcfunction` line 4; `effects/tick.mcfunction` lines 23–26; the four `effects/effect_add_*.mcfunction` helpers and `effect_remove_mana.mcfunction`.

Pause/veto behavior: `effects/tick` gates `effect_remove_mana` on `tag=!sin_greed_active`; while that tag is present the bad-mana pulse and its timer decrement are both paused. During global time-stop scheduling, `tick_not_timestopped` (the path that visits `mobs_tick`) is not selected, so these imported native effects and custom status timers do not advance on that scheduler path. The `effect_add_mana`, `effect_add_manamax`, and `effect_add_manaregen` helpers have no such Greed guard and continue. Separately, the `lbc.skill=19` branch calls `projectiles/arrows2_nodebuff`, which explicitly sets `effect_remove_mana` to zero; that is a veto/cancellation, not a pause. The nodebuff function does not zero the three positive mana timers. Sources: `effects/tick.mcfunction` lines 23–26; `projectiles/arrows2.mcfunction` line 55; `projectiles/arrows2_nodebuff.mcfunction` line 1.

## Scope boundary

The current delivery scope from the coordinator is the 30 potion families, their three delivery paths, and two duration variants; the mana-specific sub-scope includes existing amplifier 16 plus custom amplifiers 17–20. Negative-mana stages and Crux are a later complete combat-pipeline package; this audit’s combat source notes above are reference findings only. Do not add a clamp at -20,000 or otherwise invent an interim stage-5 policy. Preserve source behavior until the later combat-pipeline scope defines the complete behavior.
## Source links

Paths above are relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/`. Main files: `data/lbc/function/players_tick.mcfunction`, `players_1s.mcfunction`, `players_5s.mcfunction`, `other/manaregen.mcfunction`, `other/negative_mana*.mcfunction`, `other/overload_crux/{give,use}.mcfunction`, `other/negative_mana/explode.mcfunction`, `workingitems/spell1_default.mcfunction`, `other/craft_ground.mcfunction`, `other/craft/overload_armor.mcfunction`, `data/lbc/loot_table/overload_crux.json`, and `data/lbc/advancement/true_advancements/potions/effect_reverse_mana.json`.
