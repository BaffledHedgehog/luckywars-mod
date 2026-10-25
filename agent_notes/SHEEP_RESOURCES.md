# Sheep wand resource audit

Static resource preparation only; no build, server run, or graphical client verification was performed. These leaves are not a claim of tested or ready behavior.

## Narrow loot leaves

- `lbc:ported/sheep_wand` issues only `skywars2:sheep_wand`; `lbc:ported/academy_sheep_wand` issues only `skywars2:academy_sheep_wand`. No broad pool was exposed or changed.
- Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json`, unique entry tagged `{transformable:1,staff:1,raycastspell:1,sheepwand:1}`. Edible rofl items are excluded.
- Converted only the entry ID and 26.3 `functions`/`function` names to `modifier`/`type`, following `ported/danmaku_staff.json`. Original count, components, name, all six lore rows, nested styles, score reference `sheep_wand_mana_use` / `lbc.math`, and custom-data flags are preserved. Pool weight/quality are intentionally not copied into the deterministic leaf.
- Academy follows the existing Academy leaf convention: append `upgradable:0,upgrade_available:1,upgraded:1,mode:1`, append the unchanged green nonitalic `upgraded_item` line, and apply `lbc:tier_x`. Source tier is `function/other/magic_academy/upgrade/sheepwand.mcfunction` -> `item_modifier/tiers/x.json`; generic markers/lore originate in `function/other/magic_academy/interacted_upgrade.mcfunction` and `item_modifier/add_upgrade_desc.json`.
- The actual author lore matches permanent mob transformation and excludes players. The earlier temporary/player-inventory interpretation was a terminal-reading error, withdrawn in SHEEP_DECISIONS.md. User choices retain the male-hole branch and replace its visual/physics/terrain implementation. No lore rewrite is needed.

## Author resource pack (read only)

Root: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.

- `assets/lbc/items/sheepwand.json`: model type `model`, model `lbc:item/sheepwand`.
- `assets/lbc/models/item/sheepwand.json`: parent `minecraft:item/handheld`, layer0 `minecraft:item/sheepwand`.
- `assets/minecraft/textures/item/sheepwand.png` exists. Existing item-model reference remains exactly `lbc:sheepwand`.
- All referenced translation keys and the Academy success-message key exist in all three author language files below. No RP, template, language, Java, build, server, or git files were changed. Existing keys are reused without aliases or copied language assets.

### assets/minecraft/lang/en_us.json

```json
{
  "sheep_wand": "Polymorph's wand",
  "type": "Item type: ",
  "staff": "Magic wand",
  "magic_damage": " Magic Damage",
  "mana_on_activate": " Mana cost to activate",
  "cast_distance": " Maximum cast distance",
  "sheep_wand.lore.1": "Transforms every mob into a magic sheep. (Not a player)",
  "sheep_wand.lore.2": "Does something with the succubus potion.",
  "upgraded_item": "This staff has been upgraded at the Academy of Mages!",
  "upgrade_succesful_sheepwand": "Now, if the creature's gender is male, the rod will explode its ejaculate, creating a black hole!"
}
```

### assets/minecraft/lang/ru_ru.json

```json
{
  "sheep_wand": "Жезл полиморфа",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "mana_on_activate": " Расход маны на активацию",
  "cast_distance": "Максимальная дистанция использования",
  "sheep_wand.lore.1": "Превращает любого моба в волшебную овцу. (Не игрока)",
  "sheep_wand.lore.2": "Делает нечто с зельем суккуба.",
  "upgraded_item": "Этот жезл был улучшен в Академии Магов!",
  "upgrade_succesful_sheepwand": "Теперь, если пол существа мужской, жезл взорвёт его эякулят, создав чёрную дыру!"
}
```

### assets/minecraft/lang/rpr.json

```json
{
  "sheep_wand": "Жезл полиморфа",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "mana_on_activate": " Расход маны на активацию",
  "cast_distance": "Максимальная дистанция использования",
  "sheep_wand.lore.1": "Превращает любого моба в волшебную овцу. (Не игрока)",
  "sheep_wand.lore.2": "Делает нечто с зельем суккуба.",
  "upgraded_item": "Этот жезл был улучшен в Академии Магов!",
  "upgrade_succesful_sheepwand": "Теперь, если пол существа мужской, жезл взорвёт его эякулят, создав чёрную дыру!"
}
```

## Integrated qualification

Build `65a39f13` passed 26 recorded suite results
in two actual mode1 matches (79.34 seconds), including actual
Sheep input/potion trial/promotion, permanent replacement, native shader-hole
physics/terrain, player input and actual match-stop cleanup. Danmaku, ordinary
Acid Academy, player rules, Time Stop, KFC counters and acquisition also passed.
Exact 348 sources / 965 classes /
2818 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-sheep-65a39f13.jar`; evidence: `build/verification/sheep-final/`.
Main remains04396ca4; READY ONLY is the user's existing choice. No author RP
edits or translation fragment are needed. Native fixtures do not qualify real
client pixels, disk restart/unload/disconnect or peak-load performance.
