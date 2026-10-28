# Complete Phantom armor вЂ” 28 October 2026

## Author decisions

The author explicitly chose:

- Preserve existing Invisibility II and Strength II when an enemy approaches;
  proximity alone stops their refresh, rather than immediately clearing them.
- Automatically resume the set when suppression/enemies no longer prevent it;
  repairing the source's reliance on a subsequent inventory change.
- Remove only the set's contributions, preserving effects from other sources.
- The blinding flash requires an actual melee hit, including hits against mobs;
  spells and other attributed non-arrow damage no longer trigger it.

The subsequent clarification overrides the original action behavior (user):

> СЃРЅРёРјР°Р№ РЅРµРІРёРґРёРјРѕСЃС‚СЊ СЃ СЃРёР»РѕР№ РїРѕСЃР»Рµ Р»СЋР±РѕРіРѕ РІС‹СЃС‚СЂРµР»Рµ/РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРё РїСЂРµРґРјРµС‚РѕРІ/СѓРґР°СЂР° РјРёРЅРёРјСѓРј РЅР° 1 СЃРµРєСѓРЅРґСѓ (РµСЃР»Рё РёРіСЂРѕРєРё СЂСЏРґРѕРј - С‚Р°Рј РєР°Рє РІ Р»РµРіР°СЃРё - Р¶РґР°С‚СЊ РїРѕРєР° РѕРЅРё РЅРµ СѓР№РґСѓС‚)

Implementation uses twenty active ticks after the most recent action. Repeated
actions extend the lock; they do not accumulate separate timers. A nearby enemy
still prevents reactivation after this minimum expires. Native item-use charging
keeps the bonus disabled throughout use and for the minimum afterwards. The
opening melee hit retains its Strength bonus before withdrawal.

## Source and complete equipment

