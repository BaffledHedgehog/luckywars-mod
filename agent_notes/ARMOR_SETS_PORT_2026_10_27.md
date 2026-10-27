# Paladin, Shitty and Erosion armor ? 27 October 2026

User requests continued migration in cohesive batches. This batch implements
all twelve pieces of three existing source sets, including their complete worn
abilities. It does not replace them with new registered items or change original
loot, rarity, attributes, enchantments, model, name, lore or crafting flags.

## Complete source behavior

[Bounded source/resource audit](NEXT_ENCOUNTER_SOURCE_2026_10_27.md) records exact
legacy dispatchers, helpers and all15 source-equivalent native roots/slot leaves; three additional source-exact
`*_full` tables supply complete outfits.

Paladin: four worn `paladin:1` pieces grant60ticks of Resistance amplifier3
(levelIV) every20active ticks. Partial outfits and backpack copies do not count.
No source Magic Stopper gate or mana fee is added. The previous ordinary effect
expires naturally when the complete set is removed.

Shitty: worn `gownoleggings:1` count n controls a once-per20active-tick aura.
Enabled Gownoman skill9 doubles n. Poison and Hunger last60ticks at amplifier0,
range0.01..10n; n>=4 also gives Wither amplifier3 within4(n-3). Effective counts
1,2,3,4,6,8 are reachable from four worn slots; source5/7 branches are naturally
unreachable. Native team/role/Greed policies protect allied players and technical
actors; friendly mobs remain eligible under the established player projectile
policy. Effects receive the actual wearer as source for shared later-damage and
hunger-debt attribution. Existing `govnoq`/`craftusable` craft and Fartus admission
remain untouched. Sneeze count n, speed0, normal-distance delivery and exact
source spread are preserved:16for n1/2, otherwise8n, at ownerY+1.

Erosion: every5active ticks, a partial outfit reduces Protection by one on each
worn Erosion piece. Four pieces instead choose exactly one slot uniformly in
source FEET/LEGS/CHEST/HEAD order; the choice may hit an exhausted zero-level
piece and do nothing. Native enchantment mutation floors at zero/removes the
entry and preserves other enchantments and stack metadata. Initial Protection
100/180/255/120, durability9000 and all source stats remain. No separate saved
stack or inventory-wide scan exists, so swaps cannot mutate an old item copy.
The user-approved full-int enchantment range is unchanged.

## Runtime and acquisition

`LbcArmorSets` shares one5/20active-tick phase and reads four worn slots only
when due. No per-wearer clock, task, actor, inventory cache, timer HUD or persistent
modifier is introduced. Time Stop pauses work; equipped passives continue through
ordinary stun/ice. Ordinary-item dimension, match-role and epoch admission reuse
existing policy. Stop resets only its phase/epoch; normal effect/match lifecycle
owns granted effects. No source Magic Stopper is invented for these passives.

Three original reforge rules already target `lbc:neko_set_paladin`,
`lbc:neko_set_shitty` and `lbc:neko_set_erosion`, each selecting one of four pieces,
not a whole outfit. This patch exposes those three roots and their twelve exact
slot leaves plus three complete-outfit `*_full` tables in the ready loot catalog
(18entries). The three full tables were present in source but absent from the
packaged JAR; their original four independent slot pools are now included.
Broad unresolved armor pools remain as
before; no weights, raw Lucky Block distribution or fake recipe are added.
The actual author RP already has all models/equipment assets and RU/EN strings,
so this batch needs no resource-pack additions. The RP remains read-only.

## Qualification

Final native and exact-artifact evidence is recorded after completion at
`build/verification/armor-sets/qualification.json` and
`build/verification/armor-sets-gameplay-results.json`. Checks cover source leaves,
real root/reforge acquisition, worn and removed gear, reachable aura bands/skill,
teams/Greed/technical roles, native later-damage attribution, all four random
Erosion choices, zero and metadata preservation, actual5/20tick scheduling,
Time Stop/stun/dimension guards and shared item regressions. Shader/client pixels,
all armor families, the whole Lucky Block pool and peakTPS are not claimed.

The fixture waits for actual native entity-chunk readiness before publishing its
aura targets, rather than treating synchronous block-chunk loading as entity
query readiness. It establishes its live cadence after the first completed END
tick. Combat assertions are performed once, never retried to mask a failure.

Final build `23a25d43` passed 24 results in two actual mode1 matches
(90.57 seconds), including nine real native normal-reforge selections,
all18single/full acquisition tables and the actual5/20tick scheduler. Exact
artifact comparison matched520Java sources,1473classes,3224resources. All175
protected author paths and31live overrides remained exact. Fixture shutdown and
snapshot restoration passed. No graphical-client or peakTPS proof is claimed.
Automatic installation targets the existing8d2f7513main baseline after normal
shutdown; the source RP is unchanged and needs no additions for this batch.

Installer PID18284 installed23a25d43 after normal main shutdown and world unlock,
without restart. The installed JAR hash matches the qualified artifact. Retain
8d2f7513immediate rollback and6890ae59fallback. Obsoletec260a27darchive was removed
only after checking its exact hash/path and verifying the new main and rollback.
