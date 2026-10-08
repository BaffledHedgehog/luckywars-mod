#!/usr/bin/env python3
"""Apply reviewed prose corrections atomically to the kit-game notes."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / "agent_notes/lbc_kit_game.md"
EXTRA = "У `blockgen.py` есть ещё точный риск: 1 105 из 1 106 текущих числовых `.mcfunction` не имеют завершающего newline, а генератор открывает их с `mode='a'` и пишет целую вторую команду `setblock ... strict` без конечного newline. В этих 1 105 файлах новая команда склеится с существующей в одну повреждённую строку; ID 967 с newline получит отдельную строку, но `strict` не является допустимым режимом `/setblock`."

REPLACEMENTS = {
    "**PHD kit.**": """**PHD kit.** Событие изменения инвентаря запускает craft, если в трёх или четырёх слотах `player.crafting.0..3` лежат предметы из `#minecraft:bioitems`. Craft начинает общий score `hash` со значения `#game_seed`, переносит каждый подходящий ингредиент через временный блок-буфер, складывает значения символов ID с позиций 10–37 (буквы `a..z` и `_` кодируются числами 1..27), затем берёт остаток по 144 и выдаёт `lbc:kit/pill`; это не фиксированный рецепт. Каждый подходящий ингредиент возвращается в свой crafting slot после расхода одной штуки: одиночная стопка помечается как `minecraft:air`, у стопки больше одной уменьшается count на 1. Есть важная особенность реализации: `currhash` не сбрасывается перед каждым символом, а символы вне таблицы a–z/`_` не задают новое значение, поэтому цифры/прочие знаки могут повторять последнее закодированное значение; результат зависит от seed, порядка и строк ID, а не только от четырёх предметов как набора. Хеш складывается в глобальных scoreboard/storage и координатном буфере, но команды выполняются синхронно в контексте игрока. Источники: [crafting trigger](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kits/changed_inventory_phd.mcfunction), [craft handler](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kits/phd_craft.mcfunction), [символьный хеш](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kits/phd_hash.mcfunction), [вариант для crafting slot 0](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kits/phd_hash_0.mcfunction), [pill loot reference](../world/datapacksdisabled/LBC/data/lbc/loot_table/kit/pill.json).""",
    "**Expansion (skill 24).**": """**Expansion (skill 24).** Пока игрок крадётся, счётчик набирает 60 тиков; `expansion_start` ставит камень под ногами только если там воздух, заполняет четыре соседние клетки копией центрального блока, создаёт четыре маркера и переносит на них ID/team игрока. Каждый активный маркер раз в tick пробует с шансом 1% заменить блок под собой одним из 1 106 равновероятных ID, затем продолжает крест на одну клетку в стороны, копируя блок только в воздух; с шансом 10% он также создаёт ветку на уровень ниже. Выбор блока не ограничен воздухом, а в начале каждого прохода маркер удаляет `Items` у block entity в своей клетке, поэтому сундуки/другие контейнеры могут потерять содержимое; сами поставленные блоки при завершении не убираются. Изолированные red/brown mushroom blocks и stem функция превращает в варианты со всеми directional faces=true. Отпускание sneak завершает платформу и убивает принадлежащие игроку markers; если игрок продолжает красться, после счётчика 80 каждый tick есть шанс 20% на такое же завершение. После завершения остаётся tag `expansion_cd`: без sneak его score уменьшается с шансом 50% за tick, и tag удаляется только когда score дошёл до 0, поэтому повторный старт требует отпустить sneak и дождаться случайного спада cooldown. Равномерный выбор ID даёт повторяющимся `test_block` (458/971) и `test_instance_block` (394/972) вдвое большую вероятность, чем уникальному blockstate. В snapshot `blockgen.py` отличается от runtime-файлов именно в ID 394/458: генератор записал бы `glow_item_frame`/`item_frame`, а реальные `.mcfunction` ставят `test_instance_block`/`test_block`; ID 165 `iron_chain` эквивалентен `minecraft:iron_chain`. Не запускайте этот вспомогательный генератор: он открывает файлы в append-режиме и приписывает суффикс `strict`. Полная таблица фактических ID→blockstate приведена в [каталоге](lbc_kit_game_blocks.md). Источники: [charge/start](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick.mcfunction), [start geometry](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_start.mcfunction), [marker update](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker.mcfunction), [directional continuation](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x.mcfunction), [active/end check](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_active.mcfunction), [cleanup](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_end.mcfunction), [random selection](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/place_random_block.mcfunction), [runtime ID 394](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/blocks/394.mcfunction), [runtime ID 458](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/blocks/458.mcfunction).""",
    "Skill 20 `Simbiont`": """Skill 20 `Simbiont` каждый player tick вызывает внешний `lbcr:podchinenie_simbiont` для подходящей ближайшей non-player сущности в двух блоках, затем обходит всех не-strict mob во всём мире с совпавшим `lbcID2`. Tick helper ставит `tmp=1`, а игрок получает Resistance V на 1 секунду, если хотя бы один такой mob обработан. В `simbiont_tick_mob` после обновления score `lucky_block_count = Health×10` стоит команда `kill @s`, если score не больше 10 000 000; буквально это удаляет подходящих мобов с Health≤1 000 000, то есть обычных мобов, если внешний helper не изменил их здоровье выше порога. При `HurtTime:10s` и отсутствии `damage_repeat` функция сравнивает сохранённое здоровье с текущим и наносит владельцу разницу через `damage ... outside_border`; отсутствие действительной записи здоровья может повлиять на первую разницу. Обратный callback считает число принадлежащих мобов (это именно count из `execute store result ... if entity`), делит `blocked_damage` на него целочисленно, поднимает результат минимум до 10 и передаёт `score×0.1` каждому мобу как урон; при нуле мобов деление не проходит и селектор урона пуст. `lbcID2`, `addicted`, `simbidaun`, `damage_repeat` и общий storage `lbc.math` связывают два направления обмена; точная внешняя подчиняющая механика не входит в этот пакет. Источники: [owner tick](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_tick.mcfunction), [controlled mob tick](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_tick_mob.mcfunction), [mob damage feedback](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_tick_mob_damaged.mcfunction), [owner damage distribution](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_damaged.mcfunction), [distributed hit helper](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_damaged_storage.mcfunction), [damage to owner](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/simbiont_damaged_storage_mob.mcfunction).""",
}

def main() -> None:
    text = TARGET.read_text(encoding="utf-8")
    lines = text.splitlines()
    replaced = set()
    for i, line in enumerate(lines):
        for marker, replacement in REPLACEMENTS.items():
            if line.startswith(marker):
                lines[i] = replacement
                replaced.add(marker)
    missing = set(REPLACEMENTS) - replaced
    if missing:
        raise SystemExit(f"expected prose markers not found: {sorted(missing)}")
    for i, line in enumerate(lines):
        if line.startswith("У `blockgen.py` есть ещё точный риск:"):
            lines[i] = EXTRA
            continue
        if line.startswith("**PHD kit.**"):
            lines[i] = line.replace(
                "`currhash` не сбрасывается перед каждым символом,",
                "`currhash` не сбрасывается перед каждым символом или новым craft-вызовом,",
            ).replace(
                "повторять последнее закодированное значение;",
                "повторять значение предыдущего символа или игрока;",
            )
            if "заранее force-loads этот chunk" not in lines[i]:
                lines[i] += " `load` заранее force-loads этот chunk и ставит yellow shulker по координате buffer Y=94, поэтому PHD не зависит от того, находится ли игрок рядом с буфером."
        if line.startswith("Trinket pass") and "нет проверки, свободен ли destination slot" not in line:
            lines[i] = line + " При переносе нет проверки, свободен ли destination slot: найденный trinket из hotbar заменяет содержимое inventory slot 4, затем 5 и далее, а сам hotbar slot очищается."
        if line.startswith("Levelups и trinkets хранятся") and "perk_random` разрешает только один random-perk" not in line:
            lines[i] = line + " Тег `perk_random` разрешает только один random-perk reroll за весь пересчёт: первый ID 14 бросает 1..44 и до десяти раз перебрасывает 14; последующие ID 14 не бросают повторно, а если десятый переброс всё ещё даёт 14, у него нет ветки эффекта."
        if line.startswith("**Expansion (skill 24).**") and not any("blockgen.py` есть ещё точный риск" in x for x in lines):
            lines.insert(i + 1, EXTRA)
            break
    temporary = TARGET.with_name(TARGET.name + ".review.tmp")
    temporary.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    temporary.replace(TARGET)

if __name__ == "__main__":
    main()
