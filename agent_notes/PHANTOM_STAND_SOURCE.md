# Phantom Knives / Stand source audit — 2026-10-20

Read-only source audit; no production edits/build. Legacy paths below are relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Existing user annotations were not replaced. Source presence is not runtime verification.

## Live knives contract

- Entry: `function/workingitems/spell1_default.mcfunction:36`, `spell1_default_left.mcfunction:10`. Activation arms shared player `coldown13=25` only when its previous value is below -16 (normally -20). No mana/ammunition/item cost. Both deny initial activation within 20 blocks of `stopper_magic`. Initial suspicion of a missing mainhand gate was incorrect: native `ScoreHolderArgument.getNames` throws `NO_ENTITIES_FOUND` for the empty conditional selector before `ExecuteCommand.checkScore` returns. An unrelated item therefore does not blindly arm the burst.
- `function/tick2t.mcfunction` → `mobs_2t` → `players_2t:3-4`: every two active scheduled ticks, run burst while `phantomknifes_active` and score >=0, then subtract 5. Waves at 25,20,15,10,5; score 0 is a no-op. Ready again at -20: nine scheduler decrements, approximately 18 ticks after activation depending on phase. Do not translate score25 into a 25-tick cooldown.
- `function/items/changed_inventory/sel.mcfunction:64-76` maintains active tag from hotbar/weapon presence. `workingitems/phantomknifes/start_pre` checks actual current hands for every wave. Main and offhand may BOTH fire, use current aim/held items, and share one timer. Losing a knife hand skips that hand without resetting elapsed timer.
- Live offhand `phantomknifesleft/start` dispatches `phantomknifes/start_default_left`, `start_stand_left`, `start_stand_x2_left`. These reverse wave order. The separate `phantomknifesleft/1..5` are orphan files, not live launch implementations (including their odd backwards fifth origin).
- Normal wave uses `phantomknifes/1..5`: eyes-local origins x=+.2,+.1,0,-.1,-.2; forward .5. Velocity is local (+.3,0,1), (+.15,0,1), (0,.01,1), (-.15,0,1), (-.3,0,1), quantized at .001 from marker coordinates then multiplied by1.5. Do not normalize these vectors: outer knives are slightly faster. Each wave plays trident riptide_1, master volume1 pitch2.
- Native arrows: pickup disallowed, damage coefficient2, PierceLevel127, tags killedarrow/phantomarrow1; gravity/drag/native arrow damage remain active. Explicit player UUID Owner, lbcID2 and team_number copied. Coefficient is not fixed HP damage: native impact speed matters. No custom TTL in these files; use native lifetime and source ground cleanup (`function/projectiles/prein:8`, `projectiles/arrows:5`). No custom knives hit payload or lbcr damage function found.

## Stand knife variants

`phantomknifes/start` chooses ordinary unless BOTH have_stand and stand_clocks_active; both tags plus overheaven<=0 choose Stand; both plus overheaven>=1 choose x2. Choice is evaluated each wave. Missing overheaven score with both tags matches neither Stand branch, though resets initializes it.

| Variant | Per wave / full burst per hand | Arrow coefficient | Local fan |
|---|---|---|---|
| Ordinary | 1 / 5 | 2 | offsets above |
| Stand | 4 / 20 | 10 | -10,-5,+5,+10 degrees |
| Overheaven | 8 / 40 | 20 | -15,-11,-7,-3,+3,+7,+11,+15 degrees |

Stand outer wave yaw offsets are -20,-10,0,+10,+20 degrees, reverse for offhand; origins follow corresponding x offsets. `1_stand..5_stand` and `_x2` launch each arrow at speed1.5 in its rotated direction via `rc_vpered`. Same pickup/piercing/native physics. Owner/team are taken from temporary player `owner` tag; native port should capture exact player reference, not reproduce globally selected owner ambiguity.

## Stand acquisition / wider dependencies

