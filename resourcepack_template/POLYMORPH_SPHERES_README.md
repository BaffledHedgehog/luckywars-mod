# Сферы полиморфа

Модель: `lbc:polymorph_sphere`.

Скопируй `assets/lbc/items/polymorph_sphere.json`,
`assets/lbc/models/item/polymorph_sphere.json`,
`assets/lbc/textures/item/polymorph_sphere.png` и
`assets/lbc/shaders/post/polymorph_bloom_sample.fsh` по тем же путям в РП.

Файл `updates/polymorph/end_of_frame.json` положи в
`assets/minecraft/post_effect/end_of_frame.json`. Это полный вариант текущего
авторского файла с одной изменённой стадией bloom. Если после этой подготовки
ты менял свой end_of_frame, перенеси только две правки в pass с
`fragment_shader: minecraft:post/bloom_sample`:

1. fragment_shader → `lbc:post/polymorph_bloom_sample`.
2. В inputs добавить `{"sampler_name":"PolymorphOutline","target":"minecraft:entity_outline"}`.

Остальные стадии, история эффектов, контуры, чёрные дыры и ядерный гриб сохранены.
Применение — F3+T. Авторский ресурс-пак автоматически не менялся.

Сфера — поворачиваемый к камере плоский item_display с текстурой объёмной сферы.
Одна двусторонняя плоскость вместо сотен модельных граней. Внутри силуэта нет
полупрозрачности: alpha254 использует уже имеющийся непрозрачный emissive-путь
item.fsh; alpha0 остаётся только снаружи круга. Dither меняет оттенки, не opacity.
Цвета буквально взяты из существующей sheepwand.png:
#DF14FF, #EF59E6, #F365E2, #F87FD9, #FD8CD4.

Сервер задаёт максимальную яркость, отсутствие тени, billboard=center,
teleport_duration=1 и розовый glowing-контур #DF14FF. Дополнительный bloom
берётся из реального контура display. Просто розовый блок в изображении мира
не становится новым источником bloom. Прежние правила остального bloom не менялись.

GPU-проверка: `python tools/check_polymorph_shader.py`; результат и хеши
прочитанных авторских файлов — `build/verification/polymorph/gpu-results.json`.
Проверяются восемь вариантов native item-шейдера и отдельная bloom-маска.
Физический игровой клиент этой проверкой не заменяется.
