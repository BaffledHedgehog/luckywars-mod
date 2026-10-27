# Liquid Abyss: source damage, status interaction and native fixture contract

Read-only audit, 27 October 2026. Only this new note is written. No gameplay code, fixture, runner, build, server, Git, RP, source or older note changes. This is a primary-source contract and proposed verification, not a runtime qualification.

Source root S: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Native source cache J: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.
Vanilla data V: adjacent `minecraft-merged-d6f9bb6878-26.3.jar`. Local overrides R: `src/main/resources/data/minecraft/tags/damage_type/`.
Related source inventory audit: [ARMORED_INVENTORY_SOURCE_2026_10_27.md](ARMORED_INVENTORY_SOURCE_2026_10_27.md). That note's warning against assuming two accepted hits is resolved here by the actual damage tags and pipeline, not by adding a damage multiplier.

## Exact activation and independent source calls

`tick2s:1` schedules itself after 2s; `:2` dispatches outside Nexus/Imprinted. `tick2smobs:6` dispatches `players_2s` for nonspectator players. `players_2s:4` independently probes `container.*`; `:5` independently probes `weapon.*`. Both invoke the same helper. Native player `getSlot` exposes container indices only in the 36 non-equipment slots; weapon includes selected main hand and offhand. A selected main-hand stack therefore satisfies BOTH probes. A source-ordered implementation must re-evaluate the second probe rather than replacing the two calls with one boolean.

Assuming a living admitted player, unchanged inventory and accepted raw damage without absorption:

| Physical location of matching stacks | Source calls per 40 ticks | Raw requested total |
|---|---:|---:|
| Backpack only | 1 | 4 times full clear count |
| Selected main hand only | 2 | 8 times full clear count |
| Offhand only | 1 | 4 times full clear count |
| Backpack plus offhand, or main plus offhand | 2 | 8 times full clear count |
| Armor/crafting/cursor only | 0 | 0 |

There is no third call for two occupied hands. Fatal first damage or changed admission/inventory may prevent a later meaningful call; two selector lines do not require hurting a dead or removed participant. Ordinary nonfatal equal-amount calls are independently accepted despite native hurt cooldown, as shown below.

`other/liquidvoiddamage:1` returns only when ALL are true: global `legacy lbc.math` is NOT 1, global `#lbcskill swrg.math` is 1, owner `lbc.skill` is 2, and neither weapon hand contains the flag. Thus current + enabled skill2 is hands-only; disabled skills or any other skill preserves ordinary inventory activation; legacy1 bypasses ONLY this helper guard. It does not change counting, cadence, damage type or void resistance. No distinct old item flag/leaf is established.

No existing `GameConfig` legacy-mode field or native writer/reader of `legacy lbc.math` was established in this audit. A future integration must state its actual compatibility representation; do not infer legacy mode from item identity or silently invent a new selectable mode. `config.lbcSkills` and actual profile `lbcSkill` are established native equivalents for the skill switch. Skill2 remains unselectable until its wider family contract is complete.

The helper has no marker/beacon, artifact aura, personal nomagic, mana, RNG, durability, consumption, private score, task, cooldown, title, sound, particle or input action. Native shared role/map/ending/Time Stop rules still apply. Passive effects continue under active-input stun/ice under the author rule; do not accidentally route this through an active-use gate.

## Full `/clear 0` count and numeric semantics

Helper line2 stores the result of `/clear @s *[minecraft:custom_data~{liquidvoid:1}] 0` in storage `lbc.math tmp` as **int with scale4**. Line3 invokes the macro helper with this storage. The macro tests positive `void_resistance` then calls native damage with that stored number.

