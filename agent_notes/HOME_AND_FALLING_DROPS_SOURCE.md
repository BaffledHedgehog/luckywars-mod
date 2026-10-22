# Home and falling drops - source audit (22 October 2026)

Read-only audit; no gameplay implementation, builds, runtime changes, installation, commits or RP writes. This file is the sole write. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Actual RP root is `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`, overriding historical 26.3(1) notes for this audit.

## Dispatch and exact tokens

`function/drops/specialdrops.mcfunction:12-14` executes home, normal falling, old falling in that order, each at the dropped item and without replacing executor. This follows zombie TNT and precedes infinity/forest. Normal home/falling tokens actually live in `loot_table/barriersnottraps.json`, IMBA in `imba/barriersnottraps.json`; neither `traps.json` nor `imba/traps.json` defines them. Old falling is in `legacy_luckyblock.json`; no old home token was found. Do not silently decorate IMBA or old variants. Exact relevant entry objects follow (all count 1):

`loot_table/barriersnottraps.json`:
```json
{
  "type": "minecraft:item",
  "weight": 10,
  "quality": 5,
  "name": "minecraft:repeating_command_block",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_f",
        "minecraft:item_model": "lbc:drop_not_trap"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "drop_netherite",
          "italic": false,
          "color": "yellow"
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        {
          "translate": "trap_desc",
          "italic": false,
          "color": "white"
        }
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{lightning:1,drop:1}"
    }
  ]
}
```

`loot_table/barriersnottraps.json`:
```json
{
  "type": "minecraft:item",
  "weight": 10,
  "quality": 9,
  "name": "minecraft:repeating_command_block",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:tooltip_style": "lbc:tier_b",
        "minecraft:item_model": "lbc:drop_not_trap"
      }
    },
    {
      "function": "minecraft:set_name",
      "name": [
        {
          "translate": "drop_sethome",
          "italic": false,
          "color": "yellow"
        }
      ]
    },
    {
      "function": "minecraft:set_lore",
      "mode": "replace_all",
      "lore": [
        {
          "translate": "trap_desc",
          "italic": false,
          "color": "white"
        }
      ]
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{sethome:1,drop:1}"
    }
  ]
}
```

`loot_table/imba/barriersnottraps.json`:
```json
{
  "type": "minecraft:item",
  "weight": 0,
  "quality": 5,
  "name": "minecraft:repeating_command_block",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {}
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{lightning:1,drop:1,imba:1}"
    }
  ]
}
```

`loot_table/imba/barriersnottraps.json`:
```json
{
  "type": "minecraft:item",
  "weight": 0,
  "quality": 9,
  "name": "minecraft:repeating_command_block",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {}
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{sethome:1,drop:1,imba:1}"
    }
  ]
}
```

`loot_table/legacy_luckyblock.json`:
```json
{
  "type": "minecraft:item",
  "name": "minecraft:barrier",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1
    },
    {
      "function": "minecraft:set_components",
      "components": {}
    },
    {
      "function": "minecraft:set_custom_data",
      "tag": "{lightning_old:1,drop:1}"
    }
  ]
}
```

## Home source

`function/drops/sethome.mcfunction:1?8`: increment global `*id.cloud lbcID`; summon marker at floor(x,y,z)+(.5,.5,.5), tags `hometp.id`, `rc_1sec`, `magic`; assign its score using nearest marker selector; spawn `lbc:sethomer`; write current ID into nearest item within 1 with custom_data `tpid:0`. This is a nearest-selector source race, not an explicit returned-item handle. The mirror is transferable; IDs bind mirror to location, not original player UUID. No duration or owner-death expiration is set.

`function/tick1s.mcfunction:3` dispatches `rc_1sec` entities except nexus/imprinted; `raycasters_1s.mcfunction:8` invokes `other/mirror`. That routine every second emits 5 force end rods at +1Y; strict-sets light-blue stained glass at marker Y?1; strict-clears +1 through +2Y; adds chunk forceload at marker. The fractional marker Y=.5 means marker-floor support block is original drop floorY?1; destination Y is original floorY+1.5. No automatic restoration or per-marker release occurs here.

`workingitems/spell1_default.mcfunction:40` and `spell1_default_left.mcfunction:15` route `supersethomer:1` to `teleporthome` / `_left` unless stopper_magic marker within 20. Each emits 50 force portal particles at +.81Y, enderman teleport sound volume/pitch 1 to all; reads held main/offhand `tpid` into player's `lbcID`; iterates home markers and teleports player to marker +1Y when nearest-marker ID at that marker equals player's ID. No mirror consumption/cooldown is added in these functions; absent marker yields effects without teleport. Selectors operate in execution dimension: no cross-dimensional destination lookup is specified.

