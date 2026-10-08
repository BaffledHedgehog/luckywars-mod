from pathlib import Path
p=Path('agent_notes/lbc_other_misc.md'); s=p.read_text(encoding='utf8')
block='''
## Shock effect: выбор цели и цепная молния

Основной вход `shock_effect/zap` вызывается damage-hook при положительном `effect_shock_potion` и также из эффекта при `HurtTime:10s`. Он сохраняет ID атакующего/команду, помечает ближайших союзников и самого атакующего защитными тегами, затем наводит визуальный луч на ближайшую допустимую цель-моба в пределах 10 блоков. Луч проходит позициями с шагом 0,25, максимум до контрольного порога Distance 50; при столкновении с небезвоздушным блоком срабатывает hit-ветка. При цели функция наносит 6 урона типа arrow; `as_target/run` переносит запас `effect_shock_potion` в цепную переменную, помечает уже поражённую цель и выбирает следующую незатронутую цель в радиусе 20, исключая damager, союзников, spectator и strict_map_object. Повторные цели отсекаются тегом `chained`; очистка временных тегов находится в конце исходного луча. Условия selector `dx/dy/dz` и `distance` применяются вместе — это важно для hit-check, а не одно большое сферическое попадание.

Ссылки: [вход zap](../world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap.mcfunction), [пошаговая проверка луча](../world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction), [переход к следующей цели](../world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_target/run.mcfunction), [урон и остановка цепи](../world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/hit_entity.mcfunction).
'''
if '## Shock effect: выбор цели и цепная молния' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
