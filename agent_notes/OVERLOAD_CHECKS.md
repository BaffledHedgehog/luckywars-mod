# Focused Overload verification

`tools/smoke_lbc_loadouts.py --server-dir <isolated-server> --overload-only` drives three checks around one real match, after the reusable `bots` fixture:

- `overload-menu` in lobby checks the five source tables `lbc:ported/overload_{crux,helmet,chestplate,leggings,boots}`, expected native item type/components, and Polymer proxy/restore roundtrip.
- `overload` in a live match checks all negative-mana stage boundaries, fractional Stage V damage with and without the helmet (including `manamax == 0`), stage effects and helmet/chest gates; ray collision, target/damage attribution and observer exclusion; full-set hit healing; real survival and Creative Crux `ItemStack.use` with exact 7500 debt and fresh armor conversion (no immediate replacement Crux or +10M); automatic issue/rearm separately from consumption; full-set wand cost bypass (actual debit enters negative mana, with no +10M) versus partial-set strict spending; time-stop and magic/beacon gates; native leggings jump; boots slam start over a clear eight-block column and prevention by a solid blocker; movement-state clear preserving mana. Fixtures restore temporary player state and discard temporary entities in `finally`.
- `overload-cleanup` after `/sw stop` checks the overload status, custom effects/timers, armor cache and observer/player cleanup.

The default full runner reaches the same menu group after mana-potion cleanup, starts one additional match for `overload`, then stops and runs `overload-cleanup`. The complete suite includes these three groups in addition to the existing mana-potion groups.

Focused run passed in 52.44 seconds on JAR SHA-256
`72b4ec22bd53dd142748582b58319b25793f713d89275e414c7001b829bf46d5`:
four unique check groups, one match. Evidence: `build/verification/overload.log`
and `overload-results.json`. The initial run failed because its fixture retained
spawn protection; the corrected fixture reuses `readyForItemDamage`.
Full-suite and installation status: [SERVER_TESTS.md](../SERVER_TESTS.md).

Full run passed on the same JAR: 311.16 seconds, twenty matches,
56 completed scenarios / sixty unique native check names. A separate
41.42-second isolated run confirmed native lethal-debt death and big-ray
removal of a stone block below the ray origin (`overload-burst.log` and
`overload-burst-results.json`). The tested JAR is installed on the stopped
main server. A graphical client and CPU benchmark were not used.
