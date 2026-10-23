# Pisa boats native port

Source contract: [PISA_BOATS_SOURCE.md](PISA_BOATS_SOURCE.md).
Qualified ready leaf: `lbc:ported/pisa_boats_drop`;
source has no separate old token. This closes one acquisition and both native
relic variants, rather than exposing an unfinished source pool.

`LbcPisaDrops` summons61 ordinary oak boats plus one oak chest boat. The native
command coordinate helper preserves facing the origin before worldY+20,
0..61 local vertical offsets and0.4-block outward lean per step, including
native trigonometric rounding. Entities retain vanilla gravity, interactions,
break behavior and naming; there is no added lifetime, limit or movement loop.
The first and top boats show the original `pizansk` name. Source plank particles
and wood-break sound are emitted once.

The actual27-slot chest holds one native dirt relic in slot14. Both complete
component sets are preserved: native enchantments, modifier IDs/amounts,
mainhand slots, name, tier and crafting flags. Enabled skill9 of the nearest
eligible player at the elevated summon origin chooses the stronger reward;
the authoritative native profile supplies the skill. Source selector role
protection excludes observers/service roles without ally filtering. The relic
works with existing native equipment and `govnoq` crafting consumers. Boats
have no custom attack/damage payload; no new damage mechanism is introduced.

The existing dropped-token dispatcher owns admission, one-unit debit and
same-tick replay prevention. All prospective boat footprints require resident
chunks; the service neither force-loads chunks nor keeps tickets. Invalid
native spawn bounds skip only the corresponding summons as in source.
The actor set uses load/unload callbacks rather than a new per-tick world scan.
Match epochs/time stop and actual stop are shared with other ported items.
Chest contents are cleared before lifecycle discard, because native chest-boat
discard otherwise ejects container items. Stale managed chest boats are handled
before the generic stale-epoch entity rejection for the same reason.

Item/proxy and public leaf use the original presentation; internal relic reward
tables remain hidden from `/loot` suggestions. Actual resource pack and template
are unchanged. Forest/Infinity/Magic Bean/Ultraponos and broad pools remain
unavailable until all their mechanics/rewards close.

## Validation scope

`PisaDropChecks` and `tools/smoke_pisa_batch.py` cover real source leaf decoding,
62 native actors/geometry, both real reward variants, native chest interaction
and removal, equipment modifiers and actual existing crafting consumption,
role/skill selection, no-load admission, one-unit debit/replay, freeze, simulated
load/unload callbacks and actual stop/cleanup. Native falling is checked with
one actual boat tick; full natural trajectories are not claimed. Room/portal/HUD
regressions run in the same two native matches.
Qualification result is recorded below after execution. Real client pixels,
actual disk restart/unload/disconnect and peak capacity are outside these fixtures.

## Qualified result - 23 October2026

Build5a623666 passed two actual isolated mode1 matches:12 recorded suite
results in69.77s, including Pisa and shared Room/Portal/HUD checks. The artifact
matches234Java sources,648compiled classes and2707processed resources.
Guarded evidence: `build/verification/pisa-batch-final/`; runner:
`tools/smoke_pisa_batch.py`. Known fake-player Polymer packet-context warning
recurred; no final suite failed. Installed main while stopped, rollbackf5061b34
retained, main not started by the agent. Three user-owned Java files preserved
and excluded from commit.

Retained attempts: initial fixture compile used the older two-argument boat
interaction API; corrected to the native26.3 hit-position overload. First native
attempt caught the omitted three-table build resource allowlist; all three are
now included and checked explicitly in the release archive. Catalog needs no
new internal entries: the ready leaf uses the existing `lbc:ported/` prefix,
while internal reward tables remain hidden. Final build/native evidence follows
both corrections. No RP/template edits were made.
