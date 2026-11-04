# Cases / keys / rewards source audit — 2026-11-04

Read-only planning, not implementation or qualification. Only this note and ignored build/verification/cases-2026-11-04 evidence written. No production/resource/RP/server/build/test/commit changes. Protected Academy/key and other author WIPs untouched. TMT remains last. Read current root AGENTS, agent_notes/AGENTS and NIGHT_REMAINING_CHAINS cases paragraph; Serena initial instructions applied.

Source root W = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/. References below are relative to W. Saved evidence: [hash manifest](../build/verification/cases-2026-11-04/source-sha256.json), [complete rooted loot JSON](../build/verification/cases-2026-11-04/loot-closure.json). There are **103 reachable loot tables, zero missing referenced loot definitions** from the five case roots plus all active roll function loot roots, including duplicate compensation. This establishes finite data closure only; it does not establish runtime or UI readiness. 134 source files hashed. No Paper/Ignite behavior is attributed without a live registered path; this bounded audit found the operative chain in swrg source and makes no claim those Java extensions implement it.

## Native ownership already available

[GameMenus.java](../src/main/java/me/skywars2/game/GameMenus.java): renderCases (line452) imports source page5000 slots0..4 with null actions. renderKeyShop (line302) is live and already mirrors source shop6000, updates the canonical Profile and calls players.save(). Existing OriginalGuiDesign is the source item/model presentation owner. Do not create an independent menu or currency store.

[PlayerStore.java](../src/main/java/me/skywars2/game/PlayerStore.java): Profile owns coins, memcoins, keys[5], ordinary kit/skill, killSound/suffix/winEffect/cage, and distinct LBC choices/unlocks. save() owns file persistence; checkpointProfiles()/restoreProfiles() own typed profile snapshots. Ordinary kit/skill access already reads the exact swrg.kitN/swrg.skillN entity tags through OriginalGuiDesign.matchesAccess; see the follow-up mapping below. No LBC ID translation or protected Academy/key work is required. Cosmetic tags/advancements still require their own consumer audit. A future session owner needs pending reward/RNG/phase/debit/commit state joined to existing whole-coordinator snapshots, plus persistence decisions; no new rollback engine.

## Exact economy

W/function/gui/select/6000.mcfunction -> native renderKeyShop:

| key index / menu slot | 1 / 5 / 10 keys | currency |
|---|---|---|
| 0 / case0 | 100 / 400 / 700 | coins |
| 1 / case1 | 150 / 600 / 1050 | coins |
| 2 / case2 | 150 / 600 / 1050 | coins |
| 3 / case3 | 200 / 800 / 1400 | coins |
| 4 / case4 | 100 / 400 / 750 | memcoins |

Keys are scoreboard persistent in source, initialized zero in core/player/first_time. No additional entry fee exists in audited opening path. Each accepted opening removes exactly one matching key at phase1, not on page5000 selection or reward25. Source has no transactional crash journal.

## Whole menu / animation / reward lifecycle

1. gui/select/5000 requires selected matching key >=1. It sequentially assigns unoccupied shared case IDs1..8, then sets page5100/5200/5300/5400/5500. Because assignments continue rather than return, a lone opener ends on8, not1. When all IDs occupied it can set a page without obtaining an ID; no queue/failure/refund is expressed there. IDs model the eight physical shared backing containers, not reward classes.
2. core/setup starts gui/roll/player, which schedules itself every1t. It uses per-ID, per-tier @r admission at physical coordinates10241024,y=1..8,10241024; matching page, case_id and roll_timer=0 are necessary. gui/main + gui/page/5000-5400 copies nine visible backing slots into player enderchest9..17. Native inventory/UI isolation must replace these world/enderchest writes.
3. gui/roll/{0..4}: initialize roll1; debit matching key; fill 27 slots from root loot; remove already-owned rewards and insert compensation through have functions; set roll2. Phases2..20 shift the 27-slot strip left. Phases2..14 set timer1,15..18 timer2,19 timer3,20 timer4; driver decrements positive timers. Ownership filtering runs again at22. Increment happens before reward25 check. Winning logical item is backing Slot2. This is an animation over preselected finite items, not a probability reroll on every visual frame.
4. reward25 dispatches a typed reward from Slot2; anvil sound. Phase30 calls gui/roll/reset: case_id0, remove fast tag, page5000, roll0, gui/clear and conditional page render. No original containers/interpreter needed in a port.
5. core/player/reset clears case_id/roll/timer and page. Its adjacent add/remove matching key statements cancel exactly; **no refund** is established. Source has no durable pending reward or explicit compensation for interruption between debit and payout. Ordinary closing/disconnect and cross-reset policy therefore need explicit native ownership semantics.

