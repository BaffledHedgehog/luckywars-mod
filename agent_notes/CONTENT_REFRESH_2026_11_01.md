# Vanilla content catalog refresh through26.3,1 November2026

User requests: complete the infinite shulker's current vanilla items, add every
new Mob to Mobgun, and inspect other stale exhaustive lists since1.21.11.
Author explicitly classifies sulfur cube as magical, like slime/magma cube.
Actual RP and template stay untouched.

## Exact sources and additions

Both versions were compared using local vanilla data generators, not guessed
release notes. `CONTENT_REGISTRY_DELTA_2026_11_01.md` and the ignored verification
registry-delta.json record source JAR hashes, exact bundled baseline libraries,
commands and full added IDs. Data generation used net.minecraft.data.Main
--reports; neither main server nor test server was started for that audit.

Item registry1505->1658:153 new IDs, no removals. Original infinity graph has
1503distinct vanilla bases in1816entries. Missing usable IDs are all153new items
plus the older stripped_bamboo_block omission. Air remains excluded because it
is an empty stack. Existing command/operator/test items and17shulker bases remain.

All154new rewards are direct vanilla item entries of weight1 appended to the
existing infinity_no_shulker table, as requested by the author. No154 separate
singleton resources are created. Original1816entries and26rolls
remain exactly structurally identical, with all original functions/counts/weights
and potion/book variants retained. Final1970entries cover all1657non-air vanilla
item IDs. Standard/upgraded shulkers both use that existing common source; lucky
shulker distribution is unchanged. No duplicated custom item payloads or new
reward engine. `tools/refresh_infinity_catalog.py` derives missing IDs from a
native report, defaults to a read-only check, and requires --apply to append
direct entries. Packaged content_catalog_refresh.json pins registry expectation
and native verification vectors. This is vanilla base-ID coverage, not every
possible component combination (for example every map destination).

Native ENTITY_TYPE157->161 adds cushion, poplar_boat, poplar_chest_boat and
sulfur_cube. Only sulfur_cube is a Mob subclass missing from Mobgun. Mannequin,
armor stand/player, furniture and boat variants are not new Mob types. Outcome115
is appended with nativeFormat/offset1/source tags; all113previous direct presets
and89potion presets remain structurally exact. Uniform selection now naturally
includes115choices including the original potion branch75. Luck7 still forces22.
Preparation validates duplicates/contiguous IDs/registered types and derives
bounds from the actual catalog, removing hardcoded113/114/89 runtime limits.
DFU remains on the shared map preparation worker, not the gameplay tick.

The second live random-Mob producer used by Flesh of Lust also appends sulfur
cube at91; random selection derives ENTRIES.size(). Its original90entries and
special luck7 index22 are retained. General mobs/monsters/magic_mobs tags gain
sulfur cube. The source-only mobs_and_markers tag inherits mobs but is not a
current packaged consumer. This repairs existing targeting as
well as generation. The previously unshipped magic_mobs tag is now packaged.
Author explicitly approved the WHOLE list for Nyauwu: its enemy pulse accepts
either this type membership or the existing magical-actor/summon classification.
Natural slime, magma cube and sulfur cube therefore qualify without source tags.
This is local to Nyauwu; LbcMagic.magical and global purge rules are unchanged.
It does not mark natural cubes as temporary magical summons.

## Other complete-list audit

`COMPLETE_CONTENT_LISTS_AUDIT_2026_11_01.md` distinguishes live exhaustive lists
from authored probability/balance sets. PhD BIO gains58new items using actual
previous analog membership: poplar's admitted wood forms/leaves, admitted wool
colors' slabs/stairs/cushions, straw bed, golden dandelion, red shrub, shelf mushroom
and sulfur cube egg. Poplar shelf is admitted through the existing #shelf parent.
Old BIO exclusions are preserved: not all colored wool was accepted, and old
buckets/chest boats/maps are not an exhaustive accepted family. Sulfur minerals
are not made biological automatically. The existing PhD hashing, reusable catalyst,
144outcomes, ordinary result slot and suppression policy are unchanged.

Five new attributes are already supported by AttributePolicy/AttributeHud/native
snapshot registry iteration. Potion46/effect40 registries and43enchantment keys
are unchanged. Trim, banner, painting, instrument and visual animal variant keys
also remain current. Newly added biome/particle/sound/archetype registries are
recorded in the delta; registry-based native consumers already discover these.
The18potion-spawner,84/28Wiki,21quiver and authored rune/reforge lists are chosen
mechanics with explicit amounts/weights, not missing-all-content enumerations;
they are not silently expanded. Deferred TMT remains separate future work.

## Verification and deployment

Evidence root: build/verification/content-refresh-2026-11-01/.
Static preservation proves the1816original infinity entries and all old Mobgun
presets exactly unchanged. New native checks compare all1657current item IDs,
decode and roll every actual appended entry through the native loot codec,
roll the full packaged parent, validate native tags/BIO, summon the appended
random Mob, and take a real PhD result from new ingredients using an independently
computed source hash. Mobgun coverage constructs unadded prototypes only in the
explicit check to identify true Mob subclasses, then actually launches each type,
checking AI/velocity/owner/epoch/no-save/cleanup. No registry scan or prototypes
are introduced into normal firing. Actual successful Nyauwu casts also check
the three natural cube types, existing summoned actors, ordinary-mob exclusion,
ally healing, wearer attribution and target-side antimagical protection.

Final qualified artifact: cb3fe94291b9dbdde1be584fad614872767180fcbf91ca941764382c640ed1f0.
Eight scoped native groups pass: content-catalog, mobgun-catalog, mobgun,
infinite-shulker, phd-crafting, witch-lust, mage-nyauwu and vanity-rollback.
Seven independently successful groups are retained from8c36ce77; its failed
content-catalog group is excluded. Final artifact differs ONLY in that check
class, removing an incorrect requirement for the unused/unshipped
mobs_and_markers tag. The focused final content-catalog rerun passes all154
actual entries and the new-ingredient craft. qualification.json records exact
cross-artifact identity and scope, not a wholesale PASS for the failed run.

Retained earlier failures: b7347685 exposed unshipped magic_mobs and a fixture
assumption that Shulker is a root (source outcome48 carries it as a passenger).
Both are fixed and native-tested. No production relaxation of targeting,
cooldown or source payloads was needed for those fixture corrections.

9773frozen inputs,2437compiled classes and7352processed resources exactly match
the qualified artifact;226author paths and31live grimoire overrides remain exact.
Original1816infinity entries,113direct Mobgun presets,89potion presets and90
general random-Mob entries are preserved. All child servers stopped normally,
player snapshots and extra level/ticket files restored, no retained locks.
Fixture25577 only. No physical client-render, peakTPS or arbitrary-map claim.

Deployment is queued through tools/install_when_stopped.py, receipt
main-install.json: waiting-for-normal-shutdown. Current main remains970eef84
until a normal stop; the worker has not stopped or restarted it. No push.
