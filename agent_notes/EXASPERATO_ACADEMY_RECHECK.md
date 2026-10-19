# Exasperato Academy source recheck

Read-only cross-check of `EXASPERATO_ACADEMY_SOURCE.md` against the disabled
legacy LBC datapack. The old datapack is the direct behavior source.

## Verified corrections

- **Exasperato raycasters tick every normal tick, not every 2 ticks.**
  `tick_not_timestopped.mcfunction:3` dispatches every `slowraycast` raycaster
  through `raycastselector`; `raycastselector.mcfunction:7` dispatches `rare`
  tags and `raycastselector_rare.mcfunction:13-14` routes both Exasperato tags.
  The three mode-1 marker lifetimes remain data values 200/100/50, but advance
  by 1 per tick: **200/100/50 ticks**. `chtick.mcfunction:1` moves one block
  per invocation, so nominal speed is one block per tick. The 2-tick path is
  separate: `tick2t.mcfunction:2` selects `marker_2t`, and these Exasperato
  summons lack that tag.
- **Mode 2's 0.5-block samples are recursion within one function call.**
  `chtick_electro_ray.mcfunction:1-6` increments Distance, tests solid-block
  collision with a 20% kill chance, and recursively samples 0.5 forward for
  Distance 1..9 before ending at 10. `chtick_electro_ray_end.mcfunction:1-16`
  resets Distance and teleports `@s` to the current execution endpoint; it does
  not kill the marker. Since the summon lifetime is 200, it travels about 4.5
  blocks per tick and repeats the endpoint payload each tick until the outer
  selector expires it (up to 200 ticks). Each endpoint call summons 1 bolt and
  13 TNT: one at center (fuse 50 ticks) and four cardinal coordinates repeated
  at fuses 60/61/62 ticks. Thus each tick can add 13 TNT at 5 distinct
  positions plus one lightning bolt.
- **Chaos counts and scatter are literal.** The mode function has 80 creeper
  summon lines and 40 lightning summon lines, all at `^ ^ ^60`; it then runs
  each through `universal_pos_razb_35_active.mcfunction`, which independently
  adds random integers -35..35 to the integer-truncated X/Y/Z. This is position
  scatter in a 71-wide cube, not velocity. No owner/team data is assigned to
  these entities.
- **Nuke path details:** `click2_upgraded_mode_nuke.mcfunction:1-5` recursively
  advances by 0.7 up to Distance 400 (nominal 280 forward blocks), stopping on
  a non-air block or a non-spectator, non-caster mob within 15 of the sampled
  point. Spawn creates an unowned `falling_nuke` marker 500 blocks above and
  sets the caller Distance to 1000. `click2_upgraded_mode_nuke_rocket.mcfunction`
  moves it down 0.25 per recursive step, invokes shared
  `lbc:workingitems/exopersoexplode` on solid collision at Y 0..319, and calls
  `lbcr:raycast_end_with_tp` at Distance 5. The helper is present at
  `data/lbcr/function/raycast_end_with_tp.mcfunction:1-2`; it resets Distance
  and teleports `@s` to the current execution position without killing it.
  The four 0.25-block recursive steps therefore move the rocket about one block
  per server tick. No TTL is specified before collision. The rocket checks
  its five sample positions whose Y is 0..319 for solid blocks.
  `exopersoexplode.mcfunction:1-45` creates a 32-creeper grid
  (`ExplosionRadius:20`, fuse zero), a 200-tick particle marker, five sounds,
  extensive particles, and no TNT. Its final command is
  `kill @s[type=!minecraft:player]`: the caller's execution entity IS the
  falling rocket, because the function call does not change `as`. Therefore
  collision deletes the rocket. The earlier claim of repeated explosions on
  following ticks was incorrect; native migration should terminate on the
  first collision before creating its payload, following the shared terminal
  contract. Creepers receive no owner/team fields. Caster tagging and the delayed
  advancement check do not provide kill-credit inheritance.
- **Academy item-specific functions are message/modifier only.**
  `can_upgrade/exasperato_wand.mcfunction` contains only the condition message;
  `upgrade/exasperato_wand.mcfunction` sends a success message and applies
  `lbc:tiers/x`. Generic `interacted_upgrade.mcfunction` does the item transfer
  and writes `upgraded:1` / `mode:1`; `interacted_2.mcfunction` routes the
  selected wand into the item-specific condition function. The item-specific
  functions encode no Exasperato-specific test predicate. Plugin copy of the
  Academy advancement is in `plugins/luckywars/src/main/resources/datapack/data/lbc/advancement/true_advancements/legends/academy/exasperato.json`.

## Source gaps and questions to preserve

- Whether the mode-2 endpoint should repeat its full payload every tick for
  the marker's 200-tick lifetime (up to 2,600 TNT plus 200 bolts); within each
  payload, whether three fuse waves at each cardinal coordinate are intended.
  Chaos also creates 80 creepers plus 40 bolts at one exact pre-scatter point.
  Source proves these effects, not intent; retain these as clarification
  questions before changing them.
- What should represent the Academy's Exasperato-specific condition/success?
  The old functions only print messages while the generic path performs the
  upgrade. Do not infer a hidden objective from those messages.
- Nuke has no explicit TTL before collision; the shared payload DOES remove
  the caller rocket. The external movement helper exists. Questions based on
  either its absence or repeated explosions on subsequent ticks are withdrawn.
- Per the project contract, all delayed Exasperato effects need kill-credit
  inheritance. These legacy paths do not establish it: mode 1 copies `lbcID2`
  and `team_number` to its markers but TNT/lightning payloads receive neither;
  mode 2 likewise has marker IDs, then summons plain TNT/lightning; Chaos
  summons plain creepers/lightning; Nuke's falling marker copies no owner/team
  and shared Exoperso payload summons have no attribution fields. Treat this
  as a migration requirement/source gap, not intended behavior.

## Exact source anchors

- Legacy tick/scheduler: `data/lbc/function/tick_not_timestopped.mcfunction`,
  `tick2t.mcfunction`, `raycastselector.mcfunction`,
  `raycastselector_rare.mcfunction`, `markers_2t.mcfunction`.
- Modes: `data/lbc/function/workingitems/awaken_tuntija_wands/exasperato/`
  (`click2_upgraded_mode_default`, `chtick`, `chtick_explode_check_id`,
  `chtick_explode`, `click2_upgraded_mode_electro_ray`, `chtick_electro_ray`,
  `chtick_electro_ray_end`, `click2_upgraded_mode_chaos_area`,
  `click2_upgraded_mode_nuke`, `click2_upgraded_mode_nuke_spawn`,
  `click2_upgraded_mode_nuke_rocket`).
- Scatter and shared final payload:
  `data/lbc/function/universal_pos_razb_35_active.mcfunction`,
  `data/lbc/function/workingitems/exopersoexplode.mcfunction`.
- Nuke endpoint and delayed achievement:
  `data/lbcr/function/raycast_end_with_tp.mcfunction`,
  `data/lbc/function/workingitems/exopersoexplode_scheduled.mcfunction`.
- Academy: `data/lbc/function/other/magic_academy/interacted_2.mcfunction`,
  `interacted_upgrade.mcfunction`, `can_upgrade/exasperato_wand.mcfunction`,
  and `upgrade/exasperato_wand.mcfunction`.
