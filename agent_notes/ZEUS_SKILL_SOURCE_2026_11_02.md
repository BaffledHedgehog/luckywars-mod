# Skill22 Blessing of Zeus — bounded source audit, 2 November 2026

Final author policy and native qualification supersede this historical implementation/source note: [completed port](ARMOR_ZEUS_SKILLS_PORT_2026_11_02.md). Do not repeat resolved questions.

Read-only audit. Only this new note written; no code, build, legacy server, RP or
template mutation. Current root/agent_notes AGENTS.md contracts apply; this note
does not reopen accepted attribution, protected-role, TimeStop or ally policies.
No claim of implementation/runtime qualification. TMT LAST.

## Complete source chain and timing

Legacy base `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Paths below relative to base.

1. `lbc/function/tick1s.mcfunction:1` reschedules itself after1s (20 game ticks).
   Line11 iterates minecraft:mobs outside Nexus/Imprinted into `mobs_1s`.
2. `lbc/function/mobs_1s.mcfunction:7` invokes `players_1s` for non-spectator players.
3. `lbc/function/players_1s.mcfunction:22`: global `#lbcskill swrg.math = 1` gate.
4. `lbc/function/swrg_kit_integration/game/skills/skills_tick_1s.mcfunction`: legacy
   mode returns failure, then skill22 plus `lbc:chance50` dispatches Zeus.
5. `lbc/predicate/chance50.json` is exact random_chance0.5.
6. Same skills directory `boltbolt_zeus.mcfunction` -> `boltbolt_zeus_rc.mcfunction`
   -> `boltbolt_zeus_hit.mcfunction` -> recursive `_rc` for another chain leg.

Thus one independent50% trial per20 scheduled ticks, not a fixed40-tick cooldown.
Mean interval40 ticks, but consecutive seconds or arbitrarily long gaps possible.
No mana debit, damage/stat scaling, held-item requirement, vanilla lightning
entity, electric debuff, fire, terrain blast, target-count cap or persistent
between-pulse chain state occurs anywhere in this chain.

Unlike mobs_tick's player path, shown mobs_1s path has no time_stopped tag guard.
That source omission does not override the modern shared TimeStop contract.

## Target/owner preparation

`boltbolt_zeus` calls `lbcr/function/dmgwithid_un.mcfunction`. That function visits
all minecraft:mobs and marks `nodamage` if their lbcID2 matches holder lbcID2,
`nodamage1` if their team_number matches holder team_number. It supplies a fallback
self nodamage tag if none exist. These are broad source/team exclusions, not WILD.
Then Zeus explicitly tags holder caster/nodamage and sets holder Distance0.

Initial ray origin is holder feet+(0,1,0), not exact native eye height. From that
origin it faces nearest minecraft:mobs within6, excluding spectator/nodamage/
nodamage1/strict_map_object, aiming at target eyes. `execute facing entity` fails
with no candidate, so no ray/sound/particle body is entered in that case.

After dispatch ends, it removes nodamage/nodamage1 from ALL minecraft:mobs and
caster from holder. These temporary global tags and global scratch scoreboard
are legacy implementation details; native service should use local visited/body
identity sets and normal shared ally/source exclusions.

## Ray math, collision and chain behavior

Every `_rc` invocation:

- increments holder Distance by1;
- emits one electric_spark, offsets(.1,.1,.1), speed1, count1, normal visibility;
- plays firework_rocket.twinkle MASTER, @a, volume.03, pitch2 at sample;
- if a non-spectator/nonvisited/nonteam minecraft:mobs overlaps the positive
  selector box dx=dy=dz=0 and distance<=5, invokes `_hit`;
- when current Distance<=25, advances exactly .25 local-forward and recurses
  only if the NEXT sample block belongs to minecraft:airs.

Without a hit,26 samples at distances0,.25,...,6.25. Initial sample is unchecked
for air; continuation tests block identity rather than collision shapes or fluids.
Plants, water and non-air blocks stop source rays. The selector box is not a
symmetric radius: dx=dy=dz=0 spans a unit positive AABB based on sample origin,
then normal entity bounding-box intersection applies. Distance<=5 is an extra
feet-distance filter, not actual5m splash damage.

`_hit` applies exactly4 native CACTUS damage, `by @a[tag=caster,limit=1]`, to ONE
matching entity (limit1, unsorted), excluding strict_map_object. It tags the same
selector nodamage regardless of whether native hurt succeeds. It sets Distance0,
faces nearest eligible within6 of CURRENT SAMPLE (not target feet/eyes), and
immediately runs another `_rc` at that same sample origin. Damage remains
original skill holder on every link. After nested work returns, lightning_bolt.impact
MASTER @a volume1 pitch2 plays at that hit sample.

Each successful body selection is attempted once per pulse because nodamage is
visited state; armor/Resistance/native hurt cooldown remain native. The recursive
chain has no per-link wait and can travel beyond holder radius6 through repeated
nearby victims. It is not periodic2s lightning on each afflicted target.

