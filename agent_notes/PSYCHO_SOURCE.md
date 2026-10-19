# Psychowand и Psychoblind: исходное поведение

Исследование 18.10.2026. Это описание исходника, не заявление о завершённом переносе.
Корень всех относительных путей ниже:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Исходник и фактический ресурс-пак читались без изменений.

## Ввод, цена и Academy

Дополнительное уточнение для нового Twitching: «Случайное короткое удержание,
затем отпускание» (user, 18 октября 2026). Это целевое поведение переноса,
отдельное от описанного ниже исторического исходника.

`lbc/function/config.mcfunction:36,91`: Psychowand стоит 600, Psychoblind — 500;
цена сохраняется в Academy. В `workingitems/spell1_default.mcfunction`,
`spell1_upgraded.mcfunction` и соответствующих `*_left.mcfunction` перед кастом
проверяются мана и отсутствие `stopper_magic` в 20 блоках.
Функции `*_pre`/Psychoblind `click`, `click_upgraded_pre` списывают цену,
вызывают общий trinket cast и бонус полного mage-набора. Shift у Academy меняет
режим через `other/magic_academy/change_mode_4[ _left].mcfunction`: цикл 1→2→3→4→1,
без расхода; текущий старый вход всё равно проверяет порог маны перед переключением.

Глобальный NPC dispatcher: `other/magic_academy/interacted.mcfunction`:
новый предмет → `interacted_2.mcfunction` ставит upgradable=1;
готовый → `interacted_upgrade.mcfunction`. Испытание вызывает
`can_upgrade.mcfunction` → `can_upgrade_book.mcfunction`:
upgrade_available=1, upgradable=0 на MAINHAND (технический шалкер не переносить).
`interacted_upgrade` ставит upgraded=1, mode=1, добавляет описание и вызывает
профильный upgrade: `upgrade/psychowand.mcfunction` — tier B;
`upgrade/psychoblind_wand.mcfunction` — tier SS.
Тексты условий находятся в `can_upgrade/psychowand.mcfunction`,
`can_upgrade/psychoblind_wand.mcfunction`.

Испытание Psychowand: `workingitems/psychowand.mcfunction`, только при случайном
результате 1–3, выбранном mainhand upgradable=1 и существе #minecraft:mobs
на расстоянии 0.01…30 с lookback>=1, scared>=1 и blindness.
У проверки готовности есть исключение команды/наблюдателей, но нет nomagic-фильтра.

Испытание Psychoblind: `awaken_tuntija_wands/psychoblind/click2.mcfunction` →
`click2_upgrade.mcfunction`: при Shift и mainhand upgradable=1 обычный залп всё равно
создаётся; себе scared+=150, lookback+=40, piss+=150, twitching+=150, скример,
have_lucky_effects, готовность улучшения немедленно.

## Psychowand

Все файлы ниже под `lbc/function/workingitems/`:
`psychowand_pre.mcfunction`, `psychowand.mcfunction`,
`psychowand_upgraded_pre[2|_left].mcfunction`, `psychowand_upgraded.mcfunction`,
`psychowand_upgraded_lib.mcfunction` (книжные spell 137…140).

Обычный один раз выбирает 1…6 для всего каста:

| Roll / Academy mode | Цели и действие |
|---|---|
| 1–3 / 1 | Игроки: один случайный фантомный звук из 16 |
| 4 / 2 | Все сущности: скример |
| 5 / 3 | #minecraft:mobs: lookback += 9 |
| 6 / 4 | #minecraft:mobs: scared += 100 |

Радиус 0.01…30, исключены nomagic_active, spectator и same_team.
Фантомные звуки — `psychowand/fantomsounds.mcfunction`: падение/шаг/ломание камня,
крипер, трава, позиции вокруг головы. `lookback.mcfunction` добавляет cave звук;
`scare.mcfunction` — четыре ghast scream.
Academy выбирает те же четыре режима детерминированно; частицы soul исходные.

После эффекта обычный и Academy вызывают `psychowand_check_hp.mcfunction` для всех
подходящих #mobs: `data get Health 1` округляет вниз; значение <=1 (то есть фактически
Health<2) вызывает `psychowand_check_hp_2.mcfunction`: враждебный vex 5000 HP,
max_health5000, follow_range200, resistance100, speed1, damage1, armor/toughness40,
life_ticks2000000; advancement staffs/psycho заклинателю; kill цели.
В библиотечном `psychowand_upgraded_lib` этой проверки здоровья нет.

