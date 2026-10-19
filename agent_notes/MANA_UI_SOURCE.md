# Mana UI: source contract

## Старый datapack: что показывается

Основной mana HUD отправляется в **actionbar** из `data/lbc/function/ui/get_data.mcfunction`: сборка компонента начинается строками 1–6, а отправка находится в строке 100. Этот обработчик вызывается из `data/lbc/function/players_tick.mcfunction:48` каждый проход обработки игрока, если `legacy != 1` и `lbc.challenge != 1`. Поэтому HUD обновляется регулярно в игровом тике; отдельного экрана или постоянного title для маны здесь нет.

Порядок и вид компонента:

1. Символ маны и цвет glyph `#4E5C24`.
2. Один пробел, текущий score `mana`, затем `§a/`, score `manamax`.
3. ` §a[§a+`, score `manaregen_visual`, `§a]`.

Для `mana >= 0` выводится glyph `\uE100`, значение `mana` aqua. Для отрицательных значений выводится `\uE132`; цвет только числа `mana` меняется по диапазонам: `-1..-5000` yellow, `-5001..-10000` red, `-10001..-15000` dark_red, `-15001..-20000` light_purple, ниже `-20000` dark_purple. Максимум и regen остаются aqua, разделители используют исходные буквальные `§a`-коды. Не нормализовать цветовые escape или spacing при переносе.

В этом же actionbar после mana условно добавляются glyph/счётчики эффектов и предметов (например, stopper, cold/frozen, нитро, заряды и cooldown-и); они добавляются в том же `storage lbc.math ui`. Их порядок определяется последовательностью команд в `get_data.mcfunction`. Отдельные UI состояния в `tick_nexus_player.mcfunction` и `ui/get_data.mcfunction` также используют actionbar; это конкурирующие записи в тот же канал, не часть mana component.

Условия: основной вызов `get_data` пропускается при `legacy == 1` и при `lbc.challenge == 1`. Если mana score не соответствует ни одному из перечисленных диапазонов, начальный компонент не задаётся; не подставлять в таком случае придуманную надпись/нулевое значение. В `get_data.mcfunction` отправка actionbar также дополнительно исключена при `legacy == 1`.

## Glyph и переводы

Внешний `all_in_one_1.21.8` подтверждает `\uE100` как `minecraft:font/icons/mana.png` (`assets/minecraft/font/icons.json`, также provider в `font/default.json`) и `\uE132` как `minecraft:font/icons/negative_mana.png` (`assets/minecraft/font/default.json`). Для совпадения вида клиенту нужен этот resource pack.

Mana HUD **не использует translate key**: текстовые части — score components, literal-разделители и glyph. В ресурс-паке имеются ключи вроде `mana` (значения `Мана: ` / `Mana: `), `mana_on_activate` и `mana_on_use`, но это не подписи этого actionbar и добавлять их в HUD нельзя. В соседних виджетах встречается существующий `ready`, но он тоже не часть mana component.

## Новый мод: текущее состояние и приоритет

В Java-источниках нового мода не найдено mana actionbar или пользовательской настройки его показа. Есть mana score bridge, но комментарий в `LbcColors.java` явно относит mana regeneration/UI к будущему переносу.

Уже существует отдельная actionbar-запись ракеты в `LbcItems.java:272–277` (`rocketHud`), вызываемая при обновлении rocket boots; при `rocketRemove` (`:281–284`) actionbar очищается пустым компонентом. Следовательно, простая периодическая отправка mana actionbar будет конфликтовать с rocket HUD: клиентский actionbar содержит последнюю полученную запись. Перенос должен определить совместное составление/приоритет этих двух записей; старый datapack подтверждает, что его mana и остальные HUD-индикаторы собираются в один actionbar, но он не определяет приоритет нового rocket HUD. Найденных mana UI settings/toggles в новом моде нет.

## Короткий review переноса mana + rocket HUD

Проверены `LbcMana.hud` и `LbcItems.refreshHud` относительно старого `get_data.mcfunction:1–6,90`. В mana компоненте совпадают glyph `\uE100`/`\uE132`, цвет glyph `#4E5C24`, пробел после glyph, aqua `mana` при неотрицательном значении, исходные отрицательные цветовые пороги, `§a/`, aqua maximum, строка ` §a[§a+`, aqua regeneration и закрывающий `§a]`. Цвета частичных компонентов заданы отдельно; унаследование от корневого glyph не должно окрашивать score-компоненты вместо их явных цветов. В тексте нет слов и translate keys.

Rocket suffix совпадает с исходной строкой `get_data.mcfunction:90`: четыре пробела + `\uE173` + пробел, белый счётчик прыжков, ` - `, белый счётчик cooldown. Glyph оставлен без дополнительной подписи/перевода. Combined actionbar сейчас содержит только mana и перенесённый rocket suffix; остальные старые статусы/cooldown-индикаторы остаются будущей работой, поэтому это намеренное ограничение текущего переноса, а не полный эквивалент исходного `get_data`.
## Точные источники

- Старый datapack: `data/lbc/function/players_tick.mcfunction:48`, `data/lbc/function/ui/get_data.mcfunction:1–6, 8–100`, `data/lbc/function/tick_nexus_player.mcfunction:7–11`.
- Новый мод: `src/main/java/me/skywars2/game/LbcColors.java` (комментарий об отложенном mana UI), `src/main/java/me/skywars2/game/LbcItems.java:272–284`.
- Только для чтения resource pack: `assets/minecraft/font/icons.json`, `assets/minecraft/font/default.json`, `assets/minecraft/lang/ru_ru.json`, `assets/minecraft/lang/en_us.json`.

## Текущий код и проверки

Исторический контракт выше описывает исходный datapack и прежний срез переноса. Текущее объединение статусов actionbar зафиксировано в [ACTIONBAR_STATUS_FIX.md](ACTIONBAR_STATUS_FIX.md). `ui-gameplay` PASS: 55,66 с / 2 матча / 19 групп; GUI/quickstart PASS: 126,58 с / 3 матча / 7 сценариев. SHA `afb96b4ec69ddd27499eed92cf2f6e5ba7cb1738df7d8e45fde9a77650c13b61`, артефакт `build/verification/skywars2-ui-lobby-acid-afb96b4e.jar`. Общая регрессия на 78 групп и проверка реальным клиентом не проводились; JAR не установлен на основной сервер (активен `341c6293`). Итог: [SERVER_TESTS.md](../SERVER_TESTS.md).
