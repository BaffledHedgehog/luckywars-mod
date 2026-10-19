# Summonervexes и Psychovex: исходник, 18 октября 2026

Исследование исходного datapack, без изменения Java и старого сервера. Все ссылки ниже — read-only источник. База ссылок: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Ввод, стоимость, крафт

- [config](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/config.mcfunction): `vex_soul_mana_use=5000` (56), `psychovex_wand_mana_use=1000` (92).
- [spells](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/spells.mcfunction): строки 9/15 направляют main/offhand summonerwand, запрещают nomagic_active, ауру антимагии игроков и stopper_magic до20; offhand учитывает lefthand.
- [main](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spellsummoner.mcfunction): обычный ПКМ без sneak, порог маны; строки4/10 vexes/psychovex. [offhand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spellsummoner_left.mcfunction): vexes есть, psychovex отсутствует. Sneak у любого summonerwand вызывает [anger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/anger.mcfunction): радиус30, союзным мобам кроме игроков/guardian/ghast причиняется мизерный урон от ближайшего врага для агрессии; временно добавляется и снимается knockback resistance. Цена не списывается.
- [craft_ground:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction) и [inventory_craft_check:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction): `stblind + summonervexes → tuntijaa_blind_vex` (Psychovex). [Ground result](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychovex.mcfunction) убивает обе item entities целиком; [inventory result](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychovex_inv.mcfunction) уменьшает каждый подходящий стак в четырёх слотах на1.
- [Psychovex loot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/tuntijaa_blind_vex.json): `{transformable:1,summoner:1,summonerwand:1,psychovex_wand:1}`. [Vex soul loot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_summons.json): `{craftusable:1,transformable:1,summoner:1,summonerwand:1,summonervexes:1}`.

## Summonervexes

[Полная функция vexes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/vexes.mcfunction): удаляет прежних summvex всей команды1..32; призывает4 vex в кресте X/Z ±1, Y+1. HP5, follow_range200, железный меч без дропа, PersistenceRequired; собственного TTL нет. Каждому назначает команду, `lbcID2`, `team_number`, summactivated. Цена5000 один раз. Вызывает общий `on_summon` trinket hook. Owner UUID/наследование убийства игроку в этой функции отсутствуют: это пробел исходника, подлежащий исправлению по действующему user-контракту.

## Psychovex

[psychovex](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/psychovex.mcfunction): при pitch85..90 призыв в2 блоках впереди самого игрока (yaw, pitch0). Иначе выбирает ближайшего другого игрока gamemode!=spectator, исключая nexus, без проверки команды и без предела дистанции. [spawn_pre](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/psychovex_spawn_pre.mcfunction) равновероятно выбирает сторону ±2 по X/Z локальной горизонтальной системы. Списывает1000 дважды; затем psychovex списывает ещё1000. Поэтому фактическая цена1000 вниз /3000 возле другого игрока при едином входном пороге1000. Если другого игрока нет, базовая1000 всё равно тратится.

[spawn](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/psychovex_spawn.mcfunction) равновероятно создаёт одного из6 культистов. Всем silent, persistence, пустой loot и нулевые drop chances, follow_range100, внешний вид через cultus armor. После призыва psychovex назначает ближайшему ещё не активированному культисту команду и ID владельца. TTL, cap и замены прежнего культиста нет. Только этот внешний путь выполняет trinket on_summon.

| Вариант | Native mob / HP | Специальное поведение |
|---|---|---|
| strangeness | wither_skeleton /30 | Лук; раз3s проверка врага до20, marker на Y+2.5, задержка1s, разрушающий луч |
| chaos | skeleton /50 | Heart of sea; раз2s при враге до50 равный выбор1..10 способностей |
| luck | skeleton /40 | Totem; knockback resistance10; раз5t стреляет по ближайшему врагу до12, при HurtTime9 —4 случайные стрелы |
| omnipresence | skeleton /40 | Лук; при HurtTime9 многократный teleport+стрела возле каждой враждебной цели до50 |
| obsession | skeleton /10 | Алмазный топор, attack11, speed0.25; каждый тик врагам до2.5 добавляет twitching5 (syringe15) |
| hypocrisy | zombie /60 | Два железных меча, speed0.2; раз2s instant_damage1 amplifier0 (для undead это лечение) |

## Dispatcher, таймеры и способности

[load](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/load.mcfunction) запускает циклы tick2s/tick3s/tick5t. [tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick.mcfunction) направляет обычный tick при отсутствии time_stopped в [tick_not_timestopped](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_not_timestopped.mcfunction), далее все mobs → mobs_tick → lucky_mob → mobs_tick_lbc → skeleton+summoned_cultist → [mobs_tick_cultist](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_cultist.mcfunction). Это HurtTime9 luck/omnipresence и obsession каждый тик.

[tick5t](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick5t.mcfunction) → mobs_5t → mobs_5t_lbc → luck_arrow_spam. [tick2s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick2s.mcfunction) → [tick2smobs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick2smobs.mcfunction): chaos/hypocrisy. [tick3s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick3s.mcfunction): strangeness. Все три schedule-цикла исключают nexus/imprinted; не следует автоматически приписывать им tick-guard time_stopped.

