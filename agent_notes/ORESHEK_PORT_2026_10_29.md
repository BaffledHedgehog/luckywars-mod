# Oreshek full consume graph - 29 October 2026

Staged source/native implementation, not a release or qualification claim. Only NEW [LbcOreshek.java](../src/main/java/me/skywars2/game/LbcOreshek.java) and this NEW note were edited. Root owns the shared47-debuff consumer, direct Nexus bridge, callbacks/lifecycle, resource packaging and all builds/native checks. Full original Chaos pool remains gated and `root_PENDING` unchanged. No server, build, commit, RP, protected-author or live-override edits were made here.

## Exact graph and reused native engines

Root raw `eat_oreshek:1` criterion is consume_item, excludes both minecraft:nexus and minecraft:imprinted, and imposes no jammer, mana, enemy/team or role predicate. The actual food is original oreshki_10 (ten-item producer); food/consumable/presentation data remain native. No alternate food or scripted debit is introduced. Both lbc/lbcr roots were inspected: this consumer graph has no lbcr call.

`other/eatables/oreshek -> random_debuff`, then .1 explosion return; otherwise Ukraine sound8/1, then .01 Nexus return; otherwise .0001 Zelensky return; otherwise revoke root. Each is its own sequential source-world RandomChance draw. Unconditional rare-branch probabilities are .1, .9*.01=.009, and .9*.99*.0001=.0000891; they are not normalized three-way weights. Default fallthrough is .8909109. Normalized choice pools or eagerly drawing all three chances would change the source RNG/callback behavior.

The native consumer calls **legacyEffects.randomDebuff(player) first**, then source-level `getRandom().nextFloat` only as far as each branch is reached. The .0001 predicate is chance001.json=0.0001, not .001 or .01. Sound helper uses original authored sound IDs, MASTER and selector-all/native audibility; no MAG/TECH condition was invented on the consume branch.

Existing Bad Wiki uses84 vanilla-effect presets (and Academy28); it is not the47 source routine. Ender Wiki's safe-surface RTP is not a direct Nexus transfer. Psycho owns its four counters, Frost owns cold/cooldown/frozen, Acid owns delayed death, KFC owns gigaponos and other established status owners remain authoritative. `LbcOreshek` does not duplicate their timers or create an isolated score-only imitation. Root's NEW shared LbcLegacyEffects must route the producer and all its real consumer closure, including previously absent generic effect_* engines. Merely incrementing raw mana scores for missing consumers does not qualify the full graph.

## All47 random_debuff branches

The source first draws uniform integer1..47 from command-context world RNG, then adds all three tags `have_lucky_effects`, `have_lucky_effects_1s`, `have_lucky_effects_5t`. Each matching branch returns after exactly one effect/score producer.

For1..17, bare `effect give @s EFFECT` uses actual native EffectCommands defaults: amplifier0, particles visible, **600ticks for noninstant effects,1tick for InstantDamage**. This was verified in the local mapped26.3 `EffectCommands.computeDurationInTicks(null,effect)`; there is no explicit seconds argument in source. For18..47, add30 to the named canonical counter; do not replace an existing positive timer with30.

