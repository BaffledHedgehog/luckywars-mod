# Приёмка weapons координатором

Приняты 256 источников после полного peer-прохода
[review_weapons_arcana.md](review_weapons_arcana.md), дополнительного аудита событий
[review_weapons_final.md](review_weapons_final.md), чтения человеческих разделов
и ручной сверки сложных ветвей координатором. Coverage/fingerprints подтверждают
перечень, а не заменяют объяснение поведения.

Сверены `gowno`/swords и периодические callers, полные `danmaku_staff/tick`,
`click`, `click_2`, Uni Wand click, Fevgalea damage_100hp, Madoka shoot,
WTF hit/tick и Banhammer tick_fall. В описании исправлены дополнительные ошибки:

- Fevgalea передаёт `floor(Health × 0.5)` в int storage, а не точную половину.
- Danmaku сначала вызывает click_2, затем списывает цену; sneak прибавляет её
  перед списанием. Fallback проверяет отсутствие wand также во всех container-слотах.
- SelectedItem у Danmaku относится к mainhand-ветке; upgrade offhand проверяет
  сам offhand. Предыдущее утверждение о чужой руке в peer refinement исправлено.
- Madoka копирует ID/команду владельца в стрелу. WTF выбирает цель без nearest-sort;
  его tick damage требует именно `shef_hated`, а не произвольную соседнюю сущность.

Контракты луков/арбалетов, charged hit/click, обеих рук, случайных исходов,
meditation, расхода маны, raycast геометрии и уборки описаны по семействам.
Callback-периоды отличены от событий advancement. Награды/создаваемые снаряды
продолжаются в отдельных уже существующих документах workingitems/LBCR/runtime;
названия моделей не используются как доказательство конечного действия.
