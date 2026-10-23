# Four native special spawn eggs - 23 October 2026

[Source audit](NEXT_SMALL_DROPS_SOURCE.md), section "Root scope selection and schema constraints".
Authoritative source: OLD/world/datapacksdisabled/LBC/data/lbc/loot_table/imba/spawnegg.json,
entries3/6/7/8, and function/drops/specialdrops.mcfunction Shahid branch.
The full23-entry imba pool remains gated; these four have independent ready leaves.

## Source payloads and ownership

All four retain their vanilla egg visual/model, source red nonitalic translated
name, dark-purple nonitalic imba_version lore, original tier and only imba:1 outer
custom data. No RP/template changes are needed. Original RP keys were verified
read-only. Native typed ENTITY_DATA supplies the actual entity type, independently
of the egg visual: Shahid uses an item entity inside a zombie egg.

- lbc:ported/slab_spawn_egg: one persistent silent invisible baby zombie. Its
  guaranteed oak-slab helmet has item_name agressive_slab and a mainhand-only+7
  attack modifier. The ordinary slab drop's+6/custom_name/tooltip stay distinct.
- lbc:ported/low_vex_spawn_egg: one native1HP/max-health1 Vex, with the existing
  operative10% native death wrapper for the complete Vex Sword.
- lbc:ported/sarvatra_creeper_spawn_egg: one native creeper, explosion radius1,
  existing5% native death wrapper for the complete ordinary Air wand.
- lbc:ported/shahid_spawn_egg: one native unpickable item entity, PickupDelay32767,
  holding the source repeating_command_block with shahid/drop flags and drop_trap
  model. Its real shared dropped-item dispatch creates one centered baby zombie
  with a real TNT passenger, fuse200. The existing four-carrier trap is unchanged.
  Existing author-approved complete melee suppression and WILD TNT policy apply.

LbcSpecialEggs extends native SpawnEggItem and implements PolymerItem. It delegates
placement, native finalization, fluid use, entity-data application and consumption
to vanilla. Item.Properties.effectiveModel must return the vanilla proxy model:
the engine overwrites a manually supplied ITEM_MODEL during finalization. The
first retained runtime attempt caught that registration issue; it was fixed.

SpecialEggMixin appends adoption to EntityType's native post-spawn processor,
after original ENTITY_DATA and before world insertion. Existing creature, slab,
creeper and zombie-TNT services own lifecycle and causal author; no second tick
loop or mob framework was added. A narrowly scoped adoptEgg method preserves
each native payload. Shahid's original nested SNBT custom-data string is converted
to an actual native compound. Its thrower is installed before dispatcher admission.
Native reflected TNT/children retain existing ownership precedence and cleanup.

Hand use requires an active participant in the arena, action permission and
unpaused time. Both clicked and prospective destination chunks must be resident;
an invalid remote click never loads a chunk or spends an egg. There is no mana
or antimagic cost. Entity interaction guards the native matching-mob path too.
No additional entity lifetime, cap or new movement physics was introduced.

## Native interaction boundaries

Author confirmed the correction (23 October2026, user):
"Да, сохранять особые параметры яйца". Matching-mob interaction must retain
the egg's special parameters, instead of producing an ordinary unconfigured baby.
SpecialEggMixin.Offspring now appends the same native default-stack processor
used for block/fluid placement before child insertion. This applies ENTITY_DATA
and the existing causal/lifecycle adoption. Vanilla still selects the child,
checks matching type and baby support, places it, inserts it and consumes one egg.
The original parent is untouched. Ordinary vanilla eggs bypass this correction.
The helper also validates special-egg role/time admission, including direct calls.

Dispenser placement and spawner editing remain native. Direct stack-based
dispenser spawning executes the same adoption processor with no player cause.
Vanilla spawner editing by nonoperators stores only the entity type; operators
may store ENTITY_DATA. Subsequent spawner-generated actors do not run the egg's
stack processor and are not claimed to inherit the configuring player's cause.
No custom spawner ownership system or change to these vanilla permissions is
part of this patch. Initial qualification below covers hand/block/fluid use;
the matching-mob follow-up separately covers offspring. Dispenser/spawner
end-to-end execution remains unqualified.

## Initial qualification

Final build77ae0b9a34a34970029f684d68307f4ab24b2ff807f48bff1e183f0a3dfe5797
passed16 suite results in two actual isolated mode1 matches (60.4s). All258 Java
sources,705 classes and2723 resources match tested artifacts exactly. Build/install
details:build/verification/special-eggs-final/qualification.json, installation.json,
special-eggs-results.json and special-eggs.log. Installed to stopped main; agent
did not start it. Previous43634ed9 and intermediate8338279b JARs are retained
under build/verification/main-rollback/. Three user sources stayed byte-identical.
The intermediate build passed the same16 suites; the final build also spells
out registration/tier identifiers in full per the user's code-search convention.
The isolated runner uses two actual mode1 matches and16 recorded suite results:
source item models/components, native block-face/offhand/water use, exact payloads,
actual seeded Vex/creeper death rewards, Shahid intermediary and single carrier,
no-melee/WILD policy, three real paused world ticks, observer/unresident/old-epoch
guards, existing creature/zombie-TNT/Vex/KFC regressions and actual stop cleanup.
The full LuckyWars mode2 loop is not qualified by source fixtures inside mode1.
The known fake-player Polymer packet-context error pair is retained separately.
No real-client pixels, actual disk restart/unload/disconnect, full natural AI
trajectories, dispenser/spawner/offspring execution or peak-capacity claim.

## Matching-mob follow-up (23 October2026, user)

The author approved retaining special parameters on matching-mob use. The actual
native Mob.interact path is now tested in both hands: one egg is consumed, one
configured slab child appears at the original parent, its native UUID remains
distinct, and its source silence/invisibility/persistence/item_name/+7 helmet,
drop chance and causal author match block use. The parent remains unchanged.
Actual player kills produce exactly one complete native slab reward. The initial
retained fixture used generic environmental damage, which does not satisfy the
native equipment-drop player-kill requirement at chance1.0; the fixture now uses
actual player-attributed damage. Production drop chance/loot rules were unchanged.

Ordinary vanilla zombie eggs still create plain babies, mismatched eggs remain
free misses, and matching-mob use cannot bypass Time Stop or observer admission.
The same existing16-suite runner repeats block/fluid/offhand/native reward,
single Shahid, creature/zombie/Vex/KFC and actual-stop regressions in two matches.
Final evidence is under build/verification/special-eggs-offspring-final/.
Build74a3cd835f02ce0be1b0bd5873820cafab946d9fa06e6a145e45b0dcae17697e
passed16 suite results in two actual mode1 matches (50.43s). All258 Java sources,
706 classes and2723 resources match the tested artifacts exactly. Evidence files:
qualification.json, installation.json, special-eggs-results.json and special-eggs.log.
Installed to stopped main; rollback77ae0b9a retained, agent did not start main.
Three preexisting user sources remained byte-identical and outside the commit.
The known fake-player Polymer context error pair remains; no final suite failed.
Dispenser/spawner operation, actual disk restart/unload/disconnect, real-client
pixels, full natural trajectories and peak capacity remain outside qualification.
