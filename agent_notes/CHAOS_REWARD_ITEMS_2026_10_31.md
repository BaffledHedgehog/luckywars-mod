# Random spellbook and Ender Whip — implemented; staged native checks passed

This worker edited only new `LbcChaosRewardItems.java`,
`ChaosRewardItemChecks.java` and this note. No build, native/main server run,
resource-pack change or commit was performed. Integration and native proof remain.
Earlier Big Bob files remain frozen.

## Sources and current implementation

Legacy read-only root is
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Both consumers are concrete rewards: `luckyblock -> lucky_items ->
transformables_staff` supplies `{unstable_spellbook:1}`; `luckyblock ->
lucky_items -> transformables_sword -> anal_whip` supplies `{anal_whip:1}`.
Native raw item identities/components remain unchanged except original authored
trial/mode/binding fields and the full target UUID clarified below.

`lbc/function/workingitems/random_spell.mcfunction` samples **1..55**. Existing
`LbcLibraryRandom.castRandom` correctly implements the separate **1..53**
`random_spell_lib.mcfunction`; its range must not be expanded or duplicated.
New raw admission delegates1..53 to that owner. Branch54 follows
`random_spell_lol.mcfunction`: sample1..242, reroll28..111 at most17 times,
invoke the existing `LbcLibrarySpells` owner, grant the source achievement.
The source has no128 command in this selector, so128 is an explicit no-op.
Branch55 uses the existing ordinary Sauvojen actor owner and awards the exact
selected `wand1..wand9` criterion. The same native Sauvojen route serves242.

Cost is the existing source scoreboard key `unstable_spellbook_mana_use` in
`lbc.math`, default2500 from `config.mcfunction:33`. Shared `trySpendWand`
preserves Overload admission/debit. Cast bonuses use shared `wandCasted`.
The source MAG admission remains. No new projectile/ray/tick/checkpoint engine
was added; existing owners retain source attribution, suppression, Time Stop and
Vanity behavior after release.

Stack-local Academy trial records `have1..have53` and invokes existing
`academy.ready` after all53. New `academy` handles only this raw reward within
the existing actor's admitted interaction: signup, already-ready messaging,
upgrade flag/mode1, tierSS and original upgraded lore. Upgraded cast modes use
the same55 branches. Sneak changes the actual used hand's mode without spending,
but still requires mana admission. Source upward wrap55->1 and downward
wrap1->54, including overlapping pitch-zero predicates, are preserved.

Whip original sources are `workingitems/ender_whip_get_id.mcfunction`,
`ender_whip_teleport.mcfunction`, and `_left.mcfunction`. The old bind guessed
the nearest HurtTime10 body. User clarification instead binds the **actual
accepted melee victim**, recording full UUID string `skywars2.whip_target` in
the physical stack and retaining original integer `teleportid` for compatibility.
Existing raw integer-only items may resolve one current matching native body;
a present full UUID never falls back to a first-word collision. Current target
must be a living native member of `minecraft:mobs` with an allowed gameplay role.
No distance/raycast/collision-safe destination replacement is invented.

User clarification requires **caster MAG suppression for whip teleport**.
Both caster artifact and beacon admission use the same shared suppression owner;
the caster can move himself into a destination MAG area. Binding remains an
accepted physical melee consequence. Teleport keeps the caster's yaw, pitch,
server velocity and shared `TeleportMomentum` wire flags. Source portal/sounds
and twitch helper's six sounds/particles/native bottle-to-succubus-potion reward
are retained. No item/mana debit, cooldown, target movement or damage is added.

## Root integration

All APIs are static; no service field/getter, new native item registration,
entity hook, tick/stop hook or private Vanity image is needed.

1. After mana construction, initialize the default once using
   `LbcChaosRewardItems.prepare(mana)`. Signature takes the existing `LbcMana`
   directly so it is safe before `game.items()` has been assigned.
2. In `LbcWands.cast`, before generic matching:
   `if(LbcChaosRewardItems.matches(stack))return LbcChaosRewardItems.use(game,player,hand);`
3. In accepted-positive native damage dispatch:
   `LbcChaosRewardItems.damaged(game,victim,source);`
   This method itself admits only actual direct `PLAYER_ATTACK` melee and main
   hand source whip. Never call it for a rejected/shielded damage attempt.
4. In `LbcAcademy.interact`, after the existing admitted/deduplicated actor
   checks and before unsupported `wand==null` return:
   `if(LbcChaosRewardItems.academy(game,player,hand))return true;`
5. Expose immediate native check `chaos-reward-items` calling
   `ChaosRewardItemChecks.run(game)`; no check tick hook.

The fixture uses actual native hand routes for both original raw rewards,
native Player.attack with a nearer first-word UUID collision decoy, absent full
UUID no-fallback, native retained velocity, caster/destination MAG,55 and54
real Sauvojen actors,53 existing external20tick queue, trial/Academy/mode and
insufficient-balance checks. It restores newly granted advancements, stats,
native RNGs and source default cost; existing shared fixture restores player,
scores, terrain/actors and configuration. These checks are written, **not run**.

## Boba Eblan rotation audit — blocker, not implemented

`lbc/function/items/misc/huynya/click.mcfunction` consumes one raw `boba_eblan`
then invokes a macro containing `rotatemap`. When source x/z scores are within
+/-16000 it runs at0,160,0; otherwise it runs at the executor. Source modes3..4
instead target other nonteam players and execute `clickbedwars.mcfunction`
(relative teleport with yaw+90/pitch-90, ponos, `govno_lot_2` reward).

The command is a **live registered Paper service**, not a missing mcfunction:
`plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java:165` calls
`RotateMapService.register`; that service registers `rotatemap`.
`RotateMapService.java` snapshots a320x320x320 cube (32,768,000 cells), Y0..319,
X/Z centered on command block coordinates, with native block entity payloads
through FAWE BaseBlock. It writes a +90-degree X-axis rotation:
`newY = 319 - relativeZ`, `newZ = zStart + oldY`; X remains unchanged.
It does not rotate block-state orientation, copy biomes, or copy entities as
part of terrain transfer. After successful changed terrain it rotates entities
aroundY160/Z(zStart+160), retaining yaw/pitch and vehicle/passenger relationships.
A global in-progress guard rejects concurrent requests. Nexus/imprinted exclude;
all online players receive the translated warning. Failed/zero-change terrain
does not execute the entity continuation, but the source item was already spent.

Current `MapManager` has ordinary template rotation/placement but no equivalent
whole-cube X-axis transform/native entity continuation owner. Implementing a
second map engine here would violate the requested shared ownership. Root must
provide a bounded shared terrain transformation path with real BE payloads,
native movement/attachment semantics, suppression/roles, causal mutation and
Vanity transaction coordination before this consumer can honestly be closed.

## Root integration and native evidence (31 October 2026)

Integrated with shared raw use, successful native damage and admitted Academy
promotion (including common token consumption/promotion criterion). Offline
runtime/check build passed. `chaos-reward-items` passed on `d05b18dc` and final
`0794b535`. Final fixture additionally exercises actual stronger artifact
suppression of the whip with both used hands; beacon/destination behavior and
native victim/UUID/momentum assertions remain. The caster MAG rule is the user's
confirmed answer. No destination gate was added to voluntary self-teleport.

See [batch evidence](DAMAGE_REACTIONS_AND_REWARD_CHAIN_2026_10_31.md) for archive
SHAs, failed receipts, preserved author state and current release limits.
The earlier worker-only unverified statements are historical handoff status.
