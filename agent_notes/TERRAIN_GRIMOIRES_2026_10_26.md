# Eight consumed Consuming and Duplicating grimoires

## Author rule (user)

> ??? ???????? ??? ?????? ??????????? ????? (?????? ? ???????? ???? ??????????? ????????)

Use the corresponding shared native wand producer. Do not duplicate terrain
algorithms or restore the old marker, template-integrity and teleport
workarounds in a separate grimoire implementation.

## Read-only source and deterministic leaves

Sources: `LBC/data/lbc/loot_table/spells_library.json`,
`function/items/libspells/select.mcfunction`, and helpers below relative to
`function/workingitems/awaken_tuntija_wands/`.

| ID | Native item and `lbc:ported/` leaf | Source helper |
| --- | --- | --- |
| 225 | consuming_grimoire | consuming_wand_upgraded_mode_default |
| 226 | consuming_avarice_grimoire | consuming_wand_upgraded_mode_avarice |
| 227 | consuming_silence_grimoire | consuming_wand_upgraded_mode_silence |
| 228 | consuming_end_grimoire | consuming_wand_upgraded_mode_the_end_pre |
| 229 | duplicating_grimoire | avarice/click2_upgraded_default |
| 230 | duplicating_hole_grimoire | avarice/click2_upgraded_black_hole |
| 231 | duplicating_divide_grimoire | avarice/click2_upgraded_divide |
| 232 | duplicating_expand_grimoire | avarice/click2_upgraded_expand |

The exact source name/lore, blank row, formatting, red Legendary and adjective
rows, tier/model, stack99 and custom metadata are retained by the importer.
Only native item IDs and obsolete schema names differ. Native default stacks
are checked against the imported components. RU/EN/RPR keys and item models
are audited against the current author RP read-only. The prior64 imported
leaves are unchanged. Broad/random library tables stay gated.

## Shared implementation

Extracted `castMode` methods are called by both paid wand wrappers and books.
The paid wrappers retain their original mana, mode-switch, Academy and cast
bonus behavior. Books use the existing consumed admission/hand guards, pass a
fixed mode and consume one real book without mana admission/debit, Academy
trial, mode switch or wand-cast bonus. Consuming preparation/readiness rejection
preserves the book.

Consuming shares its normal ray and owned delayed partitions, dimension-wide
Avarice gather, radius48 native Silence disposal and five-ray End producer.
The End starts eight blocks ahead and keeps the source yaw fan. Deferred work,
source caster buffs, entity filters, attribution, streaming and cancellation
remain shared. Earlier user decisions remain in the common engine: Silence
discards ordinary entities without death/drop, settles falling entities and
clears blocks, protects service entities and retains god-hedgehog retaliation;
Avarice gathers ordinary entities from the whole loaded dimension.

Duplicating shares normal shuffle, expanded shuffle, contact divide and shader
black hole. Mode mapping is explicit because source library order differs from
native wand mode order: hole4, divide3, expand2. Moving centers retain source
forward offsets7/8/14, lifetimes200/300/100 and captured author/epoch. No extra
moving carriers are introduced for Expand. Clone/container semantics, loaded
cross-dimension reads, immediate streaming, noisy radial erosion, input-aware
velocity pull and shader size use the already migrated common engine. No new
terrain budget or projectile pacing is introduced by the consumed wrapper.

## Verification

Full offline Gradle build succeeded. Build `b4aa411f` passed24 recorded groups
in two actual mode1 matches (62.57s). `terrain-grimoires` uses actual item input
for all8 books in both hands and with Shift at zero/negative mana, verifies
final-stack consumption and one use stat, imported/default components, native
mode/owner/epoch/launch position/lifetime, merged terrain work, normal partitions,
Silence caster buffs, common shader, independent repeated centers, parallel
families and nonconsuming guards. The input fixture cancels its queued terrain
work before execution; full Consuming/Duplicating native regressions validate
actual world changes, no-drop disposal and container/cross-dimension behavior.
A separate retained fixture verifies actual match-stop cleanup of terrain work,
partitions, centers and shader. Native fixtures do not directly observe client
rendering/audio playback or peak TPS.


Exact artifact closure:378 Java sources,1068 classes and2909 resources. Main
baseline753bcbe3 stayed unchanged through qualification. Installer7184 then
installedb4aa411f with753bcbe3 rollback and no restart. Protected user files/
deletions and author RP are unchanged. The isolated server stopped and restored
its settings/profile/scoreboard/JAR fixtures. All checks passed in the complete
first run. Only the known fake-bot chunk packet-context diagnostics occurred.

Evidence: `build/verification/terrain-grimoires-final/qualification.json`,
immutable installer input `qualification-b4aa411f.json`, `main-install.json`,
terrain-grimoires build log, runtime log and results JSON. Full SHA256:
`b4aa411fd07b59d87cdf8ea6962e87d9995b39f0c36961566653a1e7985088d6`.
