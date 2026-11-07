# Six TMT wisp fields: source reachability — 7 November 2026

**The six raw TMT item fields are source-inert in the inspected sources.**
`wisp_air`, `wisp_fire`, `wisp_light`, `wisp_nature`, `wisp_shadow`, and
`wisp_water` have item-data writers but no item-data readers. They should not
remain six `LEGACY_OWNER_GAP` blockers merely because a separate companion-wisp
function family exists. Implementing six held casts would add behavior absent
from the actual input dispatch.

This conclusion does **not** claim the separate companion functions are useless,
ported, or runtime-tested. They contain meaningful summon/passive/ownership/
cleanup behavior. Their creation input/book acquisition has no incoming game
edge in the inspected source corpus.

Only this new note was written. Original server, Java, resources, classification
data, and author files were not edited. No JVM, Gradle, JDT/semantic index, server,
or subagent was started.

## Corpus and exact search result

Read-only original root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Both `lbc` and `lbcr` were searched across all `.mcfunction` and `.json` files,
covering functions, advancements, loot, predicates, and function/loot tags within
those namespaces. Exact query:

```powershell
rg -n 'wisp_(air|fire|light|nature|shadow|water)' <root>/lbc <root>/lbcr -g '*.mcfunction' -g '*.json'
```

An independent read-only line classification counted **49 matching lines**:

| Use | Lines | Meaning |
| --- | ---: | --- |
| `drops/tmt/set_storage_nbttags.mcfunction` | 6 | TMT candidate key/value writers |
| `loot_table/wisp/<element>.json` custom-data component | 6 | Item-data writers |
| Same six loot tables' translated `item_name` | 6 | Display identifiers |
| Companion function files | 31 | Entity tags/selectors, summon tags and identifiers |

The only six matching lines containing `custom_data` are the loot **writers**.
There are no reads of these keys through SelectedItem, Inventory, equipment,
`execute if items`, `match_tool`, advancement item predicates, or data reads.
There are **no wisp references at all in lbcr** in this source corpus.

## Item fields do not become entity tags

Original source edges (all paths below are relative to `data/lbc`):

1. `function/drops/tmt/set_storage_nbttags.mcfunction:1029–1034` appends the six
   pairs `["wisp_<element>","1"]` to `storage lbc:tags tags`.
2. `function/drops/tmt/write_random_nbttag.mcfunction:1–3` selects the pair into
   `str`/`val` and calls `set_nbttag with storage lbc.math`.
3. `function/drops/tmt/set_nbttag.mcfunction:1` writes
   `Item.components."minecraft:custom_data".$(str) = $(val)`.

That write changes the dropped item's custom-data component, **not** the holder's
or any display's vanilla `Tags`. No forwarding/copying edge was found.

The six `loot_table/wisp/{air,fire,light,nature,shadow,water}.json:17` files also
write `{wisp_<element>:1,magicwand:1}` to a carrot-on-a-stick item. Their other
components include stack size 64, removed max damage, tier B, translated item
name, and `lbc:spirit_<element>` model. This corrects the earlier shorthand that
the keys occur only as entity tags/display values: **real item-data writers do
exist; item-data consumers do not.**

The generic main/offhand `magicwand:1` routes in
`function/items/spells.mcfunction:7,13` call
`workingitems/spell1` / `workingitems/spell1_left`. Those split upgraded/default
routes. None of their default/upgraded main/offhand dispatch files reads a wisp
key, wisp model, or wisp-book input. The existing TMT basics already contain
`magicwand:1`, so adding one wisp field supplies no new generic route either.

## Separate companion engine: real internal edges

For every element, `function/other/wisp_book/create_wisp/<element>/run.mcfunction`:

- Tags the player `check_id`, counts nearby owned `item_display` entities with
  `tag=wisp`, and compares their `lbcID2` owner score.
- Calls `<element>/spawn` below twelve owned companions.
- Calls `<element>/drop_as_item` at twelve or more.

For example, air's `spawn.mcfunction:1–12` summons an `item_display` with
`Tags:["wisp","wisp_air","new","rc_5t"]`, assigns owner `lbcID2` and
`team_number`, adds `has_wisps` to the player, then calls
`passive_effects/air/count` and `realign_wisps/start`.
The display's represented item is a repeating-command-block with a spirit model;
its selector `tag=wisp_air` is an **entity tag**, not the held item-data field.

