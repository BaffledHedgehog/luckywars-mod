# Pop-It native cleanup and acquisition boundaries - 27 October2026

Read-only audit after Traveller commitdb60d7d. Only this new note written. No implementation/build/server/git/RP changes, no protected-author-path/live-override edits. Source27-way audit remains [POPIT_SOURCE_2026_10_27.md](POPIT_SOURCE_2026_10_27.md). This follow-up corrects an important exact-key-only inference in that older note: native status ownership can use maps rather than source score names. It does not qualify Pop-It gameplay.

## Four requested outcomes

|Draw/source name|Current native truth|Complete selective reset boundary|
|---|---|---|
|17 dimensional_shifting|Exact production Java key occurs only in LbcKfcFood raw counter list:65. No native timer/AI/task/owner ledger for this exact status was found. No E125 native HUD entry found.|Set shared mana.score(player,"dimensional_shifting")0. Do NOT clear unrelated Hedghog dimension templates, entity dimensions, portals or effect_dimshifting aliases without evidence.|
|24 skill_issue|**Implemented under LbcItems.turnCurses**, not literal score. Source HUD E129 maps to native curseTicks, and native tick awards effect_skill_issue.|Remove ONLY player's TurnCurse map entry and zero raw skill_issue score for legacy/KFC compatibility. Preserve source rotation-history semantics and unrelated statuses. Existing broad clear(player) is too broad.|
|25 target|**Implemented under LbcHoming.curses**, not literal score. Source HUD E122 maps to native homing.curseSeconds, owned curseRay creates1200tick status diverting eligible projectiles.|Call existing homing.clear(player) (or clearEffects same wrapper) and zero raw target score. This only removes that curse map entry, preserving unrelated homing projectiles/rains/casts.|
|27 greed_cursed|Exact production tag occurs only in removal sites; no native positive producer/owned timer/AI task for tag found.|Remove greed_cursed tag only. Do NOT cancel LbcGreed.active, sin_greed_active, Greed artifact/ability, owned projectiles or prestige. Pre-remove tag determines Popit achievement.|

### Draw24 precise native state

[LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java):172-175 defines TurnCurse(owner,remaining); :299 mapUUID->TurnCurse; :300-301 separate rotations map comment explicitly retains source rotation storage through curse expiry/death, bound to match. :1403-1408 broken-totem usedTotem adds100 ticks. :1652-1672 iterates active turnCurses, reads owner/current/previous yaw/pitch, punishes rotation delta above7 (yaw also below180), updates rotations, unlocks lbc:true_advancements/potions/effect_skill_issue and decrementsremaining. :1695-1697 exposes curseTicks, HUD :507 redE129.

Setting raw skill_issue score cannot stop ticking/punishment. Existing clear(player):1700 removes turnCurses but ALSO clears many unrelated item effects/resources/curses; unsuitable for one27-way draw. New narrowly scoped API should remove turnCurses entry only. If synchronous callback currently processing same curse, existing tick loop checks map identity at loopstart; removal before subsequent tick prevents further action. Do not erase rotations map gratuitously: source snapshot survives expiry/death by explicit existing native contract. A raw compatibility score0 can coexist with map removal; do not let raw KFC catalogue re-create a separate timer.

### Draw25 precise native state

[LbcHoming.java](../src/main/java/me/skywars2/game/LbcHoming.java):52 curses IdentityHashMap target->Curse(owner,remaining); :99-103 exposed HUD seconds; :359-367 curseRay adds1200 ticks and owner; later per-projectile/Card/trace loops inspect curses for target diversion (:403,:421-442). HUD LbcItems:505 E122 confirms exact source target status. Existing :420 `public void clear(LivingEntity entity){curses.remove(entity);}` is already exact selective API; :111 clearEffects delegates clear, does not remove caster's fireballs/rains/destinies or pending rays. Use it; no need a new broad service reset. Removing curse prevents future guided-target selection; already-created independent projectile velocity/entity remains ordinary native state and is not an extra status ledger to delete. Owner identity in removed Curse is naturally released.

### Draw17 and27 absence evidence scope

