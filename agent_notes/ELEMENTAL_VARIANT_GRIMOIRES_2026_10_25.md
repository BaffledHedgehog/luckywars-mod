# Nineteen consumed elemental variants

## Source

Read-only `LBC/data/lbc/loot_table/spells_library.json` and
`function/items/libspells/select.mcfunction` select the following existing
`function/workingitems/sauvojen_staffs/` helpers. Direct use consumes a library
snowball carrier and never admits/debits mana. Metadata remains for crafting.

| ID | Native item / `lbc:ported/` leaf stem | Source helper |
| --- | --- | --- |
| 161 | ruby_ray_grimoire | ruby_ray |
| 162 | ruby_hole_grimoire | ruby_hole |
| 164 | terra_fast_grimoire | terra_preloop_upgraded |
| 166 | electro_ray_grimoire | electro_ray |
| 168 | air_burst_grimoire | air_preloop_burst |
| 169 | air_fast_grimoire | air_preloop_fast |
| 170 | air_homing_grimoire | air_preloop_homing |
| 172 | acid_burning_grimoire | acid_preloop_upgraded_lib |
| 174 | teleport_inverted_grimoire | teleport_inverted |
| 175 | teleport_cloud_grimoire | teleport_cloud |
| 177 | stone_ray_grimoire | stone_preloop_upgraded_default |
| 178 | stone_spikes_grimoire | stone_preloop_upgraded_spike |
| 179 | stone_bridge_grimoire | stone_preloop_upgraded_bridge |
| 180 | stone_shield_grimoire | stone_preloop_upgraded_shield |
| 182 | fire_big_grimoire | fire_preloop_upgraded_big |
| 183 | fire_ray_grimoire | fire_preloop_upgraded_concentrated |
| 185 | blind_gatling_grimoire | blind_preloop_upgraded_gatlingator |
| 186 | blind_phantom_grimoire | blind_preloop_upgraded_phantom |
| 187 | blind_rain_grimoire | blind_preloop_upgraded_rain |

The importer retains original modifier order, model, name/lore colors and
italic flags, empty row, Epic plus adjective rarity row, tier and stack99.
Original adjectives include Powerful, Universal, Enhanced and Smart; they are
not synthesized from the tier. The current26.3(1) author RP is verified read-only
for RU/EN/RPR keys and recursive item/model/texture dependencies. Only native
item IDs and obsolete loot-schema field names differ from the source entries.
Broad library/random tables remain gated.

## Author rule (user)

> ??? ???????? ??? ?????? ??????????? ????? (?????? ? ???????? ???? ??????????? ????????)

Treat the corresponding wand/mode payload as the shared implementation. Keep
only source-backed or explicit author differences in the consumed wrapper.
This also governs later library batches; do not build separate projectile
physics, targeting, damage or visuals for the same spell.

## Native implementation

`LbcElementalGrimoires` now covers all28 direct elemental IDs160..187. Its
existing early guards, mana-free actual-hand consumption and no-carrier path
also govern variants. Shift casts the same selected spell; it cannot cycle
wand modes or begin an Academy trial. Both actual hands can cast together.
Defaults used by `/give` reproduce the imported source presentation.

The existing Ruby/Terra/Electro/Air/Acid/Teleport/Stone/Fire/Blind payloads are
reused. Burning acid is aligned, explicitly ignited and keeps acid physics.
Phantom tar emits four30-tick charges. Burst Air emits four charges; fast and
homing Air retain their distinct modes. Cloud creates an owned, mountable
arrow at feet+0.01 with velocity0.5. Stone bridge preserves all15 material
bands; shield gives Resistance V for100ticks. Large Fire emits one native
power2 fireball at2.5 velocity. Ray variants retain native collisions and
terrain/particle/sound effects of the already ported corresponding payload.

Gatling consumes a book to add20 captured-owner shots, accumulating repeated
casts. The queue continues after the last book has left the hand. Its source
helper has no immediate cast sound: it uses a shared extracted queue producer;
existing wand casting keeps its prior immediate cue. Stone bridge/shield
similarly invoke the direct helper without an invented initial wand cue.
Existing ordinary/Academy wand admission, modes and mana remain unchanged.
Owner/epoch, shared time stop/antimagic and native lifecycle remain inherited.

The new packet fixture exposed an existing missing Blind rain aim trail. The
shared rain engine now restores source squid-ink samples every0.4blocks up to
the actual contact or100block source limit, stopping at unloaded chunks. This
applies to both wand and consumed rain without changing cloud/drop budgets.
Burning acid keeps its native initial age1 and300-tick lifetime budget.

## Qualification

Build462ab2c6 passed14 recorded groups in two actual mode1 matches (45.20s).
Exact374 sources/1056 classes/2865 resources match the tested artifact, including
all19 new deterministic loot leaves. The variant fixture uses actual game-mode
item input for all19 kinds, both hands and Shift, source/default component
equality, final-book consumption, zero/negative unchanged mana, projectile
ownership and counts, contact terrain effects, sound/particle packets,
cloud mounting, shield duration, queue accumulation/firing after consumption,
nonconsuming guards and a separate real match-stop probe. Prior nine ordinary
elemental grimoires, consumed Danmaku and full Blind rain regressions run together.
The existing rain regression now accounts for its shared emission phase: after
two service calls a first drop may have age0 or1, and its expected height is
source height minus age times1.8. This corrects a phase-dependent assertion,
without changing rain physics.
Client image/audio playback and peak TPS are not measured by these fixtures.

Evidence: `build/verification/elemental-variants-final/qualification.json`,
`main-install.json` and `tools/smoke_elemental_variants.py`. The isolated server
stopped normally and settings/profiles/scoreboard/JAR were restored with locks
released. Only the known fake-bot Polymer packet-context diagnostic appeared.
Pre-existing user hashes/deletions and author RP remain preserved. Installer5248
installed462ab2c6 over2159fbe9, with2159fbe9 rollback and no restart.
