# Overnight resource integration ? 4 November 2026

Status: static integration verified; root owns compilation, exact artifact inspection and runtime qualification. No build, server operation, commit, source-server modification or resource-pack mutation was performed here. Read repository AGENTS, agent_notes/AGENTS, WORKFLOW, RESOURCE_PACK, Serena initial instructions and the NIGHT source/native notes for Zaika, Expansion and Hakurei. Existing author WIP and resourcepack_template deletions were preserved.

## Exact changes

`build.gradle` processResources adds exactly fifteen input paths: existing canonical `lbc:touhou_item`, existing canonical `lbc:bomb_item`, and the thirteen new singleton leaves `ported/admin_zaika`, `ported/touhou_collectible_1..10`, `ported/touhou_bomb_item`, `ported/touhou_time_stopper`. These are sixteen added lines including the explanatory comment. There are no recipe, dependency, task, production/test exclusion, runtime or Java changes. Root's packaging/recipe guard should accept only these fifteen added include paths; all prior paths remain unchanged.

`skywars2/lbc_loadouts.json` adds skill24/page10000/slot24 with gate `lbc:true_advancements/legends/academy/dublicating` and lockedKey `lbc.adv.upgraded_tuntija.dublicating.lore`; skill28/page10100/slot0 with gate `lbc:true_advancements/legends/academy/danmaku` and lockedKey `lbc.adv.upgraded_staff.danmaku_staff.lore`. Both exact gate/slot/lockedKey combinations were checked against original `swrg_kit_integration/gui/{page,select}/{10000,10100}.mcfunction`. IDs and positions have no duplicates. Fourteen lines added; existing rows are intact.

No new definitions or reconstruction were necessary. Existing includes already cover all `item_defs/**/*.json`, `time_stopper.json`, the five Zaika full-set families and their complete armor dependencies, both skill GUI pages, and `skywars2/**` including `expansion_blocks.json`. Existing `loot_test_catalog.json` prefix `lbc:ported/` exposes all thirteen new leaves automatically. Therefore no extra catalog file or broad new resource wildcard was needed.

## Static proof

Ignored evidence: `build/verification/overnight-2026-11-04/resource_closure_proof.py` and `resource_closure_proof.json`. Re-run from repository root with Python. Result: **PASS STATIC ONLY**,48 native tables,51 reference edges,missing0,unpackaged0. Explicit roots are all thirteen leaves, canonical Touhou/bomb/time, Zaika selector and both source skill GUI tables. Every reached table exists and matches a current processResources include; JSON records all IDs, edges count, RP key values and SHA-256 of inspected JSON inputs.

Canonical `touhou_item` preserves all eleven original entries in source order and exact weights `[800,100,3,1000,1,10,200,100,10,10,1000]`, total3234. The nested weight10 bomb remains present. All ten collectible leaves match their corresponding canonical entries exactly after removing only pool weight. Source/native normalized JSON equality passed for the full Touhou pool, bomb, time stopper and both complete GUI tables. Normalization only resolves existing item_defs, translates existing modifier/type encoding to functions/function encoding, and omits explicit quality0 (the default). Every line, nested text component, formatting property and component order remains compared.

The canonical Zaika singleton equals its exact original Loli source entry with only selection weight/default quality0 removed. It preserves the three white translated lore lines, nested birthday line, both custom-data flags, original carrier, model and tier; no active ability was invented.

All40 presentation translate keys exist in actual RP `assets/minecraft/lang/{ru_ru,en_us,rpr}.json`; their exact values are captured. All15 referenced item models exist in actual `assets/lbc/items/`. Original source icon models `lbc:expansion` and `lbc:score_items`, white nonitalic names and full gray nonitalic lore/stat component structures compare exactly. No localization/model/style/template files were added or changed.

The static proof does not establish client rendering, runtime skill activation, native output publication or exact packaged JAR closure. Root must run the authorized fixture suites and inspect final artifacts before claiming qualification. Original Loli possession-predicate corrections and all shared Java integration remain root-owned.
