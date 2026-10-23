# Stalin encounter: source contract and implementation (2026-10-23)

Integrated and qualified on build42ad616b. Source datapack is authoritative;
Paper is comparative only. Two actual isolated mode1 matches passed16 recorded
suite results in42.05s. Installed main while stopped; rollback112316bc retained.
Agent did not start main. Final evidence:build/verification/stalin-academy-final/.

Read-only legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Actor: `function/drops/stalin.mcfunction`. Dispatcher: `function/mobs_5t_lbc.mcfunction`,
`function/other/stalin_shoot_pre.mcfunction`, `_pre2.mcfunction`, `stalin_shoot.mcfunction`.
Gun: `function/workingitems/ak47_otdacha_stalin.mcfunction`, `ak47_rec.mcfunction`,
`ak47_entity.mcfunction`, `ak47_block.mcfunction`. Reward: `loot_table/stalin.json`.
Acquisition: Stalin entry in `loot_table/loliluckyblock.json`, dispatched by `drops/specialdrops`.

The native actor remains a persistent, silent, killable zombified piglin, 500 health,
follow range100, native retaliation/movement/melee and vanilla hurt cooldown. Source
fractional token position gains only +.5Y. Red translated visible name, exact leather
head/chest/feet equippable assets, ordinary leggings, unbreakable head, hand AK model,
zero gear drop chances and both2000000-tick effects are retained. It is a combat
actor rather than a protected service NPC. Existing `lbc:stalin` is already converted
to the working native `skywars2:ak47` and semantically matches `ported/ak47`:
infinite enchantment, tier_s, exact two lore rows/event credit and transformable,
shooter, magicwand, ak47 flags, with no finite flag. Native death owns reward emission;
quiet discard owns match stop/unload/old epoch. No TTL, custom health loop or actor cap.

Important source timer detail: `_pre2` alone sets16 and plays rasstrelat. Missing
score is not zero. Once integer health<=495, a missing/out-of-range score initializes16
and decrements to15 in that same five-tick pass. Subsequent passes count to0. At0,
each pass independently has50% shot chance if any qualifying non-Stalin mob is in
sphere40. **Shooting never resets the source score.** Thus the injured actor can fire
every5ticks after the initial wait; Paper single-shot16-pass reset is not authoritative.
Mechanism suppression skips the entire pass; Time Stop pauses active tick progression.

The activation40sphere is not the ray length. Gun recursion uses801 samples at.2
(160blocks),9 arrow damage, source[-4,+4] integer yaw/pitch spread, white ash and5%
small flame, first actual native hitbox/block collision and destructible first wall.
The shared LbcAk47 engine and LbcWands resident collision/terrain leaves are reused.
Neither source gun creates a shield_blockable marker: no custom shield interception
was added. Native damage retains normal directional shield/armor/hurt behavior.
No new range/cap is introduced. Terrain consequences use WILD causal world changes.

Communist tag and custom_data communism inventory exclusions belong only to aiming.
Actual ray collision can hit a communist, another Stalin, the thrower or allies.
Spectators, observers and protected service roles remain protected at selection,
collision and final damage. Source aim selector has no distance bound; sorted
resident eligible entities are selected from the native dimension entity registry only
when a shot occurs. Player inventory includes equipment/offhand; native InventoryCarrier
inventories are also checked. A lack of eligible aim still emits shot sound.

The facing selector deliberately forks every eligible entity, sorted nearest, with no
limit. This was verified against vanilla ExecuteCommand (EntityArgument.entities and
fork), rather than assuming a single-entity syntax error. The source resets caster
Distance=0 before the fork, never per branch: the first nearest ray exhausts the shared
counter at160blocks or the first hit; each remaining branch emits only its origin
sample/collision. Native implementation retains this unusual burst with one full ray
and one origin sample per additional eligible target. Spread is drawn once for the
whole burst. No arbitrary target cap, and no Paper-style replacement cooldown.

Presentation was checked against the read-only RP `all_in_one_1.21.8`:
assets/minecraft/lang/{ru_ru,en_us,rpr}.json already contain stalin/drop_stalin/trap_desc;
assets/lbc/equipment/stalin.json has humanoid stalin and humanoid_leggings
stalin_leggings. No RP/template change is necessary or made. Token exact yellow
nonitalic name, white nonitalic trap_desc, tier_ff, drop_boss model and stalin/drop
flags are held in LbcStalinDropItem and ported/stalin_drop. Broad loli pools stay gated.

Service API: ready/spawn (dispatcher debits), tick, loaded/unloaded/stop, static managed,
owner and canAffect. Inspection View carries actor, activeTicks and nullable cooldown.
Package pulse, aimTarget and shot(actor[,direction]) permit deterministic native fixtures.
Common registration/dispatch, epoch/tick/load/stop, damage and indirect kill wiring
are integrated. Three preexisting user-modified sources remain byte-identical and
are excluded from this commit.

Integration now reuses the shared item dispatcher, per-epoch load/tick/unload/stop,
native damage role guards, WILD exception and LbcIndirectKills player resolution.
The native damaging entity remains the piglin: its thrower receives credited
MOB_KILLS, kill score and normal native death credit; the separate per-type
ENTITY_KILLED statistic belongs to vanilla's direct Player.killedEntity callback.
No global statistic change was added as part of this encounter.

Native fixture notes: the retained early attempts corrected a hit at native
actor tick0 (HurtByTargetGoal also initializes timestamp0), shield-sphere test
geometry and the direct-player-only per-type statistic assertion. These were
fixture corrections; production retaliation, radius and death attribution were
not changed to satisfy them. Final checks also invoke the actual seeded source
pulse, aim and spread path, not only the deterministic-direction physical gun.
A further fixture correction refreshes the enlarged cow's native dimensions so
the real spread shot has a wide physical target instead of relying on stale scale
metadata/eye-height margins. The final seeded production pulse passed in both
actual matches. All early attempt logs and build diagnostics remain local evidence.

Final253sources/690classes/2716resources match tested archives exactly. Native
acquisition, presentation, actual retaliation/melee/bullets, causal MOB_KILLS,
communism aim-vs-collision, walls, shields, suppression, source timer, actual
production pulse, Time Stop, native reward use, shared player AK regression and
real match-stop cleanup passed. The existing fake-player Polymer context warning
pair remains; no final suite failed. Real client pixels, full natural trajectories,
actual disk restart/unload/disconnect and peak capacity are not qualified.
