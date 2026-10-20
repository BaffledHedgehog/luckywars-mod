# KFC: исходное поведение (20 октября 2026)

Исходник только для чтения: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Ниже пути относительно него. Это аудит, не решение исправлять странности.

## Ввод и курица

`function/workingitems/spell1_default.mcfunction:37` и `spell1_default_left.mcfunction:12` вызывают `workingitems/kfc`. Offhand явно проверяет custom_data kfcgun:1, cooldown coldown17<=0 и отсутствие stopper-marker в R20. Mainhand содержит **unless score @s[SelectedItem kfcgun] coldown17 matches 1..**, а отдельного if item в строке нет: пустой селектор способен открыть ветку без KFC. Нужен ответ владельца о желаемом поведении; переносить случайную активацию молча нельзя.

`workingitems/kfc.mcfunction`: baby chicken в глазах +1 вперёд, HP/maxHP30, Fire2000 тиков, visualFire=true, Age=-19980, EggLayTime1999980, DeathLootTable empty; tags chick/notstarted/lucky_mob. Вектор Motion=6*look с округлением data-get, cooldown20 тиков; расхода боеприпаса/маны нет. Legacy временные marker reikast и глобальный remove notstarted — технические селекторы, owner отсутствует.

**Собственного урона при столкновении нет.** Это обычная движущаяся курица с native физикой, огнём и получением урона. `mobs_tick_lbc.mcfunction:2` вызывает `other/fire_pre` для chicken tag chick, кроме time_stopped>=1. В fire_pre при HurtTime9 вызывается fire; fire читает integer Health и при 1..20 вызывает fire2. fire2 уничтожает курицу и создаёт на Y+1 marker chickblazespell/activatedblaze/rc_1sec/magic с data.lifetime60. Убийство/попадание, обнуляющее HP, не обязательно оставляет живую курицу для этого условия.

Затем fire_pre читает Fire; **строго Fire==0**, не <=0, вызывает fire_drop_kfc: звук levelup 0.5/1, cloud10, loot kfc_yumyumyummy и kill. В extinguished/отрицательных значениях нужна осторожность: точный legacy equality следует обсудить, а не автоматически расширять.

`raycasters_1s` увеличивает Lifetime на20, вызывает турель, затем удаляет при Lifetime>=60: три запуска при штатном отсчёте 0. `other/spawnfireballchick` наводит на ближайшего #minecraft:mobs (включает player), исключает chick/rcdenyalways/spectator/nomagic_active/strict_map_object, без команды/owner и ограничения радиуса. Без цели fireball не создаётся. `raycastfireballchick(_2)` создаёт native small_fireball с can_deleted и скоростью1.6*look, без Owner; без отдельного ускорения, с обычной физикой/огнём. Визуальный dust и blaze.shoot. Дальнейший огонь должен наследовать автора при переносе по требованию пользователя.

## Еда и все исходы

`loot_table/kfc_yumyumyummy.json`: 1 cooked_chicken; model lbc:kfc_yum, tooltip lbc:tier_u, custom_data kfc_chick:1, translated name kfc_chick/lore kfc_chick.lore.1; native nutrition/consume. `advancement/eat_kfc_chick.json`: consume_item с custom_data, запрещены измерения minecraft:nexus/imprinted. Rewards `function/other/eatables/kfc_chick.mcfunction`: равномерный random1..20, каждый исход5%; затем выдача shooter/kfc_gun advancement и revoke самого consumption advancement. Каждая ветка печатает yum_* сообщение.

Числа эффектов далее — command amplifier, то есть отображаемый уровень на1 выше; длительности в секундах.

