# Armor reserve and Zeus skills — 2 November 2026

Status: verified and installed. Main test-server artifact4a409c08; production
artifact673e7017 excludes the eight map-authoring/deletion classes.

## Author answers for this continuation

- Skill7 is entirely unjammed. Consume worn-item armor/toughness points, with
  toughness worth 2.5 times armor. Preserve the original armor factor: 2 HP per
  armor point, therefore 5 HP per toughness point. Toughness pays first.
- Replace permanent Resistance V with an independent reserve after ordinary
  native mitigation. Existing Resistance continues to work. The terminal hit
  remains completely protected even if it exceeds the remaining reserve.
  Preserve native hit reactions, knockback, screen shake, effects and criteria.
  Void/outside-border and other invulnerability-bypassing damage ignore it.
- Zeus22: MAG/strong artifact blocks launch at its owner; TECH clips released
  electrical arcs. MAG is not a target shield against already released arcs.
- Intercal's stronger Wither synergy belongs to Govnoman9, not cloning skill15.
  The two source Wither layers and all other trinket behavior remain intact.
- Cloning15/17 is WILD and unjammed, preserving the original source/owner;
  boats/carts/XP/clouds are included, as are bosses for17. Protected map/shop
  actors and independent visual children are excluded. Joint x2*x4=8 is intended.
  Visuals must be rebuilt by their real actor owner, not cloned independently.

## Implementation

`LbcArmorReserveSkill` spends positive flat item armor/toughness points in the
four actual worn slots, including native default components. Percentage entries
remain native mitigation coefficients; penalties and other entries remain exact.
Equal-share depletion redistributes exhausted pieces' unpaid share and scales
eligible entries within each piece proportionally. It preserves all unrelated
components and modifier identity/operation/slot/display, and writes exact zero
at exhaustion. No inventory or NBT polling is added.

The native magic-mitigation RETURN hook changes only the final debit. The
surrounding hurtServer retains accepted-hit reactions, cooldown, sound, source
credit, native effects/criteria/AFTER_DAMAGE. Native equipment detection publishes
the changed attributes immediately, including another hit in the same tick.
The direct-HP hole bridge uses the same reserve while retaining its established
no-physical-response contract and pre-reserve reaction amount. No Resistance
effect is granted or removed, and no duplicate damage event is synthesized.

`LbcZeusSkill` owns a canonical scoreboard phase, once per actual active tick;
every20 ticks has one50percent trial. Time Stop pauses it. Selection/death/leave
clear it; core rewind clears only transient duplicate-tick guards. The source
finite nearest-target chain uses exact segment/AABB contacts, resident air steps,
local native spatial queries, and a pulse-start body-ID ceiling against newborn
recursive chains. Contacts are calculated once per leg, not per particle point.
Native LIGHTNING damage4 retains electrical immunity and ordinary hurt cooldown.
No native lightning entity, forced chunk load or arbitrary function is created.

Original menu slots10000:6/22 and original Paladin/BoltBolt advancement gates are
used. Existing RP names/lore/models remain appropriate; no RP changes are needed.

## Separate unfinished cloning chain

`LbcCloneSkills` is prepared but unintegrated. No adapter is installed, its
coverage gate stays false, and skills15/17 remain unavailable. Full NBT is not a
copy of private spell queues, custom actor factories or controller/source links.
The concrete remaining owners and boundaries are documented in
`CLONE_SKILLS_PREPARED_2026_11_02.md`. This continuation must not claim those two
skills are finished or expose inert copies of custom actors.

## Verification

Evidence: `build/verification/unspent-armor-skills-2026-11-02/`.
`ArmorZeusSkillChecks` uses actual native players, damage, cows, component/default
equipment, real original menu callbacks, and a staged native clock.

Final test SHA-256:
`4a409c089e0e3e8de5f779896f0cfa14e16494a1c9618b8aa69cea834e489917`.
Production SHA-256:
`673e7017b294608cc97df677265be67a0b03c67cb2cc20575a68e99241340e8a`.
Nine groups passed on this exact artifact in nine actual mode1 matches:
armor-zeus-skills, black-hole-contact, glass-armor, contained-skills,
reactive-skills, parasite, machinegun-skill, techno-armor, signed-environment.
This includes native Resistance stacking, all four worn defaults, same-tick
attribute refresh, terminal protection then leakage, real void/border hits,
direct-HP reserve depletion, native exact contacts/cooldown/owner credit and
actual staged updates for20-tick phase, Time Stop and owner launch suppression.

9816 frozen inputs, 2529 compiled classes and7354 processed resources match.
236 protected author paths and31 live grimoire overrides remain unchanged.
Production/test common entries match, and eight authoring classes are absent
from production. Child shutdown normal, exact player/config/OP/level/ticket
snapshots restored, no retained locks, fixture25577 only. The generic runner's
qualified flag concerns its default armor scope; the dedicated qualification
checks this explicit nine-match scope.

The retained2080d2d9 attempt exposed a fixture assertion using Entity's separate
invulnerableTime instead of26.3 damageCooldownTime. The final fixture checks the
actual native hurtTime and damage cooldown. Cached native resource inspection
also confirmed outside_border lacks BYPASSES_INVULNERABILITY, so its explicit
author-requested bypass is checked separately from that tag. Ordinary hit
admission was not weakened to pass the tests.

`main-install.json` confirms normal-stop installation of4a409c08, previous7130c506
retained in `build/verification/main-rollback-7130c506.jar`, restarted:false.
No main stop/restart/push, physical-client or peak-TPS claim. No RP changes.
Cloning15/17 remains explicitly unfinished/unavailable despite the compiled
prepared helper; the above native qualification does not qualify that helper.
