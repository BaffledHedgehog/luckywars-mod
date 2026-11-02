Final author answers and qualification supersede this provisional handoff: [closure](BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md).

# Bow trinket native fixture ? 2026-11-02

Owned changes: new `BowTrinketChecks.java` and this note only. No build, server, native run, git, RP, or pending-question changes.

Root integration: `BowTrinketChecks.run(game)` starts; `tick(game)` schedules completion and logs `[check] PASS bow-trinkets` or FAIL. Requires worker APIs `kfcRewards().bombSnapshot()`, `homing().fourierSnapshot()` and narrow `homing().forgetFourier(owner)`.

Uses the existing Elemental fixture with `existingStopProbes=true` and `retain=true`, and copies the established Quiver native network scheduler plus full player snapshot/restore. Production maps and RNG are never modified to force procs; source item count99 gives certainty. Real game-mode bow release and charged crossbow inputs emit actual arrows. Covers noammo/short release/melee rejection, native/default99 and raw/custom-data99 identities, exact one bomb/Fourier per Multishot volley, spectral native ammo retention, foreign nearby arrow isolation, simultaneous flags, rocket mana6->0 and5->5, original loot/default components and recipient Polymer roundtrip.

Asynchronous body phase uses an owned resident high-Y stone wall and native swept arrow collision, asserting simultaneous bedrock+cocktail terminal coexistence; native scheduled cocktail damage0.5 with actual arrow and shooter attribution; actual Volos blind charges with lifetime35/no Academy trial. A40tick window observes normal source stochastic Volos processing without replacing RNG. The native connection must have >20 scheduled updates. Restores player inventory/craft/cursor, scores/profile/advancements/stats, use state, equipment/attributes/combat/food snapshots, original connection, config, and owned blocks. Cleans only captured new entities and newly generated nearby fixture debris; preexisting blind/elemental stop probes remain retained.

Limits: does not claim rocket seven-position TNT terminal or suppression-specific behavior; root/worker suppression author decisions remain authoritative. Production lifecycle clone/rewind assertions are not implemented by this fixture. Root must compile and run isolated25577 after integration.
