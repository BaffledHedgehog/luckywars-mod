from pathlib import Path

p=Path('agent_notes/lbc_other_world_tech.md')
s=p.read_text(encoding='utf8')
old='на Lifetime39 обычная ветка снимает владельца с транспорта и ставит pad5×1×5, временно forceload-ив его чанк.'
new='на Lifetime39 найденный owner даёт вход в roadgun_end, но executor остаётся display: ride dismount применяется к нему, а не к игроку. Pad5×1×5 ставится в позиции display, временно forceload-ив его чанк. Игрок окончательно теряет mount при удалении display на40; ранний sneak удаляет его без pad.'
assert old in s
s=s.replace(old,new)
anchor='без попадания cooldown не устанавливается. Добавленный caster-tag'
new='без попадания cooldown не устанавливается. Каждый sim_cube ставит black concrete и независимо с шансом50% создаёт потомка по каждому из6 соседних направлений, если там ещё не black concrete; уничтожает не-mobs в1 блоке кроме nexus_rc/nexus_entity, наносит10 outside_border одному mob без owner/team-фильтра и удаляет себя. Шанс0.25% удаляет все sim_cube markers без радиуса. Добавленный caster-tag'
assert anchor in s
s=s.replace(anchor,new)
anchor='и снимает один заряд. Отдельно пока меч выбран'
new='и снимает один заряд. Оба hand-входа используют один use-helper, читающий и перезаписывающий mainhand: offhand-dash не меняет атрибуты самой offhand-катаны. Отдельно пока меч выбран'
assert anchor in s
s=s.replace(anchor,new)
old='В проверенном дереве LBC-function references по тегу `supply_drop` другого interaction-handler не найдено, поэтому механизм фактического получения этими источниками не подтверждён.'
new='Прямой JSON-handler convert_arrows_supply есть: клик interaction supply_drop стеком минимум2 arrow вне Nexus/Imprinted запускает обмен стрел на патроны, но не получение оружия со стоек. Один проход пытается удалить2 arrow и создаёт lbc:magnum_ammo у ближайшего non-spectator игрока; повторяется до32 проходов, пока хотя бы один container-стек содержит2 arrow. Infinite_arrow отключает clear, но не выдачу, поэтому при наличии обычного стека≥2 возможны32 бесплатные попытки. Механизм снятия Magnum/Katana со стоек этими функциями не подтверждён.'
assert old in s
s=s.replace(old,new)
anchor='## Файлы: other / nexus'
extra='''## Test Ray: вспомогательный визуальный луч

У click нет найденного внешнего автоматического caller; внутренние9 helpers доступны вручную. Он создаёт test_ray display в(0,−800,0), копирует ID/team и проверяет до400 точек по0.25 блока до solid-блока, без урона. Затем соответствующие ID displays ставятся у глаз raycaster, получают длину scaleZ=0.5×Distance и death_timer20; width уменьшается каждый вызов visual/run, на0 display удаляется. Выбор неготовых display/raycaster глобальный и limit1 без sort. Hit оставляет caster-у тег check_id, который click локально не снимает; зато raycaster после луча снимается. [Вход](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/click.mcfunction), [hit](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_ray/hit.mcfunction).

'''
assert anchor in s
s=s.replace(anchor,extra+anchor,1)
anchor='## Railroad: временный строитель пути'
extra='''Дополнительные точные различия Nuke: обычная ветка выполняет12 air-template размещений (6×дальность25 с integrity0.5 и6×60 с0.25), bruh-попадание —13 (6+7). Каждое создаёт24 creeper: радиусы расположения39/18 по четырём сторонам на высотах0/+40/−40, ExplosionRadius25 у обычной и16 у bruh. В обеих ветках Wither31 на20с до45 блоков исключает только spectator-tag, не союзников. Старый nuke_old задерживается20с без проверки stooper, очищает3 fill-зоны и создаёт9 creeper радиуса30; Wither у него не исключает spectator. Nuket_2 строит пятислойный beacon pedestal, первые20 Lifetime-вызовов чистит колонну доY319 и каждый проход выбирает один из16 цветов стекла. Автоматический nuke1_at cleanup исключает игроков/creeper/strict_map_object и slowparticlesexplode_2, но не другие friendly entities.

'''
assert anchor in s
s=s.replace(anchor,extra+anchor,1)
p.write_text(s,encoding='utf8')
print('Roadgun executor, supply-arrow trade, sim cubes, Nuke and test ray documented')
