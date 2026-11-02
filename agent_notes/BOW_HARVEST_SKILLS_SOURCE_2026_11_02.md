# Remaining skills15/17 source audit — 2 November 2026

Read-only source audit; only this new note written. No code, build, legacy server,
actual RP or template changes. Root AGENTS.md latest verified contracts and
agent_notes/AGENTS.md consulted. Resolved14/16/18 deliberately excluded;19 is already
installed per root AGENTS.md. TMT remains LAST. This is source evidence, not runtime
qualification or a claim that either skill is implemented.

## Exact sources

Legacy root `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Below paths are relative to `lbc/` unless explicitly prefixed `minecraft/`.

- `function/players_tick.mcfunction:46` checks global `#lbcskill swrg.math = 1`,
  dispatches `swrg_kit_integration/game/skills/skills_tick.mcfunction` each player tick.
  That dispatcher returns failure when `legacy lbc.math = 1`; lines4/6 dispatch
  selected15/17 to `two`/`four`. No MAG, TECH, strong-artifact, cooldown, owner or
  enemy checks in these dispatches or either aura function.
- All clone helpers: `function/swrg_kit_integration/game/skills/`:
  `two`, `two_at`, `two_at1`, `four`, `four_p_at`, `four_p_at1`,
  `four_m_at`, `four_m_at1` (all `.mcfunction`).
- Type membership: `minecraft/tags/entity_type/projectiles_double.json`;
  raycasters get a separate explicit slowraycast-only branch.
- Distribution: `function/universal_pos_razb_2_.mcfunction`,
  `universal_pos_razb_2_motion.mcfunction`, `rand_motion_razbros.mcfunction`,
  `rand_motion_razbros_big.mcfunction`.

## 15 — Два

Every tick, spherical radius10 about holder: every not-inGround and not-`doubled`
member of projectiles_double except raycasters; separately slowraycast-tagged
raycasters meeting same conditions. Any owner, team or ownership absence is accepted.
This is a proximity cloning aura, not a bow-release skill.

`two_at` summons ONE same-type body with doubled/doubledcurr; temporarily adds those
tags to original, snapshots whole entity NBT into storage, removes temporary tags
from original, calls macro merge. Original then gets permanent doubled.
`two_at1` copies NBT and scores lbcID2/team_number/Distance/Lifetime to clone;
then clone alone gets position spread. Formula per coordinate is
`floor(originalPos*100) + integerUniform`, times .01; X/Z uniform[-200,200],
Y uniform[0,200]. No velocity change in this skill. Clone removes doubledcurr.
Native owner NBT is intended to stay original owner: skill holder does not become
shooter. Preserve indirect original cause through subsequent projectile consequences.

Membership is unusually broad: arrows, fireballs, eggs, pearls, falling blocks,
fireworks, fishing bobbers, spit, potions, bullets, snowballs, tridents, TNT,
crystals, markers/displays, fangs, XP bottle/orb, clouds, TNT/spawner carts,
boats/raft, wind charges. The summon table has chest boats but tag membership
does not, so they are unreachable through the shown native tag. Marker/display
custom spells only qualify through tag plus slowraycast rule.

Secondary source branch: `function/items/trinket_effects/intercal_hit.mcfunction:3`
changes Intercal's enemy radius10 Wither from10s amplifier0 to3s amplifier3 when
global skills enabled and holder skill15. This is independent of aura dispatch.
Caller `intercal.mcfunction` sets probability `(150+50*(count-1))*.001` and feeds
`func_with_chance_universal`; do not silently omit this skill synergy.

## 17 — Косил Косой Косой Косой

Same eligibility for projectiles but spherical radius16 and `quadried` once marker.
`four_p_at` summons THREE same-type copies (confirmed three arrow summon branches),
snapshots full NBT with quadried/quadriedcurr, merges copies and copies same four
scores. Original retained, so total4. Each copy gets same position distribution
as15, then per-axis velocity `floor(Motion*1000)+uniform[-1500,1500]`, times .001.
Original gets per-axis `floor(Motion*10)+uniform[-10,10]`, times .1.

Additionally radius16 EVERY nonplayer member of minecraft:mobs not quadried:
THREE same-type copies; original retained. No team, source, boss, shop,
strict_map_object, dead/living, summon ownership or equipment/loot exclusions.
`four_m_at1` copies full NBT and scores lbcID2/team_number/Distance/Lifetime/
cyborgBossFly/delayed_death/lookback/shock/scared/piss/target. If lucky effects tag
exists, copies receive have_lucky_effects and its5t/1s tags. Explicitly joins
copies to original vanilla numbered team1..32. Both original and all three copies
receive small velocity spread formula above, no mob position spread. Removes
quadriedcurr from copies after operation. Attributes, HP, inventory, gear, tags,
AI and loot are implicitly full-NBT copies, not regenerated vanilla defaults.

