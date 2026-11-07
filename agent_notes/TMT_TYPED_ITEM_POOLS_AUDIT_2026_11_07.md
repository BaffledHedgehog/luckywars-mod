# Typed all-item selectors — bounded audit, 7 November 2026

Read-only audit; only this note was added. No Java/resource edits, compiler,
JVM, JDT/index, server, map inspection or subagents. Current implementation was
read from `LbcAllItemPools`, `LbcBolts`, manifest/builder and selector resources.
Original **functional structure templates**, not world maps, were inspected for
literal NBT `LootTable` string tags under the old LBC `data/lbc/structure` root.

## Current domains and wrappers

| Table/API | Domain/filter | Actual callers / status |
| --- | --- | --- |
| `lbc:random_item` | Uniform custom logical slots | Bridged to `skywars2:uniform_item`, domain `custom` |
| `lbc:random_glack_1` | Uniform custom + vanilla union | Bridged; GrimoireRooms room 2, GIFU rune, `27_random_items`, random_glack |
| `lbc:random_glack` | Union, then source count 1..64; max stack 99; remove max damage; limit count 99 | Existing wrapper retained; BucksTrader and a weighted `lbc:luckyblock` branch |
| `lbc:27_random_items` | 27 union rolls, source set-count 64 | Inherits bridge; retain this wrapper independently of max-stack-99 wrapper |
| `Domain.VANILLA` | All registered `minecraft` item defaults except AIR | API exists; no dedicated vanilla-only loot-table bridge in current manifest |
| `lbc:lucky_items`, `lbc:lucky_items_high_quality`, outer `lbc:luckyblock` | Authored weighted lucky drops, with conditions/functions | Separate from uniform domains; do not flatten their weighted graph |
| `lbc:infinity_no_shulker` | Authored restricted contents | Intentionally unchanged; infinity/infinity_upgraded reference it |

Current manifest: **1953 custom logical slots, 7286 custom profiles, 10239
interned entry nodes**, schema 1, complete=true. It has no type membership/index
fields. `LbcAllItemPools.Entry` currently accepts a domain, not a typed selector.
`selectionSources` snapshots contain only random_item and random_glack_1.

## Existing typed table names and native dispatch

All names below are under `lbc:`. `transformables_X` abbreviation is **T(X)**;
`T(X)_all` means the literal `_all` filename, not proof of domain completeness.
There are **20 base tables and 11 `_all` tables**. They remain legacy weighted
selectors; none currently uses the logical-slot entry.

| Source item category / intended type | Base selector | Existing broader selector | Main direct dispatch |
| --- | --- | --- | --- |
| accessory | T(accessory) | T(accessory)_all | normal/lightning base; ultra broader |
| axe | T(axe) | T(axe)_all | normal/lightning base; ultra broader |
| bow | T(bow) | T(bow)_all | normal/lightning base; ultra broader |
| hoe | T(hoe) | T(hoe)_all | normal/lightning base; ultra broader |
| pickaxe | T(pickaxe) | none | all three use base |
| potion | T(potion) | none | all three use base |
| shield | T(shield) | none | all three use base |
| shooter | T(shooter) | T(shooter)_all | normal/lightning base; ultra broader |
| shovel | T(shovel) | T(shovel)_all | all three currently use **base**, despite broader file existing |
| staff | T(staff) | T(staff)_all | normal/lightning base; ultra broader; EdibleSpells broader |
| sword | T(sword) | T(sword)_all | normal/lightning base; ultra broader |
| totem | T(totem) | none | all three use base |
| spellitem / spell | T(spells) | separate `imba/transformables_spells` | three use base; EdibleSpells gives ten base rolls |
| summoner | T(summons) | T(summons)_all | normal/lightning base; ultra broader |
| trident | T(trident) | none | all three use base |
| arcanum | T(arcanum) | T(arcanum)_all | normal/lightning base; ultra broader |
| helmet | T(helmet) | none | normal/lightning base; ultra uses source set-specific rules |
| chestplate | T(chestplate) | none | normal/lightning base; ultra set-specific; ultra `dynamite` also selects base |
| leggings | T(leggings) | none | normal/lightning base; ultra set-specific |
| boots | T(boots) | T(boots)_all | normal/lightning base; ultra `moisei` selects broader, other sets have own rules |

Native common owner: `LbcBolts.NORMAL_REFORGE` / `ULTRA_REFORGE` / independent
`LIGHTNING_REFORGE`, and `reforgeTables` / queued `ReforgeInput`. Protected
`LbcRerollCrafting` calls the shared normal selector; `LbcTinyReforge` calls the
shared ultra selector. Route changes can remain at this shared boundary without
editing protected crafting bodies. Multi-flag donors intentionally produce
multiple table results, in source order; per-unit recursion, exact-64 arrow
conversion, exclusions and original debit are separate and must remain intact.

Related subtype selectors are `spells_library`, the source `neko_set_*` set
tables, `set_randchanted`, `set_multiplying`, `halloween/metasola`, and
`magnum_ammo`. These encode narrower source rules, not synonyms for “all armor”
or “all items.” Halloween sword/staff/shovel and ported shooter selector files
also exist. `_all` currently means an authored broader branch list, often
base-table weight N plus individually weighted additions, not all modern items.

