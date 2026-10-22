# Old Bob and old TNT Drone — source/items, qualification pending

Current status (22 October2026): both old leaves are qualified. See [port contract](BOB_DRONE_DROPS_PORT.md) for the implemented user policy, tested artifact and precise limits. Source-only/pending wording below records the initial audit.

22 October2026. Only item/resource preparation is complete; runtime/build and
qualification are root work. Read source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Read AGENTS/RESOURCE_PACK and current RP26.3(1); no raw-source/RP/template writes.

## Native token APIs

LbcCreatureDropItem.Kind.OLD_BOB appended: skywars2:old_bob_drop,
{bob_old:1,drop:1}; existing register/item/flag/matches APIs apply.
LbcDroneDropItem.ITEM = skywars2:old_tnt_drone_drop; register(),matches(stack),
{droppertnt_old:1,drop:1}. Both use plain native barrier proxy/model with no
custom name/lore/tier. Default model override events preserve Polymer projection.
Both match any nonempty sourceflag+drop1 stack, not only native item identity.
Public leaves lbc:ported/old_bob_drop and old_tnt_drone_drop each return1token.
S legacy_luckyblock:1159–1176 and2206–2223 respectively have count1 barrier,
empty set_components and exact flags; no authored presentation to invent.

## Old Bob exact payload

S function/drops/specialdrops.mcfunction:61: native adult zombie at fractional
item XYZ,visible name translate bob green/nonitalic,persistent1,IsBaby0,
follow_range400. No custom health/AI/damage/lifetime/tag/death table. Ordinary
native zombie loot remains. Six equipment drop chances each.5 independently.

- Netherite sword1: Sharpness5,Smite5,Bane5,Knockback2,FireAspect2,Looting3,
  Sweeping3,Unbreaking3; nonitalic bob_sword.
- Enchanted golden apple6; nonitalic bob_apple.
- Netherite boots1: Protection4,Fire4,Feather4,Blast4,Projectile4,Thorns3,
  DepthStrider3,FrostWalker2,Unbreaking3,SoulSpeed3; nonitalic bob_boots.
- Netherite leggings1/chestplate1: Protection4,Fire4,Blast4,Projectile4,Thorns3,
  Unbreaking3; nonitalic bob_leggings/bob_chestplate.
- Netherite helmet1: Protection4,Fire4,Blast4,Projectile4,Respiration3,
  AquaAffinity1,Thorns1,Unbreaking1; nonitalic bob_helmet.

Do not raise helmet's source1values, add Mending/newer enchants, tier/model/lore,
or conflate this with normal Bob's custom reward. Current RU/RPR existing names:
Боб;Меч Боба;Яблоко Боба;Ботинки Боба;Поножи Боба;Нагрудник Боба;Шлем Боба.
EN: Bob;Bob's sword;Bob's apple;Bob's boots;Bob's leggings;Bob's chestplate;
Bob's helmet. Keys exist in assets/minecraft/lang/{ru_ru,rpr,en_us}.json.

## Old Drone exact payload

S specialdrops:33–34: explosion particle at tokenY+10,delta(.3,8,.3),speed0,
count300,normal. Bat at tokenY+20 silent,name hidden,persistent,40HP/max40;
Invisibility10,duration2000000,hidden particles. Name nonitalic literal
`український безпілотник`. Passenger spawner_minecart tagged hohlospawner,
name hidden,same literal; passenger hoglin persistent30HP/max30,name visible,
same literal,immune to zombification,Weakness99,duration2000000,hidden particles.
OLD hoglin has no custom DeathLootTable (native hoglin loot).

Cart: SpawnCount1,SpawnRange0,Delay20,MinSpawnDelay20,MaxSpawnDelay20,
MaxNearbyEntities1000,RequiredPlayerRange50. SpawnData TNT fuse100,hidden name,
tag groundbam,literal nonitalic `Українська бiмба`; passenger pig visible same
name,35HP/max35,DeathLootTable empty. Source pig attribute id is max_health
without explicit namespace; native minecraft default namespace applies. Preserve
literal Latin i in бiмба versus Ukrainian characters elsewhere. No SpawnPotentials,
extra shooter/motion/TTL or invented persistent flag on TNT/pig is authored.

Required hooks: function/projectilestick_tnt:4 detects actual OnGround;
other/groundbam:1,7 sets fuse2 then removes groundbam. No evoker/fire/electro/
strong/no_explode tag is present to activate that function's conditional extras.
tick1s:12 kills hohlospawner when no ANY hoglin NOR zoglin within5, excluding
Nexus/Imprinted; this historical source scope is superseded by the user goal below.
Root owns native lineage/producer/pig TNT lifecycle implementation/qualification.
Normal drone custom hohlopitek reward remains outside this plain old leaf.

## Boundaries

Exact names/translations need no template additions. Root owns source-order
33–34 before potions and oldBob61 after other creatures, common unit debit,
actual thrower/roles, mob allies versus WILD TNT, native reflection/chain/landing,
FireAspect/fall/burning attribution, conversion/loot/AI, freeze/reload/epoch/stop.
Leaf JSON parsing/static review only; no build/server/commit performed here.

## Cart shutdown policy (user,22 October2026)

> Отключать после гибели своего хоглина

This is the implementation goal: producer shutdown follows its own hoglin's death,
identified by explicit lineage; unrelated nearby hoglins/zoglins must not keep it
alive. It supersedes historical broad-nearby source behavior. It is not a claim
that runtime implementation or qualification is already complete.
