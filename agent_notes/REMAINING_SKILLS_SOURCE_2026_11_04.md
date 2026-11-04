# Remaining skill source audit — 4 November 2026

Read-only audit; no Java/resources/build/server/commit changes. GodClaw23 belongs
to a separate researcher. This note does not qualify any new runtime behavior.
Read AGENTS.md relevant workflow/source-bug rules and Serena initial instructions.

## Evidence roots and recommendation

`OLD` below means the actual requested legacy source:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
`F` means `OLD/lbc/function/`; `S` means
`F/swrg_kit_integration/game/skills/`. No behavior inferred from lore alone.
RP read-only root verified from RESOURCE_PACK.md and directly read:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json`.

Best independent companions to23: **33 Champion, 34 Stone Golem, 29 Mana
Vampirism**, subject to author decisions below.33 is smallest;34 is five small
state functions;29 is three outgoing/incoming functions plus selection tag.
Reuse native loadout admission, damage callbacks/classification, mana/effect APIs,
attribute IDs, TimeStop and coordinator state. No arbitrary function interpreter.
TMT remains LAST. Protected Academy/Reroll/AcidAnt/Helicopter/RP WIP untouched.

## IDs, menu and excluded candidates

Actual skill IDs differ from translation keys. `gui/select/10000.mcfunction`
sets24 from slot24; slot25 resets skill0; slot26 navigates page10100. There are
**no selectable skill25/26/27 in this dispatcher**. Page10100 slots0..8 select
IDs28..36. Ranked select/page versions bypass advancement gates.

| ID | Actual RP name | Source entry / gate | Assessment |
|---|---|---|---|
|24|Расширение территории|10000/24; academy/dublicating|Not compact: branching block clone frontier and1106 random block definitions|
|28|Храм Хакурей|10100/0; academy/danmaku|Kill->scheduled global fresh drops->four touhou_item rolls per fresh item/orb; broad collectible family|
|29|Манавампиризм|10100/1; bows/madoka_bow|Recommend after collateral/suppression answers|
|30|Самонаводка|10100/2; academy/exahoming|Cross-cutting projectile/raycast callbacks, ownership, random homing|
|31|Истинный Феникс|10100/3; academy/fire|Lives/max-health/death conversion, not independent small passive|
|32|Радужный принц|10100/4; academy/gaycaster|42 local aura files and multiple clocks/terrain; defer|
|33|Чемпион|10100/5; no gate|Recommend|
|34|Каменный голем|10100/6; academy/stone|Recommend after counter semantics answer|
|35|Абсолютное Кино|10100/7; academy/omega_flag|22 local event files plus encounter dependencies; defer|
|36|Психодоппельгангер|10100/8; academy/psychoblind|24-radius mob extraction -> upgraded Psychoblind projectile pipeline; defer|

All abbreviated academy gates are
`lbc:true_advancements/legends/academy/<name>`; bow gate is
`lbc:true_advancements/bows/madoka_bow`. Menu source:
`F/swrg_kit_integration/gui/{select,page}/{10000,10100}.mcfunction`, ranked siblings.
GUI loot definitions `OLD/lbc/loot_table/gui/page/<page>/<slot>.json`.

##29 Mana Vampirism — full small dependency closure

`game/skill_select/select.mcfunction` clears manavampire then adds it for29;
`F/only_end_resets.mcfunction` clears tag. This tag powers victim-side advancement
independently of attacker reward; it is not sufficient to rely only on outgoing
skill callbacks for the victim debit.

1. `OLD/lbc/advancement/gowno_any_damage.json` triggers player_hurt_entity,
   blocked=false, acceptable_for_manavampirism=true, excludes interaction and
   Nexus/Imprinted. Reward `F/items/swords_or_bow.mcfunction` -> global
   `#lbcskill=1` -> `S/skills_hit_or_bow.mcfunction` (legacy=false) ->29 ->
   `S/manavampire/hit.mcfunction`.
2. `hit` temporarily tags attacker manadamager, scans **all loaded same-dimension**
   `#mobs[tag=summactivated,HurtTime:10]` and `#magic_mobs[HurtTime:10]`, no radius,
   owner, team or actual-target filter; each selected entity receives15 magic
   attributed to nearest tagged attacker. Two selections can overlap; native
   invulnerability may reject second damage. Do not promise unconditional30.
3. `OLD/lbc/advancement/gowno_player_hit_player.json` uses same acceptable tag,
   blocked=false,taken>=0, target player and dimension exclusions ->
   `F/items/player_hit_player_with_bow_or_sword.mcfunction` -> global skill toggle
   -> `S/player_hit_player.mcfunction` (legacy=false) ->29 ->
   `manavampire/hit_player.mcfunction`.
