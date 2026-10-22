# Phantom Knives / Stand resource audit — 2026-10-20

Read-only audit. Only this note was written; no builds, servers, implementation, original server, RP or template changes. Rules: [RESOURCE_PACK.md](RESOURCE_PACK.md), [AGENTS.md](AGENTS.md), project AGENTS and ARCHITECTURE. Preserve all original components and user overrides; source snapshots below are exact parsed JSON before codec migration.

## Selected source entries

Source root: `E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc`. RP: `C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8` (read-only).

There is one Phantom Knives payload, not separate Stand or Over Heaven loot variants. Stand boosts are runtime player states; do not invent new lore or model states. Source does not define numeric variant IDs for these four entries: use stable custom-data identities `phantomknifes:1`, `stand_arrow:1`, `oraora:1`, `stand_clocks:1`. Native registration identifiers remain implementation choices, not source-defined variant IDs.

### loot_table/transformables_shooter.json — pools[0].entries[0]

```json
{
  "type": "minecraft:item",
  "weight": 10,
  "quality": 9,
  "name": "minecraft:carrot_on_a_stick",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_c",
        "minecraft:item_model": "lbc:phantomknifes"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "phantom_knifes",
          "italic": false,
          "color": "aqua"
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        [
          {
            "translate": "type",
            "italic": false,
            "color": "aqua"
          },
          {
            "translate": "shooter",
            "italic": false,
            "color": "aqua"
          }
        ],
        {
          "translate": "phantom_knifes.lore.1",
          "color": "white",
          "italic": false
        },
        {
          "translate": "phantom_knifes.lore.2",
          "color": "white",
          "italic": false
        },
        {
          "translate": "phantom_knifes.lore.3",
          "color": "white",
          "italic": false
        }
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{transformable:1,shooter:1,magicwand:1,phantomknifes:1}"
    }
  ]
}
```

### loot_table/stand_arrow.json — pools[0].entries[0]

```json
{
  "type": "minecraft:item",
  "weight": 10,
  "name": "minecraft:tipped_arrow",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:item_model": "lbc:stand_arrow",
        "minecraft:tooltip_style": "lbc:tier_q",
        "minecraft:potion_contents": {
          "custom_effects": [
            {
              "id": "minecraft:raid_omen",
              "amplifier": 119,
              "duration": 20
            }
          ]
        }
      }
    },
    {
      "function": "minecraft:toggle_tooltips",
      "toggles": {
        "minecraft:dyed_color": false
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "stand_arrow",
          "color": "aqua",
          "italic": false
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        {
          "translate": "stand_arrow.lore.1",
          "color": "white",
          "italic": false
        },
        {
          "translate": "stand_arrow.lore.2",
          "color": "white",
          "italic": false
        },
        {
          "translate": "stand_arrow.lore.3",
          "color": "white",
          "italic": false
        },
        {
          "translate": "craft_with_time_clocks",
          "color": "white",
          "italic": false
        },
        [
          {
            "text": "Neqrest",
            "color": "green",
            "italic": false,
            "type": "text"
          },
          {
            "text": " No Nut November 2023 Event",
            "color": "dark_gray",
            "italic": false,
            "type": "text"
          }
        ]
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{craftusable:1,stand_arrow:1}"
    }
  ]
}
```

### loot_table/stand_oraora.json — pools[0].entries[0]

```json
{
  "type": "minecraft:item",
  "weight": 10,
  "name": "minecraft:carrot_on_a_stick",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_s",
        "minecraft:item_model": "lbc:oraora"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "oraora",
          "color": "aqua",
          "italic": false
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        {
          "translate": "oraora.lore.1",
          "color": "white",
          "italic": false
        },
        {
          "translate": "oraora.lore.2",
          "color": "white",
          "italic": false
        },
        [
          {
            "text": "Neqrest",
            "color": "green",
            "italic": false,
            "type": "text"
          },
          {
            "text": " No Nut November 2023 Event",
            "color": "dark_gray",
            "italic": false,
            "type": "text"
          }
        ]
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{oraora:1,magicwand:1,spellwithcooldown:1}"
    }
  ]
}
```

