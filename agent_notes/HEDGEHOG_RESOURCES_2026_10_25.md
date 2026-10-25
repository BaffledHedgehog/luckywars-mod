# Hedgehog presentation and acquisition audit ? 25 October 2026

Initial read-only source audit, followed by the explicitly authorized native item/resource implementation recorded below. Author RP and templates remain untouched. See RESOURCE_PACK.md for author requirements.

## Native scope and candidate IDs

No existing Hedgehog/spike/god registration or behavior was found in Java; the existing `god_hedgehog` hits are Silence exclusion/retaliation support and its cow fixture only. Source resources packaged in the mod are not evidence of live native support. Candidates (implementation decision, not established registry IDs): `skywars2:hedgehog_wand`, `skywars2:hedgehog_wand_academy`, `skywars2:ender_spike`, `skywars2:blind_spike`, `skywars2:explosive_spike`, `skywars2:spike`, `skywars2:wither_spike`, `skywars2:poison_spike`, `skywars2:impulse_spike`, `skywars2:god`. Source typo `spike_standart` must remain in compatibility data.

Ordinary staff category: transformable/staff/raycastspell; weighted entry10 in transformables_staff. Source mana config2500. Academy promotion changes tier to S only; inherited name/model/lore remain. No second authored upgraded item entry. Lock metadata uses gui.locked, gui.for_open and lbc.adv.upgraded_staff.hedgehog. Wall pool: one roll, seven equal-weight entries. random_glack_1 also references wall loot at weight1. Boss loot god: one carrot-on-stick with god:1,magicwand:1. No executable god custom-data handler found in lbc/lbcr functions: reward is source inert; do not invent power from its name.

No dedicated Hedgehog crafting definition found; native category/trial/Academy metadata must be added consistently with other staffs. Full family includes65-fish wall trial,150 global wall count boss, seven functioning spikes and full boss.

## Exact source item components

### lbc/loot_table/transformables_staff.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json)

```json

[
  {
    "type": "minecraft:item",
    "weight": 10,
    "quality": 0,
    "name": "minecraft:carrot_on_a_stick",
    "functions": [
      {
        "function": "minecraft:set_count",
        "count": 1
      },
      {
        "function": "minecraft:set_components",
        "components": {
          "minecraft:tooltip_style": "lbc:tier_b",
          "minecraft:item_model": "lbc:hedgehogwand"
        }
      },
      {
        "function": "minecraft:set_name",
        "name": [
          {
            "translate": "hedgehog_wand",
            "italic": false
          }
        ]
      },
      {
        "function": "minecraft:set_lore",
        "mode": "replace_all",
        "entity": "this",
        "lore": [
          [
            {
              "translate": "type",
              "italic": false,
              "color": "aqua"
            },
            {
              "translate": "staff",
              "italic": false,
              "color": "aqua"
            }
          ],
          [
            {
              "text": "6 ",
              "italic": false,
              "color": "aqua",
              "type": "text"
            },
            {
              "translate": "magic_damage",
              "italic": false,
              "color": "aqua"
            }
          ],
          [
            {
              "text": "4 ",
              "italic": false,
              "color": "aqua",
              "type": "text"
            },
            {
              "translate": "projectile_damage",
              "italic": false,
              "color": "aqua"
            }
          ],
          [
            {
              "text": "1 ",
              "italic": false,
              "color": "aqua",
              "type": "text"
            },
            {
              "translate": "cast_count",
              "italic": false,
              "color": "aqua"
            }
          ],
          [
            {
              "score": {
                "name": "baffled_wand_mana_use",
                "objective": "lbc.math"
              },
              "italic": false,
              "color": "aqua",
              "type": "score"
            },
            {
              "translate": "mana_on_activate",
              "italic": false,
              "color": "aqua"
            }
          ],
          [
            {
              "text": "200 ",
              "italic": false,
              "color": "aqua",
              "type": "text"
            },
            {
              "translate": "cast_distance",
              "italic": false,
              "color": "aqua"
            }
          ],
          {
            "translate": "hedgehog_wand.lore.1",
            "color": "white",
            "italic": false
          },
          {
            "translate": "hedgehog_wand.lore.2",
            "color": "white",
            "italic": false
          },
          {
            "translate": "hedgehog_wand.lore.3",
            "color": "white",
            "italic": false
          },
          {
            "translate": "hedgehog_wand.lore.4",
            "color": "white",
            "italic": false
          }
        ]
      },
      {
        "function": "minecraft:set_custom_data",
        "tag": "{transformable:1,staff:1,raycastspell:1,baffledhedgehog:1}"
      }
    ]
  }
]

```

### lbc/loot_table/hedgehog_wall.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/hedgehog_wall.json)

