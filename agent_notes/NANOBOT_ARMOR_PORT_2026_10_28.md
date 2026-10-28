# Nanobots armor source contract and native implementation, 28 October 2026

Implementation owner: `src/main/java/me/skywars2/game/LbcNanobotArmor.java`.
Integration, acquisition resources, HUD and native verification are owned by the coordinating change.
This note records code and source inspection; it does not claim a build, installed artifact or successful server verification.

## Author policy

(user, 28 October 2026) TECH suppresses charge accumulation and resistance refresh. Preserve source persistence while unequipped: stored charge remains and resistance can refresh without a worn piece. Ordinary charge accumulation still requires a worn tagged piece as in the source. MAG does not suppress either operation. Already-granted Resistance retains its remaining native 40-tick duration; entering TECH does not forcibly remove an effect.

The package-private `chargeAllowed(ServerPlayer)` and `resistanceAllowed(ServerPlayer)` methods now use `!game.items().magic().mechanismSuppressed(player)`. TECH also blocks the full-set cooldown subtraction of four because it is part of accumulation. The final positive-only cooldown decrement and incoming tier loss/cooldown gain continue during TECH suppression. Ordinary gameplay role/dimension guards and shared TimeStop admission still apply. Stun and ice do not suppress equipped passives.

## Read-only source

