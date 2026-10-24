# Halloween wind family native contract

Implementation: [LbcHalloweenWind.java](../src/main/java/me/skywars2/game/LbcHalloweenWind.java).
This is the Halloween Vex wind wand, distinct from the Vex Soul summoning family.
Native IDs are `skywars2:vex_wind_wand`, `skywars2:academy_vex_wind_wand`,
and `skywars2:halloween_mace`. Registry constants are `WIND`, `ACADEMY_WIND`, `MACE`.

## Sources and presentation

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Authoritative functions are `function/other/event/halloween/vex_wand/` and
`function/other/event/halloween/halloween_mace/ability.mcfunction`; source funding
is `function/config.mcfunction` (`vex_wand_mana_use=700`). Source presentation is
`loot_table/halloween/transformables_staff.json` and `transformables_shovel.json`.
The two wand variants retain `vex_wand`, model `lbc:vex_wand`, tiers B/A. Mace retains
`halloween_mace`, model `lbc:halloween_mace`, tier S and actual native MaceItem
smash behavior, default damage5/speed-3.4, tool, weapon, durability and repair data.
Source loot carries Density5/Wind Burst2; the ported loot leaf preserves those
registry-dependent enchantments and original source name/lore formatting.

## Wand execution

`use(player,hand)` checks the actual used stack. Ordinary nontrial casts and Academy
modes1..3 spend exactly one source configured cost through `trySpendWand`; the
shared `wandCasted` hook runs exactly once, including the ordinary trial cast.
Academy Shift+RMB changes the used stack mode for free before personal nomagic and
funding. The source mode keys are `mode_default`, `mode_user`, `mode_not_user`,
`mode_windball`. Gameplay roles, frost/action gates and Time Stop still gate input.

Mode1 adds the current look vector1 to source `#minecraft:mobs` within5 of the
source eye-positioned spell context, including caster and allied players. Mode2
adds it only to caster. Mode3 excludes caster. Impulse selection is WILD and keeps
technical roles, spectators, active Greed and Time Stop protected. No extra
wand-projectile impulse is generated. Paper's `MotionBrigadier.capVector` caps
the largest absolute component at10, preserving component ratios; native impulse
application retains this source cap and sends player motion packets.

Used-stack ordinary trial Shift+RMB marks readiness immediately via Academy, then
places the existing native `lbc:air_sphere_11x11x11` template at
`floor(eye+(-5,-1,-5))` with integrity1 and native NONE rotation/mirror. This reuses
the original exact template mask, including sparse cells, rather than approximating
it with a distance equation. The map placement service restricts writes to resident
chunks and tracks map restoration. WILD trap attribution wraps terrain changes.
The meditation `execute at @s` resets the entity-selection context to caster feet;
source mobs within11 receive an outward SET10. `facing entity @s eyes` runs after
`as` but before `positioned as`, so it faces target eyes from caster feet and keeps
that rotation when moving execution to the target. Native `positioned ^ ^ ^` in the
outer spell resets the anchor to FEET. The vector is therefore
`normalize(target.eye - caster.position)*10`, not an inward pull or eye-eye vector.
The caster is included and receives straight upward SET10, because its own eye
position is above its feet. This source audit supersedes the initial unverified
inward/self-zero interpretation. Shared indirect ownership is captured for the cast and displacement
retains newer accepted hit precedence. Source gust40 spread5, burst sound1/1,
and trial forced large gust emitter20 spread6 are retained.

Mode4 creates exactly five native WindCharge actors at eyes, each with independent
source X/Z random integer offsets[-1000,1000] and Y[-200,200], scaled.0001, added to
floored source look components at scale10000. Native collision, acceleration0,
inertia1, direct damage1, radius1.2 wind explosion, exposure, jump scaling and block
triggers remain native. Burst sound is1/2. Direct targets follow the common native
projectile policy; blast selection also permits the current owner for vanilla wind
jump. Native current owner is resolved at explosion time, including reflection.
Displacement snapshots are bounded to native explosion radius and carry captured
indirect ownership, never a periodic world scan.

## Mace jump and cooldown

Source `players_tick.mcfunction` only enters `items/armor.mcfunction` when the real
jump statistic is positive, then resets it. Consequently production activation is
`jump(player)` called from the native jumpFromGround tail, not an every-frame auto
jump. Looking80..90 degrees down, held in either hand and cooldown<=0 triggers one
large gust emitter, burst sound1/1 and source cooldown5 seconds. Both hands still
trigger only once. The base native jump has already happened before the ability.

Read-only Paper `plugins/luckywars/src/main/java/me/luckywars/command/MotionBrigadier.java`
`execSet`/`parseSetComponent` confirms `motion set ~ ~1 ~` preserves current X/Z
and adds1 to current Y; it does not set Y to absolute1 or derive velocity from world
coordinates. The shared source maximum-component10 cap remains applied.

The service owns `cooldown_wind_charge` in the shared mana scoreboard, one decrement
per20 active ticks, with per-player cadence paused by Time Stop. `second(player)`
is a deterministic native fixture seam; production calls `tick()` once. It never
activates the mace from tick. HUD is source E105+white remaining seconds or
E104+green `ready`, visible only while either hand holds the source mace.
`purify` clears score and cadence; `forget` also retires current-owner projectiles.

## Integration and limits

Root integration owns item registration, shared wand dispatch, Academy identity,
loot routing, HUD append, purge, lifecycle callbacks and the native explosion-target
filter. `managed`, `canAffect`, and `explosionTarget` are the shared hooks.
`tick` prunes tracked actors and updates cooldowns; native entity ticks own flight.
Time Stop and antimagic use existing shared services. Loaded plain tagged entities
cannot resurrect actors; only a live native subclass in the current item epoch can
rejoin. Unload retires/discards actors, match stop retires all and clears cooldowns.

No build, server launch, deploy or git operation was performed for this class task.
Native fixtures and qualification belong to the separate checks/integration work;
this contract does not claim a successful runtime before those results exist.

Root source correction: the jump-only mace branch in items/armor has no mana or antimagic admission. Its native jump ability retains that absence, while shared observer/action/freeze/Time Stop/Greed guards still apply. The wand itself retains ordinary magical admission.


## Integrated qualification

Build `07ff2ecd` passed80 recorded suite results across two actual mode1 matches (150.83s). Exact 313 sources/866 classes/2800 resources match the installed tested JAR. Main installed stopped, rollback `b0dfd5ca` retained, server not started. Evidence: `build/verification/halloween-wind-final/`. Existing fake-bot Polymer packet-context ERROR pair only; no new error lines. Native fixtures include explicit boundaries; no real client pixels, disk restart/disconnect or peak-load claim. This supersedes individual task-agent pending statuses.
