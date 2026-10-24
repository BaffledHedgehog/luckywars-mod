# Defensive arcanums: bounded code review (2026-10-24)

Read-only review of `LbcDefensiveArcanums.java`, its `LbcItems`/`LbcDamage` integration, the two scoped loot leaves, and `DefensiveArcanumChecks.java`. No code edits/builds performed. User rulings take precedence: direct immunity except void, no Resistance or activation fee, 500% regeneration once per player, fresh health per healing hand.

## Finding: check fixture loses pre-existing attribution

`DefensiveArcanumChecks.java`, Fixture.close(): calls `indirectKills().clear(owner)` and `.clear(other)`. Saved and delegated `GameChecks.WandThirdPlayerState` do not save/restore the indirect-kill service's fall, status, mana-debt, hunger-debt or later-attack maps (`LbcIndirectKills.java:689-693`). Constructor checks clean held/frost state and Time Stop but does not require clean attribution. Thus a successful check run on players with an existing pending cause permanently clears it. Test damage also changes native last-attacker state without restoration. Use verified fresh test actors or add a scoped attribution snapshot/restore if running against actors with existing combat state. This is a test-isolation issue, not an identified normal-gameplay arcanum failure.

## Runtime review result

No confirmed runtime blocker found within the bounded scope. Healing rechecks native health separately before main/off hand, applies vanilla instant health amplifier0, pays only below max HP, and does not clear attacker attribution. Barrier charges one call per dispatch regardless of one/two hands, no activation fee/Resistance; long multiplication safely rejects prices exceeding int range and zero regeneration legitimately costs zero. Negative regeneration clamps to zero. At-impact held-item check removes immunity immediately after unequip. Paid frame/previous-frame lease accounts for END-tick service order; tests intentionally advance service dispatches explicitly rather than claiming server ticks elapsed.

`LbcDamage.allowDamage:160-163` rejects barrier damage before environment attribution gate. `SkyWars2.java:156-160` invokes Academy beforeDamage only after acceptance; AFTER_DAMAGE callbacks are guarded by accepted nonzero damage. Existing mana-debt damage using fell_out_of_world bypasses barrier by the explicit void ruling. Dual terrain changes are once per successful dispatch, strict bounded cells, wrapped in support candidates and trapChanges with persistent holder cause. Tests exercise actual support deletion/void credit and later accepted player's superseding hit.

Integration includes static registration, construction, END dispatch, forget and stop. Visibility/role/world checks and noMagic/antimagic prevent use outside active participants. Time Stop/frost pause dispatch; damage lease naturally expires with server tick advancement rather than extending unpaid protection indefinitely. No unauthorized source/author-pack writes found in resource scope.

## Verification limits

No tests were executed by this reviewer. Existing checks cover native loot identity, healing order/full-health recheck, price exact/below/zero, one/two hands, absent activation/Resistance, actual damage/void/unequip, terrain/drop/support credit, suppression/frost/time stop, observer and stop probes. They do not directly assert overflow rejection or preservation of a pre-existing passive attacker debt through healing/rejected barrier damage. Those are coverage gaps, not asserted runtime defects.

Parent changed Barrier leaf to four lore rows by removing obsolete activation-cost row under the user ruling. Remaining original `500%` price/name/lore/components/flags are retained; this authorized difference supersedes the earlier exact full source parity statement in DEFENSIVE_ARCANUMS_RESOURCES.md.

## Fixture isolation correction

Parent added a preflight requiring both fixture players to have no pending target-keyed indirect-cause entries. Native lastHurtByMob/player references and their timestamps/memory are saved/restored exactly, and the full profile is restored before native player resources. Checks now reject actors with existing indirect combat instead of clearing their pending causes. Runtime validation belongs to the parent suite.
