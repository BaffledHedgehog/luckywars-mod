# Структура исходников LuckyWars

## Где искать код

~~~text
src/main/java/me/skywars2/
├── SkyWars2.java                  Точка входа Fabric
├── game/
│   ├── GameController.java        Состояние матча и порядок игровых фаз
│   ├── LbcItems.java              Общий вход предметов и игровые обработчики
│   ├── NativeCheckHooks.java      Контракт подключения нативных проверок
│   ├── items/                     Предметы, лут и способности предметов
│   ├── entities/
│   │   ├── mobs/                  Мобы, призывы и боссы
│   │   └── projectiles/           Движение, попадания и жизнь снарядов
│   ├── effects/                   Статусы, магия и остановка времени
│   ├── combat/                    Урон, защита и авторство попаданий
│   ├── gui/                       Меню, таб, HUD и редактор наборов
│   ├── presentation/              Иконки, частицы, лучи и display-сущности
│   ├── academy/                   Магическая академия, обучение и испытания
│   ├── match/                     События, игровые фазы и снимки матча
│   ├── world/                     Карты, блоки и Nexus
│   ├── modes/                     BedWars и True Legacy
│   ├── players/                   Профили, команды, инвентарь и атрибуты
│   ├── commands/                  Регистрация и обработчики команд
│   ├── genie/                     Джин и интерпретатор предметов
│   ├── skills/                    Навыки, перки и клонирование
│   ├── net/                       Сетевой транспорт и метрики
│   └── core/config/               Конфигурация и исходные значения
└── mixin/                         Точки входа в ванильный код

src/main/resources/                Ресурсы, данные и метаданные Fabric
src/checks/java/me/skywars2/         Нативные проверки и редактор карт
src/checks/resources/              Метаданные Mixin для проверок
media-client/                      Необязательный клиентский плеер телефона
media-protocol/                    Общие типы и кодеки протокола телефона
resourcepack_template/             Дополнения к авторскому ресурспаку
tools/                             Инструменты разработки и проверки
docs/history/                      История переносов и подробные контракты
~~~

## Примеры по семействам

| Пакет относительно game | Что в нём искать |
| --- | --- |
| items | Жезлы лежат в items/wands, включая LbcMusicStaff. Настоящее оружие разделено на items/weapons/melee, ranged и firearms. Генерация дропов и их предметы собраны в items/drops, например LbcBeeDrop и LbcBeeDropItem. Телефон находится в items/phone, а модификаторы предметов TMT в items/tmt. |
| entities/mobs и entities/projectiles | Контроллеры ежей в mobs/hedgehogs, призывы и яйца в mobs/summons. Снарядами, наведением и проверкой траектории занимаются ProjectileLifetimes, ProjectileSteering и ResidentBlockRay. Правила слияния сущностей находятся в entities/CoreEntityMerging. |
| effects и combat | EffectLayers и ItemEffectLevels обслуживают состояния и уровни эффектов. LbcTimeStop находится в effects/time. Урон, владельцы и зачарования выстрела находятся в combat, рядом с LbcDamage, LbcIndirectKills и WeaponEnchantments. |
| gui и presentation | GameMenus обслуживает входы в интерфейс. GameBuilds лежит в gui/builds, ContentGallery в gui/gallery, а TabPresentation и AttributeHud в gui/hud. Общая визуализация лучей и эффектов находится в presentation. |
| academy | LbcAcademy управляет получением и улучшениями, SauvojenMeditation ведёт испытание, TrialWarningVisual рисует фигуры предупреждения. |
| world | MapManager, MapRotation и NativeChunkLoad находятся в world/maps. Ограниченные изменения блоков и сохранение содержимого при замене находятся в world/terrain, рядом с LbcDropBlocks. Механики измерения Nexus живут в world/nexus. |
| match и modes | Границы матча и события находятся в match и match/events. Снимки для восстановления Vanity собраны в match/checkpoints. BedWars и True Legacy имеют собственные пакеты modes/bedwars и modes/legacy. |
| players, commands и net | PlayerStore хранит профили. Инвентарь, команды игроков и атрибуты разнесены по подпакетам players. В commands находятся дерево и команды администратора, в net сетевой транспорт и задержка, в net/metrics измерения тиков. |
| genie и skills | Джин использует GenieRuntime и классы сценариев из genie/script. Реализация навыков находится в skills, клонирование акторов в skills/clone, ведьминские перки в skills/witch. |
| core/config | GameConfig и LbcSourceConfig дают конфигурацию матча и исходные значения для игровых служб. |

Три класса остаются непосредственно в game. GameController ведёт переходы матча и
вызывает игровые службы в установленном порядке. LbcItems связывает обработку
использования, регистрации сущностей и тиков предметов с нужным семейством.
NativeCheckHooks оставляет серверу общий контракт, к которому тестовый JAR
подключает реализацию из checks.

## Где проходят границы сборок

- Обычный productionJar содержит основной код и общие типы media-protocol.
  Серверная очередь, картинка и звук телефона остаются в game/items/phone.
- testServerJar берёт тот же серверный код и добавляет выход source set checks.
  Проверка может иметь тот же Java-пакет, что и проверяемый класс, а её файлы
  остаются в src/checks. Исполняемые проверки, редактор карт и validation Mixin
  загружаются только в тестовой сборке.
- media-client собирается отдельно и использует общий media-protocol. Игрок
  может подключаться обычным клиентом и использовать вариант телефона для ванили.
- Production Mixin разбиты по точке вмешательства. Например, input содержит
  CubeInteractionMixin, inventory содержит GroundItemTransferMixin, в terrain
  лежит BlockBreakEventMixin, а в media и transport находятся диалог и сетевое
  подключение телефона. Всего в плане 28 таких категорий. SkyWarsMixinPlugin
  остаётся у корня, а конфигурация skywars2.mixins.json хранит относительные
  имена, в том числе имена вложенных классов. Тестовые Mixin имеют отдельное
  пространство имён me.skywars2.validation.mixin.

Перенос меняет каталоги, полные Java-имена и нужные связи между пакетами.
Идентификаторы предметов и сущностей, пути таблиц лута и переводов, сохранения,
конфигурационные ключи и сетевой протокол остаются прежними. Команды сборки и
проверки описаны в README.md и SERVER_TESTS.md. Инструмент рефакторинга описан
в tools/JAVA_REFACTOR.md.
