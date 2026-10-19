# Аудит исходной заморозки LBC

Область: datapack `world/datapacksdisabled/LBC`, только чтение. Ниже фактический контракт исходника для переноса SnowGem18 / IceGem20; имена исходных ресурсов сохранены, предполагаемое соответствие новых предметов не выводится.

## Накопление холода и пороги

- `data/lbc/function/workingitems/icesword/pre.mcfunction`: каждый вызов устанавливает `*cold_pre=10`; если `cold` не положительный, сначала вызывает `other/freeze/add_cold`; затем прибавляет рассчитанное значение к `cold`, устанавливает `cold_cd=5`, вызывает `freeze/check` и выпускает снежинки. Это общий обработчик холода, его вызывает Ice Sword (`workingitems/icesword.mcfunction`), Ice Gem (`items/trinket_effects/ice_gem_1.mcfunction`) и другие источники.
- `other/freeze/add_cold.mcfunction`: множитель равен amplifier активного Slowness + 1; он умножает `*cold_pre`. Поэтому без активного Slowness множитель равен 1 и первый удар даёт 10; Slowness I (amplifier 0) также даёт 10, Slowness II (amplifier 1) — 20, далее множитель растёт на 1 за каждую ступень. При `cold>0` множитель повторно не вычисляется: последующие удары прибавляют базовые 10. Критичный нюанс: `check` тут вызывается после добавления; Slowness, выданный текущим порогом, повлияет на следующий приём холода только после того, как `cold` спадёт до 0.
- `other/freeze/check.mcfunction`: каждый диапазон включителен: cold 1–10 → Slowness I; 11–20 → II; 21–30 → III; 31–40 → IV; 41–50 → V; 51–60 → VI; 61–70 → VII; 71–80 → VIII; 81–90 → IX; 91–99 → X; 100+ вызывает `full`. Vanilla эффекту передаётся amplifier на единицу ниже римской степени. Каждая команда выдаёт эффект на 2 секунды и скрывает его частицы (`true`).
- `other/freeze/1.mcfunction` … `10.mcfunction`: точная матрица по командам каждого файла:

| Файл | cold | Slowness | Mining Fatigue | Weakness | Unluck | Blindness |
|---|---:|---|---|---|---|---|
| `1.mcfunction` | 1–10 | I (amp 0) | — | — | — | — |
| `2.mcfunction` | 11–20 | II (amp 1) | I (amp 0) | — | — | — |
| `3.mcfunction` | 21–30 | III (amp 2) | II (amp 1) | I (amp 0) | — | — |
| `4.mcfunction` | 31–40 | IV (amp 3) | III (amp 2) | II (amp 1) | I (amp 0) | — |
| `5.mcfunction` | 41–50 | V (amp 4) | IV (amp 3) | III (amp 2) | I (amp 0) | — |
| `6.mcfunction` | 51–60 | VI (amp 5) | V (amp 4) | IV (amp 3) | I (amp 0) | I (amp 0) |
| `7.mcfunction` | 61–70 | VII (amp 6) | VI (amp 5) | V (amp 4) | II (amp 1) | I (amp 0) |
| `8.mcfunction` | 71–80 | VIII (amp 7) | VII (amp 6) | VI (amp 5) | II (amp 1) | I (amp 0) |
| `9.mcfunction` | 81–90 | IX (amp 8) | VIII (amp 7) | VII (amp 6) | III (amp 2) | I (amp 0) |
| `10.mcfunction` | 91–99 | X (amp 9) | IX (amp 8) | VIII (amp 7) | III (amp 2) | I (amp 0) |

Каждый выданный эффект длится 2 секунды и скрывает частицы (`true`). Обозначения I, II… — уровень эффекта (amplifier + 1); 100+ вызывает `full.mcfunction`, а не ступень 11.

## Тики и удержание

