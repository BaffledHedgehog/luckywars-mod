# Railgun prerequisite source audit — 3 November 2026

Historical source audit written before implementation. For current implementation, author decisions and qualification, see [Supernova chain integration](SUPERNOVA_CHAIN_PORT_2026_11_03.md). Readiness verdicts below describe that earlier audit.

Read-only audit; only this note written. L = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc; R = sibling lbcr. Project = E:/servers_mew/skywars2_last/luckywars-mod. Correct original Paper tree = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars.

## Readiness and presentation

Exact railgun_bow search in both project and Paper src/main/java has no matches. AGENTS and REACTIVE_FAMILY_PORT explicitly gate Railgun/Shishgun. Imported loot is not a native handler, and no registered Paper override was demonstrated by literal flag search. Do not wrap arbitrary original functions or infer an unrelated futuristic-gun implementation supplies this closure.

L/loot_table/railgun.json gives one bow, model lbc:railgun_bow, tooltip lbc:tier_b. Name railgun_bow white italic false. Lore: joined aqua type + bow.lbc; white railgun_bow.lore and .lore.1; joined literal Stendari green + literal ' Beta Server Harem event' dark_gray. All italic false. Flags {craftusable:1,transformable:1,bow:1,railgun_bow:1}; no source enchantment or cooldown/mana.

## Tiny Electro + bow acquisition

other/craft/inventory_craft_check:52 requires stelectro_tiny in any player.crafting slot plus bow excluding railgun_bow. crafted_railgun_inv spawns lbc:railgun first, emits enchant particles100 and enchanting-table sound, reduces one from EVERY eligible bow slot0..3, then chance60 returns without consuming Tiny Electro; otherwise reduces one from FIRST matching Tiny Electro slot0..3. Thus catalyst is spent40%, bow(s) always spent. No weapon component inheritance is authored: output is fresh source loot.

other/craft_ground:53 runs as stelectro_tiny item when 'unless data' test says nearest bow at .001..1 does not contain railgun_bow. This is not a positive bow-exists check, so preserve awareness of the historical missing-bow edge rather than silently treating it as intentional economy. crafted_railgun spawns output/visuals, kills one eligible bow ENTITY within1 (whole stack), then chance40 kills one Tiny Electro ENTITY within1 (whole stack). Both final selectors lack explicit sort. No source success-probability gate on producing Railgun.

Tiny Electro's literal flag is emitted by loot_table/random_sauvojen_wand_tiny.json. This bounded read does not independently qualify that producer. Railgun + Reactive Shish then crafts Shishgun; Shishgun + Shit Katana is Supernova's acquisition chain. All prerequisite readiness must precede full exposure.

## Release, jam and range

items/bows main SelectedItem railgun_bow dispatch is suppressed ONLY by TECH stopper marker within20, not stopper_magic. R/railgun repeats the TECH marker check at each NEXT local+1 sample. First admitted sample always executes. Homing300 uses the same TECH continuation check. No explicit artifact selector here. Shishgun differs: release MAG+TECH and no traversal rechecks. These are genuine per-weapon jammer policy decisions if the author has not answered them already; do not infer 'magic bow' from presentation.

workingitems/railgun_bow grants hidden Resistance1second amplifier4, sets Distance0, range charge*4 when charge>=20, otherwise floor(charge/2), caps300. It ALWAYS starts eye+2 (even range0), increments Distance once per sample, recurses1 block while range>Distance; no terrain collision stop. Thus at least one sample and otherwise range samples at offsets2..range+1. There is no ballistic velocity, cooldown, extra ammo debit, numeric direct ray damage, or random base firing chance.

It reads nearest #minecraft:arrows within6: raid_omen amplifier into tmp, overridden by custom colors6710877 ->300,6710881 ->301,6710882 ->302. Non300..302 with custom effects calls railgun_bow_effect_store; base potion calls railgun_bow_effect_store_glackmojumba. These helpers inconsistently read nearest arrow within2, not6. Then nearest arrow within6 is killed. This unsafe original entity search must become exact captured native ammo/family ownership, preserving current ALL-bow ammo policy rather than stealing foreign arrows. Grants railgun advancement if global speedy>=300. Adds transient owner tag during ray and clears owner/have_effects afterward. Release trident.return volume10 pitch2 and explosion sound volume.3 pitch.8.