Scanned all src/main/java production excluding checks using exact quoted names, then semantic aliases/HUD/E125 and source-status mappings. dimensional_shifting/skill_issue/target appear in KFC counters, but24/25 map-backed services above prove literal-key absence alone is insufficient. For17 no analogous native status/HUD/producer was found. LbcHedgehogDimensions handles separate dimensional drop/structure behavior; it is not demonstrated to be source dimensional_shifting. Existing `greed_cursed` removals: LbcHedgehogSpikes:149, LbcItems.clearEffects:1397. No contains/add/owned timer for the exact tag found. These bounded absence conclusions do not claim every similarly named legacy feature is implemented.

Native resources contain legacy source-score names and GUI texts, but raw source functions are not enabled by generic filesystem presence; processResources explicit whitelist below packages tables/advancements, not the legacy function engine. Do not execute old functions to supply missing cleanup ownership.

## Existing Popit advancement and kit2 - native integration

[src/main/resources/data/lbc/advancement/true_advancements/accesories/popit.json](../src/main/resources/data/lbc/advancement/true_advancements/accesories/popit.json):1-29 retains original icon/name/description/task,parent infinite_quiver, impossible criterion. **Native copy omits original legacy function reward**. This is intentional native adaptation, not a missing requirement to package gain/kit.mcfunction. Source source-audit describes the historical reward; native unlocking is catalog-driven.

[src/main/resources/skywars2/lbc_loadouts.json](../src/main/resources/skywars2/lbc_loadouts.json):11-18 already defines id2,page9000,slot1,table lbc:kit/3_purify_potion,gate lbc:true_advancements/accesories/popit,lockedKey lbc.adv.accessories.popit.lore. LbcLoadouts.allowed:226-227 permits ranked or ungated or hasUnlock; hasUnlock:230-235 recognizes saved lbcUnlocks OR actual native advancement complete. Therefore awarding original advancement is sufficient for ordinary selection; ranked retains existing bypass. Do not manually select/grant kit2 on Popit use.

