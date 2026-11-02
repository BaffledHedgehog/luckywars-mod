# Attachment actor derived-index rewind repair ? 2 November 2026

Native actors restore with ENTITY_LOAD/UNLOAD suppressed. A GrimoireBoss or
BouncingPoop that retired after capture had already removed its entry from its
service's derived actors set. Restoring its persistent DATA/NBT alone therefore
revived an inert native body.

MatchActorCheckpoint.restoredBodies now exposes only saved canonical actor image
references, after bodiesRestored, excluding omitted/removed actors. It does not
scan worlds, enumerate chunks, load chunks or invoke ordinary admission callbacks.

At the end of LbcVanityRollback.restoreBodies, after private source/status/actor
plans have installed, pass that list to narrow reindexRestored methods in the two
owners. Clear old derived actor references and register current valid tagged
bodies with existing DATA/current-epoch/gameplay checks; bind source using the
restored attachment's exact original author/cause. Never change/save DATA, create
new attacks, cast, pay costs, resurrect unrelated actors or call entityLoaded.

Grimoire also closes stale warning holders and removes old boss-bar viewers
without global music operations. Its next ordinary tick rebuilds presentation:
phase1 laser warning uses DATA phase/age/aim, origin and current clip geometry;
kind2 child warning uses DATA age/aim. Frozen reindex must never invoke laser or
tickChild, which also contain damage/sounds/emission at other phases. Native
saved model/core/attack children are already in the canonical list; none are
recreated or replayed by this reindex.

Real reconnect's MatchRuntimeRebind does not replace native bodies, so it does
not need this full-rewind reindex. Changes are limited to MatchActorCheckpoint,
LbcGrimoireBoss, LbcBouncingPoop, LbcVanityRollback and this note. Scoped
`git diff --check` passes; no build/native process was run. Source edits frozen.
