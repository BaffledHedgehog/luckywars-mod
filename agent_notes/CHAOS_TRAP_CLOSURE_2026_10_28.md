# Chaos native trap dependency audit — 28 October 2026

Status: **incomplete dependency, not qualified**. The producer exists, but six of
the 25 original entries initially had no matching native dropped activation. Neither a
successful native roll nor this producer establishes full Chaos completion.
No build, server, resource pack, commit or source JSON changes were performed.
Owned new Java and the explicitly handed-off shared LbcSpecialDrops were edited.
Read AGENTS.md, agent_notes/AGENTS.md and the Chaos
source audit. This new note owns the correction below; historical notes remain.

Latest author decision: FULL original pool, release only after the full required
transitive chain. No filtered table or silent exclusion is authorized. The new
actor code and shared dispatcher integration below are **uncompiled and
unqualified**; root owns the remaining item/combat/child hooks and broad reward
work. The initial-hole table below is the audit baseline; new actor and boss
dispatch branches are now explicitly wired in LbcSpecialDrops.

## Exact table

Existing `src/main/resources/data/lbc/loot_table/traps.json` is byte-identical to
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/traps.json`.
Both SHA256: `39D51F84624843DD716D32FC95E2B4AEB8EFF89B91FE43C0C2EEC559D87C5EB2`.
Keep the entire original table. It has one pool, one roll and 25 item entries.
Correction to CHAOS_STATIC_GEAR_SOURCE_2026_10_28.md: they are NOT all weight10,
nor all quality+/-10. Skeleton is weight20; ranked Sauvojen is weight1; slab
quality is+5. The entries described historically as Academy are actually the
Sauvojen Tuntija GHAST BOSS, not the Academy building handled by LbcAcademy.

Entry order (all flags also carry `drop:1`):

|Index|Raw flag|Weight|Quality|Current native dispatch|
|---|---|---|---|---|
|0|explode|10|-10|LbcTntDrops|
|1|korrosion|10|-10|LbcCorrosionDrops NORMAL|
|2|creepers|10|-10|LbcCreeperDrops NORMAL|
|3|yama|10|-10|LbcHoleDrops NORMAL|
|4|lookupanvil|10|-10|LbcCageDrops ANVIL|
|5|lookuplava|10|-10|LbcCageDrops LAVA|
|6|bob|10|-10|MISSING; bob_old does not match|
|7|skeleton|20|-10|LbcCreatureDrops FAT_SKELETON|
|8|vex|10|-10|LbcCreatureDrops VEX|
|9|obsidiantrap|10|-10|LbcCageDrops OBSIDIAN|
|10|badspawner|10|-10|LbcPotionSpawnerDrops NORMAL|
|11|droppertnt|10|-10|LbcDroneDrops normal|
|12|bullets|10|-10|LbcCreatureDrops BULLETS|
|13|skrimmer|10|-10|LbcSpecialDrops -> screamer NORMAL|
|14|slab|10|5|LbcSlabDrops NORMAL|
|15|clone|10|-10|LbcIslandDrops RAISE|
|16|evilluckyblock|10|-10|MISSING|
|17|lookupfireball|10|-10|LbcCageDrops FIREBALL|
|18|lookuppotions|10|-10|LbcCageDrops POTIONS|
|19|zombietnt|10|-10|LbcZombieTntDrops|
|20|aimbob|10|-10|MISSING|
|21|sauvojen_tuntija|10|-10|MISSING; only when ranked !=1|
|22|sauvojen_tuntija|1|-10|MISSING; only when ranked ==1|
|23|forest|10|10|MISSING|
|24|ultraponos|10|10|LbcUltraponos|

Ranked reads fixed score holder `ranked` objective `lbc.event` using original
native value_check/inverted conditions. Preserve absent-objective behavior from
the native codec rather than manufacturing a default. Normal default-luck0
eligible total weight is250 outside ranked,241 in ranked. Same item output for
entries21/22 does not merge their distinct conditional weighted admission.

## Implemented producer and integration

New `LbcChaosTraps(GameController)` exposes
`public List<ItemEntity> spawn(ServerPlayer owner, Vec3 pos)`.
Native table key is package-visible `TABLE` for meaningful native fixtures.
Native CHEST context has ORIGIN=pos, THIS_ENTITY=wearer, default luck0, matching
the source `/loot spawn`; no manual random or replacement pool. Every original
component and flag stays on the rolled stack. SetThrower(wearer) occurs BEFORE
entity admission, so existing LbcSpecialDrops retains the causal wearer rather
than pickup target. Current epoch is marked and existing freeze preparation and
shared native ENTITY_LOAD/drop admission are used. No new delayed timer, scan,
private loot selection or creeper explosion carrier exists. Release admission
checks MAG at the wearer; subsequent shared physical/magical trap rules remain.
Returns exactly admitted tokens, convenient for scoped cleanup/provenance tests.

Root integration needed: LbcItems field/getter `chaosTraps()`; build.gradle
explicit include `data/lbc/loot_table/traps.json`. This producer has no private
tick/stop/unload state. Merely adding these hooks is insufficient for complete
Chaos until the missing entries below are implemented.

## New actor implementation saved for root review

`LbcChaosTrapActors.java` contains exact original normal Bob, Aimbob, nine Evil
Lucky Block mobs and Reactive Shish entity payloads, mechanically extracted
from the original function text rather than approximated gear. Source native
DeathLootTable names and zero equipment drop chances are retained. Normal Bob
is not aliased to OLD_BOB. Runtime actor creation uses the existing shared
SourceKnockbackBalance and native TagValueInput load path.

Forest creates the original magical marker, stores its source800 active-tick
lifetime across unload in an owned age tag and uses the original global5tick
phase. Exact uniform1..12 mapping invokes native `place feature`; source index7
does BOTH spruce placement and Reactive Shish, while other11 indexes create a
baby zombie at+1,+1. Native spreadplayers follows each pulse, radius10 and
Nether under120. Feature/terrain writes use shared causal trap attribution.
Resident checks prevent synchronous loading; Time Stop holds the marker and
MAG removes its magical lifetime actor. Previously released native physical
mobs retain their original independent suppression behavior.

Root-facing API: `matches(ItemStack)`, `ready(ItemEntity)`, `spawn(ItemEntity)`;
also `spawn(ItemEntity,String flag)` permits exact interleaving with existing
source branches (forest16, Sauvojen17, evil24, Aimbob53, Bob59). No debit occurs
in this service. `loaded/unloaded/tick/stop`, `snapshot/trackedCount`,
`managed/owner/canAffect/canTarget`, `inheritConversion`, `inheritProjectile`
and `inheritDrop(parent,ItemEntity)` provide explicit lifecycle/combat hooks.
Native skeleton projectile owner stays unchanged; owned tags carry the wearer
through child/source unload. Native drop hook must run before child insertion
to retain provenance on transitive gadget rewards; no nearest-player inference.
No new getter is referenced by either new Java file itself, so neither depends
on a not-yet-added root API to compile. Root integration remains necessary.

Root owns the transitive Bob/Aimbob reward chains,20gear leaves each, End and
Baffled snowball gadgets; chaos_core owns the full Sauvojen boss. Root's hold
means none of these is claimed implemented or qualified by this actor producer.
The native death table IDs alone do not close their reward engines. Required
reward resources include `bob.json`, `aimbob.json`, `end_of_everything.json`,
`reactive_shish.json` -> `reactive_shish_item` and conditional
`end_of_technomagic`, and evil mobs' full `luckyblock.json` chain.

## Missing native closure, reported immediately

Source paths below are relative to the original `data/lbc/function/` root.

* Normal Bob is `drops/specialdrops.mcfunction:59`: raw bob:1, tag bob,
  DeathLootTable lbc:bob, six gear drop chances0. Existing BOB_OLD has bob_old:1,
  no normal Bob table and six chances.5. Aliasing these changes authored loot.
* Aimbob is `drops/specialdrops.mcfunction:53`: exact equipped skeleton,
  instant_damage20t amp100 (native undead healing), custom table lbc:aimbob,
  all equipment chances0. It has20 equal-weight gear outputs plus a separate
  End/Baffled event pool; follow [the existing full audit](AIMBOB_COMPASS_SOURCE_2026_10_27.md).
  That audit explicitly records missing event gadget engines and warns against
  deleting event pool/selecting only convenient reward pieces. A plain skeleton
  summon with empty/vanilla rewards would not close the dependency.
* Sauvojen Tuntija source `drops/sauvojen_tuntija.mcfunction`: ghast atY+20,
  100024HP, explosion power3, native source attributes/tags/loot/sound/smoke.
  `tick2smobs:3` invokes `other/sauvojen_tuntija/select` every40ticks with an
  eligible original#mobs target within110. Uniform1..10 emits nine persistent
  wand clouds or clears31cubed;35% ResistanceV20t; six source boundary teleports;
  below99900 integer HP invokes `drops/infinity`. That helper emits exact
  persistent Infinity reward, drops surrounding owned wand clouds' rewards,
  and removes the boss. Nine cloud engines and their attribution/target policies
  are required. Existing LbcSauvojen player-owned drone emit methods do not
  automatically implement hostile boss cloud targeting or source cloud lifetime.
  Existing LbcAcademy building admission does not recognize this raw flag.
* Forest `drops/forest`: magic marker lifetime800, rc_5t; `drops/forest_loop`
  uniform1..12 generates source features (including source index7 spruce AND
  Reactive Shish), other indexes ordinary baby zombie at+1,+1, then native
  spreadplayers radius10 (Nether under120). Needs source feature queue/causal
  terrain tracking and complete Reactive Shish actor/reward engine, not a tree
  approximation or a frozen inert marker.
* Evil Lucky Block `drops/evilluckyblock`: nine distinct native hostile mobs
  at source exact offsets, source head profile/invisibility/names/silence,
  original lbc:luckyblock reward. Opening a broad missing LuckyBlock reward
  pool as a side effect violates the bounded original task; its actual full
  reward closure must first be audited and implemented.

None of these six entries is silently removed, aliased, filtered after native
roll or relabeled ready. Root has been notified that full completion expands
beyond the previously assumed shared dispatcher coverage19/25.

## Required finite qualification

Do not publish successful table rolls as successful trap activations. First use
the real native table codec and scoped seeded/weighted boundary contexts to
exercise all25 indexes, ranked1 and non1 score states, source quality/luck math
and identical payloads. Then actually insert causal tokens and run shared native
dispatch and its source delay/terrain work for every entry, including all nine
boss clouds and complete rewards. Retain wearer/epoch through delayed and
unloaded tokens, pickup-target changes, merge, same-tick debit guards, Time Stop,
role/friendship and trap-specific suppression policies. Existing cage potion
producer spans up toY+2000, island uses31cubed clone with+31Ydestination, forest
is800active ticks; isolate fixtures using native owning service snapshots and
its full terrain queue drain. Do not merely count token disappearance.

No production diagnostic counters were added: source actor, block, token and
existing service snapshots give behavioral assertions; native table fixture can
inspect the actual entries. Qualification remains the root task's responsibility.

## Full Evil Lucky Block reward graph audit (author chose complete original pool)

Author decision supersedes the earlier hold: FULL original25-entry pool and full necessary transitive chain; no filteredpool. Source table stays byte-identical. New shared dispatcher now recognizes/pre-admits actor and boss flags and invokes them in original source order before one debit. This is implementation, not qualification.

Recursive graph of current native-resource `luckyblock.json` (all conditional edges retained): **333 tables, 3696 item entries, 506 distinct raw1 flags**. No missing referenced source table file. Gradle packaging is a textual include-pattern estimate, not a compiled artifact. Java literal search below excludes checks, and is evidence of candidates requiring a producer/flag audit, NOT a blanket declaration that every candidate lacks mechanics: typed-item aliases, generic metadata and vanilla components can provide behavior. Existing flags with a Java occurrence are likewise not automatically complete.

### Every referenced table and current include estimate

|Table ID|Matches current Gradle include pattern|
|---|---|
|lbc:aimbob|NO|
|lbc:alotofarrow|NO|
|lbc:anal_whip|yes|
|lbc:artifact|yes|
|lbc:attributefood|NO|
|lbc:badapple|NO|
|lbc:barriers|NO|
|lbc:barriersnottraps|NO|
|lbc:big_bob|NO|
|lbc:bob|NO|
|lbc:boba_eblan|NO|
|lbc:buckets|NO|
|lbc:clocks|NO|
|lbc:crocodildo|NO|
|lbc:cyberkatana|yes|
|lbc:cyborgboss|NO|
|lbc:death_loot|yes|
|lbc:diamondtools|NO|
|lbc:dildomet|yes|
|lbc:divider_by_metal_pipe|NO|
|lbc:drone|NO|
|lbc:drops/magic_bean|NO|
|lbc:electro_shield|yes|
|lbc:elytra|NO|
|lbc:end_of_everything|NO|
|lbc:end_of_technomagic|NO|
|lbc:enderpearls|NO|
|lbc:evoker_arrow|NO|
|lbc:evolution_sword|yes|
|lbc:explosivepotion|NO|
|lbc:feather_greed|NO|
|lbc:fevgalea_star|yes|
|lbc:fireballs|NO|
|lbc:food|NO|
|lbc:foodblocks|NO|
|lbc:fragmentsword|yes|
|lbc:gaycaster|yes|
|lbc:god_dice|NO|
|lbc:greed_greed|NO|
|lbc:halloween/lucky_items|NO|
|lbc:halloween/soulhunter_mace|yes|
|lbc:halloween/transformables_shovel|yes|
|lbc:halloween/transformables_staff|yes|
|lbc:halloween/transformables_sword|yes|
|lbc:hedgehog_wall|yes|
|lbc:helicopter_boss|yes|
|lbc:herman_antigravity|NO|
|lbc:hohlopitek|yes|
|lbc:ice_wand|yes|
|lbc:imba/alotofarrow|NO|
|lbc:imba/barriers_imba|NO|
|lbc:imba/barriersnottraps|NO|
|lbc:imba/bucket_rofl|NO|
|lbc:imba/buckets|NO|
|lbc:imba/diamond_area_block|NO|
|lbc:imba/diamondtools|NO|
|lbc:imba/elytra|NO|
|lbc:imba/enderpearls|NO|
|lbc:imba/fireballs|NO|
|lbc:imba/food|NO|
|lbc:imba/foodblocks|NO|
|lbc:imba/logs|NO|
|lbc:imba/lucky_items_imba|NO|
|lbc:imba/potions|NO|
|lbc:imba/slabs|NO|
|lbc:imba/spawnegg|NO|
|lbc:imba/spells|NO|
|lbc:imba/transformables_spells|NO|
|lbc:imba/traps|NO|
|lbc:imba/weapons|NO|
|lbc:imba/wool|NO|
|lbc:imitator|NO|
|lbc:inf_shulker|NO|
|lbc:inf_steak|NO|
|lbc:infinite_lucky_block|NO|
|lbc:infinite_totem|yes|
|lbc:infinity_no_shulker|NO|
|lbc:infinity_tuntija|NO|
|lbc:island_teleporter|NO|
|lbc:kiuaskivi_arrow|NO|
|lbc:knifes|NO|
|lbc:lightarrow|NO|
|lbc:lightning_bolt_egg|NO|
|lbc:logs|NO|
|lbc:loliluckyblock|NO|
|lbc:lucky_grenades|NO|
|lbc:lucky_items|NO|
|lbc:lucky_items_high_quality|NO|
|lbc:luckyblock|NO|
|lbc:luckyblock_imba|NO|
|lbc:luckyblock_loli_block|NO|
|lbc:luckyblock_neko_block|NO|
|lbc:luckyblock_without_rec|NO|
|lbc:lust_death|NO|
|lbc:madoka_bow|yes|
|lbc:malevich_rnd|yes|
|lbc:metal_pipe|yes|
|lbc:moisei_boots|yes|
|lbc:neko_set_assasin|yes|
|lbc:neko_set_assasin/boots|yes|
|lbc:neko_set_assasin/chestplate|yes|
|lbc:neko_set_assasin/helmet|yes|
|lbc:neko_set_assasin/leggings|yes|
|lbc:neko_set_assasin_pre|yes|
|lbc:neko_set_berserk|yes|
|lbc:neko_set_berserk/boots|yes|
|lbc:neko_set_berserk/chestplate|yes|
|lbc:neko_set_berserk/helmet|yes|
|lbc:neko_set_berserk/leggings|yes|
|lbc:neko_set_berserk_pre|yes|
|lbc:neko_set_chaos|yes|
|lbc:neko_set_chaos/boots|yes|
|lbc:neko_set_chaos/chestplate|yes|
|lbc:neko_set_chaos/helmet|yes|
|lbc:neko_set_chaos/leggings|yes|
|lbc:neko_set_chaos_pre|yes|
|lbc:neko_set_erosion|yes|
|lbc:neko_set_erosion/boots|yes|
|lbc:neko_set_erosion/chestplate|yes|
|lbc:neko_set_erosion/helmet|yes|
|lbc:neko_set_erosion/leggings|yes|
|lbc:neko_set_erosion_pre|NO|
|lbc:neko_set_femboy/boots|yes|
|lbc:neko_set_femboy/chestplate|yes|
|lbc:neko_set_femboy/helmet|yes|
|lbc:neko_set_femboy/leggings|yes|
|lbc:neko_set_flesh|yes|
|lbc:neko_set_flesh/boots|yes|
|lbc:neko_set_flesh/chestplate|yes|
|lbc:neko_set_flesh/helmet|yes|
|lbc:neko_set_flesh/leggings|yes|
|lbc:neko_set_flesh_pre|yes|
|lbc:neko_set_glass|yes|
|lbc:neko_set_glass/boots|yes|
|lbc:neko_set_glass/chestplate|yes|
|lbc:neko_set_glass/helmet|yes|
|lbc:neko_set_glass/leggings|yes|
|lbc:neko_set_glass_pre|yes|
|lbc:neko_set_mage|yes|
|lbc:neko_set_mage/boots|yes|
|lbc:neko_set_mage/chestplate|yes|
|lbc:neko_set_mage/helmet|yes|
|lbc:neko_set_mage/leggings|yes|
|lbc:neko_set_mage_pre|yes|
|lbc:neko_set_multiplying/boots|yes|
|lbc:neko_set_multiplying/chestplate|yes|
|lbc:neko_set_multiplying/helmet|yes|
|lbc:neko_set_multiplying/leggings|yes|
|lbc:neko_set_nanobots|yes|
|lbc:neko_set_nanobots/boots|yes|
|lbc:neko_set_nanobots/chestplate|yes|
|lbc:neko_set_nanobots/helmet|yes|
|lbc:neko_set_nanobots/leggings|yes|
|lbc:neko_set_nanobots_pre|yes|
|lbc:neko_set_nyauwu|yes|
|lbc:neko_set_overload/boots|yes|
|lbc:neko_set_overload/chestplate|yes|
|lbc:neko_set_overload/helmet|yes|
|lbc:neko_set_overload/leggings|yes|
|lbc:neko_set_paladin|yes|
|lbc:neko_set_paladin/boots|yes|
|lbc:neko_set_paladin/chestplate|yes|
|lbc:neko_set_paladin/helmet|yes|
|lbc:neko_set_paladin/leggings|yes|
|lbc:neko_set_paladin_pre|NO|
|lbc:neko_set_phantom|yes|
|lbc:neko_set_phantom/boots|yes|
|lbc:neko_set_phantom/chestplate|yes|
|lbc:neko_set_phantom/helmet|yes|
|lbc:neko_set_phantom/leggings|yes|
|lbc:neko_set_phantom_pre|yes|
|lbc:neko_set_randchanted/boots|yes|
|lbc:neko_set_randchanted/chestplate|yes|
|lbc:neko_set_randchanted/helmet|yes|
|lbc:neko_set_randchanted/leggings|yes|
|lbc:neko_set_shitty|yes|
|lbc:neko_set_shitty/boots|yes|
|lbc:neko_set_shitty/chestplate|yes|
|lbc:neko_set_shitty/helmet|yes|
|lbc:neko_set_shitty/leggings|yes|
|lbc:neko_set_shitty_pre|NO|
|lbc:neko_set_shulker|yes|
|lbc:neko_set_shulker/boots|yes|
|lbc:neko_set_shulker/chestplate|yes|
|lbc:neko_set_shulker/helmet|yes|
|lbc:neko_set_shulker/leggings|yes|
|lbc:neko_set_shulker_pre|yes|
|lbc:neko_set_steampunk|yes|
|lbc:neko_set_steampunk/boots|yes|
|lbc:neko_set_steampunk/boots_spectral|yes|
|lbc:neko_set_steampunk/chestplate|yes|
|lbc:neko_set_steampunk/helmet|yes|
|lbc:neko_set_steampunk/leggings|yes|
|lbc:neko_set_steampunk_pre|yes|
|lbc:neko_set_techno|yes|
|lbc:neko_set_techno/boots|yes|
|lbc:neko_set_techno/chestplate|yes|
|lbc:neko_set_techno/helmet|yes|
|lbc:neko_set_techno/leggings|yes|
|lbc:neko_set_techno_full|yes|
|lbc:neko_set_techno_pre|yes|
|lbc:neko_set_technomk2|yes|
|lbc:neko_set_technomk2/boots|yes|
|lbc:neko_set_technomk2/chestplate|yes|
|lbc:neko_set_technomk2/helmet|yes|
|lbc:neko_set_technomk2/leggings|yes|
|lbc:neko_set_traveller|yes|
|lbc:neko_set_traveller/boots|yes|
|lbc:neko_set_traveller/chestplate|yes|
|lbc:neko_set_traveller/helmet|yes|
|lbc:neko_set_traveller/leggings|yes|
|lbc:neko_set_traveller_pre|NO|
|lbc:nekolucky_items|NO|
|lbc:nexus_item_black|NO|
|lbc:nexus_item_green|NO|
|lbc:nexus_item_red|NO|
|lbc:nexus_item_white|NO|
|lbc:nexus_wire_black|NO|
|lbc:nexus_wire_blue|NO|
|lbc:nexus_wire_red|NO|
|lbc:nexus_wire_white|NO|
|lbc:nuke|NO|
|lbc:one_punch_glove|yes|
|lbc:penis_cultus|yes|
|lbc:ported/babylon_gate|yes|
|lbc:ported/barrier_arcanum|yes|
|lbc:ported/healing_arcanum|yes|
|lbc:ported/kiuaskivi|yes|
|lbc:ported/neutral_ball|yes|
|lbc:ported/sun_arcanum|yes|
|lbc:ported/tannerkivi|yes|
|lbc:ported/void_arcanum|yes|
|lbc:potions|NO|
|lbc:purifypotion|NO|
|lbc:purifypotion_1|yes|
|lbc:railgun|yes|
|lbc:random_glack|NO|
|lbc:random_glack_1|NO|
|lbc:random_item|NO|
|lbc:random_sauvojen_wand|yes|
|lbc:random_sauvojen_wand_tiny|yes|
|lbc:rasengan|NO|
|lbc:razorpine_with_leaves|yes|
|lbc:reactive_shish_item|NO|
|lbc:redstone|NO|
|lbc:rofls|NO|
|lbc:sauvojen_tuntija_air|yes|
|lbc:sauvojen_tuntija_ananta|yes|
|lbc:sauvojen_tuntija_blind|yes|
|lbc:sauvojen_tuntija_electro|yes|
|lbc:sauvojen_tuntija_fotja|yes|
|lbc:sauvojen_tuntija_swargia|yes|
|lbc:sauvojen_tuntija_teleport|yes|
|lbc:sauvojen_tuntija_terra|yes|
|lbc:sauvojen_tuntija_wand|yes|
|lbc:shadowglove|NO|
|lbc:shish_wall_wand|yes|
|lbc:shishgun|yes|
|lbc:shock_potion|yes|
|lbc:siggewinne_syringe|yes|
|lbc:slabs|NO|
|lbc:slr|yes|
|lbc:spawnegg|NO|
|lbc:spells|NO|
|lbc:spells_library|NO|
|lbc:stacid_wand|yes|
|lbc:stalin|yes|
|lbc:stand_arrow|yes|
|lbc:stews|NO|
|lbc:stopper_magic|NO|
|lbc:stopper_techno|NO|
|lbc:stoppers|NO|
|lbc:succubus_potion|yes|
|lbc:sussy_molten_penis|NO|
|lbc:swapper|NO|
|lbc:tnt_chestplate|yes|
|lbc:transformables_accessory|yes|
|lbc:transformables_accessory_all|yes|
|lbc:transformables_arcanum|yes|
|lbc:transformables_arcanum_all|yes|
|lbc:transformables_axe|yes|
|lbc:transformables_axe_all|yes|
|lbc:transformables_boots|yes|
|lbc:transformables_boots_all|yes|
|lbc:transformables_bow|yes|
|lbc:transformables_bow_all|yes|
|lbc:transformables_chestplate|yes|
|lbc:transformables_helmet|yes|
|lbc:transformables_hoe|yes|
|lbc:transformables_hoe_all|yes|
|lbc:transformables_leggings|yes|
|lbc:transformables_pickaxe|yes|
|lbc:transformables_potion|yes|
|lbc:transformables_shield|yes|
|lbc:transformables_shooter|yes|
|lbc:transformables_shooter_all|yes|
|lbc:transformables_shovel|yes|
|lbc:transformables_shovel_all|yes|
|lbc:transformables_spells|yes|
|lbc:transformables_staff|yes|
|lbc:transformables_staff_all|yes|
|lbc:transformables_summons|yes|
|lbc:transformables_summons_all|yes|
|lbc:transformables_sword|yes|
|lbc:transformables_sword_all|yes|
|lbc:transformables_totem|yes|
|lbc:transformables_trident|yes|
|lbc:traps|yes|
|lbc:triple_golden_sword|yes|
|lbc:troll_box|NO|
|lbc:tuntijaa_acid_acid|yes|
|lbc:tuntijaa_acid_corrupt|yes|
|lbc:tuntijaa_air_exasperoso|yes|
|lbc:tuntijaa_air_homing|yes|
|lbc:tuntijaa_blind_psycho|yes|
|lbc:tuntijaa_blind_vex|yes|
|lbc:tuntijaa_bolts_bolts|yes|
|lbc:tuntijaa_electro_electro|yes|
|lbc:tuntijaa_electro_shield|yes|
|lbc:tuntijaa_fire_blaze|yes|
|lbc:tuntijaa_greed_avarice|yes|
|lbc:tuntijaa_greed_destroy|yes|
|lbc:tuntijaa_stone_tanner|yes|
|lbc:tuntijaa_stone_wall|yes|
|lbc:tuntijaa_terra_awakened|yes|
|lbc:tuntijaa_terra_razor|yes|
|lbc:unstable_reactive|yes|
|lbc:voidman|yes|
|lbc:weapons|NO|
|lbc:witch_gens|NO|
|lbc:wool|NO|
|lbc:wtf_item|NO|
|lbc:yameteluckyblock|NO|

### Raw output flags absent as literals in nonfixture Java at audit time

|Raw flag|Exact bottom table and native item IDs|
|---|---|
|aimbobapple|lbc:aimbob -> minecraft:golden_apple|
|anal_whip|lbc:anal_whip -> minecraft:carrot_on_a_stick|
|anvils|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|armor_toughnessattribute|lbc:attributefood -> minecraft:cookie|
|armorattribute|lbc:attributefood -> minecraft:dried_kelp|
|arrows1|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|attack_damageattribute|lbc:attributefood -> minecraft:cooked_beef|
|attack_speedattribute|lbc:attributefood -> minecraft:sweet_berries|
|bad_apple|lbc:badapple -> minecraft:golden_apple|
|bad_apple_imba|lbc:imba/lucky_items_imba -> minecraft:repeating_command_block|
|baffled_of_everything|lbc:end_of_everything -> minecraft:snowball|
|bedrock_apple|lbc:imba/food -> minecraft:golden_apple|
|big_bob|lbc:big_bob -> minecraft:carrot_on_a_stick|
|blood_slab|lbc:imba/slabs -> minecraft:acacia_slab|
|blue|lbc:imba/wool -> minecraft:snowball|
|boba_eblan|lbc:boba_eblan -> minecraft:carrot_on_a_stick|
|brown|lbc:imba/wool -> minecraft:snowball|
|bucket|lbc:imba/bucket_rofl -> minecraft:snowball|
|bucks|lbc:imba/foodblocks -> minecraft:snowball|
|chaos_hoe|lbc:imba/diamondtools -> minecraft:netherite_pickaxe|
|chaos_pic|lbc:imba/diamondtools -> minecraft:netherite_hoe|
|chaos_sho|lbc:imba/diamondtools -> minecraft:netherite_shovel|
|clocks|lbc:clocks -> minecraft:carrot_on_a_stick|
|copypaster|lbc:imitator -> minecraft:repeating_command_block|
|crocodildo_boss|lbc:crocodildo -> minecraft:repeating_command_block|
|crowbar|lbc:loliluckyblock -> minecraft:iron_sword; lbc:transformables_sword_all -> minecraft:iron_sword|
|cum_slab|lbc:imba/slabs -> minecraft:birch_slab|
|cunny_slab|lbc:imba/slabs -> minecraft:carrot_on_a_stick|
|cyan|lbc:imba/wool -> minecraft:snowball|
|cyberset|lbc:random_sauvojen_wand_tiny -> minecraft:carrot_on_a_stick; lbc:sauvojen_tuntija_electro -> minecraft:carrot_on_a_stick; lbc:transformables_staff -> minecraft:carrot_on_a_stick|
|debriser|lbc:imba/lucky_items_imba -> minecraft:ancient_debris|
|diamond_apple|lbc:imba/food -> minecraft:golden_apple|
|diamond_beef|lbc:imba/food -> minecraft:golden_apple|
|diamond_berries|lbc:imba/food -> minecraft:golden_apple|
|diamond_carrot|lbc:imba/food -> minecraft:golden_apple|
|diamond_chicken|lbc:imba/food -> minecraft:golden_apple|
|diamond_fish|lbc:imba/food -> minecraft:golden_apple|
|diamond_mutton|lbc:imba/food -> minecraft:golden_apple|
|diamond_porkchop|lbc:imba/food -> minecraft:golden_apple|
|diamond_potato|lbc:imba/food -> minecraft:golden_apple|
|diamond_rabbit|lbc:imba/food -> minecraft:golden_apple|
|diamond_stew|lbc:imba/food -> minecraft:golden_apple|
|dirter|lbc:imba/lucky_items_imba -> minecraft:coarse_dirt|
|double|lbc:transformables_bow -> minecraft:bow|
|dragfireball|lbc:fireballs -> minecraft:snowball|
|dragfireball_imba|lbc:imba/fireballs -> minecraft:snowball|
|drone_pult|lbc:drone -> minecraft:carrot_on_a_stick|
|eat_big_explode|lbc:rofls -> minecraft:golden_apple|
|eat_book|lbc:rofls -> minecraft:golden_apple|
|eat_buffs|lbc:rofls -> minecraft:golden_apple|
|eat_chaos_explode|lbc:rofls -> minecraft:golden_apple|
|eat_cum|lbc:imba/buckets -> minecraft:salmon|
|eat_damage|lbc:rofls -> minecraft:golden_apple|
|eat_damage_all|lbc:rofls -> minecraft:golden_apple|
|eat_danmaku|lbc:rofls -> minecraft:golden_apple|
|eat_evoker|lbc:rofls -> minecraft:golden_apple|
|eat_explode|lbc:rofls -> minecraft:golden_apple|
|eat_fire|lbc:rofls -> minecraft:golden_apple|
|eat_glowing|lbc:rofls -> minecraft:golden_apple|
|eat_haste|lbc:rofls -> minecraft:golden_apple|
|eat_lightning|lbc:rofls -> minecraft:golden_apple|
|eat_poison|lbc:imba/buckets -> minecraft:salmon; lbc:rofls -> minecraft:golden_apple|
|eat_polymorph|lbc:rofls -> minecraft:golden_apple|
|eat_psycho|lbc:rofls -> minecraft:golden_apple|
|eat_regeneration|lbc:rofls -> minecraft:golden_apple|
|eat_slab|lbc:imba/slabs -> minecraft:oak_slab|
|eat_strength|lbc:rofls -> minecraft:golden_apple|
|eat_teleport|lbc:rofls -> minecraft:golden_apple|
|eat_teleport2|lbc:imba/enderpearls -> minecraft:chorus_fruit; lbc:imba/lucky_items_imba -> minecraft:chorus_fruit|
|eat_weakness|lbc:rofls -> minecraft:golden_apple|
|eat_wither|lbc:rofls -> minecraft:golden_apple|
|end_of_everything|lbc:end_of_everything -> minecraft:snowball|
|ender_pernul|lbc:imba/enderpearls -> minecraft:snowball|
|enderset|lbc:rofls -> minecraft:golden_apple; lbc:transformables_staff -> minecraft:carrot_on_a_stick|
|evolution|lbc:evolution_sword -> minecraft:golden_sword|
|fast|lbc:transformables_bow -> minecraft:crossbow|
|fastexplode|lbc:transformables_bow_all -> minecraft:crossbow|
|fastfired|lbc:transformables_bow_all -> minecraft:crossbow|
|fireball|lbc:fireballs -> minecraft:snowball|
|fireball_imba|lbc:imba/fireballs -> minecraft:snowball|
|gain_bucket|lbc:imba/buckets -> minecraft:salmon|
|gayradar|lbc:loliluckyblock -> minecraft:snowball|
|golden_beef|lbc:imba/food -> minecraft:golden_apple|
|golden_beetroot|lbc:imba/food -> minecraft:golden_apple|
|golden_berries|lbc:imba/food -> minecraft:golden_apple|
|golden_bread|lbc:imba/food -> minecraft:golden_apple|
|golden_cake|lbc:imba/food -> minecraft:golden_apple|
|golden_chicken|lbc:imba/food -> minecraft:golden_apple|
|golden_chorus|lbc:imba/food -> minecraft:golden_apple|
|golden_cookie|lbc:imba/food -> minecraft:golden_apple|
|golden_fish|lbc:imba/food -> minecraft:golden_apple|
|golden_kelp|lbc:imba/food -> minecraft:golden_apple|
|golden_melon|lbc:imba/food -> minecraft:golden_apple|
|golden_mutton|lbc:imba/food -> minecraft:golden_apple|
|golden_pie|lbc:imba/food -> minecraft:golden_apple|
|golden_porkchop|lbc:imba/food -> minecraft:golden_apple|
|golden_potato|lbc:imba/food -> minecraft:golden_apple|
|golden_rabbit|lbc:imba/food -> minecraft:golden_apple|
|golden_stew|lbc:imba/food -> minecraft:golden_apple|
|greedcurse|lbc:barriersnottraps -> minecraft:repeating_command_block; lbc:imba/barriersnottraps -> minecraft:repeating_command_block|
|grimoire_dungeon|lbc:barriersnottraps -> minecraft:repeating_command_block|
|gvozd|lbc:transformables_potion -> minecraft:lingering_potion|
|harpoon|lbc:transformables_trident -> minecraft:trident|
|heal1|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|honew|lbc:imba/foodblocks -> minecraft:snowball|
|human_eye|lbc:imba/food -> minecraft:golden_apple|
|human_flesh|lbc:imba/food -> minecraft:golden_apple|
|hylianshield|lbc:transformables_shield -> minecraft:shield|
|imba_fast_crossbow|lbc:imba/weapons -> minecraft:crossbow|
|imba_gatlingator|lbc:imba/weapons -> minecraft:crossbow|
|imba_shotgun|lbc:imba/weapons -> minecraft:crossbow|
|imba_sniper_crossbow|lbc:imba/weapons -> minecraft:crossbow|
|imba_super_shotgun|lbc:imba/weapons -> minecraft:crossbow|
|imbaxe|lbc:imba/diamondtools -> minecraft:netherite_axe|
|impulse|lbc:knifes -> minecraft:snowball|
|inf|lbc:inf_steak -> minecraft:cooked_beef|
|inf_shulker|lbc:inf_shulker -> minecraft:black_shulker_box|
|infinity_rand|lbc:infinity_tuntija -> minecraft:repeating_command_block|
|islandtp|lbc:island_teleporter -> minecraft:snowball|
|jack_o_lantern|lbc:halloween/lucky_items -> minecraft:snowball|
|kapusta|lbc:imba/foodblocks -> minecraft:snowball|
|kill_rand|lbc:loliluckyblock -> minecraft:snowball|
|knockback_resistanceattribute|lbc:attributefood -> minecraft:golden_carrot|
|krazha|lbc:shadowglove -> minecraft:carrot_on_a_stick|
|lava1|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|lava_bucket|lbc:imba/buckets -> minecraft:salmon|
|light_blue|lbc:imba/wool -> minecraft:snowball|
|light_gray|lbc:imba/wool -> minecraft:snowball|
|lightning_bolt|lbc:imba/lucky_items_imba -> minecraft:carrot_on_a_stick|
|lime|lbc:imba/wool -> minecraft:snowball|
|luckattribute|lbc:attributefood -> minecraft:glow_berries|
|luckyblockwand|lbc:infinite_lucky_block -> minecraft:carrot_on_a_stick|
|luckygrenade|lbc:lucky_grenades -> minecraft:snowball|
|luckyspawner|lbc:barriersnottraps -> minecraft:repeating_command_block; lbc:imba/barriersnottraps -> minecraft:repeating_command_block|
|madoka_bow|lbc:madoka_bow -> minecraft:bow|
|magenta|lbc:imba/wool -> minecraft:snowball|
|magicbean|lbc:drops/magic_bean -> minecraft:repeating_command_block; lbc:imba/barriersnottraps -> minecraft:repeating_command_block|
|max_healthattribute|lbc:attributefood -> minecraft:apple|
|most|lbc:lucky_items -> minecraft:snowball|
|most_imba|lbc:imba/lucky_items_imba -> minecraft:snowball|
|movement_speedattribute|lbc:attributefood -> minecraft:melon_slice|
|multiverse_fish|lbc:imba/food -> minecraft:golden_apple|
|netherite_apple|lbc:imba/food -> minecraft:golden_apple|
|netherite_carrot|lbc:imba/food -> minecraft:golden_apple|
|netherite_fish|lbc:imba/food -> minecraft:golden_apple|
|netherite_potato|lbc:imba/food -> minecraft:golden_apple|
|netherite_stew|lbc:imba/food -> minecraft:golden_apple|
|netherstar_fish|lbc:imba/food -> minecraft:golden_apple|
|nexus_compass|lbc:nexus_item_green -> minecraft:carrot_on_a_stick|
|nexus_wire_black|lbc:nexus_wire_black -> minecraft:repeating_command_block|
|nexus_wire_blue|lbc:nexus_wire_blue -> minecraft:repeating_command_block|
|nexus_wire_red|lbc:nexus_wire_red -> minecraft:repeating_command_block|
|nexus_wire_white|lbc:nexus_wire_white -> minecraft:repeating_command_block|
|night_vision|lbc:aimbob -> minecraft:iron_helmet|
|one_punch|lbc:one_punch_glove -> minecraft:iron_sword|
|op|lbc:transformables_potion -> minecraft:potion|
|orange|lbc:imba/wool -> minecraft:snowball|
|otchim|lbc:imba/spawnegg -> minecraft:snow_golem_spawn_egg; lbc:loliluckyblock -> minecraft:repeating_command_block|
|penisgun|lbc:dildomet -> minecraft:crossbow|
|pink|lbc:imba/wool -> minecraft:snowball|
|poleaxe|lbc:transformables_axe -> minecraft:netherite_axe|
|projegadget|lbc:imba/bucket_rofl -> minecraft:snowball; lbc:imba/foodblocks -> minecraft:snowball; lbc:imba/wool -> minecraft:snowball; lbc:lucky_items -> minecraft:snowball|
|pumpkin_king|lbc:halloween/transformables_sword -> minecraft:wooden_sword|
|purple|lbc:imba/wool -> minecraft:snowball|
|railgun_bow|lbc:railgun -> minecraft:bow|
|random_damage|lbc:sussy_molten_penis -> minecraft:poisonous_potato|
|random_health|lbc:sussy_molten_penis -> minecraft:dried_kelp|
|random_speed|lbc:sussy_molten_penis -> minecraft:golden_apple|
|randtp|lbc:swapper -> minecraft:snowball|
|rasengan|lbc:rasengan -> minecraft:snowball|
|ruchka|lbc:lucky_items -> minecraft:snowball|
|rune_ansuz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_fehu|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_gifu|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_hagalaz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_kenaz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_nautiz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_raido|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_thurisaz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_uruz|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|rune_wunjo|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick|
|sea_legend|lbc:transformables_trident -> minecraft:trident|
|shetka|lbc:loliluckyblock -> minecraft:carrot_on_a_stick|
|shish_gun|lbc:shishgun -> minecraft:bow|
|shish_of_everything|lbc:end_of_technomagic -> minecraft:snowball|
|shulker_new|lbc:transformables_bow -> minecraft:bow|
|silk|lbc:transformables_pickaxe -> minecraft:diamond_pickaxe|
|skeletons|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|slimeball|lbc:transformables_sword -> minecraft:slime_ball|
|smallfireball|lbc:fireballs -> minecraft:snowball|
|smallfireball_imba|lbc:imba/fireballs -> minecraft:snowball|
|snow_bucket|lbc:imba/buckets -> minecraft:salmon|
|spell|lbc:imba/transformables_spells -> minecraft:carrot_on_a_stick; lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|spellwithcooldown_n|lbc:nexus_item_black -> minecraft:carrot_on_a_stick; lbc:nexus_item_green -> minecraft:carrot_on_a_stick; lbc:nexus_item_white -> minecraft:carrot_on_a_stick|
|summonerblaze|lbc:transformables_summons -> skywars2:summoner_blazes|
|summonereliteskeletons|lbc:transformables_summons -> skywars2:summoner_elite_skeletons|
|summonerghast|lbc:transformables_summons -> skywars2:summoner_ghasts|
|summonerghostrider|lbc:transformables_summons -> skywars2:summoner_ghost_rider|
|summonerguardians|lbc:transformables_summons -> skywars2:summoner_guardians|
|summonerrazoritel|lbc:transformables_summons -> skywars2:summoner_ravager|
|summonerskeletons|lbc:transformables_summons -> skywars2:summoner_skeletons|
|summonerwand|lbc:siggewinne_syringe -> skywars2:siggewinne_syringe; lbc:transformables_summons -> skywars2:summoner_blazes; lbc:transformables_summons -> skywars2:summoner_elite_skeletons; lbc:transformables_summons -> skywars2:summoner_ghasts; lbc:transformables_summons -> skywars2:summoner_ghost_rider; lbc:transformables_summons -> skywars2:summoner_guardians; lbc:transformables_summons -> skywars2:summoner_ravager; lbc:transformables_summons -> skywars2:summoner_skeletons; lbc:transformables_summons -> skywars2:summoner_vexes_wand; lbc:tuntijaa_blind_vex -> skywars2:psychovex_wand|
|superdice|lbc:god_dice -> minecraft:snowball|
|supply_beacon|lbc:cyborgboss -> minecraft:panda_spawn_egg|
|techrunner_katana|lbc:transformables_sword_all -> minecraft:carrot_on_a_stick|
|teleport1|lbc:lucky_items -> minecraft:snowball|
|teleport1_imba|lbc:imba/lucky_items_imba -> minecraft:snowball|
|throwable_metal_pipe|lbc:imba/lucky_items_imba -> minecraft:snowball|
|tikva|lbc:imba/foodblocks -> minecraft:snowball|
|tnts|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|tomahawk|lbc:transformables_axe -> minecraft:golden_axe|
|transdimensional_fish|lbc:imba/food -> minecraft:golden_apple|
|trollge|lbc:troll_box -> minecraft:snowball|
|unresolved_artifact|lbc:artifact -> minecraft:repeating_command_block|
|unstable_spellbook|lbc:transformables_staff -> minecraft:carrot_on_a_stick|
|witch_gens|lbc:witch_gens -> minecraft:carrot_on_a_stick|
|wither1|lbc:transformables_spells -> minecraft:carrot_on_a_stick|
|wool|lbc:imba/wool -> minecraft:snowball|
|yellow|lbc:imba/wool -> minecraft:snowball|
|yellow_lightings_bottle|lbc:imba/food -> minecraft:golden_apple|
|zaika|lbc:loliluckyblock -> minecraft:carrot_on_a_stick|

Concrete event dependency: forest Reactive Shish reward `reactive_shish` alwaysrolls `reactive_shish_item` and independently5% `end_of_technomagic`. The latter emits native snowball `{gadget:1,shish_of_everything:1}`; source `projectiles/eggs:19` invokes `other/shishend/1`, then all11stages plus `randomize_position` and `risk_of_rain` subfolders. There is no existing Java literal consumer for that gadget flag. Root owns event engine work and full reward packaging.

Native raw rewards are not automatically converted by a generic loot-table rewrite: repository search found no LootTableEvents replacement pipeline. Existing typed native producer paths must be compared per output before claiming broad raw table behavior.

### Actionable consumer classification, not a literal-name missing-port count

Machine receipt: `build/verification/chaos-armor/trap-reward-flag-classification.json`.
All221 candidates have exact bottom table/item IDs, full live selector commands
and line numbers, consume-advancement triggers/reward functions and definition
resource hits. Generated TMT storage, lore, comments and ordinary names are
excluded from selector evidence. Counts193 actual legacy selector/consume
criterion candidates,19 known native-engine/generic metadata comparisons and9
without a direct raw1 selector/consume criterion. These are triage categories,
not193 verified missing engines or9 proven inert items. Source generic tags,
ENTITY_DATA and native item components matter separately.

|Bounded family|Actual legacy entrypoint|Current native equivalent / required comparison|
|---|---|---|
|Seven ordinary summoners|items/spells -> workingitems/spellsummoner and offhand variant|ALREADY raw-supported: JSON-loaded `LbcSummonerItems.kind` accepts data.getIntOr(k.flag()); `LbcWands` -> `LbcSummons`. Their absent Java string literals are not missing ports. `summonerwand` itself is category metadata.|
|Seven consumable raycast spell aliases tnts,skeletons,wither1,heal1,arrows1,lava1,anvils|workingitems/raycast_spells/select|Shared library spell effects exist, but raw output admission differs: `LbcLibrarySpells.kind` requires spellid plus libspell, while original transformables_spells outputs carry raycastspell plus alias. Reuse the correct shared spell engine and original one-item debit; do not merely repackage raw entries.|
|Teleport/island swap/Rasengan/dice/troll/rare snowball gadgets|projectiles/eggs:2,3,6,9,11,13,21,23,27-29|Native throw/router and each original payload required; direct literal and equivalent engine-name searches found no current Rasengan/superdice/troll/copier engine. Existing teleport/island geometry may be reused with original gadget-specific selectors, gates and causal owner. End/Baffled/Shish assigned root separately.|
|Normal and Imba fireball/projectile specials|projectiles/specials:12-18,27-28 and post-spawn ownership/motion lines|Native vanilla projectile entities already exist; raw snowball subtype routing and exact launch, ExplosionPower, Motion and owner policies are separate missing admission work. Existing wand projectile engines are reusable; no helper creeper detonation.|
|Bridge/building gadgets and12 colored wool variants|projectiles/gadgets/bridgebuilders, projectiles/gadgets/wools/select|Actual raw subtype selectors, terrain shapes, strict/air replacement and owned delayed terrain needed; color flags are meaningful in this router, unlike generic color/name occurrences.|
|Ten Imba runes|workingitems/raycast_spells/select:48-66 -> other/imba/runes/*_cast|Original consume/debit/cast/delayed modifier paths; no current raw rune consumer was found. Existing native damage/effect/attribute services can supply primitives.|
|Special edible flags eat_* / gain_bucket|Per-item consume_item advancement -> other/eatables/*|These are real consumption criteria and helpers, not merely names. Preserve native food/potion components plus scripted additions; existing KFC/CannedFood consumers do not constitute a generic eat_* dispatcher.|
|Eight attribute foods|Consume advancements carrying *_attribute flags -> source helper parameter/attribute operations|Native FOOD/CONSUMABLE alone does not establish the permanent attribute mutation. Shared attribute and effect-layer services exist; audit native maximum/operation/source modifier IDs and signed caps.|
|Copier|items_tick -> items_tick_tag/copypaster -> items_copypaster|Original dropped-copying behavior has a live source call; old Paper SwrgNativeService also registers lbc:items_copypaster. No current Fabric copier implementation was found by source/class search. Existing native stacks/merges can be reused, with original components and bounded proximity.|
|Special swords/bows/crossbows/tridents|items/swords, items/bows, items/crossbows, corresponding left/input consumers|Source aliases include anal_whip,evolution,ruchka,shulker_new,fastfired,fastexplode,sea_legend,harpoon,etc. Existing shared damage, armor and projectile engines do not automatically recognize every original alias. Compare each actual selector against the matching current service, not lore/name presence.|
|Witch generators/large boss and utility items|workingitems/spell1_default, spell1_default_left -> witch_gens,big_bob,clock_fragment and other helpers|Actual right/offhand source consumers exist. Native base mob/effect APIs are primitives; an original tagged boss or recipe output still needs its scripted tick/reward closure.|
|Nontrap drop aliases luckyspawner,magicbean,greedcurse,etc.|drops/specialdrops:26-27,45,63 plus their tick markers|Actual source dispatcher branches. Full Evil reward pool transitively reaches them even though initial25 trap pool does not. No catalog readiness claim follows from including their source tables.|
|Supply Beacon|cyborgboss ENTITY_DATA creates item_display with supply_drop,new,rc_1sec -> raycastselector and other/techno/supply_crate/*|Raw supply_beacon flag has no direct selector because native ENTITY_DATA tags start the behavior. Its scripted crate child is a real dependency shared with Shish risk_of_rain; assigned root event work, not an inert flag.|

Specific false-positive/inert exceptions: raw `spell` and `projegadget` are
category metadata, not individual effects. `gvozd` and `op` potion flags have no
function/advancement consumers; their actual potion contents are native source
components, so no invented flag engine should be added. `krazha` and `shetka`
have no direct raw selector or consume criterion in the audited LBC functions,
advancements, Paper Java or Ignite sources; preserve the source items without
inventing lore behavior. `zaika` is used by loot-table inventory conditions
instead of a function consumer, so its table gate matters. `drone_pult` appears
only in an unreferenced clear helper in the audited functions, which is not
evidence of a reachable right-click engine. `cunny_slab` is a subtype marker:
audit the general eat_slab source consumer and tag-based child cascade rather
than declaring its raw flag inert or inventing an independent activation.

No333-pool packaging or catalog enablement was performed. No build or server
was run. This classification bounds concrete next work while preserving every
original conditional table branch; it does not qualify the broad pool.

## Frozen implementation handoff and exact root hooks

Author release gate: preserve FULL original25-entry pool and release only after
the entire required transitive chain works. No filtering/retry/no-drop fallback.
Source `traps.json` stays unchanged. New producers plus shared dispatcher are
saved for root integration/qualification; full reward work stays staged.

1. `LbcItems.entityLoaded`: call `chaosTrapActors.loaded(entity)` beside the
   current creatureDrops/slabDrops notifications, before later early returns.
   This automatically captures a native projectile whose owner is a managed
   actor, adding causal wearer and current epoch while retaining the native
   skeleton owner. Pair entityUnloaded, tick and stop with the service methods.
   No player-forget removal is required for already released physical mobs.
2. `LbcIndirectKills.playerSource(Entity)`: query
   `game.items().chaosTrapActors().owner(source)` BEFORE its existing
   `if(source instanceof Projectile) source=projectile.getOwner()` unwrap.
   A native arrow's skeleton can be unloaded while its own persistent cause
   tags remain. Querying only after unwrap loses that arrow's retained cause.
   The same owner query also resolves direct Bob/evil/forest-child attacks.
3. `ZombieTntDropMixin` existing Mob hooks: add actor canTarget to both
   canAttack and setTarget admissions; add actor canAffect to doHurtTarget.
   Its existing convertTo/addFreshEntity WrapOperation must call
   actor inheritConversion(parent,converted) BEFORE the original insertion.
   Native equipment, AI, effects, drops and conversion behavior stay native.
4. `WandProjectileMixin` existing canHitEntity admission: include
   actor canAffect(projectile,target). It understands both native owner and
   persistent arrow tags. This runs before native Flame/other onHitEntity
   side effects, so a final hurt-only guard is insufficient. Also keep the
   shared damage guard in the root's source admission path for native hits
   that do not pass a projectile collision callback.
5. `CreatureDropIgniteMixin` existing Ignite.apply TAIL: include new
   LbcChaosTrapActors.managed(item.owner()) and its canAffect admission, then
   use existing indirectKills.ignited with friendlyFire=false, as for the
   ordinary hostile actors. This preserves Bob's native Fire Aspect tick
   credit after the native hurt cache expires. No second ignition mechanic.
6. Native Entity.spawnAtLocation overload which creates ItemEntity: wrap its
   `ServerLevel.addFreshEntity(child)` call and invoke
   actor inheritDrop((Entity)this,child) BEFORE insertion. This covers native
   death loot and gear without replacing roll/looting/equipment behavior.
   A RETURN injection is too late for ENTITY_LOAD/drop admission. Do not
   infer the author from the player nearest the reward or its eventual user.

`LbcSpecialDrops` already owns all new recognition/readiness/source-order
spawn calls and the single debit. It now persists the admitted thrower UUID in
an owned entity tag and a match-scoped author cache, so delayed drop unload and
wearer logout do not lose provenance or make unrelated casters merge. Its
stop clears the cache with pending/debit state. LbcDroneDrops was not edited.

Owned files frozen: LbcChaosTraps.java, LbcChaosTrapActors.java,
LbcSpecialDrops.java, this note and the classification JSON under
build/verification/chaos-armor. Root owns getters/lifecycle/damage/mixin hooks,
all event gadgets, broad reward packaging and final qualification. Exact source
Bob/Aimbob SNBT comparisons and all9 evil payload count passed; no compilation,
runtime, full reward or client presentation claim is made.

## Infinity producer and14 native instant branches (new bounded dependency)

New owned `LbcInfinity.java` only; no Items/SpecialDrops/resources edits in this
subtask. Read original `drops/infinity`, `other/instants/select`, `select2`,
`select_old`, `random_infinity`, inventory `sel:38-40,54`, `players_tick:15`,
`players_15s:4-6` and `swrg_kit_integration/game/skills/looper_explain_infinity`.

`produce(ServerLevel,Vec3,LivingEntity nullableAuthor)` creates the exact original
native ItemEntity payload: instantitem1/infinity_rand1, source model/tooltip,
entity name, no gravity, glowing, source Age/Health, itemteamblue and bluegays
Aqua team. Thrower and epoch precede insertion. This method creates the item
only; the boss's shared `infinity(level,at,nullableAuthor)` owns source sound,
smoke and all original nearby wand-cloud rewards, avoiding duplicate emissions.
`recognized/ready/spawn(ItemEntity)` handle raw infinity_spawn1/drop1 through
that same full helper; shared dispatcher owns debit. Ownerless tokens work via
the level+nullable-player overload. Source does not add a MAG or TECH gate.

`tick()` owns one300tick phase, epoch reset and duplicate-tick guard, and loops
ordinary admitted players. It skips Time Stop/ENDING and does not manually tick
native effects. `tickPlayer` recomputes actual instantitem presence in main36
inventory plus hands/armor. With #lbcskill swrg.math1 and lbc.skill2 it admits
infinity_rand ONLY in SelectedItem; other states use any native inventory slot.
The original global clear removes exactly ONE infinity_rand after the branch,
even if a different stack was selected for skill2 eligibility. Same-tick guard
prevents inventory callback re-entry. Looper skill3 emits one of14 literal
infinity_N explanation keys; title/subtitle and original advancement follow.

Exact uniform1..14 native outcomes:

|Branch|Native source mutation|
|---|---|
|1|Shared mana maximum100000000|
|2|Shared base mana regeneration100000000|
|3|Armor/toughness bases10000000; burning_time base-1000000|
|4|Attack damage base100000000|
|5|Attack speed10000000; block break speed214708364|
|6|Knockback and explosion knockback resistance bases10000000|
|7|Maximum health base214708364; no refill|
|8/9/10|Native tags infinite_resistance / infinite_heal / infinite_saturation|
|11|Exact literal source luckyblockwand stack, InfinityI, model lbc:lucky_block, tierU, source name/lore/flags; preserve overflow native drop|
|12|Fall multiplier-1; movement/water efficiency, step height, sneaking speed, safe fall, oxygen bases10000000|
|13|All19 original infinity_scale modifiers, source operations/amounts including range32 and falling-.9375|
|14|All4 original small modifiers; gravity deliberately shares infinity_scale ID with branch13|

All base values and permanent modifiers use native AttributeInstance behavior,
including vanilla ranges and duplicate modifier ID failure. Repeated13/14 does
not replace or compound existing IDs. No trait baseline, private HP ledger or
manual heal is added. Parent shared source reset owns eventual base/tag/modifier
cleanup; forget/stop here clear only attempt/phase state, preserving already
granted native traits/effects during logout or jammer transitions.

`pulse300` grants source INFINITE DURATION effects every300 active ticks:
Resistance amplifier4, InstantHealth amplifier99, Saturation amplifier50 via
ItemEffectLevels and ordinary native addEffect.300 is refresh cadence, not
duration. Existing native signed/layer/cap behavior stays authoritative.

Root hooks: add getter `infinity()` and exactly one `infinity.tick()` call from
normal Items active ticking; call forget(player)/stop() with shared lifecycle.
SpecialDrops recognize/pre-admit/spawn infinity_spawn at original source line15
before forest16 and Sauvojen17, retaining shared single debit. Boss uses the
same produce API. No new resource or catalog entry is needed for these methods;
the branch11 luckyblockwand's complete reward engine remains part of the full
release gate. Static checks passed14case coverage and exact19/4 modifier
attribute sets; no build/runtime qualification was performed.
