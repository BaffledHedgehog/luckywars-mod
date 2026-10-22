# Next complete acquisition after Library — Bees source audit

22 October 2026. Read-only recommendation; no implementation or qualification.
Source root S: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

Recommend the normal/old Bees token pair. S `function/drops/specialdrops.mcfunction:45`
invokes `drops/bees` at the dropped item's fractional position, with no nearest-player
relocation. The normal and legacy tokens share `{bees:1,drop:1}` and the identical
payload. No native Bees dispatcher branch exists in `LbcSpecialDrops.java` at audit.

## Complete closure

S `function/drops/bees.mcfunction:1–10` is the entire payload:

1. Ordinary setblock oak_fence at token position (omitted mode means replace).
2. Setblock beehive[honey_level=5] at +(0,1,0), explicitly `destroy`.
3. Seven native bees, each HasNectar:1b and DeathLootTable:"lbc:honey":
   +(0,1,1), +(0,1,-1), +(1,1,0), +(-1,1,0), then three at +(0,2,0).
4. Broadcast a two-part component: nearest nonspectator player selector in gold,
   followed by yellow `now_beekeeper`. No explicit separator beyond translated text.

No bee anger, target, persistence, custom health, NoAI, owner, lifetime, custom tag,
rotation or hive assignment is authored. Preserve native defaults/AI, including
hive entry/exit, sting/poison and native death; do not pre-anger or tame the bees.
The three overlapping upper bees are explicit independent summon commands; retain
all seven rather than inventing spread. The hive has no authored bees NBT.
The fence uses ordinary replacement, the hive uses destroy: do not convert either
to Library's strict no-drop placement. Block-entity contents and destroyed-block
loot must follow the existing native displacement contract.

S `loot_table/honey.json:1–62` closes the sole custom reward dependency entirely in
vanilla items: one pool, uniform rolls1..4 and uniform bonus_rolls0..4; four equal
weight1 entries, each count1: honey_block, honey_bottle, honeycomb, honeycomb_block.
No conditions, custom component/item, nested table, mechanic or custom function.
Keep native luck/bonus-roll evaluation and per-bee loot context; do not substitute
one fixed reward or combine seven bees into one death roll. The project already
contains this table; its presence is not proof the acquisition/AI is implemented.
A bounded search found no bee-specific recurring function hook beyond the summons.

## Exact acquisition presentation

S `loot_table/barriersnottraps.json:358–401`: repeating_command_block count1,
weight10/quality-5, tier_f/drop_not_trap, yellow nonitalic `drop_bee`, one white
nonitalic `trap_desc`, `{bees:1,drop:1}`; no entry conditions.
S `loot_table/legacy_luckyblock.json:1484–1503`: plain barrier,count1, empty
set_components, same flags; no name/lore/tier/model authored.
S `loot_table/imba/barriersnottraps.json:182` has a separate imba-tagged occurrence;
its presentation was not promoted or fully audited. Keep all whole pools gated.

Read-only current RP root:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2618`: RU/RPR `Дроп: Пчёлы`, EN
`Drop: Bees`. At372, RU/RPR ` теперь пчеловод`, EN ` is now a beekeper`.
Preserve leading spaces and EN author spelling. Reuse original `trap_desc` and
existing tier/model resources; no new translation namespace is justified.

## Implementation and qualification boundary

Reuse shared source-order/one-debit/role/load/time-stop/epoch handling and attributed
ordinary block placement. Do not invent enemy-only targeting or magic gating.
Track actual thrower through hostile retaliation, bee stings/poison, knockback,
fall/void and terrain consequences; native bees also need role/ally policy guards.
The source attribution gap is fixed by the standing user requirement.

Native hive storage deserves focused inspection before coding: custom death loot
and native bee state must survive entry/exit and hive-item transfer where vanilla
persists them, without leaking owned actors after epoch/stop. Exercise anger at
hive destruction, sting poison/death, lethal attribution, seven separate rewards,
luck, hive storage/exit and quiet match cleanup. Do not turn native breeding into
an invented guarantee that offspring inherit custom death loot. Actual persistence
and client rendering remain separate evidence limits.

No new author question was established by this bounded audit. If native hive
lifecycle forces a semantic change, ask about that concrete conflict before
changing it. The unusually explicit overlap and hive destroy mode are recorded
rather than silently normalized.

Infinity/forest remain deferred reward trees; magicbean reaches lucky_items_1;
Pizanski has custom skill/reward/boat dependencies; shulkerbullets needs a target
and projectile lifecycle. None is promoted by this note. Completed drops are not
reselected. Bees is a recommendation for the next turn, not an opened leaf.
