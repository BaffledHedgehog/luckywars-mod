# Cunny native closure audit - 4 November 2026

Read-only bounded audit in E:/servers_mew/skywars2_last/luckywars-mod; only this new note written. Legacy E:/servers_mew/cringe1_21_4_skywars is read-only. No Java, resources, tests, builds, server, git or RP changes. Root retains release qualification. Read AGENTS, agent_notes/AGENTS, Serena initial instructions, NIGHT_REMAINING_AFTER_ANSWERS and NIGHT_REMAINING_IMBA_SOURCE. This is source evidence, not runtime proof.

Author ruling33 is binding: Cunny permanently destroys Chef. Preserve all other Chef rules and indirect-kill attribution. TMT LAST. No arbitrary mcfunction interpreter. Earlier unanswered-question wording is historical and does not revoke the author ruling.

Paths below: L = legacy world/datapacksdisabled/LBC/data/lbc; F=L/function; S=legacy world/datapacksdisabled/swarogskywars/data/swrg/function; J=src/main/java/me/skywars2/game.

## Entry, acquisition and debit

L/loot_table/imba/slabs.json produces jungle_slab with magicwand/raycastspell/cunny_slab and max_damage64. The prior high-quality audit establishes real reachability through lucky_items_high_quality -> luckyblock_imba -> imba/lucky_items_imba -> imba/slabs. That broad sampler must not be replaced with a reduced ready subset.

F/other/imba/cunny_slab/cast has its ray recursion commented out: live body clears ALL cunny_slab inventory matches and spawns four jungle markers at cardinal offsets5. end clears all and makes one marker, but no live local cast->end edge exists. A bounded exact call search did not establish a literal live external call to cast; generic dynamic ray dispatch needs confirming before claiming ordinary input activation. Do not invent click admission from lore.

raycastselector_imba:10 routes jungle actors to tick: mega_jungle_tree feature, one of16 template/rotation choices (sob four weights; angry/splurt/sweat four rotations), four correctors, then spreadplayers radius30 under300 and chance1 global jungle-marker removal. Each corrector offsets integer X/Z independently -10..10, tries barrel one below only replacing airs, chooses12 woods/16 rotations and two uniform100 pasta entries for waxed glowing sign faces. The100 pasta files are finite string data, not100 abilities. Templates/feature/sign writes require shared terrain ownership and BedWars protection; do not create one entity per projected cell.

L/loot_table/imba/cunny_slabs.json contains exactly22 equally listed variants cunny0..21, uniform rolls1..8 and bonus_rolls0..8. All have magicwand/craftusable, source item model/lore and max_stack_size99. Both main/offhand activation are live: F/workingitems/spell1_default:100 and spell1_default_left:84 -> cunny_item/select or select_left -> select1. Selector reads the used hand; terminals clear ONE inventory match for that variant, which can clear a different slot. Native policy should debit the accepted used hand once, retaining output-before/after-reset semantics deliberately. No local terminal mana/cooldown/jammer check exists; outer native MAGIC admission must follow established author policy rather than silently adding terminal gates.

## Exact finite outcomes

Sources are F/other/imba/cunny_slab/cunnys/<number>.mcfunction and named helpers. There are12 active effects (0..10,21), not22 implemented powers.

| Value | Executable source effect | Native closure requirement |
|---|---|---|
|0|Calls swrg:game/start/0; broadcast; consumes1|Full restart semantics described below; cannot substitute Supernova replacement|
|1|Delete all four armor slots of other players distance>=0.01 outside same team; particles|Canonical inventory/equipment mutation and protected-role policy|
|2|Named modifiers +6armor/+3toughness/+6attack, -50%scale/+50%maxhealth/+20%speed; grants inf_shulker,inf_steak,evolution_sword|Existing attribute owners, food, infinite shulker, evolution sword; preserve modifier idempotence|
|3|Infinite regenerationIII plus expanding grassifier|Virtual fair terrain frontier; each step seeks surface, converts one block and branches four neighbors; lifetime is not finite cell count|
|4|20 summon calls around caster, including duplicated cardinal distance2 positions|Six source cultist Loli presets, team/source ownership and shared summon trinket event|
|5|Grant imba/nahida_dakimakura|Actual use_nahida advancement -> sustained plapping. Start -100%maxhealth/maxabsorption/attack/camera; each use reward random_attrs_up26; tick removes debuffs if advancement not renewed, then revokes advancement|
|6|Random nonspectator enemy tagged love_girl; caster girl_seeker|Shared causal pair state; airborne target motion+.1 toward nearest seeker,2tick particles; MAG stopper checks in visual seek helper only. No partner expiry is defined here; leave_death removes tags|
|7|Spawn named anal whip at every other nonspectator player; embed target UUID[0]|Reuse LbcChaosRewardItems whip engine with intentional conversion to canonical full target UUID; existing engine uses skywars2.whip_target|
|8|Spawn cunny_slabs once; every player twitching+60/cumming+160 and one of6 sounds|Shared effects/movement/sound clocks; source global player scope must be reconciled with protected roles|
|9|Random integer X/Z ?1000000, teleport Y320; platform marker at absoluteY100; slowfall60seconds;23 rolls cunny_slabs|Shared ticketed teleport and bounded33x33quartz platform atY100, retry until fill success. No random_glack call|
|10|Give luckyblock_all_stacks,op_hoe,god_dice; kill one random entity|Canonical loot owners, finite actual candidate selection; Chef must use LbcWtf.erase(Entity), never ordinary death/knock|
|11..20|TODO comment, tellraw to self, clear1|Exactly inert consumable outcomes. Do not invent missing abilities or exclude their probability|
|21|At up to64 random entities call the same cultist summon helper|Same six presets/source ownership/trinket hook, bounded selection; not64 arbitrary functions|