## Exact source probabilities and terminals

These probabilities describe raw generated tokens **before owned-token substitution**, not the final conditional distribution of newly unlocked rewards. Every leaf weight in the listed group is1. Full exact leaf JSON preserved in evidence.

- case0/main chooses case3/select, coins/main, case1/select, case2/select equally: 25% each. Thus bars/sounds each12.5%, kits25%, skills25%, direct coins25%.
- case1/select rarity weights550/350/90/10 (55/35/9/1%). Tier a/b/c/d contain7/5/5/3 equal leaves (20 total). reward/1 grants swrg.kit1..20 tags, or coins if token substituted.
- case2/select same rarity weights, with4/3/2/2 equal leaves (11 total). reward/2 grants source skill tags, or coins. These are ordinary swrg rewards, not an automatic LBC unlock translation.
- case3/select chooses bars or sounds 1:1. **Bars a/c/d weights550/90/10 normalize to550/650,90/650,10/650**, with16/3/1 equal leaves. There is no350 b branch. Sounds a/b/c/d weights550/350/90/10, with7/3/1/2 leaves. reward/bars and reward/sounds add source persistent tags.
- coins/main weights550/350/90/10 for rewardcoin1/2/3/4 =20/50/100/1000 coins. Case4's same IDs award memcoins through reward/memcoins.
- case4/select weights55/35/10/1 total**101**, not100: probabilities55/101,35/101,10/101,1/101. common/rare/epic/legendary have16/6/3/4 equal leaves. reward/4 grants suffix_1..26 or drop_1..3 advancement and/or memcoins. Do not infer familiar55/35/9/1 odds here.
- have1/2/3 delete .id of already-owned kit/skill/bar/sound tokens in all backing slots; have4 similarly checks suffix/drop advancements. loot insert have/a..d or a_m..d_m fills freed slots with same-rarity compensation tokens. Source d token is1000 currency, c100,b50,a20. No redraw/unowned-only pool or duplicate protection that guarantees a new unlock exists. Existing owned tags/advancements therefore affect final payout distribution.
- reward/4's drop terminal game/start/player_drop_request only prints Discord/Telegram manual-contact hints; reward advancement records entitlement. It does **not** grant the advertised external drop/item automatically. No communication action was taken by this audit.

## Persistence, rollback and dependency boundary

Source scoreboard, player tags and advancements persist in vanilla player/world storage, while active strip/session state also depends on physical backing containers, page and timer. Native Profile fields alone do not cover ordinary reward ownership or an in-flight opening. Reuse PlayerStore for balances/keys and canonical reward ownership; keep previews separate from the committed reward. Must define a single accepted debit and single durable payout, replay-safe advancement/tag updates, reconnect behavior and match-reset interaction before exposing clicks. Existing checkpointProfiles helps rollback profiles but is not evidence that reward tags/advancements or session phase are already captured. The user requires full-world rollback, including profiles. Preserve that requirement: accepted session/debit/payout and tag/advancement ownership must join the existing whole rollback consistently; excluding economy is not a proposed default.

This is a finite independent economy chain, not a blanket TMT blocker. Ordinary kit/skill consumers are already registered, as detailed below; their implementation is not a remaining dependency. Cosmetic presentation/ownership, case4 manual entitlement and transaction/reconnect semantics remain material scope. A partial click-to-random-item implementation would not close this source chain. No arbitrary function bridge, no new whole-NBT scan, no physical source containers required.

Material author questions for ROOT backlog only (question file untouched): preserve source101 and650 probability totals or normalize to intended rarity labels? Retain duplicate compensation behavior? On disconnect/reset/crash after debit, resume the committed result or refund exactly once? Retain case4 manual-contact entitlement and destinations or define a native claim flow? The source facts above are not approvals of any new policy.


## Follow-up: actual ordinary reward consumers (same day)

The earlier mapping-dependency wording was too broad. Exact case rewards already feed native ordinary selections and consumers. This is static registered-path evidence, not fresh gameplay qualification. Protected LBC kits14/16 (key/key_luck) are a separate namespace and selection system; ordinary swrg.kit14/swrg.kit16 below require no changes to them.

