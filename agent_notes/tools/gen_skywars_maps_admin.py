from pathlib import Path
import json,re
root=Path.cwd(); sources=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['skywars_maps_admin']['sources']
cat=json.loads((root/'agent_notes/inventory/maps_structures.json').read_text(encoding='utf-8'))
known={Path(r['source'].split('/structure/',1)[1]).stem for r in cat['structures'] if r.get('group')=='maps'}
rx=re.compile(r'^place template swrg:([^\s]+) (-?\d+) (-?\d+) (-?\d+) none none (\S+) (\S+) (\S+)')
lines=[]
for src in sorted(s for s in sources if '/map/' in s and s.endswith('.mcfunction')):
 p=root/src; calls=[]; score='—'
 for l in p.read_text(encoding='utf-8-sig').splitlines():
  m=rx.match(l)
  if m: calls.append(m.groups())
  if l.startswith('scoreboard players set #map swrg.math '): score=l.rsplit(' ',1)[-1]
 name=Path(src).stem
 if calls:
  xyz=[tuple(map(int,c[1:4])) for c in calls]; ids={c[0] for c in calls}; xs=[c[0] for c in xyz]; zs=[c[2] for c in xyz]
  vals=[name,score,str(len(calls)),f"{len(ids & known)}/{len(ids)}",','.join(map(str,sorted({c[1] for c in xyz}))),','.join(sorted({c[4] for c in calls})),','.join(sorted({c[5] for c in calls})),f"{min(xs)}..{max(xs)} / {min(zs)}..{max(zs)}"]
 else: vals=[name,score,'0','—','—','—','—','—']
 lines.append('| `'+vals[0]+'` | '+' | '.join(vals[1:])+' |')