Legacy paths are relative to
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`:
`items/changed_inventory/sel.mcfunction:93вЂ“95`, `players_5t.mcfunction:17`,
`armor/phantom_set_bonus*.mcfunction`, `workingitems/phantom.mcfunction`,
`items/swords.mcfunction:42` and `advancement/gowno.json`.

All four original leather pieces retain durability600, tier_c, armor3/6/5/2,
original attribute IDs, red nonitalic names, all seven lore rows, equipment/model
assets and crafting/transformable/slot flags. Source item registration is not
replaced with new item types. Existing normal reforge uses index12, weight1 in
each slot pool; Ultra reforge selects the original family. Root gives one random
piece; `_pre` makes five draws with replacement; `_full` gives all four pieces.
The two full/pre roots become packaged, and all seven tables enter the ready
loot catalog. Other unresolved armor pools are unchanged.

Original Phantom advancement/loadout predicates and source Mage-combination flags
and lore are retained. This patch does not claim the downstream femboy set's
separate runtime/crafting migration. All existing RP dependencies and18used keys
are present in RU/EN/RPR. The actual author pack remains read-only; no RP update.
Thirty-one source/resource hashes are archived in this verification scope.

## Shared runtime

`LbcPhantomArmor`, owned by the common armor service, grants hidden InvisibilityII
and StrengthII for6000ticks on equipment admission and every five active ticks
when there is no enemy player in the exact0.01..8sphere and no Magic Stopper.
Enemy proximity preserves already-issued effects. MAG or removal withdraws only
owned bonuses. Ordinary stun does not disable this equipped passive. Role,
dimension, Time Stop, match epoch and normal lifecycle rules remain shared.

Accepted melee uses `LbcDamage.isMeleeAttack`; it flashes even on a mob hit, as
the original producer did. The original visible BlindnessI100ticks goes to
enemy players in the sphere. The fifty NORMAL dark dust particles retain
wearerY+1, spread0.3/0.6/0.3 and speed0.3, sent to other players. MAG and personal
no-magic suppress this magical flash; the general activity lock still applies.
Native rejected damage does not produce the flash. Effects retain real wearer
attribution through the existing effect/damage bridge.

The common native action wrappers observe consuming use/use-on/entity
interactions, attacks, spear hits and punch packets after their native outcome.
Method wrappers cover Fabric's early item-callback returns. A fresh-player-owned
projectile hook covers native and queued projectiles; the native shot flag and
an idempotent saved tag prevent reload/ordinary flight from becoming another
launch. Danmaku's successful card factory, the paid laser-minigun shot and the
held Void Arcanum's successful activation/upkeep explicitly cover producers
without a native projectile. Previously fired electro traces and detached
summoned drones are not reclassified as fresh held-item input.

## Effect ownership

`OwnedEffectLayers` extends the existing signed effect chain rather than copying
effect formulas. It retains an independent same-amplifier external baseline and
owner contributions, preserving different amplifiers in the native chain.
Accepted external grants made during a lease are retained too. Detached shadow
durations advance exactly at the native duration-countdown boundary, never by
wall time or separate world ticks; native pauses and infinite durations remain.
Clears and forced replacements discard stale ownership. Withdrawal rebuilds the
already admitted chain through native effect-update callbacks, retaining native
modifiers and presentation without rechecking a newly equipped cleansing item.
Absorption remains under its existing separate engine and is explicitly excluded
from this helper. Explicit player/epoch/stop cleanup removes owned state.

## Verification

`PhantomArmorChecks` verifies actual loot and all four normal/Ultra reforge
producers; complete/partial/backpack equipment, exact proximity boundaries;
external same/higher/lower/negative levels, mid-lease grants, independent native
ticks, two owner keys, infinite baseline, native force replacement/clear and
expired-grant rejection. Native bow input has a real survival arrow; fresh arrow
publication and actual player attack are exercised. Native accepted/rejected
mob hits, audience/team/particles, MAG, actual scheduled cooldown, Time Stop and
automatic recovery are checked.

The expanded two-match runner also covers Assassin/other armor sets, accessories,
native gear, consumables, signed formulas/layers, Time Stop/contact, Danmaku,
Void Arcanum, minigun visuals, rifle hand admission and physical projectiles.
It requires normal child shutdown and exact fixture snapshot restoration.

Initial native candidatef0b0206b passed the new Phantom tests in both matches but
failed the existing Void Arcanum standoff assertion twice. It is unqualified and
was never installed; its full log and results are retained. That fixture assumed
only two participating players while this runner also createsTestGamma. The
world-wide nearest selector could choose this bystander once the measured cow
entered its one-block standoff. The fixture now temporarily excludes unrelated
preexisting living actors using the existing technical-role guard, logs them and
restores only the tags it added. All original displacement, distance, velocity
and tilt assertions remain. This is fixture isolation, not a chase behavior change.
The earlier fixture-only renamed invulnerability setter/import compilation
corrections precede the final build.

Final build `723b2a3a` passed38recorded results in two actual mode1matches
(123.62seconds), plus two scoped third-participant setup/cleanup checks. Exact
539sources,1532classes and3230processed resources match the tested JAR. All
198protected author paths and29live grimoire override JSONs remain unchanged.
Normal child stop and exact fixture snapshots restored. Final native results,
source/resource proof and exact-artifact proof are archived beside qualification.

Candidate dffc4190 had passed the same expanded run before the author's target-
MAG answer. It was never queued/installed and is superseded by723b2a3a, whose
complete final run includes the target-only MAG test. Both pre-answer results
and the original failedf0b0206bevidence are preserved separately.

Installer11752 adopted723b2a3ain place of queued78fc5140 and waits for main
ccac368fto stop normally and unlock its world. Handoff receipt status:
`waiting-for-normal-shutdown`. No forced stop or restart. Exact ready artifact:
`build/verification/skywars2-phantom-armor-723b2a3a.jar`. Retain723b2a3aready,78fc5140previous qualified and
ccac368fmain/rollback, plus the separate network-audit tool. Obsolete746e62aaand
pre-answerdffc4190archives were path/hash verified and removed (81,734,982bytes);
cleanup receipt is archived. Queued installation does not mean the live JVM has
already loaded the update.
Physical client frames, fatal-PvP advancement execution, arbitrary-map behavior
and peakTPS are not claimed by these native checks.

The separate question about a victim entering MAG while the wearer remains
outside is answered in `JAMMER_QUESTIONS.md` (user): protect that victim.
The flash now checks the target's Magic Stopper field too. A real target-only
volume21blocks above the caster, with the target2blocks above the caster, proves
the source is admitted while the target is protected; retiring the field lets
the same target accept blindness. Normal flash visuals remain visible.