### loot_table/stand_clocks.json — pools[0].entries[0]

```json
{
  "type": "minecraft:item",
  "weight": 10,
  "name": "minecraft:carrot_on_a_stick",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_x",
        "minecraft:item_model": "lbc:stand_clock"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "stand_clocks",
          "color": "aqua",
          "italic": false
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        {
          "translate": "stand_clocks.lore.1",
          "color": "white",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.2",
          "color": "white",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.3",
          "color": "green",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.4",
          "color": "green",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.5",
          "color": "green",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.6",
          "color": "green",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.7",
          "color": "green",
          "italic": false
        },
        {
          "translate": "stand_clocks.lore.8",
          "color": "gold",
          "italic": false
        },
        [
          {
            "text": "Neqrest",
            "color": "green",
            "italic": false,
            "type": "text"
          },
          {
            "text": " No Nut November 2023 Event",
            "color": "dark_gray",
            "italic": false,
            "type": "text"
          }
        ]
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{stand_clocks:1,magicwand:1,spellwithcooldown:1}"
    }
  ]
}
```

## Existing RP resources and translations

Full source name/lore arrays above preserve line order, nested formatting, literal event attribution and explicitly false italics. Phantom: aqua name; aqua type+shooter line then three white description lines, tier C. Stand Arrow: aqua name, four white translated lines then mixed green/dark-gray event attribution, tier Q; tipped arrow Raid Omen amplifier 119 duration 20; hidden dyed-color tooltip. ORAORA: aqua name, two white translated lines plus event attribution, tier S. Clocks: aqua name, two white, five green, one gold translated lines plus event attribution, tier X. No extra dynamic state lore appears in those source payloads.

| Model | Existing item definition |
|---|---|
| `lbc:ora` | `assets\lbc\items\ora.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/ora"}}
```

| `lbc:oraora` | `assets\lbc\items\oraora.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/oraora"}}
```

| `lbc:overheaven` | `assets\lbc\items\overheaven.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/overheaven"}}
```

| `lbc:phantomknifes` | `assets\lbc\items\phantomknifes.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/phantomknifes"}}
```

| `lbc:stand_arrow` | `assets\lbc\items\stand_arrow.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/stand_arrow"}}
```

| `lbc:stand_clock` | `assets\lbc\items\stand_clock.json`: present |

```json
{"model": {"type": "model", "model": "lbc:item/stand_clock"}}
```


### Existing ru_ru values