| Selection | Exact source producer | Native ownership requirement |
|---|---|---|
| 1 | `effect give @s blindness` | native effect / approved ItemEffectLevels; 600ticks |
| 2 | `effect give @s darkness` | native effect / approved ItemEffectLevels; 600ticks |
| 3 | `effect give @s glowing` | native effect / approved ItemEffectLevels; 600ticks |
| 4 | `effect give @s hunger` | native effect / approved ItemEffectLevels; 600ticks |
| 5 | `effect give @s infested` | native effect / approved ItemEffectLevels; 600ticks |
| 6 | `effect give @s instant_damage` | native effect / approved ItemEffectLevels; 1tick |
| 7 | `effect give @s levitation` | native effect / approved ItemEffectLevels; 600ticks |
| 8 | `effect give @s mining_fatigue` | native effect / approved ItemEffectLevels; 600ticks |
| 9 | `effect give @s nausea` | native effect / approved ItemEffectLevels; 600ticks |
| 10 | `effect give @s oozing` | native effect / approved ItemEffectLevels; 600ticks |
| 11 | `effect give @s poison` | native effect / approved ItemEffectLevels; 600ticks |
| 12 | `effect give @s slowness` | native effect / approved ItemEffectLevels; 600ticks |
| 13 | `effect give @s unluck` | native effect / approved ItemEffectLevels; 600ticks |
| 14 | `effect give @s weakness` | native effect / approved ItemEffectLevels; 600ticks |
| 15 | `effect give @s weaving` | native effect / approved ItemEffectLevels; 600ticks |
| 16 | `effect give @s wind_charged` | native effect / approved ItemEffectLevels; 600ticks |
| 17 | `effect give @s wither` | native effect / approved ItemEffectLevels; 600ticks |
| 18 | `scoreboard players add @s cold 30` | existing Frost canonical state/adoption |
| 19 | `scoreboard players add @s cold_cd 30` | existing Frost canonical state/adoption |
| 20 | `scoreboard players add @s delayed_death 30` | existing Acid delayed-death canonical state/adoption |
| 21 | `scoreboard players add @s effect_big_explosion 30` | shared real effect consumer (root); not an inert raw score |
| 22 | `scoreboard players add @s effect_evoker 30` | shared real effect consumer (root); not an inert raw score |
| 23 | `scoreboard players add @s effect_evokerarrow 30` | shared real effect consumer (root); not an inert raw score |
| 24 | `scoreboard players add @s effect_explosion 30` | shared real effect consumer (root); not an inert raw score |
| 25 | `scoreboard players add @s effect_firebreak 30` | shared real effect consumer (root); not an inert raw score |
| 26 | `scoreboard players add @s effect_gravity_curse 30` | shared real effect consumer (root); not an inert raw score |
| 27 | `scoreboard players add @s effect_hedgehog_boom 30` | shared real effect consumer (root); not an inert raw score |
| 28 | `scoreboard players add @s effect_kiuaskivi 30` | shared real effect consumer (root); not an inert raw score |
| 29 | `scoreboard players add @s effect_lightarrow 30` | shared real effect consumer (root); not an inert raw score |
| 30 | `scoreboard players add @s effect_lightning_bolt 30` | shared real effect consumer (root); not an inert raw score |
| 31 | `scoreboard players add @s effect_nuking 30` | shared real effect consumer (root); not an inert raw score |
| 32 | `scoreboard players add @s effect_remove_mana 30` | shared real effect consumer (root); not an inert raw score |
| 33 | `scoreboard players add @s effect_small_explosion 30` | shared real effect consumer (root); not an inert raw score |
| 34 | `scoreboard players add @s effect_snus 30` | shared real effect consumer (root); not an inert raw score |
| 35 | `scoreboard players add @s effect_stand_arrow_hit 30` | shared real effect consumer (root); not an inert raw score |
| 36 | `scoreboard players add @s effect_traps 30` | shared real effect consumer (root); not an inert raw score |
| 37 | `scoreboard players add @s effect_witch_gens_reset 30` | shared real effect consumer (root); not an inert raw score |
| 38 | `scoreboard players add @s frozen 30` | existing Frost canonical state/adoption |
| 39 | `scoreboard players add @s gigaponos 30` | existing KFC gigaponos owner/adoption |
| 40 | `scoreboard players add @s lookback 30` | existing Psycho owner/adoption |
| 41 | `scoreboard players add @s nomagic 30` | existing Mana nomagic owner/adoption |
| 42 | `scoreboard players add @s piss 30` | existing Psycho owner/adoption |
| 43 | `scoreboard players add @s scared 30` | existing Psycho owner/adoption |
| 44 | `scoreboard players add @s shock 30` | existing Electro shock owner/adoption |
| 45 | `scoreboard players add @s skill_issue 30` | existing turn-curse/skill_issue owner/adoption |
| 46 | `scoreboard players add @s target 30` | shared actual target-curse consumer (root) |
| 47 | `scoreboard players add @s twitching 30` | existing Psycho owner/adoption |

