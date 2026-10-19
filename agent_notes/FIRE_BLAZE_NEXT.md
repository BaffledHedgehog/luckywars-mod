# Fireblaze / Fallingstars — bounded source note

Historical first pass. Superseded by [the full source trace](FIRE_BLAZE_SOURCE.md)
and [the implemented port](FIRE_BLAZE_PORT.md). The shared Academy dispatcher
does set `upgraded:1` and `mode:1`; all four modes are reachable.

Read-only trace of `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`; no runtime validation. No code/resources/build/server changes.

## Live donor and recipe result

`RC_Raycast` tracks `minecraft.used:minecraft.carrot_on_a_stick`; `players_tick.mcfunction:19` dispatches `items/spells`, which routes main/offhand `raycastspell` to `spell` (`items/spells.mcfunction:5,11`). The Fireblaze recipe is exactly `stfire:1 + fallingstars:1` (`other/craft_ground.mcfunction:22`, `other/craft/inventory_craft_check.mcfunction:22`); result is `tuntijaa_fire_blaze`, with both craft functions consuming the two inputs (`crafted_blazefirewand.mcfunction:1-6`, `_inv.mcfunction:1-12`).

Despite the `fallingstars` marker/name, this ordinary donor is presented in `transformables_staff.json` as the Blaze Wand (`lbc:blazewand`, `blaze_wand`). Mainhand and offhand raycast entries use `blaze_wand_mana_use=2800` (`lbcr/raycast.mcfunction:6`, `raycast_left.mcfunction:6`, `lbc/function/config.mcfunction:32`). Cast begins at eye position, offhand offset is 0.5; cost is deducted once after the recursive ray (`lbcr/raycast_loop4cd.mcfunction:1-5`). The loop advances 1 block at a time, max `Distance=200`, and stops on a mob within 15 of the ray point, a non-`#minecraft:airs` block behind the point, `nomagic_active` within 16, or `stopper_magic` within 20 (`raycast_loop4.mcfunction:1-5`).

On endpoint it creates a `blazespell` marker one block above with `lifetime:100`, copies caster `lbcID2` and `team_number`, then ends the ray (`raycast_end4.mcfunction:1-7`). Once per second, `raycasters_1s` dispatches `blazespell` to `other/spawnfireball`; the marker excludes same-id/team players and fires a small fireball toward the nearest eligible mob (`tick1s.mcfunction:3`; `raycasters_1s.mcfunction:1,14`; `spawnfireball.mcfunction:1-4`; `raycastfireball.mcfunction:1-5`). Its lifetime counter adds 20 each pass, so it can fire for up to five passes. The small fireball gets a cleanup tag, but no owner NBT; damage attribution is absent/unclear in this legacy source, and must be supplied in the port. A nearby Blaze at the endpoint causes team reassignment and mana refund, but endpoint processing continues and still creates the marker (`raycast_end4_blaze_nyam.mcfunction:1-3`, then `raycast_end4.mcfunction:3-7`).

`lbc:tuntijaa_fire_blaze` returns one carrot-on-a-stick result: model `lbc:blazefire`, tier A, name `fireblaze_wand`, two lore lines, custom data `{transformable:1,staff:1,magicwand:1,fireblaze_wand:1}` (`loot_table/tuntijaa_fire_blaze.json:7-79`). Magic-wand use is item-use triggered; ordinary `click` calls `click2` and costs 50 (`workingitems/awaken_tuntija_wands/fireblaze/click.mcfunction:1-7`; `config.mcfunction:90`). `click2` spawns one random-direction small fireball per eligible mob within 30, excluding spectator/caster/same team/same id; projectile gets caster id/team scores but no owner field (`click2.mcfunction:1-3`, `at_random_fireball.mcfunction:1-14`).

No separate Academy result item/table was found. Academy upgrade applies only `lbc:tiers/s` (tooltip style S); it does not add `upgraded:1` (`other/magic_academy/upgrade/fireblaze_wand.mcfunction:1-2`, `item_modifier/tiers/s.json`). Upgraded callbacks exist with `world`, `mega` (ExplosionPower 5 fireballs), `pillars` (lava columns) and `kiuas` modes, but reachability from this result is unclear (`click_upgraded.mcfunction`, `click2_upgraded.mcfunction`, `click2_upgraded_mode_*.mcfunction`).

Do not conflate the recipe donor with `truefallingstars`: that distinct flag costs 4000 and its loop creates thirteen descending fireballs with ExplosionPower 2/4 (`config.mcfunction:30`; `raycast_loop5.mcfunction:1-5`; `raycast_end5.mcfunction:9-24`).

## Native helpers and clarifications

- `LbcAir` has an owner-aware native projectile and target handling (`LbcAir.java:54,99,171`); `LbcExasperato` has swept projectile/entity collision and native explosion paths (`LbcExasperato.java:233-249,563-578`). `LbcClouds.AcidProjectile` shows tracked throwable impact handling and owner-aware world changes (`LbcClouds.java:77,253-274,303-389`). `BlockAreaWork` is bounded queued block work if destructive modes are retained (`BlockAreaWork.java:42,97`).
- Clarify whether `fallingstars` (ordinary Blaze Wand, 2800 mana) is the intended recipe input, or whether `truefallingstars` was meant. Clarify whether the Academy Fireblaze should only become tier S or should gain access to the coded upgraded modes. Both legacy projectile branches omit explicit projectile ownership, so author attribution needs a native solution. The upgraded pillar mode writes lava from Y=0 through 319; confirm whether that reach and world impact are intentional before preserving it.