4. Attacker checks **current mana before addition** against manamax+5000.
   If already >= threshold: instant_health1second amplifier0 (ordinary vanilla
   Heal I,4HP). Else add1000mana; no clamp to threshold, so last addition can
   overshoot by999. Heal branch adds no mana. Both branches independent of target
   actually possessing1000mana.
5. `OLD/lbc/advancement/skill_manavampire_player_hit_by_player.json` triggers
   entity_hurt_player, blocked=false, acceptable tag and source entity tag
   manavampire; excludes spectator victim/Nexus/Imprinted. Reward
   `S/manavampire/damaged_by_manavampire.mcfunction`: subtract1000mana without
   floor, revoke advancement. No local toggle/legacy/suppression check here.
6. Source tag `OLD/minecraft/tags/damage_type/acceptable_for_manavampirism.json`
   =arrow + #is_player_attack; current native resource adds skywars2:cultus_arrow.
   Therefore **not arbitrary nonarrow attributed damage**, and not melee-only:
   acceptable arrows included. Plain magic damage should not recurse into this
   source tag. Source entity types from `OLD/minecraft/tags/entity_type/{mobs,
   magic_mobs}.json`; use existing native tags, not an invented undead predicate.

No MAG/TECH/artifact checks on these skill routes. Source effect is unjammable
unless author changes policy. Existing general role/TimeStop integration still
applies to native implementation.

GUI: repeating_command_block carrier, item_model lbc:mana_vampire,
white nonitalic name `lbcskill.26`, seven gray nonitalic `.lore.1..7`, statistics
index28. RP describes melee/bow steal1000, victim overload, mana above max,
large extra magic-monster damage (summons included), heal beyond max+5000.

##33 Champion — full small dependency closure

