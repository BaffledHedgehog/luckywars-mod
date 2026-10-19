# Mystical Artifact: Looper reveal status

Native Looper now projects the description separately for each recipient,
without rewriting the shared server item. See [LOOPER_PORT.md](LOOPER_PORT.md)
for the implementation and [SERVER_TESTS.md](../SERVER_TESTS.md) for current
verification and deployment evidence. Passive types 1:6 and 1:8
are also migrated: [current contract](ARTIFACT_PASSIVES_PORT.md).
Type 1:5 and shared custom-effect cleansing are implemented:
[current contract and verification status](ARTIFACT_DEBUFF_PORT.md).
Type 1:4 arrow suppression is implemented:
[current behavior and verification status](ARTIFACT_ARROWS_PORT.md).
Type 1:2 native unstable-entity suppression is implemented:
[bounded behavior and verification status](ARTIFACT_EXPLOSIONS_PORT.md).
Type 1:3 liquid/falling-block hardening uses the user-requested radius-16 sphere:
[current behavior and verification status](ARTIFACT_LIQUIDS_PORT.md).
Type 1:7 now has all 16 direct color branches, amplified water effects and
Purple's custom teleport timer: [current contract and limits](ARTIFACT_COLORS_PORT.md).
Shared mana now has native regeneration, spending, amplified/reverse flow,
mage/belt bonuses and the source mana HUD: [core port and limits](MANA_PORT.md).
Five mana potion families now share that component, native drink/splash/cloud
delivery and common custom-effect cleansing: [potion contract](MANA_POTIONS_PORT.md).
Negative-mana combat and Crux passed focused/full server verification and are installed:
[Overload contract and status](OVERLOAD_PORT.md). First actual spell consumers:
[Tiny Fire/Blind and cast trinkets](WANDS_FIRST_PORT.md), followed by
[Tiny Air/Stone/Teleport and the user-approved single terminal hit](WANDS_SECOND_PORT.md).
[Tiny Ruby/Terra](WANDS_THIRD_PORT.md) use the separately approved piercing behavior.
[Tiny Acid](WANDS_FOURTH_PORT.md) reuses the existing native acid projectile lifecycle;
Tiny Electro's linked reflection/conduction pipeline is a separate next batch.
The remaining staffs and personal nomagic providers are subsequent work.
[Next migration plan](NEXT_MIGRATION_PLAN.md).
This limited migration does not implement the effects of
artifact families 2/3/4, the full mana engine, or staffs.

Уточнение (user): постоянное раскрытие заменено описанием для конкретного
получателя с Looper через Polymer; один серверный стек остаётся общим.
Актуальный контракт и статус новой проверки — [LOOPER_PORT.md](LOOPER_PORT.md).
Описание старого односекундного изменения предмета ниже историческое.

Источник — старый LBC (`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`). Выдача в `loot_table/artifact.json` содержит 23 состояния `mystical:type`: 1:{1–8}, 2:{1–6}, 3:{1–3}, 4:{1–6}; для начального `1:1` оба поля уже выбраны, это не цикл смены `type`.

**Триггер и регистрация.** `function/load.mcfunction` запускает `function lbc:tick1s`; `tick1s.mcfunction` сам себя планирует через `schedule ... 1s` и вызывает `mobs_1s` для игроков. Далее цепочка `players_1s` → при `#lbcskill swrg.math = 1` `skills_tick_1s` → при `@s lbc.skill = 3` `looper_tick`. Это dispatch уже выбранного навыка Looper, не общий триггер у предмета. Само определение в `looper_tick.mcfunction` проверяет только предмет в main hand с `unresolved_artifact:1` и вызывает `looper_explain_artifact`; находка определения не означает, что навык 3 выдан/зарегистрирован в конкретной игре.

**Результат функции.** `looper_explain_artifact.mcfunction` копирует предмет в контейнерный слот служебного блока `10241024 71 10241024`, по 23 парам `mystical:type` подставляет `artifact_1`…`artifact_23` во вторую lore-строку, удаляет только `unresolved_artifact` и копирует предмет обратно. Она не меняет `mystical` или `type`; состояния заданы таблицей добычи заранее. Две anti-magic зависимости для текущего этапа — отдельная механика и не требуются для раскрытия.

Перенесённый узкий эффект выполняет пункты source-контракта: dispatch при
выбранном `lbc.skill == 3`, main-hand marker gate, 23 явных translation keys,
замена только второй lore-строки и удаление только `unresolved_artifact`.
Следующий шаг для этой реализации — дождаться итоговой серверной проверки и
записать фактический статус. Остальные магические эффекты остаются вне scope.
