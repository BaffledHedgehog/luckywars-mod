# Bees drops — qualified

22 October 2026. Normal and old Bees drops are implemented and qualified in two
actual isolated mode1 matches. The tested build is ready, not installed.
Payload source: [bounded audit](NEXT_DROPS_AFTER_LIBRARY_SOURCE.md).

## Item/source contract

Read-only source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
S `loot_table/barriersnottraps.json:358–401`: repeating_command_block count1,
weight10 quality-5; `lbc:tier_f`, `lbc:drop_not_trap`; yellow nonitalic `drop_bee`,
one white nonitalic `trap_desc`; `{bees:1,drop:1}`; no entry conditions.
S `loot_table/legacy_luckyblock.json:1484–1503`: plain barrier count1, empty
set_components and same flags; no authored custom name/lore/tier/model.

`LbcBeeDropItem.Kind.NORMAL.item()` = `skywars2:bees_drop`;
`Kind.OLD.item()` = `skywars2:bees_drop_old`. Both expose `flag()` = `bees`.
`register()` installs default item-model overrides through DefaultItemComponentEvents
so Polymer keeps the authored models. `matches(stack)` accepts any nonempty stack
with both source flags, preserving source dispatch compatibility; unflagged vanilla
lookalikes fail. `kind(stack)` returns OLD only for a matching native old item,
otherwise NORMAL for matching stacks, null for nonmatching stacks.
`matches(stack,kind)` compares that result.

Standalone leaves `lbc:ported/bees_drop` and `lbc:ported/bees_drop_old` each return
one native token; pool acquisition weights are deliberately not activated. Both
leaves and internal `data/lbc/loot_table/honey.json` are explicitly whitelisted
in the build. No whole pools or unfinished game modes are opened.

## Complete reward resource

Existing `src/main/resources/data/lbc/loot_table/honey.json` is byte-identical to
S `loot_table/honey.json:1–62`, SHA-256:
`8020D2E78250A658769A8EA7815695051B96FC8B9EBE69F8B9838DE9C0AC8D7A`.
No resource conversion or edit was needed. One pool uses uniform rolls1..4 and
uniform bonus_rolls0..4, with four equal weight1 count1 entries: honey_block,
honey_bottle, honeycomb, honeycomb_block. No custom items, nested tables,
conditions, or custom components. Native luck-dependent bonus rolls and actual
bee death loot context must remain; do not hand-compute fixed rewards.

S `function/drops/specialdrops.mcfunction:45` dispatches at fractional token XYZ,
not nearest player's XYZ. S `function/drops/bees.mcfunction:1–10` places ordinary
fence then destroy-mode honey_level5 hive, summons seven native nectar bees with
that death table (including three independent bees at the same upper position),
then broadcasts nearest nonspectator gold selector plus yellow `now_beekeeper`.
`LbcBeeDrop` owns native actors and hive state, with shared attributed terrain,
role guards, source ordering/unit debit, epoch/time-stop and quiet cleanup.

## Resource-pack evidence (read-only)

Current pack:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2618,2609,372` already supplies:

| Key | RU/RPR | EN |
| --- | --- | --- |
| drop_bee | Дроп: Пчёлы | Drop: Bees |
| trap_desc | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |
| now_beekeeper | ` теперь пчеловод` | ` is now a beekeper` |

Leading spaces and original spelling are retained through translatable components.
Existing `assets/lbc/items/drop_not_trap.json` points to `lbc:item/drop_not_trap`.
Old token uses native barrier presentation. No pack, template, translation or
shared integration files were written by this item/resource task.

## Native lifecycle and attribution

The source-order branch follows Lucky Pigs and precedes Enzan. All required
chunks are preflighted without synchronous loads. The dispatcher debits one unit
per active tick; fake give visuals, stale epochs and paused time do not activate.
The authored seven offsets, including three overlapping upper bees, are retained.
No taming, anger, target, custom AI, lifetime, health or invented hive binding is
added. Native replacement uses flags258 for the fence; the hive first destroys
its previous block with loot, then uses the same native replacement flags.
The tested lower container is removed without contents; the destroyed upper
container ejects its inventory and block loot. No manual spill override is added.

Persistent entity tags retain the actual author's UUID and match epoch through
native hive serialization; native hive code deliberately replaces the entity UUID.
`DeathLootTable` and nectar are preserved by native storage. The shared killer
resolver recognizes these bees for native sting damage, accepted sourced poison
and resulting fall/void consequences; later accepted attacks keep priority.
An author cache preserves team checks through same-match disconnection; current
online handles are preferred. Existing offline reward limits are not expanded.

`BeeDropMixin` guards native stings before damage/poison and observes `storeBee`.
Native entry, NBT loading and BEES component application all use that method.
Stale managed occupants are rejected there, while ordinary occupants pass through.
Mob target guards allow clearing targets and protect observers, service NPCs and
allies; native retaliation against the thrower remains possible, like Lucky Pigs.
Managed bees and touched hives are tracked by direct handles, without world scans
or new chunk tickets. Match stop quietly discards managed live bees and removes
only managed stored occupants. It does not emergency-release the hive near fire:
modified resident chunks are marked dirty directly. Ordinary stored bees remain.
Unloaded actors detach; persisted epochs reject stale actors on later admission.

## Qualification

Build and two actual isolated mode1 matches passed on `8a972305`: 84 recorded
suite results, 85.58 seconds including boot, maps and all earlier special-drop
regressions. Both public leaves/native prototypes and Polymer roundtrip passed.
Native checks cover exact seven positions/defaults, actual seven independent
deaths/honey loot, luck, ordinary replacement versus destroy inventory handling,
hive entry/storage/emergency exit/new UUID, preserved author/loot/nectar and BEES
component roundtrip. Actual hive destruction triggers native retaliation.
Disposable native victims exercise lethal stings and subsequent fall/void,
including newer-attacker priority, self/allied/role guards and accepted poison.
Native poison is not claimed to kill naturally. Source-order multi-flag debit,
actual dropped-stack ticks, freeze, load callbacks and stale admission also pass.

The asynchronous stop probe retains prior probes, waits an actual tick for native
admission and freezes its real live/stored actors. Actual match stop removes six
managed live bees and the stored managed bee while preserving an ordinary occupant,
without XP grants or pending-token debit. Full block-entity/player snapshots and
isolated JAR/config/profile/scoreboard backups are restored; both locks released.

Ready artifact: `build/verification/skywars2-bees-drop-8a972305.jar`, SHA256
`8a97230578a3b390334e976cbe41ab13ba59c9e30016eb2f19cf3b48396fa533`.
185 Java sources, 540 compiled classes and 2676 processed resources match tested
source/release JARs. Evidence: `build/verification/bees-drop-final/qualification.json`,
results and server log. Main JAR96ee50a7/user Reroll6ef80947/RP/template unchanged.
Only the established fake-bot Polymer packet-context ERROR pair remains.

Limits: real client rendering/chat packets, natural foraging/pathfinding timing,
actual disk restart/unload, physical hive-item placement, delayed post-sting
self-death and disconnection are not fixture-qualified. Hive transfer exercises
the real BEES component and native storeBee API. Disconnection safety and native
post-sting behavior are source-reviewed. No peak capacity benchmark or new offline
coin framework is claimed. Next: [OP Anvil audit](NEXT_DROPS_AFTER_BEES_SOURCE.md).
