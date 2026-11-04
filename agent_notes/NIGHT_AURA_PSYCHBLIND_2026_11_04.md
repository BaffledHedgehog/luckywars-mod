# Rainbow Aura32 and Psychblind36 ? source/API audit

4 November2026. Read-only audit requested while reactive-trinket QA continues.
No production/resource/RP edits, builds, server runs or commits. Missing Homing30,
Phoenix31, TMT and other skill families are deliberately outside this task.

Source F = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
S = F/lbc/function/swrg_kit_integration/game/skills/. Exact numbered bodies for77
files, SHA manifest, Paper regen/selection excerpts, actual RP presentation and
qualified dc033ee7 JAR APIs are in ignored `build/verification/aura-psychblind-2026-11-04/`.
This note describes source and a proposed complete integration; neither skill is
implemented/qualified by this audit.

## Selection and actual entry routes

| Skill | Source page/slot | Source gate | Locked translation | Existing button |
| --- | --- | --- | --- | --- |
|32 Rainbow Prince|10100/4|lbc:true_advancements/legends/academy/gaycaster|lbc.adv.upgraded_staff.gaycaster.lore|model lbc:rainbow_aura, name lbcskill.29, nine lore lines|
|36 Psychodoppelganger|10100/8|lbc:true_advancements/legends/academy/psychoblind|lbc.adv.upgraded_tuntija.psychoblind.lore|model lbc:psychoballs, name lbcskill.35, five lore lines|

F/lbc/function/swrg_kit_integration/gui/select/10100.mcfunction:9,13,28,31
supplies gates/IDs. Ranked selector admits the same slots without achievements.
Source stats arrays are index31 and35, not translation suffix29/35. Existing
native source GUI JSON and actual RP models/translations already exist; no new
model/translation or wand reward is needed. Both remain absent from ready skill
catalog. Add readiness only after full chain qualification, preserving unrelated
entries, modes2/4, skill2 and unanswered families.

Rainbow selection invokes S/rainbow_aura/damaged_change_red through game/skill_select/select.
Paper SwrgNativeService also has a live skill32 selection call to its native red
attribute helper; this confirms selection bootstrap, not the complete seven-color
engine. The ongoing datapack paths are S/skills_tick -> Rainbow/tick or Psychblind/tick,
S/skills_tick_1s, S/skills_tick_5s, game/skill/tick_5t and tick_10t. They require
legacy!=1 and enabled #lbcskill at their parent dispatch. Player/marker ticks
exclude Nexus/Imprinted and source TimeStop; observer/active participant checks
must use current native roles rather than literal gamemode alone.

Rainbow damage entry is F/lbc/advancement/cocksuckers.json -> items/damaged ->
S/damaged -> rainbow_aura/damaged. It is entity_hurt_player with NO blocked=false
or positive-health-loss condition. Source shield/absorption and fatal accepted
criterion events can cycle color. Existing IncomingArtifactCriterionMixin is the
finite correct native hook; AFTER_DAMAGE with damage>0 would miss source behavior.
Use pre-hurt admission/selection capture when needed so fatal events are not lost.

## Rainbow: one complete seven-state cycle

The source folder has42 files, including empty gain_red and aura_orange_tick_10t.
Dead red_spin_particle functions have no live producer/dispatch (selector line is
commented); do not invent that old effect. Initial red then each criterion event:
red -> orange -> yellow -> lime -> aqua -> blue -> purple -> red. Source damaged
uses return branches, so it advances exactly once rather than cascading all tags.

| Color | Entry / exit state | Live payload and cadence |
| --- | --- | --- |
|Red|attack_damage +5 ADD_VALUE, modifier minecraft:uhf78arf7ahfkdhmfiashfdrehf; remove on orange|Every TICK, despite function name aura_red_tick_10t: four rotating redstone item-particle arms at Y+4, source ticker-derived angles|
|Orange|remove red attack modifier|Every5ticks FireResistance I20ticks to self; damage1 ON_FIRE with actual caster to nonself/nonallied source mobs in sphere3.5; three long small-flame loops. No setFire/ignition operation exists|
|Yellow|armor+40 (uhf78arf7ahfkdhmfiash), armor_toughness+40 (uhf78arf7ashfdrehf), knockback_resistance+100 (ahfkdhmfiashfdrehf); remove all on lime|Everytick seven raw-gold particle points around radius2, Y triangular wave0..3 derived from ticker modulo60|
|Lime|/regen add .025; aqua transition adds -.025|Everytick totem particle at Y+1 radius1, yaw ticker*18 degrees. Heal .025HP/tick = .5HP/s through the actual regen owner, not Regeneration potion|
|Aqua|manaregen+=5; blue transition subtracts5|Every100ticks +500 mana, hard source10000 ceiling independent of manamax; managram display Y+8 and underwater exit sound|
|Blue|no permanent attribute modifier|Every20ticks exact source nanobots_tier operation below, plus five electric/dust walkers|
|Purple|no permanent attribute modifier|Everytick one purple particle walker and, when grounded over nonair, clone support block into up to four adjacent qualifying air cells at floor(Y-.5)|

