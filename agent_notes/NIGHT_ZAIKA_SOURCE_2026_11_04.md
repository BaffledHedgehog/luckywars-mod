# Admin Zaika: finite source audit, 4 November 2026

Read-only source investigation; only this note was written. No build, server run, code, resources or commits. Serena initial instructions, repository AGENTS and agent_notes/AGENTS/RESOURCE_PACK were read. Original server and real RP remained read-only. TMT remains LAST.

## Finding

**The source item is a passive loot-selection token. No source active ability was found.** `magicwand:1` alone does not establish a spell. The earlier status sentence ?active ability unported? should not be interpreted as evidence that an active source ability exists. Source closure supports inventory possession -> loot predicate -> selected pool, and nothing resembling rays, targets, combat or Chef erasure.

Exact legacy root used below: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/` (D). Native root: repository `src/main/resources/data/lbc/loot_table/` (N).

## Identity and presentation

D `loot_table/loliluckyblock.json:1285?1348`, pool 0 entry 23, gives one `minecraft:carrot_on_a_stick`, weight10, no local conditions, model `lbc:zaika`, tooltip `lbc:tier_s`, custom data `{zaika:1,magicwand:1}`. Native canonical leaf N `item_defs/lbc/zaika__e7f523430e71.json` already retains the exact source components.

Name is translated `admin_zaika`, nonitalic, with no explicit color. Three white nonitalic translated lore lines use `admin_zaika.lore.1/2/3`; fourth line is green nonitalic `JustSh1sh_` followed by dark gray nonitalic ` Happy Birthday 11.12.2023!`. Preserve the nested component structure, inherited formatting, line count and ordering.

Real author RP was read at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. `assets/minecraft/lang/{ru_ru,en_us,rpr}.json:2692?2695` contain existing keys. RU: ?????? ??????; ?????? ?????????? ? ???????????!?; ???????? ???????? ??????? ??? ?? ????????; ?????-????? ? ? ????-????? ????? ?????? ?????? ????!?. EN describes protection from traps and sets from Neko. Do not duplicate or rewrite these keys.

Existing `assets/lbc/items/zaika.json` selects `lbc:item/zaika`; `assets/lbc/models/item/zaika.json:3?5` uses `minecraft:item/zaika` and `minecraft:item/zaika_frame`. No new model/texture/font is indicated.

## Complete direct behavior

D `loot_table/nekoluckyblock.json:7?72`: with no selected mainhand `luck_key:1`, no carried Zaika selects `lbc:nekolucky_items`; carried Zaika selects `lbc:nekolucky_zaika_items`. Each branch has weight10. Holding luck_key excludes both of these branches; do not override the existing key behavior. This uses custom-data possession, not the translated name or selected hand. Armor/offhand are part of the old Inventory NBT domain; native Neko explicitly represents inventory, armor and offhand.

D `loot_table/nekolucky_zaika_items.json`: one roll, five equally weighted (10 each) full sets: `neko_set_{steampunk,techno,paladin,mage,phantom}_full`. Each full table references its four corresponding armor slot tables. Exact finite dependency walk: **26 tables, 25 reference edges, missing0** (one selector, five full sets, twenty armor leaves). No recursive or dynamic table reference in this branch. These are existing equipment families, not Zaika-spawned actors or damage. Retain exact component definitions, not reconstructed simplified sets.

D `loot_table/loliluckyblock.json` has **12 negative Zaika gates**. Zaika removes these entries from eligibility; it does not cancel traps that are already spawned, cleanse debuffs, protect another player, or filter every harmful item globally. Pool0 indices (0-based), weight, entry:

| Index | Weight | Excluded entry | Other source condition |
|---|---:|---|---|
|2|10|drop_enzan|final_luck_modifier !=7|
|5|10|drop_stalin|final_luck_modifier !=7|
|6|10|drop_mom|final_luck_modifier !=7|
|9|10|lbc:malevich_rnd|final_luck_modifier !=7|
|10|10|drop_transformator|final_luck_modifier !=7|
|12|10|drop_otchim|final_luck_modifier !=7|
|19|10|drop_helicopter|ranked !=1|
|20|1|drop_helicopter|ranked =1|
|22|10|drop_muravei|none|
|27|10|lbc:crocodildo|ranked !=1|
|29|10|lbc:herman_antigravity|gamemode not3..4|
|32|10|lbc:lust_death|gamemode not3..4|

Source marker lines: 247,405,476,609,680,756,1133,1204,1277,1394,1432,1478. Keep other score conditions and weights unchanged. Zaika's own entry has no anti-duplicate condition and may drop again while carried. The remaining eligible Loli pool is not rewritten into Neko full sets; the lore's grammar must not drive invented Loli behavior.

## Input, clocks, jammers, damage and crafts

D `function/items/spells.mcfunction:7,13` routes generic magicwand items through `workingitems/spell1` / `spell1_left` under generic magic suppression checks. `spell1.mcfunction` sets Distance0, adds temporary caster/raycaster tags, chooses upgraded/default dispatcher and removes tags; left dispatcher chooses its corresponding upgraded/default handler. Zaika has no upgraded flag and **none of the default or upgraded dispatchers has a Zaika branch**. The generic wrapper is not a ray, damage, cooldown or mana expenditure for this item. There is no source Zaika cooldown, mana cost, secondary activation, target predicate, spawned entity, projectile, loot-on-hit, damage type, terrain effect, or recipe. No craftusable/transformable flag on the canonical item.

Exact `zaika` search across D found only the two selector tables, custom-data list and TMT metadata registry (`drops/tmt/set_storage_names.mcfunction:2691?2694`, `set_storage_nbttags.mcfunction:1048`). TMT metadata does not add an ability and is deferred. No matching Zaika/translated-name Java handler exists in old Paper `plugins/luckywars/src/main/java` or Ignite `mods/luckywarsignite/src` Java. Therefore no unregistered class is being promoted to live behavior. Generic Paper magicwand occurrences are unrelated producers/TMT metadata, not a consumer for Zaika.

Passive pool predicates have **no MAG, TECH, artifact or armor suppression gate**, and no time/countdown. Source-preserving default is to leave the inventory loot effect independent of jammers. Generic attempted-use suppression is behaviorally irrelevant here because no Zaika branch executes. Do not invent a permanent magic aura, periodic poller or spell service.

**Chef: no direct or indirect erasure edge exists in this item chain.** Filtering acquisition of some unrelated outcomes does not execute their powers. No item-specific Chef ruling is necessary unless an author requests a new active power. If such a new power includes instant erasure, obtain that explicit author ruling before exposure.

## Native status and compact integration plan

Native canonical definition, authored presentation and `loliluckyblock` reference already exist. Native Neko tables already translate old NBT into inventory/offhand/equipment item predicates. `src/main/java/me/skywars2/game/LuckyBlockChecks.java:180?196` contains native Neko branch coverage at inventory slot9, offhand and chest equipment; its `rules` check at214 explicitly reports these branches. This audit did not execute those tests and does not claim current artifact qualification.

Native active Java has no Zaika consumer, which is consistent with this source finding. `agent_notes/ADVANCED_SKILLS_PORT_2026_11_04.md:78?82` records prior author status inquiry, existing singleton/presentation/loot marker, no ready leaf. Exact text searches of `AGENTS.md`, pending/jammer/night question documents and ANSWER/AUTHOR documents found no earlier ruling adding a Zaika active spell or changing its passive suppression policy.

Small closed follow-up batch: (1) verify source-vs-native Loli negative gates retain inventory/offhand/armor semantics and all twelve branches; (2) verify the canonical leaf is reachable through the intended ready catalog policy without adding input logic; (3) exercise actual lucky-block loot contexts with canonical carried Zaika in storage/mainhand/offhand/equipment, no-Zaika and luck-key controls, ranked/gamemode/final-luck score controls, actual MAG/TECH/strong artifact controls; (4) verify exact five full-set outputs and source presentation. Reuse existing native item predicates and LuckyBlockChecks. No arbitrary-function interpreter, global scans, new timers, or resource-pack patch is needed.

Unresolved exposure: repository ready policy may still require qualification before catalog publication. Until verified, keep current availability unchanged and describe Zaika as a passive token, not an unported active wand. No need to expose downstream spells/traps simply to implement this selector.

## Questions for root's separate author queue

1. **Terminology / intended scope:** prior status inquiry was recorded as ?active item not yet?; source actually defines passive inventory loot filtering only. Is the intended request to finish/qualify that original passive token, or is a new active phone ability desired? Suggested interim: preserve original passive behavior, no invented active spell and no new exposure before existing ready-policy qualification. This question is not a claim that source needs author redesign.
2. **Only if desired to change source:** source Loli logic filters twelve specific entries, while Neko alone changes to five full sets; it does not grant universal trap immunity or make Loli drop sets only. Is a broader change intended? Suggested interim: keep all original gates and text verbatim. Do not silently fix lore by changing gameplay.

No chat question was asked. Root owns `NIGHT_QUESTIONS_2026_11_04.md`; this report did not edit it.
