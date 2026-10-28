# Reactive, Techno and MK2 armor — 28 October 2026

Source closure: [read-only audit](NEXT_GEAR_SOURCE_2026_10_28_C.md). The decisions below supersede its open questions. Native qualification is complete.

## Author decisions (user)

- «реактивный шлем не надо глушить - считаем что шлем немеханически фильтрует воздух»; «оостальное да, глушить». Keep Reactive physical Blindness/Darkness/Nausea filter unjammable. TECH alone suppresses other active abilities, including MK2 night vision and already-running flight; MAG does not. Intrinsic equipment attributes and electrical full-set immunity remain.
- «Да, собственные слои и исправить снижение». Flight owns only its own Levitation/Slow Falling layers and original gravity IDs; never clear unrelated effects. Without MK2 legs use the explicit +0.1 downward gravity fallback instead of the old shared stale macro value.
- «Да, WILD, и для владельца этот урон как бы замыкает броню и должен полностью проходить». Water discharge includes allies and wearer. Clarification: «Снять только электрический иммунитет комплекта». The owner's six-point electrical damage bypasses only equipment's electrical immunity, retaining native armor, Resistance, absorption and absolute Greed.

## Shared native ownership

`LbcArmorSets` owns one `LbcTechnoArmor` service, using its existing role/dimension/equipment admission. Source reactivefuel/nitro/reactivefuelsub/rocket_sound/technomk2shield/coldown8 objectives remain shared with existing KFC/resource initialization and purification. Keep source phased two/five/ten/twenty-tick abilities and 200-tick nitro regeneration. Preserve exact original leaf component payloads, overlapping techno/reactive/MK2 flags, attributes, lore, models, HUD glyphs and sound IDs. MK2 boots themselves do not carry reactive; the other MK2 parts do. No invented MK2 chest ability: its source function is empty.

Reactive boots own lift16 or full-set lift31, native 200-tick Slow Falling, source fuel/recharge and particles. Techno helmet has BOTH live source consumers: private per-tick entity markers and once-per-second player direction particles. Nitro uses ten attributed radius-one native explosions, not helper creepers. Wyvern leggings use the actual native jump callback with source cooldown40 seconds and source effects/damage. Native incoming hurt criterion creates three electrical retaliation traces with source lifetime15 through the existing shared electric ray engine. Shield/absorption criteria count; rejected hurt attempts do not. Full MK2 replaces ordinary Techno bonuses with its 100-tick shield recharge and native owned Resistance V.

`LbcTechnoFlight` changes native gravity/effects without teleportation or replacing player input. Airborne sneak starts a two-tick initial delay; source per-tick pitch modes, sound counter, subfuel expenditure and ground regeneration follow. Shared `speedy` is derived from successive floor(Y*100). Preserve original gravity IDs, localized exhaust origins, and source landing check below1..5 (the initial audit abbreviated this as1..4). Only own gravity/effects are removed on stop, equipment/role change, TECH, forget or map lifecycle. Already-issued nonflight night vision/Resistance expire naturally; TECH blocks new grants. Paused maintenance runs even through `LbcItems` global Time Stop early return, withdrawing invalid Reactive and MK2 flight owners without advancing ability/fuel clocks.

Water shock uses shared ELECTRIC damage, original literal-water feet and source radius10. Native WILD attribution credits the wearer while protecting existing excluded gameplay roles. A narrow `SelfWaterDamage` marker, unwrapped through the existing captured-damage service, bypasses only Techno electrical full-set protection for that source and its owner. Ordinary armor/effect/absorption/Greed checks stay native. Mixed Techno/MK2 four-slot electrical immunity remains unchanged for other electrical sources.

## Complete acquisition and presentation

Expose exactly23 bounded source tables: Reactive nine (five leaves, root/andboots/pre/full), Techno seven (four leaves, root/pre/full), MK2 six (four leaves, root/full) and existing Hermes boots dependency. Do not invent a MK2 pre table or enable unrelated broad LuckyBlock pools. Existing normal slot and Ultra-family reforges are retained. Only newly enabled Techno pre/full require native26.3 modifier/type codec conversion; their original old_exoskeleton outputs remain.

