# Thirteen-TNT special drop — 22 October 2026

Current shared-helper update: [CREEPER_DROPS_PORT.md](CREEPER_DROPS_PORT.md)
adds the two creeper outcomes. `LbcSpecialDrops` now owns the one admission-time
thrower capture used by both actor services. `DropTrapMergeMixin` replaces the
TNT-only hook and applies the same narrow UUID comparison to both trap families.
TNT no longer keeps a duplicate token-owner map. Two native regression matches
passed on56a15ffb alongside the creeper/chest checks; original qualification below
remains historical evidence for the TNT/cache batch.

## Author ownership and performance decisions (user)

For the thrown trap tokens themselves, rather than ordinary loot after a blast:

> Да, сохранять отдельные стеки разных авторов (рекомендую)

Subsequent performance preference:

> вообще такое в норм игре не будет случаться, так что если это валияет на производительность можно этого не делать

The narrow merge hook runs only during a native merge attempt. Ordinary stacks
without custom data return immediately; only identical TNT/creeper trap tokens compare
causal author UUIDs. Same-author and ownerless stacks still merge natively.
No world query, timer or separate per-tick merge scan is added. This is an
implementation cost bound, not a measured throughput claim.

## Implemented behavior

`skywars2:tnt_drop` is a Polymer PAPER token with the complete ordinary source
presentation: yellow nonitalic trap_tnt, white nonitalic trap_desc, model
lbc:drop_trap, tooltip lbc:tier_ff, and exact custom_data explode1/drop1.
Native give and `lbc:ported/tnt_drop` use the same prototype. Existing RP assets
are reused; original resource pack and resourcepack_template are unchanged.

Drop the token during a match to emit13 real native TNT at the token's exact
fractional position plus0.1Y. All have fuse70, gravity and vertical velocity1.
Nine horizontal vectors form the source0/±0.1 grid; four use cardinal±0.2.
They remain real flying TNT until native detonation. No instant replacement,
new fixed lifetime, ground-contact detonation, spawn cap or lost queued blast
is introduced. Source world-generated tokens remain ownerless.

The five chest outcomes and TNT now share `LbcSpecialDrops`, renamed from the
chest-only dispatcher. Its source-order dispatch, one unit debit per item/tick,
resident-chunk checks, fake-give exclusion, epoch and time-stop rules are reused.
Chest checks retain their accessor alias; no second input scheduler is added.
Multiple recognized flags execute in source order with one final unit debit.

## Causal ownership, native protections and cleanup

The actual ItemEntity thrower is captured at ENTITY_LOAD, before deferred
activation. Pickup target UUID, nearest player and loot recipient never supply
an invented attacker. Native player drops assign the thrower before insertion.
Captured references retain a departed author for already initiated effects;
adding attribution does not cancel source TNT when that author dies/disconnects.
Observer/service victims stay protected. A narrow current-epoch TNT exception
allows previously emitted damage after its author becomes a match observer.

`LbcTntDrops` tracks native TNT and causal references. The existing TNT owner hook
checks Electro reflection first, then retained cause. Native TNT chain priming
inherits the marker, epoch and actual current owner before insertion, preserving
its own vanilla fuse and gravity. The native save hook persists the current
reflected/retained owner for primary and chain TNT using the vanilla owner field.
Ownerless payloads stay ownerless. Serialized UUID resolution follows native
entity availability; a real chunk-unload/offline-player round trip is not yet
qualified by this batch's in-memory native NBT tests.

Global TNT protection is applied once by LbcDamage after difficulty and before
ordinary armor/effects: settings0..5 yield100/80/60/40/20/0 percent player damage.
The payload does not get UNPROTECTED_TNT. No temporary Resistance workaround or
per-service damage multiplier is used. Existing target helpers exclude allied
players, observers and service NPCs before native exposure/damage/knockback;
friendly mobs remain physical targets. Ordinary native self damage is retained.
The trap is not a magical projectile and acquires no antimagic restriction.

Primary TNT uses the exact native class, allowing reuse of the existing scoped
explosion block-discovery cache and empty-air fast paths. Block destruction,
entity damage and TNT physics otherwise use native mechanisms. This turn adds
no new explosion algorithm and does not claim a measured peak TNT capacity.
Native explosion world-change attribution and map mutation journaling are reused.
Per-dimension time stop preserves fuse/velocity. Stop and stale epochs quietly
remove live primary/chain TNT and clear pending tokens/causes before later blasts.

## Verification status

Build and two isolated native mode1 matches passed on the final exact-native-
class/cache version, SHA256
103e3bd2cf0717c23604524516de41a218f409619c210ca965381047fb2a0c57.
The runner completed12 suite results in55.41 seconds, including the chest
dispatcher regression and both services' real stop/epoch cleanup. An earlier
pre-cache run also passed; the final artifact alone is published as ready.
`TntDropChecks` includes all source
vectors/count/fuse/location, native physics, real explosions/protection0..5,
role/allied guards, actual Electro reflection, time stop, native merge attempts,
departed authors, ownerless/reflected NBT, chain priming and real match cleanup.
The fixtures use actual server entities and native item drop/thrower plumbing;
they do not qualify real-client throw packets or full chunk-unload persistence.

Ready archive: build/verification/skywars2-tnt-drops-103e3bd2.jar.
Evidence: build/verification/tnt-drops-final/ (build/native/server logs,
results.json and qualification.json). All144 current Java sources match the
sources JAR;2649 processed resource files match the release, with Fabric metadata
and bundled jars verified separately after Loom transformation. CRC passed.
The isolated server was stopped normally, saved configuration restored, and
runner/world locks released. Main JAR remains69de8601, not installed. The existing
Polymer chunk-packet-context diagnostic on native fixture bot join remains;
this turn does not claim a new client-rendering fix or measured peak capacity.

## Sources and next work

[TNT_DROP_SOURCE.md](TNT_DROP_SOURCE.md) records source commands, dispatch and
active RP verification; [CHEST_DROPS_PORT.md](CHEST_DROPS_PORT.md) retains the
five chest behavior contract. No whole Lucky Block pool or unfinished game
mode is enabled by publishing this one ready outcome.

The creeper drops are now qualified in the linked current contract. Next: cage traps, using the same dispatcher, native actors,
shared target policies and damage ownership. Their reward dependencies and
terrain behavior require a bounded source audit before those leaves become
public test entries. Crocodildo remains a separate author-decision/reward task.
