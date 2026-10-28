# Complete Flesh armor

Source closure: [FLESH_ARMOR_SOURCE_2026_10_28.md](FLESH_ARMOR_SOURCE_2026_10_28.md).

## Author decisions (28 October 2026)

Jammer policy:

> Ничем — это физическая броня

Full-set multiplier:

> Удваивать, как в описании

These supersede the literal source amount2 (x3). Use amount1
ADD_MULTIPLIED_TOTAL under the original fixed identifier
`minecraft:44a8aa48-d53d-42c0-b229-4557d3c581b1`. Each original equipped piece
still contributes its unchanged native MAX_HEALTH ADD_VALUE amount: head8,
chest16, legs12, feet6. With vanilla base20, the partial sum62 becomes124
for a full set. There is no heal, proportional HP refill or health timer.

## Physical passive and acquisition

`LbcFleshArmor` joins the existing armor owner, equipment notifications, shared
role/dimension eligibility, entity-load callback and stop lifecycle. Four actual
equipped stacks must carry raw `flesh:1`; carried/held items do not substitute
for an armor slot. Base item/model and extra slot flags do not define runtime
identity. MAG, TECH and personal mana suppression do not suppress this physical
attribute. A paused world retains its physical equipment maximum.

The original bonus remains a native permanent modifier, as with the source
`attribute modifier add` command. Repeated validation never removes/readds an
unchanged modifier, heals current HP, or refreshes effect clocks. Native attribute
callbacks own current-health clamping, packets and shadow absorption reconciliation.
Only this exact identifier is changed; other equipment, attributes, Health Boost
and signed absorption keep their own owners. Replacing an old x3 modifier is
atomic through a native detached AttributeInstance copy: the live attribute never
briefly drops to the unmultiplied maximum and discards otherwise valid HP.

Disconnect forget does not remove the saved physical bonus or reduce HP before
saving. Actual current equipment is revalidated on load; stop removes only the
set's identifier from online players. No private HP state or duplicate effect
engine is introduced.

All original four leaves/root/five-draw pre/four-piece full tables become public
ready entries. Pre/full packaging is explicit. Existing normal slot-reforge and
Ultra family-reforge acquisition remain authoritative; unrelated whole Lucky
Block pools stay gated. Keep original durability6000, leather bases, all modifier
IDs/components, red translated names, aqua lore, tier_C tooltip, models/equipment
asset and original Flesh PvP advancement. Existing RP descriptions already promise
the selected +100% bonus. No RP change or replacement template is needed.

## Shared native equipment load order

Native LivingEntity reads permanent attributes and then saved Health before its
equipment. Transient equipment HP modifiers are absent from the attribute save;
Player also loads the selected inventory item after this call. A full124HP Flesh
player saved at110HP could therefore be clamped to40 before equipment restored.
The same ordering affects ordinary equipment that grants HP, not just this set.

`EquipmentHealthLoadMixin` installs encoded equipment and the selected player
inventory before the original native setHealth call. The ordinary native equipment
engine applies modifiers and captures its normal cache; later vanilla decoding
produces equivalent stacks, preventing a first-tick remove/readd clamp. Flesh's
saved set identifier is reconciled from actual loaded armor before that one health
validation. Existing ShadowAbsorption loading guards and native permanent effect
modifiers remain authoritative. This changes load order; it never restores lost
HP afterward or replays healing.

The strict native round-trip with signed absorption exposed a second load bug:
ShadowAbsorption.load used forceAddEffect, which invokes the player's packet
callback while ServerPlayer.connection is still null before login. During this
specific preconnection load phase, install the canonical absorption carrier in
the native active-effect map directly, as native effect decoding does. The ledger
still reconciles normally; normal join synchronization sends it later. Connected
loads keep the existing callback path. No duration, healing or shadow-balance
formula changes.

## Verification boundaries

Scoped runner: `tools/smoke_flesh_armor.py`, with adjacent Berserk/Glass/Nanobots,
armor sets/Shulker/incoming artifacts, attributes, signed effects/layers and Time
Stop. Native acquisition/payload checks use actual consumed-donor normal/Ultra
reforges, complete/partial/carried/raw-flag equipment, idempotence at110HP,
atomic x3 migration, independent modifiers/effect/shadow clocks, native equipment
changes, jammer fields, roles, original advancement, forget/load/stop and in-memory
native serialization. Additional controls cover Health Boost plus shadow absorption,
an ordinary selected-mainhand HP item with no Flesh, and ordinary mob HP armor.
Loaded HP is asserted immediately after native reading and again after the first
equipment update, without a fixture setHealth repair.

No physical-client pixels, real socket reconnect/death/respawn or peakTPS claim.
Exact artifact, native qualification, baseline and stopped-only install receipt:
`build/verification/flesh-armor/`.

The first native batch `a0c31f64` stopped the new Flesh fixture at its immediate
post-equipment HP-clamp assertion. Native AttributeMap queues dirty attributes;
LivingEntity.refreshDirtyAttributes applies the health callback afterward.
The fixture had executed detectEquipmentUpdates alone. Run both ordinary native
equipment and MAX_HEALTH callback phases before asserting the resulting HP. Production and native health behavior
stay unchanged; initial log/results/proof are retained as `initial-a0c31f64-*`.

The next `1afbffc6` batch reached the additional signed-absorption round-trip and
failed on the actual preconnection packet callback above. Its in-memory duplicate
also temporarily borrows the real player's native cached PlayerAdvancements;
restore that receiver in each probe's finally block, including exceptional exits.
The missing fixture cleanup caused downstream neighboring checks and lobby stop
to fail; the runner still stopped its owned child normally and restored exact
fixture snapshots. Preserve this rejected batch under `initial-1afbffc6-*`.

Batch `4036a16d` passed Flesh, including every native load control, then exposed
a second fixture issue: executing ALL dirty callbacks on an unpublished duplicate
registered it as a waypoint receiver with no connection. The child reported a
native waypoint exception and exited itself; the runner rejected the incomplete
qualification and restored its exact snapshots. Invoke only native
onAttributeUpdated(MAX_HEALTH) for these bounded memory probes; real registered
players continue all ordinary callbacks on subsequent server ticks. Gameplay
bytecode is unchanged by this correction. Keep rejected log/results/proof under
`initial-4036a16d-*`; do not represent its zero exit code as a successful run.

Final immutable build `f6969660` passed32recorded results in2actual native
matches (102.36seconds), including standalone shadow-absorption. Child shutdown
was normal and exact fixture snapshots restored; final log has no fatal/crash
signatures.557sources/1581classes/3238resources and build.gradle match the JAR;
198author paths and29live loot overrides remain exact. The final correction
after4036a16d changes only the new fixture source/class; gameplay is identical.
Main installation uses the existing stopped-only worker and never forces
shutdown/restart. Consult main-install.json for the actual installation receipt.
