# Next port audit — 1 November 2026

Read-only source audit against current Java after installed `6763758b`. Recommended bounded batch: **grappling hook + steampunk pickaxe + steampunk sword**. These are three actual item consumers, independent of full kit selection, random pools, BedWars, wands and the completed Chaos/dungeon chain. No runtime verification claimed; no builds/server/source/RP/question edits performed.

## Exact acquisition and dispatch

Legacy root below: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

| Mechanism | Existing acquisition | Actual legacy entrypoint and dependency closure |
|---|---|---|
| Hook, exact `grapping_hook:1` spelling | [kit/hook](../src/main/resources/data/lbc/loot_table/kit/hook.json), raw carrot carrier with `magicwand:1` | Legacy `function/workingitems/spell1_default.mcfunction` and `spell1_default_left.mcfunction`: selected/main and offhand respectively; TECH `stopper` within20 blocks gates activation. Both call `swrg_kit_integration/items/grapping_hook`. Requires `hook_cooldown>=120`; otherwise `uncharged_hook`. `*_work` removes the same owner's former seat/anchor, scans via `*_loop` in .33 steps (Distance up to100), accepts block/body endpoint via `*_loop_end`, then `*_success` mounts the player on an invisible native pig and damages matching held hooks; damage32 breaks them. `function/mobs_tick_lbc.mcfunction:10` dispatches `hook_sit` to `*_tick`, which faces/moves toward its same-owner anchor and gives slow falling near it. Include seat/anchor retirement, actual cooldown writer and Time Stop/Vanity continuity in implementation scope. |
| Steampunk pickaxe, `steampunk_pickaxe:1` | [kit/steampunk](../src/main/resources/data/lbc/loot_table/kit/steampunk.json) | Legacy [advancement/steampunk_pickaxe.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/steampunk_pickaxe.json): `minecraft:item_durability_changed`, SelectedItem exact flag, excludes spectator/nexus/imprinted; reward `swrg_kit_integration/items/steampunk_pickaxe_use`. That entry rolls `chance25`, rejects TECH stopper20, starts eyes+1 and invokes `*_use_rc`; always revokes the advancement to rearm. Payload is17 overlapping axis-aligned3-cubes (`air destroy`) at local forward offsets0..16. Preserve native drops, causal terrain credit and native admission; a generic vein-miner is not equivalent. |
| Steampunk sword, `steampunk_sword:1` | Same [kit/steampunk](../src/main/resources/data/lbc/loot_table/kit/steampunk.json) | Legacy [function/items/on_sprint.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/on_sprint.mcfunction):4 checks SelectedItem flag and TECH stopper20, calls `swrg_kit_integration/items/steampunk_sword`. `steampunk_sword1` starts `sprinted`, timer1, cooldown5. Main function clears tag on sneak and, while sprinted/timer<=200, passes timer*.05 and timer*1.25 to `steampunk_sword_1`; replaces fixed movement-speed/attack-damage modifiers and emits cloud/rocket sound. Include sprint event/timer/reset/cleanup closure, not only right-click routing. |

## Evidence that these consumers are missing

`rg -n 'steampunk_pickaxe|steampunk_sword|grapping_hook|hook_sit' src/main/java --glob '*.java' --glob '!**/*Checks.java'` returned **zero matches**. Current loot files retain all three exact flags, so this is a consumer gap, not missing assets. The live hand dispatcher [LbcWands.cast](../src/main/java/me/skywars2/game/LbcWands.java) routes neighboring raw utilities explicitly; [LbcRewardTools](../src/main/java/me/skywars2/game/LbcRewardTools.java) accepts only `raycastspell` with `krazha`, `shetka`, or `lightning_bolt`, and does not bridge these flags. No matching native implementation under a differently named class was located. Static code presence alone would still require native verification.

Reuse the existing item input/controller admission, shared resident ray/terrain worker, native item durability and attribute APIs, indirect-kill terrain causality, Time Stop and Vanity actor/checkpoint owners. Sword attribute state should use fixed canonical identities and removal on retirement, rather than duplicate accumulating modifiers. Hook native actors must participate in existing actor ownership/rewind lifecycle.

## Suppression and source questions before implementation

All three payload entrypoints explicitly check TECH `stopper`, not `stopper_magic`, even though the hook carries `magicwand:1`. Therefore establish intended beacon/artifact policy from existing user contracts before treating that presentation flag as MAG authorization. Hook has both hand dispatchers, sword/pickaxe SelectedItem semantics only. Preserve those differences unless user requirements override them. Confirm whether ongoing hook travel/sword modifiers stop or only new activation is denied; source entry gating alone does not settle sustained suppression. Pickaxe terrain destruction needs the user's mandatory causal kill attribution. Hook dual-held durability debits and native seat implementation are source facts to resolve deliberately, not silently normalize.

