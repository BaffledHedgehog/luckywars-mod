# Equipment, inventory and artifact suppression audit — 27 October 2026

Read-only production review. Only this new note was written. No Java, resources,
existing notes, original server, actual resource pack, build, tests or server were
changed/run. Read AGENTS.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. Paths
below are relative to `src/main/java/me/skywars2/` unless another root is given.
This is code-path evidence, not an observed gameplay run or proof of the installed
JAR's hash. The parent audit owns installed-artifact verification.

`MAG` means a proposed magic classification, `TECH` a proposed mechanism
classification; neither is an authorization to add a gate. Source omission or a
comment saying “deliberately” is not evidence that the user approved immunity.
Ask the user before changing the candidates below. For every proposed gate also
ask whether it blocks new activation, pauses an existing effect, or removes it.
“Other jammers must not suppress other jammers” takes priority over blanket gates.

## Central paths checked first

- `game/LbcItems.java:132` dispatches accepted-hit consumers **before** its local
  Ice/Terra suppression at :157. That later guard does not cover earlier consumers.
  `game/LbcItems.java:915` and `game/LbcDamage.java:162` apply role/team/Greed,
  attribution and particular actor policies; they do not impose a blanket jammer
  veto on all magic damage, effects, teleports, equipment or inventory changes.
- `game/LbcItems.java:1620` ticks each service; there is no common jammer skip.
  `game/LbcMana.java:160` gates ordinary mana processing, but :155 `visible()` is
  role/dimension admission only. A consumer using visible() does not inherit active().
- `game/LbcMagic.java:522,974,999` globally removes actors by native entity-type
  tags and explicit `magic`/`mecha`/summon tags in the beacon sphere. Magic catches
  small fireballs, fangs, shulker bullets etc.; TECH catches TNT, arrows, lightning,
  creepers etc. Exact sets: `src/main/resources/data/minecraft/tags/entity_type/`
  `magic_entities.json:1` and `mecha.json:1`. Native Items, ordinary skeletons,
  falling sand and virtual displays are not automatically either category.
  Pulsed actor removal is not an activation veto or a universal damage filter.
- Native beacon APIs differ: MAG aura+beacon :406; pure MAG beacon :467;
  TECH beacon :455. Native TECH is a **block**, not a legacy marker (:121,:546).
  Searching only legacy `stopper` markers therefore misses the new provider.
- Effect rejection `game/LbcMagic.java:325` and :331 is artifact cleansing/Greed,
  not a general MAG/TECH veto. Native addEffect/heal/attribute changes are not
  implicitly stopped by a beacon. Existing native potions normally keep their lifetime.

## Confirmed mismatches and questions with the clearest evidence