## Общие психические состояния

`lbc/function/tick5t.mcfunction` → `mobs_5t.mcfunction:6–7`:
каждые ПЯТЬ тиков для #mobs вне spectator:
`psychowand/lookbackloop.mcfunction` поворачивает yaw +20°, lookback -=1;
`scaredloop.mcfunction` выполняет 16 независимых 15% проверок, scared -=5.
Восемь проверок смещают X/Z на ±0.5 (кардинальные и диагональные),
восемь меняют yaw/pitch на ±5° (четыре одиночных и четыре сочетания).
Итого lookback9 ≈45 тиков/180°, scared100 ≈100 тиков.
Повторное наложение складывается.

HUD `lbc/function/ui/get_data.mcfunction`: порядок lookback U+E113 red,
piss U+E116 red, scared U+E119 red, screamray U+E120 green,
twitching U+E123 red. Выводятся сырые значения счётчиков без пересчёта времени;
lookback — единицы по 5 тиков, twitching — по 2 тика, остальные — тики.
Префикс текста — две позиции пробела перед glyph и одна после; у twitching
только один пробел перед glyph.

`players_2t.mcfunction:16` вызывает `other/twitching.mcfunction` каждые ДВА тика
только для игроков, если twitching>=1 и нет marker stopper в 20 блоках.
1% — `twitching_sound`, 10% — белое стекло, 40% — slime sound,
50% — `twitching_random.mcfunction`; затем twitching -=1.
Рандом 1…6: принудительный RC_Raycast=1 / бросить зелье / выстрелить луком
при стрелах / заряженный арбалет / снежок / жемчуг. Ветви используют
`other/twitching_{bow,crossbow,random_potion,snowball_sel,pearl_sel}*.mcfunction`.
Это реальное использование/расход предметов, не только частицы.
Звуковая ветвь также заполняет стеклянные бутылки через twitching_bottle_fill.

`effects/tick.mcfunction:31` при have_lucky_effects, piss>=1 и !sin_greed_active
каждый тик вызывает `workingitems/yellow_lightning_asplayer.mcfunction`:
self particle lbc:piss200; жёлтое стекло3 окружающим; nonplayer follow_range=2;
blindness/slowness по 1 секунде, piss -=1, эффект и advancement effect_piss.
Обычное попадание Psychoblind НЕ ставит have_lucky_effects для twitching;
twitching при этом работает независимо через players_2t.
Снятие эффектов/смерть должны очищать новые состояния через общий lifecycle.

## Psychoblind

Все режимы в `lbc/function/workingitems/awaken_tuntija_wands/psychoblind/`.
Обычный `click2.mcfunction`: три marker заряда, lifetime100,
psycho_charge/rare/slowraycast/magic, owner lbcID2 и team_number,
разброс `lbc/function/universal_razbros.mcfunction`.
Точный разброс: yaw ±30° с шагом .01°, pitch ±20° с шагом .01°;
`random_rot.mcfunction` — yaw ±180°, pitch ±90°, шаг .001°.
Academy `click_upgraded_2.mcfunction`:

| Mode | Поведение |
|---|---|
| 1 default | 4 обычных заряда, lifetime100 |
| 2 mindblowing_ray | screamray +=40; игрок отслеживает взгляд каждый тик |
| 3 spray | 1 carrier lifetime100; каждый тик выпускает случайный обычный заряд lifetime20 |
| 4 invisible_charge | 3 заряда lifetime100; та же механика, без ink-частиц |

Диспетчер: `tick_not_timestopped.mcfunction` → `raycastselector.mcfunction` →
`raycastselector_rare.mcfunction:21–23`. Lifetime увеличивается раз в тик,
удаление проверяется ПОСЛЕ обработчика.
`tick[ _invis].mcfunction`: 9 точек с шагом .25, движение на 2 блока за тик;
проверок стен нет (прохождение стен подтверждено lore).
Homing: если вообще нет #mobs <=9.99 и есть #mobs 10…20,
`lbcr/function/daraycastwithid_psycho.mcfunction` разворачивает заряд к цели;
фактический selector не имеет limit/sort и для facing не исключает strict_map_object.
`tickend.mcfunction` переносит marker к финальной точке, Distance=0.

