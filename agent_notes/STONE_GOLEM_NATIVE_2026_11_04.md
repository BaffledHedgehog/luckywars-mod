# Stone Golem native skill34 — 4 November 2026

Parent integration, author decisions and actual executed qualification: [ADVANCED_SKILLS_PORT_2026_11_04.md](ADVANCED_SKILLS_PORT_2026_11_04.md). Worker handoff below is historical; its unrun tests are not runtime evidence.

Implementation ownership: `LbcStoneGolemSkill.java` and `StoneGolemSkillChecks.java` only.
Root integration owns catalog/HUD, admission policy, native criterion/statistic hooks,
shared damage wrapper, lifecycle, coordinator, build, fixture execution and release.
This note records implementation and intended checks; it does not claim execution.

Source: [remaining-skills audit, Stone Golem section](REMAINING_SKILLS_SOURCE_2026_11_04.md#34-stone-golem--full-small-dependency-closure).
Source route is `F/players_1s.mcfunction:22` → `S/skills_tick_1s.mcfunction` →
`S/stone_golem/tick_1s.mcfunction`. Ten one-second dispatches add one layer,
max5; each second refreshes hidden Resistance for40ticks at amplifier layers−1.
Native effect combination/expiry retains stronger foreign effects. No effect is
removed on layer loss, skill loss, clear or stop. Source addition/removal sounds,
stone50/iron-ore60 block particles and hurt pitch1.5 are retained.

The constructor accepts `(GameController, Predicate<ServerPlayer>, int secondLayerThresholdHP)`.
The root author decision supplies admission and2HP versus10HP layer2 threshold.
Incoming criteria reset charge and remove at most one layer for the attempt:
layer1 taken80 native units; layer2 configured HP×10 taken units; layer3 resisted80;
layer4 resisted100; layer5 one native boolean draw. Resistance statistics are
`Stats.DAMAGE_RESISTED`; shield-blocking statistics never qualify as resistance.

`withIncomingAttempt(player, BooleanSupplier)` brackets the existing shared native
damage execution, including its existing direct-HP bridge. `statistic(player, Stat<?>, int)`
receives raw native awards from the shared statistic callback; lifetime totals,
saturation and old global scheduled score resets are irrelevant. The topmost
matching-player frame receives each award, so nested attempts cannot reuse outer
statistics. `incoming(player, source, original, actual, blocked)` consumes that
frame at most once. Without a frame it uses finite actual×10 taken units and zero
resisted units: original−actual includes armor and cannot identify resistance.

Owner lifecycle uses native skill34/gameplay/ordinary-dimension/role checks.
Injected admission and TimeStop gate charge, effect refresh and incoming interaction.
Per-world modulo20 phase advances on active server ticks and pauses under TimeStop.
`layers`/`charge` support the root HUD. `clear` and `stop` retire scalar bookkeeping;
an in-flight native scope still unwinds safely if match teardown occurs during damage.

The typed checkpoint copies UUID→immutable(layers,charge) and per-world phase;
`prepareFrozen`, `Checkpoint.prepare(lookup)`, plan installation and `publishFrozen`
match existing resident-owner contracts. Unresolved player references are pending.
Capture/install boundaries have no persistent damage frames; capture during an
in-flight native attempt is rejected. Native player effect snapshots and RNG state
belong to the existing whole coordinator, not this service.

`StoneGolemSkillChecks.start/tick` is a scheduled suite for `stone-golem`, using
`game.items().stoneGolemSkill()`. It requires actual active fixture players and
approximately1100 real server ticks: first layer after200ticks, charge/cap5,
actual TimeStop pause, nonempty full coordinator capture/clear/restore including
phase, native ResistanceV zeroHP criteria, IV100/III80 resisted thresholds, actual
front shield blocking after8use ticks, exact configured layer2/layer1 taken
thresholds without cross-attempt accumulation, foreign-effect retention and
skill-loss cleanup. A supplementary nested raw-award boundary verifies isolation
and rejects reentrant checkpoints; it does not replace the actual scheduler tests.
The suite restores native statistic totals and fixture player/configuration state.

No build, server process, commit, resource-pack mutation or production installation
was performed by this implementation owner. Actual results belong to root evidence.
