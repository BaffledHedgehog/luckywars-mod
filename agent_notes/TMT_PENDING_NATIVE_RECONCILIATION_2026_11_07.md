# Pending native TMT identities — snapshot reconciliation, 7 November 2026

Read-only followup to
`build/verification/tmt-remaining-domains-2026-11-07/d2b02684-tmt-catalog-domains.json`.
Only this note was added. No production/resource edits, compiler/JVM, JDT/index,
server, protected Academy/Reroll changes, or subagents.

## Counts that must not be confused

The snapshot has **151 PENDING_NATIVE_DOMAIN rows**, covering **52 distinct item
IDs: 36 skywars2 native identities and 16 vanilla carrier IDs**. There are exactly
36 native-default rows. Several canonical source variants repeat the native
default finding; conversely one vanilla carrier can contain many different real
selectors. Thus neither 151 nor 52 is a count of missing semantic abilities.
The five separate `LEGACY_OWNER_GAP` rows are clocks, gayradar, kill_rand,
steampunk_rocket and trollge, already partly represented among carrier rows.

This is a snapshot audit; other agents are separately closing Bucks, potion and
shield domains. Current source may be ahead of the JSON. No row below is marked
supported merely because an owner class exists.

## All 36 native identities, grouped by actual boundary

Names below have the `skywars2:` prefix.

