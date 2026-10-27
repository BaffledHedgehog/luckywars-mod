# Complete Shulker armor abilities

The existing four original leather pieces now use their source worn abilities.
[Exact source/resource review](SHULKER_ARMOR_SOURCE_2026_10_27.md).
Original rarity, lore, models, durability900, armor/toughness and the boots'
knockback resistance remain unchanged. The flag-only legacy helmet is admitted
too; abilities do not require the modern transformable/slot flags.

Helmet: one real Shulker bullet every200active ticks, spawned three blocks above
the wearer with initial upward speed5 and Steps40. It selects the nearest
eligible enemy player, retaining native homing/contact behavior. A direct handle
replaces the old nearest-shell selector. Its native/current projectile owner,
causal tracking, harmful-target guards, Time Stop and epoch cleanup share the
existing creature service; the existing nine-bullet trap remains WILD.

Leggings remove Levitation every two active ticks. Per the author's correction
in this session, Magic Stopper also suppresses the helmet and leggings; their
missing source gates were an acknowledged legacy mistake. Chestplate rolls20percent from the actual
entity_hurt_player criterion, including blocked shield hits, and uses the shared
resident surface search within the original40x40 square. Original reverse-portal
and portal particles and both sounds remain anchored to the function origin.

Four worn pieces refresh ResistanceII for20ticks and apply the single source
movement modifier `minecraft:shulker_armor`, -10 multiplied-total. The source
noarrows operation deletes arrow-tag entities within16, reusing its existing
interception effects and erasure. A real jump teleports near a point five local
blocks forward with maxRange3; this is not an offhand-swap ability. Chest,
full-set refresh/interception, helmet, leggings and jump teleport respect Magic Stopper. Existing
speed restriction persists when entering its aura until equipment changes;
gear removal, death/forget and match stop remove the owned modifier.

Teleports preserve facing and use the same finite resident-column surface
helper as other ports, with Nether ceiling120. No chunks are loaded merely to
search for a destination, and absence of a valid landing does not create one.
Other role/dimension/Time Stop boundaries remain shared.

The already working normal-slot and Ultra family reforges retain their original
weights and outputs. Seven exact loot leaves are now public test entries; the
previously unpackaged five-draw and complete-outfit tables are also included.
This does not enable the wider unported Lucky Block or full-set parent pools.
The author's existing RP resolves all four models/equipment assets and keys;
no template or actual RP changes are required by this port.

## Verification scope and earlier failures

Native fixtures exercise original seven loot tables (one/five/four outputs),
normal and Ultra reforge inputs, actual worn pieces, shell swept contacts and
changed native owner, enemy/team/role boundaries, real jumps and resident-floor
landings, incoming environmental and full-shield criteria, the author's new
helmet/leggings suppression, real2/200tick scheduling and actual match stop.
Third-party fixture players are temporarily excluded by an owned technical tag,
then restored; their position/team is not repurposed as a fabricated target.

Initial native evidence is retained under `build/verification/shulker-armor/`.
It exposed a real clock lifecycle error: the new armor phase advanced in lobby
after stop. The production clock now runs only during a match. A sound assertion
also incorrectly required the teleported wearer to hear both origin sounds even
after leaving their native16block audible range; a stationary native listener now
observes them. A later fixture roundtrip between dimensions needed actual client
load/teleport acknowledgement before testing damage: vanilla correctly rejects
damage while the player is still changing dimension. No damage or sound rule
was weakened to satisfy those assertions.

Fatal wearer admission is captured at the existing native hurt-attempt boundary;
that particular death scenario and the shared Nether120ceiling are code-reviewed
rather than directly exercised here. Physical client pixels, arbitrary-map
landings, actual network latency and peakTPS are not claimed by these fixtures.

## Qualified and installed

Final offline build passed. Two actual mode1matches passed28recorded results
in124.78seconds, including the new suite, retained native stop/cleanup probe,
previous armor sets, native gear, jump/incoming artifacts, effect layers and
Time Stop. Owned child shutdown and exact fixture snapshots restored normally.

Build `154cc3a7`, SHA256:
`154cc3a7118e0759ac33a955b2952781b349de664000c995cb430cb8d2f86261`.
Exact525sources/1490classes/3226resources match the archives. All195preexisting
dirty author paths and31live grimoire overrides retain their original bytes or
absence. Evidence is `build/verification/shulker-armor/`; ready artifact is
`build/verification/skywars2-shulker-armor-154cc3a7.jar`.

Installer PID14244 installed over a4683fe2 with the main server stopped, without
forcing shutdown or restarting it. The installed hash matches the tested JAR;
`main-install.json` is authoritative. Retain154cc3a7, immediate rollback a4683fe2,
fallback46034d3c and the separate network-audit tool. Older23a25d43ready JAR was
removed only after installed/new/rollback hash and absolute-path verification.
