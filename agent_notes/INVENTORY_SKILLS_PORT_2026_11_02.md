# Three native inventory skills — 2 November 2026

User decisions: skills4,8,11 are unaffected by magic, technical and strong
artifact suppressors. Preserve their ordinary achievement unlocks. Parallel
Streams copies all hand modifiers in both directions; latest balance decision
is **70% reduction of negative offhand attack-speed penalties**, retaining30%.
Earlier50%/90% proposals are superseded. Two normal swords yield
`4 - 2.4 - 0.72 = 0.88 attacks/second` with unchanged native base speed.

## Source and actual access

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/swrg_kit_integration/game/skills/inventory_changed.mcfunction` dispatches
Tanner4, armor-enchantment8 and offhand11 helpers. The native catalog now adds:

| ID / source slot | Skill | Existing achievement gate |
| --- | --- | --- |
|4 / page10000 slot3 | Eternal Rock | `lbc:true_advancements/arcanums/tannerkivi` |
|8 / page10000 slot7 | Not RandChanted | `lbc:true_advancements/armor/randchanted` |
|11 / page10000 slot10 | Parallel Streams | `lbc:true_advancements/armor/techno_full` |

Live GameMenus uses OriginalGuiDesign by **source slot**, not runtime ID. Existing
slots3/7/10 already use correct `lbcskill.3/.7/.10` names/lore in the author's RP.
Do not replace these with unrelated raw-ID4/8/11 GUI loot icons or remove unlock
checks. No resource-pack change or duplicate translation key is required.

## Item transformations

Eternal Rock examines four worn slots. Existing DAMAGE-bearing armor becomes
unbreakable, and each unmarked worn item gains exactly +3 slot-specific armor
toughness. Correct the old swapped chest/legs guard. Effective native attribute
defaults are retained, including ordinary armor values; all other components,
damage, enchantments, names, lore and stack identity/count survive. Preserve
source `tanner_upgraded` presence semantics. Existing conflicting custom modifier
IDs are retained and the added ID is disambiguated.

Not RandChanted adds one level to the source ten entries: blast protection,
feather falling, fire protection, projectile protection, protection, respiration,
soul speed, swift sneak, thorns, unbreaking. Absent entries become1 even when
normal enchant compatibility would forbid them. Only ENCHANTMENTS is upgraded;
stored enchantments and unrelated components remain. Use native positive-int
storage policy, saturating at INT_MAX rather than overflowing old NBT bytes.
`armory_upgraded` is checked by presence and prevents repeated application.

Erosion's suppressed Protection is canonical in its saved stack record. Skill8
increments that saved level and skips visible Protection until ordinary erosion
recovery outside antimagic. It neither cancels suppression nor loses the upgrade
when the former Protection returns. The existing suppression owner validates
the item/family identity and owns this helper/schema.

Parallel Streams examines both held item identities. Preserve original entries
and copy all exact MAINHAND entries to OFFHAND, and exact OFFHAND entries to
MAINHAND, reading only the untouched input list. Retain every operation, negative
or positive coefficient, native attribute holder and Entry.display metadata.
Copy IDs are deterministic, distinct from source IDs and disambiguated against
all authored IDs, so vanilla main/offhand weapon values can coexist. Native
effective defaults replace old manual vanilla-tool attribute tables and cover
current materials/weapons. Any explicit custom attribute carrier qualifies;
the source had no weapon-only whitelist for explicit components.

Only negative attack-speed coefficients applying in OFFHAND are multiplied by
0.3. MAINHAND keeps the full original coefficient, including OFFHAND-to-MAINHAND
copies taken before weakening. Negative HAND/ANY attack-speed entries are split
over exact native slots: original full value/ID outside offhand, a fresh ID and
30% value in offhand. Other shared-slot entries, positive/zero speeds, other
attributes, displays and slot coverage remain. Enumerate current native slots,
including BODY/SADDLE. `offhand_mastered` prevents feedback copies or repeated
penalty reduction. These source item upgrades remain on traded items and after
deselection; changing the skill does not erase someone else's item properties.

## Admission, lifecycle and performance

LbcLoadouts owns catalog/selection and active-player admission. Existing native
InventoryChangeTrigger and onEquipItem events call it. Match start/respawn and
debug selection also reach the same implementation. Check current config,
unlocks, role, ordinary dimension and live match. No suppressor check is added.
Debug in-match selection still requires a genuine operator with an actual debug
GUI open; a copied tag alone is not sufficient.

Ordinary idle ticks never scan inventories. Transformations read four worn or
two held references; marker presence uses the existing read-only CustomData
mixin without copying NBT. Changed items are marked dirty only on real mutation.
Native equipment synchronization owns the actual attribute application. A
synchronous reentrancy guard prevents repeated inventory callbacks. Only real
events deferred by Time Stop enter a deduplicated UUID set; resume resolves the
current body and current selection, cleanup removes pending work. There is no
new projectile/world job engine, serialized shadow inventory or item cache.

The prior scalar reader gains a read-only `contains` operation; mutable NBT never
escapes. Full compound copies remain only for actual component mutations and
the erosion record update, not per-frame marker inspection.

## Verification and retained failures

Evidence/qualified artifacts belong under
`build/verification/inventory-skills-2026-11-02/`. The first compilation corrected
the26.3 `advancements.triggers.CriteriaTriggers` package in the native fixture.
Initial native9acf6da8 passed item transformations, live jammer positive controls,
native equipment sums and erosion77→78 recovery, then rejected the fixture's
attempted in-game selection with only an operator/tag. Fix the test to open the
actual operator debug skill menu and click its native buttons; do not weaken
runtime menu authorization. Its failure log is retained.

Final test-server artifact `test-7968842a.jar`, SHA-256
`7968842ab3871414cc4aaa1407352381b0b75a9ef413981350203019408d77e5`;
production `production-f007d6eb.jar`, SHA-256
`f007d6ebf0cdd16680888d3981173fa6dd33584045c1c9bdb3030c6ab7cdd375`.
Shared entries match exactly; production omits all8map editing classes. This
installation uses the test-server build.

Three native groups passed in3actual mode1 matches: inventory-skills,
combat-feedback and isolated lbc-loadout. Coverage includes original component
preservation/defaults/display metadata, per-slot guards/native attribute sums,
INT_MAX enchantment saturation,43custom hand entries and collision handling,
HAND/ANY×3operations across every current slot, both held identities and real
dual-sword0.88, actual inventory/equipment callbacks under MAGIC/TECH fields,
source advancement-lock positive/negative controls, real debug-menu callbacks,
erosion77→78 saved recovery, Time Stop defer/resume and global skill-disable.

9794frozen inputs,2476compiled classes and7353processed resources exactly match
the selected JAR;238author paths/31live grimoire overrides unchanged. Native
children stopped normally, exact player/config/OP/level/ticket snapshots restored,
no retained locks, fixture25577only. Existing applied/deleted RP/template files
remain untouched. Qualification and normal-stop installation receipt live beside
the artifacts. No main stop/restart, remote push, clientFPS or peakTPS claim.

Receipt confirms main `7968842a` installed while stopped, preserving previous
`b7f05f27` for rollback; `restarted:false`.
