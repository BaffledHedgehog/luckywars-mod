# Remaining Academy source audit — 25 October 2026

Read-only source audit; only this new note was written. No native/build/server/git/RP changes or runtime qualification. Read project AGENTS and agent_notes/AGENTS.md; preserve user authority, indirect kill attribution, narrow pool admission, and prepared-only server state.

Source root S: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Native root N: `E:/servers_mew/skywars2_last/luckywars-mod/`.

## Readiness

| Source flag | Native ordinary / acquisition | Missing Academy tier |
|---|---|---|
| stacid | Working `LbcWands.ACID_WAND`, `REGULAR_ACID`, cost250; public `lbc:ported/acid_wand`. Actual native ant reward table nests this leaf at10%; fat skeleton reward/checks include it in nine working wand outcomes. | A |
| baffledhedgehog | No matching native Java handler/item registration found by source flag; copied carrot-stick loot is not native readiness. | S |
| danmaku_staff | No matching native Java handler/item registration found. Ordinary volley and bomb system must precede promotion. | S |
| sauvojen_wand | No native combined Sauvojen item/handler found; nine individual elemental wands are separate items. | X |
| sheepwand | No matching native Java handler/item registration found. | X |
| unstable_spellbook | No matching native Java handler/item registration found; copied broad tables remain insufficient. | SS |

Exact native anchors: `src/main/java/me/skywars2/game/LbcWands.java:149,251,717`, `LbcClouds.java:376`, `LbcAcademy.java` enum; `src/main/resources/data/lbc/loot_table/ported/acid_wand.json`, `ant_rewards.json`; `LbcAntDrops.java`, `LbcCreatureDrops.java`, `CreatureDropChecks.java:61`, `AcidAntDropChecks.java` reward checks. These checks are existing code evidence, not rerun qualification in this audit. Ordinary acid sends a big300-tick charge with random yaw ±30° / pitch ±20°. Native cloud code already exposes `volleyAcid(..., big, ignited)`.

All six source `function/other/magic_academy/upgrade/<flag>.mcfunction` only announce success and set the tier shown above. Actual mechanics and trial readiness live elsewhere; `can_upgrade/<flag>.mcfunction` only prints the condition. Shared source upgrade flow must supply upgraded state. Existing exasperato alias is outside this remaining set; infbolt deliberately stays outside Academy.

## Recommended next cohesive implementation

Implement only **ordinary acid Academy trial + A-tier two-mode promotion**, reusing the already native item, acquisition leaves and shared acid owner. No broad loot table enablement is required.

Source `function/workingitems/sauvojen_staffs/acid_preloop.mcfunction` fires the normal big charge, then calls Academy readiness when the caster is on fire and selected stack has `upgradable:1`. `acid.mcfunction` debits configured250 after invoking that helper. `acid_preloop_2.mcfunction` contains the corresponding downward variant. Preserve attribution and actual used-stack/per-hand handling under the established native rules rather than copying mainhand selectors.

Source `acid_upgraded_pre.mcfunction` switches two modes when sneaking, announcing `mode_default` / `mode_ignited`; otherwise `acid_upgraded_pre_2.mcfunction` casts and debits250 with existing hooks. `acid_preloop_upgraded.mcfunction` creates the same300-tick big marker, owner/team, adding `on_fire` only for mode2. `acid_preloop_upgraded_lib.mcfunction` is an explicitly ignited producer helper. No extra damage/range buff occurs in the tier-only upgrade function. Verify mode state remains independent per hand, free mode selection follows current user rules, trial readies the real used stack, and ignited downstream acid behavior/terrain ownership uses the existing shared subsystem.

## Larger leaves and decisions

- Danmaku source `function/items/wands/danmaku_staff/click.mcfunction`, `click_2.mcfunction`, `click_bomb.mcfunction`: ordinary activation adds20 ticks and charges configured1000; Shift refunds that charge, starts a bomb only at score≥50, and readiness requires pre-reset score≥1500 plus upgradable. `shoot_1_upgraded` has8 homing directions; `shoot_10_upgraded`17 fan shots; left upgraded rows each6 ring shots. This is a complete ordinary projectile/score/bomb port, not an enum addition. Existing detailed source notes: `agent_notes/lbc_items_weapons.md` Danmaku section.
- Sauvojen source `function/items/wands/sauvojen_wand/cast.mcfunction`, `cast_2[_upgraded].mcfunction`, `flying_wand_tick_upgraded.mcfunction`, `meditate/`: ordinary chooses among9 elemental flying displays; trial is a long destructive multistage meditation. Existing `agent_notes/lbc_items_weapons.md` documents terra/electro/acid/scare/fire stages. Suspicious source: upgraded tick increments twice before modulo10, making even indices unreachable; blindness check ends in a condition without `run`. Ask whether to fix these when this leaf is selected. Trial includes very large lava/world-clear masks; obtain explicit treatment with source geometry before implementing.
- Unstable book source `function/workingitems/random_spell.mcfunction` rolls1..55, invokes underlying ordinary spell helpers, then debits book cost. `random_spell_check.mcfunction` tracks only have1..have53, requires all53 and writes through a remote container/mainhand scratch slot. Academy mode selection `random_spell_upgraded_change.mcfunction` calls53-mode cycling yet announces54/55 too. Decide intended53 versus55 progression/selection; implement actual stack ownership instead of scratch-container behavior. Full dependency closure includes missing sheep/hedgehog/Danmaku/combined Sauvojen and additional spells, so do not expose this book early.
- Sheep/hedgehog: source flags are present in copied loot/Academy routes, but the narrow flag search did not establish their ordinary raycast dispatch and concrete trial semantics. Their `raycastspell:1` presentation means flags alone are not a sufficient dispatch audit. Trace the actual spell-id/raycast helper chain before choosing either for implementation; no source semantics or readiness inferred here.

No new user decision is evident for the narrow acid scope from inspected functions. Larger suspicious branches above are questions for their own selected migration, not reasons to block acid.

## Integrated qualification

Build `1229b044` passed 28 recorded suite results
in two actual mode1 matches (124.28 seconds), including ordinary/awakened acid, Gaycaster, acid ants and actual retained-projectile match-stop cleanup.
Exact 344 Java sources / 942 classes /
2814 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-acid-academy-1229b044.jar`. Evidence: `build/verification/acid-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