All source paths below are under `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- `function/armor/nanobots/tick1s.mcfunction`: resistance first, equipment-gated helper second, positive cooldown decay last.
- `function/armor/nanobots/tick1s1.mcfunction`: four sequential slot gains and full-set cooldown subtraction.
- `function/armor/nanobots/damaged.mcfunction`: tier loss, conditional cooldown gain, immediate resistance refresh.
- `function/armor/nanobots/resist.mcfunction`: five resistance and dust bands.
- `function/players_1s.mcfunction:49`: calls the one-second helper without a worn gate.
- `function/items/changed_inventory/sel.mcfunction:20,30`: `nanobots_active` reflects any armor slot with custom data `nanobots:1`.
- `function/items/damaged.mcfunction:10` and `advancement/cocksuckers.json`: the outer tier >= 1 gate and native `entity_hurt_player` criterion. There is no source jammer, worn, attacker, health-loss or blocked-damage filter on this Nanobots call.
- `function/resets.mcfunction:98,99` and `function/other/purify.mcfunction:35,36`: set both scores to zero.
- `function/load.mcfunction:150,151`, `function/load_scores_reload.mcfunction:80,81`: objective creation/reload handling.
- `function/ui/get_data.mcfunction:29`, `function/ui/get_data_nanobots.mcfunction`: HUD begins at tier 4, glyph U+E114, tier number and red/gold/yellow/green/aqua bands.

The non-TMT reference search also found shared score writers: KFC `other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction:70,71` adds one to each; Netherite stew sets both to MAX_INT; Nexus red adds 100 tier; Rainbow Aura blue adds 3 with its own upper check and then its own normalization; Vanity snapshots/restores both scores. These source operations must remain authoritative. The Java owner therefore uses `LbcMana.score` directly with the original misspelling `nanobots_coldown`, with no private counter cache or blanket normalization.

Acquisition references include four `loot_table/neko_set_nanobots/*` pieces, `neko_set_nanobots`, `_pre`, `_full`, four transformable armor pools, `function/ultra_reforge.mcfunction:39`, and `advancement/true_advancements/armor/nano.json`. Acquisition and HUD are integrated outside this owner. TMT text/tag catalog entries are not behavior.

## Exact runtime order

On each shared twenty-active-tick armor pulse, the owner first grants resistance and particles using the existing tier, even with no piece worn. With any tagged armor worn, it checks head, chest, legs and feet in that order, gaining 2, 4, 3 and 1 respectively. Each individual addition independently requires current cooldown <= 0 and current tier <= 119. The check is repeated after each addition; there is no final clamp. Thus a chest piece can take 119 to 123, while a full set starting at 119 gains only the head's 2 before subsequent slots fail. At tier 118 a full set gains only 2 to 120. Negative tiers are accepted by these source comparisons.

A full set then subtracts four cooldown regardless of whether tier gains were permitted. This is unconditional with respect to the current tier/cooldown and can drive the cooldown negative. Finally every admitted player with positive cooldown loses one, including players wearing no piece. A full set at cooldown 5 therefore becomes 1 then 0, with no gains that pulse; at cooldown 4 it becomes 0 and does not receive the final decrement. A tier crossing a resistance threshold during charging receives the new band on the following pulse, not immediately.

At each actual native hurt-player criterion, the outer tier >= 1 gate is checked. Tier 1..4 becomes 0; tier >= 5 loses 5. Cooldown <= 20 gains 5 (20 becomes 25), while >20 remains unchanged. Then resistance/particles refresh immediately at the resulting tier. Neither charge loss nor cooldown gain needs a worn piece. The owner does not infer criterion events from positive health damage; shield and absorption behavior follows the actual vanilla criterion call. `withIncomingAttempt` captures pre-hurt gameplay admission so fatal criteria are not lost after the player's alive flag changes.

Resistance uses 40 ticks, nonambient, hidden particles/icon, source player attribution, and `ItemEffectLevels.create`. Bands are tier 4..11 amplifier 0, 12..23 amplifier 1, 24..39 amplifier 2, 40..59 amplifier 3, and >=60 amplifier 4. Below tier 4 the source grants nothing; it does not remove any already-running Resistance instance. Stronger or longer unrelated native effects retain vanilla merging behavior.

Each qualifying refresh also sends exactly five normal-distance dust particles at wearer x/y+1/z, spread .3/.6/.3, speed 0, scale 1. The source float colors are (.82,.275,.275), (.82,.584,.275), (1,.933,0), (.427,.82,.275), (0,.969,.969). The native particle API takes packed RGB; each float channel is converted to its native eight-bit component by truncating channel*255. No visual counter/tier changes are cached.

## Integration and lifecycle

Construct `LbcNanobotArmor(GameController)` within the existing armor owner. Call `pulse(ServerPlayer)` from its existing `pulse20` before any unrelated early return, without another ticker, scanner or player collection. The owner independently enforces ordinary participant/role/dimension/liveness and TimeStop guards, matching the equipped passive policy.

Nest `withIncomingAttempt(player, nativeHurt)` around the already-shared native incoming wrapper; call `incoming(player)` once at the native `EntityHurtPlayerTrigger` hook. Do not additionally call it from a generic hurt callback, which would duplicate losses and omit blocked criteria.

`reset(player)` sets both authoritative scores to 0 for the established source reset lifecycle (death/respawn, purify, match reset as applicable). It does not delete Resistance; the source reset helper only resets scores here. `forget(player)` keeps native scoreboard values and has no retained player-state cleanup; disconnect/reload therefore preserves source counters. `stop()` resets currently online native players. Existing match/player reset admission handles players who next enter from offline state. The only temporary Java state is a synchronous, exception-safe incoming-attempt call stack; it owns no persisted timers.

The implementation adds no gameplay tick, inventory sweep, world scanner, item normalization, source writes, live overrides or resource-pack changes. Build and runtime qualification must be recorded by the coordinating integration after it completes.

## Root integration and qualification - 28 October 2026

Builddb830314 SHA256db83031463e9994195e1b6b7cba4af8be43c62c213d99e2d83e0a81a4b9af44e passes34recorded results in2actual isolated
mode1matches,111.07seconds. Exact546Java sources/1551compiled classes/3232processed
resources match the selected JAR.198preexisting author paths and29live overrides
are byte-identical; child stop and exact settings/profile/scoreboard/player-file
snapshot restoration are normal. Source-only knockback audit passes1302processed
tables. No physical-client pixels/audio or peakTPS claim.

Root wired the existing LbcArmorSets pulse20, native IncomingArtifactCriterionMixin
criterion/attempt wrapper, shared HUD E114 and public-purify reset. A private clear
variant preserves counters during actual disconnect cleanup; ordinary equip changes
never reset them. Native match stop and GameController purify/respawn/reset paths
zero both. Tests cover real public clear and disconnect cleanup as well as actual
stop. No tick listener or inventory-wide scanner was added.

All7tables equal original source after established codec migration. The source
pre/full files existed but were not previously packaged; explicit build includes
now supply both. Catalog exposes all7; normal and Ultra native reforge Work plus
imported full-set PvP criterion dispatch are checked. No real new fatal PvP death
was used solely for this advancement assertion. All four RP item/model pairs, equipment,
RU/EN keys and existing U+E114 glyph were read-verified; no template additions.

Native tests include threshold/dust geometry/effect layers, exact signed cooldown
and sequential119gate, carried/wrong-slot behavior, scoreboard edits/KFC ledger,
TECH-only suppression and bare persistence, real stun/noMagic and TimeStop/roles,
real environmental/rejected/absorbed/fully shield-blocked hurt,200actual scheduled
player connection ticks per match and shared armor/combat regressions. KFC's full
existing food suite also passes; no new execution of outcome7 is claimed by the
isolated nano interoperability assertion.

Initial6a82223d failed the fixture's absorption assertion because native max_absorption
was0 and setAbsorptionAmount(10) silently clamped to0. The corrected fixture sets
capacity100 and positively checks10yellowHP before the unchanged single hurt call.
Initial log/results/artifact proof remain under build/verification/nanobot-armor.
Early compile-only test API mismatches (26.3 criterion package/color access) were
corrected before native testing. No production behavior was weakened to pass tests.

Installer11820 targets qualifieddb830314 over main31b84a20 after ordinary shutdown
and world unlock, without forced stop/restart. Exact main-install receipt records
whether installed or waiting. Artifact and qualification:build/verification/nanobot-armor/.

## Ответы автора (user)

Чем подавлять накопление заряда и выдачу Сопротивления наноботной брони:

> Только глушителем механизмов

Сохранить накопленный заряд и обновление Сопротивления после снятия всей брони:

> Сохранить как в легаси

Эти ответы относятся к броне. Они не задают новое правило для отдельной Nano-ловушки.

Receipt confirms worker11820 installeddb830314over31b84a20, without restart.

Obsoletec33bd703and failed6a82223darchives were exact path/SHA-verified and removed,81,920,772bytes. Retain db830314/31b84a20/b3c13ce9 plus network audit tool.
