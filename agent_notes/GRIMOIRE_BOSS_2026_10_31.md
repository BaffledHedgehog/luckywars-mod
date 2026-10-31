Integrated native status: [batch result and pending Uni MAG ruling](GRIMOIRE_DUNGEON_CHAIN_2026_11_01.md).

# Grimoire dungeon boss — implementation awaiting integrated native verification

31 October 2026. This note describes new source files; it does not claim a native
check pass, packaging, installation or release. Integration belongs to the parent
workstream.

`LbcGrimoireBoss.spawn(ServerLevel, Vec3, float, ServerPlayer)` creates the original
stationary invisible Warden with 1000 native maximum/current HP, armor/toughness1,
and `lbc:grimoire_boss` death loot. A separate real item display retains the existing
`lbc:grimoire_boss` model, scale4 and vertical offset6. The source facing is supplied
explicitly; default overload uses -90 degrees. Native entities own persistent
Fabric attachment values for phase, lifetime, total age, exact parent UUID, aim,
danmaku rotation, epoch, author and original WILD causal order. The shared native
actor checkpoint serializes those attachments; no separate checkpoint adapter was
introduced. Loaded indexes, bossbars and warning displays are derived state.
The `spawn(ServerLevel, Vec3, float, LbcSpellSource.Source)` overload accepts the
already admitted dungeon marker's source directly. It preserves source author,
epoch and causal order without looking up a player, so room completion still
spawns its boss after the original caster disconnects. The player overload delegates
to this path; boss attacks bind the new real hitbox as their emitter.

The initial laser starts at70 ticks, tracks from71, and ends at250. Idle has source
1% selection of three phases. Meteor updates retain the50% gate, random inclusive
-15..15 center offsets,49 independent40% tuff displays, downward1/tick movement,
10 falling-block damage/cap2, source block break exception and100tick lifetime.
Meteors are independent after emission and survive their boss's death, matching
their absence from the source `boss_piece` tag. Danmaku retains the30% gate,
three nearest eligible targets, one-in-seven random angle versus +93 degrees,
10block starting offset, and24 extra end emissions per eligible target. Idle
lifetime reset remains laser-only, as in the original functions.

Prisms warn until31 ticks and fire one7 arrow damage per exact body intersection
over27blocks, through terrain. Their original1/11/21 sounds remain. Warnings use
the existing `TrialWarningVisual.worldBeam` item-display bloom/fade model, never
particles. The newly telegraphed main beam has a fixed startup aim; active tracking
begins at71. Main damage is separately evaluated with shared `LbcDamage.hurtSegment`,
zero padding, .3step cadence,10 magic damage and two admitted targets per sample.
Air-only voxel clipping uses the shared native block traversal and stops at an
unloaded boundary. Visible live beams retain harmless source-style particle lines;
these do not select targets or implement warning geometry.

Native damage retains shared role/Greed exclusions and WILD causal credit,
including the initiating player/allies. Accepted actors do not depend on their
author remaining online/alive. Parent relationships use exact UUIDs, so nearby
bosses cannot steal pieces or protect another boss from death cleanup. Shared
Time Stop pauses canonical phase/child clocks and warnings. Actor unload removes
only derived indexes/presentation; saved bodies and clocks remain canonical.

Required integration: ItemService owns one instance and invokes tick/loaded/
unloaded/stop; dungeon reward dispatch calls spawn; package the existing source
loot table and its item references. `GrimoireBossChecks.run(GameController)` is an
independently callable native fixture for HP/model/loot declaration, WILD identity,
warning countdown and actor freeze, exact main-ray hit/miss, causal credit,
reload and neighboring-boss cleanup. It has not been run by this subtask.

Still requires integrated native evidence: complete random phase/cadence and
meteor/danmaku trajectories, death loot consumer, actor region reload, full Vanity
coordinator rewind and visual client presentation. User confirmed: antimagic blocks attacks, retaining the boss and main visual.
The main visual has no magic-removal tag; beacon/artifact suppress attack clocks,
and shared boundary clipping prevents rays from crossing antimagic. Existing
physical meteor debris remains physical. Main MAGIC damage uses native hurt-time,
so repeated equal ten-HP attempts deal at most one accepted hit per ten ticks;
there is no cooldown bypass for this beam. Both rulings have dedicated native checks.

Read-only source: `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/`
under `E:/servers_mew/cringe1_21_4_skywars/`, especially `mobs/grimoire_boss/*`,
`room_reward/spawn_boss*`, and `lbc/function/raycastselector*.mcfunction`.
