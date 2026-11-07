# Typed uniform logical selectors — implementation handoff, 7 November 2026

Code/resource preparation only. No compiler, JVM, JDT/index, server, installation,
commit, protected author source or resource-pack edits. Native qualification is
still required; the earlier 16-suite parent does not qualify these new changes.

## Ownership and integration

Owned changes: `LbcAllItemPools.java`, `tools/build_uniform_item_pools.py`,
`skywars2/all_item_pools.json`, new `TypedUniformItemPoolChecks.java`, and 56 NEW
`data/lbc/loot_table/uniform/*.json` resources. Existing weighted selectors,
`lucky_items` graphs and original 27-item wrappers were not edited here.

`LbcAllItemPools.uniformTable(String legacy)` returns the dedicated table key for
31 transformables, rofls/slabs/spells_library, and 12 functional selectors. It
returns unknown and set-specific selector keys unchanged. `uniformTables()` is
the immutable complete 46-entry mapping. Translate only at designated uniform
draw boundaries; keep the original reforge planning API/table order intact.

Native entry syntax is `{"type":"skywars2:uniform_item","domain":"custom",
"types":["axe_all","hoe_all"]}`; optional `item_type` accepts a single name.
`type` remains the native entry discriminator. `spell`/`spellitem` normalize to
`spells`; `summoner` normalizes to `summons`. Duplicate and reordered types have
the same union vector. Public Java APIs accept Domain plus Collection<String>:
`prepareTypes`, `slots`, `draw`; `types()` exposes available names. Prepare new
ad-hoc combinations during setup, before calling draw. Native CODEC decoding
prepares combinations, including combinations decoded after initialization.

Root integration:

- Translate normal/ultra reforge tables at the actual draw boundary only.
- Translate the two EdibleSpells selector draws, retaining their outer counts.
- Original `27_*` callers can reference corresponding `uniform/27_*` tables.
  Each new table ALREADY contains the original 27 rolls, pool/table functions,
  entry count/stack modifiers and conditions. Do not nest a second 27-roll loop.
- In `infinity_no_shulker`, replace only inner item selection with domain
  `vanilla`, types `["no_recursive_shulker"]`; retain its 26 rolls. Leave the
  distinct original infinity self-return pool unchanged.
- Add `TypedUniformItemPoolChecks.run(game)` to a dedicated native suite and
  include its class family and all new selector resources in the component.

## Eligibility and source profiles

20 base views retain EXACT original transformables closure, profile subsets and
native gates. Eleven `_all` views retain their original closure/profile subsets;
additional authored top-level category flags and actual native semantic
tags/classes can add members using full global source profiles. Source members
are never widened to all profiles merely because another caller has an
unconditional variant. Transport materials are not custom category metadata.
Dedicated rofl/slab/grimoire closures retain their original source profiles.

Base counts: accessory33, axe4, bow11, hoe2, pickaxe2, potion13, shield3,
shooter8, shovel3, staff25, sword34, totem2, spells10, summons8, trident2,
arcanum6, helmet18, chestplate19, leggings18, boots18.

Manifest `_all` counts: accessory50, arcanum8, axe5, boots22, bow23, hoe3,
shooter15, shovel5, staff114, summons11, sword50. Runtime actual semantic tags
may add justified native members only to broader views. Other closures: rofl27,
slab83, grimoire268. These are reconciliation counts, not completeness proof.

Combining views unions logical IDs and their allowed profile sets once at
bootstrap/CODEC binding. There is no category coinflip, duplicate-category
weight or per-draw registry/NBT scan. TMT is only admitted to a typed view if its
original source closure reaches TMT. None of these 34 current closures does.
Global custom/union keep exactly one fresh five-form runtime factory ticket.

If selected slot profiles are all blocked by actual native conditions, draw a
uniform random permutation of remaining logical slots until one is admitted.
Lazy sparse Fisher–Yates allocates only after the first failed admission; no
replacement, arbitrary retry cap or infinite loop. A raw zero-count source
callback is successful emission, never condition failure. `drawSlot` remains
an exact-index seam without fallback.

## Vanilla source fidelity and helper primitives

The vanilla source alias has 1657 logical items and 1969 distinct source profile
roots. Its membership equals full current non-AIR vanilla defaults; all 17 plain
vanilla empty shulker boxes remain eligible. Source has no custom recursive
container factory, so excluding all vanilla boxes would alter source behavior.
Its exact per-item functions/counts are retained rather than replaced with bare
default profiles. Vanilla global API remains native defaults; union retains
existing source-aware vanilla profiles.

Root restored generic Minecraft repeating_command_block and introduced FOUR
dedicated native implementation primitives. They are excluded ONLY as independent
standalone roots; full authorities are explicitly included:

| Helper primitive | Full source authority |
| --- | --- |
| item_defs/skywars2/bucks | imba/random_bucks |
| item_defs/skywars2/academy_token | kit/academy_token |
| item_defs/skywars2/blood_anal_boom | trinket/blood_anal_boom |
| item_defs/skywars2/mana_gem | trinket/mana_gem |

Each now has one logical slot. Currency has one full source profile. The other
three retain two full source profiles each where their authored/ported contexts
differ. Academy token differed only in name serialization (`[component]` versus
`component`), which Minecraft decodes equivalently. Bounded identity normalization
for these four helper carriers coalesces singleton name arrays, while retaining
both original raw profile trees. Other native complete-default admission remains
explicit consumed-block/Oguzok policy, not a blanket default exclusion/admission.

Current manifest: 1953 authored slots, 7289 source profiles, 10247 interned nodes,
34 views, 923 manifest memberships, 1016 typed profile references; 7,767,532 bytes
minified. Typed views refer to existing nodes rather than duplicate components;
runtime vectors reuse immutable Node bindings. Temporary Gson/decoder maps are
released after binding. Typed indices add small slot/list/reference structures;
no retained JSON tree or per-draw parser cache. No heap measurement was run.
The old empty reroll_9999/material_block remains outside catalog source closure
and is an explicit informational parse finding; no new blocked closures exist.

## Checks prepared and checks run

New native suite compares all base/subtype membership sets against pinned source
metadata, requires broader views contain their source members, checks every
typed index through actual registered native entry CODEC and count64/max99
modifier, checks count99 per view and component validity, union deduplication,
all 12 actual native 27-roll tables and original entry/pool wrapper fidelity.
It explicitly checks vanilla RCB isolation, four full helper authorities and
one currency slot with real count1..64 metadata. Condition fallback uses real
native denied entries and a native admitted zero-count entry, checking one visit
per slot and finite all-blocked behavior. Existing global exhaustive checks
remain intact.

Executed offline: generator `--self-test`, regeneration plus exact `--check`,
and owned-file `git diff --check`. No native or compile result is claimed here.
