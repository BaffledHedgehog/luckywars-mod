# Boba regional turn implementation ? staged, not qualified

User scope: permanent gravity orientation belongs only to the exact rotated 320-cube;
players keep native gravity. Every transported body, including players, receives the
single positional/look/momentum turn. Players retain normal camera input afterward.

MapRotation captures detached native section palettes and copied BE data on the server,
then computes destination palettes on the existing MapManager executor. The worker
never accesses the live level, chunks, registries, entities or block entities. A completed
preparation rebases intervening live block changes into the destination and recaptures
current BE data before any publication. All terrain/BE writes complete in one server
work step, then native entity identity/vehicle graph transport, spawn-reference updates,
and region activation occur. Native lighting completion/packets can follow later.
Moved fluids receive fresh native scheduled ticks after region activation.

This intentionally accepts the potentially expensive one-tick 320-cube native commit.
No peak TPS or physical-client claim. Unexpected exceptions during publication are
logged; this is not a transactional rollback of arbitrary native exceptions.

BobaRegionPhysics owns disjoint half-open boxes and quarter turns, including overlapping
cubes and four-turn cancellation. The world journal is data/luckywars/boba-regions.csv,
written with atomic replacement where supported. MapManager SpawnState includes these
regions, so existing core/Vanity rewind restores them. Arena removal clears the field;
normal shutdown retains the persistent world field. Region entry/exit is queried from
actual coordinates; no persistent per-entity NoGravity mutation or whole-world rule.

BobaGravityMixin redirects native base, LivingEntity air/fluid/lava, boat and squid
special gravity paths. Native collision shapes and candidate step heights are evaluated
in the regional frame, including local grounding, support-block lookup, fall displacement,
restitution and jump impulses. Ordinary native body bounding boxes rotate about their
entity position, while players keep native boxes. The client remains responsible for
rendering native entity models; no arbitrary roll renderer is added.

BobaFluidMixin retains native source/amount/dropoff/slope/replacement/placement logic.
UP/DOWN and horizontal neighbor plane use the bounded frame. SpreadContext's cache key
projects to the local horizontal plane, avoiding X/Z collisions for vertical spreads.
Flow vectors, liquid shape/AABB, water replacement and lava downward mixing use the
same frame. All adapters delegate unchanged outside rotated regions.

Remaining boundaries to assess in native qualification: vanilla mob AI path planning,
boat waterline selection and entity immersion sampling retain their native world-Y
assumptions. Specialized entity subclasses that override bounding-box construction need
separate inspection. These are not claimed to be a complete arbitrary-axis AI/renderer
port. Ordinary gravity, native collision movement and fluid propagation are implemented.

BobaChecks adds repeated/overlap/exterior/persistence/rewind geometry checks and native
item/cow gravity, support-wall collision, player exemption and water-flow checks.
MapRotationChecks updates look/momentum expectations and verifies that first observed
writes already include complete terrain and entity publication. Existing full-world
rewinds remain the fixture cleanup owner.

Validation in this scoped worker: pinned 26.3 sources and javap invocation-owner audit;
git diff --check passed. No compilation, native server checks, main server stop, install,
commit or resourcepack changes were performed. Root owns final integration/qualification.
