# Native utility chain — 4 November 2026

Status: qualified and installed; main server was not stopped or restarted.

## Author decisions

For the IMBA teleporter, Dirter and Bedrock Bastion:

> оставь все как в легаси

For God Dice:

> Ничем не глушить

The unusual teleporter platform height is deliberate:

> там же эффект защиты от пустоты дается - он игрока перемещает наверх из пустоты и в итоге он на платформе остается нормально

The White Arrow is the same attack as the hedgehog's arrow:

> ну ты то датапак посмотри емае, все там есть. По факту да, это та же белая стрела что и у ежа

The source handler was initially missed by searching only the `lbc` namespace.
Its actual player handler is in `lbcr`; that earlier absence claim is corrected.
The author confirmed the follow-up about the reusable item and permanent mana cost:

> Да, как в легаси

The implementation preserves the source cost and no item debit.

## Source and implementation

Legacy root is read-only:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

* IMBA teleporter: `lbc/function/projectiles/gadgets/teleport_imba*.mcfunction`.
  Native used-hand input, TECH within 24 blocks, 1488 blocks along the view,
  +30 void-resistance seconds and Slow Falling for 600 ticks. After 20 active
  ticks, it places a 5×1×5 white-concrete pad relative to the player's current
  coordinates. Source Y≤1 or Y≥321 maps to 320 before the pad's Y−2 offset.
  Existing `LbcMana.voidProtectionSecond` owns the independent return to Y=320;
  the teleporter does not add a second return teleport. Each player owns their
  pending delay; TimeStop pauses it. Accepted pad work shares the terrain queue.
* Dirter: `lbc/advancement/dirter_place.json` and
  `lbc/function/workingitems/imba/place_ground.mcfunction`. A genuine native
  coarse-dirt BlockItem placement pays once, then the source player-centred 9×9
  fills publish grass, dirt, stone, deepslate and bedrock at the literal source
  heights. Both endpoint orders retain native fill semantics. No jammer applies.
* Bastion: `lbc/function/workingitems/imba/bastion_create.mcfunction` and the exact
  existing `bedrock_bastion.nbt`, including its AIR. Native egg interaction uses
  the source marker's position without publishing a panda or service marker.
  The exact 17³ template has 4913 cells, two palette entries and no entities or
  block entities. It is placed at offset −8/−1/−8. No jammer applies. Source
  particles and sounds remain; x2/x4 uses the canonical finite clone budgets.
* God Dice: `lbc/function/projectiles/gadgets/superdice.mcfunction` and its six
  leaves. No jammer applies. One native hand debit, source announcements and
  normal six-face draw; final luck modifier 7 retains the second draw in 4..6.
  Native death preserves Greed's explicit exclusion; the other outcomes retain
  source attribute quantization, permanent effects, literal repeated increments
  and the real White Arrow reward. There is no separate persistent dice ledger.
* White Arrow: `lbcr/function/raycast*.mcfunction` and the shared
  `raycastarrow` source actor. The native player arrow and hedgehog delegate to
  one precise hitbox/particle/sound tracing step. Source 20 active steps of 2.5
  blocks pass through walls; hostile player ownership/team exclusions remain.
  Greed, void resistance and protected service roles survive; Chef is permanently
  erased through the shared execution owner. Source activation checks MAG and
  maximum mana ≥500; current mana may become negative after the two 500-point
  debits. The item remains reusable, as explicitly confirmed by the author.

Large terrain work uses the existing shared fair queue, bounded chunk leases,
canonical BedWars protections and world/causal journals. No per-cell actors,
arbitrary function interpreter or live-world asynchronous reads are added.
Accepted finite structures drain. Typed work, pending delays and active rays join
the existing whole-coordinator checkpoint; short swap charges keep their agreed
rewind cancellation. Native item prototypes are decoded once from their canonical
loot definitions, preserving names, lore, tier, models and source counts. Ready
leaf tables reference those definitions. The full IMBA pool and TMT remain gated.

## Evidence and limits

Stage: `build/verification/imba-utility-2026-11-04/`.
Baseline protects 275 author paths and the installed `a47f6a1e` archive.
The author RP and deleted templates are untouched. No physical-client, large-map
peak-TPS or arbitrary network-latency claim is made. Qualification is complete.

The first runtime candidate stopped in the fixture's disposable lethal witness:
a nested native `/gamemode` command is queued until its enclosing command ends.
The fixture now waits for the next native tick before asserting enrollment and
using the lethal die. Its owned UUID's player files are included in the wrapper's
exact snapshot set. The next run reached the complete Bastion comparison and
exposed another fixture mismatch: raw legacy NBT uses `Name`, whereas 26.3
`NbtUtils.readBlockState` reads the data-fixed `id`. The independent raw source
oracle now handles its exact BEDROCK/AIR palette explicitly and also compares all
prepared cells before checking the published structure. Production template
decoding already passed through StructureTemplateManager's data fixing.

The next run passed the complete utility suite. Its replay check encountered
ordinary grass decay directly beneath the genuinely placed opaque coarse-dirt
block. The independent terrain oracle now allows that single covered cell to
become dirt; every other cell remains exact. It also waits for the restored
White Arrow to finish before asserting completion. This changes only the check,
not Dirter's source placement or world random ticks. Rejected candidate logs
remain in the verification stage.

Next independently verified gap: the **Ruchka pen**, source
`lbc:items/swords/ruchka` and `lbc:projectiles/gadgets/ruchka*`.
The original normal lucky table reaches its canonical definition, but Java has
no consumer. It has both melee and thrown routes, 600 Twitching, source VFX and
TECH radius 20 checks. Reuse the existing Psycho status owner and actual hit
ownership; do not confuse it with the utility items completed here. This audit
did not implement or expose Ruchka. TMT remains last. Search all datapack
namespaces (not only `lbc`) when tracing any future item dependency.

## Qualified result

Exact test SHA256: `368d1b4c8fb31be095f396254657786d475e0702a7c561e18ae02cca7d0a55a1`.
Production SHA256: `a26c4aad6be87d29e23e6fea124c4700d35c358ae29e385d6a73434fdc191a7d`.
All12 groups in12 isolated native matches passed on this archive (8mode1,
4mode3): utility input and whole replay in both modes, plus teleport gadgets,
God Hedgehog, Cunny input/replay, clone skills, Vanity rollback and BedWars
protection/lifecycle. Child shutdown was normal; controlled file snapshots
were restored exactly and no fixture locks or cleanup errors remained.

18 owned Java families and13 resource inputs were overlaid onto the previously
qualified archives. Every archive entry was verified;3168 retained classes
were checked for references to removed members.10039 other input hashes and
all275 protected author paths stayed unchanged. Both build/libs archives and
the main test JAR were updated with the normal stopped-server installer;
rollback is the previous `a47f6a1e` archive. No main stop/start or push occurred.

Ready tables: `lbc:ported/teleporter_imba`, `lbc:ported/dirter`,
`lbc:ported/bedrock_bastion`, `lbc:ported/god_dice`, `lbc:ported/white_arrow`.
The direct source God Dice/White Arrow leaves are also admitted. Their source
counts/functions/components remain canonical. The full IMBA root is not
enabled, and no TMT or game-mode/skill/broad rewind release gate changed.
Existing RP models were checked read-only; no RP installation is required.

Machine evidence: `build/verification/imba-utility-2026-11-04/qualification.json`,
`main-install.json`, `package-proof.json`, `linkage-proof.json`,
`exact-templates.json`, `canonical-definition-proof.json` and three final
`368d1b4c-*-results.json` files. Local scoped commit proof is `local-commit.json`.
