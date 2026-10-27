# Infinite Quiver and original arrow family ? 2026-10-27

Implementation is frozen for the parent agent's initial compile. No build, server, native fixture, or git action was run by this agent for this change. QuiverChecks is registered by the parent; its checks are pending execution, not claimed passing.

## Source and approved decisions

Source: QUIVER_REGEN_BELT_SOURCE_2026_10_27.md and the original disabled LBC datapack functions and loot tables. The two copied loot resources were migrated with tools/import_game_resources.py:migrate_loot_schema, preserving the original 29 entries, weights (28 ? 10 plus Stand ? 1), component values and count distributions. The parent independently compared their source semantics.

The source trigger remains container slots 0?35 or either hand. User answer22 replaces the original independent hand/container generators with one full physical count and one original-table invocation per 300 ticks. Full clear-count scope includes inventory/equipment, personal crafting and cursor; it counts without consuming. Armor/crafting/cursor alone do not trigger generation. Each quiver unit contributes one original roll, with no duplicate hand batch. Passive generation continues through ordinary input stun/frost; Time Stop pauses it. Beacon suppression remains the source gate.

All custom behavior is disabled outside the map's ordinary dimension, including both Nexus and Imprinted. Epoch, observer/technical role, ending, Greed, Time Stop and relevant source suppression checks remain in place. Default damage protects other allied players, admits the owner and friendly mobs. Every actual native lightning bolt remains WILD as explicitly required; its TNT siblings retain the ordinary ally protection.

## Implemented closure

LbcQuiver configures the original Strong damage5, Weak damage1 and Reactive NoGravity on actual ordinary native bow/crossbow arrows. Other vanilla potion/spectral behavior and original pickup components remain native. Stand living-hit handling stays in its existing service; ground return uses the original Stand table. No unrelated Babylon arrow is reclassified.

Original special arrows101/102/103/111/112/113 have flight, first-ground and living-effect paths. Native tipped-arrow items carry POTION_DURATION_SCALE .125; the original raw20 marker therefore transfers2 ticks and the source marker normalizer reduces2 to1. Tests distinguish this real transfer from direct synthetic effect admission. Living102/103/112/113 retain per-target captured-owner FIFO contributions; Greed pauses before target validity checks and preserves both action and duration. Original101 shares the existing Babylon FIFO through acceptQuiver101; ground101 delegates the shared native no-proxy explosion service. Source111 delegates the existing resident Kiuas terrain service without mana charging.

Evoker offsets, shock effect order/durations and capped Wither, trapper chance/movement/floor jitter/age behavior, light trail particles and timing, all27 TNT and16 lightning offsets, and 200/400 tick trail cleanup are implemented. Marker and child authority is installed before native add callbacks. Child entities carry captured owner, epoch, role and friendly-fire policy, including inherited native TNT chains. All explosions and bolts are actual native entities/mechanics, without instant creeper proxies.

## Shared integration contract

LbcQuiver exposes ctor(game), tick/loaded/unloaded/forget/stop/clear, owner/cause/managedSecondary/canAffect/inheritChild, validEffectArrow(Arrow,int), and native effect TLS methods. Required mixins are QuiverNativeMixin and QuiverNativeMixin$Block. Parent integration supplies the LbcItems accessor/lifecycle, ColorEffects routing, shared hazard/indirect kill hooks, Babylon101 FIFO and explosion delegate, Kiuas terrain, quiver-owned fangs, resource packaging and GameChecks registration. The parent owns the actual randomstat levelup and shared explosion-chain validation.

## Native verification prepared

QuiverChecks uses real registered native player connections and acknowledges the latest actual packet after every teleport, preserving player snapshots including read-only scores, food and profiles. Scheduled world ticks drive its real bow/crossbow collisions, special-arrow effect transfer, Greed-paused mixed101 FIFO, independent original loot endpoints across all29 entries, approved generation scopes and batch timing, full native column and trail cleanup. Native weighted transformable-accessory producer and actual reforge work provide the acquisition witness; a debug leaf alone is not presented as full normal acquisition. No manual entity/effect/world ticks are used. The parent owns compilation and execution/results.

## Frozen SHA-256

- `src/main/java/me/skywars2/game/LbcQuiver.java`: `e8972db0cbe06d28270506d081a0c5b1912a4c2c2d1bbd924be30509aea4ebc4`
- `src/main/java/me/skywars2/game/QuiverChecks.java`: `1df6eb98777b53754094dcff98fd381f6a88bb40a27b2774c2489d5d3b967a17`
- `src/main/java/me/skywars2/mixin/QuiverNativeMixin.java`: `611205ddb309a1044a3ddf7807a46974269dc995589e3dc6bf101ff3b41dd05c`
- `src/main/resources/data/lbc/loot_table/arrows.json`: `bb357773ac4415da4e02e3da5e519c9cf44b927f17d9f654938f8ecb634b9699`
- `src/main/resources/data/lbc/loot_table/ported/infinite_quiver.json`: `48095328555fab1364b1f3ce9175aca9e090c0d3983da303b4d9ae310cae9f76`
