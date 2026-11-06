# Twelve native Hakurei/Touhou items — qualified, installed (6 November 2026)

Ten existing collectible rewards, the bomb and the clock now have real registered
Polymer identities. This completes their native item/default/loot representation;
the already-qualified gameplay stays with the existing event-driven
LbcTouhouCollectibles, LbcHakureiSkill, LbcDanmaku and LbcTimeStop authorities.
There is no new gameplay timer, inventory poll or NBT loop.

| Existing ready loot | Native identity |
| --- | --- |
| `lbc:ported/touhou_collectible_1` | `skywars2:touhou_collectible_1` |
| `lbc:ported/touhou_collectible_2` | `skywars2:touhou_collectible_2` |
| `lbc:ported/touhou_collectible_3` | `skywars2:touhou_collectible_3` |
| `lbc:ported/touhou_collectible_4` | `skywars2:touhou_collectible_4` |
| `lbc:ported/touhou_collectible_5` | `skywars2:touhou_collectible_5` |
| `lbc:ported/touhou_collectible_6` | `skywars2:touhou_collectible_6` |
| `lbc:ported/touhou_collectible_7` | `skywars2:touhou_collectible_7` |
| `lbc:ported/touhou_collectible_8` | `skywars2:touhou_collectible_8` |
| `lbc:ported/touhou_collectible_9` | `skywars2:touhou_collectible_9` |
| `lbc:ported/touhou_collectible_10` | `skywars2:touhou_collectible_10` |
| `lbc:ported/touhou_bomb_item` | `skywars2:touhou_bomb_item` |
| `lbc:ported/touhou_time_stopper` | `skywars2:touhou_time_stopper` |

## Exact source graph and design

The ten ready collectible singleton definitions retain their exact source
functions. Their formerly empty repeating-command-block base references become
direct native item IDs. The canonical bomb similarly replaces its empty snowball
reference; the canonical clock changes only its item ID. No shared empty vanilla
base definition is changed. Bomb and clock ready aliases retain their canonical
references. Names, lore, translated components, tiers, counts, absent fields and
explicit component removals remain authored as before.

The canonical `lbc:touhou_item` pool now references the ten singleton definitions
instead of repeating their functions. All eleven entries remain in their source
order, with weights 800/100/3/1000/1/10/200/100/10/10/1000 (total3234).
The bomb remains the sixth branch, using its existing canonical reference.
No uniform-pool/TMT changes or new reward admission are included.

The common reader takes each carrier's native description ID, preserving block.*
keys for BlockItem carriers as well as item.* keys for ordinary items.
The common deferred prototype reader now supports the native component-patch
removal syntax (`!component`) using vanilla's nullable component initializer.
This preserves the bomb's explicit damage removals without another parser or
source special case. Vanilla repeating-command-block EPIC rarity and default
snowball stack16 remain; the bomb's authored max-stack99 override still applies.
Both snowball identities inherit vanilla happy-ghast-food and PhD bioitem membership
through additive tags; native tag-oracle checks also cover transitive/Fabric membership.

The two active items delegate native use to the SAME bomb/clock handler as the
existing Fabric input callbacks, retaining failed-use guards and exactly one
debit. They do not publish intermediate ordinary snowballs. Legacy raw stacks
continue through the same unchanged callback/service path. Collectible intake
and permanent bonuses remain unsuppressed; bomb/clock launch remains MAG-blocked.
The clock extends the common stop rather than starting a second engine.

## Verification and installation

Qualified test `b4840bdf`, production `e187372a`. Eight real isolated native matches:

- Mode1: native-touhou; hakurei-skill; native-tools; ordinary-accessories; native-melee.
- Mode3: native-touhou; hakurei-skill; native-tools.

The native item oracle compares all12 full outcomes against independently
decoded original vanilla-carrier source definitions, checks /give defaults and
ready leaves, all carrier tags, Polymer roundtrips and native codec persistence.
Every weighted branch is actually resolved, including rare life/full-power
rewards, and64 seeded full-pool outcomes match an independent original-carrier
oracle. Existing Hakurei checks perform real pickups for all10 rewards, failed
then successful capacity admission, unjammed intake in a real MAG field, counter
thresholds and residuals, attributed/nested/same-tick deaths, merged native XP,
TimeStop and nonempty coordinator rewind. Bomb/clock launch and MAG rejection
now execute actual native game-mode item use; source one-unit debits and the
second clock's exact22-second extension are retained. Native tool regressions
exercise the shared reader and prior carrier machinery.

Fresh6 Java families/15 resources; 10256 guarded source inputs and282 author
paths are unchanged. Retained binary links are verified. Each fixture exited
normally; exact snapshots and temporary BedWars maps are restored/removed.
Main and both libs are installed with an exact-hash receipt and rollback;
the main SkyWars server was never started or restarted.

Existing12 item models and RU/EN keys were checked read-only in the author's RP.
No RP/template files changed. Tests do not establish physical-client visuals,
peak TPS, disk-restart persistence or general full-world rewind qualification.
TMT-last, full-IMBA, mixed/type catalogs, modes2/4 and skill2 gates stay unchanged.

Evidence: `build/verification/native-touhou-items-2026-11-06/qualification.json`,
`pool-proof.json`, `presentation-proof.json`, `remaining-catalog-proof.json`,
`main-install.json` and `local-commit.json`.
