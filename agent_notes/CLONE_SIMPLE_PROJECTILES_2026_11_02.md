# Simple projectile clone images, 2 November 2026

Implementation only; no compilation or native/runtime qualification performed in this delegated scope.

`CloneSimpleProjectileStates` exposes body-specific `recognizes`, `capture`,
`protectedActor` and separate `captureStatuses`. Every ordinary field is explicitly
captured at operation time and installed before final native publication. All twelve
projectile types have actual registered custom EntityType constructor factories.
PsychoVex instead requires its explicit owning-service constructor preserving caster;
no vanilla Vex substitution is admitted.

Body coverage: Air age/sourceLifetime/flyingStyle/emitterStyle/mode/origin; Blind
rainDrop/age/lifetime/phantom/emitterStyle; Ruby age/sourceLifetime; Stone age/lifetime;
Teleport age/sourceLifetime/flyingStyle/mode; Terra kind/age/sourceLifetime/emitterStyle/
modelPaused and fresh model; Ice activeTicks/hand/independent initiating stack/service
and fresh display; Lifestealer age/lifetime; AcidCorruption ball/age/immutable terrain
cause; Fartus kind/age/launchYaw/launchPitch/service and fresh display; Psycho charge
age/lifetime/invisible/spray; ThrownSword age/grounded/embeddedBlock with normal loaded
model creation. Existing loaded methods own final service membership; Ice/Fartus
pre-register the unpublished body only so their newly published child is accepted.
Rollback removes owned entries and children, including partial installs.

Air and Blind trial booleans are protected directly; Ice with a trial initiating stack
is also protected. Independent native child exclusions required in the composite:
`skywars2.ice_display`, `skywars2.ice_crystal`, `skywars2.fartus_display`. Terra and thrown
sword models are packet ElementHolder children and cannot be native discovery roots.

Separate status overlay copies every Psycho Affliction field into a fresh entry;
mutable forced item is copied per clone. AcidCorruption delayed-death entry preserves
its immutable original causal Owner and current delayed_death scoreboard value.
Common native/status/lifetime/source overlays remain the root composite's ownership.

Inspected independent producers: Blind GatlingQueue is player keyed; RainCloud is a
pure virtual producer with position/age/nextTargetTick/targets. Neither belongs to a
Charge image. Teleport cloud/racer arrows have no separate per-body private record;
normal loaded registration follows the copied CLOUD tag. Root relationship adapter
must handle their passengers. Terra swordCharges is player resource state; finishing
models and ChargedSwords pulse/firing queues are independent producers/presentation,
not projectile-owned fields. Their virtual closure is outside this body image file.

Source-only assertions pass for nine explicit field-copy images, twelve real custom
factories, trial guards, display pre-registration/publication order and rollback,
and combined status composition. Scoped git diff --check passes. Runtime still needs
actual native body/child/rollback/owner/lifetime/once-composition qualification.
