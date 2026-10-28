# Sauvojen Tuntija boss dependency audit — 28 October 2026

Source-only dependency findings for the original Chaos `lbc:traps` pool. This
note itself implements no boss, Infinity consumer, shared spell refactor or
acquisition route, and makes no runtime qualification claim. After this audit's
initial hold, the author answered: retain the FULL ORIGINAL POOL and release
only after its entire dependency chain is complete. Boss and shared spell source
work consequently resumed in Java; those changes still need complete native
qualification. No restricted pool is authorized. The two ranked/conditional Sauvojen entries in the
original 25-entry table remain separate source entries.

Read-only legacy roots:

- Datapack: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
- Paper: `E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`.
- Native mod: `src/main/java/me/skywars2/game/` in the current workspace.

This audit follows [agent_notes rules](AGENTS.md), including mandatory wearer
credit for direct and delayed consequences. Source absence of ownership does
not authorize removing causal attribution during the port. Author credit must
also not turn a hostile boss into an allied player summon.

## Actor and live source entry

`lbc/function/drops/sauvojen_tuntija.mcfunction` emits the wither-spawn sound
(volume1, pitch1, minimum-volume.5), 1000 cosy campfire smoke particles at Y+10
(spread1,10,1), and a native ghast at Y+20. Its exact actor fields are:

- `DeathLootTable:"lbc:sauvojen_tuntija"`, `PersistenceRequired:1b`.
- `Health:100024.0f`, `minecraft:max_health` base100024.
- `ExplosionPower:3`, `minecraft:follow_range` base256,
  `minecraft:knockback_resistance` base256.
- Tags `sauvojen_tuntija` and `rcdenyalways`; literal blue, bold, nonitalic
  name `Sauvojen Tuntija`. Source does not set CustomNameVisible.

Paper `runNativeSauvojenTuntijaDrop`, lines5027–5076, recognizes the raw
`sauvojen_tuntija:1` drop, spawns the corresponding ghast, preserves those
attributes/persistence/loot/name fields, and consumes one token. This method is
spawn evidence; it is not evidence that the new Fabric mod has a complete boss.

`lbc/function/tick2smobs.mcfunction:3` invokes
`other/sauvojen_tuntija/select` for such a ghast when another eligible original
`#minecraft:mobs` actor is within110, excluding `spectator`, `rcdenyalways` and
`friendly_tuntija`. This is the source two-second/40-tick selector cadence.
Full runtime routing, including any legacy `bossselector` integration in the
other original execution layers, must be traced before qualification; a
`bossselector` filename was not located by this bounded file search.

## Ten-way selection and stationary emitters

`lbc/function/other/sauvojen_tuntija/select.mcfunction` uniformly chooses1..10.
Branches1..9 create stationary item displays for Ruby, Terra, Electro, Air,
Acid, Teleport, Stone, Fire and Blind, in that order. Their tags include
`savoujen_cloud` (original spelling) and `magic`, plus their individual
`sauvojen_KIND` and cadence tags. They use `lbc:st/KIND`, the exact source
2.5-scale tilted transformation, fixed display context and interpolation/teleport
duration1. These are not the player's flying-wand drones: source boss emitters
do not perform the player drone's retreating movement or Academy mode cycling.

Branch10 replaces the complete inclusive cube from(-15,-15,-15) to(15,15,15)
with air using strict native fill: 31³ cells, subject to native command limits.
It emits 1000 cosy campfire smoke particles with spread10,10,10. It is not a
spherical corrosion template or a generic explosion.

Every selection emits forced portal particles at Y+2, spread1,1,1, speed1,
count50. A source35-percent roll gives the ghast ResistanceV for20ticks, hidden.
The same function wraps its coordinates in source order: Y>=300 toY40; Y<=0
toY200; X>=120 toX=-100; X<=-120 toX=100; Z>=120 toZ=-100; Z<=-120 toZ=100.
Native selector box endpoint details need direct qualification at the bounds.

Nearest-target facing excludes `spectator`, `rcdenyalways`, `friendly_tuntija`
and `strict_map_object`; it does not exclude the Chaos wearer or that wearer's
allies. Do not substitute the ordinary player-owned wand target predicate.