## Functional structure consumers

Literal string-tag scan of **160 original LBC structure NBTs** found:

- `item_gallery.nbt`: **770** containers across the twelve `27_*` tables below.
- `grimoire_dungeon/room_2.nbt`: **5** `random_glack_1` containers. Native
  `LbcGrimoireRooms` likewise writes that table, already covered by union bridge.
- `gallery.nbt` / `item_gallery.nbt`: infinity-family references; retain their
  explicit shulker restrictions/loot-key behavior as a separate domain.

All `27_*` selectors have 27 rolls:

| Table | Existing inner selection | Typed uniform target for all-item behavior |
| --- | --- | --- |
| `27_acessories` (source spelling) | accessory_all or totem, 1:1 category lottery | CUSTOM + accessory OR totem membership |
| `27_bows` | bow_all | CUSTOM + bow |
| `27_grimoires` | spells_library | CUSTOM + explicit library-spell subtype |
| `27_instruments` | axe_all/hoe_all/shovel/pickaxe/trident/shield, six equal categories | CUSTOM + union of these six types |
| `27_potions` | potion | CUSTOM + potion |
| `27_random_items` | random_glack_1 + count64 | UNION + no type; already bridged |
| `27_rofls` | rofls | CUSTOM + authored rofl membership, not a guessed carrier class |
| `27_shooters` | shooter_all | CUSTOM + shooter |
| `27_slabs` | slabs | CUSTOM + authored slab membership, not arbitrary block items |
| `27_staffs` | staff_all | CUSTOM + staff |
| `27_summons` | summons_all or arcanum_all, 1:1 category lottery | CUSTOM + summoner OR arcanum membership |
| `27_swords` | sword_all | CUSTOM + sword |

A combined typed uniform target must deduplicate outer logical slots. Keeping
the current two-stage equal-category lottery would still overweight a smaller
category and would not mean “equal chance per eligible item.” This distinction
belongs only to designated all-item selectors, not actual weighted lucky drops.
No structure NBT or author template needs rewriting if its dedicated `27_*`
table routes to the new selector while retaining rolls/count functions.

## Preserve the weighted lucky graph

**Fifteen base typed tables** are shared by both `lucky_items` and
`lucky_items_high_quality`: accessory, arcanum, axe, bow, hoe, pickaxe, potion,
shield, shooter, shovel, staff, summons, sword, totem and trident. Consequently
replacing those `transformables_*` resources globally would also change genuine
lucky-drop probabilities. This is the main unsafe shortcut.

Concrete source constraints: bow has two Firework branches distinguished by
`final_luck_modifier=4` (including weight 10000); shooter has a weight-10000
Mobgun branch gated at 7; potion has four weight-1000 variants gated at 10;
accessory has weight-400 Artifact gated at 6. Leaf functions also retain authored
counts, components, enchantment rolls and conditions. Neither flattened item IDs
nor global default ItemStacks can replace those contextual prototypes.

## Reuse plan / remaining gaps

1. Keep the existing global logical identity, interned entry nodes and native/TMT
   factories. Add immutable typed membership vectors during manifest binding;
   runtime draws should index a prepared vector, not scan registries/NBT/tables.
   Each typed outer slot should point to its eligible source-preserving profiles.
2. Build type membership from authored semantic category flags/native capability
   evidence and explicit subtype source closure. Do not classify custom swords
   or staves by carrot-on-a-stick/repeating-command-block transport. Vanilla-only
   types need vanilla item classes/tags/components. Keep one TMT factory ticket;
   its membership in typed views needs an explicit audited policy rather than
   counting five transports or assuming every advertised baseline flag is a type.
3. Add a shared optional typed-selector key to the existing uniform loot entry or
   new **selector names** backed by that entry. Route native reforge callers at
   their common table-resolution boundary and dedicated `27_*` all-item selectors.
   Leave original weighted T(...) tables available unchanged to lucky graphs.
4. Preserve profile conditions and native leaf functions. A typed caller's wrapper
   must not become an extra global slot/profile or leak its count/components into
   unrelated draws. Existing `Node`/`Prepared` machinery can reuse the same interned
   conditions/functions; typed caller profiles may need separate references.
5. Expose dedicated vanilla-only, custom-only and union selectors plus their typed
   views. Current public manifest bridges cover only custom and union roots; the
   vanilla API alone is not proof all requested callers are wired.
6. Keep count1..64/max99/remove-damage and 27-roll/count64 wrappers on their real
   callers. Neither means “force every catalog default to stack99.” Preserve
   infinity_no_shulker and set-specific reforge restrictions separately.

Meaningful next verification: exact logical slot membership/no duplicates for
every type and combined type, vanilla-only purity, stable weighted lucky resource
hashes/condition outcomes, actual normal/ultra/lightning/crafting reforge output,
and real gallery-container rolls/count/component behavior. This note proposes
the bounded next implementation; it does not claim typed pools are already done.
