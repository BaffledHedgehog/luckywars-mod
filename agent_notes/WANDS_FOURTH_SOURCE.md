# Tiny Electro и Tiny Acid: source audit

Аудит фактических команд старого datapack и связанного `lbcr`-raycaster pipeline. Это source contract, не runtime-проверка и не перенос кода. Старые файлы `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/{lbc,lbcr}` и ресурс-пак только читались.

Текущий статус переноса Tiny Electro — в [WANDS_ELECTRO_PORT.md](WANDS_ELECTRO_PORT.md). Это более поздняя реализация; этот source audit сохраняет исторические findings старого datapack и не утверждает runtime PASS.

## Каст и предметы

ПКМ main/offhand проходит через `items/spells` и `spell1_default` / `_left`; обе ветки требуют цену маны и отсутствия `stopper_magic` marker в 20 блоках. Внешний route блокирует `nomagic_active`. У Tiny Electro ключ `stelectro_tiny`, модель `lbc:st/electro_tiny`, имя `wand_electro_tiny`; цена 100. Tiny Acid: `stacid_tiny`, модель `lbc:st/acid_tiny`, `wand_acid_tiny`; цена 63. Нет отдельного cooldown. Wrapper сначала исполняет трассу/endpoint, затем играет звук, списывает mana, добавляет Mage-full-set `mage_set_bonus` и вызывает общий trinket `cast_wand` hook.

Loot сохраняет старые item tags `magicwand:1`, `staff:1`, `transformable:1`, `craftusable:1`; у Electro также `cyberset:1`. Lore содержит Tiny-ключи `.lore.1-.3`, а затем обычные Electro `.lore.4-.7` / Acid `.lore.4-.5` — это отдельные рецепты улучшения предметов, не cast payload. EN/RU Tiny lore (дословно): Electro `Mgntn pl` / `Мгнтн пл`, `SZDT nrgtchsk dg.` / `Сздт нргтчск дг.`, `Ksn with lktrprvdshchm blkm beat in blast!` / `Ксн с лктрпрвдщм блкм бьт в блст!`; Acid `Knc grlrdnh tl` / `Кнц глрднх тл`, `Szdt xltny shr.` / `Сздт кслтнй шр.`, `In mzht pjch g!` / `В мжт пджчь г!`. Общие tail lore описывает комбинирование и crafting effects. Сохранять key, текст/форматирование и source order.

## Tiny Electro: короткий ray до lightning endpoint

`electro` вызывает `electro_2`, затем играет `firework_rocket.twinkle`, берёт 100 mana и запускает Mage/trinket hooks. В `electro_2` каждый шаг увеличивает `Distance`, рисует `electric_spark`, проверяет entity в текущей нулевой `dx/dy/dz`-ячейке с `distance..8` и исключениями `raycaster`/`spectator`, а также проверяет текущий блок на non-air. Entity terminal вызывает `electro_end` из точки на 1 блок назад (`positioned ^ ^ ^-1`); wall terminal вызывает его также из точки на 1 блок назад. При отсутствии terminal идёт шаг `^ ^ ^.35`, максимум 101 проверка/35 блоков. Endpoint не является полным lightning effect: он создаёт marker с `lifetime:40`, тегами `raycastmolnia`, `slowraycast`, `shield_blockable`, `mecha`, `player`; копирует caster `Rotation`, `lbcID2`, `team_number` и добавляет `actived`.

Живой pipeline: `tick.mcfunction` запускает `tick_not_timestopped`; тот каждый тик обходит `#minecraft:raycasters[tag=slowraycast]` и вызывает `raycastselector`. Для `raycastmolnia` выбирается `lbcr:raycast_molnia`. Каждое выполнение прибавляет `Distance`, рисует transition dust и electric spark, играет тихий firework звук и пытается нанести `.5` arrow damage максимум двум мобам в текущей ячейке через `dmg_bypl_0_5nocd`. Повторные шаги могут снова пытаться наносить урон (фактический урон также зависит от native hurt cooldown). Вода или non-air запускают `raycast_electro42`: частицы `end_rod`, если блок conductive — проводящая ветка, иначе marker уничтожается. На Distance 11 вызывается отражение (`molnia_prelomlenie_pre`); до Distance 10 луч шагает на `.2` за выполнение. Ограниченный lifetime очищает marker по `Lifetime`.

Damage helper временно исключает игроков с тем же `lbcID2` и `team_number` через `nodamage`/`nodamage1`; итоговый damage target — `#minecraft:mobs`, без `spectator`. Lightning endpoint tags позволяют generic shield-block helper проверить `shield_blockable`. Electro также может завершить трассу wall и entity в одном sample: две source `execute` команды независимы, поэтому endpoint может быть создан дважды до остановки recursion. Перенос обычных single-terminal жезлов должен свести их к одному terminal; этот источник фиксирует старый overlap, не целевое дублирование.

