# Terra Sword source audit

Scope: read-only inspection of the old `world/datapacksdisabled/LBC` datapack, with targeted plugin/mod searches. This records source behavior; it does not establish that this disabled datapack was loaded or that each path was runtime-tested. No Java was changed.

User target for the new implementation: regular projectile model `lbc:terrasword_charge`; improved sword right-click projectile model `lbc:terrasword_true_charge`; improved sword melee arc model `lbc:terrasword_true_beam`. Keep both swords' projectiles at limited range with native swept hitboxes. Reuse `LbcTerra`; do not add Creative/developer exemptions. These are user requirements, not claims about old source.

## Exact dispatch paths

- **Ordinary hit / charge:** `lbc:gowno` advancement (`player_hurt_entity`) → reward `lbc:items/swords` → `workingitems/terrasword`. The held item's `terrasword:1` branch increments `swordcharged2`; if its current value is 0–3, next value is 1–4. `swordcharging2` is incremented and immediately reset to 0. It has no timed cooldown/passive recharge. Sources: `data/lbc/advancement/gowno.json`; `data/lbc/function/items/swords.mcfunction`; `data/lbc/function/workingitems/terrasword.mcfunction`.
- **Ordinary right-click:** `RC_Raycast` is objective `minecraft.used:minecraft.carrot_on_a_stick` → `players_tick` → `items/spells` → selected `raycastspell` calls `workingitems/spell` → `lbcr:raycast` checks selected `terrasword` and `swordcharged2>=1` → `raycast_preloop13` creates regular Terra projectile and subtracts 1 charge. Sources: `load.mcfunction`, `players_tick.mcfunction`, `items/spells.mcfunction`, `workingitems/spell.mcfunction`, `lbcr/function/raycast.mcfunction`, `lbcr/function/raycast_preloop13.mcfunction`.
- **Improved sword melee hit / arc:** same `gowno` → `items/swords` checks `true_terra:1` → `workingitems/sauvojen_staffs/true_terra_sword_hit` creates `terra_ch` marker. It has no mana/cooldown check on this melee branch. `gowno` is revoked at end of `items/swords`, allowing a subsequent hit to trigger again.
- **Improved sword right-click / charge projectile:** `RC_Raycast` → `players_tick` → `items/spells` → `workingitems/spell1` → `spell1_default` for main hand or `spell1_default_left` for offhand. `magicwand:1` dispatch tests `true_terra:1`, mana ≥ `true_terra_mana_use` (default 1000), and absence of `stopper_magic` within 20. It calls `awaken_tuntija_wands/terra/click`, which debits 1000 then calls `click2`; `click2` creates `fast_terra`. This is distinct from Academy `stterra` (unrelated Sauvojen staff). Sources: `items/spells.mcfunction`, `workingitems/spell1.mcfunction`, `spell1_default.mcfunction`, `spell1_default_left.mcfunction`, `awaken_tuntija_wands/terra/{click,click2}.mcfunction`, `function/config.mcfunction`.
- **Flight dispatch:** regular Terra marker is sent by `raycastselector` → `raycastselector_player` → `lbcr:raycast_terra`. `fast_terra` is sent by `raycastselector` → `raycastselector_rare` → `lbcr:raycast_terra_fast`. `terra_ch` is sent by `raycastselector_rare` → `terra/tick_awaken` → `tick_awaken_loop`. The separate wandering Sauvojen wand entity has its own `flying_wand_tick_terra_upgraded` cycle (math1=2 calls Terra `click2`; math1=4 calls `true_terra_sword_hit`); this is not the sword's melee/right-click dispatch.

## Projectile behavior and numbers

