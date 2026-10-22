# Shooter pools acquisition source audit - 21 October 2026

Read-only source audit; no Java, resources, source server or RP edits. This new note contains no replaced historical/user notes. AGENTS.md, ARCHITECTURE.md and agent_notes/AGENTS.md apply. Existing item-specific user contracts remain authoritative (Mobgun 2.5s; minigun 2.5 damage/native movement penalties; TECH helicopters; shield and Govnomet decisions).

## Exact recursive contract

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Every path below is relative to that root. Ordinary `lbc/loot_table/transformables_shooter.json` has one pool, one roll, eight entries, no pool condition. All seven weapon types are carrot_on_a_stick, count1. Entry indices zero-based:

| Index | Output | Weight | Quality | Condition |
|---|---|---:|---:|---|
|0|phantomknifes|10|9|none|
|1|mobgun|10|0|none|
|2|mobgun, identical item/components to index1|10000|0|fixed score holder final_luck_modifier, objective lbc.math, exactly 7|
|3|kfcgun|10|0|none|
|4|lbc:razorpine_with_leaves|10|0|none|
|5|attack_helicopter|10|8|none|
|6|laser_minigun/1|10|8|none|
|7|woodificator|10|8|none|

Quality must remain attached to entries (Minecraft effective loot weight is based on context Luck; fixed final_luck_modifier is an independent eligibility test). At zero context Luck outside score7, each seven normal leaves weighs10; at score7 Mobgun aggregate weight10010/10070. Do not deduplicate the two Mobgun entries or interpret range7 as >=7. Their legacy lore3.5 is superseded by the user 2.5 clarification.

Expanded `lbc/loot_table/transformables_shooter_all.json`: one roll; ordinary table weight7; stalin1; direct Magnum1; tuntijaa_terra_razor1; direct Govnomet1; helicopter_boss default1; slr default1. Total13. No expanded branch conditions/quality. Do not flatten nested weights: ordinary occupies7/13 then rolls its own weighted pool.

Complete recursive nested tables (no further table dependencies):

- `lbc/loot_table/razorpine_with_leaves.json`: pool0 weapon razorpine weight10 quality0 count1, pool1 wheat_seeds count16. Preserve both outputs.
- `lbc/loot_table/stalin.json`: AK47 count1, infinity1, tier_s; this expanded acquisition is infinite AK, not finite AK.
- `lbc/loot_table/tuntijaa_terra_razor.json`: razorterra count1 tier_s, lore magic_damage6.
- `lbc/loot_table/helicopter_boss.json`: boss_helicopter count1 tier_s cooldown lore30.
- `lbc/loot_table/slr.json`: slr mode1 count1 tier_s; mainhand base_attack_damage +8 and base_attack_speed -2 add_value.

Direct Magnum: tier_ss, mode0, raycastspell1. Govnomet: tier_d, govnoq1/craftusable1/magicwand1. All weapons have transformable1/shooter1. Minigun source adds mainhand/offhand attack_speed -1 add_multiplied_total, hidden tooltip; current user-approved item implementation supersedes source stats/lore. Exact complete JSON of each reachable table is appended below, preserving every component, condition, name and lore key for implementation review.

## Callers and lifecycle

- `lbc/function/reforge.mcfunction:8`: shooter1 => spawn ordinary at item position; each matching category independently rolls; kills count1, otherwise reduce_count_s recurses until stack drained.
- `lbc/function/ultra_reforge.mcfunction:8`: shooter1 => expanded; same independent categories and recursive count consumption via reduce_count. Also exact64 arrows => magnum_ammo (separate acquisition, not a shooter-pool dependency).
- `lbc/function/workingitems/awaken_tuntija_wands/infinite_bolt/end.mcfunction`: radius1 transformable ground items plus exact64 arrows => Ultra.
- `lbc/function/other/craft/crafted_reforge.mcfunction`: nearest limit1 transformable item at distance0.001..1 => Ultra, enchant feedback; chance30 consumes invoking tiny teleport item. `other/craft_ground.mcfunction:63` requires stteleport_tiny and a nearby transformable with Thrower before calling it.
- `lbc/function/other/craft/reroll_machine.mcfunction`: each transformable player.crafting.0..3 calls corresponding reforge_crafting_0..3 (line8 ordinary shooter); 5000 mana per processed slot, count_reduce once per matching slot.
- `lbc/function/items_tick_act.mcfunction:4` => other/transformpre for entity tag transformable. transformpre sets item Health1000000 and lightning within4 invokes other/transform; ordinary output given nearest nonspectator; reduce_count_l recursively drains stack.
- Loot callers: `lbc/loot_table/27_shooters.json` and `random_item.json` => expanded; `lucky_items.json` and `lucky_items_high_quality.json` => ordinary (including conditional duplicate entries in those outer tables; audit those broader lucky pools separately).
- No literal playeritem identifier exists under this legacy data root. Native/context players and actual ground/crafting entities must be distinguished from a claimed playeritem feature.

