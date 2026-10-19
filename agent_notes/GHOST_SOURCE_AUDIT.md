# LBC10 «Призрак»: аудит исходника

Read-only сверка старого datapack и ресурспака; production, tests и ресурспак не изменялись.

## Механика набора

- [`loot_table/kit/ghost.json`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/ghost.json>) имеет `rolls: 4`: выдаются четыре одинаковых splash potion, `minecraft:max_stack_size: 4`, custom color `14737663`, имя `ghost_potion` (`italic:false`). Каждое зелье даёт invisibility II и strength II (amplifier 1) на 100 тиков, у обоих `show_particles:false`. В таблице нет других предметов или функций.
- [`function/swrg_kit_integration/game/kit_select/select.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kit_select/select.mcfunction>) связывает `lbc.kit=10` с этой loot table.
- Выбор в GUI slot 9 задаёт `lbc.kit=10`, если получено достижение `true_advancements/armor/phantom`; карточка показывается там же. Достижение [`advancement/true_advancements/armor/phantom.json`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/phantom.json>) требует убить игрока в полном комплекте брони с custom_data `phantom:1` на каждой части. Награда вызывает `swrg_kit_integration/gain/kit`, которая показывает title/subtitle `new_kit`.
- [`function/swrg_kit_integration/gui/page/9000.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction>) помещает карточку `gui/page/9000/9` в enderchest slot 9 при разблокированном phantom; закрытый слот получает модель `lbc:locked` и modifier `swrg_unlocks/phantom`. Карточка loot table задаёт модель `lbc:windik`, имя `lbckit.9`, пять lore-строк `lore.lbckit.9.0`…`.4` и строки статистики.

## Связанные эффекты и граница вывода

`effect_high_invisibility.json` — advancement-трекер на выдачу эффекта невидимости amplifier ≥1 (с исключением spectator), с toast и без reward-функции. Сам по себе он не запускает дополнительную механику набора.

В datapack также есть отдельный scoreboard-эффект `effect_ghosting`: [`function/effects/tick.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/tick.mcfunction>) обновляет невидимость; при спринте в воздухе вызывается `ghosting_tick1`, который при `HurtTime:10s` создаёт vex, толкает сущность вперёд, рисует частицы/проигрывает звуки и меняет armor, toughness, attack damage, gravity и fall-damage modifier. Источник добавления времени — [`function/other/event/halloween/soulcorruptwand/apply_ghosting.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/apply_ghosting.mcfunction>), который прибавляет 5 секунд и применяет modifiers. Просмотренные ссылки не связывают этот эффект с `lbc.kit=10` или `loot_table/kit/ghost.json`; считать его дополнительной способностью LBC10 оснований нет.

## Сверка ресурспака (только чтение)

В `assets/minecraft/lang/en_us.json` и `ru_ru.json` уже есть исходные ключи карточки: `lbckit.9`, lore `.0`–`.4`, а также `ghost_potion`. RU карточка: «Призрак», `> Взрывное зелье призрака x4`, ` * Сила II 00:05`, ` * Невидимость II 00:05`, затем две пустые строки; EN: “The Ghost”, “- Splash ghost potion x4”, “ * Strength II 00:05”, “ * Invisibility II 00:05”, затем две пустые строки. Это совпадает с выдачей и её порядком эффектов в таблице, кроме того, что английское описание эффекта в карточке перечисляет Strength перед Invisibility, тогда как JSON potion contents перечисляет Invisibility перед Strength.

Карточка использует `lbc:windik`; ресурс-пак содержит `assets/lbc/items/windik.json` и `assets/lbc/models/item/windik.json`. Advancement high-invisibility использует `lbc:advancement_icon/invisibility`, для которого найдены `assets/lbc/items/advancement_icon/invisibility.json` и соответствующая item model. Для phantom уже существуют RU/EN ключи `lbc.adv.armor.phantom` и `.lore`; lore предупреждает об уязвимости к стрелам и раскрытии после удара.

## Вывод

Прямое поведение LBC10 по изученным источникам — четыре splash-зелья, каждое invisibility II + strength II на 5 секунд без частиц, с gate по достижению phantom и обычным уведомлением `new_kit`. Новая дополнительная способность набора сверх перечисленного не обнаружена. Отдельный `effect_ghosting` существует, но связан с soulcorrupt wand, а не с выдачей LBC10. Переводы и модели для карточки/связанных advancement уже есть в разрешённом только для чтения ресурспаке.
