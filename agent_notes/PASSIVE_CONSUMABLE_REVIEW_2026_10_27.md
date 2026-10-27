# Canned Food and Hylian independent review — 27 October 2026

Read-only production/resource review after the parent's integration-ready
message, at its reported baseline `f6285db`. This reviewer wrote this new note
only: no production, fixture, RP, build, server or Git changes. No runtime
qualification is claimed by this review.

## Decision and acquisition boundary

No unresolved material source/integration defect was found in the frozen
consumer, Hylian passive, shared scheduling hooks or scoped leaves fingerprinted
below. Build and actual native qualification remain the parent's responsibility.

**Canned Food normal-play acquisition remains unfinished.** Its complete
consumer and exact operator/staging leaf do not establish the missing LuckyWars
producer or justify enabling `lucky_items`. The build whitelist adds only the
two narrow `ported` leaves and Hylian's exact monster-tag dependency; the
unresolved whole LuckyBlock pools remain closed.
Hylian retains the existing packaged `transformables_sword` entry and reforge
route, whose actual current roll/passive qualification is a separate native
fixture obligation.

Read contracts: [Canned source](CANNED_FOOD_SOURCE_2026_10_27.md),
[acquisition correction](CANNED_FOOD_ACQUISITION_2026_10_27.md),
[native timing](CANNED_FOOD_TIMING_2026_10_27.md), and
[Hylian shortlist/source](REMAINING_SMALL_PASSIVES_2026_10_27.md). The original
`other/konserva.mcfunction` and `players_5t.mcfunction` Hylian line were also read
directly from the disabled author datapack.

## Canned Food

The registered singleton is invoked by the existing Inventory Hazards
40-active-tick phase before either Liquid Abyss probe. The effective order is
Canned container, Canned hands, Liquid container, Liquid hands. Each Canned
condition reads current physical hunger and current matching inventory scope;
the first broad clear can remove the second probe's selected-hand trigger.
Physical FoodData admission is intentional: the existing `foodlvl` compatibility
objective is a DUMMY ledger, and this patch does not pretend it supplies a live
FOOD-criterion read or reproduce the historical criterion's possible phase lag.

Container activation scans slots0..35; hands activate separately. Armor,
crafting, cursor, body/saddle and nested contents do not independently activate
the consumer. Once admitted, native clear-one uses its ordinary broader physical
inventory/equipment, crafting and cursor scope. Extra custom data is accepted
through the native partial predicate; only top-level `konserva:1` matches. The
existing shared `food_count` increments after clear, without conditioning on
effect acceptance. No skill2, legacy, mana, beacon, magic-suppression or active
stun/ice admission condition was added.

The source payload order is preserved: visible Saturation amp100/duration1;
seven ordered item-particle calls, count2 each; ranged MASTER burp; one uniform
11-way draw through `LbcTraveller.buff`; native clear1; shared counter+1. The
shared buff mapping was compared with all eleven original source outcomes and
matches durations, amplitudes and invisibility's hidden presentation. Instant
Health and Saturation are native one-tick effect grants, without direct healing
or feeding. Equal same-amplifier grants therefore retain ordinary layer/merge
semantics, rather than guaranteeing two feed/heal applications before a native
living update. Consumed items do not become persistent material outputs.

Particle position uses current native eye position plus look direction0.3;
the seven vanilla item payloads, spread0.1 and speed0 match source. Cached26.3
`ServerLevel.java:1314-1325` confirms the selected overload delegates with
`overrideLimiter=false, alwaysShow=false`. Lines1480-1492 require the same level
and strict32-block viewer block-center distance. This is the source NORMAL
audience. `LbcOverload.sound` independently supplies volume1/pitch1/minVolume0
MASTER burp with the native16-block sound range. No common global or32-block
sound audience was substituted.

Common owner/map/living/observer/technical-role/ENDING and authoritative Time
Stop conditions apply. The consumer is stateless; its clock belongs to the
existing service and is reset there. No extra lifecycle cache or cleanup is
needed. Actual effect updates, criterion timing, packet boundaries and native
40-tick scheduling still require the parent's tests.

## Hylian and existing melee integration

The parent subsequently found a material packaging omission that this initial
review did not catch: the raw `data/minecraft/tags/entity_type/monsters.json`
was absent from the prior artifact and explicit resource whitelist. The parent
added that exact path to the whitelist without editing the tag. Independent
follow-up confirmed the raw/native tag is byte-identical to the author source,
contains44 unique IDs including Bee, and all44 resolve through cached26.3
`EntityTypeIds` keys actually registered by `EntityTypes.register`. Unqualified
IDs use the normal minecraft namespace. The prebuild JAR still lacked the tag
when inspected; qualification must verify the exact tag in the newly built
artifact and actual runtime tag membership. The source/package fix is reviewed,
but that artifact/runtime boundary is not inferred from a raw resource file.

