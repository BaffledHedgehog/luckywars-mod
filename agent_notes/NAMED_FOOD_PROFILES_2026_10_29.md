# Named food profiles - 29 October 2026

Status: staged consumer implementation; no build/server/qualification/packaging/readiness claim. Full original Chaos graph gate stays closed until downstream consumers and produced pools are qualified. Owned files: new `LbcNamedFoods.java` and this note only. Existing Java, resources, RP, build configuration, author overrides and servers were not edited by this agent.

Read-only original root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Read all assigned top-level consume advancement criteria and their full `function/other/eatables/*.mcfunction` bodies; considered `agent_notes/FOOD_REWARD_SOURCE_2026_10_29.md`, existing native KFC/AttributeFood/Mana/SurfaceTeleport/Psycho/Blaze/Electro/AwakenedAcid/Erecto consumers and native LootCommand spawn behavior.

## Exact bounded source set and API

There are **40**, not41, assigned live root criteria/functions:17golden,11diamond,5netherite,bedrock apple,netherstar fish,transdimensional fish,multiverse fish,human eye,human flesh,banana. The earlier informal41 count was incorrect. There is no authored golden_apple/golden_carrot function or matching consume root to invent; ordinary vanilla golden foods and AttributeFood carrier flags remain separate.

`new LbcNamedFoods(game)` exposes `public void consumed(ServerPlayer,ItemStack)`. Root should call it from existing native completion alongside the other food consumers. There is no private state/lifecycle/tick, debit, nutrition replacement, custom potion engine or runtime mcfunction execution. Literal typed Java profiles retain ordered native effect calls; extras are explicit native operations. Multiple exact-one flags independently grant their profiles; banana's actual flag is `eat_banana`, all other assigned flags equal the function stem. Hidden advancement revocation is implicit in repeated native consumption; these source root reward functions must not also run through a separately packaged hidden advancement.

All40 original criteria explicitly exclude Nexus and Imprinted and have no source MAG/TECH, game-role or Time Stop condition. Implementation uses current AttributeFood admission: active/nonending match, live present ordinary-dimension player in actual map level, gameplay permission, valid nonobserver role and shared dimension/global/input Time Stop admission. This is current project policy, not an assertion that these extra gates exist in legacy criteria. There is no invented blanket MAG/TECH check on the food profile. Native effect admission and the raw-counter consumer's own original suppression remain shared.

## Literal effect profiles

Full named functions are the following, with their complete ordered native effect/duration/amplifier/hidden payloads present literally in Java:

| Family | Original function stems |
|---|---|
| Golden17 | golden_beef, golden_beetroot, golden_berries, golden_bread, golden_cake, golden_chicken, golden_chorus, golden_cookie, golden_fish, golden_kelp, golden_melon, golden_mutton, golden_pie, golden_porkchop, golden_potato, golden_rabbit, golden_stew |
| Diamond11 | diamond_apple, diamond_beef, diamond_berries, diamond_carrot, diamond_chicken, diamond_fish, diamond_mutton, diamond_porkchop, diamond_potato, diamond_rabbit, diamond_stew |
| Netherite5 | netherite_apple, netherite_carrot, netherite_fish, netherite_potato, netherite_stew |
| Other7 | bedrock_apple, netherstar_fish, transdimensional_fish, multiverse_fish, human_eye, human_flesh, banana |

Static verification independently extracted all direct `effect give` commands and compared the39 literal ordered Java effect profiles; banana's conditional heal/Strength branches are explicit and separately checked. **PASS all40 actual criterion flags,39 full ordered effect profiles and both explicit banana effect branches**, including duplicate Raid Omen calls, original amplifiers, infinite durations and invisibility's hidden flag. This is a source comparison, not native execution or a formula mirror gameplay test.

