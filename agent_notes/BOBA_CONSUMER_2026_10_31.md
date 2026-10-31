# Boba raw consumer — staged implementation, 31 October 2026

This component is not released. Native root checks now pass; see the evidence below. Main installation,
restart and push remain gated on the user's entire Chaos chain requirement. This task
does not change source datapacks, resource packs, templates, commands or functions.

## Source and policy

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- `loot_table/boba_eblan.json`: carrot on a stick, `magicwand:1,boba_eblan:1`,
  existing tier_k/thing model, white nonitalic name and two white nonitalic lore lines.
- `function/items/spells.mcfunction` lines 7/13: owner must lack `nomagic_active`
  and no nonspectator `nomagic_active` player may be within 20. Offhand is configurable.
- `function/workingitems/spell1_default.mcfunction` line 103 and
  `spell1_default_left.mcfunction` line 87: Boba has **no local magic beacon check**.
  User clarification overrides this gap: the consumer now uses personal active tag
  plus shared full suppression (beacon and artifact), without a personal nomagic-score gate.
- `function/items/misc/huynya/click.mcfunction`: inventory `/clear` of one matching
  `boba_eblan` item occurs before mode selection and plugin admission. The clicked
  hand need not lose its item if an earlier inventory slot matches. Busy rotation
  therefore still consumes one token; the consumer reports handled input.
- Modes 1/2 invoke rotation at `(0,160,0)` when integer x/z are both within
  inclusive `[-16000,16000]`; otherwise they invoke at the caster. The source's
  `function/check_pos_changes.mcfunction` stores Pos x/z with scale **1**. Native
  selection uses current floored coordinates, without retaining stale shared scores
  that unrelated legacy rays also reused. This threshold is not 160 blocks.
- Modes 3/4 execute `clickbedwars.mcfunction`: other enemy players at distance
  at least 0.01 receive local-left one block, yaw+90, pitch -90, `other/ponos`
  then `govno_lot_2`. Existing shared target admission additionally preserves
  protected roles/Greed/allied policy. Both teleport wire flags and server momentum
  retain velocity. No map rotation/warning occurs on that branch.
- `function/other/ponos.mcfunction`: five independent uniform 1..11 sound draws,
  `govno_lot`, 1000 dirt block-crumble particles and 1000 sneeze particles. Native
  sneeze uses its valid simple particle type; the old textual sneeze block_state
  argument is not a native sneeze parameter. No equivalent complete ponos helper
  exists in LbcKfcFood, LbcLegacyEffects or LbcFartus; their effects are different.

Read-only plugin source:
`E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/RotateMapService.java`.
Its global busy guard precedes once-only online-player `map_rotate_warn`. The
new shared MapManager engine owns both warning and busy feedback, terrain/BE copy,
entity movement, lighting, queued work, lifecycle and checkpoint integration.
The consumer calls only `maps.rotateMap(owner, origin)` and does not duplicate them.

## Presentation

Read `agent_notes/RESOURCE_PACK.md` and the user's actual read-only pack
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/{ru_ru,en_us}.json`.
Existing Boba keys, lore and `map_rotate_warn` are present in both languages.
The raw consumer never rewrites item components, names, lore, model or tooltip.
No new presentation assets or translation keys are required.

## Integration and checks

New production file: `src/main/java/me/skywars2/game/LbcBoba.java`.
Create one stateless `LbcBoba(game)` in LbcItems, expose `boba()`, and route matching
raw native inputs to `boba().use(player,hand)` before generic wand handling.
There is no tick state, cleanup collection or checkpoint adapter to register.

`BobaChecks` supplies `token(int)`, `geometry()`,
`busyUses(GameController,ServerPlayer)` and `rejectedUses(GameController,ServerPlayer)`.
The caller must own exact inventory/player restoration and an accepted asynchronous
rotation while invoking `busyUses` in one callback. Both native hands are exercised;
checks cover exact one-token busy debit, no mana price, preserved caster momentum,
unchanged engine progress in the callback, inventory-order debit, personal active-tag
and disabled-offhand rejection. Geometry includes both sides of the integer threshold.

The root's asynchronous MapRotationChecks owns preload/capture/native raw accepted
use/polling/terrain+BE+entity verification/Vanity restore and check-suite registration.
No build or native child was launched by this consumer subtask. A helper assertion
is scaffolding, not a passing native receipt or proof of Bedwars gameplay support.

Follow-up scaffolding adds `BobaChecks.bedwars(game,owner)` for actual TestBeta,
both native hands in modes 3 and 4. It requires a caller-owned full Vanity image,
temporarily removes Beta's scoreboard team and restores its team/player image and
configuration in `finally`. It verifies real shared enemy admission, exact local
teleport/angles, both velocities, inventory-order debit, no map work, and 94 actual
native govno items (ten rolls of three plus one roll of 64) at the moved enemy.
The consumer also explicitly gates server freeze, dimension Time Stop and actor
freeze/tag before debit, including input-exempt Time Stop owners. This follow-up
has not been compiled or executed by the subtask.

## Root native evidence and clarified policy

The user confirmed both beacon and artifact block Boba, and requested rotating
pristine respawn islands with the map. The completed shared terrain engine and
actual native two-hand/Bedwars/BE/entity/rewind results are described in
[BOBA_ROTATION_AND_HAGALAZ_2026_10_31.md](BOBA_ROTATION_AND_HAGALAZ_2026_10_31.md).
Final b51226c7 passes the full-sized Boba fixture plus three neighboring groups.
The root retains both failed initial receipts and exact final input/preservation
proofs. Earlier worker-only no-run statements describe handoff, not current status.
Main install remains prohibited until the entire chain is complete.
