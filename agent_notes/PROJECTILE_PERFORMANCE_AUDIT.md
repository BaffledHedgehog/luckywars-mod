# Аудит стоимости projectile handlers (read-only)

Проверены текущие `LbcClouds`, `LbcTrinkets`, места их вызова в `LbcItems` и projectile lifecycle. `LbcWands` ray и Terra намеренно не анализировались. Сборку, сервер и измерения не запускал; выводы описывают число операций в коде, не MSPT или предел сущностей.

## Tiny Acid / облачная кислота

`LbcClouds.tick()` копирует зарегистрированные `CloudState`/`AcidState` в переиспользуемые batch-списки и обходит каждый активный объект по одному разу за вызов (`LbcClouds.java:186-203`). Для каждого кислого снаряда `tickAcid()` обновляет возраст и при загруженном chunk вызывает `acidImpact()` (`318-331`). Сам снаряд отдельно проходит стандартный entity tick Minecraft: это нативные velocity, gravity/drag и swept collision (`AcidProjectile` `77-92`); custom tick не реализует второй цикл физики.

На один живой, загруженный AcidProjectile за custom tick приходятся:

- одна nearby-player spatial query для shield check (`341-346`);
- одна nearby-entity spatial query для проверки источника огня, если снаряд ещё не горит и обе проверки блоков не нашли fire/lava (`348-357`);
- одна nearby-LivingEntity spatial query радиусом 2.5 для poison/fire pulse (`364-373`);
- sneeze particle в незажжённом состоянии либо пять flame particles в горящем; ещё flame particle на каждую цель при нанесении fire damage (`350`, `359-360`, `366-369`);
- с вероятностью 20% — звук extinguish; текущий `sound()` проходит список всех подключённых игроков, отбрасывая игроков другого уровня/за пределом слышимости, и отправляет packet прошедшим фильтр (`374`, `377-391`).

Spatial calls идут через `ServerLevel.getEntities*` с ограниченными AABB. В локальном Minecraft 26.3 bytecode `Level.getEntities` делегирует в `LevelEntityGetter.get(AABB)`, а `EntityLookup.getEntities(AABB)` обходит доступные непустые entity sections и проверяет entity в пересечённых секциях; это section-based broad phase, не полный обход всех сущностей мира. Работа после запроса линейна числу найденных локальных сущностей: проверяются `alive`, `minecraft:mobs`, spectator tag и для игроков `allowDamage`. Для wand-пути дополнительные owner/allied/observer проверки выполняются только на найденных целях (`311-316`, `364-371`). Один target может быть обработан несколькими соседними снарядами в одном тике; код не дедуплицирует цели между снарядами. Это соответствует independent projectile effects; дедупликация изменила бы геймплей.

При `P` активных acid projectile основной custom путь имеет порядок `P` вызовов tick и примерно `2P` nearby queries за тик, до `3P` в незажжённых снарядах вне fire/lava, плюс работа по локальным кандидатам и по списку подключённых игроков для сработавших звуков. Поэтому 200–300 снарядов означают соответственно сотни локальных запросов за тик, но из этого нельзя вывести их длительность или вместимость без профиля сервера. Burning не выполняет поиск источника огня, но всё ещё проверяет shield и targets.

## Trinket и прочие зарегистрированные projectiles

`LbcTrinkets.shot()` — событийный обработчик запуска, а не tick-loop. При каждом обработанном запуске он отдельно считает Blaze и Ghast в inventory/crafting cursor (`LbcTrinkets.java:95-100`, `103-127`); каждый `count()` проходит inventory slots. При большом темпе выстрелов это повторная работа пропорциональна числу запусков × размеру инвентаря. Вероятностные проверки ограничивают создание дополнительных SmallFireball/firework, но не эти два подсчёта.

`damaged()` вызывается при успешном положительном уроне через `SkyWars2.java:90-96`. Для игрока-жертвы возможны ICE и Gneiss inventory counts (`LbcTrinkets.java:176-187`); для игрока-атакующего — Snow и Intercal counts (`190-200`). Срабатывание ICE/Snow делает локальный поиск целей и применяет frost (`210-221`); Intercal делает локальный поиск living mobs радиусом 10, фильтрует allies, spectator и недопустимых игроков, затем накладывает эффект (`197-207`). Эти обходы происходят на damage events/успешных proc, а не каждый projectile tick. Увеличение числа уроносящих снарядов увеличивает и число таких событий.

`projectiles` в `LbcTrinkets` — набор для load/unload/stop cleanup, он не обходится каждый тик (`154-174`). В `LbcItems`, напротив, tracked Primed TNT обходится один раз на тик, чтобы проверять fuse tags; pending dynamite snowballs пакетно преобразуются в native TNT, после чего физику/взрыв обрабатывает Minecraft (`LbcItems.java:507-538`). Эта стоимость линейна по tracked dynamite/pending throws и не содержит пространственного поиска.

## Возможные оптимизации

- Самый локальный кандидат в acid — не повторять `lookupOrThrow(...ON_FIRE...)` и создание эквивалентного wand `DamageSource` на каждом burning tick каждого снаряда (`362-363`); его можно сохранить в `AcidState`, если подтверждена неизменность owner/damage attribution. Это не требует менять число эффектных pulses или цели.
- В event handlers Trinkets можно исследовать единый проход inventory для нужных identity counts вместо нескольких последовательных `count()` (`103-127`, `176-200`), сохранив поиск в crafting slots/cursor и момент снимка данных. Это меняет только способ чтения; перед реализацией нужно подтвердить точную эквивалентность существующему `clearOrCountMatchingItems`.
- Acid shield / ignition / effect lookup теоретически могут делить один расширенный локальный candidate snapshot, но только если доказана эквивалентность разных AABB, типов сущностей, entity-box intersection и расстояний. Простое слияние сейчас не следует считать безопасным; лучше сначала снять профиль.
- Звуковую рассылку можно профилировать отдельно: сейчас rare sound path делает `O(V)` проверки игроков на срабатывание (`V` — подключённые игроки). Замена на vanilla radius-playback способна поменять слышимость, минимальную громкость, координату и audience; это уже требует сверки поведения и не предлагается как механически нейтральная правка.

Частоту кислотных particles/effects, damage cadence, shield checks, число снарядов и независимое воздействие снарядов нельзя сокращать под видом CPU-only оптимизации: это меняет видимый/боевой контракт. Здесь не заявляется benchmark или подтверждённое узкое место.

## Ссылки

- [LbcClouds.java](../src/main/java/me/skywars2/game/LbcClouds.java): native projectile `77-92`, tracker `149-213`, AcidProjectile spawn `278-315`, acid tick/effects/sound `318-391`, cleanup `394-399`.
- [LbcTrinkets.java](../src/main/java/me/skywars2/game/LbcTrinkets.java): inventory count `95-100`, projectile shot `103-138`, lifecycle-only tracking `154-174`, damage procs/area searches `176-221`.
- [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java): entity registration hooks `465-505`, native TNT tracking/conversion `507-538`, item manager tick `719-728`.
- [SkyWars2.java](../src/main/java/me/skywars2/SkyWars2.java): positive-damage dispatch `90-96`.
- [TrinketBowMixin.java](../src/main/java/me/skywars2/mixin/TrinketBowMixin.java): projectile-launch event hook `26-35`.
- Native 26.3 merged jar bytecode: `Level.getEntities(AABB)`, `EntityLookup.getEntities(AABB)`, `EntitySectionStorage.forEachAccessibleNonEmptySection`.