Do not clamp yellow resistance100 to vanilla1 or replace named attributes with
potion buffs. Current signed/native attribute admission is already shared. All
entry sounds are source-defined; preserve them and stop replay from retriggering
a color change. Reverse only the skill-owned attribute/regen/manaregen deltas on
selection loss/ending; preserve other equipment, levels and effects. Source reset
removes aura tags and yellow modifiers; attack removal is also in levelups_remove.
Named ownership avoids source stale-tag/modifier leaks without a broad stat reset.

Aqua exact resource rule: mana<=9500 gets+500; resulting mana9501..9999 is set10000;
already>=10000 stays unchanged. It does not clamp an existing over10000 value,
and it may exceed normal manamax. Native LbcMana.get/set/baseRegen are available.
Use raw source mana update, not regenerate() which imposes ordinary regen caps.
Managram: Distance increases5 per tick until100 (20ticks), then reverse decreases1
per tick to0 (100ticks); scale each axis Distance*.25, yaw+10 per tick; remove at0.
Native item_display teleport/interpolation duration1 is already source presentation.

Blue exact two-line order is unusual: if tier<=217 add3; THEN if resulting tier
118..219 set120. Examples114->117,115->120,216->120,217->220,218->120,220->220.
Lore says caps120; do not silently call ordinary nano gain or clamp every value120.
Current LbcNanobotArmor.tier reads canonical mana score nanobots_tier; its normal
armor pulse/resistance/cooldown remains authoritative. Five visual walkers start
atY+.8 with +X,+Z,-X,-Y,-Z .1 vectors. Each has seven .1 movement samples per tick,
5% direction change per sample to one of six axes, then3% removal per tick. Purple
walkers likewise have seven .1 samples per tick and20% removal. No hard visual TTL
or arbitrary population cap appears in source; use bounded cooperative work, not
one full entity/world enumeration per sample.

Purple is PERMANENT supporting-block expansion, not temporary terrain. There is
no timer or restore function. Source checks grounded and block atY-.1 nonair;
four destinations +/-X,+/-Z atY-.5 must be minecraft:airs_only. Each copies one
support cell atY-.5, including BE data; removes Items only from the destination;
normalizes red/brown mushroom/stem six faces to true. Source container contents
remain intact. This differs critically from current LbcExpansionSkill.snapshot,
which strips Items AND writes that cleared inventory back to the live source.
Do not call that helper for Rainbow or clear source containers. Use existing
MapManager.snapshot/restoreSnapshot/journal, strip Items in only the copied NBT,
preflight target before BE/clear side effects, and supply normal typed terrain
cause. Preserve chest/shop/bed/other protected native admission. Do not add a
private undo map, bypass arena protection, or replace authorized map lifecycle.

## Lime dependency: actual regen composition

Legacy Paper command Regeneration.java:205-233 folds ordered /regen operations
from base0 first, then item modifiers. .025 and -.025 have no duration, so cancel
on transition rather than expiring. Task runs everytick and heals only positive
rate, caps maxHP; beacon stopper_magic within AABB20 suppresses actual healing.
The old plugin's excluded-dimension return aborts processing of unrelated players;
that source control-flow bug must not become a new whole-server stop.

Current native LbcRegenerationBelt.rate(player) already reproduces physical slot,
item-ID grouping/order and shadow-aware native heal, but begins item processing
at0. It has NO command-base API. Implement a narrow rate(player,base) or typed
base contributor and feed .025 only while lime. This matters for multiplicative
or SET item modifiers: separately healing .025 would produce the wrong result.
Reuse its existing beacon AABB20/native heal admission; do not copy the Paper
inventory scanner, add a parallel healing clock or alter other regen item values.