## Nine asynchronous payloads

Producer files are `lbc/function/raycast/sauvojen_KIND.mcfunction`; their
payloads route through `lbc/function/raycastselector_tuntija.mcfunction` to
`lbcr/function/raycast_*.mcfunction`. The `data.lifetime` column below is the
new child charge's source lifetime, distinct from its emitter's `Lifetime`.

| Kind | Emitter cadence | Child source fields and behavior | Native reuse mismatch found |
| --- | --- | --- | --- |
| Ruby |60ticks (`area_3s`) |One `raycastruby` slow ray; lifetime60. Seven source samples spaced.3, purple dust, destructive air cube±.6 at each sample; endpoint displacement1.8 per tick. |`LbcRuby.flying` sets sourceLifetime50. Existing dig-cube engine is reusable with explicit lifetime60 and hostile terrain cause. |
| Terra |20ticks (`area_1s`,`rc_1sec`) |One `raycastterratuntija` slow ray; lifetime60. Seven samples spaced.3; each applies7 CACTUS to at most2 original#mobs in the source zero-size selector box, excluding Sauvojen and summoned cultists. Solid contact uses `lbcr:placegrass`. |`LbcTerra.flying` uses lifetime200 and regular damage8. Reusing it unchanged is incorrect; source target sampling, damage7 and lifetime60 need a native variant. |
| Electro |20ticks (`area_1s`,`rc_1sec`) |Four `raycastelectro` slow rays, each lifetime20. Nineteen samples spaced.2; .5 GENERIC damage per source sample, excluding source protected entity tags; shared deflection/conduction helpers. |`libraryFlyingElectro` lifetimes35,20,25,30 differ. `mechanismElectro` defaults30 and mechanism policy; source children carry magic/shield tags. A distinct producer must preserve four20-tick source rays while sharing the electrical engine. |
| Air |60ticks (`area_3s`) |One `raycastair` slow ray, lifetime120. Five samples spaced.2; original#mobs/block collision summons exactly3 TNT with native short fuse1, then kills the ray. |`LbcAir.flying` has lifetime120/flying trail but currently detonates three shared immediate protected blasts. Exact delayed native TNT semantics and hostile cause require review. |
| Acid |20ticks (`area_1s`,`rc_1sec`) |One `raycastacid2` slow ray, lifetime300, shield_blockable2; original shared acid collision, damage, fire reactions and in-air helpers. |`LbcClouds.sourceAcid(level,origin,yaw,pitch,300,true,author)` already exposes source300-tick acid, native owner empty and separate causal author. Ordinary `flyingWandAcid` uses a player-owner filter and is not an equivalent substitute. |
| Teleport |80ticks (`area_4s`) |One `raycastteleport` slow ray, lifetime60. Five samples spaced.3; applies Levitation41 for20ticks to eligible original#mobs at each source box; nonair kills the ray; does not perform the ordinary player teleport effect. |`LbcTeleport.flying` exposes lifetime60/flyingStyle; source target and hostile-cause policy still need review. |
| Stone |60ticks (`area_3s`) |One `raycaststone` slow ray, lifetime80. Five samples spaced.4 and stone block-marker particles; eligible mob/block contact fills a strict stone cube±1.5, plays stone.place and kills the ray. |`LbcStone.flying` exposes lifetime80 and this cube branch. Its firstHit and terrain ownership remain player-policy dependent. |
| Fire |5ticks (`rc_5t`) |Native small fireball aimed at nearest eligible actor; explicit Motion=direction*.7, `can_deleted`; blaze.shoot volume1/pitch1. No custom charge lifetime is encoded here. |Ordinary `LbcSauvojen.emit` launches direction*2.5 with firecharge.use. Reuse native fire mechanics with explicit source velocity/sound and hostile ownership policy. |
| Blind |10ticks (`rc_10t`) |One `raycastblind` slow ray, lifetime120, shield_blockable; four samples spaced.33; source targets receive BlindnessI80ticks and WitherII80ticks; nonair kills the charge. |Ordinary flying Sauvojen calls `LbcBlind.spawn` with lifetime100. Explicit120 and hostile target/cause policy are necessary. |

