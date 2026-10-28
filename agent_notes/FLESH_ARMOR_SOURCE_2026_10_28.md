Current author decisions and implementation: [Flesh armor port](FLESH_ARMOR_PORT_2026_10_28.md). Use x2, no MAG/TECH suppression; this supersedes the preparation questions below.

# Flesh armor source audit — 28 October 2026

Read-only audit; no implementation/runtime qualification. Reviewed project AGENTS,
agent_notes/AGENTS.md and RESOURCE_PACK.md. No existing author notes changed.
S = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc.
RP = C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8.

## Complete passive and lifecycle

- S/function/items/changed_inventory/sel.mcfunction:148–150 counts equipped
  armor.* with raw custom_data flesh:1; >=4 calls armor/flesh_set_bonus_add,
  <=3 calls armor/flesh_set_bonus_remove. Inventory-held pieces do not count.
- Those two single-command functions add/remove MAX_HEALTH modifier
  minecraft:44a8aa48-d53d-42c0-b229-4557d3c581b1, amount 2,
  add_multiplied_total. This means **x3**, not x2; RP lore says +100%%.
  Preserve this discrepancy for explicit author decision. Full source pieces
  add 42HP to vanilla20 before the set multiplier: 62 ->186HP, absent other
  modifiers. A partial set only gets its equipment HP additions.
- S/advancement/penis.json is inventory_changed -> sel. It excludes spectator,
  nexus and imprinted. S/function/players_tick.mcfunction:69 revokes this
  advancement each tick; leave_death:121 also revokes it. This is an inventory
  event passive, not a periodic healing effect or hurt proc.
- S/function/resets.mcfunction:112 explicitly removes the fixed modifier;
  :127 invokes sel again. start:1, other/idgive:5 and other/new_game/click:11
  invoke resets; resets:222 invokes leave_death. No additional Flesh-specific
  callback, score, delayed timer, particle, damage/healing command, item modifier,
  load-time repair or death-removal command was found by all-source Flesh/ID
  search. load creates generic health objectives only. Vanilla equipment
  replacement/breakage changes per-piece attributes and the next inventory event
  reevaluates the complete set. Repeated add uses the same ID, never stacks.
- No noMAG/noTECH test, stopper/stopper_magic predicate, or jammer dependency
  occurs in the Flesh branch/functions. Classification as biological, magical
  or technical cannot be inferred from the literal source; suppression policy
  requires author ruling. Do not silently reuse another armor's policy.
- Adding maximum health does not source a heal. Removing equipment/set bonus
  lowers the maximum; native current-health clamping and synchronization need
  explicit runtime checks, especially removing one piece above the new maximum,
  signed/negative modifiers, death/respawn and reconnect. Do not invent ratio
  preservation, bonus current HP, refill, or custom health state from this source.

## Items, acquisition and original advancement

Seven tables: neko_set_flesh/{helmet,chestplate,leggings,boots}.json plus
neko_set_flesh.json, neko_set_flesh_pre.json and neko_set_flesh_full.json.
Root chooses one of four leaves, equal default weight, one roll. Pre rolls root
five times (duplicates possible). Full has four independent one-roll pools,
one per piece. nekolucky_items references pre; 6_sets references full. These
are original source pool links, not evidence that either broad native pool is live.

All leaves are corresponding leather armor, max_damage6000, item_model
lbc:armors/flesh_<piece>, tooltip_style lbc:tier_c, equippable matching slot and
asset_id lbc:flesh. Exactly one MAX_HEALTH add_value equipment modifier each:

| Piece/slot | Amount | Original ID |
|---|---:|---|
| helmet/head | 8 | hdtfbvjd6vgfd |
| chestplate/chest | 16 | getbub7kcrdtkjib6itgalth |
| leggings/legs | 12 | genehverthbvdfcgjx_galth |
| boots/feet | 6 | gevrf7nik7mutcrftkjujukhjth |

No armor/toughness modifier is supplied. Preserve signed native attribute math,
all original IDs/components and custom_data {transformable:1,<piece>:1,flesh:1}.
Names use flesh_<piece>, red, italic:false. Lore has exactly two lines: first
the ordered aqua italic:false components type + slot key (helmet/chestplate/
leggings/boots), then aqua italic:false flesh.lore. Do not append status lore.

Normal source transformables_<slot>.json includes that exact Flesh leaf; native
LbcBolts NORMAL slot rules at74–77 retain these tables. ULTRA has the concrete
flesh -> neko_set_flesh family rule at116 (source ultra_reforge:40). The native
reforgeInput path at489 and live refilling path at542 are shared acquisition;
availability must be qualified through actual consumed-donor outcomes, not only
direct debug loot. No new broad LuckyBlock pool is necessary.

S/advancement/true_advancements/armor/flesh.json: original goal frame, visible,
toast/chat true, parent armor/nano, leather chestplate Flesh-model icon.
player_killed_entity requires non-spectator wearer with flesh:1 in all four
equipment slots and victim type minecraft:player. Reward is shared
swrg_kit_integration/gain/levelup. Armor/antimagic is its child. Existing GUI
page/select8000 uses this unlock at slot14 and item_modifier/swrg_unlocks/flesh.

## Presentation and integration boundary

RP assets exist: assets/lbc/items/armors/flesh_<piece>.json -> corresponding
assets/lbc/models/item/armors model -> minecraft:item/armors/flesh_<piece>
textures (both layer0/layer1); assets/lbc/equipment/flesh.json uses humanoid
lbc:flesh and humanoid_leggings lbc:flesh_leggings, both textures present.
No RP write or replacement asset is needed. Existing minecraft/lang keys:

- RU names: Шлем из плоти; Нагрудник из плоти; Поножи из плоти; Ботинки из плоти.
- EN names: Flesh Helmet; Flesh Chestplate; Flesh Leggings; Flesh Boots.
- RU flesh.lore: `Бонус полного сета - увеличиват здоровье на 100%%`.
- EN flesh.lore: `Full set bonus - increases health by 100%%`.
- Advancement title keys lbc.adv.armor.flesh: RU `Живой!` +19 trailing spaces;
  EN `Alive!` +18 trailing spaces. Preserve raw source strings.
- Advancement lore RU: `Экипируйте полный сет брони из Плоти и убейте в ней игрока!`;
  EN: `Equip full set of Flesh Armor and kill another player!`.

Current native Flesh references found only LbcDamage identity and LbcBolts
reforge rule; LbcArmorSets has no Flesh passive. Shared LbcArmorSets
inventoryChanged/tick/loaded/forget/stop and its existing service integration
are the appropriate lifecycle seam. Keep equipment attributes in native vanilla
equipment processing and signed AttributePolicy; use one fixed native modifier
and idempotent reconciliation, with exact removal on all relevant transitions.
Do not route a non-damage passive through the shared hurt criterion or create
another generic health tracker. Shared readiness/catalog packaging, native
reforges, imported original advancement and health synchronization need proof
alongside actual source-item attributes. This audit makes no build, install,
physical-client visual or runtime-health claim.