## Native observed implementation versus remaining scope

`src/main/java/me/skywars2/game/LbcBolts.java:54` NORMAL_REFORGE / :77 ULTRA_REFORGE route shooter to exact names; :386 independent category collection; :394 radius1 sphere snapshots inputs; :413 rolls tables with ground item as THIS_ENTITY. It drains each source count, emits all nested outputs, preserves unmatched/empty outputs, awards owner, queues bounded work (8 units/1ms), pauses and lifecycle-cancels via owned generation/epoch. New outputs cannot be captured by the same cast. `LbcLoadouts.java:396` invokes native loot registry with COMMAND context, source and origin, no explicit Luck value (zero default). `rollTable` syncs ranked only; it does not set final_luck_modifier.

Current pool resources preserve all source leaves, including exact7 condition converted to `condition` / minecraft:int_value_check / test7. They still produce legacy carrier weapons unless promoted/replaced by native item modifiers; do not equate handler support with native pool emission. Existing qualified handlers cover Phantom, Mobgun, KFC/Razorpine/Terra, Woodificator, helicopters/minigun, AK/Magnum/SLR/Govnomet; per-item port notes own behavior evidence. This audit does not claim new runtime validation.

No native equivalent was located for generic lightning-item transformation, reroll_machine crafting-grid reroll, or tiny-teleport ground Ultra recipe (no corresponding identifiers/calls in src/main/java). LbcCraftingRecipe has tiny-teleport donor handling for other recipes; that alone is not proof of ground Ultra support. Treat these as explicitly separate mechanic gaps until a broader caller audit proves coverage. Ordinary/expanded pool promotion must preserve presentation plus native item identity and all extra outputs, without enabling unrelated unsupported mechanics.

Author clarification recommended before changing acquisition balance: retain the strange exact-luck7 weight10000 Mobgun boost (almost guaranteed ordinary Mobgun), or was this a source debug/special-luck artifact? Also determine intended priority/scope of source reroll machine, lightning-item and tiny teleport ground mechanics; this audit does not silently authorize implementing their bugs.

## Complete source tables

