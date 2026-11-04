# Native BedWars arena protection owner ? 4 November 2026

Implementation handoff; not compiled or runtime-qualified by this agent. Owned:
`LbcBedwarsArena.java`, `BedwarsProtectionChecks.java`,
`mixin/BedwarsProtectionMixin.java`, `mixin/BedwarsCopyProtectionMixin.java`,
`mixin/ArenaBlockMixin.java`, this note, and the THREE-line preflight/feed at the
start of `MapManager.restoreSnapshot(ServerLevel,BlockPos,BlockSnapshot,int)`.
Other MapManager regions and all global integration remain root-owned. No source
server/RP/author WIP edit, build, server operation or commit. Baseline supplied by
root: first overnight a4de834c / installed223c424b; protected author manifest lives
in ignored `build/verification/bedwars-native-2026-11-04/baseline.json`.

## Integration API

`LbcBedwarsArena(GameController,Policy)` belongs to root's `LbcBedwars`; native
bridges resolve `game.bedwars().arena()`, safely tolerating uninitialized owner.
Policy has `active()`, `canBreakBed(ServerPlayer,int team)`, `bedChanged(Bed)`,
`lifecycleAllowed(Lifecycle)`. Root must include actual participant/role/input/
TimeStop policy in canBreakBed; active is initialized mode3/countdown policy,
not a blanket global game-mode check in every native hook.

Public records:

- `Bounds(minX,minY,minZ,maxX,maxY,maxZ)` inclusive six integers;
- `Marker(UUID id,ServerLevel level,BlockPos pos,MarkerKind kind,int team)`;
- `Bed(int team,UUID marker,BlockPos foot,BlockPos head,BlockPos spawn)`;
- `Topology(ServerLevel level,List<Bounds>,List<Marker>,List<Bed>)`.

MarkerKind is BED,SPAWN,SHOP,COPPER,IRON,GOLD,DIAMOND,STAR,LOOK. Markers retain
actual placed UUID/realm/position; duplicate generator positions remain separate
records. Bed parts/team positions are explicit root topology, never inferred from
rendered text or vanilla dye ordinal. Records defensively copy coordinates/lists;
index rejects duplicate bed teams/parts, marker identities or mismatched realms.
Root remains responsible for complete semantic source-marker validation.

`beginCapture(level,bounds)`, `captureCell(pos,state)`, `activate(topology)` build
one final placed original-type baseline. Root opens builder before placement and
activates after team init, empty-team cleanup, spawn beacons and removal of lobby
overlay. ArenaBlockMixin records actual successful final current states before
Policy.active is tested; allowed identical-state attempts also feed at HEAD.
MapManager.restoreSnapshot feeds identical cells before its no-op early return.
AIR removes a pending entry. Only the declared capturing realm/bounds are read;
no fullheight/chunk/world scan and no capture of ordinary later player additions.
Caller may explicitly feed any completed placement cells through captureCell.

Original/capturing/bed-part indexes use primitive fastutil long maps, avoiding
Long allocation on ordinary block-policy reads. Activated original map is immutable
and shared with reusable checkpoints. Bed/beacon removal copies only a small
primitive unlocked-coordinate set, not the large baseline on every broken bed.

## Native type policy / transaction scopes

`allowsType(world,pos,wantedState)` and `allowsRemoval` consult the SAME owner.
Default `STRICT_IGNITE` compares original Block identity; all same-type properties
and NBT remain allowed. Optional `BUKKIT_PLANTS_FLUIDS` includes the original
water/lava/powdersnow/flowers/grass/fern exemptions but is NOT enabled automatically.
This source default is pending root nightQ8, not new author approval.

`lifecycle(reason,level,exactBounds,Runnable)` checks the root Policy before issuing
a lexical server-thread footprint permit. Lifecycle enum is MAP_PLACEMENT,
MAP_RESET,MAP_RENEWAL,COORDINATOR_REPLAY. Scope closes in finally. Update flags,
item tags, MAGIC identity and mere maps.busy/vanity.busy are not privileges.
Authorized beacon setup is performed before activation while builder remains
open; later bed removal authorizes only its exact registered parts/beacon.
Root must apply lifecycle permission to native world-publication steps, not turn
it into a global ability bypass. No private terrain undo exists.

Native clone/template calls use a separate synchronous Transaction. `admitCopy`
preflights final types against original policy (ignoring other copy temporary
permissions), then permits native BARRIER staging only at those exact admitted
cells. Native processed template list is reused, so processor RNG is not replayed.
Transaction finally closes every cell permit, including failure. Bed notifications
are deferred until outer native transaction completion: transient barriers cannot
falsely remove bed entitlement when a same-type copy finishes with a bed again.

## Native hooks (root registers mixin configuration)

Existing ArenaBlockMixin cancellable HEAD rejects BEFORE indirect support/source
attribution and map-overlay observers. Successful RETURN feeds capture and emits
bed changes; denied writes do neither. Existing journal/MAG/etc. callbacks remain.
MapManager.restoreSnapshot checks policy BEFORE Clearable.clearContent or BE NBT
load; an allowed identical-state snapshot still restores NBT correctly.

Register these nested mixins, not the unannotated holder classes:

