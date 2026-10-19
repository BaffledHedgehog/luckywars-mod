# Mystical artifact color resources

This note records the resource-side type 7 port. Source color behavior and its caveats are documented in [ARTIFACT_COLORS_EFFECTS.md](ARTIFACT_COLORS_EFFECTS.md).

`tools/import_lbc_weapons.py` selects exactly one `minecraft:repeating_command_block` entry from the read-only legacy `LBC/data/lbc/loot_table/artifact.json` whose custom data has `unresolved_artifact:1`, `mystical:1`, and `type:7`. It migrates the legacy loot schema through the existing importer and changes only the item id to `skywars2:mystical_artifact`, writing `data/lbc/loot_table/ported/mystical_artifact_colors.json`. The independent static check repeats selection from the source and compares the full migrated payload.

The importer also copies these 18 legacy tag files byte-for-byte into `data/minecraft/tags/block/`: `colored`, `in_colored`, and the 16 dye colors (`black`, `blue`, `brown`, `cyan`, `gray`, `green`, `light_blue`, `light_gray`, `lime`, `magenta`, `orange`, `pink`, `purple`, `red`, `white`, `yellow`). Their `minecraft` namespace is intentional: Minecraft 26.3 does not provide these block tag definitions as built-ins. The `colored` and `in_colored` files retain `replace:false` and 128/64 entries; each individual color tag retains its 13 source entries, including the source duplicate wool entry. Gradle explicitly allows every file, and the static checker independently compares each destination's bytes with the read-only source and checks those counts.

Run `python tools/import_lbc_weapons.py` to repeat the import. The static source audit runs as part of `python tools/check_design_sources.py`. Neither command builds Gradle output or starts a server.

## Java branch audit (14 October 2026)

Read-only comparison of `LbcColors` with `other/artifact/afkartifact5t`, `colors`, and all 16 `colors/*.mcfunction` found the color dispatch order and direct branch payloads aligned. This includes green's `xp add ... 3` as experience points (Java `giveExperiencePoints(3)`, not levels) and its dust RGB (Java `0x36f31d` matches source `[0.212,0.953,0.114]`). The five-tick pulse and two trigger positions also match.

Final integration uses native alliances in place of legacy `team_number`; ordinary solo matches have no assigned teams, so this does not port a team game mode. Blue's secondary `waterpower` and `realdrownpre` engines were subsequently added through the shared living-entity effect index: water placement, in-water bonuses and in-air penalties. Purple retains both distinct source actions: holder spread radius 200/190 and hostile additive `effect_rtp_1` spread radius 10. Java stores the nominal 600 ticks directly instead of converting Raid Omen 40 on the following entity tick; see the phase limitation in [ARTIFACT_COLORS_PORT.md](ARTIFACT_COLORS_PORT.md). This correction supersedes the early audit's mistaken suggestion that target spreading replaced holder spreading. The full mana engine remains outside this batch.
