# Projectile visual timing audit

Снимок **до исправления**; актуальное поведение Terra описано в
[PROJECTILE_VISUAL_SYNC.md](PROJECTILE_VISUAL_SYNC.md).

Read-only source audit, 2026-10-16. No code, legacy server, or resource pack was changed; no build or server was run. Scope: Java source in `src/main/java` plus existing project notes/check fixtures.

## Finding

- **Only Terra is a custom projectile with an attached virtual item display.** `LbcTerra.TerraProjectile` (`src/main/java/me/skywars2/game/LbcTerra.java:176-225`) owns `ElementHolder` + `ItemDisplayElement`, attached by `EntityAttachment(holder, this, false)`. `attachModel()` sets `interpolation_duration=1` (`setInterpolationDuration(1)`) and `teleport_duration=1` (`setTeleportDuration(1)`); `syncModel()` updates angles then calls `holder.tick()`; `destroyModel()` calls `holder.destroy()`.
- **All five Terra variants share this same display lifecycle:** `Kind.REGULAR`, `ACADEMY`, `SWORD`, `TRUE_CHARGE`, `TRUE_BEAM` (`LbcTerra.java:86-91`). There is no separate projectile visual class per variant.
- **Observed timing shape matches the reported risk:** `LbcItems.tick()` calls `terra.tick()` (`LbcItems.java:774-783`); during gameplay `GameController.tick()` invokes `items.tick()` (`GameController.java:429-434`). `LbcTerra.tick()` runs swept movement/collision through `step()` and removes a projectile when `step()` returns false (`LbcTerra.java:284-308`). `step()` sets the projectile to the collision endpoint, calls `syncModel()` (and therefore `holder.tick()`), then returns false for shield/block hit (`LbcTerra.java:357-367`); `remove()` destroys the virtual display and discards the entity (`LbcTerra.java:475-480`) in that same manager tick. Thus the movement/display update and destruction are issued in one server tick; source alone cannot prove which packet a connected vanilla client renders first, but Terra is the sole direct match for the user's one-tick visual/hit symptom.

## Other projectile visuals checked

- `LbcClouds.AcidProjectile` is a native `ThrowableProjectile` with Polymer `MARKER` fallback (`LbcClouds.java:77-110`), not an `ItemDisplayElement`/`ElementHolder` visual. Native `super.tick()` owns flight/collision; block hits stick in place and entity/non-block impacts can discard. `LbcClouds.tick()` separately advances effects/age (`LbcClouds.java:207-224,339-360`). Acid has no virtual display whose removal could race the hit animation.
- `LbcTrinkets.shot()` creates native `SmallFireball` and `FireworkRocketEntity`, then tracks them for cleanup (`LbcTrinkets.java:102-137`). These use native projectile visuals; no holder tick or custom display destroy path. `LbcWands` also creates native SmallFireball/FireworkRocket projectiles (`LbcWands.java:182,472`). Bow/crossbow projectiles likewise remain native (`TrinketBowMixin.java:26-32`; see `LbcTrinkets.spawn`).
- Full Java-source search finds no other `ItemDisplayElement`, `ElementHolder`, `EntityAttachment`, `ManualAttachment`, `nativeDisplay`, `PolymerProjectile`, `setTeleportDuration`, or `setInterpolationDuration` use beyond Terra and the non-projectile `ZoneBarrier` (`ZoneBarrier.java:24-59,77-86`). The zone barrier uses a manually positioned display and holder ticks, so it is not exposed to projectile hit/removal ordering.

## Reusable code and existing checks

- No shared projectile-display helper/factory exists. Terra's own `attachModel` / `syncModel` / `destroyModel` are the only direct lifecycle implementation. `agent_notes/TERRA_REGULAR_API.md:9-13,20,42` documents the already chosen Polymer attachment pattern and viewer lifecycle; it explicitly relies on `EntityAttachment` plus manual `holder.tick()`.
- Existing server-side checks to reuse: `GameChecks.verifyTerraDisplay()` verifies model and transform (`GameChecks.java:8186-8203`); regular/academy behavior and cleanup dispatch are `GameChecks.java:579-590`, including match-stop display cleanup (`GameChecks.java:7735-7739,8105-8112`); sword behavior/cleanup dispatch is `GameChecks.java:591-596` and its fixture verifies shared Terra display and cleanup (`GameChecks.java:8657-8671`). These check object state/lifecycle, not client packet/render ordering.
- Acid checks already cover native gravity/drag, swept block collision/sticking, persistence and finite lifetime (`GameChecks.java:3592-3671`). Trinket-projectile checks exercise actual native firework behavior and cleanup (`GameChecks.java:1008-1015,1075-1248`). Neither category currently contains a custom virtual projectile display.
- `ZoneBarrier` is a nearby reference for a second `ElementHolder` lifecycle, with `ManualAttachment`, explicit `holder.tick()`, and `holder.destroy()` (`ZoneBarrier.java:43-59,77-86,112-115`); its attachment strategy is different and should not be copied as a projectile helper.

## Audit conclusion

The only other source-confirmed candidate that shares the reported failure mechanism is **none**: all Terra variants are the one root-owned fix target; acid, trinket/wand fireball and firework, and other native shots do not have the coupled virtual-display tick/destroy path. Existing Terra fixture methods can retain regression checks for model, collision, and cleanup. The source has no connected-client assertion for collision-frame packet ordering, so existing checks do not establish that visual timing is fixed for a real client.


