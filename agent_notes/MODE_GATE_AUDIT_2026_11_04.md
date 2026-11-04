# Remaining mode / kit gate audit — 4 November 2026

Follow-up: skills23/29/33/34 now have native implementations and qualification work recorded in [ADVANCED_SKILLS_PORT_2026_11_04.md](ADVANCED_SKILLS_PORT_2026_11_04.md). The HEAD-f804e140 catalog audit below is historical.

Read-only audit of HEAD f804e140. Only this unique note was written. No Java,
resources, builds, server, RP or protected author WIP changed. AGENTS and Serena
manual read; Serena symbol lookup returned a cross-drive C:/E: ValueError, so
focused exact-text/source slices were used. Supernova/Railgun/Shishgun/Balance
completion supersedes historical blockers in earlier notes.

## Current gates, verified in code

- `src/main/java/me/skywars2/game/GameController.java:651` `startFailure` rejects
  every `config.gameMode != 1`. Native Balance's old-mode overlay does not remove
  this top-level gate. `GameConfig.java:119,153` still permits modes1..4; menus
  cycle them (`GameMenus.java:173`), with3/4 treated as Bedwars (`:224`). Enabling
  modes requires their actual lifecycle, not removal of the rejection alone.
- `src/main/resources/skywars2/lbc_loadouts.json` has kits
  1,2,3,4,5,7,8,9,10,11,12,13,15,17,18,19,20,21,22,23,24,25.
  Thus6,14,16 are absent.14/16 concern the protected Academy/key work: do not
  treat their absence as authorization to edit that WIP.
- Same catalog has skills1,3..22 (21 total);2 and23..36 remain absent. Existing
  service support for skill2 branches is compatibility plumbing, not public
  selectable skill2 qualification. `LbcLoadouts.apply`/selection use catalog
  entries; no hidden automatic admission closes these gaps.
- `LootTestCatalog.java:37` explicitly hides native BLUE wire because its green
  outside reward requires full random_glack/TMT. Preserve this TMT-last gate.

## Exact kit6 / skill2 dependencies

Original root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/`.
Paths below are relative to `LBC/data/lbc/` unless marked otherwise.

- `function/swrg_kit_integration/game/kit_select/select.mcfunction` grants kit6
  `lbc:kit/lucky_baffled`. Its `loot_table/kit/lucky_baffled.json` is exactly
  16 `luckyblock_block_only` plus one `luckyblock_neko_block` and one
  `luckyblock_loli_block`. Native three-kind blocks already exist in
  `LbcLuckyBlocks.java`; their opening routes are `lbc:native_lucky/{normal,neko,loli}`.
  Therefore missing block placement/opening is historical. Full kit release
  still inherits full reward graph/TMT qualification; a catalog-only enable
  would overstate completeness. `agent_notes/LUCKY_BLOCK_NATIVE_PORT_2026_11_01.md`
  final section explicitly retains this gate because packaged tmtrainer_drop
  is preparation and has no native generator/multi-marker activation.
- `function/other/instants/select.mcfunction` splits skill2 from ordinary pickup;
  `select2.mcfunction` handles mainhand Infinity, black/blue/red Nexus wires and
  Greedstone. Infinity, black/red and Greedstone now have native owners, so older
  statements calling those missing are stale. Blue ->
  `other/nexus/wires_other_worlds/green` remains the full random_glack/TMT edge.
  Existing cold/liquid/bomb/other skill2 guards must be preserved even while
  selection stays closed. Do not use this audit to enable2 early.

## Concrete non-TMT work remaining

- Native Bedwars lifecycle is still a real independent dependency: original
  `function/start_bedwars.mcfunction` grants lives/team setup, start reward and
  delegates to `function/bedwars/game/start.mcfunction`, then start1/start2,
  player teleport, bed-team lookup and spawn beacon routines. The source
  `swarogskywars/data/swrg/function/game/start/{1,config}.mcfunction` deliberately
  excludes3/4 from ordinary cage removal/chest fill/stage scheduling. Reusing
  mode1 unchanged would violate this source split. Team selection, shared
  spawn/team elimination, beds/respawn/economy/shop and nonhostile teammate/shop
  target filtering require a coherent native batch. This is larger than a
  safe immediate gate edit.
- Stand reward lifecycle exists in `function/start_stand.mcfunction` (6s delayed
  grant) and `start_stand_give.mcfunction` (`lbc:stand_oraora`). This audit does
  not claim stand combat complete merely because the loot table exists.
- Skills23..36 remain catalog-absent, independent of full TMT. Concrete compact
  candidate23 has source files `swrg_kit_integration/game/skills/god_claw_{tick,
  start,active,end}.mcfunction`, plus `other/attributes/*_up_godclaw.mcfunction`.
  Source tick requires60 sneaking ticks, active phase up to90, chance20 cooldown
  decrement outside sneak, owned marker continuity, and26 random attribute
  outcomes. `god_claw_start` creates a slowraycast magic marker with owner and
  team identity. No GodClaw Java owner or catalog entry was found. The marker's
  ray/contact dispatch must be included in its source closure before coding.

## Bounded next batch recommendation

Port and qualify skill23 God Claw alone: close its owned slow-ray/contact source
graph and26 attribute outcomes, implement native input/actor/cooldown ownership,
TimeStop and typed full-coordinator state, source exact cleanup, then expose only
skill23 after real-match qualification. Reuse existing spell rays, attributed
damage, attribute lifecycle and full coordinator; no interpreter or private
checkpoint. This is independently valuable non-TMT work and avoids protected
Academy/reroll/RP paths. Root should select it only after checking marker closure
against other audit findings. Keep all mode gates, kit6, skill2 and blue wire
closed; full TMT stays LAST. No runtime tests were run for this read-only audit.
