# Независимая сверка расходников и крафта

Job `lbc_other_consumables_craft`: 221 путей из `research_jobs.json`, в исходном порядке. Манифест ниже совпадает с `job.sources`; покрытие сверено с ним, не выведено из наличия автокаталога. Основной документ: [lbc_other_consumables_craft.md](lbc_other_consumables_craft.md).

## Проверенные контракты и найденные особенности

- Наземный крафт запускается секундным [`tick1s`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction), только вне Nexus/Imprinted; inventory-путь вызывается из `players_tick` при наличии `craftusable` в сетке. Выдача/получатель и расход различны; конкретный handler определяет возможность повторного вызова и число слотов.
- `crafted_spruce_leaves` отличается от прочих рецептов: `stterra_tiny` и второй item с `Thrower` запускают массовую замену всех соседних item entities на spruce leaves и удаление у них компонентов. Ни товарная выдача, ни расход catalyst в helper-е не выполняются.
- В eatables consume advancement — момент однократного потребления; reward получает игрок, а revoke/early return определяет повторную активацию индивидуально. KFC случайно выбирает ровно один исход из 20. `slab` работает по предметным слотам игроков, мобов, других сущностей и item entities; replacement loot table применяется по-slot.
- Greedstone расходуется одной единицей, увеличивает `greed_prestige`, даёт `greed_cursed`; diamond marker получает случайные координаты X/Z -130..130, Y 16..300 и ставит структуру через `schedule ... 20` (20 тиков). `players_1s` создаёт curse markers каждую секунду, пока в пределах 20 нет avarice marker; цикл marker-ов накапливает Lifetime 20 за секунду, а при 180+ формирует отдельные активные curse fields. Условия good/no-greed diamond веток независимы, значит обе могут отработать при игроках обеих категорий около одного diamond.
- Kapusta вызывается предметным egg-projectile bridge, а затем tag `kapusta` обслуживает `raycastselector_rare`. Один локальный цикл проверяет 8 точек 0..1,4 с шагом 0,2 (7 перемещений); `Distance=8` вызывает endpoint LBCR, который только сбрасывает Distance и оставляет display на той же позиции. `dx=dy=dz=0` ограничивает hit volume единичным кубом, который `distance<=5` не расширяет. Hit damage: 6 arrow (с owner, если найден игрок по lbcID2) либо 6 generic до 3,5 блока; новые melons проходят через общий raycast chain. Throw не задаёт `data.lifetime`, поэтому локальный source не задаёт конечное время жизни marker; общий runtime/lbcr service продолжает обслуживать его до столкновения или иной очистки.
- `mana_potions/select` пустой и помечен `non_runtime`; `bad` делает один случайный бросок 1..10: всегда уменьшает mana на 2000, при 1..8 уменьшает manamax на 1000, при 1..6 также уменьшает manaregen на 1. Пороги вложенные и коррелированные: исходы 1..6 дают все три потери, 7..8 — mana+manamax, 9..10 — только mana.
- Отрицательная мана делится на ветки `explode`/`damage_with_tmp_void` во внешнем caller-е `other/negative_mana_5`: взрыв создаёт 6 обычных и 15 больших синхронных проходов; threshold и общие mana-счётчики остаются в [lbc_mana.md](lbc_mana.md). Обычный/full-set/большой damage вычисляется как -mana×0.00006/0.0001/0.00025, а full-set hit пытается добавить caster +300 mana.

## Чтение и границы

Все source paths из job сверены с точным ordered manifest и соответствующими строками coverage. Ручное семантическое чтение выполнено по всему craft dispatcher/craft Ground entry and handlers, eater activation table, greed/kapusta/mana_potions/negative_mana mechanics; для еды сверялись advancement contracts и каждый reward/helper из указанного family. Где reward/helper передаёт эффект в другой владелец-пакет, этот результат не приписывается текущим source без подтверждённой связи; в основном документе дана точка вызова и ссылка на пакет/контракт.