```json

{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_x",
                "minecraft:item_model": "lbc:spike_ender"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "ender_spike",
                  "color": "dark_purple",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "ender_spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_confuse:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_a",
                "minecraft:item_model": "lbc:spike_blind"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "blind_spike",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "blind_spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_blind:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_x",
                "minecraft:item_model": "lbc:spike_explosive"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "explosive_spike",
                  "color": "gold",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "explosive_spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_boom:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_a",
                "minecraft:item_model": "lbc:spike"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "spike",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_standart:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_a",
                "minecraft:item_model": "lbc:spike_wither"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "wither_spike",
                  "color": "gray",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "wither_spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_wither:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_b",
                "minecraft:item_model": "lbc:spike_poison"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "poison_spike",
                  "color": "dark_green",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "lore": [
                {
                  "translate": "poison_spike.lore",
                  "color": "yellow",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_poison:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:snowball",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_s",
                "minecraft:item_model": "lbc:spike_impulse"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "impulse_spike",
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
                  "translate": "impulse_spike.lore",
                  "color": "aqua",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{special:1,spike_impulse:1}"
            }
          ]
        }
      ]
    }
  ]
}

```

### lbc/loot_table/god.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/god.json)

```json

{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
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
                "minecraft:item_model": "lbc:god"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "god",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_lore",
              "mode": "replace_all",
              "entity": "this",
              "lore": [
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:alt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:alt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:alt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                },
                {
                  "text": "",
                  "color": "dark_red",
                  "italic": false,
                  "font": "minecraft:illageralt"
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{god:1,magicwand:1}"
            }
          ]
        }
      ]
    }
  ]
}

```

### lbc/item_modifier/swrg_unlocks/hedgehog_academy.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/hedgehog_academy.json)

```json

[
  {
    "function": "minecraft:set_name",
    "entity": "this",
    "name": [
      {
        "translate": "gui.locked",
        "color": "red",
        "bold": true,
        "italic": false
      }
    ]
  },
  {
    "function": "minecraft:set_lore",
    "mode": "replace_all",
    "entity": "this",
    "lore": [
      {
        "translate": "gui.for_open",
        "color": "red"
      },
      {
        "translate": "lbc.adv.upgraded_staff.hedgehog",
        "color": "red"
      }
    ]
  }
]

```

### lbc/advancement/true_advancements/staffs/hedgehog.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/hedgehog.json)

```json

{
  "display": {
    "icon": {
      "id": "minecraft:carrot_on_a_stick",
      "components": {
        "minecraft:item_model": "lbc:hedgehogwand"
      }
    },
    "title": {
      "translate": "lbc.adv.staff.hedgehog"
    },
    "description": {
      "translate": "lbc.adv.staff.hedgehog.lore"
    },
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "parent": "lbc:true_advancements/staffs/unstable_book",
  "criteria": {
    "requirement": {
      "trigger": "minecraft:impossible"
    }
  }
}

```

### lbc/advancement/true_advancements/legends/academy/hedgehog.json

[Source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/hedgehog.json)

```json

{
  "display": {
    "icon": {
      "id": "minecraft:carrot_on_a_stick",
      "components": {
        "minecraft:item_model": "lbc:hedgehogwand"
      }
    },
    "title": {
      "translate": "lbc.adv.upgraded_staff.hedgehog"
    },
    "description": {
      "translate": "lbc.adv.upgraded_staff.hedgehog.lore"
    },
    "frame": "goal",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "parent": "lbc:true_advancements/legends/academy/unstable_book",
  "criteria": {
    "requirement": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "player": [
          {
            "condition": "minecraft:inverted",
            "term": {
              "condition": "minecraft:entity_properties",
              "entity": "this",
              "predicate": {
                "nbt": "{playerGameType:3}"
              }
            }
          }
        ],
        "items": [
          {
            "predicates": {
              "minecraft:custom_data": "{upgraded:1,transformable:1,staff:1,baffledhedgehog:1}"
            }
          }
        ]
      }
    }
  },
  "rewards": {
    "function": "lbc:swrg_kit_integration/gain/trinket"
  }
}

```

## Exact existing language values

These snapshots preserve original spelling and formatting input; reuse keys, no new translation aliases or templates needed.

### assets\minecraft\lang\en_us.json

