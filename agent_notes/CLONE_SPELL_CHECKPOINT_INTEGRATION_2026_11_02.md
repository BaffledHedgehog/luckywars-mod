# Clone actor spell checkpoint integration ? 2 November 2026

MatchSpellCheckpoint now composes CloneFinalAbilityStates.Checkpoint using its
existing shared Links graph. Frozen restoration resolves all roots, prepares policy,
control, summons and logical actor plans before mutation, then installs their typed
state. Stable capture/restore/publication calls cloneSkills.checkpointBoundary.

Short queue cancellation was inspected narrowly: LbcWands.stop only clears
ElectroTrace set; SauvojenStars.stop only removes its own detached 40/100/25-tick
actors. Invoke these after successful frozen owner install, plus cloneSkills.clearPending.
Do not call sauvojen.stop or erecto.stop: restored long Drone/Sphere state survives.
Hammer state is likewise preserved by its typed checkpoint. No original source/cost
or already-emitted world history is replayed.

Client presentation is deferred. The new MatchSpellCheckpoint.publishFrozen method
checks stable frozen state and completed install, publishes final owner models once,
and is idempotent. restoreFrozen never attaches these long actors' client visuals.

ROOT wiring required outside this task's exclusive source scope:

- LbcVanityRollback final publication phase must call image.spells.publishFrozen.
  Its current spells.restoreFrozen runs in restoreBodies before later authorities,
  status ownership and canonical native views, so early publication there is invalid.
- MatchRuntimeRebind.publishFrozen must call spells.publishFrozen after its body/
  private installation; prepareFrozen currently invokes restoreFrozen too early to
  publish client models.
- LbcItems must expose cloneSkills() as root's engine accessor (not present in this
  task's initial read; intentionally referenced using root's instructed API).

Only MatchSpellCheckpoint.java and this own note were edited. Scoped diff --check
passes. No compilation, build/native process, installation or runtime qualification
was performed. Source edits are frozen for root integration and exact validation.
