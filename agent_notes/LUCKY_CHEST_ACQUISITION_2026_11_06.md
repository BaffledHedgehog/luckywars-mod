# Lucky SkyWars chest acquisition — qualified infrastructure, 6 November 2026

The existing MapManager chest path now selects the authored mode family:

| Mode | Default / middle / center | Upgrade |
| --- | --- | --- |
|1 ordinary SkyWars | matching `_0` table | `swrg:chests/center_0` |
|2 Lucky SkyWars | matching `_lb` table | `swrg:chests/center_lb` |
|3/4 BedWars | no SkyWars chest fill | no SkyWars chest fill |

All six table IDs are explicit in one selector. Bootstrap supplies the current
GameConfig through an IntSupplier, so settings changes need no reload or copied
mode state. The old constructor remains compatible with independent map-catalog
readers. This is no new service, scheduler, polling pass or command interpreter.

Initial map fill, Supernova/Balance renewal, ordinary phase-event refill/upgrade,
remaining-life island restore and Madoka region restore already share the same
MapManager owner. They now select the same mode-aware family. MatchEvents no
longer repeats container clearing through command/NBT execution before calling
the native clearContent path. Source permanent promotion of default/middle
anchors to the center category happens in that same native marker pass.
Non-marker transports remain excluded. BedWars is rejected before any marker,
block, inventory or category write. No per-tick source JSON/NBT reading was added.

## Complete finite source closure

All62 actual `_lb` dependency tables were traced and checked. Their entry and
pool weights, chances/conditions, providers, enchantments, context functions,
names/lore/styles and other components are preserved except these explicit
semantic adaptations:

- Three source lucky-block quantity profiles now issue the existing registered
  `skywars2:lucky_block`, with their exact original authored components, instead
  of placing a red mushroom building block. No second block/item implementation
  or new model allocation was introduced. Legacy block-state component values
  are preserved; real BlockItem placement resolves the actual registered block.
- Twenty-three quantity profiles retain the exact original number providers,
  slots, operations and random calls. Their arbitrary old technical modifier IDs
  become the existing `minecraft:count` marker. The shared post-fill normalizer
  now recognizes them, sets the actual stack count and removes only this marker,
  retaining legitimate attributes. Keeping quantities after native slot filling
  preserves the source one-slot profiles and original random layout, rather than
  making vanilla split the quantities across extra slots during generation.
- Locator no longer reads uninitialized `swrg:lang` storage. Source RU/EN
  initializers establish yellow/nonitalic Locator; the existing author-pack
  `swrg.item.compass` key supplies those same localized values directly. Existing
  source lucky names/lore keys and models were checked read-only. No RP edit.

The source `_lb` tables contain vanilla equipment plus the three lucky-block
profiles; this finite container closure is distinct from the complete reward
graph reached when the player later opens a Lucky block. The latter still has
the full-TMT dependency. No public root, mode, kit or reward gate was opened.

## Actual qualification and boundaries

Test `5d18fc89`, production `f5a2420c`. Seven isolated matches passed on this exact
test archive: Lucky acquisition, ordinary chests and both native Lucky-block suites in actual
SkyWars1; Lucky acquisition, BedWars basic and lifecycle in actual BedWars3.

The acquisition checks temporarily select mode2 only synchronously inside an
already admitted1/3 match and restore the real configuration before yielding.
This tests finite content routing, not a successful public mode2 startup. All62
dependencies genuinely load. All23 quantity profiles roll32 spread-out fixed
seeds; actual resulting counts match source post-fill semantics and preserve
other components. Three native block profiles have independent original-carrier
component oracles (only the base ITEM_NAME follows the new registered block;
visible authored name stays exact), Polymer/server-codec roundtrips and real
native placement with one-item debit. The shared registration also restores
the exact original compostable components for NORMAL/NEKO/LOLI using vanilla
context-provider builders; the full component oracle remains strict. Actual start-style/repeated/upgrade/event,
remaining-life island and Madoka fills are checked, including permanent center
promotion, local radii, non-marker exclusion and BedWars no-write controls.
Real map chest snapshots, original marker tags, test actors, player/profile/
statistics and core state are restored. Each child exits normally; fixture and
auxiliary snapshots are exact and temporary BedWars maps are removed.

Only6 fresh owned Java families and24 resources are overlaid on each qualified
parent archive. 10272 other source inputs and282 author
paths remain byte-exact; protected Academy work is not shipped. Every archive
entry and retained-class linkage is checked. Both build outputs and the stopped
main server receive the exact qualified bytes with rollback, no restart or push.
Gradle512MiB/one worker/no persistent daemon and native fixture2GiB run sequentially
with free-RAM checks. User Minecraft/Velocity/editor heaps and RP stay untouched.

Modes2/4 still await complete startup/reward qualification; full TMT stays last.
Full custom/mixed/type pools, dependent kits/skill2, protected Academy, general
world rewind/release, physical-client/peakTPS/disk-restart validation remain
separate. This is not a full mode2 release or a measured TPS improvement.

Evidence: `build/verification/lucky-chest-acquisition-2026-11-06/qualification.json`, `main-install.json`,
`closure-proof.json`, `canonical-definition-proof.json`, `presentation-proof.json`,
`package-proof.json`, `linkage-proof.json`, sealed actual native logs/results and
`local-commit.json`. Source references: original SWRG `loot_table/chests/` and
`function/game/chests/set_count.mcfunction`, `function/core/lang/{ru,en}.mcfunction`.
