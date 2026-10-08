"""One-time coordinator copy edit; refuses to rewrite user-annotated documents."""
from pathlib import Path

notes = Path(__file__).resolve().parents[1]
path = notes / "PLUGIN_ITEMS_AND_HOOKS.md"
text = path.read_text(encoding="utf-8")
if "(user)" in text:
    raise ValueError("Document contains a user annotation; preserve it with a targeted manual edit")


def paragraph(prefix, replacement):
    global text
    start = text.index(prefix)
    end = text.find("\n\n", start)
    if end == -1:
        end = len(text)
    text = text[:start] + replacement + text[end:]


paragraph("Это статический разбор", "Это разбор исходников старого сервера: регистрация прослежена по коду, запуск на сервере не проверялся. В `SigmaWarsMain.onEnable` нативные сервисы предметов создаются независимо от `legacy.enabled`. При `false` пропускаются `LegacyFunctionRuntime` и `SwrgNativeService`; флаг не управляет независимыми datapack мира или прямым запуском vanilla-функций. Условия старого datapack и Java-слушателей описываются отдельно.")

# Keep each paragraph's existing source links after its readable description.
prefix = "`ItemCustomData` делает"
start = text.index(prefix)
end = text.index("\n\n", start)
links = text[text.index("[Менеджер]", start):end]
paragraph(prefix, "`ItemCustomData` читает `DataComponents.CUSTOM_DATA` и проверяет наличие сырого ключа, не его значение. `hasAll` требует все ключи, `matchesAny` — хотя бы один. Менеджер принимает ПКМ/ЛКМ по воздуху и блоку, включая отменённые события, кроме spectator. Повторы объединяются в пределах одного тика по UUID игрока, руке и стороне клика; обе руки и ПКМ/ЛКМ независимы. Совпадения обрабатываются по списку способностей. Встроен только `TerraWand` по ключу `stterra`: ПКМ списывает `stterra_wand_mana_use` из `values.yml` (резервное значение 1000) и выпускает `terra` из глаз игрока; при нехватке маны появляется сообщение в actionbar. Сам обработчик жезла не задаёт кулдаун, проверку приседа или отмену клика. " + links)

prefix = "`NexusCompass`, `RocketArmor`, `MetalPipe`, `Malevich`"
start = text.index(prefix)
end = text.index("\n\n", start)
links = text[text.index("[loot metal pipe]", start):end]
paragraph(prefix, "`NexusCompass`, `RocketArmor`, `MetalPipe`, `Malevich` распознаются через Bukkit PDC: вложенный `PublicBukkitValues`, namespace плагина, `lws` или `lucky_wars`. Например, таблица `lbc:metal_pipe` задаёт `lws:item.lws:type=metal_pipe`; одного сырого флага `metal_pipe:1` недостаточно. Таблицы `lbc:nexus_item_green` и `lbc:malevich_rnd` тоже содержат одновременно сырой datapack-флаг и PDC. " + links)

