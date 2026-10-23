# Antigravity native storm port

User chose the finite15-second Paper storm over legacy perpetual gravity
escalation (23 October2026). Exact native/datapack/command distinctions and
container bytecode proof: [ANTIGRAVITY_SOURCE.md](ANTIGRAVITY_SOURCE.md).

Ready public leaf: `lbc:ported/antigravity_drop`; item
`skywars2:antigravity_drop`, original tier_ff/drop_trap/name/lore/flags.
One31-launch storm starts at the floored token position: one initial3x3x3 cube,
then30 random horizontal-offset cubes every10 active ticks through300. Original
block states become native BlockDisplays with the source half-block offset,
view128, no shadow, default brightness and one-tick transformation/position
interpolation. The native entity type tracks every tick. One service updates
all motion/storms, replacing per-display scheduled jobs; no new cap/absolute TTL.

Display velocity grows by.04 while below3, retaining the source floating-point
loop. The nearby1x1x1 query raises each eligible living entity's Y velocity to
max(current,displaySpeed+.2) without changing X/Z. Each10-tick pulse also raises
living entities in the fixed origin AABB17/60/17 to at least.55 and grants the
source30-tick ambient hidden Slow Falling. Players at relativeY>=30 take4 native
GENERIC damage per pulse; the original no-air message/tag has entry/reset
behavior. Wild targeting includes caster/allies, with shared observer/service
role exclusions. Freeze retains its normal movement lock; stun still permits
external movement. The existing translated legacy titles are reused with native
0/40/10 timing; the finite Paper mechanics do not replace authored RP text with
hardcoded English. No RP/template changes are needed.

The damage retains the actual thrower and a positionless generic source;
WILD admission uses its causal token tag. Lift and terrain hazards register with
the shared indirect-kill service, so later accepted impacts follow the existing
last-source rule and native fall/void deaths retain credit. Source damage
protections/cooldown remain native; no new damage type or gravity modifier.

Block conversion mirrors actual CraftBlock.setType(AIR,false): explicitly
remove BE then replace with flags530. Containers are not carried or ejected;
the old source also removes their contents. This is established by local Paper
bytecode, not guessed from the absence of an explicit drop statement. SkyWars
has no protected Bedwars region-snapshot layer; no Bedwars mode is enabled by
this patch. Its future region semantics must be wired before enabling that mode.

Only loaded cube footprints are read; absent chunks are skipped without tickets
or synchronous loads. Shared token admission owns unit debit, fake/replay and
match guards. Existing display motion runs before dropped-token activation so
new initial cubes start moving next update. Time Stop pauses ages/motion in its
dimension. A display expires after20 consecutive active ticks with no eligible
player inside its source80/110/80 AABB; accompanying players may keep it alive
past the15-second producer and past build height, as in source. Removed/unloaded
displays detach from motion tracking. A reloaded visual without live motion
state is quietly discarded; callbacks do not reconstruct a stale storm.
Stop removes all displays/storms and owned notification tags without block loot.

## Qualification

Offline build38da8388; two actual isolated mode1 matches passed12 recorded suite
results in67.63s. Antigravity native geometry/state, first random cube, motion,
max lift/roles/ally/self, slowfall/altitude generic owner, native void kill credit,
300 active ticks, accompanied display320 ticks, stale19/20 cleanup, pause350
service ticks plus real ticks, no-load/fake/debit/epoch callbacks and actual stop
passed. Pisa/Room/HUD regressions ran in the same matches.237 source files,
656 compiled classes and2708 resources exactly match qualified archives.
Evidence: `build/verification/antigravity-batch-final/`; guarded runner:
`tools/smoke_antigravity_batch.py`. Initial compile-only attempt used obsolete
velocity-sync field names; corrected to26.3 needsSync/syncVelocity before final
build/runtime qualification. Known fake-player Polymer packet-context warning
recurred; no final suite failed.

Installed main while stopped; rollback5a623666 retained; agent did not start main.
Three user-owned Reroll/AcidAntDropChecks/HelicopterEncounterChecks files preserved
and excluded from commit. No actual client pixels, disk restart/unload/disconnect,
full natural physics or peak-capacity/CPU benchmark is claimed. Shared updates
are an architectural simplification, not a measured throughput claim.
