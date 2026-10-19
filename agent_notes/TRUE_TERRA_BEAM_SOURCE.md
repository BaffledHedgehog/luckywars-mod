# True Terra beam source audit

The legacy datapack source is under `world/datapacksdisabled/LBC`, so this copy is explicitly disabled and does not establish live runtime behavior. A generated `.lwfn` copy exists under `plugins/luckywars/build/resources/main/legacy-datapacks/LBC`; that shows packaging only. The corresponding Paper source tree has no `true_terra_sword_hit` function.

## Legacy beam path

- `data/lbc/function/workingitems/sauvojen_staffs/true_terra_sword_hit.mcfunction` summons a `slowraycast` marker tagged `terra_ch`, `rare`, `shield_blockable`, and `magic`, with lifetime 50; it copies rotation, owner ID, and team number.
- `data/lbc/function/raycastselector_rare.mcfunction` routes `terra_ch` to `lbc:workingitems/awaken_tuntija_wands/terra/tick_awaken`. It does not use `lbcr:raycast_terra_fast` (that route is for `fast_terra`).
- In `data/lbc/function/workingitems/awaken_tuntija_wands/terra/tick_awaken_loop.mcfunction`, each call increments `Distance` first. At `Distance=4` the fourth recursive call starts at `.99`, advances the marker another `.33` to `1.32`, and returns before damage or particles. Thus each tick has three damage/particle phases, at positions 0, `.33`, and `.66`, with total movement 1.32.
- Each phase calls `lbcr:damagewithid_9` separately on three collision lanes: center, then lateral `+.6` and `-.6`, with each side lane shifted backward `.25`. Each lane selector uses `dx=.3,dy=0,dz=.3` and the damage helper considers up to three mobs. The nine visual dust points use lateral offsets `0, ±.2, ±.4, ±.6, ±.75` and backward offsets `0, .07, .15, .2, .25`; the visible outer dots extend beyond the three collision lanes. A mob intersecting multiple lane selectors can take a separate hit from each lane.
- `data/lbcr/function/damagewithid_9.mcfunction` marks the owner and teammates, then runs `damage @s 5 arrow by <owner>` and removes the temporary tags. The legacy `data/minecraft/tags/damage_type/bypasses_cooldown.json` includes `minecraft:arrow`, so these separate arrow damage calls bypass hurt cooldown. The `nodamage`/`nodamage1` tags in `dmgwithid_un_playeronly.mcfunction` serve the follow-up player-only routine; they are not this beam's cooldown mechanism.
- This is distinct from `data/lbcr/function/raycast_terra_fast.mcfunction`, which advances by `.3`, traverses up to 25 steps, and invokes `dmg_bypl_11` for 17 cactus damage.

## Java history and requested behavior

The earlier Java beam implementation did one spatial query and at most one 5-damage `hurtServer` hit per entity per tick using ordinary arrow damage. Vanilla hurt cooldown could suppress it after the preceding melee hit. That is historical behavior before the current correction, not a description of the requested implementation.

The user clarified the target behavior: **(user)** «сохрани старую кратность урона но уменьши этот урон с 5 до 1 за тик» — preserve the old hit multiplicity while reducing each hit from 5 to 1 per tick. The correction being implemented uses three swept intervals and three separate collision lanes (center and ±.6, side lanes back `.25`), with 1 damage per lane hit. This preserves up to nine separate 1-damage hits per target per tick where its native hitbox intersects all three lanes across all three phases. A `.3` native-hitbox padding extends collision coverage toward the visible outer dots; repeated lanes remain separate damage events by design.
