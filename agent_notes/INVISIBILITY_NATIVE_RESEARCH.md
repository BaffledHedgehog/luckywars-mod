# Полная невидимость игроков: аудит Minecraft 26.3 + Polymer

## Вывод

После исследования root реализовал и проверил [порт](FULL_INVISIBILITY_PORT.md):
same-ID Interaction, native schema/default values, event-driven обновления,
исключение для вселившегося наблюдателя по ответу пользователя и готовый
Polymer fallback для ПКМ. Фактические результаты — [проверки](INVISIBILITY_CHECKS.md).
Далее сохранено исходное обоснование выбора API; оно не заменяет runtime-отчёт.

Обычный vanilla `INVISIBILITY` не даёт полной невидимости игрока. Лучший исследованный путь без собственного packet engine — per-viewer Polymer-представление наблюдаемого игрока как vanilla `EntityTypes.INTERACTION`, используя тот же entity ID. У типа уже есть `NoopRenderer` и `isPickable() == true`; client target selection продолжает выбирать proxy, а пакет атаки адресован ID настоящего серверного игрока. Это перспективная реализация, но требует отдельной реализации/проверки синхронизации размеров и entity flags; одного `setInvisible(true)` или смены типа недостаточно.

Эта подмена касается только представления наблюдателя. Реальный серверный `ServerPlayer`, его эффекты, коллизии, команда и серверная проверка удара не меняются. Сам игрок/владелец не получает proxy для себя, так как его avatar является локальным игроком.

## Причины по vanilla исходникам

* `net/minecraft/world/entity/LivingEntity.java:885` синхронизирует invisible shared flag как `hasEffect(MobEffects.INVISIBILITY)` без проверки amplifier. Поэтому vanilla invisibility всех уровней одна и та же на стороне entity.
* `net/minecraft/world/entity/Entity.java:2825-2835`: `isInvisibleTo` возвращает false для зрителя из той же команды, если `canSeeFriendlyInvisibles()`. Следовательно даже обычный полностью невидимый игрок получает специальный rendering path от teammate: тело будет полупрозрачным.
* `net/minecraft/client/renderer/entity/LivingEntityRenderer.java:92-109,138-139,298`: обычный invisible body не рисуется; для teammate `isInvisibleToPlayer=false`, из-за чего `forceTransparent=true` и тело рисуется полупрозрачно. Даже без тела renderer продолжает проходить слои.
* `net/minecraft/client/renderer/entity/player/AvatarRenderer.java:48-71` добавляет armor и player-held-item layers; стандартный `shouldRenderLayers` скрывает слои только для spectator. `net/minecraft/client/renderer/entity/layers/ItemInHandLayer.java:26-61` действительно рисует предметы рук. Значит invisible effect сам по себе не скрывает armor/предметы.
* Огонь идёт отдельным путём: `EntityRenderer.extractRenderState` записывает `displayFireAnimation` из entity, а `EntityRenderDispatcher.submit` (стр. 145-165) отдельно вызывает fire renderer. `EntityRenderer.extractRenderState` (стр. 241-244) также записывает glow outline state. Эти визуальные эффекты не являются частью модели тела.
* В 26.3 hitbox debug переместился в `net/minecraft/client/renderer/debug/EntityHitboxDebugRenderer.java:40-45`: цикл вообще пропускает `entity.isInvisible()`. Остальные проверки только о видимости во frustum и камере. Это требует оставить на client proxy shared invisible flag выставленным.
* Picking: `net/minecraft/client/player/LocalPlayer.java:1266-1302` зовёт `ProjectileUtil.getEntityHitResult` с `EntitySelector.CAN_BE_PICKED`; `net/minecraft/world/entity/Interaction.java:88-90` объявляет `isPickable() == true`. `net/minecraft/client/Minecraft.java:1662-1666` для ENTITY вызывает `gameMode.attack(player, hitEntity)` без проверки, что entity является живым игроком. В обычном сетевом атакующем пакете передаётся entity ID. Поскольку серверный трекер всё ещё сопоставляет этот ID с реальным ServerPlayer, серверная melee обработка и урон сохраняют обычную цель; Polymer не заменяет server entity.

## Polymer и proxy Interaction

Локальные исходники Polymer 0.18.2+26.3:

* `eu/pb4/polymer/core/api/entity/PolymerEntityUtils.java:90-100` регистрирует конструктор PolymerEntity на исходный EntityType; callback может вернуть null или представление, контекстное для viewer.
* `eu/pb4/polymer/core/api/entity/PolymerEntity.java:31-44,53-58` содержит per-viewer `getPolymerEntityType`, замену списка видимого equipment и `modifyRawTrackedData`.
* `eu/pb4/polymer/core/mixin/entity/ClientboundSetEntityDataPacketMixin.java:38-64` вызывает изменение tracked data, после чего оставляет только accessor-ы, допустимые для выбранного proxy-типа. Значит width/height Interaction нужно добавлять как tracked entries типа Interaction и с корректными accessor ID/serializer; произвольные accessor-ы игрока будут отфильтрованы.
* `eu/pb4/polymer/core/api/entity/PolymerEntityUtils.java:217-225` имеет refresh одного игрока или всех watchers, повторно создавая pairing. Это пригодно при входе/выходе из invisibility и при необходимости обновить presentation; частые refresh на каждое движение не нужны, если tracker's обычная синхронизация положения достаточна.
* Vanilla `net/minecraft/client/renderer/entity/EntityRenderers.java:155` регистрирует `EntityTypes.INTERACTION` с `NoopRenderer`. `net/minecraft/world/entity/Interaction.java:40-55,88-90` задаёт синхронизируемые width/height и `isPickable=true`.
* `EntityRenderDispatcher.submit` всё равно отдельно рисует огонь по `renderState.displayFireAnimation`; следовательно NoopRenderer сам по себе огонь не убирает. В `modifyRawTrackedData` надо гарантированно сбрасывать для viewer общий entity flag `ON_FIRE`. Там же сбрасывать `GLOWING`, иначе не следует полагаться на renderer-specific невидимость outline. Оставить `INVISIBLE` для F3+B.
* Equipment для proxy нужно скрыть через `getPolymerVisibleEquipment(...)=emptyList()`. Это исключает armor и предметы в руках из обычных entity equipment packets. Также проверьте любые отдельные пакеты/поля, которые Polymeric/ванильная avatar-синхронизация передаёт независимо от equipment packet.

### Размеры и hitbox

`Interaction` начинает с width=1.0/height=1.0, а при update `DATA_WIDTH_ID` или `DATA_HEIGHT_ID` вызывает `refreshDimensions()` (`Interaction.java:38-55`). Серверный `ServerPlayer` bbox автоматически не станет размером клиентского proxy. Для сохранения обычного targeting следует переписывать два Interaction tracked data поля значениями ширины и высоты актуального bbox настоящего игрока. Учитывать crouch/swim/pose и scale. `Interaction` строит AABB вокруг entity position и использует noPhysics, поэтому замена сохраняет подбираемый ray hitbox, не создавая блокирующей клиентской физики.

Для не невидимого состояния callback должен возвращать `EntityTypes.PLAYER`, чтобы Polymer оставлял настоящее клиентское представление игрока. При смене порога invisibility нужно переиздать pairing через `PolymerEntityUtils.refreshEntity(target)`; обратный переход также важен, иначе client entity type останется proxy. Для нормального состояния callback обязан вернуть `EntityTypes.PLAYER`, а не `null`: `getPolymerEntityType` возвращает конкретный клиентский type, и `null` для этого интерфейса недопустим.

## Рекомендация и ограничения

Для требования «скрыть от всех остальных, сохранить vanilla crosshair melee и серверный урон» прототипировать Polymer per-viewer `Interaction` proxy: no-op renderer убирает avatar/armor/held layers без клиентского мода и без удаления entity ID; `Interaction.isPickable()` сохраняет ray targeting. Вместе с proxy отправлять правильный bbox, `INVISIBLE=on`, `ON_FIRE=off`, `GLOWING=off` и пустой equipment. Серверу оставить исходные эффекты и реальный hitbox игрока.

Это не буквально native vanilla effect feature: server mod меняет Polymer-представление типа для клиента. Vanilla effect amplifier `>=2` только задаёт условие включения; сам vanilla эффект не умеет скрыть слои или отключить glow/fire. Работа для обычных незатронутых vanilla клиентов ожидается, поскольку используется vanilla `interaction` type, но должна быть подтверждена интеграционным прогоном: Polymer filtering tracked data, корректные accessor IDs на конкретной версии, атака/урон по server player, respawn/dimension change/re-entry tracking, смена позы и удаление proxy при окончании эффекта. Для теста конкретно проверить F3+B: невидимый flag должен быть виден entity hitbox renderer'у как невидимость и поэтому box должен отсутствовать.

Если entity-type proxy создаёт несовместимые побочные эффекты в остальных client feature paths, полноценная альтернатива — клиентский renderer mixin; одна серверная vanilla команда/команда scoreboard не может подавить avatar layers/fire/glow у каждого клиента и одновременно сохранить обычную видимость melee target.

## Источники

Minecraft source archive: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.
Polymer sources: `C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/eu.pb4/{polymer-core,polymer-common}/0.18.2+26.3/*/*-sources.jar`.



