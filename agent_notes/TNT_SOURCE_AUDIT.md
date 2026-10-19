# kit15 Dynamite: source contract audit

Scope: read-only comparison of old server datapack/Paper/ Ignite and current Fabric project. No production code changed; no server run. `s` on NBT `fuse` is a Short tag suffix and its numeric value is ticks: e.g. `1000s` = 1000 ticks = 50 s, `2s` = 2 ticks = 0.1 s.

## Source behavior / transfer contract

- Kit 15 resolves to `lbc:kit/dynamite` (gate: datapack `swrg_kit_integration/game/kit_select/select.mcfunction`, score `lbc.kit=15`). Loot gives 8 snowballs, model `lbc:dynamite`, translate name/lore `dynamite` / `dynamite.lore`, `italic:false`, custom data `{dynamite:1,special:1}`. Preserve all these component details and existing translation keys.
- Snowball dispatcher calls `lbc:projectiles/specials` for `custom_data.special:1`. Its dynamite branch summons TNT at local `^ ^1 ^`, tagged `edited6`, `can_deleted`, `groundbam`, `dynamite`, with fuse `1000s` (= 50 s). Then it copies snowball Motion components into TNT via three `store result ... double 0.0001 ... data get ... 10000` operations. This ordering means launch velocity is assigned after summon; do not replace with zero/default velocity. This branch does not set TNT `Owner` or damage source.
- `projectilestick_tnt` first removes old `daleko`, tags fuse-zero TNT as `daleko`, then on-ground `groundbam` calls `other/groundbam`, and finally any remaining `dynamite` calls `other/dynamite`. Thus contact processing precedes the stop-motion fuse update on a tick.
- `other/dynamite` processes Motion X, then Y, then Z. For each component it computes integer score `Motion[i] * 100000`; if that rounds to zero, it writes `fuse:1s` (one tick) and removes `dynamite`. It does not test sign, speed magnitude beyond integer quantization, or require all 3 axes to be zero. So a near-zero component on any axis ends the special fuse early.
- `other/groundbam` unconditionally overwrites fuse with `2s` (2 ticks), then optional tag-specific effects (`evoker`, `fire`, `electro`, `strong`, `no_explode`) and removes `groundbam`. Dynamite branch itself only sets `groundbam`; no fire/electro/strong/no_explode tags. The delayed fuse is therefore 2 ticks on ground contact. Normal vanilla TNT explosion follows: default power/radius behavior, entity damage/knockback and block effects; no custom radius, damage, sound, particles, owner, or source is assigned by these helpers. Vanilla TNT behavior supplies its usual fuse hiss/explosion presentation. Do not invent custom damage or attribution.
- `can_deleted` marks the projectile for cleanup policy, but `end_resets.mcfunction` removes all entities in the_end/nether/imprinted; it does not establish a special dynamite detonation. Match-level cleanup and deleted-projectile policy still need a direct counterpart in the Fabric lifecycle if this item can survive round end.

## Runtime variants and caveats

- The active datapack source above is exact evidence of intended command behavior. Paper `SwrgNativeService` maps kit 15 to the same loot table and invokes `lbc:projectilestick_tnt`; its function runtime therefore uses the datapack lifecycle. Paper also has unrelated TNT jacket/charge TNT paths; do not conflate them with kit15.
- Ignite source contains only Bedwars mixins/hooks; no Dynamite/TNT lifecycle implementation or kit15-specific override found. This is an absence-of-source finding, not evidence from a live run.
- Several old datapack copies/build artifacts exist. Prefer `world/datapacksdisabled/LBC` as the audited source, and check packaged datapack equality if runtime provenance matters.

## New mod reuse

- The new project has no found generic Primed TNT lifecycle helper for custom `fuse`, `groundbam`, or owner/source handling. `MatchEvents.dropTnt()` is a separate recurring match event (`summon minecraft:tnt ... {fuse:100s}`), not a reusable kit weapon contract.
- Reuse the existing match-level entity/lifecycle boundary and `GameController.command(...)` only where command-backed entities are already supported; do not treat `MatchEvents.dropTnt()` as the Dynamite implementation. Preserve item presentation through the current loadout/translation infrastructure.

