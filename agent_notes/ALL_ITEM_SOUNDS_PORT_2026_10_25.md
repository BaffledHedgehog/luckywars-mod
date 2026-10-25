# All-item sound audit and Danmaku grimoire

Author request: check sound effects across all items and continue migration.
The ready-item source inventory covers271 deterministic/narrow ported loot
leaves mapped to101 implementation owners, with no unclassified leaves.
[Active families](ALL_ITEM_WAND_SOUND_AUDIT_2026_10_25.md),
[drops/passives](ALL_ITEM_DROP_SOUND_AUDIT_2026_10_25.md),
[consumed grimoire](DANMAKU_GRIMOIRE_PORT.md).

## Sound changes

- Ordinary/Academy Ender broadcasts retain source selector16 independently of
  volume2. The private caster sound remains unchanged.
- Physical projectile conversion by Tannerkivi uses MASTER and the projectile's
  exact coordinates instead of BLOCKS at a rounded block center.
- Academy Acid Corruption delayed-death feedback sends exactly four cave
  sounds privately to the affected player rather than nearby bystanders.
- Absolute Barrier plays the original beacon activation once per newly paid
  hand latch; steady upkeep does not replay it or add an activation charge.
- Stand clocks use source volume1/pitch1 and minimum0.5 for far listeners.
  Native sparse positions now restore the original trail dust and impact
  sounds on resume; no marker entities, arbitrary TTL/cap or chunk loads.
- Ordinary Exahoming plays its source firework shoot cue on accepted paid use,
  including a miss. Cached26.3 SoundEvents registers this exact event; the
  initial assumption that it was a retired event was disproved and discarded.
- Helicopter boss combat cues restore their source volume, pitch, audience,
  minimum volume and shot offsets. Shot selector center is independent of its
  local sound offset. Match cleanup stops both encounter audio events.
- Academy-token and Ultraponos sounds retain exact token coordinates.

The inventory includes1,656 legacy playsound sites and285 native sound sites;
262 literal native vanilla IDs were checked against1,983 cached26.3 registry
IDs with zero unknown IDs. Relevant concrete native custom events and their
OGG files exist in the author's read-only RP. Two missing legacy-only custom
keys belong to unported menu/book paths, not enabled native item playback.
All271 leaves are classified; this is not an assertion that every conditional
mode has been played through or heard on a real client.

## Port and regression

Direct spellid159 Danmaku grimoire now has a native consumable and only one
scoped loot leaf. Authored components/name/lore/stack99 are preserved. It needs
no mana; mana1200 remains metadata. Captured ordinary hand queues survive
consumption/putting other items in hand, accumulate, and support simultaneous
hands through the existing dispatcher. Free bomb uses the shared charge from50,
without the legacy accidental1000 refund; grimoire cannot promote a staff.
Author follow-up: Shift bombs never consume the book (even below50); only
normal casts consume it. This replaces the first implementation's source throw
consumption rule.
[Detailed behavior and author-assumption record](DANMAKU_GRIMOIRE_PORT.md).

The joint Ender regression exposed an older ordering bug: the Greed rejection
branch cleared accumulated RTP timers before their explicit pause branch.
Greed now pauses those existing timers before effect-rejection cleanup; new
external effects remain rejected. Native Time Stop and equipped cleansing
behavior otherwise retain their existing order. This matches the previously
accepted pause/resume requirement rather than weakening Greed immunity.

## Verification

Source inventory and RP/registry receipts are under
`build/verification/all-item-sounds/`.

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


The fixture's first run also caught a missing build.gradle whitelist entry for
the new grimoire JSON. The explicit ready leaf is now included; the final
qualifier checks all271 ported leaves actually exist in the tested artifact.
Earlier diagnostic attempts remain under ignored evidence; they are not final
passing qualification. Main-server JAR is not installed/restarted during this
work, following the user's current READY ONLY preference.
