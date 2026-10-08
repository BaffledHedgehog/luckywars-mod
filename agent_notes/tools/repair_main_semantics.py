from pathlib import Path
import re
p=Path('agent_notes/lbc_other_main.md'); s=p.read_text(encoding='utf8')
fix={
'negative_mana_1':'Применяет `minecraft:slowness` с длительностью 1 секунду и amplifier 1 (Slowness II) к `@s`. Эффект выдаётся только если текущая позиция выполнения является воздухом (`if block ~ ~ ~ minecraft:air`); функция не создаёт и не выдаёт предмет.',
'effect_skill_issue_2-0':'Это macro-функция: при вызове `with storage lbc.math` читает `$(uid).Rotation[0]` и `[1]`, записывает их в scoreboard `rot0old` и `rot1old` с масштабом 1. Здесь нет эффекта игроку: сохранённые углы используются следующим шагом обработки, который опирается на эти старые значения.',
'stopnuke':'Выполняет `stopsound @a master minecraft:lbcsounds.nuke`, останавливая звук nuke у всех игроков. Её планирует `stoppernuke` через `schedule function ... 1s`, то есть через секунду; функция сама не удаляет взрыв и не меняет игроков иначе.',
'falling_power':'Создаёт marker в фиксированной позиции `0 310 0` с тегами `slowraycast`, `movingpower`, `spawned_new_power`, `can_deleted`, `magic`, `other` и `lifetime:800`, затем передаёт его обработчику `falling_power_data`. Это запуск сущности-последовательности, а не выдача эффекта или предмета.',
'falling_power_data':'Для текущего marker задаёт случайные координаты X и Z от −100 до 100, не меняя Y, затем снимает тег `spawned_new_power`. Вызывается в контексте marker через `execute as`; текущая позиция выполнения остаётся позицией вызывающей функции, если не добавлено `at @s`.',
'raycastfireball_2':'Переносит три компонента `Pos[0..2]` ближайшего marker с тегом `reikast` в `Motion[0..2]` ближайшего small fireball с множителем чтения 1000 и множителем записи 0.001. Это задаёт движение снаряда из сохранённого в marker вектора; функция не просто показывает трассу. Геометрия селектора fireball — `distance=..0.001` от текущей позиции выполнения.',
'raycastfireball_2_big':'Копирует три координаты `Pos` marker `reikast` в `Motion` ближайшего tagged fireball с коэффициентами `data get ×1000` и `data store ×0.001`, затем удаляет у него временный тег `current`. Так задаётся вектор движения большого fireball, а не его позиция.',
'raycastfireballchick_2':'Передаёт X/Y/Z marker `reikast` в `Motion` ближайшего small fireball: чтение умножает `Pos` на 1000, запись использует коэффициент 0.0016. Это определяет скорость chick-fireball на основе вектора marker; селектор ограничен `distance=..0.001`.',
'liquidvoiddamage_recur':'Macro читает `$(tmp)` из переданного storage `lbc.math` и наносит это количество урона `@s` с типом `outside_border`, только если у сущности нет `void_resistance` 1 или выше. Повторные вызовы рассчитывает вызывающая `liquidvoiddamage`; эта функция выполняет один шаг урона.',
'stopper_player_remove_smth':'Удаляет у `@s` два атрибутивных modifier: один у `movement_speed`, второй у `attack_damage`. Caller проходит по игрокам не в spectator в пределах `distance=..20` от контекста stopper; команда `at @s` устанавливает позицию каждого выбранного игрока.',
'craft_uniwand_with_grimoire_value_check':'По scoreboard `value` выбирает ровно один item modifier на wand в `container.0`: `..−11` → `tiers/ff`, `−10..−1` → `f`, `0..9` → `d`, `10..99` → `c`, `100..999` → `b`, `1000..9999` → `a`, `10000..99999` → `s`, `100000..999999` → `ss`, `1000000..` → `x`. Каждая совпавшая ветка делает `return`, поэтому последующие диапазоны уже не применяются.',
'manaregen_inwater':'Файл пуст: у функции нет исполняемых команд, поэтому ручной или автоматический вызов не меняет состояние.',
'hedgehog_boom':'Если в пределах 20 блоков нет marker с тегом `stopper_magic`, функция передаёт управление внешней функции `lbcr:raycast_loop9cd_upgrade`. Сам локальный файл не содержит дальнейших действий; фактический эффект задаётся этим внешним dependency, код которого находится вне исследуемого datapack LBC.',
'drownpre':'Файл пуст и не имеет эффекта при вызове. Вызовы других drown-функций нужно проверять отдельно; этот источник не накладывает эффект и не удаляет предметы.',
'infinitestewused':'При advancement `lbc:stewused` заменяет предмет в main hand на одну `minecraft:cooked_beef` с custom_data `{inf:1}`, именем `infinite_steak` и tier style `lbc:tier_c`, затем отзывает advancement для следующего употребления. Расход входного stew уже следует из consume trigger advancement.',
'infinitestewused_old_sch':'После отложенного запуска заменяет main hand у игроков с тегом `scheduled_eating` на одну rabbit stew с custom_data `{inf_old:1}` и именем `horn`, затем снимает тег со всех игроков. Время задержки задано вызывающей функцией `infinitestewused_old`; этот обработчик не назначает новый schedule.',
'nitroboots':'Файл пуст: этот источник сам не задаёт эффекта для ботинок. Его нельзя трактовать как активный обработчик только по имени.',
'absurd_child_tick_random_msg':'Выбирает случайное целое 1..20 и для каждого из 20 результатов отправляет соответствующий переводимый `absurd_child_msg_N` всем игрокам в пределах `distance=..30`. Это только случайная реплика; существо/механика продолжает жить в вызывающем tick-handler.',
'kill_rand_hihihiha':'Для случайных исходов 1..6 убивает текущую сущность, если у неё нет `void_resistance` и нет исключающего тега `sin_greed_active`. Исход 7 выбирает случайного не-spectator игрока без `owner`/`sin_greed_active` и при отсутствии void resistance, измерения Nexus и stopper marker в пределах 24 убивает выбранную цель; затем выполняет advancement grant для текущего `@s` и снимает временный тег `owner`. При отсутствии стоппера условие считается выполненным; в Nether/Nexus branch explicitly excludes `nexus`.',
'killanvil':'Удаляет все сущности с тегом `killedanvils`; caller планирует очистку через 60 game ticks после создания наковальни. Это уборщик временных наковален.',
'killcart':'Удаляет все сущности с тегом `killedcart`; caller `firestorm` планирует очистку через `25s`, то есть через 25 секунд.',
'speed_up':'Если у `@s` надеты boots с custom_data `nyauwu:1` и `mana < manamax`, добавляет modifier `movement_speed` с UUID `916c…726d` и значением 1 `add_multiplied_total`. При `mana >= manamax` удаляет этот modifier, возвращая базовое состояние.',
}
for name,para in fix.items():
 h='### `lbc:other/'+name+'`'; i=s.find(h)
 if i<0: continue
 a=s.find('**Фактическая работа.** ',i); b=s.find('\n',a)
 s=s[:a]+'**Фактическая работа.** '+para+s[b:]
# Effect command durations in Minecraft are seconds; distinguish them from SNBT fuse ticks.
s=re.sub(r'(\d+) ticks, amp (\d+)',r'\1 seconds, amplifier \2',s)
# These summaries were generated by a loose give-token match; effect IDs are not items.
s=re.sub(r' выдаёт/создаёт предметы или loot `minecraft:[^`]+`(?:, `minecraft:[^`]+`)*\.', '.', s)
s=s.replace('эффекты частиц:','частицы:').replace('звук:','звук:')
p.write_text(s,encoding='utf8')