```json
{
  "craft_with_time_clocks": "Можно соединить с Часами Повелителя Времени!",
  "lbc.adv.legends.overheaven": "ОВЕРХЭВЕЕЕЕН!!!!!!!!!!!!",
  "lbc.adv.legends.overheaven.lore": "Получите ОверХэвен на свой стэнд, забравшись выше 200 высоты с 'Часами Повелителя Стар-Платины' в инвентаре!",
  "lbc.adv.legends.stand": "Ора Ора Ора Ора Ора Ора!",
  "lbc.adv.legends.stand.lore": "Получите стар платиновый стенд, проткнув себя 'Стрелой Абсурда'",
  "lbc.adv.legends.stand_clocks": "ЗА ВАРДООООООООООООО",
  "lbc.adv.legends.stand_clocks.lore": "Получите 'Часы Повелителя Стар-Платины', соеденив 'Стрелу Абсурда' с 'Часами повелителя времени'",
  "lbc.adv.shooter.phantom_knifes": "Бабл Квас              ",
  "lbc.adv.shooter.phantom_knifes.lore": "Взмахните 'Фантомными ножами' сразу из двух рук. Кстати, они пробивают щиты!",
  "oraora": "Шквал Ударов!",
  "oraora.lore.1": "Ваш стенд позволяет делать шквал ударов,",
  "oraora.lore.2": "во время которых ваш стенд наносит 100 ударов!",
  "phantom_knifes": "Фантомные ножи",
  "phantom_knifes.lore.1": "Таинственный артефакт, при взмахе",
  "phantom_knifes.lore.2": "которого запускаются несколько",
  "phantom_knifes.lore.3": "призрачных острых лезвий (ПКМ)",
  "shooter": "Прочее стрелковое",
  "stand_arrow": "Стрела Абсурда",
  "stand_arrow.lore.1": "С шансом 10%% может убить, но",
  "stand_arrow.lore.2": "с шаносм 90%% цель получает стенд!",
  "stand_arrow.lore.3": "(надо ударить цель либо выстрелить стрелой из лука)",
  "stand_clocks": "Часы Повелителя Стар-Платины",
  "stand_clocks.lore.1": "На ПКМ Останавливают время на 20 секунд!",
  "stand_clocks.lore.2": "Перезарядка - 40 секунд!",
  "stand_clocks.lore.3": "Наличие этих часов значительно усиливают способности стэнда (если получен):",
  "stand_clocks.lore.4": "Шквал ударов - сила атаки в 3 раза выше",
  "stand_clocks.lore.5": "Если нет жезла вампиризма - Постоянное горение",
  "stand_clocks.lore.6": "Жезл вампиризма: Урон x5, Регенерация x5",
  "stand_clocks.lore.7": "Фантомные ножи: Количество ножей x4, Урон x10",
  "stand_clocks.lore.8": "Если поднятся выше 200 блоков, стэнд будет улучшен до Over Heaven!",
  "type": "Тип предмета: "
}
```

### Existing en_us values

```json
{
  "craft_with_time_clocks": "Can be combined with the Time Lord's Clock!",
  "lbc.adv.legends.overheaven": "OVERHEAVEN!!!!!!!!!!!!",
  "lbc.adv.legends.overheaven.lore": "Obtain OverHeaven for your stand by going over 200 blocks above sea level with 'Star-Platinum Lord's Clock' in your inventory!",
  "lbc.adv.legends.stand": "Ora Ora Ora Ora Ora Ora!",
  "lbc.adv.legends.stand.lore": "Obtain Star Platinum Stand by shooting yourself with 'Absurd Arrow'",
  "lbc.adv.legends.stand_clocks": "THE WOOOOOORLLLLLLDDDDDDD!!!",
  "lbc.adv.legends.stand_clocks.lore": "Obtain 'Star-Platinum Lord's Clock' by combining 'Absurd Arrow' and 'Time Lord's Clock'",
  "lbc.adv.shooter.phantom_knifes": "Bubble Kvass",
  "lbc.adv.shooter.phantom_knifes.lore": "Swing the Phantom Knives from both hands. They can penetrate shields, by the way!",
  "oraora": "Flurry of Blows!",
  "oraora.lore.1": "Your stand allows you to perform a flurry of attacks",
  "oraora.lore.2": "during which your stand deals 100 hits!",
  "phantom_knifes": "Phantom knifes",
  "phantom_knifes.lore.1": "Strange knife, when swinging,",
  "phantom_knifes.lore.2": "launches some",
  "phantom_knifes.lore.3": "sharp phantom blades (Right Click)",
  "shooter": "Other shooting",
  "stand_arrow": "Arrow of the Absurd",
  "stand_arrow.lore.1": "Can kill with 10%% chance, but",
  "stand_arrow.lore.2": "with a 90%% chance the target gets a stand!",
  "stand_arrow.lore.3": "(need to hit the target or shoot an arrow from a bow)",
  "stand_clocks": "Star-Platinum Lord's Clock",
  "stand_clocks.lore.1": "On RMB stops time for 20 seconds!",
  "stand_clocks.lore.2": "Reload - 40 seconds!",
  "stand_clocks.lore.3": "The presence of this clock significantly enhances the abilities of the stand (if obtained):",
  "stand_clocks.lore.4": "Flourish of Blows - Attack Power is 3x higher",
  "stand_clocks.lore.5": "If there is no wand of vampirism - Constant burning",
  "stand_clocks.lore.6": "Stand of Vampirism: Damage x5, Regeneration x5",
  "stand_clocks.lore.7": "Phantom Knives: Knives x4, Damage x10",
  "stand_clocks.lore.8": "If you rise above 200 blocks, the stand will be upgraded to Over Heaven!",
  "type": "Item type: "
}
```
- `assets\lbc\textures\gui\sprites\tooltip\tier_c_background.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_c_frame.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_q_background.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_q_frame.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_s_background.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_s_frame.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_x_background.png`: present.
- `assets\lbc\textures\gui\sprites\tooltip\tier_x_frame.png`: present.

