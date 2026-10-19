# Air skill, victory fireworks, and advancement audit

## Airborne sprint skill (source skill 12)

The old datapack's `game/player/skills/tick.mcfunction` calls `wind_rider` only
when the player has skill 12, the sprint predicate is true, and the on-ground
predicate is false. `wind_rider.mcfunction` applies the local motion command
`motion add ^ ^-0.01 ^.1 @s` after forcing pitch to zero, then emits two cosmetic
particles and a wool-step sound. The Fabric port already had the matching
sprint/airborne condition and yaw-relative horizontal impulse; it now states the
source vector directly in `MatchEvents.tickSkill` (0.1 forward, -0.01 Y per
server tick). The server tick hook runs at `END_SERVER_TICK`; the impulse is
therefore added after player movement and is explicitly sent to that player's
client with `ClientboundSetEntityMotionPacket`, since vanilla `Entity.setDeltaMovement`
only stores the server vector. The source GUST/SOUL particles and wool-step
sound (volume 0.5, pitch 0.8) are also emitted through native APIs.

Sources: [old tick dispatcher](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction>),
[wind rider](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/wind_rider.mcfunction>),
[sprint predicate](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/sprint.json>),
[airborne predicate](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/on_ground.json>),
[Fabric MatchEvents](../src/main/java/me/skywars2/game/MatchEvents.java).

## Victory fireworks

Old `game/end/fireworks.mcfunction` immediately creates four markers at the
winner's position and runs `spreadplayers 0 0 20 192 false` every second while
`#gamestat` is 5 or 6. The command's center is world origin `(0, 0)`, its minimum
distance is between marker positions, maximum range is 192 blocks, and `false`
means team separation is disabled; marker Y is placed at ground height. It summons
one rocket at each marker. Each rocket starts with two no-trail explosions; the
randomizer gives each a separate base RGB color and gives both a shared fade
color, shape (`small_ball`, `large_ball`, `burst`, `creeper`, or `star`), and
twinkle value. `LifeTime` starts at 60 and is overwritten by an inclusive random
score in 10-40.

`MatchEvents.victoryTick(ServerPlayer winner, int endingTicksRemaining)` creates
four owned marker entities once, reuses `spreadplayers` on each ending second, and
spawns four rockets at their resulting ground positions. The port uses the same
world-origin layout and pairwise minimum separation; rocket NBT preserves the two
explosions and inclusive lifetime range 10-40. `PlayerStore.Profile.winEffect != 0`
suppresses this default generator. Alternate win effects remain pending, and the
old scoreboard selector is not proven to map one-to-one to the profile field.

The old selector returns early for winner effects 1 (`pumpkins`) and 2
(`golden_swo`); only its default branch calls fireworks. Those effect engines are
not yet implemented in Fabric.

Exact source NBT: `LifeTime:60` initially, then overwritten to 10?40;
`FireworksItem:{id:"minecraft:firework_rocket",components:{"minecraft:fireworks":{explosions:[{shape:"small_ball",has_trail:0b,has_twinkle:0b,colors:[I;0]},{shape:"small_ball",has_trail:0b,has_twinkle:1b,colors:[I;0]}]}},count:1}`.

Sources: [firework scheduler/spawn NBT](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/fireworks.mcfunction>),
[random rocket fields](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/firework_random.mcfunction>),
[winner effect selector](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/win_effects/select.mcfunction>),
[pumpkin effect](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/win_effects/pumpkins.mcfunction>),
[golden sword effect](<../../../cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/win_effects/golden_swo.mcfunction>),
[Fabric MatchEvents](../src/main/java/me/skywars2/game/MatchEvents.java).

## Vanilla advancement suppression and imported LBC criteria

