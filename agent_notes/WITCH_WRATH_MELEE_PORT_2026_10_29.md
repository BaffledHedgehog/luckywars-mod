# Wrath control and melee-only Witch progress

Staged native implementation, 29 October 2026. The original full25 Chaos release
gate remains closed. This is not complete Witch acquisition or a Vanity rewind.

Requirements are in [author decisions](AUTHOR_DECISIONS_2026_10_29.md).
Original functions are indexed in [Witch source](WITCH_GENS_SOURCE_2026_10_29.md)
and [actor source](WITCH_ACTORS_SOURCE_2026_10_29.md). Later author decisions
override those audits' historical questions and proposed behavior.

## Damage progression

(user) Only direct melee damage counts toward Wrath and against Vanity.

The shared mutable `dmg_dealt` objective now counts actual HP loss at the native
combat tracker, after armor, effects and absorption, capped at remaining HP.
Units remain tenths of HP, as in the original statistic. Rejected hits, healing,
fully absorbed hits and excess overkill do not add fictitious damage.

The native Player attack, sweep and stab hurt calls mark their actual impact and
victim. This proof travels in the DamageSource through existing causal wrappers
and Time Stop replay. No per-tick polling, player lifetime-total resynchronization
or recent-HurtTime target search is introduced. A spell merely using
`player_attack` is insufficient: Soulhunter's splash is one existing example.
Conversely, a genuine native spear stab counts despite using `spear` damage.
Credited NPC, summon, projectile and spell damage cannot manufacture this proof.

New damage objectives use dummy criteria. Existing native-criterion objectives
keep their criterion, scores and display metadata; vanilla's generic award skips
only this named objective. Vanilla player statistics and other statistic
objectives retain normal updates. Native reset and the original Witch-reset
consumer remain authoritative. Mushroom mining keeps its existing native bridge.
KFC's broad score-increment outcome no longer adds damage progress; repeated
food outcomes are tested against the actual consumer. No loot pool or RP changes.

## Wrath

`LbcWitchWrath` runs on the existing item scheduler. Original native selectors
provide their real scope and positive unit-box conversion geometry. In particular,
unbounded mob/affected-player selectors are GLOBAL, including loaded foreign
dimensions; older source-audit wording calling these local was inaccurate.
Resident native foreign-world actors are included in the gameplay fixture.

The common Communism team/AI bridge now also serves authority subjugation.
Nonplayer conversion copies `lbcID2` and `team_number`; the player branch copies
only `team_number`. Native teams and summon orders follow the current holder.
Controlled players remain authors of their own attacks. Sauvojen conversion
retains its friendly-holder flag and its health exception. Protected roles and
sealed dimensions retain the common guards.

Mob maximum-health BASE and HP use original integer quantization. Affected
players receive integer effective owner maximum health and the source's distinct
instant-heal/outside-border health branches, using live native HP/absorption
instead of an obsolete dummy health cache. This deliberately does not replace
the source behavior with a new generic health multiplier.

Antimagic, including the stronger artifact, protects both holder and target;
TECH permits the authority. Real Time Stop pauses it. Current-holder handover
changes live team/control and the emitter's future source policy, preserving its
exclusions and causal flags. Already inherited child policies remain immutable;
the actual old and new native arrow publications are tested separately.

(user) The player conversion helper intentionally removes `cantleave` from ALL
players at completion, including restrictions that already existed.

The native AFTER_DEATH hook captures the holder's consequence before magic
equipment cleanup. It uses the shared inherited-kill path and existing core
death/lives handling. Pending consequences wait for thaw and retain epoch/cause;
the test exercises this production boundary without killing/replacing the actual
core fixture participants. No second respawn or death engine is introduced.

## Verification and limits

Candidate SHA256:
`01f4d7961aacdb513d78b79bf7cfde918a63fc93f78a255863179d9fae90c9b9`.
Build: offline JDK25 `build/verification/witch-wrath/build-12.log`.
Frozen inputs: `inputs-01f4d796.json` in the same verification directory.
All10978 source/processed inputs are frozen:615 Java files,1775 classes,
5176 source resources,3411 processed resources and build.gradle. Processed JAR
bytes match; only the normal Loom `fabric.mod.json` nested-jars field is normalized.

The native six-suite match passes `witch-progress`, `witch-wrath`,
`witch-passives`, `witch-envy`, `time-stop` and `kfc-counters`.
Proof: `01f4d796-native.log` and `01f4d796-results.json`.
It covers real attacks/sweep/spear, damage-type aliases, native/dummy criteria,
fatal/absorbed/rejected damage, queued melee/thaw, KFC exclusion, global native
mob control, source counters/teams/health, player conversion, current-holder
handover versus old child credit, MAG/TECH, Time Stop and the death boundary.

Five neighboring suites pass on the SAME artifact in a separate real mode1 match:
`ordinary-tools`, `shish-encounters`, `apocalypse-actors`, `phantom-armor` and
`classic-melee`. Proof: `01f4d796-neighbors-native.log` and
`01f4d796-neighbors-results.json`. The two final runs take33.17s and43.92s,
both with normal child shutdown and exact file restoration. These are11 gameplay
suites plus their ordinary setup checks. Existing Apocalypse cadence/lifetime
limits remain; its dispatcher check does not establish all natural deadlines.
The known pre-suite Polymer chunk-context diagnostic remains in the fixture.

Rejected artifacts/logs remain. Early fixture corrections included attack-field
location, natural food regeneration, an attacker frozen without a Stand, and a
TECH marker that had migrated into a real block. Removing only its old marker
left a legitimate jammer that killed the test arrow. The final fixture restores
that native block and terrain provenance, and checks suppression is gone. The
production jammer was not weakened. All fixture players' equipment/ability state
is isolated and restored.

199 existing dirty paths, including the author's in-flight question file, and
31 live grimoire override files retain their exact starting bytes/deletions.
Main `../mods/skywars2.jar` remains d9087f0a. Only the owned isolated child runs;
normal shutdown, exact snapshot restoration and a released world lock are required.

No full acquisition, remaining Witch actor families, full Vanity world/player/core
rewind, physical client, arbitrary latency, real replacement-login/respawn or
peak-TPS qualification is claimed. In particular, a player-NBT-only Vanity
checkpoint would not satisfy the user's requested full rewind.
