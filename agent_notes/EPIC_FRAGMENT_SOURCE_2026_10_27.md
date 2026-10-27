# Epic and Fragment swords - complete four-variant source preparation

27 October2026, read-only audit. Source root S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`; paths below relative to S. No code/build/server/RP changes or runtime verification. Native Java has no live item handlers for cool/cool_old/fragmentsword/fragmentsword_old; existing wand/shared producers are not implementations of these swords. This is independent research, not permission to enable partial variants.

## Input and mapping

All four enter `advancement/gowno.json` accepted unblocked non-arrow player_hurt_entity, victim not interaction, owner outside nexus/imprinted -> `function/items/swords.mcfunction:5-9`. Mainhand flag decides branch. Current cool/fragmentsword reject nomagic_active nonspectator players within20 and stopper_magic markers within20; old variants have no branch-local magic suppression. No per-item charge, mana, cooldown, sneaking or RMB branch. Revoke gowno enables repeat hits; attributed non-arrow children can recursively trigger current selected item, so native ownership repair must consider existing recursion policy. Arrow children do not retrigger arrow_only-excluded event.

Current cool rolls inclusive random1..17 once, calls named helper. Current fragment uses cooltype0..16 without reroll; missing/out-of-range cooltype has no action. Old cool rolls1..17 with inline commands. Old fragment uses its own inline0..16 mapping. Existing epicswordattacks/*_old files are NOT called by these inline old dispatchers; do not substitute them merely because filenames match.

|Current cool roll|Current fragment cooltype|Helper|Old cool roll|Old fragment cooltype|
|---|---|---|---|---|
|17|0|absorption|15|15|
|1|1|arrows|6|6|
|2|2|bounce|7|7|
|3|3|evoker|4|4|
|4|4|explode|8|8|
|5|5|flame|17|2|
|6|6|flash|9|9|
|7|7|heal|2|1|
|8|8|instantdamage|16|16|
|9|9|invisibility|5|5|
|10|10|randomtp|12|12|
|11|11|resistance|1|0|
|12|12|screamer|11|11|
|13|13|speed|3|3|
|14|14|strength|10|10|
|15|15|water|14|14|
|16|16|weakness|13|13|

## Current17 branch behavior

Notation effects duration in seconds / zero-based amplifier; hidden=true only when stated. Particles coordinates relative owner feet, sound master to all players unless target stated. Source E=`function/workingitems/epicswordattacks/`; every row lists all non-scratch side effects. Source fake *tempt lbcID2 captures owner team before filtered selectors.

|Helper:lines|Gameplay/targets|Feedback|
|---|---|---|
|absorption:1-3|self absorption30/1|falling_honey at y1 spread.3/1/.3 speed0 count150 normal; player.burp1/1|
|arrows:1-10|8 arrows at y1.62, world-axis XZ offsets cardinal/diagonal +/-1, velocity corresponding axes +/-8 each (diagonals unnormalized), PierceLevel10, killedarrow; default damage/pickup/no owner fields|end_rod y1 spread.3/1/.3 speed.1 count40 normal; skeleton.shoot1/1|
|bounce:1-6|self jump_boost10/2; all entities distance.01..6 !spectator !same_team levitation1/7|sneeze y.1 spread3/.1/3 speed0 count140 normal; affected nonspectator players player_pumped_up green; iron_golem.damage1/0|
|evoker:1-4|tag evokercast1 -> scheduled three-stage fang closure below|witch y2 spread.3/0/.3 speed0 count35 FORCE; evoker.cast_spell1/2|
|explode:1-4|self resistance1/4 hidden, slow_falling4/0 hidden; creeper at owner feet ExplosionRadius2/Fuse0/ignited1, no owner/team tag|fire.extinguish1/1|
|flame:2-5|self fire_resistance25/0 then8/1 (two independent level layers)|flame y1 spread.3/1/.3 speed0 count45 normal; blaze.shoot1/1|
|flash:1-6|self glowing5/1; nonspectator enemy players distance.01..6 blindness5/1|player_blinded gray to affected players; red_mushroom_block block_marker y1 spread6/3/6 speed0 count600 FORCE to enemy players distance.01.. with NO maximum; illusioner.prepare_blindness1/2|
|heal:1-3|self instant_health1/1, native instant heal/undead semantics|heart y1 spread.3/1/.3 speed0 count25 normal; player.levelup1/1.5|
|instantdamage:1-7|#mobs distance.01..10 !spectator !same_team damage8 arrow by tagged dmger owner; global temporary tag removed after|player_crippled red to affected nonspectator players; damage_indicator y2 spread10/5/10 speed0 count400 normal; player.death1/1|
|invisibility:1-3|self invisibility20/1 hidden|white_wool block_marker y1 spread6/3/6 speed0 count50 FORCE; evoker.prepare_wololo1/1|
|randomtp:1-6|all entities .01..6 !spectator !same_team; spreadplayers centerowner min0 max20 respectTeamsfalse, Nether under120 otherwise normal|each target enderman.teleport1/0 at target and portal y1 spread.3/1/.3 speed2 count150 default normal; player_teleported light_purple affected nonspectator players|
|resistance:1-3|self resistance5/1|iron_block block particles y1 spread.3/1/.3 speed0 count45 normal; anvil.break1/1|
|screamer:1-4|no damage/effect|ghast.hurt1/1 globally then10/0 to enemy players .1..15; elder_guardian at owner spread0 speed1 count10 FORCE to same enemy recipients|
|speed:1-3|self speed2/2|cyan flash RGBA[0,1,1,1] y1 spread10/10/10 speed0 count100 normal; firework_rocket.large_blast1/1|
|strength:1-3|self strength5/0|sweep_attack y1 spread.3/1/.3 speed0 count40 normal; ender_dragon.growl1/2|
|water:1-2|self conduit_power10/1|dolphin.attack1/0|
|weakness:1-5|all entities .01..7 !spectator !same_team weakness6/0|player_weak gray to affected nonspectator players; falling_nectar y2 spread4/.1/4 speed.1 count350 normal; elder_guardian.curse1/1|

Source effects default visible particles when no true argument; flags must not be normalized. Native signed-effect layering and approved attribute/item caps remain authoritative. Flash and invisibility block-marker particles plus source extreme effects are presentation requirements, not evidence physical client rendering was verified.

## Old inline behavior and exact differences

All old branch particle modes FORCE except randomtp portal (default normal); all ordinary old sounds master @a distance<=20 volume10, pitch as current branch unless exception below. Old no tellraw feedback. Target same_team uses scratch owner team copied by dispatcher/old routine. Inline selectors have NO !spectator and no explicit type restriction unless described; native role/ally defaults still supersede unsafe source selectors.

|Old fragment type / cool roll|Full old action and difference from current|
|---|---|
|0 /1 resistance|self resistance5/1; iron_block same particle geometry45; anvil.break10/1|
|1 /2 heal|self regeneration1/99, NOT instant_health; heart25 same geometry; levelup10/1.5|
|2 /17 flame|self fire_resistance16/0 only; flame45 same geometry; blaze.shoot10/1|
|3 /3 speed|self speed2/2; WHITE flash RGBA[1,1,1,1], otherwise same geometry100; large_blast10/1|
|4 /4 evoker|same tag/cast1 three-stage closure; witch35 same geometry FORCE; cast_spell10/2|
|5 /5 invisibility|self invisibility20/1 hidden; campfire_cosy_smoke y1 spread.3/1/.3 speed.4 count70 instead of block marker; prepare_wololo10/1|
|6 /6 arrows|same8 arrows, positions/velocity/PierceLevel10/killedarrow, no owner; end_rod40 same geometry; skeleton.shoot10/1|
|7 /7 bounce|self jump_boost10/2; enemy entities distance.1..4 levitation1/7; sneeze140 same geometry; iron_golem.damage10/0|
|8 /8 explode|self resistance1/4 hidden, NO slow_falling; radius1 instant creeper at local forward-.5 (pitch/yaw apply); fire.extinguish10/1|
|9 /9 flash|self glowing3/1; enemy entities .1..4 blindness3/1; smoke y1 spread3/.3/3 speed0 count200; prepare_blindness10/2|
|10 /10 strength|self strength5/0; sweep_attack40 same geometry; growl10/2|
|11 /11 screamer|enemy players .1..15 ghast.hurt volume1000000/pitch0; as each target at itself elder_guardian spread0 speed1 count1 FORCE @s; NO global1/1 sound|
|12 /12 teleport|enemy entities .1..5 spreadplayers ownercenter min0 max20 normal, NO Nether under120 branch; portal same geometry150 at targets; sound executed as target WITHOUT at target, so originates at owner, recipients ownerdistance<=15, volume1/pitch0|
|13 /13 weakness|enemy entities .1..5 weakness6/0; falling_nectar350 same geometry; curse10/1|
|14 /14 water|old fragment conduit_power4/1; old COOL conduit_power3/2 (different!); dolphin.attack10/0|
|15 /15 absorption|self absorption30/1; falling_honey150 same geometry; burp10/1|
|16 /16 instantdamage|enemy entities .5..10 effect instant_damage1/0, NOT damage8 arrow; native undead inversion applies, no explicit owner field; damage_indicator400 same geometry; player.death10/1|

Unused *_old helper files differ from inline old: bounce_old has no same_team and uses.01..4 with tellraw; flash_old/instantdamage_old lack same_team. They are not a shortcut for old ports. Exact dispatcher mappings above are mandatory.

## Nested entities, ownership and cleanup

`other/evoker/cast1:1-4`: add evokercast2 to executor; summon fangs at EACH player tagged evokercast1, local up.5/forward1.5 (feet anchor, current yaw/pitch), remove executor evokercast1; schedule cast2 in5t. cast2:1-4 globally tags all evokercast2 as3, summons at local up.5/forward3, removes2, schedules cast3 in5t. cast3:1-2 summons tagged3 at local up.5/forward4.5 then removes3. Stages2/3 read player's NEW position/rotation at execution, not captured original ray. Source schedule replace/shared tags allow overlapping casts to interfere. Fangs have no Owner/Warmup override or custom damage; native defaults and engine attack clock apply. Do not invent damage amount from text alone; native owned fang default6 can reuse existing lifecycle.

Arrow helpers set no owner/damage/pickup/NoGravity/lifetime. Engine default physics/damage apply; source central `projectiles/pre:17` -> prein kills killedarrow on ground. Arrow special on-hit path `projectiles/arrows:5` also kills killedarrow but is entered only through matching potion-special machinery; do not assume generic immediate all-hit deletion. Native user requirement mandates original owner, correct swept collision and shared projectile lifetime/cleanup. No source custom fixed age exists here.

Creeper helpers set only radius/fuse/ignited; source does not assign owner, team, magic/epoch or suppression tags. Native explosion terrain/entity behavior and inherited fall/chain attribution must use original owner. Do not claim friendly-player protection existed in source blast. Instantdamage current uses arrow8 and cannot recursively proc arrow-excluded sword event; old magic instant damage/fangs/blast are non-arrow and may retrigger after ownership repair. Existing shared no-recursive-producer rules need explicit integration, not extra charge/payment.

## Loot, acquisition, Looper and advancement

Current Epic: `loot_table/transformables_sword.json:300-360` iron_sword, weight10/quality8/count1, lbc:tier_b, model lbc:epicsword, max_damage300; name epic_sword dark_purple italicfalse; lore type+sword aqua italicfalse then epic_sword.lore light_purple italicfalse; flags transformable1/sword1/cool1. Old Epic `legacy_luckyblock:2010-2056`: golden_sword/unbreakable{}, same model/name/lore content but no type row/tier style; flag cool_old1.

Current fragments table:17 equally weighted entries (cooltype0..16), each iron_sword/count1, tier_u/model lbc:fragmentsword/max_damage600; name fragment_sword light_purple italicfalse; lore type+sword aqua italicfalse + fragment_sword.lore dark_purple italicfalse; flags transformable1/sword1/fragmentsword1/unresolved_fragment1 plus cooltype. Old fragments17 equal entries: golden_sword/unbreakable{}, same model/name and TWO lore lines including type row, NO tier style; only fragmentsword_old1/cooltype. No attribute rewrite on these four source variants.

Current transformables_sword:765 references fragmentsword; legacy_luckyblock:3772 references fragmentsword_old. Ordinary transform/reforge sword routes select current transformables_sword; source legacy luckyblock supplies old variants. No dedicated ingredient recipe or input helper was found for these flags beyond these pool routes. Presence in source pool does not establish native imported acquisition.

Looper: `swrg_kit_integration/game/skills/looper_tick:2` detects unresolved_fragment mainhand and calls looper_explain_fragment. Helper :1 copies held stack to scratch chest; :2-18 replaces lore[1] with fragment_N yellow italicfalse for current cooltype0..16; :21 removes unresolved_fragment; :23 writes stack back. No change to gameplay cooltype. Native current Java search finds no unresolved_fragment/cooltype handler; don't infer Looper implements this just because other explanations are native. Preserve independent already-approved Looper recipient/lore rules.

Epic advancement parent vampire, player_killed_entity targetplayer with mainhand transformable/sword/cool1. Fragment advancement parent terra:17 inventory_changed criteria cool_1..cool_17 for current flag/cooltype0..16 and nonspectator owner; default AND requirements mean collection over time, not all simultaneous inventory. Old variants do not match these current flags.

## Reuse assessment and pending choices

Existing `LbcEvokerBass.spawn` is PRIVATE: native owner/epoch/role-controlled fang lifecycle is reusable internally, but `use` spends wand mana, adds formation choice/advancement and uses different placement. Expose explicit-origin owned fang producer and separate3-stage moving-owner scheduler; do not call wand use. Its fangHit native6 indirectMagic/role guard is appropriate default semantics, subject to all existing attribution policy.

`ItemEffectLevels.create` / native addEffect and independent layered effects can implement17 effects without hard-coded heal substitutions. `LbcOverload.sound` covers audience/range/minimum handling; current global audiences and old range20/very loud screamer must be explicit. Existing FullInvisibility and signed fire-resistance world effects must be reused (current flame25/0 +8/1 intentionally layers source fire support). LbcEnderWiki wand teleport chooses one destination and includes wand payment/feedback; not exact spreadplayers all-target behavior. Existing LbcCreeperDrops has owned native creeper lifecycle/explosionTarget, but its public egg/drop routes have unrelated radius/rewards/trap formations; extract explicit producer, do not invoke drop spawn. Native arrows use ordinary Arrow ownership/physics plus shared tracking/lifetime; no existing wand producer was established as exact8-arrow world-axis shortcut.

Pending author questions: preserve old million-volume screamer? preserve current flash600 block markers globally to nonteam recipients and invisibility markers? exact overlap policy for legacy global scheduled fangs versus isolated owner sequences? native recursion policy after adding missing legacy fang/blast/instant-effect owner? These are reported to parent, no user chat questions. Do not silently normalize old variants into current behavior. No partial enabling or runtime qualification is claimed.

## Original-byte SHA256 ledger

- `function/workingitems/coolsword.mcfunction` `f270b3c44b2dc3aef7997d99ff13d336a0b1deab9248e328f3bd7e3ec7ffcf68`
- `function/workingitems/coolsword_old.mcfunction` `43c863a4193c76ab20bc85b2d4f3f5d69442557ff1c91f2a0fb9ec10cf78268a`
- `function/workingitems/fragmentsword.mcfunction` `5712e528219d0c7cfad1c5275875d7a7152d4d675c99c6acb3d5a96c5cbb9ccf`
- `function/workingitems/fragmentsword_old.mcfunction` `64fdfd336c6f70d66dbfdd03c891b4db16ad581c1f237528e2ff5aa0e1b60986`
- `function/workingitems/epicswordattacks/absorption.mcfunction` `9c61d4ded09d1e6412e55d4e3002487e24b545aafa2a4ae3e1fa2b8c680c3952`
- `function/workingitems/epicswordattacks/arrows.mcfunction` `f45ae7f8f2cd71541b65009fd8fe611599d93c4d44f4bc27a51f7c5807782bd5`
- `function/workingitems/epicswordattacks/bounce.mcfunction` `9e8316bc38a968226d1857b21fe3ceb66045e802b564509aa83ad29a38455715`
- `function/workingitems/epicswordattacks/bounce_old.mcfunction` `9f9866bec039545b0bd9ee347f485f2f56f912ea17f56373d23325498abddc96`
- `function/workingitems/epicswordattacks/evoker.mcfunction` `51d2a4a4959438746ac348ef1e794d11bf0f8dba0251e46ae5784f30c4705cba`
- `function/workingitems/epicswordattacks/explode.mcfunction` `f49524838dd31a9591d5482777c4be388c47a54c4c11cbac860da0ae66342628`
- `function/workingitems/epicswordattacks/explode_old.mcfunction` `12cb829ff5b59ccd47b2836b9e62185884ecbae955e005078964589bcef7ab84`
- `function/workingitems/epicswordattacks/flame.mcfunction` `ba26706c290c1d7563dccfde5682e80e819fe7ee8ad3297448c4a1dbf5b184d2`
- `function/workingitems/epicswordattacks/flame_old.mcfunction` `5f7d9774eb235ae799e3119ff863b606524a3aa4bf2949bb68bca64a89470aff`
- `function/workingitems/epicswordattacks/flash.mcfunction` `22e3d364ffece92d6d9e617e89bb80198c0829f12a5b262544db35724a8849c1`
- `function/workingitems/epicswordattacks/flash_old.mcfunction` `a0a61cdac3c6d218fb7f2bdc28fb3aca585cd2360b6f19ea4e44d91d63178fae`
- `function/workingitems/epicswordattacks/heal.mcfunction` `d1f3da66cda9d393e77b869a8816b3290c4a1dc6443f6e0a6fabe8f27412dd7c`
- `function/workingitems/epicswordattacks/heal_old.mcfunction` `5e4076ca3da5f862bdbf3cd2eb349f83cf02a6951a79b388a16c4fda409fdd09`
- `function/workingitems/epicswordattacks/instantdamage.mcfunction` `e5bcbe4e38dbed38e318e30f049df8c48071d82729859df7ea48c918cc242711`
- `function/workingitems/epicswordattacks/instantdamage_old.mcfunction` `01047ad54b46c0b16e21da628cc96b97d5597bc1ec67d698711f70b087c21b60`
- `function/workingitems/epicswordattacks/invisibility.mcfunction` `06a0cae600006f41ddfeee3e4fa64e7fede41f5edac011f324d7844e5191b018`
- `function/workingitems/epicswordattacks/randomtp.mcfunction` `554c294ee7cfa9b4b11a22681a83ee512c456c1dc70406b801c656784a0837f6`
- `function/workingitems/epicswordattacks/resistance.mcfunction` `6f0e7dcb9a00490114d3d6dc41386a9855990027253e9501950aa731784f2b8a`
- `function/workingitems/epicswordattacks/screamer.mcfunction` `63ff8cfc1ddf132f8614cbefc1ef28163efeefe943a410446e622cdd088ae2dc`
- `function/workingitems/epicswordattacks/speed.mcfunction` `a2849ff98f20071a4ebe35d7d8bd684196f7f6b2622141cea53323ec7132863b`
- `function/workingitems/epicswordattacks/strength.mcfunction` `4c22c3b870d565219b9b6088716082e5ba8d83e0388ec412c200b89d1de4127c`
- `function/workingitems/epicswordattacks/water.mcfunction` `d70b79e747cd49419aded16d8dcf32c0a74e7d8be4bd0cf6c0a15b58be498fe1`
- `function/workingitems/epicswordattacks/weakness.mcfunction` `6511a2b232ef3405196c9e82166422650bd56fb615e2955be056c766f0524710`
- `function/other/evoker/cast1.mcfunction` `a852496e205fded44e80838da64dd60d79f16a9b9113a4addb53d3c14e723437`
- `function/other/evoker/cast2.mcfunction` `875f8d147af355894d08adfa65a5be68166d8a7e708ec54c72b931d9d8e2d6c6`
- `function/other/evoker/cast3.mcfunction` `a5c093a0d1199a1220c615dab552e678c6e6c3c25e61a6c59f366faa66de4638`
- `function/items/swords.mcfunction` `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f`
- `advancement/gowno.json` `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f`
- `loot_table/fragmentsword.json` `95cf9d79155a0d5348fc1140ccc9eb6b0d93ac8f2da358a1d94daf939cd5ce7a`
- `loot_table/fragmentsword_old.json` `19c28503bfb8f166ddd182af1fa84f2985bf9993d73d80af675c222f553f28c7`
- `loot_table/transformables_sword.json` `9f4de28a3a531f37ad0884ca5cd59fce602e15f363e86846d21805032d71d997`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `function/swrg_kit_integration/game/skills/looper_explain_fragment.mcfunction` `e49168ed4da47d46322802459048d61a30ba0ff17fdf42cc327c75b075aaed2f`
- `advancement/true_advancements/swords/epic.json` `111593bf84401066ac7e1f5e3d38484f54e8834b94936b27de15575e184ea650`
- `advancement/true_advancements/swords/fragment.json` `7b0ea58bbab09f2ad06ae8d5aa274ef04e0e7a00df06d4dbbcbc895734011217`
- `function/projectiles/pre.mcfunction` `e4f47beebf2095493dfacb8318c03376947939b4a9cdc248835102032be74315`
- `function/projectiles/prein.mcfunction` `d008be5bad4db7eb0baefd3c74536a5306d208af45daa2c7707eff4888ebfc4b`

## Exact dispatch line references

`coolsword` branch starts: 17:L2, 1:L3, 2:L4, 3:L5, 4:L6, 5:L7, 6:L8, 7:L9, 8:L10, 9:L11, 10:L12, 11:L13, 12:L14, 13:L15, 14:L16, 15:L17, 16:L18.

`coolsword_old` branch starts: 1:L3, 2:L7, 17:L11, 3:L15, 4:L19, 5:L24, 6:L28, 7:L39, 8:L44, 9:L48, 10:L53, 11:L57, 12:L60, 13:L64, 14:L68, 15:L71, 16:L75.

`fragmentsword` branch starts: 0:L2, 1:L4, 2:L6, 3:L8, 4:L10, 5:L12, 6:L14, 7:L16, 8:L18, 9:L20, 10:L22, 11:L24, 12:L26, 13:L28, 14:L30, 15:L32, 16:L34.

`fragmentsword_old` branch starts: 0:L1, 1:L5, 2:L9, 3:L13, 4:L17, 5:L22, 6:L26, 7:L39, 8:L44, 9:L48, 10:L53, 11:L57, 12:L60, 13:L64, 14:L68, 15:L71, 16:L75.

## Read-only RP text/model audit

`en_us.json` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "epic_sword": "Epic sword",
  "epic_sword.lore": "Hits have a random effect",
  "fragment_sword": "Fragment of incredible sword",
  "fragment_sword.lore": "Hit got definite, but unknown effect.",
  "fragment_0": "Absorption II",
  "fragment_1": "Arrows",
  "fragment_2": "Jump boost + throws up target",
  "fragment_3": "Evoker fangs",
  "fragment_4": "Explosion",
  "fragment_5": "Flames",
  "fragment_6": "Blinding flash",
  "fragment_7": "Healing",
  "fragment_8": "Splash damage",
  "fragment_9": "Invisibility",
  "fragment_10": "Random teleport on target",
  "fragment_11": "Resistance II",
  "fragment_12": "Screamer on target",
  "fragment_13": "Haste",
  "fragment_14": "Strength",
  "fragment_15": "Conduit power",
  "fragment_16": "Weakness on target",
  "player_pumped_up": "You got lifted up!",
  "player_blinded": "Some bright light blinded you!",
  "player_crippled": "You're crippled!",
  "player_teleported": "You're suddently got teleported!",
  "player_weak": "Someone made you weaker!"
}
```

