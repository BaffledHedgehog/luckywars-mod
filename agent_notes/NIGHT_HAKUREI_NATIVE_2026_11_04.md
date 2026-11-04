# Native Hakurei / Touhou implementation — 4 November 2026

Status: implementation and scheduled fixture prepared; root integration and exact artifact qualification are required. No build, server operation, commit or resource-pack mutation was performed by this agent. Existing unrelated workspace edits were retained. Serena initial instructions, repository AGENTS, agent_notes/AGENTS, WORKFLOW and RESOURCE_PACK were read. Serena Java symbol search fails on the C:/E: mount boundary in this session; narrow rg/code reads were used after that concrete failure. TMT LAST.

## Source and authorized correction

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

Exact handlers: `swrg_kit_integration/items/touhou_items/*.mcfunction`, `items/wands/danmaku_staff/bomb_tick_fast.mcfunction`, `items/wands/danmaku_staff/bomb_tick_2_fast.mcfunction`; closure audit: [NIGHT_SKILLS_SOURCE_2026_11_04.md](NIGHT_SKILLS_SOURCE_2026_11_04.md), skill28 section.

Root selected actual victim item/XP publications using the existing CombatRewards death scope, applying the standing real-victim-drop rule and removal of one-tick selectors. This corrects legacy global Age0/1 scans and their collateral/recursive duplicate rolls. This agent adds no collector, loot reroll of victim loot, age polling or arbitrary function interpreter. Root owns the final author question and publication decision in NIGHT_QUESTIONS_2026_11_04.md.

Pickup/reward/input have no added local MAG/TECH/artifact gates. Magic-tagged Danmaku actors retain existing common actor suppression and target protection. Ordinary role/owner/epoch/dimension/input and native TimeStop policies still apply. Any picker receives the complete ten-collectible behavior, independently of skill28.

## New owners

`LbcTouhouCollectibles(GameController, Predicate<ServerPlayer>)`:

- `inventoryChanged(ServerPlayer)` dirties an inventory event; `tick()` handles at most32 inventories per tick. It reads custom-data only for this changed inventory, consumes all matching stacks in one transaction, and suppresses its own reentrant dirty notifications.
- `Counters(power,timeOrbs,bombFragments,lifeFragments)` owns persistent typed counters, with source scoreboard mirrors for HUD/status. All ten exact IDs are handled, source power150 cap and six attack/four multiplied-base health modifiers retain exact `minecraft:touhou_power1..10` identifiers.
- Time25, bomb5 and life10 conversion happen ONCE per select invocation. Time threshold is checked only if new time-orb input was present. Saved excess does not silently loop each tick. New other collectible input checks the saved bomb/life thresholds once again, as source does.
- Source mana100/unit, lives/unit, regen/unit and coins2/unit go through existing mana/participant/profile owners. Source full-power and time-orb title/subtitle components, coin and honey sounds use original keys/styles.
- `use(ServerPlayer, InteractionHand)` handles source bomb/time consumables; the actual ordinary input hook must call it before vanilla egg/snowball publication and mark a handled rejected custom consumable as handled to avoid falling through to vanilla. It debits one only after the shared controller accepts.
- `forget(ServerPlayer)` resets owned counters/modifiers; `stop()` resets all. Root should call `inventoryChanged` after a new playable loadout/join/rebind as well as actual item pickup/inventory mutation. Failed full-inventory pickup must not consume source entity or counters.
- `checkpoint().prepare(Function<UUID,Entity>)` returns the standard `MatchFieldCheckpoint.Plan`. `prepareFrozen()` / `publishFrozen()` protect lifecycle cleanup during coordinated replay. Include dirty queue and counter plan in existing skill/coordinator owner. The native player image owns inventory/resources/attributes; plan reapplies exact owned modifiers and mirrors after canonical player restore.

`LbcHakureiSkill(GameController, Predicate<ServerPlayer>)`:

- `eligible(ServerPlayer)` checks28 plus supplied owner policy. `reward(owner,level,Vec3,entityCount)` accepts FOUR rolls per actual captured entity, not stack count/XP value. Shared collector should pass one per successfully published real ItemEntity/ExperienceOrb, deduplicated by actual scope identity.
- `publishing()` identifies Touhou output publication so shared death scopes do not count descendants. Root must also exclude any skill18 duplicate/other synthetic reward publications from actual-victim candidates.
- `tick()` drains accepted records fairly with32 seeded native loot rolls and64 record visits maximum. Canonical `lbc:touhou_item` keeps ALL eleven entries/3234 total including nested bomb. Native table gets a saved per-record SplitMix-derived nonzero seed; queue copies retain remaining rolls, RNG, immutable output stacks and announcement bit. Rejected entity publication retries the SAME rolled stack without reroll.
- Accepted queue is independent of later skill selection, paused by the real publication world TimeStop/input policy, and cancelled on explicit owner forget/match epoch retirement. `forget` / `stop` are lifecycle cancellation; collectible counters and inventory are never truncated by this short-effect cancellation.
- Same standard `checkpoint().prepare(Function<UUID,Entity>)` plan and `prepareFrozen` / `publishFrozen` hooks; no body references live in this queue.