Magic deletion applies: `other/stopper_magic.mcfunction:5` calls `stopper_magic_destroy` on magic entities within 20 (witch particle then kill); `other/artifact/nomagic.mcfunction:7` kills magic within25; danmaku bomb routines also select eligible magic raycasters. End resets/new-game click remove all forceloads in vanilla dimensions. Marker-specific lifetime is absent, but game cleanup/magic suppression must be kept separate from a claimed permanent lifetime. Removing marker itself does not release its chunk ticket in mirror routine.

Exact produced mirror loot:
```json
    {
    "pools": [
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:carrot_on_a_stick",
                    "functions": [
                        {
                            "function": "minecraft:set_name",
                            "name": [
                                {
                                    "translate": "magic_mirror",
                                    "color": "aqua",
                                    "italic": false
                                }
                            ]
                        },
                        {
                            "function": "minecraft:set_lore",
                            "mode": "replace_all",
                            "lore": [
                                {
                                    "translate": "magic_mirror.lore.1",
                                    "color": "white",
                                    "italic": false
                                },
                                {
                                    "translate": "magic_mirror.lore.2",
                                    "color": "white",
                                    "italic": false
                                }
                            ]
                        },
                        {
                            "function": "minecraft:set_components",
                            "components": {
                                "minecraft:tooltip_style":"lbc:tier_d",
                                "minecraft:item_model": "lbc:sethomer"
                            }
                        },
                        {
                            "function": "minecraft:set_custom_data",
                            "tag": "{magicwand:1,supersethomer:1,tpid:0}"
                        }
                    ]
                }
            ]
        }
    ]
}
```

## Falling source contract

`drops/fallingblock.mcfunction`: smoke at exact token position +7Y, spread (.3,6,.3), speed0 count300 **normal**; strict air fill from token block through +15Y inclusive (one column, 16 cells); align only XZ then summon at floorX+.5, original fractionalY+14, floorZ+.5. `fallingblock_old`: identical smoke with **force**, regular air fill, summon at original fractional X/Z and Y+14. Both load netherite BlockState and `Time:1`; neither supplies velocity, HurtEntities, FallHurtAmount/Max, DropItem, CancelDrop, lifespan or ownership.

Both schedule same `lbc:drops/fallingnetherite 25t` without append: default replace, global callback identity. Later activation postpones the single pending callback. Callback scans all matching loaded falling blocks in scheduled execution dimension; it has no source-token proximity, ID, age, author or original-position filter, and summons lightning at each entity's current position -4Y. It does not kill entity or force lightning after landing: entities already landed are absent. Source legacy predicate is `BlockState:{Name:"minecraft:netherite_block"}`; do not copy that predicate into 26.3 serialization unchanged. Scheduled functions do not preserve token executor/position/dimension. Native source schedule uses server command context, normally overworld, so cross-dimensional throws need explicit policy rather than assumed same-world capture.

Strict versus regular fill differences include normal neighbor/update/container behavior; reuse qualified native fill gates, not unconditional custom clipping. Source attribution is missing. Standing user requirement in agent_notes/AGENTS.md demands author inheritance for delayed consequences: retain thrower/match epoch through support removal, falling entity, lightning/fire/chain effects, suffocation and any subsequent native terrain consequence, without inventing author lifetime. A global scan can hit unrelated native netherite entities: decide attribution from each entity's retained cause; a callback's most recent thrower is not automatically every entity's author.

## Native 26.3 API evidence

