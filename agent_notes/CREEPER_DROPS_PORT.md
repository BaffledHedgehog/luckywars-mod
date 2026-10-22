# Creeper special drops — 22 October 2026

## Kill ownership decision (user)

> Бросившему ловушку игроку (рекомендую)

The actual player who throws the trap receives kill attribution, including its
native delayed consequences. Attribution does not turn the autonomous hostile
creeper into a friendly summon or give its author control over native AI.

## Exact source payloads and public entries

[Source audit](CREEPER_DROPS_SOURCE.md) records the original functions, token
components, active resource pack and local native API behavior.

`skywars2:creeper_drop`, available through `lbc:ported/creeper_drop`, preserves
ordinary source presentation: yellow nonitalic `trap_creepers`, one white
nonitalic `trap_desc` lore row, model `lbc:drop_boss`, tooltip `lbc:tier_ff`,
and custom data `creepers:1,drop:1`. Existing resource-pack translations and
assets are reused. The original resource pack and resourcepack_template remain
unchanged; no new RP assets or translation keys are needed.

One ordinary activation emits exactly20 real native creepers at the token's
fractional position, without a Y offset. Ordered `(dx,dz,radius)` is:

```text
(2,0,1),(-2,0,2),(0,2,1),(0,-2,2),(2,-1,1),(-1,2,2),(-2,-1,1),(-1,-2,2),(-2,-2,1),(2,2,2),
(2,0,1),(-2,0,2),(0,2,1),(0,-2,2),(2,1,1),(1,2,2),(-2,1,1),(1,-2,2),(-2,-2,1),(2,2,2)
```

Repeated coordinates and source order are intentional. All20 actors replace
their ordinary death loot with `lbc:creeper_wand`.

`skywars2:creeper_drop_old`, available through `lbc:ported/creeper_drop_old`,
preserves the old undecorated barrier presentation and custom data
`creepers_old:1,drop:1`; it acquires no invented name, lore or boss model.
One old activation emits exactly16 native creepers at local `^ ^ ^4`, using
the token's yaw and pitch. Radii alternate1/2. Source yaw steps are0 through300
in20-degree increments, with15 rotations between16 summons. The token retains
its pitch and finishes with yaw increased300. This incomplete ring is preserved.
Old actors have ordinary native creeper death loot, without a custom table.

Both variants retain native20HP, gravity, movement, hostile player targeting,
retaliation, avoidance and the default30-tick fuse. They are neither instantly
ignited nor made persistent, weightless or invulnerable. No new timeout, owner
lifetime, spawn cap or replacement explosion algorithm is introduced. Ordinary
native despawn remains possible. Emitted actors continue after their author dies
or disconnects.

## Shared activation and causal ownership

`LbcSpecialDrops` owns admission, scheduling and debit for creeper, chest and TNT
tokens. Every matching flag executes in source order: ordinary creepers, old
creepers, the five chest outcomes, then TNT. The item loses one unit after the
combined activation, at most once per server tick. Held tokens and temporary
fake give visuals do not activate. There is no second creeper input queue or
world scan. Actual spawn points must already have resident chunks; deferral does
not force chunk loads or consume the token.

The shared dispatcher captures the actual ItemEntity thrower at ENTITY_LOAD,
before deferred activation. Current real thrower lookup takes precedence over
the retained reference. Pickup target, nearest player and loot recipient never
supply an invented attacker. The narrow native trap merge guard compares author
UUIDs on matching trap merge attempts; it adds no per-tick merge scan.

Primary creepers retain that causal author. Native potion clouds and native TNT
primed by their explosions inherit the cause and current match epoch before
insertion; further native children retain it as well. TNT owner resolution keeps
Electro reflection precedence. Ownerless traps acquire no invented player cause;
the primary creeper blast retains its native NPC source. Native potion clouds have no
owner by default, so an ownerless token does not invent a cloud author.
Retained references survive synchronous
native explosion and child callbacks before terminal records are reclaimed.

