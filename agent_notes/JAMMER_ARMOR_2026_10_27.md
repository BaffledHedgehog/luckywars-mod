# Approved jammer interactions and audit — 27 October 2026

Author decisions and pending questions are recorded verbatim in
[JAMMER_QUESTIONS.md](JAMMER_QUESTIONS.md). This patch implements the answered
armor group and subsequent decisions 5–11. The three audit notes describe the preceding `e93b059`
source snapshot; their old ungated observations are historical after this patch.

## Behavior

- Paladin full-set Resistance IV and Traveller full-set six cleanses/one buff
  now test the native anti-magic beacon before granting/refreshing. TECH does not
  block them. Personal no-magic and artifact aura remain separate existing rules.
- Biological Gowno/Gownoman poison, hunger and Wither remain active under both
  jammers. Their source effects, radii, role/team rules and skill scaling remain.
- Each worn Erosion piece temporarily loses all Protection under MAG. Its exact
  former positive integer level is saved in private custom data, with its item
  identity. Decay pauses for the saved level and resumes normally after recovery.
  Full and partial sets retain their original independent/random decay rules.

Already-granted Paladin/Traveller potions retain their current lifetimes, as
explicitly confirmed by the author. No universal effect removal policy
is introduced, and no ordinary potion is removed merely because of beacon entry.

## State ownership and recovery

`LbcErosionSuppression` scans four worn slots plus its own retained references
per player tick; inventory/container/cursor scans happen only at inventory events.
It handles dynamic beacon placement and position-only exit without waiting for
the five-tick decay phase. Mutating only Protection and its own data preserves
other enchantments, presentation, durability, counts and unrelated custom data.

Equipment and the existing inventory-change path reconcile transfers. Copies
and registry-backed ItemStack NBT roundtrips carry their own restoration payload.
Native entity load restores dropped items/nonplayer equipment; item-entity
updates replace a copied stack so native data synchronization observes changes.
Removal, forget and stop restore owned references even inside the beacon.
Replacement-family/item checks prevent transplanting the old level onto another
output. The recursion guard prevents inventory notifications reentering edits.

Lifecycle is connected through existing `LbcItems` load/forget/stop dispatch and
the existing equipment/inventory hooks, not a new world-wide entity scan.
Previously prepared resource-pack files and live grimoire overrides are untouched.

## Answered spell, weapon and summon interactions

MAG instantaneous rays and the shared AK TECH ray use the existing indexed
first-sphere intersection API. Interception wins an exact collision tie; it draws
only the admitted portion and does not invent a terrain/entity terminal at the
field boundary. Ruby cutting stops its sampling before that boundary. Physical
tiny acid is the exception required by the author's later all-acid clarification.
The SLG/SLR firing branch selects TECH without changing other shooters sharing
the old admission helper. Ammo, cooldown, recoil and ordinary collision remain.

Released common Electro/Tesla/Erecto arcs use TECH interception, retaining MAG
immunity. The actual mana-cast admission remains MAG. Shared short-arc consumers
inherit this classification; they do not independently reimplement the policy.
TECH is resolved before reflection from a Greed target beyond it; an earlier
shield still wins over a later TECH boundary. A zero-length intercepted higher
ray cannot sample its origin and damage a victim inside the field.

All released shared acid variants and custom hard-acid charges are physical:
ordinary/tiny/Academy/awakened, volley, source/drop/cloud and other consumers.
Factory tags and old loaded tags no longer identify acid as magic/mechanical.
Flight, contact and aura/poison continue inside both fields. Acid-corruption's
physical acid seeding guide follows this rule; its other magical modes retain
their previous checks. Owner casting gates remain. Blood/Confuse instead filter
each affected victim's MAG-protected position, independent of caster location.

Soul Vex and all six Psychovex/Cultus presets regain source `summactivated` at
assignment. The existing central MAG purge owns deletion; no duplicate per-mob
field scan is added. Their biological bodies do not become TECH actors.
Broken Totem/Screamer's turn disorder is explicitly jammer-immune, while its
ordinary expiration, cleansing/Greed and Time Stop behavior remain separate.

Remaining audit candidates have no inferred new classification. Other jammers
are not suppressed just because a broad helper gained a new use elsewhere.

