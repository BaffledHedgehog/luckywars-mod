# BrBad kit 17: old-source mechanics contract

Read-only audit of the disabled LBC datapack in `E:/servers_mew/cringe1_21_4_skywars`. This records source behavior; it does not claim that every old datapack function ran in the live server.

## Item issuance and dispatch

- `data/lbc/function/swrg_kit_integration/game/kit_select/select.mcfunction:28` gives loot `lbc:kit/brbad` for scoreboard `lbc.kit=17`.
- `data/lbc/loot_table/kit/brbad.json` rolls four separate entries (one each): `minecraft:lingering_potion`, each with `minecraft:raid_omen` custom effect duration `20` ticks and amplifier 234, 235, 236, or 237. Colors are respectively 3672329, 16760906, 10197915, 16777215. Potion contents are hidden in tooltip. Names/lore translate keys are `termit`/`termit.lore`, `sulfur`/`sulfur.lore`, `phosphin`/`phosphin.lore`, `gremuch`/`gremuch.lore`.
- `data/lbc/function/tick.mcfunction` calls `tick_not_timestopped` unless `time_stopped=1`. Each server tick, `tick_not_timestopped.mcfunction` executes as every `area_effect_cloud` outside `minecraft:nexus` and `minecraft:imprinted`, if it has any `raid_omen` custom effect. It then calls `raycastselector_area_cloud` as/at that cloud. This handler itself has no tick-rate gate.
- `raycastselector_area_cloud.mcfunction` reads the `raid_omen` amplifier into scoreboard and dispatches exactly: `-22` thermite, `-21` sulfur, `-20` phosphine, `-19` fulminate. Loot values 234–237 map to those signed values in stored NBT representation. `126` routes a separate shock effect. It is a cloud-effect dispatcher, not a geometric raycast. Do not assume Bukkit/native amplifier representation: verify how the target API exposes 234–237 versus the stored negative values before mapping.
- None of these handlers checks a cloud owner, team, observer state, or match state. The cloud remains the command source; TNT is summoned without copied owner data. Area targets use `tag=!spectator`, which is a datapack entity tag filter, not evidence of the target mod's observer/dead/team policy.

## Handler contracts

### Thermite — `function/effects/termit_tick.mcfunction` and `termit_tick_storage.mcfunction`

Runs once per cloud tick. Increments cloud scoreboard `Lifetime` by 1; when `Lifetime=1`, adds another 100, so the first working value becomes 101. Computes integer storage values `tmp1=floor(Lifetime*0.0125)`, `tmp2=floor(Lifetime*0.0078)`, `tmp3=floor(Lifetime*0.003)` (commands use `store result ... int`). The repeated `fill` corners are `(+r,+r,+r)` to `(-r,-r,-r)`: axis-aligned cubes, not spherical radii.

At radius `tmp1`, replacement order is significant: `#to_nylium -> crimson_nylium`; `#logs -> crimson_hyphae`; `#leaves -> nether_wart_block`; `#to_shroomlight -> shroomlight`; `#to_soulsand -> soul_sand`; `#to_magma -> magma_block`; `#to_netherrack -> netherrack`; `#fluids -> obsidian`; `#airs_to_fire -> fire`; `#to_lava -> lava`. Then at `tmp2`, `#solid -> magma_block`; at `tmp3`, existing `magma_block -> lava`. Tags resolve in the `minecraft` namespace (`data/minecraft/tags/block/`). `#logs`/`#leaves` are vanilla tags; the datapack overrides/customizes the listed other tags. Thus source sequence permits later passes to see blocks written by earlier passes.

Every tick emits 5 `flame` particles at `~ ~.5 ~`, spread `.2,.5,.2`, speed `.5`, force mode. With `lbc:chance20` (20% each tick), plays lava-extinguish sound to players within 20 blocks, volume .5, pitch 1, minimum volume .1. It runs `termit_tick_storage` as cloud context. Then each entity in `#mobs` within Euclidean `distance=..tmp1`, except datapack tag `spectator`, takes `damage @s 4 lava`. No explicit owner/team exclusions.