|Mechanic|Actual production path without the relevant gate|Question for user|
|---|---|---|
|Broken Totem/Screamer Skill Issue turn punishment|`game/LbcItems.java:1761` checks native MAG beacon plus legacy markers of **both** kinds. Native TECH beacon is never queried; punishment :1777 and timer continue beside it.|Should native TECH pause it exactly like a legacy stopper? Strong provider-integration mismatch; no code changed.|
|Paladin full-set Resistance IV|`game/LbcArmorSets.java:22,46,61`: live worn set and passive cadence, neither jammer queried.|MAG, TECH or exempt? If MAG, stop new refreshes in beacon?|
|Shitty/Gownoman armor poison, hunger and full-set Wither aura|Same admission; `game/LbcArmorSets.java:62` through :76 directly grants attributed effects.|MAG or biological/nonmagical? Does a beacon block aura creation or also protect targets inside it?|
|Erosion armor loses Protection levels|`game/LbcArmorSets.java:82` through :97 directly edits enchantments every5ticks, including full-set random piece.|MAG or unavoidable equipment decay? Pause decay under MAG?|
|Traveller full-set six cleanses + one random buff|`game/LbcTraveller.java:46` full branch has no gate; chest :42 and helmet :51 already have MAG-beacon guards.|Should full set also be MAG? Source/port calls omission intentional, but no explicit author approval for immunity was found.|
|Overload leggings jump|`game/LbcOverloadArmor.java:49,124` uses mana.visible(), not mana.active(); grants Levitation/Resistance and drains nearby mana without either jammer.|MAG or exempt movement? Apply gate to jump/drain together?|
|Overload boots slam|`game/LbcOverloadArmor.java:69,151,175` start/ongoing gravity, Resistance, impact damage and mana transfer have no jammer check. LbcMana calls armor tick even when active() fails (:297).|MAG? Decide start versus existing slam separately. Source omission is documented, not an author immunity ruling.|
|Holy Shield purification|`game/LbcHolyShield.java:23` through :49 directly damages undead/heals others and dispels glyphs; native using-item hook has no jammer gate.|MAG, or protected counter-magic/jammer exemption? Keep vanilla blocking separate from purification.|
|Mortis lunge, regeneration and terrain trench|`game/LbcSpecialTools.java:94` uses active(), not magic(); :107 and :116 apply effects/teleport/destroy/place blocks directly.|MAG supernatural lunge or exempt? Existing Scythe/Hammer use is MAG-gated.|
|Nano kit trap|`game/LbcItems.java:1170,1369,1418` tracks `nano_trapik`, triggers and subtracts base max health/armor/toughness without either gate. Legacy loot `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/nanobot.json:16` gives only slowraycast/nano_trapik/player; global mecha/magic removal does not match it.|Likely TECH: should beacon remove trap, disable trigger, or both? Do not classify from the name alone.|
|Siggewinne Syringe equip buff|`game/LbcSyringe.java:77,90,110` buffs three owned mobs and maintains bonuses without owner MAG/TECH admission. Existing summon actor deletion can remove nearby summoned recipients, but a suppressed holder can buff a distant surviving owned mob.|MAG, TECH/medical, or exempt? Decide holder activation and already-buffed recipient behavior separately.|

## Further actual ungated mechanics — full reviewed inventory

Each row records a real implementation, not a test/stub. The group expansions below
list every distinct outcome under the shared omission so a family is not reduced
to its first branch.

