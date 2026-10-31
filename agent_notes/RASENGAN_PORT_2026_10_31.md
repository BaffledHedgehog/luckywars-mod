# Rasengan source closure and implementation — 31 October 2026

Source is read-only `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Exact execution closure: `players_tick.mcfunction:7` (native snowball-use score),
`items/snowballs.mcfunction:4` (`gadget:1`), `projectiles/eggs.mcfunction:11`
(`rasengan:1` plus suppression), `other/rasengan.mcfunction:1..108` (terminal;
no further function calls). Raw `loot_table/rasengan.json` produces one normal
snowball with custom data `{gadget:1,rasengan:1}`, `lbc:rasengan` model,
`lbc:tier_c` tooltip and unchanged source name/lore. `lucky_items.json` references
that table. No resource modifications are needed. This closes the Rasengan branch
of `CHAOS_TRAP_CLOSURE_2026_10_28.md:773`, not adjacent gadget branches.

The spell uses the throwing player's current feet plus exactly 1.62 Y and current
rotation, irrespective of snowball location. Suppression is evaluated at feet:
magic stopper/beacon radius24, inventory mystical/type2 radius12, active non-spectator
antimagic holder radius20. Nearby non-spectator player feet within4 of the eye origin
receive hidden ResistanceIV20; caster receives hidden ResistanceV20. The native
radius1 MOB explosion order was indices `0..25,11..25,11..25,8..25,11..25,11..25`,
each index multiplied by .2 blocks:104 source helper explosions, no wall clipping.
The user clarified that repeats accumulated knockback and requested one explosion
per point. The implementation emits26 native radius1 MOB explosions, with the
native `ExplosionDamageCalculator.getKnockbackMultiplier` set to x1 at indices0..7,
x2 at8..10 and x6 at11..25. Damage remains the unmodified native power1 amount;
no additional impulses, artificial hit counts or custom damage multiplication.
Caster then receives hidden LevitationI20 and SlowFallingI200. Source contains no
mana debit, custom cooldown or snowball removal. Native use owns debit/stat/flight.

`LbcRasengan` only queues actual loaded native snowballs until ordinary item tick;
shared Time Stop holds this pending work. Shared `LbcMagic` owns exact suppression,
`LbcIndirectKills` owns captured WILD attribution and terrain causality, native
explosions own damage/armor/knockback/terrain, and native effects own all clocks.
No helper creepers, synthetic projectiles or parallel spell/physics state. Damage
keeps the `killed_by_rasengan` display attribution without creating a physical mob.
Short uncommitted dispatch may be cancelled by Vanity; native entities/player
effects remain under existing capture owners. No resident adapter is needed.

Integration: construct/access `items.rasengan()`, forward entity loaded/unloaded,
ordinary tick, player forget and match stop. Shared magic method
`rasenganSuppressed(ServerLevel,Vec3)` supplies the source gate. Register
`RasenganChecks.run(game)` as suite `rasengan`. No custom item-use hook is needed.

Checks use actual native main/offhand use and load callback, one-item debit/stat,
caster geometry despite displaced snowball,26 exact cells/native multipliers, native
damage and protected roles, exact effect clocks, retained snowball/once dispatch,
shared stasis and logout/stop cancellation. Existing Chef terrain/player/stat/
scheduled-tick restoration plus Attribution snapshots restore the fixture exactly.
All26 calculators also run actual native explosions on the same restored native
cow, comparing resulting impulse against x1 baseline and asserting unchanged HP
damage at each x1/x2/x6 cell.
No Gradle or native server was run by this subtask; root owns build and execution.

User decision resolves repeated distant explosions as knockback accumulation.
Ownerless source explosions remain WILD, consistent with existing source explosion
ports; no new ally exemption is inferred.

## Root integration and native evidence (31 October 2026)

Runtime/check build and actual `rasengan` suite passed on `d3ce7e02`.
All26 native calculators were exercised against restored target state, including
unchanged HP damage with x1/x2/x6 impulses, two-hand debit/flight, original effects,
suppression, pending lifecycle and protected roles. The named source now passes
shared role/Greed admission even though the native blast has no physical emitter.
The final archive retains this production component. Unsupported pending throw
dispatch is cancelled on rewind through the existing queued-work boundary.
See [batch evidence](DAMAGE_REACTIONS_AND_REWARD_CHAIN_2026_10_31.md) for exact
archives, failed receipts, preservation and release limits. Earlier no-run text
records worker handoff, not current verification status.
