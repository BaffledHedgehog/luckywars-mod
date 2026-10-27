# Feeding, Vampire and Guardian melee port - 27 October 2026

Status: **QUALIFIED** as `d039db36`; automatic installation is queued for normal main shutdown. Main disk remains `1a8a3825`. No main stop or restart was forced.

Implementation: [LbcClassicMelee.java](../src/main/java/me/skywars2/game/LbcClassicMelee.java), integrated through [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java). Exact source paths, original-byte hashes, loot fields and resource text: [classic melee source audit](CLASSIC_MELEE_SOURCE_2026_10_27.md). Source descriptions below distinguish native engine timing from the literal command argument.

## Admission and lifecycle

The shared accepted-damage hook handles positive, unblocked non-arrow damage, including fatal and player-attributed indirect hits. The attacking player's current mainhand custom_data selects feeding, vampire or seaguardian. Arrow-only damage does not trigger them; owner dimensions nexus/imprinted are excluded. This is the legacy accepted-hit contract, not an added RMB action.

Native match/owner/observer/role, action-blocking and Time Stop guards remain authoritative. Mainhand flag filtering prevents unrelated item hits from running the effect and area-query branches. Feeding intentionally bypasses magic suppression; Vampire and Guardian respect existing owner/nearby antimagic suppression. Established allied-player protection, technical-role exclusion and friendly-mob targeting are retained. Guardian uses the shared target helper, including existing Greed eligibility rules.

No new item registry, private cooldown, charge, mana debit, carrier or retained service timer is introduced. Effects stay in native entity state and inherit the attacking player through addEffect. Existing independent signed effect layers, duration, caps, suppression and match-boundary cleanup apply. Do not add a separate immediate heal or fixed hunger refill to mimic these effects.

## Three-family behavior

| Family | Exact native behavior | Feedback |
|---|---|---|
| Feeding / Steak Sword | Saturation duration1 TICK, amplifier127, ambientfalse, particlesfalse, iconfalse, attributed to owner; food_count+1. | generic.eat and player.burp master at owner, volume1/pitch1, source global audience. |
| Vampire Fang | Regeneration20 ticks, amplifier4, ambientfalse, particlesfalse, iconfalse, attributed to owner. No direct heal/lifesteal fraction or child damage. | No added source sound or particle. |
| Guardian swimming | Actual owner.isSwimming() always selects this branch without consuming RNG. Eligible living targets distance0.01..7 receive mining_fatigue1200 ticks amplifier127 and weakness200 ticks amplifier1; owner receives water_breathing200 ticks amplifier2. | elder_guardian.curse master at owner volume1/pitch1;10 forced elder_guardian particles at owner with spread5/5/5 and speed1, sent individually to eligible player targets. |
| Guardian dry | Only when not swimming, nextInt(4)==0 selects branch. Same target area receives mining_fatigue400 ticks amplifier127 and water_breathing40 ticks amplifier1. No owner breath grant. | Same sound/targeted particles as swimming branch. |

Source Feeding command is `effect give @s saturation 1 127 true`. Native EffectCommands.java:170-178 gives instantaneous effects the supplied duration directly: argument1 means1 tick, not20 ticks. The earlier source-prep prose calling it a one-second grant is literal-command shorthand and must not drive a20-tick implementation. Vampire's non-instant regeneration argument1 correctly means20 ticks. Native effect scheduling and hunger/health rules determine actual changes.

Guardian checks the swimming flag, not merely being wet or inside water. It is a radius proc, not a hit-victim-only effect. Dry water_breathing on enemies is literal source behavior. Source nonliving effect selectors are represented by eligible native LivingEntity targets; role/ally policy supersedes unsafe legacy selectors. No target HurtTime filter or additional RNG is added.

## Five exact loot leaves and acquisition

Compiled leaves:

- lbc:ported/feeding_sword
- lbc:ported/legacy_feeding_sword
- lbc:ported/vampire_fang
- lbc:ported/legacy_vampire_fang
- lbc:ported/guardian_sword

These preserve vanilla wooden_sword or iron_sword item identities, their ordinary durability/placement-independent weapon behavior and exact source components. No custom registered sword id replaces these stacks. Current transformable flags and legacy minimal flags remain distinct. Source names, lore row order, omitted style properties, tooltip tiers, models and Vampire attack-damage modifier ids are preserved by the exact imported leaves; see source audit for the compact component table.

The existing compiled transformables_sword pool already contains the three current families and remains unchanged. Their real transformation/reforge path now reaches the new live flag handlers. Explicit current/legacy leaves are included for exact catalog/operator acquisition; this does not claim the entire legacy luckyblock pool is newly enabled. No dedicated ingredient recipe or new right-click craft is invented.

Existing models lbc:steaksword, lbc:vampiresword and lbc:guardiansword and original translation keys are reused. No RP/template changes or language duplicates are needed. Existing source advancement definitions remain authoritative: Steak durability exhaustion, Vampire killing a target in the no_blood tag with matching current mainhand flags, Guardian hitting a player while swimming with matching current flags. Native checks triggered all three actual imported criteria, including a broken Steak Sword, a fatal Skeleton hit and a swimming hit on a player; imported parents, task frames, icons, models and text are checked separately.

## Native qualification and remaining work

`ClassicMeleeChecks` and `tools/smoke_classic_melee.py` passed two actual mode1 matches in76.08 seconds:74 recorded suite results,8 preparation results and2 scoped participant setup/cleanup results. The exact462 Java sources,1285 compiled classes and3155 resources match the tested JAR. Real native attacks, accepted external non-arrow and fatal damage, instantaneous Saturation and FoodData, Regeneration healing/hidden-layer expiry, swimming and dry25-percent RNG branches, source effect authors, player-only particles, sound audiences, team/Greed/role guards and all three actual source advancement criteria pass. Shared Late Swords, Fevgalea, Sauvojen, grimoire, player/projectile, Time Stop, signed effects/layers/FOV and firearms regressions also pass.

One rejected first run remains under `build/verification/classic-melee/attempt1/`. Its self-damage control used ordinary owner-attributed damage on a team with friendly fire disabled, so native admission rejected it before the callback. The final control captures the existing WILD cause, requires actual accepted native damage and checks the resulting effect. Production team rules were unchanged. Both runs exited normally and restored saved fixture files; the scoped third native participant is removed by normal disconnect after the final match.

Ready archive: `build/verification/skywars2-classic-melee-d039db36.jar` (39,745,913 bytes), SHA256 `d039db36808e496b4131eddd3c19035d987073dc4bd3bd93c02174590cfe5950`. Immutable qualification, raw native log/results, exact imported-resource provenance and current installer receipt are under `build/verification/classic-melee/`. Installer11424 adopted the qualified request and waits for main port24566/world lock release. Retained archives are this build, preceding Late Swords8a94c831 and exact installed1a8a3825; obsolete Fevgaleab8140165 was hash-verified and retired. All175 protected author paths and31 live grimoire overrides remain byte-exact.

Physical client pixels/audio, internet latency and peak TPS are unobserved. TPS compensation remains feasibility-only. The next [Ender/Devil audit](ENDER_DEVIL_MELEE_SOURCE_2026_10_27.md) is source preparation, not an implementation; native teleport selection and instant self-damage attribution need separate proof. The full [Epic/Fragment audit](EPIC_FRAGMENT_SOURCE_2026_10_27.md) is ready, but scheduled fang overlap and extreme feedback choices remain pending in the author's question workflow; this batch enables none of those four variants. Piston, Chaos and Gravity remain unenabled source-preparation work. Existing unrelated qualified families are outside this change.
