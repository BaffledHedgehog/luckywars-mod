# KFC wild spirits, counter coverage and consumable win button

Status: QUALIFIED on `9b2ac45e9ca4a298ee184972d90cb912fc84af2359d2b88628875809e4db92b1`.
Ready archive: `build/verification/skywars2-kfc-fixes-9b2ac45e.jar`; not installed.
This supersedes the old KFC note calling the win button a thrown snowball.

## User contract

KFC chicken Blaze spirits are WILD: acquire the nearest eligible living target,
including allied mobs, allied players and the caster. Their native fireballs can
hit those targets and subsequent owned ignition retains the same friendly-fire
policy. Observers and service entities remain protected; actual projectile owner
and kill attribution remain. Native armor, fire immunity and hurt cooldown remain.
The ordinary Blaze wand's targeting does not acquire this friendship exception.

The +1 food outcome updates real ported player gameplay counters and effect timers,
not merely compatibility scoreboard entries. Preserve the exact ordered legacy
112-entry ledger and its duplicate reverse_mana. Preserve each counter's existing +1 step and storage unit; do not normalize
different timers to a common duration (user clarification, 21 October 2026). Global clocks, entity IDs,
projectile ages, persistent currencies/unlocks and attributes are excluded.
Mana resource/maximum/regen remain the separate outcome9. See the detailed
source/state audit in [KFC_COUNTER_REVIEW](KFC_COUNTER_REVIEW.md).

The win button uses ordinary item use and consumes one item with native creative
consumption rules. Polymer sends paper with the existing lbc:win_button model;
it never creates a snowball. Its fake win message and fireworks remain and do not
award coins, wins or change match stage. Owner death/exit does not end the display;
match cleanup does, as previously confirmed by the user.

## Verification

Native KFC suites cover gun input and chicken conversion, edible outcomes, source
counters, rewards, Cultus and match cleanup. Additional focused tests cover wild
nearest-target selection, actual fireball collisions with friendly/self targets,
subsequent owned burning, protected-role controls, and non-throwing main/offhand
button use with the real Polymer client proxy. Build and two native matches passed in 42.3 seconds on the final
artifact. Evidence: `build/verification/kfc-fixes-final/`. Native armor fixtures apply
equipment changes before checking damage. Reflected/owner-transferred KFC spirits
retain WILD targeting. Native poison/wither ticks retain their original kill owner
after duration extension, with direct combat credit deliberately cleared. Main server and RP/template untouched.

All 112 source additions match the original executable order, including reverse_mana twice. Package CRC, 2638 included resource byte comparisons, Fabric version expansion and bundled SGUI passed. The isolated server stopped normally and released its world lock. Existing Polymer missing chunk-packet context warnings from synthetic-player setup remain; no check failed. Main JAR SHA-256 observed `332415d6ffac9c5f7dfb02a07f395849b4aa68d31312b512fb5748ce3e47ffe4`; unchanged by this batch. No graphical client test is claimed.
