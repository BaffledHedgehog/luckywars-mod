# Actionbar coverage audit — native ported items

HUD implementation is qualified with [the combined batch](UI_WEAPON_BATCH_PORT.md).
Scope is already ported gameplay, not creation of legacy timers. Source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.
Read current AGENTS/resource rules and native item/service state. Only LbcItems
hud/helper section and this note were edited by this task; no getter/gameplay/RP/
Reroll/build/server/deploy changes. Root owns component cache/integration/tests.

## Added and corrected

Four missing native held-item cooldowns: Mobgun, ORAORA, Homing Lightning, Stand
Clocks, plus the native KFC Gigaponos effect duration. Added existing native
Overheaven matches-remaining indicator, magic and
mechanism beacon presence icons, and native counting-only arrow ammunition row.
Restored exact relative source order of ALL represented entries: Shock is after
Screamray, Blaze precedes Gatling Fovos, No Magic/Piss/Reverse occupy their own
source positions, AK47 ammunition follows Rocket Armor near the end. Existing
mana/charges/effect timers remain authoritative native counters.

Cooldown syntax: four spaces,E105,space +WHITE active value; zero becomes four
spaces,E104,space +GREEN translated ready. Source units are preserved, not made
uniform: Mobgun displays raw tick counter; Stand abilities display seconds
ceil(native remaining ticks/20), including last partial second. No new timer,
clock update, counter mirror or gameplay mutation is introduced. Native arrows
use clearOrCountMatchingItems(predicate,true,0,craftSlots), the exact counting-only
query already used by native Mana equipment; includes cursor/craft without removal.

## Source → native → HUD matrix

Rows follow S ui/get_data and nested ui/charging/ui/cooldown order. “Existing”
means already rendered before this patch; ordering fixes apply where noted.

| Source row/counter | Authoritative current native state | HUD/result |
| --- | --- | --- |
|get_data1–6 mana/manamax/manaregen_visual |LbcMana.hud |existing E100/E132 debt color bands |
|get_data8 stopper_magic |LbcMagic.beaconSuppressed, including indexed legacy marker compatibility |NEW E175,#4E5C24,presence only |
|get_data9 stopper |LbcMagic.mechanismSuppressed |NEW E176,#4E5C24,presence only |
|get_data11 cold |LbcFrost.cold |existing E137,red,x count |
|get_data12 cold_cd |LbcFrost.cooldown |existing white parenthesized seconds |
|get_data13 frozen |LbcFrost.frozenSeconds |existing E138,red seconds |
|get_data19/count_arrows1–2 |native vanilla bow/crossbow in either hand +arrows-tag count |NEW E108,white actual count |
|charging1 swordcharged1/swordcharging1 |LbcElectro.hudCharges/charging |existing E103 white charges,aqua progress /10 |
|charging2 swordcharged2/swordcharging2 |LbcTerra.hudCharges; native one-hit charge has no fractional progress |existing E103 white charges,green0/1 |
|charging5 swordcharged5/swordcharging5 |LbcElectro.yellowHudCharges/yellowCharging |existing E103 white charges,#A55E50 progress /7 |
|cooldown2–3 coldown16/mobgun |LbcMobgun.cooldown |NEW E105/E104 RAW TICKS |
|cooldown5–6 coldown25/attack_helicopter |LbcShooters.helicopterCooldownRemaining |existing active/ready source units |
|cooldown17–18 divider_cd/greed_greed |LbcDuplicating-owned native divider_cd via existing getter |existing active/ready seconds |
|cooldown31–32 coldownoraora |LbcStand.cooldownTicks |NEW active/ready seconds |
|cooldown34–35 coldownlightning |LbcStand.lightningCooldownTicks |NEW active/ready seconds |
|cooldown37–38 cd_helicopter |LbcShooters.bossHelicopterCooldownRemaining |existing active/ready source units |
|cooldown40–41 coldowntimestand |LbcTimeStop.cooldownTicks (native Stand clocks) |NEW active/ready seconds |
|get_data25 blaze |LbcBlaze.headCharges/native blaze |existing E110,green charge count |
|get_data27 gatling_fovos |LbcBlind.queued |existing E112,green queued charges |
|get_data28 lookback |LbcPsycho.hudValue |existing E113,red raw source counter |
|get_data30 nomagic |LbcMana.noMagic/native nomagic |existing E115,red seconds,correct placement |
|get_data31 piss |LbcPsycho.hudValue |existing E116,red raw source counter |
|get_data33 reverse_mana |LbcMana.reverse/native reverse_mana |existing E118,red seconds,correct placement |
|get_data34 scared |LbcPsycho.hudValue |existing E119,red raw source counter |
|get_data35 screamray |LbcPsycho native rays queue |existing E120,green raw source counter |
|get_data36 shock |LbcElectro.shocks pulses |existing E121,red pulse count,correct placement |
|get_data37 target |LbcHoming.curseSeconds |existing E122,red seconds |
|get_data38 twitching |LbcPsycho.hudValue |existing E123,red raw source counter;ONE leading space |
|get_data43 skill_issue |LbcItems.curseTicks |existing E129,red source counter |
|get_data44 overheaven |native Stand profile mirrored remaining-match counter |NEW E131,green MATCHES,not seconds |
|get_data45 gigaponos |LbcKfcFood.gigaponosRemaining native finite map |NEW E135,WHITE RAWTICKS |
|get_data66 effect_add_mana |LbcMana owned stat-effect timer |existing E157,green ticks |
|get_data67 effect_add_manamax |LbcMana owned stat-effect timer |existing E157,green ticks |
|get_data68 effect_add_manaregen |LbcMana owned stat-effect timer |existing E157,green ticks |
|get_data69 effect_remove_mana |LbcMana owned bad-mana stat-effect timer |existing E160,red ticks |
|get_data74 effect_boost_mana |LbcMana.boost |existing E165,green ticks |
|get_data83 effect_rtp_1 |LbcColors.teleportTicks/native effect_rtp_1 |existing E142,white raw source ticks |
|get_data90 rocket_armor_jumps/jump_cd |LbcItems native RocketState |existing E173,white jumps - ceil recharge seconds |
|get_data92 hitbullet/finite AK47 |LbcAk47-owned source hitbullet |existing E206,white rounds,correct placement |