```json

{
  "hedgehog_wand": "Hedgehog wand",
  "hedgehog_wand.lore.1": "Summons BaffledHedgehog,",
  "hedgehog_wand.lore.2": "who will deat damage to",
  "hedgehog_wand.lore.3": "everyone, who will disturb him",
  "hedgehog_wand.lore.4": "(except the owner)",
  "type": "Item type: ",
  "staff": "Magic wand",
  "magic_damage": " Magic Damage",
  "projectile_damage": " Projectile Damage",
  "mana_on_activate": " Mana cost to activate",
  "cast_count": " Magic missiles per cast",
  "cast_distance": " Maximum cast distance",
  "lbc.adv.staff.hedgehog": "Baffleds on baffled!",
  "lbc.adv.staff.hedgehog.lore": "Summon 40+ hedgehogs with the 'Hedgehog Rod' near you!",
  "upgrade_succesful_baffledhedgehog": "You can now create different types of hedgehogs! (Shift+RMB to toggle)",
  "upgrade_condition_baffledhedgehog": "This hedgehog staff contains an ancient evil... Or not evil... Something unknown. Release 'it' (Shift+RMB) and your staff will be cleansed!",
  "mode_default": "Wand Mode: Default",
  "mode_sniper": "Wand Mode: Long-Range Sniper. Turn camera down + RC to auto-aim.",
  "lbc.adv.upgraded_staff.hedgehog": "Buffled is the best waifu!",
  "lbc.adv.upgraded_staff.hedgehog.lore": "Create your own buffled army! Support, attack, ranged, damage absorbing and laser warriors!",
  "ender_spike": "Baffled's Chasing Spike (RMB)",
  "ender_spike.lore": "Randomly teleports the target to another dimension on hit",
  "blind_spike": "Blinding Beauty of Baffled (RMB)",
  "blind_spike.lore": "Your opponent will piss his face with boiling water when hit",
  "explosive_spike": "Explosive Buffle (RMB)",
  "explosive_spike.lore": "Explodes into multiple buffles",
  "spike": "Baffled Spike (RMB)",
  "spike.lore": "It pricks.",
  "wither_spike": "Buffled's Wither (RMB)",
  "wither_spike.lore": "Your opponents will be unable to breathe air for a period of time",
  "poison_spike": "Buffled Miasma Spike (RMB)",
  "poison_spike.lore": "Target will lose sin factors upon hit.",
  "impulse_spike": "Impulsive utterances of baffled (RMB)",
  "impulse_spike.lore": "Instantly hits target",
  "mode_healer": "Wand Mode: Hedgehog Healer",
  "mode_tank": "Wand mode: Hedgehog Tank",
  "mode_far": "Wand mode: Hedgehog Sniper",
  "mode_laser": "Wand Mode: Laser Hedgehog",
  "gui.locked": "Locked (Click for info)",
  "gui.for_open": "For unlock you need:",
  "hedgehog_apchheee": "'Hedgehog sneezed...'",
  "godfled_spawn": "GOD AWAKEN",
  "god": "God"
}

```

### assets\minecraft\lang\rpr.json

```json

{
  "hedgehog_wand": "Ежовый Жезл",
  "hedgehog_wand.lore.1": "Призывает BaffledHedgehog'а,",
  "hedgehog_wand.lore.2": "который будет наносить урон",
  "hedgehog_wand.lore.3": "всем, кто к нему приблизится",
  "hedgehog_wand.lore.4": "(кроме владельца)",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "projectile_damage": "Урон снарядом",
  "mana_on_activate": " Расход маны на активацию",
  "cast_count": "Магических снарядов за использование",
  "cast_distance": "Максимальная дистанция использования",
  "lbc.adv.staff.hedgehog": "Баффледы на баффледе!",
  "lbc.adv.staff.hedgehog.lore": "Призовите более 40 ежей с помощью 'Ежового жезла' около вас!",
  "upgrade_succesful_baffledhedgehog": "Теперь вы можете создавать разные виды ежей! (Shift+ПКМ для переключения)",
  "upgrade_condition_baffledhedgehog": "Внутри этого ежового жезла хранится древнее зло... Или не зло... Что-то неизвестное. Высвободите 'это' (Shift+ПКМ), и ваш жезл очистится!",
  "mode_default": "Режим жезла: Стандартный",
  "mode_sniper": "Режим жезла: Дальнобойный разряд. Смотрите ВНИЗ + ПКМ для наводки.",
  "lbc.adv.upgraded_staff.hedgehog": "Баффлед - лучшая вайфу!",
  "lbc.adv.upgraded_staff.hedgehog.lore": "Улучшите ежовый жезл в академии. Создайте свою армию баффледов! Воины поддержки, атакующие, дальнобойные, поглощающие урон и лазерные!",
  "ender_spike": "Прогоняющая колкость бафледа (ПКМ)",
  "ender_spike.lore": "Случайно телепортирует цель в другое измерение про попадании",
  "blind_spike": "Ослепляющая красота бафледа (ПКМ)",
  "blind_spike.lore": "Ваш противник обоссыт себе ебало кипятком при попадании",
  "explosive_spike": "Разрывной бафлед (ПКМ)",
  "explosive_spike.lore": "Взрывается на множество бафледов",
  "spike": "Колючка бафледа (ПКМ)",
  "spike.lore": "Колется.",
  "wither_spike": "Душный приём бафледа (ПКМ)",
  "wither_spike.lore": "Ваши противники не смогут дышать воздухом некоторое время",
  "poison_spike": "Шип с миазмами бафледа (ПКМ)",
  "poison_spike.lore": "Цель потеряет факторы греха при попадании.",
  "impulse_spike": "Импульсивные высказывания бафледа (ПКМ)",
  "impulse_spike.lore": "Мгновенно попадают в цель",
  "mode_healer": "Режим жезла: Еж-Целитель",
  "mode_tank": "Режим жезла: Еж-Танк",
  "mode_far": "Режим жезла: Еж-Снайпер",
  "mode_laser": "Режим жезла: Лазерный Ёж",
  "gui.locked": "Заблокировано (Нажми для информации)",
  "gui.for_open": "Для разблокировки выполните:",
  "hedgehog_apchheee": "'Ёжик чихнул...'",
  "godfled_spawn": "БОГ ВОЗРОЖДЁН",
  "god": "Бог"
}

```