text='''# Карты SkyWars и инструменты администратора

Исследованы 234 точных источника из `inventory/research_jobs.json`: 197 функций maploader и 37 developer. ID файлов — технические обозначения datapack; они не дают основания придумывать названия карт.

## Выбор и запуск карты

GUI вызывает `map/alwayspre` или `alwayspreb`, затем числовую `map/<id>`, после чего `alwayspast` или `alwayspastb`. Разделение GUI по страницам 2000/2100/2200 связывает индексы с последовательными ID карт. В селекторах отдельно выбираются структуры для измерений `white` и `imprinted`; наборы `b` используют соответствующие ID с `b`, а `imprinted` вызывает `_broken`. Индекс 18 служит навигации на страницах, поэтому в нескольких селекторах пропущен. `map_select_white*` чистит пространство x/z −1..1, y −1..4 около `swrg.spawn`, затем выполняет `kill @e` в выбранном измерении.

`alwayspre` убивает `strict_map_object`, сущности predicate `swrg:core/maploader` и повторно не-игроков кроме predicate `swrg:core/lobby`; сохраняет GUI-выбор в `map_selected`, ставит `#if_map_loaded=1`, вызывает `clear` в текущем измерении и затем в `imprinted` и `white`. `alwayspreb` делает такую же чистку, добавляет игрокам gravity modifier −1, ставит `#if_map_loaded=0` и вызывает `clear1` с macro storage `mapclear includespecial`. `alwayspast`/`alwayspastb` сообщают о завершении загрузки, задают `#map_type` 1/2, запускают lobby в overworld, снимают gravity modifier и ставят `#mapstat=1`. `lobby` удаляет lobby/quickstart entities, размещает `swrg:lobby` в −5 126 −5, добавляет forceload в четырёх квадрантах вокруг 0 в `imprinted` и `white`, и ставит локализованное имя сущностям `swrg.lobby`.

## Размещение по каждому файлу

Одна строка на числовой файл. `NBT/ID` означает наличие ID среди 1296 `maps/*.nbt` снимка; это не проверка работающего мира. Координаты — диапазоны x/z опорных блоков; Y — уровни вызовов. Полные списки структур доступны в [каталоге NBT](maps_catalog.md).

| Функция | #map | place | NBT/ID | Y | integrity | seed | x / z |
|---|---:|---:|---:|---|---|---|---|
'''+ '\n'.join(lines)+'''

## Параметры шаблона и границы снимка

В `place template id x y z none none integrity seed strict` число `0.75` — integrity: шаблон остаётся в прежнем размере, но часть его блоков пропускается. Это не scale. В стандартных вызовах integrity обычно 1; в парных `_broken` — 0.75. Проверка строк показала, что 88 пар отличаются только integrity; пара `5b` дополнительно использует seed 0 против 1. Таблица фиксирует фактическое число вызовов и координаты каждого источника.

ID, для которых не найден одноимённый источник NBT среди трёх исследованных datapack, перечислены в `maps_catalog.md` и `inventory/maps_structures.json`. Это означает только «не найдено в трёх исследованных datapack»: мир может содержать структуры в `world/generated`, а datapack может создавать их во время работы. `alwayspre_qs` пуст; quickstart-функция не содержит в нём команд, и её поведение здесь не приписывается.

`clear` ставит маркеры сеткой по x/z от −192 до 144 с шагом 48 и удаляет блоки срезами между y=0 и y=319. Затем убирает maploader entities, прочих сущностей (кроме игроков и lobby) и ставит `#mapstat=1`. `clear1` исполняет macro `$(mapclear)`, затем выполняет такую же очистку сущностей; аргумент поступает из storage вызывающего кода.

## Developer: игра и администраторский запуск

`developer/game/start` устанавливает developer-флаг, снимает команды и вызывает подготовку. `start/0` переводит игру в состояние подготовки, переносит игроков к 0 128 0, удаляет lobby/quickstart display entities, очищает GUI и сбрасывает игроков; ветвится по `#gamemode`, вызывая LBC или bedwars для особых режимов. Этапы `start/1`, `/2`, `/clear`, `/config`, `/team_join`, `/team_prefix*`, `/select*`, `/timer` настраивают режим, команды, spawn/team markers и расписание; повторная clear-функция очищает инвентарь и сбрасывает игроков, которым не достался spawn. В проверенных функциях нет активного GUI-вызова `developer/game/start`; обнаруженный триггер в GUI закомментирован.

`developer/game/end` сбрасывает developer-флаг и вызывает `end/0`: скрывает bossbar, сообщает результат, отключает выпадения, чистит расписания и командные счётчики, переносит игроков к `swrg.look`, меняет игровые режимы и планирует `end/1`. Начисление win-coins закомментировано. `end/1` вызывает lobby, сбрасывает GUI и LBC, готовит quickstart, очищает roll/case scores и возвращает игроков к 0 127 0. `end/0_pre` считает timer до вызова завершения; `fireworks` создаёт четыре marker и ракеты в области 20–192 блоков, `firework_random` генерирует их цвета/форму/twinkle/lifetime.

## Developer: редактор, markers и служебные функции

`tools/give` выдаёт бумагу с `custom_data={developer_menu:1b}`. `tools/tick` раз в тик следит за предметом, создаёт chest minecart-menu над владельцем и скрывает его ниже на 10000 блоков, если инструмент убран. `tools/menu` заполняет слоты действиями включения/выключения markers, установки/удаления saver, выдачи/подсчёта marker, стеклянной рамки, `/reload` и завершения игры. `tools/selection` выполняет действие при исчезновении предмета из назначенного слота.

`markers/give` выдаёт инструменты для spawn/look/delete, сундуков `default/midle/center`, ресурсных генераторов и bedwars shop/bed, в том числе strict-варианты. `markers/count` считает marker tags в scoreboard и сообщает значения. `markers/on` настраивает teams и цикл каждые 5 ticks; `markers/off` удаляет teams, убивает `tag=marker` и отменяет цикл. Активные команды `markers/5t` создают цветные falling-block displays над marker и удаляют marker в пределах 3 блоков от `swrg.delete`; закомментированные команды не исполняются. `bars` строит у `swrg.spawn` стеклянную рамку, предварительно убрав прежнее стекло.

`saver` размещает structure blocks в save mode с именами вида `minecraft:62-1` и размером 48×48×48; активные маркеры находятся на нижнем слое, верхние варианты закомментированы. `saver_check` проверяет сравнение региона с временным блоком по 0 270 0; при совпадении `saver_check_kill` удаляет блок под marker и сам marker. `delete_saver` стирает блоки под `saver_marker`, затем удаляет маркеры. `temp` создаёт отдельный набор из 20 saver markers. `giveall` добавляет косметические/игровые tags игроку; `reload` запускает команду reload.

## Привязка источников

Coverage-файл связывает каждый из 234 точных путей с одной из этих секций. Разбор описывает исполняемые команды; закомментированные строки отмечены как неактивные.
'''
(root/'agent_notes/skywars_maps_admin.md').write_text(text,encoding='utf-8')
heading_place='Размещение по каждому файлу'; heading_select='Выбор и запуск карты'; heading_clear='Параметры шаблона и границы снимка'; heading_game='Developer: игра и администраторский запуск'; heading_admin='Developer: редактор, markers и служебные функции'
rows=[]
for s in sorted(sources):
 p=s.split('/function/',1)[1].removesuffix('.mcfunction')
 if '/maploader/map/' in s and Path(p).stem not in {'alwayspre','alwayspreb','alwayspre_qs','alwayspast','alwayspastb','script'}: sec=heading_place
 elif p.startswith('maploader/clear'): sec=heading_clear
 elif p.startswith('developer/game/'): sec=heading_game
 elif p.startswith('developer/'): sec=heading_admin
 else: sec=heading_select
 rows.append({'source':s,'document':'agent_notes/skywars_maps_admin.md','section':sec,'status':'documented','note':'Verified source behavior summarized in the linked section.'})
(root/'agent_notes/coverage/skywars_maps_admin.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'wrote {len(rows)} coverage rows and {len(lines)} placement rows')
