# Consuming resources — 20 October 2026

Resource import only; runtime behavior/DFU codec loading belongs to the mechanics checks.
`tools/import_lbc_consuming.py` reuses the existing item/Academy import helpers and
native loot schema migrations. Source root, read-only:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

| Native ID (also ported filename) | Source | Model | Tier |
|---|---|---|---|
| `skywars2:consuming_wand` | `loot_table/tuntijaa_greed_destroy.json` | `lbc:consuming_wand` | S |
| `skywars2:academy_consuming_wand` | same + `item_modifier/add_upgrade_desc.json`, `item_modifier/tiers/x.json` | same | X |
| `skywars2:creeper_army_spell` | `loot_table/transformables_spells.json`, creeperz:1 | `lbc:creepers` | A |
| `skywars2:destroyer_spell` | same, trap2:1 | `lbc:breaker` | A |

All four outputs live under `src/main/resources/data/lbc/loot_table/ported/`.
The ordinary payloads retain the complete source pool/entry/name/lore/style/custom
components, including both donors' max stack size 64 (one item issued) and removal of max_damage. Only outer
item identity becomes native. Academy adds the existing shared upgrade flags and
mode1, then applies the source generic upgrade lore and tierX modifiers in order.
Source Consuming lore's literal5 magic damage remains unchanged; the resource
import does not resolve the known difference from source runtime2 damage.

`src/main/resources/skywars2/consuming_presets.json` retains the raw typed SNBT
from `function/workingitems/awaken_tuntija_wands/consuming_wand_upgrade.mcfunction`.
All28 skeleton payloads are identical; the importer asserts this, stores one
`presets.void_man` with inserted entity id, and retains all28 ordered relative
positions as `spawns:[{preset:"void_man",offset:[x,y,z]}]`. `originalDataVersion:4440`
matches the established Summons native ENTITY DFU convention for source modern
`equipment`/`drop_chances`. No handwritten entity/item migration is performed.
Health40, attack10, invisibility, names, equipment, tags and death loot survive.

`DeathLootTable:"lbc:voidman"` has a two-table dependency closure:
`data/lbc/loot_table/voidman.json` and `sauvojen_tuntija_blind.json`. Voidman required
the shared native schema migration; Blind was already identical and reused.
Source item identities inside this general loot closure remain unchanged.

Existing templates were verified byte-for-byte against the source and reused:
`data/lbc/structure/{meditation,air11x11,silence_1,silence_2,silence_3,silence_4,silence_5,silence_6,silence_7,silence_8}.nbt`.
No duplicate structures were created. Meditation is47³; air11x11 is11³;
each Silence part is48³. After the author's decision, Silence uses direct sphere48
erasure; its eight historical template pieces are not required in the new JAR.

Read-only resource-pack audit used the exact path in [RESOURCE_PACK.md](RESOURCE_PACK.md).
All30 referenced translation keys, including Academy mode/condition/success,
void_man, both donors and loot dependency text, exist in both RU and EN.
All five item model definitions (`consuming_wand`, `creepers`, `breaker`,
`liquidabyss`, `st/blind`) and `assets/lbc/equipment/void.json` already exist.
Their source identifiers and payload formatting are reused. The RP and
resourcepack_template were not edited; no copied keys or substitute models.

Validation: importer completed twice; exact complete migrated source comparisons
passed for ordinary/donor payloads and the independently reconstructed Academy
payload. Preset extraction asserts count/identity and output JSON equality; all
loot dependencies compare against shared source migration; ten templates compare
raw bytes. [CONSUMING_RESOURCE_MANIFEST.json](CONSUMING_RESOURCE_MANIFEST.json)
contains the seven runtime resource include paths, output/template hashes,
RP language hashes and complete key/model inventory. The root agent owns
build.gradle includes and subsequent native server checks; no build/server run here.

## Exact source visual/audio audit

Paths below are relative to legacy `data/lbc/function/`. Each sample means each
original recursive sample, before terrain work coalescing; partition means each
active partition tick. All particle coordinates are the current sample point.
Sound tuples are `(volume,pitch,minVolume)`; an omitted minVolume stays omitted.

| Source file | Particle options; spread; speed; count; visibility | Sound |
|---|---|---|
| `workingitems/awaken_tuntija_wands/consuming_wand_loop.mcfunction` | dust RGB(.424,.251,.647),scale2; .05/.05/.05;0;2;force each sample | wither.shoot master @a[distance=..25], (.01,1,.01), each sample |
| `workingitems/awaken_tuntija_wands/consumption_part_da.mcfunction` | dust_color_transition RGB(.494,.282,.482)→(.294,.082,.282),scale2;.1/.1/.1;0;1;force each active tick | none |
| `workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_the_end.mcfunction` | dust RGB(0,0,0),scale4;2/2/2;0;4;force each sample | wither.shoot master @a, (.01,0,omitted), each sample |
| `workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_avarice.mcfunction` | dust RGB(.667,.459,.941),scale1;.05/.05/.05;0;1;force each sample | deepslate.break master @a[distance=..25], (.2,1,1), each sample |
| `workingitems/unstable_spellbook/raycast_loop8.mcfunction` | landing_obsidian_tear;0/0/0;.01;1;force each sample | none |
| `workingitems/raycast_spells/creepers.mcfunction` | portal;1/1/1;2;100;normal at impact | block.portal.travel master @a, (1,0,omitted) |
| `other/unstablearea.mcfunction` | portal;1/1/1;3;30;normal each5ticks | none |
| `other/unstablearea_rc.mcfunction` | chance40: dust RGB(.5,0,.5),scale4;.1/.1/.1;0;1;force each curved sample | none |
| `workingitems/unstable_spellbook/raycast_loop.mcfunction` | soul_fire_flame;1/1/1;.01;2;force each sample | none |

Silence has no particle command: only `stopsound @a`, source effects and eight
templates in legacy (now replaced by sphere48). Avarice impact has no added particle/sound. Consuming trial has no
explicit particle/sound: meditation template and28 Silent skeletons, darkness
and blindness20s amplifier3 with hideParticles=true. Summoned invisibility also
has show_particles=false. The template placement offsets remain source-derived.

Read-only NBT inspection additionally confirmed air11x11 has exactly1331 block
records, a single palette entry `{Name:"minecraft:air"}` and unique state index0.
A complete11³ air erasure therefore represents its full block payload.

At audit time `LbcConsuming` used Portal for normal/partition and Smoke for TheEnd,
and an aggregated wither sound with altered volume/pitch. Those differed from
the source table above; the root agent owns corrections and native verification.
