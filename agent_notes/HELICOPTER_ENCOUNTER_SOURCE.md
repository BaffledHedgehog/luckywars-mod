# Helicopter hostile encounter: bounded source contract

22 October 2026. Documentation/source inspection only. **No encounter implementation or runtime qualification is claimed.** Only this new note was written. Read repository AGENTS.md, agent_notes/AGENTS.md, RESOURCE_PACK.md and NEXT_LUCKY_ACQUISITION_SOURCE.md. Legacy and resource pack remain read-only; no Java/resources/build/server/git edits.

## Roots and corrections

Source root S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`; all source paths below are relative to S. Target root T = `E:/servers_mew/skywars2_last/luckywars-mod/`; native paths below are relative to T.

This completes and corrects the bounded gaps in NEXT_LUCKY_ACQUISITION_SOURCE.md. The live source call chain is direct from tick_not_timestopped, not other/areas. Death summons **30**, not 32 TNT (death.mcfunction:3-32). Spawn_2 contains three lines allocating/copying ID and identifying hitbox; obstruction TNT is tick.mcfunction:1. A hostile world NPC need not acquire the token thrower's player ownership. Boss attacks should retain the boss as causal entity; do not substitute a nearby player or the later killer.

## Source: registration and cadence

- `minecraft/tags/function/tick.json:3` registers lbc:tick.
- `lbc/function/tick.mcfunction:1-2`: tick_not_timestopped runs unless time_stopped >=1; otherwise time_stopped_tick. `tick_not_timestopped.mcfunction:3` dispatches every slowraycast entity in #minecraft:raycasters once **each unstopped server tick**, except nexus/imprinted. `minecraft/tags/entity_type/raycasters.json:8` includes item_display.
- `raycastselector.mcfunction:1,8` increments Lifetime and dispatches other; `raycastselector_other.mcfunction:19` returns into drops/helicopter_boss/tick. `other/areas.mcfunction:1` is an alternate helper, with no caller found in the bounded function search; it is unnecessary to establish cadence.
- `tick_not_timestopped.mcfunction:5,7` also processes projectiles then increments global ticker once. Thus shooting parity alternates per unstopped tick, shared across bosses, not per local attack count.
- `tick_not_timestopped.mcfunction:1` processes items once per unstopped tick. items_tick:1-2 -> items_tick_tag -> items_tick_act:1 -> drops/specialdrops:72 establishes the dropped token trigger. A stack consumes one unit per invocation; ordinary gameplay exposure remains LuckyWars-only, modes2..4 still blocked.
- Source predicates `lbc/predicate/chance1.json`, chance0_5.json, chance40.json, chance10.json mean .01, .005, .4, .1 independently per call. At 20 unstopped ticks/s phase1 advances 20 blocks/s and phase3 drops 20 bombs/s. These are nominal source rates, not benchmark results. Timestop stops this dispatcher, not proof that every vanilla entity physics tick is frozen.

## Source: paired state and ordered phases

`lbc/function/drops/helicopter_boss_spawn.mcfunction:1-7` creates scale3 item_display and invisible NoAI/NoGravity ravager at trigger-context local ^ ^20 ^50. Ravager: health150, armor10, toughness10, empty death table, persistent. Display tags slowraycast/other/helicopter_boss/phase_1/mecha/mob; **neither entity receives player owner or team**. spawn_2:1-3 assigns pair lbcID2, not player ownership. Display faces token horizontally. Sounds and dust use token origin.

`drops/helicopter_boss/tick.mcfunction:1-18`, in source order:
1. Non-air at display position summons immediate fuse0 TNT each call.
2. Locate matching ravager by pair ID; if none within5, tppos:1-2 teleports display to matching ravager then +3Y. If pair is absent, first teleport fails but second relative +3 still executes.
3. If display has a nonempty team, nodamage_for_team marks mobs on matching numbered team1..32. Phase1, phase2, phase3 checks execute sequentially. A phase1 transition can execute phase2 or phase3 **in the same dispatch**; phase2/3 ->1 cannot rerun phase1 until next dispatch.
4. Clear temporary nodamage; mutate models; teleport matching hitbox to tick command-context origin minus3Y. Minecraft command position does not follow an earlier tp automatically: phase1 movement and tppos do not rewrite the caller origin. Explicit execute at @s does refresh it. Preserve this distinction when measuring source positions; a natively perfectly attached hitbox is a small deliberate correction.
5. HurtTime exactly10 invokes hitted (gold dust transition, blaze hurt sound at pitch1.5), with no health threshold or phase change. A different HurtTime does not trigger this visual.
6. Missing matching hitbox within5 invokes death at display. Matching hitbox with no display within5 invokes death as hitbox. Clear temporary current tags.

Model assignments at tick:9-12 are sequential 2->1,3->2,4->3,1->4. **Actual final cycle from spawn is 1->4->3->2->4**, not a clean four-frame loop. Retaining literal source or correcting to four frames is an author-visible choice; do not describe a correction as existing source.

`tick_phase_1.mcfunction:1-4`: advance local forward1; if eligible mob within50, 1% transition to3; if eligible mob within100, aim nearest eligible eyes and shoot from local forward8; if none within100, transition2. Existence checks omit strict_map_object exclusion whereas final aimed selector excludes it; native role-safe acquisition should avoid this inconsistency.

`tick_phase_2.mcfunction:1-3`: unusual chained facing/positioning command steers horizontally1 using the nearest eligible mob (no acquisition radius). It constructs a point .5 along target eye direction plus1 along the display's prior forward direction, faces outward from the display toward that point, restores display position, then flattens pitch and moves1. Thus it blends existing heading and target direction; it is not a direct teleport1 toward target. Keep the command as the geometry reference; do not replace with generic flight/pathfinding. Independently 40% attempt subordinate item spawn: choose current paired ravager within15, detect eligible mob in x/z +/-4 below its origin (dx8/dy-10000/dz8, distance<=150), then spawn at hitbox minus3Y. Finally .5% transition1.

`tick_phase_3.mcfunction:1-4`: rotate yaw+10; drop bomb; 10% drop_bomb sound; 1% transition1. No movement command here.

## Source: hostility and attack ownership

Targets use #minecraft:mobs, exclude all helicopter_boss_hitbox entities, spectator and temporary nodamage; aimed phase1/phase2 selector also excludes strict_map_object. Players belong to source mobs tag. `nodamage_for_team.mcfunction:1-33` only handles numbered1..32. Since normal spawn is teamless, it does **not** inherit the dropping player's friendships and attacks players/mobs without that team filter. No author question is needed merely to preserve ownerless hostile NPC behavior.

`shoot.mcfunction:1-23`: one arrow per dispatch, alternating side +/-1.5, forward .5 relative to the forward8 firing context; damage1, pickup2, speed6, killedarrow tag. Owner is source **display UUID**. No shield_blockable marker is added here; preserve native arrow shield behavior. Native implementation should use the living boss/hitbox as projectile owner and keep encounter identity separately. Bombs, effects and delayed follow-ups retain the same boss causal source; the token thrower and boss killer are not attack owners.

Source suppression is established without weapon analogy: display has mecha; `other/stopper.mcfunction:4` dispatches tagged mecha within20 to `stopper_marker_destroy.mcfunction:1-2`, which kills them (nuke special case irrelevant). It is **destruction**, not paused TECH recharge. Magic stopper has no magic-tagged boss to select. Native `LbcMagic.java:923-956` already kills eligible mecha entities within20; encounter lifecycle must handle that terminal cause coherently. Missing-display source discovery is unreliable because hitbox alone is not slowraycast, so reward-on-beacon removal should be explicitly decided, rather than inferred from handheld TECH rules.

## Source: bombs and scatter

`drops/helicopter_boss/drop_bomb.mcfunction:1-2`: TNT fuse1000, explosion_power1, rtp/groundbam; immediately call universal_pos_razb_7_y on the created TNT.

`universal_pos_razb_7_y.mcfunction:1-8`: read X/Z as integer command results (floor, including negative coordinates), independent uniform integer -7..7 added to each; write integer X/Z back; **Y unchanged**. No random motion and no rtp tag removal. Native doubles plus jitter would change the source distribution.

`projectilestick.mcfunction:6` -> `projectilestick_tnt.mcfunction:4`: OnGround groundbam calls `other/groundbam.mcfunction:1-7`, setting fuse2 and removing groundbam. Source bomb has none of optional evoker/fire/electro/strong/no_explode tags, so uses ordinary native TNT explosion at power1 after contact fuse. Fuse1000 is also a timeout; do not treat ground contact as the only possible explosion. Death TNT omit explosion_power => vanilla default4, same groundbam contact behavior. Death offsets and duplicates must preserve the literal **30 commands** at death:3-32; do not turn them into a unique-position set.

`death.mcfunction:1-35`: stop both sounds globally; summon30 TNT; roll lbc:helicopter_boss; decorate one nearby item with drop_item:1-4 (Invulnerable/NoGravity/Glowing/Age=-32768); kill executor. Target should decorate the actual spawned reward and perform one terminal transition per encounter, avoiding global selectors/reward duplication. Match cleanup is cleanup without reward.

## Source: phase2 subordinate item is not bat Cultus

`workingitems/penispenispenis_random_penis_big_penis_small_penispenis_only_spawn.mcfunction:1-8` rolls uniform1..6, emits **one loose item**, not a bat/projectile, at specified hitbox-minus3Y. Exact components: repeating_command_block, item_name translate penis, model lbc:penis_N, max_stack_size1, tooltip_style lbc:tier_ff, custom_data instantitem1/penis_bomb1/penis_numN (variant2 also govnoq1); entity Age=-32768, Health10000, PickupDelay3, Invulnerable1. Called _norc helper:1-2 invokes rand_motion_very_small and removes temporary penis tag. `rand_motion_very_small.mcfunction:1-6` gives independent integer[-10,10]*.01 velocity in all axes. No team or lbcID2 copied here.

`items_tick_tag.mcfunction:12,26`, `items_tick_act.mcfunction:7`, `workingitems/penis_mob.mcfunction:1-7`: while loose, nearest non-player #mobs within1.2 and !no_penis triggers variant and consumes one item. No spectator/team exclusion in that contact selector. `other/penises/select.mcfunction:1-6` preserves item executor for variant lookup then executes as contacting mob. Players can instead pick up after delay; inventory instant path `other/instants/select_old.mcfunction:2` -> `other/penises/player_select.mcfunction:1-6` applies matching variant and clears one player inventory item.

Variant source files in `other/penises/`:
- normal:6 arrow damage.
- dirty:4 arrow damage, SlownessVI8s, WeaknessVI4s, NauseaVI6s.
- nigger (literal source filename): fuse2 ordinary TNT at target, default power4.
- holy: BlindnessII8s, LevitationXVI1s, native lightning.
- horse: Slowness128 for1s, arrow damage10, teleport local backward5 and world +5Y.
- multicast: one .1 arrow damage attempt **to same executor** for every entity !spectator in dimension; command context position changes, victim does not. Native hurt cooldown remains relevant; it is not a .1 area hit to every entity.
Each retains original sound/particle presentation. Boss-origin ownership must survive item pickup, effects, TNT/lightning and displacement; source omits this causal metadata.

## Target reuse, gaps and acceptance

Existing implementations are reusable patterns, not proof encounter exists:
- `src/main/java/me/skywars2/game/LbcLoadouts.java`: command loot roll and returned reward bundle; reuse qualified helicopter_boss original/ported payload without new duplicate weapon.
- `LbcShooters.java:385-403`: native helicopter arrow construction; player-only weapon mana/recharge/input gates are inapplicable to NPC. `ProjectileLifetimes.java`: managed lifetime cleanup.
- `LbcWands.java:1144-1165`: shared role validity, observer/service protection. Its target/projectileTarget signatures require ServerPlayer; generalize appropriate source-entity friendship instead of assigning a synthetic player owner. Preserve own hitbox exclusion.
- `LbcKfcRewards.java:140-180,192-282`: existing variants, presentation, attribution and multicast worker. Current spawnBomb creates a bat, requires an active ServerPlayer, stores player UUID, and contact/pickup activation requires that player. Extend/extract loose boss-source variant support; direct invocation is not equivalent. Source loose tooltip/max-stack components also need preserving.
- `LbcMagic.java:923-956`: existing mechanism removal; `LbcIndirectKills.java`: causal effect/displacement handling. Existing player-specific paths need a boss entity source where relevant; no fabricated player kill credit.
- Existing epoch/reset patterns provide match cleanup. Retain explicit pair UUIDs and attack/reward entities, remove pair atomically. Concurrent bosses cannot share temporary selector identity.

Necessary author-visible decisions before exact behavioral sign-off: (1) correct accidental 1/4/3/2/4 model sequence to all four frames or preserve literal sequence; (2) beacon destruction terminal behavior/reward versus ordinary death (source destroys display but has unreliable orphan detection). Any wish to change ownerless hostility, scatter quantization, very high source bomb rate or the weird phase2 steering would be a deliberate change, not a prerequisite question. Do not ask whether hostile NPC must be owned by token player, or whether weapon TECH rules automatically apply.

Tests still required after implementation: actual dropped stack debit; two simultaneous UUID pairs; one dispatch/unstopped tick and same-dispatch transitions; phase2 geometry/random loose item/contact/pickup and all six effects; exact bomb scatter and contact fuse/default powers; native arrow/bomb attribution to boss; source teamless hostility plus observer/service guards; mechanism destruction; one real reward, actual reward decoration; orphan removal and match cleanup without rewards; qualified handheld weapon regression. **None of these encounter tests, builds or graphical RP comparisons ran in this audit.** Existing qualified weapon and KFC tests do not qualify this encounter.

