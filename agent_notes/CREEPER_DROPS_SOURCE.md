# Creeper special drops — source audit (22 October 2026)

Research only; no code/build/server/RP changes. Main69de8601 remains unchanged.
Legacy root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/` below is read-only.

Subsequent implementation and native qualification are recorded separately in
[CREEPER_DROPS_PORT.md](CREEPER_DROPS_PORT.md). The author selected throwing-player
kill credit; the pending questions at the end describe the audit's earlier state.
No source geometry correction was requested, so20/16 payloads are preserved.

## Exact dispatch and payload

- `function/drops/specialdrops.mcfunction:1–2`: ordinary `creepers` first, `creepers_old` second, before all five chest flags and TNT. Every matching flag executes; final two lines kill count1 or decrement the count once. Reuse `LbcSpecialDrops`; extend its ordered dispatch, recognition and common debit, never add another token scan.
- `function/drops/creepers.mcfunction:1–20` contains **20**, not10, native creepers. No rotation, Y offset, fuse override, ignition, powered flag, name, persistence or tags. Every actor has `DeathLootTable:"lbc:creeper_wand"`. Ordered `(dx,dz,radius)`:
  `(2,0,1),(-2,0,2),(0,2,1),(0,-2,2),(2,-1,1),(-1,2,2),(-2,-1,1),(-1,-2,2),(-2,-2,1),(2,2,2),`
  `(2,0,1),(-2,0,2),(0,2,1),(0,-2,2),(2,1,1),(1,2,2),(-2,1,1),(1,-2,2),(-2,-2,1),(2,2,2)`.
  Keep overlaps and order. Position is the item entity's exact coordinate, not floored chest position.
- `function/drops/creepers_old.mcfunction:1–31`: **16** native creepers at local `^ ^ ^4`, radii1/2 alternating. Between summons the source rotates the executing item +20 yaw (15 rotations), retaining pitch: angles0 through300 degrees, not a complete evenly spaced360 ring. Final item yaw increases300 per activation. No custom death loot: ordinary vanilla creeper loot. This is a separate old variant, not an extra ring attached to ordinary.

## Tokens and original appearance

- `loot_table/traps.json:94–135`: repeating command block; weight10, quality-10; count1; model `lbc:drop_boss`, tooltip `lbc:tier_ff`; name `trap_creepers`, yellow, italic=false; one lore `trap_desc`, white, italic=false; `{creepers:1,drop:1}`.
- `loot_table/legacy_luckyblock.json` old entry: barrier, count1, empty components and `{drop:1,creepers_old:1}`; no invented old name/model/lore. `loot_table/imba/traps.json` ordinary entry is repeating command block, weight0/quality-10, empty components, `{creepers:1,drop:1,imba:1}`. Preserve distinct source records if exposed; ordinary first.
- Active original RP inspected read-only at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (documentation's26.3(1) is historical). Keys are in `assets/minecraft/lang/{ru_ru,en_us}.json`, not assets/lbc/lang. English existing values: `trap_creepers` = `Trap: Creepers`; `trap_desc` = `Active Drop. Throw the item to activate.` Existing `assets/lbc/items/drop_boss.json`, model and tier_ff tooltip sprites are present. No new RP assets or keys required for ordinary token. Air model `assets/lbc/items/st/air.json` and existing Air translations are present.

## Reward readiness and reuse gaps

- `loot_table/creeper_wand.json`: one referenced-table entry `lbc:sauvojen_tuntija_air`, independent random_chance0.05; no killed-by-player condition. It replaces normal creeper death loot for ordinary summons.
- Both tables are copied in `src/main/resources/data/lbc/loot_table/`. However the Air legacy leaf still names `minecraft:carrot_on_a_stick`, not native `skywars2:air_wand`; copied presence alone does **not** establish a ready native death-loot path. Use the qualified native `LbcWands.AIR_WAND` / `lbc:ported/air_wand` reward and complete source components/lore, with exactly one5% roll per eligible death. `LbcWands.java:95,158` registers ordinary Air and its native behavior; existing GameChecks20530/20888 exercise ported loot.
- Reuse `LbcTntDrops` actual item-thrower capture/retained cause, epoch/time-stop and quiet cleanup patterns; do not treat an autonomous creeper as a PrimedTnt or inherit its attribution policy without the author's decision. Creeper AI target and explosion damage source need role/allied guards consistent with the selected policy. Include potion cloud and chained native TNT as delayed consequences.

## Local26.3 native facts

Inspected `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `net/minecraft/world/entity/monster/Creeper.java`: ordinary player-nearest target, HurtByTarget retaliation, native movement/avoid cats, SwellGoal and default30-tick fuse. No instant detonation. `SwellGoal.java` begins within squared distance9, defuses if target dies, exceeds49, or loses line of sight. Native melee itself returns true without damage.
- Creeper saves/loads `powered`, `Fuse`, `ExplosionRadius`, `ignited`; defaults false,30,3,false. `Mob.java:382–406` independently persists `DeathLootTable`/seed. Existing ordinary source overrides only radius/death table; old only radius. Creeper swell progress is not among these saved fields.
- `Creeper.explodeCreeper()` sets dead, uses native `Level.ExplosionInteraction.MOB`, emits active-potion cloud, triggers death mob effects, then discards. This bypasses ordinary combat death loot: **self explosion should not roll the5% wand**. Ordinary combat death uses custom table; old uses vanilla. Quiet stop/unload cleanup must not manufacture combat death or rewards.
- `ServerLevel.java:1230–1232`: MOB interaction obeys MOB_GRIEFING and MOB_EXPLOSION_DROP_DECAY; with griefing false block interaction KEEP. Entity blast effects remain native. Do not substitute TNT interaction/global TNT policy accidentally.

Author decisions needed before implementation: autonomous hostile-mob attribution/owner self-and-allied interaction (root is collecting this); whether any intended correction to exact20 ordinary /16 incomplete-ring old source is desired. Preserve source facts unless explicitly changed. Cages excluded from this batch. No runtime qualification was performed.
