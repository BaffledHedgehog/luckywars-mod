# Eternal Wealth — 3 November 2026

## Final author rules

Greedstone intake, creation/spread of curse fields and cursed-land damage are
suppressed by the antimagical beacon and enhanced artifact. TECH does not suppress
them. Diamond placement, approach, cleansing and rewards are unjammable.
Cursed-land damage remains WILD, including the owner and allied mobs; positive
void resistance and active Greed retain their source exceptions.

Author clarification after the initial ownership question:

- Remove `greed_cursed` from **all** players within 10 blocks when a cursed player
  touches the diamond. Give a Divider to **each** cursed player being cleansed.
- Each uncursed touching player receives the poor reward. A diamond can produce
  both outcomes; rewards are per player, rather than one reward per group.
- Remove fields only for cursed owners directly touching the diamond (source
  distance 2). Nearby cleansed players' fields remain. This asymmetry is intentional.
- Replace fluids on both additional Y layers too; their omission was a source typo.

## Native implementation and source fidelity

[LbcEternalWealth](../src/main/java/me/skywars2/game/LbcEternalWealth.java) owns the
functional native markers, captured causal owners, intake and terrain operations.
[LbcEternalWealthItems](../src/main/java/me/skywars2/game/LbcEternalWealthItems.java)
registers Polymer identities for both source drop variants, Greedstone, and an own
server block for cursed terrain. Raw mushrooms on imported maps stay ordinary.
The curse projects the source all-false red mushroom state without an RP override.

The ordinary source token keeps its yellow name, white lore, `tier_q` and existing
`lbc:drop_not_trap` model. The imba entry deliberately keeps its source asymmetry:
command-block carrier and no added name, lore, tier or custom model. The two
canonical `item_defs` only change the server item identity; all source functions
and counts remain byte-independent JSON equivalents. New test leaves:
`lbc:greedcurse`, `lbc:ported/greedcurse_drop`,
`lbc:ported/greedcurse_drop_imba`, `lbc:ported/greedstone`.
No broad root loot pool was enabled or reweighted.

The source drop immediately places gold plus an invisible glowing frame containing
Greedstone. Intake removes one stone, grants prestige, shows the original subtitle
and creates a pending diamond at source absolute X/Z −130..130, Y 16..300. Protected
Inventory requires the stone in the main hand. Parasite rejects the harmful curse.
Diamond placement retains the 20-active-tick delay, exact original structure bytes
and embedded marker offset, using asynchronously prepared resident chunks.

Curse seeds are created once per active second outside diamond protection radius20.
They mature after180 active ticks. The source mask is the637-cell union of the7³
cube and three axis extensions, without extended corners. Solids are processed
before the source fluid tag. Active contact checks preserve the eight source
probes/radius5.5 and native1.5 out-of-world damage with ordinary hurt cooldown,
full damage callbacks and captured owner credit. This is not direct HP subtraction.
Antimagic is checked both at each source field and its target.

Diamond terrain branches run once each in source order, poor then rich, while rewards
are individual. Native Divider/feather leaves preserve Polymer identity. Source
sounds, particles, advancement and strict structure/terrain placement remain.
Cleanup deduplicates own cells and protects overlaps still covered by foreign fields.

Source evidence: legacy `data/lbc/function/other/greed/*.mcfunction`,
`function/drops/greedcurse.mcfunction`, both original item definitions and
`structure/avarice_diamond.nbt`. `source-proof.json` records hashes and checks exact
template bytes, unchanged item modifiers and the source mask. Existing RP keys/model
were checked read-only; no RP or template changes are required for this batch.

## Lifecycle and work cost

Markers carry only compact kind/age/owner tags for native load adoption. Runtime
state is typed. Inventory detection is event-dirtied with native identity fast paths
and fixed `CustomData.matchedBy` fallback; there is no per-tick NBT copying or IO.
Seed/field clocks pause in TimeStop. Match stop discards the owned actors.

Damage groups active fields by16³ section, queries living targets once per occupied
section and computes contact probes once per candidate. Empty service ticks perform
no world/entity search. No invented gameplay cap was added to field count or damage.
Large numbers of live fields still cost work; peakTPS has not been measured.

The resident checkpoint records exact functional field/diamond kind, age, owner and
UUID alongside canonical native bodies. Full capture waits for pending diamond
construction to finish. Restore rebinds resident bodies before publication and never
replays intake, rewards or placement. The actual full coordinator regression now
contains a nonempty curse field through repeated restores and point replacement.
This scoped regression does not change the pre-existing broad full-rewind gate,
Protected Inventory release gate or the remaining Nexus chain. TMT stays last.

## Exact qualification and installation

Evidence directory: `build/verification/eternal-wealth-2026-11-03/`.

- Fresh successful offline `compileJava`; exact qualified prior Loom resources
  with11 fresh Java class families,8 explicit JSON changes and the original NBT.
  No partial `processResources` output is used.9856 other source inputs and all272
  pre-existing author paths are guarded; all ZIP entries are read back and verified.
- Test JAR: `064f05b869917ce5a78fe7fb19142cbda7663d2e4ed80ccfedac35cdcf75ae40`.
  Production: `85592a45e2a4a038476a56c1518f34389f1d9ba2c7030e5f7d8d5a1e8f7e4b17`.
  MapAuthoring/MapDeletion class families remain absent from production.
- Five actual mode1 matches on this exact test JAR: `eternal-wealth`,
  `vanity-rollback`, `parasite`, `inventory-skills`, `dimension-items`.
  The new group verifies real airborne source dispatch/frame, protected inventory,
  MAG/TECH intake, actual source clocks, both fluid layers, WILD owner/ally damage,
  harmless raw mushrooms, target-only suppression, cold actor adoption, nonempty
  typed checkpoint, owner-only overlap cleanup and per-player rich/poor rewards.
- Earlier rejected candidates are retained separately: the first test found a real
  missing explicit MAG intake guard; the next found an exact-boundary assumption in
  the fixture. Both were corrected before final qualification. No failed candidate
  was installed. Native fake-player bootstrap retains its known Polymer missing
  packet-context warning; no final check failure or cleanup error remained.
- Fixture port25577 only, normal child shutdown, exact controlled snapshots/config/
  level/tickets/gallery/OP restoration and no retained locks. No physical-client
  rendering/audio, arbitrary-map or peakTPS qualification is claimed.
- Both final `build/libs` JARs were published. Normal-stop installer updated the main
  JAR with rollback `506b57d4`, receipt `main-install.json`, `restarted:false`.
  The main server was never stopped or started by this work.
