# True Falling Stars + Air → Exahoming: исходник

Исследование 18.10.2026, только чтение. Корень источников `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`; ниже `lbc/` и `lbcr/` относятся к нему. Сервер/Gradle не запускались. Это статическое чтение, не подтверждённый runtime. Пользовательские правила AGENTS.md о единственном terminal, союзниках, служебных NPC и наследовании убийств имеют приоритет над дефектами legacy.

## Рецепт, предметы, входы

- Настоящий донор **truefallingstars**, не fallingstars. Определение: `lbc/loot_table/transformables_staff.json:1195`, custom_data `{craftusable:1,transformable:1,staff:1,raycastspell:1,truefallingstars:1}`; имя `falling_stars_wand`. Второй донор stair (Air).
- Земля: `lbc/function/other/craft_ground.mcfunction:14`, stair + truefallingstars на расстоянии 0.001..1 → `other/sauvojen_tuntija/crafted_exahoming.mcfunction`. Создаётся loot, удаляется stair целиком и ближайший item carrot_on_a_stick с truefallingstars (в финальном удалении уже нет distance!).
- Сетка 2×2: `lbc/function/other/craft/inventory_craft_check.mcfunction:14` → `other/sauvojen_tuntija/crafted_exahoming_inv.mcfunction`: loot на землю, уменьшение каждого подходящего слота обоих типов на 1. Academy флаги доноров не проверяются и в результат не наследуются.
- Результат `lbc/loot_table/tuntijaa_air_homing.json`: carrot_on_a_stick, tier_s, item_model `lbc:homing`, имя exahoming_wand, `{transformable:1,staff:1,raycastspell:1,exahoming_wand:1}`. Academy `interacted_upgrade.mcfunction` добавляет upgraded:1, mode:1 и lore; `upgrade/exahoming_wand.mcfunction` меняет tier_x.
- Живые ветки обеих рук: `lbcr/function/raycast.mcfunction:7,25`, `raycast_left.mcfunction:7,25`, `raycast_upgraded.mcfunction:6,14`, `raycast_upgraded_left.mcfunction:6,14`. Mana gates 4000 truefallingstars / 2000 exahoming; stopper_magic радиус20. Цены заданы `lbc/function/config.mcfunction`. Обычные касты списывают их один раз; смена Academy режима на sneak бесплатна, но проверка входного порога остаётся. Есть общие mage/trinket hooks.
- Дополнительные вызовы Exahoming: `lbc/function/items/libspells/select{,_wand}.mcfunction` spell204..207; `workingitems/random_spell{,_lib,_upgraded}.mcfunction` mode46; `random_spell_lol.mcfunction` spell204..207; `items/wands/sauvojen_wand/flying_wand_tick_air_upgraded.mcfunction` варианты4..7. Их внешний расход относится к вызывающему заклинанию.

## True Falling Stars

`lbcr/function/raycast_loop5cd.mcfunction` → `raycast_loop5.mcfunction`: мгновенный луч, шаг0.5, Distance<=800 (~400 блоков, вопреки lore200). Terminal при не-air или мобе в ячейке dx=dy=dz=0 с distance<=15, без spectator/raycaster; антимагия16, stopper20 останавливают продолжение. `raycast_end5.mcfunction`: 14 native fireball, Motion(0,-0.2,0), первые13 power2, последний power4; высоты50,75,…375; XZ последовательно (0,0),(0,3),(3,0),(0,-3),(-3,0),(2,2),(2,-2),(-2,2),(-2,-2),(1,0),(0,1),(-1,0),(0,-1),(0,0). Нет семейного TTL/ведения/owner UUID. Урон и блоковое разрушение ванильные, power не равен фиксированному радиусу или урону. Advancement при Distance>=300 и мобе<=7.

Academy `raycast_loop5cd_upgraded{,_left}.mcfunction`: три режима; луч `raycast_loop5_upg.mcfunction` шаг1, <=200. Default тот же дождь; Big (`raycast_end5_upg_big`) один fireball power15 на +400Y; Fire rain (`raycast_end5_upg_rain`) marker +80Y lifetime100t. `lbc/function/other/blazestorm.mcfunction` каждый tick создаёт3 power1 fireball с исходным Motion вниз0.1, затем `universal_pos_razb_15_active` даёт разброс. Marker тикает через tick_not_timestopped → raycastselector → raycastselector_other. Lifetime проверяется после вызова эффекта.

Испытание `raycast_loop5cd_upgrade.mcfunction`: обычный каст также выполняется, затем14 fireball Motion вниз0.2: (0,+150,0)p1; (0,+175,+3)p2; далее высоты1100,1125,…1350 p5 с теми же XZ как у обычных позиций3..13; последний +1375 p4. Сразу `can_upgrade`, выживание не проверяется. Это не исправление на110/125: фактические числа именно1100+. `lbc/function/other/magic_academy/upgrade/truefallingstars.mcfunction` даёт tier_a.

## Exahoming

Все файлы эффекта: `lbc/function/workingitems/awaken_tuntija_wands/exahoming_*.mcfunction`.

