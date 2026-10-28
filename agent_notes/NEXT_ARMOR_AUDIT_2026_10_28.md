# Next complete armor audit — 28 October 2026

Read-only source audit; no production edits, build, test, install, or resource-pack writes. Source root below means E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc. Current native files were checked instead of trusting historical status notes.

## Recommendation: complete Nanobots armor

This is a finite four-piece family with original live acquisition already admitted by LbcBolts ReforgeRule("nanobots","neko_set_nanobots"). Carry all four leaves, the one-piece root, five-draw pre table and four-piece full table through readiness/catalog registration. The source resources contain all seven original tables; only the root and four leaves were packaged. The pre/full tables need explicit packaging. The native runtime mechanic is absent. Native references currently comprise LbcDamage's custom-set discriminator, LbcBolts acquisition rule and LbcKfcFood's mutable score names. No separate repair mechanic was found: the authored system regenerates a nanobot tier and applies Resistance.

Exact source closure:

- loot_table/neko_set_nanobots/{helmet,chestplate,leggings,boots}.json
- loot_table/neko_set_nanobots.json, neko_set_nanobots_pre.json, neko_set_nanobots_full.json
- function/armor/nanobots/{tick1s,tick1s1,damaged,resist}.mcfunction
- function/players_1s.mcfunction:49; function/items/changed_inventory/sel.mcfunction:20,30
- advancement/cocksuckers.json (entity_hurt_player reward); function/items/damaged.mcfunction:10
- function/load.mcfunction:150,151; function/resets.mcfunction:98,99; function/other/purify.mcfunction:35,36
- function/ui/get_data.mcfunction:29 and ui/get_data_nanobots.mcfunction
- function/ultra_reforge.mcfunction:39 and native LbcBolts corresponding rule

Runtime identity is nanobots:1 on actual equipped armor slots. Item payloads retain authored leather bases, per-slot flags, durability7000, lbc:armors/nanobots_* item models and lbc:nanobot equipment asset; preserve full leaf components instead of rebuilding from lore.

One-second order is critical: resist(); if ANY equipped nanobots piece, tick1s1(); then subtract1 cooldown only while >=1. tick1s1 sequentially adds head2/chest4/legs3/feet1 when cooldown<=0 and CURRENT tier<=119. Each line rechecks current tier, so source is not an unconditional +sum or clamp120. Full equipped set then subtracts4 cooldown unconditionally (including zero/negative values). This changes later recovery and must not be normalized to zero.

Resistance refresh occurs BEFORE tier growth; bands4..11/12..23/24..39/40..59/60+ grant amp0/1/2/3/4 for40ticks, hidden particles flag; explicit colored dust still emits5 particles at y+1 spread .3/.6/.3, speed0 normal. The same helper runs immediately after damage. Crucially resist() and cooldown decrement are unconditional in players_1s: removing armor stops regeneration but does NOT consume stored tier or stop Resistance refresh. Tier therefore persists until damage or reset/purify; do not clear it on unequip.

Damage hook requires tier>=1, not equipped armor. Tier1..4 becomes0; tier>=5 subtracts5. Add5 cooldown only if existing cooldown<=20 (20 becomes25); then resist(). Damage trigger is native entity_hurt_player advancement, not every attempted hit, raw health delta, or a new melee-only callback. Reuse existing advancement admission bridge; preserve shields/absorption/native trigger semantics and avoid double handling of Time Stop replay.

Reusable native services: LbcArmorSets shared20tick phase and eligibility; LbcMana scoreboard read/write APIs (use real signed integer objectives), equipment dirty hooks; existing native advancement dispatch behind Shulker damage admission; ItemEffectLevels/native effect duration chain; AttributeHud/shared HUD composer; LootTestCatalog readiness; LbcBolts existing normal/Ultra routes; normal GameController lifecycle/reset/stop cleanup. No projectile actors or terrain service required.

External existing score producers must remain interoperable: LbcKfcFood already lists both objectives. Legacy netherite_stew sets both INT_MAX; nexus/red adds100 tier; rainbow aura blue adds3 and120 boundary; vanity saves/restores both. These do not require implementing those unported families, but prohibit private-state-only storage that disconnects existing/future score writes. Use scoreboard state; do not silently saturate or clamp signed legacy arithmetic. Purify and resets zero both; verify native equivalents include these newly introduced objectives. Lifecycle should not erase ordinary unequip state.

HUD source shows U+E114 and tier colored red/gold/yellow/green/aqua at the same bands. Preserve shared HUD arbitration and existing pack assets; no new bitmap/shader/model is needed. Physical-client qualification still needs the normal pack path, not a claim based on server tests alone.

Needed author decision: source has NO stopper_magic or stopper gate around ANY nanobots helper, including Resistance refresh, damage loss or charging. AGENTS jammer contract requires asking before assigning a missing gate/exemption. Ask whether Nanobots charge/full-set recovery/Resistance continue under MAG and TECH; existing source alone cannot settle intentional omission. Do not copy Paladin's MAG gate by analogy. All remaining mechanics are explicit enough to implement without balance invention.

Qualification should cover all threshold edges, sequential119 overshoot, full-set signed cooldown, damaged20->25 and21unchanged, unequip persistence, wrong slot/inventory exclusion, KFC/scoreboard interoperability, precise native hurt trigger, Resistance chains, UI colors, Time Stop, lifecycle and seven-table readiness/reforge routes.

## Rejected duplicates and broader alternatives

OVERLOAD_ARMOR_SOURCE.md is historical: LbcOverload currently registers all4 native armor items plus Crux, and LbcOverloadArmor owns jump/slam; LbcMana hooks bitmask, debt and tick state. Five ported leaves exist. Do not reimplement it merely because note ends with historical unbuilt movement status.

ELECTRIC_ARMOR_SOURCE.md concerns existing electrical mitigation and Techno/MK2 identity, not a distinct Electric armor family. Techno/MK2 full closure includes existing reactive flight, jump, shield, flames and set discharge consumers and is larger than Nanobots; audit those dependencies before claiming completion.

Trollmask6 alone is incomplete. Full original acquisition is loot_table/troll_box.json snowball trollge:1 -> function/projectiles/eggs.mcfunction -> other/troll_box/select random1..6 -> all six exact mask payloads. Outcome1 enables global trollhunter (NOT outcome5); tick1s then spawn_trollge_pre/spawn_trollge/trollge_spawn and mobs_1s sound_trollge become dependencies. Outcome5 places many lbc:walls templates around all ordinary players plus global title/sound. Mask6 executes items/arcanums/eyelaser_pre -> eyelaser -> eyelaser_kill every tick under MAG absence. That laser samples through terrain until first nonair, red particles,0.05 generic damage per intersecting sample, finite100.5distance; source semantics must be audited against current native geometry contract. No native troll_box/trollmask implementation was found in game Java. This is finite but requires terrain-template lifecycle and summoned-hunter service; prefer Nanobots now.