### `lbc/loot_table/transformables_shooter_all.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:loot_table",
          "value": "lbc:transformables_shooter",
          "weight": 7
        },
        {
          "type": "minecraft:loot_table",
          "value": "lbc:stalin",
          "weight": 1
        },
        {
          "type": "minecraft:item",
          "weight": 1,
          "name": "minecraft:carrot_on_a_stick",
          "functions": [
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_ss",
                "minecraft:item_model": "lbc:magnum",
                "minecraft:custom_name": {
                  "italic": false,
                  "translate": "magnum_mode_standard"
                },
                "minecraft:lore": [
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
                    "translate": "magnum_lore_1",
                    "italic": false,
                    "color": "white"
                  },
                  {
                    "translate": "magnum_lore_2",
                    "italic": false,
                    "color": "white"
                  },
                  {
                    "translate": "magnum_lore_3",
                    "italic": false,
                    "color": "aqua"
                  }
                ]
              }
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magnum:1,mode:0,raycastspell:1}"
            }
          ]
        },
        {
          "type": "minecraft:loot_table",
          "value": "lbc:tuntijaa_terra_razor",
          "weight": 1
        },
        {
          "type": "minecraft:item",
          "weight": 1,
          "name": "minecraft:carrot_on_a_stick",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 1
            },
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_d",
                "minecraft:item_model": "lbc:govnomet"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "shit_cannon",
                  "italic": false
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
                  "translate": "shit_cannon.lore",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "shitgun.lore",
                  "color": "white",
                  "italic": false
                },
                [
                  {
                    "text": "Nightmare_23",
                    "color": "green",
                    "italic": false,
                    "type": "text"
                  },
                  {
                    "text": " Beta Server Harem event",
                    "color": "dark_gray",
                    "italic": false,
                    "type": "text"
                  }
                ]
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{govnoq:1,transformable:1,shooter:1,magicwand:1,craftusable:1,govnomet:1}"
            }
          ]
        },
        {
          "type": "minecraft:loot_table",
          "value": "lbc:helicopter_boss"
        },
        {
          "type": "minecraft:loot_table",
          "value": "lbc:slr"
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/transformables_shooter.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
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
        },
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
                "minecraft:tooltip_style": "lbc:tier_d",
                "minecraft:item_model": "lbc:mobgun"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "mob_gun",
                  "italic": false
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
                [
                  {
                    "text": "3.5 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "recharge_time",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "mob_gun.lore",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,spellwithcooldown:1,magicwand:1,mobgun:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "weight": 10000,
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
                "minecraft:tooltip_style": "lbc:tier_d",
                "minecraft:item_model": "lbc:mobgun"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "mob_gun",
                  "italic": false
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
                [
                  {
                    "text": "3.5 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "recharge_time",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "mob_gun.lore",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,spellwithcooldown:1,magicwand:1,mobgun:1}"
            }
          ],
          "conditions": [
            {
              "condition": "minecraft:value_check",
              "value": {
                "type": "minecraft:score",
                "target": {
                  "type": "minecraft:fixed",
                  "name": "final_luck_modifier"
                },
                "score": "lbc.math",
                "scale": 1
              },
              "range": 7
            }
          ]
        },
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
                "minecraft:item_model": "lbc:kfcgun"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "kfc_gun",
                  "italic": false
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
                [
                  {
                    "text": "1 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "recharge_time",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "kfc_gun.lore.1",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "kfc_gun.lore.2",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magicwand:1,kfcgun:1}"
            }
          ]
        },
        {
          "type": "minecraft:loot_table",
          "weight": 10,
          "quality": 0,
          "value": "lbc:razorpine_with_leaves"
        },
        {
          "type": "minecraft:item",
          "weight": 10,
          "quality": 8,
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
                "minecraft:item_model": "lbc:attack_helicopter"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "attack_helicopter",
                  "color": "green",
                  "italic": false
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
                [
                  {
                    "text": "30 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "recharge_time",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "attack_helicopter.lore.1",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "attack_helicopter.lore.2",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "attack_helicopter.lore.3",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,spellwithcooldown:1,magicwand:1,attack_helicopter:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "weight": 10,
          "quality": 8,
          "name": "minecraft:carrot_on_a_stick",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 1
            },
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_a",
                "minecraft:item_model": "lbc:laser_minigun/1",
                "minecraft:attribute_modifiers": [
                  {
                    "type": "minecraft:attack_speed",
                    "slot": "mainhand",
                    "id": "generijfyjyjted",
                    "amount": -1,
                    "operation": "add_multiplied_total"
                  },
                  {
                    "type": "minecraft:attack_speed",
                    "slot": "offhand",
                    "id": "gjcjgfcjhjgjgjed",
                    "amount": -1,
                    "operation": "add_multiplied_total"
                  }
                ],
                "tooltip_display": {
                  "hidden_components": [
                    "minecraft:attribute_modifiers"
                  ]
                }
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "laser_minigun",
                  "color": "aqua",
                  "italic": false
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
                    "text": "100 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "mana_cost",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "laser_minigun.lore",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magicwand:1,laser_minigun:1}"
            }
          ]
        },
        {
          "type": "minecraft:item",
          "weight": 10,
          "quality": 8,
          "name": "minecraft:carrot_on_a_stick",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 1
            },
            {
              "function": "minecraft:set_components",
              "components": {
                "minecraft:tooltip_style": "lbc:tier_d",
                "minecraft:item_model": "lbc:woodificator"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "woodificator",
                  "color": "white",
                  "italic": false
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
                  "translate": "woodificator.lore",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,raycastspell:1,woodificator:1}"
            }
          ]
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/razorpine_with_leaves.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
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
                "minecraft:tooltip_style": "lbc:tier_a",
                "minecraft:item_model": "lbc:razorpine"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "razorpine",
                  "italic": false
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
                  "translate": "razorpine.lore.2",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "razorpine.lore.3",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "razorpine.lore.4",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "razorpine.lore.1",
                  "color": "gold",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{craftusable:1,transformable:1,shooter:1,magicwand:1,razorpine:1}"
            }
          ]
        }
      ]
    },
    {
      "rolls": 1,
      "entries": [
        {
          "type": "item",
          "name": "wheat_seeds",
          "functions": [
            {
              "function": "set_count",
              "count": 16
            }
          ]
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/stalin.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
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
                "minecraft:item_model": "lbc:ak47",
                "minecraft:tooltip_style": "lbc:tier_s",
                "enchantments": {
                  "infinity": 1
                }
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "text": "AK-47",
                  "italic": false,
                  "type": "text"
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
                [
                  {
                    "text": "morgenshtern ",
                    "color": "green",
                    "italic": false,
                    "type": "text"
                  },
                  {
                    "text": "Beta Server Harem event",
                    "color": "dark_gray",
                    "italic": false,
                    "type": "text"
                  }
                ]
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magicwand:1,ak47:1}"
            }
          ]
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/tuntijaa_terra_razor.json`

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
                "minecraft:tooltip_style": "lbc:tier_s",
                "minecraft:item_model": "lbc:terra_razorpine"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "razorterra",
                  "italic": false,
                  "color": "green"
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
                    "translate": "shooter",
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
                {
                  "translate": "razorterra.lore",
                  "color": "white",
                  "italic": false
                }
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magicwand:1,razorterra:1}"
            }
          ]
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/helicopter_boss.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
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
                "minecraft:item_model": "lbc:helicopter_boss"
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "boss_helicopter",
                  "color": "white",
                  "italic": false
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
                [
                  {
                    "text": "30 ",
                    "italic": false,
                    "color": "aqua",
                    "type": "text"
                  },
                  {
                    "translate": "recharge_time",
                    "italic": false,
                    "color": "aqua"
                  }
                ],
                {
                  "translate": "boss_helicopter.lore.1",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "boss_helicopter.lore.2",
                  "color": "white",
                  "italic": false
                },
                {
                  "translate": "boss_helicopter.lore.3",
                  "color": "white",
                  "italic": false
                },
                [
                  {
                    "text": "Akim04ka",
                    "color": "green",
                    "italic": false,
                    "type": "text"
                  },
                  {
                    "text": " Beta Server Harem event",
                    "color": "dark_gray",
                    "italic": false,
                    "type": "text"
                  }
                ]
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,spellwithcooldown:1,magicwand:1,boss_helicopter:1}"
            }
          ]
        }
      ]
    }
  ]
}
```

