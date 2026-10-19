# Перенос таймера `nomagic`

`LbcMana` хранит персональный счётчик `nomagic` в существующем scoreboard bridge. `noMagic(ServerPlayer)` читает секунды, setter ограничивает значение снизу нулём, а `applyNoMagic(player, seconds)` прибавляет длительность и обнуляет текущую ману по контракту `other/effect_nomagic.mcfunction`. Сложение намеренно остаётся обычной арифметикой `int`; штатные источники дают конечные положительные значения.

Таймер убывает один раз в `secondPlayer`, рядом с `reverse_mana`. Он не убывает, пока игрок не `visible` (включая observer/inactive), либо помечен `time_stopped`. При положительном таймере `tickPlayer` продолжает обработку potion-эффектов и тикает Overload armor, но пропускает обычный mana regeneration и связанный с ним Crux/debt путь. Таймер не включён в `active`, `trySpend` или `trySpendWand`: source использует его как паузу ресурсного цикла, а не общий запрет способностей.

`clearEffects` очищает этот таймер вместе с временными эффектами, не трогая постоянные `mana`, `manamax` и `manaregen`. Поэтому существующие пути `resetResources`, смерть/respawn, `forget` и остановка игры очищают его через общий cleanup. Положительное значение выдаёт `lbc:true_advancements/potions/effect_no_magic` в двухсекундном цикле, как `players_2s.mcfunction`.

Ограничение текущего этапа: Java API перенесено, но источники (мечи, debuff-таблицы, еда и прочие провайдеры из `NOMAGIC_SOURCE.md`) ещё должны вызывать `applyNoMagic`; отдельные native-команды/предметы здесь не добавлялись. Командная фильтрация будущих hostile effects остаётся отдельной задачей: союзники и `BedWarsShopVillager` должны отсеиваться на trigger/origin, выборе цели и непосредственно перед уроном или эффектом.

В адресном сценарии жезлов проверены прибавка длительности с обнулением маны,
пауза regen при продолжающемся potion timer, секундный countdown, time-stop
и observer pause, разрешённый каст при положительном таймере и общий cleanse
через `LbcItems.clearEffects` с сохранением ресурсов. Итоговые хеши и полная
регрессия: [SERVER_TESTS.md](../SERVER_TESTS.md).
