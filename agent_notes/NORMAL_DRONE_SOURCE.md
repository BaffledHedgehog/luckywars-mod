# Normal TNT Drone, Hohlopitek and its spawn egg — SOURCE ONLY

23 October 2026. Bounded read-only source audit; no implementation, build, runtime
qualification, readiness or installation claim. OLD is
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Paths below are relative to OLD. Read current NEW AGENTS, existing drone source
and port contracts and current Java drone/egg entry points. No raw datapack, RP,
template, Java, server or preexisting user source was changed.

## Closed acquisition scope

The complete prospective group is normal `droppertnt` token, its native hoglin's
guaranteed `lbc:hohlopitek` reward with its working airstrike, and
`hohol_spawn_egg` (imba/spawnegg entry 11, zero based). Keep broad unready pools
gated. Existing `LbcDroneDropItem` and `LbcDroneDrops` accept only
`{droppertnt_old:1,drop:1}` and do not implement the normal reward. Java search
finds `coldownhohol` only in existing KFC counter enumeration; no ability handler.

Normal traps.json:491–533 emits count1 repeating_command_block, weight10,
quality-10, tier_ff/drop_boss, yellow nonitalic trap_hohol, white nonitalic
trap_desc, `{droppertnt:1,drop:1}`. imba/traps supplies a separate plain count1
repeating_command_block with weight0/quality-10 and added imba1. Preserve that
variant if exposed; do not silently replace its presentation with normal's.
random_glack_1 independently references hohlopitek with weight1, so public weapon
support must work independently of killing a drone. Existing transformable and
raycastspell classification remains relevant to shared item systems.

## Producer and reward

lbc/function/drops/specialdrops:31–32 is the normal pair; :33–34 is old. Their
sole actor payload difference is normal hoglin `DeathLootTable:"lbc:hohlopitek"`.
The exact recursive bat/cart/hoglin and TNT/pig payload, native spawner cadence,
particles, groundbam acceleration, native death/causal/lifecycle and user own-host
shutdown rule are already described in BOB_DRONE_DROPS_SOURCE.md and
BOB_DRONE_DROPS_PORT.md. Reuse that implementation, introducing a source-specific
normal payload rather than replacing old hoglin loot globally.

Bat40HP invisible10/persistent at tokenY+20 carries cart, which carries persistent
30HP zombification-immune Weakness99 hoglin. Cart count1/range0/delay20/min20/
max20/nearbycap1000/activation50 emits native fuse100 groundbam TNT with35HP pig,
empty pig death loot. Native OnGround changes fuse to2 once. No extra TTL.
The existing user rule closes a producer on its own hoglin's actual death;
foreign mobs, living dismount or chunk sleep cannot substitute for that identity.
Already emitted pigs/TNT remain independent. Native normal hoglin death invokes
the reward table (one entry/one roll, no chance condition), not only player kills;
quiet lifecycle discard must never synthesize that reward. Old native hoglin
loot and both variants' empty pig item loot remain distinct.

lbc/loot_table/hohlopitek.json produces one carrot_on_a_stick; entry weight10/
quality8 do not introduce a chance when it is the only entry. tier_b,
model lbc:hohlopitek, light_purple nonitalic hohlopitek and hohlopitek.lore,
`{transformable:1,raycastspell:1,hohlopitek:1,spellwithcooldown:1}`. No mana cost,
required mana maximum, ammunition, item debit or durability expense is authored.

## Actual airstrike source (namespace lbcr, not only lbc)

lbc/function/items/spells:5,11 -> workingitems/spell or spell_left ->
lbcr/function/raycast:19 or raycast_left:19 -> raycast_loophoholpre ->
raycast_loophohol -> raycast_hoholend. This source exists and must not be declared
missing because a search was limited to namespace lbc. Generic caster admission
rejects own nomagic_active and non-spectator antimagic players within20;
offhand also follows lefthand guard. Both rays require no stopper marker within20
and coldownhohol not positive. Main starts at eyes; offhand at eyes + localZ.5.
Common ray init sets Distance0 and raycaster tag, cleared after dispatch.

Each loop increments Distance, emits two porkchop item particles (delta .5 all
axes, speed0), tests an intersecting one-block `dx=dy=dz=0` mob selector constrained
also by distance15 and !raycaster/!spectator, then independently tests non-air.
The source minecraft:mobs tag includes players and villagers; integrate existing
protected service/observer roles rather than targeting everything in that tag.
Recursion advances localZ1 while Distance<=200, with no non-spectator antimagic
within16 of current sample and no stopper within20 of next sample. An empty ray
therefore samples offsets0..200 (201 samples); offhand .5..200.5. End sets
Distance400. No ray damage or explosion is authored; it selects the bomber target.
Range limit/suppression misses produce no bomber, but pre still sets cooldown200
and plays pig ambient/death/hurt, master @a, volume3/pitch1 at cast origin.

End creates marker at sample, spawns one150HP/max150 native hoglin at sampleY+20,
local lateral offset28 via yaw+90/pitch0; NoGravity1/NoAI1, immune zombification,
Invulnerable0, tags hohlopitek/lucky_mob, Instant Damage duration2000000,
default amplifier0, hidden particles. It faces the marker at feet, yaw only;
plays lbcsounds.bambam at sampleY+20, volume3/pitch1, kills marker and sets
Distance400. No name, custom death loot, persistence flag, lifetime or range kill
is supplied for this attack hoglin. It is distinct from the reward-bearing host.

