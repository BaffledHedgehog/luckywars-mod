## Banhammer correction (27 October2026)

The latest user request extends impact to no-drop mob bans and actual Carpet
removal, and replaces fixed-height falling with first native surface contact.
The centered scale4 model bottom is2 blocks below its pivot; collision now
rests that bottom on terrain. See [updated contract and evidence](BANHAMMER_COLLISION_FIX.md).
The original port qualification and author decisions below remain historical.

# Mortis, Death Scythe and Banhammer — 26 October 2026

## Source and author decisions

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- `lbc/loot_table/transformables_shovel.json`, `transformables_shovel_all.json`,
  `transformables_hoe_all.json`, `transformables_axe_all.json`, `death_loot.json`,
  `halloween/transformables_shovel.json`, `halloween/soulhunter_mace.json`.
- `lbc/function/items/swords.mcfunction:40–41`, `workingitems/mortis.mcfunction`,
  `workingitems/mortisrc.mcfunction`, `mortisbreak.mcfunction`.
- `lbc/advancement/death_skythe.json`, `function/workingitems/death_skythe_kill.mcfunction`,
  `lbcr/function/raycast_preloop14.mcfunction`, `raycast_loop14.mcfunction`.
- `lbc/function/items/swords/banhammer/{hurt,ability,rc,rc_end,tick_fall,tick_fall_end}.mcfunction`.

Author decision (26 October 2026): Banhammer retains the native disconnect,
including its author and allied players in the impact radius. Remove the old
intentional client hang: `ability_at.mcfunction` repeated the same10,000-particle
command40,392 times. This flood is not reproduced. Skill12 and absolute active
Greed protect against the disconnect. A new RP key provides the disconnect reason.

## Native behavior

Three ordinary shovel outcomes preserve actual vanilla tools, mining components,
durability, random enchantment generation, Efficiency255 and exact authored
attributes/name/lore/style/model. Mortis is still a real netherite shovel.
An accepted native melee hit uses its actual victim, including a lethal hit;
nearby unrelated HurtTime entities cannot redirect it. It lunges four blocks
beyond that victim in the attacker's horizontal facing, adds source RegenIII
for20ticks, emits the nine red dust groups and three dragon-flap sounds, and
opens the source passage behind the destination. The41 samples are0.25 blocks
apart; the passage breaks blocks with native drops and places black glass only
over source-tag air. Reads stop at resident terrain/build boundaries. Removed
supports and placed hazards retain the causal player through the shared tracker.

Death Scythe's original mainhand kill criterion gives one charge per two credited
kills, including native environmental kill credit. Both hands can cast for one
charge with no mana admission or debit. The original71 immediate one-block ray
samples pass through terrain; each emits15 sonic-boom particles. Targets within
six blocks take15 authored cactus damage, retaining the real cactus cooldown,
role/Greed/antimagic guards, allied-player protection and allied-mob projectile
hits. Six Warden impact sounds and the original skill-unlock advancement remain.
From the seventh sample onward, independent8% rolls cover each original11-cube
air template footprint. Overlapping selections coalesce before native writes;
visible terrain starts immediately and finishes in the shared CPU-budgeted
terrain queue. No structure placement or delayed beam simulation is used.

Banhammer charges once per15 accepted native melee hits. A free charged cast
from either hand finds the first resident creature/terrain hit across the
source201 samples at0.4 blocks. Friendly creatures and players are eligible
aims. It creates the source4x `lbc:banhammer3d` display200 blocks above impact,
plays supply-fall and red dust, descends exactly two blocks/tick for100ticks,
then disconnects every eligible player within5.5 blocks exactly once. This is
WILD and includes its own author. Enabled LBC skill12 and active Greed remain
immune. The original advancement is awarded on player contact; impact flash
and explosion particles remain. The display expires at200ticks. Dimension time
stop pauses its clock and movement while viewer tracking still enrolls new
connections/respawns. Death/disconnect of its author does not erase an already
launched hammer; match stop cancels all outstanding hammers and terrain tasks.

Both charged weapons expose the original charge/progress rows in actionbar.
Their four source score counters remain shared with KFC's literal +1 operation;
no conversion to seconds or surrogate private charge map was introduced.

## Acquisition and RP

Ready narrow leaves: `lbc:ported/random_enchanted_shovel`, `efficient_shovel`,
`mortis`, `death_scythe`, `banhammer` (each under `lbc:ported/`). Four complete
pools are public: `lbc:transformables_shovel`, `transformables_shovel_all`,
`transformables_hoe_all`, `transformables_axe_all`. Expanded shovels delegate to
the two already native Halloween/Soulhunter maces; their raw source aliases now
also return the native mace IDs, with exact source component migration.
Other broad LuckyWars pools/modes remain gated.

`tools/import_lbc_special_tools.py` generates only these bounded source tables.
`tools/check_special_tools.py` is read-only and verifies all12 tables, existing
25 RP translation keys and six existing models. The sole new RP text is in
`resourcepack_template/updates/banhammer.{ru_ru,en_us}.json`; merge it into the
author pack manually. The author RP and live grimoire override datapack are never
written or regenerated by this workflow.

## Verification

`SpecialToolChecks` exercises real native item input, damage/death callbacks,
source loot/pools, mining components, actual-victim and lethal Mortis lunges,
source effects/sound packets/passage/floor, mainhand and environmental kill
credit, both-hand mana-free charged casts, cactus cooldown/ownership through a
wall, role/Greed/antimagic/time-stop guards, advancement, dithered terrain,
Banhammer charge/fall/expiry and the actual native disconnect packet. A socketless
recording connection captures that packet without removing fixture players.
It checks ally/self impact, exact radius, skill12/config and real Greed impact.
An actual match-stop probe retains both a falling hammer and unfinished terrain.

`tools/smoke_special_tools.py` runs two actual mode1 matches and24 recorded groups:
new special tools; ordinary tools/crafting; both mace families; summon commands
and real Guardian beams; Syringe; friendly projectiles; KFC; special eggs; stop
and cleanup. The Guardian fixture establishes a clear beam lane independently
of earlier terrain-changing suites. No production Guardian AI is weakened for
this fixture.

Final artifact/evidence: `build/verification/special-tools-final/qualification.json`.
Client pixel rendering, audible playback, physical network disconnection and
peak-capacity TPS are not directly observed by these server fixtures.
