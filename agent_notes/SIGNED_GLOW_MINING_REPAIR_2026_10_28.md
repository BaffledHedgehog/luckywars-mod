# Negative Glowing and mining repair — 28 October 2026

## Author request (user)

> glowing - пусть отрицательные уровни инвиз дают растущий
> отрицательная скорость копания - почему то не всегда работает и игрок все еще время от времени умудрятся сломать блоки. Предлагаю так - пусть на клиенте будет полный запрет ломания (так как один фиг с отрицательной скоростью копания блоки только ставятся, тогда и случайно из-за лага игрок не сможет что то сломать) - а ещё - рейкаст там вообще неточный и блоки ставятся не у той грани на которую игрок нацелен (надо исправить)

## Visibility

Keep L=(long)amplifier+1. Negative Glowing supplies invisibility tier abs(L):
I is native invisibility, II hides particles, III+ uses the existing player-only
Interaction proxy. Zero remains neutral; positive Glowing keeps its existing
outline/halo behavior. A stronger real Invisibility effect wins. The derived
tier never creates or removes synthetic native effects, so real effect layers,
independent durations and armor ownership remain intact. Removal, expiry,
relog/respawn and per-viewer negative Blindness use the same existing callbacks.
Negative Invisibility still glows independently.

SignedEffectVisuals.invisibilityLevel is the single combined tier for native
invisible flags, immutable metadata snapshots and FullInvisibility. Signed
effect packets retain their original values; negative Glowing's vanilla client
effect is inert, with the authoritative flags/representation providing hiding.
No resource-pack/shader changes are required.

## Mining

The old face was the approximate nearest direction from block centre to eye,
which disagreed with the actual pointed surface on oblique angles. The new
loaded-only outline voxel ray uses the native block shape and hit direction,
including half-height slab surfaces. Each held tick revalidates the actual
aimed source and unchanged tool before charging progress/copying. An unloaded
cell terminates the ray; no chunk tickets or loads are acquired by traversal.
Native reach/height/protection/roles/epoch/Greed/TimeStop and full independent
block-entity copying remain. Copy destination is checked separately.

Native destroyBlock now refuses an effective negative rate regardless of whether
a cloning session was admitted. Negative STOP clears delayed native work even
without a session, and tick handles a sign change during ordinary mining. This
covers creative/instant/STOP/delayed/direct native completion without disabling
positive effective mining. Native progress still determines the sign: a negative
underwater multiplier while dry does not block ordinary positive mining.
Unbreakable hardness returns zero progress before consulting player speed; the
final prohibition additionally checks signed player speed in that zero case.
This closes creative destruction of bedrock while negative without inventing
cloning progress for unbreakable blocks. Native START/direct/client-projection
regressions cover that separate vanilla shortcut.

The client receives owner-only BLOCK_BREAK_SPEED=0 without modifiers while its
current aimed block has negative native progress. Native26.3 MultiPlayerGameMode
checks abilities.instabuild before progress, so creative clients additionally
receive instabuild=false; the server flag and game mode remain unchanged. Other
ability flags, flying/walking speeds, scope updates and unrelated attributes
stay. Dirty efficiency/water packets that omit break speed receive its zero
snapshot too. Tick transitions restore the real attribute/ability snapshots.
Normal packet sends have a cheap relevance/sign filter; terrain is never queried
from the async encoder thread. The final server destruction guard remains
authoritative even before a client receives a projection update.

## Native verification

SignedEffectVisualChecks covers full signed endpoints, native negative-glow
invisible flags, combined tier precedence/removal, true recipient metadata,
proxy/reveal/particle thresholds and source removal for real player targets.
Existing carrier, relog and unrelated post-effect checks remain.

NegativeMiningChecks adds actual packet-listener START/STOP and real outgoing
connection capture, zero owner speed on efficiency-only updates, creative
prediction/restoration, direct destroy and stale STOP guards, tool/aim changes,
oblique WEST-vs-centre-NORTH and slab UP placement. Original rate/guards/creative/
water/independent chest-content-copy assertions remain.

tools/smoke_signed_glow_mining.py runs these with full invisibility, signed math,
effect layers, attribute policy, actual TPS compensation, Phantom armor, scopes,
FOV, rifle hands and TimeStop regressions in two isolated native mode1matches.
Local evidence lives in build/verification/signed-glow-mining. Physical client
frames, real network latency and peakTPS are not inferred from packet fixtures.

Initial candidate930dd2fb completed both native matches and passed the new
geometry/guard/protocol/visibility checks, but negative-mining failed its final
no-drop assertion in both matches. Its new positive creative destruction control
legitimately emitted a log through CreativeBlockDropsMixin before that assertion.
The control is moved after the unchanged no-drop assertion; production behavior
and negative assertions are not relaxed. Initial build/server/results/artifact
evidence is preserved with initial- prefixes. This candidate was not installed.
Final c33bd703 passed38recorded results in two actual mode1matches (71.13s),
including the zero-progress unbreakable guard. All542sources,1540classes and
3230processed resources match the tested artifact; the Loom-normalized descriptor
and declared nested SGUI archive are separately verified. All198protected author
paths and29live grimoire override JSONs remain unchanged. The owned child stopped
normally and exact fixture snapshots were restored.

The existing Polymer chunk-packet context warning appears once at fake-player
startup in both old beam-collision and initial/final runs; its stack is native
PlayerChunkSender/Polymer, outside this patch's packet classes. No check fails
or new warning count is attributed to this change. Runtime warning evidence is
retained rather than claiming a warning-free startup.

Ready artifact: build/verification/skywars2-signed-glow-mining-c33bd703.jar.
The existing installer adopts this in place of queued784b026b, waiting for normal
mainccac368fshutdown/world unlock without forcing a stop or restart. Status is
recorded in build/verification/signed-glow-mining/main-install.json. Keep c33bd703
ready,784b026bprevious qualified,ccac368fmain/rollback and the separate audit tool;
obsolete723b2a3aarchive is path/hash checked before removal. No RP change needed.
