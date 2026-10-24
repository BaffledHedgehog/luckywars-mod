# Halloween wind resources ? source audit, 25 October 2026

Scope: Halloween `vex_wand` (wind wand), not Vex Soul/summoner. Source server and author RP were read only. No author RP/template additions are needed. This is source/payload validation; runtime decoding, native behavior and client pixels remain integration checks.

## Release files

- `src/main/resources/data/lbc/loot_table/ported/vex_wind_wand.json` ? `skywars2:vex_wind_wand`.
- `src/main/resources/data/lbc/loot_table/ported/academy_vex_wind_wand.json` ? `skywars2:academy_vex_wind_wand`.
- `src/main/resources/data/lbc/loot_table/ported/halloween_mace.json` ? `skywars2:halloween_mace`.

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Author RP: see [RESOURCE_PACK.md](RESOURCE_PACK.md).

## Exact item payload

Wand source `loot_table/halloween/transformables_staff.json`, first entry: carrot-on-a-stick, source weight10 quality8, tierB, model `lbc:vex_wand`, name `[translate:vex_wand,italic:false]` with no explicit color. Five lore rows: aqua type+staff; aqua score `vex_wand_mana_use`/`lbc.math` + `mana_on_activate`; white `vex_wand.lore.1`; white `vex_wand.lore.2`; orange #ff670f `halloween.lore`, all explicit italic:false where source specifies. Exact custom data `{transformable:1,staff:1,magicwand:1,vex_wand:1}`.

Academy retains the complete base payload, appends source green `upgraded_item`/italic:false, then tierA override. Source `function/other/magic_academy/upgrade/vex_wand.mcfunction` sets tierA; `interacted_upgrade.mcfunction` sets upgraded1, mode1 and calls `item_modifier/add_upgrade_desc.json`. Scoped leaf has `upgradable:0,upgrade_available:1,upgraded:1,mode:1`, following the shared completed Academy representation. No new names/translations.

Mace source `loot_table/halloween/transformables_shovel.json`, first/only entry: minecraft:mace, source weight10 quality5, tierS/model `lbc:halloween_mace`; attack_damage +5 and attack_speed -3.4, mainhand, original base modifier IDs; wind_burst2/density5. Gold `halloween_mace`, italic:false. Six lore rows: aqua type+shovel, three yellow mace lore rows, fourth gold row, orange Halloween row. Exact custom data `{transformable:1,shovel:1,halloween_mace:1,spellwithcooldown:1,craftusable:1}`. Source does not set unbreakable or hide enchants/attributes; neither does this leaf.

Standalone leaves remove pool-selection weight/quality. Python structural comparison verified every ordinary modifier payload equals the exact source after only `function`?`type`; item IDs alone become native IDs. Academy adds only completed flags, source upgrade lore and source tierA.

## 26.3 structured JSON API

Read local mapped `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`: `LootPoolEntryContainer.java` uses `LootItemFunctions.CODEC.optionalFieldOf("modifier")`; `LootItemFunctions.TYPED_CODEC` uses registry codec default `dispatch` (`type`), and DIRECT_CODEC accepts SequenceFunction.INLINE_CODEC. Thus old entry `functions` must become `modifier`, and each old function discriminator becomes `type`. Nested text component `type:score` remains untouched. `ItemAttributeModifiers.CODEC` is a list of Entry.CODEC; Entry uses attribute `type`, modifier fields, optional slot/display. `ItemEnchantments.CODEC` is an unbounded enchantment?level map. Source mace component shapes are compatible; no obsolete levels wrapper is introduced. This verifies source codec structure, not a running server decode.

## Source live dispatch, HUD and trial

