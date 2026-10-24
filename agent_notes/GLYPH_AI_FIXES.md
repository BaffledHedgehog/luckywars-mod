# Glyph native AI corrections (25 October 2026)

User corrections supersede the earlier velocity pull and artificial `.001` arrow hit.
Belial now binds a real symmetric mob duel without any admission damage, hurt animation,
health change, fabricated DamageSource, or last-hurt mutation. A real Goal uses native
navigation, target/registered Brain ATTACK_TARGET, sensing, melee reach and line of sight,
20-tick attack cooldown, swingForAttack and Mob.doHurtTarget. It supports ordinary passive
mobs through a temporary native ATTACK_DAMAGE AttributeInstance at its vanilla default;
existing attack attributes are unchanged. The temporary instance is removed on release.
Native damage/enchantments/knockback and real accepted-hit attribution remain authoritative.
Pair-scoped WILD ownership is installed before the first swing; newer real player hits retain
shared death-credit priority. Players are never driven. Source 1/20 cadence admission,
glyph-centered candidate radius30, team behavior and protected roles remain unchanged.

Bafomet now exclusively leases mob pathfinding within radius16. Non-unholy mobs choose a
native reachable outward path; unholy mobs approach the glyph and stop navigating inside
radius1.5. It writes no velocity and performs no teleport. Native pathfinding respects terrain,
obstacles and the mob's navigation type. If no outward path is reachable, it waits/replans.
Overlapping Bafomet has priority over Belial combat. Source contact explosion and WILD
WitherV, Lamashtu and Ahriman behavior remain unchanged.

The Mob.serverAiStep HEAD mixin pauses the ordinary GoalSelectors and customServerAiStep
(including Brain behavior writers), retaining their existing objects and state. The lease runs
only sensing, its exclusive Goal intent, native navigation and move/look/jump controls.
On leave, invalid role, death/unload, owner cleanup, antimagic, Greed, Time Stop, match stop,
or epoch invalidation it restores the prior native target, aggression, registered attack memory
with expiry, path and navigation speed. It never toggles NoAI or removes/recreates vanilla goals.
The existing NoAI flag is respected. Idle Brain memories/goals resume their previous state.

Integration: register GlyphAiMixin, GlyphAiMixin$NavigationAccess and
GlyphAiMixin$AttributeAccess in the existing mixin configuration. Remove shared
LbcCorruptedGlyphs.forced(source) branches; there is no artificial ForcedSource anymore.
WildSource(owner) remains the authored native outside_border Ahriman damage identity.

Native API evidence: cached Minecraft26.3 source JAR
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/
minecraft-merged-d6f9bb6878-26.3-sources.jar: Mob.serverAiStep/doHurtTarget/setTarget,
ai/goal/MeleeAttackGoal, ai/Brain, ai/attributes/AttributeMap/AttributeInstance,
ai/navigation/PathNavigation. Mob.serverAiStep is final but injectible; customServerAiStep
runs Brain/species AI after navigation in vanilla, so merely adding a highest-priority Goal
would not reliably control Brain mobs. AttributeMap has no public dynamic registration API;
a narrow accessor permits temporary per-mob capability without mutating global suppliers.

CorruptedGlyphChecks now includes actual mob.tick navigation away/toward, hostile target
preemption, inner1.5, Greed/range restoration, no synthetic first damage, real passive-mob
AI melee combat/death, attack-capability cleanup and later real-hit credit priority. The flat
fixture snapshots/restores all touched blocks, entities, profiles, advancements and stats.
These changes have not been built or run in this worker; root owns compilation/qualification.
Author RP, old datapack and the three protected user Java sources were not edited.

## Combined integration qualification

Root built `17daf0d4` and ran114 recorded suite results in two actual mode1
matches, including these native checks and actual stop cleanup. See
[combined qualification](GAMEPLAY_FIXES_2026_10_25.md) for artifact identity,
ready-only deployment state and verification limits.
