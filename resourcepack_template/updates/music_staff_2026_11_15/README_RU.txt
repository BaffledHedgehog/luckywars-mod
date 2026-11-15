Музыкальный посох — рабочие заглушки ресурспака

Объедини assets с авторским ресурспаком. Файлы авторского РП автоматически не менялись.

Вид посоха:
  assets/lbc/items/music_staff/staff.json
  assets/lbc/models/item/music_staff/staff.json
Сейчас используется ванильная палка; поменяй модель или её текстуру.

Вид стрелы:
  assets/lbc/items/music_staff/arrow.json
  assets/lbc/models/item/music_staff/arrow.json
Модель ориентирована остриём вдоль +Z. Сейчас используется ванильная текстура
minecraft:entity/projectiles/arrow. Для своей текстуры добавь PNG и поменяй
значение textures.arrow в модели. Обычные ванильные стрелы не изменяются.

Музыка:
  assets/skywars2/sounds.json, событие skywars2:music_staff
Пока это ссылка на ванильную пластинку Cat. Для своего трека замени запись sounds
на {"name":"skywars2:music_staff","stream":true}, затем положи OGG/Vorbis в
assets/skywars2/sounds/music_staff.ogg. Общий sounds.json нужно объединить,
а не заменить им остальные события ресурспака.

Оформление предмета в моде:
  src/main/resources/data/lbc/loot_table/ported/music_staff.json
Улучшенный вариант:
  src/main/resources/data/lbc/loot_table/ported/academy_music_staff.json
Редактируются имя, lore, tooltip_style и item_model как у остальных жезлов.
