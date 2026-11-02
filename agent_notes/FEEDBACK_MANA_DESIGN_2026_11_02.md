# Mana, source presentation and custom effect commands — 2 November 2026

Scope: feedback #9, #13, #14, #16 and #19. Prepared source changes; no compilation,
native fixture, deployment, server restart or physical-client verification was run
by this agent. Root owns final integration and qualification.

## Source values and mana lifecycle

`LbcSourceConfig` creates `config/luckywars/source-values.json` on the first game
controller initialization. Its `values` object contains 98 editable integer
operands. The first creation migrates existing `lbc.math` scores when present,
including zero and signed overrides; absent values use canonical LBC
`function/config.mcfunction` defaults plus the static `cfgarc1/2/3/4/m` operands
from `function/load.mcfunction`. Runtime `time_stopped` is deliberately excluded.
Defaults are bundled in `skywars2/source_config_defaults.json`.

Edit that external file, then run `/skywars reloadconfig` as an operator. Parsing,
known-key validation, exact integer validation and all derived arithmetic complete
before any live score changes. The same live scoreboard operands remain the
existing native mechanics' source. Seven display prices derive from their real
per-action operands with the original cadence (Babylon 10, Sun 4, other arcanums
20). Do not put independent values for these derived prices in the JSON: configure
their base `*_mana_tick` operands. Reload refreshes online container views once.
There is no periodic file read or inventory scan.

Two formerly hardcoded Academy prices now have dedicated editable operands:

| Operand | Original default | Actual consumer |
| --- | ---: | --- |
| `academy_wall_wand_mana_use` | 500 | `LbcWands.Spell.ACADEMY_WALL` |
| `academy_baffled_wand_mana_use` | 1000 | `LbcHedgehog` Academy branch |

Their recipient tooltips replace only the relevant score operand; base Wall and
Hedgehog retain their own 1500/2500 defaults and source lore structure. Legacy
constructor assignments are ignored only during initial `LbcItems` construction
for registered configuration operands, after migration/configuration has already
initialized those scores. Later genuine scoreboard changes still work.

`LbcMana.initializeResources` creates each missing player score independently:
`mana=5000`, `manamax=5000`, `manaregen=10`, and visual regeneration from the
existing base value. Existing zero/negative player resources are preserved. Both
the living-player load path and first resource access use it. Root must also call
it at player JOIN, before the first read-only HUD projection.

## Recipient tooltips and native presentation

`LbcTooltipScores` resolves score components in recipient-only Polymer copies,
including nested translated arguments and sibling components. Authoritative item
stacks and their trees are unchanged. Knowledge masking takes precedence. All 86
source tooltip operands in the bundled metadata are covered by the config
registry. Numbers remain signed literals with original source styles.

The common missing-design cause was bare native item registration: for example,
`LbcWands.register` supplied model/durability but no source custom name, lore or
tier. Gallery's native-registry fallback consequently exposed blank defaults.
The existing `AuthoredItemDefaultsMixin` now fills missing presentation defaults
once during native component initialization. It uses source metadata extracted
from current converted `data/lbc` loot/modifier definitions, without rewriting
those source definitions. There are 974 exact source presentations and 50
Academy native-pair tier rules derived from the existing `LbcAcademy.Wand` pairs.

Explicit registered presentation components win; authored item names also win.
Existing source stack patches, player names/lore/tiers, models, custom data,
attributes and TMT modifications keep their existing authority. This helper does
not rewrite runtime inventories. Academy fallback defaults retain their real tier
and appended green `upgraded_item` description. Baguette was identified through
the read-only RP key `item.skywars2.oguzok_sword`: its native default is now
`lbc:tier_d`. The four native chemical potions also inherit source lore and hidden
`potion_contents` display when those defaults are absent, preserving their native
potion contents and explicit components.

Original RP, language files and protected author `LbcAcademy.java` were not edited.

