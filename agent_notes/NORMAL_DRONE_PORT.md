# Normal TNT drone and Hohlopitek airstrike

## Scope and source

[Source audit](NORMAL_DRONE_SOURCE.md) records both lbc and lbcr source paths.
This group closes normal dropped drone -> native host death loot -> complete
Hohlopitek -> real bomber/TNT/pig payload, plus its special spawn egg. The old
drone is preserved separately. Public ready leaves are lbc:ported/tnt_drone_drop,
lbc:ported/hohlopitek and lbc:ported/hohol_spawn_egg. The broad imba/traps pools
are not enabled by this group. No resource-pack changes or assets are required.

## Author decisions

The author chose unlimited loaded-chunk targeting and a completely technological
weapon: only the mechanism suppressor blocks activation and the ray. Antimagic
beacons, magic shields and nomagic_active do not block it. There is no mana cost,
even with a negative balance. Native resident-chunk checks never force a load.
The accepted first-hit rule replaces the old independent entity+block end calls:
one nearest terminal creates one bomber bound directly to that cast's target.

The original coldownhohol score remains the sole cooldown authority: 200 raw
five-active-tick units, about 50 seconds. Both hands share it. The existing KFC
increment, Envy/Vanity/reset bridges and source actionbar icons/raw value reuse
that score; no second timer or unit conversion is introduced. Right click only,
with the existing configurable offhand dispatch. Source pork particles and three
pig sounds are retained; the offhand origin is 0.5 forward along the view.

## Native entities and lifecycle

The normal drop retains the bat/spawner-cart/hoglin hierarchy and source fields;
only its own host has the guaranteed lbc:hohlopitek death loot. Host death closes
its producer, independently of foreign nearby hoglins. Already emitted bombs
continue. The egg retains native bat-egg presentation and an unpickable item
ENTITY_DATA payload; that one normal token enters the existing dispatcher.

The airstrike places its own native 150-HP NoAI/NoGravity hoglin at target Y+20,
28 blocks sideways, facing its target. Every five active ticks it emits three
real fuse-100 TNT with native 35-HP pig passengers and moves one block forward.
Ground contact changes each tagged TNT fuse to two once. Native Instant Damage
and hurt cooldown govern bomber death; no invented TTL or emission cap replaces
its actual lifespan. Mechanism suppression pauses bomber movement/emission;
native effects continue. Time Stop pauses the existing actor system.

Existing drone actor tracking, native causal owners, conversions, save tags,
resident-chunk guards and quiet epoch cleanup remain authoritative. TNT is WILD,
including self/allies, with protected observer/service roles. Already emitted
current-epoch ordnance remains dangerous when its owner becomes an observer and
retains kill credit. Native loot only runs on a real death, never quiet cleanup.

The shared LbcExasperato loaded-ray queue gains a technology policy, retaining
its CPU budget, DDA and natural vertical termination. Existing magic searches
keep their original policy. No separate ray scheduler or global world scan.

## Verification

Builde7ee5492 passed18 recorded suite results in two actual mode1 matches (81.21s),
with exact260sources/711classes/2727resources verified. Installed main while stopped;
rollback74a3cd83 retained. Main was not started by agent. Three user sources
retain their starting hashes and are excluded from the commit. Evidence:
build/verification/normal-drone-final/. Known fake-player Polymer context error
pair retained; no final suite failed. No real-client pixels, actual disk restart/
unload/disconnect or peak-capacity claim.

The fixture uses real native death rewards, egg use, spawner emissions, bomber
movement/effects, three-TNT pulses, ground-fuse transition, explosions and causal
kill credit. It also covers a 240-block ray, both suppressor types and aura bypass,
negative mana, raw cooldown/HUD, Time Stop, protected roles and actual match stop.
Regression suites include the old drone, special eggs, Vex, shared reactive ray
family and KFC counters. Real-client pixels, actual disk restart/unload/disconnect
and peak-capacity performance are not covered by these fixtures.

The fixture explicitly forces legacy stopper migration to a native mechanism
beacon, removes its owned block/index, and verifies suppression is gone before
the magic-only control cast. This corrects a test-scene lifecycle leak without
changing the production suppressor or ray policy.
