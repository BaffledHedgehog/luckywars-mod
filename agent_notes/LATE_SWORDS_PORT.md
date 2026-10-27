# Triple Golden, Blazefire and Kara slabs - port contract

Status: qualified build8a94c831; automatic installation queued after normal main shutdown. Scope: Triple Golden Sword, Blazefire Sword and all three Kara slabs. Piston Sword and Chaos Sword remain source audits/questions only.

Sources: [Triple/Blaze source audit](SWORDS_PISTON_TRIPLE_BLAZE_SOURCE.md), [Kara/Chaos source audit](SWORDS_CHAOS_KARA_SOURCE.md). They retain original paths, component fields and SHA256 evidence. Read-only source data root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Implementation: [LbcChargedSwords.java](../src/main/java/me/skywars2/game/LbcChargedSwords.java), [LbcKaraSlabs.java](../src/main/java/me/skywars2/game/LbcKaraSlabs.java), shared [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java).

## Shared contract

Accepted, unblocked positive non-arrow damage, including fatal hits, reaches the native services with the attacking player as owner. The selected mainhand controls charge or Kara activation. Source `lbc/advancement/gowno.json` excludes arrow_only, interaction targets and nexus/imprinted; `lbc/function/items/swords.mcfunction:59-61` dispatches these families. The native role/input/magic guards and existing match lifecycle remain authoritative.

Established user requirements supersede legacy gaps: swept native collision, protected allied players, friendly mobs still targetable, technical-role exclusion, original-owner attribution for direct and deferred consequences, Time Stop and immediate match/epoch cleanup. Source global temporary selector tags and accidental missing ownership are not recreated. Native items retain existing presentation resources and translatable components; no RP updates are required.

## Triple Golden Sword

| Concern | Contract |
|---|---|
| Charge | Player swordcharged8; accepted mainhand non-arrow damage adds1 when score<=5, yielding melee cap6. KFC can add above6; subsequent ordinary hits do not truncate it. |
| Activation | Either admitted hand, charge>=1; costs1 and emits three copies. No item-specific mana/sneak branch. |
| Origin/direction | Eye position minus.5Y, yaw+10/0/-10, current pitch; straight1.2 blocks per active tick. |
| Carrier | Native owned projectile with fixed item display, existing lbc:triple_golden_sword model, exact source matrix, teleport/interpolation1 and zero shadows. |
| Contact | Earliest native swept entity/block collision; eligible entity takes4 arrow with original owner. Friendly-player and technical-role protection apply; friendly mobs remain eligible. Native suppression and Greed integration remain. |
| Wall/expiry | Native block collision shape embeds copy with source-style forward offset; grounded break on cleared forward.5, independent1% active-tick chance, or age100. Flight also ends at age100. |
| Presentation | Contact emits8 crit particles and strong attack sound. Break emits10 golden-sword item particles and item.break sound; embedding uses copper.break. Gameplay retires immediately on contact. The detached fixed display retains the exact contact position for2 visual ticks (bounded Terra pattern), covering interpolation and one contact frame without any additional damage; owner/epoch/forget/stop cleanup also removes it. Native contact retention and no repeated damage are verified; physical client frames remain unobserved. |
| Cleanup | Unloaded/nonresident, invalid owner, stale epoch, suppression and match stop remove owned state. Time Stop freezes flight and age. Finishing displays are separately cleaned. |

Source functions: `items/swords/triple_golden_sword/{click,damage,triple_sword_spawn,tick,tick_2_player,tick_loop}.mcfunction`; shared `items/swords/chaos_sword_imba/homing_sword_{dmg,break,break_pre}.mcfunction`. Legacy point checks/stalls and recursive backward wall search are replaced by the required native swept geometry. This service does not activate the separate golden-skeleton portal AI.

## Blazefire Sword

| Concern | Contract |
|---|---|
| Charge | Player swordcharged9, uncapped by gameplay (native integer representation saturates); accepted mainhand non-arrow damage adds1. Source KFC does not affect9. |
| Activation | Either admitted hand needs>=5; starts firefarting without immediate debit. Changing held item does not stop ongoing fire. |
| Cadence | Every2 active ticks, despite source helper name tick_5t. One outstanding pulse per owner; asynchronous resident-world traversal settles before next pulse. Time Stop pauses native continuation. |
| Ray | Captures owner eye/look per pulse; always samples0, including origin inside a block. Subsequent one-block air samples use the existing unbounded loaded-world DDA service, stopping at obstruction/nonresident boundary rather than imposing an arbitrary gameplay range. |
| Effect |10 flame particles per sample, spread.8/.8/.8, speed.01; target center sample minus1Y, radius2, once per target per pulse. Applies1 in_fire followed by.1 arrow with original owner. No explicit ignition timer is added. |
| Targeting | Shared native eligibility protects allied players and technical roles; friendly mobs remain targetable. This supersedes the source unrestricted entity selector. |
| Payment | After the pulse completes, subtract3, clamp0 and remove fire state at0. Source successful attributed in_fire can charge a still-held sword through the ordinary accepted non-arrow hook; arrow follow-up cannot. |
| Sound | firecharge.use, volume1/pitch1, range16, minimum volume.2. |

Source `players_2t.mcfunction:19`, `items/swords/blazefire_sword/{click,tick_5t,tick_5t_rc,tick_5t_rc_damaged}.mcfunction`. Source scheduled tick2t lacks a Time Stop guard; native shared Time Stop rules intentionally govern this port. Purify zeros8/9 but preserves source ongoing-fire settlement on the next pulse; full player/match cleanup cancels pulses/fire immediately. No detached local particle entity or new projectile model is introduced.

