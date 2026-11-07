# Uniform native item selection component, 7 November 2026

Implementation and source checks only. No Java build, JDT, server or other JVM
was launched by this owner. No existing loot selector, registration, shared item
owner, installed library, Main server or author resource pack was edited here.

Owned inputs: `LbcAllItemPools.java`, `UniformItemPoolChecks.java`,
`tools/build_uniform_item_pools.py`, generated `skywars2/all_item_pools.json`,
this note. Root owns registration, lifecycle and selector adapter changes.

## Pool roles and source boundaries

`lbc:random_item` is the custom-only selector. `lbc:random_glack_1` is the
custom-plus-vanilla selector. `lbc:random_glack` wraps that selector with native
uniform count1..64, max-stack99, max-damage removal and limit-count99, in that
exact order. Keep this wrapper intact. `lbc:27_random_items` keeps27 rolls and
its own count64 wrapper around the union selector.

`lbc:infinity_no_shulker` remains the existing vanilla-only26-roll selector,
with1657 equal registry identities and its original1970 source profiles. Do not
replace its per-item profiles with this component's default-only VANILLA domain.
`lbc:infinity_upgraded_luck` and `_noadv` retain26 source lucky-block draws,
their count64/component wrappers and the weighted `lbc:luckyblock` outcome
distribution. These weighted lucky-block outcomes are not the custom catalog.
Type-specific/reforging selection remains a separate follow-up; this component
does not rewrite those tables into the global union.

The runtime entry is `{"type":"skywars2:uniform_item","domain":"custom"}`
or domain `union`. Replace only each base selector's selection body with one
such entry, preserving any caller pool/table/function/condition wrappers.
Original base selector source trees are retained as `selectionSources` in the
manifest, so the generator remains reproducible after the adapter replaces them.

## Catalog authority and parser

The generator imports `identity`, `modifiers`, `standalone` and `packed` from
`canonicalize_lbc_loot.py`. Authority is the original two selection roots and
their exact reference closure, all current `item_defs` canonical definitions,
and existing authored direct singleton canonical owners recognized by the same
`standalone` predicate, plus the explicitly audited native Cunny domain. This includes canonical owners reused under `ported/`
without generating another item_defs alias. Public loot suggestions are never a
completeness filter. Explicit `gui/` owners and item_defs with only GUI usage
owners are excluded with reverse-reference evidence (127 source roots).

Arbitrary structure/chest callers are not global profiles. Their contextual
wrappers stay on their own caller. An initial over-broad all-table expansion was
corrected before freezing this implementation: it had produced89999 profiles
and64MB by importing those unrelated callers. No meaningful profile in the real
selection closure was discarded by that correction.

The canonical identity includes carrier, model, name and custom data, retaining
real modes/grimoire identities. Count/max-stack variations are profiles inside a
single identity, never extra outer tickets. Active leaf/reference/pool/table
condition and modifier boundaries remain ordered in an interned singleton-group
DAG. Inline tables are expanded. Alternatives singleton siblings retain previous
sibling failure conditions; unknown composite/reference forms emit blockers.
There are no active multiple-custom-data-merge profiles in the inspected input.
Canonical conservative identity semantics, including cosmetic identity distinctions,
are explicit; this does not assert a new universal semantic alias equivalence.

Legacy plural `functions`/`conditions` ignored by current native codecs remain
verbatim findings, and are not silently made active. The existing empty
`lbc:reroll_9999/material_block` file is reported as unparsed outside this closure;
it is not a missing selected profile. The generated `complete` flag means this
declared source closure parsed without blockers, not that full TMT/public release
or every native gameplay domain has been qualified.

Frozen audited generation:1953 authored logical slots,7286 contextual custom profiles,
10239 interned nodes,4803 closure tables,8961 accounted source paths. Runtime JSON
is7,313,251 bytes, about6.97MiB. These are representation measurements, not a
completeness percentage. Native IDs absent from authored nodes receive no ticket
unless an explicit complete-native-prototype policy is present. Parameter variants not separately
represented in canonical source or registration remain an audit limitation; the
registered native admission is not proof of all possible property combinations.

## Audit of registered native extras

Read-only fixture evidence: `build/verification/key-reuse-server/world/data/fabric/registry_entries.dat`
contains875 registered native item IDs. `catalog-domains.json` reports871 at its
older capture and has67 native-default findings, not a full item-ID membership
list. The real fixture registry initially had30 IDs absent from the manifest:

- `blood_slab`, `cum_slab`, `bucks`: root renamed the three exact canonical
  source leaves to their native carriers and added canonical ported wrappers;
  regenerated selection now represents them without duplicate default slots.
- `cunny_0` through `cunny_21`: exact registered native prototypes fully decode
  the22 indexed authored `cunny_items.json` source outcomes. Their authoritative
  `lbc:imba/cunny_slabs` source table is now a bounded extra root; its1..8 rolls
  and bonus rolls do not create extra identity slots in this global selector.
