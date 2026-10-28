Current author decisions and implementation supersede the preparation questions below: [Berserker port](BERSERK_ARMOR_PORT_2026_10_28.md). The health gate is removed, MAG alone suppresses new procs, and existing effects remain.

# Next complete armor source audit B — 28 October 2026

Read-only bounded audit. Only this new note was written; no production/resource edits, build, tests, install, original-server or resource-pack writes. Read project AGENTS jammer contract and agent_notes/AGENTS.md; the preceding NEXT_ARMOR_AUDIT_2026_10_28.md recommends Nanobots historically, now completed.

## Recommend complete Berserker armor

Finite four-piece family, seven authored loot tables, one damage-trigger passive. Current Java search finds only LbcBolts.java:99 ReforgeRule("berserk","neko_set_berserk") and LbcDamage.java:56 identity classification; no native Berserker runtime. Existing normal/Ultra reforge is a concrete acquisition route, so this is a complete family rather than a convenient isolated passive. Current build.gradle:279,382–385 packages root and four leaves, but not existing pre/full source resources. LootTestCatalog has no Berserk readiness entry.

Legacy root S = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/.

Exact runtime closure:

- S/advancement/cocksuckers.json: entity_hurt_player -> items/damaged, excludes spectator/nexus/imprinted.
- S/function/items/damaged.mcfunction:1 revokes advancement; :6 performs the complete Berserker predicate and helper call.
- S/function/armor/set_bonus/berserk4.mcfunction: instant_health with omitted duration/amplifier (native default level I), three heart particles at y+1 with spread .5/1/.5, speed0; wolf growl master at source position volume1/pitch1 to all players; Regeneration II for5seconds. No cooldown/state/projectile/damage/terrain dependencies.
- S/predicate/chance70.json: random_chance .7.
- S/function/load.mcfunction:23 creates hplvl with health criterion; load_scores_reload:23 removes it during reload.

Admission explicitly requires hplvl<=20, independent random70%, actual head/chest/legs/feet all custom_data berserk:1, and absence of stopper_magic marker within20. This is an absolute20HP threshold, not half max health. Equipment-only: carried pieces do not count. No nomagic_active condition occurs here (the adjacent Chaos branch has one). Reuse existing actual EntityHurtPlayerTrigger/pre-hurt fatal bridge used by Glass/Nanobots/Shulker, including native shield/absorbed criteria semantics; do not add another generic damage callback or duplicate Time Stop replay. Resolve the health-criterion update timing through the existing bridge/native code before replacing the source score with post-hit health; the datapack alone does not prove they are equivalent. Also preserve health criterion rounding/absorption behavior rather than assuming raw float Health.

Exact acquisition/payload closure:

- S/loot_table/neko_set_berserk/{helmet,chestplate,leggings,boots}.json
- S/loot_table/neko_set_berserk.json (one random leaf), neko_set_berserk_pre.json (five root draws), neko_set_berserk_full.json (one of each piece).
- S/function/ultra_reforge.mcfunction:23; current native LbcBolts shared normal/Ultra route.
- S/loot_table/nekolucky_items.json:14 and 6_sets.json:14 reference pre/full; transformables_{helmet,chestplate,leggings,boots}.json:16 reference leaves. These are source routes, not proof every broad pool is currently enabled; do not enable unrelated pools by inference.
- S/advancement/true_advancements/armor/berserker.json is the authored advancement (not berserk.json); existing kit integration references it. Reuse native advancement import rather than creating an unrelated unlock route.

All pieces are leather, durability400, tier_b tooltip, lbc:berserk equipment asset. Models are armors/berserk for helmet and armors/berserk_{chestplate,leggings,boots} for others. Preserve every component, modifier ID, name/lore formatting and transformable plus slot plus berserk flags. Head/chest/legs/feet armor -3/-8/-6/-2, attack damage +3/+5/+4/+3, toughness -4 each, attack speed +.15 add_multiplied_base each, movement speed +.05 add_multiplied_base each. Native signed attributes already exist; do not normalize negatives or rebuild item payloads from lore.

## Jammer decision required

MAG suppression of new healing/Regeneration proc is explicit. TECH is absent. AGENTS.md:414 says missing checks may be author omissions and requires asking before assigning exemption or new gate: ask whether TECH should suppress Berserker's proc. Do not infer TECH immunity or gate it by analogy. No universal already-granted-effect policy exists either: ask whether existing Regeneration should expire naturally after entering MAG/TECH or be removed; the source only prevents the new helper invocation and contains no Berserker-specific cleanup.

## Existing art and finite qualification

Read-only pack P = C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8.
Existing P/assets/lbc/items/armors/berserk{,_chestplate,_leggings,_boots}.json and matching models/item/armors files exist. P/assets/lbc/equipment/berserk.json references textures/entity/equipment/humanoid/berserk.png and humanoid_leggings/berserk_leggings.png, both present. Existing P/assets/minecraft/lang/{ru_ru,en_us}.json has berserker_hat, berserker_chestplate, berserker_leggings, berserker_boots, set_bonus_berserker; keep exact values. No new pack assets appear necessary; no physical-client visual proof claimed.

Reuse LbcArmorSets equipment eligibility/lifecycle, shared damage criterion dispatch, ItemEffectLevels effect chain and native instant effect handling, LbcBolts reforge, LootTestCatalog readiness, existing AttributePolicy. Qualification should cover all seven tables and actual reforge, exact payloads/signed equipment attributes, all four slots vs partial/carried sets, threshold/rounding/timing, seeded success/failure, rejected vs shielded/absorbed accepted criteria, MAG/author-decided TECH, preexisting effect chains, fatal-hit policy, Time Stop and lifecycle. No unrelated family implementation is required.

Flesh is another bounded unported candidate: four leaves+root/pre/full; changed_inventory/sel:148–150 toggles fixed max_health modifier minecraft:44a8aa48-d53d-42c0-b229-4557d3c581b1 amount2 add_multiplied_total via armor/flesh_set_bonus_{add,remove}. It has no explicit jammer checks, so requires a wider author decision. Berserker is preferable now because its MAG intent is explicit. Chaos includes its separate reactive chestplate effect selector/dependencies; Techno/MK2 remains broad. Completed families were not selected again.
