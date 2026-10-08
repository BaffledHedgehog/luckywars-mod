"""Coordinator corrections from all 27 BedWars sources and caller commands."""
import json
from pathlib import Path

notes = Path(__file__).resolve().parents[1]
p = notes / 'lbc_bedwars.md'
s = p.read_text(encoding='utf-8')
old = 'Затем `check_bed_get_team` выполняет 32 команды'
assert old in s
s = s.replace(old, '`check_bed_get_team`, вызванный **до start1**, выполняет 32 команды')
start = s.index('затем выполняет `team leave @s`, если только')
end = s.index('\n\n`tick_player_1s_for_respawn`', start)
s = s[:start] + '''затем пытается выполнить `team leave @s` через `unless score @s[gamemode=spectator,tag=!have_bed] respawn_timer matches 1..`. **Команду покидают spectator без кровати**: им в начале уже поставлен timer=0, поэтому unless проходит. Survival-игроки и игроки с кроватью этим helper команду не покидают: отфильтрованный @s не найден, resolver score holder выбрасывает ошибку до проверки unless. Пустой selector в score-команде нельзя трактовать как условие false с последующим логическим отрицанием. Spectator с кроватью проходят положительный timer/respawn; если в этом же проходе они возродились, последняя проверка также не находит spectator @s и не снимает команду.''' + s[end:]
s = s.replace('общий `game/tick` отдельно ограничен `gamestat=2..4`',
              'общий `game/tick` отдельно ограничен `gamestat=2..4`, `gamemode=3..4` и `developer!=1`')
s = s.replace('Вызов `start_player_tp` закомментирован в `start`, а его единственный внутренний выбор вызывает отсутствующую',
              'Вызов `start_player_tp` закомментирован в `start`. При ручном запуске helper уменьшает общий score players на 1, выбирает случайного игрока без have_bed и ставит current, вызывает setup для **всех** игроков с current; старые current здесь не снимаются. Пока players>=1, рекурсивно повторяет проход. Внутренний выбор вызывает отсутствующую')
s = s.replace('о зависании игры этот source не свидетельствует.',
              'о зависании игры этот source не свидетельствует. Счётчик players локально не инициализируется — ручной вызов зависит от заранее заданного значения.')
s = s.replace('Внешние эффекты', 'Внешние эффекты')
if '## Команды и цвета' not in s:
    s += '\n## Команды и цвета\n\n'
    s += 'Для bed marker и стеклянного столба используется одна таблица (цвет tellraw местами иной):\n\n'
    s += '| Команда | Цвет кровати и стекла |\n|---|---|\n'
    for n, color in enumerate(['light_blue','black','blue','cyan','purple','gray','green','magenta','brown','orange','light_gray','lime','pink','red','white','yellow'], 1):
        s += f'| {n} | `{color}` |\n'
if '## Проверенные исходники' not in s:
    jobs = json.loads((notes/'inventory/research_jobs.json').read_text(encoding='utf-8'))
    sources = jobs['lbc_bedwars']['sources']
    assert len(sources) == 27
    s += '\n## Проверенные исходники\n\n'
    s += 'Координатор прочитал все 27 функций и сопоставил внешние dispatchers. Исходники сохранены без изменений.\n\n'
    s += '\n'.join('- ['+x.split('/function/')[1]+'](../'+x+')' for x in sources)+'\n'
p.write_text(s, encoding='utf-8')
print('BedWars corrected; all 27 sources linked.')
