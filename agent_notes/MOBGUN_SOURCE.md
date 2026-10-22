# Mobgun: исходное поведение

## Поздние решения пользователя — 21 октября 2026

Самонаведение выбирает только врагов (user). Для Mobgun выбрано одно случайное зелье с правильным типом splash/lingering вместо legacy-четвёрки; cooldown остаётся 50 тиков/2,5 секунды, lore меняется с 3.5 на 2.5 (user). Ниже сохранено историческое описание исходника и обнаруженных неоднозначностей; эти вопросы теперь решены. Техническое продолжение и незавершённая интеграция — [MOBGUN_NEXT](MOBGUN_NEXT.md).

Аудит 21 октября 2026. Это описание прочитанного исходника, не подтверждение переноса или серверного теста. Изменён только этот документ.

Корень старых путей ниже: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Ресурс-пак прочитан из актуального пользовательского пути `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/` (без `(1)`). Старый сервер и пак не изменялись.

## Что это

Мобомёт создаёт случайную настоящую сущность и бросает её по взгляду. Захвата существ, магазинов, патронов, зарядки, собственного крафта, Academy-версии и переключения режимов в найденной реализации нет. Это не луч урона и не собственный движок полёта.

Полный список 114 исходов, точные точки появления, NBT и экипировка находятся в `lbc/function/workingitems/mobgun.mcfunction:5–118`; их нельзя заменять сокращённым списком обычных мобов. Здесь есть боссы, транспорт, снаряды и служебные носители пассажиров.

## Ввод, цена и полёт

- Вход: `lbc/function/items/spells.mcfunction:7,13` → `workingitems/spell1(.mcfunction)` / `spell1_left.mcfunction` → `spell1_default.mcfunction:35` / `spell1_default_left.mcfunction:9` → `mobgun.mcfunction`.
- Обе руки используют один `coldown16`. Выстрел ставит 50; `players_5t.mcfunction:4` снимает по 5 раз в 5 тиков. Номинально 2,5 секунды, с фазой общего пятикового обработчика. `resets.mcfunction:30` и `other/purify.mcfunction:10` обнуляют. KFC `all_up_scores_no_mana.mcfunction:12` добавляет 1.
- Мана, предметы и боеприпасы не списываются. Sneak не выбирает другой режим. Общие запреты магического ввода и `nomagic_active` остаются во входной цепочке. Локально проверяется `stopper` R20, а не `stopper_magic`.
- `mobgun.mcfunction:1–3`: звук `minecraft:block.piston.extend`, master, volume 1, pitch 2; равномерный random 1..114; глобальный `final_luck_modifier=7` принудительно меняет исход на 22 (дракон).
- Появление от глаз по взгляду с индивидуальным отступом 0,5–10 блоков. Дракон: отступ 10, Health 15, DragonPhase 4; wither: отступ 3, Health 15. Обычные мобы сохраняют нативное AI.
- Хвост `mobgun.mcfunction:122–131` вычисляет вектор длиной 3 через временный marker в мировом начале координат, записывает Motion только ближайшему `raycastmob,prestartf`, копирует UUID стрелка в оба поля `Owner` и `owner`, затем удаляет marker и снимает `prestartf` со всех сущностей. Это попытка native ownership, не доказательство авторства всех мобов/пассажиров.
- `raycastmob` не имеет своего tick/hit-обработчика в LBC: поиск вне двух mobgun-файлов не дал результатов. Дальше работают нативные физика, AI и столкновения, без общего TTL.
- Носители исходов 48 (shulker), 82 (end crystal), 84 (fang marker), 92 (lightning): невидимый маленький armor stand с `killwhenonground`. `tick1s.mcfunction:5` убивает его на земле раз в секунду; в `tick1s_not_nexus.mcfunction` эта строка закомментирована.
- Fang-пассажир имеет `rc_5t,magic`, `data.lifetime=600`: `raycasters_5t.mcfunction:11` вызывает fangs на Y−0,25 каждые 5 тиков; общий Lifetime в том же файле ограничивает его 600 тиками. UUID стрелка напрямую этому пассажиру/fangs не назначается.

## Особые исходы и пробелы

75 вызывает `workingitems/mobgun_random_lingering_potion.mcfunction`: random 1..86, 1..43 содержат lingering item, 44..86 splash item. Но обе половины создают entity `minecraft:lingering_potion`.
При значении 43 одновременно создаются weakness, weaving, wind_charged и oozing. Общий хвост назначает скорость и owner только одной ближайшей сущности. Это подозрительная ошибка/особенность, требующая явного решения, не молчаливого исправления.

