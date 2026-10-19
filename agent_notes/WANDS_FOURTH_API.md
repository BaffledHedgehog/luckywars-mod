# Tiny Electro и Tiny Acid: переиспользование API

Это read-only разбор API нового мода для переноса двух жезлов. Для Tiny Electro пока известны source-теги `raycastmolnia`, `slowraycast`, `mecha`, `player` и срок 40 тиков; детали Tiny Acid сверяются отдельно. Код и тесты не менялись.

Текущий статус Tiny Electro после этого исторического API-разбора: [WANDS_ELECTRO_PORT.md](WANDS_ELECTRO_PORT.md). Он описывает state-trace реализацию в `LbcWands`; этот файл остаётся снимком прежнего этапа и не является актуальной инструкцией по реализации или runtime PASS.

## Tiny Acid

Не вводить marker с ручным перемещением и самописным raycast. Пользовательское уточнение требует настоящую физику velocity, gravity, air resistance и swept collision. В новом моде уже есть готовый образец: `LbcClouds.AcidProjectile` (`ThrowableProjectile`, `PolymerEntity`) наследует движение/столкновения Minecraft, а `LbcClouds.spawnAcid(AreaEffectCloud)` задаёт owner, направление, скорость, source-теги и добавляет entity в мир. Жизнь/возраст хранит attachment `skywars2:acid_age`; `loaded`/`unloaded` поддерживают `acids`, `stop()` удаляет отслеживаемые снаряды.

Для жезла полезно переиспользовать этот тип, Polymer proxy и обработку попадания, но `spawnAcid` сейчас привязан к AreaEffectCloud и package-private. Нужен узкий overload/helper для игрока, который передаёт owner, origin/aim и матчевый epoch существующему кислотному запуску. Не дублировать `AcidProjectile`, map tracking, lifecycle callbacks, возраст или collision pipeline. `acidImpact` приватный — оставлять применение эффектов внутри `LbcClouds`, не открывать его только ради второго контроллера.

## Tiny Electro

`LbcWands` уже содержит синхронный луч для обычных жезлов: один bounded query кандидатов (`rayBounds`), затем точки с шагом 0.35 и общие проверки цели (`target`/`rayTarget`). Его нельзя считать готовой заменой источнику `slowraycast`: старый исполнитель обновлял slowraycast-сущность каждый тик, увеличивал lifetime и маршрутизировал по тегам. Для механики снаряда лучше оставить нативный entity/projectile тик и collision, а переиспользовать проверки участника/союзника/наблюдателя и bounded выбор кандидатов из `LbcWands`; не создавать второй универсальный scheduler.

В `LbcTrinkets` есть образец native projectile ownership/spawn, lifecycle и cleanup для `Projectile`, но его tracker предназначен для tagged projectile и `loaded` отсекает неактивного/observer owner. Для Electro с 40-тиковым сроком можно использовать эту схему, если сущность действительно наследует `Projectile`; если это Marker, потребуется существующее lifecycle/tracking место, а не `LbcTrinkets.spawn(Projectile)`.

Поиск нового `LbcMagic`, `LbcFrost`, `LbcOverload` и `LbcWands` не выявил отдельного lightning-bolt engine. У `LbcMagic` есть Polymer Lightning Rod для маяка, у `LbcWands` только звук молнии для Teleport. Их не следует переиспользовать как искусственный удар молнии. Метка `mecha` — source-фильтр/поведение цели; сама по себе она не требует отдельной модели движения.

## Точки интеграции и проверки

- Запускать обе способности через существующий `LbcWands.use`/проверку маны, подавления и активного игрока; не добавлять исключение для Creative. Цели и попадания сохраняют правила `game.isObserver`, `game.canUseGameplayItems` и союзников из общего wand target gate.
- Для Acid проверить native velocity/gravity/drag, Polymer roundtrip, owner, impact/status и окончание жизни; очистка/выгрузка должны использовать текущий `LbcClouds` tracking.
- Для Electro проверить реальный native tick до срока 40, точное terminal/collision поведение и удаление по сроку, а также owner/ally/observer gates. Не выводить эти детали из тегов, пока source-функции и источник Tiny Electro не сверены.

## Источники

- Новый мод: `src/main/java/me/skywars2/game/LbcClouds.java` (`AcidProjectile`, `spawnAcid`, `loaded`, `unloaded`, `tickAcid`, `acidImpact`, `stop`).
- Новый мод: `src/main/java/me/skywars2/game/LbcWands.java` (`rayBounds`, `ray`, `rayTarget`, `target`).
- Новый мод: `src/main/java/me/skywars2/game/LbcTrinkets.java` (`spawn`, `loaded`, `unloaded`, `stop`).
- Новый мод: `src/main/java/me/skywars2/game/LbcMagic.java`, `LbcFrost.java`, `LbcOverload.java` — сверены на предмет имеющегося lightning engine и повторного механизма.
- Старый native service: `E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`, `runNativeRaycastSelector` и `tickMarkerLifetime`: `slowraycast` обрабатывается как обновляемая по тикам сущность с lifetime и маршрутизацией поведения.
