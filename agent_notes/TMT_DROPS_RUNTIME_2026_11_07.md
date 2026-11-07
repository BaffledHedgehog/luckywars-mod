# TMT shared dropped-item dispatch, 7 November 2026

Component verified, **not installed**. Full TMT remains incomplete.

The sampler now admits native active drops through the existing SpecialDrops
dispatcher. Each private property runs the same native readiness and execution
path on the real dropped stack. Individually available properties run in source
order; the shared dispatcher consumes one item after the scopes close. Native
ground requirements, resident chunks, suppression, author, epoch and TimeStop
remain owned by their existing handlers. No parallel gameplay implementation or
arbitrary function executor was added. The old Balance lucky-spawner admission
has a pure preflight; this is not the Balance match-reset reward.

Whole-world replay suppresses entity-load callbacks. It previously restored drop
bodies without rebuilding their derived dispatch index. The common checkpoint
now rebuilds that index from the already collected canonical native bodies,
without spawning effects, requesting chunks or introducing another world scan.
This fixes continuation for ordinary active drops as well as mixed TMT drops.

Read-only property callbacks retain the original immutable CustomData component
and its parsed-slot cache. Actual private mutations still persist; native payload
components and outer refunds retain their existing restoration contract. The
optimization does not add a global cache or retain obsolete stacks.

Sealed candidates on full fb24be1e / e99d79ed:

- Test `01ca49032d3bd7e465c5eaa1b685ee8be57cfcc689153958cb53e4abc672610e`.
- Production `f804ce6524dde27e0cb6252dcae00417f880c1736554cf2bd9a11eb326f9c4ba`.
- Evidence: `build/verification/tmt-drops-2026-11-07/component-verification.json`.
- Ten Java families, zero resources; 10,330 other inputs and all 282 protected
  author paths preserved. Main/libs, author RP and concurrent HUD unchanged.

Twelve actual mode-1 matches pass: tmt-drops, tmt-wands, tmt-self, tmt-ranged,
tmt-native-use, tmt-held, tmt-ammo, tmt-block-use, totems, tmt-catalog,
family-drops and creature-drops. The new scene uses native selected-inventory
dropping, five Vex plus one Skeleton for one debit, same-frame rejection,
TimeStop, full capture/restore of both ordinary and mixed pending tokens, and
continued dispatch after thaw. The initial failed continuation exposed the real
missing derived index; its report is retained as diagnostic evidence, not a pass.
Final reports have no check/cleanup failures, live child or retained locks, and
verify exact fixture restoration and normal shutdown. Compile and fixture run
sequentially at 512 MiB and 2 GiB respectively.

801 families / 1,542 variants is a domain count, not full coverage. Independent
intake/drop owners, remaining native domains, final acquisition and full release
remain pending. No physical-client rendering or peak-TPS claim.
