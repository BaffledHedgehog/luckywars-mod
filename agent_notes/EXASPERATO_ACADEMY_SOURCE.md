# Exasperato: upgraded modes and Academy path

Scope: upgraded Exasperato only, including Academy marking, dispatch, and the four upgraded modes. Ordinary Exasperato is intentionally not documented here. Source is the read-only datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Corrections from the 16 October recheck

See [EXASPERATO_ACADEMY_RECHECK.md](EXASPERATO_ACADEMY_RECHECK.md) for the
corrected call chain and the source traps found during review. Mode 1 advances one block every normal tick.
Mode 2 advances 4.5 blocks and creates 13 TNT plus one bolt EACH normal tick,
for up to its 200-tick lifetime. `data/lbcr/function/raycast_end_with_tp.mcfunction`
exists: reset Distance and teleport to the recursive endpoint. Nuke descends
one block per tick. On collision its shared payload creates 32 radius-20
creepers, then `kill @s[type=!minecraft:player]` removes the caller rocket.
There is no missing-helper or recurring-next-tick-explosion design decision.
All delayed effects must retain the caster as required by the user.

## Academy and item state

The base loot table `loot_table/tuntijaa_air_exasperoso.json` gives a carrot-on-a-stick with model `lbc:exasperato`, custom data `{transformable:1,staff:1,magicwand:1,exasperato_wand:1}`, and `exasperato_wand_mana_use` displayed in lore. Config sets that objective to **1500** (`function/config.mcfunction`).

Academy interaction dispatch in `function/other/magic_academy/interacted_2.mcfunction` calls `can_upgrade/exasperato_wand`, whose only action is the `upgrade_condition_exasperato` message. The general `interacted_upgrade.mcfunction` path transfers the held item through a fixed block inventory, sets `upgradable:1`, and calls `upgrade/exasperato_wand`; the item-specific function only tells the player it succeeded. General code then sets `upgraded:1`, `mode:1`, and applies `lbc:add_upgrade_desc`. Thus `upgradable:1` is a distinct in-progress flag, and upgraded modes are selected by `mode` 1–4. The Academy advancement criteria require `{upgraded:1,transformable:1,staff:1,exasperato_wand:1}`. Its locked item modifier `item_modifier/swrg_unlocks/academy_exasperato.json` changes name/lore only; it is not the upgrade operation.

Sneak use cycles mode 1→2→3→4→1 using `change_mode_4` (offhand has a separate `_left` function). The item-use entrypoints are `spell1_upgraded.mcfunction` and `spell1_upgraded_left.mcfunction`: they require at least 1500 mana and no `stopper_magic` marker within 20 blocks. `click_upgraded_pre` dispatches the mode first, then subtracts 1500 mana, updates mage set bonus, and calls the trinket cast effect. This is one cost per cast, independent of mode.

## Four upgraded modes

All four are selected in `click2_upgraded.mcfunction` by reading `SelectedItem...custom_data.mode` (offhand if tagged `left`) and dispatching 1–4. The launch origin is the caster's anchored eyes position.

| Mode | Cost and launch | Path, collision and effect |
|---|---|---|
| 1 Default | 1500 mana; 3 projectiles with source lifetimes 200/100/50 ticks. | Advances one block EVERY normal tick via slowraycast, not the separate marker_2t scheduler. Solid collision or an eligible mob contact triggers the shared 85-TNT/50-lightning payload; native owner/team/observer filters and one terminal invocation are required by migration contracts. Fixed lifetime vs smart removal is awaiting the user. |
| 2 Electro Ray | 1500 mana; one marker with lifetime 200. | Recursion samples 10 positions at 0.5-block intervals in ONE tick, ending 4.5 blocks forward. Solid samples have a 20% chance to kill it. Endpoint teleports the marker, resets Distance, creates one bolt and 13 TNT: center fuse 50, four cardinal neighbors repeated at fuses 60/61/62. This repeats EVERY tick until removal, up to 2600 TNT/200 bolts. Counts and continuous payload await user confirmation. |
| 3 Chaos Area | 1500 mana; direct function call, no Exasperato projectile marker. | Summons **80 invulnerable, no-AI, no-gravity creepers** (`Fuse:0`, `ExplosionRadius:4`) and **40 lightning bolts**, all at local forward offset `^ ^ ^60`, tagged `ebanuty_explode`; then applies `universal_pos_razb_35_active` to up to 80 creepers and 40 bolts. For every entity, that helper replaces each coordinate with its integer-truncated current coordinate plus a separately randomized integer from −35 through +35, then adds tag `active`. This teleports them into a 71×71×71 block cube around the original 60-block point; it is position scatter, not velocity scatter. No collision or TTL logic is present in the mode itself. |
| 4 Nuke | 1500 mana; source search samples 0.7 blocks up to Distance 400 (nominal 280 blocks). | A non-air block or non-caster, non-spectator mob near the sample starts a falling marker 500 blocks above it. Four 0.25-block moves per normal tick descend one block. Existing lbcr:raycast_end_with_tp resets Distance and teleports to the recursion endpoint. A solid collision at Y 0..319 calls exopersoexplode: 32 fuse-zero radius-20 creepers, particles/sounds and final kill @s[type=!minecraft:player], which removes the caller rocket. No fixed pre-collision TTL; native caster attribution is mandatory. |