Outgoing `OLD/lbc/advancement/gowno.json` -> `F/items/swords.mcfunction:67`
(legacy=false,#lbcskill>=1) -> `S/player_hit_entity.mcfunction` ->33 ->
`S/champion/hit.mcfunction`. Advancement requires blocked=false, excludes
interaction,Nexus,Imprinted, and **excludes arrow_only**. It does not require
true melee or held sword: owner-attributed nonarrow damage can trigger it.
Source arrow_only contains minecraft:arrow; native tag can extend it.

`hit`: iron_golem.hurt sound pitch1, iron_block particles20; add armor+20 ID
7374yg374g347g37gh34 and toughness+5 ID7374yg3274g347g37gh34; remove attack ID
g8wu4g8k4wug84khu84hu48h. Existing IDs make repeated state idempotent rather
than stacking (command duplicate-add failure does not stop later lines).

Incoming `OLD/lbc/advancement/cocksuckers.json` -> `F/items/damaged.mcfunction:19`
(#lbcskill=1) -> `S/damaged.mcfunction` ->33 ->`S/champion/damaged.mcfunction`.
Advancement excludes spectator,Nexus,Imprinted but has **no blocked=false or
positive/min-damage condition**, so shield/resisted source hits may change state.
`damaged`: wolf.growl pitch2,crit particles20; remove armor and toughness IDs;
add attack+6 under the attack ID above. No timeout/decay/charge count.
No initial33 state in skill selection and no local jammer checks on either route.
Native implementation must clean all three modifiers on switching/death/reset;
repeated hits cannot stack and callback reentrancy/order needs preservation.

GUI: ordinary shield carrier, no custom item model in source definition;
white nonitalic `lbcskill.30`,three gray nonitalic lore1..3,statistics index32.
RP: received damage+6attack; outgoing hit+20armor/+5toughness; each replaces other.

##34 Stone Golem — full small dependency closure

`F/players_1s.mcfunction:22` ->#lbcskill=1 ->`S/skills_tick_1s.mcfunction`
(legacy=false) ->34 ->`S/stone_golem/tick_1s.mcfunction`.
Increment stone_cd while<=9 and stone_layers<=4; at>=10 add one layer/reset cd0
via `add_layer`,stone.break and50stone particles. Cap5. Exactly ten **one-second
dispatches** per new layer; do not implement as10ticks. Each second refreshes
resistance2seconds amplifier(layer-1),hidden particles. Lower-layer resistance
can remain temporarily shadowed by stronger existing potion until expiry.

Incoming route same cocksuckers/items/damaged/skills/damaged path as33. Always
reset stone_cd0, then remove **at most one** layer using return-run:

| Current layer | Removal condition |
|---|---|
|1|damage_taken>=80 (8HP statistic)|
|2|damage_taken>=20 (2HP statistic; actual RP explicitly agrees)|
|3|blocked_damage>=80 (8HP resisted)|
|4|blocked_damage>=100 (10HP resisted)|
|5|chance50 on received-damage callback|

`blocked_damage` objective is **minecraft.custom:damage_resisted**, not shield
blocking. `damage_taken` is minecraft.custom:damage_taken. Source statistics use
tenths HP. `items/damaged` clears blocked_damage after skill callback, schedules
`items/damaged_scheduled`2t; that function clears damage_taken for **all players**.
Thus taken-damage thresholds may accumulate recent same-player damage and global
scheduled resets can interfere. This is suspicious source plumbing; ask whether
native uses actual event damage instead. Resistance V can prevent HP damage while
resisted events still matter; use correct native resistance-stage callback,
not only CombatTracker positive final HP damage.

`remove_layer`: decrement1,iron_golem.hurt pitch1.5,60iron_ore particles.
`F/leave_death.mcfunction:389..390` resets both counters0. No local suppressor
checks. Actionbar `F/ui/get_data.mcfunction:80..81`: layer glyphU+E205 white,
optional gray ` | ` and recharge score. Native TimeStop must pause recharge and
resistance refresh consistently, with typed coordinator snapshot state.

GUI: stone carrier,item_model lbc:stone_golem,white nonitalic `lbcskill.33`,seven
gray nonitalic lore1..7,statistics index33. RP agrees on10seconds/max5,
8/2HP taken,8/10HP resisted,50% at5.

##24 and other scope warnings

24 `expansion_tick`: hold sneak60ticks; release resets initial charge, active
charge increments; at>=80active ticks per tick20%chance to end; release ends.
Cooldown decays1 on50% unsneaking ticks; requires0 to restart. Start creates
stone when needed/four clones and four cardinal magic frontier markers.
`expansion_tick_marker` deletes container Items,normalizes mushroom faces,
dispatches main/side cardinal copies,propagates owner/team,then kills old marker.
Main produces forward+two lateral children and10% downward branch; side advances
forward+10% downward. Clones only into airs_only. Each marker1%chance random block
via place_random_block -> random1..1106 -> storage macro -> blocks/<n>.
This closes over a large terrain palette and container-destructive semantics;
do not sneak it into a tiny companion batch. Magic-tagged marker suppression
requires shared raycaster audit if24 later selected (no full24 qualification here).

28 `player_killed_entity` adds danmaku_score_spawn and schedules1t; function runs
for tagged players,then globally selects item/XP Age0/1,spawns four touhou_item
outputs per selected fresh drop. This is a broad global-source attribution issue.
31 max-health base/lives conversion and death hook halves health;32/35/36 have
cross-family dependencies above. They are triaged, not exhaustively closed here.

## Current native support and author decisions

Catalog `src/main/resources/skywars2/lbc_loadouts.json` has1,3..22; no29/33/34
entry. Bounded exact searches in LbcLoadouts/LbcContainedSkills/LbcMana found no
implementation of these named states. Serena inspected LbcLoadouts.hasSkill
(lines238..248): existing shared role/alive/map/config/stage/gate admission is
appropriate and does not itself query suppressors/advance clocks.
LbcContainedSkills.recordDamage handles skill12 final unclampedHP receipt only;
do not reuse that alone for Stone resistance or shield-source callbacks.
LbcMana.add delegates get+set; existing acceptable damage/native entity tags
are available for reuse. Do not reimplement a tag/classification registry.

Required author questions before dependent implementation:

1. Suppression policy for29 outgoing reward,29 victim debit,33 switches/modifiers,
   34 recharge/layers/resistance: preserve source unjammable, or MAG/TECH policy?
2.29 extra15magic: restrict to actual struck eligible mob, or preserve global
   HurtTime10 scan collateral? Native bounded actual-target route recommended.
3.34 preserve odd2HP layer2 threshold? Actual source AND RP agree2, so no
   unsupported claim of a10HP lore discrepancy. Separately, replace shared
   two-tick cumulative damage_taken plumbing with actual per-event HP damage?
4. Confirm33 uses source broad nonarrow damage rather than true melee-only;
   source incoming includes shield/resisted callbacks. These are material
   trigger semantics, not implementation details to silently narrow.

For29 cap overshoot, victim negative mana, and heal4HP, preserve explicit source
unless author requests changes. No arbitrary additional balance question needed.
