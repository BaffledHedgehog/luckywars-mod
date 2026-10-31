# Multiplier tablets source audit — 1 November 2026

Read-only bounded audit. Only this note written; no build/tests/source/RP/server/git/PENDING changes. Read project AGENTS.md and agent_notes/AGENTS.md, RESOURCE_PACK.md. Evidence below is source behavior, not a claim of live runtime qualification.

## Roots and actual missing production

Legacy D = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc.
Paper P = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java.
New N = E:/servers_mew/skywars2_last/luckywars-mod.
RP = C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8.

Exact production search `rg -n 'tablets2|tablets3|tablets4|tablets5|multiply_[2345]|multiply_select' src --glob '!**/*Checks*'` produced no matches. Broader multiply finds ordinary vector math and armor reforge, not tablets. Source-paper/ignite search found tablet handling only in Paper; no Ignite tablet override found. This is a bounded static absence result, not runtime testing.

## Actual source precedence: Paper changes x2/x3/x4

P:1168 registers override for lbc:swrg_kit_integration/items/multiply_select. P:3988-3990 calls native select from native working-item tick. P:4017-4022 tags multiply2/3/4 working/multiply_item. P:9686-9705 implements actual native selection and mutation:

* Wait for nearest other item at Euclidean radius <=1. P:10121 onward queries nearby cube then distanceSquared <=1. Ties favor later iteration because <=. Excludes any item carrying custom-data key multiply, including x5 and unknown values; no owner/thrower/ground/age/pickup-delay filter.
* x2: clone whole target stack, ensure source vanilla defaults, scale every attribute entry amount exactly *2. All operations, positive/negative values, IDs/slots retained; no explicit cap or decimal rounding. P:10035-10108. Unlike datapack, no first24 limit.
* x3: target count clamp to [1,127] of count*3, regardless normal item max-stack. P:9697-9699. No splitting/remainder. P source uses int multiplication before clamp (extreme int overflow theoretical).
* x4: direct item.getEnchantments only, each existing enchant unsafe level*4, no explicit normal-level cap; no stored-book enchant handling. P:10110-10119. Includes curses; all registered direct enchants, not datapack hardcoded list.
* Entire multiplier item entity removed once any eligible target exists, including target no-op (plain item x2, unenchanted x4, already127 x3). No shrinking one. Whole target stack is mutated; multiplier stack count never scales effect. If target absent tablet waits.

Defaults P:466-524 and 10035-10089: armor helmet/chest/legs/boots armor values netherite/diamond 3/8/6/3, iron2/6/5/2, gold2/5/3/1, chain2/5/4/1, leather1/3/2/1, turtle2; diamond toughness2, netherite toughness3 and KB resistance.1. Tools damage/speed pairs by axe/shovel/hoe/pick/sword: wood (6,-3.2)/(1.5,-3)/(0,-3)/(1,-2.8)/(3,-2.4); stone (8,-3.2)/(2.5,-3)/(0,-2)/(2,-2.8)/(4,-2.4); iron (8,-3.1)/(3.5,-3)/(0,-1)/(3,-2.8)/(5,-2.4); gold (6,-3)/(1.5,-3)/(0,-3)/(1,-2.8)/(3,-2.4); diamond (8,-3)/(4.5,-3)/(0,0)/(4,-2.8)/(6,-2.4); netherite (9,-3)/(5.5,-3)/(0,0)/(5,-2.8)/(7,-2.4). Trident6,-2.9; mace6,-3.4. Existing attribute metadata prevents default insertion; defaults use Paper-generated luckywars keys/mainhand or armor slot. These include legacy quirks, especially trident6 and negative attack speeds becoming more negative.

## Raw datapack differences (do not accidentally port instead of override)

D/function/items_tick.mcfunction:1-2 tag untagged then process working; items_tick_tag:15-17 tags multiply2/3/4. items_tick_act:8 dispatches only with non-multiply_item within1. multiply_select:1-3 selects nearest non-multiply_item and :4 kills whole tablet. Raw selector may target dropped x5 (not multiply_item), unlike Paper.

