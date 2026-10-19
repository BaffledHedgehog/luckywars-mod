# Mystical artifact type 1:7: current port and limits

This documents the current Fabric source in `LbcColors` and `LbcMagic`. The focused server scenario and the full 17-match / 48-group regression pass on the same deployed JAR. This is not a complete mana engine. [Execution evidence and limitations](../SERVER_TESTS.md).

## Activation and dispatch

`LbcMagic` indexes players carrying artifact type 7 after inventory changes. The five-tick scheduler visits that index and calls `LbcColors.pulse` for active, living gameplay participants in the arena. It does not rescan each holder's inventory at every pulse. For each eligible holder, `pulse` checks two block positions: the block at the player's feet offset (`position + (0,-0.5,0)`) against `minecraft:colored`, and the block at `blockPosition()` against `minecraft:in_colored`. Each qualifying position dispatches through the 16 color tags in source order; if both positions qualify, both branches can run. Magic beacons suppress the color pulse.

The block tags and artifact item data remain dependencies. The implementation uses `minecraft:colored`, `minecraft:in_colored`, `minecraft:airs`, `minecraft:mobs`, and 16 `minecraft:<color>` block tags. The legacy dataset for these tags and the original effect descriptions are linked in [ARTIFACT_COLORS_NEXT.md](ARTIFACT_COLORS_NEXT.md) and [ARTIFACT_COLORS_EFFECTS.md](ARTIFACT_COLORS_EFFECTS.md).

## Implemented color branches

All branches are dispatched on the five-tick pulse. Durations below are the direct native effect tick values passed to the current Java helpers. Radius targets use native `Entity.isAlliedTo` to exclude allies; the current `GameController.Participant` model does not assign teams.

| Color | Current Java behavior |
|---|---|
| Red | Strength 6000 ticks, amp 0; clears holder Weakness; applies Weakness 1200 ticks, amp 0 to enemies within 15; crit particles. |
| White | Speed 6000 ticks, amp 1; clears holder Slowness; applies Slowness 600 ticks, amp 1 to enemies within 25; end-rod particles. |
| Blue | Water Breathing 600 ticks, amp 2, and Conduit Power 600 ticks, amp 1; nautilus particles. While either enhanced effect is active, the indexed five-tick water handler adds water (Conduit amp 1: flowing level 1; amp 2: source) in a 3×4×3 air-only volume. Water Breathing in water removes Wither and grants Strength at the breathing amplifier plus Resistance at amplifier +1; in air it applies Wither amp 3 and, at breathing amp 2, Poison amp 3. |
| Yellow | Haste 6000 ticks, amp 10; clears holder Mining Fatigue; applies Mining Fatigue 1200 ticks, amp 2 within 25; honey particles. |
| Orange | Fire Resistance 6000 ticks; ignites eligible enemy air blocks in a 3×3×3 volume within 30; lava particles. |
| Light blue | Slow Falling 6000 ticks; Levitation 40 ticks, amp 5 within 20; cloud particles. |
| Purple | Adds custom additive RTP time to hostile mobs within 20 and spreads both those targets (custom radius 10) and the holder (source branch radius 200 or 190); see below. |
| Gray | Resistance 200 ticks, amp 4; barrier particles. |
| Light gray | Resistance 6000 ticks, amp 2; removes Resistance from enemies within 15; iron-block particles. |
| Lime | Jump Boost 6000 ticks, amp 2; clears holder Levitation; applies Levitation 20 ticks within 15; sneeze particles. |
| Pink | Instant Health (1 tick), Regeneration 6000 ticks, amp 1; clears Poison, Wither, and Instant Damage; applies Wither 200 ticks, amp 1 within 20; heart particles. |
| Cyan | Invisibility and Night Vision 6000 ticks, amp 1, hidden; applies Glowing 600 ticks within 300; glow-squid particles. |
| Black | Clears holder Blindness; Night Vision 6000 ticks; applies Blindness 600 ticks within 20; for hostile mobs within 20, sets Follow Range base to 4; glow particles. |
| Brown | Saturation 1 tick, amp 1; increments `food_count`; clears holder Hunger; applies Hunger 600 ticks, amp 1 within 25; beef particles and burp. |
| Green | Grants holder 3 XP; removes 3 XP from eligible enemy players within 200; green dust. |
| Magenta | Adds 20 to holder mana; subtracts 20 from enemy players within 35 only when their mana is at least 20; witch particles. |