`ru_ru.json` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "epic_sword": "Невероятный меч",
  "epic_sword.lore": "Удары имеют случайные эффекты",
  "fragment_sword": "Фрагмент невероятного меча",
  "fragment_sword.lore": "Удар имеет определённый, но неизвестный эффект.",
  "fragment_0": "Поглощение II",
  "fragment_1": "Стрелы",
  "fragment_2": "Прыгучесть + Подбрасывание",
  "fragment_3": "Клыки Заклинателя",
  "fragment_4": "Взрыв",
  "fragment_5": "Пламя",
  "fragment_6": "Ослепляющая вспышка",
  "fragment_7": "Исцеление",
  "fragment_8": "Урон по площади",
  "fragment_9": "Невидимость",
  "fragment_10": "Телепортация цели прочь",
  "fragment_11": "Сопротивление II",
  "fragment_12": "Пугающий страж!",
  "fragment_13": "Ускорение",
  "fragment_14": "Сила",
  "fragment_15": "Сила источника",
  "fragment_16": "Ослабление цели",
  "player_pumped_up": "Вас подбросили!",
  "player_blinded": "Вас ослепило яркой вспышкой!",
  "player_crippled": "Вы покалечены!",
  "player_teleported": "Вас внезапно телепортировало!",
  "player_weak": "Вас ослабили!"
}
```

`assets/lbc/items/epicsword.json` SHA256 `00bb2aa96cf58a55c5ce783cd6646c93cf7a8d1c5e944ed74bf2b9c3f8de01b6`; {     "model": {         "type": "model",         "model": "lbc:item/epicsword"     } }

`assets/lbc/models/item/epicsword.json` SHA256 `9f10f10179a4e018611fbe553a43c5a004ef846a62535b7af41a777da6dfd37f`; {   "parent": "minecraft:item/handheld",   "textures": {     "layer0": "minecraft:item/epicsword"   } }

`assets/lbc/items/fragmentsword.json` SHA256 `40f4f2321b9f57d6c0d4252cd8a9a0f492aeee936a2e438ce71f5bcd5404b0f0`; {     "model": {         "type": "model",         "model": "lbc:item/fragmentsword"     } }

`assets/lbc/models/item/fragmentsword.json` SHA256 `d54639b501afc97579d262a9af3773f20c9d68229bec299c3c15702141ad4eac`; {   "parent": "minecraft:item/handheld",   "textures": {     "layer0": "minecraft:item/fragmentsword"   } }
