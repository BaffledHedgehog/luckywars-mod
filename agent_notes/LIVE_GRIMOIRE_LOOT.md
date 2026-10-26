# Editable live grimoire loot — 26 October 2026

The author wants to edit grimoire rarity directly and apply it with `/reload`,
without another agent change or mod build (user). `EXTRA_GRIMOIRES.json` is only
a temporary reference, not a permanent settings interface.

The primary server now has this external override pack:
`E:/servers_mew/skywars2_last/world/datapacks/grimoire_overrides/`.
Its `data/lbc/loot_table/ported/*_grimoire.json` files cover all28 added books.
Current author-edited rarity/tier values from the temporary catalog were copied
once when creating the pack. Subsequent edits belong in those live loot tables.
No automatic regeneration or copying into the author resource pack is needed.

The final `minecraft:set_lore` row holds the base rarity and optional adjective
with their colors. `minecraft:set_components` holds `minecraft:tooltip_style`.
The pack's `spells_library.json` references these28 leaves instead of embedding
copies. Original240 library entries are unchanged. To edit an older book's
library appearance, its embedded entry is also available in this live broad
table. Do not regenerate the broad override over author edits.

After editing, `/reload` updates subsequent loot rolls, including the library
pool. Existing stacks retain their saved components. Native `/give` defaults
remain registered in the mod; use `/loot` for testing these live changes.
The pack is outside the mod source and must survive mod replacement/builds.

Second-part keys/colors preserved from the legacy books:

| Key | Color |
| --- | --- |
| enhanced | gray |
| overhauled | gold |
| universal | yellow |
| powerful | dark_purple |
| smart | aqua |
| unreal | red |

Do not rerun `tools/generate_extra_grimoires.py` or the old
`tools/check_extra_grimoires.py` over manual author edits: that historical check
also invokes generation. The proposed permanent catalog-settings binding was
reverted. Future new-book acquisition changes must preserve these live overrides.

Evidence: `build/verification/grimoire-rarity/`. The final native probe loaded
the pack on the isolated server, entered an actual match, changed a leaf's tier
and adjective, ran `/reload`, and verified the new components through the broad
library reference. It stopped normally and restored its final-run fixture files.
Main JAR remains41395417; the primary server was not reloaded or restarted.