Do not reopen Uni Wand suppression, completed Chaos rewards, boss policy or full-chain release scope. Other absent raw flags were seen (`multiply:5`, `banana_palm`, `clocks`, `cunny`), but are outside this batch. In particular, the older Big Bob note's claim that banana palm lacks raw loot acquisition is stale: current and legacy `kit/palm_with_bananas` contain `banana_palm:1`; it is not evidence for treating the item as dormant.

## Follow-up closure: Java legacy overrides, timers and actual author text

Paper `plugins/luckywars/src/main/java` has no hook/pickaxe/sword mechanism override: exact flag search only finds steampunk kit acquisition (SwrgNativeService table12/GUI option and PerkRegistry unlock). `sprint_cooldown` occurs in UI keys/rendering and SwrgNativeService's reset defaults, not an alternate sword engine. Ignite `mods/luckywarsignite/src/main` Java search for `grapping|steampunk|hook_sit|hook_cooldown|sprint_cooldown` returns zero. Paper bundled datapack duplicates are resources, not registered Java consumers. This bounded audit did not qualify compiled binaries independently.

Hook cooldown is **initial120 seconds**, not a120-second reuse cooldown: `players_1s.mcfunction:19` adds1, `leave_death.mcfunction:412` resets0, and no use function resets it. Once charged, source permits repeated successful uses until death/leave reset. Hook tick has no arrival-distance retirement: it kills its own seat only when **no nonspectator player within5 blocks** (any player, not owner/passenger). Near anchor<=10 it gives slow falling to the pig. Anchor is deleted only by the next accepted `*_work` same-owner replacement among exact hook references; `leave_death` does not name/delete either seat or anchor. Dismounting near another player can thus leave the source pig alive; no separate source anchor arrival/death cleanup was found. Treat lifecycle fixes deliberately, preserving the intended grapple and existing native causal/rewind contracts rather than reproducing orphan accumulation.

Sword timer is incremented each `items/on_sprint` tick. `players_sprint_end` immediately resets timer0, removes both fixed modifiers, and clears `sprinted`. `players_1s_sprinted` decrements cooldown only at timer<=0 and clears `sprinted` when cooldown reaches0. `steampunk_sword1` starts cooldown5; modifiers update only through timer200, then retain their last values while sprint continues. This is a run ramp with retained ceiling and five-second stopped cooldown, not an automatic forced dash ending at200. Sneaking clears `sprinted` in the item helper. Source raw values are speed+.05*timer and attack damage+1.25*timer; do not substitute percent modifiers.

Authoritative current RP is read-only, as required by [RESOURCE_PACK.md](RESOURCE_PACK.md). Its `assets/minecraft/lang/{ru_ru,en_us}.json`, lines3999–4007, already contains all keys; no new RP strings are necessary:

| Exact key | ru_ru | en_us |
|---|---|---|
| `steampunk_pickaxe` | Паромеханический разрушитель | Steampunk Breaker |
| `steampunk_pickaxe.lore` | Иногда пробивает широкий тоннель | Sometimes breaks wide tunnel |
| `steampunk_sword` | Паромеханическая перчатка | Steampunk glove |
| `steampunk_sword.lore.1` | При разбеге ускоряет игрока, увеличивая урон. | Running insanely accelerates player, giving |
| `steampunk_sword.lore.2` | В конце рывка уходит в перезарядку | great damage boost, then goes to cooldown |
| `grapping_hook` | Технический крюк-кошка | Technical Hook |
| `grapping_hook.lore` | ПКМ по блоку, чтобы зацепиться за него | RC on block clings to it |

These names make clear that the sword carrier is presented as a **glove** and the hook as **technical**. Preserve exact loot formatting/models and these translations. Description supports block grapple intent; source also detects bodies, so body-hook admission remains an explicit design issue if changing it.

## Resolved implementation and native qualification

All questions for this group are resolved. Hook uses direct player velocity,
initial120 seconds/free reuse until death, source fixed contact, and SHIFT exit.
TECH cancels the active hook, glove bonuses and queued breaker work; MAG does not.
The user's final breaker rule replaces durability with actual block-break/melee
inputs: repairs and armor damage never trigger it. All three native Polymer tools,
actual kit acquisition and seven isolated native groups are now qualified on
036b29de. See [the current contract](STEAMPUNK_TOOLS_2026_11_01.md).
The audit's earlier missing-consumer/source-question statements are historical.
