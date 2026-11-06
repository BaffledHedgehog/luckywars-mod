# Eleven native ordinary tools — qualified, installed (6 November 2026)

Eleven existing ready tools now use native Polymer items and the common authored
prototype reader. Existing SpecialTools/recipe/Summons and vanilla interaction
owners retain the behavior. No new ability engine, tick task, inventory scan or
per-tick NBT parser was added.

| Existing ready loot | Native identity |
| --- | --- |
| `lbc:ported/efficient_shovel` | `skywars2:efficient_shovel` |
| `lbc:ported/random_enchanted_shovel` | `skywars2:random_enchanted_shovel` |
| `lbc:ported/random_enchanted_axe` | `skywars2:random_enchanted_axe` |
| `lbc:ported/random_enchanted_pickaxe` | `skywars2:random_enchanted_pickaxe` |
| `lbc:ported/enhanced_flint` | `skywars2:enhanced_flint` |
| `lbc:ported/hammer` | `skywars2:hammer` |
| `lbc:ported/mortis` | `skywars2:mortis` |
| `lbc:ported/poleaxe` | `skywars2:poleaxe` |
| `lbc:ported/sickle` | `skywars2:sickle` |
| `lbc:ported/silk_pickaxe` | `skywars2:silk_pickaxe` |
| `lbc:ported/tomahawk` | `skywars2:tomahawk` |

All eleven canonical source conversions change only the ID. Every source function,
count, weight, condition, name, lore, tier, attribute and enchantment setting is
preserved. Random Enchanted Axe/Pickaxe/Shovel still execute the original uniform
1..30 enchant-with-levels operation at EACH actual loot issue. The static native
prototype keeps fixed fields and does not freeze a random roll or create a delayed
inventory mutation. Use the listed loot tables for fresh generated enchants;
a bare /give has the fixed native prototype, as expected for stochastic loot.

## Shared carrier and tag authority

The common prototype factory uses actual 26.3 vanilla axe/shovel/hoe/pickaxe
property builders, preserving mining speed/tier, weapon attributes, durability,
repairs, enchantability, fire resistance and native block-transformer interactions.
The native flint inherits FlintAndSteelItem. No copies of the vanilla interaction
algorithms were introduced. Count and stochastic enchant functions remain owned
by loot; all fixed source functions are applied to native defaults.

Eight additive vanilla tag overlays retain tool/enchantment/harvest/igniter/piglin
membership. The existing bioitems tag also retains wooden-sword/carrot-carrier
PhD material membership for the related melee items. Four additive Fabric convention overlays preserve igniter, melee,
mining and shield memberships for these tools and the related fifteen native
melee items from the preceding batch. Original memberships remain intact. The
full runtime carrier tag graph is checked, not just a handpicked list.

The independent original-carrier component oracle and statistic scopes are reused
from NativeMeleeChecks. No second loot comparison or ability implementation was
introduced. Original models and RU/EN keys exist in the author RP/vanilla assets;
they were inspected read-only. No resource-pack files or templates changed.

## Exact native validation

Test `0e7d6a47` / production `7b222d22`; full hashes and sealed evidence:
`build/verification/native-ordinary-tools-2026-11-06/qualification.json`.
Eight fresh Java families, 24 exact processed resources, 10242 unchanged source
inputs and all 282 protected author working paths. Binary linkage passes and
production physically excludes MapAuthoring/MapDeletion classes.

Seven actual isolated matches passed on this exact test archive:

- Mode 1: native-tools; ordinary-tools, special-tools, native-melee.
- Mode 3: native-tools, ordinary-tools, special-tools.

The common oracle decodes the same source on the original vanilla carrier and
compares ALL component keys/values/count. Random tool outcomes are compared with
the original using identical seeds17/29/41, and must vary across fresh rolls.
Static native defaults, ready source identities, every real carrier tag, Polymer
round-trips and item-codec persistence are checked. Genuine native inputs strip
an oak log, create a dirt path, till farmland, ignite fire and mine stone with
Silk TouchX, producing native stone loot. Existing scenes cover actual tool crafts,
weighted pools, Communism/Summons allegiance, Mortis native attacks, Death Scythe
and Banhammer behavior/terrain/TimeStop. The strengthened melee regression checks
all31 original profiles and the component/roundtrip and complete carrier
tag behavior of its original fifteen items (including17 Fragment states).

An early candidate000097c7 was rejected by the strict runtime convention-tag
oracle. Fabric c:tools/melee_weapon membership was missing from a native axe.
The four shared convention overlays fix that compatibility gap for both groups;
no assertions were removed. Rejected logs remain in the stage. Final scenes were
rerun on the exact qualified archive. A second candidate e6ec2a5f passed the new
tool oracle and ordinary recipes, then exposed the missing bioitems membership
on older melee aliases and an obsolete four-outcome shovel-pool fixture. The
shared tag is preserved and the fixture now requires all five distinct native
outcomes (three shovels plus two maces); no gameplay branch was removed.

All fixture processes shut down normally and restore their recorded snapshots and
auxiliary files; no live child, retained lock or temporary local map remains.
Physical-client visuals, peak-load TPS, disk restart and broad rewind are separate
qualification gates.

## Release

The qualified test JAR is installed to the stopped main server; both build/libs
files match the qualified test/production hashes. No main restart and no push.
Receipt: `build/verification/native-ordinary-tools-2026-11-06/main-install.json`.
One heavy operation at a time: Gradle512MiB/one worker, fixture2GiB only above
3GiB physical headroom, one unpacked archive at a time. Only the confirmed own
Serena JDT was stopped. Full IMBA/TMT-last/custom-mixed-type pools/modes2,4/skill2
and broad rewind gates remain unchanged.