### Shared and ordinary dispatch chain

Regular upgraded wand use: `spell1_upgraded` / `_left` → `click_upgraded` / `_left` → `click_upgraded_pre` → `click2_upgraded` → mode function. `click2_upgraded` removes the temporary `left` tag after reading mode. Default and Electro markers carry `slowraycast,rare`: `raycastselector` → `raycastselector_rare` → mode-specific `chtick*`. The outer datapack path is `tick` -> `tick_not_timestopped` (line 3), once per normal server tick while global time is running. The earlier proposed `tick2t` / `markers_2t1` path was incorrect: that scheduler selects `marker_2t`, and `markers_2t1` does not dispatch Exasperato. Nuke's falling marker is routed by `raycastselector_other`.

The mode 1 collision explosion is not the same path as mode 2: mode 1 executes `chtick_explode_check_id` first, while mode 2 simply reaches its endpoint and summons its payload.

## Questions / suspicious source details

1. Should Chaos Area really create 80 creepers **plus** 40 lightning bolts at the same exact point 60 blocks forward before spreading? There are two blocks of repeated summons (40+40 creepers and 20+20 lightning), despite no different coordinates or properties.
2. Electro Ray repeats each of four cardinal TNT coordinates three times with 60/61/62 tick fuses. Is the three-wave fuse pattern intended, or are the repeated positions accidental? Its map-collision check only kills on a 20% chance.
3. Nuke copies no native owner/team; migration must supply them per the existing user kill-attribution contract (not a new design question).
4. `chtick_explode_check_id` applies `nodamage` to owner and same-team mobs, but then runs `tag @e remove nodamage` globally. Could this clear a tag owned by another concurrent mechanic? Its checks also use the nearest entity tagged `exasperato` for the ID/team comparison, rather than explicitly the current marker, so overlapping shots may compare against a different projectile.
5. The Academy-specific check/success functions only send messages. The actual item flags are changed in the generic `interacted_2`/`interacted_upgrade` code. Confirm whether these messages are intended to represent an Academy test condition; the condition itself is not encoded in the Exasperato-specific check file.
6. Resolved: data/lbcr/function/raycast_end_with_tp.mcfunction exists and continues movement; exopersoexplode kills the caller rocket on collision. See the recheck for exact call context.

## Source files

- `function/workingitems/awaken_tuntija_wands/exasperato/` (all mode and tick functions)
- `function/markers_2t.mcfunction`, `function/markers_2t1.mcfunction`, `function/raycastselector_rare.mcfunction`, `function/raycastselector_other.mcfunction`
- `function/workingitems/spell1_upgraded.mcfunction`, `function/workingitems/spell1_upgraded_left.mcfunction`
- `function/other/magic_academy/interacted_2.mcfunction`, `interacted_upgrade.mcfunction`, `can_upgrade/exasperato_wand.mcfunction`, `upgrade/exasperato_wand.mcfunction`, `change_mode_4.mcfunction`, `change_mode_4_left.mcfunction`
- `loot_table/tuntijaa_air_exasperoso.json`, `advancement/true_advancements/legends/academy/exasperato.json`, `item_modifier/swrg_unlocks/academy_exasperato.json`


