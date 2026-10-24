# Corruption wand presentation/resource audit - 2026-10-24

Read-only source audit. Only this note was written. Author pack remains read-only; no template additions are justified for existing assets/keys. Graphical client appearance has not been tested.

Datapack root: `E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc`. Author pack root: `C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8`. Rules: [RESOURCE_PACK.md](RESOURCE_PACK.md).

## Exact ordinary presentation

Source: `loot_table/transformables_staff.json`, entry tagged `corrupter:1`. Base carrot_on_a_stick, count 1, weight 10, quality 9; tooltip `lbc:tier_c`; item model `lbc:corruptwand`. Name `corruption_wand`, italic=false, color unspecified (preserve inheritance). Tags `{craftusable:1,transformable:1,staff:1,magicwand:1,corrupter:1}`.

Lore has nine lines, all italic=false: (1) aqua `type` + `staff`; (2) aqua literal `0-6 ` + `magic_damage`; (3) aqua score `corruption_wand_mana_use`/`lbc.math` + `mana_on_activate`; (4) aqua literal `15 ` + `cast_radius`; (5-8) white `corruption_wand.lore.1` through `.4`; (9) gold `craft_with_acid`. Config line 21 sets cost to 1800.

## Acid + Corruption continuation

Source: `loot_table/tuntijaa_acid_corrupt.json`. Base carrot_on_a_stick, count 1, tooltip `lbc:tier_s`, model `lbc:acidcorrupt`. Name `acidcorrupt_wand`, dark_green, italic=false. Tags `{transformable:1,staff:1,raycastspell:1,acidcorrupt_wand:1}`. Four lore lines, all italic=false: aqua `type` + `staff`; aqua score `acidcorrupt_wand_mana_use`/`lbc.math` + `mana_on_activate`; white `acidcorrupt_wand.lore.1`; white `.2`. Config line 86 sets cost 1210.

## Academy presentation and keys

`function/other/magic_academy/interacted_2.mcfunction` adds upgradable:1, prints green `magic_academy_root`, dispatches yellow condition messages from `can_upgrade/corrupter.mcfunction` (`upgrade_condition_corrupter`) and `can_upgrade/acidcorrupt_wand.mcfunction` (`upgrade_condition_acidcorrupt`).

`interacted_upgrade.mcfunction` adds upgraded:1 and mode:1, appends `item_modifier/add_upgrade_desc.json`: one green italic=false `upgraded_item` lore line. Original name/model/lore are retained. `upgrade/corrupter.mcfunction` prints yellow `upgrade_succesful_corrupter` and applies tiers/c (same C tooltip); `upgrade/acidcorrupt_wand.mcfunction` prints yellow `upgrade_succesful_acidcorrupt` and applies tiers/ss. Preserve misspelling `succesful`.

## Recipe donors

`function/other/craft_ground.mcfunction:16`: stacid:1 item plus corrupter:1 item at distance 0.001..1 calls `other/sauvojen_tuntija/crafted_acidcorrupt`. Output is fresh `lbc:tuntijaa_acid_corrupt` loot, not donor presentation/upgraded flags. Ground helper kills nearest carrot_on_a_stick tagged corrupter:1 (without repeating distance bound), then kills initiating stacid entity. `_inv.mcfunction` decrements each matching corrupter and stacid donor in crafting slots 0-3; emits same fresh output. Donors are ordinary Acid staff (`stacid`) and ordinary Corruption (`corrupter`), not acidwand/soulcorrupter/corrupter_old/nexus_corrupter.

## Author pack translations (verbatim JSON values)

### assets/minecraft/lang/en_us.json

