# Reactive skills6/10/19 — 2 November 2026

## Author policy (user)

> мокренькое волшебство - МАГ, телепортит - МАГ, паразит - ничем не глушить
> кстати, фича паразита - чистка ВСЕХ дебафов (новых и кастомных тоже), при чем чистка до первого тика их срабатывания - я мог сам какие то дебаффы забыть

The author approved positive lost-HP ×100×absolute regeneration for skill6,
any hostile projectile for10, and evaluating signed/mixed effects by their actual
action: keep healing negative poison, reject damaging negative regeneration;
keep Water Breathing II underwater and remove it before its harmful land action.
These decisions supersede the historical narrow skill19 source-counter audit.

## Selection and shared ownership

Original page10000 slots5/9/19 expose skills6/10/19 with the Nyauwu,
Shulker Full and Academy Acidacid achievement gates respectively. Existing GUI
keys/models/lore are reused. No RP update. `LbcLoadouts.hasSkill` owns common
config, achievement, live-participant, observer, dimension and lifecycle checks.
The owned edible-sheep spectator form has a narrow recovery exception; ordinary
observers remain excluded. No new inventory scan or copied-NBT cache is used.

`LbcReactiveSkills` owns6/10. Skill6 measures actual HP loss around the shared
incoming-damage attempt, covering ordinary and direct-HP damage. Absorption-only
or rejected hits pay nothing; nested attempts do not double-pay. Signed base
regeneration includes MIN_VALUE safely. Mana additions saturate at INT_MAX,
preserving the original ability to exceed maximum mana. MAG and stronger
antimagic artifacts suppress both6 and10; TECH does not. Paused actions defer.

Skill10 checks any foreign enemy airborne native projectile within the original
four-block sphere, plus resident virtual rays through their existing scheduler.
Self/allies/grounded projectiles are excluded. Projectile evasion uses radius5;
accepted damage independently rolls20% for radius3. Resident surface search
does not generate chunks. TeleportMomentum delta-axis flags preserve independent
client movement; server velocity, known movement, yaw and pitch are retained.

## Parasite admission and cleanup

`LbcParasite` and `LbcMagic.rejectCustomDebuff` share harmful-status admission.
Useful custom grants retain `rejectCustomEffect` and their existing rules.
Native admission precedes sentinel translation. Direct instant harm/negative
instant health wrap the entire native invocation, including signed callbacks.
Loaded native chains and owned leases are filtered before their effect tick;
useful hidden survivors and signed absorption credit retain their ownership.
Reentrant effect notifications cannot recursively re-enter cleansing.

Canonical owners reject or retire harmful mana timers, fear/twitch/forced use,
shock/stun, frost, forced teleport, delayed deaths, adverse food/pill queues,
personal stasis, harmful mutations, hostile polymorph and control effects.
Forced-form recovery precedes cleansing of restored snapshot effects. Useful
mana boosts/additions, void resistance, caster charges and positive ability
queues retain their clocks. Permanent native attributes remain native state;
the source skill's own permanent penalty is preserved.

Signed effects are classified by action, including detrimental inverted buffs.
Mixed breathing is checked at native air processing before the first harmful
step. Informational source flags and native ownership remain authoritative;
status cleanup does not substitute a complete player reset.

Every future harmful custom status must use `rejectCustomDebuff` before its first
mutation and include narrow loaded/adoption/tick cleanup. A bare native holder
used as a proxy for shock/stun is insufficient: it has no signed amplifier.
Useful grants must keep their separate admission rather than treating every
custom counter as a debuff.

On accepted damage, Parasite retains its source40% roll and one uniform choice
among26 permanent attribute penalties. Native scaled arithmetic and source
coefficients are retained; Scale uses the restored native bounds.

## Verification

Final test artifact `e11faf22`, SHA-256
`e11faf2225c83df7fdc167e8a25ff0b446fefb076f45218fcd7dff1040e47319`;
production `b46a0bb5`, SHA-256
`b46a0bb5ebb59cbdd352bb06311a76a223a8d067fef671e825a18c6189314d36`.
All shared entries match; the ordinary production artifact omits all eight
map-authoring/deletion implementation/check classes.

Seven groups passed on `d5a7360a` in seven actual mode1 matches: reactive-skills,
parasite, combat-feedback, inventory-skills, signed-environment, polymorph and
witch-wrath. The final native mob-projectile ownership addition changes only
`LbcReactiveSkills.class` and its fixture class; all other entries remain
byte-identical. Reactive-skills passed again on final `e11faf22`, including an
actual hostile Skeleton arrow without legacy IDs and an allied Skeleton arrow.
This is eight qualifying matches, with exact artifact scopes recorded separately.

Native coverage includes actual hurtServer/direct-HP and nested mana loss,
absorption-only/rejected hits, signed/extreme regeneration, real suppression
fields/artifacts, ordinary unlock/menu callbacks, projectile spheres and native
momentum packets. Parasite checks direct signed instant paths, harmful hidden
tails/useful leases, signed absorption, canonical custom queues, useful boons,
real native water-to-land tracking, first harmful air action, all26 penalties,
deterministic chance draws and an actual accepted damage event.

9801 frozen inputs, 2489 compiled classes and 7353 processed resources match
the selected artifact; Loom's dependency metadata is separately verified.
All236 protected author paths and31 live grimoire overrides remain unchanged.
Children stopped normally, exact player/config/OP/level/ticket snapshots were
restored and no fixture locks remain; port25577 only. No main stop/restart,
RP edit, remote push, physical-client pixels/audio or peak-TPS claim.

Retained failures are explicit: a fixture used the private invulnerability field;
its setter fixes compilation. First native attempts exposed a menu-context setup
error and a real instant-callback ordering bypass, fixed by wrapping the entire
instant operation. Native Absorption requires a real capacity-granting effect;
26.3 uses EntityFluidInteraction rather than the old fluidOnEyes fields. The
shock failure exposed missing custom proxy guards, subsequently fixed also for
stun and gigaponos. No immunity expectation or runtime authorization was weakened.

Qualification, byte proofs and install receipts are under
`build/verification/reactive-skills-final-2026-11-02/`. The normal-stop installer
confirms `e11faf22` is installed, `7757f568` is retained for rollback and
`restarted:false`. No RP update is required.
