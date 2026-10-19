# Looper: mystical artifact reveal

## Новый контракт (user)

> вообще навык looper с артефактами - лучше всего будет, если у чела с перком looper будет описание у него,а если чел без looper, для него будет обычный артефакт без описания (наверное так можно через polymer сделать? чтоб один предмет у разных игроков по разному отображался? но при этом чтоб как item он был один и тот же там на земле условно когда лежит)

Описание теперь зависит от получателя пакета. Looper выбран и LBC skills
включены — видны исходные `artifact_1`…`artifact_23`, жёлтые и не курсивные.
Без Looper видна исходная красная строка `mystical_artifact.lore`.
Один серверный стек/ItemEntity остаётся общим; lore, custom data и маркер
`unresolved_artifact` при просмотре не меняются. Это заменяет старое
постоянное раскрытие в главной руке. Уже раскрытые старой версией предметы
также отображаются неизвестными для получателя без Looper.

Косметический доступ работает и для наблюдателя с выбранным Looper.
Он не даёт права использовать игровые способности скопированных предметов.

## Source and scope

- Source skill dispatch: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/skills_tick_1s.mcfunction` calls `looper_tick` for skill 3.
- Main-hand gate: `.../game/skills/looper_tick.mcfunction` checks `SelectedItem.components.minecraft:custom_data.unresolved_artifact == 1`.
- Exact reveal behavior and 23 mappings: `.../game/skills/looper_explain_artifact.mcfunction` replaces lore row 2, removes only `unresolved_artifact`, then writes the item back to main hand.
- Artifact states and original two-row lore: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`; current migrated table: `src/main/resources/data/lbc/loot_table/ported/mystical_artifact.json`.
- Resource pack is read-only; existing `artifact_1`…`artifact_23` translation keys are consumed directly with their original yellow, nonitalic style.

## Implemented behavior and integration

`LbcSkills.artifactTooltip` implements the 23-state lookup through the native
Polymer item's `modifyClientTooltip` hook. Only the recipient tooltip's second
source lore line changes, before Polymer applies its normal tooltip wrappers.
All authoritative components, count and identity remain unchanged. Invalid state
or malformed/missing lore leaves the tooltip unchanged. Full literal keys and
original styles remain independently checked against the datapack and resource pack.

`LbcLoadouts` publishes per-viewer boolean capabilities in a concurrent map.
Packet conversion reads this snapshot rather than mutable player profiles or files.
Skill selection/reset and settings changes refresh it immediately; a one-second
fallback detects changes in the loaded in-memory profiles/config. Unchanged capabilities do not
resend inventories or scan entities. The hook grants no skill and activates no
gameplay ability; no new profile field or Java class is needed.

When a capability changes, native menu full-sync refreshes inventory, cursor
and the current container. Native metadata/equipment packets resend unchanged
stacks only to that viewer for already tracked dropped items and equipped artifacts.
Fabric `PlayerLookup.tracking` filters actual tracking recipients; Polymer performs
the usual per-recipient conversion. The loaded-entity list is visited only on a
changed capability, including extreme Creative heights, without loading chunks.
No stack or entity data is dirtied to force a refresh.

`GameChecks.checkLooper(game)` remains in the magic suite and covers all 23
states for two recipients, original component preservation and Polymer roundtrips,
legacy-resolved stacks, malformed/ordinary items, observer access, profile/config
changes and native tracked ground items. [Scenario details](LOOPER_VIEWER_TESTS.md).

The independent source oracle in `tools/check_design_sources.py` parses the old
`looper_explain_artifact.mcfunction`, verifies all 23 `(mystical,type)` lore
payloads/styles against the full literal key list and range mapping in
`LbcSkills`, and confirms each key exists in both read-only resource-pack
language files. Its report counter is `nativeLooperArtifactDescriptions`.

## Verification status

`python tools/check_design_sources.py` retains the 23-state independent oracle.
The viewer-dependent implementation passed its final magic focus in 62.93 s
and full regression in 260.82 s: 14 matches and all 39 runner groups.
The tested JAR is installed, its predecessor is backed up, both servers are stopped.
Native ground-stack conversion is verified for two recipients; rendered client
tooltips and refresh-packet delivery still need an actual client because the
test bots do not acknowledge chunks. Historical permanent-reveal tests do not
prove this new behavior.
Current evidence and limitations: [SERVER_TESTS.md](../SERVER_TESTS.md).
