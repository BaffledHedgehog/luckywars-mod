# Combined Sauvojen advanced source audit ? 26 October 2026

Read-only source audit; no Java/server/build/RP modifications or runtime qualification. Read root AGENTS and agent_notes/AGENTS plus RESOURCE_PACK. Existing REMAINING_ACADEMY_AUDIT_2026_10_25 correctly reports missing combined native item; its blind-check warning needs qualification below. Paths below relative to legacy `world/datapacksdisabled/LBC/data/lbc/function/`; W = `items/wands/sauvojen_wand/`, M = W + `meditate/`.

## Cast and drone contract

`W/cast_upgraded:1?4`: creates drone, debits configured sauvojen_wand_mana_use, mage-set manamax adjustment, trinket cast event. `cast_2_upgraded:4?15` uniformly rolls1..9 in order ruby,terra,electro,air,acid,teleport,stone,fire,blind; display at local forward2, scale2.5 matrix, tele/interpolation1, model st/element. Copies lbcID2 and team_number. Terra/electro/acid additionally rc_1sec. Source commented ghast is inactive.

`raycastselector:1` increments Lifetime; player dispatcher49?50 routes ordinary/upgraded. `flying_wand_tick_upgraded:1?2` converts Lifetime1 to2 and kills at400 (20 seconds global ticks). Lines3?8 snapshot identity/team and mark drone/owner caster, owner raycaster. Target lines5,17?25: nearest #mobs within150, other ID/team, !spectator,!strict_map_object; facing is mandatory, so no eligible target means no firing and no counter advance. Models still reroll every tick independently (28?36). Lines38?39 clear owner tags;41 witch trail;42 moves toward owner in horizontal plane0.1/tick and rises0.04/tick, requiring nonspectating same-ID owner. No distance tether or block-collision stop appears here.

| Drone | Fire interval global ticks | Counter modulo | Counter behavior |
|---|---:|---:|---|
| ruby |10, Lifetime%10=1|10|double increment; uses (old+1)%10|
| terra |1|10|old%10, then +1|
| electro |5, %5=1|6|old%6, then +1|
| air |10, %10=1|13|old%13, then +1|
| acid |5, %5=1|10|old%10, then +1|
| teleport |5, %5=1|5|old%5, then +1|
| stone |40, %40=1|9|old%9, then +1|
| fire |5, %5=1|8|old%8, then +1|
| blind |5, %5=1|9|old%9, then +1|

## Exact counter?helper mapping

All rows execute as drone at drone origin/direction unless explicitly marked. This is the direct helper execution context; a helper may internally select its owner or spawn remotely. Prefixes preserved so integration can inspect each producer rather than call a player-use route. Each branch resets drone Distance0 first.

