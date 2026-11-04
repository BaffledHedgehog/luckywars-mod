# God Claw / Ладонь Бога — finite source audit, 4 November 2026

Read-only source audit; no Java/resources/RP edits, builds or server operations. Serena initial instructions and agent_notes/AGENTS.md read. No runtime qualification is claimed. User annotations elsewhere preserved. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc` (paths below relative to it). Actual datapack behavior takes priority over speculative Paper fallback.

## Exact source closure

- `function/players_tick.mcfunction:46` invokes skills when #lbcskill=1; `swrg_kit_integration/game/skills/skills_tick.mcfunction:1,9` rejects legacy and dispatches skill23.
- `god_claw_tick.mcfunction:1..8`: idle sneaking charges60 ticks; releasing during initial charge resets0. Start at60 adds active AND cooldown tags. Next30 sneaking ticks advance61..90, invoke active every tick including90, then end. Active release ends immediately, after same-tick chance20 cooldown debit. Cooldown only decrements outside sneak with independent20% probability per tick: 90 costs450 non-sneak ticks on average; holding sneak during cooldown freezes it. No reset to fixed cooldown on end. At0 remove cooldown tag. Source objective created load:287, removed load_scores_reload:189.
- `god_claw_start.mcfunction:3..6`: one marker centered at floor(x),floor(y-.5),floor(z), plus(.5,.5,.5), tagged slowraycast/player/god_claw/magic. Copies owner lbcID2 and team_number; final spawned-tag selector accidentally covers ALL unspawned GodClaw markers in dimension.
- `tick_not_timestopped.mcfunction:3` -> `raycastselector.mcfunction:7` -> `raycastselector_player.mcfunction:4` -> `god_claw_tick_marker.mcfunction`. Once per unstopped actor tick; source excludes Nexus and Imprinted. Each processed marker ends killed. Source selector snapshot means newly spawned markers normally form next frontier.
- Marker line1 UNCONDITIONALLY replaces its current block with red_mushroom_block[down=false,east=false,north=false,south=false,up=true,west=false], even seed air, water, bedrock or container. No restoration path in GodClaw. Six cardinal neighbors always attempted; other20 cube neighbors each independently40%. Each requires non-airs_only block, not exact consumed state, and no GodClaw marker within.2. Source excludes only exact mushroom STATE. Queued cells recheck no eligibility before own conversion. Global competing-owner duplicate exclusion lets first claimant win.
- Marker lines32..36 copy owner and team from current marker to all unspawned GodClaw markers within2. Child radius2 includes all26 neighbor centers. Line38 contacts all nonplayer entities within2, except god_claw and clawed_entity; no mob/living restriction, faction/team/owner filtering, inventory or role protection. Killing current marker follows contact.
- `god_claw_tick_marker_mob.mcfunction`: item entity produces exactly ONE Liquid Abyss repeating_command_block with custom flag liquidvoid:1, model lbc:liquidabyss, tier_ff, source name/lore; original entire stack is killed. Mob-tag entities produce one clawed_entity silent persistent invisible skeleton,40HP/maxHP,10attack, source void leather equipment, guaranteed voidman death table, then original is killed. Other nonplayers are simply killed, including projectiles, XP, unrelated marker/displays, own actors and item containers. Source kill is not proof of discard equivalence: native death/drop hooks can matter. Clawed outputs avoid recursive contact.
- `loot_table/voidman.json`: guaranteed1 Liquid Abyss plus independent10% lbc:sauvojen_tuntija_blind pool (weight10,quality8). Must reuse entire existing native voidman definition. Existing mobs could drop contents on kill; source contains no explicit preservation/transfer. Block-container replacement has no GodClaw inventory recovery code.
- `god_claw_active.mcfunction:1..6` counts ALL currently loaded owned markers in dimension as tmp1 (NOT boolean, NOT lifetime block count). If zero, end is called but function does NOT return: random choice/feedback still occur. Uniform1..26 choice per active tick; permanent attribute-base increase proportional to frontier count; no temporary modifier. Portal particles30 force all at owner y+1. At90 end runs after gain. `god_claw_end.mcfunction` removes own markers by owner ID, removes active, retains/adds cooldown tag. No block reversal or attribute reversal.
- `other/stopper_magic.mcfunction:5` destroys magic-tag entities within20; artifact/nomagic:7 destroys within25. Thus frontier can be destroyed by actual source magic jammers; no start/input check inside skill. Other magic-clearing actors (danmaku bombs) also kill frontier. Native port must resolve jammer contract explicitly.
- `leave_death.mcfunction:411` resets score0 but GodClaw-specific search finds no tag/owned-marker cleanup there. End cleanup scans currently loaded dimension markers only; switching worlds/offline/skill swap need explicit lifecycle decision and typed native cleanup.
- `ui/get_data.mcfunction:76` shows U+E166 and dark_purple raw god_claw_cd when positive (including charge/active/cooldown). `loot_table/gui/page/10000/23.json` supplies repeating-command-block icon model lbc:god_claw, lbcskill.23 and five lore keys, win_count_skill[22]. Existing generic menu owns actual selection.

## Exact attribute arithmetic

For count n, new base = (trunc(old base*S) +/- n)/S, signed scoreboard int arithmetic before conversion; normal native attribute sanitization still applies. Source update functions merely set base. Do not reuse LbcAttributeGains.apply mapping/scales: that is a different reward family. Reuse LbcAttributeChanges.change with below values instead.

| choice | attribute | S | sign |
|---|---|---:|:---:|
|1|armor|100|+|
|2|armor_toughness|100|+|
|3|attack_damage|1000|+|
|4|attack_knockback|1000|+|
|5|attack_speed|10000|+|
|6|burning_time|10000|-|
|7|explosion_knockback_resistance|100000|+|
|8|fall_damage_multiplier|100000|-|
|9|jump_strength|10000|+|
|10|knockback_resistance|100000|+|
|11|luck|5000|+|
|12|max_health|1000|+|
|13|movement_efficiency|100|+|
|14|movement_speed|100000|+|
|15|oxygen_bonus|100|+|
|16|safe_fall_distance|100|+|
|17|scale|10000|+|
|18|step_height|500|+|
|19|water_movement_efficiency|10000|+|
|20|block_break_speed|1000|+|
|21|block_interaction_range|10000|+|
|22|entity_interaction_range|10000|+|
|23|mining_efficiency|100|+|
|24|sneaking_speed|1000|+|
|25|submerged_mining_speed|100|+|
|26|sweeping_damage_ratio|1000|+|

Each row source is `function/other/attributes/<attribute>_up_godclaw.mcfunction:1..4`; choice order is `god_claw_active:8..33`. Two subtraction outcomes improve their attributes. Zero-count call can still quantize existing base.

## Exact displayed source text

`plugins/luckywars/resourcepack_lang/ru_ru.json:4678..4683` (outside source root):
«Ладонь Бога»; «Зажимая Shift, можно активировать поглощение»; «Поглощение обесценивает всё, до чего дотягивается»; «Поглощённые блоки незначительно повышают атрибуты»; «Поглощённые мобы (не игроки) становятся пустыми»; «Поглощённые предметы становятся жидкой пустотой».
English name is Claw of God. Language source is evidence only; preserve author RP untouched.

## Decisions needed before implementation

1. Confirm MAG frontier destruction under normal magic jammer/artifact/bomb clearing; input/charge/gain outside frontier need distinct author ruling. TimeStop must pause entire native cycle under existing project contract.
2. Confirm destructive block set includes bedrock/water/containers, permanent mushroom terrain and no refunds; confirm entire dropped stack becomes1 Liquid Abyss, own/team drops and owned actors eligible. Explicitly decide native player-owned stored contents and family visual removal rather than silently dropping/duplicating them.
3. Source kills ANY nonplayer in contact. Protect administrative role objects as required by current project contracts, preserve eligible WILD entities and co-remove family visuals. Ask separately for this ability whether Chef wtf_item causes immediate elimination; source selector alone cannot establish intended Chef native semantics.
4. Confirm player exclusion, hostile void skeleton behavior and attribution of transformed mobs/projectiles/drops. Existing user rule REQUIRES owner inheritance for all direct/indirect kills despite source gap; this is not optional.
5. Confirm frontier-count gain, thirty uniform gains, zero-count final quantization, random cooldown and sneak freeze; these are actual source, not lore's apparent cumulative-block description.
6. Resolve cleanup after release/death/leave/swap/world transition; fix global transient-tag owner race and loaded-only orphan marker behavior with owned native state. Boundaries must preserve source competition between owners, no global marker scan.

## Feasible native reuse / limits

Finite skill23 is independently feasible; no TMT or general function interpreter needed. Reuse exact `LbcAttributeChanges.change`, existing AttributePolicy/negative attribute mixins and normal match attribute lifecycle; existing Liquid Abyss/LbcInventoryHazards and entire native voidman loot/mob family; existing wands jammer service and per-world LbcTimeStop; shared EffectTerrain fair budget/deadline, terrain checkpoint and typed full coordinator integration. LbcPlanetoidSkill establishes nearby skill time-stop admission pattern. Root owns concrete integration and qualifications.

Implement an owned per-world generation frontier with position index (cross-owner duplicate exclusion), event-local radius2 entity query, bounded fair per-cell work and cached canonical outputs. Persist owner/team/provenance and frontier generation counters; never scan global markers or whole NBT each tick. If budget spans ticks, define logical frontier/count timing explicitly so x2/x4 workload does not silently inflate gains. Track temporary ticket lifetime and typed pending work for rewind. Source has potentially exponential26-neighbor frontier over connected terrain; calling it compact does not remove scheduling/ownership requirements. Whole-coordinator tests must include nonempty frontier plus terrain/containers, conversions, transformed owned entities, competition, cooldown, death/leave/swap, TimeStop and ordinary positive jammer controls. No runtime success or full mode completion claimed here; TMT remains LAST.

## Read-only native review follow-up

Reviewed initial LbcGodClawSkill.java and GodClawSkillChecks.java after creation, without edits/build/server. Found concrete source mismatch: item contact outputs intentionally omitted clawed_entity, whereas source god_claw_tick_marker_mob first line explicitly tags the new item clawed_entity. This causes repeated re-conversion, new output allocations, owner/motion/pickup-delay reset across adjacent frontier contacts. Root notified to add permanent source immunity. Checks should assert exactly one output and identity survival through later generations, rather than merely nonempty singleton stacks.

Generation current/next queues,26-neighbor coordinates, cross-owner claimed-position exclusion, copied RNG/cursor and pause-before-next-gain under backlog are internally coherent. Root remains responsible for actual family-removal policies, canonical VoidMan admission, lifecycle/checkpoint registration and author jammer/Chef rulings. No runtime qualification inferred from source review.