lbc/function/mobs_5t:10 -> mobs_5t_lbc:1 -> other/hohlopitek, unless stopper20:
emit THREE native fuse100 groundbam TNT at localX+2,-2,0, each carrying a named
35HP/max35 pig with DeathLootTable empty; then teleport hoglin forward1 with its
existing yaw/pitch. TNT/pig literal names are the existing Ukrainian bomb names.
No extra TNT damage/radius/fire parameter is specified: preserve native TNT
explosion and shared configurable protection, WILD author/reflection/chain/fall/
terrain attribution, Time Stop, resident chunks and quiet epoch/stop cleanup.
Do not turn these three emissions into one blast, ray damage or a producer cart.
No finite count is authored: emission count follows the actor's actual native
life/effects. Native Instant Damage duration behavior must be checked against
the target Minecraft implementation before claiming a lifespan or total bombs.

## Cooldown/HUD and shared resets

lbc/function/players_5t:8 decrements coldownhohol by1 per5 ticks: source200 is
1000 active ticks/about50 seconds, not200 ticks. ui/cooldown:14–15 displays raw
score with U+E105 while positive and U+E104/green ready otherwise, held either
hand. KFC all_up_scores_no_mana:53 adds1 raw unit; purify:16 and resets:22 set0.
Vanity saves/restores and Envy compares this original score. No hohlopitek mana
configuration is present in these source paths; avoid inventing a mana setting
or normalizing KFC/HUD units. Shared role/magic/time guards still apply.

## Egg entry 11

lbc/loot_table/imba/spawnegg.json:991–1040 is a native bat_spawn_egg with tier_f,
red nonitalic hohol_spawn_egg, dark_purple nonitalic imba_version and imba1.
ENTITY_DATA id minecraft:item, PickupDelay32767, Item count1 repeating_command_block,
custom_data string `{droppertnt: 1,drop: 1}`, item_model lbc:drop_boss. This is
the NORMAL drone, not old. Native placement yields its unpickable token and the
existing drop dispatcher must debit/emit it once. Reuse existing special-egg
native positioning/input/ENTITY_DATA/causal/lifecycle and matching-mob policies;
do not add a second independent direct drone spawn or invent offspring support
for a payload whose actual entity type is item. Egg has no count override.

## Author decisions and implementation recommendations

1. Entity and block end calls are independent. An entity intersecting a non-air
   sample calls end twice, spawning two bombers despite Distance400. Existing
   AGENTS:2376 user rule already requires exactly one action on the first hit;
   apply that accepted correction, without asking the user to decide again.
2. End's selector uses global unsorted limit1 unactivated hoglin and global
   limit1 marker, then kills ALL hohlopitek_mark markers. Concurrent effects can
   face/activate/delete another cast's objects. Recommend bind new actor to its
   own target directly; this is a source identity bug, not intended extra power.
3. Local target26.3 sources (`.gradle/loom-cache/minecraftMaven/net/minecraft/
   minecraft-merged-d6f9bb6878/26.3/*-sources.jar`) resolve Instant Damage:
   InstantaneousMobEffect checks remainingDuration>=1, HealOrHarmMobEffect
   attempts6magic damage each tick on a normal hoglin, using native hurtServer.
   Native hurt cooldown and other real damage govern actual death. Preserve this
   effect/native life; no invented permanent actor or arbitrary TTL/cap replaces
   it. Exact total flight/bomb count still requires runtime qualification.
4. Existing own-host producer shutdown is already explicitly decided by user;
   retain it for normal, and retain WILD drone TNT role protection/author rules.
   Attack hoglin melee is disabled by NoAI, but must still use protected roles
   and native death/loot semantics. Its ordinary native loot is not the staff.
5. Complete prospective qualification needs actual host reward -> held weapon ->
   real target/flight/drop/landing/explosion, miss cooldown/hand/HUD/KFC/reset,
   source double-end policy, egg token flow, own-host death, native child authors,
   protected roles, freeze/resume, unload callbacks and actual quiet stop. Static
   JSON/resource presence alone cannot qualify this group or broad acquisition pools.

Existing AGENTS:1948–1953 requires wand activation on PKM/right click only; the
offhand source path means using the offhand with right click, never a new LKM/
Punch trigger. Source RC_Raycast admission is players_tick:19 -> items/spells;
shared right-click guards should be reused. No new user question is necessary
for cooldown, zero mana cost, own-host shutdown or single terminal behavior.
Optional range preference can be discussed if desired, but source200 is a clear
default and no existing Shish-specific unlimited-ray decision should silently
rewrite this weapon's authored range. Attack hoglin's source five-tick emission
is stopper-suppressed, not antimagic-suppressed after launch; keep shared time/
mechanism policy and do not retroactively cancel launched ordnance without an
applicable user rule.

## Accepted implementation decisions (23 October 2026)

The author's subsequent answers override the historical source200/aura defaults:
use no fixed range, while chunks are loaded; this is wholly technological and
only the mechanism suppressor blocks it. No antimagic aura check is retained.
[Implementation and runtime qualification](NORMAL_DRONE_PORT.md) records the
closed reward/bomber/egg group and its actual verification limits.
