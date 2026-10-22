# Home drop and magic mirror - QUALIFIED (22 October 2026)

Source: [home/falling-drop audit](HOME_AND_FALLING_DROPS_SOURCE.md), section
Home source. This is a complete drop/destination/mirror batch; whole Lucky Block
pools remain gated. The original resource pack is read-only and unchanged.

## Behavior

- `skywars2:home_drop`, public loot `lbc:ported/home_drop`: normal source
  repeating-command-block carrier, `lbc:drop_not_trap`, tier B, yellow nonitalic
  `drop_sethome`, white nonitalic `trap_desc`, `sethome:1,drop:1`.
- Dropping one unit creates a marker at floor XYZ + 0.5 and one bound mirror
  at the token's actual fractional position. The shared special-drop dispatcher
  owns source branch order, admission, epoch, time-stop pause and one debit.
- `skywars2:magic_mirror`, public loot `lbc:ported/magic_mirror`: source
  carrot-on-a-stick carrier, `lbc:sethomer`, tier D, aqua nonitalic name and two
  white nonitalic lore lines. Source flags remain `magicwand:1,supersethomer:1,
  tpid:0`; the native binding adds `skywars2_home` with the destination UUID.
  The public mirror leaf is intentionally unbound, as the original `sethomer`
  table was before the drop assigned its ID. Obtain a usable bound mirror by
  activating the drop.
- Explicit handles replace the source's nearest-marker/nearest-item assignment.
  Nearby destinations cannot race; the mirror remains transferable between
  players. No player UUID ownership restriction, mana cost, cooldown,
  consumption, durability use or Academy upgrade is introduced.
- Right-click uses the shared two-hand dispatcher and `offhandWands` setting.
  A valid mirror teleports to its marker +1Y in the same dimension, preserving
  the source's destination position. Before teleport it now clears every block
  intersecting the arriving player's full hitbox, including standing headroom
  when crouching or swimming and the current attribute scale (user correction).
  A block merely touching the outer face is preserved. The complete volume is
  checked against resident chunks and the native block-modification gamerule
  before any write or teleport; rejected attempts do not partially clear it.
  Missing/deleted/invalid bindings only emit
  the original portal particles and teleport sound, without teleportation.
  Legacy stacks with the mirror flag reach the same dispatcher; old numeric
  `tpid` values alone do not bind newly created native destinations.
- Every 20 active ticks the marker emits five force end rods, strict-sets
  light-blue stained glass at its floor Y-1, and strict-clears Y+1..+2. The source
  periodic geometry remains; the full arrival clearance is additional and
  immediate on use. Strict replacement suppresses container ejection. Periodic
  terrain writes retain the original thrower; arrival clearance belongs to the
  player using the mirror for indirect consequences.
- Antimagic beacon radius 20 and artifact deletion radius 25 permanently remove
  the destination. Removing suppression does not restore it. Existing glass/air
  changes remain until normal map cleanup. Use inside suppression is blocked.
  The destination is checked again before teleport, closing tick-order races.
- Death or disconnect of the original thrower does not expire the destination;
  all destinations and owned tickets are cleared at match end. Player role,
  frozen/stunned input and dimension time-stop guards reuse existing policies.

## Chunk ownership

`LbcHomes` owns a registered nonpersistent flags-14 ticket type, loading and
simulating the destination with radius two (center level 31), equivalent to the
source's need for an active forced chunk. References are counted per level and
chunk. Removing one of two destinations in a chunk does not remove the other's
ticket; moving a marker transfers its reference. No global forced-chunk list is
modified, and no synchronous chunk load/join occurs in activation or use.

Ordinary chunk unload releases the live reference and retains same-match UUID
and author until reload. A normal destination ticket prevents that unload;
restart persistence is not claimed. Unknown or stale managed markers are
discarded without creating tickets. The home registry is indexed by UUID and
contains only admitted destinations; no whole-world marker search is added.

## Initial batch qualification

Build and two actual isolated mode-1 matches passed: 54 suite results in 84.93s,
including falling-netherite/zombie-TNT/corrosion/hole/cage/Creeper/TNT/chest
regressions. Native give/loot prototypes, two-unit shared debit, exact fractional
binding/transfer, both hands/config, zero mana/unchanged stack, native Spectator
and suppression guards, 20-active-tick renewal, strict container clearance,
time-stop pause, close-marker ticket references, dangling mirrors, beacon and
artifact 24/26-block boundaries, and actual match-stop cleanup passed.

Ready artifact: `build/verification/skywars2-home-drop-93ca702a.jar`, SHA-256
`93ca702aafbc3fed43f9dc2d8577d6386a77143414646c8d7d31cf3dc39fa742`.
Evidence: `build/verification/home-drop-final/` (`build.log`, `native.log`,
`server.log`, `results.json`, `qualification.json`). All 168 then-current Java files
match the source JAR exactly; 2666 processed resources match the release JAR.
ZIP CRC, mod metadata and nested JAR entries passed. Main server JAR remains
`69de8601`, not installed. Isolated files were restored, server stopped and both
runner locks released. The two known Polymer fake-bot chunk-packet-context
ERROR lines remain the baseline; no failed suite remains in the final run.

The home-only stop ownership check is separate from actual match stop:
MapManager itself resets arena forceloads during map transitions. This batch
neither changes nor claims to preserve that broader behavior. Real-client
particles/sound/rendering, actual chunk unload/restart/disconnect persistence,
cross-dimension use and peak capacity are not qualified. Thrower death/disconnect
does not have an expiry branch but was not exercised as a runtime fixture.

## Full arrival clearance (user, 22 October 2026)

> Да, освобождать место под весь хитбокс

This supersedes the initial source-only two-cell arrival behavior: the source
may leave a ceiling at head height because destination Y is fractional.
Build and expanded fixtures passed in two actual isolated matches: 54 suite
results in 84.41s, including every previous drop regression. Immediate Y+3
head/chest clearance, suppressed inventory ejection, crouched standing headroom,
native scale 5 (exact 3x10x3 volume), six preserved outer boundary cells and
atomic rejection at native limit 89/acceptance at 90 passed. Periodic renewal
and the original destination coordinates remain unchanged.

Current ready artifact: `build/verification/skywars2-home-clearance-3ca952a0.jar`,
SHA-256 `3ca952a09b33a28f57815190bdc3f5b4aefa2325b0208d7bf88794b034a4d19b`.
Evidence: `build/verification/home-clearance-final/`. All 168 Java source files,
508 compiled classes and 2666 processed resources match their JARs exactly;
CRC/mod metadata/nested dependencies passed. Main69de8601 remains unchanged,
not installed. Isolated backups restored, server stopped, fixture locks released.
Only the same two baseline Polymer fake-bot packet-context ERROR lines remain.
Extreme-scale/build-height and unresident-arrival paths are source reviewed,
not runtime fixtures; previous client/persistence/capacity limits still apply.

The public unbound mirror is not a replacement for obtaining the bound drop.

Next bounded work: audit remaining special-drop acquisition branches and their
resource closure; do not expose whole Lucky Block pools or unfinished modes.
