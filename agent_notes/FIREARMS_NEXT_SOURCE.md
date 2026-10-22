# Next expanded-shooter sources — bounded follow-up, 21 October 2026

Source: `LBC/data/lbc/loot_table/transformables_shooter_all.json` in the legacy
server's disabled datapacks. This is a short candidate inventory, not a gameplay
audit or authorization to expand the current batch. No source pools are changed.

## 1. Govnomet / Shit Cannon

Inline item entry after `lbc:tuntijaa_terra_razor` and before
`lbc:helicopter_boss`: model `lbc:govnomet`, name `shit_cannon`, tier D,
flags `{govnoq:1,transformable:1,shooter:1,magicwand:1,craftusable:1,govnomet:1}`.
Lore includes `shit_cannon.lore`, `shitgun.lore` and original author/event text.
No native Govnomet weapon registration was found in the current Java source;
existing `govnoq` crafting/damage/reward references are not this weapon engine.

Estimated boundary: small-to-medium player weapon, subject to tracing its actual
activation/projectile/resource call graph. Do not infer behavior from its name.
Safe reuse: LbcWands used-hand dispatch, shared projectile targeting/collision,
LbcIndirectKills attribution, existing inventory/crafting flags and source
component importer. Preserve its actual projectile effects once audited.

## 2. Helicopter Boss weapon

Next nested source entry is `lbc:helicopter_boss`; its table gives a
carrot-on-a-stick with model `lbc:helicopter_boss`, tier S, name
`boss_helicopter`, source 30-second recharge lore and author/event text.
Current Java references include LbcTanner's legacy entity-tag exclusions;
these do not register or implement this weapon. It is distinct from the
already qualified Attack Helicopter weapon.

Estimated boundary: medium-to-large entity-backed feature; audit weapon
activation, spawned carrier/hitbox, attacks, ownership, cleanup and cooldown
before selecting implementation scope. Do not pull unrelated Stalin boss logic.
Safe reuse: LbcShooters native projectile helpers, LbcWands target/input helpers,
managed entity lifecycle/epoch cleanup, LbcIndirectKills and existing block
journaling/explosion helpers as the traced source requires.

`lbc:tuntijaa_terra_razor` is already represented by LbcTerra.RAZOR /
`skywars2:terra_razorpine` and its ported table; `lbc:slr` is already implemented.
Neither is a new remaining family. The two candidates above require a bounded
source audit next; no gameplay completeness is claimed here.
