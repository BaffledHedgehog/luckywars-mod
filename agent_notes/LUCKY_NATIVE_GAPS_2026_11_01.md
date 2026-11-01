# Native Lucky opening gaps — 1 November 2026

Read-only source audit; no build, server, protected source, resource, RP or PENDING edits. This does not qualify the complete reward graph.

## Resource closure

Walking minecraft:loot_table value/name references from lbc:luckyblock, lbc:nekoluckyblock and lbc:loliluckyblock reaches 3,642 distinct source tables; every referenced source JSON exists and parses as JSON. Gradle processResources is an explicit allowlist (build.gradle:28). item_defs/**/*.json at line40 includes root and nested definitions using Gradle Ant semantics. Only two reached tables lack an include: nekoluckyblock and nekolucky_zaika_items. Ordinary luckyblock and loliluckyblock are included at lines719–722; nekolucky_items at729.

nekoluckyblock.json:11 and43 still use old conditions[] and condition discriminators; the rest of this reached graph has no functions/function fields. Simply adding Neko to packaging loses its Zaika/key selection gates under the native26.3 singular condition/type codec. Entry0 chooses nekolucky_items when no inventory Zaika and no selected luck_key; entry1 chooses nekolucky_zaika_items when inventory Zaika and no selected luck_key. Port to native entity/item predicates; do not rely on removed legacy player NBT fields.

## Positive missing gameplay branch: Mom + Otchim + Absurd Child

loliluckyblock.json:286 reaches item_defs/lbc/drop_boss__a5f2a6f79256, whose line43 sets {otchim:1,drop:1}. imba/spawnegg.json:28 reaches item_defs/minecraft/snow_golem_spawn_egg__b6d3d42c6315, whose entity_data emits the same token. No production Java mentions otchim or absurd_child. Shared LbcSpecialDrops.java:32–33 therefore cannot admit them.

Mom tokens similarly exist in item_defs/lbc/drop_not_trap__51f951f5f194.json:43 and item_defs/minecraft/pig_spawn_egg__05968de6b978.json:20. No Mom token consumer exists in SpecialDrops, NpcDropItems or SpecialEggs. The existing LbcPendingSwords.java:135 only consumes an already-existing mom armorstand for the antimagic sword advancement; it never spawns it.

Original source root for paths below: E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ (read only).

- drops/mom.mcfunction:1–7: invulnerable posed armorstand with mom/rc_1sec tags; head/name visual from loot lbc:mom in main hand; nearest nonspectator lbcID2 ownership, have_mom tag and broadcast. raycasters_1s.mcfunction:4 invokes other/mom each second without jammer gate: source facing command, fire at stand position and random one of six animal death sounds (pitch0 or0.5). Reuse shared construction/fire causality and existing owner identity.
- drops/otchim.mcfunction:1–5: invulnerable invisible persistent zombie, otchim/lucky_mob tags, loot lbc:otchim head/name and nearest-player broadcast.
- mobs_5t_lbc.mcfunction:5: Otchim near mom armorstand within2.5 births child without jammer gate. other/absurd_child.mcfunction:1–8: lightning at every entity outside Nexus; particle batch5000; nearby15 advancement; broadcast; persistent glowing 100000HP allay holding luckyblock_block_only; remove nearest mom and Otchim. Native batch must preserve actual effects/causality with bounded dispatch rather than copy unbounded command loops.
- mobs_1s.mcfunction:6 gates child ticks by antimagic beacon marker radius20. other/absurd_child_tick.mcfunction:3–8: when no nonspectator within5, velocity0.5 toward nearest player using raycast_vpered_slower; chance3 random chat (20 variants, audience30); held block cycle stem→red→brown→stem through three source Lucky block tables. Artifact suppression needs explicit existing policy/user resolution; source only checks marker here. Source birth/Mom/Otchim themselves have no jammer gate.

This branch interacts with existing have_mom Envy criteria and foreign-Mom antimagic sword task. Completing only Otchim spawn leaves this dependency open.

## Limits and proposed coherent batch

A lexical sweep of reached custom_data keys found Otchim as the only key absent anywhere in production Java. Mom demonstrates why that sweep is insufficient: a criterion references its tag while its producer is absent. No claim is made that other keys have complete consumers, correct acquisition or native proof.

Coherent next unit: three native Lucky block opening paths and raw compatibility, proper Neko resource/predicate closure, Mom/Otchim/Absurd Child full chain, actual natural drop-token/spawn-egg entry points, native context/category/held modifiers and ordinary no-output/debit behavior. Keep kit6 gated until the bounded full-opening batch has proof. Existing shared token dispatcher, causal terrain work, saved native actors, clocks and menu owners should be extended; no function interpreter or private checkpoint engine.

## Important live Paper override divergence

The original plugin registers replacements in plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:1246,1264,1270. This source supersedes the matching function bodies when that runtime registration is live; registration presence alone is not a historical execution receipt.

- tickNativeMom:5893 turns the stand toward nearest nonspectator eyes, retains fire and six sounds.
- runNativeAbsurdChild:5972 searches nearby mom in a2.5 axis-aligned box, emits ONE visual strikeLightningEffect at birth (not global damaging lightning), retains particles/advancement/allay and removes parents. Child hand drop chance explicitly zero.
- tickNativeAbsurdChild:6032 replaces source movement with nearestCombatTarget within20 and distance>5:20-block native Bukkit ray, fluids NEVER, ignore passable blocks true, radius0.2; eligible LivingEntity hit excludes self/spectator/strict_map_object. END_ROD line spacing0.4; actual6 damage credited to allay. Retains3% random20 chat and held-block cycle. This is a material source discrepancy requiring existing user/source precedence before implementing movement versus beam or global birth lightning.

Mom's raycasters_1s increments Lifetime20 each tick and only expires when entity data.lifetime is positive and reached (lines1,21–22); Mom spawn sets no data.lifetime, so there is no source default lifetime limit. Otchim/child are persistent native actors, with no dedicated expiry found. Existing Fabric LbcPendingSwords135 reads Mom lbcID2; existing LbcLegacyEffects126 and LbcWitchClaims44–45 use have_mom. Credit must follow actual causal owner under user policy, with display-target/foreign-Mom identity retained separately where necessary.
