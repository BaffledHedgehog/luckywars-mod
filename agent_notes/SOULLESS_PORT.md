## Superseding correction - 27 October2026

[Qualified radiant/vector repair](METASOLA_REPAIR_2026_10_27.md).
Type3 now uses70percent force and additive player transport to preserve jumps.
Type5 retains source integrity0.1 but now applies full native strict818write flags;
previous knownShape-only placement could extinguish fire immediately. Historical
qualification below predates this fix. Buildfe61bfdb passed30results in2actual
matches and is queued for normal main shutdown installation. No RP changes.

# Ordinary Soulless and six reward fields

Implementation is `LbcSoulless.java`; this note does not claim runtime qualification.
Source evidence is [SOULHUNTER_SOURCE.md](SOULHUNTER_SOURCE.md), plus the live
`function/projectiles/eggs.mcfunction:40-44` and all functions below
`other/event/halloween/soulcorruptwand/metasola` in the read-only LBC source.

Ordinary Soulless uses the two existing hand inputs, costs1500, and applies WILD
ghosting to eligible native mobs/players in a25-block sphere about caster eyes.
The five named transient attribute modifiers are inserted once while the timer
stacks by5seconds. Native invisibility is refreshed for20ticks at amplifier1;
airborne sprint adds horizontal forward0.2. Exact native hurtTime10 creates a
native Vex, with original free AI and one vanilla wind-charge death reward.
Directly ghosting a Vex produces one equal-random usable material1..6, with
NoGravity and downward velocity0.06, and kills that Vex.

All six materials preserve the source SNOWBALL transport initialization. Native
Snowball rotation/spread/inherited velocity is calculated before same-tick source
conversion; the physical pig receives floor(Motion*2000)*0.001.
Every field owns exactly its own native pig and bright item display. Source
NoGravity=false, Silent=true, invisibility, native AI, stationary speed attribute,
fall-damage immunity and distinct25/50/20/15/500/40 HP are retained. Full HP is
the user's explicit ruling. Display translation is0.6Y with source fixed pose.
The field never searches for another field's nearby pig or nearest display.

Source Lifetime is an activation delay, without an expiration. The1/2 fields
receive +1 each active tick and a separate +20 at each20active-tick second,
reaching200/80 in100/40 active ticks; their pulse occurs at that second boundary. Other activation thresholds are
100/40/200/100. Type1 deals4magic to enemies within10; type2 uses native Instant
HealthIII on allies and self, including the source duplicate caster branch (its own stored team_number
necessarily matches itself). Type3 is WILD within20, adding local lateral
0.074/up0.08/forward0.08 toward that exact field. Type4 floors allied player
mana at500 within10. Type5 deals WILD20in_fire outside the exact0.01dead zone,
moves its own pig downward0.2 and places the native light-sphere template with
integrity0.1 through shared resident map placement and indirect terrain causes.
Its exact native mask contains2229fire,739lava and1977void_air cells in21cubed;
the source name does not describe a harmless lighting template.
Type6 refreshes ResistanceV and a minimum2 void_resistance within10 on allies
and self. Academy generation and its two modes are deliberately absent.

Match/owner closure, death, unload, nonresident position, suppressed field and
stop remove both halves together. Pair publication is registered before native
load callbacks, unknown restored managed entities are discarded, and managed
entities are unsaved. Time Stop pauses pig physics, field age/pulses and ghost
effect ticks. Greed rejects new effects and pauses existing ghosting. Existing
shared technical-role eligibility applies throughout. Captured WILD causes own
ghost displacement, field displacement/damage and field terrain effects; native
Vex/pig owner lookup is exposed for the central indirect-hit service.

Public lifecycle: `register`, constructor(GameController,LbcMana), `use`, `tick`,
`clear`, `purify`, `forget`, `stop`, `loaded`, `unloaded`, `owner`, `managed`,
`hud`, `count` and `ghostCount`. Package fixtures can use `cast`, `apply`,
`spawn`, `tickField`, `second`, `snapshot` and `vexSnapshot` and inspect exact field
`pig`, `display`, `type` and `age`. Root integration supplies shared void timer
handling, acquisition leaves, hand dispatch and native lifecycle/owner hooks.

No build, server, git, source-server, resource-pack or shared Java edits were
performed by this implementation task.


## Integrated qualification (25 October 2026)

Build `fa036823` passed86 recorded results in two actual mode1 matches (151.33s),
including all14 Soulhunter-family cases twice, the existing shared regressions,
real match-stop probes and cleanup. Exact316 source files/881 classes/2812
resources match the tested ready JAR. No new ERROR lines beyond the existing
fake-bot Polymer packet-context pair. Evidence: `build/verification/soulhunter-final/`.
Main installation was refused while its session.lock was held. The user then
requested leaving this build ready, so the main JAR remains `2558af6b`; no restart
or replacement occurred. This supersedes individual task-agent pending statuses.
Native fixture checks include explicit tick boundaries, not real client pixels,
actual disk restart/disconnect or peak-capacity qualification.

## Integrated qualification

Build `63d98904` passed 26 recorded suite results
in two actual mode1 matches (55.21 seconds), including ordinary
Soulless, all six metasolas, Soulhunter and actual retained-trial match-stop cleanup.
Exact 343 Java sources / 939 classes /
2813 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-soulless-academy-63d98904.jar`. Evidence: `build/verification/soulless-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
