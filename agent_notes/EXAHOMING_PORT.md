# True Falling Stars и Exahoming: перенос

19.10.2026: build и три матча на одном SHA прошли: два адресных и одна регрессия общих механизмов. Сборка `e985d0ba…` сохранена как `build/verification/skywars2-exahoming-e985d0ba.jar` без установки; основной JAR остаётся `a0c4cb03…`. Итоги и ограничения — в [SERVER_TESTS.md](../SERVER_TESTS.md). [Исходник и решения пользователя](EXAHOMING_SOURCE.md).

`LbcHoming` обслуживает самостоятельный truefallingstars (отдельно от fallingstars), обычный/Academy Exahoming, испытания и Curse. Ввод и предметы — `LbcWands`, Academy — `LbcAcademy`, рецепт Air + True Falling Stars — общий `LbcCraftingRecipe` для 2×2/3×3. Реализовано 8/12 сочетаний, остаются четыре. Ресурс-пак и template не изменены.

Сохранены native fireball, мощности/позиции, Sisyphus100, исходные высоты и все 14/пять шаров испытаний с немедленной готовностью (user). Наведение раз в пять тиков использует общий кэш `lifetimes().loadedTargets()`. Фиксированного TTL нет; при отсутствии подходящих загруженных целей общий smart lifetime проверяет разрушаемый рельеф.

До использования проверяются заморозка, роль и антимагия. Уже выпущенный native-снаряд продолжает движение при поле у владельца; поле в позиции самого снаряда удаляет его. Общие проверки союзников, наблюдателей и служебных NPC сохраняются.

Лучи без фиксированной дальности по загруженным чанкам (user) расширяют существующий `LbcExasperato.Search`: `searchLoadedRay()` возвращает future; очередь имеет бюджет 64 сегмента по 16 блоков и 3мс. Nuke использует тот же helper. Второй движок лучей не создан. `LbcHoming.pendingRays` отменяет незавершённые запросы при остановке/очистке.

Глобальный Curse использует индекс entity loaded/unloaded callbacks: Projectile и minecraft:projectiles, включая TNT/falling block; перенаправляет также ElectroTrace. Последнее правило владельца (user): ничейный снаряд или снаряд проклятой цели получает заклинателя, третьему игроку сохраняется автор. Общий `LbcElectro.assignOwner` используется отражением и Curse; `LbcSummons.ownerChanged` обновляет последствия владения. Отдельного механизма атрибуции нет.

HUD показывает исходный значок E122 и оставшиеся секунды. Общая очистка custom-эффектов немедленно снимает Curse. Исходные name/lore/models и модификаторы Academy сверены структурным равенством; визуальный тест живым клиентом и массовая нагрузка пока не проводились.

Следующая ограниченная семья: Teleport + lightning_bolts → boltbolt_wand. Сначала аудит самостоятельного донора и результата, затем механика и общий рецепт.

## Destiny movement particles restored - 23 October 2026

User reported the missing visual around the slowly moving Destiny center.
Legacy `workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny_tick_vpered.mcfunction`
teleports the center by 0.3 blocks and emits `minecraft:soul`, count1,
zero spread/speed, `force`, at its new position. The native movement branch
omitted this emission. Restored it immediately after a successful move, using
`ServerLevel.sendParticles(SOUL,true,false,...)`; targeting, attack rays,
movement, lifetime, freeze and suppression behavior retain their existing rules.
Like legacy, this trail emits when the center moves, rather than while it stays
within 20 blocks of its target. No resource-pack changes are needed.

Build f5061b34 and existing Exahoming menu/crafting/runtime/stop/cleanup suites
passed across two isolated mode1 matches (10 recorded results,55.9s).
Evidence: `build/verification/destiny-trail-final/`. Actual client particle pixels
were not observed; this is source restoration plus existing runtime regression,
not a new rendered-particle fixture. Known fake-player Polymer packet-context
warning recurred; no check failed. Installed while main was stopped;
ca582dcc rollback retained and main was not started by the agent.
User Reroll/AcidAntDropChecks/HelicopterEncounterChecks changes preserved.
