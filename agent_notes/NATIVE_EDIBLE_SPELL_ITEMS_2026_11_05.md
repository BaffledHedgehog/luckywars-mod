# Native edible-spell item identities - 5 November 2026

Twelve native items cover thirteen canonical source definitions: ten edible buckets,
one IMBA chorus fruit (one-item and64-item loot variants share the same native ID),
and the yellow-lightnings bottle. The previous forty named-food identities remain.
The finite existing manifest/Polymer/prototype loader is extended to52 item types;
there is no second consumption engine, new tick task or new polling read.

Source definitions remain under data/lbc/loot_table/item_defs. Only entry.name
changes; source counts, functions, conditions, names, styles, lore, models and
flags remain byte/structure-checked. Thirteen public lbc:ported/food leaves reference
these definitions. The complete IMBA root remains gated; TMT stays last.

The shared prototype loader preserves vanilla salmon nutrition/consumable and
chorus nutrition, native random-teleport consume effect and one-second cooldown.
The existing LbcEdibleSpells owner handles source gain_bucket, milk/poison effects,
ongoing lava/snow use, extra chorus teleport and yellow-bottle mana/Psycho effects.
Existing suppression, TimeStop and native consume/debit paths remain unchanged.
Food-count membership still uses the exact original41 carrier category, now with
51 explicit native aliases; the stick banana remains outside that category.

## Qualification

Test `3f29b80887e571c4232f35ada78106544867473e2161f6010c4e8d5a290cc266`; production `43dd7b7ac72aa671bdc9a21f3e606d39e9019ce5ed0c8b8ebce9d80f8e81635e`.
Six isolated matches on the same exact test archive passed:
- mode1: native-named-food, edible-spells, food-lifecycle, named-food;
- mode3: native-named-food, edible-spells.

An independent vanilla-carrier loot codec checks every effective component for52
native types, canonical/give defaults, source food membership, actual held-use
admission/cancellation/duration and Polymer roundtrip. One/64 chorus variants
are explicitly checked to share one identity and all properties. Ten real bucket
consumptions each debit once and publish exactly one correctly owned native
throwing bucket through the existing gain_bucket consumer. The existing edible
suite checks native ongoing lava/snow consumption and delayed TimeStop fangs;
all original40 named-food profiles and common food lifecycle pass again.

Five fresh Java families/27 resources;10191 other source inputs and282 author
paths guarded. Every archive entry and retained binary reference checked.
Children shut down normally, exact snapshots restored, temporary mode3 maps
removed. Both build/libs and main are installed, without main stop/start.
Receipt and evidence: build/verification/native-edible-spells-2026-11-05/.
No author RP edit, extra indexer or push. Full IMBA/mode/skill2/broad-rewind gates
unchanged. Physical client rendering, disk restart and peak TPS are not claimed.
