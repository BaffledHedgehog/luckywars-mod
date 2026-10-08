"""Apply coordinator corrections after reading all 77 armor functions and callers."""
from pathlib import Path

notes = Path(__file__).resolve().parents[1]
p = notes / 'lbc_armor.md'
s = p.read_text(encoding='utf-8')

def replace(old, new):
    global s
    assert old in s, old
    s = s.replace(old, new)

replace('на 16 точках вокруг игрока на высоте +0.6 и +1.6',
        'в 16 позициях X/Z на каждой из двух высот +0.6 и +1.6 (32 particle-команды по 3 частицы)')
replace('loot на Y+100 относительно текущего места вызова',
        'loot на абсолютном Y=100 и X/Z исходной позиции команды; после spreadplayers контекст не переносится автоматически к новому месту игрока')
replace('(то есть область ниже Y=−10)',
        '(объём по Y от −5000010 до −9 с проверкой пересечения hitbox, а не просто строгий порог координаты ног Y<−10)')
replace('Damage dispatcher требует четыре части с ключом `berserk:1` и отсутствия',
        'Damage dispatcher дополнительно требует `hplvl<=20` и predicate `chance70` (70%), четыре части с ключом `berserk:1` и отсутствия')
replace('В этом helper нет команды для снятия modifier; поведение повторного `attribute ... add` с тем же ID может зависеть от обработки дубликата.',
        'В самом helper modifier не снимается; `items/changed_inventory/sel` безусловно удаляет `shulker_armor` при проверке инвентаря, после чего полный набор снова добавляет его тиком. Повторный add существующего ID не накапливает новые modifiers.')
replace('`items/damaged` запускает при попадании, если нет `stopper_magic` в 20.',
        '`items/damaged` запускает при попадании только с нагрудником `shulkshell:1`, predicate `chance20` и без `stopper_magic` в 20.')
replace('для каждой, стоящей тоже в воде, наносит 6 cactus damage и искры/звук.',
        'для каждой, стоящей тоже в воде, из её позиции повторно выбирает одну #minecraft:mobs сущность в `distance<=0.001` и пытается нанести ей 6 cactus damage, рисует искры и играет звук. `limit=1` без sort не гарантирует, что жертва совпадёт с сущностью внешнего перебора; владелец набора также может попасть под выбор.')
replace('int(`armor amount×0.17`)', 'floor(`armor amount×0.17`)')
replace('усечённое среднее `tmp`', 'целочисленное среднее с округлением вниз `tmp`')
replace('передаёт `tmp` в macro child, помечает атакующего и наносит этот Arrow damage',
        'записывает именно **atk** в storage-поле **tmp**, помечает атакующего и передаёт в macro child. Ответный Arrow damage равен **4×число частей − среднее floor(armor×0.17)** и применяется')
replace('child attribution — ближайший player tag `atacker`',
        'child attribution — `@a[tag=atacker,limit=1]` без сортировки, обычно текущий помеченный игрок')
replace('с переданным amount `tmp`', 'с переданным storage amount `tmp` (это вычисленный atk, не одноимённый score-среднее)')
replace('Замена задаёт стеклянный item model, новые max_damage=150, equippable asset `lbc:glass`, имя/lore, armor и toughness attributes из временных scores и custom_data `glass:1`.',
        'Замена задаёт новый leather-предмет со стеклянным item model, max_damage=150, equippable asset `lbc:glass`, имя/lore и custom_data `glass:1`. Loot down_armor_* назначает и armor, и toughness одинаковое значение **floor(прежний armor×1000)×0.00095** — примерно 95% прежнего armor, а не прежнего toughness. Замена не копирует из старого предмета его прочность/прочие компоненты. Ответный расчёт далее читает уже заменённые предметы.')
replace('вычитает 1 и применяет item modifier',
        'вычитает 1 и применяет item modifier')
replace('clamp снизу в этих командах нет.',
        'clamp снизу в этих командах нет. Между слотами #tmp1 дополнительно уменьшается безусловно, даже если слот не erosion; перед настоящим слотом его обычно перезаписывает data get. Полный комплект также проходит четыре безусловных decrement перед tick_rnd; успешное чтение выбранного enchantment перезаписывает это scratch-значение.')
replace('без stopper в 20, затем снимает тег.',
        'без marker **stopper** в 20, затем снимает тег; это отдельный gate от stopper_magic у соседней ауры.')
replace('в 0.01..6 вне same-team и same-id',
        'на расстоянии **..6** вне same-team и same-id')
replace('cooldown до20 увеличивает на5',
        'cooldown при входном значении <=20 увеличивает на5 (может получить 25)')
replace('только при cooldown≤0 и входном tier≤119;',
        'каждая команда отдельно проверяет текущие cooldown≤0 и tier≤119 перед своей прибавкой;')
