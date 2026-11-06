# Native projectile weapons and shared factories — qualified, installed (6 November 2026)

Five existing ready weapons now have registered native Polymer identities:

| Existing ready loot | Native identity |
| --- | --- |
| `lbc:ported/harpoon` | `skywars2:harpoon` |
| `lbc:ported/sea_legend` | `skywars2:sea_legend` |
| `lbc:ported/shulker_bow` | `skywars2:shulker_bow` |
| `lbc:ported/minigun_crossbow` | `skywars2:minigun_crossbow` |
| `lbc:ported/minigun_crossbow_old` | `skywars2:minigun_crossbow_old` |

Their canonical conversions change only the ID. Original weights, qualities,
counts, functions, attributes, enchantments, names/lore/styles and data flags are
unchanged. Existing ready wrappers still reference the canonical definitions.
Raw legacy stacks keep their existing handlers; the current and fast_old miniguns
retain separate source behavior, including their different achievement counters.

## Shared native machinery

LbcProjectileWeaponItems supplies true BowItem, CrossbowItem and TridentItem
subtypes. The already-native Railgun/Shishgun share its BowItem factory; the six
IMBA crossbows inherit its stock crossbow proxy while retaining their own
unchanged continuous/volley/beam/jammer owner. No new ranged damage, homing,
reload, ray, projectile, time or NBT-polling engine was introduced.

The common authored prototype reader now uses exact26.3 vanilla bow/crossbow/
trident properties: durability/enchantability, wood cooking fuel, empty charged
projectiles and trident rarity/attributes/tool/weapon components. Authored fields
override those defaults in the original order, including Sea Legend's complete
replacement of its attribute-modifier component. Canonical Railgun/Shishgun and
IMBA properties now use that same deferred reader; their former hand-built and
duplicate enchantment parsers are removed. This also restores source /give
fields, including Shishgun's InfinityI, exact lore, and vanilla fallback names.

Native charge, ammo selection, shot durability, enchantments, Loyalty return and
Riptide remain Minecraft's own behavior. Existing MinigunCrossbow, ShulkerBow,
trinket/Quiver, charged bow and IMBA services receive the same accepted native
projectile families. The auto-reload tests now actually fire both canonical
native miniguns. The Shulker tests use the canonical native bow for its bow path
and retain raw special-crossbow/Madoka compatibility.

Sixteen additive tag overlays preserve the complete actual vanilla/Fabric/project
carrier graph for all13 shared-factory weapons: the5 new identities, Railgun/
Shishgun and6 IMBA crossbows. All original tag values remain; furnace, enchantment,
native type and PhD admissions follow the same graph as their source carriers.

## Verification and installation

Qualified test `7ea22e68`, production `2bfef5b0`; thirteen real isolated matches:

- Mode1: native-projectile-weapons; native-gear; minigun-crossbow; special-bows;
  imba-crossbows; charged-bows; native-special-artifacts.
- Mode3: native-projectile-weapons; native-gear; minigun-crossbow; special-bows;
  imba-crossbows; charged-bows.

The independent oracle decodes every one of the13 canonical source definitions
with its original vanilla carrier and compares every component and count, all
default prototypes, native subtypes, runtime tags, Polymer and server-codec
roundtrips. Native ready aliases match the same source. Actual inputs exercise
both-hand trident throws, collision, Loyalty pickup, dry/wet Riptide, native
durability and TimeStop; both native miniguns' repeated reloads, custom/tipped/
spectral ammo, selected-only infinite ammo, offhand, Multishot, fireworks, source
counter and TECH policy; native Shulker bullets with stacked trinkets and effects;
IMBA and charged physical/beam/recipe/jammer regressions. Shishgun exercises
both its source InfinityI free shot and an actually charged paid shot after
removing Infinity, preserving the original one-ammo debit assertion. The prior special-artifact
oracle checks the shared reader too. Existing fixture stats now track new native
weapon identities. These are behavioral checks, not a peak-TPS measurement.

Fresh11 Java families/21 resources; 10263 guarded inputs/282 author paths are
unchanged. Retained binary references pass. All test servers exit normally with
exact snapshots restored and temporary BedWars maps removed. Main/both libs are
installed with exact hashes and rollback, without starting or restarting main.
Only one heavy job runs at once; Gradle512MiB/one worker and a2GiB isolated fixture
with an available-RAM gate retain the desktop memory budget.

All13 existing models and RU/EN keys were checked read-only in the author's RP.
No RP/template files changed. No claim is made for physical-client pixels/audio,
peak TPS, disk-restart persistence or broad full-world rewind qualification.
TMT-last/full IMBA/custom-mixed-type catalogs/modes2/4/skill2 gates stay unchanged.

Evidence: `build/verification/native-projectile-weapons-2026-11-06/qualification.json`, `profiles.json`,
`canonical-definition-proof.json`, `presentation-proof.json`, `tag-source-audit.json`,
`remaining-catalog-proof.json`, `main-install.json` and `local-commit.json`.
