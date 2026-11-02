# Complex projectile active-body clone images ? 2 November 2026

Implementation only; no build, native process, publication, or skill coverage qualification was run by this owner.

`CloneComplexProjectileStates.recognizes(Entity)` selects this reviewed family;
`capture(GameController, Entity)` returns its immutable operation-local ActorImage.
`protectedActor(Entity)` identifies actual authority guards: Exasperato
`Charge.academyTrial`; Prism `Phase.ACADEMY` and charges holding a trial stack.
These return null from capture even if called without the protection predicate.
Malformed/unregistered recognized bodies return null, never an inert fallback image.

Covered body/state owners:

- Clouds: chemical lingering potions, chemical native AreaEffectCloud, AcidProjectile.
  Copy kind/lifetime, acid lifetime attachment, swept/shield segment history translated
  with cloned body position, and native cloud's external owner. Owner indexes install
  before publication; loaded now adopts these entries instead of resetting lifetime.
  `acidAuthors` is keyed by retained external author UUID, not body identity; its
  existing entries remain available to clone tags without installing/removing originals.
- Exasperato Charge: age/mode/lifetime; reject academyTrial. Search/RayTicket requests
  have no Charge link. Do not duplicate their independent virtual casts, NukeAim,
  BlastBatch backlog, or nuclear visual children when cloning a Charge.
- Exoperso Charge: age/flightAge/upgraded/aim. Each future impact naturally builds its
  own Series/blast queue. An emitted native actor with a live blastSources binding
  gets only its own independent Series binding and copies of requiredPlayers/kills;
  no other source/body or emitted queue entries are cloned.
- SpecialFireballs: actors index and pending conversion snowball membership.
  Native saved item holds variant, native body holds flight/fuse. CHILD means actual
  damaging DragonFireball/TNT/fang siblings, not a protected visual child marker.
- HalloweenWind: exact private NativeCharge constructor at original ServerLevel,
  retired flag and shots membership. A merely tagged ordinary WindCharge is unsupported.
- Govnomet: Charge age/mega/retired and charges index; exact private OwnedDirt class
  constructor and falling index. Native NBT/runtime provides falling-block state.
- Prism: phase, age/steps/bounces/original origin, accepted caster/hand, independent
  item snapshot and hit UUID set, particle and exact immediate-queue membership.
- Syringe: exact private BonusFireball constructor at original level, original external
  launching Mob and leftOrigin collision immunity. Never create/recast its owner buff.
- Neutral: exact native carrier type via type factory and projectiles membership.

All mutable body sets/queues/state objects are independent, owner map entries exist
before ENTITY_LOAD adoption, and rollback removes only this clone's additions.
No costs, new casts, owner side effects or whole checkpoint restores occur.
The image helper validates original-to-clone family binding. Registered type factories
already make matching subclasses for ordinary custom Charges and AcidProjectile;
private native subclass factories are explicit for Halloween/Syringe/Govnomet dirt.

Integration boundaries: common native/transient relationship state, lifetimes,
spellSources, indirect causes/status layers, Electro reflectedOwner (including
Govnomet dirt), and Summons tracking (including Syringe fireballs) belong to their
shared adapters. Syringe living-mob buffs/holding relationships are outside this
projectile scope and still need the living/status owner closure. Pure virtual rays,
trial authority, nuclear visuals and unrelated already-emitted blast batches are not
copied here. No new jammer admission filter is added; cloned actors retain their
ordinary native/custom flight suppression behavior.