| random | Функция в other/eatables/kfc_chick/ | Результат |
|---|---|---|
|1/2/3|xp_up/_2/_3|+1/+5/+15 уровней XP|
|4|random_item_remove|Равномерный слот1..38: inventory0..26, hotbar0..8, helmet, offhand заменяется loot govno_lot_2 (64 govno), даже пустой слот; это не просто удаление|
|5|healing|instant_health1s amp99|
|6|strength|strength20s amp3 + thunder|
|7|all_up_scores_no_mana|+1 ко всем явно перечисленным scores (см. полный исходник), tag have_lucky_effects; reverse_mana встречается ДВАЖДЫ (+2)|
|8|all_up_attributes|base armor/toughness/attack_damage/luck/max_health +1; attack_speed `(int)(base*5)+1` затем /5; knockback_resistance `(int)(base*10)+1` затем /10; movement_speed `(int)(base*100000)+2000` затем /100000. Вызывает 8 общих other/attributes/* функций, их частицы/advancement revokes|
|9|all_up_mana|mana/manaregen/manamax +1|
|10|ponos|hunger5s amp255, sneeze4000, shitting_1 pitch0|
|11|blevota|nausea20s amp4, falling_honey100, vomiting sound|
|12|tasty|saturation1s amp100|
|13|gigaponos|hunger10s amp255 hidden; loot govno_lot (10 rolls по3 govno); если #lbcskill==1 и skill9: mega buffs на10s regen10, instant_health10, fire_resistance0, haste10, resistance4, strength2, speed9|
|14|ultradreest|hunger20s amp255 hidden, levitation20s amp10, gigaponos +=400, sound shitting_2 vol20|
|15|jesus_food|levitation/glowing10s amp0 hidden, time set noon (глобальный мир), angelic sound|
|16|the_win_button|loot spawn lbc:win_button|
|17|overfeed|slowness30s amp3 hidden, saturation600s amp20 hidden|
|18|satan_food|loot spawn lbc:penis_cultus, illusioner.cast_spell|
|19|chick_alive|обычная chicken у глаз; 4 chicken sounds|
|20|yumyum|только сообщение|

`function/other/effect_gigaponos.mcfunction` вызывается players_tick при gigaponos>0: КАЖДЫЙ ТИК spawn loot govno, dirt/brown_wool particles5+5, rocket_power sound для R50; дополнительные shitting sounds при таймере350/300/250/200/150/100; decrement1. Итого400 отдельных item-spawn за полный исход14 (если не прерван), без ускорения/тяги. leave_death сбрасывает timer0. Это существенная нагрузка, не следует воссоздавать глобальный поиск ради эффекта.

## Граница и готовые механизмы

Gun+еда — не только один projectile: ветка7 затрагивает >100 legacy scores, включая модули ещё не перенесённые. Например cyborgBossFly, nanobots_tier/gens_type/greed_prestige, gatling_fovos, november, acid_rifle, screamray, laser_minigun_*, nexus_roadgun_cd/nexus_corrupter_cd, pulsar_cd. Это сохранение чисел для будущих обработчиков, не обязанность переносить эти предметы в KFC. Уже реализованные mana/nomagic/reverse_mana, food_count, bow_charge, blaze и другие имеют собственных владельцев состояния: голая запись scoreboard может не обновить Java state. Нужна сверка каждого действующего score с API владельца. Одноразовый список score-изменений сам по себе дешёвый, фонового обхода не требует. Полный список авторитетно в all_up_scores_no_mana; не заменять абстрактным «все scores», т.к. не все, часть технические и есть дубль.

Существующий `LbcShooters` владеет оружием/вводом; `LbcBlaze` уже имеет state turrets, native SmallFireball и цель nearest, но текущая турель имеет другой lifetime/velocity и team semantics: переиспользовать механизм с явными параметрами, не копировать как идентичный KFC. `LbcWands.launchFire`, LbcIndirectKills/LbcElectro owner, общая защита целей и lifecycle LbcItems — общие пути owner/огня/смерти/конца матча. `ConsumableSkillMixin` -> GameController.consumed -> MatchEvents.onConsumed уже даёт событие до расходования и native food effects; добавить KFC туда через профильного владельца, без scan инвентарей/advancement polling. `LbcMana` хранит signed resource adjustment API; LbcItems хранит resetAttributes snapshot для nano penalties: не создавать несогласованный второй reset. Эффекты еды через native MobEffects + общий attribution/cleanup; gigaponos учитывать только у активных едоков и тикать их конечный timer. Курицы — tracked load/unload + damage callbacks; использовать native физику и не raycast каждый тик.

Вопросы перед реализацией: mainhand gate; Fire==0 semantics; кому и можно ли союзникам стрелять турели (legacy нет owner/team); сохранять ли каждый технический score+1 и дубль reverse_mana; time set noon; обычная chicken исход19 без owner/lucky_mob; item-spawn400 и sneeze4000. Это спорные семантики, не помеченные здесь как ошибки и не изменённые аудитом.
