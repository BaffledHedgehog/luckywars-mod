# Next contained skills source audit — 2 November 2026

This historical preparation is now superseded by the author's resolved rules and
[completed skills1/12/13 port](CONTAINED_SKILLS_PORT_2026_11_02.md). Do not repeat
its earlier clarification requests; all three skills are exposed and qualified.

Read-only bounded audit; only this new note was written. No source, runtime, build, RP or git edits. Existing notes preserved. This is research, not a qualification or completed port.

Legacy root (`L` below): `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Project: `E:/servers_mew/skywars2_last/luckywars-mod`.
Recommended batch: **1 / 12 / 13**, a small effect conversion, death/life rule and periodic hazard. IDs2/3/4/6/8/10/11/19 excluded as requested; 5/9 already exposed. ID7 has a much larger armor mutation chain, 14 substantial bow timing/traits, 16 random-stat authority, 18 scheduled loot ownership; retain for later. TMT remains last.

## Original menu and naming

`L/function/swrg_kit_integration/gui/{page,select}/10000.mcfunction` agree on:

| Skill | Original zero-based slot | Gate | Locked key | Existing label/lore keys |
|---|---:|---|---|---|
|1|0|`lbc:true_advancements/accesories/jaakivi`|`lbc.adv.accessories.jaakivi.lore`|`lbcskill.0`, `lore.lbcskill.0.0/.1`|
|12|11|`lbc:true_advancements/axes/banhammer`|`lbc.adv.axe.banhammer.lore`|`lbcskill.11`, existing corresponding lore|
|13|12|`lbc:true_advancements/axes/hammer`|`lbc.adv.axe.hammer.lore`|`lbcskill.12`, existing corresponding lore|

Loot presentation roots are `L/loot_table/gui/page/10000/{0,11,12}.json`. Ranked page selection branches separately; preserve current project's established ranked handling rather than adding a second policy. None of these IDs is exposed in `src/main/resources/skywars2/lbc_loadouts.json` at audit time.

Root agent separately inspected actual author RP read-only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Existing lore describes slowness immunity/conversion to Resistance II, lethal other-player damage exceeding max HP plus Banhammer immunity, and every-five-second falling exploding hedgehogs. No new translation keys are indicated.

## ID1 Frozen Heart

Exact chain: `L/advancement/slowness.json` -> `function/swrg_kit_integration/game/skills/frozen_heart.mcfunction` -> `_2.mcfunction` -> `_3.mcfunction`.

Advancement is native effects_changed with Slowness present, `lbc.skill=1`, outside `minecraft:nexus` and `minecraft:imprinted`. Entry checks global `#lbcskill=1`. `_2` revokes the advancement, reads active Slowness **duration ticks** to integer storage, calls macro `_3` (`effect give @s resistance $(tmp) 1`), then clears Slowness. Hence original gives Resistance II with that tick count interpreted as **seconds**: 20x duration inflation. It does not preserve input amplifier; conversion is always amplifier1. No stopper/nomagic check exists anywhere in this short chain.

Author clarification required: equal real duration versus original 20x inflation; MAG/TECH/strong-artifact admission policy; whether restored/already-loaded Slowness converts on selection and how native hidden Slowness layers should be consumed. Infinite duration needs deliberate native handling rather than passing -1 as seconds. Respect already-qualified Parasite rejection before effect mutation and guard callback reentrancy.

## ID12 VIP

Exact chain: `L/function/leave_death.mcfunction:434` -> `swrg_kit_integration/game/skills/killed_by_player.mcfunction` -> `vip.mcfunction`. `L/advancement/lethal_damage.json` is entity_killed_player whose killer is a player, outside Nexus/Imprinted. Dispatcher requires that advancement and skill12, then always revokes advancement. VIP compares `damage_taken > floor(max_health*10)`, adds one `swrg.lives` if strictly greater, and zeroes damage_taken. It refunds a life, does not prevent native death, suppress loot, or skip normal death processing.