## Kara slabs

All three source items are supported independently: waxed oxidized, weathered and exposed cut copper slabs. Native ids `skywars2:kara_slab`, `skywars2:kara_weathered_slab`, `skywars2:kara_exposed_slab`; custom_data kara1 remains recognized. They retain their distinct vanilla slab models, Smite255, tier_ss tooltip, exact source names and two lore lines. Oxidized uses god_slab_imba; weathered/exposed use not_decaying_slab_imba. Native placement delegates to the corresponding vanilla slab; placed blocks lose item-only Kara flags, as in ordinary block placement.

Accepted mainhand non-arrow damage starts a terrain fan at owner eyes, pitch0, forward1.5. No charge/HUD or per-item cooldown is added. A central invisible owned marker advances.7 per active tick. Each tick destroys its flooredX/Z column at absolute Y0..319, emits100 forced end_rod particles with vertical spread50 and generic.explode volume.1/pitch1/range50/minimum.1. Native destroy provides block/container drops and normal updates; remaining block/fluid state is replaced with air. No direct damage command is invented.

Before moving, center creates two children at local left/right.8 and yaw+30/-30. Children inherit age and original owner, move.7 with fixed direction and do not branch. Center performs its age100 operation and creates its final children before retiring. Selector-snapshot scheduling is explicit: children start next tick; final children can act once at age101 before retiring. Native resident column/neighbor halo checks prevent terrain loading. Captured native world changes preserve support/fall/void attribution to the original attacker.

Source `items/swords/imba_kara_slab/{hit,tick_kara}.mcfunction` and its child spawn helpers, audited in the source note. Unsafe unrelated-marker selection is replaced with scoped owned carriers. Time Stop freezes movement/age/terrain effects; owner invalidation, unload, suppression, stale epoch and match stop remove the fan.

## Acquisition and presentation

Legacy Triple is guaranteed in golden-skeleton death loot and is present in the ultra-reforge sword pool. The compiled original loot alias now grants the native sword, including existing ultra-reforge references; this batch does not activate golden-skeleton encounter AI. Its standalone imported loot retains carrot_on_a_stick, durability900, mainhand attack_damage+8/speed-2.4, tier_b, exact source three-line lore and flags.

Blaze uses restored native catalog kit19, page9000/slot19, table lbc:kit/blazefire. Ordinary gate is lbc:true_advancements/legends/academy/blazefire; locked description key lbc.adv.upgraded_tuntija.blazefire.lore. Ranked selection follows the existing gate-bypass behavior. Source GUI file is9000/18.json despite actual slot19; statistics index18 is retained. The gate recognizes upgraded/transformable/staff/fireblaze_wand1. No kit19 passive attributes/effects exist beyond its sword. Granted sword retains durability600, mainhand+6/-2.4, lbckit.18 name and exact four-line source lore; GUI gray preview styling remains distinct.

Kara's three individual loot leaves are imported without enabling the whole IMBA random pool. This batch does not claim every original IMBA acquisition roll is active. Explicit loot commands can grant the three leaves.

HUD retains the source gold counters for Triple/Blaze when held in either hand. Player counters are shared across item copies/hands. All existing translate keys, omitted styles and RP models are reused; no generated language/model duplicates or resource-pack edits are needed.

## Verification and limits

Final qualification: **build8a94c831**, SHA256 `8a94c831cfd0be81b079be51f65eab1e79360f81ad9aa21a6b35b21e672ef678`. Two actual mode1 matches passed72 suite results and8 separate preparation results in 76.02s; exact460sources/1280classes/3150resources match the tested JAR. Native kit gate/ranked selection, source matrix, real accepted/fatal/WILD charge, offhand activation, actual projectile collision/final contact display, loaded unbounded fire rays, full Kara column/container drops/terminal fall ownership/100+1 fan/three slab placements and actual-stop cleanup pass. Shared source families, signed effects/FOV/layers and firearm suites pass. Evidence: `build/verification/late-swords/qualification-8a94c831.json` and `native-server.log`. Installer11424 waits for normal shutdown on24566; main disk remains1a8a3825. No forced stop/restart.

Three native runs were **REJECTED**, with their original logs/results under `build/verification/late-swords/attempt1`, `attempt2` and `attempt3`. The first fixture searched the wrong declaring class for an attack ticker. The second expected kill credit after a nonfatal fall, although delayed attribution applies on death; the corrected check requires a real fatal native fall after actual support destruction. The third exposed a nondeterministic first-match collision check and a separate confirmed Chaos test gap: its actual emission origin could be outside the loaded footprint. Its second match passed the complete new family, which does not qualify the rejected run.

The final fixture preparation now occurs in a separate native command and requires an actual later server tick before consumption. Only introduced force tickets are released. A collision target lies clearly inside the swept segment and must be visible in the real native entity query before the shot; strict damage/owner checks retain detailed failure diagnostics. Chaos checks explicitly load their actual origin without changing source geometry or the exact40-bolt assertion. A rejected intermediate26.3 ChunkPos constructor compilation is retained under `attempt4-compile`. These are verification changes; gameplay collision and attribution were not weakened to satisfy a check.

Physical client pixels/audio and peak TPS are unobserved. TPS compensation remains feasibility-only, not implemented by this batch. Piston/Chaos source audits and deferred decisions remain separate unfinished work.
