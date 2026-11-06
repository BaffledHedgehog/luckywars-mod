# Additional native TMT inputs — 7 November 2026

Source implementation on sealed `df047ccc`; not compiled or exercised by this
delegated task. No server/JVM, installation, author-RP, runtime/kernel/catalog,
ammo, entrypoint or test edits. Root must compile and verify the integrated result.

New package-private `LbcTmtExtraUses` exposes three operations:

- `candidate(stack)` only evaluates native pure predicates at bootstrap.
- `order(stack)` retains ordinary callback positions before native Item.use-only
  continuations. `LbcTmtUseProfiles` merges this with the nine existing callback
  routes and the established common native wand dispatcher order.
- `use(game, player, hand)` runs only inside the selected private property against
  the actual hand object. It does not replace the held item or run a prototype.

The existing Fabric callback executes Balance, Nexus Compass/Tools/Combat, Touhou
bomb/clock, Pozdman and the conditional supply-beacon fluid raycast. This preserves
the original callback's permissions, real target, owner admission and debit.
The ten Touhou inventory collectibles are not admitted by the consumable matcher.
Ordinary block placement, marker eggs, held arcanum ticks and Nahida's continuous
use are not converted into instant air casts.

Native Item.use-owned actions call the same existing Kapusta, Overload Crux and
special-fireball owners. Win Button's ordinary native override and the TMT adapter
share `LbcKfcRewards.useWin(player, hand)`, including the single accepted-hand debit.
The original action-only `useWin(player)` remains unchanged for existing callers.

Terra exposes its existing one-hand razor body and extracts the ordinary sword's
charge release into one shared body. New `castSword/castRazor` adapters preserve
the original source admission/main-hand rules without dispatching the opposite
hand again. Public ordinary `useSword/useRazor` retain their original offhand
continuations, including the empty-charge sword case. True Terra remains the
existing common-wand property and is excluded from direct-profile duplication.

`LbcSpecialFireballs.variant` uses the scope-aware native predicate item for its
SnowballItem classification. Its ordinary inactive path keeps the real native
item class. The actual TMT transport, type holder, hand object and codecs are not
changed. The shared native owner still selects all seven payloads and performs
its normal geometry/publication and sole item debit.

Validation here: targeted native-consumer review, extracted sword release-body
comparison, source whitespace checks and symbol visibility checks. No claim of
per-variant or mixed-property native verification. Source APIs/guards outside the
explicitly edited owners remain unchanged; root separately owns global TMT
dimension admission and the integrated fixture checks.