## Bugs/decisions requiring author clarification

1. Confirm15/17 entirely unjammed as source, including strong artifact; modern
   shared admission and TimeStop lifecycle remain necessary independently.
2. Confirm broad ALL-projectile aura (enemy/self/allied/unowned; carts, boats,
   TNT, displays, XP, clouds, etc.) and original shooter/cause preservation.
3. Confirm17 clone scope: bosses, damaged/dying mobs, equipment/inventory/loot,
   tamed and summoned actors, and actor-owned custom state. Legacy does not
   exclude protected shop/academy/map actors; current protected-role contract
   should be applied rather than duplicating authority-bearing map entities.
4. Should15 and17 compose? Independent permanent tags permit two-by-four =8
   total bodies once both auras encounter a source family; copied tags prevent
   unbounded same-skill recursion. No per-holder reset in source.
5. Fix17 temporary tag typo: universal_pos_razb_2_motion removes doubledcurr,
   although caller uses quadriedcurr; four_p_at1 never clears quadriedcurr.
   Same-position later operations can select stale copies (limit3, no sort),
   potentially merging unrelated NBT. Preserve intended once count, not leak.
6. Raw full-NBT merging includes UUID/Passengers/relationships; a native port
   needs fresh body identities and deliberate link remapping, not blindly
   cloning logical entity authority. Which custom virtual spell/actor families
   should clone their active state rather than just visible native carriers?
7. Keep15 Intercal WitherIV synergy or source copy/paste bug? Its explicit
   skill15 condition is suspicious enough to ask; no evidence author removed it.

## Menu and actual resource pack

`swrg_kit_integration/gui/{page,select}/10000.mcfunction`:15 slot14 unlock
`lbc:true_advancements/bows/double`;17 slot16 unlock
`lbc:true_advancements/hoes/sickle`. Ranked counterparts bypass advancements.
Do not confuse one-based skill IDs and zero-based translation indices.

`loot_table/gui/page/10000/14.json`: arrow, item_model lbc:combo_arrow,
name lbcskill.14 white italicfalse; one gray italicfalse lore.lbcskill.14 plus
two original statistics rows with index14. `/16.json`: snowball, lbc:kosoi,
name lbcskill.16; two gray italicfalse lore lines .16.0/.16.1 then stat rows16.

Actual RP read-only path from `agent_notes/RESOURCE_PACK.md`:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Verified `assets/minecraft/lang/ru_ru.json` currently contains:

- lbcskill.14: `Два`
- lore.lbcskill.14: `> Все снаряды вокруг игрока удваиваются`
- lbcskill.16: `Косил Косой Косой Косой`
- lore.lbcskill.16.0: `> Все снаряды вокруг игрока делятся на 4 с большим разбросом`
- lore.lbcskill.16.1: `> Все мобы делятся (не более 1 раза) на 4 при приближении игрока`

No new translation keys needed for existing design.

## Likely native reuse and complexity

`LbcLoadouts.hasSkill` already supplies shared role/unlock/match admission, with
no jammer lookup. Use it; exposing entries alone does not implement these auras.
No doubled/quadried service was found in current Java. `LbcSpellSource` owns
causal source propagation; `MatchActorCheckpoint`/existing native entity rewind
are relevant to body identity and active actor replay. Modern managed virtual
spells need their owning service's explicit cloning support; native NBT alone
cannot reproduce Java queues, links or per-owner registries. `LbcDuplicating`
is terrain duplication, not a ready projectile/mob clone helper.15 moderate/high;
17 high, primarily because arbitrary actor state and once-only identity matter.
Use resident spatial candidates, sphere checks and bounded/deferred cloning,
not world scans or per-tick NBT snapshots of every entity.

## 19–24 identification only

Original slots19..24; actual RP has same-number names (there is a slot18 gap).
19 Паразит already installed, do not reopen.20 Симбионт: mob possession/minion
binding and damage shield transfer, high;21 Планетоид: persistent gravity/orbit
mob/projectile/ray state, high;22 Благословение Зевса:50% one-second pulse,
nearest eligible radius6 lightning ray, medium;23 Ладонь Бога:60-sneak-tick
activation, active/end/cooldown state, high;24 expansion: parallel60-tick
sneak/active/end machinery, high. Source dispatch names:
simbiont_tick/planetoid_tick/boltbolt_zeus/god_claw_tick/expansion_tick.
24 display name not established here: actual RP inspected has no lbcskill.24;
confirm through menu loot's exact key before claiming its name. No detailed
implementation or author decision audit performed for20–24.
