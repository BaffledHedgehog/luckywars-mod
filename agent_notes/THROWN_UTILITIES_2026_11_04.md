# Native Ruchka, Honew and throwable Metal Pipe — 4 November 2026

Status: qualified and installed; main server was not stopped or restarted.

## Author rules

> трубу не глушить, мед глушить антимагией и да, защищенным инвентарем тоже

Ruchka retains its explicit source TECH20 rule. The existing Psycho owner still
owns Twitching, including its own suppression, timer, forced inputs and cleaning.
Honew activation and projectile flight are suppressed by ordinary antimagical
fields and the enhanced artifact; targets inside antimagical fields are protected.
Protected Inventory prevents Honew inventory changes. TECH is not added to Honew.
Throwable Metal Pipe remains physical and unjammed.

## Exact sources and native ownership

Legacy root, read-only:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.

* Ruchka: `lbc/function/items/swords.mcfunction` and `items/swords/ruchka`,
  plus `projectiles/gadgets/bridgebuilders`, `ruchka`, `ruchka_2`, `ruchka_pl`.
  Accepted direct melee binds to the actually struck hostile player, rather than
  sampling an unrelated HurtTime player. One held item is consumed and the shared
  Twitching counter gains600. Throws use the source hostile-player proximity
  radius4.5 around the snowball position+Y1; source TECH20 pauses acquisition.
* Honew: `lbc:imba/foodblocks` and `projectiles/gadgets/honew`, `honew_2`,
  `honey_pl`, `other/drop_inv/*` and `other/rand_motion_small`.
  One successful payload ejects all41 nonempty inventory/equipment slots, each
  stack independently copied with its complete components. Pickup delay40;
  independent inclusive integers-50..50 times0.01 determine all three velocity
  axes. Existing item creator provenance is preserved. Exact source sounds and
  item particles remain. Author suppression/protected-inventory rules apply.
* Throwable Metal Pipe: `lbc:imba/lucky_items_imba` and
  `items/snowballs` → `projectiles/specials`.
  Native input immediately publishes a real ordinary `lbc:metal_pipe` item with
  the snowball launch velocity and actual thrower/creator. The existing native
  `LbcMetalPipe` owns airborne pickup protection, landing25 damage, sounds,
  rearming and pickup. There is no second landing engine.

`LbcThrownUtilityItems` decodes the three canonical authored definitions once,
through the existing utility prototype reader. Names, lore, models, tiers,
functions, conditions and counts remain; only the base item ID becomes native.
The three ready leaves reference these definitions without repeated presentation.
Ruchka gives4, Honew1, and the throwable pipe16, as in the source.

Ruchka/Honew travel as actual vanilla Snowballs. A persistent native attachment
contains kind, original author, epoch and a once-only payload claim; derived maps
are rebuilt on load, normal clone publication and whole-coordinator restoration.
The existing native entity tick gate delivers the payload before normal snowball
collision, after TimeStop's pause. No world scan, extra display, copied damage
engine or per-tick NBT discovery is introduced. Short projectiles retain normal
vanilla collision; active abilities and accepted terrain use their existing owners.

## Verification scope

Stage: `build/verification/thrown-utilities-2026-11-04/`.
Baseline main archive `368d1b4c`, local HEAD `3bee6ffa`,275 protected author paths.
Source and author RP are read-only; deleted templates are not restored.
No physical-client, audio-perception, peak-TPS or unusual-network claim is made.
The full IMBA root and TMT remain gated; no game-mode/skill/public rewind gate
is promoted by this batch. Exact result and installation follow qualification.

The first native candidate (`dde0eaee`) failed its fixture's Parasite positive
control before flight checks. Skill19 also requires the existing native
`acidacid` unlock; selecting the skill and enabling skills alone does not grant
it. The check now temporarily grants that exact unlock, keeps actual immunity
assertions and restores prior unlock membership, selection and configuration in
`finally`. Both rejected matches and their normal exact cleanup remain recorded.

Candidate `9f73205d` passed the full input/flight/inventory/landing suite. Its
replay fixture incorrectly expected a new Snowball object. The coordinator's
documented native contract revives and re-registers the same saved instances.
The check now requires that saved instance to be live under its authoritative
level UUID lookup, retaining every attachment/owner/position/inventory/effect/
TimeStop and resumed once-only payload assertion. No runtime change was needed.

Candidate `35713863` passed the main suite, then revealed a real missing shared
status replay: the coordinator restored native scores but did not capture
`LbcPsycho`'s private afflictions. The fix extends the canonical custom-status
checkpoint with typed target/author links, counters and forced-use bookkeeping.
It does not reconstruct effects by re-afflicting the target or assigning the
target as its own attacker. The exact replay check remains required.

## Qualified result

Test SHA256: `02284b2a42e429cb62eeecda7e9eea9d1a2b9af3270784d43a7fc8d430eb91d5`.
Production SHA256: `9c3d19410943f19b22e664aff3d8a7e1c7945cc3fac248db7aa56ee4e87421b5`.
All10 groups passed on the exact archive in10 isolated native matches: main
input/flight/inventory/landing and partial whole replay in both modes, plus
existing Metal Pipe, Psycho sound, clone skills, Vanity rollback and BedWars
protection/lifecycle. Children stopped normally, controlled snapshots restored
exactly, and no retained locks or cleanup errors remained.

The checks exercise actual native item-use callbacks, accepted `hurtServer`
melee and real entity-tick flight delivery after TimeStop, including replay of
an actor that had already flown for at least3 native ticks. Geometry/filter
negative controls also use the authoritative pre-tick seam directly. No physical
client attack-packet or persistence-across-server-restart result is claimed.

10 owned Java families and6 resource inputs were overlaid onto the qualified
utility archives. Every ZIP entry and 3184 retained class references were
checked; 10060 other source inputs and all275 author paths stayed unchanged.
Both build/libs archives and main were installed normally, keeping368d1b4c as
rollback. Ready leaves: `lbc:ported/ruchka`, `lbc:ported/honew`,
`lbc:ported/throwable_metal_pipe`. No RP update or release-gate change is needed.

Evidence directory: `build/verification/thrown-utilities-2026-11-04/`:
`qualification.json`, `main-install.json`, `package-proof.json`,
`linkage-proof.json`, `canonical-definition-proof.json`, the three final
`02284b2a-*-results.json` files and scoped `local-commit.json`.
