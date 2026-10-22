# LuckyWars luck/config owner — source audit, 21 October 2026

Author clarification after this audit: modifier consumers are Lucky Blocks and
their drops, absent from ordinary SkyWars. Native configuration and last resolved
result are retained; only LuckyWars committed starts reroll. The owner/config
integration is qualified separately in [LUCKY_MODIFIERS_PORT.md](LUCKY_MODIFIERS_PORT.md).
The questions below are historical audit findings, no longer pending decisions.


Read-only bounded audit. Only this new note written. Source root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled`; native paths below are repository relative. Existing user notes retained.

## Two different kinds of luck

1. **Global mode selector:** fixed fake player `cfg_lucky_modifier` in `swrg.math` is persistent server configuration; `final_luck_modifier` in `lbc.math` is a global resolved match variant, not an amount of player luck. Source writes to final are confined to `LBC/data/lbc/function/start.mcfunction:6-18` and `random_luck_modifier.mcfunction`. No player-scoped final modifier or live recalculation found.
2. **Player luck:** `players_2t.mcfunction:1-2` stores native `minecraft:luck get 1` into the player's `luck_amount`, then adds 1 (integer command result plus one). `load.mcfunction:120` creates the dummy objective. KFC source adds one to this score, but the next player refresh replaces it from native attribute; native KFC retains its approved score ledger. This is independent of global modifier IDs. No loot-table references to `luck_amount` were found. Do not substitute modifier7 for attribute luck7.

## Initialization and source settings

- `swarogskywars/data/swrg/function/core/default_settings.mcfunction:22`: config default 0 (random). LBC `load.mcfunction:302` initializes cfg to 0 only unless existing score is 0..100; it does not initialize final. Thus legacy persisted/out-of-range values 13..100 can survive load yet have no corresponding start branch.
- `swarogskywars/.../gui/select/1000.mcfunction:51-53`: settings page1000 slot11 increments cfg and wraps 13→0; source permits administrator or anyone when no administrator is present. `gui/page/1000-lang.mcfunction:58-70` renders cfg0..12. Source labels in `core/lang/en.mcfunction:256-268`: 0 Random,1 Disabled,2 100% Neko+Loli,3 Lot of Traps,4 Rasengans-Fireworks,5 No typed loot,6 Mystical Artifact Overfill,7 Baffled Luck,8 Sword Art,9 Full Random1-64,10 Magic Potions Overfill,11 Trees,12 Lucky Block Imba Version. Reuse existing translation keys `swrg.cfg_lucky_modifier_<n>`.
- `LBC/.../start.mcfunction:4-18`: runs LBC config, forces no_borders=0 when configured modifier>=2, then resolves final once. cfg1..12 maps identically; cfg0 draws uniform integer1..100. Draw1..75 and87..100→final1 (89%); draw76..86→final2..12 respectively (each1%). These are categorical IDs, no arithmetic formula over player luck.
- `swarogskywars/.../game/start/0.mcfunction:50-51`: only mode2 (LuckyWars) calls `lbc:start`; modes3..4 call `lbc:start_bedwars`, which follows a separate bedwars/game/start route and does not call modifier resolution in its wrapper. Normal mode1 does not resolve final. No reset of final at normal start/end found in the inspected datapack functions: old final can remain after a LuckyWars match. Do not silently call this documented legacy stale-state behavior a desired native policy.

## Consumers and loot semantics

- Source `workingitems/mobgun.mcfunction:3` and `other/random_mob_spawn.mcfunction:2`: final7 forces outcome22. `projectiles/gadgets/superdice.mcfunction:2` changes random range to4..6 at final7.
- Global fixed-score conditions appear in `loot_table/luckyblock*.json`, `lucky_items*.json`, `barriers*.json`, and ordinary transformables accessory/bow/potion/shooter/sword. Example `transformables_accessory.json:460`: artifact entry weight400 quality8 only at final6, in addition to unconditional weight10 quality8 artifact. `luckyblock.json:27-43` enables enormous weight200000000 imba branch at final12. Keep existing JSON conditions; no manual replacement selection algorithm needed.
- Entry `quality` is the vanilla loot-context luck weighting mechanism (effective nonnegative floored weight+quality*contextLuck), independent of fixed-score conditions. Existing native `LbcLoadouts.rollTable` (`:396-402`) builds COMMAND context with origin/optional THIS_ENTITY and **does not set luck**; existing callers therefore retain context luck0. A player THIS_ENTITY alone does not forward the player's luck attribute. Do not broaden this owner patch into changing already-qualified loot weighting.

## Native gap and bounded next implementation

`GameConfig.java:33-34` already stores mode1..4, defaults normal1, persists/clamps/resets settings. `GameController.java:215` blocks starting modes other than1. `GameMenus.java:145` renders settings but slot11 has no mutation; `OriginalGuiDesign.java:521` always displays cfg_lucky_modifier_0. No config field/native final selector/reset owner exists. `LbcMobgun.java:124-125` reads externally present final7. Source tables are already loaded and their native conditions can read scoreboard. `LbcLoadouts.syncRanked()` is a useful model for an authoritative global config→score bridge; `LbcMana.score(ScoreHolder.forNameOnly(...),objective)` already supplies native score access.

Bounded next step: add persisted cfg0..12 plus settings slot11 rendering/cycle and command setting; implement a small match-scoped resolved modifier owner with injectable deterministic1..100 resolver, scoreboard publication and cleanup. Preserve mode-start guard until actual LuckyWars acquisition mechanics are ported. Do not advertise modifiers2..12 as completely implemented: changing fixed-score consumers alone does not port their encounter/drop systems. Reuse GameConfig load/clamp/reset, GameMenus change permission/lobby/save path, OriginalGuiDesign setting rendering, shared native loot conditions and Mobgun.

Real author decision: because only mode1 is playable and source resolves only mode2, should this new setting affect normal matches with ported LBC items now, or remain dormant until LuckyWars mode is implemented? Also choose explicit native reset/neutral policy between matches/normal mode (source can retain stale final). No question is needed about random distribution, numeric IDs, source labels or global/player scope. Changing context luck from0 to player attribute is a separate behavior decision, not required to close fixed-modifier ownership.

Verification: cfg defaults/persistence/clamp/reset and GUI/command mutation; fixed1..12 identity; deterministic draws75,76,81,86,87,100; one random resolution per committed match (no reroll during rendering, invalid start, respawn or loot roll); scoreboard final matches resolved owner; config change during match rejected or next-match only; normal→Lucky→normal/reset lifecycle per author policy; final7 Mobgun forces22, final1 restores ordinary randomness; fixed6 accessory conditional leaf and fixed7 shooter leaf use current score while original quality/context luck remain unchanged. Modes2..4 must remain unavailable until their gameplay contract is implemented.
