# Exahoming checks

Added 18 October 2026. Static preparation only; no Gradle or server was started by the checks agent.

Root verification completed 19 October 2026 on SHA e985d0ba: build, two native
Exahoming matches and a shared regression match passed. See [SERVER_TESTS.md](../SERVER_TESTS.md)
for exact coverage, the pre-existing Polymer warning and performance limits.

- `/sw check exahoming-menu`: four ordinary/Academy source loot payloads, identity, name/model/tier, source description lines, donor flag distinction and Polymer projection.
- `/sw check exahoming-crafting`: all nine combinations of native/Academy/legacy Air and True Falling Stars in native 2x2 and 3x3 crafting; previews, exact one-plus-one consumption, extra ingredient rejection and ordinary source output.
- `/sw check exahoming`: source costs/gates, antimagic, stopped caster, observer, offhand setting and free Academy mode changes; one-terminal formation counts/powers/heights, native owner/epoch, common protected hit callback; ordinary and approved Academy Default/Big/Rain/Destiny presets; both real source trials, immediate readiness and NPC promotion.
- `/sw check exahoming-stop-probe` leaves live native fireballs for the following actual match stop. `exahoming-cleanup` checks service and entity cleanup in lobby.
- `python tools/smoke_exahoming.py --server-dir <isolated-server>` runs two complete matches using the existing stopped-process/world-lock guard and restores settings/profiles even after failure. Python syntax was checked with `py_compile`.

## Extended assertions after user confirmation

All six decisions are confirmed by the user. The following assertions are present
in GameChecks and passed in both final Exahoming matches:

- Sisyphus creates exactly one native owned fireball with ExplosionPower100. Inspect saved native data and discard immediately, before any native physics/explosion tick.
- Both ordinary sneak trials retain the normal paid cast, then their source formation, immediately set upgrade_available1/upgradable0 on the used stack, and promote through the actual Academy NPC. True Falling Stars:14 trial shots, powers1/2/eleven5/4, heights150/175/1100..1375. Exahoming:5 trial shots power20 at heights400/400/450/450/500 and source lateral offsets. Preload the source shot chunks; do not tick native entities.
- Curse stacks sixty seconds per cast, has its HUD effect, and is removed immediately by the common effect cleanser. Test a non-player curse target plus a player HUD/cleanser fixture.
- The global Curse projectile pass includes a genuinely ownerless native Arrow, an Arrow owned by the cursed target, and an Arrow owned by a third player. The first two acquire the real Curse caster; the third retains its own player owner. Assert velocity redirects toward the selected cursed target and damage ownership is preserved. Drive only service ticks, never native projectile collisions.
- Protect observer, service NPC and teammate fixtures through the shared targeting/damage rules. Snapshot and restore teams, player state, settings and any edited world blocks.
- The casting ray has no distance cap while its path remains loaded. A terminal beyond400 blocks requires a loaded corridor and a distant stone/entity fixture. Drain only the shared search queue through its root-provided helper; assert one terminal formation and one payment. Unload/border/antimagic and stop must cancel or finish through their actual shared lifecycle without a second implementation.
- Repeat live fireball stop/cleanup across two matches through the existing smoke runner.

Required helpers: native fireball snapshot; pending ray count and a queue-only drain/tick entrypoint; curseSeconds(LivingEntity); clearEffects(LivingEntity). Existing game.items().hud and common clearEffects are used for HUD/cleansing. Owner reassignment is observed directly through native Projectile.getOwner(). The third-owner case uses TestBeta while a cow is the cursed target, preserving a real separate player owner.

The loaded-distance fixture uses a narrow450-block corridor and restores all450 edited cells through MapManager snapshots. No test triggers a native high-power explosion.

Runtime and compile outcomes belong in SERVER_TESTS.md only after root actually runs them.