D/function/swrg_kit_integration/items/multiply_2.mcfunction:1 defaults only if attribute component absent; :3-26 alters indices0..23 using `data get amount 20` then stored double .1: floor(amount*20)*.1, including negative-floor bias. No clamp, scales all attribute kinds/operations. attribute_fix_item:1-73 supplies tools/armor above; trident/mace speed slot is omitted in raw source. Raw _3:2 writes count*3 with no cap. Raw _4:1-42 hardcodes all then-current42 vanilla enchants, including curses, direct enchantments only; no stored enchantments, no explicit caps. Listed order: blast_protection, feather_falling, fire_protection, projectile_protection, protection, thorns, aqua_affinity, depth_strider, frost_walker, respiration, soul_speed, swift_sneak, bane_of_arthropods, breach, density, fire_aspect, knockback, looting, sharpness, smite, sweeping_edge, wind_burst, flame, power, punch, quick_charge, multishot, piercing, infinity, channeling, impaling, loyalty, riptide, efficiency, fortune, silk_touch, mending, unbreaking, luck_of_the_sea, lure, binding_curse, vanishing_curse.

## x5 source and bugs requiring a deliberate choice

No Paper/Ignite x5-specific override found. D/function/workingitems/spell1_default:97 and _left:82 call multiply_5 for corresponding hand. D/function/items/spells:7,13 applies own nomagic_active and nearby nonspectator nomagic_active player <=20 gate, main/offhand magicwand admission. Individual x5 lines have no stopper_magic marker/beacon test. Thus source artifact suppression applies via parent; source beacon suppression is absent. No mana price or cooldown in x5 handler. Source clear removes ONE matching multiply5 from inventory, not necessarily active hand.

D/function/swrg_kit_integration/items/multiply_5:1-6 copies active_effects into shared storage, clears all current effects, initializes tmp=0 then invokes D/function/workingitems/witch_gens/vanity_respawn_return_effects_pre_x5. :10 clears one tablet even no active effects.

pre_x5:1 copies current ID; :3 reads duration; :4 produces seconds `5*floor(duration*.05)` for values outside[-1,0], so re-applied ticks are100*floor(originalTicks/20), losing original subsecond remainder before x5. :5 retains -1 as infinite, :6 converts any original duration>=200001 ticks to infinite; exactly200000 remains finite. :7 makes zero invalid deliberately. :9 reads amp. :12 calls generic return_effects whose only line is `$effect give @s $(id) $(dur) $(amp)`. :13-15 advances index and recurses through index32 (33 slots), with no existence guard.

Consequences: only visible active layer is rebuilt, hidden layers discarded; ambient/show-particles/show-icon metadata discarded to effect-command defaults; duration0 fails; missing slots leave prior shared id/dur/amp values and continue replaying stale values. Storage dur/id/amp are not reset per tablet. Empty effects may replay stale previous-player effects. Negative duration<-1 can create invalid negative seconds; finite seconds exceeding command bounds fail. These are source defects, not appropriate native shared-state architecture. Native implementation should use actual MobEffectInstance chains and explicit user policy for rounding/long threshold/hidden layers, no shared scratch storage or function runtime.

## Acquisition / RP

D/loot_table/kit/multiply_tablet.json has FOUR independent one-roll pools: repeating_command_block x2/x3/x4 and carrot_on_a_stick x5. Models lbc:x2/x3/x4/x5. Names multiply_by_2/3/4/5; lore first matching .lore then multiply.lore for2/3/4 or multiply1.lore for5. Every explicit text component italic=false, no explicit color. custom_data multiply integer2/3/4;5 adds magicwand:1. P:533 maps kit8 to this actual loot table; item definition alone is not behavior.

Read-only RP assets/minecraft/lang/{ru_ru,en_us,rpr}.json:3488-3497 already contain all ten name/lore keys. RU describes attributes/count/enchantments/player effect duration; reuse exact translations. RP assets/lbc/items/x2.json through x5.json already exist, each model lbc:item/xN. No new translations/models expected.

## Reusable native owners

