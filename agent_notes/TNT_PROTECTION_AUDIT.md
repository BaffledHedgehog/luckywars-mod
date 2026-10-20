# TNT protection audit — 2026-10-20

Уточнение пользователя имеет приоритет над прежними комментариями/тестами: legacy fuse=0 — прямой native TNT-взрыв без конфиг-защиты; fuse=1/2 — прямой native TNT-взрыв с конфиг-защитой игроков 0/20/40/60/80/100%. st_air / Sarvatra Mula во всех режимах защищён. Fuse >=3 сохраняет настоящую сущность, физику и задержку. SNBT `2s` означает short 2 тика.

Просмотрены AGENTS.md (навигация, KFC/Cultus), ARCHITECTURE.md (общий TNT lifecycle, Exoperso), production Java и legacy `world/datapacksdisabled/LBC/data/lbc`; для Air также `data/lbcr`.

| Production место | Legacy / нынешний fuse | Требуемая семантика |
|---|---|---|
| LbcAir.detonate:186–193, все режимы | Сейчас 2; lbcr:raycast_air_explode — 3 TNT с 1s | Прямые защищённые native взрывы; сохранять число/позиции и Academy attribution. Пользователь явно защитил все режимы st_air |
| LbcShooters:369–373, SLR explosive | items/shooter/slr/{explode,dmg_with_id_explode}:2s; сейчас 2 | Прямые защищённые взрывы, сохранить два попадания/геометрию |
| LbcClouds:296–300, Fulminate | Сейчас 2; комментарий фиксирует source SNBT 2s | Прямой защищённый взрыв; сейчас отсутствует EXPLOSIVE tag |
| LbcKfcRewards:223, Cultus variant 3 | other/penises/nigger.mcfunction:3 — fuse:2 | Прямой защищённый взрыв третьего варианта; multicast — отдельный шестой вариант без TNT |
| LbcExasperato.burst:623–630 | chtick_explode:2s; сейчас 85 TNT по 2 | 85 отдельных прямых защищённых взрывов с прежними точками и 50 молниями; сохранить blastTrial |
| LbcExasperato.electroPayload:449 | chtick_electro_ray_end:50s/60s/61s/62s | Оставить 13 неподвижных noGravity TNT на тик с исходными задержками |
| LbcExasperato.instantBlast:573–578; chaos; nuclear queue | Прямой вызов level.explode с незаспавненным PrimedTnt fuse0; chaos legacy Creeper Fuse0 | Для source fuse0 убрать конфиг-защиту, native TNT damage/block physics сохранить. Не выводить политику из нынешнего служебного fuse0 для всех будущих caller |
| LbcExoperso → exopersoBurst → nuclear queue | Тот же instantBlast, 32 отдельных callback | Сохранить очередь CPU, серию и before/after callback, native owner и chain attribution; политика source0 без конфиг-защиты |
| LbcHoming:317, Destiny | exahoming_click_upgraded_mode_destiny_loop_end:fuse0 | Прямой native взрыв без конфиг-защиты |
| LbcItems:762/783, динамит/каст-тринкет | fuse1000; динамические landing fuse2 / stopped-motion fuse1 в :745/750 | Сохранять летящую сущность и fuse1000; при переходе 1/2 выполнить защищённый прямой взрыв в точке триггера |
| MatchEvents:563, TNT rain | command fuse100s | Оставить сущности/задержку 100 |

Production constructors `PrimedTnt` и `level.explode` исчерпываются таблицей (GameChecks исключён). Нативные fireballs/creepers/фейерверки — отдельные vanilla carriers, не заменять их на TNT по совпадению слова explosion. Legacy inairtrail2 содержит fuse0, phase_air_explode — fuse1; наличие в legacy само по себе не доказывает текущий production caller.

## Флаги и риски

`LbcExasperato.EXPLOSIVE` сейчас объединяет три ответственности: оптимизация native block rays, фильтр target через `wands().target` до damage/knockback и наследование цепному TNT (`HazardPropagationMixins` :34/:66/:93). Нужен независимый признак защищённого TNT: нельзя выключать кеш/owner/lifecycle вместе с конфиг-защитой source0. Нельзя считать EXPLOSIVE синонимом настроечных процентов. Сохранять epoch/Academy/Exoperso inheritance в цепочке отдельно от выбора защиты. Режим защиты должен жить на source, не зависеть от mutable fuse после запуска.

Существующие GameChecks закрепляют устаревшие delayed entities: Fulminate ~3917, Air ~16344/16423, SLR ~8273, Exasperato ~11526/11531. После изменения их утверждения про fuse2/число живых TNT должны замениться проверкой native explosion callbacks/количества и owner. Тесты delayed ray ~12402/12420 должны продолжить ожидать fuse50/60/61/62. Stop-cleanup тесты, вручную удерживающие fuse2 TNT заменой на fuse200/1000, требуют реального delayed payload fixture. Native damage tests должны проверить обе политики при всех шести процентах, блоки, owner, knockback и независимые Cultus удары; тесты, ожидающие защиту любого instantBlast из-за общего EXPLOSIVE, больше не являются источником требований.

## Применение аудита

Таблица выше фиксирует исходные точки до правки. Интеграция завершена на SHA
`eb8113b826619985bd5d988da2b070434640e500b3bfb1caf45d034bc8d7e98d`.
Air, SLR, Fulminate, Cultus и контакт динамического динамита используют общий
`LbcExasperato.protectedBlast`; временный PrimedTnt нужен только как native
DamageSource с owner, его нет в entity manager и клиентских spawn-пакетах.
Серия Exasperato 85 использует ту же бюджетную очередь, что и Nuke/Exoperso.
Позиции, число и мощность взрывов сохраняются, перегрузка очереди их не удаляет.
Исходный fuse=0 использует `instantBlast` и флаг `LbcDamage.UNPROTECTED_TNT`;
EXPLOSIVE отвечает за прежние target/cache/chain правила независимо от конфига.
Обычный задержанный и цепной TNT учитывает защиту по умолчанию.

Конфиг действует после сложности, до брони/эффектов. Ступень 5 не запускает
нулевой hurt; настоящий Resistance остаётся и работает штатно вместе с конфигом.
Все режимы Sarvatra Mula проверены на контакте с игроком при 0% и 100% защиты.
14 suites прошли за 36,83 с; проверены native HP, мобы, owner, GUI, отсутствие
world TNT, полное выполнение серии и очистка. Delayed Ray 50/60/61/62 сохранён.
Подробная граница доказательств и журнал — SERVER_TESTS.md; архив готов без установки.
