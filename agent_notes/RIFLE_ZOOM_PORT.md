# Зум SLR и Magnum — 22 октября 2026

## Rifle scope / SLR ammo / minigun laser - QUALIFIED

[Contract](SCOPE_VISUAL_FIX.md). SLR/Magnum use client-only native FOV projection while retaining
lbc:zoom scope overlay and a6-tick lbc:zoom_out exit animation. The server's
abilities, movement attributes, position and velocity remain authoritative and
unchanged. Native ability resends/attribute updates and lifecycle cleanup are
covered. Client FOV Effects must be enabled; zero disables this native zoom.
Actual RP remains untouched. Apply the two supplied scope JSON template values
(ZoomFactor1, Duration0.3) to avoid multiplying the old shader crop with FOV.
SLR shows the native arrow count in the existing HUD row, including both hands,
observer camera/cache and ignorance behavior. Minigun ManualAttachment now
explicitly enrolls viewers; actual spawn/model/scale/removal packets, forward
native model geometry, lifetime and frozen late-viewer behavior are checked.

Offline build/two actual isolated mode1 matches passed on ca582dcc:
166 recorded suite results, 132.78s;
231 sources / 644 classes /
2704 resources exactly match tested artifacts.
Evidence: build/verification/scope-visual-final/. Installed to main mods/skywars2.jar while stopped; rollback 3cb2c7d1 retained. The agent did not start the main server.
User Reroll and concurrent user formatting of AcidAntDropChecks and
HelicopterEncounterChecks were preserved and excluded from the commit.
No real client GPU/mouse behavior or peak-capacity benchmark is claimed.
<!-- scope-visual-status-end -->

Пользователь сначала запросил удержание ЛКМ, затем выбрал переключение (user):

> без клиентского мода, пусть тогда переключение

Нативный клиент 26.3 не передаёт отпускание/удержание ЛКМ в воздухе.
`Minecraft.startAttack` посылает `ServerboundPunchPacket`, в том числе после
попадания в сущность. `continueAttack` повторяет этот пакет при копании блока.
Поэтому общий `handlePunch` после штатной обработки переключает зум только
для SLR/Magnum. Повторяющийся каждый тик поток копания и дубликаты в одном
тике не переключают эффект заново; между отдельными нажатиями нужен промежуток
минимум два серверных тика. Это переключение по клику, не эмуляция удержания.
Обычные атаки/копание и ПКМ-стрельба не менялись.

`LbcRifleZoom` добавляет/удаляет именно `lbc:zoom` через `ServerPlayer.addPostEffect`
и `removePostEffect` — тот же нативный API, что у `/posteffect add/remove`.
Не исполняет строковую команду, не меняет операторские права и не сбрасывает
чужие эффекты через `clear`. Модель, патроны, кулдауны и урон не затрагиваются.
Ресурспак уже содержит `assets/lbc/post_effect/zoom.json` с ZoomFactor=4;
ресурспак и template не менялись.

Поддерживаются обе руки, приоритет у основной. Состояние привязано к конкретному
стеку и измерению: замена/убирание оружия, смена измерения, смерть или блокировка
действий снимают эффект на следующем игровом тике. Выход игрока и конец матча
очищают его через `forget`/`stop`. Обходятся только состояния игроков, которые
пользовались зумом; сканирования сущностей/мира или команд каждый тик нет.
Проверка идёт перед ранним выходом тика предметов при Stand. Действуют обычные
правила допуска предметов и запрета действий, без отдельных новых исключений
для Creative/developer и без привязки к антимагии/проверке маны.

Проверки: `rifle-zoom`, `rifle-zoom-stop-probe`, `rifle-zoom-cleanup`.
Проверяются настоящий вход `handlePunch`, отдельные клики/повторный поток,
обе пушки и руки, прототип `/give`, замена равного стека, чужой posteffect,
настоящая остановка времени, смерть, `forget` и реальный stop матча.
Детерминированный счётчик для потока повторов проверяет состояние; реальная
мышь/клиентские пакеты и GPU-изображение зума этим не проверяются.

Build и два изолированных матча прошли на `5e46c6b4…`: 10 результатов suites
за 44,59 с, включая прежние Magnum/SLR и настоящую очистку зума после stop.
В старом SLR fixture исправлено устаревшее ожидание защиты союзной коровы:
по предыдущему уточнению пользователя дружественные мобы доступны снарядам.
Боевая реализация не менялась; перед следующей NPC-проверкой HP сбрасывается.
172 Java-исходника, 516 классов и 2668 ресурсов совпадают со сборкой.
Готовый JAR: `build/verification/skywars2-rifle-zoom-5e46c6b4.jar`, без установки.
Доказательства: `build/verification/rifle-zoom-final/qualification.json` и лог.
Основной JAR96ee50a7, пользовательские правки Reroll, ресурспак/template сохранены.
Повторяемый прогон: `python tools/smoke_rifle_zoom.py --server-dir build/verification/key-reuse-server`.
Runner использует изолированный порт25576, исключающие lock, резервные копии
JAR/config/profiles/scoreboard и восстановление только после остановки сервера.
Смена измерения проверена чтением реализации, отдельного native fixture нет.