J `ClearInventoryCommands:73` sets `countingOnly = maxCount == 0`, calls `Inventory.clearOrCountMatchingItems(predicate,true,0,craftSlots)` and skips the noncounting broadcast/slotsChanged branch. J `Inventory:182` visits the whole physical inventory, then four personal crafting cells, then cursor. Source 1.21 inventory contains its original 41 slots; native 26.3 inventory contains 43 including body/saddle, so reusing the actual native clear utility counts those compatibility equipment slots too. They do not independently enable the container/weapon dispatcher. A stack is counted once physically even if selected main hand also satisfies the weapon trigger. Each helper invocation recounts it.

J `ContainerHelper` sums each matching physical stack's actual `getCount()`. It does not recurse into shulker/container/bundle contents, nested custom-data compounds or charged projectiles. A carried outer stack with matching top-level custom data contributes its own count only; merely containing a matching inner stack does not contribute. Predicate matching is the native partial custom-data predicate, not a substring scan of SNBT or any descendant `liquidvoid` key. Extra unrelated custom data is retained.

Counting-only does not shrink, replace an empty cursor, call `setChanged`, consume an item or mutate components. Storage `tmp` is an internal source macro carrier; there is no player-facing persistent counter to introduce. Native inventory/equipment/craft/cursor snapshots must remain byte/component/count equivalent after a nonfatal pulse.

J `ExecuteCommand:306` uses `IntTag.valueOf((int)(v * scale))`: multiplication is in double, then Java double-to-int narrowing **saturates**, rather than wrapping integer multiplication. For nonnegative returned int C, source tmp is `min(INT_MAX, 4*C)`; native damage subsequently parses that decimal as float. For C=536870912, tmp=2147483647 and float damage rounds to 2147483648; Java `int C*4` instead wraps negative and is wrong. Preserve the actual clear utility's int-return behavior; if malformed overfull stacks overflow its own int sum, do not invent arbitrary-precision counts or positive damage. A negative macro amount cannot pass the native nonnegative damage argument. Normal source-generated stacks do not approach this range, but a bounded oversized-stack/native-clear numeric control can prove the conversion without applying a fatal huge hit.

## Source and effective native damage tags

`liquidvoiddamage_recur:1` supplies `outside_border` with NO `by`/`from` entity. J `DamageCommand` constructs an environmental `DamageSource(holder)` for this grammar; `:96` invokes actual `target.hurtServer` and reports acceptance. Do not add owner as direct/causing entity, a WILD marker, a player attack type, a manual health debit or a fake projectile.

Source datapack tags explicitly place `minecraft:outside_border` in bypasses_armor, bypasses_cooldown, bypasses_effects, bypasses_enchantments, bypasses_invulnerability, bypasses_resistance and no_knockback. Source bypasses_blocking inherits `#minecraft:bypasses_armor`.

V outside_border data is exhaustion0, message_id outsideBorder, scaling when_caused_by_living_non_player. With null causing entity, difficulty scaling does not apply: no HARD 1.5 multiplier or EASY adjustment should be invented.

The effective native tag union, read from V then R with normal replace/union and recursive tag references, contains outside_border in:

| Effective tag | Origin / implication |
|---|---|
| bypasses_armor | Vanilla V already contains outside_border; local file adds custom types with replace=false. Full native armor/toughness is bypassed. |
| bypasses_cooldown | R explicitly restores source membership. Consecutive equal calls bypass `damageCooldownTime > 10`, not merely the red hurt animation. |
| bypasses_effects | R explicit. Native magic absorption returns before Resistance/enchant protection. |
| bypasses_resistance | R explicit; signed Resistance mixin also returns unchanged for this tag/effects bypass. |
| bypasses_enchantments | R explicit, additionally covered by effects bypass. |
| bypasses_invulnerability | R explicit. Native generic temporary/permanent and ability invulnerability exceptions apply; this does not bypass every independent admission rule. |
| bypasses_blocking | R inherits bypasses_armor. |
| bypasses_shield | V inherits tags; actual 26.3 runtime identity should be asserted, retaining version compatibility. |
| bypasses_wolf_armor | Vanilla V, including invulnerability inheritance. Not a player-item mechanic. |
| no_knockback | V already contains outside_border; R appends custom types. No default damage knockback. |

