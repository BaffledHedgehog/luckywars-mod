# Source wool trails and Bucket — 5 November 2026

Status: qualified and installed; no main stop/start or restart performed.

## Rules and scope

The author answered the jammer and pickup clarification:

> цветные мосты ТЕХ, ведро ничем (ну да защита инвентаря защищает)
> кста проверь - ведро если че не просто шлем заменяет - старый шлем при этом выкидывается на землю (и его игрок можнт обратно подобрать сразу)

The sixteen colors now use TECH20 for launch and active generation. Ordinary
green Most keeps its existing TECH20 rule. Bucket is unjammed. Protected
Inventory blocks its helmet exchange; normal native5 generic damage still
follows damage protections. The original helmet is actually dropped on the
ground with pickup delay0, overriding the old source40 delay. Native item pickup
owns retrieval; the runtime does not insert a replacement directly into inventory.

The prior author instruction protects foreign inventory from destructive changes;
the new payload uses the same existing Protected Inventory authority. TMT remains
last. This batch neither enables the whole IMBA root nor adds an interpreter for
arbitrary datapack functions. The unused `tntmost` command has no canonical item
acquisition and is not invented as a public reward.

## Source and implementation

Read-only legacy root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

* Wool: `lbc:imba/wool` references sixteen canonical color definitions. Each
  gives16 snowballs; the unchanged group rolls64 times, totaling1024 items.
  `lbc:projectiles/gadgets/bridgebuilders` → `wools/select` fills actual
  AIR only, at floor coordinates X/Z±0.5 and Y−2.5. Source color priority is
  lexical black..yellow. All colors use the existing Most bridge engine in
  `LbcBalanceExtras`, which already runs in ordinary matches before the old-mode
  supplier gate. Color, original creator and cause order are decoded once.
  Four-cell writes use the shared terrain journal, resident checks and BedWars
  role/type protection. No second bridge engine, background scan or tick NBT
  parsing is added. Global item admission uses existing scalar `ItemData` reads.
* Bucket: `lbc:imba/bucket_rofl`, `projectiles/gadgets/bucket`, `bucket_2`,
  `bucket_pl` and `drop_helmet`. The existing thrown-utility Snowball owner
  handles source hostile-player acquisition4.5 around projectile position+Y1,
  actual hand debit, original author and once-only payload claim. Exact head
  equipment resolves through `lbc:imba/bucket_head`: leather helmet carrier,
  source200 durability, armor3, model `lbc:bucket_reversed`, asset `lbc:bucket`.
  The occupied helmet is independently copied with all nested components and
  creator provenance, published with immediate pickup, then its exact slot is replaced.
  Protected Inventory preserves that slot. Native generic5 damage uses the
  actual projectile and caster through the existing damage hooks.
* Source `execute ... at @a` changes the function position to the selected target.
  All ten `lbcsounds.bell` MASTER10/pitch1 packets and their radius5 audience
  therefore use target feet. The same trace corrects secondary Ruchka/Honew
  sound and particle origins; first Honew sounds remain at target+Y1.

The source Bucket helmet wrapper still had ignored old `functions`/`function`
codec keys. Only its four static native loot modifiers are migrated to
`modifier`/`type`. A reversible JSON proof preserves every value, count,
condition, component and modifier. No other old function payload is activated.

Native Polymer wool items use the sixteen canonical definitions and ordinary
Snowball use, retaining source aim, actual item-used stats, counts and debit.
Bucket reuses the existing native thrown item class. Shared palette holders,
ordinary Most and Bucket projectiles retain native TimeStop and canonical clone
behavior; the existing typed coordinator images preserve accepted native bodies.
Creator attachment registration runs during mod bootstrap before world decoding.

Ready leaves are `lbc:ported/wool_trail_<color>`,
`lbc:ported/bucket_projectile`, `lbc:ported/wool_trails` and
`lbc:ported/bucket_head`. These reference source definitions, rather than copying
presentation. Source models and the Bucket equipment asset already exist in the
author RP; it remains read-only and no template is restored.

