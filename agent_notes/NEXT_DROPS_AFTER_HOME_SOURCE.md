# Remaining special drops after home — source audit (22 October 2026)

Read-only source audit. This document qualifies a proposed bounded scope, not an implementation or runtime clearance. Sole changed file is this note. No build, server, source, resource-pack or template writes performed.

## Recommendation

Port the complete normal/old Skrimmer dropped-token pair next. They have no reward, structure, terrain or spawned-actor dependency. Their ordinary feedback already exists in `src/main/java/me/skywars2/game/LbcItems.java:1074` (`screamer`), used by broken totem and psychic wands. Neither dropped token is recognized or dispatched by `LbcSpecialDrops.java:31` / `:86`; the old feedback variant is absent. Thus existing shared feedback is not completion of either acquisition leaf.

Source root S: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Active read-only RP root R: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/`. RESOURCE_PACK.md still names a historical 26.3(1) path; this audit used the active path supplied for this task.

## Exact token presentation

- Normal: S `loot_table/traps.json:579–620` (entry begins before component excerpt): repeating_command_block, count1, weight10, quality-10; tooltip_style `lbc:tier_ff`, item_model `lbc:drop_trap`; name single component `{translate:"trap_screamer",italic:false,color:"yellow"}`; one lore component `{translate:"trap_desc",italic:false,color:"white"}`; custom_data `{skrimmer:1,drop:1}`. Do not add bold or change inherited styles.
- Old: S `loot_table/legacy_luckyblock.json:2396–2416`: barrier,count1, set_components empty, custom_data `{skrimmer_old:1,drop:1}`. No name/lore/model/tier is authored; preserve plain barrier.
- S `loot_table/imba/traps.json:266–285` contains a separate plain repeating_command_block `{skrimmer:1,drop:1,imba:1}` entry (weight0/quality-10). This does not authorize opening the imba pool or silently styling that distinct entry.
- R `assets/minecraft/lang/ru_ru.json:2598`: trap_screamer = `Ловушка: Скример`; `:2609`: trap_desc = `Активный Дроп. Для активации выбросте предмет.` Preserve source spelling. EN at same lines: `Trap: Screamer` / `Active Drop. Throw the item to activate.`
- R `assets/lbc/items/drop_trap.json:1` references model `lbc:item/drop_trap`; R `assets/lbc/models/item/drop_trap.json:1` uses minecraft:item/generated and layer0 minecraft:item/drop_trap. No RP additions are needed for this pair.

## Source execution contract

S `function/drops/specialdrops.mcfunction:29–30`: each matching flag independently selects nearest `@p[gamemode=!spectator]` at dropped-token position, then executes at target. No radius, sight, enemy-only, magic or owner-lifetime condition. Self, allies and Creative players are source-eligible. Current cage target helper `LbcCageDrops.java:63` already supplies nearest player with native role guards; preserve observer/shop exclusions mandated by current AGENTS. Team/WILD treatment requires the existing trap policy decision, not accidental filtering to the next enemy. No eligible target still reaches source shared debit at specialdrops:82–83. Execute normal then old if both flags exist; all other flags retain source ordering; debit one stack unit once.

S `function/drops/skrimmer.mcfunction:1–18`: repeat the five sound commands three times in order: GHAST_HURT pitches0,1; AMBIENT_CAVE pitches0,1,2. All use MASTER, recipient target only, target XYZ, volume1, omitted minVolume (native default0). Then WITHER_SPAWN pitch0 volume1. One CAMPFIRE_COSY_SMOKE packet at target XYZ+(0,1.62,0), delta(0.5,0.3,0.5), speed0.05,count5000, `normal`, recipient target only. Blindness6seconds=120active ticks, amplifier10, hidden particles (`true`).

S `function/drops/skrimmer_old.mcfunction:1–17`: same fifteen ordered GHAST/CAVE commands but volume100000, no WITHER_SPAWN. CAMPFIRE_SIGNAL_SMOKE uses the same offset/delta/speed/count, `force`, target only. Blindness10seconds=200active ticks, amplifier10, hidden particles. Root asked about the unusual volume and the author explicitly retained it; see the user clarification below.

## Old sound volume (user, 22 October 2026)

> Сохранить 100000

This resolves the audit's original open question: retain volume100000 for the old variant, volume1 for normal. Do not normalize it during implementation or later cleanup.

Native existing normal helper uses ClientboundSoundPacket MASTER, exact target XYZ,volume1,pitch; independently seeded sounds; `ServerLevel.sendParticles(player,CAMPFIRE_COSY_SMOKE,false,false,...)` and MobEffectInstance(BLINDNESS,120,10,false,false),owner. This is the closest verified local API example, including target-only normal particle flags. The old particle call must retain force semantics and native default particle-limiter behavior; do not conflate force with broadcast or hand-expand5000 particles. Client rendering/volume/pitch clamping and perceived loudness remain client qualification limits.

## Harm, owner, protections and lifetime

No source damage, knockback, teleport, blocks, mana, cooldown, replacement actor, periodic task or custom restoration exists. Blindness is nevertheless harmful and can indirectly cause fall/void death. Carry actual thrower through effect attribution and impaired-movement causal pipeline; the datapack attribution gap is not desired behavior (agent_notes/AGENTS user requirement). Root is independently inspecting that pipeline. Role guards apply to selection and effects; avoid protecting only damage while still sending hostile feedback to an observer.

The dropped branch has no explicit nomagic guard, unlike psychic-wand call sites which exclude nomagic_active. Neither branch creates a magic-tagged actor. Do not invent blanket magic rejection from those other callers. Existing native negative-effect protection/cleansing must remain effective: LbcItems.usedTotem:1065 invokes normal screamer before vanilla clears death-protection effects; GameChecks:3130 already asserts resulting blindness clearance. Reuse helper without changing those existing callers.

Source item dispatch is under tick.mcfunction:1 -> tick_not_timestopped -> items_tick -> items_tick_act:1; global time stop pauses dispatch. Native shared dispatcher already checks timeStop at LbcSpecialDrops:89. Follow native active-tick status-effect pause behavior and epoch/match cleanup; no source duration extension, owner-death cancellation or invented timer is justified. Stop must not replay queued sounds/particles; already delivered client effects cannot be unsent. Persistent blindness requires existing match cleanup policy, not a new world scan.

## Why not source-order infinity / forest yet

S specialdrops:15 invokes drops/infinity before forest:16. Infinity summons a persistent, glowing, no-gravity reward item with instantitem/infinity_rand, calls random_infinity on pickup, team presentation and nearby wand-cloud drop hooks. `function/other/instants/random_infinity.mcfunction:1` selects14 massive stat/resource outcomes and invokes looper integration. This is a reward/integration branch, not a standalone spawned visual.

Forest `function/drops/forest.mcfunction:1` creates lifetime800 forest/rc_5t/magic marker. raycasters_5t:8 invokes forest_loop, whose random1..12 native feature placement also spawns zombies; outcome7 spawns30HP named reactive_shish with `DeathLootTable:"lbc:reactive_shish"`. `loot_table/reactive_shish.json:1` guarantees reactive_shish_item and has5% end_of_technomagic. Full tree therefore requires those real rewards and their mechanics, terrain feature causality, marker lifetime, anti-magic removal and recurring budget qualification. Do not substitute generic zombie loot or placeholder rewards. Defer it as a complete dependency tree.

## Qualification required after implementation

Exact normal/old token components and RP references; both flags/source order/one debit/no target; nearest self/allies/roles; target-only15/16sounds and normal-vs-force smoke;120/200active-tick hidden blindness and native protections; actual-thrower fall/void attribution and newer-attacker precedence; time-stop pause and real match stop; no regressions in broken-totem/psychic helper callers. Real client sound/particle/presentation checks remain distinct from server packet/effect assertions. Whole traps/legacy/imba pools and unrelated modes stay gated.
