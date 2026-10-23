# Seven encounter/reward spawn eggs

## Source and scope

Read-only source: old LBC/data/lbc/loot_table/imba/spawnegg.json entries
4,12,13,14,16,17,18 (zero-based). The payloads are copied exactly into native
TypedEntityData; only fatskeleton -> fat_skeleton_rewards and muravei ->
ant_rewards resolve the already qualified scoped internal reward tables.
No source actor preset is replaced by its ordinary dropped-token variant.
No broad imba pool, unimplemented reward or additional acquisition is enabled.

Ready leaves after qualification:

- lbc:ported/fat_skeleton_spawn_egg
- lbc:ported/lucky_pig_spawn_egg
- lbc:ported/honey_bee_spawn_egg
- lbc:ported/stalin_spawn_egg
- lbc:ported/transformator_spawn_egg
- lbc:ported/helicopter_spawn_egg
- lbc:ported/muravei_spawn_egg

Outer names are original red/nonitalic translated keys with one dark-purple
nonitalic imba_version lore row, source tier and vanilla egg model. The full
source component records and exact Java-payload comparison are retained under
build/verification/encounter-eggs-final/source-payloads.json and source-proof.json.
Current read-only RP26.3(1) RU/RPR/EN keys were verified; no new assets, template
or source-resource-pack changes are required.

## Native behavior and ownership

Existing LbcSpecialEggs native SpawnEggItem/ENTITY_DATA and pre-insertion adoption
own use, placement, debit, fluid clicks, spawner data and matching-mob offspring.
Only lifecycle/cause attachment is added to existing creature, pig, bee, Stalin,
Transformer and ant services. No replacement native spawn/finalization, parallel
actor registry, new lifetime or per-tick inventory scan is introduced.
The helicopter egg remains a native unpickable item payload, then enters the
existing helicopter dispatcher; it does not spawn a second direct encounter.

Source distinctions include the skeleton's enchantments/plain bone without the
ordinary drop's modifiers, vanishing equipment, Transformer baby/Birthday date
and no separate text_display, and the ant's vanishing helmet instead of the
ordinary drop's unbreakable/zero-drop preset. Transformer pairs therefore allow
an absent label; ordinary dropped Transformer pairs keep their original label.
All source health, attributes, native effects, loot and native AI are retained.

User's global rule: kill inheritance is important for all items, including
indirect consequences absent from the datapack because of its limitations.
Player-used eggs and player-thrown helicopter tokens now retain that actual
player as causal author; a genuinely ownerless world encounter remains so.
Helicopter arrows/TNT still have their native boss owner; explicit reflection
wins normally. A small weak cause map attaches to existing hitbox/display state
and survives boss removal while launched attacks still reference it. It clears
at match stop. No nearest-player attribution is invented. This supersedes the
older agent-only ownerless-trigger statement in HELICOPTER_ENCOUNTER_PORT.md.

Common role/service guards, per-dimension Time Stop, resident-chunk admission,
existing actor cleanup and quiet actual match stop remain authoritative.

## Verification

Build6b5b6fde passed24 suite results in two actual mode1 matches (63.87s),
exact261sources/714classes/2734resources verified. Installed main while stopped;
rollbacke7ee5492 retained. Main was not started by agent. Three user sources
retain their starting hashes and are excluded from the commit. Evidence:
build/verification/encounter-eggs-final/. Known fake-player Polymer context error
pair retained; no final suite failed. No real-client pixels, actual disk restart/
reload/disconnect, dispenser/spawner execution or peak-capacity claim.
 Tests exercise
actual native item use/debit, source-decoded actor differences, full seeded
native death rewards, Stalin melee/credit, ant acid, shared seven-arc Transformer
hits/credit, native helicopter arrow collision/credit, special matching-mob
children, protected admission, actual Time Stop and actual quiet match stop.
Existing family regression suites run in both real mode1 matches.

Real-client pixels, actual disk restart/unload/disconnect, dispenser/spawner
execution and peak-capacity performance are not claimed by these fixtures.

## Native chunk visibility teardown correction

The retained failed-native-unload-crash.log shows an actual native section
visibility update throwing ConcurrentModificationException while ENTITY_UNLOAD
removed a sibling entity. GameController now queues only the native event
notification and drains it from a detached snapshot before normal tick work,
even during time freeze. ENTITY_LOAD cancels a pending event for the same object;
shutdown drops queued references. Direct service calls retain their synchronous
semantics. There is no new entity scheduler or asynchronous world access.
The same seven-family force-release/actual-stop scenario passed in both final
matches. Actual disk serialization/reload was not separately qualified.

The earlier failed-trace-fixture.log was a fixture error: Transformer electric
arcs are existing ElectroTrace objects, not native projectile entities. The
corrected test exercises the real seven-trace engine, native target death and
causal kill credit. Production arc behavior was not changed to fit the test.