Each `<element>/drop_as_item.mcfunction:1` calls `loot give @s loot lbc:wisp/<element>`.
These six overflow functions are the only source calls to the six wisp loot
tables; no acquisition root/loot entry outside that family was found.

Existing global tick/cleanup consumers are genuine:

| Incoming route | Companion work |
| --- | --- |
| `function/raycasters_5t.mcfunction:15` | `tag=wisp` -> passive particle tick |
| `function/players_tick.mcfunction:55–57` | `has_wisps` orbit; shadow score/charge work |
| `function/players_2t.mcfunction:20` | air charge/attack score work |
| `function/players_2s.mcfunction:17` | light score/passive work |
| `function/players_1s.mcfunction:53` | nature score/passive work |
| `function/leave_death.mcfunction:344–349` | six delete-by-owner cleanup roots |

Fire/water count functions alter attack/max-health modifiers after counting
owned tagged displays; air/light/nature/shadow similarly derive player state
from real companion bodies. Existing actors therefore have meaningful behavior.
These are downstream engine edges, **not creation or held-item input edges**.

## Creation/book reachability and dynamic dispatch

Queries for `wisp_book`, `create_wisp`, six `wisp/<element>` loot references and
six `spirit_<element>` model references found no external creation caller,
advancement reward, item-use predicate, book component/click event, or loot root.
There are no incoming calls to the six `create_wisp/<element>/run` roots.
Outside the wisp-book directory, references are the downstream tick/cleanup
routes listed above. No separate wisp-book acquisition item was located.

To avoid mistaking literal search for a complete dynamic call graph, all **72**
macro lines starting `$` and containing function/custom-data syntax in lbc/lbcr
were enumerated and inspected. Dynamic function names are confined to:

- `load_2.mcfunction:1`: `swrg_kit_integration/lang_$(lang)`;
- `swrg_kit_integration/game/skills/place_random_block_storage.mcfunction:1`:
  `.../skills/blocks/$(tmp)`;
- `other/imba/cunny_slab/correction_cunny_uoh_pasta_select.mcfunction:1`:
  `.../cunny_slab/pasta/$(t)`;
- `items/trinket_effects/func_with_chance_universal.mcfunction:1`:
  `function $(tmpf)`, whose source assignments are explicit trinket-effect paths
  (bedrcock/cocktail/Fourier/gneiss/gauntlet/ice/intercal/polymorph/etc.), not wisps.

Other macro uses of custom data are fixed spell-number/library, ice-crystal owner,
communism, inventory-storage, and the TMT item-data **write** above. No macro
constructs a wisp path from these six item fields.

## Paper/Ignite reference check

Read-only Java roots:
`E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java` and
`E:/servers_mew/cringe1_21_4_skywars/mods/luckywarsignite/src`.

In Paper, the six exact keys occur only in
`me/luckywars/swrg/SwrgNativeService.java:13552–13557`:
`cleanupPlayerWisps` -> `removeTaggedEntitiesByOwnerScore`.
That is entity cleanup, not a held custom-data reader or acquisition/creation
handler. UI sources read/display shadow-wisp scoreboard state. No separate wisp
ability/input registration was located. Ignite's inspected source has no wisp
key/book/model occurrence. Embedded Paper datapack source copies reproduce the
same six loot writers and downstream engine references rather than supplying a
missing creation input.

This is bounded static source evidence. It does not rule out manual admin
`/function` invocation, externally injected world storage/books, or unavailable
third-party content; none establishes a source-authored TMT item-data consumer.

## Catalog consequence

For the user-authorized omission of no-effect properties, classify these **six
raw scalar fields** as source-inert/no-reader with the evidence above. Preserve
their source rows/provenance for audit. Do not represent that exclusion as a
completed native wisp implementation, and do not infer six held casts from the
names. The separate dormant companion engine and its missing book acquisition
may remain a separately described source question; they do not justify six
unported TMT property blockers. No catalog code/data was changed in this audit.
