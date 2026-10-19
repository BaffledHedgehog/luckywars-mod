# Исходный план переноса BrBad (kit 17)

Исторический план этой пачки. Текущая реализация и отличия по уточнению
пользователя — [BRBAD_PORT.md](BRBAD_PORT.md); проверенные результаты —
[SERVER_TESTS.md](../SERVER_TESTS.md). Вместо старого marker-перемещения кислота
использует нативный `ThrowableProjectile` и невидимый proxy Polymer.

## Источник и scope

Selector `lbc.kit=17` выдаёт `lbc:kit/brbad`: четыре lingering potion с `raid_omen`, duration 20 и amplifier 234–237 (`termit`, `sulfur`, `phosphin`, `gremuch`). Старые имена/lore keys существуют в пользовательском resource pack.

Каждый тик datapack находит `AreaEffectCloud` с `raid_omen`; `raycastselector_area_cloud` направляет signed amplifier -22..-19 четырём handlers. Это cloud dispatcher, геометрического raycast здесь нет. При использовании native `getAmplifier` сверить представление 234–237, не переносить отрицательные NBT-значения вслепую. Termit преобразует блоки и наносит lava damage в растущей области; sulfur даёт Slowness/Wither наземным целям в радиусе 6 и с шансом 3% вызывает acid helper; phosphin накладывает четыре эффекта мобам в радиусе 8; gremuch создаёт TNT с fuse **2 тика** и удаляет cloud. В SNBT `2s` означает short со значением 2, а не две секунды.

## План и готовые компоненты

- Выдать исходные loot components без переписывания переводов и оставить штатный vanilla lingering throw.
- Обработку cloud sentinel effects встроить в существующий `LbcItems` lifecycle; переиспользовать его server tick, tracked entity/batch паттерны и очистку при stop. Не заводить глобальный опрос мира или отдельный движок.
- Для игроков учитывать `GameController.allowDamage/isObserver`; `LbcItems.active` годится для владельца, но не заменяет фильтр получателей. `MapManager` уже знает `AreaEffectCloud` при очистке entities. `LbcEffectTriggerMixin` реагирует на player effects-changed, но не обрабатывает cloud ticks.
- Локальный 26.3 source JAR подтверждает `AreaEffectCloud(Level,x,y,z)`, `setPotionContents`, `setOwner`, `setRadius`, `setDuration`, `setWaitTime`; `tick()` вызывает server tick на `ServerLevel`. `ProjectileUtil.getHitResultOnMoveVector(Entity,Predicate,ClipContext.Block)` возвращает `HitResult`; `ServerLevel` наследует `clip(ClipContext)` через `Level`/`BlockGetter`. Классами raycast пользоваться только при подтверждённой геометрической зависимости.

## Неготовое и проверки

- [lbc:drops/muravei_acid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/muravei_acid.mcfunction) существует: создаёт marker `raycastacid/slowraycast`, lifetime 300, `lbcID2=-100`, вызывает `universal_razbros`. До переноса sulfur проследить его движение, попадания и block/shield фильтры; замену не придумывать.
- Termit использует unqualified block tags, то есть namespace `minecraft`. Пользовательские определения находятся в `LBC/data/minecraft/tags/block/` (`to_nylium`, `to_shroomlight`, `to_soulsand`, `to_magma`, `to_netherrack`, `fluids`, `airs_to_fire`, `to_lava`, `solid`); `logs/leaves` — ванильные теги. Сверить их содержимое и исходные счётчики Lifetime/tmp1–3. Сохранить порядок преобразований и радиусы; бюджет работы на тик не должен молча обрезать область.
- Проверить: (1) loot/text/components; (2) четыре amplifier mapping, без реакции обычного raid omen; (3) radius/duration/effects/damage/particles/TNT fuse 2 и lifecycle; (4) observer, dead/respawn, dimension/team filters и cleanup; (5) термитные tag sets/порядок преобразований и sulfur acid с существующим helper.

## Источники

- [Selector](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kit_select/select.mcfunction), [loot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/brbad.json), [tick/router](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_not_timestopped.mcfunction), [raycastselector_area_cloud](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_area_cloud.mcfunction)
- Handlers: [termit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/termit_tick.mcfunction), [termit blocks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/termit_tick_storage.mcfunction), [sulfur](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/sulfur_tick.mcfunction), [phosphin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/phosphin_tick.mcfunction), [gremuch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/gremuch_tick.mcfunction)
- New-code context: [LbcItems](../src/main/java/me/skywars2/game/LbcItems.java), [GameController](../src/main/java/me/skywars2/game/GameController.java), [MapManager](../src/main/java/me/skywars2/game/MapManager.java), [LbcEffectTriggerMixin](../src/main/java/me/skywars2/mixin/LbcEffectTriggerMixin.java); prior analyses: [LBC_CONSUMABLE_NEXT](LBC_CONSUMABLE_NEXT.md), [INTEGRATION_REVIEW](INTEGRATION_REVIEW.md).
- API source: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.
