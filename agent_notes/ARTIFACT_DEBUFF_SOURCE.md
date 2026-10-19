# Mystical artifact type 5 source contract

The type-5 item payload is imported from the unique `minecraft:repeating_command_block`
entry in the legacy `data/lbc/loot_table/artifact.json` whose custom data contains
`unresolved_artifact:1,mystical:1,type:5`. The migrated standalone table is
`data/lbc/loot_table/ported/mystical_artifact_debuff.json`; only its server item
id changes to `skywars2:mystical_artifact`. The source weight, quality, count,
model, tooltip, translated name, lore, and remaining custom data are retained.

## Runtime effect selection

`function/other/artifact/noeffects1.mcfunction` clears these effects every five
ticks while the type-5 passive is active:

- Always clear: blindness, slowness, weakness, mining fatigue, glowing, hunger,
  instant damage, nausea, poison, unluck, and wither.
- Clear levitation only when none of the six protections below is present.

The levitation checks are exact slot and custom-data checks:

1. `armor.feet` with `{reactive:1}`
2. `armor.feet` with `{technomk2:1}`
3. `armor.legs` with `{technomk2:1}`
4. `armor.legs` with `{techno:1}`
5. Any `container.*` slot with `{reactive_shish:1}`
6. Any `weapon.*` slot with `{reactive_shish:1}`

The last two checks cover carried inventory and either hand. These protections
apply only to levitation; the other eleven effects are always cleared.

## Combined passive ordering

`function/other/artifact/afkartifact5t.mcfunction` dispatches type 8 first,
type 5 second, and type 6 last. Type 8 (`metabolism`) applies Hunger III and
Regeneration II when the source health condition is met. Type 5 then removes
the newly applied hunger, while leaving regeneration. Type 6 (`nohp`) performs
an unrestricted effect clear last, so it also removes effects from types 8 and
5 in a shared cycle. This note records source behavior; it does not claim the
type-5 runtime behavior has been implemented in the new mod.

## Source files

- `world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`
- `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noeffects1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`
- `agent_notes/ARTIFACT_DEBUFF_NEXT.md`
