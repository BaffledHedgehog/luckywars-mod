# Skills15/17 bounded coverage audit — 2 November 2026

Read-only source review, except this note. No build, native process, resource edits or gameplay implementation. Scope excludes the separately assigned status and Wands/VoidArcanum/Homing/Hedgehog/ChargedSwords/FevgaleaStar work and root-owned native relationships/polymorph. This is a concrete remaining-owner list, not a new requirement to clone every private field or native brain/path cache.

Legacy root below means `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

## Confirmed remaining gaps

| Owner | Exact admitted source equivalent | Missing reconstruction | Native NBT + loaded sufficient? |
|---|---|---|---|
| `LbcStand.fists`, `FistDisplay` | `items/oraora/projectile_create.mcfunction:1,3,4`: `oraora_fireball,slowraycast` displays | `Fist` owner/level/epoch/variant/direction/age, display identity, membership. Position and rotations native. `FistDisplay.owner` needs its own factory. These displays perform collision and damage in `tickFist`; they are main gameplay carriers. Do not copy player `bursts`/`retained` authority merely because a fist is copied. | No. `loaded` discards a tagged fist absent from `fists`. |
| `LbcGodHedgehog.actors` | `other/god_hedgehog/hedgegod_eater_tick_spawn_new.mcfunction:1`, `spawn_dim_scrap.mcfunction:1`, `spawn_white_arrows1at.mcfunction:1`, `hedgegod_white_arrow_shoot.mcfunction:3`: eater, floating white arrow and raycastarrow all explicitly `slowraycast` | `Actor.encounter/kind/point/direction/age`, correct Visual/RayBody factory and actors membership. Eater/white display and ray marker are gameplay carriers; warning is a regenerated visual child. Current `cloneProtected` blanket-excludes both native carrier classes. Existing encounter clone covers Boss and blastSources only. | No. Loaded actor has no encounter/member association and is rejected. Keep external original encounter/cause unless copying its boss in that operation; no shared mutable Actor. |
| `LbcBabylon.projectiles` | Native projectile families are admitted even without `slowraycast`: Arrow/ThrownTrident/FireworkRocketEntity | Exact private `BabylonArrow`, `BabylonTrident`, `BabylonRocket` factories; arrow `transportAmp`, `blockEffectDone`; live `retired` state and membership. NBT potion contents do not replace final `transportAmp` used by block impact. | No. Public EntityType creates vanilla classes and helper checks exact class. Correct subclass with owner/epoch can be registered by `loaded`, but construction and arrow private state still need owner image. |
| `LbcDuplicating.markers` | `workingitems/awaken_tuntija_wands/avarice/click2.mcfunction:1`, `click2_upgraded_default.mcfunction:1`, `click2_upgraded_expand.mcfunction:1`, `click2_upgraded_black_hole.mcfunction:1`, `click2_upgrade2.mcfunction`: `dublicating`, `dublicating_2`, `dublicated_hole`, all `rare,slowraycast` | Virtual `MarkerState`: owner/level/epoch, point/direction/mode/lifetime/age/cancelled/retainedOwner/erosionSeed, independent owner work (LiveShuffle/HoleTerrain) appropriate to the current source slice; visual Handle regenerated. There is no native body to discover. Current mode4 alone is not the entire gap: modes1/2 and spray centers also have explicit source admission. | No native body/NBT. `Seed` and `rnd_clone` short-lived work must be reviewed as source admitted gameplay work, rather than blanket-excluded as implementation queues; legacy `tick.mcfunction`/`tick_2.mcfunction:1` explicitly create `slowraycast` `rnd_clone`/`rnd_clone_2` markers. |
| `LbcSheep.holes` | Current source faithfully tags the marker `sheep_black_hole,slowraycast,magic,rare` at `LbcSheep.java:236` | `Body` factory; Hole entity/owner/cause/epoch/level/point/velocity/tilt/terrain association and holes membership. Visual Handle regenerated; erosion private state belongs to hole owner. Ordinary/academy polymorph is separately root-owned. | No. Private Body differs from public Marker; `loaded` rejects unregistered managed bodies. |

## Native sufficient / exclusions

`LbcPursuitItems` is only item registration: BOUNCING_POOP delegates to `LbcBouncingPoop`, DEUS to Wands. It is not a separate actor owner.

`LbcBouncingPoop` is a plain native ItemDisplay with persistent CompoundTag attachment DATA containing author/epoch/order/age/mega/team; direction is native rotation. Its source `swrg_kit_integration/items/govno_raskid_def.mcfunction:1` is `govno_poprigun,slowraycast,player`. Native NBT attachment copy plus `loaded` already rebuilds actors membership and source binding. Admit it as a main gameplay display and verify attachment serialization, rather than demand a redundant private-flight copier. Mutable attachment independence follows NBT reconstruction; source mutates through fresh `copy()` updates.

`BlackHoleVisuals` handles and per-viewer ItemDisplayElement slots are presentation. They must not be cloned independently. Actual black-hole owners here are Duplicating, Sheep, and separately assigned VoidArcanum. Do not duplicate every shader slot.

GodHedgehog `Laser` queue is encounter attack work; it is not demonstrated by this audit to be an independently admitted slowraycast body. Do not demand an independent virtual clone merely from its name. Boss-owned attack reconstruction remains the encounter owner responsibility. Native TNT blast sources already have the encounter adapter path.

Native vanilla transient navigation/collision/runtime need only reproduce legacy native NBT semantics; no exact brain/path reconstruction requirement is introduced. Custom registered Projectile subclasses remain covered by future instanceof Projectile admission, but exact factory/private state still requires the owner where not serialized. NPC/player authority forms and visual children remain protected.

## Reviewed code anchors

`src/main/java/me/skywars2/game/LbcStand.java:92,110,445,474,513`; `LbcGodHedgehog.java:74,110,254,285,295,427`; `LbcBabylon.java:125,150,176,182`; `LbcDuplicating.java:38,51,106,302,522,621`; `LbcSheep.java:75,85,236,344`; `LbcBouncingPoop.java:33,71`; `CloneEncounterStates.java:28`; `LbcCloneSkills.java:135`.