Native chunk unloading does not kill or discard an actor. Loaded tracking is
released while the causal reference is retained by actor UUID for same-match
reload. Native entity serialization preserves actor state. Stop clears retained
references; loading a stale epoch quietly discards the actor without death loot.
A full chunk-unload/reload and offline-author round trip remains a runtime
qualification item until explicitly verified.

Shared target and damage helpers protect observers, service objects and allied
players before native explosion exposure, damage and knockback. Native author
self damage remains possible. These guards do not add friendly-pet AI or a new
antimagic condition. Per-dimension time stop freezes managed actors and native
children. Match end, stop and stale epochs quietly discard loaded actors and
children, clear pending inputs and causal state, and do not manufacture combat
death rewards.

## Native death loot and terrain behavior

Ordinary combat death resolves `lbc:creeper_wand` once: its source
`random_chance` remains0.05 and its reward leaf is the qualified
`lbc:ported/air_wand` / `skywars2:air_wand`. No killed-by-player condition is
added. The table uses26.3's singular `condition` field and condition `type`;
leaving the legacy `conditions` array would silently remove its chance gate.
The global legacy raw Air leaf is unchanged. Native creeper self explosion
bypasses ordinary death loot, so it does not roll the5% reward. Quiet cleanup
also produces no reward. Old combat deaths use native vanilla creeper loot.

Creeper detonation remains native `ExplosionInteraction.MOB`: terrain destruction
obeys `MOB_GRIEFING` and native mob explosion drop-decay behavior. Disabling mob
griefing keeps blocks while retaining native entity blast effects. Creeper
blasts do not acquire the global TNT damage-protection setting merely because
they are explosions. Actual primed TNT children remain native TNT and use the
existing shared TNT configuration. Map mutation attribution and journaling use
the existing native explosion hooks.

## Verification status and next work

Build and two isolated native mode1 matches passed on SHA256
56a15ffbb5e1e1f8c3e9e16de65149da6a4fab53c68e4f09f0d72a7581fd1f8c.
The runner completed18 suite results in55.84 seconds, including TNT/chest
regressions and all three services' actual match-stop/epoch cleanup.
`CreeperDropChecks` verifies complete geometry/radius multisets, native give and
ported items, fake/held exclusions, per-unit dispatch, both flags together,
native AI/gravity/health/fuse defaults, actual fuse explosions and mobGriefing,
role/allied guards and actual throwing-player blast source. It samples512 fixed
native ENTITY-context loot seeds, then verifies a successful ordinary nonplayer
combat death produces exactly one complete native Air reward. Self explosion
produces no Air reward. Native poisoned-creeper detonation creates one correctly
owned cloud and primes two native TNT blocks; a child blast protects allies and
quiet stop discards pending children. Time stop checks real motion/fuse progress;
a departed native thrower remains captured independently of pickup target.

Ready archive: build/verification/skywars2-creeper-drops-56a15ffb.jar.
Evidence: build/verification/creeper-drops-final/ (build/native/server logs,
results.json and qualification.json). All147 Java sources match the sources
JAR;2652 processed resources match the release, with Fabric metadata and bundled
jars verified separately. CRC passed. The isolated server stopped normally,
configuration was restored, and runner/world locks were released. Main remains
69de8601, not installed; original RP/template unchanged.

This qualifies isolated native fake-player fixtures, not real-client appearance
or throw packets, actual chunk-unload/offline-author persistence, or measured
peak capacity/MSPT. The preexisting Polymer chunk-packet-context diagnostic on
fixture bot join remains; this batch does not claim to fix it.

Public ported entries expose these two bounded outcomes; they do not enable a
whole Lucky Block pool or unfinished game mode. Existing chest/TNT behavior
requires regression coverage alongside the new creeper checks.

Next: cage traps, with their own source audit and terrain contract. Crocodildo
remains a separate encounter/reward task.
