# Developer mode, content gallery and native map authoring

User request, 1 November2026: an operator-only gallery of MOD content, a central
debug perk menu, persistent `/developer 1|0`, direct map saving and native tools.
The author explicitly excludes vanilla novelty items from the gallery.

## Commands and use

- `/gallery`: create the persistent stone-brick showroom under the caller on
  first use; subsequent calls refresh the owned showroom at its saved position.
  Fixed frames carry distinct ready mod item/component identities. Right-click
  copies one item for an operator; the central ender chest opens the original
  SkyWars menus in a separate operator debug context, usable during a match.
- `/developer 1`: enable global practice, mark the caller as developer, select
  Creative and give `skywars2:dev_tools`. One player can start; natural completion
  is disabled and participant deaths retain lives. `/developer 0` restores normal
  rules. Persistent configuration is separate from the existing player tag.
- `/mapsave tools` gives the same paper tool; `/mapsave` opens its native menu.
- `/mapsave pos 0` and `/mapsave pos 1` select inclusive block corners in the
  current dimension. `/mapsave saveskywars <name>` or
  `/mapsave savebedwars <name>` captures non-air terrain, container/block-entity
  NBT and non-player entities, including map markers, yaw and numeric teams.
  Ordinary active matches reject capture; practice matches permit it.
  User: loading centers X/Z around zero and preserves the original Y coordinate.

The tools menu includes marker visualization, all15 authored marker items,
counts, cages, a BedWars team selector, explicit match stop, reload and a save
submenu with corner selection. Save links suggest the command so the author can
type a map name. The obsolete temporary-structure cleanup button now clears the
selection, because saving creates no structure blocks.

## Native contracts

Legacy source lives under the old server's `world/datapacksdisabled`, not its
absent `world/datapacks`. The source audit is retained in the ignored evidence
directory `build/verification/developer-tools-2026-11-01/legacy-audit.md`.
Preserve `swrg.midle` spelling and separate generator/bed/shop tags; these are
not extra SkyWars chest categories. The original global minecart inventory loop,
hardcoded map62 saver, scratch blocks and broad helper cleanup are not executed.

Gallery discovery reads active public qualified loot definitions and native mod
items, deduplicating full item/components with count ignored. It does not sample
random rewards to discover rare variants. Definitions and placement are queued;
only loaded chunks are modified. Existing frames are owned explicitly. Native
creative attacks are intercepted as well as ordinary frame use. The layout is
stored in `world/data/luckywars/content-gallery.json` and rebuilt after restart
or successful data reload. No RP or template assets are required or modified.

Debug access is checked again at each menu action. The operator's persistent
`skywars2.debug_perks` tag bypasses only perk gates while they remain an operator;
closing the menu ends its during-match menu access, while selected perks continue
to work. Native kit/skill/level-up changes apply immediately. Ordinary menu gates,
other players and unimplemented loadout entries retain their existing behavior.
Vanilla kit stacks, experience and loot are delivered through native APIs, so
debug selection also completes immediately inside a command execution context.

Saved maps are independent native templates/manifests under
`world/data/luckywars/maps/<id>/`. A complete directory is atomically published;
failure removes only private staging files. World reads stay on the main thread,
up to16384 operations/8ms per tick, skipping empty chunk sections. Compression and
file writes use a worker with detached NBT. Capture requires already resident
full/entity chunks, then pins them with its own tickets; it does not generate
terrain. Keep the selected region unchanged until capture completes.

Limits are1024 blocks per axis,32M selected cells,2M non-air blocks,1024 chunks,
4096 entities, world-height bounds and X/Z within ±8192. New IDs use the first
unoccupied numeric SkyWars slot or numeric BedWars slot with suffix `b`, retaining
all bundled entries. Menus paginate25 cards per page with native previous/next
buttons, literal custom names plus actual spawn counts. Reload and the existing
MapManager loader discover persisted maps. BedWars saving does not enable its
still-unimplemented gameplay mode.

## Qualification

Qualified final artifact:
`a5a8532a35b00311815ce5dfd5cc5b5d5a8897107ef43d4672ac6d75fbb3e816`.
All seven scoped groups pass on this exact artifact: content-gallery,
developer-tools, developer-mode, map-authoring, operator-settings, developer-solo
and lbc-loadout. The last group also passes the existing lbc-menu migration and
native selector checks. The actual solo test starts with one real ServerPlayer,
applies lethal native damage and creates a replacement body through PlayerList;
native AFTER_RESPAWN restores participation. It then survives20 natural solo
match ticks with unchanged lives. The separate developer-mode group exercises
death callbacks and live debug kit selection. Do not substitute that callback
test for the actual solo death/respawn proof.

Map authoring writes both real SkyWars/BedWars files, checks air exclusion,
container inventory, markers and numeric teams, places the saved structure
through native StructureTemplate and discovers both files through a fresh
MapManager. The tiny owned fixture saves are archived under
verified-native-map-saves.zip and removed from the stopped fixture afterward.
Gallery checks cover actual canonical public definitions, native fixed frames,
component identity and ownership. The full plaza has not been visually inspected
on a physical client; no client-FPS, peak-TPS or arbitrary-map claim.

9781 frozen source inputs,2457 compiled classes and7352 processed resources match
the artifact;226 author paths and31 live overrides remain unchanged. All child
servers stopped normally; exact player/profile/config and extra level/ticket/OP
files were restored, with no retained locks. Fixture25577 only.

Retained failures:95cdced9 exposed deferred command-based vanilla kit delivery,
now replaced by native stack/experience/loot APIs. Broad LBC regression attempts
initially lacked its legacy-profile seed, then ran unrelated menus which overwrote
that seed/selections. The final isolated existing lbc-menu/loadout pair passes;
no gameplay assertion was weakened to hide those fixture failures.

Evidence: build/verification/developer-tools-2026-11-01/qualification.json and
scoped native receipts/logs. Shared normal-stop installer is queued; no main
server stop/restart, RP/template change or push was performed.