## `/skywars effect` command reference

`/skywars effect <id> <time>` applies the selected custom status to the invoking
active gameplay player. Both effect and reload subcommands enforce operator
permission on their own nodes, so merging into a public `/skywars` parent cannot
drop permission checks. IDs autocomplete. Bare integers mean seconds; `10s` and
`200t` explicitly select seconds/ticks. Positive durations must fit in signed
integer ticks. Seconds-based source counters round up to their native granularity.
The existing immunity, cleansing, pause and lifecycle owners remain authoritative.

All 52 supported IDs:

```text
acid_rifle all_seeing_eye blaze delayed_death dimensional_shifting
effect_add_mana effect_add_manamax effect_add_manaregen effect_big_explosion
effect_boost_mana effect_chain_lightning effect_evoker effect_evokerarrow
effect_explosion effect_firebreak effect_gravity_curse effect_hedgehog_boom
effect_kiuaskivi effect_lightarrow effect_lightning_bolt effect_nuking
effect_remove_mana effect_rtp_1 effect_rtp_2 effect_rtp_3 effect_shock
effect_small_explosion effect_snus effect_stand_arrow_hit effect_timestop
effect_traps effect_vacuum effect_witch_gens_reset frozen gatling_fovos
gigaponos living_legend lookback nomagic personal_stasis piss polymorph ray
reverse_mana scared screamray shock skill_issue stun target twitching
void_resistance
```

`personal_stasis`, `polymorph`, `stun`, and `effect_gravity_curse` use the actual
native producers and authoritative countdowns. The gravity grant shares the same
modifier installation as the existing melee event; that event retains its RNG,
sound and targeting order. Self-stun uses an explicit self-only entry into the
existing stun owner; ordinary projectile stun still excludes its caster. Other
IDs use their real existing source-counter adoption/queue owners. Queued source
actions retain their original queue semantics and may leave already-created
native consequences after their queue duration ends.

Excluded: resources, charges, cooldowns, nanobot tier/progression, permanent
attributes, match-count `overheaven`, cold buildup/intensity and arbitrary
mcfunctions. These are not generic timed statuses. The source `effect_timestop`
queue remains distinct from direct timed `personal_stasis`. Vanilla effects keep
the native `/effect` command.

Root integration: register `SkyWarsFeedbackCommands.register(dispatcher)` after
building the `/skywars` parent. The parent must have copied children, not a
redirect to `/sw`, because Brigadier redirects would bypass its new local
children. Preserve existing `/sw` and other compatibility aliases. Root also
owns copying/redirecting other custom command roots beneath `/skywars`.

## Checks and remaining qualification

Executed: `python tools/check_feedback_mana_design.py` — PASS, 974 source
presentations / 50 Academy pairs / 86 source tooltip operands / 98 settings /
7 derived prices. Modified-file whitespace checks pass. Pinned local 26.3 source
was read to verify component initializer, score, component tree and command APIs.

`ManaFeedbackChecks.run(game, fixturePlayer)` is prepared for root's native
fixture: duration parsing/rejection, preservation of zero/negative resources,
nested recipient resolution without author mutation, separate Academy prices,
native gallery defaults, custom patches and Baguette D. Caller owns fixture
snapshots. Root must compile, run native qualification, verify initial brand-new
UUID before first HUD, test actual command parsing/permission/tab completion and
check physical-client tooltips. This note does not claim those checks passed.

Frozen owned files: `LbcMana`, `LbcSourceConfig`, `LbcTooltipScores`,
`AuthoredItemPresentation`, `SkyWarsFeedbackCommands`, `ManaFeedbackChecks`,
`AuthoredItemDefaultsMixin`; three narrow price edits in `LbcWands`, one in
`LbcHedgehog`; native grant additions in `LbcPendingCombat`, `LbcPolymorph`,
`LbcShooters`; three bundled metadata JSON files and the targeted Python check.
