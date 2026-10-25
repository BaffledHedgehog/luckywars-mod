# Eleven consumed awakened-acid and Exasperato grimoires

## Author rule (user)

> ??? ???????? ??? ?????? ??????????? ????? (?????? ? ???????? ???? ??????????? ????????)

Books invoke the corresponding shared wand producer. Existing projectile
physics, target filters, hurt-cooldown policy, effects and lifecycle remain in
that producer. Source carrier snowballs/markers and instant creepers are not
reintroduced.

## Read-only sources

`LBC/data/lbc/loot_table/spells_library.json`,
`function/items/libspells/select.mcfunction`, and the following helpers relative
to `function/workingitems/awaken_tuntija_wands/`.

| ID | Native item and deterministic `lbc:ported/` leaf | Source helper |
| --- | --- | --- |
| 188 | awakened_acid_grimoire | acidacid/click2 |
| 190 | awakened_acid_caust_grimoire | acidacid/click_upgraded_mode_caust_lib |
| 191 | awakened_acid_caust_burning_grimoire | acidacid/click_upgraded_mode_caust_lib_1 |
| 192 | awakened_acid_gatling_grimoire | acidacid/click_upgraded_mode_gatling_lib |
| 193 | awakened_acid_gatling_burning_grimoire | acidacid/click_upgraded_mode_gatling_lib_1 |
| 194 | awakened_acid_rifle_grimoire | acidacid/click_upgraded_mode_rifle |
| 199 | exasperato_grimoire | exasperato/click2 |
| 200 | exasperato_triple_grimoire | exasperato/click2_upgraded_mode_default |
| 201 | exasperato_chaos_grimoire | exasperato/click2_upgraded_mode_chaos_area |
| 202 | exasperato_ray_grimoire | exasperato/click2_upgraded_mode_electro_ray |
| 203 | exasperato_nuke_grimoire | exasperato/click2_upgraded_mode_nuke |

The importer preserves exact source name/lore/blank rows/component formatting,
red Legendary rarity and adjective colors, tier/model, stack99 and metadata
flags/mana. Native item IDs and obsolete source schema names are the only
import changes. All RU/EN/RPR keys and item models are audited against the
current author RP read-only. Existing53 imported leaves are unchanged.
The disabled source selector189 is not added. Broad/random library stays gated.

## Native behavior

Both hands and Shift cast the book's fixed spell and consume one real book
without a mana gate/debit, mode switch, Academy trial or wand-cast bonus.
Existing common gameplay/antimagic/time-stop/offhand guards preserve a book
when rejecting its input. Source metadata mana belongs to library crafting.

`LbcAwakenedAcid.castMode` is extracted from the paid wand wrapper. The ordinary
500-tick great-acid charge, 500-tick caustic charge and eight 200-tick burst
charges use the same native cloud projectile producer. Ignition is explicit
for the two burning books. The rifle adds10 releases to the player's existing
counter; it uses the same five-tick cadence, current aim, suppression pause,
ownership and cleanup as the wand. The consumed rifle carries an empty trial/
ignition stack and continues after its last book is consumed. Two books add20.
The existing once-per-target-per-tick burning damage rule remains shared.

Exasperato directly uses existing ordinary/triple/chaos/electro-ray/nuke
producers, including native instant blasts, loaded-chunk aiming, +500 nuke
spawn and the common nuclear shader lifecycle. The ordinary producer now accepts
an explicit Academy-trial flag: books passfalse, while paid wands preserve
existing admission. This prevents an offhand consumed book from inheriting an
unrelated main-hand trial. No damage, trajectory, mode timing or budget change
is introduced for paid wands.

The ordinary Exasperato regression also exposed an existing hidden-section
lifecycle bug: a newly added charge can be discarded before any unload event
is dispatched, leaving a removed object in the tracked set. The tick and live
count/snapshot boundaries now prune removed entries explicitly. This preserves
collision/payload behavior and releases stale actor references without relying
on chunk tracking callbacks.

## Verification

Full offline Gradle build succeeded. Build `753bcbe3` passed18 recorded groups
in two actual mode1 matches (54.87s). `corrosive-grimoires` exercises all11 exact
import/default presentations, both hands and Shift at zero/negative mana,
actual final-stack consumption/stat count, acid variant/ignition/lifetime,
owned explosive payload counts, native chaos without carrier TNT/creepers,
nuke ray contact, accumulated continuing rifle, mixed parallel hands, unrelated
main-hand trial exclusion and rejecting guards. A separate actual-stop probe
retains the rifle and native acid/explosive actors. Prior ordinary and shaping
grimoires plus full awakened-acid and ordinary/Academy Exasperato suites run
jointly. Native fixtures do not directly observe client rendering/audio
playback or peak TPS.


Exact artifact closure:377 Java sources,1065 classes and2901 resources. Main
baseline d49c7381 stayed unchanged through qualification. Installer12628 then
installed753bcbe3 with d49c7381 rollback and no restart. Protected user files/
deletions and author RP are unchanged. The isolated server stopped and restored
its settings/profile/scoreboard/JAR fixtures. The only final ERROR lines are the
known fake-bot chunk packet-context diagnostics; all gameplay checks passed.

An initial fixture-only failure was corrected by collecting native lightning
before discarding it, rather than mutating the entity iterable during traversal.
The Exasperato regression failure reproduced independently and led to the
hidden-section actor cleanup fix above. The final complete run passes both
matches, including that original collision regression.

Evidence: `build/verification/corrosive-grimoires-final/qualification.json`,
immutable installer input `qualification-753bcbe3.json`, `main-install.json`,
corrosive-grimoires build log, runtime log and results JSON. Full SHA256:
`753bcbe30fa6c19aa2f51242319bc014042825f83f8dc7ead6c6fac4b262ef02`.
