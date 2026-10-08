# Индикаторы предметов LBC

Этот участок содержит 11 функций UI. Они собирают текст в общем storage `lbc.math`
и показывают его конкретному игроку; значения маны, зарядов и откатов сами здесь
не расходуются и не уменьшаются. Пиктограммы используют символы Private Use Area
и требуют соответствующего клиентского resource pack.

## Сборка панели: `get_data`

Источник: [ui/get_data.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/get_data.mcfunction).
Обычный вход — [players_tick:48](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction):
каждый проход игрока, если глобальный `legacy` не равен 1 и `lbc.challenge` игрока не равен 1.
Вначале функция заменяет `ui` полосой `mana/manamax [+manaregen_visual]`, затем
добавляет подходящие индикаторы и передаёт результат в actionbar. Отдельную
панель `ui2` всегда обнуляет, наполняет для ведра на голове/skill_issue и показывает
как title при наличии этих условий. При `legacy=1` команды показа пропускаются,
даже если функция вызвана вручную.

| Mana | Цвет текущего значения | Значок |
|---|---|---|
| 0 и выше | aqua | E100 |
| −5000..−1 | yellow | E132 |
| −10000..−5001 | red | E132 |
| −15000..−10001 | dark_red | E132 |
| −20000..−15001 | light_purple | E132 |
| −20001 и ниже | dark_purple | E132 |

Если у игрока вообще нет score `mana`, ни одна строка начального `ui set`
не выполняется: общий storage может сохранить предыдущую панель. Это отличается
от нулевой маны. Функция не прячет уже отправленный actionbar/title отдельной
командой при исчезновении условия: в следующий проход собирается новый текст,
а отсутствие нового title оставляет его обычное ванильное затухание.

В радиусе 20 добавляет значки `stopper_magic` и `stopper`. Для положительных
счётчиков добавляет значок и само число; эти числа здесь не переводятся в секунды:

`cold`, `cold_cd`, `frozen`, `acid_rifle`, `blaze`, `delayed_death`, `gatling_fovos`,
`lookback`, `nomagic`, `piss`, `ray`, `reverse_mana`, `scared`, `screamray`, `shock`,
`target`, `twitching`, `dimensional_shifting`, `void_resistance`, `all_seeing_eye`,
`living_legend`, `skill_issue`, `overheaven`, `gigaponos`, `sprint_cooldown`,
`effect_firebreak`, `effect_explosion`, `effect_big_explosion`, `effect_rtp_4`,
`effect_traps`, `effect_evoker`, `effect_shock`, `effect_kiuaskivi`,
`effect_lightarrow`, `effect_evokerarrow`, `effect_dimshifting`,
`effect_witch_gens_reset`, `effect_hedgehog_boom`, `effect_stand_arrow_hit`,
`effect_nuking`, `effect_vacuum`, `effect_timestop`, `effect_chain_lightning`,
`effect_add_mana`, `effect_add_manamax`, `effect_add_manaregen`, `effect_remove_mana`,
`effect_snus`, `effect_gravity_curse`, `effect_small_explosion`, `effect_lightning_bolt`,
`effect_boost_mana`, `effect_shock_potion`, `god_claw_cd`, `expansion_cd`, `deus_cd`,
`effect_ghosting`, `stone_layers`, `stone_cd`, `effect_rtp_1`, `effect_rtp_2`, `effect_rtp_3`.

Дополнительные условия панели:

| Условие | Что добавляется |
|---|---|
| Ботинки `custom_data.techno:1` | `nitro` |
| Predicate `reactive_boots`: ботинки `technomk2:1` или `reactive:1` | `reactivefuel` |
| Predicate `bow_or_crossbow`: в любой руке предмет из `#minecraft:vanilla_shooting` | Подсчёт стрел |
| В любой руке `swordwithability:1` | Панель зарядов меча |
| В любой руке `spellwithcooldown:1` | Панель откатов |
| `shadow_wisp_charge≥1` | Значок заряда Shadow Wisp |
| `nanobots_tier≥4` | Цветное число Nanobots tier |
| В любой руке `nexus_compass:1` | `nexus_compass_cd`, если ≥1; `ready`, если ≤0 |
| Ботинки `explosive_feets:1` | `rocket_armor_jumps` и `rocket_armor_jump_cd` |
| В любой руке `ak47:1,finite:1` | `hitbullet` |
| `#lbcskill swrg.math=1` и `lbc.skill=28` | Четыре счётчика Touhou |
| Шлем `bucket_head:1` | В начало `ui2` добавляется значок E124 и `mana`; показывается title |
| `skill_issue≥1` | В `ui2` добавляется E130; показывается title |

При совпадении нескольких условий показываются все соответствующие части по
порядку команд. UI лишь отражает эти flags/scores; применение способности
описано в предметных разделах. Поля `ui`, `ui2`, `arrow`, `tmpI` общие для всех
игроков, поэтому каждый вызов должен сохранить правильного исполнителя `@s`.

## Заряды: `charging`

Источник: [ui/charging.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/charging.mcfunction).
Для подходящего предмета в любой руке добавляет число готовых зарядов и, где
указано, текущий прогресс с визуальным знаменателем. Знаменатель — текст UI;
сам helper не заряжает оружие и не проверяет, совпадает ли его реальная формула
с этим текстом. Разные совпавшие метки могут добавить несколько индикаторов.

| Custom_data | Готовые заряды | Прогресс в подсказке |
|---|---|---|
| cyberkatana:1 | swordcharged1 | swordcharging1/10 |
| terrasword:1 | swordcharged2 | swordcharging2/1 |
| death_skythe:1 | swordcharged3 | swordcharging3/2 |
| vexsword:1 | swordcharged4 | swordcharging4/10 |
| shit_katana:1 | swordcharged5 | swordcharging5/7 |
| techrunner_katana:1 | swordcharged6 | swordcharging6/3 |
| banhammer:1 | swordcharged7 | swordcharging7/15 |
| fevgalea_star:1 | fevgalea_charge | Нет дроби |
| triple_golden_sword:1 | swordcharged8 | Нет дроби |
| blazefire_sword:1 | swordcharged9 | Нет дроби |
| fartus_nadristailo:1,upgraded:1 | fartus_sword | Нет дроби |

## Откаты: `cooldown`

Источник: [ui/cooldown.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/cooldown.mcfunction).
Для этих меток предмета в любой руке показывает raw-score при значении ≥1
либо зелёный текст перевода `ready` при значении ≤0. При отсутствующем score
ни одна из двух числовых ветвей не проходит. Собственного уменьшения счётчика
или блокирования использования предмета здесь нет.

| Custom_data | Score |
|---|---|
| mobgun:1 | coldown16 |
| attack_helicopter:1 | coldown25 |
| time_stopper:1 | coldown1 |
| reactive_shish:1 | coldownshish |
| hohlopitek:1 | coldownhohol |
| greed_greed:1 | divider_cd |
| oraora:1 | coldownoraora |
| homing_lightning:1 | coldownlightning |
| boss_helicopter:1 | cd_helicopter |
| stand_clocks:1 | coldowntimestand |
| divider_by_metal_pipe:1 | metal_divider_cd |
| nexus_corrupter:1 | nexus_corrupter_cd |
| nexus_roadgun:1 | nexus_roadgun_cd |
| halloween_mace:1 | cooldown_wind_charge |
| nexus_pulsar:1 | pulsar_cd |

Три особых предмета сначала проверяют соответствующий тег греха. `gluttony_cooldown:1`
показывает `ready` при `sin_gluttony`, иначе `no_sin`; `lust_cooldown:1` при
`sin_lust` показывает `lust_cd_small` и отдельное `Shift: lust_cd`, иначе `no_sin`;
`indolence:1` при `sin_indolence` показывает `indolence_cd`/ready по знаку, иначе
`no_sin`. `soulhunter_mace:1` вызывает отдельную панель. В конце при положительных
`danmaku` и `danmaku_score` добавляются оба числа независимо от конкретной метки
предмета; это выполняется только если сам `cooldown` был вызван.