### `lbc/loot_table/slr.json`

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
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
                "minecraft:tooltip_style": "lbc:tier_s",
                "minecraft:item_model": "lbc:super_rifle",
                "minecraft:attribute_modifiers": [
                  {
                    "type": "minecraft:attack_damage",
                    "slot": "mainhand",
                    "id": "minecraft:base_attack_damage",
                    "amount": 8,
                    "operation": "add_value"
                  },
                  {
                    "type": "minecraft:attack_speed",
                    "slot": "mainhand",
                    "id": "minecraft:base_attack_speed",
                    "amount": -2,
                    "operation": "add_value"
                  }
                ]
              }
            },
            {
              "function": "minecraft:set_name",
              "name": [
                {
                  "translate": "karich_rifle",
                  "italic": false
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
                  "translate": "karich_rifle.lore.1",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.2",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.3",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.4",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.5",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.6",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.7",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.8",
                  "italic": false,
                  "color": "white"
                },
                {
                  "translate": "karich_rifle.lore.9",
                  "italic": false,
                  "color": "white"
                },
                [
                  {
                    "text": "Daeris_ ",
                    "color": "green",
                    "italic": false,
                    "type": "text"
                  },
                  {
                    "text": "No Nut November 2023 Event",
                    "color": "dark_gray",
                    "italic": false,
                    "type": "text"
                  }
                ]
              ]
            },
            {
              "function": "minecraft:set_custom_data",
              "tag": "{transformable:1,shooter:1,magicwand:1,mode:1,slr:1}"
            }
          ]
        }
      ]
    }
  ]
}
```
