# Shared Witch acquisition - native stage, 29 October 2026

This stage connects the real Witch Gens item and FIVE complete acquisition
producers: Gluttony, Wrath, Greed, Indolence and Envy. All seven source modes
can be selected; their conditions read the existing live owners. Lust and
Vanity remain entirely disconnected from acquisition until their actual active
families/checkpoint exist. Neither tag-only success, failed-admission punishment
nor item debit substitutes for those missing dependencies. The original full25
Chaos pool is unchanged and its transitive release gate remains CLOSED.

Author decisions remain in [AUTHOR_DECISIONS_2026_10_29.md](AUTHOR_DECISIONS_2026_10_29.md).
In particular, Wrath/Vanity damage progress is direct melee only; Indolence
fingers/rebirth are nonmagical; full Vanity must rewind the world, all players
and core state including offline/dead/newcomer cases. No NBT-only checkpoint.

## Shared implementation and source rules

`LbcWitchClaims` reads `LbcSourceCounters`, native player selectors and actual
participant lives/match time. There is no parallel progress or authority ledger.
Thresholds: food64, direct-melee damage5000, mushrooms64, sleep3600. A present
nonspectator incumbent requires STRICTLY greater progress, including Greed
prestige; ties and missing scoreboard operands do not compare. Greed also needs
greed_cursed and a raw greed_greed item in native Inventory, including equipment.
Envy needs no have_mom on caster and have_mom on every other qualifying player;
zero others succeeds. Vanity's diagnostic reads actual lives<=1, melee score0
and canonical match time>=600seconds, not a fabricated/stale scoreboard clock.

Raw vanilla item callbacks enter the existing two-hand dispatcher. Each eligible
hand independently switches the shared gens_type, including missing0->1 and7->1.
Unset/invalid nonsneak modes and an already held authority do nothing. There is
no mana requirement/debit. Native clear consumes ONE first matching inventory
item, which need not be the used hand. Other held authorities are retained.
Old selected authority tags are cleared globally from ONLINE players including
spectators; incumbent qualification itself uses the exact native nonspectator
GLOBAL selector, including an actual foreign dimension.

Source message order, existing translations/advancements, Greed curse/active
cleanup and Indolence active cleanup are retained. Gluttony drops ALL THREE
original actor items. Indolence delegates the existing actual finger producer
and raw claw reward. Runtime code does not force-load chunks. Reused
`glutton_summons.json` is converted with the existing26.3 schema migration and
packaged directly; no pool/weight change or replacement aliases.

Source parent admission checks the stronger antimagic artifact, not ordinary
MAG or TECH beacons. Common role, dimension, frozen/stunned/polymorph/FPV and
Time Stop guards remain. Failed supported qualification gives the original
10second Wither through ItemEffectLevels (approved item amplifier cap), one
owner-only wither sound and source counter feedback. Acquisition failure/mode
switching do not run successful-cast bonuses. Successful claims reuse shared
Nyauwu/trinket callbacks without inventing paid Mage-set maximum-mana growth.
The existing one-argument wand callback retains its previous paid behavior.

Native item stop now removes online authority/active tags at the existing
match-end boundary, before the existing passive/actor cleanup. Ordinary death
does not revoke authority or introduce a second progress-reset owner.

No author resource-pack/template edits. All reused keys already exist;
`build/verification/witch-acquisition/rp-reuse.json` records the read-only check.
The pending-question file and31 live loot overrides remain untouched.

## Exact native proof

Final artifact SHA256:
`c62d337e6ed889000b6e443c230de9b7b8dce07bba0f562c05c1afe76157b012`.
Offline Gradle build passed. ONE actual mode1 fixture match passed11 gameplay
suites in96.39seconds: witch-gluttony, witch-acquisition, witch-indolence,
witch-progress, witch-wrath, witch-passives, witch-envy, time-stop, mage-nyauwu,
indolence-rebirth and polymorph. The large Gluttony snapshot's setup pause is
fixture overhead, not a production TPS measurement.

New native checks cover the packaged raw source root/eight lore rows, real
UseItem callback/both hands, exact lower thresholds/ties/greater incumbents,
spectator policy, foreign-world incumbent, Greed equipment token, Envy's empty
set, native clear order, real source rewards and20finger production. They also
cover private item-capped failure, ONE Nyauwu25 pulse only on actual success
without Mage growth, real MAGIC/TECH/source artifact admission, actual frozen
input/thaw and scoped authority cleanup retaining canonical progress.
The neighboring true replacement-player death/respawn, hands/passives/ownership,
armor and natural polymorph checks passed on the SAME final artifact.

11036 frozen source/processed inputs and5230 packaged class/resource entries
match exactly;27 source references are hashed.199 protected author paths,
31 overrides and installed main d9087f0a remain unchanged. Normal child exit,
exact snapshot restoration, no retained locks/recovery archive/cleanup errors.
Evidence: `build/verification/witch-acquisition/final-audit.json`,
`inputs-c62d337e.json`, `source-inputs.json` and `c62d337e-*` native/results files.

Rejected b46ff856 exposed the missing raw-item callback edge; its logs remain.
Earlier315a309f passed4+7 suites in two matches before boundary cleanup was added;
those results are archived separately and are not the final artifact proof.
No physical-client/ping/peakTPS, natural600second Vanity qualification, complete
offline/rejoin/cross-restart authority reconciliation or full Chaos qualification.
Next: Lust flesh/dragon/original90mob producer, then full Vanity and complete
authority lifecycle closure. No main install/restart/push or question-file edits.