- `academy_blaze_wand`, `academy_fireblaze_wand`, `academy_psychowand`,
  `academy_psychoblind_wand`: defaults are bare wand transports. Full profiles
  now derive from the actual source base templates using the qualified Academy
  promotion: transmute carrier, retain all base components, ready/upgraded/mode
  flags, exact a/s/b/ss tiers and appended upgraded lore. The script reads and
  guards **git blob4aa9601b:src/main/java/me/skywars2/game/LbcAcademy.java**;
  it never reads the protected working-tree Academy to define this transform.
- `oguzok_sword`: the single allowed native default. Qualified `LbcWtf.SWORD`
  deliberately supplies its full wooden-sword/tool/model prototype and `sword()`
  returns `new ItemStack(SWORD)` without deferred payload. The exact qualified
  git blob and SHA are retained in the explicit policy.

After the user's all-mod-block scope correction, `consumed_block` has a second
explicit complete-default policy. Qualified `LbcConsumedBlock.ITEM` constructs a
real `LbcBlockModels.CubeItem` linked to its real BLOCK and exact source model.
Qualified `LbcBlockModels.CubeItem` extends PolymerBlockItem, retaining native
placement. Both git blobs and SHA hashes are recorded in the policy. Its
generated-terrain use does not make this playable block a developer/service item.
TMT ability classification remains a separate catalog concern and is unchanged.
The membership check now selects this exact native slot and verifies full default
components, source model and BlockItem-to-BLOCK linkage. There is one ticket,
with no broad default admission for future native IDs.

After reconciliation866 of875 fixture native IDs have authored nodes, six TMT
transports belong to the one factory, only `dev_tools` is service-excluded, and
consumed_block plus Oguzok have the two explicit complete-default policies
(866+6+1+2=875). There are zero
unresolved IDs in that exact fixture registry set. Unknown future registrations
produce findings and `complete=false`; they never silently create empty payloads.

Mana is an important positive control: the three native transport IDs are already
represented by9 drink,5 splash and6 lingering authored dose identities. The
constructor's `PotionContents.EMPTY` defaults add no slots. Checks now materialize
every one of those source identities and require a real native potion payload.
Checks also materialize all26 explicitly audited extra source domains, validating
full presentation and exact Academy/Cunny private flags.

## Native selection and lifecycle

`LbcAllItemPools.register()` registers the native26.3 `SingleEntryContainerBase`
MapCodec in `LOOT_POOL_ENTRY_TYPE`. Root must instantiate the service and expose
`game.items().allItemPools()`. Call `initialize()` once at SERVER_STARTED after
item/dynamic registries and the full game are available. Call `reload()` after a
successful END_DATA_PACK_RELOAD; it builds privately and publishes atomically,
leaving the old catalog usable if binding fails.

Binding uses `server.reloadableRegistries().lookup()` serialization context for
native entry/function/predicate references. All decoding/registry enumeration
occurs during initialization/reload. Retained data is immutable native DAG nodes,
shared condition/function holders and slot arrays; local Gson/provenance/cache
graphs are released after construction. Expected retained memory is tens of MiB,
not measured because this owner ran no JVM. Actual heap/startup time requires
root qualification under the user's sequential memory budget.

CUSTOM has one equal outer index per source/native logical slot and exactly one
TMT factory. VANILLA has exactly one default native slot per current non-AIR
minecraft item. UNION concatenates the logical sets under one index, rather than
flipping a fixed50/50 branch. Union vanilla source quantity/property profiles stay
inside that vanilla identity; existing infinity remains untouched.

The winning TMT slot calls the real `LbcTmtRuntime.generate(random)` once, producing
a fresh one of its five native forms. Source token/carriers never add tickets.
The selected logical slot is fixed before eligible native source profiles are
considered; rejected profiles do not reroll a different item. Raw native output
keeps source count0 stacks until the existing caller's final count wrapper can
apply. An entirely ineligible selected identity can emit no item, preserving
native conditions; the bucks one-item guard intentionally exposes that failure
instead of silently switching to another identity.

## Checks added and work remaining

`UniformItemPoolChecks.run(game)` compares full actual vanilla membership, all
authored slot IDs/source-accounting links, complete registered native admission,
the exact union index, and one TMT ticket. It executes every vanilla index,
generates fresh private TMT outputs through the real factory, reaches all five
forms with a fixed seed, and tests native entry CODEC/64-and99/max-stack wrappers.
A forced first outer-index seam tests that those wrappers retain all seven TMT
private ability slots through the actual registered native entry.

Python synthetic count/mode identity and ordered-boundary checks pass.
Generator `--check` reproduces the manifest exactly. `git diff --check` passes.
Java compile, live binding, the explicit native suite, actual bucks trades and
all relevant regression suites remain root-owned and unrun by this owner. Keep
public bucks/TMT acquisition gated until the coherent full release is qualified.
