from pathlib import Path
p=Path('agent_notes/lbc_workingitems_other.md');s=p.read_text(encoding='utf-8')
old='Неизученными остаются не функции этого job, а конечные эффекты внешних зависимостей: velocity/hit/cleanup для общих raycast projectiles, `drops/skrimmer`, Magic Academy upgrade transaction, `lbcr:raycast_end2_mode_*`, freeze consumers и внешний loot table.'
assert old in s
s=s.replace(old,'Обработчики за пределами этого задания описаны отдельно: движение, попадания и cleanup — [runtime](lbc_runtime.md), [projectiles](lbc_projectiles.md) и [LBCR](lbc_lbcr.md); `drops/skrimmer` — [drops](lbc_drops.md); Magic Academy — [magic](lbc_other_magic.md); freeze — [эффекты](lbc_effects.md) и [misc](lbc_other_misc.md); выдаваемые предметы — [loot](lbc_loot_definitions.md). Реально отсутствующие определения перечислены в [RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md).')
p.write_text(s,encoding='utf-8')
print('Cross-job dependencies linked to completed documentation')