[src/main/resources/skywars2/lbc_unlock_notifications.json](../src/main/resources/skywars2/lbc_unlock_notifications.json):21-23 maps Popit to new_kit subtitle. LbcLoadouts.advancementAwarded:162-175 sends native newly-completed title/subtitle based on catalog notification; existing advancement award hook must be used, not a synthetic new achievement. LbcLoadouts.apply:346-348 and grant:377 onward give selected eligible kit through existing native loot; no duplicate Popit-specific reward grant. Kit2 provides three purify potions, not Popit. build.gradle:468 includes kit/3_purify_potion and:470 onward true_advancements/** in processResources whitelist. This is verified packaging configuration/source inclusion, not inspection of a newly built JAR or runtime test.

## Accessory acquisition - actually configured package routes

Current raw Popit definition survives in src/main/resources/data/lbc/loot_table/transformables_accessory.json:497 itemmodellbc:popit,:504 literalPop-It,:545 exactflags transformable1/accessory1/magicwand1/popit1. build.gradle:441 includes this table. No build JSON filter/rewrite was found in processResources: its include list plus version expansion preserves this configured table. Thus current source leaf is packaged by configured resource task; implementing usable-item recognition is still required for gameplay.

Normal native accessory reforge rule LbcBolts:56-57 maps accessory to transformables_accessory. Ultra:79-80 maps accessory to transformables_accessory_all; that packaged table's pools[0].entries[0] references lbc:transformables_accessory with weight10 (src resource lines7-9). build.gradle:323 includes accessory_all. Natural lightning reforge rule LbcBolts:447 also accessory->transformables_accessory. The existing reforgeInput planner:438-440 and reforge worker:536 onward resolve these tables; no new standalone Popit recipe is necessary. Do not turn on entire unresolved artifact pools or claim every raw source luckyblock acquisition path is restored.

No separate original old Popit leaf or kit2grant-of-Popit exists. A dedicated exact native ported leaf may be added by parent for explicit catalog/operator use, but original current table route already exists. Existing packaging plus matching flags is distinct from registered custom-id support. Preserve source item components/base and no-consume behavior; native Popit implementation can recognize source custom_data in raw vanilla carrot stack.

## Actionable implementation boundary and uncertainty

Required for complete27-way cleanup: add ONLY narrow turn-curse reset for24; use existing homing.clear for25; raw17score0; exact27tagremove. Also retain prior selective Psycho/Electro and owned death/reverse-mana cleanup work from source audit. No new author question is required for these two status identities: exact native HUD/source identity resolves them. Do not retain earlier note's mistaken suggestion24/25 are raw-only. True17gap is that native producer is absent; clearing rawscore is exact bounded behavior until future producer joins cleanup API. Final gameplay/loot/advancement qualification belongs to parent; this audit runs no tests/build.

## Native file evidence hashes

- `src/main/java/me/skywars2/game/LbcItems.java` SHA256 `c012ed97686b181ac0d0a35fea3e541f8a6c2ecc23efffc0f1dbdfd33dfe48e3`
- `src/main/java/me/skywars2/game/LbcHoming.java` SHA256 `27f01c1ed9e60379d846332c440c919cfd19735934700698a934f2e7ea41f625`
- `src/main/java/me/skywars2/game/LbcKfcFood.java` SHA256 `bf9c559274c416698199276b648c2eabf492499e8f6616bb7b72b5d87687af3a`
- `src/main/java/me/skywars2/game/LbcHedgehogSpikes.java` SHA256 `a3005fc5c65ebb94200b4557e4bda0f17a10b8579a725e896b3b4ae5327e54d6`
- `src/main/java/me/skywars2/game/LbcLoadouts.java` SHA256 `aca0e966dfb0168b4849e665c5f3456eb9524e547f517c7f47320f47ce03c2db`
- `src/main/java/me/skywars2/game/LbcBolts.java` SHA256 `186a92f64a7daafe6eb58c39be9f23f1137613468c1a8d5dc399d14fe32cf5e2`
- `src/main/resources/data/lbc/advancement/true_advancements/accesories/popit.json` SHA256 `be5913dcd25f983330a7169083ce0ba021a98ca4ffc2f977b3c6566804fac33c`
- `src/main/resources/skywars2/lbc_loadouts.json` SHA256 `99aad58d57f2215005b5aaad9a3b6dcdbdf21d80ecac06020443b1f96ba7555f`
- `src/main/resources/skywars2/lbc_unlock_notifications.json` SHA256 `47af6ee6bc870c75a34f53608b2df312807ba0a81243861fc1caf923b1fa5e2b`
- `src/main/resources/data/lbc/loot_table/transformables_accessory.json` SHA256 `a13fe35ff7aa946045b63495118c3b50e8c897a6533555be14b1a6dfb79c18b5`
- `src/main/resources/data/lbc/loot_table/transformables_accessory_all.json` SHA256 `d9b7779a62026e6f36d5f2a9af3dccbe28874a4b36e85187bfb35db6441cc906`
- `build.gradle` SHA256 `945f5a90c369adcba1ab5e39aae5bdd44cbc2594ce614d03d73d1e187056ab37`

## Final API and suppression check

Rechecked exact current LbcHoming scope: clearEffects(entity):111 only calls clear(entity); clear:420 only curses.remove(entity). It does NOT alter reflected projectile owners (Electro ownership), Time Stop state, fireball steeringTargets, pendingRays, rains, destinies or projectile velocity. A proposed clearCurse(ServerPlayer) can safely delegate that exact removal plus raw target0; no need to invoke forget/stop/global clear. Separate full stop:460-462 cancels rays/removes all tracked actors and must NOT be used for Popit. Already reflected projectile ownership remains native; curse cleanup only stops future curse redirection.

Outcome24 proposed clearTurnCurse(player) should remove turnCurses map entry and raw skill_issue0 ONLY, preserving rotations history and all unrelated status state. Broad LbcItems.clear has many additional effects and is not suitable.

Suppression source boundary: standalone spell1_default44/offhand21 has NO stopper_magic/beacon check; outer items/spells requires owner not nomagic_active and no active artifact player within20. Current LbcMagic.suppressedAt:409-414 combines active artifact sphere with beaconSuppressed; invoking it directly would introduce a beacon veto absent this nonpaid accessory branch. Factor pure artifactSuppressedAt from existing query and retain explicit owner nomagic_active compatibility. Current root AGENTS user rule at4083 explains wand use prohibited inside antimagic beacon **because it uses mana**; Popit does not use mana. No concrete author instruction found requiring beacon veto for this specific free accessory. Existing global role/Time Stop/item input admission remains; this does not disable beacon gates on actual paid wands. AGENTS:4365 onward additionally requires artifact inventory cursor/crafting inclusion; reuse author-approved artifact detection scope rather than deleting that support while extracting aura-only query.
