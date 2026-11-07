# Test report fixes — 7 November 2026

User's 17-point report is the authority for this batch. Internal candidates carry
the complete qualified TMT parent; public TMT acquisition remains disabled.
Main `mods/skywars2.jar` and build/libs are not replaced, and the main server is
not started. The author's RP, deleted templates and concurrent HUD work are preserved.

## Changes

| Report | Result |
| --- | --- |
| 1 | Gallery fallback resolves authored loot before bare native defaults. Each frame retains a branch loot recipe and right-click rerolls it with the acting player context. Native tracking restricts gallery frames to 20 blocks, including FPV viewpoint. |
| 2 | Shared opaque-cube item/block helpers retain one predicted placement sound and one break event per audience. Consumed matter uses a matching mushroom sound carrier with its existing explicit black model. Creative, instant mining and server-controlled survival mining have separate event audiences. |
| 3 | Otchim spawn preset restores `Silent:1b`. |
| 4 | Native End Crystals explode when damaged by another explosion; native removal still precedes the next blast. |
| 5 | Weakness multiplies the complete signed attack attribute by `0.75^level`; negative levels use the reciprocal. Other modifiers remain intact. |
| 6 | Void Arcanum excludes already captured bounding boxes from homing selection while continuing their physical pull and damage. No target teleportation is introduced. |
| 7 | Railgun keeps its original charge-dependent range, up to 300 blocks, and one-block discrete explosion samples. A sampled ray now waits for an owned asynchronous chunk lease instead of terminating at a cold chunk. Completion, stop and rewind release leases. |
| 8 | MOB/legacy-creeper blasts publish one central flash and no 26.3 block-fragment burst. Railgun's duplicate explicit flash is removed. Native TNT presentation remains separate. |
| 9 | Pumpkin, Ruchka, Honew and Bucket support allied and self-catching WILD hits. Native `Projectile.leftOwner` arms self-contact; inventory protection and protected entity roles remain effective. |
| 10 | Thrown pipe uses impact-centered native knockback, living/self contact and a motion-aligned virtual model over the real physical/pickup item. Ordinary pipe keeps its normal rendering and ground-only activation. Owner arming begins only after a completed movement segment has left the thrower. |
| 11 | Thrown fireballs restore Fire Charge use sound and request native position/velocity correction every active tick. Actual camera/render offset still needs a real-client check. |
| 12 | Native perk statistics resolve all authored statistics text, add missing base/challenge rows, capture accepted start choices, and credit offline winners. Durable per-event receipts avoid replay duplicates. Startup imports raw `stats:stats` counters and retains the complete source before checked atomic persistence and legacy cleanup. No per-tick disk writes. |
| 13 | Rotated client-simulated entities receive client-only NoGravity plus precise correction. Native falling-block support, spawn, landing and bounce use the rotated frame. Suffocation checks use the rotated eye slice while retaining actual embedded-head damage. Existing unsupported sand is scheduled after map commit. |
| 14 | All 16 color-artifact effect branches match legacy durations/amplifiers/radii. Added dyed wool/concrete stairs/slabs, wall banners and candle cakes to the proper tags: 192 memberships/triggers. |
| 15 | Only the mystical-tree terminal creeper suppresses its lingering potion cloud; its real invisibility remains. Ordinary potion creepers are unchanged. |
| 16 | Reconciled 145 stale vanilla-carrier entries against ready native identities; 133 are reachable from mystical-tree loot. Only item IDs change; all remaining resource bytes and selection/functions/count metadata are preserved. Final audit finds zero remaining exact stale identities or ambiguity. |
| 17 | `GameController.begin()` consumes stale Cunny/rune/queued-start intents. This fixes the real interleaving where a selected-map callback entered COUNTDOWN but the old Cunny PREPARING driver intercepted every following tick. Shared start and separate Balance/Supernova completion paths were reviewed. |

## Verification and remaining limits

Evidence lives in `build/verification/test-report-2026-11-07/`.
The sealed `component-verification.json` records exact candidates and native runs.
Internal test `76e441e2` / production `7b0d706c` are built on full parent `93c5f4a7`.
Fifteen unique native suites pass. Reused run evidence is accepted only after archive
comparison proves every gameplay class byte-identical to the final candidate; only the
two updated check families differ. Failed diagnostic runs remain in the evidence directory.
Compile uses one Gradle worker, a 512 MiB heap and the qualified parent archive.
Native fixtures run singly on port 25577 with a 2 GiB heap and restore their saved files.
No main-server operation or author-file restoration is part of this batch.

The original canonical-loot test had stale expectations from before nine legitimate
definitions were added. Its count is now 3623. Its check-only expander preserves
the native distinction between ignored `functions` and active `modifier` fields;
Mellstroy's bootstrap template and the native jungle slab have explicit witnesses.
Antimagic Sword is checked by its real native identity and tier S.

New native checks: `test-report-core`, `native-test-report-combat`,
`rotated-physics`, `perk-statistics`, and `cunny-queued-restart`.
Gallery checks exercise 24 actual operator clicks and require distinct random enchantments.
Void checks retain acceleration/speed bounds and now test captured-target exclusion.

Real-client confirmation remains necessary for fireball visual offset, perceived block
audio/particles, pipe motion appearance and gallery rendering cost. Server packet and
native physics checks cannot establish the final rendered frame or audio playback.
Exact outer rotated-region boundaries retain the existing feet-based region membership.

The existing malformed, unbundled `reroll_9999/material_block.json` is left untouched.
This batch does not claim completion or public release of the remaining TMT transfer.
