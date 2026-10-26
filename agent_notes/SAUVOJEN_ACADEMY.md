# Combined Academy Sauvojen Tuntija

Source: legacy LBC `items/wands/sauvojen_wand/*_upgraded.mcfunction`,
`workingitems/awaken_tuntija_wands/`, `items/swords/fevgalea_star/`,
and `items/wands/sauvojen_wand/M/`. See the historical
[source audit](SAUVOJEN_ADVANCED_SOURCE_2026_10_26.md),
[ordinary port](SAUVOJEN_PORT.md) and [meditation](SAUVOJEN_MEDITATION.md).

## Source corrections and implementation

The complete upgraded dispatch has **80**, rather than 83, source branches:
Ruby10, Terra10, Electro6, Air13, Acid10, Teleport5, Stone9, Fire8, Blind9.
The author-approved Ruby counter fix makes all ten branches reachable in order.
Its final Silence branch still removes its own carrier, as the source's broad
non-player erasure requires; no immunity or extra post-Silence cycle is invented.

Each native producer retains its author, hand, captured stack and epoch. Upgraded
drones last400 working ticks, aim/fire only with a qualifying nearby target,
and independently choose their source model every tick. They move horizontally
0.1 away from their owner's eyes and rise0.04. The old audit's toward-owner
description was incorrect. Ordinary producer motion and200-tick cutoff remain.

All payload adapters accept explicit origin/direction. They do not temporarily
teleport or rotate the owner, re-enter paid item use, award another item trial,
or create additional mana/Mage callbacks. The few source helpers explicitly
using the owner's rotation retain that distinction. Air's nuclear helper
descends from the drone and consumes that drone on impact. Detached instantaneous
Erecto/Psycho/Rifle helpers emit one source release, rather than paid player
queues. Source Reverse remains owner-centered.

Terra's Fevgalea closure includes the full spark beam, moving rain with falling
children, and all three randomly selected attribute-based charges. Native
command execution verifies that missing Health on the source display stores
zero over a previously nonzero scratch value; owner HP is not substituted.

`lbc:ported/academy_sauvojen_wand` supplies the original model, name and first
four styled lore rows plus `upgraded_item`, tierX and upgraded/mode flags.
The existing central Academy admission/promotion/single-use route gains one
Sauvojen row. Both hands retain configured2500 mana input and the shared bonuses.
Main-hand Shift use of the admitted original stack starts the destructive trial.
Build resource selection includes the leaf and the existing stone-cylinder NBT.
No new resource-pack assets are needed.

Time Stop, suppression, protected roles, Greed, source-specific friendship,
attribution, chunk residency, owner removal, unload and actual match-stop
cleanup remain shared with the native item services. Trial explosions explicitly
bypass native allied-player filtering while retaining technical-role/Greed guards.

## Verification

Build `c6815dec` passed94 recorded results in two actual mode1 matches (127.35s).
Exact454 sources,1256 classes and3142 resources match the tested JAR. All80 source
rows pass with native origin/ownership/stack/payment invariants. Source targeting,
cadences, sequential Ruby0..9, all model sets, lifetimes, both hands, suppression,
Time Stop, actor lifecycle and actual match-stop cleanup pass. Fevgalea failed
command storage is checked after native command-context drainage. Full source
trial terrain, complete web graph/first contact, actual allied-player native TNT
damage/author, per-owner cleanup and participant-state restoration pass.

Shared grimoire, terrain, signed effect, FOV/layer, firearm, visual, player and
projectile regressions also pass. The SLR fixture now preserves and restores
preexisting player attribution rather than requiring it empty. Item grant audit
passes130 Lbc factory calls. Early failures and their normal shutdown/restoration
receipts remain under `build/verification/academy-sauvojen/attempt-*`; they are not
release qualification. Physical client pixels/audio, full stochastic trial
survival and peak TPS are unobserved; native lifetime/phase ticks are explicitly
stepped inside actual matches.

Immutable qualification: `build/verification/academy-sauvojen/qualification-c6815dec.json`.
Ready: `build/verification/skywars2-academy-sauvojen-c6815dec.jar`.
Installer11424 waits for normal main shutdown; main remains1a8a3825 until the live
receipt in that evidence directory confirms installation. No forced restart.
Keep c6815dec,72ea985d,1a8a3825. Obsolete9d2591dc was removed: cumulative330 JARs,
11786058439bytes. The175 protected author files retain all original bytes except
our single added Academy enum row;31 live grimoire files remain untouched.