Способности лежат в [cultists](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/). Strangeness: stranger_3s_pre помечает owner/team nodmg; stranger_3s создаёт lifetime21 marker и schedule stranger_3splus1 через1s. Последний работает по всем strange_destroyer markers, у каждого луч21 шаг по1, урон1 outside_border каждой цели в2.5 на каждом шаге без dedup; fill3×3×3 air strict разрушает любые блоки. Marker имеет ID/team, но damage не имеет by owner. Это реальная опасная механика, не упрощать без ответа пользователя.

Chaos: [2s_yes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/2s_yes.mcfunction) выбирает StrengthIV8s; SpeedV8s; ResistanceIV8s; fireball ExplosionPower2; splash7 arrow всем врагам до30; trident damage0.2 до75; до10 целей в1.5..50 по9 evoker_fangs вокруг; stun ближайшей цели без радиуса (stone2 блока, Slowness1285s); terra marker lifetime200;6 electro markers lifetime20/25/30/35/40/45. Guards same_id/same_team в выборках есть не во всех вторичных точках. Fangs/fireball/trident записывают native Owner культиста; terra копирует ID/team, electro не копирует их. В Java требуется единое настоящее авторство игрока во всех последствиях.

Luck: [spam](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/luck_arrow_spam.mcfunction) фильтрует same_id/team; стрелы damage0.5 (syringe1.5), burst4 damage2 (+ещё4 syringe). Omnipresence: [teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/omnipresence_teleport.mcfunction) исключает owner/team, но перебирает каждую цель; spreadplayers самого культиста в радиусе10 возле каждой, затем стреляет damage1 (syringe3). Не сводить к одному врагу молча.

## Academy, внешнее использование и перенос

Поиск всех `summonervexes` / `psychovex_wand` в исходном data не выявил Academy режимов, upgrade или trials. Отдельный `vex_wand` в Academy — другой Halloween-жезл; не смешивать. [flying_wand_tick_blind_upgraded:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_blind_upgraded.mcfunction) при cold%9=8 напрямую вызывает psychovex_spawn_pre возле ближайшего игрока. Этот вызов создаёт культиста без внешнего назначения команды/ID и trinket hook, а расход списывается с flying-wand executor; учитывать как отдельное подозрительное пересечение, не объявлять полноценным режимом Psychovex.

При переносе повторно использовать `LbcPsycho` для twitching и готовых psycho guard/effects, `LbcBlind` для нативных стрел/снарядов, `LbcBlaze` и `LbcWands.launchFire/adoptFire` для fireball, существующие Terra/Electro damage/helpers. Ввод/ману/крафт оставить LbcWands/LbcMana.trySpendWand/LbcCraftingRecipe. Призывы хранить семейными записями с UUID владельца и match epoch; ENTITY_LOAD/UNLOAD/after damage/AI goal callbacks вместо глобальных world-scans. Чистить штатным lifecycle и обеспечивать owner/team/observer/service-NPC guards при выборе и перед ударом. Не вводить произвольный TTL/cap. Дешёвые периодические задачи обходят только реестр своих активных культистов. Загруженные блоки изменять через общую бюджетную очередь и LbcIndirectKills.

Нужны ответы пользователя: расход1000/3000; замена vex всей команды/владельца; выбор любого другого игрока/врага; offhand Psychovex; многократный Omnipresence; Strangeness разрушение и многократный урон. Исследование не устанавливает новые требования вместо пользователя.

## Уточнения пользователя — 18 октября 2026

- Psychovex: сохранить стоимость1000 при взгляде вниз и3000 при призыве возле игрока, входной порог1000 (user).
- Psychovex: призывать возле ближайшего врага, без ограничения дистанции (user). Исходный выбор любого другого игрока заменяется этим целевым правилом.
- Summonervexes: заменять только vex самого владельца, а не всей команды (user).
- Strangeness: сохранить полное разрушение блоков и отдельную попытку урона1 на каждом шаге луча, включая многократное попадание в одну цель (user).
- Omnipresence: один teleport возле одного ближайшего врага (user).

Root подтвердил: исходный `data/minecraft/tags/damage_type/bypasses_cooldown.json` включает `minecraft:outside_border`. В целевом vanilla этот тег пуст; для сохранения именно Strangeness планируется отдельный scoped DamageType `skywars2:strangeness`, повторяющий нужные native outside_border tags и cooldown bypass. Это пояснение решения переноса, а не подтверждение завершённого runtime-поведения.

Уточнение (user): «Сохранить ванильные 8». Исходное поле трезубца `damage:0.2`
ванилью игнорируется; перенос сохраняет фактические 8 урона.

Lifecycle (user): «Исчезать после смерти владельца». Это относится к собственным
призванным vex и культистам; старое продолжение боя после смерти не переносится.

Исследование выше фиксирует исходник и целевые решения. Реализация и подтверждённые
проверки ведутся отдельно: [PSYCHOVEX_PORT.md](PSYCHOVEX_PORT.md),
[SERVER_TESTS.md](../SERVER_TESTS.md). Java в рамках самого исследования не изменялась.
