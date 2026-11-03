# Nexus frame and wire acquisition source audit — 3 November 2026

Read-only audit. No Java, resources, build, git or server changes. Source root L = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Native project = `E:/servers_mew/skywars2_last/luckywars-mod`. Read project and agent_notes AGENTS.md and Serena initial instructions; Java navigation used scoped symbols. This is source evidence, not live qualification.

## Executable dispatch

`data/minecraft/tags/function/tick.json` registers `lbc:tick`; `L/function/tick.mcfunction:3` executes `tick_nexus` in Nexus. `tick_nexus:3` initializes non-player entities without `dated` (`tick_nexus_data`: NoGravity, Silent, NoAI, XP conversion, dated). Line 4 executes `tick_nexus_raycasters` for `#minecraft:raycasters` with `nexus_rc`; that tag includes markers, armor stands, area clouds, item/block displays and interactions. Line 5 iterates every red-wire item, testing blue and black item entities independently within radius 3; it invokes `craft_white_wire`. This is entity proximity, not a crafting-grid recipe, player action or ownership gate.

`tick_nexus_raycasters:1` increments Lifetime. A center_cube marker over **exact air** invokes spawn_blue_wire; an explose_wired actor invokes explode_wire. This is not an airs-only tag test. The dispatch has no local jammer condition.

## Blue and black acquisition / destructive spread

`L/function/other/nexus/spawn_blue_wire.mcfunction:1` rolls `lbc:nexus_wire_blue`; line 2 makes the nearest matching blue item glow (no distance cap), creates an explose_wired+nexus_rc marker, plays laser rev-down at volume 3/pitch 0, then kills the center marker. Blue is the historical identifier for the visually green wire (model `lbc:nexus_wire_green`).

`explode_wire:1` replaces its own cell with black concrete. Six independent 80% trials enqueue adjacent cells unless already black concrete. Six independent 5% trials enqueue cells at ±4 on each axis without a block predicate. It kills **all interaction and item_display entities** within 2.5 (`minecraft:inter_itdisp`), including unrelated ones. If two or more explosion markers occupy radius .01, it kills that colocated cohort. Independent 0.015625% (`0.00015625`) trial calls spawn_wire_black, which rolls `lbc:nexus_wire_black`. Independent 0.5% trial kills **all** explose_wired markers in the current dimension, without distance or owner restriction. Existing author confirmation explicitly preserves that global stop; do not ask it again. A lone marker survives and can run again next tick, even over black concrete. Duplicate-kill does not imply a scheduled lifetime limit.

`spawn_cube.mcfunction` currently comments out all marker/terrain summons and only records coordinates. Registered Paper `command/NexusCloneCommand.java:179..194` actually supplies center_cube+nexus_rc and nexus_tree+nexus_rc markers during cloning; `CommandRegistry.java:72` registers clonetonexus. This is a source of prebuilt island actors, not evidence that the future native world generator already emits them.

## White proximity crafting

`craft_white_wire:1` spawns one white wire immediately, then particles/sound. It kills red @s if count 1; kills one count-1 blue and one count-1 black within 3; then reselects a blue and black within 3 and writes `int(existing count * .99999)` and does the same for red @s. For ordinary positive stack sizes this lowers count by one. Important literal quirk: selectors are re-evaluated after count-1 entities die, so an additional blue/black stack can lose one in the same craft. Selectors have no sort, owner or explicit identity binding. A native transactional one-each recipe is an intentional correction, not exact emulation of this quirk. Radius is centered on the red entity, and blue/black need not be mutually within 3.

## Converter creation and release

`tree_end_pre` rolls integer 1..4 uniformly, clears a 3×3×3 cube, and dispatches tree_end_1..4. Each variant creates a 1×1 interaction at block-centered XZ, two displays at cell center (reward and cage), and rolls its color pool into an item entity. It copies that complete Item into the reward display and kills the temporary item. Frame models are `lbc:nexus_item_cage_red/green/black/white`, repeating-command-block carriers. This is a stored reward chosen at creation; interaction must not reroll it.