| Exact IDs | Concrete current consumer | Reconciliation / remaining boundary |
| --- | --- | --- |
| academy_token, magic_academy_drop | `LbcAcademy.loaded` -> private `isDrop` -> `tick` drops loop; token on-ground requirement, original template placement/acquisition | Real LOOSE_ITEM operation. Ordinary held-use is the wrong event. Preserve protected body; source must remain projected/retained through its actual indexed producer lifecycle. |
| bad_apple_imba | `LbcBadApple.recognized`, `spawn(ItemEntity)`; `LbcItems.entityLoaded` explicitly detects the physical dropped item | Real independent loose producer. Ordinary native factory works; current TMT special-drop admission does not cover that separately indexed owner. |
| helicopter_encounter | `LbcHelicopterEncounter.isTrigger`, `spawn(ItemEntity)` and pending tick; exact `{helicopter_boss:1,drop:1}` | Real loose encounter factory/retained provenance. Keep source flag pair, direction/position, ownerless semantics and checkpoint; no synthetic held cast. |
| multiply_2, multiply_3, multiply_4 | `LbcMultiplierTablets.kind`; `LbcLooseRewards.loaded/tick/tablet` | Actual source data is `multiply:2/3/4`, despite keyless default finding. Physical near-donor operation doubles attributes/triples quantity/quadruples enchantments and debits one tablet. Shared loose-property adapter needed; do not confuse these items with inert `multiplying` armor category flag. |
| corrupted_dust | `LbcCorruptedGlyphs.kind` requires DUST identity **and** valid STRING `glyphkind`; `useOn` -> `spawn` -> one debit | Five pending rows are one bare default plus four authored parameterized profiles. Bare default has no kind and fails use; do not exclude real variants. Current `LbcTmtBlockUses.supports` admits BlockItems/eggs, not this genuine SimplePolymer Dust `useOn` factory. Explicit factory opt-in is a small real closure. |
| magnum_round_acid, magnum_round_electric, magnum_round_explosive, magnum_round_incendiary, magnum_round_standard, magnum_round_void | `LbcMagnum.registerRound`; `use` builds selected `MODES[mode]+"_round"` marker and `clearOrCountMatchingItems(...,1,craftSlots)` | Six real **ammunition/intake** traits. Their flags are read from inventory ammo, not the gun or a generic held weapon. Private TMT ammo must satisfy selected mode and own one actual physical debit; no free shots or unconditional bullet effects. Existing Quiver payload support alone does not establish Magnum support. |
| harpoon, sea_legend | `LbcProjectileWeaponItems.NativeTrident extends TridentItem`; source Loyalty/Impaling and Riptide components; `NativeGearChecks.throwHarpoon/projectiles` exercises actual charge/throw/return/dry-Riptide admission | Native component/charged-input boundary, not a custom on-hit flag body. Raw names do not justify a new ray cast. A genuine typed Trident use/release factory could be reused, with retained selected source and actual hand/debit/durability/projectile pickup. Current five TMT transports are not tridents. |
| electro_shield, electroblocker | Shield and blocker owners/components | Separate shield agent's scope. Snapshot's keyless defaults are not proof of inertness; authored profiles contain energy_shield/energyshield or electroblocker. Require actual held blocking/response evidence. |
| lightsaber_empty | `LbcNovemberLightsaberItems.selector`; `LbcNovemberLightsabers.use` main-hand path chooses faction from source red/blue glass or creates choice pads | Real free/stateful native use, not a plain sword-stat profile. Needs private native selector context and original faction/counter ownership. |
| lightsaber_jedi, lightsaber_sith | `LbcNovemberLightsaberItems.blade`; `LbcNovemberLightsabers.onKill`; native BLOCKS_ATTACKS component | Kill/faction semantics plus actual blocking components. General melee flag admission alone does not supply either. Source main hand, victim faction and shared counters must survive composition. |
| imba_elytra, imba_firework, imba_quick_crossbow | Existing native transport/component factories; elytra/firework are separate outputs of `imba/elytra` | Real native equipment/firework/charged-crossbow semantics. Empty custom data is not an empty prototype. Separate component prerequisites from ordinary stats/enchantments already generated; verify current ranged/continuous binding rather than adding guessed flags. |
| bucket_head | `LbcUtilityItems.Kind.BUCKET_HEAD` native leather helmet and authored equipment/armor components; `LbcThrownUtilities.equipHead` issues it | Do not infer a second bucket-launch ability from this helmet flag. Its physical equipment effects are components; separate thrown bucket factory already has its own slot. Determine which component traits the TMT generator/equipment owner actually carries before excluding/admitting. |
| halloween_mace | `LbcHalloweenWind.isMace` identity or `halloween_mace:1`, native wind owner | Real native-use/physical weapon prerequisites. Requires tracing its actual use admission and composition path; not closed by a family name or generic shovel flag. |
| admin_zaika | Native `nekoluckyblock.json` item-slot custom-data predicates (`container.*`, `armor.*`, offhand), and Loli gates; `ZaikaPassiveChecks` | Real **loot-context inventory passive**, explicitly no active spell. `TmtItemPredicateMixin`/TypedInstance mixins only change selected item identity inside a scope; they do not themselves establish inventory-wide custom-data predicate aggregation. Reuse read-only property-aware inventory query and test actual lucky BLOCK loot contexts/all slots. |
| nexus_wire_blue, nexus_wire_white | `LbcNexusWireItems.kind/isBlue/isWhite`; `LbcNexusAcquisition.loaded/itemChanged`, cage interaction/color and green+black joining work | Blue means historical **green** wire. Actual loose/context acquisition; white is not an ordinary instant consumable. Existing red/black intake bridge does not cover these. Keep Nexus-only source location/range/cage policy. |
| termit, sulfur, phosphin, gremuch | `LbcItems.registerChemicalPotion/chemicalContents`: RAID_OMEN amplifiers 234/235/236/237; `LbcClouds` actual chemistry branches | Full native chemical payloads, not dose-free defaults. Potion agent's domain; must preserve native cloud/terrain lifecycle. |
| wtf_item | `LbcFartus.isMaterial`; `LbcWtf.melee/attachLabel` -> Chef, with genuine melee acceptance and source debit | Real **melee consumed catalyst**, plus crafting material use. Generic Fartus wand support does not close it. Need source-specific melee property scope and one physical decrement; keep native Chef body/brain/provenance/checkpoint owner. |
| oguzok_sword | `LbcWtf.registerSword/sword`: ordinary native wooden-sword components; separate Chef-produced equipment | Concrete candidate for ordinary-component-only exclusion: no dedicated custom ability in its constructor. Do not confuse the Chef-producing `wtf_item` with the sword it equips. Check that no outside identity hook adds behavior before final classification. |
| god | `LbcHedgehogItems.GOD=create(...,usable=false)`; its `use` returns PASS. `LbcGodHedgehog` drops this item after boss death | Strong trophy/no-effect candidate: source exact `god:1` occurrence located in loot writer, not a held input reader. Separate boss/eater actor Tags do not activate from a trophy's custom data. Do not invent an active God cast; final exclusion should cite this writer/no-reader audit, not class absence. |

## The 16 vanilla carrier IDs contain additional distinct source selectors

