# After Potion Spawner — next bounded source closures

22 October2026. Read-only source facts/recommendation; no implementation or
qualification. S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

Recommend OLD Island Lowering (`clone_old`) as the next finite closure; alternatively
OLD TNT Drone (`droppertnt_old`) is the next vanilla-reward producer closure.
They can be planned together if producer ownership/cleanup is ready. No full pool
is authorized by this note; normal drone and portal need additional work below.

## Old Island Lowering — complete finite native closure

S `function/drops/specialdrops.mcfunction:42` selects nearest any nonspectator
at token and runs drops/falled_old at that player's fractional position.
S `drops/falled_old.mcfunction:1–5` has four commands plus blank line:

- Clone source X/Z−8..+8,Y−12..0 to destination corner(−8,−25,−8), no optional
  clone mode specified (native replace/normal), so destination translates−13Y.
- Strict-air original17×13×17 volume (3757cells).
- Teleport NONPLAYER entities within9, except strict_map_object,−13Y each.
- Resistance2seconds,amplifier4,hidden particles on selected player executor.

No actor/reward/structure/custom ability table dependency. Player is NOT teleported
by line3, unlike normal Island Raise; preserve that explicit distinction and
native command failure followed by subsequent commands. Clone includes block
entities/native scheduled behavior; source support removal and destination
suffocation/water/lava consequences need causal attribution. Existing common
role guards must protect NPCs beyond the old strict_map_object exception.
Native clone limit/build bounds/chunk residency affect success independently
from later clear/teleport/resistance. Do not normalize it into an atomic move or
invent safe arrival clearing. At low Y, clone can fail before following commands.

S `loot_table/legacy_luckyblock.json:2771–2788`: barrier,count1,empty components,
{clone_old:1,drop:1}; no authored name/lore/tier/model. Normal clone is excluded
from a small patch because its31³=29791cells plus all-entity teleport is larger.
Current RP26.3(1) trap_clone:2600 RU `Ловушка: Поднятие острова`, EN
`Trap: Island Raise`, describes normal framing, not an old custom name; keep old
plain barrier. No source ambiguity established beyond explicit command outcomes.

## Old TNT Drone — producer closure with native rewards

S `function/drops/specialdrops.mcfunction:33–34`: particle explosion at tokenY+10,
delta(.3,8,.3),speed0,count300,normal; spawn bat at tokenY+20. Bat silent/invisible
amp10 duration2000000 hidden particles, persistent,40HP/max40. Its passenger is
spawner_minecart tagged hohlospawner, carrying persistent30HP/max30 hoglin,
immune to zombification, Weakness99 duration2000000 hidden particles. Source
literal actor names are Ukrainian text; preserve exact original text/NBT.

Cart spawner: SpawnCount1,SpawnRange0,Delay20,MinSpawnDelay20,MaxSpawnDelay20,
MaxNearbyEntities1000,RequiredPlayerRange50. It spawns native TNT fuse100 tagged
groundbam, each carrying a35HP/max35 pig, empty death loot. Native passengers,
bat flight, cart movement/activation, hoglin death/dismount, projectile physics,
TNT reflection/chain/explosion/terrain attribution and epoch/stop need full closure.
No invented TTL, owner-death cancellation or equivalent fireworks proxy.

Required non-native hooks:
S `function/projectilestick_tnt.mcfunction:4` calls other/groundbam on actual
OnGround; S `other/groundbam.mcfunction:1,7` sets fuse2 and removes groundbam.
Conditional evoker/fire/electro/strong/no_explode branches are absent on these TNT
presets, so do not invent those effects. This is landing acceleration of the
source100tick fuse, not a25tick approximation or unconditional fuse2.
S `function/tick1s.mcfunction:12` kills hohlospawner cart when NO hoglin NOR zoglin
within5, excluding Nexus and Imprinted dimensions. It uses any nearby such mob,
not an explicit passenger relationship. Inspect root's existing producer handle
policy or ask about that cross-instance survival scope before silently changing
it. Cleanup must not bypass full actor lineage on dismount/landing/death.

Old hoglin has NO custom DeathLootTable, hence native hoglin loot closes reward
side. S `loot_table/legacy_luckyblock.json:2206–2223` defines plain barrier,
count1,empty components,{droppertnt_old:1,drop:1}; no custom presentation.

Normal drone at specialdrops:31–32 differs by hoglin DeathLootTable lbc:hohlopitek.
That table grants custom carrot-stick {transformable:1,raycastspell:1,hohlopitek:1,
spellwithcooldown:1}; function/other/hohlopitek spawns paired pig TNT. No Java
hohlopitek flag implementation was found in this bounded scan. Therefore normal
must remain deferred until actual reward item ability/cooldown/raycast closure;
its normal traps490–533 presentation cannot justify a placeholder reward.

## Portal not selected

S `drops/portall.mcfunction` creates/spreads randomportal markers, bedrock and
paired gateways; depends on no_borders scoreboard and lbc:the_nether predicate.
Destination writes at marker use `execute at` while `data get entity @s Pos`
continues to read dropped item, and broad randomportal selectors span prior
markers. This is concrete selector semantics, not a standalone decorative portal.
Native gateway state/teleport safety and cross-instance marker scope require
separate audit/author judgment. Not promoted to fill batch size.

No source/RP/build/server writes or extra migrations were performed. Completed
creature/Slab/feedback/cage/drop leaves were not reselected. User source ordering,
one shared debit, attribution, roles and quiet cleanup still govern future work.
