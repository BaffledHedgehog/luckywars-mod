# Mana port: current implementation

## Implemented

`LbcMana` stores the established scoreboard contract: `mana`, `manamax`, `manaregen`, `manaregen_visual`, `effect_boost_mana`, and `reverse_mana`. `resetResources` sets `mana=5000`, `manamax=5000`, base and displayed regeneration to `10`; the match calls it when a player enters play. `LbcItems.tick()` calls `mana.tick()` once per server tick, and `tickPlayer` makes at most one ordinary mana adjustment in that tick. There is no second native regeneration addition.

Displayed regeneration is base `manaregen` + mage armor by slot (head/chest/legs/feet: `4/6/7/4`) + `2` for each mana belt counted in inventory. Belt counts are cached and invalidated through `inventoryChanged`; the implementation first checks whether a belt exists in inventory and only then uses the native count helper over inventory plus the 2×2 crafting slots/cursor. The inventory gate is deliberate and retained from source behavior.

While `effect_boost_mana > 0`, displayed regeneration is `floorDiv(regeneration * 3, 2)` and the boost counter loses one tick per game tick. Raid Omen amplifier 16 is queued by effect callbacks and consumed on the next eligible mana tick; the brief duration-2 callback edge is normalized to one tick before the effect is removed, so the boost counter receives one remaining tick. Amplifiers 17–20 now use their own stat timers in the same component: [mana potion contract](MANA_POTIONS_PORT.md). One-tick inputs transfer immediately in the effect callback so vanilla expiration cannot discard them.

`reverse_mana` is stored in seconds and decremented once per second. While positive, mana loses the displayed regeneration for each active tick. The normal positive-regeneration path runs only below maximum; negative regeneration still runs and is capped above at `manamax`. Mana has no lower clamp. Ordinary positive costs go through `trySpend`, which rejects negative costs and refuses overdraft; `add` is the explicit signed adjustment API for effects that intentionally allow negative mana.

Regeneration and checked spending require a living gameplay participant in the arena, outside the ending phase, without time stop or magic suppression. Creative remains legal; an observer's copied items/effects grant no resource processing. Raid Omen XVI conversion pauses during time stop but can convert under magic suppression; the resulting boost waits until regeneration resumes. The reverse seconds counter also pauses during time stop, but not merely because a magic beacon is nearby. These are server-thread APIs.

Death cleanup calls `clear`, which clears all five mana-potion timers and reverse effects but preserves `mana`, maximum, and base regeneration. Match entry resets resources. HUD cleanup is sent when the HUD becomes invisible and on player/match cleanup. Combined actionbar uses the source mana glyph/numbers/separators and appends rocket HUD only while rocket state exists; no words or translation keys are added. `LbcItems` caches the last mana/max/regen/rocket values and sends on a value change, otherwise refreshing at most once every 20 ticks to keep the actionbar alive. GameController and its match tick own this work on the server thread.

## Explicitly not ported

This implementation does not include Nyauwu armor passives, remaining wand/spell mana consumers, the complete source actionbar or third-party/SurvivalVersion integrations. The first consumers are [Tiny Fire/Blind and their cast bonuses](WANDS_FIRST_PORT.md). Negative-mana penalties, Crux and tentacles belong to [LbcOverload](OVERLOAD_PORT.md). The HUD scope is currently mana plus the transferred rocket indicator; cold, frozen, other cooldowns and remaining source HUD indicators are future work. This is a code-scope statement, not a claim that all mana behavior has been ported.

## Evidence and limits

`ui-gameplay` PASS: 55,66 с / 2 матча / 19 групп; GUI/quickstart PASS: 126,58 с / 3 матча / 7 сценариев. HUD и mana potion подтверждены. SHA обоих прогонов `afb96b4ec69ddd27499eed92cf2f6e5ba7cb1738df7d8e45fde9a77650c13b61`; артефакт `build/verification/skywars2-ui-lobby-acid-afb96b4e.jar`. Общая регрессия на 78 групп и проверка реальным клиентом не выполнялись; JAR не установлен на основной сервер (активен `341c6293`). Итог: [SERVER_TESTS.md](../SERVER_TESTS.md).

HUD читает значения scoreboard через read-only `hudValue`: чтение не создаёт `ScoreAccess`, objective или player score. `ServerScoreboard` рассылает обновления для отслеживаемых objectives; пометка scoreboard dirty при изменении не означает запись на диск при каждом вызове. Отдельного benchmark стоимости scoreboard/HUD не проводилось.

The previous core-only baseline passed the focused scenario (57.12 seconds, three groups) and full regression on the same JAR (300.90 seconds, 18 matches / 50 groups). Its full log/results are archived as `lbc-loadouts-before-mana-potions.log`/`-results.json`. Current potion-build evidence and installation status: [SERVER_TESTS.md](../SERVER_TESTS.md). Source behavior and exceptions are documented in [MANA_CORE_SOURCE.md](MANA_CORE_SOURCE.md) and [MANA_UI_SOURCE.md](MANA_UI_SOURCE.md). No CPU benchmark or graphical-client verification was performed. HUD glyph rendering depends on the user's read-only resource pack; the resource pack and template were not edited.

Implementation references: `src/main/java/me/skywars2/game/LbcMana.java`, `LbcItems.java`, `LbcColors.java`, `LbcMagic.java`, and match lifecycle in `GameController.java`.

Current Overload and acid-projectile changes are summarized in [OVERLOAD_PORT.md](OVERLOAD_PORT.md);
the earlier focused/full server-run record there predates the current pending shared smoke.
The complete four-piece Overload set enables wand spending into negative mana.
Wand costs use the player's actual mana, never temporary mana or a +10,000,000 substitution.
Tiny Fire/Blind, Air/Stone/Teleport, [Ruby/Terra](WANDS_THIRD_PORT.md) and [Acid](WANDS_FOURTH_PORT.md)
use this API; remaining wand consumers are future work.
The personal [nomagic timer](NOMAGIC_PORT.md) pauses regeneration and the
Crux/debt cycle, while leaving eligible casting and potion processing intact.
