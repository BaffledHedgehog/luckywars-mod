# Lucky pigs — 22 October 2026

Bounded complete acquisition leaf, not activation of whole Lucky Block pools.
Read-only source root S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
RP root R:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8/`.

S `function/drops/specialdrops.mcfunction:43` calls `drops/luckypigs` for
`{applepig:1}`; shared final debit consumes one token. The four summon commands
in `function/drops/luckypigs.mcfunction:1–4` create pigs at token fractional XYZ
offsets +X,+Z,-X,-Z, each one block away. Visible literal name `Lucky Pig` is
gold/nonitalic; `DeathLootTable` is `lbc:applepig`. Other health/AI/age/equipment,
rotation and persistence defaults are native. No lifetime, magic/mecha marker,
owner-death cleanup, taming or target-following behavior is authored.

Token source S `loot_table/barriersnottraps.json:94–135`: weight10/quality5,
count1 repeating command block, tier_c/drop_not_trap, yellow/nonitalic
`drop_apple_pigs`, white/nonitalic `trap_desc`, `{applepig:1,drop:1}`. No ancestor
or entry selection condition exists. Legacy and imba entries remain part of
their gated pools. Native `skywars2:lucky_pigs_drop` retains all presentation
components through Polymer; public leaf `lbc:ported/lucky_pigs_drop` yields one
unconditional native prototype. Whole source selection weights stay on source
pools; no acquisition pool is opened by this leaf. R already has model and
RU/EN/RPR keys (`Дроп: Lucky-свиньи` / `Drop: Lucky pigs`), so no RP/template write.

S `loot_table/applepig.json:1–38` is a complete vanilla reward closure: one roll,
enchanted golden apple weight1/quality3 versus golden apple weight10/quality0,
count1 each. Native death loot retains Minecraft luck-sensitive weighting and
loot context; do not substitute a fixed1/11 probability or grant extra pork.
Existing native resource `lbc:applepig` is modernized to26.3 modifier/type keys
without changing the authored reward. This internal table is not a new public
test leaf.

Native `Pig.thunderHit` calls `Mob.convertTo` outside Peaceful. The26.3
`ConversionType` copies name visibility, entity tags and custom name/data, but
does NOT copy Mob's custom loot table. Therefore lightning-converted piglins
retain Lucky Pig presentation and epoch but use ordinary native piglin loot.
The pre-insertion shared conversion hook carries only causal/cleanup state;
no reward override, equipment, anger, AI or persistence change is introduced.
Ordinary offspring remain ordinary native pigs without these tags/rewards.

`LbcLuckyPigs` preflights all four resident destination chunks before shared
dispatch/debit, uses native pigs without finalizeSpawn and resets constructor
rotation to summon-default zero. It tracks admitted pigs and converted piglins,
retains actual thrower across same-match unload/reload, and quietly discards
stale actors on load or match stop. Stand uses native frozen-entity handling;
no additional actor scan or per-pig projectile/timer is introduced. Native
conversion targeting/melee and damage retain role/NPC/ally safety. As with
ordinary Creeper drops the thrower remains physically eligible; no team is
assigned and ordinary neutral AI is preserved. Converted attacks inherit the
thrower's kill provenance through `LbcIndirectKills`, with existing later-hit
precedence.

Enzan was initially staged while awaiting the extreme-amplifier decision. The
author subsequently approved Levitation128 for3seconds; it joins this batch as
a separate complete leaf, documented in [ENZAN_DROP_PORT.md](ENZAN_DROP_PORT.md).
Infinity/forest still require reward closure; whole source pools stay gated.

Build and two actual isolated matches passed on3236fe1c:72 suite results,79.24s,
including Enzan and all prior drops.178sources/526classes/2671resources match
tested artifacts. Ready build/verification/skywars2-enzan-pigs-3236fe1c.jar,
not installed; evidence build/verification/enzan-pigs-final/qualification.json.
Main96ee50a7/user Reroll/RP/template unchanged. Native combat apple loot, seeded
quality/luck, real thunderHit conversion and ordinary piglin equipment loot,
role/allied targeting/melee, causal kill statistics, native offspring factory,
same-match load callbacks, time stop, epoch and real quiet stop are checked.
No real client rendering, disk restart/unload, natural breeding or capacity
qualification is implied. Reproduce combined checks through
`python tools/smoke_enzan_drop.py --server-dir build/verification/key-reuse-server`.
