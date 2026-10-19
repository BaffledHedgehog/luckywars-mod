# Vanilla hooks для навыков 2, 4 и 13 (MC 26.3)

## Текущее состояние в коде

- **Навык 2:** `PlayerFallSkillMixin` инжектится в `Player.causeFallDamage` на `RETURN` и передаёт дистанцию падения в `GameController.landed` / `MatchEvents.onLanding`. `Player` в MC 26.3 действительно переопределяет этот метод; он возвращает до `super` при `mayfly`, поэтому текущий guard также исключает полёт.
- **Навык 4:** `ConsumableSkillMixin` инжектится после серверного `ConsumeItemTrigger.trigger` внутри `Consumable.onConsume`, затем `MatchEvents.onConsumed` фильтрует предмет по `swrg:food`. Это тот же trigger и порядок, что advancement `consume_item`: до food listeners и до снятия единицы стака. `javap -p -c` по бинарному Loom JAR подтверждает вызов `net/minecraft/advancements/triggers/ConsumeItemTrigger.trigger(ServerPlayer, ItemStack)` и его порядок. Ранее предполагавшийся пакет `net/minecraft/advancements/critereon` для mixin target неверен относительно проверенного runtime bytecode: цель — `net/minecraft/advancements/triggers/ConsumeItemTrigger`. Из-за возможного устаревания/расхождения source snapshots target сверять по фактическому бинарнику; в коде mixin уже использует `triggers`.
- **Навык 13:** текущая реализация — private `SkillVex extends Vex` внутри `MatchEvents`, с `SkillState` как owner-ссылкой. `canAttack` проверяет актуальность матча/состояния/навыка, живого владельца, владельца и союзную команду цели, а для игроков — `controller.allowDamage`. `doHurtTarget` повторяет проверку перед ударом. `shouldBeSaved() == false` предотвращает сохранение и последующую загрузку как обычного ownerless Vex. Создаётся существующая `EntityTypes.VEX`, отдельный тип/сетевой ID не регистрируется. Vex удаляются при удалении skill state (смерть/выход владельца), остановке матча и завершении матча.

Текущая логика Vex намеренно **не задаёт TTL и лимит числа**: в реализации они оставлены без них ради сохранения баланса. Раннее предложение использовать Data Attachment UUID владельца и собственный жёсткий TTL/лимит было вариантом до выбора реализации; оно не описывает текущий код и не выполнено.

## API и caveats

По локальным Mojang-mapped sources Minecraft 26.3:

- Для падения `Player.causeFallDamage(double,float,DamageSource)Z`, descriptor `(DFLnet/minecraft/world/damagesource/DamageSource;)Z`, подтверждён как override в `Player.java` (1415+). Его `RETURN` означает обработку landing/fall path, не обязательно прошедший урон. Для фактически полученного fall damage API-вариант — Fabric `ServerLivingEntityEvents.AFTER_DAMAGE`, фильтр `source.is(DamageTypes.FALL)` и `damageTaken > 0`; для любого контакта с землёй — `LivingEntity.checkFallDamage(double,boolean,BlockState,BlockPos)V`, descriptor `(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V`.
- Общий food hook — `FoodProperties.onConsume(Level,LivingEntity,ItemStack,Consumable)V`. `Consumable.onConsume` вызывает `ConsumeItemTrigger.trigger` перед `ConsumableListener` и `stack.consume(1,user)`. Текущий мод перехватывает сам trigger, а не `FoodProperties`.
- У vanilla Vex `setOwner(Mob)` не принимает Player, `VexCopyOwnerTargetGoal` копирует target этого vanilla owner, а `setLimitedLife(ticks)` наносит периодический starve damage вместо жёсткого despawn. Текущий `SkillVex` не использует vanilla owner/lifetime.

**Источники:** [.minecraft sources jar](../.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar), бинарный `minecraft-merged-d6f9bb6878-26.3.jar` для проверки `javap`; точные entries — `net/minecraft/world/entity/player/Player.java`, `net/minecraft/world/entity/monster/Vex.java`, `net/minecraft/world/item/component/Consumable.java`, `net/minecraft/world/food/FoodProperties.java`. Fabric aggregate sources jar пуст по Java-классам; сигнатура `AFTER_DAMAGE` была сверена с embedded `fabric-entity-events-v1-6.0.4+3434d6d95d.jar`.

