# Снаряды и дружественные мобы

Target requirement (user, 21 October 2026): all projectile impacts may damage friendly mobs, including the caster's summons. Allied players remain protected. Existing explicit exceptions for Ruby pulls, trial projectiles, Stand self acquisition, Electro meditation/conduction and the electrical egg remain in place. Lifestealer's special caster-area drain is owned by its existing service and uses the common projectile eligibility helper.

## Реализация

`LbcWands.projectileTarget` допускает дружественных мобов и собственные призывы, защищает союзных игроков. `validTarget` сохраняет жизнь, роль, наблюдателей и служебных NPC; обычный helper исключает стрелка. `target` сохраняет выбор врагов для AI/аур. `ProjectileLifetimes.target` дополнительно сохраняет Living/MOBS и одно измерение. Второго индекса целей нет.

Обновлены firstHit/native collision, native урон и отражение, Terra/обычные лучи, Electro/managed lightning, Shooters, Exasperato/TNT, Acid, Psycho, Consuming, Stand, Cultus, Bolt и метеоры. Air, Stone, Blind, Ruby, Teleport, Bedrock и Exoperso используют общий контакт. Прежние явные исключения для союзных игроков сохранены: Ruby, испытания, Stand, медитация/проводимость Electro, электрическое яйцо.

Поджог fireball/managed lightning и огонь блоков сохраняют владельца и признак снаряда через `LbcIndirectKills`; обычные неснарядные опасности сохраняют прежнюю проверку союзников.

Вредные splash/lingering эффекты проходят `LbcIndirectKills.allowProjectileEffect`: чужой союзный игрок защищён, дружественный моб допустим. Полезные эффекты и обычное нативное применение своего зелья к себе сохранены. Splash wrapper нужен потому, что native addEffect передаёт стрелка вместо зелья; cloud/status/instant effect используют прежние mixins. Нативные собственные стрелы и Ender Pearl сохраняют self-поведение; глобальной неуязвимости владельца нет.

Самонаведение выбирает только врагов (user, ответ 21 октября 2026): Air, Homing, Psycho, Consuming, повороты Bolt и перенаправление проклятия сохраняют прежнее приобретение цели. Физический контакт может задеть дружественного моба. Прямой контакт Homing и наложение проклятия используют новую проверку удара.

## Проверки

Build и два настоящих изолированных матча общей политики прошли на SHA
`a40e004121897c8c08f1b43d77219c01af5f85ac66042977a64232fa3e2df3bf`
за 46,31 секунды. Подтверждены дружественные мобы/собственные призывы,
защита союзных игроков и служебных NPC, native potion-сценарии и настоящий
LargeFireball: взрыв вне траектории поражает дружественного моба, сохраняя
защиту союзного игрока/NPC. Подробные suites и границы —
[SERVER_TESTS](../SERVER_TESTS.md). Это целевые проверки текущей политики;
результаты дополнительных регрессий здесь не объявляются.

Mobgun также прошёл build и два изолированных матча на том же SHA за 40,46 с.
[Контракт Mobgun](MOBGUN_PORT.md). Архив `build/verification/skywars2-mobgun-friendly-a40e0041.jar` сохранён;
установки нет, RP пользователя и template не менялись.