## Narrow shared controller additions authorized by root

`LbcDanmaku.useTouhouBomb(ServerPlayer)` adds the source paired yellow-glass fast bomb to the existing bomb owner. No staff charge/mana or `wandCasted` bonus. Distance300; Lifetime advances10 until310; initial scale fixed +/-30; source nested-floor radius; damage2 magic; retained60 empty ticks after crossing300. Ordinary staff bombs retain their prior4/scale/damage1 behavior. Real caster, target policy, hostile-projectile clearing and TimeStop remain shared.

`LbcDanmaku.checkpoint(MatchSpellCheckpoint.Links)` returns a typed nested `Checkpoint`; `.prepare(Links)` returns `Runnable` after all links resolve. Images include cards, both bomb bodies, actual owners, fast/charge/age/empty and external consumed-cast hand timers. ROOT MUST compose this image into `MatchSpellCheckpoint` capture/prepare/install, using that checkpoint's common Links graph. Existing Danmaku had no native private checkpoint in inspected sources. Actor/player native body snapshots remain canonical.

`LbcDanmaku.cloneProtected(Entity)` excludes negative companion shells; `cloneImage(Entity)` reconstructs card state or positive bomb with one matching negative companion and copied fast/age/empty. ROOT MUST route managed Danmaku through `CloneActorStates` protection/capture and gameplay classification as appropriate; do not clone inert display shells. Companion publication is owned by this image and rolled back with the positive shell.

`LbcTimeStop.useTouhouClock(ServerPlayer)` uses `begin(level,220)` and existing per-world engine, adds actual caster UUID immunity for the remaining stop (survives item debit), preserves have_stand/Stand-clock exemptions, and exempts native NoAI mobs during this consumable stop as source does. The new caster lease is stored/restored by the EXISTING TimeStop Checkpoint, removed on end/forget/stop. No duplicate clock engine, scheduler/gamerule emulation or extra cooldown/MAG launch gate.

Source ambiguity for ROOT question file: Touhou source sets common remaining11 seconds, including when another stop already exists; native method therefore sets220 rather than inventing a global max-duration refresh. Standing user Stand-clock behavior is20 seconds/refresh; decide explicitly how a Touhou consumable interacting with an active Stand stop should behave. Current source default can shorten a longer stop. Caster leases persist through subsequent shared begin/refresh until end. Review that mixed-stop policy before publication.

Chef/direct removal: source fast bomb directly kills eligible projectile/raycaster/mob_projectile classes and damages ordinary mobs2 magic. Native shared `hostileProjectile` and protected-role policy are retained; no new permanent Chef erasure was added. Any desired change to Chef membership/direct-kill behavior requires the per-item author decision; the existing GodClaw permanent transformation ruling is not generalized here.

## Resources and qualification hooks

Twelve NEW leaves: `lbc:ported/touhou_collectible_1..10`, `touhou_bomb_item`, `touhou_time_stopper`. Collectible leaves retain the exact corresponding canonical source entry/functions and reference the already-existing `item_defs/minecraft/repeating_command_block__6711510709f0`. No definition copies or RP files were written. Bomb/time leaves reference existing canonical tables.

Root owns LbcItems getters `touhouCollectibles()` / `hakureiSkill()`, constructor policies, actual pickup/inventory/input/death+XP integration, catalog/HUD readiness and shared checkpoint composition.

`HakureiSkillChecks.start(game)` / `.tick(game)` implement staged `hakurei-skill`: actual native item touches for all ten IDs by skill0 picker, native3234 source/style checks,99-unit fragments and once-only residual conversions, real full-inventory failed then successful pickup, ordinary field positive control for unjammed intake, actual credited cow death item/XP entity counts, unrelated newborn exclusion, shared TimeStop retained queue, actual nonempty whole-coordinator capture/restore, native shared bomb/time use and persistent caster exemption. Root should additionally qualify simultaneous/nested deaths, actual merged XP award paths and fresh clone inputs; these are not claimed passed here. No tests were executed by this agent.