```json

{
  "corruption_wand": "Corruption Wand",
  "corruption_wand.lore.1": "Gives a random debuff in a 15 blocks radius:",
  "corruption_wand.lore.2": "poisoning, desiccation, hunger or instant damage.",
  "corruption_wand.lore.3": "Cursed mobs may drop 'Demonic ash'",
  "corruption_wand.lore.4": "You can draw powerful glyphs with it.",
  "acidcorrupt_wand": "Mifrak Haolamott",
  "acidcorrupt_wand.lore.1": "An extremely toxic acid. Does not burn, but",
  "acidcorrupt_wand.lore.2": "even the blocks are corrupted. And the whole world shook.",
  "craft_with_acid": "Can be combined with the staff of Lahazor La Kadur ha Haretz",
  "type": "Item type: ",
  "staff": "Magic wand",
  "magic_damage": " Magic Damage",
  "mana_on_activate": " Mana cost to activate",
  "cast_radius": " Cast radius",
  "upgraded_item": "This staff has been upgraded at the Academy of Mages!",
  "magic_academy_root": "Magic Academy",
  "upgrade_condition_corrupter": "Try applying all of this wand's debuffs at once. This will teach you to choose the effect instead of relying on randomness!",
  "upgrade_succesful_corrupter": "You can now control debuffs applied by the wand by pressing Shift+RMB",
  "upgrade_condition_acidcorrupt": "Meditate with a wand (Shift + RMB) - This will cast a powerful curse on you that will make you invulnerable for 30 seconds, but then you will instantly die. However, you can prevent death if you clear all your effects during this time!",
  "upgrade_succesful_acidcorrupt": "I believe you have overcome the curse of the wand! And this has opened up the reverse effect of the effects of diseases and decay! The wand now has new modes (Shift+RMB to switch)! (The reverse effect of the wand - Gilgul Neshamoth - makes the owner invulnerable due to the souls of subordinates and summoned mobs, and the titular disease imposes an inevitable deadly curse (each subsequent hit increases the death timer by 2 minutes)! The fourth mode of the wand - Plague - imposes a negative mana effect on the target!)"
}

```

### assets/minecraft/lang/ru_ru.json

```json

{
  "corruption_wand": "Жезл разложения",
  "corruption_wand.lore.1": "Накладывает случайный дебафф в радиусе 18 блоков:",
  "corruption_wand.lore.2": "отравление, иссушение, голод, моментальный урон.",
  "corruption_wand.lore.3": "Порченные мобы могут дропнуть 'Демонический порошок'",
  "corruption_wand.lore.4": "Вы можете начертить пентаграмму с его помощью.",
  "acidcorrupt_wand": "Мифрак Хаолямотт",
  "acidcorrupt_wand.lore.1": "Чрезвычайно токсичная кислота. Не горит, но",
  "acidcorrupt_wand.lore.2": "разлагает даже блоки. И целый мир содрогнулся.",
  "craft_with_acid": "Можно объединить с жезлом Лахазор Ла Кадур ха Харетз",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "mana_on_activate": " Расход маны на активацию",
  "cast_radius": "Радиус использования",
  "upgraded_item": "Этот жезл был улучшен в Академии Магов!",
  "magic_academy_root": "Академия магов",
  "upgrade_condition_corrupter": "Попробуйте наложить все дебаффы этого жезла одновременно. Так вы научитесь выбирать эффект, вместо того, чтобы доверять рандому!",
  "upgrade_succesful_corrupter": "Теперь вы можете контроллировать дебаффы, накладываемое жезлом, нажав Shift+ПКМ",
  "upgrade_condition_acidcorrupt": "Медитируйте с жезлом (Shift + ПКМ) - Это наложит на вас сильнейшее проклятие, которое сделает вас неуязвимым на 30 секунд, но затем вы мгновенно погибнете. Однако, вы можете предотвратить смерть, если очистите все свои эффекты за это время!",
  "upgrade_succesful_acidcorrupt": "Я полагаю, что вы смогли преодолеть проклятие жезла! И это открыло обратное действие эффектам болезней и разложения! Теперь жезлу доступны новые режимы (Shift+ПКМ для переключения)! (Обратный эффект жезла - Гильгуль Нешамоот - делает владельца неуязвимым за счёт душ подчиненных и призванных мобов, а титульная болезнь накладывает неизбежное смертельное прокляти (каждое следующее попадание увеличивает таймер смерти на 2 минуты)! Четвертый режим жезла - Чума - накладывает эффект негативной маны на цель!)"
}

```

### assets/minecraft/lang/rpr.json

