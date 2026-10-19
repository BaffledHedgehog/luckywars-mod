# Следующий шаг: выборочная очистка эффектов артефактом

Исторический план: тип 5 теперь реализован вместе с общей очисткой кастомных
статусов типом 6. Актуальное поведение и статус проверок —
[ARTIFACT_DEBUFF_PORT.md](ARTIFACT_DEBUFF_PORT.md). Описание «ещё нет» ниже
относится к состоянию до этой пачки.

## Исходный контракт

- `mystical:1,type:5` запускается из `other/artifact/afkartifact5t.mcfunction:7` на том же 5-тиковом цикле, что типы 6/8: только для носителя `afkartifact5t_active`, `tag=!spectator`, с предметом в `Inventory`, и вне 20 блоков от legacy `stopper_magic`.
- `other/artifact/noeffects1.mcfunction` снимает ровно: blindness, slowness, weakness, levitation условно, mining fatigue, glowing, hunger, instant damage, nausea, poison, unluck, wither.
- Levitation снимается только если отсутствуют все шесть исключений одновременно: `armor.feet` с `{reactive:1}`; `armor.feet` с `{technomk2:1}`; `armor.legs` с `{technomk2:1}`; `armor.legs` с `{techno:1}`; любой `container.*` с `{reactive_shish:1}`; любой `weapon.*` с `{reactive_shish:1}`. Проверки последних двух покрывают carried inventory и обе руки. Другие эффекты этими исключениями не защищаются.
- В dispatcher порядок: тип 8 (`metabolism`), тип 5 (`noeffects1`), тип 6 (`nohp`). При 5+8 тип 5 очищает выданный в этом же цикле Hunger, но не Regen; при типе 6 полная очистка выполняется последней.

## Что уже есть в новом проекте

- `LbcMagic` уже хранит битовую маску известных типов на игрока, обновляет её из общих dirty-inventory событий и обходит только учтённых носителей раз в пять тиков. Типы 1, 6 и 8 поддержаны; добавление типа 5 можно ограничить той же маской и пассивным циклом.
- `LbcMagic` умеет только `removeAllEffects()` для type 6; селективной очистки и levitation guard там нет. `LbcItems.effectsChanged()` также вызывает `removeAllEffects()` и сбрасывает временные теги/состояние предметов, поэтому его нельзя повторно использовать для type 5.
- Loot payload исключений присутствуют в ресурсах: reactive броня `neko_set_steampunk`, techno броня `neko_set_techno`, technomk2 броня `neko_set_technomk2`, жезл `reactive_shish_item.json`. В Java `LbcItems`/`LbcMagic` нет соответствующих механик этих исключений; наличие payload подтверждает перенос данных предмета, не их поведения.

## Безопасный следующий объём

Добавить только type 5 в существующий снимок инвентаря и пятитиковую обработку `LbcMagic`: снять перечисленные эффекты, а levitation снимать после точных проверок слотов и custom data выше. Сохранить текущие gates допуска, живого игрока, мира матча и подавления legacy/native маяком радиуса 20. Не менять type 6/8 и поведение защитных предметов. Отдельно проверить сочетания типов 5/6/8 и каждое исключение levitation; runtime-проверки ещё не выполнены.

Источники: старые `function/other/artifact/noeffects1.mcfunction`, `afkartifact5t.mcfunction`, `function/players_5t.mcfunction`; новые `LbcMagic.java`, `LbcItems.java` и перечисленные loot tables.