## Source links

- [Kit loot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/dynamite.json)
- [Kit selection gate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/kit_select/select.mcfunction)
- [Snowball dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/snowballs.mcfunction)
- [Special projectiles](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials.mcfunction)
- [TNT tick dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectilestick_tnt.mcfunction)
- [Dynamite stop-motion helper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/dynamite.mcfunction)
- [Ground TNT helper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbam.mcfunction)
- [End cleanup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/end_resets.mcfunction)
- [Paper runtime bridge](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java)
- [Ignite source tree](E:/servers_mew/cringe1_21_4_skywars/mods/luckywarsignite/src/main/java/me/luckywars/bedwars)
- [New match event TNT helper](../src/main/java/me/skywars2/game/MatchEvents.java)

# Gain notification restoration audit (2026-10-11)

## Findings and compact design

The importer strips legacy function rewards from all 357 native advancement JSONs; its report helper accumulates the exact advancement ID → gain mapping before deleting `rewards.function`. Four requested source categories account for 108 entries: kit 25, skill 32, levelup 29, trinket 22. Across the complete tree, 122 function rewards are removed: the other 14 are default_kit 3, default_skill 2, default_kill_effect 2, death_effect 1, skill_acidman 5, challenge 1. Therefore adding a generic callback without a four-category ID map would miss the separate categories and incorrectly classify other removed reward functions.

Use the native `PlayerAdvancements.award(AdvancementHolder, criterion)` lifecycle already intercepted by `ObserverAdvancementMixin`: inject after successful award (`RETURN`, only `true`), read `advancement.id()`, and dispatch that exact ID through a prebuilt map for the wanted 108 IDs. Show the matching subtitle exactly once per newly completed award. The existing mixin HEAD guard already cancels awards for `game.isObserver(player)`; retain that guard, and also ensure callback dispatch is only for successful award, never failed/duplicate criterion awards. The map can be produced from the same reviewed source rewards that populate `GAIN_PREFIX`, using the importer’s existing `--print-id-map` logic as the reference; runtime should not rescan resource files.

Each of the four source functions emits exactly two commands: `title @s title " "`, then `title @s subtitle {"translate":"new_kit"|"new_skill"|"new_levelup"|"new_trinket"}`. There is no explicit timing command, sound, or additional reward logic in these functions. Thus preserve the blank title followed by a client-translatable subtitle and do not add a sound. Native advancement display/toast, icon, title, description, frame, and criteria remain in the imported advancement JSON; this callback restores only the stripped subtitle notification.

Reuse `GameController.command(...)` if dispatching vanilla `title` commands: it runs through the native command dispatcher in the match level. Or use existing packet pattern in `GameController`: send `ClientboundSetTitleTextPacket(Component.literal(" "))` and `ClientboundSetSubtitleTextPacket(Component.translatable(key))`; the controller already sends these packets for phase subtitles. Prefer translatable Components so client language is respected. No new class is needed: mapping/dispatch belongs in the existing advancement mixin callback or an existing controller entry point; the existing observer gate stays authoritative.

## Exact IDs by source reward function

IDs below are full `lbc:true_advancements/` plus the listed path. These are the IDs from old datapack source `true_advancements/**/*.json` `rewards.function`, not inferred from item names.

### `kit` (25)

- `accesories/`: `devilstomach`, `popit`, `sand_amulet`
- `arcanums/`: `barrier_and_heal`, `kiuaskivi`
- `armor/`: `chaos_full`, `moisei_boots`, `multiplying`, `nano`, `phantom`, `shitty`, `steampunk`, `technomk2`
- `axes/`: `randchanted`
- `bows/`: `firework`
- `hoes/`: `op_hoe`
- `legends/academy/`: `acid_tuntija`, `badwiki`, `blazefire`, `confuse`, `erecto`, `exasperato`, `fartus_nadristailo`, `lightning`, `ruby`

