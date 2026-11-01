# Ordinary / Neko / Loli Lucky Block opening source audit — 1 November 2026

Read-only bounded audit; only this note written. No builds, tools, server actions or source/resource changes. Current release baseline is e67403fb (PhD complete); older notes describing completed kits, dungeon, or Chaos dependencies as missing are historical. Kit6 and modes2..4 remain unavailable.

Paths: `legacy` = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data; native paths are repository relative. All legacy searches used --no-ignore. Source inspection is not executed qualification.

## Actual entrypoint and existing native support

Opening is ordinary native block breaking, not a dedicated right-click function. Three legacy `minecraft/loot_table/blocks/` overrides recognize exactly six false face properties: red_mushroom_block (ordinary), brown_mushroom_block (Neko), mushroom_stem (Loli). Their corresponding source item tables `lbc/loot_table/luckyblock_block_only.json`, `luckyblock_neko_block.json`, `luckyblock_loli_block.json` supply that block_state component for vanilla placement. Model/name alone does not classify a placed block.

**All three override source files already exist** in `src/main/resources/data/minecraft/loot_table/blocks/`; the canonical Oct31 loot expansion retained their source definitions. However build.gradle processResources uses explicit includes and does not include data/minecraft/loot_table/blocks; parent confirmed packaged build omission. They also retain old loot schema. Thus source presence is NOT active native opening support. Port native block/item/drop integration or deliberately convert/package only scoped overrides; no function interpreter is needed. `src/main/java/me/skywars2/mixin/CreativeBlockDropsMixin.java:37-69` retains vanilla destroyBlock drops even for ordinary Creative players, excluding developer/observer; it also rejects frozen/native blocked destruction/use. `SkyWars2.java:336-365` rejects observer/action-blocked attack/place and otherwise passes vanilla handling. `LbcItems.java:1110-1113` defines those blocks as Vanity, polymorph, frost, stun, time stop, FPV control; MAG/TECH are not general block-opening gates.

## Break outcomes and luck

Retained red override `:9-38` first rolls legacy_luckyblock once when fixed `legacy lbc.math=1`, even with Silk Touch. `:44-109` otherwise rolls luckyblock `luck_amount` times when legacy=0, all-false and no Silk Touch; `:111-187` supplies one fallback roll only when THIS_ENTITY does not have score luck_amount >= -999. Missing score is distinct from present zero/negative score. `:190-229` Silk Touch emits luckyblock_block. Normal shaped red mushroom fallback is excluded also for the all-false-except-up=true special state (`:248-277`); final special pool is empty (`:288`).

Retained brown override `:5-129` similarly rolls nekoluckyblock by luck_amount or fallback once, without legacy gate; `:132-194` Silk Touch returns luckyblock_neko_block. Its last pool additionally competes with tmtrainer_drop at luck_amount>=16, enormous weight100000000000000000 (`:233-246`). Retained mushroom_stem `:5-120` follows the same ordinary/fallback no-Silk pattern into loliluckyblock; `:123-185` Silk Touch returns luckyblock_loli_block. Non-special brown/stem shapes retain source red_mushroom fallback with random count -6..2 and explosion decay. No manual rounding/clamping replacement is warranted.

Entry output functions apply Fortune ore_drops followed by max99 per generated stack, not an aggregate cap over all rolls. Keep ordering and all pools. Legacy `lbc/function/players_2t.mcfunction:1-2` refreshes luck_amount from integer native luck attribute +1. Native search found luck_amount only in KFC's approved score ledger (`LbcKfcFood.java:58`), not an equivalent periodic refresh. This is a concrete integration gap to verify, rather than evidence all loot definitions are absent. Native COMMAND helper `LbcLoadouts.java:400` has a different context from BLOCK breaking: do not use command loot or assume THIS_ENTITY alone forwards context luck. BLOCK native context and explicit roll-count score require separate checks.

Global final_luck_modifier is categorical and independent of player luck. `legacy/lbc/loot_table/luckyblock.json:9-43` includes weighted lucky_items/high_quality and modifier12 imba; later modifier-conditioned branches remain source data. Native modifier owner is already qualified in LUCKY_MODIFIERS_PORT.md and applies only to LuckyWars. Do not enable it in ordinary mode or reopen answered config policy.

## Callers, follow-up consumers, ownership

