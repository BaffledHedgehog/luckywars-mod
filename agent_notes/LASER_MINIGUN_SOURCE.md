# Laser Minigun — source audit (2026-10-21)

Read-only audit; no implementation, build, server or resource-pack changes. Paths below are relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/` (DP), or current read-only RP `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/`.

## Item and reachable entry

- Ordinary shooter pool: DP `loot_table/transformables_shooter.json:381–480`: weight10, quality8, carrot_on_a_stick ×1; tooltip `lbc:tier_a`, model `lbc:laser_minigun/1`; tags `{transformable:1,shooter:1,magicwand:1,laser_minigun:1}`. Both hand attack-speed modifiers −1 add_multiplied_total, hidden attributes. Aqua nonitalic name `laser_minigun`; four lore lines: aqua `type`+`shooter`, aqua literal `4 `+`projectile_damage`, aqua literal `100 `+`mana_cost`, white `laser_minigun.lore`. Preserve components exactly.
- Pool reachable through DP `function/other/transform.mcfunction:9`, `reforge.mcfunction:8`, `reforge_crafting_0..3.mcfunction:8`.
- DP `function/items/spells.mcfunction:7,13` gates initial interaction through nomagic and optional offhand dispatch; `workingitems/spell1.mcfunction:6` selects default, `spell1_default.mcfunction:69` invokes laser click. No sneak/alternate/upgraded laser mode. `spell1_default_left.mcfunction` contains NO laser entry: actual legacy mainhand only.

## Spool, shot, spread, sound

All following under DP `function/other/techno/laser_minigun/`.

- `shoot/click.mcfunction:1–5`: play rev_up only if cd inactive; cd=6, initialize sound=1 if not revved, add revved, schedule next tick. No mana requirement to begin spinning.
- `shoot/run.mcfunction:1–10`: while cd>0 add spin1 and subtract cd1; if cd now0 subtract spin1. At spin≥30 run full_rev; cd0+revved runs rev_down; positive spin animates and schedules again. Sustained refreshed clicks reach spin30 in30 working ticks (1.5s). `shoot/tick.mcfunction:1–2` pauses within20 of stopper marker. Both selectors are sequential, so when first pass changes cd1→0 the second can run same player again in that tick (additional coast decrement).
- `shoot/full_rev.mcfunction:1–4`: spin clamped30, sound countdown decremented, spin sound every5 full-rev ticks; shoot once per full-rev tick if mana≥100. `shoot/cast.mcfunction:1–13` subtracts100 actual internal mana, decrements spread100 when≥1100, grants laser advancement at≤1000, plays shot volume2 pitch0, spawns short-lived raycaster marker at eye-relative left0.3/down0.2/forward0.5.
- Initial/reset spread12000 (`function/resets.mcfunction:37`); independent yaw/pitch random integer ±spread ×.001 degrees (`ray/cast.mcfunction:1–10`): first paid shot11.9°, floor1°, reached on110th paid shot. `shoot/rev_down.mcfunction:1–4` resets spread12000, removes revved, rev_down sound and stops rev_up within10. `function/other/purify.mcfunction:31` sets spread0, hence perfect aim until reset, advancement on next shot. Spin/cd/sound reset in `function/resets.mcfunction:121–123`.
- No held-item, mana-suppression, player-alive/spectator check in scheduled shoot tick; changing items during6t input grace can still fire, and model modifier targets current mainhand. No separate ordinary cooldown beyond input grace/spool.

## Ray, damage, display

- `ray/cast.mcfunction:12–21`: display model `lbc:laser_minigun_ray`, copies owner/team, sets scale0, synchronously samples then kills temporary marker.
- `ray/loop.mcfunction:1–2`, `check.mcfunction:1–4`:400 samples separated.25, first at origin, terminal sample99.75 blocks away. Any block outside #minecraft:airs stops (including fluid/transparent blocks). Block test precedes entity test but does not return early. Entity test is dx=dy=dz=0 sample-cell query, distance≤2, #minecraft:mobs, excludes spectator tag, owner ID/team. It then changes executor back to matching nonspectator owner player.
- `ray/hit_entity.mcfunction:1–4` performs another query (up to3 entities, with no type filter), damages nearest entity at each candidate's exact position for4 generic by owner, repeats owner/team exclusions, makes10 electric sparks and terminates. This is not a precise hitbox ray and can hit multiple/coincident entities; block-hit makes20 electric sparks (`hit.mcfunction:1–5`). No fire, terrain damage or explicit hurt-time bypass.
- Display scaleZ = sample count×.5 (`as_display.mcfunction:1`); beam model length.5 block, thus displayed length=count×.25 (one sample beyond tested terminal position). Owner marker anchors beam; done tag set by `tp.mcfunction:1–2`. Lifetime6 working ticks; shrink X/Y to timer×.04 and kill0 (`ray/visual/run.mcfunction:2–6`); stopper radius20 pauses visual age. Global selector/temporary-score state is legacy machinery, not a design requirement.
- Direct damage already uses owner via `damage ... by @s`; native port must additionally preserve indirect/environmental kill attribution through existing LbcIndirectKills, per user contract. No delayed damaging effect belongs to this item.

## Existing presentation assets

RP `assets/minecraft/lang/{ru_ru,en_us,rpr}.json:2196–2197,2290–2291`: existing item/lore/advancement keys. RU name «Лазерный пулемёт», lore «Как же он стреляет лазерами»; EN «Laser Machine Gun», «How does it shoot lasers?».
RP `assets/lbc/items/laser_minigun/{1..8}.json:4` → matching `models/item/laser_minigun/{1..8}.json`; texture `minecraft:item/lasermg` (model1:5–6); ray item/model uses `minecraft:item/laser_mg_ray` (ray model:5–6). RP `assets/minecraft/sounds.json:353–389` defines existing `lbcsounds.laser_mg_shot`, `_rev_up`, `_rev_down`, `_spin` with matching `sounds/lbcsounds/*.ogg`.
DP `item_modifier/rotate_laser_minigun.json:1–168` changes frames with ordered conditions 2→1,3→2,...,8→7,1→8; because functions are sequential, 2 can become1 then8 in same invocation. Verify observed intended frame order before reproducing this quirk.

## Questions requiring author decision

1. Enable both hands using shared native input/state, despite missing legacy offhand entry? Stop firing immediately on item switch, or preserve6t residual firing?
2. Preserve sampled cell/multi-entity hit quirks and nonair wall rule, or use existing native closest collision ray with4 damage? At block+entity same sample legacy can still damage.
3. Preserve perfect spread from Purify? Preserve sample400 beam endpoint +.25 visual offset and ordered animation frame skip?

Port direction: reuse LbcItems lifecycle, LbcMana spending, LbcWands projectileTarget/validTarget (friendly mobs hittable; owner, allied players, service NPC and spectators protected), LbcIndirectKills attribution and native Polymer model/display facilities. Keep only active per-player spool states; one candidate spatial query per shot, bounded native trace, no marker or per-sample world-wide selectors. Do not add new keys/assets for existing design. Audit alone does not establish native implementation or runtime verification.