| Element: source line | Counter | Payload helper | Execution exception |
|---|---:|---|---|
|ruby:9|0|`workingitems/sauvojen_staffs/ruby_preloop`|drone|
|ruby:10|1|`workingitems/sauvojen_staffs/ruby_hole`|drone|
|ruby:11|2|`workingitems/sauvojen_staffs/ruby_ray`|drone|
|ruby:12|3|`workingitems/awaken_tuntija_wands/avarice/click2`|drone|
|ruby:13|4|`workingitems/awaken_tuntija_wands/avarice/click2_upgraded_black_hole`|drone|
|ruby:14|5|`workingitems/awaken_tuntija_wands/avarice/click2_upgraded_divide`|drone|
|ruby:15|6|`workingitems/awaken_tuntija_wands/avarice/click2_upgraded_expand`|drone|
|ruby:16|7|`workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_avarice`|drone|
|ruby:17|8|`workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_the_end`|drone|
|ruby:18|9|`workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_silence`|drone|
|terra:7|0|`workingitems/sauvojen_staffs/terra_preloop`|drone|
|terra:8|1|`workingitems/sauvojen_staffs/terra_preloop_upgraded`|drone|
|terra:9|2|`workingitems/awaken_tuntija_wands/terra/click2`|drone|
|terra:10|3|`workingitems/awaken_tuntija_wands/terra/razorterra`|drone|
|terra:11|4|`workingitems/sauvojen_staffs/true_terra_sword_hit`|drone|
|terra:12|5|`items/swords/fevgalea_star/click_uncharged_shift_rc`|drone|
|terra:13|6|`items/swords/fevgalea_star/click_uncharged`|drone|
|terra:14|7|`items/swords/fevgalea_star/hit_shoot`|drone|
|terra:15|8|`items/swords/fevgalea_star/hit_shoot`|drone|
|terra:16|9|`items/swords/fevgalea_star/hit_shoot`|drone|
|electro:7|0|`workingitems/sauvojen_staffs/electro_preloop`|drone|
|electro:8|1|`workingitems/sauvojen_staffs/electro_ray`|drone|
|electro:9|2|`workingitems/awaken_tuntija_wands/erecto/click2`|drone|
|electro:10|3|`workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_ray_loop`|drone|
|electro:11|4|`workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper`|drone|
|electro:12|5|`workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sphere`|drone|
|air:7|0|`workingitems/sauvojen_staffs/air_preloop`|drone|
|air:8|1|`workingitems/sauvojen_staffs/air_preloop_burst`|drone|
|air:9|2|`workingitems/sauvojen_staffs/air_preloop_fast`|drone|
|air:10|3|`workingitems/sauvojen_staffs/air_preloop_homing`|drone|
|air:11|4|`workingitems/awaken_tuntija_wands/exahoming_click_loop`|drone|
|air:12|5|`workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_sisyfus`|drone|
|air:13|6|`workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny`|drone|
|air:14|7|`workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_curse`|drone|
|air:15|8|`workingitems/awaken_tuntija_wands/exasperato/click2`|drone|
|air:16|9|`workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_default`|drone|
|air:17|10|`workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_electro_ray`|drone|
|air:18|11|`workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_chaos_area`|drone|
|air:19|12|`workingitems/awaken_tuntija_wands/exasperato/click2_upgraded_mode_nuke_rocket`|drone|
|acid:7|0|`workingitems/sauvojen_staffs/acid_preloop`|drone|
|acid:8|1|`workingitems/awaken_tuntija_wands/acidacid/click2`|drone|
|acid:9|2|`workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_caust`|drone|
|acid:10|3|`workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_default`|drone|
|acid:11|4|`workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_gatling`|drone|
|acid:12|5|`workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_rifle_tick`|drone|
|acid:13|6|`workingitems/awaken_tuntija_wands/acidcorrupt/loop`|drone|
|acid:14|7|`workingitems/awaken_tuntija_wands/acidcorrupt/mode_reverse`|owner as+at|
|acid:15|8|`workingitems/awaken_tuntija_wands/acidcorrupt/mode_chuma`|drone|
|acid:16|9|`workingitems/awaken_tuntija_wands/acidcorrupt/mode_titul`|drone|
|teleport:7|0|`workingitems/sauvojen_staffs/teleport_preloop`|drone|
|teleport:8|1|`workingitems/sauvojen_staffs/teleport_inverted`|drone|
|teleport:9|2|`workingitems/awaken_tuntija_wands/boltbolt/loop`|drone|
|teleport:10|3|`workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_snow_meteor`|drone|
|teleport:11|4|`workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_mode_watering`|drone|
|stone:7|0|`workingitems/sauvojen_staffs/stone_preloop`|drone|
|stone:8|1|`workingitems/sauvojen_staffs/stone_preloop_upgraded_default`|drone|
|stone:9|2|`workingitems/sauvojen_staffs/stone_preloop_upgraded_bridge`|drone|
|stone:10|3|`workingitems/sauvojen_staffs/stone_preloop_upgraded_shield`|owner as+at|
|stone:11|4|`workingitems/sauvojen_staffs/stone_preloop_upgraded_spike`|drone|
|stone:12|5|`workingitems/awaken_tuntija_wands/stonekivi/loop`|drone|
|stone:13|6|`workingitems/awaken_tuntija_wands/stonekivi/loop_earth`|drone|
|stone:14|7|`workingitems/awaken_tuntija_wands/stonekivi/comet`|drone|
|stone:15|8|`workingitems/awaken_tuntija_wands/stonekivi/loop_sun`|drone|
|fire:7|0|`workingitems/sauvojen_staffs/fire_preloop`|drone|
|fire:8|1|`workingitems/sauvojen_staffs/fire_preloop_upgraded_big`|drone|
|fire:9|2|`workingitems/sauvojen_staffs/fire_preloop_upgraded_concentrated`|drone|
|fire:10|3|`workingitems/awaken_tuntija_wands/fireblaze/at_random_fireball`|drone|
|fire:11|4|`workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas`|drone|
|fire:12|5|`workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega`|drone|
|fire:13|6|`workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars`|drone|
|fire:14|7|`workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world`|drone|
|blind:7|0|`workingitems/sauvojen_staffs/blind_preloop`|drone|
|blind:8|1|`workingitems/sauvojen_staffs/blind_preloop_upgraded_phantom`|drone|
|blind:9|2|`workingitems/sauvojen_staffs/blind_preloop_upgraded_rain`|drone|
|blind:10|3|`workingitems/awaken_tuntija_wands/psychoblind/click2`|drone|
|blind:11|4|`workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_default`|drone|
|blind:12|5|`workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_invisible_charge`|drone|
|blind:13|6|`workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_mindblowing_ray_loop`|drone|
|blind:14|7|`workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_spray`|drone|
|blind:15|8|`workingitems/summoner/psychovex_spawn_pre`|nearest noncaster nonspectator player origin; still drone executor|