N/src/main/java/me/skywars2/game/LbcLooseRewards.java:13-67 provides tracked loaded/unloaded native item entities, current epoch, gameplay/dimension/time-stop gates, once-per-tick update and nearest Euclidean radius1 selection. Extend shared loose-item owner for tablets rather than another scanner/controller. Preserve tablets whole-entity debit (copier currently shrinks1).
N/.../LbcGlassArmor.java:56-65 already rebuilds ItemAttributeModifiers.Entry preserving operation/slot/display; native DataComponents.ATTRIBUTE_MODIFIERS accepts this directly. Existing effective item defaults need materialization before replacing an implicit component; source legacy default quirks are above.
N/.../EffectLayers.java:11-22 exposes hidden link and deep copy; native copy constructor omits hidden successors. Shared layer APIs should preserve/transform chains if user requests hidden layers. OwnedEffectLayers is existing ownership infrastructure; avoid new effect engine.
N/.../EnchantmentRules.java:18-56 and mixin/ItemEnchantmentLevelsMixin.java provide wide authoritative server enchant levels and safe client projection. Reuse direct ENCHANTMENTS mutable builder; leave STORED_ENCHANTMENTS unchanged per source unless user changes scope. Existing LbcItems use/input owner and shared antimagic policy should admit x5 both hands.

## Prioritized questions for chat (do not write PENDING)

1. x5: repair stale storage/33-slot replay and preserve hidden layers plus effect metadata, with exact tick duration*5 and infinite remaining infinite? Or retain legacy whole-second rounding and >200000-tick-to-infinite rule? This choice materially changes useful effects.
2. Suppression: x2/3/4 are source unjammed physical tablets; x5 source artifact gate but absent beacon test. Should x5 follow current common magic rule (both beacon+artifact), with physical tablets unjammed?
3. x2 source doubles negative attack-speed penalties and anomalous trident/default stats; retain actual Paper behavior? x3 actual cap127 and whole tablet-stack debit; retain those? These are confirmed behavior, not assumed design.

Recommended default outside unresolved user choices: actual Paper x2/3/4 semantics, no source functions at runtime, native local effect transformation fixing replay bug, existing component/effect/loose-item owners, source RP and actual kit chain retained.

## Acquisition follow-up: exact catalogue correction

Important correction to the requested identifier: palm is source kit24, NOT kit23. Legacy display key lbckit.23 and gui/page/9000/23 are zero-based artwork indices; selection ID is24 because GUI back occupies18. Source kit23 is fast_sword (D/function/swrg_kit_integration/game/kit_select/select.mcfunction:34), palm is kit24 (:35). Paper P:549 maps24 to palm_with_bananas, and P:606 has GuiSelectionOption(slot24,id24,gui/page/9000/23,gate academy/fartus_nadristailo). P:605 slot23/id23 uses gui22 and is ungated fast_sword. Do not replace or alias kit23 for palm.

Exact minimal catalogue rows when the whole group is enabled:

```json
{"id":8,"page":9000,"slot":7,"table":"lbc:kit/multiply_tablet","gate":"lbc:true_advancements/armor/multiplying","lockedKey":"lbc.adv.armor.multiplying.lore"}
{"id":24,"page":9000,"slot":24,"table":"lbc:kit/palm_with_bananas","gate":"lbc:true_advancements/legends/academy/fartus_nadristailo","lockedKey":"lbc.adv.fartus_nadristailo_upgrade.lore"}
```

No modifiers on either source row. At follow-up inspection both were absent from N/src/main/resources/skywars2/lbc_loadouts.json (13 existing kits). Raw table rolling is insufficient menu acquisition.

D/function/swrg_kit_integration/gui/page/9000.mcfunction:18 provides tablet menu icon in enderchest7 only with multiplying advancement; :70 locked model and :96 swrg_unlocks/multiplying modifier. select/9000:11 sets lbc.kit8 only if gate complete, :39 emits locked lore. Palm page9000:35 icon enderchest24 only when academy/fartus gate complete, :86 locked model and :112 swrg_unlocks/fartus_nadristailo; select9000:28 sets kit24 when gate complete, :56 emits locked lore. Ranked select9000_ranked:25 allows kit24 without gate (same ranked bypass for tablet8 earlier in file); current LbcLoadouts.java:227 implements ranked bypass already. Native catalogue loader uses slot to derive the gui icon so slot24 must stay24 even though artwork table is23 (verify existing menu index handling before enabling; source offsets after back18 are intentional).

Unlock route tablet8: D/advancement/true_advancements/armor/multiplying.json: frame challenge; trigger player_killed_entity, killer nonspectator wearing custom_data multiplying:1 in ALL four armor slots and victim minecraft:player. Parent armor is presentation, not an additional explicit criterion. Reward function swrg_kit_integration/gain/kit displays blank title/new_kit subtitle. Current N/data/lbc/advancement/true_advancements/armor/multiplying.json is present and converted to native criterion form, retaining all four armor predicates and player victim, with function reward removed.