`LbcClassicMelee.tick` is registered once in `LbcItems.tick`, with its five-tick
phase reset in `LbcItems.stop`. The passive inspects only mainhand partial
`hylian:1` custom data and actual `#minecraft:monsters` entity-type membership,
using entity-position squared distance<=49. Monster witnesses are presence
conditions, including own/allied monsters; they are not new damage targets.
The holder retains ordinary role/admission protections.

The existing magic-beacon predicate supplies the source `stopper_magic`
20-block boundary and its established native beacon representation. Mechanism
beacons, arbitrary nomagic status and broader owner/eye magic suppression are
not new Hylian gates. The passive bypasses the accepted-hit service's
`actionsBlocked` admission, so equipped operation continues under input stun or
ice while authoritative Time Stop still pauses it. Strength40ticks amp2 is a
visible self-owned native grant; changing hand or losing the witness stops
refresh and leaves ordinary effect expiry. No sound, charge, RNG, mana,
consumption, durability or task was added.

The existing `damaged` method remains independently admitted and retains its
Feeding/Vampire/Sea Guardian ordering and magic checks. The new periodic path
does not call that accepted-hit path or change incoming damage sources. Shared
hooks add singleton/scheduler/check dispatch only; the test fixture itself was
still under separate ownership and was not qualified by this reviewer.

## Independent leaf comparison

A read-only JSON comparison independently extracted original lucky_items
entry74 and transformables_sword entry1, normalized only the26.3
`functions -> modifier` and `function -> type` keys, then compared each complete
entry with its new scoped resource. Both comparisons returned true. This
verifies payload/presentation equality, not normal acquisition or original pool
probabilities of a single-entry staging table. The original full Hylian sword
table retains its prior source-audit hash.

Canned Food remains count16 vanilla repeating_command_block, modelkonserva,
tierD, original translated name/two lore rows and `konserva:1` only. Hylian
remains count1 vanilla netherite sword with its original model/style/lore and
transformable/sword/hylian flags; vanilla sword attributes are not replaced.
Neither requires an RP edit.

## Frozen SHA-256 fingerprints

Paths are relative to `E:/servers_mew/skywars2_last/luckywars-mod`. Mutable test
files, runtime receipts and later release documentation are outside this
production snapshot. After the parent normalized the two owned Java files to
LF, reconstructing CRLF from each current file reproduced its exact earlier
reviewed hash. Only line endings changed; the two fingerprints below reflect
the final LF bytes.

| Path | SHA-256 |
| --- | --- |
| `src/main/java/me/skywars2/game/LbcCannedFood.java` | `00e04b1995a20d198f189019e5024a703e20315cf3d479bd7b8bddc124c9f852` |
| `src/main/java/me/skywars2/game/LbcClassicMelee.java` | `e964bcf568babd8b9ed31d4dd1ca53dec1af6a4bfeab53f196a4193f230a7c66` |
| `src/main/java/me/skywars2/game/LbcItems.java` | `fee9a703beaccf2e4f0df0c2470bdba53055755bc096ac1270ac2a9d24f56184` |
| `src/main/java/me/skywars2/game/LbcInventoryHazards.java` | `6987ebf06276f065f6751e659277a2323887d73025174a042f56a0697ea96998` |
| `build.gradle` | `d1a555260fbe56b8e1abbcd8a7ce9849046d652fe81fbde38623d1b859e39da2` |
| `src/main/resources/data/lbc/loot_table/ported/canned_food.json` | `d485333901072bd5f0757ac0e6d05ce06865de01618608ec075a5c52a7cbb913` |
| `src/main/resources/data/lbc/loot_table/ported/hylian_sword.json` | `e4a99b6a09c3e27f72b866ce1ef8573a6b62618757cd090a8b9fad930ba05bb8` |
| `src/main/resources/data/lbc/loot_table/transformables_sword.json` | `1191ded3ba88eb63f559c8a5b8b2fff9bf7b32eb8accab6b483af7b64c5727be` |
| `src/main/resources/data/minecraft/tags/entity_type/monsters.json` | `561747ac78e9454224b1d394d815eff5937493aee1a4f049a15b659b732af243` |

No physical client pixels/audio, latency, peakTPS or complete Canned Food
normal-play acquisition claim follows from this review.
