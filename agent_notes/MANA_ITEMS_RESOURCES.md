# Mana potion source payloads

The imported definitions use two read-only OLD LBC snapshot files under `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/`:

- `imba/potions.json`: 30 entries, five mana effect families across three delivery forms and two durations (3600 and 9600 ticks; 03:00 and 08:00).
- `transformables_potion.json`: 9 source entries representing five regular payloads. Four regular payloads each occur twice: a weight-10 entry and a weight-1000 entry conditioned on `final_luck_modifier` being exactly 10. `remove_mana` occurs once. The duplicate entries and their weights/conditions are preserved as separate entries.

All mana custom effects use `minecraft:raid_omen`; family identity is their amplifier. Every item payload retains its source name and lore components, potion contents/color, tooltip style and display settings, custom data, entry weight, and condition semantics. The item ID changes to the appropriate native Polymer delivery ID; the legacy score condition uses the equivalent 26.3 codec described below.

| Family | Amplifier | Regular source delivery / duration | Native Polymer IDs by delivery |
| --- | ---: | --- | --- |
| `add_manamax` | 17 | drink, 1 tick | `skywars2:mana_potion`, `skywars2:mana_splash_potion`, `skywars2:mana_lingering_potion` |
| `add_manaregen` | 18 | drink, 1 tick | same three IDs by delivery |
| `add_mana` | 19 | drink, 1 tick | same three IDs by delivery |
| `remove_mana` | 20 | lingering, 20 ticks | same three IDs by delivery |
| `temporary_mana` | 16 | drink, 4800 ticks | same three IDs by delivery |

The 15 tables live at `data/lbc/loot_table/ported/mana_<family>_<delivery>.json`, with `<delivery>` equal to `drink`, `splash_potion`, or `lingering_potion`. Each contains its matching imba entries and any real regular source entries for that family and delivery. This is 39 entries total: 30 imba plus all 9 regular source records. No regular splash entries or missing family/delivery combinations are synthesized. The importer converts the regular source `minecraft:value_check` to the Minecraft 26.3 `minecraft:int_value_check` codec: its numeric provider remains `minecraft:score` targeting the fixed holder `final_luck_modifier` and objective `lbc.math`; the source scale is exactly 1 and is omitted because the 26.3 score provider yields the same integer scoreboard value. The primitive source `range: 10` is an exact point, serialized as `test: 10`; it evaluates true at 10 and false at 9 or 11. This retains the original equality condition rather than interpreting 10 as an upper bound. The independent `check_design_sources.py` reads both OLD LBC files and compares every full migrated entry, including item ID, weight, conditions, components, names, and lore. The 15 tables are listed individually in the `processResources` allowlist in `build.gradle`.

The source design keys for imba variants are `item.minecraft.{potion,splash_potion,lingering_potion}.effect.<family>` and the matching lore keys, with `imba_version` as the second lore line. Regular variants use the existing short keys `mana_potion`, `max_mana_potion`, `regen_mana_potion`, `add_mana_potion`, and `remove_mana_potion`, along with their `.lore` keys. The user's resource pack remains read-only; its translations and visual assets stay authoritative.

This note documents source payloads and import scope. It does not claim Polymer item registration or mana potion runtime behavior has been completed or tested.