Unlock route palm24: D/advancement/true_advancements/legends/academy/fartus_nadristailo.json: inventory_changed nonspectator with item custom_data {fartus_nadristailo:1,upgraded:1}; parent academy/terra is presentation. It is obtaining the Academy-upgraded Fartus item, not a separate banana-eating challenge. Current native advancement file is present, retaining exact native inventory predicate; LbcAcademy.java:61 maps FARTUS to LbcFartus.ACADEMY and native source flags are supplied by LbcFartus.java:82. Again this is source inspection, not fresh gameplay qualification.

No new unlock notification rows needed: N/skywars2/lbc_unlock_notifications.json:93 already maps armor/multiplying -> new_kit; :306 already maps academy/fartus_nadristailo -> new_kit. N/game/LbcLoadouts.java:162-176 emits completion subtitle using existing native advancement callback. Existing converted advancement resources remove runtime legacy reward functions; retain this architecture.

All required current resources exist: loot_table/kit/multiply_tablet.json, kit/palm_with_bananas.json, gui/page/9000/7.json and23.json, item_modifier/swrg_unlocks/multiplying.json and fartus_nadristailo.json, plus both gate advancements. Existing read-only RP ru_ru.json:1392 locked multiplying lore; :5208 Fartus upgrade locked lore; :5230-5237 lbckit.23 palm title/seven lore lines. No RP/template change needed for acquisition. Existing icon gui7 uses lbckit.7 and eight lore lines plus stats; palm gui23 is separate from ungated fast-sword gui22.

Scope of follow-up: catalogue and native advancement/static dependency audit only; no tests/build/server/source/PENDING modifications. Minimal integration is two correctly numbered catalogue rows plus qualification of actual source menu selection -> match grant -> five native consumers; existing gate resources/notification rows should be reused.

## Previous-kit-acquisition correction: installed mechanical tools

Urgent bounded static correction for the already resolved/tested mechanical tools. Current lbc_loadouts.json lacks actual source kits12/13. Do not reopen gameplay policy; direct table checks did not establish real menu acquisition.

Exact missing rows:

```json
{"id":12,"page":9000,"slot":11,"table":"lbc:kit/steampunk","gate":"lbc:true_advancements/armor/steampunk","lockedKey":"lbc.adv.armor.steampunk.lore"}
{"id":13,"page":9000,"slot":12,"table":"lbc:kit/hook","gate":"lbc:true_advancements/armor/technomk2","lockedKey":"lbc.adv.armor.technomk2.lore"}
```

Paper P:537-538 maps IDs12/13 to these tables; :595-596 exact slot/ID/gate/locked lore. Datapack D/function/swrg_kit_integration/game/kit_select/select.mcfunction:23-24 grants them for lbc.kit12/13. D/.../gui/select/9000.mcfunction:15-16 admits slot11/12 only with corresponding advancement and :43-44 emits respective locked lore; ranked bypass remains existing shared behavior.

Current source-tree resources exist and are already canonical: N/src/main/resources/data/lbc/loot_table/kit/steampunk.json pools roll1 to lbc:ported/steampunk_pickaxe and roll1 to lbc:ported/steampunk_sword. Its rocket pool rolls0; retain0, do not enable rocket as part of repair. kit/hook.json rolls1 to lbc:ported/grapping_hook. All three ported leaf resources exist. This confirms packaging inputs, not a newly inspected installed JAR.

Current menu artwork tables gui/page/9000/11.json and12.json, locked modifiers swrg_unlocks/steampunk.json and technomk2.json, gate advancements true_advancements/armor/steampunk.json and technomk2.json exist. Both native gate criteria are nonspectator player_killed_entity with minecraft:player victim and full four armor slots carrying reactive:1 (steampunk) or technomk2:1 (hook).

No unlock notification additions: skywars2/lbc_unlock_notifications.json:123 already steampunk -> new_kit, :132 already technomk2 -> new_kit. Minimal fix is two catalogue entries followed by actual menu-to-match grant qualification and release handling by root. No new questions, gameplay changes, source/build/tests/server/git writes in this audit.
