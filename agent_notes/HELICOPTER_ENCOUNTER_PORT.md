# Hostile Helicopter Boss encounter

## Author decisions (user, 22 October 2026)

Mechanism suppression:

> Уничтожать с обычной наградой

Four-model animation:

> Да, исправить цикл

The hostile encounter is distinct from the already qualified handheld Boss
Helicopter weapon. Source: [HELICOPTER_ENCOUNTER_SOURCE.md](HELICOPTER_ENCOUNTER_SOURCE.md).

## Implemented behavior

- Polymer token `skywars2:helicopter_encounter`, qualified table
  `lbc:ported/helicopter_encounter`, original `lbc:drop_boss` model, `lbc:tier_ff`,
  yellow/nonitalic `drop_helicopter`, white/nonitalic `trap_desc`, and exactly the
  two legacy trigger flags. Existing RP resources suffice; the original RP is
  read-only. This patch does not expose the complete unfinished Lucky Block pools.
- A real dropped trigger queues activation outside ENTITY_LOAD. One successful
  spawn consumes one unit per tick. Vanilla `/give` fake pickup-visual entities
  (native age5999 sentinel) cannot summon an encounter. Failed/unloaded spawns
  preserve the token. Normal-mode native checks acquire the token explicitly;
  they do not grant LuckyWars drops to ordinary SkyWars acquisition tables.
- A direct pair owns one animated display and one invisible native ravager:
  150 HP, armor10, toughness10, NoAI/NoGravity, empty native loot. Spawn geometry
  uses the dropped entity's local +20Y/+50Z context. Rotation and invisibility
  are initialized before native spawn packets; all four model frames cycle.
- Three source phases retain per-unstopped-tick dispatch and sequential
  transitions, alternating arrows, steering, free six-variant Cultus drops and
  falling ground-contact TNT. Finite-radius AI uses bounded native entity
  queries; the source unlimited phase2 selector only visits loaded entities.
  The service checks chunk residency before block access and quietly clears an
  unloaded pair, without forcing distant chunks.
- Hostile arrows/TNT retain their real NPC causal owner after its removal.
  Generic instantaneous TNT blasts now also retain the actual supplied living
  owner, rather than losing it through a removed EntityReference. Source fuse0
  contact explosions bypass configured TNT protection; short ground-contact
  explosions reuse the common protected native blast and preserve power1/4.
  The shared dynamite scanner excludes encounter TNT to avoid double handling.
  Native lightning remains WILD for boss hitboxes too. An arrow's subsequent
  ownership transfer replaces its retained causal reference, including reflection.
- Shared role/service guards apply before native projectile/explosion impact.
  No nearest-player or trigger-thrower kill credit is fabricated. A naturally
  hostile, teamless boss remains the source of its attacks. Player-owned Cultus
  keeps its existing player attribution and guards.
- Ordinary death and mechanism suppression produce exactly30 source-position
  TNT and one existing qualified weapon reward. The actual newly rolled reward
  receives glow/no-gravity/invulnerability/extended age. Pair state detaches
  before callbacks, preventing duplicate rewards and invisible orphan hitboxes.
- The free Cultus payload reuses all six existing KFC effects. It has native
  item physics and source small random motion, no bat, native pickup/contact
  activation and a stable boss cause. Already dropped payloads survive boss
  death; epoch changes and match stop remove them without effects. The existing
  player-mounted form remains separate in presentation and uses the same effects.
- Dimension time stop pauses the encounter and its managed attacks. Epoch stop,
  entity unloading and cancellation clean up quietly. Synchronous damage paths
  recheck lifetime/epoch before further attacks or displacement after cleanup.

## Qualification

Build and two native matches passed on
`497c091e087342493b14eade1dfe92e2ff364c815eed79038da5d8f9327675ee`.
Ready, not installed: `build/verification/skywars2-helicopter-encounter-497c091e.jar`.
Evidence: `build/verification/helicopter-encounter-final/`. The runner reported
18 suite results over two real match cycles in44.06s, including KFC/WILD/Cultus,
the existing Boss Helicopter weapon, shooter pools and match-cleanup regressions.
All137 Java sources match the sources archive; all2643 processed resources,
JAR CRC, original Fabric metadata and bundled SGui passed archive verification.

The full two-match runner is `tools/smoke_helicopter_encounter.py`, using only
`build/verification/key-reuse-server` on port25576. It preserves config, profiles
and scoreboard files, checks world locks, and stops its child server normally.
The exclusive runner lock and world lock are released. Main remained at the
previously installed69de8601; this turn did not install or restart it. No original
resource-pack files were changed, and no template assets were required.

Polymer's missing chunk-packet-context diagnostic immediately after fixture bots
join remains present, as in the earlier qualified baselines. This run does not
claim to fix that existing diagnostic or qualify actual client chunk rendering.

The native fixture checks prototype/loot/give equality, actual dropped-stack
consumption, concurrent pairs, HP/defenses/models, arrows/role guards, time stop,
combat death, source reward flags, all six free effects including dead-owner
causality, mechanism reward death, unloading and match cleanup. Random phase3
scatter and every probabilistic phase transition are not deterministically
qualified by this fixture. A real client's visual inspection remains separate.

## Repository maintenance

`b0fe490` stops tracking generated build state, test worlds, caches, personal IDE
settings and scratch sources/diagnostics; useful local builds and evidence remain.
`12beb0a` checkpoints the previously qualified migration through modifier ownership.
The user requested periodic local commits and removal of obsolete temporary files;
no history rewrite, push or main-server installation is implied.