Do not solve this by disabling all advancement triggers or by replacing the
advancement manager with an empty tree: the intended LBC namespace is
`lbc:true_advancements/**`, and its definitions/progress must remain available
for the advancement UI and `LbcLoadouts` gates. The current source contains 357
JSON files under that directory. Before conversion, 122 had a legacy
`lbc:swrg_kit_integration/gain/*` function reward; those functions are not
present under current mod resources and are unsafe to load without their old
integration. The reviewed [importer](../tools/import_lbc_advancements.py) removes
only those audited function rewards, normalizes two unqualified `impossible`
triggers, and converts legacy condition JSON to the native 26.3 codec shape while
preserving its criteria meaning. It preserves display, icon, parent, and any
non-function rewards. The source set currently has no XP or loot advancement
rewards. Existing explicit LBC callbacks (for example the effect-skill issue in
`LbcItems`) remain separate and unchanged.

For native 26.3 codecs, the importer converts legacy `player`/`entity` arrays of
loot predicates into one condition (a singleton is unwrapped; multiple entries
become `minecraft:all_of` terms), recursively renames loot-predicate discriminator
`condition` to `type`, converts flat entity-predicate keys to the 26.3
`EntityPredicate.CODEC` map (`type` becomes `minecraft:entity_type`, other predicate
keys become their registered `minecraft:` sub-predicate IDs), wraps raw legacy
player entity predicates as `minecraft:entity_properties`, and prefixes the sole
damage-tag holder-set reference with `#`. These preserve source conjunctions and
gamemode guards rather than weakening/removing criteria. The conversion follows
local 26.3 sources:
`InventoryChangeTrigger.TriggerInstance`/`PlayerTrigger.TriggerInstance` use
`Optional<Holder<LootItemCondition>>`, `AllOfCondition` evaluates all `terms`,
`EntityPredicate.CODEC` dispatches sub-predicates through `entity_sub_predicate_type`,
`EntitySubPredicates.bootstrap` registers the supported IDs, and `TagPredicate.codec`
decodes `id` as a registry holder set.

The old gain functions only displayed unlock-subtitle text; native advancement
completion/progress is the unlock record now. For any future UI compatibility,
the old ID→gain mapping is grouped below; every suffix is under
`lbc:true_advancements/`:

- `challenge` / `new_challenge` (1): `legends/academy/root`.
- `death_effect` / `new_death_effect` (1): `event/pumpkin_sword`.
- `default_kill_effect` / `new_kill_effect` (2):
  `event/soulcorrupt_wand_upgraded`, `event/soulhunter_mace`.
- `default_kit` / `new_swrg_kit` (3): `event/eat_halloween_food`,
  `event/halloween_mace`, `event/wind_wand_upgraded`.
- `default_skill` / `new_swrg_skill` (2): `event/soulcorrupt_wand`,
  `event/wind_wand`.
- `kit` / `new_kit` (25): `accesories/devilstomach`, `accesories/popit`,
  `accesories/sand_amulet`, `arcanums/barrier_and_heal`,
  `arcanums/kiuaskivi`, `armor/chaos_full`, `armor/moisei_boots`,
  `armor/multiplying`, `armor/nano`, `armor/phantom`, `armor/shitty`,
  `armor/steampunk`, `armor/technomk2`, `axes/randchanted`, `bows/firework`,
  `hoes/op_hoe`, `legends/academy/acid_tuntija`,
  `legends/academy/badwiki`, `legends/academy/blazefire`,
  `legends/academy/confuse`, `legends/academy/erecto`,
  `legends/academy/exasperato`, `legends/academy/fartus_nadristailo`,
  `legends/academy/lightning`, `legends/academy/ruby`.
- `levelup` / `new_levelup` (29): `accesories/infinite_quiver`,
  `accesories/mana_belt`, `accesories/regeneration_belt`,
  `accesories/save_platform`, `arcanums/babylon_gate`, `arcanums/barrier`,
  `arcanums/black_hole`, `arcanums/black_hole_2`, `arcanums/black_hole_max`,
  `arcanums/heal`, `arcanums/neutral`, `armor/antimagic`, `armor/assasin`,
  `armor/berserker`, `armor/chaos`, `armor/flesh`, `armor/glass`, `armor/mage`,
  `armor/shulker`, `armor/techno`, `armor/tnt_jacket`, `armor/traveller`,
  `axes/poleaxe`, `axes/tomahawk`, `bows/railgun`, `bows/railshish`,
  `bows/randchanted`, `bows/shulker`, `legends/academy/blood`.