### assets\minecraft\lang\ru_ru.json

```json

{
  "hedgehog_wand": "Ежовый Жезл",
  "hedgehog_wand.lore.1": "Призывает BaffledHedgehog'а,",
  "hedgehog_wand.lore.2": "который будет наносить урон",
  "hedgehog_wand.lore.3": "всем, кто к нему приблизится",
  "hedgehog_wand.lore.4": "(кроме владельца)",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "projectile_damage": "Урон снарядом",
  "mana_on_activate": " Расход маны на активацию",
  "cast_count": "Магических снарядов за использование",
  "cast_distance": "Максимальная дистанция использования",
  "lbc.adv.staff.hedgehog": "Баффледы на баффледе!",
  "lbc.adv.staff.hedgehog.lore": "Призовите более 40 ежей с помощью 'Ежового жезла' около вас!",
  "upgrade_succesful_baffledhedgehog": "Теперь вы можете создавать разные виды ежей! (Shift+ПКМ для переключения)",
  "upgrade_condition_baffledhedgehog": "Внутри этого ежового жезла хранится древнее зло... Или не зло... Что-то неизвестное. Высвободите 'это' (Shift+ПКМ), и ваш жезл очистится!",
  "mode_default": "Режим жезла: Стандартный",
  "mode_sniper": "Режим жезла: Дальнобойный разряд. Смотрите ВНИЗ + ПКМ для наводки.",
  "lbc.adv.upgraded_staff.hedgehog": "Баффлед - лучшая вайфу!",
  "lbc.adv.upgraded_staff.hedgehog.lore": "Улучшите ежовый жезл в академии. Создайте свою армию баффледов! Воины поддержки, атакующие, дальнобойные, поглощающие урон и лазерные!",
  "ender_spike": "Прогоняющая колкость бафледа (ПКМ)",
  "ender_spike.lore": "Случайно телепортирует цель в другое измерение про попадании",
  "blind_spike": "Ослепляющая красота бафледа (ПКМ)",
  "blind_spike.lore": "Ваш противник обоссыт себе ебало кипятком при попадании",
  "explosive_spike": "Разрывной бафлед (ПКМ)",
  "explosive_spike.lore": "Взрывается на множество бафледов",
  "spike": "Колючка бафледа (ПКМ)",
  "spike.lore": "Колется.",
  "wither_spike": "Душный приём бафледа (ПКМ)",
  "wither_spike.lore": "Ваши противники не смогут дышать воздухом некоторое время",
  "poison_spike": "Шип с миазмами бафледа (ПКМ)",
  "poison_spike.lore": "Цель потеряет факторы греха при попадании.",
  "impulse_spike": "Импульсивные высказывания бафледа (ПКМ)",
  "impulse_spike.lore": "Мгновенно попадают в цель",
  "mode_healer": "Режим жезла: Еж-Целитель",
  "mode_tank": "Режим жезла: Еж-Танк",
  "mode_far": "Режим жезла: Еж-Снайпер",
  "mode_laser": "Режим жезла: Лазерный Ёж",
  "gui.locked": "Заблокировано (Нажми для информации)",
  "gui.for_open": "Для разблокировки выполните:",
  "hedgehog_apchheee": "'Ёжик чихнул...'",
  "godfled_spawn": "БОГ ВОЗРОЖДЁН",
  "god": "Бог"
}

```

## Model/texture dependency verification

- `assets/lbc/items/god.json`: present

- `assets/lbc/models/item/god.json`: present

- `assets/minecraft/models/item/generated.json`: **MISSING**

- `assets/minecraft/textures/item/god.png`: present

- `assets/lbc/items/hedgehogwand.json`: present

