# Standalone Fevgalea Star

The standalone tier X sword uses the shared native Fevgalea rain and three
charge projectiles already supplied to Academy Sauvojen. The standalone beam
reads the actual caster's floored current Health at each contact sample; an
Academy item display retains the separately verified zero-Health result.
The full source, helper hashes and author RP audit are in
[FEVGALEA_STAR_SOURCE.md](FEVGALEA_STAR_SOURCE.md).

## Acquisition and presentation

Native item: `skywars2:fevgalea_star`; native leaf: `lbc:ported/fevgalea_star`.
The native shapeless recipe accepts exactly one True Terra and one True Falling
Stars in either 2x2 or 3x3, including the existing Academy Falling Stars donor
and exact imported donor flags. One of each is consumed on actual pickup;
remaining donor counts/components are preserved. Preview has no random or
world side effects. Actual take emits the source green dust and enchantment
sound. This follows the approved native crafting policy instead of deleting
whole ground stacks or every duplicate legacy crafting slot.

Source presentation is retained: carrot-on-a-stick carrier,4000 durability,
mainhand damage+16/speed-2.2, green nonitalic original name,12 original lore
rows, tier X tooltip and all five authored flags. Existing author RP supplies
the model, texture, translations and imported challenge advancement. No new
template assets or duplicate translation keys are required.

## Main-hand inputs

| Input | Charge admission | Mana | Result |
|---|---|---|---|
| Right click | <=0 |1000 |100-tick moving rain, local up15 from eyes |
| Right click | >=1 |0 |121-sample ray, current caster Health per contact sample, spends1 charge |
| Shift+right click | <=6 |5000 |Instant Health99 for1 native tick, Resistance V140 ticks, adds7 void-resistance seconds |
| Shift+right click | >=7 |0 |Enemy-only radius20, floor(each target's current HP/2) damage, spends7 charges |

Charged branches have no mana admission. Full Overload bypasses admission on
paid branches while preserving the actual debit/negative balance. The source
does not invoke Mage/trinket wand-cast bonuses, so neither does this sword.
Fevgalea ability input is main-hand only. The shared eligible offhand wand can
still cast independently, including when Fevgalea's own mana check fails.

The rain/charge closure retains source lifetimes100/25/40, local rotations,
per-tick displacement1/3.3/1.8, three charge collision strips, integer attribute
formulas, source particles and sounds. Native imported ARROW/OUTSIDE_BORDER
cooldown tags remain authoritative. In particular, the beam may hit the same
target at several adjacent samples; it is not reduced to one hit per cast.

## Charge, ownership and lifecycle

Charge is the existing per-player `fevgalea_charge` score, independent of item
stack. The original accepted, unblocked, non-arrow attributed damage hook is
preserved while the sword is in the main hand, including fatal hits and
attributed effects. This is broader than melee. Interaction victims and the
original Nexus/Imprinted exclusions remain. The source's previously unowned
children now retain required caster credit; their carrier/focus tag prevents
that added attribution from feeding the original hook recursively.

The original glyph/x/#41BBE0 score is displayed in actionbar while held in
either hand. The existing KFC "+1 to everything" counter increments it once.
Purify, owner departure/death and match stop clear the score; shared producers
retain Time Stop, suppression, shield, role/Greed, loaded-chunk and epoch guards.

The source beam has no team filter. The established native default protects
allied players and permits hits on friendly mobs. An optional exception is
recorded in [PENDING_QUESTIONS.md](PENDING_QUESTIONS.md), without asking the
sleeping author in chat. The seven-charge area explicitly excludes allies in
the source and keeps that restriction.

## Qualification and installation

Build `b8140165` passed80 recorded results in two actual mode1 matches in70.85s.
Exact457 Java sources /1263 classes /3144 resources match the tested JAR.
Actual native item input covers all four routes, mana thresholds/free charged
casts, full Overload overdraft, absence of Mage growth, independently paid
main/offhand casting, actual melee/external non-arrow/fatal callbacks, source
geometric beam contacts, fractional current-HP area damage, recursion guards,
allied protection, Time Stop, purify and actual match-stop cleanup. Native
recipe tests cover real pickup/quickmove/full inventory and both crafting grids,
exact donor debit/components/remainders and actual-take presentation.

Shared Sauvojen ordinary/Academy/meditation/helper, reactive/vex/neutral-craft,
grimoire, player/projectile, Time Stop, signed-effect/FOV/layer and firearm
regressions also pass. Fixtures explicitly step controlled service behavior
inside native matches. Physical client rendering/audio, internet latency and
peakTPS remain unobserved. The previously known Polymer fixture packet-context
warnings are retained in raw logs; no suite failure remains.

The first rejected attempt incorrectly expected only one beam hit. Its test
was corrected to the source's seven geometric contacts at the controlled
position,17damage each; production semantics were retained. The second rejected
attempt used raw Overload items without authored flags; its fixture now rolls
all four real migrated loot leaves. Both rejected attempts and their exact
normal shutdown/restoration receipts remain under the evidence directory.

Evidence: `build/verification/fevgalea-star/`.
Ready: `build/verification/skywars2-fevgalea-star-b8140165.jar`.
SHA256: `b814016594ae937aacfbd83ddb950986b594c95a5598e0d183923f5ab6fff504`.
Installer11424 adopted this qualified artifact over pendingc6815dec and waits
for normal main shutdown/world unlock (24566, RCON off). Main remains1a8a3825;
no stop/restart was forced. Keepb8140165,c6815dec,installed1a8a3825; obsolete
72ea985d was retired. Cumulative cleanup331JARs /11825523396bytes.

All175 protected author paths and31 live grimoire override files retain their
original bytes. No RP/template update is needed. TPS compensation remains a
feasibility discussion and is not implemented by this port.
