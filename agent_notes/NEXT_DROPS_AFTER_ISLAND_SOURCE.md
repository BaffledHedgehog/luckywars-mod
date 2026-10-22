# After Island drops — next closed source candidates

22 October2026. Source-only recommendation, not implementation or qualification.
S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Current island work and completed leaves are excluded. No build/server/source/RP
writes. This note recommends OLD Bob as the next small finite native closure,
with OLD TNT Drone as a larger producer companion if lifecycle scope fits.

## Old Bob — closed vanilla equipment and ordinary zombie loot

S `function/drops/specialdrops.mcfunction:61`: a single native adult zombie at
fractional token XYZ; CustomNameVisible1,PersistenceRequired1,IsBaby0;
custom name translate bob,green,italic:false. Only authored actor attribute is
follow_range400. Native health, attack damage, movement/AI and zombie loot remain;
no custom tags, DeathLootTable, recurring marker, custom item flag or actor
lifetime is authored. All SIX equipment slots have independent drop_chance.5.

| Slot | Native item/count | Enchantments |
| --- | --- | --- |
|mainhand |netherite_sword1 |sharpness5,smite5,bane_of_arthropods5,knockback2,fire_aspect2,looting3,sweeping_edge3,unbreaking3 |
|offhand |enchanted_golden_apple6 |none |
|feet |netherite_boots1 |protection4,fire_protection4,feather_falling4,blast_protection4,projectile_protection4,thorns3,depth_strider3,frost_walker2,unbreaking3,soul_speed3 |
|legs |netherite_leggings1 |protection4,fire_protection4,blast_protection4,projectile_protection4,thorns3,unbreaking3 |
|chest |netherite_chestplate1 |same authored map as legs |
|head |netherite_helmet1 |protection4,fire_protection4,blast_protection4,projectile_protection4,respiration3,aqua_affinity1,thorns1,unbreaking1 |

Every equipment name is italic:false, translated respectively bob_sword,
bob_apple,bob_boots,bob_leggings,bob_chestplate,bob_helmet; NO name color, lore,
tier, custom_data or item model is authored for gear. Do not normalize helmet's
lower1levels or add modern extra enchantments/Mending. Existing user-approved
native no-conflict enchantment handling applies without changing source levels.

S `loot_table/legacy_luckyblock.json:1159–1176`: plain barrier,count1,
empty set_components,{bob_old:1,drop:1}; no token name/lore/model/tier.
A bounded search found no bob_old recurring source hook or Java dispatch branch.
This is a distinct old acquisition, not completion of normal bob's reward tree.

Current read-only RP26.3(1) `assets/minecraft/lang/{ru_ru,rpr,en_us}.json` already
contains all names: RU/RPR Боб,Меч Боба,Яблоко Боба,Ботинки Боба,Поножи Боба,
Нагрудник Боба,Шлем Боба; EN Bob,Bob's sword,Bob's apple,Bob's boots,Bob's leggings,
Bob's chestplate,Bob's helmet. Preserve translation keys/style and literal values.
No template/translation change needed for the plain old token/native equipment.

Meaningful qualification: actual source-native adult/gear/AI, all six death drop
chances and native looting behavior, ordinary zombie drops, native combat/Fire
Aspect/knockback and indirect fall/burning attribution, role/allied protection,
newer attacker priority, conversion to Drowned with copied cause/epoch but native
conversion equipment/loot rules, freeze/unload/epoch/quiet stop. Do not add a
custom boss HP/attack pattern merely because equipment is strong.

## Old TNT Drone — closed rewards, real producer lifecycle

S specialdrops:33–34 creates explosion feedback then batY+20 carrying tagged
spawner_minecart carrying persistent30HP weak99 hoglin. Minecart produces
fuse100 groundbam TNT carrying35HP pig with empty death loot. OLD hoglin has
ordinary vanilla hoglin loot, no custom DeathLootTable. Source token plain barrier
at legacy_luckyblock:2206–2223,{droppertnt_old:1,drop:1}.

Required additional hooks: projectilestick_tnt:4 -> other/groundbam:1,7 sets
fuse2 on real OnGround then removes tag. tick1s:12 kills hohlospawner cart only
when no ANY hoglin nor zoglin within5, excluding Nexus/Imprinted; this broad
cross-instance scope needs explicit decision/author judgment before replacing
it with only-own-passenger logic. See [prior exact producer audit](NEXT_DROPS_AFTER_POTION_SOURCE.md).
No infinite generator workaround, invented projectile motion or new TTL.

Normal drone remains blocked by lbc:hohlopitek custom reward ability/cooldown/
raycast closure. Portal remains unaudited gateway/spread/marker cross-instance
scope. Normal Bob should not be promoted without independent full lbc:bob reward
closure. Infinity/forest/LuckyRoom/magicbean/Vexsword stay deferred. No reward
placeholder or already completed acquisition was chosen to enlarge the batch.
