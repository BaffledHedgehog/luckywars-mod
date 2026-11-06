# TMT native inputs and event composition — 7 November 2026

Component verified, not installed. This is a checkpoint toward the author's full
TMT request. It does not complete the sampler, acquisition or all-event audit.

## Behavior

Five registered Polymer carriers retain their actual native input classes:
carrot-on-a-stick, bow, snowball, crossbow and golden apple. Client projection
uses the corresponding vanilla item. The real item type, inventory object and
serialized carrier remain unchanged while a private property runs.

The coordinator preserves the source player/tick guard and the four source entry
groups documented in TMT_CATALOG_CLOSURE_2026_11_06.md. Accepted melee properties
use the real native damage callbacks. Consume properties use the existing food
owners, one nutrition/debit/event and one food-count increment. Infinite food
returns the same TMT with its private properties, rather than an ordinary steak.

The shared wand dispatcher now exposes its original 53 native routes without
copying their effect implementations. Additional ordinary input callbacks are
reused for Cunny slabs, Mellstroy, throwable utilities, God Dice, White Arrow,
teleport gadgets, bouncing poop, Clown items and hedgehog spikes. Native defaults
and canonical loot overlays both contribute inputs; custom data on a vanilla
carrier remains eligible (notably Big Bob). No gameplay function interpreter.

The finite direct-use catalog currently admits 419 semantic families / 487
variants from 867 registered native defaults and 1,787 canonical input overlays.
It is cached until reload. These are direct-use counts, not a complete-content
percentage or an assertion that every variant has an individual gameplay test.
The pre-existing empty reroll_9999/material_block resource is reported separately.

Version, mode and artifact type stay within each property. Native identity-only
predicates are projected only during that callback, including 26.3's inherited
TypedInstance predicates. getItem/typeHolder, network identity and item codecs
remain native TMT. Inventory artifact masks and trinket identifiers combine
private properties without sharing their type selector.

Ranged properties share one genuinely accepted native volley and its ammo/trinket
roll. Physical expansion precedes conversion; destructive converters receive
separate codec images. Minigun reload debits one next chamber. A shot accepted
inside another property completes synchronously after all outer property scopes
are restored, so sibling improvements are retained.

The item-atlas template and GPU evidence are recorded in TMT_ATLAS_2026_11_06.md.
Server model is lbc:tmt/procedural; tint color[0] is 0xFA0000 | (seed & 0xffff).
No pre-generated TMT sprite set or author RP edits. Physical Minecraft rendering
remains an author-side test after applying the template.

## Verification and carry-forward

Internal test f41ff0bf / production 24314d13, based on the full sealed
57d1d0c7/f8c38689 component, retain the login entity-protocol hotfix byte-exact.
Full hashes, exact archive checks and frozen inputs are in
build/verification/tmt-runtime-2026-11-06/component-verification.json.

Twelve actual mode1 matches passed on f1c099ea: native use, carriers, food
lifecycle, ranged, properties, wands, self, classic melee, jump artifacts,
incoming artifacts, reactive artifacts and entity protocol. The final archive
differs ONLY in TmtNativeUseChecks.class, adding a real four-property player
attack. That check passed again on f41ff0bf. Three additional mode3 matches
passed: carriers, ranged and BedWars basic behavior. All fixture JVMs stopped
normally and exact auxiliary snapshots were restored.

Specific TMT checks include actual Big Bob input, one thrown bucket and native
debit, all inherited identity predicates, exception cleanup, codec copies,
deferred native shot continuation, 512 composed physical arrows, independent
terminal converters, one reload/trinket debit, three-property infinite food and
a real four-property melee hit. General owner suites also guard ordinary items.
This does not claim every combination, peak TPS, physical client rendering or
complete supported rewind.

35 explicit Java families / 3 resources were compiled with Java25, Gradle512MiB,
one worker. Native fixtures used 2GiB sequentially. 10,285 other source inputs and
282 protected author paths remained unchanged. Concurrent HUD work was preserved
in the worktree and deliberately excluded from this component's compile/commit.
No main server launch, installation or build/libs replacement. Main remains the
qualified 83b0ea27 login hotfix; production libs remain b90ffea1.

Future candidates must use this internal archive, or carry forward all 35
families and 3 resources explicitly, plus the atlas template. Git status alone
does not describe committed-but-unreleased TMT work.

## Work still required for the full TMT request

- Unified seven-draw sampler covering meaningful native use, ranged, consume,
  passive, inventory, equipment, damage and tool properties, with explicit
  dispositions for unsupported legacy owners and service markers. Direct-use
  bootstrap alone is insufficient.
- Complete native event adapters where aggregate flags alone cannot preserve
  private selectors, identity or producer state; audit real block/tool actions,
  launches, death protection, continuous item use, clones and supported rewind.
- Token/active-drop creation of exactly one newly generated TMT, one logical
  TMT entry in random pools, and public test acquisition after the whole chain.
- Final full requirement audit and release checks. Keep the overall goal active;
  do not mark full TMT complete from this checkpoint.
