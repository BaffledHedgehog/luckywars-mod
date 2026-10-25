# Chef mannequin rework ? 25 October 2026

Status: QUALIFIED READY. This qualification supersedes the earlier Chef AI
and stasis behavior. See [AI audit](CHEF_AI_REWORK_SOURCE.md) and
[presentation audit](CHEF_PRESENTATION_REWORK_SOURCE.md).

## User contract

Chef remains a real native Mannequin bound to the exact victim of an accepted
WTF-label melee strike. Infinite health;20 accepted cumulative damage causes
200 active ticks of prone knock. Knock retains gravity and void recovery; actual
Stand Time Stop remains a separate complete freeze and pauses knock duration.
No NPC subtitle. Original provided PNG is preserved byte for byte. The held
sword uses lbc:oguzok_sword, native wooden sword properties with+3 attack damage
and?2.4 attack speed. The new native combat base1/4 gives4 damage and1.6 attacks
per second before other modifiers; falling critical strikes apply1.5.

The requested AI uses native collision/travel and deliberate inputs, including
terrain-aware walking, sprint jumping, short parkour, bridging, pillars and
mining. The camera/head must not snap between grid nodes. Swim, extinguish with
a real bucket, water-clutch and retrieve its own source. Hunger/exhaustion gates
sprinting; food is consumed with native timed use when outside melee danger.

## Implementation

LbcChefBrain uses fractional sixteenth-block floors from actual voxel shapes,
standing-body sweeps, diagonals, step/drop/jump edges and a cached bounded A*
frontier. Terrain-only routing gets first opportunity; costly bridge/mining/
pillar actions follow when walking/parkour cannot reach the target. Long jumps
require a clear supported runway, sprint eligibility and actual takeoff speed.
Pillars align/settle before launch; real bridge blocks attach ahead with reach,
face and collision guards. Mining retains accumulated native hardness progress
and chooses the fastest retained tool. Block changes retain owner attribution.
No terrain changes use per-tick teleportation or no-gravity movement.

LbcWtf.Chef feeds smooth body/head orientation and world-relative movement into
native travel. Waiting stops input while preserving vertical motion. Knock
sets native SWIMMING pose with0.6-high prone box, prevents actions/damage and
horizontal voluntary movement, while native gravity and collision still tick.
Wake checks standing/crouching clearance. Void rescue is exceptional relocation
and is allowed during knock; target and remaining knock persist.

LbcChefSurvival owns a retained water bucket and food supply. A bounded ballistic
collision prediction selects a visible reachable support, uses native
BucketItem.emptyContents, and verifies real source water. Water itself handles
fall/fire effects. Recovery uses native BucketPickup only for its retained,
unchanged source after actual contact and settling. It never sweeps arbitrary
water. The custom hunger clock accounts for actual locomotion and jump/attack
events; native food-use completion must consume the retained stack before
nutrition is credited. Threats interrupt use without credit. No Player-only
FoodData.tick is called on a Mannequin.

## Presentation and delivery

Native skin patch skywars2:entity/chef is correct and expands to
assets/skywars2/textures/entity/chef.png. Original PNG SHA256:
`bc1ca79f70a6e01688495db3abb3fccccb60298c40f7b6d049d6dbf46848eb36`.
Both mod resources and generated Polymer server pack contain the same bytes.
The manual author RP did not contain that path at audit time and server pack
loading is optional. Actual client pack loading has not been observed, so a
specific delivery failure is not asserted as proven. A minimal exact PNG is
provided at resourcepack_template/assets/skywars2/textures/entity/chef.png for
manual copying; the read-only author pack is never edited.
ChefMannequinAccess invokes native setHideDescription(true), which sends an
empty description in synchronized metadata rather than only hiding a server
getter. The author sword item/model/texture chain already exists and needs no
new RP model or translation.

## Qualification boundaries

ChefChecks includes real voxel terrain fixtures, full bridge/pillar traversal,
fixed-victim native damage/ownership, prone falling knock, low-ceiling wake,
knocked void recovery, food completion/interruption, real water landing/pickup
and fire contact/extinguishing. Separate chef-natural uses only real server
and world ticks:200-tick falling knock followed by slab/stair traversal,
construction/crossing and an actual attributed hit. It observes bounded
consecutive body/head turns and terrain scope. Fixtures restore players,
profiles, teams, stats, world blocks and scheduled block/fluid ticks.

Selected deterministic terrain and natural chase do not qualify arbitrary map
navigation or a universal human-level AI. Client-rendered pixels/smoothness,
physical disk restart/disconnect and peak-load performance require separate
evidence. Ready-only preference persists; do not install or restart main.

## Qualified artifact

Build `4bf4649e` passed22 recorded results across two actual mode1 matches
(96.93s including startup/map loading and regressions). Both matches passed
12 explicit Chef scenario groups plus the naturally ticking chase, stop probe
and cleanup; shared Sheep, Danmaku, player rules and Time Stop regressions passed.
Exact350 Java sources/975 classes/2818 resources match the tested artifact.
Ready JAR: `build/verification/skywars2-chef-rework-4bf4649e.jar`.
Evidence: `build/verification/chef-rework-final/qualification.json`, archived
build/runtime logs and driver result. Full SHA256:
`4bf4649ed8c74c24815968967230e4ce7c3bb5f929dd9a6e10eca521bafc4185`.

Main disk JAR stayed04396ca4; this task did not install, stop or restart main.
Five pre-existing user files and deleted applied RP fragments are preserved.
The isolated instance was stopped and its modified files restored. A known
pre-existing fake-bot Polymer packet-context diagnostic appears at bot setup;
no new runtime error was admitted by qualification. Native evidence does not
prove the actual client's loaded packs/rendered skin or universal path quality.

The test harness now calls native Entity.commonTick() before explicit tick(),
matching ServerLevel's tickNonPassenger clock/history wrapper. Without it,
nextBuild=tickCount+2 could never expire in the test while working in normal
world ticks. Natural terrain was also corrected to provide real body headroom
when descending the elevated step; impossible untouched geometry is not an AI
contract. Pillar completion is an actual attributed hit from a built column,
not an arbitrary height checked immediately upon entering melee radius.
