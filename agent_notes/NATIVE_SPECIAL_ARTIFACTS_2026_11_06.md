# Six native special artifacts — qualified, installed (6 November 2026)

Six existing ready artifacts now have registered native Polymer identities. Their
existing gameplay owners stay shared: Wands.nativeTool and WitchIndolence, Zaika's
inventory/loot predicates, DeathCompass, Totems and passive consumption.
No new tick task, NBT polling, ability timer or second damage engine is added.

| Existing ready loot | Native identity |
| --- | --- |
| `lbc:ported/admin_zaika` | `skywars2:admin_zaika` |
| `lbc:ported/indolence_claw` | `skywars2:indolence_claw` |
| `lbc:ported/death_compass` | `skywars2:death_compass` |
| `lbc:ported/devil_totem` | `skywars2:devil_totem` |
| `lbc:ported/infinite_totem` | `skywars2:infinite_totem` |
| `lbc:ported/canned_food` | `skywars2:canned_food` |

## Canonical items and native behavior

All six canonical conversions change only the item ID. Every count, function,
weight, condition, name, lore, tier, enchantment and data flag stays unchanged.
The four referencing ready wrappers and the existing canonical Infinite Totem
alias still share their definitions. In particular, Canned Food still yields16
nonedible units; the default /give item is one unit with the same components.
Its passive eating continues through the existing inventory owner.

The common deferred prototype reader now accepts both `item_model` and
`minecraft:item_model` when choosing its native effective model. The source
Infinite Totem uses the unprefixed spelling; its /give and loot prototypes now
retain the exact existing `lbc:infinite_totem` model. The same factory retains
vanilla totem UNCOMMON rarity, stack1 and the actual DEATH_PROTECTION component,
and Recovery Compass UNCOMMON rarity. Registry-bound Infinity still binds after
registries load. No custom survival or substitute Resistance implementation exists.

Native Minecraft chooses and debits the held totem, applies its health/effects
and emits its used_totem criterion/stat/animation. Existing Totems handles its
source refunds and Devil burst, hostile skeletons, lava and credited delayed
death. Refunds keep the actual consumed identity and all modifications; bounded
high-level debt handling is retained. Existing raw vanilla aliases remain valid.

Zaika and Indolence Claw reuse Wands.nativeTool. The existing Indolence owner
retains authority admission for BOTH hands, cooldown, global finger count,
patterns, WILD damage, MAGIC suppression and pause/rewind policy. Zaika retains
its inventory effects and weighted block-drop predicates, without invented flags
or activation behavior. Death Compass remains a real Recovery Compass proxy;
the original scheduled targeting, borrowed death, Warden and model updates remain.

Three additive tag edits preserve every actual direct vanilla/Fabric/project
carrier membership: compass, durability enchantment and PhD bioitems. All original
values remain. Native fixtures assert the complete runtime tag graph too.

## Exact checks and installation

Qualified test `986e56fd`, production `c5d5b14e`; eleven real isolated native matches:

- Mode1: native-special-artifacts; totems; death-compass; witch-indolence;
  passive-consumables; zaika-passive; native-touhou.
- Mode3: native-special-artifacts; witch-indolence; passive-consumables;
  native-touhou.

The six-item independent oracle checks all source/default/ready components,
canonical counts, Polymer roundtrips, server codecs and complete carrier tags.
Real lethal hits in both hands consume the native Infinite Totem and publish
exactly one complete same-identity refund while applying native survival effects.
The full Totem integration covers hand precedence, missing flags, high/IntMax
refunds, WILD damage, hostile actors, native criterion and credited final death.
Its source-stat assertion tracks the actual native consumed item. Indolence
checks alternate actual native offhand use and the existing mainhand callback;
all source authority/pattern/clone/terrain/cadence guards remain. Existing Compass,
Canned Food and Zaika integrations run against actual native source issuance.
The prior12 Touhou/default/weighted-pool oracle guards the shared prototype reader.

Fresh10 Java families/9 resources; 10261 guarded inputs/282 author paths remain
unchanged. Retained binary references pass. Each fixture exited normally with
exact snapshots restored; temporary BedWars maps were removed. Main and both libs
are installed with exact hashes and rollback; main SkyWars was never started or
restarted. One heavy job runs at a time; Gradle512MiB/one worker and a2GiB fixture
with an explicit available-RAM gate preserve the desktop memory budget.

Six existing RP models and RU/EN keys were checked read-only. No RP/template
files changed. These checks do not establish physical-client pixels/audio,
peak TPS, restart persistence or broad full-world rewind qualification.
TMT-last/full IMBA/custom-mixed-type catalogs/modes2/4/skill2 gates are unchanged.

Evidence: `build/verification/native-special-artifacts-2026-11-06/qualification.json`,
`canonical-definition-proof.json`, `presentation-proof.json`, `tag-source-audit.json`,
`remaining-catalog-proof.json`, `main-install.json` and `local-commit.json`.