## Native rare branches

**Explosion:** source summons native TNT with fuse0/explosion_power15, then Donbass sound4/1. Port calls the existing `exasperato.encounterBlast(owner,sourceLevel,sourcePosition,15,sourceInit)`; this is its actual unprotected native TNT primitive, not Exasperato32/nuclear, a creeper, or a replacement explosion engine. TNT_EXPLODES/native TNT interaction and all native exposure/terrain/knockback/cooldown callbacks remain. SourceInit stamps WILD author/order/epoch on the actual unpublished PrimedTnt before native explosion. Metadata on that actual source remains readable after it is discarded in finally, so deferred captured DamageSource/children do not require a growing source map. Root wires canonical inherited cause/filter/admission and native child/conversion propagation. Physical explosions are not cancelled by an invented jammer test.

As with the already approved fuse0 primitive, execution is synchronous native explosion rather than publishing an extra ticking TNT carrier. The source command listing places Donbass after summon; source entity ticks can otherwise delay the actual explosion until later. No identical audio-versus-entity-tick timing claim is made from this translation alone.

**Nexus:** source draws two independent inclusive integer coordinates -4800..4800 in the **source world's RNG**, transfers directly to minecraft:nexus Y2332, then removes only attribute modifier minecraft:antigravity_effect. No surface search, portal position scale, safe-arrival platform or clamp to ordinary build height. The stable root bridge is `hedgehogDimensions.nexus(player,player,random)` and owns native transfer semantics/causal arrival; the consumer does not draw an unrelated four-dimension roll first. Root must retain rotation/native direct teleport motion behavior.

**Zelensky:** call actual `nukeEvent.start(player,origin,true)` and spawn the exact original native piglin SNBT plus four shared native bomber hoglins. This is not an Exasperato alias. Piglin retains the original four netherite slots with equipment asset lbc:zelensky, all four drop chances0, visible two-component aqua/yellow source name, ordinary AI/gravity/default health and **normal native zombification**. It has no invented NoAI, invulnerability, immortality, freeze or immunity-to-zombification flag. Production adds only causal/epoch tracking tags. `piglinPayload()` exposes a detached exact original payload for native verification.

The four source bomber spawn contexts are owner-relative yaw +45,-45,+135,-135, pitch0, local forward28 and worldY+20; each faces the original target feet with pitch0. Existing `DroneDrops.spawnBomber` formula adds90degrees internally, so the caller passes `ownerYaw+sourceRelative-90`. Shared drone service owns exact hoglin Health150/InstantDamage2000000/NoAI/NoGravity/native bombs/death/lifecycle. No duplicate drone engine or fake four mobs are created. Existing resident/spawnable checks can reject a missing-chunk output; no fallback substitute is issued. Root qualification must verify the rare event's actual marker/piglin/four drones in a resident fixture.

Native piglin/TNT descendants carry `skywars2.oreshek.actor` and serialized author/order/epoch metadata. Methods managed/wild/owner/cause/canAffect/canTarget + inherit(parent,child)/inheritChild(child) support the current common damage, mob-target, native explosion, projectile/drop and conversion hooks. Existing native AI determines ordinary piglin aggression/gold behavior; the service does not replace piglin goals. Observer authors do not cancel accepted WILD consequences; current protected-role/absolute-Greed guards remain in shared native hit admission. Physical accepted children persist across author forget and use native lifetimes; stop/epoch retires managed actors. The nuclear service and shared drones retain their own separate established ownership/lifecycle.

## Persistent early-return latch and exact reset boundaries

In source, consume_item completes the hidden root advancement **before** running its reward function. Rare early returns skip the final root revoke. Consequently later nuts may still be eaten natively but **none of their Oreshek script executes** until reset. The ordinary path revokes immediately and can proc next time. This is a dispatch latch, not just an optional death decoration.