lines = text.splitlines()
for i, line in enumerate(lines):
    if line.startswith("| [NexusCompass]"):
        link = line.split(" —", 1)[0]
        lines[i] = link + " — создан и зарегистрирован в `SigmaWarsMain` | ПКМ основной рукой с PDC type `nexus_compass` отменяет клик, читает `plugins/luckywars/nexus_coords.json` и телепортирует в случайную записанную точку загруженного мира, имя которого содержит `nexus`. | Кулдаун 30 секунд. Исключён только `minecraft:imprinted`; также мешает MARKER `stopper_magic` в кубической области ±20 блоков. Если нет файла, координат или мира, телепортации и кулдауна нет, но клик уже отменён. Индикатор предмета проверяет обе руки. |"
    elif line.startswith("| [RocketArmor]"):
        link = line.split(" —", 1)[0]
        lines[i] = link + " — зарегистрирован в main, передан UI | Надевание брони и вход пересчитывают состояние; начало планирования тратит заряд и добавляет ускорение по взгляду, вертикальную поправку, частицы и звук. Сапоги распознаются по PDC type `rocket_armor`/`rocketarmor`/`rocket`, `additionals` (строка/список) либо специальному контейнеру glider; namespaces plugin/lws/lucky_wars. | Максимум 3 заряда; по одному восстанавливаются за 15 секунд (300 тиков). Состояние обновляется только в survival/adventure. Каждый тик компонент GLIDER сапог зависит от наличия заряда. Исключены Nexus/Imprinted и MARKER `stopper` в области ±20. Сначала событие планирования без elytra отменяется и вызывается `setGliding(false)`, затем проверяются мир/stopper и ракетные сапоги. Смерть и снятие сапог сбрасывают состояние; перезарядка не сохраняется. |"
    elif line.startswith("| [MetalPipe]"):
        link = line.split(" —", 1)[0]
        lines[i] = link + " — зарегистрирован в main | Выбрасывание и появление Item с PDC type `metal_pipe` ставят задержку подбора MAX и включают отслеживание каждый тик. При переходе на землю звучит `lbcsounds.metal_pipe`, задержка становится 20 тиков, а все LivingEntity в области ±2.5 по каждой оси получают 25 урона. Источник урона — бросивший игрок, если онлайн, иначе Item. | Кулдауна нет; повторный отрыв от земли разрешает новый удар. В Nexus/Imprinted обработчик выходит до сброса задержки, оставляя MAX. Владелец и союзники не исключаются. Удалённые/невалидные сущности убираются из отслеживания. |"
    elif line.startswith("| [Malevich]"):
        link = line.split(" —", 1)[0]
        lines[i] = link + " — зарегистрирован в main | Клики и drop/spawn не запускают полезное действие; работают две периодические задачи. Каждые 5 тиков игрок с PDC type `malevich` в инвентаре/броне/второй руке и `lbc.skill != 2` получает первый непустой результат `lbc:malevich_rnd` в каждый пустой слот хранения, брони и второй руки. Каждые 20 тиков лежащий Malevich в радиусе 3 удаляет другие Item и заменяет их результатом той же таблицы. | Исключены Nexus/Imprinted и MARKER `stopper_magic` в области ±20. Набор `postConsumeCooldown` только читается, записи в него не найдены. Если преобразований не было и Malevich drops больше 100, каждый подходящий предмет заменяется 1–2 TNT с fuse 0–10. Objective `lbc.skill` используется без проверки на отсутствие. |"
    elif line.startswith("| [MagicProjectiles]"):
        link = line.split(" —", 1)[0]
        lines[i] = link + " — менеджер зарегистрирован в main; встроен только Terra | Снаряд — ItemDisplay с тегом `magic_projectile` и PDC `mp_type`/`mp_owner`; каждый тик движется и проверяет попадание лучом. Terra: скорость 1.5 блока/тик, жизнь 200 тиков, радиус 0.4, зелёный след и звук. Попадание в LivingEntity, кроме ArmorStand/владельца/spectator, даёт 8 indirect-magic урона и отталкивание 0.4 по направлению. При попадании в сущность или блок пытается вырастить до 12 цветов в смещениях ±2, над подходящей почвой с воздухом. | Сам снаряд не списывает ману и не задаёт кулдаун. Каждые 100 тиков менеджер принимает сохранённые загруженные display; неизвестные типы удаляет. Класс `ElectroProjectile` имеет скорость 3, жизнь 35–45, урон стрелы 0.5; попадание в проводник даёт 0.5 всем Living в области ±10. Но Electro не зарегистрирован среди встроенных типов. `/mp spawn` — тестовая команда для OP. |"
text = "\n".join(lines) + "\n"

prefix = "`ManaService` регистрирует"
start = text.index(prefix)
end = text.index("\n\n", start)
links = text[text.index("[Mana]", start):end]
paragraph(prefix, "`ManaService` хранит `mana`, `mana_max`, `mana_regen` в PDC игрока: восстанавливает при входе, сохраняет при выходе и каждые 600 тиков, пополняет каждый тик до максимума. Переход GameState в LOBBY или STARTING сбрасывает ману всех онлайн-игроков на базовые значения `values.yml`; это отдельный ресурс от datapack scoreboard. `PerkService` сохраняет выбор перков по UUID в `plugins/lws/perks.json`, загружает при входе и сохраняет при выходе/каждые 600 тиков. Владение изначально пустое; выбор в GUI сам по себе не подтверждает реализованный боевой эффект перка. " + links)