Source `/effect give` duration is native ticks for instantaneous effects, seconds*20 for ordinary effects, and native-1 for infinite. No `applyInstantaneousEffect` shortcut replaces native `addEffect`. All calls use `ItemEffectLevels.create`; only already-approved item amplitude caps apply (regeneration/wither5,poison/resistance4,blindness/darkness2). Other large original amplifiers remain literal. Carrot ordering is preserved: diamond clears blindness/darkness after its six grants; netherite clears them between its four finite grants and three infinite grants. No extra cleansing is applied to unrelated foods.

Important source literals retained without guessing intent:

- golden_beetroot instant_health duration600 is600native ticks, not600seconds.
- golden_stew instant_health duration1000 is1000native ticks. Its piss+1000/tag commands precede its four effect grants.
- netherite_apple saturation10000 and bedrock_apple saturation28800 are native tick durations because saturation is instantaneous.
- golden_porkchop health_boost is **3seconds, amplifier90**, not90seconds/amplifier3.
- diamond/netherite/netherstar/transdimensional/multiverse fish issue Raid Omen amplifier4 and then16 as two original effect calls. Netherite stew issues the same infinite amplifier5 grant twice; both calls remain.
- netherite stew's instant_damage/instant_health/saturation are genuinely infinite native effects; only invisibility hides particles/icon. No fabricated removal, heal-to-full or immunity is added.

These surprising literals are source quirks for the central pending/author record, not reasons to silently alter the profile. Whether the author wants different intended durations/amplifiers remains a deferred question; current implementation preserves literal source.

## Additional named mechanics

Diamond chicken makes **five independent native `lbc:spawnegg` rolls** at wearer feet, then plays chicken.death MASTER @a at pitches2,1,0, volume1, through shared sound delivery. SpawnEgg items retain their original payloads; no replacement mob or filtered pool is synthesized.

Golden chorus uses the established shared surface destination helper: no_borders current authority selects wearer-centered radius200, otherwise origin-centered radius190, min spread0, no team grouping. It keeps native source maximum world height (no invented Nether120 ceiling) and existing velocity/look-preserving teleport API. A failed resident destination search does not invent a fallback surface. Existing shared finite resident-column semantics remain the project's surface engine; this class does not create a duplicate spread/physics engine or claim unmodified vanilla sampling distribution.

Human eye grants nausea/hunger/blindness1second,amp1, then adds all_seeing_eye12000 and adopts raw status owners. Golden pie grants its original speed/resistance, then adds living_legend200. Those numbers remain source counter units, not newly interpreted seconds.

### Human flesh

After the same three20tick debuffs, an actual native `EntitySelectorParser` executes `@a[gamemode=!spectator,name=!<consumer>,sort=random,limit=1]`. Native @a retains its global/cross-dimension candidate population and random ordering. There is no radius/team/enemy filter and no resampling if the chosen player is rejected by current protected-role/Greed/Time Stop policy. The candidate's dimension need not be an ordinary one; the source criterion only limits the consuming player.

Capture the two actual positions/worlds before moving either participant. Execute at most two explicit native transfers: consumer to selected player's saved location, then selected player to consumer's saved location. Shared teleport momentum policy preserves velocity. Successful displacement records WILD wearer credit, retaining source allies/self consequences and shared protected-role policy.

Literal source first `tp consumer selectedPlayer` copies the selected player's yaw/pitch; second `tp selectedPlayer marker` uses the newly summoned marker's default0/0 rotation. The native pair retains these angles rather than silently changing them to each player's old look. No physical/global `owner`, `tp` or `replacer` tag is created, selected, bulk-cleared or killed. In particular, existing unrelated tags/markers cannot steal a destination or be removed. This intentionally bounded replacement of accidental global source-tag collisions is part of the parent's explicit instruction, not an unrequested global behavior change. Failed first transfer stops the pair; there is no new global teleport search, marker or fake player fallback.

### Banana

