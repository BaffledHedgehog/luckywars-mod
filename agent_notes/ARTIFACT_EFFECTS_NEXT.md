# Next artifact passives after Looper

Исторический план: типы `1:6`/`1:8` теперь перенесены и прошли 12 матчей /
33 группы. Текущий контракт — [ARTIFACT_PASSIVES_PORT.md](ARTIFACT_PASSIVES_PORT.md).
Следующий ещё не реализованный этап — [тип 1:5](ARTIFACT_DEBUFF_NEXT.md).

Looper reveal is implemented and its focused native checks pass. This note
plans a separate behavior migration; none of the effects below are implemented
by the Looper work.

## Source gates and current behaviors

The artifact table assigns `mystical:1,type:2..8` to resolved
descriptions. Old inventory selection marks `afkartifact5t_active` when a
matching artifact is in `container.*` or `weapon.*`; `players_5t` runs the
artifact dispatcher only for non-spectators and outside a nearby
`stopper_magic` area. Exact gates and dispatch are in:

- `function/items/changed_inventory/sel.mcfunction`
- `function/players_5t.mcfunction`
- `function/other/artifact/afkartifact5t.mcfunction`

| State | Source behavior and dependencies |
|---|---|
| `1:2` — anti-explosives | `function/items/changed_inventory/sel.mcfunction` sets `notnt_active`; `function/players_2t.mcfunction` calls `function/other/artifact/notnt.mcfunction`. That searches/kills unstable entities within 24 blocks and also stops nearby nuke entities. `function/projectiles/arrows_f.mcfunction` and `function/effects/effect_{explosion,small_explosion,big_explosion,firebreak,lightning_bolt}.mcfunction` have additional type-2 suppression gates. It spans several projectile/effect systems. |
| `1:3` — hardening fluids/falling blocks | `function/other/artifact/afkartifact5t.mcfunction` calls `function/other/artifact/nofluid.mcfunction` every five ticks. It transforms lava, water, powder snow in a ±12 block cube and converts nearby falling blocks via `function/other/artifact/block_otverditel.mcfunction`. This is a large block-edit path with falling-entity work. |
| `1:4` — block arrows | `function/items/changed_inventory/sel.mcfunction` sets `no_arrows_active`; `function/players_tick.mcfunction` calls `function/other/artifact/noarrows.mcfunction` each tick. It removes arrows within 16 blocks and emits particles/sound. A native port should react around arrow/entity lifecycle rather than scan every player and arrow every tick. |
| `1:5` — remove debuffs | `function/other/artifact/afkartifact5t.mcfunction` calls `function/other/artifact/noeffects1.mcfunction` every five ticks. It clears a fixed set of effects, with armor/item exceptions for levitation. Preserve that list and its protection gates if migrated. |
| `1:6` — remove all effects | The same dispatcher calls `function/other/artifact/nohp.mcfunction`, whose source is simply `effect clear @s`; all active potion effects are removed every five ticks. |
| `1:7` — colorful artifact | The same dispatcher checks the colored block at/under the player and calls `function/other/artifact/colors.mcfunction`. Its color branches grant effects and may affect nearby players, mobs, XP, mana, fire or world position; it has broad gameplay dependencies. |
| `1:8` — metabolism | The same dispatcher calls `function/other/artifact/metabolism.mcfunction` every five ticks. When the `hplvl` score is below `MaxHealth`, it gives Hunger III and Regeneration II for one second. Native code can use current/max health rather than mirror scoreboard state. |

## Proposed next batch

Start with `1:6` and `1:8`: both are player-local passives with no projectile,
block-region, team-score, mana, or world-generation dependency. Reuse the
inventory-change dirty-UUID pattern to maintain only eligible artifact holders;
the current `LbcMagic.artifactActive` predicate matches only `mystical:1,type:1`,
so this needs a typed holder set or a shared artifact-state snapshot. Process
the small holder set at the source five-tick cadence. Preserve the source
beacon-only suppression gate with `beaconSuppressed`, rather than assuming
the broader player-artifact suppression applies to these passives too.
For `1:6`, clear effects
only when the holder currently has effects. For `1:8`, compare health with max
health before applying the two source effects. Use gameplay eligibility so
observers remain excluded, and clear tracked state on inventory change, death,
disconnect, and match stop.

Defer `1:5` until the levitation protection conditions and exact intended
debuff list are covered; it is close to `1:6` but has more item/armor
interactions. Defer `1:2`, `1:3`, `1:4`, and `1:7` for their broader entity,
projectile, block, or world side effects. Do not infer these effects from lore
alone; the source functions above define current behavior.

This is a proposed bounded scope, not a migration or runtime validation report.
The current status of Looper and the prior beacon work is in
[LOOPER_PORT.md](LOOPER_PORT.md) and [MAGIC_PORT.md](MAGIC_PORT.md).
