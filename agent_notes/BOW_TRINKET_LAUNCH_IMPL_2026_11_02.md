Final author answers and qualification supersede this provisional handoff: [closure](BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md).

# Bow trinket launch owners ? 2026-11-02

Implementation prepared; no build, native test, server, git, RP or PENDING mutations performed by this worker. Root owns final admission policy, questions, integration and verification. Files frozen after this note.

## Owned files and APIs

- `LbcKfcRewards.java`: `public ItemEntity launchTrinketBomb(ServerPlayer)` returns the real native free item (or null); package `bombSnapshot()` and `bombCount()` expose read-only tracked publications for checks. Existing player Cultus bat spawn and hostile loose spawn now share `bombStack(variant,styled)` with the trinket. Existing Cultus presentation remains unchanged; trinket and hostile loose stacks retain max-stack-size1/tier_ff styling.
- `LbcHoming.java`: `public boolean launchFourier(ServerPlayer)` accepts a typed virtual slowraycast effect without mana payment or wand passives. `fourierSnapshot()` yields immutable owner/epoch/position/direction/age views. Existing Homing tick, full checkpoint, cloneNearby, origin transform, stop and owner retirement include this list. No new entity scan, command engine, generic scheduler or shared-file edit.

## Bomb source and behavior

Read-only source: `data/lbc/function/items/trinket_effects/charge_penis.mcfunction` and `raycast_vpered_slow.mcfunction` in the disabled legacy LBC datapack. Uniform six variants preserve exact models, translated name, instantitem/penis_bomb/penis_num data and variant2 govnoq. Existing CultItem constructor provides invulnerability, Health10000, Age-32768 and PickupDelay3. Native velocity is floor(lookComponent*10000)*0.0002, reproducing command numeric conversion including negative components. Native item pickup and tracked mob contact remain the existing bomb/inventory-mine pipeline; ownership, independent item publication, clone image and retirement remain in the existing Bomb map.

Source summons at caller eyes; only sound used erroneous Y+10. Root supplied explicit author authorization to correct discovered legacy sound positions, so all six authored sounds emit at the actual eyes launch position (volume0.5/pitch1/master through existing sound helper). No bat is created for the bow launch.

`TimeStopNativeMixin` already cancels resident native nonpassenger/passenger ticks using `timeStop().frozen(entity)`, so free player CultItems inherit native physics/age/pickup pause. The existing additional hostile-only CultItem pause branch remains unchanged.

## Fourier source and behavior

Read-only source graph: `items/trinket_effects/fourier_11`, `homing_evoker/tick`, `homing_evoker/find_pos`, `tick_not_timestopped`, `raycastselector`, `raycastselector_player`, and `lbcr:dmgwithid_un_playeronly`.

The slowraycast dispatcher runs each unpaused tick, increments Lifetime first, emits a fang before steering, then kills at Lifetime>=130. This implementation emits on the final pass and removes after its 130th active pass. Caster feet within2 suppress fang emission; nearest eligible source `minecraft:mobs` within40 supplies steering. Shared projectileTarget protects caster/allied players while retaining friendly mobs per root instruction. No target means no movement.

The source steering command creates a lookahead of prior forward + 0.1*directionToTargetFeet, normalizes that direction, then advances1; implementation preserves this rather than snapping directly onto target.

**Suspicious source surface formula reported to root, pending author resolution:** source temporarily positions at motion_blocking height+1000 and requires distance<=1007 before returning to surface. Exact condition is `(surfaceY+1000-markerY)^2 <= 1007^2`, not abs(surfaceY-markerY)<=7. Implementation retains that exact inequality and falls back to current marker position otherwise. No silent normalization.

Fangs reuse `evokerBass().spawnOwned`, preserving real native caster, epoch, guarded native attack tick, vanilla fang clock and TimeStop behavior. Provisional launched-effect magic suppression follows existing Homing resident position/segment policy. Root owns remaining launch suppression questions; helper currently requires valid owner only and performs no launch magic check. No author policy inferred from absent local function guard.

## Verification boundary

Source review and narrow textual review only. No claim of compilation or runtime success. Root/checks worker were sent stable snapshot APIs for exact native fixture assertions. Source terrain oddity and pending launch policy must be resolved before release qualification. Existing whole-rewind framework owns native CultItem/Bomb state as before; Fourier direction/remaining/cloneMask/owner/position are explicitly included in Homing typed checkpoint restoration.
