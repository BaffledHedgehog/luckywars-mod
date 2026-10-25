# Acid Academy presentation - prepared 25 October 2026

Added only `src/main/resources/data/lbc/loot_table/ported/academy_acid_wand.json` and this note. READY ONLY: no installation, restart, RP write, translation edit, or template restoration.

The leaf `lbc:ported/academy_acid_wand` creates native `skywars2:academy_acid_wand`, count1, model `lbc:st/acid`, final tooltip `lbc:tier_a`. It copies all authored name/lore components from ordinary `ported/acid_wand.json` verbatim: light-purple nonitalic name `sauvojen_wand_acid`, aqua type/staff and dynamic mana row, four gold description rows and one yellow combination row. Appends only green nonitalic `upgraded_item`, exactly matching the legacy helper and native LbcAcademy promotion. No extra mode lore was invented. Custom data retains craftusable/transformable/staff/magicwand/stacid=1 and adds upgradable=0, upgrade_available=1, upgraded=1, mode=1, following existing native Academy leaves.

Legacy source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Read `loot_table/stacid_wand.json`, `function/other/magic_academy/upgrade/stacid.mcfunction`, `can_upgrade/stacid.mcfunction`, `interacted_upgrade.mcfunction`, and `item_modifier/add_upgrade_desc.json`. Source common promotion sets upgraded/mode1 and appends the marker; the stacid branch announces `upgrade_succesful_stacid` and applies tier A. Its condition uses `upgrade_condition_stacid`. The spelling `succesful` is authored and must remain exact.

Read-only RP: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Both `assets/minecraft/lang/ru_ru.json` and `en_us.json` already contain all14 required keys: `sauvojen_wand_acid`, `.lore.1` through `.lore.5`, `type`, `staff`, `mana_on_activate`, `upgraded_item`, `mode_default`, `mode_ignited`, `upgrade_condition_stacid`, `upgrade_succesful_stacid`. No duplicate translations or template fragments are needed. Authored RU mode values are 'Режим жезла: Стандартный' and 'Режим жезла: Подожжённый'; EN values are 'Wand Mode: Default' and 'Wand Mode: Ignited'. Existing `assets/lbc/items/st/acid.json` resolves to `lbc:item/st/acid`; tier-A background exists.

Validation: JSON parses; original seven lore rows/name are structurally identical, with only the generic eighth marker appended. Both language key sets and model entry were checked directly. This is static presentation verification, not native registry/loot codec or real client-pixel qualification. The table intentionally uses the repository current codec (`modifier` and modifier `type`), not the legacy datapack `functions`/`function` fields. The retained `stacid_wand_mana_use` score component requires the existing native loot resolver; its score text must remain dynamic. Native item registration must exist before runtime table loading. Parent integration should exercise native loot decoding and confirm final A tier/custom flags/marker count and mana row resolution.

## Integrated qualification

Build `1229b044` passed 28 recorded suite results
in two actual mode1 matches (124.28 seconds), including ordinary/awakened acid, Gaycaster, acid ants and actual retained-projectile match-stop cleanup.
Exact 344 Java sources / 942 classes /
2814 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-acid-academy-1229b044.jar`. Evidence: `build/verification/acid-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
