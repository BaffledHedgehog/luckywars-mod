# Steampunk durability breaker — implementation only, 1 November 2026

Read-only source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/steampunk_pickaxe.json`
and `function/swrg_kit_integration/items/steampunk_pickaxe_use{,_rc}.mcfunction`.
The source durability criterion checks SelectedItem, then draws chance25 before
TECH20 denial. The payload destroys 17 overlapping world-axis 3-cubes, centered
at eyes + forward distances 1 through 17. It has no mana/MAG/vein/Fortune behavior.

Added only `LbcSteampunkBreaker.java` and `SteampunkBreakerChecks.java` plus this note.
Root owns criterion mixin/bootstrap/getter/check wiring and canonical source loot.
Call `durabilityChanged(ServerPlayer, ItemStack actualChanged, int newDamage)` from
the real native durability trigger **before** damage is assigned. The isolated
`changedPickaxe` predicate requires actual main-hand stack identity and any changed
damage value, including repair. Final user ruling: armor triggering is not intended;
only the pickaxe itself changing durability qualifies, including hits and repairs.
`matches(ItemStack)` reads only the source flag; `count()` counts accepted jobs.

One actor RNG nextFloat draw per admitted durability event; exactly `< .25` accepts.
Direction/origin captured once; duplicate cube cells coalesce in first native fill
visit order (maximum459 source visits). Native destroyBlock(true), followed by air
replacement where needed, retains actual loot/container behavior without held-tool
Fortune. Shared trapChanges supplies existing terrain causality. Missing chunks
use MapManager.prepareChunk tickets; no synchronous loading. Work belongs to the
shared Stonekivi queue and defaults to snapshotIdle=false, so partial accepted
work blocks Vanity capture until complete/cancelled; no private rewind adapter.

Async checks expose start/tick and require `game.items().steampunkBreaker()` plus
`lbc:ported/steampunk_pickaxe`. They exercise actual hurtAndBreak and count the
real native callback with scoped actor RNG control, both probability boundaries,
armor/unchanged/TimeStop/global freeze/spectator denial, actual native negative-damage
repair callback and successful proc, live TECH/MAG markers,
independently reconstructed diagonal/negative geometry, full171-cell cardinal
corridor, exact170cobblestone +1chest +7container diamonds native drops, adjacent
miss, fixed accepted direction, no mana cost and shared capture/cancellation.

No build or native check was run by this sub-agent; integration/compiler evidence
must be recorded by root. No existing sources/build/server/git/RP changed here.

Follow-up user requirement: TECH also cancels an already accepted pending breaker.
Work validity now checks caster TECH, including while awaiting chunk preparation;
MAG still has no effect. Native checks admit work, introduce a live TECH marker,
then prove cancellation leaves the witness gold block intact and settles capture.
Each accepted proc now captures one immutable `coldCause(owner,false)` UUID/order
and reuses it for every batch. Resident cells run together inside one shared work
step, checking EffectTerrain's shared soft deadline every16 visits. Source count
is finite; chunk misses still return to shared asynchronous preparation. Final
durability policy is implemented in the isolated predicate per the user ruling above.

## Superseding user correction (1 November 2026)

> ? ?????? ????????? ????????? ??? ??????? - ?? ????? ???????????? ???????????? ????? ????? ??????? ????? ??? ????

The durability criterion is removed. Successful native player block destruction
and the native melee-contact branch each draw the25% chance independently of
Unbreaking/durability. Armor damage, repairs and standalone durability changes
cannot proc. Accepted tunnel writes do not reenter player block-destruction input.
The prior durability-specific implementation/check descriptions above are historical
and superseded. Current native checks must verify the actual action mixins.