Попадание: `lbcr/function/damagewithid_psycho.mcfunction`, `*_2.mcfunction`,
`*_at.mcfunction`, `*_killl.mcfunction`. Intersection через dx=dy=dz=0
и distance<=15; до ДВУХ #mobs за обработку, исключаются damager/nodamage/spectator.
Owner и команда восстанавливаются по player scores; союзные мобы не защищены
этим player-only nodamage. nomagic/strict_map_object в hit отсутствуют.
4 arrow damage by caster; darkness20 секунд; scared+=15, twitching+=10,
lookback+=5; cave звуки 2/0 и 2/2; campfire_signal_smoke200 только жертве.
Обычный заряд умирает после попадания, spray carrier не умирает от него.

Spray: `click_upgraded_mode_spray_tick.mcfunction`, `tickend_spray.mcfunction`:
5 точек по .25, движение 1 блок/тик; выпущенный charge получает
`random_rot.mcfunction`, а не направление carrier.

Mindray: `players_tick.mcfunction:49` → `click_upgraded_mode_mindblowing_ray_tick`:
screamray -=1, луч от глаз вдоль текущего взгляда; `*_loop.mcfunction`
301 точка с шагом .33 (~99 блоков), сфера3.5 на КАЖДОЙ точке, до первого non-airs.
`*_ponosed.mcfunction`: piss/scared/lookback +=1, have_lucky_effects, полный скример,
0.01 generic damage без owner. Исключены только caster и spectator.
Одна цель может получить много вызовов за тик; на первой точке стены эффект
выполняется до проверки стены. screamray складывается при повторном касте.
Скример `lbc/function/drops/skrimmer.mcfunction`: 16 звуков, cosy_smoke5000,
blindness6 секунд amplifier10. Поэтому повторные точки особенно дороги.

## Крафт и оформление

`lbc/function/other/craft_ground.mcfunction:23`: dropped stblind +
psychowand в 0.001…1 → `sauvojen_tuntija/crafted_psychoblind.mcfunction`.
`other/craft/inventory_craft_check.mcfunction:23` проверяет наличие двух признаков
в player.crafting.* → `crafted_psychoblind_inv.mcfunction`.
Результат `lbc/loot_table/tuntijaa_blind_psycho.json`: carrot_on_a_stick,
модель lbc:psychoblind, tooltip lbc:tier_s, custom_data
transformable/staff/magicwand/psychoblind_wand=1, прежние translate/lore.
Ground удаляет весь stack обеих сущностей; inventory уменьшает каждый совпавший
слот (не ровно по одному донору). Это техническая странность исходного крафта.

Psychowand находится в `lbc/loot_table/rofls.json`, модель lbc:psychowand.
Фактический пак `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json`
подтверждает прохождение стен и описание вызова призрака при 1 HP;
условия Academy: одновременные психо-эффекты / Shift-медитация.

## Открытые вопросы и интеграция

До ответа пользователя не объявлять намеренными и не исправлять молча:
порог Health<2 против lore1 HP; враждебный vex5000 HP; отсутствие фильтров
союзников/антимагии в mindray; многократное наложение на каждую точку;
player-only защита союзников/homing; масштаб принудительного использования
инвентаря при twitching; old lifetime100/20 против общего smart lifetime.
Ground/inventory craft anomalous consumption следует заменить только по явному
решению/общему уже принятому контракту нормального нативного крафта.

Переиспользовать `LbcWands.target`/mana/hit helpers, `LbcMana.trySpendWand`,
`LbcAcademy` (режимы/испытания), `LbcCraftingRecipe` (donor recipe),
`ProjectileLifetimes`, `LbcIndirectKills` (прямой и отложенный owner),
общий `LbcItems.clearEffects` и lifecycle. Не подключать старый tick/advancements
как параллельный движок. Нового второго mana/target/lifetime менеджера не нужно.

## Нативная имитация мыши: API 26.3

