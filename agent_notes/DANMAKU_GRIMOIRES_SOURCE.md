# Consumed Danmaku grimoire: source trace

Read-only source research, 2026-10-25. No implementation, acquisition change, build, server run, or behavioral verification. Existing sheep resource files are preserved.

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Paths below are relative to this root unless explicitly native.

## Narrow scope and feasibility

A self-contained explicit Danmaku-grimoire leaf and consumed-item handler appears possible without enabling other grimoires. The only direct Danmaku entry is spellid **159**, `libspell_danmaku`. **160 is not Danmaku**: `libspell_swarogia`, model `lbc:spelllib/swarogia`, and `function/items/libspells/select.mcfunction:72` routes it to `workingitems/sauvojen_staffs/ruby_preloop`. There is no separate Academy Danmaku grimoire entry. Do not synthesize one from the staff upgrade.

Native `src/main/java/me/skywars2/game/LbcDanmaku.java:97` already exposes package-local `queueExternal(p, hand, academy)`: adds 20 independently queued ticks, captures hand and Academy state, emits without holding the consumed item, and pauses for time stop / suppression. This is infrastructure, not an enabled grimoire or acquisition route. A future direct 159 handler would pass `academy=false`, consume exactly one, and invoke this queue, after normal gameplay/action/suppression checks. Sneak bomb/refund behavior below requires deliberate handling; queueExternal alone does not implement it. Broad `spells_library`, random grimoires, universal wand crafting, and random-spell 127/128 routes should remain excluded.

## Exact source entry (only 159)

`loot_table/spells_library.json:11572` onward, unique spellid159 entry:

```json
{
  "type": "minecraft:item",
  "name": "minecraft:snowball",
  "functions": [
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_c",
        "minecraft:max_stack_size": 99,
        "minecraft:item_model": "lbc:spelllib/danmaku"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "libspell_danmaku",
          "color": "white",
          "italic": false
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        [
          {
            "translate": "type",
            "italic": false,
            "color": "aqua"
          },
          {
            "translate": "libspell",
            "italic": false,
            "color": "aqua"
          }
        ],
        {
          "text": "",
          "type": "text"
        },
        {
          "translate": "danmaku_wand.lore.1",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.2",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.3",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.4",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.5",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.6",
          "color": "white",
          "italic": false
        },
        {
          "translate": "danmaku_wand.lore.7",
          "color": "white",
          "italic": false
        },
        [
          {
            "translate": "rarity",
            "italic": false,
            "color": "white"
          },
          {
            "translate": "rare",
            "italic": false,
            "color": "yellow"
          }
        ]
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{transformable:1,gadget:1,libspell:1,mana:1200,unresolved_grimoire:1,spellid:159,spellwithcooldown:1}"
    }
  ]
}
```

The table actually has 240 entries in this snapshot, one pool with `rolls:1`; 159 has no explicit weight/condition, so its direct probability is 1/240 in this source table with default equal weights. A narrow deterministic leaf must not pretend that this original broad distribution is preserved. Do not expose the whole table to obtain this one item. Existing wrappers include `loot_table/random_grimoire.json`, `random_grimoires.json`, and `27_grimoires.json`.

## Real consumption path and mana

`function/load.mcfunction:21` creates `snowball` as `minecraft.used:minecraft.snowball`. `function/players_tick.mcfunction:7` dispatches used snowballs to `items/snowballs`; that calls `projectiles/eggs` when the nearest snowball has `gadget:1`. `function/projectiles/eggs.mcfunction:30` dispatches `libspell:1` to `items/libspells/select`. Vanilla throwing consumes the snowball item; `select:1` reads spellid from its Item custom data, adds caster/raycaster tags, sets Distance=0, and `select:5` kills the thrown libspell snowball within 10 blocks. `select:71` calls `items/wands/danmaku_staff/click_2` directly for159, bypassing staff `click`.

This consumed route does not test or subtract player mana. `mana:1200` is stored grimoire metadata, not an observed spend in this throw path. Staff `click.mcfunction` instead calls click_2 then subtracts `danmaku_staff_mana_use`. Do not incorrectly charge1200 just because it appears in the item tag. `unresolved_grimoire:1` is removed by the Looper skill's `looper_explain_grimoire` helper after replacing the blank lore row with `mana_usage_2` + the stored mana value; it is not random spell resolution. Universal-wand crafting reads stored mana, but is a separate feature.

`click_2.mcfunction`: non-sneak adds20 to player danmaku; sneak casts bomb if charge>=50 and unconditionally refunds `danmaku_staff_mana_use`. Because libspell bypasses staff debit, this source route has a real sneak refund discrepancy. Do not silently describe it as a zero-cost normal staff cast or claim queueExternal preserves that branch. Native normal staff rejects insufficient bomb charge and does not grant that old refund.

## Duration, patterns, and source holding quirks

`players_tick` ticks danmaku while its score>=1 unless a stopper_magic marker is within20. `items/wands/danmaku_staff/tick.mcfunction` derives score modulo20 and parity, dispatches right for even and left for odd, then removes1. It specifically recognizes held spellid159 (ordinary patterns only) and also has a fallback to right-hand patterns on even ticks if neither a held159 nor any held/container Danmaku staff exists. Thus the source attempts to keep firing after consuming the final grimoire, but holding/inventory state can affect dispatch. Native external queue intentionally decouples emission from that remaining stack.

One addition20 is20 ticks (~1 second at20TPS), ten phases for a captured hand. Right normal patterns at modulo0,18,16,...2 call shoot_1..10; left at19,17,...1 calls shoot_1l..10l. Right per-phase totals:9,2,2,2,2,2,9,4,4,4 =40 cards,8 homing. Left:2,2,2,2,2,2,2,2,2,17 =35 cards,12 homing. Right combines a fan, paired lateral/diagonal positions and homing bursts; left ends in a larger homing burst. These are helper command counts, not independently observed runtime outcomes. Source fallback after final consumption favors right; native captured offhand queue would instead preserve offhand pattern, a behavior choice to acknowledge.

`shoot.mcfunction` and `shoot_homing.mcfunction` create card item displays with `lbc:card_1` / `lbc:card_2`, copying caster identity and team. `detect.mcfunction` invokes detect_rec three times/tick; detect_rec advances0.4 per substep, homing steers by0.06 toward a valid nearest target, collision kills at nonair, and age limit is60. `damage.mcfunction` deals0.65 generic and credits3 danmaku_score per hit. Source cards omit an attacker; native captures kill credit without adding attacker impulse. `click_bomb` requires50 charge, copies charge to bomb Distance, zeroes charge, and may mark staff Academy eligibility at1500; bomb helpers and display raycasters are already native Danmaku behavior, not grimoire variants.

## Chance routes (excluded dependencies)

Direct159 dispatch has no random success/failure chance. `select.mcfunction:7-11` rerolls spellid128 uniformly1..242 up to five times while the result stays128; it can land159, but would require all other spell outcomes to port faithfully. `select:43` routes127 to `workingitems/random_spell_lib`, which rolls1..53; outcome53 adds20 danmaku (1/53), the other52 are other spells. These are random grimoires, not extra direct Danmaku entries. Neither belongs in the narrow159 port.

Remaining work before implementing: decide old sneak refund behavior and source post-consumption hand quirks, preserve authored item components/name/lore/model/keys, provide only an explicit leaf/acquisition intentionally scoped to159, wire transaction-safe consumption and queueing, and verify native behavior in the supported build/runtime. This document is static research only.
