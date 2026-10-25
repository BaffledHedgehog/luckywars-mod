# Twelve consumed Homing, Psychoblind and Boltbolt grimoires

## Author rule (user)

> Р’РЎР• РіСЂРёРјСѓР°СЂС‹ СЌС‚Рѕ РїСЂРѕСЃС‚Рѕ РѕРґРЅРѕСЂР°Р·РѕРІС‹Рµ Р¶РµР·Р»С‹ (СЃРѕРІСЃРµРј Сѓ РЅРµРјРЅРѕРіРёС… РµСЃС‚СЊ РјРёРЅРёРјР°Р»СЊРЅС‹Рµ СЂР°Р·Р»РёС‡РёСЏ)

The consumed wrappers call the same native spell/mode producers as paid wands.
They keep only direct library differences: consume the actual book, no mana
admission/debit or mode switching, no Academy trial or wand-cast bonus hook.

## Source and exposed scope

Read-only source: `LBC/data/lbc/loot_table/spells_library.json`,
`function/items/libspells/select.mcfunction` and the selected helpers below.
Helpers are relative to `function/workingitems/awaken_tuntija_wands/`.

| ID | Native item and deterministic `lbc:ported/` leaf | Source helper |
| --- | --- | --- |
| 204 | exahoming_grimoire | exahoming_click_lib |
| 205 | exahoming_curse_grimoire | exahoming_click_upgraded_mode_curse |
| 206 | exahoming_destiny_grimoire | exahoming_click_upgraded_mode_destiny |
| 207 | exahoming_sisyphus_grimoire | exahoming_click_upgraded_mode_sisyfus |
| 208 | psychoblind_grimoire | psychoblind/click_upgraded_mode_default |
| 209 | psychoblind_invisible_grimoire | psychoblind/click_upgraded_mode_invisible_charge |
| 210 | psychoblind_ray_grimoire | psychoblind/click_upgraded_mode_mindblowing_ray |
| 211 | psychoblind_spray_grimoire | psychoblind/click_upgraded_mode_spray |
| 212 | boltbolt_grimoire | boltbolt/click_lib |
| 213 | boltbolt_meteor_grimoire | boltbolt/click_upgraded_mode_snow_meteor |
| 214 | boltbolt_watering_grimoire | boltbolt/click_upgraded_mode_watering |
| 215 | boltbolt_racer_grimoire | boltbolt/click_upgraded_mode_wind_racer |

The importer preserves all source modifiers and component formatting, models,
white nonitalic names, gold lore, blank row, stack99, metadata mana2000 and
custom flags. All12 use the source red Legendary rarity row; optional Smart,
Unreal, Powerful, Enhanced and Universal adjectives retain their own colors.
The current26.3(1) author RP is audited read-only for RU/EN/RPR translation
keys and recursive item/model/texture dependencies. Native `/give` defaults
match narrow loot output. Broad/random library tables remain gated.

## Shared implementation

Homing's existing launch producer is extracted without changing paid-wand
admission or callbacks. Ordinary grimoire keeps its firework-shoot cue; other
direct modes retain their own payload cues. Both hands and Shift consume one
book without mana admission/debit, so zero/negative balances remain unchanged.
Source metadata is not interpreted as a cast price. Async resident ray search,
captured owner/epoch, targeting and native projectile ownership are shared.
Normal produces ten homing fireballs; Sisyphus one power100 fireball; Curse
lasts1200ticks; Destiny captures a600-tick moving center. Prior author ownership,
infinite resident ray and timing decisions remain in the shared engine.

Psychoblind's common producer keeps four ordinary, three invisible and one
sprayer charge with100-tick lifetime. Source grimoire ordering differs from
wand mode numbering: invisible is wand mode4, spray mode3. No rebalance/count
change was needed. Scream Ray adds40 ticks, accumulates repeat casts and remains
active after the final book leaves the hand. Its tick, effects and rendering
are the existing native wand service.

Boltbolt's common producer retains five wandering bolts,200-tick lifetime and
40-tick strike interval, the native snow meteor and its visuals/impact, five
watering centers plus240s Water Breathing and Strength III, and the native
owned speed2 wind racer carrier. Grimoire rays are anchored at the eyes for
either consumed hand, as the library selector specifies. The meteor readiness
guard rejects without consumption. Existing wand hand offsets, sound, mana,
mode switching and Academy handling remain in the paid wrapper.

## Qualification

Build6fb1a606 passed16 groups in two actual mode1 matches (50.31s). Exact375
sources/1059 classes/2877 resources match the tested artifact, including all12
new narrow leaves. `advanced-grimoires` exercises all12 source
loot/default components, actual final-book use from both hands and Shift,
unchanged zero/negative mana, source counts/flags, async completion, actual
Curse target, captured owners/epochs, snow meteor, watering buffs, racer mount,
parallel mixed hands, cumulative Scream Ray continuation and nonconsuming
guards. The independent stop probe retains actors/centers/queue through an
actual match stop. Prior ordinary elemental, full Exahoming, Psycho and
Boltbolt native suites run together. The old Psycho regression is aligned with the existing author rule: allied
mobs may receive projectile/ray effects, while service NPC and antimagic
targets stay excluded. No gameplay filter was changed for this correction.
No client-frame/audio or peak-TPS claim.

The combined runner observes an actual advancing native game clock between
independent suites. Otherwise two Academy promotion fixtures can legitimately
share the same tick and trigger the existing duplicate-click suppression.
No Academy cooldown/deduplication code is changed.

Evidence: `build/verification/advanced-grimoires-final/qualification.json`,
`main-install.json` and `tools/smoke_advanced_grimoires.py`. The isolated server
stopped normally, settings/profiles/scoreboard/JAR were restored and fixture
locks released. Only the known fake-bot Polymer packet-context diagnostic
appeared. User hashes/deletions and author RP remain unchanged. Installer21840
installed6fb1a606 over462ab2c6 with462ab2c6 rollback and no restart.