### `skill` (32)

- `accesories/`: `jaakivi`, `magnet`, `mystical_artifact`
- `arcanums/`: `tannerkivi`
- `armor/`: `erosion`, `nyauwu`, `paladin`, `randchanted`, `shitty_full`, `shulker_full`, `techno_full`
- `axes/`: `banhammer`, `hammer`
- `bows/`: `double`, `madoka_bow`, `minigun`
- `hoes/`: `death_skytche`, `sickle`, `socializm`
- `legends/academy/`: `acidacid`, `acidcorrupt`, `big_stone`, `boltbolt`, `consuming`, `danmaku`, `dublicating`, `exahoming`, `fire`, `gaycaster`, `omega_flag`, `psychoblind`, `stone`

### `levelup` (29)

- `accesories/`: `infinite_quiver`, `mana_belt`, `regeneration_belt`, `save_platform`
- `arcanums/`: `babylon_gate`, `barrier`, `black_hole`, `black_hole_2`, `black_hole_max`, `heal`, `neutral`
- `armor/`: `antimagic`, `assasin`, `berserker`, `chaos`, `flesh`, `glass`, `mage`, `shulker`, `techno`, `tnt_jacket`, `traveller`
- `axes/`: `poleaxe`, `tomahawk`
- `bows/`: `railgun`, `railshish`, `randchanted`, `shulker`
- `legends/academy/`: `blood`

### `trinket` (22)

- `bows/`: `blaze`, `ghast`, `penis_gun`
- `legends/academy/`: `acid`, `air`, `bass`, `bedrock`, `blaze`, `blind`, `corruption`, `electro`, `ender_random`, `evoker`, `exoperso`, `falling_stars`, `hedgehog`, `ice_wand`, `neutral`, `polymorph`, `prism`, `psycho`, `snow_wand`

## References

- [Importer and `--print-id-map`](../tools/import_lbc_advancements.py)
- [Observer award guard](../src/main/java/me/skywars2/mixin/ObserverAdvancementMixin.java)
- [Loadout advancement gate checks](../src/main/java/me/skywars2/game/LbcLoadouts.java)
- [Existing native command dispatcher and title packet examples](../src/main/java/me/skywars2/game/GameController.java)
- Old source gain functions: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/{kit,skill,levelup,trinket}.mcfunction`
- Old source advancement reward mappings: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/`

## Native unlock notification implementation

`LbcLoadouts` now loads the generated `lbc_unlock_notifications.json` once at class initialization. `ObserverAdvancementMixin` keeps its existing HEAD observer denial and handles `award` at RETURN only when it returned true; `LbcLoadouts.advancementAwarded` also requires completed progress, refuses observers, and sends the existing blank-title packet plus an unstyled translatable subtitle packet. Duplicate criterion awards and incomplete advancements therefore send nothing. No timer, persistent counter, or extra Java class was added.

`tools/import_lbc_advancements.py --write-unlock-notifications` reads old source advancement JSON from the configured read-only datapack root and generates exact source reward ID mappings. It emits all 122 audited source gain mappings: the four primary families, default kit/skill, default kill effect, death effect, challenge, and the five `skill_acidman` IDs. `skill_acidman` entries require all ten exact prerequisite advancement IDs before showing `new_skill`; if any prerequisite is missing/incomplete, no notification is sent. The source gain functions checked for the simple categories all issue `title @s title " "` followed by the respective `title @s subtitle {"translate":"..."}`; no times or sounds are set. The new public `sourceSubtitle(AdvancementHolder)` returns the mapped translatable subtitle for inspection.

No server or build was run. The importer generation completed and reported: kit 25, skill 32, levelup 29, trinket 22, default_kit 3, default_skill 2, default_kill_effect 2, death_effect 1, challenge 1, skill_acidman 5.
