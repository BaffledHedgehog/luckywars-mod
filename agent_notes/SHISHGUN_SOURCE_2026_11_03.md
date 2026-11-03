# Shishgun prerequisite source audit — 3 November 2026

Historical source audit written before implementation. For current implementation, author decisions and qualification, see [Supernova chain integration](SUPERNOVA_CHAIN_PORT_2026_11_03.md). Readiness verdicts below describe that earlier audit.

Read-only source audit. Only this note was written. Project: E:/servers_mew/skywars2_last/luckywars-mod. L = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc; R = sibling lbcr. Paper = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars (not another plugin).

## Readiness

Shishgun is NOT demonstrated native-ready. Exact literal shish_gun/shishgun/shish_bow searches in project src/main/java returned no matches; bundled loot/model/advancement references are not implementation. Project AGENTS explicitly keeps Railgun/Shishgun gated; REACTIVE_FAMILY_PORT.md agrees. The already ported Shish encounter is a different feature. The same exact literals in Paper src/main/java returned no matches, while its imported datapack resources contain original presentation only. Thus there is no evidenced registered Paper Shishgun override to supersede the original functions. Do not substitute an unrelated futuristic gun engine or call imported legacy functions at runtime.

## Item, generation and consumption

L/loot_table/shishgun.json: one minecraft:bow, tooltip lbc:tier_s, model lbc:shishgun_bow, Infinity I. Name translate shish_bow, bold true, green, italic false. Lore ordered: joined type + bow.lbc (aqua, italic false), then shish_bow.lore (gold, italic false). Flags exactly {craftusable:1,transformable:1,bow:1,shish_gun:1}. The literal loot entry also appears in transformables_bow_all; broad pool presence does not establish that a gated pool is ready.

L/function/other/craft_ground: railgun_bow dropped item + reactive_shish item at distance .001..1 -> other/shishgun. It spawns loot lbc:shishgun, green dust100 and three riptide sounds, then kills nearest matching ingredient ENTITY of each kind (whole stacks; final kills lack a distance bound). Inventory check requires both flags anywhere in player.crafting.* -> other/shishgun_inv: same spawned loot/visuals, reduces one in EVERY matching slot0..3 of each kind with count_reduce. These are historical source debit quirks, not automatically authorization to invent a new recipe economy. Railgun itself is a separate unready dependency (Tiny Electro + eligible bow); Reactive Shish alone is already ported.

Outgoing dependency: shit_katana + shish_gun -> other/supernova or supernova_inv. The core is internal, not a separately obtained Shishgun projectile. Full Supernova must retain the exact item flag and require the entire native weapon/recipe closure before exposing its acquisition chain.

## Input and charge

L/function/items/bows: bows_get precedes special weapons; mainhand SelectedItem shish_gun dispatches workingitems/shish_bow only if NO stopper_magic and NO stopper marker within20. TECH + MAG release suppression, no source artifact selector explicitly in this branch. Ordinary native release still exists when the special branch is jammed. Bow trinket and skill hooks occur AFTER the special handler, then bow, bow_charge, bow_charge_sub reset to0. TMT hook remains separate and LAST/deferred; it cannot justify arbitrary-function execution.

items/bows/charge_add increments bow_charge by1 only when time_stopped is not active; charge1 with infinite_arrow grants its source arrow, then skill charge hook and advancement revoke. charge_query calls shish_bow_charging for selected mainhand Shishgun and clears counters when selected item is not bow.

shish_bow_charging computes charge modulo8; at multiples8 with charge>=5 calls charging_particle. Particle helper emits spruce-leaf particle eye+2 and firework blast (volume10, pitch1), calculates floor(charge/4), tells player '<===> : ' plus distance BEFORE cap700, then traverses charging_particle_full. Preview begins at eyes, increments Distance and steps1 while target distance>Distance; green dust, plus a larger green dust burst if the sample at local -.5 is non-air. Preview does not stop on obstruction. No mana cost, item cooldown or random launch chance is authored.

