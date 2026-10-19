# Mystical Artifact anti-explosive effect: next scope

Historical source plan. The bounded native implementation is now described in
[ARTIFACT_EXPLOSIONS_PORT.md](ARTIFACT_EXPLOSIONS_PORT.md); verification and
deployment evidence are maintained in [SERVER_TESTS.md](../SERVER_TESTS.md).
The nuke engine remains outside this batch.

## Legacy source

The old server runs Paper/Minecraft 1.21.11; the target project uses Fabric/Minecraft 26.3.
Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.

`lbc:function/items/changed_inventory/sel.mcfunction` sets `notnt_active` while an item
with custom data `mystical:1,type:2` is in `container.*` or `weapon.*`, and removes the tag
when neither slot group contains one. `lbc:function/players_2t.mcfunction` dispatches
`other/artifact/notnt` for that holder; `mobs_2t.mcfunction` calls the player function
for non-spectators every two ticks.

`other/artifact/notnt.mcfunction` does four things for each active holder:

1. At every `#minecraft:unstable` entity within radius 24, emits one explosion particle.
2. At each such entity, plays `minecraft:entity.generic.explode` to `@a` at volume 1,
   pitch 1, category `master`.
3. For every `tag=nuke` entity within radius 24, runs `other/stoppernuke` as and at it.
4. Kills every `#minecraft:unstable` entity within radius 24.

The holder dispatch has no `stopper_magic` or other suppression-marker check.

The legacy datapack defines the selector tag in
`data/minecraft/tags/entity_type/unstable.json`, with `replace:false` and these exact
values:

- `minecraft:dragon_fireball`
- `minecraft:end_crystal`
- `minecraft:fireball`
- `minecraft:firework_rocket`
- `minecraft:small_fireball`
- `minecraft:tnt`
- `minecraft:tnt_minecart`
- `minecraft:wither_skull`
- `minecraft:creeper`
- `minecraft:wind_charge`
- `minecraft:breeze_wind_charge`

The Paper 1.21.11 jar and both target Loom Minecraft 26.3 jars contain no vanilla
`data/minecraft/tags/entity_type/unstable.json`; this is a legacy datapack-defined tag,
not a built-in tag. The current target resources also do not contain this tag. A port
must provide it explicitly or define an equivalent native entity set. The list includes
creepers and firework rockets as well as TNT, so treating it as a TNT-only selector would
change behavior.

## Nuke and stopper behavior

`other/stoppernuke.mcfunction` removes the local netherite-block/beacon construction,
clears the block at y+2, schedules `other/stopnuke` one second later, and kills the
current marker. `other/artifact/notnt` invokes it for any nearby `tag=nuke` marker.
`other/stopper_marker_destroy.mcfunction` also invokes it for a `tag=nuke` marker being
destroyed. The nuke setup in `other/nuke/nuke.mcfunction` tags its marker `nuke1`, and
schedules `nuke1` 20 seconds later only when no `tag=stooper` entity is within 10 blocks;
that scheduled function acts at surviving `tag=nuke1` markers. Killing the marker
prevents that delayed marker-based step. The legacy nuke/time engine and its block
cleanup sequence are not migrated to the target. Keep marker/nuke cancellation out of a
bounded anti-explosive port until that engine and cleanup lifecycle are available.

## Target hooks and practical scope

The target already routes entity load/unload events through `SkyWars2.java` to
`LbcItems.entityLoaded/entityUnloaded`. `LbcItems` tracks match-owned `PrimedTnt`
(dynamites) in a set and ticks only those entries; that set does not cover every native
TNT or minecart. `LbcClouds` tracks its `AreaEffectCloud` entities and can create native
TNT from a cloud. `LbcTrinkets` similarly indexes selected native projectiles. These
patterns offer lifecycle and cleanup points for a future anti-explosive index.

`LbcMagic` already provides the closest performance analogue for arrows: entity load and
unload maintain an arrow set, and `tickArrows` compares holder count with tracked-arrow
count. It queries local entities for each holder when holders are fewer, otherwise it
walks tracked arrows and uses Minecraft's nearby-player query. This avoids a full
world-entity scan per holder every tick. A new anti-explosive tracker could use the same
loaded/unloaded lifecycle and choose between local radius queries and a tracked-entity
set based on counts; native queries alone are simpler for a small holder count, while
indexing limits repeated queries when unstable entities are sparse. Keep the spherical
24-block distance check after any box-based query.

Recommended bounded next scope: port the holder inventory gate and radius-24 suppression
of native TNT plus the exact legacy unstable entity set, with the existing target
entity lifecycle and cleanup. Preserve the particle/sound position and then remove the
selected entities. Do not port the `tag=nuke` / `stoppernuke` branch as part of this
scope; it depends on the unported nuke marker/time engine and world-block cleanup.

For the previously ported arrows artifact, `LbcMagic.arrowHolder` now excludes players
carrying the `time_stopped` entity tag. This is a compatibility gate for the arrows
artifact only; the legacy time-stop engine itself remains unported.

## Source references

- `data/lbc/function/items/changed_inventory/sel.mcfunction`
- `data/lbc/function/players_2t.mcfunction`
- `data/lbc/function/mobs_2t.mcfunction`
- `data/lbc/function/other/artifact/notnt.mcfunction`
- `data/lbc/function/other/stoppernuke.mcfunction`
- `data/lbc/function/other/stopnuke.mcfunction`
- `data/lbc/function/other/stopper_marker_destroy.mcfunction`
- `data/lbc/function/other/nuke/nuke.mcfunction`
- `data/lbc/function/other/nuke/nuke1.mcfunction`
- `data/minecraft/tags/entity_type/unstable.json`
- Target hooks: `src/main/java/me/skywars2/SkyWars2.java`,
  `src/main/java/me/skywars2/game/LbcItems.java`, `LbcClouds.java`, `LbcTrinkets.java`,
  and `LbcMagic.java`
