# Island drops — source contract, qualification pending

Current qualification (22 October2026): both variants passed the native combined two-match run. See [port contract](ISLAND_DROPS_PORT.md) for user override, final artifact and limits. Earlier pending wording below records the initial source-only audit.

22 October2026. Both item/resource leaves prepared; runtime integration/build and
actual qualification remain root work. Both payloads are complete vanilla-native
closures. Normal's larger29791cell clone is a cost/qualification concern, not an
unfinished custom dependency. No build/server/raw-source/RP edits were performed.

Source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Exact source execution

S `function/drops/specialdrops.mcfunction:41–42` independently tests clone then
clone_old. At EACH branch it selects nearest @p[gamemode=!spectator] at current
token position, then runs at that selected player's fractional XYZ. There is no
source radius/enemy-only/sight condition or invented centering/rotation.
Common current role guards still apply. Reevaluate target per branch: raise's
entity teleport can move the token itself, changing the next branch's origin.
Do not use one cached common target for combined flags.

| Contract | RAISE: drops/falled.mcfunction:1–3 | LOWER_OLD: drops/falled_old.mcfunction:1–5 |
| --- | --- | --- |
| Source clone bounds | X/Y/Z−15..+15 | X/Z−8..+8,Y−12..0 |
| Volume |31³=29791 |17×13×17=3757 |
| Destination minimum |(−15,+31,−15) |(−8,−25,−8) |
| Translation |+46Y |−13Y |
| Clear | strict-air original cube | strict-air original prism |
| Teleport selector | ALL entities distance≤16,!strict_map_object | NONPLAYER entities distance≤9,!strict_map_object |
| Per-entity teleport | relative +46Y at each entity | relative−13Y at each entity |
| Final effect |none | target resistance2seconds,amp4,hidden particles |

Clone commands omit optional filters/modes: retain native replace/normal clone,
including air, block entities and scheduled behavior. Source and destination
volumes do not overlap. Block positions use native command flooring; entity
selection and relative teleport use authored fractional coordinates. Preserve
native yaw/pitch and relative teleport semantics without invented motion/safe
arrival clearing. Teleport sphere differs from clone cube; do not replace one
with the other's AABB. Old player executor stays in place and gets resistance;
normal player executor is included in the teleport unless otherwise excluded by
standing role policy. The source has no custom reward, loot function, actor,
marker, structure, schedule or recurring hook.

Native failure ordering is important: failed clone still reaches strict fill,
teleports and old resistance. Native clone limit, loaded chunks/build bounds and
per-command behavior are distinct checks; do not silently make the whole payload
atomic. At low Y old clone may fail before later commands. Preserve attributed
source support removal and destination suffocation/fluid/fall/void consequences
under common native protections. User attribution requirements improve the
original datapack's missing owner provenance.

## Exact token presentation

S `loot_table/traps.json:666–709`: count1 repeating_command_block,weight10,
quality−10; lbc:tier_ff, lbc:drop_trap; yellow/nonitalic translated trap_clone;
one white/nonitalic trap_desc lore line; {clone:1,drop:1}. No entry conditions.
S `loot_table/legacy_luckyblock.json:2771–2788`: plain barrier,count1,empty
set_components,{clone_old:1,drop:1}; no authored name/lore/tier/model.
No full traps/legacy/imba acquisition pool is opened by standalone leaves.

`LbcIslandDropItem.Kind.RAISE/LOWER_OLD` exposes item()/flag(); class exposes
register(),matches(stack,kind),matches(stack),kind(stack). Matching accepts any
nonempty sourceflag+drop1 stack, independently per flag. Native item IDs and
public leaf basenames: island_raise_drop and island_lower_drop_old
(skywars2 item namespace, lbc:ported loot namespace). DefaultItemComponentEvents
preserves model overrides after Polymer defaults. Root owns registration and
build whitelisting. Leaf JSON parsing/static presentation review passed only.

## Current resource pack (read-only)

`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2600`: RU/RPR trap_clone
`Ловушка: Поднятие острова`, EN `Trap: Island Raise`. At2609 trap_desc RU/RPR
`Активный Дроп. Для активации выбросте предмет.`, EN
`Active Drop. Throw the item to activate.` Preserve exact author spelling,
styles, keys and original model/tier resources; no new old-token name invented.

No new author ambiguity was established: old nonplayer-only selection, normal
all-entity selection and independent failure ordering are explicit source facts.
Root must qualify current-role exclusions, combined flags/source order, original
thrower, native block-entity/scheduled-clone behavior, per-command limits,
teleport causality, time-stop/epoch/debit/quiet stop and capacity. Client visuals,
actual disk persistence and natural player outcomes require separate evidence.

## Failed-copy fallback (user, 22 October 2026)

> при таком раскладе пусть только телепортация существ исполнится, а остров не поднимется (ну и не удалится естественно), предмет удалится

This supersedes legacy continuation after a rejected copy: both variants preserve
source/destination terrain, execute only their native entity-teleport sequence,
and debit one token. Old Resistance is granted only after a successful copy.
Qualification is recorded in ISLAND_DROPS_PORT.md; source descriptions of the
original unconditional clear remain historical facts.