Обычный click → click_loop: мгновенный луч шаг1, Distance<=150 (151 проверяемая точка при старте0), firework trail, столкновения как у донора, антимагия16/stopper20. На terminal `click_end`: чёрная фигура частиц на+10Y (12 направлений, последний угол320), 10 fireball power2. Координаты относительно попадания: (0,40,0),(8,50,0),(0,60,8),(-8,70,0),(0,80,-8),(12,90,12),(-12,100,-12),(12,110,-12),(-12,120,12),(0,130,0). get_id присваивает lbcID2/team_number, но не native Owner. Семейного TTL нет.

`lbc/function/tick5t.mcfunction:4-5` каждые5t зовёт `homing_with_id{,_slow}.mcfunction`, кроме nexus/imprinted. Gate требует существование какого-нибудь моба distance8.., затем выбирается ближайший моб **без** ограничения8 и верхнего радиуса; владелец и его команда исключаются через временный nohomo, strict_map_object/spectator исключены. `raycast_vpered_slow.mcfunction` полностью заменяет Motion направлением к глазам *2; `raycast_vpered_slower.mcfunction` — *0.5. Между коррекциями действует native fireball физика. Нельзя описывать это как плавную интерполяцию или кольцо8+.

Academy четыре режима (`click_upgraded{,_left}`, `click_upgraded_pre`):

1. Default: обычные10 power2.
2. Sisyphus: тот же луч, один `homing_fireball_slow` power100 на+40Y; та же фигура частиц.
3. Destiny: marker в глазах, lifetime600t, owner ID/team; тикает каждый незамороженный tick через `tick_not_timestopped`/`raycastselector_other`. `dmgwithid_un_playeronly` исключает владельца/команду. Если есть цель<=30, шанс5% (`lbc/predicate/chance5.json`) пускает мгновенный луч к ближайшей; шаг0.5, <=60 (~30 блоков), terminal блок/моб<=5; на terminal **3 TNT fuse0**. Если нет цели<=20, marker движется0.3/t к ближайшей цели20.. без верхней границы. Поэтому на дистанции20..30 может одновременно атаковать и двигаться. TNT без native owner; семейного контроля friendly fire взрывов нет.
4. Curse: луч шаг1<=150, проходит блоки, ищет моба в ячейке distance<=8. `mode_curse_get` повышает target на60 у @e с dx/dy/dz0,distance8,!raycaster,limit1: там потерян фильтр mobs/spectator и sort. Раз в секунду `tick1s.mcfunction` → `other/target_curse.mcfunction`: ВСЕ projectiles измерения направляются к ближайшей проклятой цели; ВСЕ raycasters разворачиваются к одной цели без sort. target уменьшается1/сек, поэтому60 означает60сек; повторные применения складываются. `raycast_vpered_pre` снимает собственный lbcID2 при совпадении проклятого игрока; target_curse_rc снимает также team_number. Это legacy возможность превратить свои снаряды против себя/команды, требующая явного пользовательского решения.

## Academy и дефекты

Открытие задания `other/magic_academy/interacted_2.mcfunction` ставит upgradable:1 и зовёт can_upgrade/{id} (только текст условия). Обычный sneak-cast при upgradable запускает испытание **после обычного эффекта**, причём проверяет SelectedItem даже при касте второй рукой. Exahoming trial `exahoming_click_upgrade`:5 fireball homing_fireball power20 в (0,+500,0),(50,+450,0),(0,+450,50),(-50,+400,0),(0,+400,-50); native motion/owner/active ID не задаются. Сразу `other/magic_academy/can_upgrade` → `can_upgrade_book`: upgrade_available:1, upgradable:0; возвращение NPC добавляет upgraded/mode и tier_x. Условий выживания/попадания/ожидания нет.

Два независимых terminal if (моб и блок) способны удвоить эффект: перенос должен исправить согласно уже принятому user правилу единственного terminal. Начальный Distance сбрасывается внешним вводом; прямые библиотечные вызовы требуют проверки caller. Ground craft удаляет весь донор-item, inventory handler расходует все совпавшие слоты; переносить как нормальный рецепт1+1 через общий helper.

Нужны ответы пользователя о сохранении power100, глобального Curse60сек, высот/немедленной готовности обоих испытаний и фактической дальности400б донора против lore200. Native fireball TTL из datapack установить нельзя: явного TTL у этих шаров нет, поведение при выгрузке/удалении движком требует отдельной проверки native кода. Owner прямого/взрывного/огненного и отложенного урона в переносе обязателен; отсутствие UUID/автора у TNT не является контрактом сохранения.


## Ответы пользователя — 19.10.2026 (user)

Sisyphus сохраняет мощность 100. Оба испытания сохраняют исходные высоты, все 14 шаров донора / пять шаров результата и немедленную готовность улучшения.
Лучи работают без фиксированной дальности, пока загружены чанки; старые пределы выше описывают только legacy.
Curse глобален. Последнее правило владельца приоритетно: ничейные снаряды и снаряды проклятой цели переходят заклинателю; снаряды третьего игрока сохраняют владельца.
Перечисленные выше вопросы закрыты этими ответами. Реализация и пределы проверки — [EXAHOMING_PORT.md](EXAHOMING_PORT.md).
