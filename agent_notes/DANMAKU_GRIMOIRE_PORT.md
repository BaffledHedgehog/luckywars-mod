# Consumed Danmaku grimoire159

Source: [grimoire trace](DANMAKU_GRIMOIRES_SOURCE.md),
[next-family review](NEXT_PORT_SOURCE_2026_10_25.md).

## Implemented scope

The native `skywars2:danmaku_grimoire` is available through the narrow deterministic
`lbc:ported/danmaku_grimoire` leaf. It imports exactly the direct spellid159
entry from `spells_library`, changing only the item ID after schema conversion.
The broad240-entry library and random grimoire routes remain gated; spellid160
is Swarogia, not another Danmaku grimoire. No Academy grimoire is invented.
All original name/lore components, C tier, model, flags and maximum stack99
remain. Existing author RP model/textures/RU/EN/RPR keys are read only and
verified by `tools/import_lbc_danmaku_grimoire.py`; no RP fragment is required.

An admitted normal use consumes one (creative follows native consumption),
awards its use stat, and plays the native snowball throw sound. It creates no
throwaway snowball entity. Stored `mana:1200` is source metadata, not a mana
price; zero/negative mana does not prevent use or change its balance.
Normal use adds20 ordinary-pattern ticks to a captured-hand consumed queue.
The queue continues with no staff held. Repeated casts accumulate; both hands
can activate through the existing parallel-use dispatcher, with40 main-hand
and35 offhand cards per20ticks. The queues retain each actual hand instead of
overwriting earlier casts' hand. Active remaining duration uses the existing
Danmaku actionbar row; staff-paid and consumed provenance remain distinct so
putting a staff away cannot suppress an already consumed grimoire cast.

The author confirmed (25 October 2026): Shift use never consumes a grimoire,
including below50 charge. Only normal casting consumes the book. From50 charge
Shift invokes the existing ordinary bomb and clears the charge; the same final
book remains reusable after recharging. Below50 it preserves both book and
charge. Grimoire bombs never satisfy/promote a forged staff trial. The old1000
mana refund is removed by explicit author instruction, replacing the earlier
assumption. Existing native input feedback/stat/sound behavior is retained.
No mana cost/admission was added. Input guards reject without consuming for
observer/dead/offhand-disabled/blocked/time-stop/antimagic states. Accepted
consumed queues pause under time stop/antimagic, and clear with owner/match
lifecycle just like the existing Danmaku engine.

## Verification contract

`danmaku-grimoire` checks actual native game-mode item use, original components,
counts/stats, no snowball entity, zero/negative mana, final-stack continuation,
main/off40/35 patterns (8/12 homing), accumulated casts and simultaneous75-card
hands, suppression/roles, bombs49/50/1500 and no Academy readiness.
`danmaku-grimoire-stop-probe` retains consumed cards/remaining queue before an
actual match stop; `danmaku-grimoire-cleanup` verifies epoch/actors/queue cleanup.
Joint build and runtime qualification is recorded in the combined sound report.

## Joint parent qualification

Buildccb40b6c passed46 recorded results in two actual mode1 matches (88.93s).
Native packet captures cover the corrected Ender/Exahoming/Tanner/Acid Corruption/
Barrier/Clock/Helicopter cues, with the prior SLR/Magnum/spawn regressions.
Actual grimoire use, ordinary/Academy Danmaku, Ender/Wiki, defensive arcanums,
Corruption, helicopter encounter, Ultraponos, Tanner, Time Stop/contacts and
Academy acquisition passed, including three actual match-stop cleanup families.
Exact355 sources/988 classes/2819 resources match the tested artifact. All271
ported leaves, including the new grimoire JSON, are present in the packaged JAR.
The only error lines are the known Polymer packet-context diagnostic while
creating fake bots; no new runtime errors occurred. Real client audio and every
conditional mode's runtime are not independently observed.

Ready: `build/verification/skywars2-all-item-sounds-ccb40b6c.jar`.
Evidence: `build/verification/all-item-sounds-final/qualification.json` and
`tools/smoke_all_item_sounds.py`. The isolated server stopped, settings/profiles/
scoreboard/JAR restored and fixture locks released. Five pre-existing user
file hashes and applied-template deletions are preserved. READY ONLY: main
remains04396ca4; no installation/stop/restart or author RP/template change.
Obsolete Hole Erosion54941d0e archive was removed after qualification; previous
Weapon Sound593a39df and main fallbacks remain. Cumulative cleanup290 JARs/
10,231,414,603bytes/9.53GiB. The native packet fixture covers selected source
boundaries; inventory classification is broader than runtime/client playback.

## Author correction qualification

The explicit author follow-up supersedes the old throw-consumption rule:
Shift never consumes a grimoire; only ordinary casts do, and no mana refund.
Build95c3db2f passed14 results in two real mode1 matches (64.26s). Both hands
preserve their final book at49/50/1500 charge, can reuse it for another bomb
after charging, and finally consume it for a normal20-tick cast. Existing
staff/Academy regression and actual consumed-queue/card stop cleanup pass.
Exact355 sources/988 classes/2819 resources match. Ready:
`build/verification/skywars2-grimoire-consumption-95c3db2f.jar`; evidence:
`build/verification/grimoire-consumption-final/qualification.json` and derived
`run.py`. Main04396ca4 remains unchanged; READY ONLY, no install/restart/RP edits.
Earlier46-result sound qualification above is historical; this narrowly
scoped correction reruns the relevant grimoire/staff/stop checks.
