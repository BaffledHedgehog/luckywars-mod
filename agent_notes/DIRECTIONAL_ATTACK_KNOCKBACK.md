# Look-directed attack knockback

The user's `attack_knockback` contribution follows the attacker's complete native
look vector (yaw and pitch). Looking up launches, looking down drives downward,
and diagonal looks split the same force between horizontal and vertical axes.
The existing attribute bound remains0..10; native getKnockback contributes half
that value before target resistance. Vertical strength scales with the attribute
and applies to airborne victims as well as grounded ones.

The replacement runs inside LivingEntity's six-argument native knockback method,
only for extra-effect force with a direct living attacker. Ordinary hurt/sweep,
projectile, collision and explosion knockback stay on their existing paths.
Native target overrides (for example Creaking's knockback immunity) still run
before this implementation. A zero attribute delegates the exact native path.

Split total extra power into min(total, attribute/2) and its remainder. Apply
signed knockback resistance to both, damp existing X/Z once as native, retain
native grounded lift/clamp for the remainder, then add the attribute's full look
vector without the0.4 vertical cap. Sprint and enchantment remainders retain
native horizontal force. Resistance>=1 blocks impulse and negative resistance
amplifies it. Existing ordinary damage push is retained. No extra native damage,
attack admission or hit is synthesized.

Active Greed remains absolutely immune. Frozen Time-Stop victims receive no
immediate attribute impulse. Native Player.causeExtraKnockback still owns
attacker slowdown/sprint cancellation and PvP's motion packet, sync flag reset
and old server-velocity restoration. A small wrapper also directs the attribute
share in Player's nonliving-target push branch; other push calls are untouched.

`/sw check attack-knockback` exercises actual native Player.attack and
Mob.doHurtTarget, upward/downward/diagonal/airborne impulses, values2/4/10,
resistance0/0.5/-0.5/1/2, real sprint and KnockbackII remainder, unchanged zero
attribute/base force, Greed and rejected hurt, native watched Creaking immunity, frozen/resumed Time-Stop velocity, the
nonliving extra branch and an actual recorded PvP motion packet/restoration.
`tools/smoke_directional_knockback.py` runs it and selected combat regressions in
two fresh disposable matches, restoring fixture files and stopping normally.

Build875fed3a passed32 recorded suite results across two actual mode1 matches
in60.94 seconds. Exact404 sources /1138 classes /3139 resources match the tested
JAR; protected author edits/deletions and live grimoire overrides are preserved.
The earlier Creaking fixture was corrected to set its native watched/nonmoving
state before asserting immunity; the final two matches both pass that case.

Installer6808 installed875fed3a after normal main shutdown/world unlock,
backing up6e648580 and not restarting the main server. Evidence and immutable
qualification: build/verification/directional-knockback-final/. Ready artifact:
build/verification/skywars2-directional-knockback-875fed3a.jar. Retain installed
875fed3a, rollback6e648580 and fallback1265875f; old385f9f11 archive was retired.
Cumulative archive cleanup:320 JARs /11393011558 bytes.
Client-rendered trajectories and peak-capacity TPS are not directly observed.
