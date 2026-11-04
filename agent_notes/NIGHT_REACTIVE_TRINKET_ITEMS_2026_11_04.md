# Nine reactive trinket item/source audit - 2026-11-04

Read-only preparation after qualified BedWars ea6d6572 / installed dc033ee7. Only this note and ignored `build/verification/reactive-trinkets-2026-11-04/source-items.json` written. No Java/resources/question file/RP/template/build/server/commit changes. TMT LAST. Author Academy/key WIP untouched.

Evidence JSON retains complete original item JSON and GUI JSON, exact function arrays, identity SNBT, gate/locked key, all direct acquisition references with source lines, chance source text, terminal source text, actual RU/EN/RPR values, model file paths and source/native hashes. L = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`; F=L/function.

## Presentation, acquisition and finite package closure

All nine original items use repeating_command_block, max stack99; white nonitalic translated name; gray nonitalic lore in original order. No style/model edits required. All nine actual RP item models exist (modern assets/lbc/items plus original model definition), and all40 unique name/lore/locked keys exist in each of RU/EN/RPR. Hedgehog model is `lbc:hedgehog_gauntlet`, not trinket_16; Glass Shard identity is **sunprism**, Drojjevoe identity **drojji**. Preserve exact lore suffixes:4/6/8 use0/1;12 uses1/2;15/17 use1/2/3;22/24 use1/2;23 uses1/2/3/4.

| ID / canonical leaf | slot | model | gate suffix | locked key suffix |
|---|---:|---|---|---|
| 4 / drojjevoe_odealo | 3 | lbc:trinket_3 | acid | acid.lore |
| 6 / mandoahuet | 5 | lbc:trinket_5 | bass | bass.lore |
| 8 / nether_plug | 7 | lbc:trinket_7 | blaze | blaze.lore |
| 12 / banangay | 11 | lbc:trinket_11 | ender_random | ender_random.lore |
| 15 / twilight_sparkle | 14 | lbc:trinket_14 | falling_stars | falling_stars.lore |
| 17 / hedgehog_gauntlet | 16 | lbc:hedgehog_gauntlet | hedgehog | hedgehog.lore |
| 22 / poly_gem | 21 | lbc:poly_gem | polymorph | polymorph.lore |
| 23 / glass_shard | 22 | lbc:glass_shard | prism | prism.lore |
| 24 / psycho_soul | 23 | lbc:psycho_soul | psycho | psycho.lore |

All page11000, slot=id-1. Gates are `lbc:true_advancements/legends/academy/<suffix>`, locked keys `lbc.adv.upgraded_staff.<suffix>.lore`. Source `swrg_kit_integration/gui/select/11000` supplies exact achievement locks; ranked selection bypass is existing behavior. Grant dispatcher `swrg_kit_integration/game/trinket_select/select_1` grants the canonical `lbc:trinket/<leaf>` for matching numeric ID. Exhaustive bounded literal-reference search of LBC loot JSON + function text found one direct acquisition reference per leaf, this dispatcher; each raw GUI table is separately retained. Original source leaf paths are `L/loot_table/trinket/<leaf>.json`; GUI paths `L/loot_table/gui/page/11000/<slot>.json`.

All nine native canonical files already exist under `src/main/resources/data/lbc/loot_table/trinket/`, but current processResources finite includes omit these nine paths and catalog has none of their IDs. Existing prepared files replace vanilla carrier entry with common `lbc:item_defs/minecraft/repeating_command_block__6711510709f0`; that leaf is pure repeating_command_block and existing item_defs wildcard packages it. **All nine authored function arrays (components/name/lore/identity) are structurally identical to original**, though whole-file hashes/JSON differ because of this existing carrier rewrite. Future closure should include these nine exact canonical paths, reuse common leaf, add nine exact catalog entries only after consumers qualify. No extra loot dependency edges in nine source leaves.

## Actual count/chance and source trigger contracts

n is actual matching identity count from source `/clear ... 0`, conditional n>=1. Independent trinket checks may coexist; probability is not divided by stack size. Universal macro passes raw random_chance; p>=1 is certainty. Native established inventory/crafting/cursor count policy should be reused.

| ID | source chance | trigger / root |
|---|---|---|
|4|n1=15%; n>=14 guaranteed; n2..13 source defect below|successful source sword route, trinket_effects/acid -> charge_acid_ball|
|6|n1=15%; n2..13=(150+50(n-1))/1000; n>=14 guaranteed|sword route, bass -> workingitems/basscast|
|8|(500+100(n-1))/1000, certain n>=6|on_summon -> spawn_blaze -> summon_blaze|
|12|(50+25(n-1))/1000, certain n>=39|damage route -> rtp -> rtp_gas/rtp_gas2|
|15|(50+20(n-1))/1000, certain n>=49|players_1s airborne -> on_falling_second_check -> on_fall only speedy<=-150 -> twilight_sparkle|
|17|(100+20(n-1))/1000, certain n>=46|damage route -> hedgehog_gauntlet -> owned spike|
|22|(2+n-1)/1000, certain n>=999|players_tick -> trinket_effects/tick -> polymorph|
|23|(5+n-1)/10000, certain n>=9996|same player tick -> sunprism|
|24|(40+10(n-1))/1000, certain n>=97|sword route -> psycho_soul|

**Concrete Drojji source defect:** acid_chance computes storage `lbc.math tmp=(150+70(n-1))/1000`, but acid_chance_storage reads `$(tmpd)`. No tmpd producer exists within this trinket chain. Thus actual middle-count result depends on missing/stale shared storage; do not silently call the intended computed formula actual behavior. Root must backlog preserve-versus-correct decision. Bass also has concrete intentional-looking discontinuity: n13=75%, n14=100%, not80%; no smoothing authorized.

## Suppression and reuse boundaries

Counted admissions/chance functions for all nine contain no local stopper/stopper_magic/nomagic/artifact clause. Swords caller requires !legacy and global trinkets>=1; damage/onfall/on_summon/tick have their actual global gates; no blanket magical admission can be inferred. Acid release explicitly tags magic; Twilight horse tags magic; native shared MAG/artifact actor erasure still matters (source artifact nomagic kills magic/magicalsheep within25). Hedgehog spike has no magic/mecha tag in its spawn. Soul particle has slowraycast/player without magic/mecha in its spawn. Polymorph delegates lbcr:raycast_end8_mob (special blackhole-sheep branch for specific transformable succubus potion item), Prism delegates real upgraded Prism rays. Reused native engines have approved suppression/target/role policies: source-local absence is not permission to invent overrides. Root should backlog genuinely new admission decisions. No source assertion that every part is unjammable.

Nether Plug8 is already implemented: `LbcTrinkets.summoned()` counts nether_plug and rolls .5+.1(n-1), calls `LbcBlaze.summonFriend()` (real20HP Blaze at owner+2Y, followrange200, empty loot, native owned/team role). LbcSummons already calls it from actual successful summon paths. Its item/package/catalog exposure is missing; do not implement another summon engine. Existing caller admission can be magic-suppressed as appropriate to original summon; Nether Plug function itself adds no new admission check.

## Further concrete questions/risks for root backlog

- Psycho Soul source picks all nearby nonplayer non-strict-map mobs within24 with HurtTime10 and NoAI=false, then permanently sets NoAI=true plus soul particle. Actual causal target/owner binding must obey standing author rule; this supersedes accidental unrelated nearby capture, and BedWars shop role must stay excluded.
- Polymorph source picks random **any nonplayer entity** within24 excluding magicalsheep, not only living mobs; full accepted conversion/reward cases and native protected roles require deliberate handling.
- Twilight creates real8HP juvenile horse with infinite Slow Falling, saddle/diamond horse armor, rider; timer100 decrements through players_2t while within2 of player. Multiple nearby players could multiply source passes; no full-world idle scan or arbitrary new fixed lifetime should be invented. Source resets owner falltimer.
- Banangay teleport always uses spreadplayers around absolute0,0 radius6 (Nether under125) then +0.5Y; do not assume radius around victim or border-dependent branch, which source comments disabled. Native safe bounded terrain policy needs deliberate reuse.
- Sunprism requires see_sky and eligible enemies below; source performs target-aim and vertical cast independently, potentially both. Do not collapse into single direct damage.
- Hedgehog actual4arrow damage at0.4 contact, owner fallback, particles/sound then discard; full ray/terrain/lifecycle required.
- New suppression policies beyond already approved reused engines require root backlog; no chat/questions added while author sleeps.

No implementation ownership inferred from audit; root approval of scoped next task is still required. Static evidence does not qualify runtime catalog exposure.
