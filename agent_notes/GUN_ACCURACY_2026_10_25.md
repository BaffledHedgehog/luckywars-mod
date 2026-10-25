# Native gun accuracy

Author request (user):

> о кстати - сделай чтоб у магнума и у slg был разброс если стрелять не в прицел и ещё больше разброса если игрок двигается в момент выстрела.
>
> И с АК-47 но там только штраф за движение

Superseding balance (user):

> разброс в движении пусть до 20 градусов доходит в зависимости от скорости игрока у всего, разброс не в прицеле пусть будет 5

Magnum and SLG-88/SLR have a five-degree hip-fire cone. Their authoritative
RifleZoom scope removes that base cone. Both AK variants have no hip penalty.
All three player gun engines add a speed-dependent penalty, with a TOTAL
twenty-degree cap (not twenty plus five). These are maximum angular deviations
from the shot's aim direction, sampled uniformly over solid angle.

Implementation balance choice: movement penalty grows linearly from zero to
twenty degrees at 0.3 blocks/tick and stays capped above it. Actual native known
movement and external velocity are considered, using the greater speed;
ground-contact vertical jitter and speed at or below 0.01 are ignored.
Airborne vertical velocity counts, including jumps/falls. No movement-attribute
or pressed-key proxy is used. Example maximum cones:

| Speed, blocks/tick | Rifle without scope | Rifle scoped / AK |
| --- | --- | --- |
| 0 | 5 degrees | 0 |
| 0.075 | 10 degrees | 5 degrees |
| 0.15 | 15 degrees | 10 degrees |
| 0.3 or higher | 20 degrees | 20 degrees |

One shared direction sampler feeds native hits and their existing trails.
SLR keeps its authored muzzle offset and sound origin. Camera recoil, damage,
payload modes, ammo, timers and dual-hand policy remain in their existing
engines. Magnum recomputes spread for every scheduled burst shot. Scope state
is checked against the actual live player/held stack, never inferred from a
possibly saved post-effect. Shared AK NPC/boss ray overloads remain unchanged.

GunAccuracyChecks exercises actual item use and captures native tracer packets
for sixty hand/scope/speed combinations per match, with seeded expected ray
directions. It additionally changes speed between paid Magnum burst shots,
checks jump/fall/push handling and validates cone bounds at vertical aim.
Existing payload fixtures explicitly aim when they require exact hits.
Rendered client feel and busy-main TPS require manual observation.

Validation: build7682f8c0 passed24 recorded results in two real mode1 matches (45.13s), including120 actual item-use traces and the existing payload/ammunition/scope/FOV/sound/stop regressions. Evidence is retained in build/verification/gun-accuracy-final/.