Uses effective max-health attribute command integer truncation and native data-get Health integer floor, publishes original shared tmp/tmp1 lbc.math values. Makes independent source chance50 and chance15 draws, dropping complete original `lbc:govno_eaten` and `lbc:wtf_item` respectively before deciding its final effect. If integer max health exceeds integer current health, grant instant_health1native tick,amp1 and return from the banana branch; otherwise grant strength5seconds,amp1. It does not always heal, heal immediately, heal-to-full or also grant Strength on the heal branch.

Loot output uses normal native ItemEntity copy/default pickup delay at exact wearer feet, wearer thrower/epoch attribution and existing entity lifecycle. All table outputs are retained; no broad-pool entry filtering. The source loot tables and their further consumers remain root-owned closure requirements.

## Raw counter authority and adoption

All source additions use original actual scoreboard objectives and Java int scoreboard addition semantics. Mana/manamax/manaregen raw values are not preclamped through effect admission. A root-owned `game.items().adoptRawFoodStatuses(player)` runs **after the exact raw writes/tags**, attaching existing native consumers rather than incrementing a second counter or copying effect engines. Call sites: golden_stew,golden_pie,human_eye,transdimensional_fish,multiverse_fish,netherite_stew. Plain mana-only additions already use native score authority.

| Producer | Exact additions |
|---|---|
| netherite_fish | mana+20000 |
| netherstar_fish | mana+100000, manaregen+10, manamax+10000 |
| transdimensional_fish | void_resistance+5760, dimensional_shifting+5760, mana+1000000, manaregen+100, manamax+100000 |
| multiverse_fish | void_resistance+11520, dimensional_shifting+11520, mana+10000000, manaregen+1000, manamax+1000000 |

Netherite stew writes all29 original objectives in original source order. Every value is INT_MAX except end_damage21474. Adds exactly have_lucky_effects,have_lucky_effects_5t,have_lucky_effects_1s after the writes, then adopts owners. Static comparison **PASS exact29 name/value payload**. Individual consumers upstream/downstream:

| Raw objective | Original next consumer / existing native authority or closure |
|---|---|
| acid_rifle | players_5t -> awakened acidacid rifle tick; existing LbcAwakenedAcid Rifle registry must adopt |
| all_seeing_eye | players_tick -> effect_allseeingeye; shared LbcFoodStatuses |
| blaze | mobs_5t/players_10t -> blaze head/fire consequences; existing LbcBlaze owner registry |
| delayed_death | effects/tick_1s + players_5s -> effect_death; shared legacy effect closure/root-owned |
| dimensional_shifting | players_1s -> effect_shifting -> teleport_nahui; shared FoodStatuses/Hedgehog dimension engine |
| end_damage | damage_end / damage_end_loop; raw delayed end-damage consequence, root audit required |
| gatling_fovos | players_2t -> blind_loop_gatling; shared Blind gatling owner registry/adoption |
| lolidefecation | witch_gens/lust and reset/vanity helpers; raw witch-generation stat, no new named-food engine |
| greed_prestige | witch_gens/greed, other/greed/pickup and resets; shared Greed generation/pickup authority |
| mana | native LbcMana current resource, normal regen/reverse/drain/cast consumers |
| living_legend | players_tick -> effect_living_legend; native FoodStatuses exact filtered terrain |
| lookback | mobs_5t / psychowand/lookbackloop; existing LbcPsycho private status owner must adopt |
| manamax | maxmana/manaregen/casts; native LbcMana maximum resource |
| manaregen | players_tick/manaregen chain; native LbcMana regeneration authority |
| reverse_mana | players_1s / negative_mana; native LbcMana reverse timer, raw adoption must retain value |
| nanobots_coldown | source Nanobot armor source timing; existing armor Nanobots owner/score contract |
| nanobots_tier | source Nanobot tier/state choice; existing armor Nanobots owner/score contract |
| nitro | Reactive/MK2 flight and sound/fuel loops; shared LbcTechnoArmor/Reactive consumers |
| nomagic | players_1s -> effect_nomagic; native LbcMana personal timer (not a new blanket food gate) |
| piss | effects/tick -> yellow_lightning_asplayer; existing Psycho/Electro native yellow stream owner must adopt |
| ray | players_tick -> existing Erecto ray emitter; shared LbcErecto owner registry |
| reactivefuel | Reactive boots and MK2 flight fuel; existing shared armor/reactive authority |
| scared | mobs_5t -> psychowand scaredloop; native LbcPsycho owner |
| screamray | players_tick -> upgraded psychoblind ray; native LbcPsycho ray owner |
| shock | tick2smobs -> other/shock; shared LbcElectro pulse owner |
| sleep_time | player_speed / witch_gens/indolence; shared movement/status closure/root audit |
| target | tick1s/players_2s -> other/target_curse; shared target/homing curse closure/root audit |
| twitching | players_2t -> other/twitching; native LbcPsycho forced-use owner |
| void_resistance | players_1s -> effect_void_resistance and shared void/death consumers; exact raw protection timer |