Новый запрос пользователя: Twitch имитирует обычные ЛКМ/ПКМ по текущему взгляду,
включая сущности и блоки. Таблица предметов исходного Twitch выше историческая.
Проверен локальный source jar `.gradle/loom-cache/minecraftMaven/net/minecraft/`
`minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.

В 26.3 отдельный `handleAttack(ServerboundAttackPacket(int entityId))`;
`ServerboundInteractPacket(int entityId, InteractionHand hand, Vec3 location,
boolean usingSecondaryAction)`, location относительно позиции цели.
Старых createAttackPacket/interactAt factories нет. Вместо handleAnimate —
`handlePunch(ServerboundPunchPacket)`: mainhand swing и resetAttackStrengthTicker.
UseItem/UseItemOn packet handlers вызывают ackBlockChangesUpTo(sequence):
отрицательная запрещена, положительная меняет ACK реального клиента.
Для серверной имитации использовать native вызовы без fake inbound sequences:

- `player.gameMode.useItem(player, level, stack, hand)`;
- `player.gameMode.useItemOn(player, level, stack, hand, BlockHitResult)`;
- `player.interactOn(entity, hand, relativeHit)`;
- `player.attack(entity)`;
- `player.gameMode.handleBlockBreakAction(pos, action, face, level.getMaxY(), 0)`.

GameMode block-break sequence используется только debugLogging; его destroyAndAck
не отправляет network ACK. Одиночный mining click — START затем ABORT, а не
destroyBlock в обход времени добычи. PiercingWeapon использует собственный STAB.

Порядок `Minecraft.startUseItem`: MAIN затем OFF; для каждой руки entity interact
(Success завершает), либо block useItemOn (Success/Fail завершают), затем nonempty
item use (Success завершает). Две руки — fallback, не два обязательных действия.
Использование лука/еды сохраняет native start/releaseUsingItem и расход предмета.

Fabric `fabric-events-interaction-v0-0.6.10+b1792f7da0-sources.jar` подтверждает:
GameMode.useItem HEAD уже вызывает UseItemCallback; useItemOn HEAD — UseBlockCallback;
handleBlockBreakAction START HEAD — AttackBlockCallback; Player.attack HEAD —
AttackEntityCallback. Вручную повторно их НЕ вызывать.
UseEntityCallback находится только в packet handleInteract, поэтому перед прямым
interactOn вызвать его один раз с абсолютным EntityHitResult; native только при PASS.
Исходники Fabric — `net/fabricmc/fabric/mixin/event/interaction/`
ServerPlayerGameModeMixin.java, PlayerMixin.java, ServerGamePacketListenerImplMixin.java.

Общий helper проверяет server thread, живого участника/арену/стадию, observer,
frozen/time-stop; вычисляет aim с occlusion блоками, loaded-only и native reach.
Сохранять border/Y/spawn protection/mayInteract/features/cooldowns/hands-busy:
часть проверок находится только в packet handler, прямой GameMode их не повторяет.
Не изменять настоящий Shift игрока поддельным inbound packet.

## Решения пользователя — 18 октября 2026 (user)

Сохранять фактический исходный порог исполнения Psychowand: Health < 2 HP
(округление исходного data get). Призрак остаётся враждебным vex; его HP и
MAX_HEALTH теперь 2 000 000 000 вместо исходных 5000.

Mindray накладывает воздействие один раз на подходящую цель за тик;
многократность по соседним точкам исходной трассы не сохраняется.
Для него действуют обычные защиты союзников, наблюдателей, служебных сущностей
и антимагии через общие helpers.

Самонаведение Psychoblind сохраняет исходное кольцо 10…20 блоков.

Twitch: после первоначального согласования шести native ветвей пользователь
уточнил охват до ВСЕХ действий мыши ЛКМ/ПКМ. Имитировать семантику обычного
клика по текущему прицелу: атака/взаимодействие с сущностью, блоком, предметом,
а не отдельный список расходников.
Владелец общего helper — `game.items().mouseAction`: server thread,
native GameMode hooks и Player.attack; UseEntityCallback вызывается отдельно,
поскольку он расположен в packet handler. PiercingWeapon использует STAB.
Поддельные inbound sequence не создавать.

Эти решения являются целевыми требованиями. Данный документ не утверждает,
что они проверены на работающем сервере; историческое описание выше сохранено.

