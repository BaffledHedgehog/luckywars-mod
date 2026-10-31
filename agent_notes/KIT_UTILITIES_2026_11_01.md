# Mechanical kit acquisition correction and next utility preparation,1 November2026

## Completed correction: mechanical kits12 and13

Previous036b29de qualified the three native abilities and their isolated canonical
leaves. It did NOT package kit/steampunk or kit/hook, or register their actual menu
bindings. The previous statement that ordinary kit acquisition was complete was
therefore too broad. This batch closes that missing path, without changing ability
rules already confirmed by the user.

Exact source catalogue additions are kit12/page9000/slot11, gated by
lbc:true_advancements/armor/steampunk, and kit13/page9000/slot12, gated by
lbc:true_advancements/armor/technomk2. Existing notification mappings and source
artwork/gates are reused. Original disabled rocket pool remains zero rolls.
All prior kit/skill/levelup/trinket entries compare exactly after removing these
two additions. Their actual source tables point at the previously qualified
native Breaker/glove/hook leaves; the two tables are now packaged.

Native mechanical-kits-menu executes in the actual lobby. It tests refusal with
source advancement locked, native award and original-slot selection, then normal
LbcLoadouts.apply grants exactly two items for12 and one for13. Progress, profile,
attributes, configuration and inventory are scoped/restored. Registered native
kit-utility-items and all three existing ability suites also pass on the same JAR.

## Prepared next group: four multiplier tablets and banana palm

[Exact Paper/datapack/RP audit](MULTIPLIER_TABLETS_SOURCE_2026_11_01.md).
Five SimplePolymerItem prototypes preserve existing names/models/lore/carriers;
five canonical leaves retain source modifier order. Original tablet/palm table
pools and metadata are preserved. Native source-table tests pass. Existing banana
food consequences are already owned by LbcNamedFoods and are not duplicated.
Stateless native-component attribute/enchantment transformation primitives are
prepared; their gameplay callers are NOT enabled or qualified.

New kit8/tablets and kit24/palm catalogue entries are intentionally still absent;
kit23 is the unrelated existing fast-sword kit. Thus this preparation does not
activate the new unfinished abilities through ordinary kit selection. Questions
were sent in chat; answers have not arrived. Do not infer answers or mark all
five mechanics complete. There is no runtime legacy-function bridge, private
rewind engine or new item/effect polling system. No new RP/template files needed.

## Native evidence

Evidence root: build/verification/kit-utilities-2026-11-01/.
Initial f2ea86a9 acquisition-only attempt rejected because the two new source kit
tables were not on the resource allowlist. Its normal-exit receipt is retained.
3d730b27 passes acquisition-only source-table/prototype checks after packaging.
Final a3f4644c passes mechanical-kits-menu (four lobby repetitions),
kit-utility-items, steampunk-breaker, steampunk-glove and steampunk-hook in four
fresh native mode1 matches on one immutable archive.
Elapsed runner time: 76.76 seconds.

9564 inputs,2345 compiled classes and7165 processed resources verify exactly;
native bundled-jar metadata preserved. Child exit is normal, exact launch/player
snapshots and additional level/ticket bytes restored, no retained child/world
locks. All224 pre-existing author paths and31 live grimoire override files remain
exact. No physical-client or loaded-server peakTPS qualification is claimed.

Release scope is the completed ordinary mechanical-kit acquisition correction,
with inert next-item definitions prepared. Full tablet/palm ability release still
requires their chat decisions and actual gameplay implementation/tests. Main
baseline036b29de; use only the shared normal-stop installer for the qualified
mechanical acquisition correction. Never stop/restart main to install it.