|Mechanic|Production evidence and central-filter qualification|Question/classification to resolve|
|---|---|---|
|Feeding Sword, current and old|`game/LbcClassicMelee.java:48` directly feeds/increments food_count before magic(owner); no actor to purge.|MAG feeding enchantment or ordinary food exemption? Source bypass alone does not settle it.|
|Old Epic and old Fragment swords, all17 types|`game/LbcPendingSwords.java:53,56,95`: old bypasses current admission. Outcomes are absorption; eight arrows; jump+Levitation; fang stages; explosion; fire resistance; blind; heal; area harm; invisibility; teleport; Resistance; screamer; speed; strength; conduit; weakness (:98–114). Arrows are globally TECH-purgeable, fangs call MAG-gated spawnOwned; other direct outcomes are not covered.|MAG gate old family, preserve all, or classify outcomes individually? Current family already gates activation.|
|Enhanced flint/holocaust melee fire curse|`game/LbcPendingSwords.java:52` grants RAID_OMEN111 directly before current sword gate. Downstream terrain policies belong to Kiuas/Quiver audit.|MAG curse or nonmagical fire? Do not treat the later current-sword gate as coverage.|
|Old reactive mystical2, types1–6|`game/LbcReactiveArtifacts.java:47,69`: old bits bypass currentMagic. 1 self Regen/Saturation;2 Strength;3 visual smoke;4 forward WILD movement;5 area WILD Wither;6 area Regen.|MAG old artifacts or explicit legacy immunity? Type3 is presentation-only.|
|Current reactive smoke after creation|Activation is MAG-gated (:37,:47); owned100tick cloud `game/LbcReactiveArtifacts.java:98` refreshes Blindness/follow-range with no gate at owner, center or target. It has no native actor for global purge.|Should a newly placed/entered MAG beacon stop an already active cloud? Separate from ordinary already-granted potion lifetime.|
|Old jump mystical3, types1–3|`game/LbcJumpArtifacts.java:44`: Slow Falling; Levitation; Speed bypass current magic() (:24,:39).|MAG old family or explicit legacy immunity?|
|Incoming current mystical4 type1 shield|`game/LbcIncomingArtifacts.java:85` invokes shield before currentContainer beacon gate (:88); shield directly grants native effects (:106).|MAG or protected defensive jammer?|
|Old incoming mystical4 types1–6|`game/LbcIncomingArtifacts.java:95`: Resistance, instant self-harm, Speed, Slowness, Strength, Weakness all run outside current beacon gate.|MAG old family or explicit legacy immunity? Harm outcome must be considered as well as benefits.|
|Old Sand Amulet volley|`game/LbcIncomingArtifacts.java:173,185` emits falling sand without magicHit. Sand has no magic/mecha tag and neither native entity type group catches it.|MAG amulet; should gate old activation and existing falling payload?|
|Current Sand Amulet existing sand/queued terrain|Activation :172 has magicHit; volley :185 and Terrain work :356 retain owned physical sand/queued writes without a later jammer test.|Should MAG retire existing payload/work, pause it, or only gate activation?|
|Standalone Pop-It beside MAG beacon|`game/LbcPopit.java:26` rejects owner artifact tag/nearby artifact only; :33 explicitly omits beacon/personal mana-timer veto. Wands routes directly (:446).|MAG cleanser or protected counter-magic/free-accessory exemption? Existing note inferred free-accessory exemption; no specific author immunity ruling found.|
|Canned Food|`game/LbcCannedFood.java:23,34,40`: passive hungry-holder consumption, Saturation, one of11 buffs, item clear and food_count proceed without either gate.|Ordinary food exemption or MAG random buff? Decide consumption versus bonus separately.|
|KFC food20 outcomes|`game/LbcKfcFood.java:92,97,120`: XP1/5/15; random slot change; heal; Strength; counter additions; attributes; mana stats; hunger; nausea; Saturation; mega/drop; gigaponos; levitation/glow/time; win button; slowness/saturation; cultus drop; chicken; empty result. No jammer admission. Greed/cleansing remains separate.|Food exemption or MAG special outcomes? Do not remove ordinary nutrition when classifying bonuses.|
|Edible excrement reproduction/skill9 healing|`game/LbcKfcFood.java:100` drops replacement and grants skill9 Regen/Heal without gate.|Ordinary/biological or MAG healing?|
|Ongoing KFC gigaponos|`game/LbcKfcFood.java:268` progresses lift/drops/sounds without a beacon test; custom effect rejection remains.|Pause existing effect under MAG/TECH, or food/biological exemption?|
|Liquid Abyss carried damage|`game/LbcInventoryHazards.java:103,109` counts item and directly applies outside-border damage; no actor/global jammer filter. Armored Inventory and void resistance are separate.|MAG/TECH hazard or exempt curse?|
|Carried bomb1|`game/LbcInventoryHazards.java:117,133,147`: consume then direct6 arrow-type damage, **no arrow entity** to purge.|TECH explosives or MAG curse?|
|Carried bomb2|:134 applies source effects plus direct4 arrow-type damage, likewise no actor for damage.|TECH/MAG?|
|Carried bomb3|:135 creates native2tick TNT; globally TECH-purgeable, but consumption/spawn itself has no jammer veto.|Is global removal sufficient, or should TECH prevent consumption/creation?|
|Carried bomb4|:136 applies source effects plus native lightning. TECH globally purges bolt; direct effects already happened and source consumption is not gated.|TECH electrical; must unrelated MAG leave it active?|
|Carried bomb5|:137 applies effects/direct10 arrow damage and backwards/up teleport; no actor covers these operations.|TECH/MAG/biological?|
|Carried bomb6|:138,:155 queues repeated direct.1 arrow-type damage with no jammer test; no projectile actor exists.|TECH/MAG? Decide cancellation or pause of an admitted queue.|
|Devil Totem held summon/self-harm|`game/LbcTotems.java:90` spawns ordinary Wither Skeletons, then harms holder. Skeleton tags :104 are owned MOB/UUID only, not magic/summactivated/mecha; global purge does not cover them.|MAG evil summon or explicit totem exemption?|
|Devil Totem activation burst/lava/death debt|`game/LbcTotems.java:54,116,143,160` invokes direct magic harm/Wither, lava work and delayed death without jammer gate.|MAG magical consequences or exempt consumed totem? Decide new activation, terrain continuation and debt separately.|
|Infinity totem refunds|`game/LbcTotems.java:54,67` returns queued copies without gate; native vanilla survival remains distinct.|MAG Infinity enchantment or totem exemption? User approved quantity N, not a suppression exemption.|
|Death Scythe / Banhammer charge collection|`game/LbcSpecialTools.java:83,99` collects kills/hits via active(), not magic(). Actual use checks magic().|Should MAG block charging as well as release?|
|Released Banhammer falling payload|`game/LbcSpecialTools.java:264` virtual holder falls/impacts without jammer query; no native actor for global removal. Initial use/ray :181–199 is MAG-gated.|MAG retire on entry, or release-only admission?|
|Scythe queued block erasure|`game/LbcSpecialTools.java:162` work validates lifecycle/time but not later beacon; initial ray/target is MAG-gated.|Should a later MAG beacon cancel admitted queued terrain work?|
|Physical landing Metal Pipe|`game/LbcMetalPipe.java:63` directly damages25 on landing. ItemEntity not purgeable. Held-pipe hit :79 adds sound only.|Likely ordinary physical exemption; ask before declaring TECH merely because metal. Metal Divider use :87 already MAG-gated.|