- `function/config.mcfunction:117`: admission/cost700. Both default hand dispatches (`workingitems/spell1_default*:105/92`) require >=700 and no magic stopper within20. `cast.mcfunction` deducts700, retains mage/trinket hooks; normal radius5 forward additive motion includes caster and non-spectator source mobs.
- `cast.mcfunction` uses trial `upgradable:1` + sneak to call `cast_2meditate`. The latter calls common can_upgrade, places source air_sphere_11x11x11 at (-5,-1,-5), emits gust_emitter_large, and sets radial motion magnitude10 for source mobs within11. `can_upgrade/vex_wand` gives source trial text. Source trial not a summon.
- `cast_upgraded` and `_left` shift mode through common change_mode_4 helpers; messages mode_default/mode_user/mode_not_user/mode_windball. `cast_upgraded_2` mode1 all nearby,2 caster only,3 others (0.01..5),4 five native wind charges with owner/team and original spread helper. Source upgraded dispatch still gates >=700 even for shift. Current global user rule supersedes this: Academy mode switches FREE at ANY mana. Ordinary admission remains700; actual casts cost700.
- `players_tick.mcfunction:6` jump1 invokes `items/armor`; `items/armor.mcfunction:14` requires pitch80..90, mace in either hand and cooldown<=0. TMT trigger functions also call items/armor; preserve the observed source chain rather than inferring an unconditional right-click ability from lore.
- `other/event/halloween/halloween_mace/ability.mcfunction`: cooldown_wind_charge=5, gust_emitter_large, breeze wind_burst sound, source `motion set ~ ~1 ~` (audited native MotionBrigadier interpretation: preserve XZ, add1Y). `players_1s.mcfunction:43` subtracts1 each second. No mana charge.
- `ui/cooldown.mcfunction:50/51` exact prefixes are four spaces + U+E105 + space with white current seconds, or four spaces + U+E104 + space with green translate ready. Author `assets/minecraft/font/default.json` maps E104 to `minecraft:font/icons/ready.png`, E105 to `minecraft:font/icons/reloading.png`, ascent8 height9.

## Author presentation verification

Author `assets/lbc/items/vex_wand.json` references `lbc:item/vex_wand`; matching model exists. `assets/lbc/items/halloween_mace.json` references `lbc:item/halloween_mace`; matching model exists. Existing author language keys were read in en_us, ru_ru and rpr, retaining spelling and incomplete English lore exactly. No missing target keys found.

| Key | en_us | ru_ru (=rpr checked) |
| --- | --- | --- |
| `vex_wand` | Vexwinder | Призракодув |
| `vex_wand.lore.1` | Accelerates everyone within 5 blocks towards | Порывами ветра ускоряет вперёд всех вокруг вас |
| `vex_wand.lore.2` | (Works on ghost fuel... Booo!!!) | (Работает на призрачном топливе) |
| `halloween_mace` | Antichrist's Pneumohammer | Пневмолот Антихриста |
| `halloween_mace.lore.1` | On jump (while looking down): | При прыжке (если смотреть под себя): |
| `halloween_mace.lore.2` | Launches up, like the wind charge | Запускает вверх, как от снаряда ветра |
| `halloween_mace.lore.3` | Recharge time: 5 seconds. | Перезарядка 5 секунд. |
| `halloween_mace.lore.4` | Can be combined with 'Soulless wand' | Можно объеденить с 'Бездушным жезлом' |
| `halloween.lore` | You can feel Halloween vibe from this item. | От этого предмета веет духом Хэллоуина |
| `upgraded_item` | This staff has been upgraded at the Academy of Mages! | Этот жезл был улучшен в Академии Магов! |
| `upgrade_condition_vex` | Meditate with this wand (Shift+Right Click), The insane wind burst will smash everuthing around, Then you need to get back for upgrade! | Медитируйте этим жезлом (Shift+ПКМ) - огромная волна воздуха обязательно снесёт ВСЁ вокруг вас, и затем вернитесь за улучшением! |
| `upgrade_succesful_vex` | Now your wand can use different modes! Shift+Right Click to Switch! | Медитация открыла вам новые режимы жезла! (Shift+ПКМ) |
| `mode_default` | Wand Mode: Default | Режим жезла: Стандартный |
| `mode_user` | Mode: User only | Режим жезла: Только пользователь |
| `mode_not_user` | Mode: NOT user | Режим жезла: НЕ пользователь |
| `mode_windball` | Mode: Launch wind charges | Режим жезла: Метание зарядов ветра |

No presentation blocker found. Scope does not enable broad Halloween loot pools, Soulhunter crafting, advancements or Vex Soul. Trial terrain and native ownership remain behavior integration responsibilities.


## Integrated qualification

Build `07ff2ecd` passed80 recorded suite results across two actual mode1 matches (150.83s). Exact 313 sources/866 classes/2800 resources match the installed tested JAR. Main installed stopped, rollback `b0dfd5ca` retained, server not started. Evidence: `build/verification/halloween-wind-final/`. Existing fake-bot Polymer packet-context ERROR pair only; no new error lines. Native fixtures include explicit boundaries; no real client pixels, disk restart/disconnect or peak-load claim. This supersedes individual task-agent pending statuses.