The static closure finds NO outside_border membership in fire/projectile/explosion, no_impact, no_anger, arrow_only or acceptable_for_manavampirism. Do not turn this inventory hazard into an accepted attributed sword hit/arrow or erase all native hurt reactions. Source helpers add no explicit feedback, but native hurt/death events/sounds, damage stats and normal callbacks may still occur.

J `LivingEntity.hurtServer:1171` checks native cooldown with `damageCooldownTime > 10 && !BYPASSES_COOLDOWN`; bypass takes the full-damage branch, updates lastHurt/cooldown/hurt animation, then resolves native attribution. J `Player.actuallyHurt` still processes ordinary absorption AFTER armor/effect bypass. Therefore two admitted nonfatal calls normally request and apply their own full amounts; no manual reset of hurt cooldown and no coalescing into one larger hit is justified.

## Status, invulnerability and attribution boundaries

- **Positive absorption remains real protection.** BYPASSES_EFFECTS does not skip `getAbsorptionAmount` subtraction in Player.actuallyHurt. A yellow buffer6 against two raw4 hits yields yellow0 and visible HP loss2, absent other policy; test the actual native buffer rather than assuming bypass means direct HP.
- **Negative absorption is shadow health, not a hit buffer.** `ShadowAbsorption` hides a segment in its stored ledger and changes visible maximum. Its implementation explicitly preserves native damage/death; stored shadow HP never intercepts a hit. Compare actual visible HP and complete ledger (entries, independent timers, capacity/stored/visibleMaximum/positiveCapacity/flags) before and after. Do not clear and reconstruct only the active effect icon.
- **Signed Resistance is bypassed.** `SignedResistanceMixin` returns damage unchanged for BYPASSES_EFFECTS or BYPASSES_RESISTANCE, including negative/overflowing signed levels; it must not heal a Liquid Abyss hit or increment resisted stats.
- **Greed is absolute.** GreedGuardMixin priority2000 rejects hurtServer before native immunity exceptions, and setHealth rejects direct loss. LbcDamage/LbcWands also reject it. outside_border does not grant a Greed exception.
- **Creative invulnerability is not immunity here.** The imported native bypass tag passes the ordinary Player ability check; CreativeVoidDamageMixin additionally preserves creative environmental behavior for border/void. Keep a positive native command control. Do not confuse survival abilities.invulnerable with Creative mode or assume a tag defeats removed/changing-dimension/unloaded-client checks.
- **Native loaded admission matters.** J ServerPlayer.isInvulnerableTo independently rejects changing dimension and `!connection.hasClientLoaded()`. A fake-player listener must acknowledge actual loaded state. Dedicated scheduler registration is separately needed for actual effect/player updates.
- **Void resistance is helper-local.** Source suppresses calls only for scoreboard `void_resistance>=1`. It is not a generic outside_border immunity in LbcDamage. Zero/negative values admit. The helper does not decrement, consume or reset it; shared `LbcMana.voidProtectionSecond` naturally decrements positive values per admitted second and can rescue Y<0. Fixtures must separate that normal clock from the helper's lack of consumption.
- **Environmental identity and old hostile credit coexist.** LbcIndirectKills.environmentOwner has no outside_border hazard mapping, and damaged() only special-cases FALL/FELL_OUT_OF_WORLD for stored fall causes. Plain border damage should not acquire a synthetic owner/capture stamp or overwrite the existing laterAttacks/status maps. Native previous hostile kill credit can remain; asserting `getKillCredit()==null` after a clean environmental hit is invalid when a real prior attacker existed. Seed a native hostile hit, preserve its credit, then observe unowned border damage. Fatal attribution should use a disposable nonparticipant or a separately authorized real player death fixture, never manual dying/CriteriaTriggers.
- **Shared guards remain effective.** SkyWars2 ALLOW_DAMAGE -> LbcItems.allowDamage -> LbcDamage/Wands/family guards still execute. Player match/observer protection and defensive spell guards are independent of vanilla tags. TimeStopDamageMixin can queue raw environmental hits, but the future source passive scheduler must pause before submitting new hits during authoritative active Time Stop, avoiding a newly queued recurring inventory debt.