## Trinkets and equipment requiring family choices

|Mechanic|Evidence|Decision needed|
|---|---|---|
|Snow Pearl cold proc|`game/LbcTrinkets.java:202` calls Frost; `game/LbcFrost.java:71,80,93` has target/cleansing admission, no source jammer gate.|MAG activation? No hidden common frost gate covers this.|
|Ice Gem incoming cold aura|`game/LbcTrinkets.java:185,216` and same Frost path, no jammer gate.|MAG?|
|Gneiss resistance proc|`game/LbcTrinkets.java:188` directly grants ResistanceV.|MAG stone charm or ordinary physical?|
|Intercal Wither proc|`game/LbcTrinkets.java:203` directly grants attributed Wither (including skill15 stronger layer).|MAG?|
|Blaze bow/crossbow proc|`mixin/TrinketBowMixin.java:38` -> `game/LbcTrinkets.java:104`. No use gate; small fireball is globally MAG-purgeable.|Should MAG suppress proc generation as well as remove actor?|
|Ghast bow/crossbow proc|Same callback :119 creates FireworkRocket without magic/mecha tag. Neither beacon entity group includes firework_rocket; damage has no general gate.|MAG trinket or TECH firework? Existing projectile lifetime is not protection.|
|Nether Plug summon proc|`game/LbcTrinkets.java:142` -> `game/LbcBlaze.java:295` creates owned blaze without its own admission. Parent summon use is normally MAG-gated.|No isolated use gap claimed: decide proc's own origin and actor classification if parent can summon remotely/under other admission.|
|Blood Anal Boom and Mana Gem cast procs|`game/LbcTrinkets.java:147` -> Items.castWandDynamite :1337 and mana.add. Paid wand callbacks occur after successful MAG admission (`LbcWands.java:797`, `LbcBolts.java:302`). TNT is globally TECH-purgeable.|No generic missed cast gate claimed; decide whether a TECH beacon should block extra TNT creation while ordinary paid MAG casting is still allowed. Mana Gem's admitted direct refund needs no separate duplicate gate by default.|
|Techno/MK2 full-set electrical immunity|`game/LbcDamage.java:174` checks all four flags and rejects electrical damage without either jammer.|TECH ability disabled by mechanism, or inherent protective material/exempt equipment?|
|Held reflective shield|`game/LbcElectro.java:443,449,463` shield reflection bypasses blocker-only suppressed() check; neither TECH nor MAG gate.|MAG/TECH or protected defensive jammer? Explicit user reflection geometry does not itself answer jammer classification.|
|Electroblocker inventory aura|`game/LbcElectro.java:449` currently uses combined **MAG** suppression; no mechanism query. Legacy common branch uses stopper/TECH (`ARMORED_INVENTORY_SOURCE_2026_10_27.md:29`).|Wrong family versus intended protected jammer exemption. Requires author ruling; do not blindly switch it and suppress another jammer.|
|Existing Shulker full-set speed modifier|New grants are MAG-beacon-gated; `game/LbcShulkerArmor.java:99` validates equipment only, so entering beacon preserves modifier until inventory change.|Already documented source persistence. Is that intended, or should MAG remove this owned modifier immediately? All newly granted abilities already gated after author correction.|

