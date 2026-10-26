# Ordinary tools and Hammer + Sickle — 26 October 2026
Follow-up: [native special tools](SPECIAL_TOOLS_PORT.md) now closes expanded
axe/hoe and ordinary/expanded shovel pools, including native mace aliases.
The gated status below describes the earlier ordinary-only milestone.


Eight source outcomes now have narrow test leaves: random enchanted iron axe,
Tomahawk, Poleaxe, Hammer, OP Hoe, Sickle, random enchanted iron pickaxe and
Silk Pickaxe. The three ordinary weighted axe/hoe/pickaxe pools are public in
`/loot`. Their expanded `_all` counterparts remain gated. All mining/equipment
behavior uses the real vanilla tool, durability, attributes and enchantments;
no replacement fake tool or additional projectile ability was inferred from
the Tomahawk name. Source weights, qualities, names, styles, lore, tool slots,
negative attack modifier and enchantment levels are unchanged.

## Source boundary

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

- `lbc/loot_table/transformables_{axe,hoe,pickaxe}.json`, `op_hoe.json`.
- `lbc/function/other/craft/inventory_craft_check.mcfunction:1`,
  `other/socializm_inv.mcfunction`, `other/socializm.mcfunction`.
- `lbcr/function/raycast.mcfunction:17`, `raycast_left.mcfunction:17`,
  `communism.mcfunction`, `podchinenie_communism.mcfunction`.
- Existing author RP at the path in [RESOURCE_PACK.md](RESOURCE_PACK.md):
  all22 translation keys and Hammer/Sickle/Communism item definitions exist.
  No new RP additions or rewriting of applied template keys is needed.

Source ground/automatic crafting is replaced by the established native shapeless
crafting workflow: exactly Hammer and Sickle, one unit of each, in either the
player2×2 grid or workbench3×3. Preview has no sound/particles or consumption.
Only a successful actual result take commits donors and emits the source300
fireworks and level-up sound at pitch0. Full inventories cannot consume donors.
The result retains the exact source `socializm` name, one white lore row,
`lbc:communism` model, tierS and `{communism:1,raycastspell:1}` flags.

## Native ray and allegiance

Communism is reusable with no mana cost, mana admission, charge or cooldown.
Both hands use the shared native use dispatcher. The main-hand origin is the
eyes; offhand begins half a block forward as in the source. There are exactly
201 immediate one-block samples: the source increments Distance first and tests
`<=200` before recursion. The ray affects intersecting nonplayer source-tag
`minecraft:mobs` along its path, continues through mobs and through the original
`minecraft:airs` tag (including plants), and stops after sampling solid terrain.
Unloaded chunks/build limits stop it without loading new terrain. There is no
new projectile physics or artificial multi-tick ray travel.

Each eligible actor changes scoreboard team to the caster's (or leaves its old
team if the caster has none), gains `communist`, and clears its old attack
target without a fake damage event. Role/service/observer and active Greed
guards remain authoritative. Players are never subjugated. Source trade sounds
play once per occupied sample; happy-villager particles play at every sample.
Repeated samples deduplicate identity/team/ownership mutation, preserving the
observable sample effects. The unported Sauvojen boss remains role-protected;
its encounter and broader acquisition are not qualified by this batch.

The native summon owner record also changes, including ordinary summons and
Psychovex cultists. New melee allegiance, source damage, retaliation and future
native projectiles therefore use the new owner. The original summon is retained,
with no despawn/recreation, equipment loss or HP reset. Already emitted detached
projectiles retain their existing author. Native mob conversion carries the
captured ownership override before entity insertion. Unload removes references;
actual match stop clears ownership overrides. No global entity/tick scan is
added: the ownership map is weak and queries occur only during actual ray use.

Native resources and checks:
`tools/import_lbc_tools.py` imports only these13 source/recipe tables;
`tools/check_ordinary_tools.py` is read-only and does not run any generator.
`OrdinaryToolChecks` uses actual final-stack input, menu result transactions,
weighted rolls, real mobs/cultists and future arrows. `tools/smoke_tools.py`
reuses the isolated mode1 runner with exclusive locks, normal shutdown and
fixture restoration. Live grimoire datapack/user files remain outside writes.

## Verification

Build `ad9c6155b196f8386e0841106df20a9c9d9474b78aa260ba4f3a2ab73029fe63`
passed24 recorded groups in two actual mode1 matches (56.51s): tools, Syringe,
ordinary summons, Vex/cultists, summon crafting, friendly projectiles, KFC/Cultus,
creepers and special eggs, plus actual match-stop probes and cleanup. All390
source files,1107 class files and3124 packaged resources match the tested JAR.
The native tool group checks all8 leaves/three100-roll pools, source components,
real2x2/3x3 take/shift/full-inventory transactions, both hands, negative mana,
plants/walls, roles/players, stop/antimagic, native cultist melee allegiance and
future arrow ownership. Physical conversion carry is wired into the existing
conversion-before-add hook; this new carry path is code-reviewed, while the
separate ordinary-summon/Syringe native conversion regressions pass.

The initial combined run found an existing nondeterministic Syringe assertion:
equip refresh may itself emit a legitimate10% Blaze shot before the fixture
explicitly requests one. The test now snapshots existing entities immediately
before the explicit emission. No runtime probability or attack code changed.
The final complete run has no failed checks. The two known fake-player packet
context diagnostics are the only ERROR lines, as in the prior qualification.

Immutable evidence: `build/verification/tools-final/qualification-ad9c6155.json`;
results/log/build/byte verification and source/RP checker sit in the same folder.
Installer22752 verified main-world unlock, kept `7bcadc7c` as rollback and
installed the qualified artifact with no restart (`main-install.json`). The
main JAR hash was independently verified after installation. User files,
applied template deletions and live grimoire datapack hashes are unchanged.
Only the hash-verified obsolete `081a2f7a` archive was deleted; retain current
`ad9c6155`, previous `7bcadc7c` and fallback `41395417`.

Client pixels/audio playback and peak server TPS are not directly observed.
