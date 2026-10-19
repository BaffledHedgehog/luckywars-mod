# Следующий этап переноса льда

Исторический план после Snow Pearl / Ice Gem. Ice Sword теперь реализуется
через общий `LbcFrost`; актуальное поведение и результаты —
[ICE_SWORD_PORT.md](ICE_SWORD_PORT.md), [SERVER_TESTS.md](../SERVER_TESTS.md).
Уточнение сверки: source исключает только ARROW, а не все непрямые удары;
описание ниже сохраняет источники, но прежнее melee-only предложение заменено.

## Где старый код добавляет холод

- **Ice Sword** напрямую: `LBC/data/lbc/function/items/swords.mcfunction` проверяет выбранный предмет `{icesword:1}` и условия `nomagic_active` / `stopper_magic`, затем `workingitems/icesword.mcfunction` выбирает `@e[distance=0.001..7,nbt={HurtTime:10s},predicate=!lbc:same_team]` и вызывает `workingitems/icesword/pre`.
- **Snow Pearl (ID 18)** напрямую после шанса: `items/trinket_effects/snow_pearl_1.mcfunction` вызывает `pre` по радиусу 7 и HurtTime 10. Уже реализовано в `LbcTrinkets.damaged`.
- **Ice Gem (ID 20)** напрямую после шанса: `items/trinket_effects/ice_gem_1.mcfunction` вызывает `pre` для `type=#mobs`, `tag=!spectator`, не союзников в радиусе 12. Уже реализовано в `LbcTrinkets.damaged`.
- **Snow Wand upgraded snow meteor** вызывает не `pre` напрямую: `workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_snow_meteor_explode.mcfunction` выбирает мобов до 10 блоков, наносит `damage ... freeze`, затем вызывает `icesword_player_x5`; тот пять раз вызывает `icesword_player`, где Slowness ведёт в `icesword/{0..10}`, а без Slowness — в `pre`. Это ветка умения посоха со снежной структурой/уроном; не включать в минимальный следующий этап.
- `workingitems/iceshard.mcfunction` меняет `cold` напрямую и вызывает `other/freeze/check`, но не использует `icesword/pre`; считать отдельной механикой.

## Ice Sword: уточнённый контракт

После успешного non-arrow урона с активным игроком-владельцем и выбранным
`icesword:1` обрабатывается исходная область всех недавно раненых целей до
7 блоков. `AFTER_DAMAGE` вызывает `LbcItems.damaged`, который проверяет
подавление и делегирует общему `LbcFrost.hitTargets` / `add`.

Предмет Polymer не добавляется в каталог тринкетов: это оружие из случайного
пула transformables. Отдельный payload `lbc:ported/ice_sword` сохраняет его
оформление; общая перековка и lucky-выдача остаются незавершёнными.
Suppression `nomagic_active` и `stopper_magic` проверяется локально в событии;
перенос источников этих тегов и lifecycle подавителя нужен отдельно.

Проверять по узким источникам: `function/items/swords.mcfunction`, `function/workingitems/icesword.mcfunction`, `function/workingitems/icesword/pre.mcfunction`, `predicate/same_team.json`, `function/other/freeze/add_cold.mcfunction`, `function/other/freeze/check.mcfunction`, `loot_table/transformables_sword.json`. Native точки интеграции: `src/main/java/me/skywars2/SkyWars2.java` (`AFTER_DAMAGE`), `src/main/java/me/skywars2/game/LbcTrinkets.java` (`damaged`), `src/main/java/me/skywars2/game/LbcFrost.java` (`add`).

Snow Wand потребует отдельной ветки: существующие resources содержат source `transformables_staff.json`, а `ice_wand.json` и staff advancements/loot, но `lbc_loadouts.json` и native event handling их не описывают. Кроме reuse холодной цепочки понадобятся wand cast/mana/meteor, шаблон snow sphere, damage type `freeze` и очистка созданной области. Это существенно шире Ice Sword melee proc.
