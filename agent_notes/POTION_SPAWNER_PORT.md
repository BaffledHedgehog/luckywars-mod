# Native Potion Spawner drops

22 October 2026. QUALIFIED on `884de7db` in two actual isolated mode1 matches.
[Source audit](POTION_SPAWNER_SOURCE.md).

Two complete leaves are migrated: `lbc:ported/potion_spawner_drop` and
`lbc:ported/potion_spawner_drop_old`. The normal token retains its source trap
frame/name/lore and the old token remains a plain barrier. Both use native
SpawnerBlockEntity/BaseSpawner, not a separate timer or simulated potion emitter.

Source count1,initialDelay1,min4/max5 and all18 equal-weight potion potentials
remain. Normal range6/nearby200/player35 differs from old15/1000/100. Placement
is tokenY+10, with the normal-only green particle burst atY+5. Native activation,
random positions, collision/spawn-rule rejection, exact-class nearby count,
weighted selection and delay progression remain authoritative.

The producer persists its original author UUID and match epoch in block-entity
data and passes them through native potion entity tags. Native potion owner
remains unset, preserving source collisions, while the common cause resolver
uses the separate author. A real later native projectile owner takes priority.
Accepted splash effects retain the actual potion as their source; direct damage
and accepted poison/wither/levitation consequences use common kill inheritance,
including newer-attacker priority. The trap is WILD for ordinary players/mobs.
All harmful and beneficial potion effects still respect service/observer roles.

No source lifetime, owner-death cancellation, world scan or chunk ticket is
introduced. Native block ticking and projectile simulation respect time stop.
Unloaded candidates must not force chunk loads. Block replacement, detached BE
references and stale epochs cannot continue emitting. On match stop the producer
is closed in persistent metadata, leaving its physical spawner block/configuration
intact; loaded potions are quietly discarded. Unloaded actors/blocks are rejected
on later loading by epoch. Ordinary vanilla spawners remain unchanged.

The common dispatcher preflights all matching branches, executes normal then old
in legacy source order before Library, and consumes exactly one item for the
whole ordered dispatch. Both flags at once leave the last old configuration.
Only the two standalone leaves are exposed, not the complete trap/reward pools.
Actual resource pack and resourcepack_template remain unchanged.

## Qualification

Build and two actual isolated mode1 matches passed: **118 recorded suite
results**, 85.51s. All previous special-drop suites, retained probes/cleanup
and advanced anvil/enchantment checks ran in both matches. Ready `build/verification/skywars2-potion-spawner-batch-884de7db.jar`;
evidence is `build/verification/potion-spawner-batch-final/qualification.json`,
result and log. 208 Java sources / 591 compiled classes / 2690
processed resources match the tested source/release JARs. The isolated server is
stopped, files restored and locks released. Main JAR and user Reroll hashes remain
unchanged. This artifact is ready and **not installed**.

Both source configurations and all18 weighted native entity/item/effect payloads
passed native decoding and actual BaseSpawner production. Real splash effects,
direct instant damage, native wither death, nonlethal poison/subsequent fall,
accepted levitation/void, newer-attacker priority and actual native owner changes
are checked. Splash reaches thrower/allied participants while harmful/beneficial
effects protect service roles. Author spectator state does not cancel production
or attribution. Original native potion owners remain null.

Native activation, delay progression/reset, occupied-candidate rejection and
nearby-cap rejection passed. The cap fixture temporarily uses1; source200/1000
are independently checked, with no full-population capacity claim. Actual world
BE ticking emits a potion. Native BlockEntity codec and LevelChunk installation
create a new identity preserving author/epoch; serialized closed/old-epoch BEs
cannot resume. Source flag ordering/one debit, actual tick consumption, freeze,
replacement, actor reload guards and actual-stop closure/removal also passed.
Physical source spawner and unrelated ordinary spawner stay intact at stop.

Attempt1 caught an interface directly in the defined mixin package, causing
IllegalClassLoadError; the interface now lives under the ordinary game service.
Attempt2 caught the stop fixture positioning players before resuming a previous
time freeze; native resume restored their distant positions and correctly
prevented activation. The fixture now positions after resume and reports native
activation/collision diagnostics on failure. Failed evidence is retained under
attempt1/attempt2. Only the corrected final artifact above is qualified. The final
log contains only the known fake-bot Polymer packet-context ERROR pair.

Limits: no actual vanilla-client rendering/particles/audio, disk restart/chunk
unload/disconnect, offline coin framework, full200/1000 population or peak-capacity
benchmark. Persistence uses native codec/new BE installation; actor reload guards
use callbacks. Elapsed time includes startup/maps/regressions, not per-tick CPU.
Whole reward pools/modes remain gated. Actual RP/template are unchanged.

Next finite source closure: [old Island Lowering](NEXT_DROPS_AFTER_POTION_SOURCE.md).
That next-source audit is not another implemented/qualified migration.
