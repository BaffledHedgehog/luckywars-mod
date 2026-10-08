from pathlib import Path
p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
start=s.index('God Hedgehog update')
end=s.index('\n\n## Файлы: other / artifact',start)
new='''God Hedgehog `tick` вызывается для `silverfish[tag=god_hedgehog]`: при Y≤0 существо переносится на (0,250,0), каждое обновление получает Slow Falling100s, а live God Hedgehog silverfish считаются через `execute store result ... if entity` (записывается число сущностей, не boolean). При счёте ≥2 текущий executor убивается. Глобальный `ticker % 500` выбирает ветку: 1 телепортирует и убивает прочих silverfish без тега; 10/20/30/40/50 запускают laser; 100..140 — dimension scrap; окна 240..280, 281..320, 321..360, 361..400, 401..440 вызывают по одному macro-варианту белой стрелы на случайном не-spectator игроке для каждого вошедшего tick.

| Ветка | Полный эффект и остановка |
|---|---|
| laser (`death_laser` → `laser_rc`) | Сбрасывает Distance, даёт владельцу Resistance V на1s, выбирает ближайшего допустимого моба в200 от глаз и рекурсивно создаёт 101 TNT с `fuse:0,explosion_power:10` с шагом3 (до300 блоков); затем обнуляет Motion и смещает silverfish на15 блоков, повернув к (0,150,0). Без цели raycast-часть не запускается. |
| dimension scrap (`spawn_dim_scrap`) | Создаёт item_display с моделью `lbc:hedgegod_eater` на10 блоков перед глазами, проигрывает звук и опускает silverfish на2 блока; дисплей продолжает цикл через `raycastselector_rare`/`hedgegod_eater_tick`. |
| eater-display цикл | Distance растёт на1 за вызов; на1 меняется масштаб, на3 с шансом96% создаётся дочерний display у ближайшей допустимой цели, на79 scale умножается ещё на20, на80 ставится `lbc:air_sphere_7x7x7` и мобам в4 наносится10 `outside_border`, на81 scale становится практически нулевым из-за целочисленного `data get`, а на83+ display удаляется. |
| white-arrow `hedgegod_white_arrow_tick` | При `Lifetime=40` вызывает `hedgegod_white_arrow_shoot`: звук/частицы, marker с Rotation90° и данными lifetime20, затем удаление текущего executor. Следующие шаги marker зависят от общих raycast-функций. |
| white-arrow формы | Каждая из пяти форм вызывает общий макрос создания одного display; параметры `i/j/k/r` различаются. Форма1 берёт `ticker%20` с scale0.1 (store в int округляет); форма2 — `ticker×7 mod180−90`; форма3 выбирает k от−10 до10; форма4 — `ticker×45 mod180−90`. Обычный и четыре формы используют пять окон ticker выше. |

Источники: [tick](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction), [laser](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/death_laser.mcfunction), [raycast](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/laser_rc.mcfunction), [eater cycle](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction), [eater burst](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_boom.mcfunction), [arrow shoot](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_white_arrow_shoot.mcfunction), [white-arrow macro](../world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows1.mcfunction).'''
s=s[:start]+new+s[end:]
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
