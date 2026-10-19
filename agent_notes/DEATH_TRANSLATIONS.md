# Death-message translations — 16 October 2026

The template contains 131 death-message additions per language: the 124 from
the earlier pass plus four owned `.player` keys (`cactus`, `even_more_magic`,
`genericKill`, `sulfurCubeHot`) and three missing `.item` variants. The existing
`cactus.item` override is reused. The custom electric message ID is
`skywars2.electric`.

All three custom electric types now use this message ID. Environmental damage
with one-name base text selects `.player` for an explicit attacker; native
named-weapon `.item` selection is preserved. Cactus keeps its existing two-name
resource-pack base key. Component keys and argument counts are checked natively.

Vanilla keys follow project damage-type JSON and local Minecraft 26.3 English
language data. That client jar has no Russian language file; Russian translations
follow available Minecraft language data. Eleven existing source-pack death
overrides were preserved and omitted. Coverage is limited to project-defined
types and explicitly handled mixin message IDs, not the full vanilla registry.

Merge `resourcepack_template/assets/minecraft/lang/{en_us,ru_ru}.json` into the
corresponding source-pack languages. Both files parse, contain no source-pack
duplicates, and preserve generated argument slots. Four UI keys already present
in the source pack were removed from the template. The source pack was read only.
