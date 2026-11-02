# Combat and reward skills 14/16/18 — 2 November 2026

Status: verified and installed. Main test-server artifact is 7130c506; production
artifact d26e450f omits the eight map-authoring/deletion classes.

## Author decisions

- All three skills are unjammed. Ordinary arrows, including automatic arrows,
  still follow the shared TECH deletion rule (explicit subsequent answer).
- Machinegun accelerates to one burst per tick, then grows its burst size more
  slowly. Chosen rate: one additional arrow per twenty completed saturated bursts.
- Soul Hunter transfers every positive player base-attribute surplus, including
  attack speed. Clamp negative surplus to zero; use configured match HP as the
  health baseline. Remove old fractional HP/movement coefficients.
- Fallen Apart doubles the actual killed target's item drops, without rerolling
  the loot table, copying unrelated newborn items, or doubling experience.

## Native ownership and performance

`LbcCombatRewards` takes attribution after shared indirect-kill resolution and
before native player cleanup changes donor attributes. Death receipts use body
identity: native `Entity.equals` compares numeric IDs, and player respawn reuses
the old ID for a new body. Alive/removed bodies leave the transient identity map.
A genuine mob kill keeps
the uniform source 26-choice positive base gain. Player kills add their positive
base surplus over native/configured defaults; equipment modifiers are excluded.
Classic-PvP attack-speed baseline is 100000 rather than ordinary 4.

Native death scopes collect successfully published item entities for that exact
victim. Nested deaths retain separate scopes. Final stacks and positions are
copied once after the outer death operation, preserving rolled counts/components.
Unpaused rewards are immediate. Time Stop defers earned rewards until normal
execution; lifecycle/epoch changes and full core rewind clear transient receipts.
No global young-item scan, second loot-table roll, or per-tick NBT copy is used.

`LbcMachinegunSkill` observes actual native bow use in either hand; native final
release remains intact. Source charge/progress stay in the canonical scoreboard.
Each emitted arrow has an independent 50% normal-arrow consumption decision;
tipped/spectral-only ammunition cannot supply automatic fire. New arrows are
plain native owned arrows, without copying supplied ammunition components or
replaying final-release enchantments. Existing shared shot/trinket dispatch runs
once per successful automatic publication.

After source cadence reaches one burst per tick, burst size is
`1 + max(0, progress - 19) / 20`. Progress advances per completed burst, never per
arrow. A fair owner queue allows at most one unfinished burst per owner, with
128 arrow publications/tick and a soft 4 ms delivery budget. It accumulates no
catch-up debt. Time Stop freezes charge and delivery. Real release/death/selection
clears the draw; ordinary effect cleansing does not. Core rewind may cancel short
unfinished delivery while restoring the canonical scoreboard phase.

Source speed/spread quantization and the late charge-9800 speed discontinuity
are retained. Shared projectile lifetime, epoch, freeze and block-hit cleanup
own the actual native arrows. No world-wide projectile selector is added.

The original menu slots 10000:13/15/17 and their original advancement gates expose
these three skills. TMT remains last; deferred skills 2/7 and unrelated gates
are unchanged.

## Verification

Evidence directory: `build/verification/combat-reward-skills-2026-11-02/`.
The first two native attempts proved actual burst completion but incorrectly
expected arrows to survive the fixture's TECH field. The next fixture also had
to remove the native beacon created from its legacy marker, rather than only
discarding that marker. These failures are retained; runtime suppression was
not weakened. The author's explicit rule keeps TECH deletion for auto-arrows.
The player-death probe then exposed a breakable test helmet destroyed by its
lethal hit; that fixture helmet is now unbreakable. Its next actual replacement-
body kill exposed a runtime defect in value-based death deduplication: entity IDs
survive respawn. Identity-based receipts fix the second death's missing reward.
The native probe also explicitly checks attack-speed transfer on that second kill.

Final test SHA-256:
`7130c506e243be780863eaedba50489b6b5419a027b6aabd0952402805092a4b`.
Production SHA-256:
`d26e450f63205ae9a7d75ef289a8c87d07fec8c902b4300ed106e7647586f7ec`.
All eight groups passed on this exact artifact in eight actual mode1 matches:
combat-reward-skills, machinegun-skill, combat-reward-player-death,
contained-vip-fall-respawn, parasite, reactive-skills, combat-feedback,
signed-environment. This includes actual bow updates/release/pause, genuine
foreign-player deaths and replacement bodies, source gains, exact inventory and
equipment drops, and positive donor attack-speed transfer on a second death.

9811 frozen inputs, 2516 classes and 7354 processed resources match the artifact.
236 protected author paths and 31 live grimoire overrides remain unchanged.
Production/test common entries match; eight authoring classes are absent from
production. Child shutdown was normal, exact player/config/OP/level/ticket
snapshots restored, no retained locks; fixture port25577 only.
After testing, surplus blank lines at the check file's EOF were trimmed;
compileJava produced byte-identical classes, recorded in post-test-format-proof.
The generic runner's qualified flag describes its default two-match armor scope;
the dedicated qualification validates this explicit eight-match scope instead.

`main-install.json` confirms normal-stop installation of 7130c506, previous
0971bd4f retained in `build/verification/main-rollback-0971bd4f.jar`, restarted:false.
No main stop/restart/push, physical-client or peak-TPS claim is made.
Description-only RP fragments are under
`resourcepack_template/updates/combat_reward_skills_2026_11_02/`; the original RP
and existing author template translations were not changed.
