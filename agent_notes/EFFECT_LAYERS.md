# Independent status-effect layers

## Requirement (user, 27 October 2026)

Remove vanilla's rejection when the target already has a stronger effect.
Different amplifiers of the same status can be granted in either order, including
through `/effect`: a long weak effect followed by a short strong effect already
works; the inverse must also be accepted.

The implementation keeps one independent remaining-duration clock per signed
amplifier. The strongest numerical amplifier acts; weaker clocks continue to
expire and surviving weaker layers resume when stronger ones end. Same-amplifier
grants retain the maximum remaining duration, with infinite duration winning.
This does not add effect amplitudes. Existing shadow absorption remains its
separate additive signed ledger.

## Source audit and implementation

Native sources were read from
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `net/minecraft/world/effect/MobEffectInstance.java`: `update`,
  `tickDownDuration`, `downgradeToHiddenEffect`, recursive codecs.
- `net/minecraft/world/entity/LivingEntity.java`: `addEffect`,
  `forceAddEffect`, `tickEffects`, effect callbacks.
- `net/minecraft/server/level/ServerPlayer.java`: update packets and criteria.
- `net/minecraft/server/commands/EffectCommands.java`: native feedback trusts
  `LivingEntity.addEffect`'s boolean result.

The ordinary-effect merge stores ordered native hidden nodes. Native recursive
countdown already ticks all layers. Downgrade now removes expired intermediate
nodes before promoting a survivor, preventing loss of deeper live layers.
Hidden flags belong to their own layer and do not rewrite the active flags.

The existing-effect entity interception runs at the native `update` invocation,
after existing projectile/Greed/marker HEAD guards and `canBeAffected` admission.
Every admitted live grant succeeds, while active update callbacks run only when
active details change. Hidden grants do not refresh active attribute modifiers
or call `onEffectStarted`. Native force replacement stays exact replacement.

Existing signed constructor/persistent-codec widening, item caps, signed effect
formulas, visual transformations and shadow absorption are unchanged. Native
recursive persistence/stream codecs store layers; active-effect packets continue
to describe only the acting layer. Promotion retains native update callbacks.

## Validation state

`EffectLayerChecks.run(GameController)` supplies synchronous native checks for
both grant orders, signed extrema, expired intermediate layers, independent
clocks, same-level maximum duration, infinite durations, incoming hidden chains,
active flags/modifier identity, real finite/infinite `/effect` success, recursive
persistent/stream codecs, actual entity save/load, force replacement, clear and
the separate absorption ledger.

Both mixins and the native check suite are registered in qualified build72ea985d.
The combined artifact passed38 recorded results in two actual mode1 matches
(54.28s), including this suite twice, real finite/infinite signed commands,
recursive codecs/entity persistence and modifier identity; existing signed math,
shadow absorption, player/projectile/Time Stop, firearm/FOV and actual-stop
regressions passed. Exact449 sources /1236 classes /3140 resources match.
Evidence:build/verification/speed-fov-limit/qualification-72ea985d.json.

Automatic installer11424 adopted this release and waits for normal main shutdown;
main disk remains1a8a3825. No forced stop/restart. Source and native packets are
qualified; physical client frames, latency and real reconnect are unobserved.
All175 protected author paths and31 live loot overrides remain unchanged.

## Scope and pending decisions

Same-level duplicate clocks were not requested; the native maximum-duration
semantics are retained without needing clarification.

Native hidden effect nodes have no per-layer source field; promotion uses a null
source. Existing indirect-kill attribution behavior remains scoped to the current
native callbacks. Adding a hidden lower layer does not replace active attribution.
Per-layer source tracking is not introduced by this change.