## Proposed meaningful native verification

1. Use a side-effect-free fixture constructor; initialize inside try/finally. Capture all participants before detaching originally owned native states. Use actual scheduled current-map participants and the proven fresh **ordinary registered Connection + normal listener** pattern. No direct player/world/effect/listener ticks and no FakeClientConnection no-op tick claims. Wait for observed real listener update and a strictly later server tick before spatial queries. Snapshot full raw scores including entry absence, config skill enablement, actual profile skill/legacy representation, status ledgers, combat tracker, previous source/hostile credit, effects/hidden layers/shadow state, equipment/attributes/cache, inventory43/craft/cursor, full food NBT, stats, advancements and connections. Restore profiles last after native award callbacks.
2. Before ability expectations, assert runtime outside_border tag membership and independent native `/damage <uuid> 4 minecraft:outside_border` acceptance. Use no attacker, positive HP, loaded state and identical armor/effects/invulnerability conditions. Submit two actual commands in the same native turn without cooldown reset and assert exact total8 plus genuine last source/direct/causing identity. Add native normal non-bypass damage as a contrasting cooldown control only if necessary; no artificial cleared cooldown between the two border hits.
3. Exercise full slot/trigger matrix above. Count using the same native clear0 utility and independently compare a real clear command return while checking all stack refs/count/components remain unchanged. Add armor, craft, cursor and new body/saddle contributions after activation; bundle/container nested matching contents contribute zero unless their outer stack itself matches. No recursive inventory scan or inventory cache stale after selected-slot/offhand changes.
4. Script actual shared40-active-tick cadence, not 40 service calls under a claimed native scheduler. Establish the exact native phase, observe ticks1..39 no damage, tick40 one/two expected raw environmental requests, tick80 repeated same inventory without consume. Change selected hand/config/profile between real phases to prove enabled current skill2 hand admission, disabled skills ordinary behavior and actual legacy1 exception. No enabling the broader deferred skill2 catalog.
5. Cover zero/negative/positive void resistance, ordinary armor+toughness+Protection, signed positive/negative Resistance, yellow absorption, negative shadow ledger, real Creative and invulnerable flags, Greed, observer/technical/map/ending, authoritative Time Stop, and actual passive continuation under shooter stun/ice. Keep independent source counts and exact native absorption/HP results; avoid vague `health decreased` checks. Normalize unrelated profile/passives and positive async mana to avoid Overload pollution.
6. Prove no custom sound/particle/title/stat/item/mana payment beyond the independent native damage control. Damage-taken stats and native hurt feedback are expected pipeline outputs; snapshot/restore them rather than forbidding all native activity. Stop/owner removal/epoch/map unload must prevent future submissions and leave no cached item flags or delayed tasks; existing normal native effects/hostile metadata must not be stolen.
7. Acquisition: exact existing `lbc:voidman` first pool is guaranteed one vanilla repeating_command_block, model lbc:liquidabyss, tier_ff, dark_purple nonitalic liquid_abyss name and one same-style liquid_abyss.lore, top-level liquidvoid1 only. Keep all other original voidman pools intact. A raw roll proves table payload only. Qualifying acquisition additionally needs an actual already-live producer: native LbcConsuming reads the original Void Man preset with DeathLootTable lbc:voidman. Trace its real production spawn/kill route, verify native death loot contains the item, and scope all extra table outputs/actors. Preset/table packaging alone is not observed acquisition. No new legacy item alias, recipe, advancement, or fake operator leaf is required by the source.

