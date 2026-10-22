# Advanced anvil and native creature drop batch

22 October 2026. QUALIFIED on `8945afbe` in two actual isolated mode1 matches.
[Creature source audit](NEXT_SMALL_DROPS_SOURCE.md).

## Confirmed anvil policy

Author chose stronger +25% weaker, rounded down; identical or adjacent positive
levels gain at least one. Missing enchantments transfer exactly. Examples:
1+1=2, 1+2=3, 3+1=3, 99+99=123, 255+255=318, 100+1=100.
Positive integer addition saturates safely at Integer.MAX_VALUE. This is an
integer storage bound, not a vanilla enchantment-level or item-applicability cap.

Anvil operation cost never exceeds ten XP levels. The same native cost slot
drives presentation, pickup eligibility and exact XP debit; the old 40-level
rejection cannot discard a valid result. Native material repair, rename and
input consumption remain. Overflowing fee products cannot turn expensive valid
operations into empty/free results. Books still apply to any item and different
enchantments coexist, as required in the previous batch.

Only AnvilMenu changes its merge and maximum-level checks. Enchantment registry
maxima, enchanting-table level selection and loot algorithms are untouched.
ItemEnchantments representation accepts positive int levels for persistence;
native table/loot selection retains its original independent level bounds.
For levels above ten Polymer renders truthful decimal tooltips. Components sent
to vanilla clients are bounded at 255, preserving the authoritative real stack
in Polymer restoration metadata. This avoids the vanilla client's constructor
rejecting a larger level. Knowledge-hiding projection runs after this conversion.

## Three closed creature mechanics / four public leaves

| Public `lbc:ported/` leaf | Complete behavior |
| --- | --- |
| `old_vex_drop` | Five native one-HP Vex in the source local-coordinate ring |
| `old_skeleton_drop` | Original persistent Fat Skeleton and its complete native equipment/loot |
| `shulker_bullets_drop` | Nine native Shulker Bullets, original trap presentation |
| `shulker_bullets_drop_old` | Same bullet behavior with original plain barrier presentation |

All source metadata, local coordinates and ordered branches remain. Vex creation
rotates the token through four 72-degree steps. Skeleton equipment, attribute
IDs and guaranteed/default drop chances remain native. Bullets retain native
Steps=40, zero initial steering vectors, null native owner and independent
nearest eligible player selection at each bullet, including self/allies.
Author explicitly corrected the legacy selector: only the nine new bullets
receive targets; previous nearby bullets remain untouched.

Native mob AI, projectile steering/collision/levitation/destruction, lifetime,
equipment and ordinary death loot remain authoritative. Actual source UUID
and epoch are retained separately; assigning a player as a bullet's native
owner would incorrectly suppress self collisions. The common resolver attributes
accepted hits and levitation/fall/void consequences, with newer-attacker priority.
Bullets are WILD; ordinary mobs retain shared role/ally guards and native thrower
retaliation. All roles and service NPCs remain protected. No new despawn timer,
owner-death cancellation, world scan or chunk ticket is introduced.

The shared dispatcher preflights loaded source footprints, executes independent
flags in source order and debits one unit together. Both bullet presentation
kinds share one trigger, so they never spawn two batches. Existing time stop,
epoch/unload callbacks and quiet actual-match cleanup cover native actors. Native Skeleton-to-Stray conversion copies the persistent tags; the whole native skeleton family remains managed without changing vanilla conversion outcomes.
No full pool or unfinished normal Vex/Skeleton reward tree is exposed. Potion
Spawner remains a separately deferred producer. Actual RP/template remain unchanged.

## Qualification

Build and two actual isolated mode1 matches passed: **112 recorded suite results**,
88.39s. All previous special-drop suites, retained stop probes and quiet cleanup
ran in both matches. Ready `build/verification/skywars2-anvil-creature-batch-8945afbe.jar`; evidence is
`build/verification/anvil-creature-batch-final/qualification.json`, results and log.
204 Java sources / 579 compiled classes / 2688 processed resources match the
checked source/release JARs. The isolated server is stopped, files restored and
locks released. Main JAR/user Reroll hashes remain unchanged; not installed.

Actual Survival AnvilMenu take/XP/material/book debit, chosen merges through
Integer.MAX_VALUE, high prior-work taxes, native persistence and safe client
component encode/decode passed. Native sword/book/Polymer-item projections retain
truthful levels and exact real-stack roundtrips, including challenge1 hidden
knowledge. Existing native table and OP book fixtures retain their level bounds.
Creature fixtures cover local Vex geometry, source gear and native drops/melee,
per-bullet selection, previous-bullet isolation, WILD impacts, accepted levitation,
direct/fall/void credit, newer attackers and changed native owner, freeze/debit/
reload guards and actual quiet stop. Skeleton conversion uses the real native
convertTo API, including copied tags, equipment, persistence and later melee credit.

Attempt1 exposed persistence being run inside a networking PacketContext: the
fixture now uses plain native RegistryOps outside that context. Attempt2 exposed
the fixture comparing double trigonometry too tightly with native Mth table
trigonometry; geometry tolerance is 0.001 block. Attempt3 exposed a real cause
resolution gap: unwrapping a null-owned projectile lost its separate author.
The common resolver now falls back to the original managed projectile, including
direct damage before death. Failed evidence is retained under attempt1/2/3.
A first passing artifact before the Stray coverage expansion is retained under
before-conversion; the final artifact above includes native conversion coverage.

Limits: no literal unmodified client execution, rendered pixels/audio or natural
human input perception; component levels sent to clients are explicitly bounded
at255 and their native codec roundtrip is tested. No actual disk restart/chunk
unload/disconnect, offline coin-framework test, natural AI/powder-snow conversion
waiting or peak-capacity benchmark. Disk storage uses the real ItemStack codec;
unload guards use callbacks and natural conversion uses the native conversion API.
Whole source reward pools/normal Vex-Skeleton branches and Potion Spawner remain
unqualified. Actual RP and template are unchanged.