```json

{
  "corruption_wand": "Жезл разложения",
  "corruption_wand.lore.1": "Накладывает случайный дебафф в радиусе 18 блоков:",
  "corruption_wand.lore.2": "отравление, иссушение, голод, моментальный урон.",
  "corruption_wand.lore.3": "Порченные мобы могут дропнуть 'Демонический порошок'",
  "corruption_wand.lore.4": "Вы можете начертить пентаграмму с его помощью.",
  "acidcorrupt_wand": "Мифрак Хаолямотт",
  "acidcorrupt_wand.lore.1": "Чрезвычайно токсичная кислота. Не горит, но",
  "acidcorrupt_wand.lore.2": "разлагает даже блоки. И целый мир содрогнулся.",
  "craft_with_acid": "Можно объединить с жезлом Лахазор Ла Кадур ха Харетз",
  "type": "Тип предмета: ",
  "staff": "Магический жезл",
  "magic_damage": "Магический урон",
  "mana_on_activate": " Расход маны на активацию",
  "cast_radius": "Радиус использования",
  "upgraded_item": "Этот жезл был улучшен в Академии Магов!",
  "magic_academy_root": "Академия магов",
  "upgrade_condition_corrupter": "Попробуйте наложить все дебаффы этого жезла одновременно. Так вы научитесь выбирать эффект, вместо того, чтобы доверять рандому!",
  "upgrade_succesful_corrupter": "Теперь вы можете контроллировать дебаффы, накладываемое жезлом, нажав Shift+ПКМ",
  "upgrade_condition_acidcorrupt": "Медитируйте с жезлом (Shift + ПКМ) - Это наложит на вас сильнейшее проклятие, которое сделает вас неуязвимым на 30 секунд, но затем вы мгновенно погибнете. Однако, вы можете предотвратить смерть, если очистите все свои эффекты за это время!",
  "upgrade_succesful_acidcorrupt": "Я полагаю, что вы смогли преодолеть проклятие жезла! И это открыло обратное действие эффектам болезней и разложения! Теперь жезлу доступны новые режимы (Shift+ПКМ для переключения)! (Обратный эффект жезла - Гильгуль Нешамоот - делает владельца неуязвимым за счёт душ подчиненных и призванных мобов, а титульная болезнь накладывает неизбежное смертельное прокляти (каждое следующее попадание увеличивает таймер смерти на 2 минуты)! Четвертый режим жезла - Чума - накладывает эффект негативной маны на цель!)"
}

```

## Asset existence and references

- `assets/lbc/items/corruptwand.json`: present

```json

{
    "model": {
        "type": "model",
        "model": "lbc:item/corruptwand"
    }
}

```

- `assets/lbc/models/item/corruptwand.json`: present

```json

{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "minecraft:item/corruptwand"
  }
}

```

- `assets/minecraft/textures/item/corruptwand.png`: present

- `assets/lbc/items/acidcorrupt.json`: present

```json

{
    "model": {
        "type": "model",
        "model": "lbc:item/acidcorrupt"
    }
}

```

- `assets/lbc/models/item/acidcorrupt.json`: present

```json

{
    "parent": "minecraft:item/handheld",
    "textures": {
      "layer0": "minecraft:item/acidcorruptwand"
    }
  }

```

- `assets/minecraft/textures/item/acidcorruptwand.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_c_background.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_c_frame.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_s_background.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_s_frame.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_ss_background.png`: present

- `assets/lbc/textures/gui/sprites/tooltip/tier_ss_frame.png`: present

## Source inconsistencies / transfer cautions

Ordinary stat line and English lore say radius 15; Russian/rpr lore says 18. Preserve exact author texts and literal stat rather than silently reconciling. Academy ordinary remains tier C (upgrade does not raise tooltip tier). Continued wand retains original four lore lines plus upgrade marker; no name/model replacement occurs. Russian/rpr values are valid Unicode with no U+FFFD replacement characters; terminal rendering may be lossy, so use the exact UTF-8 values recorded above. Existing condition/success strings describe effects but are not proof the runtime implementation fully matches them. Missing assets, if any, are explicitly marked above. No full-pack mutation, runtime edit, or client visual verification performed.
