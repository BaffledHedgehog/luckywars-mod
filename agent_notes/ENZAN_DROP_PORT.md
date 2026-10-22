# Ватердроп Энзана — 22 октября 2026

Уточнение пользователя (user):

> Сохранить Левитацию 128 на 3 секунды

Read-only источник S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/drops/specialdrops.mcfunction:66` вызывает `drops/levitation` по
флагу `{levitation:1}`, после Lucky Pigs и до Stalin. Три строки
`function/drops/levitation.mcfunction:1–3` дают ближайшему неспектатору
Левитацию60тиков/amplifier127, затем независимо именное ведро воды; звук
`minecraft:block.wool.break` MASTER1/1 идёт из позиции токена, без minVolume.
Полёт, падение и водный clutch остаются нативными; прямой урон не добавлен.

Токен S `loot_table/loliluckyblock.json:182–222` сохраняет count1,
`lbc:tier_ff`/`lbc:drop_trap`, жёлтый/nonitalic `drop_enzan` и белый/nonitalic
`trap_desc`, `{levitation:1,drop:1}`. Вес10 и исключения модификатора7/набора
zaika относятся к исходному Lucky Block acquisition и не переносятся в ручную
безусловную leaf-таблицу `lbc:ported/levitation_drop`. Остальные пулы закрыты.
Нативный предмет — `skywars2:levitation_drop`, Polymer repeating command block.
В RP уже есть модель и RU/EN/RPR переводы `Ловушка: Ватердроп Энзана` /
`Trap: Enzan's Waterdrop`; RP и template не изменяются.

`LbcEnzanDrop` переиспользует выбор ближайшего игрока у клеток: self, союзники
и Creative допустимы, служебные NPC/наблюдатели исключены. Мана, кулдаун,
антимагия, новые таймеры/маркеры/акторы не добавляются. Нативное addEffect
получает фактического бросившего как source; существующие status hooks
сохраняют причину падения/пустоты только принятого воздействия и приоритет
более позднего нападавшего. Мгновенная очистка может запретить эффект,
но отдельные выдача ведра и звук всё равно выполняются.

Ведро — обычный `minecraft:water_bucket`, custom_name green/nonitalic literal
`Enzan` с extra dark_gray/nonitalic literal ` Beta Server Harem event`.
Оно выдаётся через обычный инвентарь, остаток выбрасывается у получателя.
Предварительная проверка пустого слота для нестакающегося ведра сохраняет
награду и при полном Creative-инвентаре, где Inventory.add иначе обнуляет стек.
Отдельных Creative/developer исключений поведения токена нет.

`LbcSpecialDrops` владеет единичным debit/порядком составных флагов, admission,
пауза Stand, проверками эпохи/выгрузки и фильтром фиктивного `/give` item.
Нет подходящего игрока — эффект/ведро пропускаются, звук и debit сохраняются.
Настоящие эффекты и предметы после выдачи живут по общим нативным правилам;
отдельная очередь/таймер их повторной выдачи не создаётся.

Build и два настоящих изолированных матча прошли на `3236fe1c…`: 72 результата
suites за79,24с вместе с Lucky Pigs и всеми предыдущими special drops.
178 Java-исходников,526классов,2671ресурс совпадают со сборкой, обе новые leaf
и внутренний applepig явно включены processResources whitelist.
Архив `build/verification/skywars2-enzan-pigs-3236fe1c.jar`, без установки;
доказательства `build/verification/enzan-pigs-final/qualification.json` и лог.
Основной96ee50a7, пользовательский Reroll и RP/template не изменены.
Проверены native addEffect/cleansing, безопасный player dying-hook, настоящие
смерти мобов от падения/пустоты, приоритет более позднего удара/непринятого
эффекта, точное ведро, полный Survival-инвентарь, роли/self/allies/Creative,
общий debit/пауза/эпоха/stop. Full Creative fallback проверен чтением кода;
реальные звук/рендер/траектория игрока/clutch/монеты не проверялись.