## Samples and children

R/railgun: Distance++; tmp>=100 -> railgun_lol; unless tmp302 summon immediate creeper ExplosionRadius1, Fuse0, ignited, Invulnerable, Silent, NoGravity, NoAI. Emits explosion and cyan dust particles. If have_effects and eligible noncreeper #mobs within3 excluding owner/strict_map_object, calls railgun_effect macro. Macro applies effect to all noncreeper #mobs within3 excluding owner; the final selector itself does not repeat strict_map_object guard.

Base effect store copies ONLY first custom effect id/duration*.05/amplifier. Potion-name helper maps known vanilla potion strings to one effect, default amp0/duration15seconds; strong amp1, long30seconds. Turtle Master maps only resistance. This deliberate source loss conflicts with current ALL-effects preservation policy; do not repeat loss or transport effects by clobbering existing potion data.

tmp300 homing: after first sample, each subsequent step faces nearest noncreeper #mobs excluding owner/spectator/strict_map_object, with no explicit distance bound or same-team filter. tmp301 adds another radius1 immediate creeper at EVERY sample in addition to base creeper. tmp302 suppresses base creeper; it still runs particle/effect/traversal branch. Children have no source Owner/author assignment here; native shared causal ownership/epoch/WILD policy must supply it.

R/railgun_lol:
- 101: chance35 each sample -> TNT fuse1.
- 102: for each #mobs distance.01..8 excluding spectator/owner/same_team, if no fang within1 -> projectiles/arrows/evoker (nine fangs center + eight offsets).
- 111: railgun_lol_kiuas fills5x5x5 fire replacing #minecraft:airs, applies raid_omen10seconds amplifier111 to noncreeper mobs within3 excluding spectator/owner.
- 103: chance10 each sample -> Erecto sphere tick5t: sixteen shield_blockable/mecha/player slowraycast raycastmolnia_phantom markers lifetime8, random rotation, copied lbcID2/team, actived. Must reuse native Erecto/phantom-ray child implementation rather than visual-only imitation.
- 112: lightarrowtrail magic/other marker lifetime200. raycastselector_other -> inairtrail: end-rod every active tick, transition flashes at50/100/150, inairtrail2 at199, kill>=200. Reuse existing native Quiver light-trail end payload.
- 113: trappertrail magic/other marker lifetime400. selector -> inairtrappertrail: once Lifetime>=120, chance25 each tick with player within80 -> evoker_rand. Reuse existing native Quiver trap-trail child lifecycle.
- 301: the extra immediate creeper above.

The ordinary base damage is native creeper explosions; variant child damage/fire/effects retain own native mechanics. Existing WILD self/ally/protected-role and causal-credit rules already apply; source homing/fang selectors have their distinct acquisition filters. No new general WILD question is needed.

## Integration gate

Use one successful native captured-family transaction, current trinket composability, all actual ammo effects/components, no nearest-entity stealing, no per-child trinket reroll/mana spend, and native owner transfer/TimeStop/full rewind/terrain barrier/epoch cleanup. Shulker/Madoka and trinket arrow layers are already implemented per BOW_TRINKETS_SPECIAL_BOWS_2026_11_02.md; older provisional audit statements are superseded. Shared OriginalAmmoPipeline here means the agreed architecture, not a confirmed existing class spelling.

Still author-dependent: precise mapping of ALL ammo/trinket effects onto a ray that consumes its carrier (parent is grouping this with Shishgun), source-specific TECH-only vs changed jam policy, and source crafting debit/missing-bow quirks unless already covered by author recipe policy. These are separate from already resolved all-bow preservation and single-family stacking requirements. TMT remains LAST, no arbitrary function bridge.

Native readiness requires item + exact source range/charge/release + all special-ammo children/effects + both recipes + upstream catalyst readiness + delayed native services + full ownership/checkpoint/TimeStop and actual scenarios. This bounded source note is not runtime qualification. No Java/resources/build/server/git/RP changes.