- `assets/lbc/models/item/hedgehogwand.json`: present

- `assets/minecraft/models/item/handheld.json`: **MISSING**

- `assets/minecraft/textures/item/hedgehogwand.png`: present

- `assets/lbc/items/spike.json`: present

- `assets/lbc/models/item/spike.json`: present

- `assets/minecraft/textures/item/spike.png`: present

- `assets/lbc/items/spike_blind.json`: present

- `assets/lbc/models/item/spike_blind.json`: present

- `assets/minecraft/textures/item/blind_spike.png`: present

- `assets/lbc/items/spike_ender.json`: present

- `assets/lbc/models/item/spike_ender.json`: present

- `assets/minecraft/textures/item/ender_spike.png`: present

- `assets/lbc/items/spike_explosive.json`: present

- `assets/lbc/models/item/spike_explosive.json`: present

- `assets/minecraft/textures/item/explosive_spike.png`: present

- `assets/lbc/items/spike_impulse.json`: present

- `assets/lbc/models/item/spike_impulse.json`: present

- `assets/minecraft/textures/item/impulse_spike.png`: present

- `assets/lbc/items/spike_poison.json`: present

- `assets/lbc/models/item/spike_poison.json`: present

- `assets/minecraft/textures/item/poison_spike.png`: present

- `assets/lbc/items/spike_wither.json`: present

- `assets/lbc/models/item/spike_wither.json`: present

- `assets/minecraft/textures/item/spike_wither.png`: present

## Exact sound and presentation commands

Each line includes the complete source selector/range, category, position, volume, pitch and minimum volume where supplied. Missing radius means the source selector has no radius bound; do not substitute a distance-limited audience.

### lbc/function/items/trinket_effects/hedgehog_gauntlet_1.mcfunction

```mcfunction

1: playsound entity.silverfish.death player @a ~ ~ ~ 1 1

```

### lbc/function/items/trinket_effects/hedgehog_gauntlet_spike_damage.mcfunction

```mcfunction

7: playsound entity.zombie.break_wooden_door block @a ~ ~ ~ 1 1

```

### lbc/function/other/baffledend/hedgehogrider.mcfunction

```mcfunction

1: summon minecraft:silverfish ~ ~ ~ {CustomNameVisible:0b,Passengers:[{id:"minecraft:wither_skeleton",equipment:{mainhand:{id:"minecraft:iron_axe",components:{"minecraft:custom_name":{"italic":false,"translate":"hammer"},"minecraft:lore":[[{"translate":"type","color":"aqua","italic":false},{"translate":"axe","color":"aqua","italic":false}],{"translate":"hammer.lore","color":"white","italic":false}]},count:1}},drop_chances:{mainhand:1.0f}}],CustomName:"BaffledHegdehog"}

3: playsound minecraft:block.fire.extinguish master @a ~ ~ ~ 0.5 1

```

### lbc/function/other/baffledwall/tick_rc.mcfunction

```mcfunction

2: playsound minecraft:entity.silverfish.hurt master @a ~ ~ ~ 1 1

```

### lbc/function/other/baffledwall/tick_rc_pl.mcfunction

```mcfunction

2: playsound minecraft:entity.silverfish.hurt master @a ~ ~ ~ 1 1

```

### lbc/function/other/baffledwall/tick_rc_pl_heal.mcfunction

```mcfunction

2: playsound minecraft:entity.silverfish.hurt master @a ~ ~ ~ 1 1

```

### lbc/function/other/baffledwall/tick_rc_pl_laser.mcfunction

```mcfunction

2: playsound minecraft:entity.silverfish.hurt master @a ~ ~ ~ 1 1

```

### lbc/function/other/baffledwall/tick_rc_pl_sniper.mcfunction

```mcfunction

2: playsound minecraft:entity.silverfish.hurt master @a ~ ~ ~ 1 1

3: playsound minecraft:item.trident.return master @a ~ ~ ~ 5 1

```

### lbc/function/other/god_hedgehog/hedgegod_eater_boom.mcfunction

```mcfunction

1: playsound lbcsounds.energy_3 master @a ~ ~ ~ 1 1

2: playsound lbcsounds.energy_3 master @a ~ ~ ~ 1 1.5

3: playsound lbcsounds.energy_3 master @a ~ ~ ~ 1 2

```

### lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction

```mcfunction

4: #tellraw @a {"score":{"name":"@s","objective":"Distance"}}

```

### lbc/function/other/god_hedgehog/hedgegod_white_arrow_shoot.mcfunction

```mcfunction

1: playsound block.beacon.power_select master @a ~ ~ ~ 2 2

```

### lbc/function/other/god_hedgehog/laser_rc.mcfunction

```mcfunction

2: summon tnt ~ ~ ~ {fuse:0,explosion_power:10,CustomName:{"translate":"godbam"}}

```