The axis-cube replacement extents are at most `tmp1` blocks on each coordinate axis; mob damage uses a spherical distance selector. The stored int conversions mean small values can be 0; there is no explicit work cap or radius clamp in these functions. In the old `minecraft` tag `to_netherrack.json`, `#minecraft:bamboo_plantable_on` is a vanilla tag reference; for 26.3 this removed/renamed vanilla tag must be updated to `#minecraft:supports_bamboo`.

### Sulfur — `function/effects/sulfur_tick.mcfunction`

Every cloud tick: places template `lbc:air11x11` at `~-5 ~-5 ~-5`, with flags `none none` and integrity `.005`; emits 10 `sneeze` particles, spread 4 on each axis, speed 0, normal mode. A `chance3` predicate (3%) runs `drops/muravei_acid` from `~ ~1 ~`.

Then every tick gives entities of `#minecraft:mobs` with `OnGround:1b`, within spherical radius 6 and `tag=!spectator`, Slowness duration 2 seconds/amplifier 4 and Wither duration 10 seconds/amplifier 0. Those are effect durations in seconds (vanilla command syntax), unlike SNBT short tick fields.

Acid helper chain: `drops/muravei_acid.mcfunction` plays zombie-villager-cure sound; summons marker at local `^ ^ ^` with `data.lifetime=300` and tags `raycastacid, player, slowraycast, shield_blockable2, magic`; sets its rotation from the helper execution context, sets scoreboard `lbcID2=-100`, calls `lbc:universal_razbros`, then adds `actived`. `universal_razbros.mcfunction` randomizes yaw by -30..30 degrees and pitch by -20..20 degrees (stored as hundredths) and increments `Lifetime`. `function/raycastselector.mcfunction` ticks `slowraycast` entities, dispatching `tag=player` to `raycastselector_player`; that dispatch routes `tag=raycastacid` to `lbcr:raycast_acid`.

The `lbcr:raycast_acid` chain **does move the ordinary muravei marker**. In `raycast_acid.mcfunction`, unless it has `nogravity`, if the current block is in `#minecraft:airs`, execution is shifted `^ ^ ^0.5` and then `tp @s ~ ~ ~ ~ ~2` runs at that shifted origin: this advances the marker 0.5 blocks and adds 2 degrees pitch. If the current block is not air, that advance is skipped. The separate `nogravity` branch calls `raycast_acid2_inair2_player`; it advances in 0.4-block steps only while `Distance` is 1..4 and the next block is air, but `muravei_acid` does not set `nogravity` or initialize `Distance`, so the ordinary helper uses the 0.5-block branch. This is incremental movement, not a geometric collision raycast.

Damage is checked before the root function's final movement commands: `execute positioned ^ ^ ^-0.5 if entity @e[type=#minecraft:mobs,distance=..2.5,tag=!spectator] run function lbcr:damagewithid2` shifts execution origin half a block behind the marker, gates the damage helper on at least one eligible mob within 2.5 blocks of that shifted origin, and retains marker as `@s`. `damagewithid2` repeats the radius query from that shifted origin, tags nearby mobs `nodamage` when their `lbcID2` or `team_number` equals the marker’s, then selects the damage branch based on marker `on_fire`; it clears `nodamage` afterward. Ordinary state applies Poison for 15 seconds amp 0 (`damagewithid2_lite`, excluding `tag=muravei`); on-fire state deals 3 `on_fire` damage and emits flame particles (`damagewithid2_increased`). On-fire state is added by fire/lava blocks at `^ ^ ^-0.5`, current position, or `~ ~-1 ~`, or by on-fire mobs within 2.5 or on-fire raycasters within 3.5. The two root fire-state `execute` checks are sequential: if the non-fire leaf marks the marker `on_fire`, the following on-fire check runs in this same tick. The root also has a 20% fire-extinguish sound chance; leaves emit sneeze (non-fire) or flame (on-fire). No block corruption occurs in these functions. `data.lifetime=300` is generic max lifetime; the selector increments Lifetime once per tick and only the `on_fire` leaf adds 3 more, so the marker lasts about 300 ticks while not on fire or 75 consecutive on-fire ticks (mixed periods vary). The old Paper `SwrgNativeService.runNativeRaycastSelector` is a generic route and lifetime increment. Shield handling is separate in `lbc:function/items/shields/shield_block_2.mcfunction`: within 2 blocks it sets `donotblock` for same player ID/team; otherwise it kills `shield_blockable2` raycasters only when the block at the raycaster is air. The `lbcr:` sources are under `data/lbcr/function/`, separate from the requested `lbc:` functions.