- **Ordinary charge projectile (`raycastterra`):** starts at eye position; TTL 40, advances 0.3 blocks per recursive step, Distance counter stops at 7; its last damage sample is at +1.8 blocks from launch (7 invocations, not a 7-step added distance), with green transition dust. At a non-air block it places grass features and kills itself. Damage helper `dmg_bypl_8` applies 8 cactus damage to up to two entities per step. Its selector uses a 1×1×1 block sample cube (`dx=dy=dz=0`); `distance=..10` is an additional origin-distance filter, not a 10-block-wide hit volume. `dmgwithid_un_playeronly` protects caster ID/team via temporary `nodamage` tag; target is not recorded as already hit, so repeated checks can select the same entity. Sources: `lbcr/function/{raycast_preloop13,raycast_terra,dmg_bypl_8,dmgwithid_un_playeronly,placegrass}.mcfunction`; dispatcher `lbc/function/raycastselector_player.mcfunction`.
- **Improved right-click charge projectile (`fast_terra`):** TTL 20; recursive ray advances 0.3 blocks; Distance 26 ends the ray at +7.5 blocks (N=1 samples launch position; N=26 samples +7.5); each step uses `dmg_bypl_11` for 17 cactus damage. At a non-air block it places flower features; particles are pale lime. `damagewithid4_1` marks caster ID and team as excluded, but no per-projectile hit memory. Sources: `workingitems/awaken_tuntija_wands/terra/click2.mcfunction`; `lbcr/function/{raycast_terra_fast,dmg_bypl_11,damagewithid4_1,placeflowers}.mcfunction`.
- **Improved melee arc (`terra_ch`):** TTL 50; each `tick_awaken_loop` pass adds one Distance, then runs three damage samples per segment (center and two ±0.6 lateral offsets), advances 0.33 blocks per segment, and recurses while Distance ≤3. `tick_awaken` resets Distance to 0 on every server tick. Exact trace from marker position P at tick entry: N1 damages/renders at P; N2 at P+0.33; N3 at P+0.66; recursive N4 context is P+0.99. N4 Distance=4 teleports the marker to context +0.33 = P+1.32, then returns. The unwind checks blocks at prior contexts P+0.66, P+0.33, P. On the next tick Distance resets and the sequence repeats from P+1.32. At each of N1, N2, and N3, three lateral damage checks run (center, +0.6, −0.6: nine helper invocations total per tick); particles render along those segments. It advances exactly 1.32 blocks per tick until TTL 50 (nominal 66 blocks), or a block at one of the three unwind/sample positions kills it; N4 teleports without checking its P+0.99 or destination P+1.32 context, leaving a collision-sampling gap. It renders several pale-green dust particles. Any non-air block kills the marker; this arc does not place flowers. Its `damagewithid_9` applies 5 arrow damage to up to three mobs per sample selector and protects caster ID/team using temporary tags. Each sample selector is `dx=0.3,dy=0,dz=0.3`, a 1.3×1×1.3 block volume; the three centers are offset laterally (0, +0.6, −0.6), so these are overlapping/broad sample boxes, not thin ray tests. No hit-once tracking exists across samples or ticks. Sources: `true_terra_sword_hit.mcfunction`; `terra/tick_awaken_loop.mcfunction`; `lbcr/function/damagewithid_9.mcfunction`.

## Source predicates, resolved user contract, and oddities

**Target behavior confirmed by user (user):**

> Сохранить зарядку от любого нестрелового урона
> Оставить связку: попадание заряда тоже выпускает дугу

Current port contract: `commonLbcItems.damaged` calls `LbcTerra.damaged(owner)` after non-arrow, active, arena, and magic-suppression guards. That hook handles the basic charge with cap 4 and the improved `truebeam` behavior for held main hand. The two user statements above resolve the previous behavior questions; old datapack details below remain historical source behavior. Selected server smoke verification has passed: `smoke_terra.py --swords` exited 0 with PASS for `terra-sword-menu`, `terra-sword`, and `terra-sword-cleanup`. The report [terra-swords-results.json](../build/verification/terra-swords-results.json) records the completed menu/loot/Polymer restoration, native use/melee and arrow damage/swept-hit/suppression, and match-stop projectile/display cleanup checks; artifact SHA-256 `1ce3fa37a598543bd5a49ecc004a3cf6076e1b42a2f78a4fd8ad5e82bce0a271`, elapsed 47.21 seconds. This records only the selected Terra smoke checks; root is running wand regressions.