Из внешней границы остаётся полный downstream raycast lifetime/cleanup для spawned kapusta entities: локальная ветка передаёт marker в `lbcr:raycast_end_with_tp` и общий `slowraycast/rare` servicing, а код `lbcr` не входит в этот 221-source job. Loot-table item contents и advancement grant details сверялись по соседним JSON-контрактам; их полный перечень вынесен в [loot definitions](lbc_loot_definitions.md) и [advancement contracts](lbc_advancements.md), они не являются командами этих handlers.

## Точный манифест `job.sources`

### `craft` — 87 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/ananta.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/ananta_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_acid.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_acid_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_air.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_air_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_blind.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_blind_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_booom.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_booom_1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_dust.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_dust_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electro.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electro_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electroshield.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electroshield_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electrosword.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electrosword_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_fire.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_fire_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_mana.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_mana_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_potion.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_potion_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_railgun.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_railgun_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_reforge.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_spell.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_spell_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_spruce_leaves.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_stone.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_stone_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_swargia.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_swargia_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_techno_armor.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_teleport.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_teleport_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_terra.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_terra_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_boots.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_boots_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_chestplate.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_chestplate_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_helmet.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_helmet_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_leggings.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_leggings_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/divide_by_metal_pipe.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/divide_by_metal_pipe_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fartus_nadristailo.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fartus_nadristailo_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fevgalea_star.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fevgalea_star_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fotja.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/fotja_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/gaycaster.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/gaycaster_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/ice_wand.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/ice_wand_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_make_unbreakable.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/mellstroy_mask.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/mellstroy_mask_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_boots.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_boots_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_chestplate.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_chestplate_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_helmet.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_helmet_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_leggings.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_leggings_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/overload_armor.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/reroll_machine.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shish_wall_wand.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shish_wall_wand_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded_lukcy.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded_lukcy_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/soulhunter_mace.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/soulhunter_mace_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/stand_clocks.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/stand_clocks_inv.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/swargia.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/craft/swargia_inv.mcfunction`

### `eatables` — 98 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/banana.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/bedrock_apple.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/big_explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/book.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/buffs.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/chaos_explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/cum.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/damage.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/danmaku.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/danmaku_1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_apple.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_beef.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_berries.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_carrot.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_chicken.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_mutton.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_porkchop.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_potato.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_rabbit.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_stew.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/evoker.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/fire.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/gain_bucket.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/glowing.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beef.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beetroot.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_berries.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_bread.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cake.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chicken.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chorus.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cookie.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_kelp.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_melon.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_mutton.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_pie.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_porkchop.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_potato.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_rabbit.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_stew.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/haste.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_eye.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_flesh.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/all_up_attributes.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/all_up_mana.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/blevota.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/chick_alive.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/gigaponos.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/gigaponos_mega.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/healing.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/jesus_food.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/overfeed.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/ponos.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/random_item_remove.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/satan_food.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/strength.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/tasty.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/the_win_button.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/ultradreest.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/xp_up.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/xp_up_2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/xp_up_3.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick/yumyum.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lava.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lightning.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/multiverse_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_apple.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_carrot.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_potato.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_stew.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherstar_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek_explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek_zelensky.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/poison.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/polymorph.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/psycho.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/regeneration.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/shit.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab_at_entity.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab_at_item.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab_at_mob.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab_at_player.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/snow.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/strength.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/transdimensional_fish.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/weakness.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/wither.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/eatables/yellow_lightings_bottle.mcfunction`

### `greed` — 15 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_near.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_near1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_near_greed.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_near_no_greed.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_rand_pos.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_rand_pos_coldwn.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_rand_pos_coldwn2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_spawn.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/damage.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/damage1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/damage2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/exterminate.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/fill.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/pickup.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/greed/summon.mcfunction`

### `kapusta` — 8 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/check.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode_2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode_new_kapusta.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode_new_kapusta_1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode_new_kapusta_2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/throw.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/tick.mcfunction`

### `mana_potions` — 2 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/mana_potions/bad.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/mana_potions/select.mcfunction`

### `negative_mana` — 11 source(s)

- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/explode.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_2_1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big_2.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_big_2_1.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage_restore_hp.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_full_set.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_full_set_2.mcfunction`
