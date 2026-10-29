# Indolence fingers and native rebirth - staged, 29 October 2026

(user) The exact finger selected for respawn is removed after five ticks; do
not select another nearest skeleton when that delayed cost becomes due.

> пальцы считаются немагическими сущностаями. Перерождение в них тоже немагическое. Только незримые длани глушит глушилка

Neither MAGIC nor TECH erases the physical fingers or blocks reincarnation.
Invisible hands retain their already implemented MAGIC-only policy.
Earlier approved reward repair: each armor modifier uses its actual equipment
slot; source chest/legs/feet modifiers incorrectly used head.

## Source and implementation

Sources are the legacy LBC `workingitems/witch_gens/indolence_get.mcfunction`,
`respawn.mcfunction`, the Indolence respawn helpers and `lbcr:podchinenie`, plus
Paper `SwrgNativeService.processPlayerResetPre`/respawn handling. Exact SHA256
references are in `build/verification/indolence-fingers/source-inputs.json`;
the extracted native payloads are in `source-payloads.json` alongside it.

`LbcIndolenceFingers.acquired` implements the successful-claim producer edge:
native GLOBAL random20 nonplayer selection, including non-mob entities, source
persistent silent skeleton/bow/zero-drop/empty-loot payload and finger/void
equipment assets. It does not invent a nearby mob gate or a caster fallback.
The original shared subjugation cell runs at ALL global fingers, with native
team/AI/counter ownership, trade sound, physical source provenance and a dropped
raw claw. Protected map roles/sealed dimensions retain their common guards.
This API is ready for shared Witch acquisition; it is NOT proof that the raw
Witch Gens acquisition and common admission/debit have been migrated completely.

Wrath's matching nonplayer subjugation branch now reuses the same helper.
The existing player/cantleave branch remains separate, preserving its approved
global cleanup. The helper retains allowed Sauvojen conversion while keeping
the normal protected-role policy for other callers. No second AI/team manager.

`GameController.died` retains the native death, attribution, kill/death statistics
and awaiting-respawn lifecycle. When the holder has an eligible loaded global
finger, only the participant's life debit is waived. Actual native replacement
respawn then selects a random eligible finger and uses the existing movement
flags, source reward armor, native bow/64 arrows and original raw claw leaf.
Native effect chains, accepted effect causes, owned leases and signed absorption
state transfer to the legitimate new player. No canceled death/fake player or
second lives engine. Without fingers, the ordinary island respawn/life debit
continues, with just the authority claw reissued. If fingers disappear between
death and respawn, the existing ordinary safe-spawn path is used.

The delayed cost captures the selected UUID, runs once after five actual running
ticks and pauses with Time Stop. Moving near another finger does not change it.
No-drop discard, loaded/unloaded actor lifecycle, epoch cleanup and current
authority control reuse common owners. Existing accepted child causes remain
immutable. Runtime code does not force-load chunks.

No RP additions: existing `lbc:finger`, `lbc:void`, `lbc:assasin` and
`lbc:invisible_hand` are reused; no author RP/template edits were made.

## Exact native proof and limits

Artifact SHA256:
`777d147a9127c15e3eaecf121604a839bd8720302116eeddbbddc653d7d7b282`.
`gradlew build --offline` passed. Two actual mode1 fixture matches passed
11 unique gameplay suites /12 executions in31.00s and99.11s:
indolence-fingers, indolence-rebirth, witch-wrath, polymorph-death,
witch-gluttony, witch-indolence, witch-progress, witch-passives, witch-envy,
time-stop and polymorph.

New checks exercise the actual producer API/native random selector, native body
equipment/AI and physical classification under real MAGIC/TECH markers. Reward
modifiers apply in all four corrected slots (+20 armor). Fixture-only explicit
native equipment detection is needed for the vanilla test players, as in other
armor checks; it is not a production ticking shortcut. Rebirth uses genuine
lethal native attacks and PERFORM_RESPAWN replacement players: life preservation,
one credited death/kill, hidden native effect chains, removable restored owned
leases, exact reward items, actual jammer coverage, natural five-tick chosen
finger cost with Time Stop, subsequent ordinary life debit and continuing match.

11028 frozen source/processed inputs and5224 packaged class/resource entries
match exactly.199 protected author paths,31 live overrides and installed main
`d9087f0a` are unchanged. Both children stopped normally; exact snapshots were
restored, no retained locks/recovery archive/cleanup errors.
Evidence: `build/verification/indolence-fingers/final-audit.json`,
`inputs-777d147a.json`, `777d147a-*-results.json` and matching native logs.
The standalone result/log omits the second star component. Rejected a1730ab9
logs remain: its fixture omitted native equipment detection; rebirth and Wrath
already passed there. Build failures and the intermediate build are retained.

No physical client/ping/peakTPS, cross-dimension replacement respawn or full
unloaded respawn-cost roundtrip claim. Neighbor hands/passives retain their own
bounded foreign-world selector proof. Full original25 Chaos release gate stays
CLOSED: shared Witch acquisition, Lust active families and FULL Vanity
world/all-player/core rewind still remain. No main install/restart/push.
Questions stay in CHAT; the author's pending-question file was not modified.