Parent chose persisted native player entity tag `skywars2.oreshek.returned` as the sole latch; the hidden advancement has no public-progress consumer beyond leave_death and need not be repackaged. No synthetic advancement, disabled reward function or interpreter was added. Latch is set BEFORE the shared debuff, so a death observed during the reward has the same already-completed state. Normal fallthrough clears it. All rare-return paths leave it set even if their native operation cannot execute (source `return run` still skips the final revoke on command failure).

`pendingDeathMessage(player)` reads the marker. `died(player)` / `resetBoundary(player)` broadcasts **raw literal U+E202** to all current players and clears only that latch, returning whether it ran. Duplicate boundaries are idempotent: first clear consumes the marker and later reset does not broadcast twice. `forget` does not blindly clear it; root must call the native reset boundary before ordinary cleanup.

Source proof shows the boundary is broader than the function name:

* `lbc:leave_death:1` reads eat_oreshek completion for tellraw U+E202; line3 revokes.
* `lbc:resets:222 -> leave_death`; `lbc:start`, other/new_game/click and other/idgive(player) call resets.
* swarogskywars normal/developer **game/start/0** and **game/end/0** run resets for every player. `swrg:core/player/reset_pre:1` directly calls leave_death; `core/player/reset:26` calls resets.
* Existing read-only Paper source `SwrgNativeService.onPlayerDeath:1875` schedules `processPlayerResetPre`, `onPlayerQuit:1890` calls it for an active match, and `processPlayerResetPre:2824 -> runNativeLeaveDeath:3908` broadcasts/clears. `runNativeResets:2887` also calls it, reached by resetPlayerForLobbyAndRespawn.

Root therefore owns calls on actual death, active quit and applicable match/player reset/start/end **before tag cleanup**. No arbitrary UI/item forget should consume a latch. A raw unadapted source advancement would contain a disabled function reward; preserving this implementation artifact as a live resource is neither required nor safe. If future evidence finds a real public advancement dependency, add its exact resource/criterion through root separately.

## API and current limits

Public API: constructor(game), static matches, consumed, pendingDeathMessage/died/resetBoundary; static managed/wild; cause/owner/canAffect/canTarget; executing/currentEmitter/inherit/inheritChild; loaded/unloaded/tick/forget/stop; snapshot and piglinPayload plus completed-consume/branch counters and lastOutcome. Native food owns sole stack debit and its FOOD/CONSUMABLE effects; no player health or private consumption loop is synthesized.

Initial consume admission follows current shared gameplay participant/map/alive/roles/sealed-dimension/TimeStop policy. No entire-Oreshek MAG/TECH gate. Accepted detached consequences keep actual source level and cause. Metadata remains on the actual native entity, so no player-local private timer is introduced. Counter diagnostics do not qualify effects by themselves.

At freeze this task has not compiled or run the new service. Required remaining root proof: all47 shared producer/real consumer routes; world-RNG boundaries and conditional draw order with subsequent native RNG preserved; native real consumption and one debit; latch normal/early/repeated/reset/U+E202 semantics; TNT15 native hit/terrain/WILD child behavior; direct Nexus coordinates/Y/modifier cleanup; exact source piglin and four shared bombers + real nuclear activation; native TimeStop/roles/epoch/lifecycle and exact fixture restoration. Full Chaos release remains blocked until that entire chain is qualified. No partial raw-score or spawn-only path is represented as full engine closure.

## Source files / hashes

