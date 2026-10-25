# Thirteen consumed Erecto, Fireblaze and Stonekivi grimoires

## Author rule (user)

> ВСЕ гримуары это просто одноразовые жезлы (совсем у немногих есть минимальные различия)

Use the corresponding wand/mode producer; do not duplicate projectile physics,
targeting, effects or terrain algorithms for the consumed item.

## Source and exposed leaves

Read-only source: `LBC/data/lbc/loot_table/spells_library.json`,
`function/items/libspells/select.mcfunction`, and selected helpers below,
relative to `function/workingitems/awaken_tuntija_wands/`.

| ID | Native item / deterministic `lbc:ported/` leaf | Helper |
| --- | --- | --- |
| 216 | erecto_grimoire | erecto/click_upgraded_2_mode_default |
| 217 | erecto_ray_grimoire | erecto/click_upgraded_2_mode_ray |
| 218 | erecto_sniper_grimoire | erecto/click_upgraded_2_mode_sniper_lib |
| 219 | erecto_sphere_grimoire | erecto/click_upgraded_2_mode_sphere |
| 220 | fireblaze_grimoire | fireblaze/click2 |
| 221 | fireblaze_kiuas_grimoire | fireblaze/click2_upgraded_mode_kiuas |
| 222 | fireblaze_mega_grimoire | fireblaze/click2_upgraded_mode_mega |
| 223 | fireblaze_pillars_grimoire | fireblaze/click2_upgraded_mode_pillars |
| 224 | fireblaze_world_grimoire | fireblaze/click2_upgraded_mode_world |
| 233 | stonekivi_grimoire | stonekivi/loop |
| 234 | stonekivi_earth_grimoire | stonekivi/loop_earth |
| 235 | stonekivi_sun_grimoire | stonekivi/loop_sun |
| 236 | stonekivi_comet_grimoire | stonekivi/comet |

The importer retains every modifier/component, stack99, metadata flags/mana,
exact name/lore styling, blank row, model, tier and red Legendary/adjective
rows; only native item IDs and obsolete schema field names differ. Current
26.3(1) author RP keys/models/textures are checked read-only for RU/EN/RPR.
Native `/give` defaults match narrow loot. Broad/random library remains gated.

## Shared behavior

Existing consumed admission guards apply before actual-hand consumption.
Both hands and Shift cast the selected spell without mana admission/debit,
mode switching, Academy trial or wand-cast bonus hooks. Default and Academy
wand admission and pricing remain in their original paid wrapper.

Erecto shares one producer for central/side volley, cumulative40-tick ray,
contact sphere and sniper discharge. The source library sniper is deliberately
distinct from the paid wrapper: no3900 extra mana debit and no look-down
aim-assist/refund branch. It still emits the same source sniper trace, one-second
zero-movement windup, recoil, sound and flash. An empty trial stack prevents
consumed central/side arcs or Stonekivi work from satisfying a wand trial.

Fireblaze shares ordinary30-block and Mega100-block volleys and global World,
Pillars and Kiuas producers. Random directions, native projectile types, owner/
epoch and terrain work attribution remain in the native engine. Pillars/Kiuas
keep infinite Fire Resistance. No new budget, timing or projectile behavior is
introduced by the consumed wrapper.

Stonekivi shares its ordinary layered, Earth, Sun/Magmify and Comet contact
work. The actual used mode is passed directly instead of manufacturing a wand
stack. Existing template preparation/readiness, per-tick terrain budget,
immediate streaming, attribution and cancellation remain shared. A readiness
rejection preserves the consumed item.

## Qualification

Full offline Gradle build succeeded. Build `d49c7381` passed16 recorded groups
in two actual mode1 matches (96.51s). `shaping-grimoires` covers all13 imported/
default presentations, both actual hands/Shift, final-book counts, zero/negative
unchanged mana, owned volley/ray/sphere/sniper, downward library sniper without
assist/refund, Fireblaze native types/buffs/work, four Stonekivi work sequences,
ray accumulation after book consumption, parallel use and nonconsuming guards.
A separate actual match-stop probe retains common centers, rays and terrain
jobs. Full Erecto, Fireblaze and Stonekivi native suites plus prior ordinary
elemental grimoires run together. Client pixels/audio and peak TPS are not
directly measured by these native fixtures.


Exact source/class/resource closure:376 Java sources,1062 classes and2890
resources. Main baseline6fb1a606 remained unchanged through qualification;
installer18888 then installedd49c7381 with6fb1a606 rollback and no restart.
Protected user files/deletions and read-only author RP were preserved. The
isolated server stopped and restored its fixture state. Only the known fake-bot
chunk packet-context diagnostics occurred; no check failure or other ERROR.

Evidence: `build/verification/shaping-grimoires-final/qualification.json`,
immutable installer input `qualification-d49c7381.json`, `main-install.json`,
shaping-grimoires log/results/build log. Full SHA256:
`d49c738151732cc813b49cd67527c6c5489a1d377f87ac930e80f3c9dab55089`.
