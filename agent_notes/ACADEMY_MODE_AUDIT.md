# Academy mode mana audit — 2026-10-20

Scope: Java audit followed by the integrated fix: free Shift+RMB mode selection at any mana balance, preserving cast prices and interaction protections. Source line numbers in the inventory describe the pre-fix snapshot.

`mana.visible` is eligibility, **not** a mana balance/HUD check: [LbcMana.java](../src/main/java/me/skywars2/game/LbcMana.java#L135) checks gameplay access, alive/removed, map and ending stage. Keep it. `trySpendWand(required,cost)` additionally checks active eligibility/time/antimagic, affordability (Overload exception), and writes mana even for cost=0 ([LbcMana.java](../src/main/java/me/skywars2/game/LbcMana.java#L154)).

| Mode wands | Selection gate / suggested edit |
| --- | --- |
| Stonekivi (4), Ruby (3), Teleport (3), Air (3), Blind (4), Stone (4) | [LbcWands.java](../src/main/java/me/skywars2/game/LbcWands.java#L374): remove only `mana.get(player)<mana.score(spell.cost,"lbc.math").get() && mana.overloadArmor(player)!=15` from branch guards at original lines 375–376, 395–397, 410–412, 425–427, 440–442, 455–457. Keep visible, time, antimagic and pre-dispatch readiness/location guards. |
| Fire (3), Exasperato (4), Electro (2) | [LbcWands.java](../src/main/java/me/skywars2/game/LbcWands.java#L469): already have no balance/cost gate; preserve their eligibility/time/antimagic/frost guards. |
| Wall (4, pitch selects direction) | [LbcWall.java](../src/main/java/me/skywars2/game/LbcWall.java#L34): remove only `mana.get(owner)<500 && mana.overloadArmor(owner)!=15` at 37. Keep visible/time/antimagic and outer Wands time/area antimagic checks. Casting remains fixed 500. |
| Bedrock (5) | [LbcBedrock.java](../src/main/java/me/skywars2/game/LbcBedrock.java#L77): already free before paid room creation; retain `mana.visible`, lifecycle/frost/time/antimagic and pitch 89..90 no-op behavior. |
| Blaze (2), Fireblaze (4) | [LbcBlaze.java](../src/main/java/me/skywars2/game/LbcBlaze.java#L82): delete numeric affordability return at 87 only. |
| Lightning Bolts (3), Boltbolt (4) | [LbcBolts.java](../src/main/java/me/skywars2/game/LbcBolts.java#L247): delete affordability return at 255 (`!donor`); Lightning modes already switch freely. Keep Reforge cast cost=0 special case and paid casts. |
| Consuming (4) | [LbcConsuming.java](../src/main/java/me/skywars2/game/LbcConsuming.java#L144): remove selection `trySpendWand(owner,price,0)` at 149. Existing `castable` covers the helper's non-resource protections; retain `ready()` and normal paid cast at 156. |
| Duplicating (4) | [LbcDuplicating.java](../src/main/java/me/skywars2/game/LbcDuplicating.java#L90): remove selection `trySpendWand(owner,price,0)` at 95. Existing `castable` covers the helper's non-resource protections; retain `ready()` and normal paid cast at 102. |
| Falling Stars (3), Exahoming (4) | [LbcHoming.java](../src/main/java/me/skywars2/game/LbcHoming.java#L114): delete numeric affordability return at 119 only. |
| Psychowand (4), Psychoblind (4) | [LbcPsycho.java](../src/main/java/me/skywars2/game/LbcPsycho.java#L159): delete numeric affordability return at 164 only. |

Total: 21 Academy mode wands; 16 have affordability gates, 5 already select without affordability checks. No global mana helper change is required. Preserve all normal `trySpendWand` calls after mode returns, configured costs, Overload casting behavior, cast bonuses and trial handling.

Shared dispatch: [LbcWands.use/cast](../src/main/java/me/skywars2/game/LbcWands.java#L317) enforces offhand configuration and `actionsBlocked`; [LbcItems.actionsBlocked](../src/main/java/me/skywars2/game/LbcItems.java#L617) is frost/stun. Delegated valid/paused/castable checks preserve observer/gameplay, lifecycle, time stop and antimagic protections. Do not move mode selection ahead of these guards.

## Applied and verified

All listed affordability checks were removed. Selection-only `trySpendWand(price,0)` calls are gone; actual cast payments remain. Delegated cost reads now happen after the mode return, so selection neither reads the price nor writes mana. The five already free selectors retain their behavior. No new mode framework/class or resource-pack key was added.

Build `fc764811ef43fd254fc390a308baa73d5d8d04ff6d2adeccabb19bf58c117c87` passed eight native server suites in 33.30 seconds. `academy-modes` covers all 21 IDs, both hands, 0/-1234 mana, 300 cycle/wrap transitions, unchanged mana/Mage maximum and no new entities/spell jobs; 84 unpaid actual mode-1 casts are still refused. Stone/Blind/Air casting and stop regressions passed. Matrix uses the real server `LbcWands.use` dispatcher; it does not synthesize client input packets. No manual real-client visual or mass-load/MSPT claim.

Archive: `build/verification/skywars2-academy-modes-fc764811.jar`; evidence: `build/verification/academy-modes-verification.json`. Not installed on the main server. The user rule is recorded in AGENTS.md for future wand ports.
