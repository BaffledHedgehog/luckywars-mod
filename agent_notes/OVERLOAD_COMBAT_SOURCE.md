# Исходный контракт negative mana и Overload Crux

Только аудит старого datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC`; не описание текущей реализации и не runtime PASS. Главные входы: `data/lbc/function/other/manaregen.mcfunction`, `other/negative_mana*.mcfunction`, `other/negative_mana/*.mcfunction`, `damage_with_tmp_void.mcfunction`, `other/overload_crux/{use,give}.mcfunction`, `workingitems/spell1_default.mcfunction`.

## Negative mana: стадии и урон

`other/negative_mana.mcfunction` выбирает одну стадийную функцию для незакрытой overload-шлемом головы; пороги включительно такие: I `-1..-5000`, II `-5001..-10000`, III `-10001..-15000`, IV `-15001..-20000`, V `≤-20001`. При активном overload helmet стадии I–IV подавлены; для V вызывается отдельная `negative_mana_5_helmet`. Независимо от шлема активный overload chestplate при любом `mana≤-1` вызывает `negative_mana_resist`.

| Стадия | Эффекты, повторяемые вызовом | Дополнительное действие |
|---|---|---|
| I | Slowness II | — |
| II | Slowness III, Mining Fatigue I, Weakness I | Вызов закомментирован для Nausea |
| III | Slowness VII, Mining Fatigue III, Nausea 4 сек., Weakness II, Hunger I | — |
| IV | Slowness 128, Mining Fatigue 128, Nausea 6 сек., Weakness 128, Hunger III | — |
| V | Те же предельные Slowness/Mining Fatigue/Weakness, Nausea 6 сек., Blindness I (amplifier 0) 2 сек., Hunger VII | Вычисляет урон; без шлема коэффициент 0.1, с overload helmet — 0.045 |

Значения amplifier в таблице показывают фактические числа datapack (то есть Slowness `1 1` = amplifier 1/Slowness II). Stage V считает `tmp = max_health`; затем копирует `mana` в `tmp1` и выполняет `tmp1 /= manamax` (scoreboard integer `/=` использует `floorDiv`), затем `tmp -= tmp1`. Рассчитанная double-величина `tmp * 0.1` либо `tmp * 0.045` сохраняется в storage и целиком передаётся в `damage_with_tmp_void`; score-вариант нужен для ветвления, не квантует фактический damage. Без шлема ветвление сравнивает score `floor(tmp*0.1)` с `hplvl`, сохранённым из NBT Health через `data get` (floor); в helmet-функции сравнение использует objective `hplvl` у `@s` (ceil-представление source health). При `damageScore >= healthScore` вызывается `other/negative_mana/explode`; иначе идёт луч. Явного clamp по health/урону нет. Делитель `manamax` не проверяется на ноль: если `manamax == 0`, `/=` сообщает ошибку и оставляет `tmp1` в уже скопированном значении `mana`; арифметика после команды продолжает использовать эту копию, то есть quotient эквивалентен `mana` в этой ветке, а не нулю/старому случайному значению.

Chestplate-ветка накладывает Resistance amplifier 0–4 в тех же диапазонах mana. Это отдельная от stage selection проверка. Stage V `explode` не наносит числовой blast damage: она вызывает шесть обычных лучей и пятнадцать больших лучей, проигрывает `lbcsounds.impulse` всем в радиусе 40 и удаляет вызывающую сущность `@s`.

## Лучи: геометрия, collision, target и урон

Обычный `tentacle` берёт случайный yaw `-180..180°`, pitch `-40..40°` (целые random values умножены на 0.01) и `dmg = mana * -0.00006`. Из позиции вызывающего entity, обычно глаз игрока, `tentacle_2_1` посещает 31 точку (30 перемещений по 0.3 блока, около 9 блоков суммарно); продолжает только если текущий блок входит в `#airs`. На каждой точке ставит одну dragon-breath частицу и выбирает максимум одну сущность `type=#minecraft:mobs`, исключая `spectator` и текущий тег `caster`, с `distance=..3` плюс `dx=dy=dz=0`. Селектор включает игроков и мобов из entity-type tag; выбор может повторяться на следующих точках луча. `tentacle_damage` вызывает `damage <target> <dmg> outside_border by <caster>`. Отрицательная mana даёт положительную величину damage. При caster с `overload_full_active` каждый hit дополнительно вызывает Saturation и Instant Health с source duration `1`; это мгновенные эффекты и Minecraft применяет их как 1 tick, затем добавляет +300 mana без явного max clamp.

Большой луч (`tentacle_big`) использует случайные yaw и pitch оба `-180..180°`, `dmg = mana * -0.00025`, шаг 1, 31 точку/30 перемещений (около 30 блоков); на каждой точке dragon-breath particle count 10 с spread 0.3, уроняется до двух `#mobs` в радиусе 2 через `outside_border by caster`. После каждого шага безусловно пытается `fill ~1 ~1 ~1 ~-1 ~-1 ~-1 air strict`, затем продолжается, пока `Distance≤30`; в отличие от обычного луча, проверка `#airs` отсутствует. `negative_mana/explode` вызывает 15 таких больших лучей.

`damage_with_tmp_void` не использует значение `tmp` как damage argument луча: оно только выбирает стандартный `tentacle` или `tentacle_full_set`; оба вычисляют собственный `dmg` пропорционально текущей mana. В full-set helper yaw/pitch задаются случайно в пределах ±10°/±10°, `dmg=mana * -0.0001`, стартовый `Distance=-30`; он посещает 61 точку (60 перемещений по 0.3 блока, около 18 блоков), пока не упрётся в не-air блок. Каждый луч на стадии V/при уроне с полным overload set исполняется из глаз вызывающего.

## Crux: предмет, выдача, use, экипировка

Loot table `data/lbc/loot_table/overload_crux.json` выдаёт carrot-on-a-stick с custom data `{craftusable:1,magicwand:1,overload_crux:1}`, моделью `lbc:overload_crux` и max stack 99; config задаёт стоимость 7500 mana. В просмотренной `other/craft/inventory_craft_check` отдельного рецепта Crux нет. Предмет помечен для общей craft-системы; сам Crux выдают через `other/overload_crux/give` при `mana≤0` и отсутствии `obtained_crux`, после чего ставится этот tag.

Use trigger is `workingitems/spell1_default`, requiring the selected item `{overload_crux:1}` and no `stopper_magic` marker within 20. It runs at eye position. `use` subtracts 7500 without checking mana affordability, plays `overload_jump` and enchantment-table sounds to all players, then calls the full-set ray exactly five times. It then replaces matching diamond helmet/chestplate/leggings/boots with Overload pieces and removes one Crux from inventory. The `use` function does not grant a replacement Crux or set `obtained_crux`.

`other/overload_crux/give` is a separate automatic path: at `mana<=0` without `obtained_crux`, it drops a Crux, emits dragon-breath particles, plays elder-guardian curse sound, and sets the tag. `manaregen` removes the tag at positive manamax; `leave_death` also clears it. The `stopper_magic` spell-dispatch guard blocks Crux use entirely: cost, rays, and armor conversion.

## Источниковые дефекты/неполные вызовы

- В `other/craft/overload_armor.mcfunction` ветки выдачи chestplate и helmet проверяют наличие `minecraft:diamond_boots`, хотя затем loot-replace адресует chest/helmet; сами `kill` в этих ветках тоже ищут boots вместо соответствующей брони. Воспроизведение не запускал.
- `other/negative_mana/explode` вызывает `tentacle_big` без проверки solid/air и каждый шаг выполняет `fill ... air strict`; это потенциальный блокоразрушающий побочный эффект, не оговорённый в вызывающей функции.
- Stage V вызывает `negative_mana/explode`, который заканчивается `kill @s`: при вызове на игроке это самоуничтожающая ветка при `damage≥health`, тогда как альтернативная ветка шлёт луч. Это именно исходный datapack вызов.
- Деление на `manamax=0` в обеих stage-V функциях не защищено; стадия V также не ограничивает рассчитанный урон перед сравнением с текущим Health.
- Crux use не проверяет affordability 7500 mana. Не нашёл Crux-specific recipe в просмотренном `inventory_craft_check`; общие ground-craft dispatch и флаг `craftusable` требуют отдельного сведения, если перенесённый pipeline должен поддержать крафт.

## Связь с Java-переносом

Для native эффектов/временных облаков см. [LbcClouds.java](../src/main/java/me/skywars2/game/LbcClouds.java); индексацию эффектов и anti-magic interception — [LbcMagic.java](../src/main/java/me/skywars2/game/LbcMagic.java). Это ссылки на существующие subsystem helpers, не свидетельство, что damage rays/Crux уже реализованы. В просмотренном Java игровом коде есть native `DamageSource` handling в [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java) и участник-урон в [GameController.java](../src/main/java/me/skywars2/game/GameController.java); данный аудит не приписывает им datapack `outside_border` эквивалент.

Основные старые helper пути: `data/lbc/function/damage_with_tmp_void.mcfunction`, `data/lbc/function/other/negative_mana/{tentacle,tentacle_2,tentacle_2_1,tentacle_damage,tentacle_damage_restore_hp,tentacle_full_set,tentacle_full_set_2,tentacle_big,tentacle_big_2,tentacle_big_2_1,explode}.mcfunction`; вход и предмет: `other/negative_mana{,_1,_2,_3,_4,_5,_5_helmet,_resist}.mcfunction`, `other/overload_crux/{give,use}.mcfunction`, `workingitems/spell1_default.mcfunction`, `other/manaregen.mcfunction`, `items/changed_inventory/sel.mcfunction`.