- `skill` / `new_skill` (32): `accesories/jaakivi`, `accesories/magnet`,
  `accesories/mystical_artifact`, `arcanums/tannerkivi`, `armor/erosion`,
  `armor/nyauwu`, `armor/paladin`, `armor/randchanted`, `armor/shitty_full`,
  `armor/shulker_full`, `armor/techno_full`, `axes/banhammer`, `axes/hammer`,
  `bows/double`, `bows/madoka_bow`, `bows/minigun`, `hoes/death_skytche`,
  `hoes/sickle`, `hoes/socializm`, `legends/academy/acidacid`,
  `legends/academy/acidcorrupt`, `legends/academy/big_stone`,
  `legends/academy/boltbolt`, `legends/academy/consuming`,
  `legends/academy/danmaku`, `legends/academy/dublicating`,
  `legends/academy/exahoming`, `legends/academy/fire`,
  `legends/academy/gaycaster`, `legends/academy/omega_flag`,
  `legends/academy/psychoblind`, `legends/academy/stone`.
- `skill_acidman` / conditional `new_skill` (5):
  `legends/tuntija_wands/acid`, `legends/tuntija_wands/acidacid`,
  `legends/tuntija_wands/acidcorrupt`, `staffs/acid`, `staffs/corruption`.
- `trinket` / `new_trinket` (22): `bows/blaze`, `bows/ghast`,
  `bows/penis_gun`, `legends/academy/acid`, `legends/academy/air`,
  `legends/academy/bass`, `legends/academy/bedrock`, `legends/academy/blaze`,
  `legends/academy/blind`, `legends/academy/corruption`,
  `legends/academy/electro`, `legends/academy/ender_random`,
  `legends/academy/evoker`, `legends/academy/exoperso`,
  `legends/academy/falling_stars`, `legends/academy/hedgehog`,
  `legends/academy/ice_wand`, `legends/academy/neutral`,
  `legends/academy/polymorph`, `legends/academy/prism`,
  `legends/academy/psycho`, `legends/academy/snow_wand`.

The old source functions are in
[`LBC/data/lbc/function/swrg_kit_integration/gain`](<../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain>).
Their commands confirm they only show the mapped subtitle; `skill_acidman`
conditionally delegates to `skill`. The current manager's native award callback
can persist the completed full advancement ID directly without restoring those
function rewards.

Schema audit after import: 357 files, 787 criteria, zero external parents;
trigger distribution is inventory-changed 468, impossible 235, player-killed-entity
50, player-hurt-entity 14, effects-changed 8, tick 2, location 2, consume-item
2, and six one-count vanilla triggers. Criterion-condition nodes are only
`minecraft:inverted`, `minecraft:entity_properties`, `minecraft:all_of`, and
`minecraft:any_of`; no external predicate resource IDs occur. The old inline
`entity_properties.predicate.nbt` checks and item predicates retain their source
conditions; structural discriminators are converted for the 26.3 codec.

Implemented server-side boundary: a mixin on
`net.minecraft.server.ServerAdvancementManager` preserves manager entries only
whose IDs have namespace `lbc` and path prefix `true_advancements/`; remove all
other IDs from both its holder lookup and `AdvancementTree` before player
advancement listeners are initialized. Filtering only the client tree hides
tabs but leaves vanilla trigger registration and reward granting active.
The manager retains the full native criteria. Audited notification-only function
rewards are removed pending their separate text implementation. The tree is rebuilt
from all retained holders followed by `repositionNodes()`; this avoids accidentally
removing retained descendants when a removed parent has children. All 357 LBC
JSON files in the current directory have parents under
`lbc:true_advancements/`. On
reload, retain the same filter and let `PlayerAdvancements.reload` rebuild
listeners against that filtered manager; do not wipe the player's LBC
`AdvancementProgress`.

