# Замечания координатора к world/tech

Раздел253 источника принят после независимого прохода и дополнительного
чтения координатором. Ниже сохранены первоначальные замечания и их решения.

- Orb Gun: experience query points читает очки прогресса текущего уровня,
  не весь накопленный опыт. После попытки списания N+1 команда store result
  storage tmp int0.5 пишет floor(N/2) в storage, **не меняя score tmp=N**.
  Условие стрельбы проверяет score≥1: N=1 запускает луч с нулевым damage;
  N=0 уходит в shoot_cant. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/shoot.mcfunction).
- Void Magnum: fill с относительными углами±0.4 преобразует координаты в
  целые позиции блоков. Это не куб со стороной менее1; включает1–2 блока
  по каждой оси в зависимости от позиции. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/hit_block.mcfunction).
- Для Nexus wires, конвертеров/craft, дерева и compass пока преобладают
  автоматически собранные карточки команд. Нужны краткие человеческие
  описания результата, правил расхода, области и завершения.
- 28 формулировок «caller не найден» требуется сверить также с JSON
  advancements/rewards. Отсутствие ссылки среди functions не означает
  отсутствие игрового входа.

Первоначальные замечания исправлены исследователем: Orb Gun, геометрия Void
Magnum, человеческий обзор Nexus и шесть advancement-входов. Остальные22
no-caller claims сверены с JSON и ограничены буквальными ссылками.

При дополнительном чтении координатор исправил ещё несколько положений:

- Water Wisp даёт процент базового HP через add_multiplied_base, не доли HP.
- Shadow cooldown блокирует зарядку, но не поглощение накопленного заряда;
  selector limit1 без sort не гарантирует ближайший projectile. Заряд
  пополняется до100n с возможным небольшим перескоком, затем ограничен500.
- Центр орбиты расположен на1 блок выше глаз. Ограничение12wisps учитывает
  только радиус30; общий new-tag без distance/sort допускает выбор чужого
  display при незавершённом параллельном spawn.
- Четыре группы Light очистки перечислены по фактическим effects/scores.
- Все converter-ветки извлекают предмет из display одинаково, оставляя TNT
  при отсутствии источника. Результаты red/green/black wire вне Nexus —
  отдельные пути; они больше не приписываются converter-цветам.

Дополнительная проверка Nuke/Railroad, Cyborg и технических лучей завершена.
Координатор прочитал все13 Nuke и8 Railroad helpers,6 Cyborg,6 Katana,
18 Laser Minigun,9 Test Ray,30 Magnum shot helpers и связанные входы.
Технические сводки исправлены по фактическому потоку команд:

- Nexus RC сбрасывается в конце; ветки по обеим рукам последовательные.
- Roadgun end остаётся исполнением display, а не owner-player; ride dismount
  не снимает игрока напрямую. Pad ставится у display, удаление происходит на40.
- Corrupter оставляет caster-tag и не ставит cooldown при промахе; sim_cube
  распространяет black concrete по6 направлениям, наносит10 без friendly-filter
  и с0.25% удаляет все sim_cube markers.
- Supply Drop имеет advancement-вход обмена2 arrow на magnum_ammo, до32
  проходов; infinite_arrow отменяет расход, но не выдачу. Это отдельная
  механика от получения оружия со стоек, которое источники не подтверждают.
- Katana заряжается от items/swords damage-events, не каждый tick удержания;
  charge сбрасывает speed amount оружия в0.05. Attack-damage modifier ставится
  игроку; offhand-dash всё равно перезаписывает mainhand-item.
- Cyborg вызывается player-interacted advancement и выбирает все подходящие
  leather/techno stands без distance, не только исходную стойку.
- Magnum offhand-вход использует mainhand mode. Standard/incendiary/void
  проходят через мобов, void — также через блоки; premature stopper может
  оставить hit. Каждый cooldown-handler уменьшает score всех игроков;
  повторные выстрелы вновь ставят полный cooldown.
- Laser Minigun требует поддержания окна повторными кликами, не одного
  удержания. Spread измерен в0.001°, визуал сужается и не следует каждый тик;
  hit-helper повторно выбирает сущности без mobs-filter.
- Test Ray — ручной визуальный путь без damage, оставляющий check_id при hit.
- Для Railroad уточнены питание каждые7 шагов, высоты и остановка таймера
  без игрока в75 блоках; конец ставит2 oak-wood блока.

[Полный шаблонный аудит](inventory/worldtech_patterns_audit.json) сверяет40
обычных и39 bruh-команд Nuke,24 creeper offsets в каждом,6 полных Magnum-loop
и gameplay-команды cast. Оба white-concrete NBT прочитаны:7×7×7/343 блока
и5×1×5/25 блоков. Расхождений0. Весь ordered manifest253 — в
[независимом отчёте](review_world_tech.md).

Изменения внесены только в описание. Приёмка локальных контрактов не заявляет
успешное исполнение внешних command bridges, loot и raycast реализаций;
они сохраняют отдельные связи и собственные разделы проверки.
