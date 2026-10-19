# Модели снарядов Terra: сверка с ресурс-паком

В read-only RP `all_in_one_1.21.8` найдены все четыре item definition, модели и одноимённые PNG в `assets/minecraft/textures/item/`. Item-модели наследуют `minecraft:item/handheld`.

| Item model | RP item definition → model | Texture | Связь/статус в новом моде |
|---|---|---|---|
| `lbc:terra_charge` | `assets/lbc/items/terra_charge.json` → `lbc:item/terra_charge` | `terra_charge.png` | Подключён в `LbcTerra` для обычного жезла `stterra` |
| `lbc:terrasword_charge` | `assets/lbc/items/terrasword_charge.json` → `lbc:item/terrasword_charge` | `terrasword_charge.png` | Подключён к обычному Terra Sword |
| `lbc:terrasword_true_charge` | `assets/lbc/items/terrasword_true_charge.json` → `lbc:item/terrasword_true_charge` | `terrasword_true_charge.png` | Подключён к Academy Terra и ПКМ улучшенного Terra Sword |
| `lbc:terrasword_true_beam` | `assets/lbc/items/terrasword_true_beam.json` → `lbc:item/terrasword_true_beam` | `terrasword_true_beam.png` | Подключён к дуге улучшенного Sword и Razorterra |

Первые три должны быть ориентированы как старый `chaos_sword` display: OLD LBC `data/lbc/function/items/swords/chaos_sword_imba.mcfunction:1` задаёт `item_display:"fixed"` и matrix `[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,3.0f]`. Это базовый transform display-сущности, не item model JSON. Для летящего предмета yaw/pitch должны следовать его реальной velocity; дуга beam при ударе требует отдельной сверки ориентации и размеров. Её мгновенность из названия модели не следует. Реальный клиентский вид не проверен.

В `LbcWands` зарегистрированы `skywars2:tiny_terra_wand` и обычный
`skywars2:terra_wand`. [Обычный Terra](TERRA_REGULAR_PORT.md) использует
виртуальный display с `lbc:terra_charge`; Tiny остаётся мгновенным лучом.
Academy Terra использует тот же полёт и `lbc:terrasword_true_charge`.
Оба Terra Sword подключены к тому же компоненту; [поведение и проверки](TERRA_SWORD_PORT.md).
Дуга развёрнута горизонтально; первые три модели сохраняют Chaos Sword
transformation. Текущий общий явный scale — 1,5; пользовательская настройка
сохранена при добавлении хвоемёта. Razorterra использует ту же ориентацию дуги,
но собственные скорость, урон и TTL: [контракт](TERRA_CRAFTING_PORT.md).

Связи: `stterra` → `terra_charge`; TerraSword → `terrasword_charge` (тот же снаряд, отдельный вид); улучшенный Terra Sword → `terrasword_true_charge` при ПКМ и `terrasword_true_beam` дугой при ударе. Пользователь уточнил: «Да, улучшенный Terra Sword». Улучшенный сосновый хвоемёт также → `terrasword_true_beam`. Уточнение снимает прежнюю неоднозначность; заметка описывает ресурсы, а не реализацию поведения.

Дополнение (user):

> у улучшенного в magic academy терра жезла нужна модель снаряда lbc:terrasword_true_charge

Это отдельная связь для улучшенного жезла; она не заменяет привязку ПКМ
улучшенного Terra Sword к той же модели.

## При переносе

`polymer-virtual-entity:0.18.2+26.3` уже подключён в `build.gradle` и присутствует
в установленном bundled Polymer. В локальном sources JAR подтверждены
`ItemDisplayElement.setItem` / `setItemDisplayContext`, `DisplayElement` с
`setTransformation`, `setInterpolationDuration`, `setTeleportDuration` и
`EntityAttachment.ofTicking`. Использовать эти API для визуала, привязанного
к настоящему снаряду, без второй физической Display-сущности в мире и
собственной реализации пакетов. Это уменьшает число серверных сущностей,
но стоимость клиентской отрисовки/рассылки всё равно надо измерять.

Ставить соответствующий полный `ITEM_MODEL`, display context FIXED,
переносить базовый transform через корректный native codec/декомпозицию
исходной матрицы, не угадывать порядок строк/столбцов JOML. Визуал создавать
один раз на снаряд; сохранять интерполяцию и удалять вместе с владельцем,
unload и stop матча. Ориентацию проверить на клиенте по ±X/±Y/±Z и диагоналям.
Не добавлять эти модели к Tiny Terra без отдельного требования: он пока
мгновенный луч. Общий план lifetime — [PROJECTILE_LIFETIME.md](PROJECTILE_LIFETIME.md).
