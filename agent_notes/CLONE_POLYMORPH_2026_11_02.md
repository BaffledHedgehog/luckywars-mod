# Polymorph clone images, 2 November 2026

Bounded implementation/source review only; no compilation, build or native process
was run. Scope: LbcPolymorph, new ClonePolymorphStates and this note.

Dispatcher exposes recognizes, recognizesGameplay, protectedActor and capture.
Gameplay Orb is the private Visual ItemDisplay itself, not an independent cosmetic
child. Its real Visual factory, immutable causal Authority and current age are copied;
normal native display image owns model/position/metadata. Orb flight motion is now
mirrored in native delta movement on spawn and tick. Clone install takes its new
native velocity, preserving source15 unchanged motion and source17 spread. Original17
native spread is consumed next Orb tick. Retained native source Display.tick at
build/verification/vanity-actors/native-src/net/minecraft/world/entity/Display.java:153
has only removed-vehicle/client interpolation handling and never integrates motion;
there is no second native movement step.

A nonplayer Body clone uses actual private Body constructor and an independent
hidden original through root copyAuxiliary. Auxiliary callback runs before native
owner-image installation/publication. It creates a new private Form and registers
both maps, so full native/owner cloning sees the hidden body already masked. Hidden
position aligns with copied sheep. The original source Form, original mob and source
sheep remain separate. Body duplicate-hit cache starts fresh, matching the reviewed
transient native behavior rather than retaining an old attack receipt.

New Form copy constructor copies saved pre-transform epoch/cause, original visibility/
gravity, health/absorption/fire/air, remaining duration, equipment, native effects,
owned suspended leases, shadow-absorption NBT, status counters, attribute image,
inventory and mode. Every mutable stack/effect/lease/NBT/collection is copied again
for every install. It never invokes ordinary Form construction on a concealed mob,
never suspends original leases again, and never reruns cast/cost/transform admission.
Remaining source ten-second clock is preserved, including already elapsed time.

Pending clone Forms retire their maps quietly if auxiliary construction/publication
fails; they do not execute finish/resume or death continuation. Final sheep native
publication clears that transient flag. Main-image rollback first removes its Form
maps, then rolls back the auxiliary full body graph. Original forms are unaffected.

Permanent doubled/quadried flags flow original->sheep at initial transformation,
copied sheep->copied hidden original before auxiliary publication, and sheep->original
on normal restoration. Restoring a clone therefore cannot reset either once flag.

All concealed originals are protected from independent proximity scanning. Player
Form Bodies are also protected; no player authority, inventory, camera or saved
player identity can be cloned by this dispatcher. A tagged/typed Orb or Body without
its actual owner record returns null rather than a visual-only sheep/orb shell.
Root must admit recognizesGameplay Orbs despite generic ItemDisplay exclusions and
provide its package auxiliary transaction plus the cloneSkills accessor.

Source assertions pass for complete Form saved-value coverage, real factories,
auxiliary pending cleanup, native velocity and every once transfer; scoped diff
--check passes. Native clone/expiry/death/rollback/TimeStop/owner/status/once-composition
qualification remains the root integration task.


## Suspended causal effects follow-up

Narrow causal fix adds LbcIndirectKills.SuspendedEffectCauses: an immutable map of
actual active effect-type to original immutable Owner, plus the pending mana Owner
only when its queued Status references the same actual active effect instance.
Form captures this before native effects/owned leases are suspended, and its copy
constructor and FormCheckpoint retain the immutable image. Thus both ordinary and
cloned forms preserve original poison/Wither/blindness and other accepted status
causes rather than manufacturing the polymorph caster as a replacement author.

Normal restoration grants each saved effect once, records the accepted actual
native instance, resumes owned leases, then rebinds Status/pendingMana only if that
exact accepted instance remains native-active. RAID_OMEN mana attribution additionally
requires its source amplifier20. No effect is granted or instant effect retriggered
by causal rebinding. Missing/rejected/replaced restored effects do not gain stale
saved authors. Existing mana drain/debt ledgers remain with the ordinary owner image.

Source-only checks verify capture order, immutable Form/copy/checkpoint flow,
after-resume alias identity and absence of addEffect in causal rebinding. Scoped
whitespace check passes; root owns compilation and native poison/mana probes.


## Removal semantics and native fixture scheduling

Native4 exposed that tick conflated positive-health discarded bodies with lethal
sheep because native isAlive becomes false on removal. Polymorph tick now passes
lethal=true only for health<=0 or native RemovalReason.KILLED. Positive-health
DISCARDED/unloaded temporary sheep restore their independent original normally.

The clone fixture now records all three copied Forms/hidden originals plus source
Form and poison cause, discards the three sheep together, and stages at -1. It calls
no manual polymorph.tick and does not check restoration within the creation callback.
The next actual fixture scheduler tick checks removed Form maps, live visible restored
originals, HP17, emerald gear/Haste and original poison attribution, plus source Form
identity remaining intact, then begins the ordinary Air flight continuation probe.
No reflective field mutation or tick-guard bypass was added.

Source-only assertions and scoped diff --check pass. Root owns the next build/native
run; this change itself makes no native success claim.
