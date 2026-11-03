# Red/black Nexus wires and altitude entry — 3 November 2026

## Author decisions

The author answered that **all three outside-world wire consumers are unjammable**:
red, historical `nexus_wire_blue`/green, and black. This includes the enhanced
antimagical artifact. The green policy is recorded here for its subsequent port;
this batch implements red and black only.

The black wire's gateway is intentional: retain its exact exit `(1,15000000,1)`.
The resulting extreme altitude must invoke the original random Nexus entry. The
author explicitly confirmed this feature rather than a normal-height replacement.

## Completed native chain

[LbcNexusWireItems](../src/main/java/me/skywars2/game/LbcNexusWireItems.java) registers
native Polymer red and black wires. Source repeating-command-block carriers,
`tier_q`, exact existing red/onyx models, names, colored nonitalic lore and instant
flags are preserved. Only the native item ID changes in each source root table;
all count/functions/other modifiers remain identical. Scoped testing leaves are
`lbc:ported/nexus_wire_red` and `lbc:ported/nexus_wire_black`; original roots remain
usable. Existing author RP resources were checked read-only; no RP patch is needed.

[LbcNexusWires](../src/main/java/me/skywars2/game/LbcNexusWires.java) implements:

- One red wire for one native XP entity in Nexus, regardless of its XP value.
  Conversion runs before spawn merging. The existing XP merge engine retains its
  ordinary-world behavior; in Nexus it lets native award splitting reach the wire
  producer. Produced wires float as in the source dimension. Time-stopped XP waits
  as its actual native body, then converts once when resumed.
- Outside Nexus/Imprinted, red intake consumes one matching item and grants exactly
  20 native XP points,5000 mana,5 charges to each of the seven source swords and100
  nanobot tier. [LbcSourceCounters](../src/main/java/me/skywars2/game/LbcSourceCounters.java)
  shares its canonical writer with Envy; Electro's two resources and Terra update
  real native owners as well as scoreboard mirrors. Charging progress is separate.
  Existing signed/int/native resource policies and native nanobot refresh are retained.
- Black intake consumes one and accepts a gateway placement in the current world:
  independent source integer X/Z−2000..2000, Y0..319. Black is processed before red,
  matching source ordering. Protected Inventory requires the corresponding main-hand
  wire; otherwise inventory/offhand consumers retain source broad clear-one semantics.
- Gateway chunk preparation is asynchronous. A dedicated nonpersistent temporary
  loading ticket is shared per requested chunk and removed after its last job, or
  on stop/restore cleanup. It does not populate the match-long map-loading cache.
  Placement uses the shared world-mutation/causal-owner path and a real native
  `TheEndGatewayBlockEntity` with exact teleport enabled. The erroneous legacy
  `id:beacon` is not copied, since the author requires a functioning gateway.
- Every20 active ticks, a living nonspectator ordinary-world player at Y≥14500000
  enters Nexus at independent X/Z−4800..4800, Y2332. This is environmental entry,
  without a held item, mana payment, jammer test or spell-owner admission requirement.
  [LbcHedgehogDimensions](../src/main/java/me/skywars2/game/LbcHedgehogDimensions.java)
  owns the shared native transfer, including retained motion/look and removal of
  the original `minecraft:antigravity_effect` gravity modifier.

Inventory indexing is event-dirtied, with native identity fast paths and fixed
custom-data predicates; no per-tick NBT copy or world-entity scan is added. The
altitude pulse visits the online player list. XP uses native entity-load callbacks
and a pending set, with no duplicate XP/charge ledger. Gateway work and release
bookkeeping are linear in pending jobs/chunks. No gameplay count cap was invented.

The resident checkpoint stores pending XP UUIDs and pulse phase alongside canonical
native actor bodies. Full capture waits for accepted gateway terrain work to finish.
Restore rebinds paused XP before publication; it does not replay consumed wires or
resource rewards. The full coordinator regression includes an actual paused XP orb
in Nexus through repeated world restores and point replacement.

## Source evidence and remaining boundaries

Legacy sources under `data/lbc/function/`:
`tick_nexus_xp_trans`, `other/instants/select_old`, `other/instants/select2`,
`other/nexus/wires_other_worlds/{red,black,black_at_pre,black_at}`,
`players_1s:23`, `tp_nexus_rtp`, `tick_nexus_player_tp_3`.
`source-proof.json` records source hashes, item-table equivalence and resolved rules.

This completes natural XP acquisition of red wire and the two outside-world consumers.
It does **not** claim the entire Nexus family or natural black-wire acquisition:
trees, converters, center cubes, recursive wire explosions, dedicated tools and the
green wire's full `random_glack` reward graph still require their own complete ports.
No broad reward pool was filtered, reweighted or enabled here. Protected Inventory2
and the prior broad full-rewind release gates remain unchanged. TMT remains last.

## Exact verification and installation

Evidence directory: `build/verification/nexus-red-wire-2026-11-03/`.

Test SHA256:
`602fd22c497e6ccd6213b4ef65995a4a87dc7909f3577886a9d9a61fa181e11c`.
Production:
`3beebde731409c62feb58afca736b4020db7b1295d598345bb2c444ca0fc6161`.

Fresh successful offline Java compilation;12 fresh class families and5 explicit
JSON overlays over the exact qualified final Loom archive/resource recipe.9864 other
source inputs and272 pre-existing author paths are guarded. All archive entries
are read back and verified. Production retains the exclusion of map authoring and
deletion classes. Both final `build/libs` JARs were published.

Eight actual mode1 matches pass on this exact test artifact: `nexus-red-wire`,
`vanity-rollback`, `entity-merging`, `nanobot-armor`, `witch-passives`,
`dimension-items`, `eternal-wealth`, `inventory-skills`. The wire fixture checks
native inventory scheduling, exact debit/resources and untouched charging progress,
actual cross-dimension XP conversion, differing orb values, no merging before
conversion, paused checkpoint continuation, excluded dimensions, black coordinates,
real gateway destination/teleport, released tickets and actual high-altitude Nexus
entry. The full coordinator separately restores a nonempty paused XP producer.

The initial focused candidate was rejected because the fixture queried XP rewards
before native entity sections became accessible; it was corrected to await actual
entity-section readiness. No failed candidate was installed. The subsequent focused
candidate passed before the final nonempty full-coordinator test was added. All final
checks pass; the known fake-player Polymer packet-context bootstrap warning remains.

Fixture25577 only; normal child shutdown, exact controlled snapshot/config/player/
level/ticket/gallery/OP restoration and no retained locks. This is no physical-client,
arbitrary-map or peakTPS qualification. Main installed via the normal-stop helper,
rollback064f05b8, `restarted:false`; no main-server stop/start or remote push.
