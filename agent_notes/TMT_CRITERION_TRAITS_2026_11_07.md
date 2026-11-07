# TMT source labels and native criteria — 7 November 2026

Component verified internally; not installed. Full TMT remains unreleased.

The original active LBC source writes harpoon1 and sea_legend1 in
function/drops/tmt/set_storage_nbttags.mcfunction268/641. Its write_random_nbttag
and set_nbttag path changes custom_data only. drops/tmt_at chooses the same five
carriers; it does not turn them into tridents or copy a trident's enchantments.
Native Harpoon/SeaLegend inherit vanilla TridentItem. The original flags have
real observers in advancement/true_advancements/tridents/{harpoon,sea_legend}:
player-kills-player with mainhand transformable1/trident1/harpoon1, and using-item
with transformable1/trident1/sea_legend1 excluding spectators. Neither predicate
requires a trident item ID. AdvancementFilterMixin retains these definitions;
the TMT generator supplies both prerequisite category fields.

The catalog now preserves these two provenance families with their true KILL/
MAIN_HAND and USING/USED_HAND domains. Native criteria remain the event owners;
no charge, throw, Riptide, enchantment transfer, callback loop or scheduler was
invented. Ordinary native tridents and the separate random-enchantment generator
remain intact. Earlier proposals inferred new charge machinery from the item
IDs; literal reader evidence supersedes that inference.

The authored aimbob minecraft:trident pending row was actually slow_falling1,
not its RiptideXV. This scalar item field has only source writers (aimbob480,
set_storage_nbttags667), with no reader in either legacy namespace or native
item-data consumers. It is explicitly excluded as inert. Actual slow-falling
effects/potions/pills are separate. Bucket-head and Oguzok sword are classified
as ordinary equipment/weapon components with no additional item-data ability;
the real thrown bucket, Chef and their physical outputs stay unchanged.

TmtCriterionTraitChecks exercises actual loaded native criterion predicates:
all five Harpoon carriers, mainhand/offhand/plain/nonplayer cases. The kill
criterion is triggered at its native boundary; no actual player death is claimed.
SeaLegend uses real native bow+arrow and apple holding ticks in both hands,
with plain and spectator negatives. A labelled carrot starts no trident charge,
spin, projectile or borrowed enchantments. Full private payloads/scopes and prior
criterion progress are checked/restored. The physical bucket is genuinely equipped
with armor3/durability200; Oguzok's held component/effective attributes are
compared with a native wooden sword. These are classification/provenance tests,
not replacement weapon engines.

Internal test db00bbe5 and production3bd7dcfe carry full6a564a55/984ff6bd.
Nine suites pass: tmt-criterion-traits, tmt-catalog, tmt-properties, tmt-carriers,
tmt-native-use, tmt-self, native-gear, uniform-item-pools, typed-uniform-item-pools.
Exact hashes and native PASS/normal shutdown/exact restoration/no failures,
cleanup errors, live PID or retained locks are sealed in
build/verification/tmt-criterion-traits-2026-11-07/component-verification.json.

Three Java families, zero resources;10471 other inputs and282 author paths
preserved. Main/libs/author RP/HUD/four protected WIP sources unchanged.
One512MiB compiler then one2GiB fixture ran sequentially and exited.
Fresh966families/1830variants/92pending rows is reconciliation, not completion.
Remaining real owners/domains/acquisition/atlas/coherent release are still needed.

Next literal audit identifies three genuine consumers: Energy shield uses
LbcElectro.tickShields during native blocking; electroblocker uses the existing
dirty inventory cache; Halloween mace uses the existing shared SELF/JUMP owner.
Energy is a SimplePolymerItem with BLOCKS_ATTACKS, so its native Item.use already
starts blocking. Broaden the existing shield admission rather than impersonating
ShieldItem or creating another blocker. Ordinary IMBA elytra/rocket/Quick-Charge
crossbow variants require component-only classification separately; their base
native classes alone are not evidence of additional legacy TMT abilities.