- `gowno.json` has `blocked:false`, rejects damage sources tagged `minecraft:arrow_only`, excludes interaction-entity victims, and excludes nexus/imprinted dimensions. It has **no `cactus_only` filter**: its exact damage gate accepts any non-arrow damage that satisfies those other conditions, not specifically melee. Cactus damage is credited to the actual player: `dmgwithid_un_playeronly` tags the matching player `nodamage` (and only summons a marker fallback if no tagged player exists), then `dmg_bypl_8` / `dmg_bypl_11` damage `by @e[type=#minecraft:player_marker,tag=nodamage]`; that entity-type tag contains both `player` and `marker`. Therefore, when the attacker holds Terra, ordinary and improved charge-projectile cactus hits can satisfy `gowno` and re-enter `items/swords`: ordinary hits add `swordcharged2` (up to 4), while `true_terra` hits create another `terra_ch`. The criterion does not distinguish melee from these cactus hits. `terra_ch` arrow damage is excluded by `arrow_only expected:false`. `gowno_any_damage` is a separate reward to `items/swords_or_bow`; do not conflate it with the exact branch above.
- `items/swords.mcfunction` Terra branch redundantly checks nearby `nomagic_active` twice. Both Terra sword branches also require no `stopper_magic` marker within 20 blocks; regular branch's duplicate check and true Terra branch's check have no charge/cooldown cost.
- Ordinary `raycast_terra` tests a 1×1×1 block selector cube (`dx=dy=dz=0`) at each 0.3-block step, with an additional `distance=..10` filter from the ray origin. The distance limit does not widen the cube; adjacent samples can still reselect the same target. A 40-tick TTL is only a failsafe, not a 40-block range.
- `terra_ch` resets Distance each tick; exact motion is +1.32 blocks/tick with damage/particles at offsets 0, +0.33, +0.66 (and block checks as recursion unwinds), up to 66 blocks for TTL 50 (1.32 blocks × 50 ticks). Nine damage-helper invocations execute each tick (three 1.3×1×1.3 selector boxes at each of three forward samples; lateral offsets 0, +0.6, −0.6 overlap); a single entity can fall in multiple sample volumes. Block checks skip the N4 and destination positions, then resume next tick at +1.32. `fast_terra` and `terra_ch` are meaningfully different projectiles (17 cactus/flower placement vs 5 arrow/multi-sample arc); the proposed `true_charge` and `true_beam` models map to those two paths respectively.
- The old implementation is marker stepping and particles, not a native swept projectile; no old source model references for the user's three new model IDs were found. Native swept hitboxes and limited range remain target behavior for the port.
- Focused plugin/mod text search found Terra translations and bundled datapack copies in `plugins/luckywars`, but no separate Terra Java implementation and no Terra implementation in `mods/luckywarsignite`. No runtime claim made.

## Main old-source paths

Root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/`.

- `data/lbc/advancement/gowno.json`
- `data/lbc/function/{players_tick,items/spells,items/swords,workingitems/spell,workingitems/spell1,workingitems/spell1_default,workingitems/spell1_default_left}.mcfunction`
- `data/lbc/function/workingitems/{terrasword,sauvojen_staffs/true_terra_sword_hit,awaken_tuntija_wands/terra/click,awaken_tuntija_wands/terra/click2}.mcfunction`
- `data/lbc/function/workingitems/awaken_tuntija_wands/terra/{tick_awaken,tick_awaken_loop}.mcfunction`
- `data/lbc/function/raycastselector_{player,rare}.mcfunction`
- `data/lbcr/function/{raycast,raycast_preloop13,raycast_terra,raycast_terra_fast,dmg_bypl_8,dmg_bypl_11,damagewithid4_1,damagewithid_9,damagewithid_un_playeronly,placegrass,placeflowers}.mcfunction`












