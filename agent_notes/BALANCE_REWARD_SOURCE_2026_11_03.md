# Balance reward source readiness — 3 November 2026

Historical source audit written before implementation. For current implementation, author decisions and qualification, see [Supernova chain integration](SUPERNOVA_CHAIN_PORT_2026_11_03.md). Readiness verdicts below describe that earlier audit.

Read-only audit; only this note written. No Java/resources/build/server/git changes. Original datapack, Paper source and RP remain read-only. TMT stays LAST. Read Serena manual, project AGENTS, agent_notes/AGENTS and original plugins/luckywars/agents.md. Author instructions take priority over summaries.

Roots: L = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc; P = E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars; F = E:/servers_mew/skywars2_last/luckywars-mod.

## Verdict and identity correction

`lbc:balance` is NOT currently a native ready/public tool. F/src/main/resources/data/lbc/loot_table/balance.json still produces the vanilla carrot_on_a_stick with `{magicwand:1,item_balance:1}`, tier_x, model lbc:balance, gold item_balance name and eight lore keys. Exact Java searches find no item_balance consumer, Balance registered item or new_game implementation. Bundled loot/model/translation presence does not establish behavior readiness.

Crucially this item is NOT a combat ray/stage/selfcast wand. L/function/workingitems/spell1_default.mcfunction:59 routes item_balance straight to `lbc:other/new_game/click`. Do not attach an unrelated wand/ray family to this reward. Generic spell1 temporarily assigns caster/raycaster and clears Distance, but click has no ray queue, projectile, stages or selfcast tree.

## Actual intake and suppression

L/function/items/spells.mcfunction:7 admits mainhand magicwand only when caster lacks nomagic_active and there is no non-spectator nomagic_active player within20. spell1 chooses ordinary versus upgraded by upgraded flag; canonical Balance has no upgraded. Exact search finds NO item_balance route in spell1_default_left, so existing source provides mainhand activation only. Shared offhand magicwand admission alone is not a Balance consumer.

Balance's own dispatch line has no stopper/stopper_magic, mana threshold, mana debit or cooldown. Thus local marker MAG/TECH suppression is absent at that line, while shared anti-magic artifact suppression above still applies. The old overload temporary mana addition in items/spells is historical source implementation and must not be copied. No new native Balance admission policy is qualified. TimeStop/observer/use callback gates must follow native lifecycle and an explicit author decision where it differs from source.

## Complete direct behavior boundary

L/function/other/new_game/click.mcfunction is a second legacy match transition, distinct from Supernova newworld completion:

- Disable block/entity/mob drops; teleport nonplayers except supernovacore to0,-600000,0 and kill three times; clear ALL online inventories and effects; call lbc:resets per player.
- Clear all58 everythingend and8 baffledend scheduled functions; stop nuke/exoperso/backrooms music; zero darkness/abyss/powerfall/trollhunter flags.
- Reset gamerules in overworld/nether/end/imprinted; remove all forced chunks in first three; clear overworld weather; swrg:maploader/clear.
- Place exactly four templates swrg:-1-6,-1-7,-1-10,-1-11 at X/Z -48/0 and Y48; force spawn chunks and remote10241024 chunk.
- Set ALL lives to1; clear GUI/reset attributes; teleport all to0,128,0, randomly assign nonspectators to spawn markers, face look marker; fill chests; play burning_balance at extreme Y endpoints.
- Set legacy=1 and launch legacy_tick, legacy_tick_1s, legacy_tick_5t and legacy_tick_5s immediately. Create eight legacy cooldown objectives; give resistance/instant_health/saturation; reenable drops; remove levelups and gamehealthmodifier; zero #lbcskill; grant new_world advancement to nonspectators.

Inventory clear consumes Balance incidentally; no independent per-item debit appears. This is not a cheap item-use leaf. Direct transitive closure includes lbc:resets/reset_gamerules, swrg GUI/attributes/chests/map clear, four fixed legacy-map templates and ongoing old-equipment gameplay. Legacy loops dispatch old mystical artifacts, jump boots, healing, mana hat/belt, reactive boots and decrement old evoker/bass/ender/corrupt/vampire/foxus/blood/wiki cooldowns. Old wand routes follow Balance in spell1_default. The desired legacy-mode equipment pool/runtime must be audited before claiming complete source closure. Do not introduce arbitrary legacy function execution support to make the item appear functional.

## Registered Paper authority and existing native reuse

Exact search P/src/main/java for item_balance, other/new_game, balance_user and shishgun found only balance_user in SwrgNativeService's reset tag list. No registered Balance/new_game override was found. Existing registered `swrg:game/start/1` override belongs Supernova completion; Balance click does not call it, so it does not automatically replace this transition. Registered override precedence still applies to every transitive swrg function when implementing actual source behavior.

F/MapManager.load(id,broken,Consumer<Result>) is the existing map replacement owner; F/GameController.restartFromRune owns full fresh-round restart through stopMatch/restoreLobby/requestStart. These are useful existing APIs, but neither expresses this exact fixed legacy-mode transition: ordinary full restart clears/regrants native loadouts, enrolls participants, changes profile starts and selects normal maps. Need a controller-owned typed legacy transition using existing map/template/chest/lifecycle owners, not raw broad kill/clear/schedule commands or a second world-reset framework. See SUPERNOVA_MAP_TRANSITION_SOURCE_2026_11_03.md for the separately audited map API limits.

Recipe readiness does not close reward readiness: LbcCraftingRecipe.Kind.SHIT_KATANA and native LbcElectro.SHIT_KATANA already exist. LbcShooters exact shish/railgun search has no match; do not identify an unrelated native shooter as Shishgun by similarity. Supernova ingredient/event producer and this reward's second transition remain independent gates.

## Genuine author questions before a Balance port

1. Is the reward intended to launch the original fixed old/legacy mode, including inventory/effect/perk wipe, lives1 and old equipment loops, or a new native round? Source behavior is specific and differs from Supernova's same-match map replacement.
2. Which dimensions/participants should this second global transition affect? Source clears all online inventories/effects and teleports all, including Nexus/Imprinted, but later spawn assignment excludes spectators. Offline/joining/vanity/dead membership requires native ownership policy.
3. Preserve mainhand-only and shared anti-magic-artifact gate, while ignoring marker MAG/TECH, or explicitly change it? No Balance-specific mana/cooldown exists to infer.
4. Preserve fixed four-template legacy map/old gameplay and dropped forced-chunk behavior, or define supported native substitutes? Existing ordinary map restart is not an authorized semantic substitute.

No evidence from this audit supports publishing lbc:balance as working merely because Supernova can grant its raw loot. TMT remains deferred LAST.
