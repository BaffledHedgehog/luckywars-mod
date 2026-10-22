# LuckyWars modifier configuration and match owner

## Author scope (user)

> Только LuckyWars, как в исходнике

> че? модификатор удачи это постоянная настройка, его в настройках в гуи даже можно менять

> чево блин, в обычном скайварсе так то нет лаки блоков и предметов непосредственно из них вообще (а модификатор тока на него влияет и дропы с него) так что там ни на что модификатор не влияет (тот же мобган - его нет в обычном скайварсе, только в режиме с лаки блоками)

`luckyModifier` is persistent configuration. Match cleanup must never reset this
field or rewrite the user's GUI selection. Its consumers are Lucky Blocks and
their drops. Ordinary SkyWars has neither; do not describe a retained source
score as a gameplay bonus leaking into normal SkyWars. The native owner retains
the last valid resolved result between matches, as the source does, and resolves
again only on the next eligible LuckyWars start.

## Implemented common configuration

- `GameConfig.luckyModifier`: default 0, JSON persistence, load/save clamp 0..12,
  command aliases `luckyModifier` and `cfg_lucky_modifier`, canonical persisted
  field `luckyModifier`. Invalid command values preserve the existing setting.
- Settings page1000 slot11 cycles 0..12 through the existing lobby/admin control
  path. Rendering clones original source styling and uses the complete existing
  `swrg.cfg_lucky_modifier_0` through `_12` keys. No RP changes are needed.
- `/settings` retains its OP permission; the online `admin` tag only controls the
  public GUI. Source aliases and value suggestions are available to operators.
- `GameLuckyModifiers` uses the existing mana/score helper for the two global
  score bridges. Configuration publication never rolls or changes an active
  category. Match start/end are constant work, without per-player/tick scans.
  Ordinary start/end never reset or reroll a retained result. An explicit reset
  helper exists for isolated checks/debugging and is not used by match lifecycle.
- The pure source resolver preserves fixed1..12 and the random100-entry table:
  89 neutral1 outcomes, one outcome each for2..12. This categorical selector is
  unrelated to the player's `minecraft:luck` attribute or loot-context luck.

## Qualification status

The lifecycle integration build and full runner passed on
`69de8601c2bf35faf7b719de97febb5f6eaccdeb981ca7881ee5ebec23e55de4`.
Ready, not installed: `build/verification/skywars2-lucky-modifiers-69de8601.jar`.
Evidence: `build/verification/lucky-modifiers-final/`. Two real ordinary matches
retained the snapshot of resolved category, configured selector, scoreboard and
disk across start/stop. Both included shooter-pool, Mobgun and ambient lightning
acquisition regressions. The independent LuckyWars service fixture covered a
retained previous6/configured7, invalid result initialization, Lucky7 then cached7,
ordinary configured12 retaining7, and next Lucky6. Original configuration bytes
and score fixtures were restored. No production mode guard was bypassed.

The final archive again passed CRC, all2642 resource byte comparisons, original
Fabric metadata and bundled SGui checks. Main remained332415d6, original RP was
read-only, and the isolated server stopped normally with its world lock released.

The log retains Polymer's chunk-packet missing-context diagnostic immediately
after fixture bots join, with a `PlayerChunkSender` stack. The same diagnostic is
present in the earlier qualified lightning-acquisition, Tiny Reforge and Reroll
Machine logs, before this patch. It is recorded rather than hidden; this run does
not qualify a real client's chunk rendering or fix that pre-existing diagnostic.

### Earlier configuration-only qualification

The final build and isolated native settings run passed: all13 network-click
states/source styles, persisted JSON defaults/clamp/roundtrip, live admin changes,
OP dispatcher restrictions, unchanged unfinished mode guards and original native
loot conditions at categories1/6/7/12. The standalone owner also passed fixed,
random, idempotence, config-publication isolation and explicit-reset checks, with
exact fixture score restoration.

Ready configuration archive: `build/verification/skywars2-lucky-settings-5283a768.jar`.
SHA256: `5283a7684e5c5f92af0f8269532af158b3190a6e98c8989a70884295802ecb04`.
Evidence: `build/verification/lucky-modifiers-settings-final/`. All2642 processed
resources matched the archive, JAR CRC and original Fabric metadata were checked;
bundled SGui remained present. The isolated server stopped normally and released
its world lock. The main JAR remained332415d6; neither main server nor original RP
was changed.

The configuration-only archive above is historical. The subsequent integration
calls `startMatch()` after the minimum-player check, before chest filling and kit
rolls, and `endMatch()` on finish, cancellation/lobby restoration and shutdown.
Only gameMode2 resolves a category; gameMode1/3/4 retain the current result without
using randomness. End only releases the once-per-match guard. No saved setting or
valid resolved score is cleared. Fresh/invalid result scores initialize to neutral1.

The standalone owner and deterministic resolver are checked by
`LuckyModifierChecks`. `tools/smoke_lucky_modifiers.py --settings-only` restricts
qualification to configuration and loot conditions. The full runner includes two
ordinary start/stop matches and shared shooter/Mobgun/lightning acquisition
regression. Direct ported-item fixtures in that isolated runner are test setup,
not a claim that normal SkyWars should offer LuckyWars loot. The LuckyWars service
branch is checked independently; the unfinished mode guard remains in force, so
this does not qualify a real LuckyWars gameplay start.

LuckyWars gameplay remains unavailable under the existing mode guard. This patch
does not claim that all variant2..12 encounter/drop mechanics are ported. It does
not change vanilla quality weighting or forward player luck into loot contexts.

Source audit: [LUCKYWARS_LUCK_SOURCE.md](LUCKYWARS_LUCK_SOURCE.md).
Next encounter audit: [NEXT_LUCKY_ACQUISITION_SOURCE.md](NEXT_LUCKY_ACQUISITION_SOURCE.md).