- `load.mcfunction` запускает `tick1s`, который перепланирует себя на `1s` (`tick1s.mcfunction`); каждый такой запуск обходит все `#minecraft:mobs` вне nexus/imprinted и зовёт `mobs_1s`. Там `cold>0` вызывает `other/freeze/cooldown`, а `frozen>0` — `tick_1s_frozen`. Это отдельная scheduled-фаза раз в секунду, не 20-тиковая фаза: обычный `tick.mcfunction` каждый игровой тик направляет выполнение в `tick_not_timestopped`, где сущности получают `mobs_tick`; `effects/tick` удерживает frozen клетку через этот частый путь. Первое уменьшение cold/frozen происходит при следующем вызове `tick1s`, не синхронно с попаданием. Нюанс: `tick1s` также вызывается напрямую из `clock_fragment/select*.mcfunction` и `clock_fragment/jump_*.mcfunction`; такие вызовы дополнительно выполняют секундную обработку мобов, так что интервал не гарантирован ровно в 20 игровых тиков.
- `freeze/cooldown.mcfunction`: пока цель не заморожена, сначала заново применяет ступень/частицы по текущему `cold`; затем уменьшает положительный `cold_cd` на 1. Если после этого `cold_cd==0`, уменьшает cold на 1; отрицательный cold зажимается к 0. Обработка идёт только в scheduled-фазе `tick1s` (1 раз в секунду); при cold_cd=5 первые четыре вызова уменьшают cd с 5 до 1; пятый уменьшает cd до 0 и в тот же вызов уменьшает cold на 1. При cd=0 cold уменьшается на каждом следующем секундном вызове, пока источник снова не выставит cd.
- Порог 100 не делает обычное «замедление X»: `freeze/full.mcfunction` сохраняет UUID и исходные координаты, сбрасывает cold/cold_cd в 0, ставит `frozen=10`, добавляет `have_lucky_effects` и звук. Это десять секунд заморозки.
- Пока frozen положительный, `effects/tick.mcfunction` зовёт `freeze/block/tick` на каждом обработанном игровом тике (эффекты обрабатываются через `mobs_tick` только у сущности с `have_lucky_effects`). `block/tick` добавляет этот тег снова, переносит вызов в контекст UUID storage и ставит тег для следующего тика. `block/tick_3` телепортирует сущность к сохранённой позиции с исходным yaw/pitch, делает строгую заливку льдом в объёме `~ ~ ~` – `~ ~1 ~`, накладывает на 1 секунду Mining Fatigue amplifier 127 (уровень эффекта 128) и Weakness amplifier 127 (уровень эффекта 128); игроку также Resistance amplifier 1 (Resistance II) на 1 секунду. Выбрасывает одну снежинку каждый тик, сбрасывает cold в 0. Здесь нет прямой команды изменения атрибутов скорости или отдельного запрета атаки: неподвижность обеспечивается повторным телепортом в сохранённую точку, а боевые/добывающие ограничения — эффектами.
- `tick_1s_frozen.mcfunction` уменьшает frozen на 1 раз в секунду; при достижении 0 зовёт `block/destroy.mcfunction`: `fill ... air destroy` на два блока по высоте, частицы выпадающего льда (100) и снега (30). `fill ice strict` использует режим fill без обновления соседей; он не сохраняет и не восстанавливает блоки. При освобождении область очищается командой fill с destroy. Состояние до заморозки не записывается: сохраняются только координаты сущности.
- У frozen-цели нет естественного cold-decay: `cooldown` вызывает `check` только если frozen не положительный; block tick также каждый игровой тик сбрасывает cold в 0.

## Вход Ice Sword / Ice Gem и ограничения селекторов

- Ice Sword hit: `workingitems/icesword.mcfunction` выбирает сущности на расстоянии `0.001..7` с `HurtTime:10s` и `predicate=!lbc:same_team`, после чего вызывает `pre`; сама команда включена из `items/swords.mcfunction` для выбранного Ice Sword и при отсутствии ближайшего `nomagic_active` игрока и stopper_magic marker.
- Ice Gem: `items/trinket_effects/ice_gem.mcfunction` через chance-dispatch запускает `ice_gem_1`; там радиус 12, тип `#mobs`, `tag=!spectator`, `predicate=!same_team`, вызов `pre` каждому подходящему мобу. `ice_gem_1` — единственный найденный файл этой версии; `ice_gem_2` отсутствует. Snow particles / sounds — дополнительные команды источника.
- Успешное полное замораживание может выдать достижение Ice Sword: `full.mcfunction` проверяет игрока с HurtTime 10 и игрока-атакующего в пределах 7 с advancement `lbc:gowno`. Это побочный эффект datapack.

## Частицы, очистка и сохранение состояния

- `freeze/check.mcfunction` перед пороговыми эффектами вызывает `particle.mcfunction`; там snowflake у ног с количеством, взятым из cold * 0.5 (`store result ... int 0.5`). `pre` и источники Ice Gem / Sword также имеют собственные частицы.
- Время жизни состояния хранится в scoreboard objectives `cold`, `cold_cd`, `frozen` (создаются в `load.mcfunction`). `leave_death.mcfunction` обнуляет все три при очистке игрока. В приведённом механизме отдельной постоянной записи в storage есть только UUID и координаты для текущей ледяной клетки; она заменяется новым freeze для того же storage UUID. Код не показывает долговременное сохранение прогресса холода при перезапуске datapack.
- `freeze/full` ставит `have_lucky_effects`, а `block/tick` поддерживает этот тег каждый тик; это необходимо, потому что именно тег допускает сущность в `effects/tick` (см. `mobs_tick.mcfunction`). Моб с `sin_greed_active` исключается из замораживающего block tick в `effects/tick`; но cold/frozen секундные обработчики в `mobs_1s` сами по себе этого исключения не имеют.

## Файлы исходника

Все пути ниже относительно `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/`:

- `data/lbc/function/other/freeze/{add_cold,check,cooldown,full,particle,tick_1s_frozen,1..10}.mcfunction`
- `data/lbc/function/other/freeze/block/{create,destroy,tick,tick_2,tick_3}.mcfunction`
- `data/lbc/function/workingitems/icesword/pre.mcfunction`, `icesword.mcfunction`, `icesword_player.mcfunction`
- `data/lbc/function/items/trinket_effects/ice_gem.mcfunction`, `ice_gem_1.mcfunction`
- `data/lbc/function/tick1s.mcfunction`, `mobs_1s.mcfunction`, `mobs_tick.mcfunction`, `effects/tick.mcfunction`, `leave_death.mcfunction`, `load.mcfunction`