## Tiny Acid: шесть кислотных marker-снарядов

`acid` зовёт `acid_2`, играет `zombie_villager.cure`, списывает 63 mana, затем Mage/trinket hooks. Каждый 0.35-block sample рисует `sneeze`; при entity в текущей selector-ячейке вызывается `acid_end`, при wall helper вызывается из точки на `.35` назад. Без столкновения максимум 101 sample/35 блоков. Две terminal проверки независимы и при совпадении entity+wall source может выполнить `acid_end` два раза, то есть запустить две одинаковые volleys; это terminal overlap, а не контракт переноса.

`acid_end` поворачивает pitch на -90 и зовёт `raycast_preloop11_2`. Helper играет cure звук и создаёт шесть `raycastacid` marker-ов от endpoint с lifetime 300; их начальные позиции сгруппированы в offsets 0..0.05 по направлению, затем получают случайные yaw -180..180 и pitch -90..-60, caster `lbcID2`/`team_number`, `actived`. Это не native projectile: marker raycaster двигается через существующий `slowraycast` pipeline каждый игровой тик; общий marker lifetime counter также растёт по 1 каждый тик до 300.

`tick_not_timestopped` вызывает `raycastselector` для каждого `slowraycast` marker-а каждый тик (кроме остановленного времени); `raycast_acid`/`raycast_acid2_player` применяют `sneeze` во время полёта и каждый шаг ищут мобов в радиусе 2.5. При обычном состоянии эффект — Poison I на 15 секунд; при переходе marker-а в `on_fire` — 3 fire damage с визуальными flame частицами. Burning состояние появляется от fire/lava блоков или близкого огня/горящего raycaster и остаётся на marker-е. Owner и вся его команда защищены: acid markers копируют `lbcID2` и `team_number`, `damagewithid2[_1_player]` выставляет `nodamage` для same-ID/same-team, а damage/effect selector исключает этот тег и `spectator`. Значит для wand-specific acid reuse нельзя полагаться только на общую проверку observer/alive: она не эквивалентна owner/team immunity.

## Реиспользование в текущем моде

`LbcClouds.AcidProjectile` уже является Polymer-совместимым ThrowableProjectile с нативными velocity, gravity, drag и swept collision; `LbcClouds` уже ведёт возраст до 300 ticks, очищает сущности через существующее lifecycle и реализует sneeze/combustion/poison/fire damage. Его `target()` фильтрует mobs, spectator-tag и недопустимых участников. Однако он не исключает owner/allies; `GameController.allowDamage` проверяет spectator/participant/lives, не команды. Tiny Acid требует wand-only source/team immunity, поэтому новый флаг/путь должен ограничить изменение acidImpact лишь Tiny wand projectile-ам; старые potion/cloud acid должны сохранить текущее поведение. Старый Tiny cast создаёт 6 случайно направленных marker-ов; выбранный пользователем native projectile подход позволяет переиспользовать тип и acidImpact, выдавая шесть native projectiles с source spread и 300-tick возрастом.

В доступных текущих helpers есть acid projectile; отдельного готового lightning-chain helper-а для Tiny Electro не найдено. `LbcFrost` относится к frost state/cleanup, не к молнии. Для Electro отдельный port отложен: необходимо будет сохранить отражение, conductive-water ветку, owner/team damage guard и shield interaction при native implementation.

## Primary source paths

- `data/lbc/function/workingitems/spell1_default.mcfunction`, `spell1_default_left.mcfunction`, `items/spells.mcfunction`, `config.mcfunction`.
- `data/lbc/function/workingitems/sauvojen_staffs_tiny/{electro,electro_2,electro_end,acid,acid_2,acid_end}.mcfunction`.
- `data/lbcr/function/{raycast_molnia,raycast_electro42,dmg_bypl_0_5nocd,damagewithid2,damagewithid2_1_player,damagewithid2_lite,damagewithid2_increased,raycast_preloop11_2,acid_get_id_22,raycast_acid,raycast_acid2_player,raycast_acid2_check_if_fire}.mcfunction`.
- `data/lbc/function/{tick,tick_not_timestopped,tick2t,tick5t,markers_2t,markers_2t1,raycasters_5t,raycastselector}.mcfunction`; `data/lbc/function/items/shields/shield_block_2.mcfunction`.
- `loot_table/random_sauvojen_wand_tiny.json`; resource-pack `assets/minecraft/lang/{en_us,ru_ru}.json`, keys `wand_electro_tiny*`, `wand_acid_tiny*` and generic lore tails.
- Current reuse points: `src/main/java/me/skywars2/game/LbcClouds.java`, `GameController.allowDamage`, `LbcFrost.java`.