Crucial accounting: `L/function/load.mcfunction:169` registers `damage_taken` as **minecraft.custom:damage_taken**. `L/function/items/damaged.mcfunction:24` schedules `items/damaged_scheduled` two ticks later; that scheduled function resets the objective for **all players**. Therefore literal source is recent accumulated native damage-stat units since last global reset, potentially multiple accepted hits, not max(0,health_before-health_after), not raw pre-armor damage and not necessarily only the final strike. Exact reset timing has incidental cross-player coupling. Ask author whether intended rule is one accepted lethal hit exceeding max HP or exact legacy recent accumulation; preserve strict greater-than and ten units/HP either way.

Second branch: `L/function/items/swords/banhammer/tick_fall_end.mcfunction` excludes skill12 players from kick ability within5.5 when skills globally enabled. There is no damage/death hook here and no jammer check. Native project **already has** this carrier guard in `src/main/java/me/skywars2/game/LbcSpecialTools.java:235` (`immuneToHammer`), currently checking config+profile rather than `game.lbc().hasSkill`. Preserve Greed immunity and WILD mob erasure/other kick rules; make skill admission canonical when implementing.

Native integration inspected read-only: `mixin/IncomingArtifactCriterionMixin.java` wraps `LivingEntity.hurtServer` through `items.withIncomingDamageAttempt`; `game/LbcReactiveSkills.java:48` tracks actual health loss for skill6 and deliberately excludes death payout. Do not reuse that health-loss number for VIP. `GameController.died` decrements participant lives after death callbacks, then clears item/loadout state; refund/skip-debit decision belongs before this decrement, without cancelling actual death/respawn. `SkyWars2.java:186` calls it from AFTER_DEATH. `BlackHoleDamage.accepted` also enters native death before its synthetic hurt criterion, so criterion-only observation can be too late for lethal VIP decisions. Need capture at native accepted damage-stat mutation or equivalent native post-mitigation amount **before death dispatch**, with a nested-attempt-safe context accessible by GameController.died. Verify stat award/death order in pinned actual Minecraft bytecode before coding; this audit did not qualify that precise bytecode location. Cover raw-HP bridge explicitly because it synthesizes damage/death/stat paths.

Author clarification required: suppression per MAG/TECH/strong-artifact; final-hit versus incidental recent-stat-window semantics; use direct other-player lethal source versus credited indirect player source (source advancement is killed-player trigger, not simply arbitrary stored last attacker).

## ID13 Falling Hedgehog

Exact chain: `L/function/players_5s.mcfunction` -> `swrg_kit_integration/game/skills/skills_tick_5s.mcfunction` -> `falling_hedgehog.mcfunction` -> `falling_hedgehog_at.mcfunction`; landing via `L/function/other/silverfish_tick.mcfunction:8` -> `falling_hedgehog_explode.mcfunction`.

Global skills-enabled dispatch; inner tick rejects legacy mode. Every5s summon invulnerable silverfish with `angry_hedgehog,lucky_mob` at ownerY+100. Select tagged silverfish near that point (legacy nearest identity is not safely bound), spreadplayers minimum0 maximum70 respecting teams=false, then teleport selected silverfish another100 blocks above its spread landing position. Flash particle and large-blast sound volume7 pitch0.8. On native OnGround: reset OnGround false, summon silent ignited Fuse0 creeper radius7 with translated name `angry_baffled`, kill silverfish. No owner ID, team filter, cooldown cap or stopper/nomagic gate in these functions. Creeper uses ordinary source explosion terrain rules; invulnerability is on the carrier only. Do not collapse this into other existing projectile hedgehogs.

Author clarification required: WILD including owner/allies versus safe teams; terrain destruction rule; MAG/TECH/strong-artifact suppression of creation and existing actors; whether original spread safe-ground +100 is desired. Resident-only native placement and identity-bound actor work should replace global selector accidents. Respect world height/residency, time pause, actor cleanup/checkpoints without loading/generating broad terrain. No source claim about peak TPS.

## Suppression evidence and open decisions

All three short source chains omit `stopper`, `stopper_magic`, `nomagic` and strong-artifact admission tests. This is a source fact, not permission to infer author policy from adjacent items or skill6/10. Root agent has opened author chat questions for these decisions. No implementation should freeze an invented policy. Preserve global skills, actual match/role admission, original achievement gates and normal lifecycle cleanup regardless of chosen jammer policy.
