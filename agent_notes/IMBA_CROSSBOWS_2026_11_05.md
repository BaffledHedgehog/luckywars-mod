# Native IMBA crossbows — 5 November 2026

The author explicitly confirmed: all five special variants are suppressed by
TECH and the strong artifact; ordinary MAG does not block them. The sixth item
is an ordinary native crossbow with source Quick Charge V, without another
special attack or new suppression rule.

## Source and implementation

Read-only legacy source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
The source paths are `lbc/function/items/crossbows`, the five
`lbc/function/items/crossbow/imba_*` inputs, their two `using_item`
advancements, `raycast_vpered_big_with_razbros`, `raycast_vpered_razbros`,
`rand_motion_razbros_small`, `universal_razbros_withpre[_small]`, and Erecto's
`click_upgraded_2_mode_sniper_bruh[_2/_3]` and `_charge_bruh`.
`lbcr/function/damagewithid_extreme` supplies20 arrow damage. Crucially,
`minecraft/tags/damage_type/bypasses_cooldown.json` already contains `arrow`
in the actual legacy pack. It is not the dungeon boss's ordinary10-tick damage.

Six native Polymer CrossbowItem identities bootstrap from their existing
canonical item definitions, including all names, source lore, tiers, enchantment
levels and other components. Only the obsolete special-arrow prohibition is
removed, following the author's earlier instruction to support custom ammo on
all bows/crossbows. Canonical definitions now reference the native item; their
counts/functions/conditions remain intact. The ready leaves reference those
same definitions, without a second definition or RP asset.

`LbcImbaCrossbows` shares native successful-volley hooks and the existing
`LbcInfiniteAmmo.drawReload`/held-first selection used by the minigun crossbow.
Fast uses one selected arrow per active held-use tick, speed7 and per-axis0.4
spread. Gatlingator uses three, preserving the source last partial stack. Only
the selected infinite arrow is free, with native intangible pickup behavior;
another inventory flag cannot make unrelated ammunition free. Native charge,
durability and arbitrary weapon/ammo components remain on the actual item.

Shotgun emits256 unpickable source pellets at speed5 with independent per-axis
0.5 spread for each accepted native arrow. Their actual native ammo, damage,
critical/fire/piercing and other flags are preserved. The final ballistic family
then passes through existing special-bow and trinket augmentation; the latter
rolls once per volley, without replacing unrelated ammo/weapon properties.

Sniper, Super Shotgun and Gatlingator reuse `LbcErecto` on the same `LbcWands`
trace set: respectively1×200ticks,32×50ticks,4×10/20/30/40ticks. The source
ordinary sniper starts at feet and recoils by15 degrees; Gatlingator starts
one block beyond the eyes. Spread remains30/30 degrees for Super and30/5 for
Gatlingator. Source pink dust, trident sound, flash and18-block/0.3-step
progression are retained. Mechanical impacts use native radius20 blasts and
the shared TECH explosion protection. The ordinary wand sniper remains separate
inside that same helper, preserving its prior impact behavior.

The shared damage helper admits exact native body/line intersections, then
computes source unit-cell multiplicity; it does not widen target admission.
Each base damage/payload consumer queries once per segment, rather than once
per source particle sample. Existing quiver/trinket handlers still own their
specific effects. Detached native ammo contexts retain component-complete
effects; trace cloning copies their contacts and state without NBT per tick.
Source firework behavior is retained: replacement retires only the accepted
arrows. A native firework survives alongside the source beam/pellet attack.

TimeStop pauses the shared traces and denies frozen input. Physical arrows
retain their existing native clone/replay lifecycle; short electrical traces
cancel on full rewind under the author's previously accepted common rule.
No separate ray engine, polling advancement executor, nearest-arrow selector,
fake caster, new resource-pack build or arbitrary function interpreter is added.

## Exact-archive qualification

Test archive `7aad4511250080080dc9d4f33bd5c6877f8819a2732d37c347e3a76f45ed16a2`; production `a9320ff6a4080b5791d48bf7eda5292e6a68d2dd1d5b477dddf1476e425e444c`.
Six isolated native matches passed on this exact test archive: IMBA crossbows
in modes1 and3, plus existing minigun-crossbow, bow-trinkets, special-bows and
Erecto in mode1. The new scenario exercises actual native use/draw/volley hooks,
all six loot/give prototypes and Polymer roundtrips,256 pellets, source beam
counts/lifetimes/origins, seven20-HP source-cell contacts on a cow body, retained
poison payload, TimeStop, selected infinite ammo, paid custom ammo, continuous
use, firework preservation and real TECH/strong-artifact input suppression.

All child servers shut down normally and exact fixture snapshots were restored;
temporary local BedWars maps were removed.11 fresh Java families/12 resources,
10136 other inputs and275 author WIP paths are guarded. Archive entries and
retained-class linkage are verified. Evidence is under
`build/verification/imba-crossbows-2026-11-05/qualification.json`.

Both build/libs artifacts and main were updated without stopping or starting
main. Existing TMT-last, full IMBA, modes2/4, skill2 and broad-rewind release
gates remain unchanged. No physical-client presentation, peak-TPS or disk-restart
persistence claim. The earlier547cca07 archive passed the original six checks;
it was superseded to include native firework and strong-artifact verification.
Author RP and275 protected WIP paths remain untouched.