Cadence entry files:

- `raycasters_1s.mcfunction:1,15` increments emitter Lifetime20 and selects the
  one-second areas with the original nearest-target predicate.
- `tick3s.mcfunction:7` and `raycast/select3s.mcfunction` select three-second
  areas and increment Lifetime60.
- `tick4s.mcfunction:2` and `raycast/select4s.mcfunction` select four-second
  areas and increment Lifetime80.
- `raycasters_5t.mcfunction:1,7` increments Lifetime5 and handles Fire.
- `area_effect_cloud_10t.mcfunction:1,3` increments Lifetime10 and handles Blind.

Each individual `raycast/sauvojen_KIND` producer destroys its emitter after
firing when emitter Lifetime>=200. Therefore three-second emitters can reach240
and four-second emitters240 before that helper's destruction check; replacing
this with an unconditional per-tick death at200 changes source output counts.
`raycast/destroy.mcfunction` plays enchantment_table.use volume2/pitch1.3 and
emits30 witch particles (spread.1, speed.5), then kills the emitter. It does not
drop a wand on normal lifetime expiry.

All quantities above describe inspected source fields and commands, not measured
native performance, accepted runtime collisions or proof of an implemented boss.

Electro's four charges have identical initial aim and no initial spread. Source
`lbcr/function/raycast_electro.mcfunction` samples positions0,.2,...,3.6 (nineteen
positions); `molnia_prelomlenie_pre.mcfunction` resets Distance and calls
`lbc/function/universal_razbros_small.mcfunction`. That helper independently
adds integer uniform[-800,800] to the yaw/pitch hundredths, then writes back at
scale.01: ±8degrees per axis after each tick's segment. The existing native
electrical engine's sample counter18 already describes those nineteen inclusive
positions; its ordinary player counter10 does not. At nonair contact,
`raycast_electro42.mcfunction` emits5 end rods at local forward-.5, spread.1,
speed.1, then conducts if the block is in the exact `minecraft:electroprovod`
tag; otherwise it kills the charge. Conduction uses original#mobs radius10,
excludes spectator/rcdenyalways, emits4 firework particles and1 GENERIC damage
except `techno_set_bonus_active`. Any established author rule about released
electrical arcs supersedes a literal inference from source magic tags; the new
variant must explicitly choose the shared interception rule, not accidentally
inherit its policy from an unrelated producer constructor.

## Infinity threshold, death loot and cloud rewards

The end of `other/sauvojen_tuntija/select.mcfunction` stores Health as an integer
score (`data get ... Health 1`) and invokes `drops/infinity` when that score
matches `..99900`. This includes equality and integer truncation: do not replace
it with an unqualified floating-point `health<99900` check. The threshold runs
at an eligible selector event, not an independent every-tick health listener.

`lbc/function/drops/infinity.mcfunction` creates a special repeating-command-
block item with `instantitem:1,infinity_rand:1`, model `lbc:infinity`, tooltip
`lbc:tier_x`; item entity Age=-32768, NoGravity, Glowing, visible dark-aqua
translated `infinity` name, Health999999 and the source aqua team. It plays
wither.death volume1/pitch1/minimum-volume.5 and emits2000 forced signal smoke
particles, spread1,1,1, speed.1. It executes `drop_wand` on every
`savoujen_cloud` item display within40, then teleports the ghast to(0,-10000,0)
and kills it. Preserve the distinction between this sequence and quiet removal.

`lbc/loot_table/sauvojen_tuntija.json` independently gives one repeating command
block with `infinity_spawn:1,drop:1`, model `lbc:infinity`, tierX. Thus ordinary
native ghast death has an additional causal drop-dispatch dependency. Paper
`runNativeInfinityDrop` at4858 recognizes `infinity_spawn`, produces the special
Infinity item, performs sound/smoke and cloud conversion, and consumes one token.
Paper `spawnInfinityItem` at9220 creates the `instantitem/infinity_rand` stack.

