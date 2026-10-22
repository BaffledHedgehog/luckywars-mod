# Next complete acquisition after Bees — OP Anvil source audit

22 October 2026. Recommendation only; no implementation/runtime qualification.
Only this note was written. Source root S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

Recommend the normal/old OP Anvil pair. Both functions are four commands and their
reward closure uses vanilla enchanted books, not unported custom item abilities.
No opanvil branch exists in current `LbcSpecialDrops.java` at audit.

## Dispatch and native payload

S `function/drops/specialdrops.mcfunction:51–52` dispatches normal then old at the
dropped token's fractional position. Neither branch selects a player or relocates
to one. Preserve independent matches/source order and the shared one-unit debit.

S `function/drops/opanvil.mcfunction:1–4`:

1. Strict fill air from token XYZ through +(0,5,0), six cells inclusive.
2. Align XZ, move to block center XZ, summon falling_block at Y+5 with anvil,
   Time1, DropItem1, HurtEntities0, FallHurtMax0, fall_distance0, FallHurtAmount0.
3. Summon ONE native experience_orb Value1395 at original fractional token XYZ.
4. Native loot spawn `lbc:opbook` at that same original position.

S `function/drops/opanvil_old.mcfunction:1–4` changes only the fill to ordinary
(no strict), leaves falling-anvil XZ fractional, and uses `lbc:opbook_old`.
No lightning, damage-on-impact, schedule, actor tag, custom AI or arbitrary launch
motion is authored. The falling anvil explicitly does not hurt entities. Native
settling/discard/drop behavior remains relevant; do not reuse netherite lightning
handling. Preserve strict versus ordinary neighbor/container/drop semantics and
per-command failure followed by later commands, without an all-or-nothing rewrite.

## Exact reward closure

S `loot_table/opbook.json:1–591`: one roll, one enchanted_book, 43 ordered native
set_enchantments functions, each add:true with its own independent random_chance
0.333. This is not a one-third chance for the whole book and not a single random
enchantment. Incompatible enchantments can coexist by source command semantics;
no random-enchant compatibility filter is authored. Last item function at570
sets tier_u; pool function at580 names it translatable `book_of_absurdity`,
italic:false, with color unspecified (do not invent color/lore). The table includes
minecraft:lunge3 at559 and piercing4 at468. Validate all registry keys on current
26.3 and native stored-book component conversion before qualification.

S `loot_table/opbook_old.json:1–55`: one roll, one count1 enchanted_book, direct
stored_enchantments map of28 entries; no name/lore/model/tier. This intentionally
includes piercing5 at44, unlike normal piercing4, and excludes normal's curse/
newer additions. Preserve the entire exact map; do not derive old from normal or
normalize overlevel values. No nested table, custom loot condition or custom
mechanic dependency occurs in either table.

Both current project resources are byte-identical to source:
`opbook.json` SHA256 A0AF60846608AD85E19CAF7DB143AC959733D696C3A0AFD50C31AE2E25701FE7;
`opbook_old.json` SHA256 E1882E531693CBB9E60E74E5E6BFE66438E48A864438D87E7DD0261D32D3A948.
Presence does not establish that native decoding and public acquisition work.

## Tokens and resource-pack contract

S `loot_table/barriersnottraps.json:534–577`: normal count1 repeating_command_block,
weight10 quality8; tier_b/drop_not_trap; yellow nonitalic `drop_op_anvil`, one white
nonitalic `trap_desc`; `{opanvil:1,drop:1}`. No entry condition.
S `loot_table/legacy_luckyblock.json:3516–3535`: plain count1 barrier, empty
set_components, `{opanvil_old:1,drop:1}`; no custom name/lore/tier/model.
Source flag matching on any nonempty stack remains established compatibility.
Native variants and standalone public leaves can expose the pair without opening
whole Lucky Block/legacy/trap/imba pools. Internal book tables need explicit build
whitelisting alongside public leaves.

Read-only current RP:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2622`: RU/RPR `Дроп: Книга абсурда`,
EN `Drop: Book of Absurdity`. At273: RU/RPR `Книга абсурда`, EN `Book of absurdity`.
Retain exact capitalization, source components, original trap_desc and existing
tier/model resources. No RP or template changes were made.

## Qualification and remaining limits

Reuse source-order/unit debit, attributed terrain, loaded chunks, native actor
ownership/epoch/time-stop and quiet cleanup. Validate native anvil lifecycle,
strict/ordinary six-cell clearing, fractional versus centered geometry, exactly
one1395orb, exact book variants and independent random chances. Indirect terrain
fall/void/suffocation attribution remains required even though anvil impact damage
is disabled. Actor cleanup must not produce extra anvil items/rewards at stop.
Actual native anvil use of generated books and XP collection matter beyond fixture
metadata. No new author ambiguity was established: piercing5 and independent
incompatible enchantments are explicit authored data, not missing instructions.

Lucky Room was not promoted: its four all-false red mushroom blocks are RP Lucky
Blocks (`assets/minecraft/blockstates/red_mushroom_block.json:1–16`), so treating
its10 placement commands as an ordinary decoration would omit acquisition/reward
closure. Vex normal reaches custom vexsword; infinity/forest/magicbean and other
previously deferred trees stay deferred. No completed drop was reselected.
