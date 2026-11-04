# Zaika passive native follow-up, 4 November 2026

Scope is the CLOSED original passive token audited in
[NIGHT_ZAIKA_SOURCE_2026_11_04.md](NIGHT_ZAIKA_SOURCE_2026_11_04.md).
No author questions were asked. No invented active ability, polling service,
suppression category, target/combat rule, Chef erasure, or universal trap immunity.
Root owns the separate author queue and any integration/predicate corrections.

This agent added only:

- `src/main/java/me/skywars2/game/ZaikaPassiveChecks.java`;
- `src/main/resources/data/lbc/loot_table/ported/admin_zaika.json`;
- this note.

The ready leaf references existing singleton
`lbc:item_defs/lbc/zaika__e7f523430e71`; it has no reconstructed components.
Original RP and all protected author paths remain untouched. No build, server,
commit, artifact installation, or RP editing was performed by this agent.

## Root integration

Expose synchronous `ZaikaPassiveChecks.run(game)` as `zaika-passive` in the
existing isolated native check dispatcher. There is no tick hook or production
service to register. Add the helper to the authoring compile/package policy and
the ready leaf/catalog only under root's existing artifact qualification policy.
Helper uses the ordinary loaded datapack tables, not the marker lucky-block
override used by `lucky-block-rules`; run in a source-data fixture.

The native Loli table still contained twelve legacy `Inventory` NBT possession
checks at inspection time, unlike Neko's native inventory/equipment/offhand slots.
Root was informed: replace only those possession predicates with the existing
Neko native slot union, preserving other score predicates, entries and weights.
The new helper intentionally checks all seven carried positions and will fail
if a legacy predicate loses equipment/offhand possession semantics.

## Added meaningful checks (not yet executed)

Loaded native pool entry `expand(context)` checks give exact eligibility without
random sampling: all 33 Loli entries, the complete twelve-entry excluded set,
original excluded weights 10 (ranked helicopter 1), all 32 combinations of
final-luck 1/7, ranked 0/1 and mode 1/3/4/5, and all seven carried positions
(storage, main hand, offhand, four equipment positions). The independent source
score oracle verifies six final-luck gates, both helicopter ranked branches,
Crocodildo ranked gate, Muravei unconditional gate and both mode gates. Every
unrelated entry retains its baseline eligibility. Zaika's own original entry
remains eligible and gives the exact canonical token while carried.

Neko checks use loaded root predicates and actual native lucky-block BLOCK
outputs, canonical Zaika in every carried position, no-Zaika control, and
mainhand luck_key priority. No native canonical luck_key producer was found;
that control uses its exact source custom-data marker on a tripwire hook.
It does not fabricate the token being qualified.

The five selector entries are each expanded deterministically with weight10,
then generate their exact full-set outputs. Each contains its four corresponding
canonical armor leaves, with full native component equality. Original Techno
also gives its old_exoskeleton armor stand: this was verified in original
`neko_set_techno_full.json` and retained. The 26 named source-table closure is
selector + five full tables + twenty armor tables; native item-def factoring
adds reference nodes, and the original basic armor stand is not a sixth set.

Canonical presentation checks include carrier/model/tier, exact two custom-data
flags, untranslated styled name, three styled translated lore lines and exact
nested birthday line. Both actual UseItem hands preserve token/mana and shared
native payload count. Real MAG and TECH markers and a nearby player's native
strong artifact have positive suppression controls, while own native BLOCK
loot and all twelve Loli exclusions continue functioning. Markers are removed
synchronously before any tick, preventing TECH legacy marker-to-block conversion.
No claim is made that Zaika disables an already spawned trap or benefits another
player, nor that it changes any ordinary spell's jammer policy.

Player/equipment/attributes/scores/profile restoration reuses existing fixture
snapshots; fake global scores are scoped and restored; markers and artifact are
cleaned before restoring participants. Root must compile and run the final
artifact, correct any integration issues and retain actual qualification logs.
Server assertions do not establish physical-client rendering or performance.