`other/sauvojen_tuntija/drop_wand.mcfunction` converts each matching emitter to
its corresponding original normal wand, then kills that emitter. The item entity
has Age=-32768, NoGravity, Glowing, CustomNameVisible and source Health999990;
its entity display name is gold, its actual item name light-purple, with exact
original model/lore/tier/flags. The wand definitions are Ruby2000mana/tierC,
Terra1000/C, Electro400/A with cyberset, Air1000/B, Acid250/B, Teleport3000/C,
Stone2000/B, Fire100/B, Blind250/B. Preserve their complete original payloads.
Paper `dropSauvojenCloudsAround` at9244 and `dropSauvojenCloud` at9262 implement
the same radius40 conversion; `resolveSauvojenWandDefinition` begins9315.

The current Fabric game-source search for `infinity_rand` and `infinity_spawn`
found no consumer. A loot token, translated name, native ghast summon or
preserved reward JSON alone cannot establish completed Infinity mechanics.
Exact Infinity pickup/use behavior and its own transitive output pool must be
audited before this branch can be released as complete.

Further source closure identified
`lbc/function/other/instants/random_infinity.mcfunction`: uniform14 outcomes;
manamax100000000, manaregen100000000, giant armor/toughness/burning-time base
changes, attack damage100000000, attack speed/block-break changes, both
knockback resistances, max health214708364, the three infinite-effect tags,
an Infinity-enchanted LuckyBlock wand, movement/oxygen/fall base changes, the
original large `minecraft:infinity_scale` modifier group, and the original tiny
modifier group (`infinity_scale1`, with its distinct gravity ID). Source clears
one raw infinity_rand token, sends title/subtitle and grants
`lbc:true_advancements/legends/infinity`. The wand outcome reuses the broad
LuckyBlock pool and does not authorize reducing that pool. Source entry is
`other/instants/select2.mcfunction:1` for selected hand; historical inventory
entry is `other/instants/select_old.mcfunction:1`. Source
`players_15s.mcfunction:4–6` gives infinite ResistanceV, InstantHealth amplifier99
or Saturation amplifier50 for those tags; reset/remove_tags clear all three.
The current shared ItemEffectLevels/native attribute policy remains
authoritative when implementing those explicit source amplitudes.

## Shared implementation constraint

Most current spell services require a real ServerPlayer owner, use that owner's
team/self filtering in delayed collision, and remove children on owner lifecycle
changes. The boss is hostile to its causal author and that author's allies under
the original target selectors. A fake player, swapping to an unrelated online
player, removing attribution, or globally making all of an author's ordinary
spells hostile would each change behavior.

A future shared implementation should carry explicit emitter/source, causal
author, epoch and target policy through each child and delayed payload. A scoped
per-charge policy could preserve the existing nine engines, but would need
complete native collision, damage, status, terrain, reflection and electrical
integration; merely setting a tag at launch would not fix asynchronous owner
filters. Subsequent implementation is developing this explicit policy in
`LbcSpellSource`, with narrow scopes on each actual child charge. That ongoing
work is not qualification evidence. Existing player Sauvojen
and Academy behavior must retain their current policies and clocks.

The current explicit source record design contains emitterUUID, causal
authorUUID, match epoch, captured `LbcIndirectKills.Owner` and immutable target
exclusions. Each actual child entity has its own binding. Nested scopes restore
the prior source in finally/close; ordinary charges install a null scope rather
than accidentally inheriting a surrounding hostile callback. Native reflection
to another player restores that projectile's ordinary owner policy. Persistent
unload bookkeeping stores UUID/record only, not unloaded entity handles. A fresh
player retaliation triggered synchronously by hostile damage needs its own
cleared scope so it remains a new causal direction. Electrical traces use their
own explicit source record because they are native geometry records rather
than entity carriers. This is the intended review contract for the ongoing
shared integration, not a claim that all hooks or regression checks have passed.

## Scope receipt

Only this new UTF-8 note was written for this bounded follow-up. No Java, legacy
server/datapack, resource pack, build or deployment changes were made during the
follow-up. Earlier new Chaos armor work is separate and remains subject to the
parent's build/check process. See [Chaos source audit](CHAOS_STATIC_GEAR_SOURCE_2026_10_28.md)
for the original25-entry trap dependency boundary.
