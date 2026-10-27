# Devil Sword - 27 October 2026

Status: **QUALIFIED** as `5b5b67e3`; automatic installation is queued for normal main shutdown. Main disk remains `1a8a3825`; no main stop/restart was forced. Exact source/helper hashes, current/old components, original acquisition and existing RP text: [Ender/Devil source audit](ENDER_DEVIL_MELEE_SOURCE_2026_10_27.md).

Implementation: [LbcDevilSword.java](../src/main/java/me/skywars2/game/LbcDevilSword.java), integrated through the accepted-damage hooks in [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java) and [SkyWars2.java](../src/main/java/me/skywars2/SkyWars2.java).

## Input and native state

Accepted positive, unblocked non-arrow damage attributed to a player checks the current mainhand `heal:1` flag, including fatal and attributable indirect hits. This is the actual source advancement input, rather than an invented RMB action or a melee-only restriction. Ordinary role/match/owner, observer/technical-role, input-blocking, Time Stop and magic-suppression admission applies; source nexus/imprinted exclusions remain. No mana payment, private charge, cooldown, carrier, item registry or retained service timer is introduced.

The active Strength amplifier is read separately before each descending source condition. Hidden lower layers do not satisfy these conditions. A specific upper helper does not skip the later ANY-Strength helper2: its lower Strength/Speed grants remain real independently timed layers under the already approved shared effect policy. A hit with no Strength reaches only helper1 at the end.

| Starting active Strength | Ordered helper calls | Upper resulting effects |
|---|---|---|
| Absent | 1 | Strength I |
| Amplifier0 | 2 | Strength II, Speed II, Nausea II |
| Amplifier1 | 3, then2 | Strength III, Speed III, Nausea II |
| Amplifier2 | 4, then2 | Strength IV, Speed IV, Nausea II, Instant Damage I |
| Amplifier3 | 5, then2 | Strength V, Speed V, Nausea II, Wither IV, Instant Damage II |
| Amplifier4 or5 | 6, then2 | Strength VI, Speed VI, Nausea II, Wither VI, Instant Damage III; explicit `swords/devil` award |
| Other admitted signed/full-int amplifier | 2 | Ordinary independent native grants; a stronger preexisting active layer is retained |

Every ordinary grant lasts200 ticks with source-default particles/icon/ambient properties. Instant Damage lasts1 native tick, preserving `/effect` instantaneous-duration semantics. Effects go through `ItemEffectLevels` and the existing signed formulas/layer system; Wither VI already equals the approved item cap. Do not replace these native effects with a fixed direct-damage approximation or add another self-damage timer.

Self-cast effect grants keep the player as effect source. Actual scheduled Instant Damage and Wither use native null-entity MAGIC/WITHER damage through the shared effect implementation. The indirect-kill ledger deliberately does not turn self-cast effects into player-owned hostile effects. Consequently their later damage cannot re-enter this sword's player-attributed hit ladder. Existing hostile damage credit, defenses, Greed and native owner death behavior remain authoritative; no synthetic self-attack credit or recursion guard is invented.

## Acquisition, presentation and achievements

`lbc:ported/devil_sword` imports entry3 of the original ordinary `transformables_sword` pool; `lbc:ported/legacy_devil_sword` imports entry64 of `legacy_luckyblock`. Both remain vanilla wooden swords with exact source component arrays, modifier identifiers and omitted properties. The shared source name, current tierF/type row and old distinct lore/flags/modifier id are preserved. Do not synthesize an omitted attack-speed modifier. The already compiled ordinary reforge pool remains unchanged and now reaches this live handler. Explicit old/current leaves also support scoped catalog/operator acquisition without enabling the entire old luckyblock pool.

The existing `lbc:devilsword` model and original translation keys are reused. No RP/template changes are required. Helper6 grants the existing task `lbc:true_advancements/swords/devil`; its separate child `devil_full` remains the original five-item inventory challenge. Native checks triggered the real helper6 grant and real inventory-menu changes: four matching source items do not complete the challenge, while adding the fifth matching accessory does. The accessory is a criterion fixture, not proof of its own unported ability; donor abilities are not invoked. Imported parents, frames, icon/components and text are checked; prior advancement progress is restored.

## Verification and remaining work

Two actual mode1 matches passed in96.01 seconds:76 recorded suite results,8 ordered terrain-preparation results and2 scoped participant setup/cleanup results. Exact464 Java sources,1291 classes and3157 resources match the tested JAR. Checks cover both leaves and their original ordinary-pool payload, every helper and signed/full-int Strength branch, independent lower-layer clocks, real melee/non-arrow/fatal/self admission, mana-free behavior, source magic/role/hand guards, both actual advancement routes and full scheduled effect expiry. Shared Classic Melee, Late Swords, Fevgalea, Sauvojen, grimoire, player/projectile, Time Stop, signed effects/layers/FOV and firearms regressions passed.

The asynchronous proof registers exactly two owned memory connections in the real server connection list. Unmodified native `Connection.tick` invokes native listener/player updates; the fixture never calls player, world, effect or listener tick directly. Native listener counters must advance together before observations. Harm I removes exactly6HP; source Harm III/Wither VI matches an independent native `/effect` control throughout200 actual updates. Existing hostile credit is retained, no self-owned damage entity is invented, and no recursive effect refresh occurs. Wither, strong/weak Strength/Speed and Nausea layers expire naturally. Owned connections are unregistered and exact prior listeners, food NBT, stance, movement, profiles, stats, effects and advancement state are restored.

The rejected first run is preserved under `build/verification/devil-sword/attempt1/`: ordinary test fake connections did not participate in the native connection scheduler, so server-clock advancement alone did not update effects. Only the fixture scheduler was corrected; production behavior and assertions remain. Both runs shut down normally and restored saved fixture bytes. An initial PowerShell log capture also ended on javac's deprecation warning; the retained daemon log proves the build actually passed, and a terminal up-to-date build confirmed it before native testing. Final changed checks were rebuilt successfully before the passing run.

Ready archive: `build/verification/skywars2-devil-sword-5b5b67e3.jar` (39,779,266bytes), SHA256 `5b5b67e3f44c5ff82d1ada024c3bb814a5df2309ca8a924a8dccd0a00d0c101d`. Immutable qualification, raw results/log, source provenance, failed-attempt evidence, current install receipt and cleanup receipt are under `build/verification/devil-sword/`. Installer11424 adopted the new qualified request and waits for normal main port24566/world-lock release. Retained archives are this build, preceding Classic Meleed039db36 and exact installed1a8a3825. Obsolete Late Swords8a94c831 was hash-verified and retired; cumulative cleanup334JARs /11,944,468,666bytes. All175 protected author paths and31 live grimoire overrides are byte-exact.

Physical client pixels/audio, latency and peak TPS remain unobserved. TPS compensation remains a feasibility question and is not introduced here. Ender, Antimagic and Omen nearby-HurtTime targeting choices are deferred in [PENDING_QUESTIONS.md](PENDING_QUESTIONS.md); those families are not enabled by this batch. [Antimagic/Omen source preparation](ANTIMAGIC_OMEN_MELEE_SOURCE_2026_10_27.md) records the existing shared timer/terrain services, which must be reused rather than duplicated.