- Knives are one branch of `loot_table/transformables_shooter.json`: model lbc:phantomknifes, flags transformable/shooter/magicwand/phantomknifes. No knife-specific recipe found. Do not enable incomplete random shooter pool merely for this branch.
- Stand arrow loot (`loot_table/stand_arrow.json`) is tipped arrow with raid_omen amplifier119, duration20, craftusable/stand_arrow flags. Obtained through `loot_table/arrows.json`, `loliluckyblock.json`; generic arrow pipeline maps119 to effect_stand_arrow_hit (`function/projectiles/arrows2:43`) and ground drops (`arrows_f:13`). `items/swords:55` also invokes stand hit on nearby hurt enemies when held as melee item. Effects tick repeatedly invoke `other/stand_arrow/hit_pre` outside anti-magic and decrement score; hit_pre drops an arrow each invocation.
- `other/stand_arrow/hit`: random1..100;1..10 kills unless sin_greed_active;11..100 grants Stand only when no have_stand and no positive overheaven. Gain sets have_stand and gives stand_oraora. Following no_stand branch repeats the same condition and is unreachable after gain.
- Clocks craft: `other/craft_ground:40` and `other/craft/inventory_craft_check:40`, time_stopper + stand_arrow. Ground recipe kills one entire nearby entity of each kind; inventory recipe reduces each matching input slot once and drops one clocks result (`other/craft/stand_clocks*`). This includes suspicious overconsumption when several matching crafting slots exist.
- `items/changed_inventory/sel:113-121`: have_stand_clocks_active checks container or weapon; stand_clocks_active is added only for container presence, removed when neither container nor weapon. Check slot semantics before porting.
- `players_10t:3` calls `other/stand_arrow/tick_10t_stand_clocks` with Stand+have_stand_clocks_active outside anti-magic: damage1 in_fire if no lifestealer in Inventory; y score>=20000 and overheaven<=0 upgrades. Upgrade sets overheaven5, tells upgraded_overheaven, drops homing_lightning, awards legends/overheaven.
- Match start (`start:3`, `start_stand*`) restores Stand for positive overheaven, decrements it once, schedules oraora grant6 seconds later. `resets` removes have_stand and initializes missing overheaven0; `purify` sets overheaven0. This is cross-match persistence, not burst TTL.
- Clocks right-click mainhand (`spell1_default:75`) invokes powerful time stop, cooldown40 units decremented once/second (`players_1s:13`). `time_stop_powerful` freezes broad entity sets, excludes Stand/clocks owners, sets global time_stopped21 and stops scheduled systems/gamerules. No offhand clocks activation found. Full Stand also changes oraora, lifestealer and lbcr/vampirism, and grants homing lightning. These are separate large families; a knives-only port must explicitly retain their migration status.

## Existing native owners to reuse

- `LbcShooters.use/tick/forget/stop`, existing two-tick burst, Arrow setOwner/baseDamage/pickup/orient, projectile tag/loaded/unloaded, canHit and block hit cleanup already own shooting lifecycle. Add knives there; do not create second generic arrow pipeline. Existing burst maps are Razorpine-specific (ammo and volleys differ), so extend explicitly.
- `LbcWands.use` dispatches shooters; `target` provides live enemy/observer/team checks; current `canFire` includes game role, map, alive, anti-magic, frost and time stop. Source continuation only checks held hands, so suppressing an already armed burst is a behavior decision, not directly established by source.
- `LbcItems` owns shooter tick, entity epoch load/unload, accepted damage, forget/stop. `LbcIndirectKills` already resolves projectile native owner and shooter owner; use its accepted-hit displacement attribution for falls. Owner inheritance for consequences is required (user); legacy omissions are not intended behavior.
- No native stand/overheaven handlers found across these classes; resource JSON alone does not implement them. Do not attach new incidental magical-charge pushes. Parent requested globaloverridesapply hook crosscheck; no matching literal/function was found under this datapack or initial plugin source searches, so that external hook remains an explicit audit limitation.

## Confirmed decisions and remaining Stand questions

- Update 21 October 2026 (user): preserve 90% gain / 10% death with Greed protection; one attempt and one returned arrow per hit. ORAORA is exactly 100 fists / 10 seconds and requires Stand in both hands. Implemented scope and remaining clocks dependencies: [STAND_PORT.md](STAND_PORT.md). The earlier questions below are historical observations, now settled by these answers where applicable.
- Different usable items in both hands must be usable in parallel (user); the native port preserves the common two-hand dispatcher.
- Count actual Stand clocks in the inventory and both hands (user). A stale active tag does not supply the bonus.
- Skip armed knife waves inside anti-magic while the timer continues; do not replay skipped throws (user).
- This batch implements knives and their conditional Stand variants. Full acquisition, clocks abilities, ORAORA and cross-match Overheaven remain the next linked migration, not completed functionality.
- Before that migration, clarify Stand arrow's 90% gain/10% death (Greed immunity), the unreachable no_stand branch, and repeated arrow drops from repeated debuff ticks. These remain observations, not approved behavior changes.
- Clocks crafting must use the established native exact-ingredient recipe path; inspect its legacy multi-slot overconsumption and wider time-stop dependencies before implementation.
