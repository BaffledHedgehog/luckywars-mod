# Electric pulse cadence with precise contact - 31 October 2026

The user chose to keep precise line/body admission and restore damage frequency without widening hits. `LbcDamage.hurtSegment` now separates admission from pulse counting: a target must intersect the actual finite ray; only then do electric sources retain the legacy 1x1x1-selector sample multiplicity. The virtual cells never admit a nearby target or one beyond the clipped endpoint. Strict overlap excludes cell-boundary-only samples.

Each pulse still calls native damage separately with the source amount and target cap. Armor, attribution, cooldown bypass, movement, arc age, reflection, shields, mechanism suppression and Time Stop retain their existing handlers. There is still one spatial query per segment and no new saved state. Non-electric sampled rays, native lightning, conductor area pulses and Yellow Conduction retain their separate behavior.

Source references are read-only legacy `world/datapacksdisabled/LBC/data/lbcr/function/raycast_molnia.mcfunction`, `raycast_molnia_chain.mcfunction` and `raycast_electro.mcfunction` under the original server. The ordinary moving arc uses 11 samples / 2 blocks per working tick; chain scheduling remains every five ticks. The change restores pulse count conditional on true contact, not the old broad hit admission.

Evidence in `build/verification/electric-contact-cadence/`:

- Runtime/check build: `./gradlew.bat --offline build -x sourcesJar` passed. Final JAR SHA-256 `59441aa3f1e10accf37dfacacb28bfed7c91e56293d1bc841a11eb83ee047774`. The earlier full build also passed; the final build skips only source-JAR packaging.
- `838d7165` passed Tesla, Erecto, hostile-electro, electro-family and raw-raycast-spells in fresh native mode1 matches. Its ordinary Electro check retained an obsolete five-hit assertion and failed; that proof is retained.
- `59441aa3` passed the final ordinary Electro group. Its updated fixture uses an independent native unit-cell oracle, including the actual shield-clipped endpoint, rather than fixed old line counts.
- `production-continuity.json`: production classes and data are byte-identical between these two artifacts; only GameChecks classes differ. This is six staged passing groups, not an assertion that the first aggregate run was wholly successful.
- Tesla additionally checks four real arcs over two advances through active hurt cooldown, exact HP, adjacent misses, eight ray directions, strict native selector overlap and finite-endpoint misses. Hostile arcs restore ten separate half-point pulses after exact contact, preserving actual electrical immunity and velocity behavior.
- Both native children exited normally, fixture snapshots were restored exactly, and no locks remained. `preservation.json` confirms all196 author paths, live31 loot overrides and main JAR `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087` remain exact.

No RP update. Original25 Chaos release gate remains closed; no main install, restart or push.