## Source defects to repair without inventing gameplay

- Detection omits strict_map_object, while damage/tag/next-target selectors exclude
  it. A protected overlapping body can trigger hit logic without any actual victim;
  with another valid target this can recursively retrigger from unchanged sample.
  Use identical protected-role admission for detection, damage and visited state.
- Nested hit resets the SAME holder Distance, then returns to outer ray whose
  continuation reads the mutated count. It can emit old-direction tails after a
  completed chain or miss; recursion depth also grows with eligible body count.
  Use bounded local ray/chain work, stop the replaced leg, and local visited state.
- Damage and visited-tag selectors are reevaluated separately; death callbacks or
  removals can change the unsorted limit1 result. Capture exact native victim body
  before damage, mark visited before callbacks, preserve original causal holder.
- Global nodamage/caster cleanup can affect unrelated legacy work. No native
  global tag mutation or shared scratch Distance is needed.

These are concrete implementation defects, not evidence for multi-hit damage,
WILD friendly fire or unlimited same-body retries.

## Suppression: only author decision not settled by existing shared policies

The complete source chain has NO MAG, TECH, strong-artifact/nomagic, caster-frozen,
target-anti-magic or travel-through-suppressor checks. Confirm whether22 should
remain entirely unjammed like its literal source, or follow a specified magical
suppression rule. Do not inherit LbcBolts jammer rules merely because its class
name suggests lightning. Existing TimeStop, protected roles, allies and causal
ownership policies already apply and need no renewed question.

The other observed defects above can be fixed as ordinary native safety/correctness
work while preserving the intended once-per-pulse chain; no new balancing decision
is required unless author explicitly wants pathological legacy tails retained.

## Existing Java reuse and limits

`LbcLoadouts.hasSkill(player,22)` supplies live match/unlock/role admission once
entry22 is exposed. `LbcLoadouts.tick` already has20-tick counter and tickSecond,
but tickSecond currently refreshes presentation flags; does not implement Zeus.
Use normal passive admission, not active item-use/mana/Frost-casting gates.

`ProjectileLifetimes.loadedTargets()` supplies cached resident living-body snapshot
with revision updates; per-cast radius/sphere/box queries can filter this without
new world scans, forced chunks or per-tick inventory/NBT work. Shared
`LbcWands.target` is ordinary self/ally/summon exclusion plus protected roles;
do not use its WILD chain-target alternative. Check resident air at each source
sample, and stop at unavailable chunks rather than generating/loading them.

Native damage should retain4 CACTUS and holder source with existing indirect-kill
receipt path. `LbcSpellSource` is available for captured source scopes/children;
this instantaneous holder-owned pulse does not require a fake projectile or
lightning actor. Avoid owner rebinding from a previous damage callback.

`LbcBolts` offers NO public/package Zeus-equivalent helper today:

- `libraryChainArea`/private Chain/tickChains are WILD,16-radius propagation,
  persistent current target sets,40-tick cadence and electro.lightning, which
  changes22 in targeting, damage, timing, lifecycle and presentation.
- `grimoire`, `emitAt`, `libraryDonor` produce other spell modes and effects.
- private `rayAt` uses150m caller ranges, geometric firstHit, .5-step visuals,
  suppressionFraction and caster/target MAG admission. Reusing it unchanged
  would alter exact source .25 sample/air-block collision and suppression.
- `target`/chainTarget/projectileTarget in LbcBolts also impose target MAG and
  nomagic checks. Neither is a neutral passive target selector.

Reuse shared resident targets, native damage attribution, role admission and
existing work/checkpoint conventions; add narrow Zeus behavior. Complexity medium
for bounded chain geometry; excessive populations require fair work budget and
state/checkpoint ownership if any pulse is deferred across ticks. Do not truncate
earned source chain solely to fit an arbitrary victim cap.

## Menu/presentation exact source

Original page10000 slot22, ID22, unlock
`lbc:true_advancements/legends/academy/boltbolt`; ranked variant bypasses achievement.
`lbc/loot_table/gui/page/10000/22.json`: carrot_on_a_stick, model lbc:zeus,
white italicfalse name lbcskill.22, two gray italicfalse lore lines
lbcskill.22.lore.1 and .2, then existing two gold/yellow statistics rows.
Source statistics erroneously reference index21 despite slot/ID22; note this
presentation bug rather than silently treating skill21 statistics as Zeus data.

Actual RP read only per agent_notes/RESOURCE_PACK.md:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json`:

- lbcskill.22: `Благословение Зевса`
- lbcskill.22.lore.1: `Мобы вокруг вас получают удары цепными`
- lbcskill.22.lore.2: `молниями каждые пару секунд.`

Existing keys/model suffice; no new RP assets or text needed for source design.
