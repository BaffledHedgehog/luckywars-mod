from pathlib import Path
import re
p=Path('agent_notes/lbc_other_main.md')
s=p.read_text(encoding='utf-8')
fix={
'blownedbow':'В `transformation` функция запускается, когда в выборке `@e[type=minecraft:tnt,distance=..7,limit=1,nbt={fuse:2s}]` есть TNT. Суффикс `s` в SNBT здесь означает Short: fuse равен двум игровым тикам (0,1 с), не двум секундам. Функция создаёт один item entity с `minecraft:crossbow`: quick charge V и двумя заряженными ракетами, компонентами custom_data для `transformable`, `bow`, `fastexplode`, `craftusable` и tier `s`; затем удаляет исходный marker и создаёт частицы взрыва.',
'blownedbow_old':'Старая ветка вызывается из `transformation_old` при TNT с `fuse:2s` в пределах селектора `distance=..7` (SNBT Short — два тика, 0,1 с). Она создаёт item entity со старой версией кастомного арбалета `ghast_crossbow_old`, содержащей заряженные фейерверки и custom_data старой ветки, затем удаляет marker; детали предмета перечислены в командах ниже.',
'random_mob_spawn':'Сначала выбирает целое значение 1–90; если `final_luck_modifier` равен 7, подменяет его на 22. Дальше ровно одна из 90 условных ветвей вызывает `return run summon`, поэтому создаётся только одна выбранная сущность, а не весь перечень. На результат влияют значения scoreboard и контекст вызова; отдельного caller в исследованных файлах не найден.',
'absurd_child':'Функция выбирает ближайший armor stand с тегом `absurd_child_mom`, удаляет его, затем помещает предметы из loot table `lbc:luckyblock_block_only` в слот `weapon.mainhand` ближайшего allay с тегом `absurd_child`. Она также удаляет текущую сущность `@s`; предмет не выдаётся игроку напрямую.',
'death_compass_attack':'Вызывается после проверки смерти: `death_compass_check` сравнивает X и Z текущей позиции игрока с `LastDeathLocation` (Y не участвует). Здесь расходуется один предмет с custom_data `death_compass`, появляется warden на 15 блоков выше с `Health:150` и `DeathLootTable:"lbc:death_loot"`, игрок получает darkness на 30 тиков (1,5 с), затем выполняется `spreadplayers` с максимальным разбросом 10. Nether-ветка дополнительно ограничивает выборку условием `under 120`; точные селекторы есть в исходнике.',
'arrowgive':'Функция уменьшает scoreboard `arrowgens` на единицу и рекурсивно вызывает себя, пока значение остаётся в диапазоне 1..; сама она не создаёт стрел. В обратном графе найден только её собственный рекурсивный вызов, поэтому внешний вход не установлен.',
'arrowgive_pre':'Считывает количество предмета с custom_data `arrowgenerator` в `arrowgens`, создаёт предметы таблицы `lbc:arrows`, а при появлении рядом item entity с тегом `stand_arrow` выдаёт advancement `infinite_quiver`; также проигрывает звук. Advancement/reward следует читать отдельно от рекурсивной функции `arrowgive`, которая только расходует счётчик.',
'craft_ground':'Это диспетчер наземного крафта: вызывается секундным циклом для выброшенного предмета с `craftusable:1`, только вне `minecraft:nexus` и `minecraft:imprinted`; ингредиенты ищутся рядом с расстоянием 0,001..1. Каждая проверенная пара custom_data направляет в отдельную recipe-функцию; полный перечень условий и результатов приведён в таблице ниже. Перед передачей отмечается ингредиент, чтобы повторная проверка не запустила рецепт повторно.',
'craft_uniwand_with_grimoire':'Когда рядом найден предмет с `libspell:1`, функция увеличивает счётчик spellcount главного wand, вычисляет делитель как floor(spellcount/5) с минимумом 1 и добавляет к wand долю маны grimoire. Затем обновляет tier/value и lore, копирует spellid в ключ `spell$(grim)` и уменьшает количество grimoire через `reduce_count_l`; пока предметы остаются, обработка повторяется. Итоговый порядок и значения tiers задают helper-ы `tier_check`/`value_check`.',
'datafixer':'Это разовая утилита очистки сохранённых блоков: серия команд удаляет LootTableSeed в кубе координат x/y/z от 0 до 39 (63 750 команд в файле). Найденного runtime-caller нет; запуск вручную изменяет мир в этой области, поэтому это не обычный тик/предметный обработчик.'
}
for fid,para in fix.items():
 h='### `lbc:other/'+fid+'`'; i=s.find(h)
 if i<0: continue
 a=s.find('**Фактическая работа.** ',i)
 if a<0: continue
 b=s.find('\n',a)
 s=s[:a]+'**Фактическая работа.** '+para+s[b:]
# Correct a parser artifact: the old command scraper treated effect give as an item grant.
s=s.replace('выдаёт/создаёт предметы или loot `minecraft:resistance`, `minecraft:levitation`, `minecraft:slow_falling`','')
s=s.replace('выдаёт/создаёт предметы или loot `minecraft:darkness`','')
p.write_text(s,encoding='utf-8')
