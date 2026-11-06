# Остаток переноса — проверено 6 ноября 2026

Это текущий срез квалифицированного JAR `0e7d6a47` после инструментов,
а не перечень старых TODO. Исходные исторические аудиты часто содержат уже
закрытые задачи; их нельзя складывать как актуальный остаток.

## Готовые предметы и техническая нативизация

В проверенном срезе 776 верхнеуровневых готовых `lbc:ported/`
таблиц первый разрешаемый исход у 749 использует
зарегистрированный `skywars2:` предмет. У 27
он пока ванильный. Эта пачка перевела ещё 11 таких исходов.

Это НЕ процент готовности мода и НЕ число уникальных предметов/неперенесённых
способностей. Несколько таблиц могут описывать варианты одного предмета; обычный
ванильный стек может иметь уже работающую специальную механику по компонентам.
Шлем-ведро — в том числе служебный результат попадания; не каждая такая строка
сама по себе требует новой способности. Старые Balance-варианты, вложенные food
таблицы и неоднородные первые исходы здесь не служат знаменателем. Типы `item` и
`loot_table` с неявным minecraft: учитываются одинаково.

Метод и точные списки/хеши:
`build/verification/native-ordinary-tools-2026-11-06/remaining-catalog-proof.json`.

## Крупные оставшиеся ветки

- Полный TMT: генератор свойств из законченного контента и остальных допустимых
  свойств, все исходные генераторы, новая схема текстур из предметного атласа.
  Оставлен последним по прямому решению автора; черновик не является выпуском.
- Полные равномерные custom/mixed/type пулы и торговец Bucks. Разделение пулов
  сохранено в ALL_ITEM_POOLS_CONTRACT_2026_11_05.md; TMT занимает один слот и
  генерируется после выбора. Ванильный пул/бесконечный шалкер уже отдельно
  квалифицированы. Эти оставшиеся пулы нельзя считать готовыми по числу JSON.
- Режимы2/4 и полное открытие связанных наградных цепочек. В актуальном
  GameController.startFailure допускаются1 и квалифицированный3. BedWars3 уже
  работает; утверждения старых аудитов «всё кроме1 заблокировано» устарели.
- Закрытые зависимые наборы/навыки. В текущем каталоге отсутствуют наборы6/14/16;
  часть зависит от полной наградной цепочки/TMT, часть от отдельной ветки Academy.
  BLUE/зелёный провод Nexus явно скрыт LootTestCatalog.publicNativeItem именно
  из-за полной random_glack/TMT награды. Не открывать эти ворота одной правкой меню.
- Отдельная незавершённая/неквалифицированная ветка Academy и её проверок:
  защищённые рабочие изменения LbcAcademy/LbcRerollCrafting и связанных сцен
  не смешиваются с прошедшим проверку JAR. Это не означает, что уже законченные
  академические жезлы/комнаты отсутствуют целиком.
- Общая квалификация отката мира и проверки полного выпуска: многие способности
  уже имеют точное частичное восстановление и свои проверки, но это не равнозначно
  доказанному полному откату всего мира, пиковой производительности, дисковому
  перезапуску и физическому клиентскому прогону.

Отдельно: фрагменты ускорения времени остаются отключённым экспериментом.
TimeFragmentSprintChecks прямо помечен OFF-only; проверки ускорения не означают
существование готового активного предмета. Это отдельная сложная/необязательная
для текущего готового каталога ветка, не новая причина закрыть Stand/Touhou часы.

Нельзя честно назвать остаток «ещё N предметов» или «пара вечеров»: технический
хвост обычных предметов уже небольшой, а оставшиеся большие ветки неравноценны
маленьким предметным пачкам. Основная работа обычных способностей выполнена;
последние пачки в основном закрывают нативную идентичность и совместимость
уже работающих механик.

## Текущий технический список ванильных первых исходов

Это список для следующего аудита; он не объявляет способности отсутствующими.

- `lbc:ported/admin_zaika` — `minecraft:carrot_on_a_stick`
- `lbc:ported/bucket_head` — `minecraft:leather_helmet`
- `lbc:ported/canned_food` — `minecraft:repeating_command_block`
- `lbc:ported/death_compass` — `minecraft:recovery_compass`
- `lbc:ported/devil_totem` — `minecraft:totem_of_undying`
- `lbc:ported/harpoon` — `minecraft:trident`
- `lbc:ported/indolence_claw` — `minecraft:carrot_on_a_stick`
- `lbc:ported/infinite_totem` — `minecraft:totem_of_undying`
- `lbc:ported/malevich` — `minecraft:repeating_command_block`
- `lbc:ported/minigun_crossbow` — `minecraft:crossbow`
- `lbc:ported/minigun_crossbow_old` — `minecraft:crossbow`
- `lbc:ported/moisei_boots` — `minecraft:leather_boots`
- `lbc:ported/op_hoe` — `minecraft:netherite_hoe`
- `lbc:ported/sea_legend` — `minecraft:trident`
- `lbc:ported/shulker_bow` — `minecraft:bow`
- `lbc:ported/touhou_bomb_item` — `minecraft:snowball`
- `lbc:ported/touhou_collectible_1` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_10` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_2` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_3` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_4` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_5` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_6` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_7` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_8` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_collectible_9` — `minecraft:repeating_command_block`
- `lbc:ported/touhou_time_stopper` — `minecraft:snowball`