replace('`players_10t` раз в 10 тиков зовёт эффект, если нет stopper в 20;',
        '`players_10t` раз в 10 тиков зовёт эффект, если нет stopper в 20;')
replace('`slam` добавляет gravity +0.6, ставит tag/score и звук;',
        '`slam` добавляет gravity +0.6, ставит tag overload_slam, **overload_jump=2** и звук; score overload_slam здесь не инициализируется заново, поскольку caller требует его 0;')
replace('Он рассчитывает damage `int(0.04×(предударный score)^2)`, радиус `int(0.05×score)` и `tmp2=int(0.1×score)`; затем clamp score к минимуму60 и при значении200+ ставит140.',
        'Сначала он рассчитывает damage `int(0.04×(предударный score)^2)`. **Потом** при score<=60 ставит 60, при score>=200 ставит 140; радиус `int(0.05×score)` и tmp2 `int(0.1×score)` берутся уже после этих замен. Например, score=2 даёт damage=0 и radius=3, score=200 — damage=1600 и radius=7.')
replace('добавляет владельцу mana `radius×100` за выбранную точку попадания независимо от того, прошёл ли damage-командный hit,',
        'однократно за весь slam добавляет владельцу mana `radius×100` даже при отсутствии жертв,')
replace('При score=18 удаляет Levitation', 'При score=18 удаляет Levitation') if 'При score=18 удаляет Levitation' in s else None
replace('На земле без полёта ботинки заряжаются;',
        'На земле без полёта ботинки заряжаются;')
replace('с такими leggings меняет gravity через формулу из Rotation.',
        'с такими leggings сначала удаляет оба gravity fix, потом добавляет `fix2 = (floor(pitch×100)+6420)×0.001` (примерно −2.58 при pitch=−90 и −1.08 при −75). Без leggings up только очищает/выдаёт Levitation и не удаляет прежние gravity fix из hover/down.')
replace('Down устанавливает gravity +0.1 без mk2 legs либо Rotation×0.001 с ними.',
        'Down удаляет оба fix и без mk2 legs добавляет fix2=+0.1; с mk2 legs вычисляет **floor(pitch×70)×0.001**. После этого **в любом варианте** зовёт macro hover_1 по storage.tmp: без leggings tmp не обновлён, поэтому пытается повторно добавить тот же fix2 с остаточным параметром (ID уже добавлен предыдущей командой).')
replace('раз в секунду `players_1s` (при `techno_active`, без stopper) строит от каждого non-spectator игрока точку на +3, поворачивает её к его глазам и рисует 8 flame particles по направлению на дистанциях0.1..3.2;',
        'раз в секунду `players_1s` (при `techno_active`, без stopper) перебирает non-spectator игроков на distance>=1 от исходной позиции владельца. `as` не меняет position: для **каждой цели** точка остаётся на +3 над исходной позицией владельца и поворачивается к глазам выбранного игрока. От неё рисуются 8 flame particle-команд на 0.1/0.5/0.9/1.3/1.7/2.1/2.6/3.2, каждая отправляется ближайшему к этой точке non-spectator @p. Внутренний stopper gate проверяется у поднятой точки и исключает strict_map_object;')
replace('`powerhelmettick` дополнительно показывает владельцу зелёный particle в позиции mobs и других игроков, кроме player-self,',
        '`powerhelmettick` дополнительно показывает владельцу зелёный particle у всех non-player #minecraft:mobs без distance gate и у non-spectator игроков на distance>=0.01; у игроков проверяет stopper в 20 от **цели**,')
replace('Фактическая логика |', 'Фактическая логика |')
replace('После удара снимает gravity modifier, score0 и tag.',
        'После удара снимает gravity modifier, score0 и tag. В boot tick проверка блока выполняется раньше OnGround; после первого impact score=0 предотвращает вторую обработку в том же проходе. Урон не указывает attacker; team-фильтр есть, отдельного spectator-фильтра в impact нет.')
replace('после двух decrement-вызовов.',
        'на третьем leggings tick: проверка score=18 стоит **до** decrement (20→19, 19→18, затем clear и 18→17).')

if '## Проверенные исходники' not in s:
    import json
    jobs = json.loads((notes / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    sources = jobs['lbc_armor']['sources']
    assert len(sources) == 77
    s += '\n## Проверенные исходники\n\n'
    s += 'Координатор прочитал все 77 функций и сопоставил их с caller-командами. '
    s += 'Список позволяет перейти от поведения к исходнику; исходные файлы не изменялись.\n\n'
    s += '\n'.join('- ['+x.split('/function/')[1]+'](../'+x+')' for x in sources)+'\n'
p.write_text(s, encoding='utf-8')
print('Armor corrections applied; all 77 sources linked.')
