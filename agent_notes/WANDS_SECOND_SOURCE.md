# Tiny Air, Stone и Teleport: source contract

Только аудит старого LBC; это не объявление переноса готовым. Источник: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

Уточнение (user), целевое поведение отличается от старого кода:

> погоди ка, независимое исполнение жезла от стены и сущности это баг, надо исправить. Когда видишь странное поведение - спрашивай меня все таки

При переносе terminal выполняется один раз при первом попадании. Ниже
двойное исполнение описывает исходный баг, который не следует сохранять.
В wall-команде `unless block` проверяет текущую точку; последующий
`positioned ^ ^ ^-0.35` переносит только исполнение terminal на предыдущую.
Это уточняет неточную формулировку о проверке блока позади в исходном разборе ниже.

## Общий dispatch и луч

ПКМ через `interaction_rightclickable` → `players_tick` → `items/spells` выбирает main/offhand `magicwand:1` и зовёт `workingitems/spell1` / `_left`. Внешний dispatcher запрещает свой каст при `nomagic_active` у игрока или другого не-наблюдателя в радиусе 20; `spell1_default[_left]` дополнительно требует score mana ≥ cost и отсутствия `stopper_magic` marker в радиусе 20. Отдельного cooldown для этих трёх веток нет. Старый `items/spells` временно даёт полный Overload +10M на время route; переносить это нельзя: действуют текущие правила Overload и реальное списание.

`spell1.mcfunction` сбрасывает `Distance=0`, ставит `caster` и `raycaster`, исполняет spell ветку, затем снимает оба тега. Tiny-лучи синхронные рекурсивные функции: каждый вызов `*_2` увеличивает `Distance` на 1; при `Distance<=100` следующий вызов сдвигает локальную позицию на 0.35 блока. Начальный вызов идёт из глаз. Таким образом в спокойном коридоре проверяются 101 позиции от 0 до 35 блоков включительно, шагом 0.35; на каждом шаге сначала проверяется сущность, затем блок в текущей точке; если там препятствие, wall-terminal исполняется на предыдущей точке (`positioned ^ ^ ^-0.35`). Сущностная проверка — `@e[dx=0,dy=0,dz=0,distance=..8,tag=!raycaster,tag=!spectator,limit=1]`, не только `#minecraft:mobs`, без team-фильтра и без явно заданной сортировки; следовательно это может выбрать игрока/не-mob entity и не гарантирует ближайшую цель. Все три эффекта выполняются на текущей точке луча, не телепортируют игрока.

Terminal checks — два независимых `execute` в одном ray function. `*_end` ставит `Distance=1000`, но это не `return`: если на той же итерации найдена сущность и одновременно `unless block` обнаружил преграду, terminal helper вызывается дважды до проверки рекурсии. Это создаёт 2 Firework rocket для Air, повторный fill для Stone, и повторный apply эффекта для Teleport. Terminal helper воспроизводит свой звук; внешняя wrapper-функция всегда воспроизводит такой же после ray. Одиночный endpoint поэтому слышен дважды, совпавшие entity+wall дают три одинаковых звука (две terminal-команды и wrapper).

## Три ветки

| Ключ и цена | Terminal payload | Звук / завершение |
|---|---|---|
| Air: `stair_tiny:1`, 250 mana; item model `lbc:st/air_tiny`, translate key `wand_air_tiny` | Луч рисует `minecraft:firework` particles. `air_end` summon-ит `firework_rocket` в точке терминала с `Life:0,LifeTime:0,ShotAtAngle:1b`; `FireworksItem` — одна ракета с одной explosion `{shape:"burst", colors:[16770831], fade_colors:[12422150], has_trail:1b, has_twinkle:1b}`. Owner/цель не задаются; нет постоянного entity-тикера у datapack, rocket завершается нативным коротким lifetime. | `entity.generic.explode`, master, один раз в `air_end` и один раз после `air_2` в `air.mcfunction`; при wall+entity terminal звук/ракета могут дублироваться. |
| Stone: `ststone_tiny:1`, 500 mana; model `lbc:st/stone_tiny`, key `wand_stone_tiny` | Луч рисует stone block particles. `stone_end` выполняет в terminal position точный вызов `fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:stone strict`: область 3×3×3 и режим `strict` следует сохранять; при wall+entity вызов повторяется. | `block.stone.place`, master: в `stone_end` и затем после ray в `stone.mcfunction`. |
| Teleport: `stteleport_tiny:1`, 750 mana; model `lbc:st/teleport_tiny`, key `wand_teleport_tiny` | Луч рисует cloud particles. `teleport_2_end` даёт Levitation duration 1 second, amplifier 30, двум селекторам: всем `#minecraft:mobs` в сфере радиуса 2 от терминала; затем `#minecraft:mobs,dx=0,dy=0,dz=0,distance=..12` (вторая выборка дополнительно ограничена маленькой областью около terminal point). Пересекающиеся цели могут попасть в оба effect-give. Это не телепорт. | `lightning_bolt.thunder`, master, из `teleport_2_end` и после ray в `teleport.mcfunction`; при dual endpoint end вызов/звук повторяется. |

После каждого ray wrapper списывает указанную цену, добавляет Mage full-set `mage_set_bonus` к `manamax` и вызывает `items/trinket_effects/cast_wand`: Blood Anal Boom roll/снаряд и Mana Gem прибавка маны — общие зависимости успешного каста. Источник платит после эффекта, relying на dispatcher mana gate; перенос должен брать цену ровно один раз через текущий wand spending API.

## Спорные места для реализации

- Tiny Stone меняет блоки: сверить целевую игровую политику редактирования арены и native `fill strict`; старый source не содержит отдельного ограничения по аренам/защищённым блокам.
- Луч target selector не исключает союзников и способен выбирать не-mob сущности; Teleport effect targets `#minecraft:mobs`, Air/Stone создают/меняют мир в точке, найденной по общей `@e`-проверке. Согласовать с текущей friendly-protection политикой до объявления точного target behavior.
- У всех трёх нет per-wand cooldown; не придумывать его. Учитывать повторный terminal вызов при совпадении wall/entity.

## Первичные файлы

- `function/items/spells.mcfunction`, `function/players_tick.mcfunction`, `function/workingitems/spell1.mcfunction`.
- `function/workingitems/spell1_default.mcfunction`, `spell1_default_left.mcfunction`, `config.mcfunction`.
- `function/workingitems/sauvojen_staffs_tiny/{air,air_2,air_end,stone,stone_2,stone_end,teleport,teleport_2,teleport_2_end}.mcfunction`.
- `loot_table/random_sauvojen_wand_tiny.json`, `function/items/trinket_effects/cast_wand.mcfunction`.
