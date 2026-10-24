# Eternal Life and Absolute Barrier native contract

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Implementation: [LbcDefensiveArcanums.java](../src/main/java/me/skywars2/game/LbcDefensiveArcanums.java).
This records implementation; compilation and native qualification belong to the integrating batch and are not claimed here.

## Author rulings, 24 October 2026 (user)

Barrier costs500% of the holder's regeneration per tick. Two held barriers do not add another charge. Remove the100-mana activation fee: that was a datapack workaround for exploiting the one-second Resistance duration. Remove Resistance entirely; provide direct immunity while holding a paid barrier to all damage except genuine void. Existing unrelated Resistance remains untouched. Two barriers retain the source terrain box and sneaking floor extension. Check actual current HP before each healing hand; mainhand filling health prevents offhand healing and cost.

## Source and presentation

`function/items/arcanums/select.mcfunction:4,13-14` and `select2.mcfunction:4,13-14` dispatch main then off. Their handoff conditions and `end.mcfunction` / `end2.mcfunction` clear their respective source latches. Barrier payment is now shared under the author ruling; healing stays independent.

`loot_table/transformables_arcanum.json:125-288` defines repeating command block proxies, gold nonitalic translated names, tier A tooltips, and flags `transformable`, `arcanum`, `arcanumt`, plus each item flag. Models remain `lbc:arcanumhealing` and `lbc:arcanumbarrier`. Native Items retain proxies/models/styles/flags; shared loot decoding owns complete names/lore. The obsolete source100-mana activation lore line is removed by the integrating batch's scoped loot.

Existing `healing_arcanum`, `barrier_arcanum`, and `.lore.1` / `.lore.2` keys were read in RU and EN from the author's read-only resource pack at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. No translations, resource-pack edits or template additions are introduced.

## Healing

`function/config.mcfunction:7,74-75` supplies500 mana per attempt /10000 per second for lore. `function/items/arcanums/healing.mcfunction:1-5` grants the original heal advancement for health0..1, applies Instant Health I, charges mana, emits five hearts spread2 and plays experience pickup volume1/pitch1.

Native code invokes Instant Health I immediately (ordinary players heal4HP), retaining native inverted-healing and effect guards. It checks actual missing HP before each main/off attempt and awards the original criterion only at HP + Absorption<=1. Legacy load.mcfunction:23 defines hplvl as the native health objective; native ServerPlayer updates that objective with ceil(Health + Absorption). Healing admission itself uses fresh real HP under the user ruling. Healing leaves existing direct/indirect damage ownership intact.

## Barrier payment and immunity

Legacy `barrier{,2}.mcfunction`, `barrier_spawn{,2}.mcfunction` and `barrier_tick.mcfunction` used100 activation, separate hand charges, Resistance V20ticks and stale global admission. These payment/defense details are superseded by the author ruling. Config scores40,800/sec and100 remain historical/source initialization values; they do not determine current cost or immunity.

`barrierPrice(player)` computes500% of current native visible regeneration once per held-barrier player dispatch. Zero or negative regeneration costs0; it never grants mana. Unrepresentable charges are rejected. Balance must fund the full charge without overdraft. Exact-balance payment grants its paid frame at remaining0; next failed dispatch removes state.

`blocksDamage(LivingEntity,DamageSource)` is the native pre-hurt gate. It requires a live eligible player in the correct level, a barrier still actually held, successful payment this or the preceding server tick, and no antimagic or personal noMagic. The preceding tick covers damage before the END-tick service. Immediate unequip stops immunity at the next damage invocation. `DamageTypes.FELL_OUT_OF_WORLD` is the genuine void exception; other nonvoid damage is blocked under the author's ruling. No Resistance is added, refreshed or removed; there is no effect suffix.

## Terrain and ownership

`barrier_tick.mcfunction:2-3` clears3x3x3 at feet y0..2 with two barriers, plus3x3 floor y-1 while sneaking. Native code performs one synchronous <=36-block strict deletion per paid frame, without drops, container side effects, neighbor updates, queue or new actor. Only resident chunks are touched. Six source electric-spark faces remain, ten particles each, once per paid frame.

Existing `trapChanges` and `withSupportCandidates` track support/terrain consequences. A shared `LbcIndirectKills.Owner` is captured through `coldCause` at first successful paid acquisition and retained through continuous holding, including addition of a second hand. Upkeep cannot supersede a later real player hit by refreshing influence order. Both hands reuse this cause for the single terrain operation.

## Lifecycle and verification boundary

Only active participants dispatch. Actual dimension/per-entity Time Stop and frost pause upkeep; payment freshness expires naturally during a pause. Antimagic blocks upkeep and impact-time immunity. Personal noMagic clears state. Unequip is observed while paused. Failed payment, forget, disconnect, death, dimension handoff and match stop clear state. State is unsaved. No cooldown, attribute cap, added debuff or packet pipeline is introduced.

Integration fixtures should verify both healing hands/current HP, single/double charge, exact/insufficient mana, nonvoid immunity/native void damage, immediate unequip, unrelated Resistance preservation, strict terrain/container deletion, support credit/later-hit priority, suppression, actual Time Stop/frost and lifecycle. No real-client pixels, actual disk restart/chunk reload or peak-load performance claim follows from this contract alone.

## Qualification

Qualified build `daa2f866` passed 56 recorded suite results in two actual matches (147.39s). Exact 301 sources / 821 classes / 2792 resources match the tested artifact. Installed stopped main; rollback `e9548260`, main not started. Includes manual native dispatch boundaries, actual damage and support deaths, actual stop cleanup; no client pixels, actual disk restart/unload/disconnect or peak-load claim. Evidence: `build/verification/defensive-arcanums-final/`.