The three have_lucky_effects tags reenter source effects/tick, tick_5t and tick_1s parents. They are preserved, not mistaken for a new private scheduling system. Root owns new LbcLegacyEffects and necessary owner adoption. This class never claims that setting raw scores alone implements the descendant mechanics, never adds INT_MAX times in a loop, and never substitutes a summon-only or generic radius effect.

### Exact requested downstream file index

Paths below are relative to original `lbc/function/`; they are the actual previously audited hits, not invented runtime dispatch names.

| Counter | Exact source files |
|---|---|
| gatling_fovos | `players_2t.mcfunction`; `workingitems/sauvojen_staffs/blind_loop_gatling.mcfunction`; producer `workingitems/sauvojen_staffs/blind_preloop_upgraded_gatlingator.mcfunction`; `resets.mcfunction` |
| end_damage | `damage_end.mcfunction`; downstream `damage_end_loop.mcfunction`; `resets.mcfunction`; vanity/vanity_respawn producers under `workingitems/witch_gens/` |
| target | `tick1s.mcfunction`; `players_2s.mcfunction`; `other/target_curse.mcfunction`; `other/target_curse_rc.mcfunction`; `raycast_vpered_pre.mcfunction`; `resets.mcfunction` |
| lolidefecation | `workingitems/witch_gens/lust.mcfunction`; `workingitems/witch_gens/reset.mcfunction`; `workingitems/witch_gens/vanity.mcfunction`; `workingitems/witch_gens/vanity_respawn.mcfunction`; `resets.mcfunction` |
| sleep_time | `player_speed.mcfunction`; `workingitems/witch_gens/indolence.mcfunction`; `workingitems/witch_gens/reset.mcfunction`; `workingitems/witch_gens/vanity.mcfunction`; `workingitems/witch_gens/vanity_respawn.mcfunction`; `resets.mcfunction` |
| greed_prestige | `workingitems/witch_gens/greed.mcfunction`; `other/greed/pickup.mcfunction`; `workingitems/witch_gens/reset.mcfunction`; `workingitems/witch_gens/vanity.mcfunction`; `workingitems/witch_gens/vanity_respawn.mcfunction`; `resets.mcfunction` |

## Remaining closure and validation

Explicit exclusions: Oreshek, inf/inf_old, gluttony/Halloween/badapple wrappers and already-owned gain_bucket are not duplicated here. KFC remains existing native KFC ownership. Acquisition JSON migration, all named original carriers/food components, weighted parent tables, SpawnEgg outputs, banana WTF/Govno descendants and all29 active raw-score consumers must be qualified by root before broad producer readiness/full Chaos release. Existing FoodStatuses Time Stop question remains central pending; this file adds no private alternative timer.

No author chat question was sent. Deferred source quirks are documented above for root's central pending record. Only static source/Java tuple and counter comparisons were run; no compile, native gameplay test, client visual proof, server operation or resource edit was performed by this agent.
