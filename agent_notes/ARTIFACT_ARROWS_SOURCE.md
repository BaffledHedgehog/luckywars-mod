# Mystical Artifact arrows variant source

This note records the legacy source behavior behind `mystical_artifact_arrows.json`.
The source server and its resource pack were read only.

## Loot payload

Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`.
The selected entry is the repeating command block with custom data
`{transformable:1,accessory:1,unresolved_artifact:1,mystical:1,type:4}`. The importer
migrates the loot schema, keeps that entry's complete functions and its parent pool
settings, and changes only its item id to `skywars2:mystical_artifact`. The independent
comparison in `tools/check_design_sources.py` selects the source entry separately and
compares the complete generated loot table. `build.gradle` allowlists the new output.

The Polymer server item id is shared with the other mystical artifact variants. The
original item model and component metadata remain in the loot payload; client rendering
still depends on the user's resource pack, which is read only.

## Effect and gates

The legacy source is Minecraft/Paper 1.21.11; the target project is Fabric 26.3. In the
legacy source, `data/lbc/function/items/changed_inventory/sel.mcfunction` adds
`no_arrows_active` when an item carrying `mystical:1,type:4` is found in `container.*`
or `weapon.*`, and removes the tag when neither search finds one.
`data/lbc/function/players_tick.mcfunction` runs `lbc:other/artifact/noarrows` when `@s`
has that tag. The legacy shared player tick is called by `mobs_tick.mcfunction` only
for non-spectator players not tagged `time_stopped`; the alternate
`time_stopped_tickentity.mcfunction` also calls it for players not tagged
`time_stopped`.

The effect function selects every entity matching
`type=#minecraft:arrows,distance=..16` from the holder's current position. For each one,
it emits `minecraft:cloud` at the arrow with offsets `0 0 0`, speed `0.3`, count `20`,
normal mode; plays `minecraft:block.fire.extinguish` on `master` at that arrow position,
volume `1`, pitch `1`, addressed to `@a`; then kills the selected arrow. The exact tag
payload was checked locally in both the old server jar
`E:/servers_mew/cringe1_21_4_skywars/versions/1.21.11/paper-1.21.11.jar` and the target
Loom Minecraft 26.3 jars under
`E:/servers_mew/skywars2_last/luckywars-mod/.gradle/loom-cache/minecraftMaven/`.
Both contain `data/minecraft/tags/entity_type/arrows.json` with exactly
`minecraft:arrow` and `minecraft:spectral_arrow`; there is no project datapack override.
The tag does not include tridents.

Sound delivery was additionally checked against the old Paper 1.21.11
`PlaySoundCommand` bytecode and the target 26.3 source: with volume 1 and
the default minVolume 0, only players in the same level within 16 blocks
receive the sound. The native port preserves this culling and reuses one packet
for its listeners; `@a` does not mean audible sound in other dimensions.

The native arrow implementation checks the holder's `time_stopped` entity tag in
`LbcMagic.arrowHolder`. This is a compatibility gate for the arrows artifact; it does
not mean the legacy time-stop engine has been ported.

## Behavior gotchas

- There is no owner, shooter, or self-owner exclusion: the holder's own nearby arrows
  are affected along with other players' arrows.
- There is no in-ground check, so embedded arrows are also affected and killed.
- `distance=..16` is measured from the holder executing the player tick, not from the
  artifact item or each other; the effect covers a 16-block sphere.
- The selector has no stopper marker or stopper-radius condition. The artifact tick is
  not suppressed by `stopper_magic` in these source gates.
- The loop has no item-drop or arrow-recovery command: selected entities are killed.
- The older `noarrows_old.mcfunction` used a separate 4-block selector that summoned
  arrow items before killing entities. It is not called by the current `players_tick`
  gate described above.

## Source references

- `data/lbc/loot_table/artifact.json`
- `data/lbc/function/items/changed_inventory/sel.mcfunction`
- `data/lbc/function/players_tick.mcfunction`
- `data/lbc/function/mobs_tick.mcfunction`
- `data/lbc/function/time_stopped_tickentity.mcfunction`
- `data/lbc/function/other/artifact/noarrows.mcfunction`
- `data/lbc/function/other/artifact/noarrows_old.mcfunction`
- Local vanilla tag in old Paper 1.21.11 jar:
  `data/minecraft/tags/entity_type/arrows.json`
- Local vanilla tag in target Fabric 26.3 Loom jars:
  `data/minecraft/tags/entity_type/arrows.json`