- [lbc/advancement/eat_oreshek.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_oreshek.json) - `fef1e891de1f1af6853db43af3cd0322ff7c89dfdbb7f1c9059cc4e76bad5d31`
- [lbc/loot_table/oreshki_10.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/oreshki_10.json) - `a8d185bd8592365d3efbe517d00894040bf8913129187e334eef8bc35a85ff72`
- [lbc/function/random_debuff.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/random_debuff.mcfunction) - `036e2af51b9a2a49d6ea1973a9bf5ab5e114fec16aa1230164271ca4f82102c3`
- [lbc/function/other/eatables/oreshek.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction) - `0dbe68c779619f62a48d18c82b0e25739c1aad4e8aadb58e8de2c4e94ea9f29d`
- [lbc/function/other/eatables/oreshek_explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek_explode.mcfunction) - `66ec0da87858e3f6685abd4afc44ca235d68e819c6b9dd72366ca8635859ef9c`
- [lbc/function/other/eatables/oreshek_zelensky.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek_zelensky.mcfunction) - `bb42f294f86da69dc83266d08d906614b9fbfa2e742f97c254287c90ce47ea92`
- [lbc/function/tp_nexus_rtp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tp_nexus_rtp.mcfunction) - `1848809ef5509e995a8237f7a722a94774f901807f224c02445702bf931131d0`
- [lbc/function/tick_nexus_player_tp_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_player_tp_3.mcfunction) - `4598450adb156c837c86371091c49f074eb787419b713f76c88569634108e344`
- [lbc/function/leave_death.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) - `71fde472367495b17a72c5eaac1039161b3992cd642683994ccc062a763ec715`
- [lbc/function/resets.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) - `c10ba440615050e16d2fcb55d6a8032c289ba0cc462da3c29c1c9de59f65d9cc`
- [lbc/predicate/chance10.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance10.json) - `8ac1cc3ce1736d9d5f2300a2ba82f0c6c7164e30a8d0bca71707955ae350b50a`
- [lbc/predicate/chance1.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance1.json) - `a3944d3cac7a486c3344c8c3a89b2edb77a36d2d9e9013d611db1ec0d828b8b4`
- [lbc/predicate/chance001.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance001.json) - `e61e66d8a584ff7d05be80ec2553494b33fcc0b9043fbfcdd521854313d383d9`

Nuclear transitive source and service: [SOURCE_NUKE_PORT_2026_10_29.md](SOURCE_NUKE_PORT_2026_10_29.md). Full food resource graph and existing consumers: [FOOD_REWARD_SOURCE_2026_10_29.md](FOOD_REWARD_SOURCE_2026_10_29.md).

Additional reset boundary sources: [game/start/0](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/0.mcfunction), [game/end/0](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/0.mcfunction), [developer/game/start/0](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/developer/game/start/0.mcfunction), [developer/game/end/0](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/developer/game/end/0.mcfunction), [core/player/reset_pre](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/player/reset_pre.mcfunction), [core/player/reset](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/player/reset.mcfunction).

Existing plugin boundary implementation: [SwrgNativeService.java](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:1875).


## Bounded OreshekChecks candidate (not yet native proof)

New `OreshekChecks.run(game)` uses the actual native source oreshki_10 nut and
`ItemStack.finishUsingItem`, preserving one debit and native food/consume
components. A scoped proxy fixes only world random47 and the actual Oreshek
rare-branch float calls; unrelated native random delegates to the original.
Tests target fallthrough, firstTNT early-return latch, subsequent nativeconsume
script suppression, idempotent reset/glyph packet, exact native Nexus transfer,
role/TimeStop admission and all47 actual shared producer selections. The raw30
branches start at7 and must become37; existing queue/Frost/Psycho/KFC/Kiuas/etc
ownership is inspected immediately, without ticking dangerous counters.

The real radius15 firstbranch detonation runs in an existing loaded column at
maxY+256, with all other existing entities required farther than32 and the full
blast volume above the terrain build ceiling. This isolates actual native
player damage while no terrain can intersect the blast; it is not an intercepted
or fabricated explosion. All player attributes/effects/combat/stats/scores/
advancements/food/tags/inventory and service bookkeeping/attachments are saved
and restored in finally using the prior fixture pattern. RNG references restore
in their own finally blocks. No production clocks advance.

These are candidate checks pending the root build/native run. They do NOT
qualify Zelensky piglin/fourbombers, delayed700tick bruh detonation, full nuclear
terrain/cascade/peakload, actual death/disconnect/match-boundary integration or
all47 downstream actions over their source lifetimes. Stand/Hedgehog source
marker versus existing common artifact policy remains separately unresolved.
