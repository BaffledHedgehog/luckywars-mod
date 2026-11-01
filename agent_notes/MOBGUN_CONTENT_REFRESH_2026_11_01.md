# Mobgun current-content refresh — 1 November 2026

Native integration follow-up: all90 current vanilla Mob types were actually
launched in the isolated fixture; mobgun-catalog and the existing mobgun group
both PASS on8c36ce77. Final cb3fe942 has byte-identical production and resources;
only an unrelated catalog check class changed. Full evidence and queued
installation are recorded in [the consolidated contract](CONTENT_REFRESH_2026_11_01.md).
Shulker coverage follows the source passenger tree in outcome48, rather than
incorrectly requiring a standalone root; its carrier receives velocity3.

## Scope and source evidence

User requests all missing current vanilla mobs, retaining author source outcomes and
special actors. Baseline is the actual old server 1.21.11; target is actual26.3.
The registry investigator generated native vanilla reports, rather than treating
Minecraft version numbers as calendar labels:

- `build/verification/content-refresh-2026-11-01/native-registry/reports/registries.json`
- `build/verification/content-refresh-2026-11-01/native-registry-1.21.11/reports/registries.json`
- `build/verification/content-refresh-2026-11-01/registry-delta.json`
- `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, native `EntityTypes.java`, `EntityTypeIds.java`, and entity superclass declarations.

Native registry reports contain161 current versus157 baseline entity types. Four
new entity IDs are cushion, poplar_boat, poplar_chest_boat and sulfur_cube. The
only missing actual Mob was sulfur_cube. Existing Mobgun already includes all
five prior native additions (nautilus, zombie_nautilus, copper_golem, camel_husk,
parched), and every other current native Mob type. Both versions contain46
potions and40 effects, with no registry additions; the89 source potion choices
remain unchanged.

## Implementation and policy

Append native outcome115 for `minecraft:sulfur_cube`, offset1, original
`raycastmob`/`prestartf` tags, `nativeFormat:true`. Native format avoids feeding a
current-only entity ID through an obsolete DFU schema. The current entity loader,
velocity3, native AI, ownership, descendant attribution, source epoch, no-save
and shared disposal continue to own this mob. No new private actor engine.

Every preexisting JSON outcome and every potion entry is structurally identical
to HEAD; the static comparison passed. Original source variants and technical
actors remain. Luck7 still forces source outcome22; special75 still chooses one
of the source potion combinations. Random selection remains uniform per outcome
and per potion entry; appending a type naturally changes the common denominator.
No individual source entry receives an extra weight.

Preparation now derives the direct outcome maximum and deterministic ascending
potion choice list. Both groups reject duplicate numeric IDs, holes, nonpositive
IDs and reserved direct outcome75;75 must remain inside the direct draw range.
Direct native entity IDs must exist in the target registry. Hardcoded113/114/89
runtime cardinalities are removed. No runtime entity prototypes or reflection.

## Native verification contract

`MobgunCatalogChecks.run(GameController)` is the new public check API. Root owns
wiring the `mobgun-catalog` group and executing the common isolated native build
and server fixture. This file does not claim an unexecuted native PASS.

The check constructs each vanilla registry entity type in the fixture level with
COMMAND, never inserts or ticks the prototype, records `instanceof Mob`, and
immediately disposes it. This explicitly includes golems even though their native
category is MISC. Player, mannequin, armor stand, boats, displays, projectiles,
cushion and other non-Mob technical entities do not expand required coverage;
existing author special presets remain allowed.

After registry closure, the check actually launches a catalog outcome for every
current native Mob type, including sulfur_cube. It asserts exact native root
type, ordinary AI, source offset, velocity3, every root/passenger owner, actor
marker, source epoch, no-save, removal of `prestartf`, and immediate complete
cleanup. TestAlpha's player snapshot and prior Mobgun cooldown are restored.
Existing `mobgun` checks independently retain all source outcomes, all potion
choices, luck7/dragon, cooldown, suppression, input, descendants and blast credit;
existing stop/cleanup groups retain the actual lifecycle boundary probes.

No build/server/resource-pack/user-protected files were edited by this task.
The central coordinator records actual compile and native evidence after running
the newly wired group.