Direct source outcome callers besides block overrides: `lbc/function/workingitems/spell1_default.mcfunction:45` and `spell1_default_left.mcfunction:22` roll ordinary luckyblock for wand; `drops/evilluckyblock.mcfunction:1-9` assigns ordinary table as mob death loot; `other/grimoire_dungeon/room_rewards/luckyblock_mobs.mcfunction:1-3` does likewise. Infinity tables, random_glack_1/random_item and other nested loot references can also enter these outcome tables: retain the shared loot graph, not a private block-only duplicate. Item-producing callers include other/give:1-3, absurd_child:6 and absurd_child_tick:6-8, room_rewards/luckyblocks, luckyroom:7-10, greed/fill:2,5,8,11, and luckyblockspawn:2 (all-false block writes). This list is bounded caller evidence, not a claim to have enumerated the entire recursive loot graph.

A roll produces stacks; it does not itself execute drop flags. Legacy `lbc/function/items_tick.mcfunction:1-2`, `items_tick_tag.mcfunction:5,19`, `items_tick_act.mcfunction:1`, `drops/specialdrops.mcfunction` form the immediate dropped-item execution chain. A stack can be ordinary equipment, another Lucky Block, a transformable, or a drop/encounter token. Actual native consumers must be checked by raw custom_data and registration, not by table presence. Many are already ported, including evil actors/dungeon/Uni and PhD; do not mark them missing from an older source audit.

No block loot definition carries placer identity into the placed block. A dropped reward's native thrower/causal owner is distinct from nearest-player selection and from the player who later drops a token. Mandatory agent_notes/AGENTS.md contract requires causal ownership for damage and delayed consequences. Shared indirect-kill ownership and native item dispatcher should own this integration; avoid inventing nearest-player credit. No source blanket team/MAG/TECH check exists on block rolls; harmful downstream abilities retain their own qualified target/team/suppression policy and protected roles. Breaking a reward block is not permission to bypass those checks. Death/leave/rewind lifecycle must be tested at shared owners, not through a private LuckyBlock state engine.

## Coherent next scope / unresolved author decisions

Next scope is **actual three-type placement → native break loot → admitted reward token → existing consumer ownership**, with deterministic BLOCK-context fixtures (all false vs one face true; Silk/Fortune; no score/0/negative/positive luck; ordinary legacy 0/1; modifier neutral; explosion/no actor; Creative/observer/frozen; two players; death/leave and rollback). Preserve modes2..4 guard. Do not expand into all unimplemented reward encounters or re-port already completed items without identifying a concrete missing raw consumer.

Before implementation clarify only real policy: should native luck_amount continuously follow source integer luck+1, and how should the approved KFC score increments compose with that refresh? Who receives downstream causal ownership when block placement and breaking belong to different players, and when destruction is world/explosion-originated? Does author want any suppression on physical block opening itself (none in inspected source), independently of downstream MAGIC/TECH? These are unresolved in this bounded audit; do not infer answers from unrelated wand/encounter policies. Also verify fixed legacy neutral publication: absent fixed score can differ from explicit0 for the ordinary gate.

No actual placement/break/loot execution was run here. Unpackaged override files are existing source definitions; full chain ownership/luck parity and all random rewards are not established by this read.

Counter dependency: legacy load.mcfunction:145-146 creates lolidefecation as mined mushroom_stem and lucky_block_count as mined red_mushroom_block; leave_death.mcfunction:294 resets lucky_block_count. These counters need consumer audit before claiming all opening follow-ups complete. No special shears branch was found in the three inspected block tables: Silk Touch is the explicit tool gate. Placement-neighbor mutation parity has not been established; allfalse recognition must be tested after adjacency changes.


## Native investigation correction (same batch)

The high-luck Brown branch needs all three pools read together: the repeated Neko
pool explicitly excludes luck_amount>=16. At that threshold the two remaining
pools each yield TMT (the last competes overwhelmingly with Silk/ordinary output).
Therefore the deterministic native probe expects two TMT markers and no ordinary
Neko marker, including Silk; it does not expect sixteen Neko rewards plus TMT.
The source weight is preserved. Gson/DFU's normal Codec.INT path reads the authored
10^17 as1569325056; the native competing sum is safe. No weight nerf was required.
The earlier audit was a source-definition read, not proof of packaged support.
The old block schemas/allowlist and Luck bridge are addressed by the new component,
whose separate final note records actual execution and pending author decisions.
