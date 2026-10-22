# Govnomet and Helicopter Boss resource audit ? 21 October 2026

Current implementation and resolved author decisions: [NEXT_SHOOTER_PORT](NEXT_SHOOTER_PORT.md).
The bounded source findings below are historical source observations; they do not override that contract.

Audit only; no implementation, source datapack, RP or template edits. Current RP is `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`; `RESOURCE_PACK.md` still names stale `26.3(1)`. User RP read-only and exact presentation requirements remain authoritative.

## Exact source payloads

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Govnomet is the inline weight-1 entry in `loot_table/transformables_shooter_all.json` (item_model line 85, custom_data line 141). Helicopter Boss is the separate `loot_table/helicopter_boss.json`, referenced at shooter_all line 147. Its entry weight is 10 within a one-entry, one-roll table. Shooter_all reference omits weight (default 1). Death drops this same table; there is no separate boss reward table.

Preserve all components/functions and array order below. Govnomet name intentionally has no explicit color; do not add white. Both attribution lore rows contain literal text, not translations. No extra attributes or lore should be added by the importer.

### Govnomet inline entry

```json
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
}
```

### Helicopter Boss complete table

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

## Resource checks

Read-only Python checks passed using existing `import_lbc_firearms.verify_model` (recursive parent/explicit texture resolution), `import_lbc_shooter_pair.translate_keys`, and `import_lbc_woodificator.RP`:

- All 11 required keys exist in all three current RP languages: `ru_ru`, `en_us`, `rpr`.
- `lbc:govnomet`, `lbc:helicopter_boss`, and `lbc:helicopter_1` through `_4` item definitions resolve to geometry, parents, and existing PNG textures. Four helicopter frames are shared with the already ported Attack Helicopter.
- Both `tier_d` and `tier_s` tooltip background/frame PNGs exist.
- `minecraft:lbcsounds.helicopter` resolves to `lbcsounds/helicopter.ogg` (stream true); `minecraft:lbcsounds.kazahstan` to `lbcsounds/kazahstan.ogg` (stream false); `minecraft:lbcsounds.drop_bomb` to `lbcsounds/drop_bomb.ogg` (stream false). All three actual OGGs exist.
- Other source sounds are vanilla: gravel.place, blaze.hurt, firework_rocket.blast. Govnomet uses brown vanilla dust (`color:[0.388,0.251,0.137],scale:1.0f`), count 1 normal and 4 mega; no extra projectile item model or custom texture.

### Exact existing translation values

These are evidence only; do not copy them into mod language files or recreate template additions.

`ru_ru` SHA-256 `3e7b3f2c6aa2e9e223db99d52d4d1a9de9954bd546bad5ba3543b775c7dfb47b`

```json
{
  "boss_helicopter": "Казахский Боевой Вертолёт",
  "boss_helicopter.lore.1": "Шкибиди доп доп ес ес ес",
  "boss_helicopter.lore.2": "Этого предмета точно не должно существовать.",
  "boss_helicopter.lore.3": "Стреляет непрерывным потоком дерьмища в цель!",
  "helicopter_boss": "АТАКУЮЩИЙ БОЕВОЙ ВЕРТОЛЁТ!",
  "recharge_time": "Время перезарядки (секунд)",
  "shit_cannon": "Говномёт",
  "shit_cannon.lore": "Стреляет говном",
  "shitgun.lore": "Стоит объединить с Электрокатаной!",
  "shooter": "Прочее стрелковое",
  "type": "Тип предмета: "
}
```

`en_us` SHA-256 `d81685d3f4c65c833543ee9ece5f48904d01ba8b8c04e9a2dd2176dadeaa8782`

```json
{
  "boss_helicopter": "Kazakh Combat Helicopter",
  "boss_helicopter.lore.1": "Shkibidi extra extra es es es",
  "boss_helicopter.lore.2": "This item definitely shouldn't exist.",
  "boss_helicopter.lore.3": "Shoots a continuous stream of shit at the target!",
  "helicopter_boss": "ATTACKING GUN HELICOPTER!",
  "recharge_time": " Recharge time (seconds)",
  "shit_cannon": "Shit thrower",
  "shit_cannon.lore": "Shoots shit",
  "shitgun.lore": "Worth combining with Electrokatana!",
  "shooter": "Other shooting",
  "type": "Item type: "
}
```

`rpr` SHA-256 `d6976e0779f458511485596879f46ee0e91de68504a5303964b4d0f4f76e338a`

```json
{
  "boss_helicopter": "Казахский Боевой Вертолёт",
  "boss_helicopter.lore.1": "Шкибиди доп доп ес ес ес",
  "boss_helicopter.lore.2": "Этого предмета точно не должно существовать.",
  "boss_helicopter.lore.3": "Стреляет непрерывным потоком дерьмища в цель!",
  "helicopter_boss": "АТАКУЮЩИЙ БОЕВОЙ ВЕРТОЛЁТ!",
  "recharge_time": "Время перезарядки (секунд)",
  "shit_cannon": "Говномёт",
  "shit_cannon.lore": "Стреляет говном",
  "shitgun.lore": "Стоит объединить с Электрокатаной!",
  "shooter": "Прочее стрелковое",
  "type": "Тип предмета: "
}
```

## Dependent functions and tables

Govnomet: `workingitems/govno`, `govno_default`, `govno_govnomen`, `govnoselect`; `raycastselector_player` dispatches to `lbcr:raycast_gavno` and `lbcr:raycast_gavno_mega`, with their `_pre`/hit helpers. Those ray functions contain dust/sound references, no additional loot table or custom model. The skill-9 mega path therefore needs no additional RP assets.

Boss: `drops/helicopter_boss_spawn` supplies `helicopter_boss` entity-name translation and first shared frame; `_spawn_2` links hitbox. `drops/helicopter_boss/tick` cycles all four shared frames; `shoot` uses vanilla arrows; `drop_bomb` summons vanilla TNT. `tick_phase_3` plays drop_bomb sound. `death` stops kazahstan/helicopter, creates TNT, then loot-spawns `lbc:helicopter_boss`; `drop_item` protects that stack. No ammunition/reward table dependency beyond the source table itself.

## Bounded helper plan

Reuse `stage_gun` pattern from `tools/import_lbc_firearms.py`: migrate source schema in memory, select exactly one inline carrot-on-a-stick entry by `govnomet:1`, deep-copy and substitute only the native item ID, then `build_single_entry_table`/`write_table` to `ported/govnomet.json`. For boss load `helicopter_boss.json`, migrate and substitute only its single native item ID, preserve full table and functions, write `ported/helicopter_boss.json`. Assert equality against the source after only explicit ID substitution. Reuse recursive model verifier and translator walk; check RU/EN/RPR, tier D/S and three custom sounds. Preserve a source-compatible boss table alias if native death uses legacy loot lookup, following existing firearm ammo alias convention; do not promote aggregate shooter_all merely for these entries. Native Polymer items should supply existing source item_model and tooltip styles unchanged. No RP/template changes are needed by this audit.

Server serialization and file dependency checks do not prove client rendering; visual parity is still a graphical-client check.
