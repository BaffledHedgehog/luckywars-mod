# Piston, Triple Golden and Blazefire sword source contract

Read-only audit, 27 October 2026. Source root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`. This is source behavior, not a claim of native implementation or runtime verification. Only this new note was written. Root/notes AGENTS and RESOURCE_PACK read. User requirement: inherit original owner for direct and deferred kills; source gaps must be repaired through shared native attribution. Suspicious behavior below requires the author's answer; no silent correction is authorized.

## Input and charge

| Item | Activation | Charge | Tick/reset |
|---|---|---|---|
| Piston | mainhand spell1_default:83, offhand spell1_default_left:68; no per-item mana/cooldown/charge guard | none | immediate synchronous terrain operation |
| Triple | same files :86/:71; swordcharged8 >=1 | accepted `gowno` non-arrow, unblocked player_hurt_entity while mainhand; +1 only when existing score <=5, therefore maximum6; click debits1 for three swords | purify:57 and resets:15 set0; KFC all_up_scores_no_mana:47 adds1 without cap |
| Blaze | same files :101/:85; swordcharged9 >=5; click only adds firefarting | same accepted non-arrow damage event mainhand adds1 without cap | players_2t:19 calls misleadingly named tick_5t every2 ticks; pulse debits3, stops/clamps0 at <=0. purify:58/resets:16 zero charge but do NOT explicitly remove firefarting |

Shared input: `interaction_rightclickable` -> items/spells -> spell1/spell1_left -> default unless upgraded. Both hands run sequentially; offhand gate lefthand swrg.math !=1. Owner tag nomagic_active and any nonspectator nomagic_active player within20 prevent activation. These three default branches have no stopper_magic check. No sneaking branch. `gowno` excludes interaction target and nexus/imprinted; it excludes damage tag arrow_only, not only ordinary melee. Thus Blaze's own attributed in_fire while the sword remains selected can recursively gain charge on successful non-arrow damage; arrow follow-up cannot. Preserve native accepted-damage/fatal integration and avoid accidental recursive charging introduced solely by fixing originally unowned sources.

HUD ui/charging:9,10 adds one score segment if either hand matches: text `    \uE103 ?fx`, gold score swordcharged8/9. Piston has none. Charge belongs to player, shared between copies/hands.

## Piston exact terrain algorithm

click sets Distance0, fake check0, fake if1; ray starts at eyes and sounds piston.extend master to all, volume1/pitch1. Ray increments Distance then 15% redstone-block particles; first block other than literal minecraft:air enters ray_2; otherwise recurse .33 blocks while Distance<=200. Hence samples0..66 at most201 samples. Cave/void air and liquids count as obstruction. ray_2 centers floorXYZ+.5 and ray_2_r recursively checks local-forward1, floors/centers each next cell, until literal air. This is a digital path with full yaw/pitch, not an axis-aligned vanilla piston direction. Unwind: abort if fake if0; clone current cell to local-forward1 TWICE, storing success of second; abort on failure; set current air; if it remains non-air set fake if0 and clear forward destination. No piston pushability/material restriction or chain bound is coded. Block entities travel via clone. No entity movement, damage, carrier or lifetime is defined. `ray_pre` additionally used by other/imba/chaos_sho_use, but does not initialize fake if; keep separate from this item's click.

The two clone commands are literal source behavior; duplicate execution alone does not establish an ordinary-block failure bug. Parent inspected cached native26.3 `net/minecraft/server/commands/CloneCommands.java:245-252`: clone stages BARRIER at every destination before applying final state, so the second ordinary STONE clone still changes BARRIER to STONE and succeeds. BARRIER-source cells are a separate zero-change/failure case (potentially even the first clone); partial failure semantics remain relevant. This native26.3 code evidence does not establish observed legacy1.21.4 server runtime. No author question should assume normal identical-clone failure. Source BARRIER/partial failure and resident-world behavior need a specific decision before the later Piston implementation batch; they do not block the three clear sword families.

## Triple carrier and collision

Three item_display from eyes minus0.5Y at yaw+10,0,-10, unchanged pitch. Exact fixed matrix and component payload are in appendix. Tags thromn_sword/slowraycast/player/magic, owner lbcID2/team copied; teleport/interpolation1, no shadows. Central tick_not_timestopped:3 -> raycastselector (Lifetime+1) -> raycastselector_player:39 -> tick, therefore1 active tick cadence, Time Stop freezes flight/age.

Each active step checks current and .3-ahead blocks are #minecraft:airs, not inground. Performs5 samples at offsets0,.3,.6,.9,1.2; final fifth sample teleports carrier to that point (1.2 blocks/tick). Each sample checks one #minecraft:mobs via dx=dy=dz=0 (one-block selector volume, entity hitbox intersection), distance<=8. Player carrier excludes owner nodmg, all same-score team nodmg1, spectator tag; owner/team tags assigned to nonspectator players globally for call and removed afterwards. No strict_map_object exclusion in player hit selector. First matching target takes4 arrow attributed to owner identified by nodmg, otherwise4 generic; crit8 + strong attack sound, then carrier killed. No homing/rotation update during normal flight. No explicit return on kill in sampling source.

If current block becomes non-air, homing_sword_break_pre recursively backs .1 until air then moves carrier forward.5 and marks inground, copper.break volume1.5. Grounded breaks if .5-forward becomes air or1% chance per tick; all break at Lifetime>=100. Break sound item.break volume.5 and10 golden_sword item particles (bone alternative shared helper). A .3-ahead obstruction can stall an ungrounded carrier in air until age100, since impact checks current block only.

External producer: golden_skeleton/portal_tick_attack creates same matrix/model plus evil. Faces nearest eligible nondungeon nonspectator, non-strict_map_object mob within30 eyes then universal_razbros; evil path excludes dungeon_mob and spectator, attributes to arbitrary nearby dungeon_mob within20 or generic fallback. Complete producer/helper texts included below.

## Blaze ray

No carrier/model projectile. While firefarting, regardless of held item, pulse plays firecharge.use within16 (volume1 pitch1 minVolume.2); resets Distance0 (never increments), tags caster, casts from eyes. Every sample emits10 flame with .8 spread/.01 speed; at sample minus1Y hits EVERY entity within2 except caster/hitten, once per pulse through hitten. Applies1 in_fire by caster then.1 arrow by caster, then hitten tag. No team/type/spectator/technical-role filter, no Fire NBT ignition. Recurses forward1 only if next block #minecraft:airs. No range or recursion cap whatsoever: open sky runs to command-chain limit/unloaded boundary. Clears global hitten, removes caster, debits3, clamps/removes fire tag at<=0. tick2t is scheduled independently and has no time_stopped guard; source Blaze pulses can continue in Time Stop (native shared Time Stop policy takes precedence if already required).

Native API suggestions: one shared owner/charge state, independent hand activation, explicit origin/rotation producer; reuse native visual projectile ownership, swept collision and shared cleanup. Blaze needs author-approved range/target policy before release. Source reset can leave tag until next pulse (including a free pulse at zero); native match-stop must clear owned state immediately.

## Acquisition, loot and advancement

Piston is weight10 entry in transformables_sword, reachable from ordinary reforge/transform sword routes; no dedicated recipe found by full identifier search. Triple standalone loot is included in transformables_sword_all (ultra-reforge) and guaranteed golden_skeleton death loot. Ordinary transformables_sword has no Triple entry. Blaze is kit/blazefire awarded by swrg_kit_integration/game/kit_select/select:30 for lbc.kit19; GUI page9000 slot19 selection requires academy/blazefire advancement. No dedicated sword craft recipe found. Piston task advancement parent anal_whip requires player kill while mainhand custom_data transformable/sword/piston_sword. Triple icon appears on kill_gold_skeleton advancement requiring kill golden_skeleton; no standalone triple sword advancement. Blaze sword has no own combat advancement; academy unlock is wand advancement.

Exact loot payloads and all directly relevant source lines follow, with SHA256 of original bytes. Piston has no max_damage field; do not invent one. Triple attack_damage+8, speed-2.4, durability900; Piston+7/-2.5; Blaze+6/-2.4/durability600. All are carrot_on_a_stick. All modifier ids base_attack_damage/base_attack_speed, add_value mainhand. Triple/Piston tier_b tooltip; Blaze omits tooltip_style. Exact names/lore styles and flags below are authoritative; do not normalize absent styles.


## Exact item component contract

| Field | Piston | Triple | Blaze |
|---|---|---|---|
| Definition | loot_table/transformables_sword.json:780-850 | loot_table/triple_golden_sword.json:7-80 | loot_table/kit/blazefire.json:7-72 |
| Base/count | carrot_on_a_stick /1 | carrot_on_a_stick /1 | carrot_on_a_stick /default1 |
| Model | lbc:piston_sword | lbc:triple_golden_sword | lbc:blazefire_sword |
| Tooltip | lbc:tier_b | lbc:tier_b | absent |
| Max damage | absent |900 |600 |
| Name translate/style | piston_sword; white, italic false | triple_golden_sword; green, italic false | lbckit.18; italic false; color absent |
| Lore | first array type+sword both aqua italic false; then piston_sword.lore.1/.2 white italic false | first array type+sword both aqua italic false; then triple_golden_sword.lore.1/.2 white italic false | lbckit.18.lore.1 #A6F0E6 italic true; .2/.3/.4 white italic false |
| Custom data | transformable:1,sword:1,piston_sword:1,magicwand:1 | swordwithability:1,transformable:1,sword:1,magicwand:1,triple_golden_sword:1 | swordwithability:1,blazefire_sword:1,magicwand:1 |

No empty lore lines. set_name component is an array of one object. set_lore replace_all; Triple additionally entity:this. Modifier ids minecraft:base_attack_damage / minecraft:base_attack_speed, operation add_value, slot mainhand. Absence of other component styles is meaningful.

Triple carrier matrix, exact: `[0,0,2.5,0,1.7678,-1.7678,0,0,1.7678,1.7678,0,0,0,0,0,3]`; item carrot_on_a_stick/count1/model lbc:triple_golden_sword; fixed display, interpolation_duration1, teleport_duration1, shadow_radius0, shadow_strength0. Source `function/items/swords/triple_golden_sword/triple_sword_spawn.mcfunction:1-5`. No extra random rotation for player producer. Mob producer alone uses universal_razbros.

## Source SHA256 ledger

Paths below are relative to the read-only data root. Line references in behavior sections refer to `lbc/function/` unless stated. These hashes cover original bytes; definitions remain authoritative in source, without copying their full bodies here.

| Source path | SHA256 |
|---|---|
| `lbc/advancement/gowno.json` | `b65994157053db257f20ffdd9304e2a0efb204114d2b73e4eb7da9f58b9a2e2f` |
| `lbc/advancement/interaction_rightclickable.json` | `d06ced365ba81fdde1cb1abd0027970a1879cc636efca698d1bc072c3512a0bb` |
| `lbc/advancement/true_advancements/legends/kill_gold_skeleton.json` | `7a93ac3092fa34b4fb7567054e717a43ecacc4ec206b1aac3e7e35d99b21d5ce` |
| `lbc/advancement/true_advancements/swords/piston_sword.json` | `6dc0b6a43b11397d7f1d97e6cdbc76d43f5f1e5de3a9bc7274b3a81cce255d62` |
| `lbc/function/items/spells.mcfunction` | `80ab5607eee4b352b1701391c643ea052b60fa834b2ba9a709287f6dc2af652e` |
| `lbc/function/items/swords/blazefire_sword/click.mcfunction` | `fafe2b55bbdd3ec2cdc858eda0cea7ad7ee7665c3f3dc75ae004e99947a2fa3c` |
| `lbc/function/items/swords/blazefire_sword/tick_5t.mcfunction` | `1021d4ef90df93f8cb41d4459b6ed6c07c750d71206a80e696d67bd207c547b9` |
| `lbc/function/items/swords/blazefire_sword/tick_5t_rc.mcfunction` | `d3a6b8c4f513f18eed3d72d69cf8cfa297825f56abe25680546fdd23e38fe472` |
| `lbc/function/items/swords/blazefire_sword/tick_5t_rc_damaged.mcfunction` | `817bd7f5e92bfe06fd319267261117cd5c40bfb280bcb49f7458eaa493dd84c2` |
| `lbc/function/items/swords/chaos_sword_imba/homing_sword_break.mcfunction` | `f7c862a6fca2d6f32ac839fc29289e6647143ebc70edf6b7c39fbe1bea79d351` |
| `lbc/function/items/swords/chaos_sword_imba/homing_sword_break_pre.mcfunction` | `c0410dd7b4fb6925c6087736a7001cdbc393e11651c11fbd1a5c1583bbe43856` |
| `lbc/function/items/swords/chaos_sword_imba/homing_sword_break_raycheck.mcfunction` | `83e5e73377c4a65f9d86c2abdc25da8df0078949354d024f7df5c1d4de2097d0` |
| `lbc/function/items/swords/chaos_sword_imba/homing_sword_dmg.mcfunction` | `cc19f893ed5a9c3a6cba3f4f6b811184ae8a02cbd3fd14a9bff1b35dd3d77132` |
| `lbc/function/items/swords/chaos_sword_imba/homing_sword_dmg_mob.mcfunction` | `b93166817927be10ea5a248d8aeefac90fc1373739e9711be9b2fc621524d46f` |
| `lbc/function/items/swords/piston_sword/click.mcfunction` | `c639cb9cc24a69ee85fa6bd1b806ff3b609138e6a8fe05904b5036a3938ebbf8` |
| `lbc/function/items/swords/piston_sword/ray.mcfunction` | `873ab30e8549c9289fdb71c806e3e4256d0906f75fa04ad497353a917a8471df` |
| `lbc/function/items/swords/piston_sword/ray_2.mcfunction` | `e94aa2c6214d90c852d82a04f9229ca78710681f3cf615b8a48c764405786c80` |
| `lbc/function/items/swords/piston_sword/ray_2_r.mcfunction` | `deea9de2f50de30765d3839915ef4934eae4ad47f5c5372da6056cfc07cb63ef` |
| `lbc/function/items/swords/piston_sword/ray_2_r_blocked.mcfunction` | `c3487cb38134a945199781f839734d4a7e8bb52e3215a552d8095418c3d972c8` |
| `lbc/function/items/swords/piston_sword/ray_pre.mcfunction` | `29e8e995031aa83e3dceaedab5ee2823f48bd1ca7490833d7f38340be7759ebc` |
| `lbc/function/items/swords/triple_golden_sword/click.mcfunction` | `a822b3a6fb8ddf3df5c734164a2d172ae0f3a71fc01dd9677208cd886d4e93cc` |
| `lbc/function/items/swords/triple_golden_sword/damage.mcfunction` | `7365fa54bbbc4f10e2b36a54635e5f68d1d3af8122a818a98accf070199a3e12` |
| `lbc/function/items/swords/triple_golden_sword/tick.mcfunction` | `9982b6ff5217ebc8de6b6f2dac041836d8ec80bfbecc5783e0c6572382ae379e` |
| `lbc/function/items/swords/triple_golden_sword/tick_2.mcfunction` | `ecf9446f7a8b028be3f158a97c6bf8d986bc9ea0fb3112c9291f834d33dd9a20` |
| `lbc/function/items/swords/triple_golden_sword/tick_2_mob.mcfunction` | `f6ef142eb824890fd8323af44a1b18de29952ae9817e31e7d2f2701173e8d708` |
| `lbc/function/items/swords/triple_golden_sword/tick_2_player.mcfunction` | `d82be976cca721069b90a378a83a04403a6cf8b262d73497f4f08bf3cac4ecf1` |
| `lbc/function/items/swords/triple_golden_sword/tick_loop.mcfunction` | `4d20274a037ddd9a42284bf4fdb8eefe255e24e281254d7022684080e0f58e97` |
| `lbc/function/items/swords/triple_golden_sword/tick_loop_mob.mcfunction` | `1e4af0441f53554b84f3273f31d1259f6d9f310b5460ef6b77a40753f39aa05c` |
| `lbc/function/items/swords/triple_golden_sword/triple_sword_spawn.mcfunction` | `ba22a6f92e585e7a93667cd7ee3545afc7bc6f6f82642699c90dcd0b4f285365` |
| `lbc/function/items/swords.mcfunction` | `36162bd3115e12c5d8b0626f619d0eb88b408505282519a191ea7d1bdeb33f1f` |
| `lbc/function/mobs_2t.mcfunction` | `f7b3fdb02223b2906d8cf42dc25589f90300f42f5f3679b319904b12f650f888` |
| `lbc/function/other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction` | `92d0ebd88f2d2680f099447c5cd158a82914a6923d1062b0cc3a9fb83e751701` |
| `lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/10s.mcfunction` | `1589dd97d30a53938fe8bfc40afa1ecda97d0ca4fdef0cdf2d63af176f71473e` |
| `lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick.mcfunction` | `a802350e9ac1f31c444e69f98bbbde8a5f6b510d60278157b26e98502129031d` |
| `lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick_attack.mcfunction` | `68f91d1c0b5331d69d5be5806ea4b6ac7bba68160637a178d2a8b427b1788bf4` |
| `lbc/function/other/purify.mcfunction` | `1a26be8a2874f20862f05fee6e536b6f778c38349160c08f1af00baf78ba631c` |
| `lbc/function/players_2t.mcfunction` | `3337da465d208c7b602454f34df96ce933a74c3c7ce3c0735e2346a0066ae70d` |
| `lbc/function/raycastselector.mcfunction` | `7b4951e7c1ac5aae5fd606e2d8ea53db0bbd8f9125fd77cd1ff4b0836f0c62b6` |
| `lbc/function/raycastselector_player.mcfunction` | `288deedb5f8835078612af498ecbe8cf9814784274d853511758fe0ce0e46094` |
| `lbc/function/resets.mcfunction` | `c10ba440615050e16d2fcb55d6a8032c289ba0cc462da3c29c1c9de59f65d9cc` |
| `lbc/function/tick.mcfunction` | `86e363bdb2197ab7a44dcdd892d719b07259a3772de71dbf03ac12fc46668965` |
| `lbc/function/tick2t.mcfunction` | `fcc4b8f246e9e03b4d1f0d12c011c29c791a64cb2960fe8f08098e9e164de2a0` |
| `lbc/function/tick_not_timestopped.mcfunction` | `55faddfdca07e3a486d0f826e9593969ae5fa05260cb2392c162569efa8cf5cc` |
| `lbc/function/ui/charging.mcfunction` | `03af83402fdbd2225d22ac2b069e4fdd1dab298f8577dcacc46ada32203c84fe` |
| `lbc/function/universal_razbros.mcfunction` | `1966d1df2e751d08bbd2d3e8fd6f84cc0f3cad7e0c294a07ab0a80cb7d92985e` |
| `lbc/function/workingitems/spell1.mcfunction` | `29f387521ee377010308e5bf819b0c8d372f406fd064ef41bf46700d643b833d` |
| `lbc/function/workingitems/spell1_default.mcfunction` | `f96b0af185d52fd8a18a6f15c1d9ecefd7678e725d0449673b3e39f9793f9773` |
| `lbc/function/workingitems/spell1_default_left.mcfunction` | `1fc3c74097023eafe4a7aedd15fb1c45e53efee3c6f8308efbd2e5207be83fe7` |
| `lbc/function/workingitems/spell1_left.mcfunction` | `65ef42b260171e33ec9f9231f324c2f425bfa9e0b3d66fd242ab4b144bfeaa43` |
| `lbc/loot_table/golden_skeleton.json` | `b3902f63be9f282e6a331c9964a0f15ad3f408dde56a36298952fb22e60ab4ac` |
| `lbc/loot_table/kit/blazefire.json` | `dd18048af13dcbc5ac1c027eeec7ddd33da74461c0d2fb4ac30bd13231394849` |
| `lbc/loot_table/transformables_sword.json` | `9f4de28a3a531f37ad0884ca5cd59fce602e15f363e86846d21805032d71d997` |
| `lbc/loot_table/transformables_sword_all.json` | `81f3d3501daeeb6058534c1bbfa0960192aea8029689c3755c94d3509d592a0d` |
| `lbc/loot_table/triple_golden_sword.json` | `de0eb55594f37b9b07e5e249d925eb8f52ae311c3495cb502e617eb39f5f95a0` |

## RP exact text and model audit

All existing resources read only; no template additions needed. English and Russian exact values below, including inherited styles from loot. `rpr` currently supplies same visible Russian strings for these keys. Source language hashes and model hashes retained.

`assets/minecraft/lang/en_us.json` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "type": "Item type: ",
  "sword": "Sword",
  "piston_sword": "Piston Sword",
  "piston_sword.lore.1": "RMB: pushes blocks in the direction you are looking",
  "piston_sword.lore.2": "There is no limit to the number of blocks that can be pushed.",
  "triple_golden_sword": "Triple Golden Sword",
  "triple_golden_sword.lore.1": "Grimoire Library Relic",
  "triple_golden_sword.lore.2": "Releases multiple copies of itself on impact",
  "lbckit.18": "Dirnuin",
  "lbckit.18.lore.1": "'Flame sword, that didn't leave scorches.'",
  "lbckit.18.lore.2": "Hits charge firethrower",
  "lbckit.18.lore.3": "Firethrower shoots flame stream, as long as",
  "lbckit.18.lore.4": "charges remained. Needs at least 5 charges to start."
}
```