The source `follow_range` write for Black is a permanent base-attribute mutation in this branch. No timer or restoration is attached to artifact type 7, and the separate type 6 potion cleanup does not restore permanent attributes.

## Mana and food counter boundary

`LbcColors` delegates its established scoreboard resources to the shared `LbcMana` bridge. Magenta retains +20/−20 arithmetic and Brown increments the separate `food_count`; match entry resets mana to 5000 and food count to 0. The subsequent [mana core port](MANA_PORT.md) adds regeneration, maximum, boost/reverse modifiers, spending API and HUD without introducing a separate resource for the artifact. Actual spell consumers and advanced negative-mana combat remain outside that core batch. [Legacy subsystem evidence](ARTIFACT_COLORS_DEPENDENCIES.md).

## Purple custom RTP timer

Purple does not apply a native Raid Omen potion in this implementation. For each eligible hostile mob, it verifies that Raid Omen can affect the target, then adds 600 to the `effect_rtp_1` scoreboard timer and indexes the entity for teleport ticks. The timer is additive, not a reset-to-600 duration. While the holder keeps pulsing Purple, each pulse can add another 600; a teleport tick subtracts 5. The nominal source value is 600 ticks per target pulse, but repeated pulses extend the timer cumulatively. The source branch also separately spreads the holder every color pulse, using radius 200 around the holder when `no_borders` is enabled or radius 190 around world origin otherwise (Nether uses `under 120`). The custom target loop spreads eligible targets at radius 10 every five ticks. These are distinct actions; neither substitutes for the other. The 600-tick custom timer is an approximate duration port, not strict frame parity with the legacy effect-to-scoreboard conversion on the following entity tick.

Every five ticks, the indexed teleport set is processed. A target is removed/cleared if it is no longer eligible or is outside the arena level; otherwise `sin_greed_active` pauses processing and leaves the timer unchanged. Positive timers use synchronous native `spreadplayers` centered on the target, radius 10 (Nether adds `under 120`), and then decrement by 5. Entity load and index refresh re-add living entities with a positive stored timer; unload removes them from the in-memory set. The shared player `clearEffects` route also clears this timer. Native spreadplayers runs on the server thread; its cost has not been benchmarked here.

The color pulse queries two block positions per eligible holder every five ticks. It does not query inventories or enumerate a radius just to find holders. Enemy selection reads nearby world entities/players; no world data access is moved off-thread.

## Behavior still dependent on other systems

- Colored block and entity tags must be registered with the intended source values for the selectors to match.
- Purple's native spreadplayers command is synchronous and can affect server tick cost; no performance claim is made without measurement.
- Enemy filtering follows Minecraft native alliances. The current match participant data has no team assignment, so a custom team engine is not represented by this port.
- Blue has custom water-power and breathing handlers for amplified Conduit Power and Water Breathing. `LbcColorEffectsMixin` indexes living entities when relevant effects are added, updated, or removed; unrelated effects do not update the index. Server assertions confirm player water placement, Strength/Resistance, hidden Wither/Poison and cleansing. The generic mob path is implemented but not separately exercised by this color scenario.
- Shared regeneration, limits and HUD now come from [LbcMana](MANA_PORT.md). Spell consumers and advanced overload remain subsequent dependencies.
- Black's Follow Range base value has no artifact-specific cleanup path.

## Source

- [LbcColors.java](../src/main/java/me/skywars2/game/LbcColors.java): color mapping, target selection, scoreboard helpers, purple timer and teleport loop.
- [LbcMagic.java](../src/main/java/me/skywars2/game/LbcMagic.java): artifact indexing, five-tick scheduling, and entity load/reload handling.
- [LbcColorEffectsMixin.java](../src/main/java/me/skywars2/mixin/LbcColorEffectsMixin.java): effect add/update/remove hooks that maintain the generic living-entity water-effect index.
- [LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java): entity lifecycle forwarding and shared effect-clearing entry point.
- [GameController.java](../src/main/java/me/skywars2/game/GameController.java): match-start resource reset and participant model.
- [ARTIFACT_COLORS_NEXT.md](ARTIFACT_COLORS_NEXT.md), [ARTIFACT_COLORS_EFFECTS.md](ARTIFACT_COLORS_EFFECTS.md), and [ARTIFACT_COLORS_DEPENDENCIES.md](ARTIFACT_COLORS_DEPENDENCIES.md): legacy dispatch, source effects, and dependencies.
