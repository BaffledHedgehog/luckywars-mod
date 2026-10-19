# Wall Wand port

`LbcWall` implements both regular and Academy Wall as immediate casts: it owns no persistent per-player/entity state and adds no tick callback. `LbcWands` owns item registration, input routing, mana eligibility/spending, and Academy cast bonus hooks; `LbcAcademy` owns NPC upgrades and the Wall trial. This reuses the shared first-hit, suppression, mana, indirect world-change, and map-template helpers.

Regular Wall costs 1500 mana and builds the vertical air-only brick plane at the first entity/block hit within 100 blocks. Academy Wall costs 500 per cast, travels up to 180 blocks at 0.6 spacing, and uses the selected mode: vertical wall, horizontal floor, 5×5×5 brick dismantle, or the original 5×5×5 stair template. Dismantling drops the removed brick blocks and refunds 500 mana when at least 15 bricks were removed, per user correction. Stair placement picks one of four yaw directions with a single normalized rotation. Shift+right-click changes the Academy mode in the used hand: pitch <= 0 advances, pitch > 0 reverses.

Placement and dismantling are recorded through the shared indirect-kill/world-change path. Ray suppression uses one analytic segment/sphere-boundary query; template placement reuses `MapManager.placeLoadedTemplate`, rotates bounds before clipping, and touches loaded chunks only. Staff advancement award and Academy trial readiness use existing owners.

The Wall trial checks four cardinal brick probes around the caster's eyes, one block forward on the horizontal look direction, up to nine blocks per probe. Per user correction, it ignores the shared start cell and requires four distinct brick positions. Readiness applies to the actual casting hand; the existing Academy NPC promotes the ready item.

The implementation is present in [LbcWall.java](../src/main/java/me/skywars2/game/LbcWall.java), with routing and mana in [LbcWands.java](../src/main/java/me/skywars2/game/LbcWands.java), trial/upgrade handling in [LbcAcademy.java](../src/main/java/me/skywars2/game/LbcAcademy.java), and bounded structure placement in [MapManager.java](../src/main/java/me/skywars2/game/MapManager.java).

User corrections recorded in [AGENTS.md](../AGENTS.md): «пусть максимум за возврат 500 юудет»; «Да, исправить направления»; «Да, по горизонту вперёд».

Trial correction (user): «Да, четыре разные преграды».

Validation: build and four native server runs passed on `a628fc57…`: Wall,
Bedrock (two matches), Stone and Academy. Wall includes both mana boundaries,
four distinct trial directions and offhand readiness, NPC promotion, all modes,
15/14-brick drop/refund boundaries, eight staircase yaws, fall kill credit and
an actual beacon sphere blocking the ray. Both native Wall donor IDs also work
in the Bedrock recipe without requiring legacy data. Full evidence and test
limits are in [SERVER_TESTS.md](../SERVER_TESTS.md).

The JAR is installed with a backup of `863d710b…`; the main server was verified
stopped and was not started automatically. Legacy server and user resource pack
were read-only. No new language keys or resource-pack template edits were needed.
