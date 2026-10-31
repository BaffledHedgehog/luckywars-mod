# Damage reactions and reward consumers — staged native checks passed

This batch fixes direct black-hole HP drain and ports ten Imba runes, Rasengan,
Big Bob, the unstable spellbook and Ender/anal whip. Existing loot definitions,
item components, Polymer presentation and shared ability owners remain canonical.
No resource-pack update is required for this batch.

## Damage and legacy delayed work

Previously `BlackHoleDamage` subtracted HP without ordinary damage callbacks.
It now invokes shared Fabric admission, native effect reactions and advancement
criteria, death admission/protection, and the surviving-hit event in native order.
Native death owns its death listeners; no duplicate death event is manufactured.
The seven existing incoming-equipment attempt owners are shared with native
`ServerPlayer.hurtServer`, rather than copied into a second implementation.
Repeated drain still bypasses mitigation/hit cooldown and does not renew hurt
animation or change velocity. Real incoming criteria, nanobot wear and reactive
artifact mana were checked synchronously and exactly once per accepted drain.
Native totems and mirrored polymorph HP retain their existing behavior.

The confirmed next-tick infinite-stew workaround is removed. A scoped wrapper
around native `ItemStack.finishUsingItem` returns its replacement after native
debit and bowl processing, within the same call. Changing the held item afterward
is no longer overwritten on the next tick; both hands are checked. No pending
stew timer/checkpoint adapter remains. Native one-tick instantaneous-effect
instances are distinct from this custom delay and preserve their agreed behavior.
This is a scoped audit/fix, not a claim that every historical delay is removed.

## Reward ports and shared owners

- [Ten Imba runes](IMBA_RUNES_2026_10_31.md): original native modifiers, creative,
  participant lives, Nexus/Nether transitions, gallery, loot, map clearing and
  real controller restart. Stronger anti-magic artifact suppresses every rune.
  The existing Exasperato search queue now also supports original sampled rays;
  its DDA defaults are unchanged. The original `item_gallery.nbt` is included in
  the runtime JAR whitelist. Hagalaz clearing uses accepted shared terrain work,
  which completes before a Vanity save point can be taken.
- [Rasengan](RASENGAN_PORT_2026_10_31.md): native snowball use, 26 native power-one
  explosions instead of 104 repeated blasts. Knockback multipliers are x1 at
  cells 0–7, x2 at 8–10 and x6 at 11–25; damage is not multiplied. Native damage
  roles, WILD causality and source suppression are retained. No helper creepers.
- [Big Bob](BIG_BOB_DISPENSER_2026_10_31.md): free ten-nut dispenser in either hand,
  explicitly unjammed by the user. Its nuts reuse the existing food owner.
- [Unstable spellbook and whip](CHAOS_REWARD_ITEMS_2026_10_31.md): source 55-branch
  book dispatch reuses existing library/Sauvojen producers and Academy. The whip
  binds the actual accepted melee victim by full UUID and preserves momentum.
  User confirmed caster anti-magic suppression; native checks cover both beacon
  and stronger artifact, including the artifact with either used hand.

The global tests exposed an actor/terrain consistency problem after dimension
transfer. KENAZ now uses the native destination portal ticket. Before capture,
Vanity prepares actor chunks through existing asynchronous MapManager tickets;
its terrain image explicitly includes these resident destinations even before
visible ChunkMap publication. No synchronous generation or new checkpoint engine
was introduced. Full production rollback passes after this correction.

## Evidence and limits

Evidence directory: `build/verification/damage-reactions-chaos-chain/`.
Final archive: `chain-0794b535.jar`, SHA-256
`0794b535b63b39dcc04d7d9a7b444233d90375d774c453afae59391d1f8b8db1`.
Offline runtime/check build passed with `build -x sourcesJar`.

There are **21 distinct staged passing native groups**, not one all-groups run:
`staged-native-evidence.json` maps each group to its actual archive. Initial
`d05b18dc`, follow-up `de145976`, `d3ce7e02` and `869880d5` preserve their failed
receipts. Failures led to the Rasengan protected-role fix, gallery packaging,
destination residency and corrected fixture client acknowledgements. The old
Chaos table fixture now expands canonical loot references before comparing its
unchanged frozen source tables; gameplay/RNG assertions remain.

`869880d5` passes production Vanity rollback and actual Nautiz restart. Final
`0794b535` passes all five global rune payloads with a complete native rewind
between each, plus book/whip checks. `candidate5-diff.json` proves its production
classes and data identical to `869880d5`; only those two check families differ.
Earlier exact archive/input reports retain the narrower change boundaries.
Focused runner `qualified=false` is not promoted to a full release claim.

Every child stopped normally; exact fixture snapshots were restored and no locks
remain. `final-preservation.json` verifies 9,532 frozen inputs, 195 unchanged author
paths, one insertion-only Academy integration into the 196th protected path,
31 untouched live loot overrides and unchanged main JAR `d9087f0a`.
Only our Academy insertion belongs in the commit; its other author changes must
remain unstaged. No physical-client, arbitrary-map or peak-TPS claim is made.

## Subsequent resolved scope

The user confirmed waiting for the entire Chaos chain before any main mod update,
and confirmed Hagalaz permanently erases Chef. Both pending chat questions in this
batch are resolved; do not ask them again. The next
[Boba/Hagalaz batch](BOBA_ROTATION_AND_HAGALAZ_2026_10_31.md) implements and checks
those answers, Boba map rotation and rotated pristine respawn islands. A confirmed
dungeon/boss/Uni Wand reward continuation still prevents full-chain release.
The archived receipts above retain this batch's original limits and failures.
