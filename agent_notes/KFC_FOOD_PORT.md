# KFC edible port — 20 October 2026

`LbcKfcFood` owns native `skywars2:kfc_chick` registration, the existing
pre-consumption event, all twenty equal random outcomes, and the finite
`gigaponos` map. The item copies native cooked chicken FOOD and DEFAULT_FOOD
CONSUMABLE behavior; original model, tooltip, custom data, name and lore come
from the existing ported KFC loot payload. Existing resource-pack files remain
read-only. Creative players follow the same active-match role checks.

Source: read-only `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`:
`function/other/eatables/kfc_chick.mcfunction`, every dispatched child function,
`function/other/effect_gigaponos.mcfunction`, all eight attribute helpers and
update helpers, plus the four food outcome loot tables. Source audit and
questions: `agent_notes/KFC_SOURCE.md`.

## Behavior

- `consumed(player,stack)` runs before native stack spending and nutrition,
  matching the established ConsumableSkillMixin event. `applyOutcome(player,n)`
  is package-private for meaningful deterministic game checks. Random outcome
  four retains its independent uniform 38-slot roll, including empty slots.
  Confirmed by the owner (user): replace only that one slot with one item.
  Source `/loot replace` without a slot count
  can overwrite following slots because `govno_lot_2` splits 64 unstackable
  items. That bug is fixed; source slot labels and 38-way choice are retained.
- All source messages, sounds, particles, effect seconds/amplifiers/visibility,
  XP levels, native chicken, global noon change, mana and eight base attributes
  are represented directly. Attribute command flooring is retained, including
  negative luck (Java integer casts would truncate toward zero instead).
  Native EffectCommands treats instantaneous effect duration as ticks, so the
  food helper does the same (saturation 600 stays 600 ticks, not 12000).
- Outcome thirteen emits `govno_lot`: ten rolls of three govno, not 300.
  Its optional skill-nine buffs use the established config/player skill data.
- Outcome fourteen adds 400 to the active eater timer. Every tick emits one
  existing `govno` roll, dirt/brown-wool five particles each, original rocket
  sound with radius 50/minimum volume .3, and the precise sounds at
  350/300/250/200/150/100. No invented propulsion or global entity search.
  Repeated eating adds another finite 400 ticks. Only active timer entries are
  visited. The scoreboard mirrors the owned timer; it is not polled.
- Full cleansing and debuff cleansing must call `clear(player)` immediately.
  Starting/ticking gigaponos uses the existing HUNGER rejection policy, which
  includes both full and debuff cleansing artifacts. Death/leave/respawn/end/stop
  go through existing LbcItems lifecycle wiring. Native potion effects continue
  through the common native rejection/cleanup hooks.
- Outcomes sixteen/eighteen issue the exact `win_button` and `penis_cultus`
  loot tables. Their abilities/native item IDs belong to the separate output
  service, not this food service.

## Outcome seven boundary

The literal source version is isolated in `addExactSourceScores`. The checked-in
list retains all 112 ordered additions, including `reverse_mana` twice and the
commented-out nexus compass exclusion. `reverse_mana` and `nomagic` are routed
through existing LbcMana setters so cleansing policy stays authoritative.
The user clarified that the bonus targets player counters in the current port;
attributes remain the separate source outcome eight. The source ledger is kept
as bookkeeping; it never increments server clocks or unrelated global state.
Live owner bridges now cover Psycho scared/lookback/twitching/piss (+1 each),
Psycho screamray (+1 tick), Electro shock (+1 pulse), cyber/yellow katana charge
and charging counters (+1 each), and Blaze Head (+1 charge through its existing
grant API instead of a duplicate raw score add). Existing custom-effect rejection
remains authoritative for afflictions, shock, screamray and Blaze Head.
`food_count` is already scoreboard-owned by LbcColors. Other event-time legacy
score writes retain `have_lucky_effects`; absent item handlers remain bookkeeping.
Root integration owns additional shooter/Terra/current-player counter bridges.
This document does not claim all 112 legacy side effects are live.

## Wiring and resources

Root integration owns shared LbcItems registration/constructor, consumption,
tick, clear, forget and stop calls, plus LbcMagic.clearDebuffs and architecture.
Food service itself adds no framework, recurring inventory scan or entity scan.

`tools/import_kfc.py` includes source-schema migration for `govno_lot`,
`govno_lot_2`, `win_button`, `penis_cultus`, in addition to the existing gun and
food imports. Output service owns the final native-ID remaps of win_button and
penis_cultus in that importer and their resource payloads.
Required resource includes (in addition to existing govno):

- `data/lbc/loot_table/ported/kfc_gun.json`
- `data/lbc/loot_table/ported/kfc_chick.json`
- `data/lbc/loot_table/kfc_yumyumyummy.json`
- `data/lbc/loot_table/govno_lot.json`
- `data/lbc/loot_table/govno_lot_2.json`
- `data/lbc/loot_table/win_button.json`
- `data/lbc/loot_table/penis_cultus.json`

## Verification performed

No compilation or server start performed by this task. Read-only source/data
checks verified all twenty switch cases, exact ordered 112-score equality and
reverse_mana multiplicity against the source; exact schema-migrated equality of
the four outcome tables before the output service native-ID remaps; existing
KFC RU/EN keys, model/texture and tooltip sprite presence via the importer.
Native 26.3 source ZIP confirms food defaults, instantaneous effect duration,
clock manager noon API and helper symbols. Runtime gameplay, immediate-cleanse
integration and lifecycle checks remain the root task's required validation.
