# Bounded edible spell native checks — staged

29 October 2026. New files only: `EdibleSpellChecks.java` and this note.
No builds, server runs, resource/RP edits, production edits or commits.
These assertions have not yet been compiled or run by this agent.
Full Chaos and broad food release remain gated.

## Runner

Root command: `edible-spells` -> `EdibleSpellChecks.run(game)`.
Call `EdibleSpellChecks.tick(game)` once per actual game tick alongside the other
async check runners. Maximum200 real server ticks; ordinary completion should
take roughly20–40ticks. It does not invoke production service ticks manually,
rewrite clocks/random fields, fake warmup, or directly call consumed/using/fangHit.

Requires an actual active mode1 match, idle TestAlpha/TestBeta, closed menus,
no borrowed TimeStop, empty edible child/body service, and no borrowed owner
cold state. It forces only its own chunks for an isolated25cube at Y224,128
blocks from TestAlpha, requiring air throughout and other players farther64.
Force flags are restored only for newly forced chunks.

## Actual native paths asserted

Food inputs expand the registered native `lbc:rofls` and `lbc:imba/buckets`
leaf entries with fixed native loot context/seed17. All components are retained;
only count is copied to2. No synthesized food flags or fake consumable
components replace actual source outputs. Multi-criterion damage/poison/wither
food is excluded from single-effect witnesses. Root must package the normalized
tables and their actual transitive bucket table before running.

* `ItemStack.finishUsingItem(level,owner)` invokes actual native Consumable,
  consume-item criterion and shared GameController callback, then exactly one
  native stack debit. No direct service call stands in for the callback.
* Self arrow15 must reduce native HP and retain ARROW damage type even while
  the caster belongs to a no-friendly-fire team.
* Actual food effects verify Poison amp9 -> shared cap4, Regeneration amp10 ->
  cap5, Weakness600amp9 hidden, all three200amp1 hidden buffs, Haste1200amp127,
  and a vanilla unflagged golden apple negative control.
* Source regeneration helper is hidden, but the carrier is a golden apple:
  native26.3 Consumable runs criterion before onConsumeEffects, whose lower
  native Regen I changes visibility even under stronger Regen VI. The complete
  native food assertion therefore expects200ticks/amp5/**visible**. This follows
  native MobEffectInstance.update rather than pretending carrier effects stop.
* One isolated actual chaos food executes native immediate blast, then inspects
  exactly one native lightning and one attributed/current-epoch rocket before
  either delayed actor ticks. The original five shapes, colors, fade colors,
  trail/twinkle and one-item rocket count are asserted independently against
  literal source values. No native-world creeper or fuse0 TNT remains.
  Children are removed immediately; this is not a sustained fireworks benchmark.
* Actual native edible fang is consumed, paused by real TimeStop for5server
  ticks, then allowed to reach its real native attack tick. Caster and allied
  TestBeta, on a no-friendly-fire team, must each take exactly6HP. Current epoch,
  native owner, frozen admission and owned cleanup are checked.
* Lava/snow inputs start through actual `ServerPlayerGameMode.useItem` and wait
  for real player ticks and actual UsingItemTrigger hook. Harm/cold must occur
  while count remains2. Cancelling use then calling native finish must debit
  exactly once without an additional using-only damage/cold application. Root's
  independent gain_bucket completion may publish its native reward normally.

## Restoration and scope

Both borrowed players snapshot and restore inventory/crafting/cursor, native
attributes/modifiers, effects/tags, position/motion/known movement, camera/pose,
abilities and damage immunity timers, full profile, existing player scores,
advancement criteria, food data, combat fields, equipment cache and touched
custom/item-use statistics. Per-participant indirect kill/status attribution is
saved independently. The temporary native client listeners perform real load
and teleport ACKs, including after restoration, before reconnecting the original
listener; no dimension-change immunity is force-cleared privately.

Native player abilities/mode/physics/immunity timers/tags are explicitly checked
after restoration. The old teams are restored. Native source children only are
cleaned; teardown does not delete arbitrary entities spawned elsewhere while
the async fixture is running. All captured terrain is restored and compared.
Failure before fixture preparation does not clear borrowed services/entities.

This bounded fixture does not qualify all25spell flags, all named food families,
global slab conversion, both spreadplayers branches, permanent sheep camera,
52bolt workload,720Danmaku cards,32nuke blasts, natural reload/disconnect,
stale-epoch rejection or peakTPS. Those require separate isolated fixtures.
It never invokes those large emitters merely to inflate branch coverage.

## Read-only book motion review

`LbcEdibleSpells.book` uses native connection.teleport for its pitch-40 edit.
`TeleportMomentumMixin.Connection` wraps
`ServerGamePacketListenerImpl.teleport(PositionMoveRotation,Set)` at lines27–35:
zero-motion coordinate/camera edits snapshot/restore the full server Vec3 and
use relative motion flags. Its Entity wrapper also preserves motion across
native teleportTo. Thus the existing global mixin already protects book motion;
no edible production patch was made or requested for that behavior.


## Root native review correction

Candidateaac03998 exposed an incorrect pure-vanilla visibility expectation in
this fixture. The established shared EffectLayers merge retains source hidden
RegenerationVI200 separately above native visible golden-apple RegenerationII100.
The corrected assertion checks BOTH independent layers, including icon/particle
visibility, instead of expecting the lower apple to change the stronger layer.
No production effect rule was weakened. Earlier logs/proofs remain local.
