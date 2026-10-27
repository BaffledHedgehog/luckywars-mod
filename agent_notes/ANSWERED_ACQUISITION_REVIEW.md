# Fragment knowledge and answered acquisition review

27 October2026. Read-only acquisition review plus a narrow `LbcSkills` recipient
projection extension and new `FragmentKnowledgeChecks`. No builds, servers,
git, catalog entries, source pools or protected files were changed by this child.

## Fragment recipient knowledge and task

Source: [complete Epic/Fragment audit](EPIC_FRAGMENT_SOURCE_2026_10_27.md),
`swrg_kit_integration/game/skills/looper_explain_fragment.mcfunction`.
Current `fragmentsword:1,unresolved_fragment:1,cooltype:0..16` vanilla stacks
now enter the existing Polymer recipient projection. Only an entitled Looper
recipient sees the original `fragment_0`..`fragment_16` yellow/nonitalic lore[1].
Unknown recipients retain `fragment_sword.lore` dark_purple/nonitalic. Legacy
`fragmentsword_old` remains unchanged. The authoritative item, cooltype,
unresolved flag, native components and client round-trip stay unchanged.
Inventory/item-entity/equipment refresh uses the same published Looper snapshot
and existing rare refresh path as current artifacts. Knowledge hiding still runs
last and retains its question-mark name/lore priority.

The original `lbc:true_advancements/swords/fragment` is already packaged with17
native inventory_changed criteria `cool_1`..`cool_17`, mapping cooltype0..16 and
current transformable/sword/fragmentsword flags. Default AND requirements collect
these over time; no new production criterion/award producer is required. Legacy
does not match. No manual award shortcut was added.

`FragmentKnowledgeChecks.run(GameController)` is synchronous, with `.tick` a
no-op for existing runner integration. It uses the actual original current `lbc:fragmentsword` table and the isolated
`lbc:ported/legacy_fragment_sword`17-entry legacy staging table, with native
expanded weights to select every exact current/old type,
real recipient Polymer conversion and authoritative round-trip, final knowledge
priority, and actual `CriteriaTriggers.INVENTORY_CHANGED` dispatch. It verifies
all17 current criteria accumulate across successive single-item inventories;
all17 legacy variants leave current progress empty. It restores profile,
advancements, native inventory state, cosmetic snapshots and forceWritable
scores. No direct player/effect/world ticks are used. This is prepared coverage,
not an observed runtime pass; root owns integrated compilation/execution.

## Existing ordinary acquisition routes

`LbcBolts.NORMAL_REFORGE` already routes source `sword:1` donors to
`lbc:transformables_sword` and `accessory:1` to `lbc:transformables_accessory`.
`LbcRerollCrafting` uses those same original normal tables. These routes need
native weighted checks against the actual full table; importing isolated leaves
does not create a normal route. Source pool entries below are zero-based.

|Actual original table / entry|Result|Weight / quality|
|---|---|---|
|transformables_sword /5|current Epic, cool1|10 /8|
|transformables_sword /7|current Ender|10 /9|
|transformables_sword /8|current Gravity|10 /9|
|transformables_sword /14|nested lbc:fragmentsword|10 /8|
|transformables_sword /17|current Piston|10 /0|
|transformables_accessory /0|current Sand Amulet|10 /10|
|transformables_accessory /7|nested lbc:artifact, conditional final_luck_modifier==6|400 /8|
|transformables_accessory /8|nested lbc:artifact, unconditional|10 /8|

The sword table has18 source entries, each weight10, so total180 at native
COMMAND luck0. Fragment's nested table has17 equally weighted entries, total17.
The accessory table has11 entries/raw total500, but conditional artifact entry7
does not expand unless the actual `final_luck_modifier lbc.math` equals6.
Its ordinary effective total is100 when that condition fails. Do not script an
RNG draw using raw500 without checking actual expanded native conditions.
Player Luck does not automatically become native COMMAND LootContext luck.
Existing `OrdinaryAccessoryChecks.weighted` demonstrates effective expansion,
source offsets and restoration of the native random object.

The original current artifact table has independent tutorial-book pool0 and
pool1 of exactly23 gameplay entries: mystical1x8, mystical2x6, mystical3x3,
mystical4x6. The latter pool's total is224 at native luck0, because mystical1
type5 has weight4 and all other22 have weight10. Existing LbcMagic supports
the eight current1 variants; existing LbcJumpArtifacts supports current3;
the answered incoming/reactive implementations close current4/current2 after
shared integration. This establishes source family closure, not runtime proof
that every nested normal weighted route has passed. Root should select each
native family entry through the real normal reforge and preserve its two-pool
tutorial-book result rather than flattening it into an isolated artifact leaf.

`artifact_old` has21 source entries, including six mystical_old1 variants which
are not implemented by LbcMagic's exact current1 predicates. Its old2/3/4
completion does NOT close the whole old pool. The reference at
legacy_luckyblock pool0/entry127 remains an unresolved broader legacy route.
Old Fragment is similarly referenced at legacy_luckyblock pool0/entry141;
the complete isolated old Fragment family staging table can be tested, but
that does not enable the whole legacy Lucky Block pool or mode2.

Other packaged references (`lucky_items`, `lucky_items_high_quality`, the `_all`
tables) retain unresolved wider content and cannot be declared enabled just
because they contain a completed narrow family. No catalog/normal eligibility
was invented by this review.

## Chaos boundary

Source Chaos Sword is `chaos_swo:1,imba:1` in `imba/diamondtools` pool2/entry0,
not the ordinary18-entry sword reforge table. An isolated `ported/chaos_sword`
exists, but its IMBA/diamond-tools and wider Lucky Block dependencies remain
separate unresolved acquisition work. No ordinary source weight or fake catalog
route should be substituted for it.

## Limits

No native runtime or physical client rendering claim is made in this note.
Actual normal weighted reforge, nested artifact bookkeeping/tutorial output,
source full-pool eligibility and integrated suite results belong to root's
qualification. Old whole pools, all unfinished IMBA/Lucky Block content and
unanswered independent artifact families remain unexposed by this child.

Frozen handoff SHA256:

- LbcSkills.java: `71e853b5561fd789fb2dac17b33a191f89f4ec89f02ed7bd5c418f4614e43e64`
- FragmentKnowledgeChecks.java: `a6412faaf4e92b06cdc878d97a62c59673110b155d2357d07dc4ba9ce9a6c7cc`
