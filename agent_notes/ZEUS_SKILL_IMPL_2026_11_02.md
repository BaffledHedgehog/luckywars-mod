# Zeus isolated service implementation — 2 November 2026

Final author policy and native qualification supersede this historical implementation/source note: [completed port](ARMOR_ZEUS_SKILLS_PORT_2026_11_02.md). Do not repeat resolved questions.

Only new `src/main/java/me/skywars2/game/LbcZeusSkill.java` and this note written
during this bounded implementation task. Source audit remains
`ZEUS_SKILL_SOURCE_2026_11_02.md`. No integration, catalog exposure, admission,
source/RP/resource edits, build, test or server process performed here.

`pulse(ServerPlayer)` assumes caller has admitted a live skill22 holder, resolved
launch MAG suppression, and excluded paused state, then performs source50% random trial.
Caller should invoke every20 active ticks. Author answer: MAG/strong artifact
block launch only; TECH terminates released arcs on path. Service clips every
segment with existing mechanismSuppressionFraction, including its initial point;
never queries target/travel MAG. Targets strictly before TECH boundary can receive
the hit; boundary/beyond cannot. No mana, item-use, Frost or active-input gate.
No service persistence needed: accepted
pulse finishes synchronously and owns only finite local resident-body snapshot,
visited identity set and local segment state. Damage-created bodies cannot grow
that pulse's work; one visited attempt per captured body. Reentrant pulse is
rejected during native callbacks. No arbitrary target-count cap or catch-up debt.

Nearest eligible feet within6 of current sample, direction to eyes; initial origin
holder feet+1. Each leg has26 quarter-block samples0..6.25 and requires resident
chunks, with air-tag check before every noninitial sample. Root explicitly
confirmed author's existing exact-ray migration requirement: collision uses
`LbcBeamGeometry.intersects` and exact native AABB segment entry to select first
body. No positive1m legacy selector cube or widened beam. Shared wands.target plus
validRoleTarget protect self/allies/summons/protected roles; no WILD lightning.

Each encountered body receives one native4 LIGHTNING attempt credited to holder.
Legacy source used CACTUS; root requested shared electrical protections. Existing
LbcDamage.LIGHTNING is exactly the shared electricity type retaining ordinary
hurt cooldown, unlike ELECTRIC's bypass tag. Techno/MK2 immunity and ordinary
shared electrical armor rules therefore remain canonical without custom bypasses.
Native hurt cooldown, armor and Resistance remain untouched. Local visited state
is set before hurt callbacks. Rejected native damage still consumes the source
one attempt. Then next leg begins at that ray sample. No recursive Distance
scratch, old-leg continuation, cross-pulse global tags, electrical status,
lightning body, explosion or fire. Spark count/offset/speed and both exact source
MASTER sound IDs/volumes/pitches use existing coordinate sound dispatch.

Synchronous work is finite: at mostN successful legs plus one terminal miss,
26samples per leg, each scanning snapshotN. This is not a measured peak-TPS claim;
large-population qualification belongs to root integration. No per-tick world or
inventory scan, chunk generation, NBT copies or queued replay state introduced.

Author suppression answer is incorporated as above; MAG launch admission remains
root-owned. Service does not make22 selectable. Root owns integration, checks and builds.