## Native verification

New `ArmorSuppressionChecks` uses actual source loot, native markers, scheduled
player updates and registered 5/20-tick item callbacks. It checks both jammers,
biological aura, partial/full levels including INT_MAX, dynamic fields, movement,
copies, actual registry NBT, ENTITY_LOAD for items/mob equipment, crafting/cursor,
native normal reforge, replacement safety, forget and retained normal-stop state.

`tools/smoke_armor_suppression.py` owns only the isolated port25576 server, runs
two actual mode1 matches, then stops it normally and restores exact snapshots.
Regressions include armor sets, Traveller, Shulker, effect layers, Time Stop and
contacts. A scoped third participant is created/removed through the existing
native fixture. Main server and source server are never started/stopped by it.

The initial two-match run passed the new suppression/stop probes but correctly
rejected the old Traveller test: it explicitly demanded full-set operation under
MAG, contrary to the new author decision. That assertion now requires rejection
inside the beacon and the original six-plus-one draws after leaving it. Initial
failed evidence remains in `build/verification/jammer-audit-armor/initial-*`.
No production behavior was weakened to satisfy the previous expectation.

A second composition failure was diagnosed with actual hit values: after the
301-tick Traveller fixture, cached native equipment modifiers still held armor9
until the next registered player update. The reflected shell was correctly
removed, applied Levitation, credited the new owner and dealt2.88HP. Shulker's
exact4HP positive control now waits for both real player updates and checks the
native empty-equipment attributes before starting. Diagnostics and failed logs
are retained in `traveller-fixed-*` and `diagnostic-*`; gameplay damage is unchanged.

Expanded-run evidence `expanded-initial-*` exposed two more fixture issues. The
acid seeding count included already removed charges awaiting ENTITY_UNLOAD in
the same callback; it now asserts exactly one live seed and keeps the native
purge survival positive control. A passing `spell-focus-*` run confirms this.

`tesla-composition-*` then reproduced the previous equipment leak independently:
Tesla conduction gave owner999.34/ally999HP with armor9/0, both clients loaded,
neither changing dimension and no effects. Traveller's old snapshot restored
inventory but not cached native equipment attributes. Its snapshot now captures
and restores every supported attribute plus `lastEquipmentItems`, for both
participants, so the next fixture does not inherit phantom armor. The exact1HP
WILD conduction assertion remains; Tesla production and damage are unchanged.

The preexisting author-edited `AcidAntDropChecks.java` is preserved byte-for-byte.
Its static burst-tag assertion still lists `magic`; that single historical
expectation predates the author's new all-acid physical classification. It is
not represented as a passing updated regression. The new shared-acid checks
exercise the current source-acid constructor and actual beacon pulses instead.

Physical-client pixels, arbitrary latency and peak-TPS performance are outside
this fixture's claims. Final qualification and installation receipt are recorded
under `build/verification/jammer-audit-armor/`.

## Qualified and installed

Final offline build passed. The full runner passed40recorded results in two
actual mode1matches,155.66seconds, including all four new suites and the actual
normal-stop recovery probe. Previous armor, Traveller, Shulker, Academy acid,
Tesla, Erecto, Blood/Confuse, effect layers and Time Stop/contact regressions
passed together. Owned child shutdown and exact fixture snapshots restored.

Artifact SHA256:
`746e62aaaadfcefcc992f488bdd8bbd1259b2e353fc45db01c156533d17a109c`.
Ready JAR: `build/verification/skywars2-jammer-audit-746e62aa.jar`.
All530Java sources,1507classes and3226processed resources match the corresponding
source/main archives; Loom's declared nested SGUI metadata is accounted for.
All195preexisting author paths retain their bytes or absence;31live grimoire
override files also retain exact hashes. No resource-pack changes were required.

Installer PID24408 installed the tested JAR over154cc3a7 after verifying the
main server was stopped, without stopping/restarting it. The installed main
hash was independently checked. Receipt: `main-install.json` in the evidence
folder. Retain746e62aa, immediate rollback154cc3a7, fallbacka4683fe2 and the
separate network-audit tool. The older46034d3cready JAR is obsolete and removed
only after absolute-path and all retained-artifact hash verification.