Ruby lines19?20 also give same-ID players levitation3s amplifier30 and slow_falling60s amplifier0 on branch9;21 branch8 has90% chance additional cold increment. **Semantic choice:** fresh cold0 emits1,3,5,7,9 repeatedly; even branches including8 never run. Preserve odd-only accident or fix to full ten-mode cycle. This is unrelated to harmless native selector/ownership cleanup.

## Per-tick independent visual lottery

Each `flying_wand_tick_ELEMENT_upgraded_change_model:1?4` uniformly rerolls each tick, not on fire and not based on counter; terra1?5 has four choices. Model prefix `lbc:`.

|Element|Models|
|---|---|
|ruby|st/ruby, consuming_wand, dublicating_wand|
|terra|st/terra, terra_razorpine, terrasword_true, fevgalea_star|
|electro|st/electro, erecto, electroblocker|
|air|st/air, exasperato, homing|
|acid|st/acid, acidacidwand, acidcorrupt|
|teleport|st/teleport, boltbolt_wand, infbolt|
|stone|st/stone, stonekivi, bedrock_wand|
|fire|st/fire, blazefire, the_sun|
|blind|st/blind, psychoblind, blindvex|

## Meditation timeline and readiness

`M/start:1?3` sets620. `players_tick:58` only ticks positive counter outside stopper_magic radius20. `M/tick:1?4` requires mainhand custom sauvojen_wand1/upgradable1, otherwise stop; then subtract1. Counter1 invokes magic_academy/can_upgrade and global trial cleanup (`tick_ok:85?86`). The can_upgrade?can_upgrade_book path writes current mainhand upgrade_available1/upgradable0 via scratch container; native should mutate actual participating stack directly. No automatic upgraded item grant occurs here.

|Countdown before decrement|Timeline/actions; tick_ok source lines|
|---|---|
|620|ruby clearing,21|
|600..550|three terra displays each tick on radius10 at Y+1, ticker yaw?1,2,3;23; phase_terra_spawn:1?6, spawn1:1|
|520,500,480,460,440|electro web batches4,8,12,14,16; each later batch damages/removes prior web first;25?29; phase_electro_tick:7?24|
|420|final web damage/remove;30|
|419..383|37 air pillars at horizontal local offset -18..+18 relative current player yaw;32?68|
|320|stone template;70|
|300|acidmedit marker at X-5;72|
|290..200|50% each tick spawn acid display at floored X/Z plus random integer[-10,10],Y+9;73 and acid_ball_spawn1:2?10|
|160|levitation15s amp0 then1s amp20;75?76|
|140|darkness15s amp0;78|
|140..40|50% each tick blind interaction/face and darkness15s/levitation1s refresh;80|
|120..40|70% each tick increment counter before outer decrement: stochastic stall,79|
|30|red warning title/subtitle9,9/18|
|20|fire/lava/fall phase;82|
|1|readiness and cleanup;85?86|

Minimum620 unpaused ticks=31s. 81 stall values add expected189 ticks, expected809 ticks=40.45s; unbounded random tail. Stopper time adds arbitrary pause. Title/subtitle stage labels620,560,520,420,310,300,160,140,30 are presentation transitions, not all actual hazard start times (1?18).

