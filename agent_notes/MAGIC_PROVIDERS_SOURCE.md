# Magic artifact and beacon source notes

Source payloads are read from `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/`. The migrated native tables preserve the selected entry's functions and components; only `name` changes. `mystical_artifact.json` selects the repeating command block carrying `unresolved_artifact:1`, `mystical:1`, and `type:1`. `magic_beacon.json` selects the panda spawn egg. Its `minecraft:entity_data` stays attached with `id: minecraft:marker` and tags `stopper_magic`, `rc_1sec`, and `marker_2t`; the native handler may control spawning, while this source payload remains intact.

## Mystical Artifact

The source assigns model `lbc:mysticalartifact`, tooltip style `lbc:tier_u`, name key `mystical_artifact` in `#C28CFF` with italic disabled, and one red non-italic lore component `mystical_artifact.lore`. Its custom data identifies it as a transformable unresolved accessory (`mystical:1`, `type:1`). The resource pack provides `assets/lbc/items/mysticalartifact.json`, `assets/lbc/models/item/mysticalartifact.json`, and animated texture `assets/minecraft/textures/item/mysticalartifact.png` (`frametime: 20`, interpolation enabled).

Existing pack text is reused without adding language entries: EN `Mystical Artifact` / `Unknown effect`; RU `Мистический Артефакт` / `Неизвестный эффект`.

## Magic Beacon

The source assigns model `lbc:stopper_magic`, tooltip style `lbc:tier_b`, name key `magic_breaker` in red with italic disabled, and two white non-italic lore keys in order: `magic_breaker.lore.1`, `magic_breaker.lore.2`. Its marker payload carries the three tags above. In the old datapack, stopper markers suppress magic effects within 20 blocks; `lbc:other/stopper_magic` removes blocks in `#minecraft:magic` within a 15-block cube and dispatches destruction for `#minecraft:magic_entities` within 20 blocks. The copied source tags are direct dependencies of that behavior. Native runtime behavior is supplied by the registered item handler.

The resource pack provides `assets/lbc/items/stopper_magic.json`, `assets/lbc/models/item/stopper_magic.json`, and `assets/minecraft/textures/item/stopper_magic.png` (`frametime: 1`, interpolation disabled). Existing text is reused: EN `Anti-Magic Beacon`, `Blocks magic in an area around itself`, `Does not block other jammers.`; RU `Антимагический Маяк`, `Блокирует магию в области вокруг себя`, `Не блокирует другие глушители.` No language or model template was needed.

The two source tags are copied unchanged to `data/minecraft/tags/block/magic.json` and `data/minecraft/tags/entity_type/magic_entities.json`. Their values are literal vanilla registry IDs (including vanilla aliases without an explicit namespace in the entity tag); neither tag expands another tag. The listed block IDs such as `torchflower_crop`, `pitcher_crop`, and `warped_wall_hanging_sign` are valid vanilla block IDs in the 26.3 target, so no optional entries are required. No additional tag or resource is required by these two loot payloads.