`assets/minecraft/lang/rpr.json` SHA256 `d6976e0779f458511485596879f46ee0e91de68504a5303964b4d0f4f76e338a`; same relevant Russian values as ru_ru.

`assets/minecraft/lang/ru_ru.json` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "type": "Тип предмета: ",
  "sword": "Меч",
  "piston_sword": "Меч-Поршень",
  "piston_sword.lore.1": "ПКМ: толкает блоки в сторону взгляда",
  "piston_sword.lore.2": "Нет ограничения на количество толкающихся блоков.",
  "triple_golden_sword": "Трёхкратный меч-трезубец",
  "triple_golden_sword.lore.1": "Реликвия Библиотеки Гримуаров",
  "triple_golden_sword.lore.2": "Высвобождает несколько своих копий при ударе",
  "lbckit.18": "Дирнуин",
  "lbckit.18.lore.1": "'Пламенный меч, не оставляющий ожогов.'",
  "lbckit.18.lore.2": "Удары заряжают огнемёт",
  "lbckit.18.lore.3": "Огнемёт продолжительно стреляет огненным потоком,",
  "lbckit.18.lore.4": "пока не потратит все заряды. Нужно хотя бы 5 зарядов."
}
```

`assets/lbc/items/piston_sword.json` SHA256 `9ef12fd0ffaab8b4f72d5b1393c2a15d1f338663de8305a8fc42ba3296c6ceaf`.
`assets/lbc/models/item/piston_sword.json` SHA256 `caf79f5e60e11ca1115122c6e7b95f7ba3253511a7e353fc4764492610880c4d`.
`assets/lbc/items/triple_golden_sword.json` SHA256 `7d75698bc569aaa6662225115ad5e0767cdc476c17f213aa0745cad565371bef`.
`assets/lbc/models/item/triple_golden_sword.json` SHA256 `7a1cd6a635969b4d58025a718bc36705ee32e41e0a9a852c66fd63e003968bc4`.
`assets/lbc/items/blazefire_sword.json` SHA256 `061cd5f57aa6af84fb6a734d1f084f8568994c0d85d75a1383d2083443ba0d63`.
`assets/lbc/models/item/blazefire_sword.json` SHA256 `99b92883f90aa8a34f27fa17e4e93f575be5ff3d2e85914c9b7927f858f2dd5f`.

Item definitions assets/lbc/items/{name}.json all use model type model -> lbc:item/{name}. Model definitions assets/lbc/models/item/{name}.json all parent minecraft:item/handheld -> layer0 minecraft:item/{name}. Names: piston_sword, triple_golden_sword, blazefire_sword. Existing PNGs are retained untouched.

Piston lore explicitly declares unlimited number of pushed blocks. An arbitrary gameplay chain cap would contradict lore; resource safety/resident-world bounds need a separate implementation decision. Triple lore says copies appear on impact, but actual input code uses RMB and consumes melee-earned charge: preserve exact lore and report mismatch rather than rewriting it.

## Blazefire acquisition closure follow-up

Native catalog entry proposal (existing catalog schema): `{"id":19,"page":9000,"slot":19,"table":"lbc:kit/blazefire","gate":"lbc:true_advancements/legends/academy/blazefire","lockedKey":"lbc.adv.upgraded_tuntija.blazefire.lore"}`. Existing native catalog lacks19; build import must include kit/blazefire. No code changed by this audit.

Source `function/swrg_kit_integration/gui/page/9000.mcfunction:30` places **loot gui/page/9000/18** in **enderchest.19**, not slot18; :56 highlights it for kit19. `gui/select/9000.mcfunction:23` maps swrg.gui_select19 to lbc.kit19 with academy/blazefire gate; :51 supplies lockedKey above. Ranked `gui/select/9000_ranked.mcfunction:20` omits gate, same slot/id. Grant `game/kit_select/select.mcfunction:30` gives exactly kit/blazefire. Full LBC search of kit19 conditions finds only grant/selection/highlight, no additional passive attributes/effects/items.

GUI preview `loot_table/gui/page/9000/18.json:13-53`: carrot_on_a_stick, model lbc:blazefire_sword; name lbckit.18 white italicfalse; lore lbckit.18.lore.1-.4 gray italicfalse, followed by two compound statistics rows with pick_rate/pick_count and win_rate/win_count, gold labels/yellow values, stats:stats indices[18]. This GUI styling differs intentionally from granted sword lore.

Gate `advancement/true_advancements/legends/academy/blazefire.json:20-60`: parent academy/fire, challenge icon lbc:blazefire; inventory_changed requires custom_data upgraded1/transformable1/staff1/fireblaze_wand1; excludes playerGameType3; reward lbc:swrg_kit_integration/gain/kit. It unlocks kit, it does not directly grant sword.

Additional source hashes:

- `loot_table/gui/page/9000/18.json` SHA256 `e108d52555ead466fc6858789ecb6764cbacfbb40af61cbc09a42db66a9c8c2f`
- `function/swrg_kit_integration/gui/page/9000.mcfunction` SHA256 `629e7fe81399f3008d2a4d1131a17eae9f0ae5b4a6bec82d71e84d6c060a4659`
- `function/swrg_kit_integration/gui/select/9000.mcfunction` SHA256 `695024fb2e69c49f87da16fdaec95b08b2876bd2affb9df96360471b4350515c`
- `function/swrg_kit_integration/gui/select/9000_ranked.mcfunction` SHA256 `d27b696bec4f9ab16b792552cd6b8c4a9f9684ae37f487a6881b98e845fd705a`
- `function/swrg_kit_integration/game/kit_select/select.mcfunction` SHA256 `e2853d5bcc59bc4356ea3d0c6c1b51dda39a5f530198cdfef5b3fa4bf5a20727`
- `advancement/true_advancements/legends/academy/blazefire.json` SHA256 `6971db979e10d70d9e605a367fef27f15f880213dd12f1143722804dcf24606c`