Extend the existing `LbcNyauwuCrafting` and native `LbcCraftingRecipe` framework with four same-slot Reactive+Techno fusions and Hermes+Reactive flying boots, in grid and tracked dropped-item forms. No global entity scan, mana/jammer gate, preview debit or duplicated crafting engine. Consume one unit per donor, retain surplus/components, publish ground outputs before debit. Keep original one-second cadence, source dust palettes and enchantment sound. Ground-only tiny electro +four native iron armor pieces produces the five-item Techno bundle, including old exoskeleton, with exact independent40% catalyst consumption.

Read-only author RP verification found all13 item models, four equipment assets and63 original translation keys in both RU/EN. No RP/template edits required. All198 pre-existing author paths and29 live grimoire overrides are protected by exact baseline hashes.

## Verification record

Rejected candidates and their complete proofs/logs remain under `build/verification/techno-armor/`:

- `8fcd1036`: runner setup rejected an outdated catalog assertion that Techno was not ready. Replace the negative sentinel with still-unported Chaos, retain positive Techno assertion.
- `7613cd29`: both actual matches reached an incorrect fixture reforge-branch lookup; encoding a native nested table inlines its value and loses its resource-name string. Assert original normal pool entry7 weight1 and actual consumed native reforge output instead. Neighbor suites passed; no gameplay change.
- `5d516395`: one-match diagnostic rejected ground cadence precondition because grid ResultSlot had already consumed one from the same mutable donor stacks. Create fresh count2/count3 ground stacks, retaining their original component payload and exact one-unit-surplus assertions. No gameplay change.

Source review independently confirmed the need for immediate Reactive landing/lift cleanup under TECH, including Time Stop. The production fix also covers MK2 gravity cleanup through the real global paused branch; a native regression invokes `LbcItems.tick` there. Debug runs are not release qualification and never authorize installation.

Final artifact `94386bef0a3a1861db48b744d7d1f7951818f0225a21c9dd1e9b622c73ef995a` passed36 recorded results in two actual mode1 matches (104.75 seconds). Exact563 Java sources,1596 classes,3257 resources and build.gradle match the archived artifact. All198 pre-existing author paths and29 live grimoire overrides remain byte-exact. The earlier one-match diagnostic of the same artifact also passed, but was explicitly not qualification. Native child stopped normally and all fixture snapshots were restored.

Coverage includes all23 source tables and13 exact leaf payloads; real normal/Ultra consumed reforges; native2x2 ResultSlot/3x3 recipes and tracked ground fusions with surplus; source five-item Techno bundle and40% chance boundary; source fuel/Nitro/shield counters and cadence; real jump Wyvern and ten native explosion packets; accepted/rejected/absorbed/full-shield native incoming criteria; actual shared electrical trace lifetime/Time Stop; private helmet marker packets; mixed-slot electric immunity, WILD water allies/self and protected/dry controls; MK2 gravity/pitch, fuel, fifth-block landing, owned layers and actual paused item scheduler TECH cleanup. Mage/Nyauwu, Flesh, Berserk, Glass, Nanobots, ordinary/Shulker armor, incoming artifacts, signed attributes/effects/layers/shadow and Time Stop pass in both matches.

The log retains one pre-suite Polymer chunk-packet-context diagnostic during initial Carpet-created bot chunk delivery. The same two ERROR messages and native Polymer/PlayerChunkSender stack already exist in the preceding Mage candidate log (`mage-nyauwu/initial-6b0f9ce2/native.log`). It occurs before Techno checks; no new armor frame or crash is present. Do not claim an error-free network log or a physical-client packet/render qualification. Details and strict diagnostic allowlist are in `release-audit.json`.

Evidence: `build/verification/techno-armor/{artifact-proof,native-results,release-audit,qualification}.json` and `native.log`. Stopped-only installation records real status in `main-install.json`; never force a main shutdown/restart. No physical-client pixels, real socket reconnect/death or natural peak TPS claim.