Unit evidence: S players_5t:4 removes coldown16 by5,workingitems/mobgun:130 sets50;
players_1s:11–13 removes ORAORA/Stand clocks/lightning by1; items/oraora/click:5
sets40,time_stop_powerful:1 sets40,other/stand_arrow/homing_lightning_spawn:26
sets30. Native corresponding budgets800/800/600 ticks are projected only for HUD.
Current native time stop is Stand Clocks; time_stopper60s is a donor-only source
item, NOT a separate implemented ability. It does not get a fake ready row.

## Legacy rows deliberately not fabricated

A name in LbcKfcFood's compatibility counter list, a read-only damage guard or a
loot-table flag is not proof of a ported timer. No owning native timer/ability
implementation was established for the following legacy-only display families;
they are excluded rather than exposing stale/synthetic scoreboard values.

| Source family | Legacy fields/flags audited |
| --- | --- |
|Equipment resources/get_data17–18 |nitro/techno boots,reactivefuel/reactive boots |
|Other sword charging/charging3–4,6–12 |death_skythe,vexsword,techrunner_katana,banhammer,fevgalea_star,triple_golden_sword,blazefire_sword,fartus_nadristailo upgraded |
|Other held cooldowns/cooldown8–29,43–60 |time_stopper,reactive_shish,hohlopitek,gluttony/lust/indolence,divider_by_metal_pipe,Nexus corrupter/roadgun/pulsar,Halloween mace,Soulhunter mace,danmaku/danmaku_score |
|Additional early statuses/get_data23–32 |shadow_wisp charges,acid_rifle,delayed_death,nanobots,ray |
|Additional retained statuses/get_data39–46 |dimensional_shifting,void_resistance,all_seeing_eye,living_legend,sprint_cooldown |
|Additional trigger queues/get_data48–65 |effect_firebreak/explosion/big_explosion/rtp_4/traps/evoker/shock/kiuaskivi/lightarrow/evokerarrow/dimshifting/witch_gens_reset/hedgehog_boom/stand_arrow_hit/nuking/vacuum/timestop/chain_lightning |
|Additional effects/get_data70–82 |effect_snus/gravity_curse/small_explosion/lightning_bolt/shock_potion,god_claw_cd,expansion_cd,deus_cd,effect_ghosting,stone_layers/stone_cd |
|Other tail displays/get_data84–98 |effect_rtp_2/3,Nexus Compass,Touhou skill,bucket-head secondary title |