### Phosphine — `function/effects/phosphin_tick.mcfunction`

Every cloud tick, each `#minecraft:mobs` entity within spherical radius 8 and `tag=!spectator` receives Poison 10 seconds amp 0, Wither 10 seconds amp 0, Weakness 10 seconds amp 0, and Slowness 10 seconds amp 0 (in that order). No OnGround filter or explicit owner/team exclusion.

### Fulminate — `function/effects/gremuch_tick.mcfunction`

Every cloud tick summons TNT at the cloud location with `{fuse:2s}`, then kills the cloud. In SNBT, `2s` is a 16-bit short with numeric value 2. TNT fuse is measured in ticks: this is two ticks, not two seconds. The kill prevents a second summon on later cloud ticks.

## Other source implementations found

- Old Paper Java `plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:542` maps kit id 17 to loot table `lbc:kit/brbad`; `:1720` contains only a generic raid_omen type check for active effects. Search found no BrBad cloud/tick or acid-marker handler in plugin Java or Ignite Java sources. Datapack source provides the concrete behavior above; do not treat Java search absence as proof of runtime activation.
- The text marker names are also present in resource-pack language JSON (`termit`, `sulfur`, `phosphin`, `gremuch`); that is display text, not implementation.

## Items to resolve/verify for port

1. Verify actual old runtime amplifier representation and the target 26.3 API accessor mapping. The datapack stores 234–237 but dispatcher matches -22..-19; don't compare blindly to a Java `getAmplifier()` value.
2. Confirm how port gameplay policy should filter active cloud handlers. Old dispatcher excludes only two dimensions; handlers exclude only datapack `spectator` tags on targets and have no owner/team/round/death checks. Existing project `RAY_CLOUD_NEXT.md` asks to respect current game damage/observer policy, which is a port policy beyond these source handlers.
3. Confirm intended damage attribution for clouds/TNT and the acid marker. Source gives acid `lbcID2=-100` and no owner identity; cloud handlers have no explicit owner assignment or copy. `lbcr:raycast_acid` moves 0.5 blocks per air tick and delegates nearby damage to `damagewithid2*`; preserve its shield logic and lifecycle if matching source behavior.
4. Termite radius growth is unbounded in source. Any cap, operation budget, or reduced tick frequency changes source behavior and needs an explicit gameplay decision; block modifications cover an increasingly large axis-aligned cube per tick.

## Key exact paths

Old datapack root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- `lbc/loot_table/kit/brbad.json`
- `lbc/function/swrg_kit_integration/game/kit_select/select.mcfunction`
- `lbc/function/tick.mcfunction`, `lbc/function/tick_not_timestopped.mcfunction`
- `lbc/function/raycastselector_area_cloud.mcfunction`
- `lbc/function/effects/termit_tick.mcfunction`, `termit_tick_storage.mcfunction`, `sulfur_tick.mcfunction`, `phosphin_tick.mcfunction`, `gremuch_tick.mcfunction`
- `lbc/function/drops/muravei_acid.mcfunction`, `universal_razbros.mcfunction`, `raycastselector.mcfunction`, `raycastselector_player.mcfunction`
- `lbcr/function/raycast_acid.mcfunction`, `raycast_acid_on_fire.mcfunction`, `raycast_acid_not_on_fire.mcfunction`, `raycast_acid2_check_if_fire.mcfunction`, plus referenced `damagewithid2`, `_lite`, and `_increased` helpers
- `minecraft/tags/block/{to_nylium,to_shroomlight,to_soulsand,to_magma,to_netherrack,fluids,airs_to_fire,to_lava,solid}.json`
- `lbc/predicate/{chance3,chance20}.json`






