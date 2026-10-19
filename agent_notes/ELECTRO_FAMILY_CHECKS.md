# Electro family checks

The focused native smoke slice is `python tools/smoke_lbc_loadouts.py --server-dir <isolated-server> --electro-family-only`.
It runs the menu payload check in the ready lobby, the behavior check in one match, then stops the match and checks trace cleanup. The existing `--wands-electro-only` slice continues to cover Tiny Electro independently.

The menu check rolls `lbc:ported/electro_wand` and `lbc:ported/academy_electro_wand`, verifies their native item IDs, source model and custom-data markers, restores each Polymer proxy to the original stack, and checks `stelectro_wand_mana_use` is 400.

The behavior check uses native use-item packets. It covers ordinary and Academy casts in main/offhand with the mixed-wand setting enabled; checks both casts debit the shared cost once each and produce the three expected trace lifetimes (40, 45, 50) for each item; verifies insufficient mana rejection and exact 400-mana Overload debit; checks first-tick trace travel at the native two-block speed; and toggles Academy between default and ray mode without spending mana. Ray mode is checked against a hostile native hitbox, a same-team player, an Adventure observer, and a solid wall in front of a hostile target. One trace remains live for the match-stop cleanup check.

The quest-enabled `upgradable:1` variant is checked for its 60-trace meditation fan, ten-tick lifetime, native speed, owner attribution, and intentional owner/team damage using one forced trace segment. No Academy progression or incomplete-trial state is simulated. These checks also do not repeat Tiny Electro's detailed shield, conductor and time-stop cases; that existing focused suite remains responsible for those shared trace semantics.