```
BedwarsProtectionMixin$Destruction
BedwarsProtectionMixin$Chunk
BedwarsProtectionMixin$Breaking
BedwarsProtectionMixin$ExplosionBlocks
BedwarsProtectionMixin$Falling
BedwarsProtectionMixin$Piston
BedwarsProtectionMixin$Fluid
BedwarsProtectionMixin$Fire
BedwarsProtectionMixin$Tnt
BedwarsProtectionMixin$Bucket
BedwarsProtectionMixin$BucketPickup
BedwarsProtectionMixin$EndermanPickup
BedwarsProtectionMixin$PistonAccess
BedwarsProtectionMixin$FluidAccess
BedwarsProtectionMixin$FireAccess
BedwarsCopyProtectionMixin$Template
BedwarsCopyProtectionMixin$DimensionPosition
BedwarsCopyProtectionMixin$Clone
```

Destruction checks Level.destroyBlock before loot/XP/particles. Chunk covers direct
live LevelChunk.setBlockState writes; detached palette work remains untouched.
Breaking preflights ServerPlayerGameMode.destroyBlock before playerWillDestroy,
tool damage and paired-block behavior; rejected native player edits resync.
ExplosionBlocks guards BlockStateBase dispatch before subclass loot/TNT callbacks.
Falling checks actual removal success before native fall() publishes its body;
a denied source returns a removed, unpublished native body rather than a duplicate.
Piston validates its finite native resolved source/destination/destroy/head set
before any drops/moving-state/BE publication. Fluid/Fire/Tnt guard source callbacks
before retained terrain can emit destruction loot or a fuse entity. Bucket guards
pre-debit fluid displacement; BucketPickup prevents filled-bucket mint from an
original protected liquid/powdersnow. EndermanPickup rejects before remove/event/
carried-block mutation. All are exact current26.3 native methods, read locally.

Template wraps placeInWorld and preflights its actual processed/mirrored/rotated
cell list before publication. Clone wraps the actual native CloneCommands.clone
method via typed private-record accessors/Coerce, bounds native scan by the command
limit and loaded area, preflights both selected MOVE source removal and destination
before either publishes, and permits exact same-type BE staging. Native source
predicates are ordinary deterministic block/NBT predicates; no command interpreter
or random palette reroll was added. Runtime mixin application must be qualified,
particularly the private native CloneCommands signature and Enderman local capture.

## Native bed transaction and full coordinator

`playerBreak` returns null for an ordinary admitted native break, false for denial,
and true after its owned registered enemy-bed transaction. It validates both real
same-color FOOT/HEAD states/facing and current root permission, retires both
protection positions plus actual BEACON at spawnY-2, writes source no-drop AIR and
plays wool break. `Policy.bedChanged` runs only after both parts/beacon succeed.
Root owns announcements, participant have-bed state, respawn cancellation and win.
Later player-built beds at retired positions are ordinary unprotected additions;
they cannot reactivate original protection or silently regain entitlement.

`checkpoint().prepare(Function<UUID,Entity>)` returns existing MatchFieldCheckpoint
Plan, with immutable topology/type map plus small removed-coordinate set and
exemption policy. `prepareFrozen/publishFrozen` suppress lifecycle callbacks while
root's canonical player/terrain/body owners restore. No body duplication, private
rewind engine or second scheduler. Root composes it in its existing BedWars owner
and whole coordinator; prepare before entering world replay publication permits.

Boba's existing mode>=3 player branch is unchanged. Approved Supernova same-match
renewal remains root-owned; root retargets baseline/topology after source map
replacement. Pending nightQ9 defaults to preserving lost-bed entitlement, not a
new approval to renew beds. Generic native source chunk MOVE (`LbcNexusClone`)
still needs root's explicit two-cell admission/authorized lifecycle handling:
void restoreSnapshot rejection must never be followed by deleting its MOVE source.
Hagalaz/source-center/shop-role policy remains root-owned; these hooks alone are
not proof every non-terrain effect respects protected roles.

## Qualification helpers / remaining root work

`BedwarsProtectionChecks.runNative(game,player,Cells)` runs ONLY against the real
active ordinary mode3 arena. Cells record has stone,chest,sand,tnt,added,copySource,
piston. First four must be actual placement-captured STONE/CHEST(nonempty)/SAND/TNT;
last three start at original AIR; piston.east()==stone; added has solid support
below. Root builds these in its actual saved-local-map fixture, not a fake policy.

Tests call actual native player placement/break, Level destruction, typed snapshot,
fall(), prime(), fire/fluid/piston transaction invokers, real ServerExplosion block
application via existing NuclearTerrain bridge, actual native template and
Brigadier clone/MOVE. Assertions check retained inventories/BE images, zero
unexpected loot/XP/falling/TNT publications, no moving BE, exact rejection of
partial MOVE, allowed same-type template/clone NBT, and ordinary added-block loot
positive control. OwnBed/enemyBed helpers exercise actual gameMode.destroyBlock
on real halves and owned beacon; root must wrap enemyBed in its real nonempty
whole-coordinator before/after replay and verify lifecycle announcements/state.
The three Access mixins expose existing native methods for those fixture calls;
they do not supply an alternate publisher or test engine.

No tests/builds/server were run by this agent. Scoped whitespace/static API review
passed. Root must compile/register/apply mixins, run actual mode3/local/bounds/
TimeStop/full-replay and renewal cases, and keep public mode3 gated until all
owners and exact final artifact are qualified. The earlier read-only audit
[NIGHT_BEDWARS_PROTECTION_2026_11_04.md](NIGHT_BEDWARS_PROTECTION_2026_11_04.md)
records source disagreements and additional release cases; it is not runtime proof.
