# First Wands batch checks

`GameChecks` provides `wands-menu`, `wands`, and `wands-cleanup`. The menu case checks four native loot payloads and Polymer representations, source mana scores (25/63/150), the LBC trinket menu cards, and settings slot 8 for `offhandWands`. It toggles the setting through the GUI, checks `swrg.lefthand_on/off`, then restores and saves the original value.

The match fixture invokes the registered native wand callbacks. It covers each hand individually with the other hand empty, disabled offhand rejection, enabled dual-cast Tiny Fire + Tiny Blind for exactly 88 mana, and independent secondary casting when primary Blind cannot pay (62 mana leaves 37 after offhand Fire). It also checks `applyNoMagic(3)` extending a two-second timer to five while draining current mana. Existing focused checks cover nomagic regeneration freeze/countdown pauses/cleanse, Creative payment, Mage cap without overdraft, Overload overdraft, native projectile ownership/trajectory/team collision, Tiny Blind wall/ally/observer behavior, trinket hooks, and TNT ownership and corrected spawn position (eyes plus 0.5 blocks on world Y; user, 20 October 2026).

The fixture restores hands, inventory/crafting/cursor, armor, player state, tags, effects, teams, scores, timers, and configuration in `finally`; newly spawned projectiles and TNT are discarded there. `--wands-only` runs bots, menu, one Wands match, and cleanup on an isolated test server. The full runner adds the Wands match after Overload cleanup.

Фактический результат финального JAR `7741ffb4…`: адресный прогон —
51,32 с, один матч / четыре уникальные check-группы; полный — 320,51 с,
21 матч / 59 сценариев / 63 уникальные check-группы. Все прошли;
проверенная сборка установлена. Хеш целиком и логи:
[SERVER_TESTS.md](../SERVER_TESTS.md). Настоящий клиент и CPU benchmark
не проверялись.