`gui/roll/reward/1` grants **reward tag** swrg.kit1..20. Imported `skywars2/gui_design.json` page swrg:3000 requires those exact tags. `OriginalGuiDesign.matchesAccess` reads `player.entityTags()`, so granting the source tag immediately satisfies ordinary menu access. `unlocked` applies the existing ranked/debug perk bypass; selection and spawn recheck the same gate. `GameMenus.renderKits` writes Profile.kit and saves, and `MatchEvents.giveKit` is invoked by GameController fresh-spawn/respawn/start paths (1045,1186,1228 in current file). It uses native ItemParser/inventory delivery, not a command/function bridge.

| Reward tag index | page3000 slot -> Profile.kit | Existing MatchEvents terminal |
|---|---|---|
| 1..10 | index-1 -> same index | KIT_COMMANDS entries1..10: TNT; wolves; potions; wooden sword/leather armor; food; thorns chest; attribute chest; poison/web/string; gold materials/fire resistance; shield |
| 11 | 10 ->11 | KIT_COMMANDS enchanting supplies +20 XP levels |
| 12 | 11 ->12 | KIT_COMMANDS poison/milk + native grantTable swrg:game/kit/12 (resource exists, nested item definition) |
| 13 | 12 ->13 | KIT_COMMANDS golden tools |
| 14 | 13 ->14 | KIT_COMMANDS rocket materials/crossbow; not LBC key kit14 |
| 15 | 14 ->15 | KIT_COMMANDS invisibility potion |
| 16 | 15 ->16 | KIT_COMMANDS fire sword/fire resistance/lava/flint; not LBC key_luck16 |
| 17 | 16 ->17 | KIT_COMMANDS cake/slime/honey/rocket/sword |
| 18 | 17 ->18 | Native grantTable swrg:game/kit/18 (resource exists; source head reward) |
| **19** | **19 ->20** | KIT_COMMANDS20 full golden equipment/ore/apple |
| **20** | **20 ->21** | KIT_COMMANDS21 enchanted oak slab +16 cobblestone slabs |

The final two rewards are offset by source back slot18: **reward tag19 is not selected kit19**, and tag20 selects kit21. Source gui/select/3000 expresses exactly the same mapping. Native renderKits intentionally skips selected ID19; it is not an unported reward. Seasonal kits22..24 are advancement-gated, outside the twenty case-kit reward tags.

`gui/roll/reward/2` grants swrg.skill1..11. Imported swrg:4000 slots0..10 require those same tags; GameMenus.renderSkills sets Profile.skill=slot+1 and saves. MatchEvents.selectedSkill rechecks gate each use; giveSkill enrolls the canonical SkillState on spawn and changed selection. No LBC skill ID or catalog mapping is involved.

| Reward/selected ordinary skill | Registered native effect boundary |
|---|---|
| 1 | onPlayerDamaged poisons actual player attacker |
| 2 | onLanding, source fall-distance area damage |
| 3 | tickSkill hurt-time speed |
| 4 | onConsumed food effects |
| 5 | tickSkill low-health resistance |
| 6 | onPlayerAttack arrow recovery |
| 7 | tickSkill low-health strength |
| 8 | tickSkill burning strength/resistance |
| 9 | onPlayerAttack darkness |
| 10 | onPlayerAttack / onPlayerDamaged probabilistic lightning |
| 11 | onPlayerAttack regeneration/heal |

GameController.events.tick is called at992; events.start at1042. Damage admission calls onPlayerDamaged/onPlayerAttack at1260/1262; landed/consumed route at1265/1273. PlayerMechanicsMixin and ConsumableSkillMixin call those real controller boundaries. The visible skill12/13 seasonal advancements are outside case skill1..11. All eleven case skills have actual registered consumers; no genuinely absent kit/skill branch was found by this bounded static audit. This does not requalify their exact behavior against every source detail.

Remaining case implementation work is the opening/session/reward dispatch itself, including persistent original tags and consistent snapshot participation, not reimplementing ordinary kits/skills. Cosmetic source tag consumers need separate closure before case0/3 exposure. Source odds and compensation are documented defaults; root can retain them faithfully without inventing a new author question. Night Q24 (resume/refund) and Q25 (manual claims) are already in root backlog; this note does not duplicate the question file.
