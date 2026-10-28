# Signed look-directed attack knockback repair ? 28 October 2026

## User requirement and scope

(user, relayed task) Remove both attack_knockback attribute bounds. Investigate the reported horizontal-only behavior for players, mobs and Carpet fake players, including strongly negative knockback_resistance. Preserve authored item data and runtime command values. Source balancing belongs to the separate integration task.

(user, subsequent clarification) Direct ALL melee knockback along the attacker?s look, including ordinary hurt push, sprint and enchantment contributions.

The final source change implements that full melee contract. The initial candidate retained the historical attribute-only contract and passed the newly added signed/Carpet native checks twice in the main agent?s candidate439d25c8 run. That evidence establishes a mismatch with the newly clarified contract, rather than an unexplained Carpet restoration failure.

## Local primary evidence

Read native26.3 source entries in `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `LivingEntity.getKnockback` casts attack_knockback to float, applies native enchantment modification, and divides by two.
- Both `LivingEntity.causeExtraKnockback` and the Player override admit only positive float strength.
- `LivingEntity.knockback` multiplies by `1 - knockback_resistance`, uses horizontal yaw/position force, and caps its grounded lift to .4. Existing directed injection only replaces positive attribute force and retains native hurt/sprint/enchantment components.
- `Player.causeExtraKnockback` retains attacker slowdown/sprint clearing and the real-player motion packet/server-velocity restoration.

Inspected installed `../mods/fabric-carpet-26.3+v260915.jar` using javap:

- `carpet.mixins.Player_fakePlayersMixin.velocityModifiedAndNotCarpetFakePlayer` reads syncVelocity but returns false for `EntityPlayerMPFake`. Installed Carpet already prevents ordinary real-client velocity restoration on fake victims.
- `FakeClientConnection.send` ignores packets. Replacing a vanilla ServerPlayer connection with a recorder does not reproduce the fake subclass condition.
- `EntityPlayerMPFake.respawnFake` constructs the actual fake subclass; its tick invokes native ServerPlayer tick and doTick. Tests therefore use that subclass and the installed native fake connection/listener.

A positive-attribute upward attack is not proved defective by this inspection: with attribute4/resistance-100, the existing grounded formula gives approximately horizontal20.2 and upward202.4. Attribute0 retains native horizontal force with only .4 grounded lift; a low positive attribute can be dominated by retained ordinary hurt push. The main agent must report actual integration results, not describe this calculation as a native runtime reproduction.

## Changes

`AttributePolicy` now classifies attack_knockback with the attributes whose lower and upper endpoints are unbounded. `AttributePolicyChecks` verifies the live registry and finite signed/extreme values through the existing policy matrix.

`DirectionalKnockbackMixin` wraps both native extra-knockback methods. For a nonzero attribute effect, an internally positive float admits the native dispatch, while a nesting-safe, finally-restored per-thread scope retains the original signed native value. This is required because otherwise negative and exact-zero-total attribute/enchantment combinations never reach LivingEntity knockback. The attribute itself is never clamped or rewritten. Native admission of damage, target overrides, slowdown, sprint clearing and packet handling remain in their original methods.

`DirectionalKnockback` now directs the full melee scalar along native attacker yaw/pitch, including ordinary hurt and sweep push, sprint and enchantments, with zero attribute included. Each nonzero living impulse halves the previous motion uniformly across x/y/z, preserving the native horizontal damping strength without a separate grounded lift or vertical .4 cap. A base .4 impulse followed by attribute4 produces2.2 along look; resistance-100 produces222.2 along look. Grounded and airborne victims use the same direction and magnitude.

Native extra scalar is reconstructed from raw signed double attribute/2 plus the native additive remainder. A negative total reverses look; an exactly zero total neither damps nor pushes. Very large attributes whose native float conversion overflows still use their original double directed component. Only unrepresentable motion arithmetic saturates to finite double endpoints; attribute values remain unchanged. Nonfinite vector results are suppressed rather than falling back to native NaN/Infinity arithmetic. Greed, Time Stop and resistance>=1 remain authoritative.

Base hurt steering requires native PLAYER_ATTACK, MOB_ATTACK, MOB_ATTACK_NO_AGGRO, MACE_SMASH or STING damage and the same direct/causing LivingEntity. Native extra melee dispatch remains effect-directed. Directly attributed spells and projectile source types do not qualify merely because the owner is also the direct entity. Native nonliving extra pushes also use the complete look-directed scalar.

## Native checks prepared for integration

`AttackKnockbackChecks` adds zero-attribute native base force for up/down/diagonal/horizontal yaw on both grounded and airborne victims; uniform previous-motion damping; look-directed sprint/KnockbackII/sweep force; resistance-10/-100; negative attributes-4/-100; attributes100/2b; both signed Double.MAX_VALUE with amplified resistance; a negative attribute plus KnockbackII whose native aggregate equals zero; and a negative native mob attack. An actual direct-attributed magic hit and projectile source whose direct entity equals its owner retain native horizontal geometry.

A fixed offline identity `TestKnockback` is constructed through real Carpet respawnFake/placeNewPlayer and enrolled through the native match gamemode path. The check asserts actual fake subclass and NetHandlerPlayServerFake, applies up/down/diagonal attacks at resistance-100, then uses the same fake as an attacker with attribute100. It does not replace the fake listener with a recorder. Finally it disconnects/discards the fake and restores the previous Participant/Profile entries and lastAliveTicks. The outer native harness must snapshot/restore TestKnockback playerdata, stats and advancements, together with its usual exact server snapshots.

## Validation and limits

Scoped `git diff --check` passed. No build, server launch, server test, install or commit was performed by this delegated worker. Main-agent integration owns compilation and actual native check outcomes for the final full-melee contract. The earlier attribute-only candidate439d25c8 compiled and passed the signed/real-Carpet checks twice, as reported by the integrating agent; that is not a final full-melee verification claim. No resource-pack or live-override edits were made.

## Integrated qualification - root, 28 October 2026

Final build31b84a20 (SHA25631b84a206f81a967e74a7812b7cbb0613e69eb84d3841d402afd489d0e3f5fec)
passed38recorded suite results in2actual isolated mode1matches plus2scoped participant setup/cleanup results,60.0s.
Real Carpet attacker/victim and resistance-100, complete base/sprint/enchant direction, signed/extreme attributes,
native melee/magic/projectile controls, source loot/mob/bow/imba/runtime behavior and neighboring combat/stop checks pass.
The direct native spear source is also covered; owner-attributed magic and arrows retain their existing geometry.
Exact544sources/1544classes/3230resources match the selected artifact.1300processed loot tables and Python migration
edge cases pass.198preexisting author paths and29live override JSONs are byte-identical. Native child stopped
normally and exact snapshots were restored, including TestKnockback playerdata/stats/advancements and usercache.
Initial439d25c8 attribute-only direction checks passed in both matches; its full qualification was rejected for
an obsolete pre-WILD arrow assertion and incompatible simultaneously retained TimeStop stop probes. Initial log,
results and artifact proof remain under build/verification/knockback-repair/initial-*. Final harness retains the
changed creature source's stop fixture, preserves all its assertions, and verifies current WILD arrow behavior.
Installer9360 is authorized to replace main b3c13ce9 only after normal shutdown/world unlock; consult main-install.json
for installed versus waiting status. No main force-stop/restart, physical-client rendering or peakTPS claim.
No RP change. Runtime commands/enchants remain unrestricted; ordinary source balancing does not rewrite live inventories.

Installation receipt confirms31b84a20installed overb3c13ce9, no restart, worker9360.