F/cunnys/4_spawn_cunny_spawn has the six complete mob presets: strangeness30HP,chaos50,luck40,omnipresence40,obsession10,hypocrisy60, distinct native equipment/scale. LbcSummons already has all six CULTISTS; an intentional preset/scale adapter is needed for these Loli variants, not a second AI engine.

## Required loot closure and real dependencies

A read-only JSON walk of nine required roots (imba/cunny_slabs,inf_shulker,inf_steak,evolution_sword,god_dice,op_hoe,luckyblock_all_stacks,anal_whip_named,imba/nahida_dakimakura) follows literal minecraft:loot_table references:12 distinct tables,zero missing. None of these12 contains random_glack/time_clock/time_fragment/tmt text. This proves only the finite table-reference closure; item behavior closure still requires consumers above. Recursive shulker contents are owned by existing lazy native engine, not an excuse to recursively materialize all possibilities.

Outcome0 is the genuine wide dependency. S/game/start/0:14?16 resets all attributes, calls lbc:resets and end_resets;50?51 branches to lbc:start for mode2 and start_bedwars for3..4. It reassigns teams, resets lives from settings, adds10 permanent coins/player, schedules start/timer and downstream kit/skill/trinket selection. It teleports all players through lobby coordinates and clears lobby entities. A same-epoch arena replacement preserving lives/loadouts is materially different. Modes2/4 and skill2 remain gated; a full literal legacy restart must not enable them as a side effect.

An active-line static literal function walk from swrg:game/start/0 visits226 names; one unresolved literal swrg:gui/fill remains. This walk is conservative (conditional branches included) and incomplete for dynamic macros. It reaches lbc:config -> drops/tmt/set_storage -> names/nbttags via lbc:start. These are TMT dictionary initialization calls, NOT evidence that restarting executes the TMT item. No random_glack invocation was established in this walk. Do not label Cunny directly TMT-blocked because those dictionaries contain its names. The demonstrated blocker is defining/implementing a typed native global restart contract across admitted modes and existing permanent profile transactions; TMT runtime is not established as a prerequisite.

## Smallest coherent native implementation

Reuse canonical item identity/hand dispatch and actual source loot roots. Add one Cunny owner with typed pending effects/RNG and no per-tick NBT scanning. Snapshot virtual jungle/grass frontiers, pair state, Nahida use phase, teleport/platform cursors and accepted effect/debit latches through existing whole coordinator; TimeStop pauses all active clocks/work. Effects must carry causal owner through summoned mobs, motion/fall/void and delayed damage. x2/x4 adapters need the same typed semantics, not function strings.

Concrete existing APIs: J/LbcAttributeGains.apply(player,choice) already implements the exact26-way numeric random_attrs_up (roll uses world RNG; choose deterministic owner RNG then apply for replay). J/LbcSummons owns six cultist engines and native orders/source tracking. J/LbcChaosRewardItems owns anal whip and target component; source UUID[0] is not its native identity format. J/LbcWtf.erase(Entity) permanently removes Chef tickets/brain/survival/body. J/LbcInfiniteShulker, LbcEvolutionSwordItem and food lifecycle already own their outputs. Shared EffectTerrain/MatchTerrainCheckpoint/fair work and native structure owners should own jungle/templates/signs/platform/grass; no synchronous global terrain fill or per-cell entities. GameController lifecycle and canonical profile coordinator must own outcome0, never a private Cunny match/economy state.

Actual missing Cunny consumers were confirmed by bounded Java exact-token search: no cunny_slab/dakimakura/girl_seeker/grassifier/plapper references. Imported item definitions/models alone do not close them. Existing building blocks substantially reduce implementation work but are not a qualified whole family.

## Decisions and limits

No new question is needed about Chef or TODO11..20: author33 and executable inert source are clear. No blanket TMT postponement is justified by the direct table graph.

Outcome0 genuinely needs the author to choose native restart intent if no previous global rule supplies it: source rerolls teams/resets lives/loadouts/rewards10coins and can reach unavailable modes; retaining current native participants/admitted mode and restarting through normal match lifecycle changes literal source behavior. Keep this uncertainty distinct from technical work that can proceed for all other outcomes. Extreme-range teleport/platform and unbounded grass/jungle frontiers should use established fair work, bounds/tickets/protection rules; do not present their implementation choices as source durations or finite total cells.

This audit does not qualify the full high-quality sampler or release Mellstroy luck. It establishes a concrete Cunny work package and one broad restart boundary; no native input, client, rollback, peakTPS or installed-artifact claim is made.