Другие важные исходы: 86 falling anvil (Time 0, FallHurtAmount 10, FallHurtMax 20); 87 fireball ExplosionPower 1; 88 firework LifeTime 40 и исходные explosion-компоненты; 96 spawner minecart (dragon, SpawnCount 20, SpawnRange 50, Delay 100, Min/MaxSpawnDelay 1000/2000, RequiredPlayerRange 50); 98 TNT fuse 40; 101 warden с dig_cooldown 1200; 108 invulnerable creaking. Включены happy_ghast, nautilus, zombie_nautilus, copper_golem, camel_husk, parched (109..114).

Нативные Owner/owner не дают произвольным мобам защиту союзников и наследование убийств. Пассажиры, AI-атаки, новые выпущенные ими снаряды, взрывы, облака, огонь, падение и размножение/призывы требуют общего авторства при переносе. Исходный пробел не считается желаемым поведением.

Основная рука содержит ещё одну подозрительную конструкцию: `unless score @s[nbt={SelectedItem:...mobgun...}] coldown16 matches 1..`. При неподходящем предмете селектор не даёт score, и `unless` может пропустить ветку. Offhand имеет корректное отдельное `if items`. Не переносить эту ошибку как намеренную стрельбу из любого жезла.

## Выдача и точное оформление

`lbc/loot_table/transformables_shooter.json:71–225` содержит две одинаковые выдачи: обычная weight 10 и weight 10000 при `final_luck_modifier=7`. Предмет carrot_on_a_stick, count 1, tooltip `lbc:tier_d`, item_model `lbc:mobgun`, custom_data `{transformable:1,shooter:1,spellwithcooldown:1,magicwand:1,mobgun:1}`.
Имя — `[translate mob_gun, italic=false]`, без явного цвета. Lore ровно три строки: (1) aqua `type` + `shooter`, оба italic=false; (2) aqua буквальное `3.5 ` + `recharge_time`, оба italic=false; (3) white `mob_gun.lore`, italic=false. **Lore 3.5 секунды расходится с реальными 50 тиками.** Сохранение оформления и исправление баланса здесь разные решения.

Производители таблицы: `lucky_items.json`, `lucky_items_high_quality.json`; обёртка `transformables_shooter_all.json` используется в `27_shooters.json` и `random_item.json`. Перековка shooter: `function/reforge.mcfunction:8`, `reforge_crafting_0..3.mcfunction:8`, `ultra_reforge.mcfunction:8`; трансформация — `other/transform.mcfunction:9`. Специального mobgun item modifier/крафта поиском `mobgun|mob_gun` не найдено.

RP уже содержит всё оформление, новые ключи и template не нужны:

- `assets/lbc/items/mobgun.json` → model `lbc:item/mobgun`.
- `assets/lbc/models/item/mobgun.json` → parent `minecraft:item/handheld_rod`, layer0 `minecraft:item/mobgun` → `assets/minecraft/textures/item/mobgun.png`.
- `assets/minecraft/lang/{ru_ru,rpr,en_us}.json:429–430`: `mob_gun`, `mob_gun.lore`. RU/rpr: «Мобомёт», «Выстреливает случайным мобом»; EN: «Mobo-gun», «Shoots a random mob».
- `lbc/advancement/true_advancements/shooter/mob_gun.json`: прежняя иконка `lbc:mobgun`, display keys `lbc.adv.shooter.mob_gun` и `.lore`; выдаётся при драконе в `mobgun.mcfunction:120`. Прежние значения обоих языков уже есть в RP:1211–1212. `kfc_gun` имеет этого родителя.

## Целевой перенос и вопросы

Все снаряды могут попадать в дружественных МОБОВ, но должны защищать союзных ИГРОКОВ (user, передано в этой задаче). Guards служебных NPC и наблюдателей сохраняются. Это новое общее требование, не свойство исходника. Решение о выборе целей самонаведения пока ожидается; сам Mobgun своего наведения не содержит.

До реализации явно решить: 50 тиков против lore 3.5; четыре зелья в исходе 43; splash item внутри lingering entity; поведение опасных native исходов/пассажиров и наследование владельца. Исправление неверного mainhand dispatch должно быть отмечено отдельно.

Предлагаемое размещение: существующий `LbcShooters` уже владеет стрелковым вводом, KFC native мобами, cooldown, owner и cleanup; регистрацию/ввод подключить через `LbcWands` и `LbcItems`. Переиспользовать `LbcIndirectKills`, общие target guards, managed native TNT/lightning/projectiles, паузу TimeStop и lifecycle матча. Не добавлять второй глобальный индекс целей или движок снарядов. Если полный набор native исходов перегружает Shooters, допустим один связный `LbcMobgun` для spawn/cooldown/native ownership/lifecycle, с делегированием прежним helpers; существующий `LbcIndirectKills.summonedOwner` тогда должен читать его owner, а не отдельный трекер убийств. Список native сущностей обязан учитывать пассажиров и созданные ими последствия; отсутствие сохранения в чанки/очистку нельзя доказать одним тегом.