## Exact firing closure

workingitems/shish_bow copies bow_charge -> temp_distance, resets Distance0, divides by4 (integer floor), caps700. Kills ALL minecraft:arrow within2 of eye anchor (not spectral arrows); gives caster Resistance1 second amplifier4. Only if distance>=6 does it invoke R/function/shishgun at eye+3. Release trident.return volume10 pitch2 and azalea break volume.3 pitch.8 occur even below threshold.

R/shishgun increments Distance; invokes R/shishgun_break; emits THREE azalea break sounds (pitch2,1,1.5, volume.2) and three spruce-leaf particles; recurses at local+1 while target distance>Distance. Thus N=floor(charge/4), capped700, yields N samples at eye offsets3 through N+2, when N>=6. There is NO collision stop, target lock, projectile speed, numeric direct hit damage or per-step jammer check in this ray. It is an instantaneous line producer, not a ballistic arrow.

R/shishgun_break summons one native TNT fuse0 per sample and one marker with tags slowraycast/explodein/magic/mecha/instant and data.lifetime4. These are distinct children. Original TNT has no Owner, author or team assignment; do not mistake this source omission for a desired loss of causal credit. Damage is native TNT blast, with caster temporarily protected by source Resistance. Existing native WILD ordnance policy supplies caster/ally danger, protected roles, death/kill credit, ownership/epoch and full rewind rather than asking already resolved general targeting questions again.

L/function/tick_not_timestopped dispatches slowraycast actors outside Nexus/Imprinted. raycastselector increments Lifetime, invokes instant selector, then expires at data.lifetime. raycastselector_instant invokes other/shishfill at Lifetime3 for explodein. It does NOT create another explosion. shishfill executes four strict spruce-leaf replacement volumes, only #minecraft:tannerkivi: (+2,+7,+2)..(-2,-5,-2), (+5,+4,+2)..(-5,-2,-2), (+2,+4,+5)..(-2,-2,-5), (+3,+6,+3)..(-3,-3,-3). Exact block tag lives in sibling minecraft/tags/block/tannerkivi.json. Preserve overlap/order and active-tick delay3, expiry4, bounded terrain capture/checkpoint behavior and TimeStop. Read source marker magic/mecha suppression semantics through the shared native lifecycle; do not convert this into a generic world scan.

## Native integration and actual remaining questions

Current authoritative BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md supersedes SPECIAL_BOW_TRINKET_SOURCE's earlier missing-implementation findings: six traits compose once on the successful native family; normal/tipped/spectral/custom ammo, Shulker/Madoka and automatic reload already work. Their ownership transfer, TimeStop, native state and terrain jobs must remain. Use the shared OriginalAmmoPipeline/final-family contract requested for this prerequisite, preserve original ammo state, avoid nearest-arrow searches and never roll/spend trinkets independently for every TNT/marker child. A literal OriginalAmmoPipeline search did not locate a class of that spelling; this audit does not assert such a concrete class already exists.

Real author decisions for the new engine, if not already answered in parent scope: how the existing ALL-ammo/stacked-arrow preservation contract applies to a Shishgun that deliberately deletes ordinary source arrows and emits TNT/terrain instead; which final carrier/family receives each arrow-only trinket trait and whether the residual spectral carrier remains; whether source multi-slot/whole-stack craft debit quirks should be retained or normalized by an already approved shared recipe policy. Source alone does not settle these conflicts. Do not re-ask general WILD self/ally damage, one-family trinket stacking, ammo preservation, TMT-last, or native ownership/checkpoint policies already accepted.

Prerequisite completion requires native Shishgun item + release/preview + exact TNT/marker children + delayed terrain + both acquisition recipes (and Railgun's independent full native closure), native full rewind/TimeStop/ownership, existing special-bow/trinket pipeline integration, loot/gallery readiness and real native scenarios. Resource presence or source audit is not runtime qualification. No Java, resources, builds, servers, git or RP were changed here.