Read local source archive `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, members `net/minecraft/world/entity/item/FallingBlockEntity.java`, `net/minecraft/world/level/block/state/{BlockState,StateHolder}.java`, `net/minecraft/server/commands/ScheduleCommand.java`.

FallingBlockEntity public `(EntityType<? extends FallingBlockEntity>, Level)` constructor + ordinary entity load path allow native state read. `readAdditionalSaveData(ValueInput)` loads BlockState via BlockState.CODEC (fallback sand only on missing/invalid data), Time default0, HurtEntities default is ANVIL tag, FallHurtAmount default0, FallHurtMax40, DropItem true, CancelDrop false. Thus loaded netherite has **HurtEntities=false and FallHurtAmount=0**; never invent crushing damage or call `setHurtsEntities(2,40)` merely because cages use anvils. `setHurtsEntities(float,int)` explicitly enables damage and changes amount. `fall(Level,BlockPos,BlockState)` also removes source world block, so it is not an equivalent summon API for this trap.

Native tick increments time, applies gravity .04, moves, applies block effects/portals, and multiplies velocity by native air drag. Landed entity attempts real block placement with replacement/survival/support conditions; otherwise ordinary item ejection is governed by DropItem and ENTITY_DROPS. Successful placement discards entity; failure/unplaceable support can eject netherite item. Out-of-height after time>100, or time>600, can eject and discard. No custom landing drop, vanish-on-landing, extra TTL or forced damage is supported by source.

`addAdditionalSaveData(ValueOutput)` writes BlockState codec, Time, DropItem, HurtEntities, FallHurtAmount, FallHurtMax, optional TileEntityData, CancelDrop. BlockState.CODEC accepts identifier string for default states or full state map keyed lowercase `id` and lowercase `properties` (StateHolder constants), and encodes default state as string. Example `BlockState:"minecraft:netherite_block"`, not legacy `{Name:...}`. Read API `getBlockState()` supplies reliable block identity for delayed scan; source entity NBT cannot be assumed stable across versions.

## RP evidence and limits

Actual RP is read-only; existing normal tokens use `lbc:drop_not_trap`, mirror `lbc:sethomer`, original tier styles. Current RP item definitions and all matching language values are recorded below. No added translations/models are indicated by this audit. Explicit component italic=false is retained; absent styles on old/IMBA remain absent. Client visual rendering has not been tested.

`assets/lbc/items/drop_not_trap.json`:
```json
{
    "model": {
        "type": "model",
        "model": "lbc:item/drop_not_trap"
    }
}
```

`assets/lbc/items/sethomer.json`:
```json
{
    "model": {
        "type": "model",
        "model": "lbc:item/sethomer"
    }
}
```

`assets/minecraft/lang/en_us.json`:
```json
{
  "drop_netherite": "Drop: Netherite Block",
  "drop_sethome": "Drop: Magic Mirror",
  "trap_desc": "Active Drop. Throw the item to activate.",
  "magic_mirror": "Magical mirror",
  "magic_mirror.lore.1": "Teleports the owner to",
  "magic_mirror.lore.2": "the mark of the mirror (Right Click)"
}
```

`assets/minecraft/lang/rpr.json`:
```json
{
  "drop_netherite": "Дроп: Незеритовый блок",
  "drop_sethome": "Дроп: Волшебное зеркало",
  "trap_desc": "Активный Дроп. Для активации выбросте предмет.",
  "magic_mirror": "Волшебное зеркало",
  "magic_mirror.lore.1": "Телепортирует владельца на",
  "magic_mirror.lore.2": "метку зеркала (ПКМ)"
}
```

`assets/minecraft/lang/ru_ru.json`:
```json
{
  "drop_netherite": "Дроп: Незеритовый блок",
  "drop_sethome": "Дроп: Волшебное зеркало",
  "trap_desc": "Активный Дроп. Для активации выбросте предмет.",
  "magic_mirror": "Волшебное зеркало",
  "magic_mirror.lore.1": "Телепортирует владельца на",
  "magic_mirror.lore.2": "метку зеркала (ПКМ)"
}
```

## Author clarification (user, 22 October 2026)

> ну по задумке блок падает и при падении молнию вызывает. В датапаке таймер это костыль вообще.

Target falling-drop behavior therefore uses physical landing to trigger lightning; source shared timer/global scan is historical evidence rather than desired implementation. Root interpretation communicated to user is a bolt at the impact point. Source -4Y callback offset is a timer approximation and should not be silently applied as an impact coordinate. Work is bounded to the two falling drops first; home remains audit-only for a later pass.

Native landing hook: in FallingBlockEntity.tick, after movement/block effects/portal handling, server branch detects `onGround() || isStuckInWater`. For netherite, concrete/water case is false. The landing branch sets bounce velocity, skips processing while current block is MOVING_PISTON, and can discard synchronously on successful placement or item fallback. A tail hook that requires `isAlive()` would miss ordinary successful landings. Conversely generic `isRemoved()` also catches void/time expiry. A retained causal record must remain readable through synchronous discard, and once-only impact should be identified by actual ground contact in the relevant tick/branch rather than removal alone. If choosing post-native-tick dispatch, capture pre-tick eligible cause and after tick establish ground contact, match/epoch validity and not-already-fired; preserve payload even if ordinary landing discarded the entity. Moving-piston waiting needs an explicit once-only contact policy rather than per-tick repeat. Avoid suppressing native placement, fallback loot or damage defaults.

## Decisions before implementation

The author clarification replaces the source timer/global scan with physical landing for the two ported falling tokens. Native unrelated netherite entities must not inherit a trap author or produce trap landing bolts. Preserve native non-crushing damage and normal landing item/block outcomes. Home includes the produced working mirror and marker renewal, magic suppression, chunk ticket lifecycle, transferable ID and match cleanup; porting only dropped token is incomplete. Any deliberate fix for source nearest-item race or unsafe global forceload lifetime should be documented separately. No gameplay qualification is claimed by this source audit.