### lbc/function/other/god_hedgehog/spawn_dim_scrap.mcfunction

```mcfunction

3: execute positioned ^ ^ ^10 run playsound lbcsounds.energy_2 master @a ~ ~ ~ 1 1 0.5

```

### lbc/function/other/god_hedgehog/spawn_white_arrows1at.mcfunction

```mcfunction

3: playsound block.beacon.activate master @a ~ ~ ~ 0.5 2

```

### lbc/function/other/magic_academy/can_upgrade/baffledhedgehog.mcfunction

```mcfunction

1: tellraw @s {"translate":"upgrade_condition_baffledhedgehog","color":"yellow"}

```

### lbc/function/other/magic_academy/upgrade/baffledhedgehog.mcfunction

```mcfunction

1: tellraw @s {"translate":"upgrade_succesful_baffledhedgehog","color":"yellow"}

```

### lbc/function/swrg_kit_integration/game/skills/falling_hedgehog_at.mcfunction

```mcfunction

4: execute at @s run playsound minecraft:entity.firework_rocket.large_blast master @a ~ ~ ~ 7 0.8

```

### lbc/function/swrg_kit_integration/game/skills/falling_hedgehog_explode.mcfunction

```mcfunction

1: summon minecraft:creeper ~ ~ ~ {Fuse:0s,ignited:1b,Silent:1b,ExplosionRadius:7b,CustomName:{"translate":"angry_baffled"}}

```

### lbcr/function/megahedgehog_spawn.mcfunction

```mcfunction

2: summon silverfish ~ ~ ~ {DeathLootTable:"lbc:god",CustomNameVisible:1b,PersistenceRequired:1b,Health:2147483647f,Tags:["lucky_mob","god_hedgehog"],CustomName:[{"color":"#FF0000","text":"𝕬"},{"color":"#FF1800","text":"𝕽"},{"color":"#FF2F00","text":"𝕮"},{"color":"#FF4700","text":"𝕳"},{"color":"#FF5E00","text":"𝕺"},{"color":"#FF7600","text":"𝕹"},{"color":"#FF8E00","text":"𝕱"},{"color":"#FFA500","text":"𝕷"},{"color":"#FFB200","text":"𝕰"},{"color":"#FFBB00","text":"𝕯"},{"color":"#FFC400","text":"𝕲"},{"color":"#FFCE00","text":"𝕺"},{"color":"#FFD700","text":"𝕯"},{"color":"#FFE100","text":"𝕲"},{"color":"#FFEA00","text":"𝕰"},{"color":"#EAEF08","text":"𝕳"},{"color":"#C6F215","text":"𝕺"},{"color":"#A3F422","text":"𝕲"}],attributes:[{id:"minecraft:armor",base:20},{id:"minecraft:armor_toughness",base:20},{id:"minecraft:max_health",base:2147483647},{id:"minecraft:scale",base:16}]}

3: playsound lbcsounds.flowey_laugh master @a ~ ~ ~ 1 1 1

4: playsound lbcsounds.flowey_laugh master @a ~ ~ ~ 1 1.05 1

5: playsound lbcsounds.flowey_laugh master @a ~ ~ ~ 1 1.1 1

6: playsound lbcsounds.flowey_laugh master @a ~ ~ ~ 1 0.95 1

8: title @a title {"translate":"godfled_spawn","color":"dark_red"}

9: title @a subtitle {"translate":"ЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖЖ","color":"dark_red","obfuscated":true}

```

### lbcr/function/raycast_end9.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {NoGravity:0b,CustomName:"BaffledHedgehog",NoAI:1b,Invulnerable:0b,Tags:["hedgehog","lucky_mob","usetimer"]}

```

### lbcr/function/raycast_end9_mode_default.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {NoGravity:0b,CustomName:"BaffledHedgehog",NoAI:1b,Invulnerable:0b,Tags:["hedgehog_default","lucky_mob","usetimer","hedgehog_upgraded"]}

```

### lbcr/function/raycast_end9_mode_healer.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {NoGravity:0b,CustomName:"HealingHedgehog",NoAI:1b,Invulnerable:0b,Tags:["hedgehog_healer","lucky_mob","usetimer","hedgehog_upgraded"]}

```

### lbcr/function/raycast_end9_mode_laser.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {NoGravity:0b,CustomName:"LaseringHedgehog",NoAI:1b,Invulnerable:0b,Tags:["hedgehog_laser","lucky_mob","hedgehog_upgraded"]}

```

### lbcr/function/raycast_end9_mode_sniper.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {NoGravity:0b,CustomName:"SniperHedgehog",NoAI:1b,Invulnerable:0b,Tags:["hedgehog_sniper","lucky_mob","usetimer","hedgehog_upgraded"]}