The oversized-count numeric case can be checked against the native command store into a fixture-owned temporary storage path and restored exact storage afterward, without hurting a participant for billions. Do not replace the actual normal count/damage gameplay controls with this arithmetic test. A minimal fixture may avoid storage entirely by comparing the source-native cast derivation with independently observed native clear result and a moderate exact damage command; describe any unobserved overflow behavior precisely.

## Evidence fingerprints

The following hashes identify the read-only primary source bytes used here. Runtime loaded tag closure, scheduler timing, physical client feedback and acquisition still require the future native run.

```text
lbc/function/tick2s.mcfunction 011f1b6e3c4e250d94f368ca5e4377af41971bc4142c56dffe65576def91fb70
lbc/function/tick2smobs.mcfunction 4585f01a50f0dc194b3e6ccfbeb7ed90a1a33eb6c0117ef7e2eefb1a7c2a095c
lbc/function/players_2s.mcfunction abdb09f5ca07c52fa5adf335311d011d8ef3f643242d17d41905db521d9a1a5e
lbc/function/other/liquidvoiddamage.mcfunction f73ac66cc77c8125b3ba1279895f86beb5c9af3a9b283a9865f615659b370e01
lbc/function/other/liquidvoiddamage_recur.mcfunction d385a3ad7034f342bbcf99a57a0572ad76057ad7932ed9c849e6e7debd83cc16
lbc/loot_table/voidman.json 3eb5b1ad6c981fbbeea0247ea573a4bf1a87c370d6a6f66c7e7f18e084bc1b4a
source bypasses_armor.json 9b10ff856eeb534ec4b13fa45b7c9cf7306a343738cd95f07069c81836d62072
source bypasses_blocking.json 21b5daa3aecc772c64e19d89ffe2f6e977082b8bb429ea3c3e736a075aa7e66f
source bypasses_cooldown.json 219f71eda49d72c2e6c2a2c29add7215d47a9a33ff38e8be6fcf3a5b3e7bc17b
source bypasses_effects.json b2bced23f7d4af1f7eae9c3241ea0b7604bd308cb148efcd06aa47b088d62fa7
source bypasses_enchantments.json 4f5a2638d80be97c800fed3bbecacb42de82f6772dd65c4f01aea154d5f4ce41
source bypasses_invulnerability.json b5551e92fe8032d6c8943e249b782f1b65929d7eec0d2e7be7d85ea02bfd7426
source bypasses_resistance.json 4f5a2638d80be97c800fed3bbecacb42de82f6772dd65c4f01aea154d5f4ce41
source no_knockback.json 32cd67610f6ae331d590481b1f522d178f6cf2d75f78c70d2aaf1897ee726e17
src/main/java/me/skywars2/mixin/SignedResistanceMixin.java f4698172fd82ad330bc21ce47893b54192922a1ebc0f42218ca685f0e8349376
src/main/java/me/skywars2/mixin/CreativeVoidDamageMixin.java c9ae172a017cc729186c47728034fb9d5a1cd7a48aaee86dad6d5654800bbfa3
src/main/java/me/skywars2/mixin/GreedGuardMixin.java 56792e4bdba3ee1ed20377a34031cf1edbcf2fd5b6233372b63ffe2991251e39
src/main/java/me/skywars2/mixin/TimeStopDamageMixin.java 4129952b6f6a4adc0e15b5211240adf9809c93a4dd6810e80a80368477a077a8
src/main/java/me/skywars2/game/ShadowAbsorption.java df51323d92ac0fb4ce70b3daf029eece59c10521deee8760cf1d671e2577b9ea
src/main/java/me/skywars2/game/LbcIndirectKills.java f8a2a10cdf104cd133c1c2b57eba9916e2d15dc66b5f5e8a85055f90b54f75fc
```

Frozen audit only. No Liquid Abyss engine/check/runner or six-bomb/Touhou implementation/qualification is claimed. Those families retain their separate complete source contracts and admission/ownership obligations.