## Obtainment and state sources

- Phantom Knives is first (zero-based index 0) entry in transformables_shooter, weight 10 quality 9, count 1. Preserve its selection weight and quality in shared tables; standalone extraction must select precisely that custom data identity.
- Stand Arrow payload is standalone and referenced by arrows / loliluckyblock source tables. Its hit dispatch uses Raid Omen 119, effect_stand_arrow_hit, other/stand_arrow/hit_pre and hit; hit_pre also returns a stand arrow via loot spawn.
- `function/other/stand_arrow/stand_gain.mcfunction` adds `have_stand` and gives `lbc:stand_oraora`; `start_stand_give.mcfunction` gives the same table to all have_stand players.
- `function/other/craft/stand_clocks.mcfunction` spawns stand_clocks and consumes one nearby item each with stand_arrow and time_stopper custom data. `stand_clocks_inv.mcfunction` spawns same loot and reduces matching ingredients in player.crafting.0..3. This is custom legacy crafting, not a vanilla recipe JSON.
- `other/stand_arrow/stand_clocks_overheaven_upgrade.mcfunction` sets overheaven=5, sends gold `upgraded_overheaven`, spawns homing_lightning and grants legends/overheaven. This does not replace the clocks payload or name/lore.
- Runtime projectile definitions: workingitems/phantomknifes/{1..5}[ _stand / _stand_x2 ].mcfunction and left variants; items/oraora/projectile_create.mcfunction uses normal/strong/overheaven state flags. Inspect source runtime conditions independently from source loot identity.

## Current native project and reusable importer path

At audit time current main loot tables contain migrated placeholder vanilla carrot_on_a_stick / tipped_arrow entries. Java source search finds no Phantom/Stand registration or handling; LbcKfcFood only contains the existing coldownoraora compatibility counter name. Existing advancements already retain source icon models and translation keys. Do not mistake those data files for implemented gameplay.

No `tools/import_lbc_shooters.py` exists at audit time. Reuse the established extraction pattern in `tools/import_lbc_slr.py` / `import_lbc_infbolt.py`, `import_game_resources.migrate_loot_schema`, source ROOT/SOURCE from `import_lbc_loadouts`, RP constant from `import_lbc_consuming`, and `import_lbc_wands.write_table`. Extract the full selected entry, migrate schema, replace only the base item with intended native registration, preserve complete payload and assert equality after permitted changes. Shared loot-table remapping must use exact custom data identity and avoid replacing unrelated shooter entries. Existing RP keys/models above require no duplicate translations or replacement art.

This audit verifies existence and exact JSON/text sources, not graphical client equivalence or gameplay.

## Projectile representation clarification

Phantom Knives source uses native `minecraft:arrow`, pickup 0, damage 2, PierceLevel 127 (normal first volley; see workingitems/phantomknifes/1.mcfunction), with copied Owner UUID, lbcID2 and team_number. There is no custom knife projectile item model in this source: `lbc:phantomknifes` is the held/advancement icon. ORAORA uses item displays: `lbc:ora` normally and `lbc:overheaven` in the evolved state; both RP item definitions exist. Numeric line positions in drops/tmt/set_storage_nbttags are catalog order, not durable variant IDs.