prefix = "`SigmaWarsMain.onEnable` зовёт"
start = text.index(prefix)
end = text.index("\n\n", start)
links = text[text.index("[main]", start):end]
paragraph(prefix, "`SigmaWarsMain.onEnable` вызывает `BedwarsBootstrap.bootstrap`: создаёт регион, пытается связать Ignite callbacks, регистрирует ProtectionListener, BedwarsShopListener, TeamChestManager, TeamZombieEggListener, ShopMobIgnoreListener, BedwarsDropsListener и запускает BeaconEffectsManager. Команды `/bedwars` и `/teaminit` регистрирует CommandRegistry. Отдельный класс BedwarsProtectorPlugin существует, но main в plugin.yml — SigmaWarsMain; регистрация альтернативного класса этим не подтверждается. " + links)

prefix = "- Beacon buff timers"
paragraph(prefix, "- `BeaconEffectsManager.start()` раз в секунду уменьшает командные таймеры. Эффекты получают онлайн-участники scoreboard team, пока у хотя бы одного участника есть `have_bed`. Усилители: SPEED 1, JUMP_BOOST 2, FIRE_RES 0, STRENGTH 1, HASTE 1, REGEN 1, RESISTANCE 0; длительность — оставшиеся секунды. Магазин продаёт соответственно 5/5/15/2/10/3/5 минут, каждое за 5 звёзд Незера; покупки суммируют время. `resetAll()` обнуляет таймеры и снимает семь эффектов у всех онлайн-игроков; его вызывают `/bedwars init|stop`.")

# The preceding bullet is part of a continuous list; paragraph() also selected
# subsequent bullets up to the next blank line. Keep those reviewed bullets.
# Recover them from the original file to avoid dropping their behavior.
original = path.read_text(encoding="utf-8")
shop_start = original.index("- Магазин:")
shop_end = original.index("\n\n", shop_start)
text = text.replace("\n\n## Ignite hook", "\n" + original[shop_start:shop_end] + "\n\n## Ignite hook", 1)

prefix = "В source `BedwarsBootstrap`"
paragraph(prefix, "`BedwarsBootstrap` вызывает `Class.forName` для BedwarsHooks и регистрацию callbacks через reflection; отсутствие класса даёт warning и не прекращает bootstrap. Manifest Ignite и mixin config связывают мод с точками внедрения, но их применение на сервере здесь не проверялось. `plugin.yml` ссылается на `me.luckywars.bedwars.mixin.BedwarsMixinBootstrap`, которого в просмотренном Java-дереве нет; по этому одному факту нельзя установить результат запуска дескриптора. Наличие build/JAR тоже не подтверждает работу на сервере.")
paragraph("Точный manifest просмотренных", "Перечень просмотренных исходников, SHA-256 и найденных регистраций — [plugin_items_review.json](inventory/plugin_items_review.json). Это результаты чтения кода, а не игровых испытаний.")
paragraph("В `SigmaWarsMain.onDisable`", "В `SigmaWarsMain.onDisable` явно очищаются itemQueues, регенерация, legacy runtime, загрузчик карт и физика. Отдельные cancel/reset для ManaService, MagicProjectiles, UsableItemManager, PerkService и предметных слушателей здесь не вызываются; их задачи/события создаются с владельцем-плагином. Это описание явного cleanup-кода, не доказательство поведения серверного disable. При обычной работе MagicProjectiles удаляет display после попадания, истечения жизни или ошибки и удаляет неизвестные типы при восстановлении. Задачи Malevich/MetalPipe работают в течение жизненного цикла плагина.")

for old, new in {
    "## Bedwars listeners: beacons, protection, shop, eggs": "## BedWars: маяки, защита, магазин и яйца",
    "## Lifecycle и очистка": "## Жизненный цикл и очистка",
    "незеритовые звёзды": "звёзды Незера",
    "не среда выполнения": "не испытания на сервере",
    "Для таблица добычи datapack": "Для таблиц добычи datapack",
}.items():
    text = text.replace(old, new)
path.write_text(text, encoding="utf-8")
