# Проверки пяти семейств зелий маны

Область этой заметки — пять семейств `add_manamax`, `add_manaregen`, `add_mana`, `remove_mana` и `temporary_mana` в трёх формах (drink, splash и lingering). Есть 15 таблиц, 39 записей и 35 уникальных payload-вариантов. Из обычного `transformables_potion` перенесены девять записей: четыре drink-семейства с разными весом/условием удачи и только lingering для `remove_mana`. Sentinel усиления 16 (boost) остаётся в отдельном `mana-core`; Crux и negative-combat сюда не входят.

Обычные payload имеют исходные длительности: 16 — drink 4800, 17/18/19 — drink 1, 20 — lingering 20. Параметры 3600/9600 и маркер `imba:1` относятся к отдельным imba-записям. Проверка не требует, чтобы случайный roll каждой таблицы выдал все варианты: точное соответствие наборов и записей проверяет `tools/check_design_sources.py`; runtime-проверка сверяет реально выбранный stack и Polymer-преобразование.

## Разрезы smoke-runner

- `mana-potions-menu` выполняется в лобби: все 15 таблиц дают правильные серверные native-типы, sentinel/цвет/lore/name и Polymer roundtrip сохраняют ожидаемые компоненты.
- `mana-potions` выполняется в отдельном матче после `mana-core` и его cleanup. Здесь проверяются native drink/splash/lingering источники, обычная duration-1 конверсия, адресаты игрок и обычная сущность, временные правила и observer-проекция.
- `mana-potions-cleanup` выполняется после штатного `sw stop`: временные таймеры и ожидающие преобразования очищены, постоянные max mana/base regeneration остаются, mana sentinel эффекты не сохраняются.

Фокус-команда: `tools/smoke_lbc_loadouts.py --mana-potions-only --server-dir <isolated-server>`. Она использует реальные переиспользуемые `TestAlpha`/`TestBeta` fixtures и один матч. Обычный `--mana-only` продолжает проверять только базовый mana runtime.

## Поведение и очистка

Для эффектов 17–20 используются настоящие loot stacks и native источники. Native drink завершается через item-use путь; splash применяется через `ThrownSplashPotion.onHitAsPotion`; lingering создаётся через `ThrownLingeringPotion.onHitAsPotion` и коротко продвигается как native cloud без ожидания полного времени жизни. Для обычного sentinel длительностью 1 проверяется немедленное преобразование до истечения native эффекта. Для `remove_mana` cloud сохраняет обычную длительность 20 и выдаёт мобу quarter-duration эффект.

Для splash проверяются дистанционное ослабление и адресация эффектов игрокам/обычным сущностям; detached Adventure observer эффекта не получает. Creative и developer не блокируют использование. Проверки таймеров закрепляют согласованные правила: amplifier 16 и 17–20 уменьшаются на единицу за mana tick; `time_stopped` останавливает все sentinel; `sin_greed_active` приостанавливает только remove mana; beacon не блокирует эффекты 17–20, хотя штатная регенерация при нём приостановлена.

Для remove mana проверяется гарантированный итог `-2000 mana`; повторными pulse проверяется вложенная случайность штрафов maximum/base regeneration, где потеря base regeneration сопровождается потерей maximum. Source skill 19 при включённом `lbcSkills` обнуляет remove mana timer после конверсии; тест восстанавливает профиль и настройку.

Существующий FakeBot lifecycle проверяет death/respawn и match-stop очистку. `clearEffects` обнуляет boost/stat timers и отцепляет ожидающие native conversions, но не откатывает постоянные значения mana, maximum и base regeneration.

После интеграции root исправил вызовы фикстуры на `ItemStack.finishUsingItem`
(включая native use remainder) и `ServerLevel.tickNonPassenger` для cloud,
чтобы пяти­тиковая фаза включала `commonTick`. Имена imba проверяются по всем
15 полным ключам с учётом формы. Адресный прогон прошёл за 57,20 с (4 группы,
1 матч), полный — за 313,34 с (53 группы, 19 матчей) на одном JAR.
[SHA, логи и пределы проверки](../SERVER_TESTS.md).