Pool dependencies (single roll, no templates referenced by these wrappers): red → Orb Gun only; green → Compass only; black → Corrupter only; white → equal weight 10 Roadgun / Pulsar. Canonical files are `L/loot_table/nexus_item_{red,green,black,white}.json`; tool native IDs are already qualified. Do not filter, replace or defer a branch silently. TMT/random_glack remains LAST, separate from these four pools.

`L/advancement/interaction_nexus_frame_{black,green,red,white}.json` uses player_interacted_with_entity, excludes spectator playerGameType 3, requires matching interaction tag and matching wire custom data. Green requires `nexus_wire_blue`. No dimension, magic, tech, artifact, cooldown or owner gate is encoded. Each reward function clears **one matching item anywhere in inventory first**, then searches nearest same-color interaction within 10 of the point 3 blocks forward from the player's eyes, executes interacted_frame_at there, and revokes its advancement. It does not bind the clicked entity or recheck successful debit. Both-hand trigger possibilities follow the advancement event; it has no mainhand restriction.

`interacted_frame_at` summons a floating TNT item at y+.5, copies the nearest wire_converter_item display's **whole Item** within 1, removes temporary tag, kills **all** item_display entities within 1, plays beacon deactivation plus 1000 detection particles and kills its interaction. If no reward display exists, the TNT placeholder survives. Exact behavior can consume a wire then choose another nearby converter, and unrelated displays can be removed. Those are source defects to distinguish from intended cage exchange.

`tick_1s_player:6` grants cage discovery advancement within 5 of a wire_converter_frame. Wire discovery uses inventory_changed with custom-data predicate in `true_advancements/nexus/wire_{green,black,white}.json`. `random_item.json` separately includes all wire colors; do not remove them while implementing natural acquisition.

## Native integration opportunities and boundaries

`src/main/java/me/skywars2/game/LbcNexusWires.java:45` already converts one real Nexus ExperienceOrb into one floating native red wire before merging, preserving one wire per entity rather than XP value. Its tick (90..106) owns dirty carrier indexes, outside red/black consumption, gateways and altitude entry; it respects match/ending/freeze/global stop and existing native per-actor pause. Extend its indexed item admission for blue/black/white proximity crafting or delegate a bounded acquisition owner; avoid a second red producer, XP ledger or global entity scan.

Existing Nexus tools already provide shared field/ray scheduling, typed checkpoint ownership, TimeStop and terrain construction barriers. Acquisition should retain per-world actor indexes and immutable stored full reward stacks, bind cage components by native identity, use native item admission/current epoch, and capture marker lifetime, frontier, RNG continuation, display association and carried rewards in the full checkpoint. Terrain changes must join existing rewind journals/barrier. Global 0.5% stop should clear all current-dimension explosion actors across owners. Release/crafting needs atomic item output and input commit so failure cannot duplicate rewards. Source floating drop behavior should remain visible and pickup-compatible.

Paper scan of the explicitly requested `plugins/luckywars/src/main/java/me/luckywars` found the registered Compass override plus cloning command; no wire crafting, frame interaction or destructive-spread override. No old mixed Paper tree was substituted. This is a bounded negative exact-text search, not runtime proof.

## Genuine author decisions

1. Preserve or correct the crafting re-selection quirk that can debit an extra blue/black entity after deleting a singleton? Proposed native behavior: bind one red/blue/black item and debit exactly one each atomically.
2. Preserve or correct converter selection/cleanup defects? Proposed native behavior: debit the hand actually used, release the clicked cage's stored stack, delete only its owned components, and fail without debit when incomplete. Literal source instead clears inventory first, selects another nearby cage, may release TNT, and removes unrelated displays.
3. Is destructive spread intended to destroy unrelated interactions/displays? Source explicitly does so; an owned-only cleanup would narrow it. Global 0.5% stop is already confirmed, not an open question.

No repeated questions about outside wire suppression (all unjammed), Compass MAG/artifact, or Orb/Pulsar unjammed policy. No main server start/stop performed.