Terra display at Lifetime40 emits lbcr raycast_terrameditator marker data lifetime20, tagged shield_blockable2, then dies (meditator_tick:1;phase_terra_tick:1?6); dependency belongs to lbcr, not combined tick. Air pillar scale(X,Z)=Lifetime?.05,Y200; rotates40?/tick; at60 explodes: four native TNT fuse1 plus8 magic damage within7 to all nonspectator #mobs; kills display (phase_air_tick:1?4,phase_air_explode:1?7). Acidmedit moves local.25 and yaw+5 per tick, dies130 (phase_acid_tick:1?3). Baller Lifetime40..50 has50%/tick marker spawn; ball pitches80..90?, random yaw, up to3 samples?.4/tick, dies40; collision4 in_fire+poison10s amp0. Block corruption advances lime?green?yellow?light_gray?gray?black powder?air; protected #minecraft:to_lime unaffected at initial conversion. Helpers have no owner/team exclusion.

Blind: real flow uses `predicate/looking_at_scare.json` vanilla player looking_at interaction tagged scare_picture; tick_blind:2 calls tick_blind_up. scared +=10 on look; >105 damages8magic and fails. Scared is not reset at start (only fire phase resets0), so prior failure can carry score. Each scare interaction4?4 is placed10 blocks in uniformly randomized yaw/pitch direction; display5? scale has ten uniform scare_face models. Face and interaction rise.1/tick and die100; face5%/tick turns toward nearest meditator. `tick_blind_check:2` is incomplete condition without run **but no actual function references it**; not evidence real looking-at flow is disabled.

`damaged:1?2` and damaged_acid:1?3 damage then fail when trial active, regardless damage acceptance. fail/stop kill ALL tagged meditators globally, fail schedules another global kill3t. Concurrent trial ownership isolation is an obvious native correction, not a user question. Direct damage, TNT, poison, lava, displaced blocks need author inheritance under existing user contract.

## Exact destructive footprint

|Source|Geometry inclusive offsets|Cells/behavior|
|---|---|---|
|phase_ruby:1?13|349-cell stepped near-circle footprint inside X/Z?10, Y0..24|8725 air destroy; per X widths:7 at?10,11 at?9,13 at?8,15 at?7,17 at?6,19 at?4/5,21 at-3..3|
|phase_stone:1|template lbc:stone_cylinder at(-23,-4,-23), NBT size47?19?47|33079 explicit cells:6964 stone+26115 air; declared volume41971, absent cells unchanged. Bounds X/Z-23..23,Y-4..14|
|phase_fire:4|X/Z?15,Y-20..0|20181 air strict|
|phase_fire:5|X/Z?15,Y-50..-21|28830 lava|
|phase_fire:6|X/Z?60,Y-21|14641 lava; union with previous42510 lava cells|

Fire phase also clears levitation, slow_falling4s, scream, scared0. Ruby uses destroy drops; fire strict avoids updates by source intent. Template places both stone and explicit air. Exact scale is material; confirm intended full destructive trial if broader scope not already authorized. Do not silently shrink into an arena or replace lava with particles.

## Current native producers and gaps (source presence, not qualification)

Current Java has no combined Sauvojen class/registration/meditation. Existing individual items cover much payload logic, but most producers assume player eye/body; calling use() would charge mana, change mode or run achievements incorrectly. Need explicit owner plus drone origin/direction, snapshot cause/team, no player movement spoofing.

- LbcWands fireVolley797,bigFire801,concentratedFire864,blindRay1337,electroVolley1047,electroRay1125,libraryFlyingElectro1019; only libraryFlyingElectro already accepts explicit origin/direction. launchFire810/adoptFire819 and target1436?1454 provide ownership/role integration.
- LbcTerra castAt309 already accepts owner/origin/direction/Kind; normal cast302/305 wraps it. LbcStonekivi castMode70 accepts owner/hand/stack/mode and needs detached source entrypoint.
- LbcAwakenedAcid castMode43, LbcAcidCorruption cast108, LbcExasperato cast170, LbcHoming grimoire153/libraryStars157, LbcConsuming castMode171 exist; all these interfaces are owner-centric and require explicit source adapter review before reuse for a drone.
- Closure includes true terra sword, Fevgalea star three variants, psychovex, electro/erecto and fireblaze modes, duplicate/black-hole and consuming global modes, boltbolt; their available individual registration is not proof a safe detached producer exists. Meditation additionally needs lbcr terrameditator, green/red gaycaster particle generators, stone-cylinder template preparation, web hazard logic, scare entities/predicate and exact terrain operations.

