# Attack Helicopter / Laser Minigun / mechanism beacon

Status 21 October 2026: QUALIFIED. Author decisions (user) are implemented;
final build and six native matches passed. All three tables are ported.
Archive `build/verification/skywars2-shooter-pair-dee146b1.jar` is ready without installation.

Sources: [Attack Helicopter](ATTACK_HELICOPTER_SOURCE.md),
[Laser Minigun](LASER_MINIGUN_SOURCE.md), [resources](SHOOTER_PAIR_RESOURCES.md).
[AK47/Magnum](AK47_MAGNUM_NEXT_SOURCE.md) is the next audit only; implementation
has not started.

## Implemented contract (author rules: user)

Attack Helicopter reuses LbcShooters and native Arrow/onHit: owner, coefficient
0.1, PierceLevel10, horizontal initial speed 6; mainhand offsets +/-0.2 and
 offhand +/-0.6. Native gravity/drag remain. Shared timer800 fires every 2 ticks
for the first200, then reloads for600. Removing it stops emission while timer
continues; holding it again during the window resumes. No ammo/durability cost.
Block hits reuse destroyRubyCell around native position +/-0.1, normal drops
and modification ownership; no TNT or extra explosion damage. Native reflection
updates actual owner; existing load/unload/forget/stop/epoch lifecycle is reused.
The helicopter is TECH: magic suppression does not block launch or volleys;
mechanism beacon suppression blocks its use/emission while timer continues.
Actionbar uses existing E104/E105 icons, ready/raw timer and shared HUD.

LbcLaserMinigun now implements six-tick input grace, 30-active-tick spool,
spin-down, spread narrowing and Purify spread0 reset, sounds and source/KFC
counters. Removing the last held copy immediately stops firing while spool
coasts down. Either hand may fire (offhand follows offhandWands); each held
copy independently pays100 mana per shot through the shared wand resource API.
Ray uses native first hit against hitbox/obstacle, one2.5 damage hit, shared
shield/target rules and magic suppression endpoint. Lore is corrected to2.5.
It retains the exact muzzle offset, 100-block bound, Polymer beam and all8
animation frames, including the previously skipped frame. Beam geometry is
half a block, scaleZ=2*length; no extra renderer yaw180 is needed.
Time stop, role checks and existing epoch/visual cleanup remain shared.

Every held minigun has movement_speed -0.5 and jump_strength -1 using native
ADD_MULTIPLIED_TOTAL, plus its preserved source attack_speed -1 modifier.
Two miniguns also receive a transient movement_speed -1 total multiplier:
native 0.5*0.5 alone leaves0.25, so this extra lock makes movement zero.
LbcMinigunMovement centralizes stack upgrade/equipment refresh and cleanup;
removing either copy removes the dual lock. No manual movement loop replaces
native attributes.

Native Polymer skywars2:mechanism_beacon reuses the existing LbcMagic engine,
block entity and chunk/load/dirty/lifecycle paths rather than adding an engine.
Suppression and mechanical entity pulse use sphere R20; mechanical block
processing uses cube +/-15. Block/entity #minecraft:mecha tags and existing
mechanical classifications are used; placer ownership is retained for effects
and terrain attribution. Magic and mechanism suppression remain separate.

Input/registration reuse LbcWands, SkyWars2 and LbcItems; LbcKfcFood owns source
counter integrations. Source resource pack/template stay read only. Tables:
lbc:ported/attack_helicopter, lbc:ported/laser_minigun and
lbc:ported/mechanism_beacon. Broad Reforge pools remain separately unqualified.

## Verification boundary

Historical prepared SHA256 was
ddbab8213e2c379245b66adc6eb76fb8851065d8d637e935d69d51cf25d3b47e.
Its build, presentation checks and two Woodificator matches passed; those
results do not qualify the newly completed gameplay or current artifact.
Historical reports: build/verification/shooter-pair-prepared/verification.json,
woodificator-results.json and woodificator.log.
Staged two-match gameplay qualification passed all 12 suites. The promoted final
build passed with SHA256
`dee146b132ed843aec0fd4834d3d40e6fcdee1d905192554029b9a6257c119c2`.
Final qualification passed six native matches on this exact SHA: two shooter-pair
with 12 suites (50.46s), two Woodificator/magic with 8+4 suites (48.87s),
and two Phantom Knives/Stand, including native loot/catalog checks.
ZIP CRC passed; 2630 packaged resources verified, 1978 source resources excluded; 111 Java sources total.
Archive: `build/verification/skywars2-shooter-pair-dee146b1.jar`, ready without
installation. Main JAR remains ba2e46e6; source RP/template are untouched.
Final detailed evidence belongs in SERVER_TESTS.md.

Client visuals were not verified. Existing Polymer fake-player chunk packet-context
diagnostic and legacy GUI loot-context warnings remain; see SERVER_TESTS.md.