## Covered paths and exemptions to retain

- Vampire/Guardian Sword (`LbcClassicMelee.java:35,59`), Devil Sword (:25,36),
  current jump artifacts (:24,39), current reactive artifacts (:37,47), current
  Epic/Fragment and Ender (:42,50,56), Gravity/Piston and Chaos flight
  (`LbcPendingCombat.java:69,79,122,205`), Fevgalea Star (:65), charged swords (:110),
  Kara slabs (:67), Vex Sword (:89) have real MAG checks. Chaos initial volley
  lacks a local use gate, but each tagged magic sword has suppression checks in
  step; do not claim a wholly unsuppressed payload. Epic queued fangs call
  `LbcEvokerBass.spawnOwned:75` and damage :127, both MAG-aware.
- Devil Scythe/Banhammer release is MAG-gated; Metal Divider use is MAG-gated.
  MAG-gated terrain may still warrant the continuation questions above; lack of
  a gate in crafting recipes alone is not a gameplay suppression defect.
- Safety Platform and Magnet use TECH at `LbcOrdinaryAccessories.java:53,56`.
  Devil Stomach :58, Death Compass :83, Quiver generation :59, Moses boots :53,
  Traveller chest/helmet :42,:51 use the intended existing pure MAG-beacon checks.
  Regeneration Belt uses Paper-compatible MAG AABB :89; Malevich inventory and
  ground conversion both use MAG AABB :62,:145,:163. Do not replace shapes or
  broaden beacon-only checks to personal no-magic/aura without approval.
- Shulker armor helmet/legs, chest, full set and jump are MAG-beacon-gated
  (`LbcShulkerArmor.java:37,46,59,68,92`); author explicitly corrected missing
  source helmet/legs checks. This is proof that source omissions are reviewable,
  not proof that all other omissions are intentional.
- Anti-magic artifact type1 and both beacons are jammers; **preserve the rule
  that other jammers do not suppress them**. Type2 explosion erasure and type4
  arrow erasure have no MAG-beacon veto in `LbcMagic.java:671,709` and directly
  kill intercepted actors. Their countermeasure/jammer status must be resolved
  before any blanket fix; absence of a gate alone is not a defect. Type3 liquid
  aura and types5/6/7/8 passives already have pure beacon checks (:759,:879,:893).
- Explicit user MAGIC exemptions in AGENTS.md:4687–4705 are Cyberkatana melee/use
  and already released electrical arcs. Do not add MAG gates to them, even if an
  electrical effect looks supernatural. Attack Helicopter family is explicitly
  TECH (AGENTS.md:3957,:3993); other auditors cover it. Do not infer those narrow
  decisions exempt unrelated armor, charms, shield or food bonuses.
- Antimagic Sword applies personal no-mana status at `LbcPendingSwords.java:51`
  without the ordinary MAG gate. Preserve pending classification as a jammer,
  rather than treating its ungated anti-magic action as an automatic repair.
- Ordinary native equipment attributes, enchantments, potion expiration, food
  nutrition, crafting and vanilla totem health restoration are not implicitly
  classified as suppressible magic here. Only their implemented custom branches
  are enumerated. Existing item presentation/acquisition does not prove an
  unported armor ability exists; no proposed missing gate on an unported engine.

Boundary: passive equipment/inventory/artifact, food and melee consumer paths,
including owned continuations and relevant mixins/central guards. Dedicated
wands/grimoires, guns, encounters, large drops and the full electric family are
assigned to other auditors; overlap findings were sent to the parent. No network,
runtime experiments or claims that historical source/test notes constitute author
approval. Decisions should be collected from this full list before implementation.