## Soulhunter Mace: `cooldown_soulhunter_mace`

Источник: [ui/cooldown_soulhunter_mace.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/cooldown_soulhunter_mace.mcfunction).
Показывает `mace_1` белым без тега `unstamined` или красным с ним. Для `mace_2`,
`mace_3`, `mace_4` показывает положительное значение либо `ready` при ≤0,
с отдельными значками E198/E199/E200. `mace_1` не заменяется текстом ready.
Helper ничего не тратит и не снимает тег усталости.

## Стрелы: `count_arrows`

Источник: [ui/count_arrows.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/count_arrows.mcfunction).
Командой `clear @s #minecraft:arrows 0` подсчитывает подходящие стрелы без удаления,
сохраняет результат в `lbc.math arrow` и добавляет значок E108 с этим числом.
Вызывается обычной панелью и [tick_nexus_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_player.mcfunction).
Число бесконечных выстрелов из custom-предмета здесь не выводится отдельно.

## Shadow Wisp: `display_shadow_wisp_charges`

Источник: [ui/display_shadow_wisp_charges.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/display_shadow_wisp_charges.mcfunction).
Читает `shadow_wisp_charge`, умножает на 0.01 и сохраняет целое `tmpI` в storage,
затем вызывает следующий макрос. При штатном положительном заряде это номер
сотни накопленного заряда; helper сам не уменьшает заряд и не ограничивает
верхнее значение. Во входе `get_data` вызывается только при score≥1.

## Shadow Wisp macro: `display_shadow_wisp_charges_2`

Источник: [ui/display_shadow_wisp_charges_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/display_shadow_wisp_charges_2.mcfunction).
Добавляет к панели E174 и Unicode escape `\uE18$(tmpI)` после подстановки storage.
Для однозначного `tmpI` получается один символ E180–E189. Ограничения на `tmpI`
в helper нет: многозначная подстановка уже не обозначает один следующий символ
Unicode, потому что escape содержит ровно четыре hex-цифры. Точное отображение
всё равно зависит от шрифта resource pack.

## Nanobots: `get_data_nanobots`

Источник: [ui/get_data_nanobots.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/get_data_nanobots.mcfunction).
Добавляет значок E114 и `nanobots_tier`: 4..11 красным, 12..23 золотым, 24..39 жёлтым,
40..59 зелёным, ≥60 aqua. Для ≤3 ничего не добавляет. Это оформление текущего tier,
а не повышение tier или лечение.

## Touhou: `get_data_touhou_skill`

Источник: [ui/get_data_touhou_skill.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/get_data_touhou_skill.mcfunction).
Добавляет отдельно положительные `touhou_power`, `bomb_fragments`, `life_fragments`,
`time_orbs` со значками E169/E170/E171/E172. Условие включённого навыка 28 проверяется
вызывающей `get_data`; прямой ручной вызов helper этого ограничения не имеет.
Новые фрагменты и жизни этим отображением не выдаются.

## Nexus: `nexus_coldown`

Источник: [ui/nexus_coldown.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/nexus_coldown.mcfunction).
Для `nexus_roadgun:1`, `nexus_corrupter:1`, `nexus_compass:1`, `nexus_pulsar:1`
в любой руке показывает соответствующий score `nexus_roadgun_cd`, `nexus_corrupter_cd`,
`nexus_compass_cd`, `pulsar_cd`: число при ≥1 или ready при ≤0. В обычном Nexus-входе
вызывается только когда выбранный предмет имеет `spellwithcooldown_n:1`.
Сами значения и используемые ресурсы не меняются.

## Отладочная команда: `beam`

Источник: [ui/beam.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/beam.mcfunction).
Единственная команда `say` отправляет в общий чат грубую фразу из исходника.
Луч, предмет или индикатор она не создаёт. Буквальных callers этой функции
в исследуемом наборе не найдено; команда остаётся доступной при ручном вызове.