Current projectile nukes are NOT effect_nuking's unimplemented player-trigger
queue; native dimension time stop is NOT effect_timestop potion queue. Existing
ported armor/re-forging identifiers do not imply nitro/reactive fuel mechanics.
Native Void Resistance compatibility checks alone do not establish its timer's
producer/decrement semantics. These distinctions prevent inventing gameplay.

## Native timers without a legacy actionbar slot

Audited firing/actor/lifecycle families: shared Shooter fire cadence, Magnum burst
queue, AK burst scheduling, laser spin/beam windows (root-owned separate change),
wand entity flight/lifetime and meditation work, Acid/Ant/Bee/Drone and other mob/
projectile lifecycles, Kiuas/Tanner aura upkeep, Academy trial windows, Home mirror
renewal, minecart spawner delays, clone work, Overload pulses and native vanilla
MobEffect durations. Their presence is not a source HUD row. No invented icons,
labels, seconds conversions or actor-remaining-life text were added. Native
vanilla effect HUD already owns ordinary potion durations. Root may separately
record an explicit user-approved display addition; this patch reuses source rows.

## RP and integration evidence/limits

Read-only current RP:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
assets/minecraft/font/default.json already maps E104 ready.png,E105 reloading.png,
E108 arrow_count.png,E131 over_heaven.png,E175 magic_stopper.png,E176
mecha_stopper.png; no font/model/translation/template change required. Existing
ready translation and exact source spaces/colors are reused.

Root replaced hand-maintained HudState field cache with final Component equality,
so new rows, held-weapon changes, styles/order and timer values invalidate
immediately, while preserving20tick heartbeat, observer-camera source and
ignorance clearing. Tests/native packet capture are root/check-agent work.
Static diff whitespace check passed. No build/runtime result is claimed here.

## Complete native KFC/player-counter owner pass

Followed EVERY concrete native addPlayerCounters/addCounters/addPlayerCounter
call in LbcKfcFood.addExactSourceScores (not only its generic string ledger):

| Native owner | Real state touched | HUD coverage / intentional absence |
| --- | --- | --- |
|Psycho |lookback,piss,scared,twitching,screamray |all existing source rows |
|Electro |shock pulse/deadline,cyber/yellow charges/progress |all source counters shown; pulse deadline itself has no legacy row |
|Shooters |heli/boss bursts and phase,stun,KFC/SLR cooldown,phantom burst |heli/boss source rows shown; stun/fire cadence/phantom delivery have no legacy row |
|LaserMinigun |native spin/fire/cooldown state |no legacy ui row; root owns separate animation/behavior task |
|AK47 |finite ammo |existing E206 |
|Magnum |fired count,mode burst remaining/delay |no legacy ui row for these shooting counters |
|Mobgun |native cooldown |NEW raw-tick source row |
|Stand |ORA cooldown,lightning cooldown,grantDelay,falling; profile matches |NEW ORA/lightning/Overheaven rows; grantDelay/falling are transport schedules without source UI |
|TimeStop |Stand clocks cooldown |NEW source-seconds row; dimension-stop remaining has no matching player queue row |
|Terra |sword charges |existing charge row |
|Blind |Gatling queued charges/schedules |existing queue row; delivery age no source row |
|Homing |curse seconds,rain/destiny durations |existing curse row; sustained-cast delivery durations no legacy UI |
|Duplicating |divider cooldown |existing divider row |
|Frost |cold stacks/cooldown,frozen |all existing source rows |
|Colors |native effect_rtp_1 |existing raw-tick row |
|Mana |five stat timers,overload_jump/overload_slam |five existing source rows; overload transport counters no source row |
|LbcItems |turn curse,Rocket charges/recharge |existing source rows |
|KfcFood itself |gigaponos finite timer |NEW E135 WHITE raw400tick budget |
|Native MobEffects |finite effect durations extended through native instance |native Minecraft effect HUD; no invented source actionbar entry |

Case-insensitive/camel-case Java search also checked sprintCooldown/stoneLayers/
stoneCooldown, not only their original scoreboard spelling: no native producer/
owned countdown beyond generic compatibility names was found. This is why those
legacy rows remain excluded. No additional native source-HUD gap was found after
this owner pass; actual behavior tests remain root/check-agent evidence.

## Current batch status

[Combined contract](UI_WEAPON_BATCH_PORT.md): artifact68387e1d is qualified and
installed,142 recorded native suite results/two actual matches. Normal Fat
Skeleton and the listed missing native HUD rows are complete; earlier pending
qualification/source-only statements record their initial audit stage.
Normal/old portal remain unimplemented.
