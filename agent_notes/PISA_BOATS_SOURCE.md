# Pisa boats source and resource audit

23 October 2026. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Implementation/qualification status belongs to the runtime contract; this document records source facts and resource checks, without claiming a server or client run.

## Acquisition and dispatch

- [Normal source token](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/barriersnottraps.json:700>): repeating command block, count1, weight10, quality10; `lbc:tier_b`, model `lbc:drop_not_trap`; custom name `drop_pizanski` yellow, italic=false; one lore line `trap_desc` white, italic=false; custom_data `{pizanski:1,drop:1}`.
- [Legacy acquisition table](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/legacy_luckyblock.json>) was searched in full: no `pizanski` token or separate old variant exists. Only the normal leaf is exposed by this port.
- [Source dispatch line64](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/specialdrops.mcfunction:64>) reads source custom_data, faces the world origin, forces pitch0, then offsets world Y by+20 before invoking `drops/pizanski`. It does not target the nearest player for tower placement.
- [Tower function](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/pizanski.mcfunction:1>) has 61 oak boats at local `(0,i,-0.4*i)`, i=0..60, plus the chest boat at `(0,61,-24.4)` (61 ordinary boats and one chest boat total). Bottom and chest boats have visible translated name `pizansk`; all other boats retain vanilla naming. The origin-facing local frame matters: these are separate boat summons, with no riding links or source invulnerability/NoGravity/TTL.
- Before spawning: oak-plank block particles at world Y-10, spread `(0.5,8,0.5)`, speed0, count100, normal; wood-break sound, master, all players, volume1, pitch1.
- Source admission/debit is the shared dispatcher. Author/epoch attribution and role protection are native migration requirements, not provided by the source function.

## Chest slot14 relic

The chest boat starts with exactly one dirt in native container slot14. The function's last command replaces that item's entire component set when `#lbcskill swrg.math=1` and the nearest nonspectator player's `lbc.skill=9`. No other chest slots are authored.

Both variants preserve custom_data `{craftusable:1,govnoq:1}`, custom name `{translate:"bullshit",italic:false}` with inherited color, and no authored lore. They remain real native dirt carrying real enchantments and mainhand attributes.

| Component | Base (`lbc:pisa_relic`) | Upgraded (`lbc:pisa_relic_upgraded`) |
| --- | --- | --- |
| Tooltip style | `lbc:tier_b` | `lbc:tier_a` |
| Smite / Bane / Looting | 10 / 10 / 10 | 20 / 20 / 20 |
| Knockback enchantment | 1 | 2 |
| Knockback resistance | +1, ID `kfnfousdangdgrf` | +2, ID `1239251353566` |
| Attack damage | +5, ID `minecraft:base_attack_damage` | +10, same ID |
| Armor | +10, ID `68942427` | +20, ID `6834242597` |
| Armor toughness | +10, ID `387081226` | +20, ID `3871081226` |

Every modifier is `add_value`, slot `mainhand`. Source IDs are retained exactly (unqualified IDs naturally resolve in the vanilla namespace). Loot JSON uses the project's 26.3 `modifier` / `type` schema, with flat native enchantment and attribute component forms. Static parsed comparison confirms both complete component objects equal the source slot14 objects. Runtime registry decoding and actual native equipment behavior still require the parent qualification run.

## Native API and resources

- Item `skywars2:pisa_boats_drop`; public leaf `lbc:ported/pisa_boats_drop`.
- `LbcPisaDropItem.register()`, `Kind.NORMAL.item()/flag()`, `matches(stack)`, `matches(stack, kind)`, `kind(stack)` follow the existing drop-item API. Matching accepts copied `{pizanski:1,drop:1}` components on any nonempty item, preserving source-flag dispatch.
- Native reward tables `lbc:pisa_relic` and `lbc:pisa_relic_upgraded` each produce one dirt; the runtime selects the table and writes the result to slot14. Both are listed explicitly in `skywars2/loot_test_catalog.json`; the public leaf is covered by its existing `lbc:ported/` prefix.
- Actual RP was read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/`.

| Existing key | ru_ru / rpr | en_us |
| --- | --- | --- |
| `pizansk` | Пизанский сральник | Pisa shithole |
| `bullshit` | Дерьмо | Bullshit |
| `drop_pizanski` | Дроп: Пизанский Сральник | Drop: Pisa Shit |
| `trap_desc` | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |

All three actual language files contain all four existing keys, at lines843/844/2609/2627. The source spelling and styles are retained. Existing model/style resources are reused; no RP or template changes were made. This confirms serialized design inputs, not client-rendered pixels.

## Bounded next-drop audit (source only)

A bounded inspection of short remaining dispatch leaves did not establish another complete small closure suitable for this batch. `forest` is an 800-tick recurring marker, random configured-feature placement, native spread and a special zombie with `lbc:reactive_shish` rewards; it is not just its one-line entry. `ultraponos` reaches a structure template, skill variant, timed producer, custom loot and multiple creeper explosions. `tmt` reaches a large item mutation/trigger subsystem. `greedcurse` creates an instant-item `greedstone`, requiring its separate behavior to be migrated. `shadow_citadel` requires its authored dungeon structure and contents. None is implemented or advertised ready by this resource patch. Continue with a separate closure audit rather than exposing their unfinished acquisition paths.
