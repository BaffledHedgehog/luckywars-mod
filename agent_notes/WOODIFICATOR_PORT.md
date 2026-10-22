# Woodificator — current port contract, 21 October 2026

Source audit: [NEXT_POOL_AUDIT_2026_10_21](NEXT_POOL_AUDIT_2026_10_21.md).
The latest user policy supersedes the audit's old twelve-tag proposal and pending
questions. The final build and four isolated native matches passed on
`1f5e4b42...`; the archive is ready without installation.

## Current user requirements

All nonair, not fully invisible source blocks convert to the nearest FULL solid
wood block by average texture colour generated offline. Preserve every block
made exclusively of wood, of ANY shape, unless it has a block entity (user).
This includes preserving wooden stairs/slabs/fences and similar shapes rather
than repeatedly replacing them with full cubes. Block entities are the explicit
exception, so wooden containers still convert. Bedrock, portal-related blocks
and other visible nonwood materials are governed by the new broad policy,
not historical woodifier allowlists. Fully invisible blocks remain unchanged.

All stored container contents must drop as item stacks before replacement
(user); source block loot, furnace XP and unrelated native entity side effects
must not run. Handle Container implementations, campfire stored stacks and
lectern books; do not silently delete contents under old strict-fill behavior.
An unloaded archaeology loot table is distinct from an already stored item.
Preserve the existing indirect-kill ownership path for terrain consequences.

## Existing geometry and integration

The source beam has 51 positions from the eyes at 1.5-block spacing (0 through
75 blocks inclusive), each covering an inclusive 5x5x5 cube. Obstacles and
entities do not stop it; there is no direct entity damage. Use is free, without
cooldown or durability consumption. LbcWands owns both-hand routing and shared
role/freeze/time-stop/suppression checks; LbcShooters owns the item behavior.
Reuse BlockAreaWork/Stonekivi bounded queued terrain processing and LbcItems
lifecycle rather than introducing a projectile or parallel scheduling system.
Overlapping cells must be processed once; stale owner/match work must not write.

## Offline colour generation

`tools/generate_woodificator_palette.py` (Python 3 + Pillow) reads the client
assets and ordered resource-pack layers; it writes a checked lookup rather than
performing texture analysis during gameplay. Example, with actual local paths:

```powershell
python tools/generate_woodificator_palette.py --client-jar CLIENT.jar --resource-pack USER_PACK --registry build/verification/woodificator-block-registry.json --output src/main/resources/skywars2/woodificator_palette.json --evidence build/verification/woodificator-palette-assets.json
```

`CLIENT.jar` is the current vanilla client asset jar; `USER_PACK` is the existing
read-only 26.3/all_in_one_1.21.8 pack. The registry snapshot must come from the
current native runtime and identify full solid target candidates. No Minecraft
map colours or runtime world palette scans substitute for texture analysis.

Generator resolves model parents, texture variables, blockstate variants and
multipart conditions. It weights rendered faces by geometric area and mean
alpha, samples UV rectangles/all six faces (including log caps), uses animation
frame durations, averages linear sRGB and chooses by CIE76 distance in D65
CIELAB. Generated metadata records asset inputs and fallback methods.

Limitations: one stable mapping per block ID approximates state-dependent
appearance; biome tint uses representative plains temperature0.8/downfall0.4.
Special renderers use explicit UV/atlas adapters, while particle-only models
must be reported as fallbacks. Model geometry, texture sampling and CIE76 are
an offline appearance approximation, not exact in-game lighting or perceptual
identity. Palette eligibility must enforce FULL solid wood output; preservation
of exclusively wooden source shapes is a separate policy. Transparent pixels
contribute no colour/weight; fully invisible sources must remain unmapped.
Changes to client assets/resource-pack require regeneration and revalidation.

## Item resources and acquisition

`tools/import_lbc_woodificator.py` extracts the canonical ordinary shooter entry,
migrates the schema and changes only the server item ID to skywars2:woodificator.
The qualified output is `data/lbc/loot_table/ported/woodificator.json`, visible
through the existing ready-table completion prefix.
It no longer imports/verifies obsolete woodifier tag closure. Shared existing
tags remain untouched. RU/EN keys, model, texture and tier D tooltip are checked
read-only in the current resource pack; no template duplicates are needed.
Original white nonitalic name/model, aqua type+shooter and white lore remain.

This single item does not qualify broad random pools. Reforge currently reaches
packaged ordinary/all shooter tables containing unfinished outcomes; filtering
/loot autocomplete does not block those internal rollTable calls.

## Verified build

Final build and four isolated matches passed on SHA256
`1f5e4b42a68c3ffaf47bb4440542c907716f8beac5f91181c3deddf0a64e4fba`.
Two Woodificator matches: eight suite invocations, 52.35 seconds including
startup/shutdown. Two additional Phantom Knives/Stand matches exercise the
shared shooter dispatch/lifecycle and the actual loot completion catalog.
All 1287 native registry IDs were independently checked: 1281 color mappings,
six invisible exclusions, 66 native full-cube targets with no block entities,
242 classified wooden source IDs subject to the actual-state BE override.

Real ray checks cover fluid/unbreakable/portal/translucent/collision-free/shaped
sources, wooden stairs/slabs/doors/buttons/fences/trapdoors/plates and sideways
log state preservation. Chest, shulker, lectern, campfire and jukebox spill exact
items once, without source-block items; repeated casts do not duplicate them.
The 2000-cell overlap union, free repeated casts, both hands/native+legacy items,
real antimagic source, time-stop pause, native suffocation ownership and match
stop cancellation passed. This is a headless native/Polymer check, not a real
client visual review or a measured production MSPT benchmark.

Evidence: `build/verification/woodificator-results.json`,
`stand-results.json`, `woodificator-palette-validation.json`,
`woodificator-palette-assets.json`, and `woodificator-verification.json`.
Archive: `build/verification/skywars2-woodificator-1f5e4b42.jar`.
Main server/JAR and the user's resource pack were not changed.

## Generated classification and regeneration

The final candidate set is derived from native wooden semantic tags plus audited
wood-only IDs, then filtered by native default-state fullCube and explicit BE
exclusions. It includes Poplar, crafting_table and mangrove_roots. The preserved
list covers all wooden shapes, including bamboo mosaic and signs; runtime BE
checks override it. Leaves, saplings, living bamboo, muddy roots, bookshelves
with books, wool beds, scaffolding with string and mixed workstations are not
classified as exclusively wood. See per-ID preserved_wood_sources evidence.

`/sw check woodificator-palette-report` exports the installed registry to the
server working directory as woodificator-block-registry.json; copy it into the
project build/verification path before generating. Regenerate when registered
blocks, client assets or pack overrides change. The shader/user pack is read-only.
