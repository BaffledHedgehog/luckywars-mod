# Unstable Prism - native port contract

## Ready entrances

- `lbc:ported/unstable_prism`
- `lbc:ported/academy_unstable_prism`

Original model/name/lore/tier/flags come from the `prisma:1` entry of
`data/lbc/loot_table/transformables_staff.json`. Academy appends the original
upgraded line and tierSS. Existing author resource-pack model and translations
were checked read-only; this batch adds no RP assets or alternate translation keys.
The Prism + Omega -> Gaycaster recipe/reward family remains a separate unopened
port; source craft lore is retained and this batch does not expose that recipe.

## Source

Read-only root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- Ordinary: `lbcr/function/raycast_preloop7*`, `raycast_loop7*`,
  `dmg_bypl_8*` and `lbc/function/config.mcfunction` (1200 mana).
- Academy: `lbcr/function/raycast_preloop7_2_upgraded*`,
  `raycast_preloop7_upgraded` and original HSV helpers.
- Progress/tier: `lbc/function/other/magic_academy/{can_upgrade,upgrade}/prisma`.

Ordinary native projectile samples9 points per tick, quarter-block spacing,
advancing2 blocks, with source150 lifetime. It retains8 cactus damage, up to2
appropriate targets per sample, independent15% destruction checks on the center
and six neighboring cells (glass survives), six near-wall45-degree direction
checks and the0.125% native EndCrystal event/random deflections. Native damage
cooldowns/armor/effects apply unchanged. Glass changes the charge into an
immediate451-sample beam at.33 spacing, source35% destruction checks, glass-based
particle palette and stable-hit advancement. All16 source palette entries plus
tinted-glass black are scoped per beam, never taken from another caster's storage.

Academy spends the same1200 and advances the existing player h/s/v counters
(h+20 modulo360, saturation80, value100). It has301 samples at.3 spacing,
7 native generic damage, and one target hit per leg; a bounce clears only its own
hit set. Native block faces replace ambiguous source side probes/random global
marker fallback for reflection at corners and negative coordinates. The source
Academy ray had no shield-blockable marker tag; ordinary/stabilized charges
retain their source shield handling. Friendly mobs can be hit (user); caster,
allied players, protected technical roles and antimagic regions remain protected.

Trial correction (user,24 October): an accepted real hit must be farther than75
blocks from the launch position, measured at the beam sample. The old age>=30
check (also accelerated by stable ray steps) is removed. Only the actually used
stack still in the matching hand can become ready, in either hand. No new fixed
range/counter replaces this condition. Source normal/stable sample/age bounds
and Academy path length are retained; unloaded chunks stop the ray without loads.

## Ownership, lifetime and CPU

[Implementation](../src/main/java/me/skywars2/game/LbcPrism.java) uses native
Projectile ownership and existing reflection/Curse/Time Stop/physical-impulse
exclusion rules. No custom actor registry or new killer-attribution framework.
Native EndCrystals enter the existing shared Mobgun actor/child/explosion owner,
so their native removal during explosion does not lose attribution. Later native
attacks can take over credit through the existing rules. Crystals have native
behavior/lifetime and disappear with their owner's existing lifecycle/quiet stop.

Immediate stable/Academy beams share a3ms per-server-tick budget across item uses
and end-of-tick work, processing small fair portions. This is a cooperative budget;
an individual native world/particle/damage call is indivisible. There is no queue
capacity cutoff. Each surviving job finishes or follows source lifecycle rules
(antimagic, death, unload, match epoch/end). Callbacks may remove other rays or
stop the match; queue iteration tolerates synchronous removal. Block edits run
inside the existing indirect terrain-cause scope and native destruction drops.

The port explicitly bundles `airs_or_glass` and `glass_default`; the existing
other glass/color tag resources remain shared. Missing `airs_or_glass` in the
initial packaging was caught by the far-beam fixture and corrected before release.

## Verification

[Native fixture](../src/main/java/me/skywars2/game/PrismChecks.java) and
[two-match runner](../tools/smoke_prism.py). Native tests cover actual use/cost/
presentation, offhand launch, ordinary sample movement/lifetime, ally/role policy,
resident-only flight, glass transition/far trial/advancement, reflected Academy
path/hue, native crystal detonation/removed-source credit, physical impulse
exclusion, actual antimagic, freeze/resume, Academy promotion and match cleanup.
Immediate/lifetime boundary calls include manual steps. The source fixture must
keep the full forward AND reflected path resident, independently of player view.

Release evidence and installation are recorded in
`build/verification/prism-final/qualification.json` and `installation.json` after
the final successful two-match run. No claim of real client pixels, actual disk
restart/unload/disconnect, every rare RNG branch or peak-capacity performance.

Final release0c88aa25 passed22 recorded suite results across two actual mode1
matches (52.35s including boot/map load), with exact278sources/746classes/
2764resources equality. Both actual stop probes had unfinished queued Academy
rays, and every surviving charge/queue was removed by the real match stop.
Regressions: CausticOmega, AdvancedSnow, Neutral, Mobgun native actors/crystals,
Academy acquisition, KFC counters/menu and Frost menu. Only the known fake-player
Polymer packet-context ERROR pair appeared; every final suite passed.
Installed stopped main; rollbackb62a6093 retained; main was not started. Three
preexisting user sources remain byte-identical and excluded from this commit.