## Psychblind: acquisition -> gather/orbit -> one existing charge

S/psychblind has four files. tick marks caster temporary doppcaster and discovers
source #mobs, spectator excluded, doppeled excluded, caster excluded, radius24.
This includes player and ordinary mob candidates, with no source same-team filter.
spawn_doppel creates one slowraycast/player/doppel/spawning/flying marker at the
candidate eyes, copies ACTUAL caster ID/team, offsets Y+1, sets pitch-90, sounds
particle.soul_escape, then adds doppeled to original candidate. The candidate is
not killed, damaged, NoAI-ed or converted; its marker is a one-time extraction.
Use existing roles/ordinary ally protection; never extract from shops or use them
as a borrowed source center. Do not infer that all magical mobs are valid shops.

Doppeled is a GLOBAL actor tag shared by all casters, not one claim per caster.
Source player leave_death removes its own doppeled; nonplayer tags live with that
native entity. Thus one caster can consume the opportunity for another, and an
expired/shot soul does not unmark the original. Mark only after successful native
actor admission. Record this deliberately; replacing it with per-caster cooldowns
would be new gameplay. Stable actor UUIDs/tags/native replay are enough; no NBT
scan, duplicate whole-world tag index or per-tick serialized lookup is necessary.

Flying doppel follows source gather geometry in eight .25 samples (2blocks/tick)
and invokes the shared Psycho tickend to commit actual marker position. Samples
show squid_ink and forced glow_squid_ink. When same-ID owner is within5 it removes
flying. Once gathered, it searches from OWNER for a nonself/nonallied source mob
within10; when found, executes click_upgraded_mode_default_1 at DOPPEL position,
produces exactly ONE visible normal Psycho charge, lifetime100, and kills doppel.
No mana debit, wandCasted, Mage bonus, spray, ray or four-shot default volley.
When no target exists, source orbits4 blocks from owner atY+3, yaw step-4, and
continues ink visuals. Owner absence has no explicit kill branch/lifetime in
source; blindly copying it leaves indefinite orphan actors.

Exact source aiming defect: target selector is @e distance..10 limit1 with no
sort, so it is NOT guaranteed nearest. Its execution facing then gets overwritten
by copying doppel ENTITY Rotation into the new charge. Displayed EN/RU lore says
nearest target; existing user Psychoblind rule AGENTS6866+ also specifies nearest
eligible homing target (10..20 ring, no eligible target nearer10). Prefer a finite
actual selected-target direction for the initial launch; preserve shared native
charge homing thereafter. Flag the initial aiming repair explicitly to parent;
this audit does not invent broader Homing30 policy or alter it.

Ordinary existing charge terminal is shared: native swept segment, blocks pass
through, ordinary role/ally/antimagic and lifetimes checks, at most two native
victims on a hit, damage4 ARROW with real caster, Darkness400ticks, scared+15,
lookback+5, twitching+10, private cave sounds/smoke; expires100ticks or on ordinary
impact. Existing owner/Psycho counters, paralysis cleanup, private sound delivery,
reflection/clone/lifecycle must remain the same. No parallel collision/raycast,
projectile physics, full-NBT body reconstruction or fabricated damage source.

## Actual native APIs and coherent next implementation

Qualified dc033ee7 JAR evidence confirms public Psycho use/tick/clear/count,
PACKAGE emitAt(owner,mode,origin,direction), psychoblind(owner,mode), and PRIVATE
spawn(owner,origin,velocity,invisible,spray,lifetime). emitAt(mode1) creates FOUR
charges; mode3 is SPRAY. Neither is the one-shot doppel terminal. Add one small
Psycho owner adapter which invokes existing private spawn with visible/non-spray,
velocity2 and100ticks plus the source .5-volume pitch.5/1.5 riptide pair. The new
skill owns only gathering/orbit phase and original doppeled claims; final damaging
flight is entirely the existing Charge. Current valid/paused/projectileTarget/
tickCharges own common suppression, target eligibility and native lifecycle.

Recommend one finite LbcRainbowAura owner and one LbcPsychblindSkill owner:

- Parent supplies selected participant admission and source phase/ticker; enroll
  only selected live owners on existing selection/reconcile hooks. incoming()
  advances Rainbow once at actual incoming criterion, with a reentrant source
  reward latch if outgoing aura can synchronously hurt its owner through some
  other mechanic. No idle global inventory scan or duplicate skill selector.
- Rainbow owns enum color and small cosmetic records; native attributes, effects,
  health/resource values and terrain keep their existing canonical owners. All
  write-capable particles/work are per-owner typed and loaded-realm bounded.
- Psychblind discovers via bounded native spatial queries for selected owners,
  retains native candidates' canonical doppeled identity, and owns typed doppel
  phase/position/rotation/owner only. Terminate orphan/removed-owner/future-epoch
  actors through the existing owner lifecycle; selection loss must not spawn a
  final charge or clear another caster's canonical claim accidentally.
- Use existing shared Work graph for visual continuations with once-per-native-
  tick readiness, typed checkpoint images and clone adapters for real gameplay
  doppel actors. Source slowraycast actors classify as projectiles under current
  LbcCloneSkills, so x2 and x4 both need deliberate managed doppel state. Final
  ordinary Charge already routes through CloneSimpleProjectileStates. Cosmetic
  displays/particles require explicit protection if represented as real bodies;
  no orphan display clone or fourfold extra terminal dispatch.
- Full coordinator image stores private color/phase/claim/control records and
  shared queued-work links, with prepare-all/install/publish order. Native player
  image owns attributes/manaregen/nano/effects and native actor image owns tags;
  do not replay entry sounds, healing, mana gain, terrain copies or new charges
  during publication. Permanent terrain uses existing shared journal/coordinator,
  not an ability-private rewind implementation. Freeze clocks and rebase cosmetic
  admission only; no last-tick catch-up burst.

## Suppression/default disagreements for parent's night backlog

Source Rainbow selection/cycle, attack/armor buffs, orange damage/FireResistance,
Aqua mana and Blue nano injection have no direct stopper/nomagic checks. Purple
and doppel/waiting visuals are NOT tagged magic. Final Psycho charge IS magic and
legacy artifact/nomagic erases it; current accepted Psycho suppression handles
owner pause, swept segment and targets. Lime actual heal uses AABB20 magic beacon
through the shared regen task. These facts do not authorize a new blanket magical
skill gate or an exception to existing shared target/role/harm admission.

Meaningful unresolved choices, for root-owned backlog only:

1. Blue code permits217->220 and preserves>=220, while source lore says limit120.
   Source-literal two-line behavior is a provisional default, not author approval.
   Also distinguish skill-generated nano charge from the existing author TECH
   suppression of armor-generated charge before reusing chargeAllowed blindly.
2. Psychblind global doppeled claim across casters/player-death reset versus a
   friendlier per-owner opportunity is real gameplay. Preserve source-global by
   default; no new approval for per-owner replacement. Initial nearest/aim repair
   is supported by displayed source intent/shared nearest rule but must be named.
3. Owner departure/skill loss should retire indefinite waiting actors through
   canonical lifecycle, while the original global claim survives appropriately;
   source lacks a clean orphan rule. Do not silently create repeat farming by
   clearing claims whenever a visual/projectile ends.

Purple permanence and destination-only Items stripping are source facts, not a
new map-renewal question. Existing BedWars normal protection remains mandatory;
no bypass is proposed. No need to reopen accepted Supernova same-match renewal,
ordinary Psycho projectile rules, or unrelated Homing/Phoenix/TMT decisions.

## Coherent qualification gates

Test a complete seven-color cycle via actual shield/absorption/environmental/fatal
criterion events; exact attributes/delta cleanup; shared item-composed lime heal;
Aqua overmanamax+500/10000 and unchanged-over10000; source Blue boundary examples;
real container support clone preserves ORIGINAL contents and strips COPY Items,
normalizes mushrooms, obeys protection and whole-coordinator terrain replay.
Run actual discovery with two real owners/teams and nonplayer/player/shop targets;
prove global original claim, gather/orbit, ONE real native100tick charge, actual
impact/cause/status/nearest shared homing, suppression, x2/x4 managed clone and
owner-leave cleanup. Nonempty full coordinator rewind must restore color/native
resources plus active gather/orbit/charge/cosmetics without regrant or duplicated
terminal. Qualify natural scheduled progression and selected-owner bounded load;
source inspection here does not prove client pixels or runtime performance.
