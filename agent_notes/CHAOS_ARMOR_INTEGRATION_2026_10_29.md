# Chaos armor: full-pool integration, NOT RELEASED

## Author decisions

- (user) Preserve the FULL original trap pool; release Chaos only after the whole dependency chain is ready. Never temporarily filter unsupported traps.
- (user) Glass bridge is under the wearer, at wearer Y minus 1.5.
- (user) All Chaos abilities use MAG suppression only; intrinsic armor attributes remain.
- (user) Keep one wolf. Align the 24 outward arrows with their flight direction.
- (user, 29 October) Aimbob helmet abilities are TECH-only. MAG permits them. TECH prevents new night-vision grants and cleansing; already issued effects are retained.

This supersedes the unanswered choices in the earlier Chaos source audit. The
installed main artifact remains **d9087f0a**, full SHA
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No Chaos artifact has been installed, queued for installation, or marked public ready.

## Staged implementation

`LbcChaosArmor` uses the shared equipped-armor admission, native incoming criterion
and jump hooks, source sprint/helmet/rescue phases, native explosions and the existing
terrain queue. `LbcChaosTraps` preserves all 25 original weighted/conditional entries.
The normal total weight is 250; ranked total is 241. Preserve skeleton weight 20,
slab quality +5 and both conditional Sauvojen entries.

`LbcChaosTrapActors` adds the missing Forest, Evil Lucky Block, Bob and Aimbob
producers, native mob AI/equipment and source death-table references. Shared
`LbcSpecialDrops` owns recognition, admission, source dispatch order and one debit.
Native mob conversion, projectile ownership, ignition and pre-insertion death loot
carry causal authorship without substituting the player for the actual attacker.

`LbcSauvojenBoss` adds the original Ghast and nine stationary spell emitters.
`LbcSpellSource` separates an enemy emitter from its causal player, including
asynchronous charges, reflected projectiles and nested reaction scopes. Shared spell
engines retain the distinct hostile source payloads. Emitted electrical arcs follow
the author's standing TECH-only suppression rule; the magical emitter still checks
MAG. Ordinary player spells retain their original targeting policy.

`LbcInfinity` implements the original fourteen outcomes, native persistent attributes,
inventory admission, one-item debit, source reward producer and active 300-tick phase.
`LbcAimbobRewards` attaches helmet behavior and apple effects to native equipment and
the existing consume-item callback. Ordinary source knockback defaults use the
previously approved import normalization; Infinity's native traits are not ordinary
item defaults.

Only the five immediate reward tables are newly packaged, with native codec
adaptation. Aimbob apples use the shared repeatable native consume callback; the
hidden legacy advancement/function/revoke loop is deliberately not enabled. The
existing advancement filter correctly excludes that old runtime trigger. The
broader Lucky Block reward pool has NOT
been enabled. No resource-pack edits are required or made in this stage.

## Verification state

Candidate `520e4cf4` compiled successfully and has an exact artifact proof in
`build/verification/chaos-armor/520e4cf4-artifact-proof.json`: 575 Java sources,
1631 classes and 3265 resources. All 198 existing author paths, 31 live override
files and the installed main JAR matched the baseline.

Its first isolated native focus run lasted 76.63 seconds in one actual match.
Infinity and adjacent Techno, Mage/Nyauwu, static armor and incoming-artifact checks
passed. The run was REJECTED: Chaos acquisition used adjacent seeds with the same
native first branch; the FIRE fixture omitted vanilla acceleration; the Aimbob
runtime criterion was missing from resource packaging. Normal child shutdown and
exact snapshot restoration succeeded. Preserve the initial proof, results and logs.
The next candidate fixes those findings and applies the later TECH-only decision.
Candidate `6f40e887` passes the nine hostile engines, reflection and Infinity,
but its focus run is also rejected: the Chaos fixture expected uncapped legacy
blindness, and Aimbob tried to retain a hidden legacy advancement removed by the
existing filter. Keep the approved item effect caps. The next candidate corrects
literal test expectations and uses only the existing native consumption callback
for repeatable apples, without re-enabling a legacy function trigger.

These checks are staged debugging, not release qualification. Synchronous engine
steps or controlled branch draws are not evidence of elapsed 40/300/800 server
frames. Forest lifetime/features, boss scheduling/random branches, actual nested
retaliation and all transitive event/reward behavior require further verification.

The final staged candidate is **dca64850**, full SHA
`dca6485038e554ba9b4d4426cd73e2ccd4f4247e34784a60e727201c43cf5cae`.
Its exact proof covers 575 sources, 1632 classes and 3266 resources. The complete
focus run passed 15 setup/check results in one actual match (83.13 seconds),
including Chaos's 17 incoming, seven jump and seven sprint branches, all 25
initial trap activations, native achievements, hostile boss payloads, Infinity,
Aimbob TECH/MAG/consumption, ordinary Sauvojen/elemental grimoires and neighboring
armor/incoming-artifact regressions. Normal child shutdown, exact snapshots and
all protected baseline bytes were checked after exit. See
`build/verification/chaos-armor/native-stage-results.json`; `qualified` and
`releaseEligible` are explicitly FALSE. Installed main d9087f0a is unchanged.

Further debug findings were corrected without changing source balance: the
inventory fixture cleared worn armor before checking the full achievement; native
atan2/float orientation needs a small angular tolerance; the original air template
needed explicit packaging. The first successful core log also used a different
PASS label from its command, so the harness waited for 180 seconds, then stopped
the child normally. This was a harness-name timeout, not a hung server. The
canonical command is now `chaos-armor-candidate`. One premature second runner was
rejected by the existing exclusive lock before touching the fixture; preserve
that log along with all rejected runs. No forced termination or restart occurred.

## Remaining full-chain gate

Keep the exact [trap/reward closure audit](CHAOS_TRAP_CLOSURE_2026_10_28.md) and
[boss source audit](SAUVOJEN_BOSS_DEPENDENCIES_2026_10_28.md). In particular, the
three apocalypse snowballs (`end_of_everything`, `baffled_of_everything`,
`shish_of_everything`) and the full Evil Lucky Block reward descendants are NOT
complete. Absence of a flag literal in Java is only a triage signal: ordinary
summoners and several other families already use generic admission. The audit
records concrete live consumers and separates real gaps from metadata.

Before release, adapt hostile Sauvojen's direct electrical damage to the common
electric tags/immunity/armor rules while preserving its source generic-damage
semantics and cooldown bypass. Its current staged generic source is not evidence
that this final shared-rule integration is complete. Real frame-based scheduling,
full boss reward conversion and actual nested player retaliation remain pending.

Do not mark Chaos ready, deploy this stage, shrink the pool, or describe initial
mob/token activation as complete reward-chain coverage. Continue from the native
debug results, then implement and qualify the remaining dependencies.