Exact local 26.3 Mojang-mapped APIs checked in the cached deobfuscated jar:

- `net.minecraft.server.ServerAdvancementManager`: private
  `Map<Identifier, AdvancementHolder> advancements`; `tree()`;
  `getAllAdvancements()`; `get(Identifier)`.
- `net.minecraft.advancements.AdvancementHolder`: `id()` identifies a holder.
- `net.minecraft.advancements.AdvancementTree`: `remove(Set<Identifier>)`,
  `clear()`, `addAll(Iterable<AdvancementHolder>)`, `repositionNodes()`,
  `roots()`, `tasks()`, `nodes()`.
  `remove` recursively removes descendants, so only IDs in the discarded
  vanilla subtree may be passed.
- `net.minecraft.server.PlayerAdvancements`: `reload(ServerAdvancementManager)`,
  `award(AdvancementHolder, String)`, `revoke(...)`,
  `getOrStartProgress(AdvancementHolder)`, and
  `flushDirty(ServerPlayer, boolean)`.

`AdvancementFilterMixin` filters the manager map at constructor tail to `lbc:true_advancements` and rebuilds the advancement tree. The importer converts the 357 source JSONs to the native 26.3 criterion codec shape, preserving the criteria while removing only audited legacy function rewards. Successful 26.3 startup and a runtime assertion confirmed exactly 357 retained LBC advancements; vanilla definitions are absent from the manager. This does not verify every criterion or unported gameplay engine. See [SERVER_TESTS.md](../SERVER_TESTS.md).

Relevant mod sources: [advancement filter](../src/main/java/me/skywars2/mixin/AdvancementFilterMixin.java),
[advancement Mixin list](../src/main/resources/skywars2.mixins.json),
[26.3 importer](../tools/import_lbc_advancements.py),
[LBC loadout gates](../src/main/java/me/skywars2/game/LbcLoadouts.java),
[LBC item trigger callback](../src/main/java/me/skywars2/game/LbcItems.java),
[sample LBC advancement with integration reward](../src/main/resources/data/lbc/advancement/true_advancements/axes/banhammer.json).

## Observer implementation and safety audit

`Spectators.tick` iterates only registered viewers; it does not scan worlds. Player targets still incur a 41-slot inventory comparison each tick, but target GUI slots and the player's inventory menu are refreshed only when that comparison finds changes. Health, absorption, fire, freeze, air, food, and XP writes are now change-gated. Effect HUD copies use cached snapshots and clientbound packets, not active server effects. Visibility and invulnerability flags are still set per viewer each tick. Target-list construction snapshots `alivePlayers()` only when its GUI opens. These are code-path observations, not measured CPU claims.

Effect copies are now sent only as clientbound update/remove packets for the observer and camera entity; copied effects are not installed on the observer server-side, so their natural potion ticks and callbacks cannot run there. Cached snapshots avoid resending unchanged effects. The `Player.isSpectator` override affects vanilla paths that consult it, but selectors such as `@a` may still include observers; mod mechanics must check `game.isObserver` explicitly. `ObserverPacketMixin` handles `ServerboundInteractPacket` before vanilla spectator interaction checks, opens the requested watch target when valid, and cancels the interaction. Adventure inventory opening is restricted by vanilla, so `/sw inventory` remains the explicit read-only GUI fallback.

Air skill source and port vectors match: source `motion add ^ ^-0.01 ^.1` runs after setting pitch to zero, yielding yaw-relative forward 0.1 and Y -0.01 each qualifying tick. The added velocity packet is needed to sync that server-side delta to the client. Old fireworks schedule four rockets immediately and then four per second. Four persistent markers are repeatedly spread from origin with pairwise minimum distance 20 and maximum range 192; this is not a radial ring around the winner. The port now uses native `spreadplayers` and spawns at the resulting ground positions.