| Item ID / pending row count | Exact reported selectors | Audit consequence |
| --- | --- | --- |
| minecraft:snowball / 11 | baffled_of_everything, bucks, end_of_everything, gayradar, jack_o_lantern, kill_rand, most, rasengan, shish_of_everything, steampunk_rocket, trollge | Eleven source selectors, not one vanilla-item gap. Preserve each actual native/legacy factory admission and source snapshot. Bucks is another agent's scope; five legacy gaps remain explicit. Existing ordinary Most is not IMBA bridge merely by name. |
| minecraft:carrot_on_a_stick / 17 | clocks, lightning | Multiple clock state profiles plus lightning. Read mode/parameter variants, not seventeen independent engines or one generic wand. |
| minecraft:repeating_command_block / 8 | copypaster, crocodildo_boss, helicopter_boss, infinity_rand, liquidvoid, yellow_lightning | Multiple loose factories/variants. Copier has proven `LbcLooseRewards.copy` native consumer; helicopter described above. Remaining specific flags stay pending until their exact spawn/readiness boundaries are located. |
| minecraft:stick / 30 | luck_key, reward/rewardtype | Most are reward descriptors; source/service status requires actual redemption/UI boundary evidence, not vanilla stick identity. `LbcCases` builds and normalizes reward descriptors from case tables; this alone is not proof every loose source token is inert. Luck-key source also has real break/loot-context reads, so do not blanket exclude all thirty. |
| minecraft:sunflower / 4 | HideFlags, rewardcoin | Candidate permanent account/UI reward descriptors. `LbcCases.tokens/normalize/apply` consumes normalized reward metadata; verify source redeem ownership before excluding each physical profile as service. |
| minecraft:stone / 1 | randomselect | Concrete service candidate: original `function/get_random.mcfunction:3–4` reads the helper Item's `random` attribute into score then kills that item. Do not add an arbitrary Stone held effect. |
| minecraft:slime_ball / 1 | slimeball | Requires exact source melee/vanilla component reader reconciliation; not supported from custom-sword naming. |
| minecraft:iron_sword / 1 | crowbar | Source profile/canonical native alias reconciliation required; a flag alone is not its authored stats/components. |
| minecraft:trident / 1 | slow_falling | Actual charged trident profile includes Riptide 15, Unbreakable and source enchantments; raw flag is not a standalone levitation cast. Same typed charged-native factory question as Harpoon. |
| minecraft:golden_apple / 1 | aimbobapple | Consume source remains separate from ordinary apple transport and seven random generator traits; locate exact native event before admission. |
| minecraft:netherite_axe / 1 | imbaxe | Historical source alias/component reconciliation with real native IMBA tool owner; preserve actual break versus damage boundary. |
| minecraft:iron_boots / 1 | reactive_old | Exact reader: `LbcBalancePassive.changed` checks IRON_BOOTS identity plus reactive_old; `second` grants Slow Falling/cloud. Need EQUIPPED FEET, original Balance mode, identity scope and event; not merely an inventory flag. |
| minecraft:cocoa_beans, minecraft:brown_dye, minecraft:nether_brick / 1 each | govnoq, randomgovno | Real donor/category reads exist: `LbcCraftingRecipe` checks govnoq; Ultraponos matches govnoq; Fartus carried-count effects are also covered by existing tests. Separate recipe/catalyst influence from a new active cast; randomgovno still needs its own reader audit. |
| minecraft:dirt / 1 | govnoq | Same donor category; Pisa source relic also has literal authored attributes/enchantments. Generic source category is not proof of a standalone active ability. |

No “already represented” claim is made solely from native ID/class matches.
Where source flags are incomplete or bare defaults lack selectors, compare the
actual native payload against a valid canonical variant before deduplication.

## Smallest reusable next closures

1. **Explicit native use-on opt-in for valid corrupted-glyph profiles.** Reuse
   existing `useOn`/placement context, actual source components, admission, native
   spawn, debit and glyph checkpoint owner. Exclude only the unparameterized
   DUST default. This is smaller and safer than adding another cast engine.
2. **One retained private loose-producer boundary** for copier/tablets first.
   `LbcLooseRewards.loaded` currently indexes actual physical source flags;
   `copy/tablet` rereads them on later ticks. A one-time projected loaded callback
   is insufficient: retain selected property/source and invoke the real owner
   when each native work quantum runs, while one physical token owns debit and
   unchanged outer remainder. Extend the same boundary to BadApple/encounters
   only after their readiness/lifecycle contracts are adapted. Academy's protected
   body stays unchanged; do not fake a player or duplicate structure placement.
3. **Typed continuous/native charged factory** after shield work: Harpoon,
   SeaLegend and authored Riptide trident can share genuine TridentItem charge,
   release, wet/dry admission and native projectile/pickup. Preserve hand, source
   private weapon copy, durability and transport remainder. This is a component
   event adapter, not a flag-triggered projectile impersonation.
4. **Passive query closure** for Zaika/luck-key and exact equipped reactive_old.
   Reuse read-only property queries with genuine loot/equipment consumers; verify
   placement slots/modes and actual BLOCK loot, rather than only a matcher unit
   test. Magnum inventory ammo is a separate debit-bearing query domain.

These are proposals grounded in concrete readers. This audit neither changes
catalog dispositions nor claims gameplay qualification; unresolved source/profile
families above remain visible until independently traced and tested.
