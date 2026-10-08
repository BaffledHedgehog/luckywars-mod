from pathlib import Path
p = Path('agent_notes/skywars_gui.md')
s = p.read_text(encoding='utf-8')
paras = s.split('\n\n')
seen_roll = False
out = []
for para in paras:
    if para.startswith('У roll есть общий цикл:'):
        if seen_roll:
            continue
        seen_roll = True
    if para.startswith('Доступ: `gui/page` каждый раз'):
        para = 'Доступ: `gui/main` каждый лобби-тик выполняет ванильный `clear @s[tag=!developer]`, очищая обычный инвентарь игроков без тега developer. `gui/page` запускается при секундном обновлении и после выбора кнопки; он сначала вызывает `gui/clear`, который стирает 27 EnderItems-ячеек у любого игрока, включая developer. Строки `gui/select/0` отдельно проверяют `admin`. Панель хранится в эндер-сундуке игрока; обработчик читает предмет в курсоре. Открытие экрана и внешняя обработка контейнера не устанавливаются только этими командами.'
    if para.startswith('На `7300` slots'):
        tail = para[para.index('На `7310`'):]
        para = 'На `7300` слоты 0–17 выбирают suffix 1–18, а слоты 19–24 — suffix 19–24, лишь если выдан соответствующий `swrg:rewards/suffix_N`. Слот 18 возвращает назад, 25 снимает suffix, 26 открывает продолжение 7310. Суффикс 19 присутствует: его кнопка находится в слоте 19. ' + tail
    if para.startswith('При `lbc.event=1` страницы Halloween'):
        links = para[para.index('Источники:'):]
        para = 'При `halloween lbc.event=1` страницы эффектов вызывают `7400_halloween` и `7500_halloween`, но обе функции пустые: сезонного изменения кнопок или доступа от них нет. `7300_halloween` действительно заменяет закрытые кнопки суффиксов 1–24 в слотах 0–17 и 19–24 на сезонную модель `lbc:locked_case_halloween` и подсказку; advancement-доступ остаётся прежним. ' + links
    out.append(para)
s = '\n\n'.join(out)
s = s.replace('kit 3000, ключей 5000, LBC-наборов 4000', 'наборов SWRG 3000, ключей 5000, навыков SWRG 4000')
s = s.replace('при `halloween lbc.event=1` подключают альтернативные страницы.', 'при `halloween lbc.event=1` вызывают пустые сезонные функции, которые ничего не меняют.')
s = s.replace('ближайшим не-наблюдателем из loot context', 'ближайшим не-наблюдателем на расстоянии не менее 0,1 блока от loot context')
s = s.replace('это предметные модификаторы datapack, а не кнопки GUI.', 'каждый использует `replace:true` и заменяет весь список attribute modifiers предмета одним указанным модификатором.')
needle = 'Оба вида получают из keys, не покупают в магазине.'
assert needle in s
s = s.replace(needle, needle + ' В `reward/bars` есть обработка `rewardbar:21`, но ни одна из 254 назначенных loot tables не выдаёт этот ID, а выбор страницы 7200 заканчивается bars20. Наличие ветки награды не делает bars21 доступным через этот GUI.')
p.write_text(s, encoding='utf-8')
print('GUI prose corrected')