```

### lbcr/function/raycast_end9_mode_tank.mcfunction

```mcfunction

1: playsound minecraft:entity.silverfish.ambient master @a ~ ~ ~ 1 1.3

2: summon minecraft:silverfish ~ ~ ~ {CustomName:"HeavyHedgehog",Tags:["hedgehog_tank","hedgehog_upgraded"],attributes:[{id:"minecraft:knockback_resistance",base:1000},{id:"minecraft:attack_knockback",base:1},{id:"minecraft:max_health",base:100},{id:"minecraft:attack_damage",base:15}],Health:100.0f}

```

### lbcr/function/raycast_loop9cd_upgrade.mcfunction

```mcfunction

1: summon minecraft:silverfish ^1 ^ ^1 {Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,active_effects:[{id:"minecraft:slow_falling",duration:-1,show_particles:0b,amplifier:1b}],DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}]}]}]}]}]}]}]}]}]}]}]}]}

2: summon minecraft:silverfish ^2 ^ ^1 {Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,active_effects:[{id:"minecraft:slow_falling",duration:-1,show_particles:0b,amplifier:1b}],DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}]}]}]}]}]}]}]}]}]}]}]}]}

3: summon minecraft:silverfish ^-1 ^ ^1 {Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,active_effects:[{id:"minecraft:slow_falling",duration:-1,show_particles:0b,amplifier:1b}],DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}]}]}]}]}]}]}]}]}]}]}]}]}

4: summon minecraft:silverfish ^-2 ^ ^1 {Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,active_effects:[{id:"minecraft:slow_falling",duration:-1,show_particles:0b,amplifier:1b}],DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}]}]}]}]}]}]}]}]}]}]}]}]}

5: summon minecraft:silverfish ^ ^ ^1 {Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,active_effects:[{id:"minecraft:slow_falling",duration:-1,show_particles:0b,amplifier:1b}],DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}],Passengers:[{id:"minecraft:silverfish",Tags:["baffled_wall","lucky_mob"],PersistenceRequired:1b,DeathLootTable:"lbc:hedgehog_wall",Health:30.0f,CustomName:{"color":"dark_red","italic":false,"translate":"grand_baffled_wall"},attributes:[{id:"minecraft:max_health",base:30},{id:"minecraft:follow_range",base:200},{id:"minecraft:knockback_resistance",base:100},{id:"minecraft:attack_damage",base:10},{id:"minecraft:armor",base:10},{id:"minecraft:armor_toughness",base:10}]}]}]}]}]}]}]}]}]}]}]}]}]}

6: tellraw @a {"translate":"baffled_awakened","color":"dark_red"}

```

### lbcr/function/raycast_loop9cd_upgraded.mcfunction

```mcfunction

2: execute if predicate lbc:sneak if score #mode lbc.math matches 1 run tellraw @s [{"translate":"mode_default"}]

3: execute if predicate lbc:sneak if score #mode lbc.math matches 2 run tellraw @s [{"translate":"mode_healer"}]

4: execute if predicate lbc:sneak if score #mode lbc.math matches 3 run tellraw @s [{"translate":"mode_tank"}]

5: execute if predicate lbc:sneak if score #mode lbc.math matches 4 run tellraw @s [{"translate":"mode_far"}]

6: execute if predicate lbc:sneak if score #mode lbc.math matches 5 run tellraw @s [{"translate":"mode_laser"}]

```

### lbcr/function/raycast_loop9cd_upgraded_left.mcfunction

```mcfunction

2: execute if predicate lbc:sneak if score #mode lbc.math matches 1 run tellraw @s [{"translate":"mode_default"}]

3: execute if predicate lbc:sneak if score #mode lbc.math matches 2 run tellraw @s [{"translate":"mode_healer"}]

4: execute if predicate lbc:sneak if score #mode lbc.math matches 3 run tellraw @s [{"translate":"mode_tank"}]

5: execute if predicate lbc:sneak if score #mode lbc.math matches 4 run tellraw @s [{"translate":"mode_far"}]

6: execute if predicate lbc:sneak if score #mode lbc.math matches 5 run tellraw @s [{"translate":"mode_laser"}]

```

## Custom sound assets

### lbcsounds.energy_2

```json

{
  "category": "master",
  "sounds": [
    {
      "name": "lbcsounds/energy_2",
      "stream": true
    }
  ]
}

```

- `assets/minecraft/sounds/lbcsounds/energy_2.ogg`: True

### lbcsounds.energy_3

```json

{
  "category": "master",
  "sounds": [
    {
      "name": "lbcsounds/energy_3",
      "stream": true
    }
  ]
}

```

- `assets/minecraft/sounds/lbcsounds/energy_3.ogg`: True

### lbcsounds.flowey_laugh

```json

