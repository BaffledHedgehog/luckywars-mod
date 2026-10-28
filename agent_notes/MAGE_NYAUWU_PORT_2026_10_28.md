# Mage and Nyauwu armor — 28 October 2026

Source closure: [read-only audit](MAGE_NYAUWU_SOURCE_2026_10_28.md). This entry supersedes its open questions. Native qualification is recorded below when complete.

## Author decisions (user)

- Suppression: «магическим». All new Nyauwu bonuses use MAG; TECH does not suppress them.
- Helmet and leggings proximity: «Сохранить условия легаси». Another player, including an ally, within 0.01–10 blocks adds 20 persistent maximum mana each eligible tick. Leggings double the entire base+belt+Mage rate only without another entity from the original `minecraft:mobs` tag within that distance. That tag includes players. Boost and plague remain subsequent shared operations.
- Full-set timing: «Только после успешного заклинания». Use the existing successful `wandCasted` callback once; rejected/insufficient-mana input cannot proc.
- Full-set damage: «только магических/призванных мобов, и давай 25 урона а не 100». Keep radius 30 and ARROW damage, explicitly exclude players. Ordinary foreign mobs receive no damage. «Да, владельцу комплекта»: source credit belongs to the actual wearer, replacing the legacy nearest-enemy-player selector.
- MAG at the target: «Да, антимагия защищает саму цель». Suppress both damage and healing for a target inside MAG, even if the caster is outside.
- Chest hit trigger: «Только настоящий ближний удар». Reuse accepted native melee damage with direct entity == attributed player and `PLAYER_ATTACK`; summoned/pet/spell/arrow attribution cannot activate it.
- Chest amount: «Да, ×70 без накопительного перемножения». Each accepted hit restores current ordinary displayed regeneration ×70, without rewriting that rate or compounding it. Preserve source plague refusal and the shared signed/capped mana update. Arithmetic is calculated in long and bounded to signed int.

The read-only author RP describes the chest as “Ускоряют восстановление маны. Игнорируют предел маны, если находиться в воде. Удары восстанавливают ману.” It does not specify the hit amount. The actual helper `other/manaregen_hit` multiplies the mutable display rate by 70; the author explicitly replaced repeated multiplication. Source water helper adds exactly one mana per tick while full in a tagged water block; retain that over-cap behavior.

## Native ownership and acquisition

Keep existing `LbcMana` Mage per-piece 4/6/7/4 regeneration and shared successful-cast +150 maximum-mana hook. Every original Nyauwu leaf has BOTH `nyauwu:1` and `mage:1`, including mixed full-Mage sets. No second Mage cast callback or private mana account is added.

`LbcNyauwuArmor` owns Nyauwu additions through `LbcArmorSets` admission/equipment/lifecycle. Boots use only original movement modifier `minecraft:916c89e6-ae62-4b11-91d7-10e7e57c726d`, amount 1 `ADD_MULTIPLIED_TOTAL`, while mana is below maximum. Remove it on unequip, MAG, ineligible roles, forget and stop; keep unrelated movement modifiers. Paused gameplay retains the existing modifier unless cleanup is required. No independent clock, health tracker or teleport exists.

Pulse heals allied nonplayer source mobs first: 10 heart particles and native Instant Health II (Instant Damage II for the original unholy tag). Then apply typed 25 ARROW damage to foreign magical/summoned mobs through the normal native pipeline. Keep Greed/roles/Time Stop/indirect damage services authoritative. Source has no line-of-sight restriction. Native team aliases are supported instead of hard-coding the datapack’s 32 repeated command lines. Resolve actual summon/Metasola owner friendship so an owner’s teamless Metasola hitbox is allied.

Expose existing `LbcMagic.magical` classification instead of copying its raw magic/summactivated/type-tag conditions. Include real, currently managed Soulless actors through their existing ownership service: their damageable pig carries a different tag from its magical visual. This does not classify every attributed NPC as magical. Soulless already removes its actor/visual pair under MAG; this shares that established classification with armor targeting.

There are 13 real source tables: seven Mage (root, four leaves, pre, full), six Nyauwu (`neko_set_nyauwu`, four `neko_set_femboy/*` leaves, `neko_set_femboy_full`). There is no invented Nyauwu pre table. Package and expose exactly those tables, retaining exact original item payloads, attributes, names, models, lore and normal/Ultra reforges. Do not enable unrelated broad LuckyBlock pools.

`LbcNyauwuCrafting` supports original same-slot Mage+Phantom fusion in the native grid and through lifecycle-tracked dropped items. Match raw flags and original equipment asset/slot, consume exactly one unit from each donor, retain surplus components/counts and publish ground output before debit. Preview is side-effect free. Ground uses the original one-second cadence and source dust/enchantment sound; no global entity scan, mana or jammer gate. `LbcCraftingRecipe` owns native take/remainders.

All original author RP models/equipment assets exist. No resource-pack update is required. The client RP and all pre-existing author changes are read-only to this patch.

## Verification

First complete-source candidate `6b0f9ce2` was rejected in both actual matches because the water-block tag was not included in the JAR. Both matches otherwise continued through all neighbor suites; child stopped normally and snapshots were restored. Preserve its proof, results and native log under `build/verification/mage-nyauwu/initial-6b0f9ce2/`. Fix the real missing resource by packaging the exact original `data/minecraft/tags/block/water.json`, retaining the original water and water-cauldron entries. Do not weaken the native water/over-cap assertions.

Final `4549c289c9dfc22059e4253b87ae1c3b1402f2732be388615917741bb6aa7c51` passed 34 recorded results in two actual mode1 matches (101.59 seconds). Exact 560 Java sources, 1585 classes, 3246 resources and build.gradle agree with the archived artifact. All 198 pre-existing author paths and 29 live grimoire overrides remain byte-exact. Native child stopped normally, all fixture snapshots restored, no final crash signatures. No forced main shutdown/restart was performed.

`MageNyauwuChecks` covers exact original eight leaf payloads and all 13 tables; consumed normal/Ultra reforges; mixed Mage compatibility, per-slot mana, successful/failed actual casts; actual native grid ResultSlot pickup and tracked ground fusion for all four parts; precise proximity/boost/water over-cap; source boots ID and MAG/TECH/roles/freeze cleanup; accepted native direct melee twice without compounding, arrow/MAG/unequip controls; 25 typed damage, ordinary/player/radius/role exclusions, ally/unholy native healing, target MAG, partial set and failed casts. Actual owned and foreign Metasola actors prove shared magical classification and logical friendship without adding a fixture team to their hitboxes. Neighbor Flesh/Berserk/Glass/Nanobots/Shulker/incoming-artifact/attribute/effect-layer/shadow/Time Stop suites pass in both matches.

Evidence is in `build/verification/mage-nyauwu/{artifact-proof,native-results,qualification}.json`; the stopped-only installation worker records actual status in `main-install.json`. Physical-client pixels, actual socket reconnect/death and natural peak TPS are not claimed. Existing author RP equipment/item assets were checked read-only and need no template additions.
