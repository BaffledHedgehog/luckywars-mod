# Core item and experience merging

The author requested larger-distance item stacking and immediate experience
stacking as in Paper. This is a server-wide engine change, including dimensions
outside the match; it does not add match-specific periodic world scans.

## Settings

Startup creates `config/luckywars/core.json` if absent:

```json
{
  "itemMergeRadius": 2.5,
  "experienceMergeRadius": 3.0
}
```

Values are in blocks, read on server startup. They select the inflation of the
native entity bounding-box search in all three axes, as with aggressive Spigot
merging; the resulting search is a box, not a sphere. Values are bounded 0..16
to bound each local entity-index query. Zero restores the vanilla behavior for
that entity type, rather than disabling all vanilla merging. Invalid files are
logged and left untouched; the server uses defaults for that start.

These are deliberately aggressive settings requested by the author, not a claim
about current Paper defaults. [Paper's 1.21 release notes](https://papermc.io/news/1-21/)
record its change from item2.5/exp3 to vanilla-like defaults; the
[Paper source](https://github.com/PaperMC/Paper/blob/main/paper-server/src/main/java/org/bukkit/craftbukkit/event/CraftEventFactory.java)
still illustrates optional spawn-time merging, summed denominations and overflow
guards. This implementation operates directly on native Fabric entities.

## Native behavior

`CoreEntityMergeMixin.Items` changes only the search box passed to native
`ItemEntity.mergeWithNeighbours`. Existing 2/40 tick movement-dependent cadence,
maximum stack size, item/components, pickup ownership/delay, age, display-item
exclusions and `DropTrapMergeMixin` causal trap/Metal Pipe guards remain native.
Large full stacks are not scanned for further merging.

An XP award creates one actual orb for its complete amount, preserving native
directional launch/motion. At `ServerLevel.addFreshEntity`, a new orb can be
absorbed by a nearby resident orb before publication/tracking. Mixed values and
native grouped `Count * Value` are summed with long arithmetic into one Value
and Count1. It is then collected in one normal pickup, including native Mending
first, without changing the player's ordinary pickup cooldown. Other newly
spawned nearby orbs join it immediately. Existing/loaded orbs retain the native
20-tick nearby-merge cadence but use the wider search and mixed-value merging.
No per-tick whole-world scan, persistent cache or global entity queue was added.

If a total cannot fit in a signed positive int, its orbs remain separate; XP is
not clamped away or wrapped. Native legacy groups above that limit remain
grouped and collectible. Integer Value persistence replaces the old short
storage, accepting old short saves too. Mending retains native item selection
and proportional rounding while widening the multiplication; an iterative loop
also avoids recursive repair frames. Time Stop skips the new XP fast paths and
keeps frozen entities under the existing native gates. No RP/client change.

## Qualification and installation

Build **b54c8454**: `build/verification/skywars2-entity-merging-b54c8454.jar`.
Full SHA256: `b54c84543c2729b6e6be760d56db511894cff64450524969b8a85e20c876c8af`.
Offline Gradle build passed. Source JAR matches all523Java files; main JAR
matches1482compiled classes and3224processed resources. Fabric metadata differs
only by Loom's expected nested SGUI registration.195preexisting author paths
and31live grimoire override files retain their initial bytes/existence.

`CoreEntityMergeChecks` ran in two actual mode1 matches through the isolated
runner, alongside TNT trap merging, Metal Pipe and Time Stop/contact regressions:
18recorded suite results,93.09seconds, no failures. Native tests cover extended
XYZ item merging, exact counts and limits, distinct components/owners, display
pickup exclusions, radius boundaries, both zero-radius fallbacks, mixed XP,
complete awards, short/int save compatibility, grouped legacy XP, int overflow,
periodic existing-orb merging, whole-group pickup and large-value Mending.
Each 1000orb spawn burst immediately retained exactly one indexed orb and3997XP
(21.6782ms cold /3.5228ms warm on this isolated server). These measurements are
not a production TPS or hardware-independent performance guarantee.

Child stopped normally; JAR/config/profile/scoreboard/player snapshots were
restored exactly. Evidence:
`build/verification/entity-merging-gameplay-results.json`,
`build/verification/entity-merging-gameplay.log`,
`build/verification/entity-merging/qualification.json`.

Installer PID15932 is queued for the running main23a25d43 server's normal
shutdown, and does not stop or restart it. The authoritative receipt is
`build/verification/entity-merging/main-install.json`. Retain b54c8454ready,
23a25d43installed/rollback and existing8d2f7513/6890ae59fallbacks while installation
is pending; no extra intermediate builds were created.