## Verification

Stage: `build/verification/wool-bucket-2026-11-05/`.
Baseline main02284b2a and local HEADe2cf1f1d;275 author paths protected.
Candidate997140ac failed the exact helmet check because old wrapper modifiers
were ignored, and was also rejected by source review for sound position.
Candidate7916edc8 passed complete partial replay; its main scene rejected a stale
chunk-holder cache entry during test setup. The fixture now validates current
`getChunkNow` residency, preflights ordinary cells and leaves the two absent
boundary cells untouched. The no-load assertion is retained. Rejected artifacts,
logs and normal exact fixture cleanup remain in the stage.

Candidate dbca05ac passed the complete mode1 plan and the main mode3 scene. Its
last replay receipt counted every new ItemEntity in the level; a native BedWars
copper generator can legitimately publish unrelated items during that window.
The receipt now identifies only the target-local old-helmet or bucket-helmet
outputs, so a repeated bucket still fails. An explicit unrelated copper witness
must survive untouched. Exact damage, ten-bell and once-only assertions remain,
with separate diagnostics. No product behavior was changed for this fixture fix.

Candidate109b729c passed all10 scenes with the source defaults. Before release,
the author confirmed TECH for colors and immediate old-helmet pickup. That
candidate was superseded without installation. The final checks additionally
cover TECH launch rejection and active pause/resume, MAG acceptance, Bucket
inside both fields and real native item pickup with exact helmet conservation.

No physical-client, audio-perception, peak-TPS or disk-restart persistence claim
is made. New game-mode/skill/public rewind release gates remain unchanged.

## Qualified result

Test SHA256: `a4d09eb0b1834ef76c4dfca5acc6497756c9dcc21bb6d612ca82eeb184ba8d6d`.
Production SHA256: `638e6859c005576f45f4632096e04cf1604b3a07b5812c816ec94d877af767ed`.
All10 groups passed on the exact archive in10 isolated native matches: the full
palette/Bucket scene and partial native whole replay in both modes; existing
Ruchka/Honew/Pipe input/replay, clone skills, Vanity rollback and BedWars
protection/lifecycle. Every child stopped normally, controlled snapshots restored
exactly, and no retained fixture locks or cleanup errors remained.

The scene checks every canonical color and native item-use/stat/debit, original
64-by16 group count without granting it, selected AIR-only source geometry at
resident positive/negative coordinates, unchanged solid neighbors and an actual
two-resident/two-absent chunk boundary without loading its absent neighbor.
Native scheduler flight, TimeStop, causal eight-body x2*x4 composition and pending
colored/Bucket actor continuation are exercised. The same native actor identities,
typed color/cause, original inventory and world cells restore together. Bucket
checks exact old-helmet nested components/creator/pickup0, source armor/model/
durability, immediate native old-helmet pickup, normal native damage attribution, target-position bell packets and
outside-radius audience exclusion. No physical-client input claim is made.

9 owned Java families and37 resource inputs were overlaid onto the qualified
thrown-utility archives. Every ZIP entry and 3201 retained class references
were checked; 10051 other inputs and all275 author paths stayed unchanged.
Both build/libs archives and main were installed normally; rollback02284b2a.
The nineteen ready leaves include16 colors, Bucket, its helmet and the group;
they retain shared source definitions. The author confirmed TECH for colors,
unjammed Bucket and immediate old-helmet pickup. No RP files were
changed and no release gate was promoted.

Evidence directory: `build/verification/wool-bucket-2026-11-05/`:
`qualification.json`, `main-install.json`, `package-proof.json`,
`linkage-proof.json`, `canonical-definition-proof.json`,
`bucket-head-codec-proof.json`, three final `a4d09eb0-*-results.json` files and
scoped `local-commit.json`.