Stale-note correction: prior remainder notes are a discovery index only. Ruby double increment is in **upgraded ruby leaf**, not generic upgraded tick. Broken blind_check is unused. Current source audit offers no runtime evidence and no blanket claim all payload adapters are ready.

## User decision — 26 October 2026

(user) Academy ruby must use all ten variants. Future Academy implementation must fix the duplicate cold increment so the complete ten-mode cycle is reachable. This approval supersedes the odd-only source accident described above; it does not establish that Academy producers or the meditation trial are implemented or qualified. Current release scope is the ordinary nine-branch slice; full Academy83-mode/trial closure remains gated.

## Ordinary implementation review snapshot — 26 October 2026

Read-only review of in-progress LbcSauvojen and detached flying overloads; production still being edited by base agent, so this is not final qualification.

- Confirmed mismatch sent to base/root: drone block lifetime guard used vanilla state.isAir(), whereas ordinary source flying_wand_tick:3 uses broad #minecraft:airs. Source tag includes water/lava, grass, torches, carpets and other passables; retain native loaded-chunk guard but match source tag semantics.
- Ordinary source fires on cadence even without a target, retaining prior/default yaw/pitch. Current native unconditional emit correctly differs from Academy's facing-gated emission. Ordinary cadence ruby60,terra20,electro20,air60,acid20,teleport80,stone60,fire5,blind10; increment1→2 then cutoff200 agree. Movement is away from owner eyes by0.1 in full3D, and current native movement matches.
- Matrix setRow values reproduce the four explicit source rows; source has no initial Rotation field, so default yaw/pitch0 retained without initial target is expected. Native display interpolation and teleport duration1 match.
- Source ordinary payload lifetimes ruby50,terra200,electro35/20/25/30,air120,acid300,teleport60,stone80,blind100; flying overloads provide independent source lifetime rather than ordinary item's configurable lifetime. Source per-tick movement distances ruby1.8(6×.3),terra1.8,air.8(4×.2),teleport1.2(4×.3),stone1.6(4×.4),blind1.8,acid1.6(4×.4),fire initialMotion2.5.
- Blind contact was expanded through damagewithid7 → damagewithid7_kill, which DOES kill the projectile. A preliminary concern about penetration was withdrawn immediately before implementation changes; native termination on eligible entity contact is correct. blind-wand_effects gives0.5generic damage,blindness200ticks amp0,wither80ticks amp0,matching current native.
- Current native drone snapshot holds actual owner,used hand,copy of stack,epoch,carrier; stopped/removed/dead/observer/foreign-level owners clean up; global and owner TimeStop pause before lifetime/movement. Native target filtering through lifetimes and wands adds required role/team/Greed protections. No runtime or final integration/readiness assertion is made by this source-only review.

Matrix verification evidence: read-only `javap -p -c` against cached mapped `minecraft-merged-deobf-26.3.jar`, class net.minecraft.util.ExtraCodecs, MATRIX4F decoder lambda$static$19. It decodes flat index i via Matrix4f.setRowColumn(i>>2,i&3,value), and encoder lambda$static$20 uses getRowColumn with same indexing. Thus four explicit setRow calls match source NBT16 values exactly; no transpose is needed. This is actual native bytecode evidence, not a guessed JOML float-array convention.

## Subsequent complete-source correction -27 October2026

The earlier83-mode count and toward-owner upgraded motion were audit errors.
The exact dispatch has80 rows (10/10/6/13/10/5/9/8/9); upgraded movement is
horizontal0.1 away from the owner's eyes plus0.04 upward. The Ruby approval
above remains unchanged. Its Silence row removes the source carrier itself.
Electro meditation faces a multi-entity selector with no limit, so warning and
damage rays fork to every previously active node, giving6/120 edges for4/16
nodes. Each damage ray terminates at its first target. Native failed data-get
store is verified independently after command-context drainage; drone Health
must not be replaced with caster Health. Current implementation and release
qualification are tracked in [the combined port](SAUVOJEN_ACADEMY.md).