{
  "category": "master",
  "sounds": [
    {
      "name": "lbcsounds/flowey_laugh",
      "stream": true
    }
  ]
}

```

- `assets/minecraft/sounds/lbcsounds/flowey_laugh.ogg`: True

## Remaining implementation dependencies

Seven spikes are special snowball conversions in lbc/function/projectiles/specials.mcfunction: confuse raid_omen115, blind116 duration6000, poison117, boom118, wither water_breathing amplifier1 duration600, standard physical arrow damage10, impulse raycast_spike. Effects require native legacy-effect behavior, owner/team attribution, role/Greed protection and queued consequences; colored arrows alone are insufficient. See source specials and effect dispatch for exact damage and payloads.

Staff/helper ordinary and Academy selectors/loop presentation are audited in SHEEP_HEDGEHOG_SOURCE.md. Source helper names and boss multicomponent gradient names above must be native components. No custom helper entity texture/model is required by the source summon chain: vanilla silverfish with names, particles/beams and boss scale16.

Native reusable foundations: used-stack Academy trials/promotion, native snowball/projectile interception, actor/indirect-kill ownership, roles/Greed/observer guards, Time Stop, stop cleanup, uncapped max health and existing Silence boss immunity. Reuse foundations without treating them as completion of Hedgehog behavior.

Audit establishes disk presence only, not client loaded-pack state, rendered pixels, playback or runtime behavior.

## Authorized native item/resource implementation

Added `LbcHedgehogItems`: WAND (`skywars2:hedgehog_wand`), ACADEMY (`skywars2:academy_hedgehog_wand`), GOD (`skywars2:god`), seven SpikeKind entries with native snowball identities and source custom-data flags. Wands and spikes dispatch through the shared Wands use path; root integration supplies their behavior branches. God returns PASS and remains an inert trophy. Source lore/design comes from narrow loot leaves.

Added `tools/import_lbc_hedgehog.py`, ten narrow ported leaves and native `lbc:hedgehog_wall` / `lbc:god` aliases. Academy uses common source add_upgrade_desc plus its own tiers/s modifier, retaining source staff model/name/lore and marking upgraded1/mode1/upgradable0/upgrade_available1. Added twelve explicit build resource includes. Verified all twelve JSON payloads parse, generator is idempotent and migrated reward aliases equal the complete source payload except native item IDs; seven equal weights remain. No build/server/commit was run by this worker. Native behavior and runtime readiness are pending root integration/qualification.

## Authorized real dimension destinations

User confirmed actual world transitions and arena's logical overworld role. `LbcHedgehogDimensions` captures the source dimension once and selects one of four destination branches, avoiding the legacy sequential predicate reevaluation bug. Nether/End/Imprinted source roles each exclude themselves; Nexus retains its source no-op because teleport_nahui has no Nexus handler. Source execute-in coordinate scaling is retained; logical overworld arrivals resolve to the isolated match arena. Nexus uses independent integer x/z[-4800,4800], y2332 and removes minecraft:antigravity_effect from the actual arriving entity. Its authored build range remains0..2031; vanilla teleport spawnable bounds permit y2332 above build height. No invented safe-surface placement or collision avoidance.

Native transitions preserve angles; normal non-gliding vertical velocity is zeroed and navigation stopped like TeleportCommand. Mob dimension changes use the actual cloned arrival and record caster-owned indirect fall/arrival hazards on that new object. Player flags/Greed/roles and ally-player protection remain; friendly mobs are allowed on native accepted impacts.

Added six minimally migrated Nexus/Imprinted dimension/type/biome JSON resources and explicit build includes. Migration uses the inspected local26.3 DimensionType/Biome/EnvironmentAttribute codecs: required dragonfightfalse, legacy skybox/cardinal rendering role, overworld clock/timelines, source colors converted to RGB strings, removed biome audio/sky/fog/spawn fields moved to current attributes. Empty Nexus zero-height air layer becomes empty layers, same generated void. Imprinted retains four bedrock layers at y-16..-13 and air at-12. Source terrain/structures from the old saved worlds are not copied.

`HedgehogDimensionChecks.run` / `check hedgehog-dimensions` qualifies actual native player transfers through all five logical roles, scaling/angles, Imprinted generated layers, Nexus RTP/gravity/no-op, protected roles and real mob replacement. Root owns compilation/runtime qualification. Six JSON parse/idempotency checks passed locally; no worker build/server/commit.

Core participant validity is already dimension-independent, and respawn/lobby returns players to the arena. Existing GameController border damage passed maps.level as its damage context even for off-world players; root was advised to use player.level. Existing individually arena-bound abilities may clean up on owner departure; no broad crossworld ability rewrite was undertaken. Lobby entity cleanup intentionally sweeps all worlds at match end.
