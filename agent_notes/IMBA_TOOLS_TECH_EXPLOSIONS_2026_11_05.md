# IMBA tools and TECH explosions - 5 November 2026

## Author rules

> Лопату ничем не подавляй, мотыгу ТЕХ (вообще ТЕХ любые взрывы к слову должен подавлять, т.е. любой нативный взрыв также должен подавляться ТЕХ)

The author selected complete origin cancellation plus protection for entities and
blocks inside TECH from an outside explosion. This latest global rule supersedes
older per-item explosion exemptions: cabbage flight remains unjammed, but its
explosion now follows TECH. Pumpkinator remains MAG-suppressed; TECH affects its
explosion only, without stopping ordinary pumpkin growth. MAG alone does not
suppress ordinary native explosions. No new amplified-artifact exception added.

## Source and native owners

Legacy source is read-only under
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
`lbc:imba/lucky_items_imba` references `imba/diamondtools`; the two source pools
each roll uniform0..1. Canonical definitions retain count1, Efficiency255, source
golden models, original names/lore and shovel tierS/hoe tierF. Only canonical
entry identity changes. Native material/tool properties match current vanilla
netherite tools. Dynamic enchantments are resolved by canonical loot, not during
registry bootstrap. Ready leaves: `lbc:ported/imba_shovel`, `lbc:ported/imba_hoe`.

`other/imba/chaos_sho_use.mcfunction` contains28 eye-local origins repeated15times
in authored order. All420 rays and their preceding mutations remain meaningful.
Each has201 samples at.33 spacing and source15% redstone particles; the existing
native Piston Sword shift owner preserves containers, atomic rollback and causal
terrain. A narrow overload binds the accepted world/cause independently of the
player's later dimension. Shared fair terrain work retains ray/sample/RNG in the
existing checkpoint graph; no new tick registry, whole-NBT polling or forcedloads.
Unavailable block samples fail as in native execute conditions, then the finite
201-sample ray continues; absent chunks never leave an indefinitely deferred job.

`other/imba/chaos_pic_use`, `chaos_pic_use_rc`, `dickcockgalactic` grant hidden
ResistanceV20ticks and visit301 samples, positions0..300 at step1. Each non-air
sample creates owned native fuse-1 TNT with zero initial motion, then independently
has70% chance of destroying the sampled block with native drops. The post-write
air check determines continuation. Shared tracing owns scheduling and TimeStop;
short pending traces cancel on rewind under the author's existing simplification,
while already-spawned native TNT restores through canonical bodies/spell authority.
Both tools use actual break/melee hooks, including Unbreakable/last-durability
actions. Repair and unrelated equipment durability do not trigger them.

## Shared explosion protection and cost

ServerLevel publication and direct ServerExplosion entry both reject protected
epicenters before work. Filtering happens before native block/fire interaction and
before entity damage, impulse or player knockback packets. Fireworks have their
own native detonation/damage hooks. Manual cabbage damage/offspring, Pumpkinator
explosion damage/stem, nuclear terrain, nuclear Wither and instant removal share
the same origin/target/cell protection. Ordinary non-explosion abilities unchanged.

One operation captures scalar centers from already-loaded fields. Empty coverage
uses a constant fast path. Only actually queried chunks cache nearby centers,
using primitive long keys; per-cell checks allocate no vectors and perform no NBT,
world scans or chunk requests. Long nuclear writes refresh coverage at application.
The focused fixture measured paired small native explosion scenes:

- 4982ad59-imba-tools-results.json: 96 blasts per variant; no TECH 62.703ms; active outside TECH 56.115ms.
- 4982ad59-imba-tools-mode3-results.json: 96 blasts per variant; no TECH 84.110ms; active outside TECH 63.110ms.

These are scoped timings, affected by JVM warmup and the small scene; they do not
establish a peak TPS guarantee or a precise general overhead percentage.

## Qualification and deployment

Test SHA256 `4982ad59b1cb01d79891329c3ae2d7b550bf25912e57223ae34b75d00e54f45e`; production `29de5a7adfd9872920699176aea1ff1b70fcca4e4826b341d44a7f9b2fd03735`.
Evidence: `build/verification/imba-tools-2026-11-05/qualification.json`.
12 isolated native matches: both new suites in modes1/3, prior produce behavior
and rewind, Piston Sword, nuclear terrain/event, shared arrow explosions, plus
BedWars protection/lifecycle. Real tools, actual native TNT/direct native blast,
outside/inside protected targets/blocks, MAG control, partial terrain queue replay
and SAME native TNT entity replay are checked. No physical-client/peakTPS claim.

18 owned Java families/five resource inputs overlay the qualified baseline;
10089 other inputs and275 author paths guarded. All archive
entries and retained binary links verified. Both build/libs and main updated with
rollback, no main stop/start or push. Author RP and removed templates untouched.
Full IMBA/TMT-last, modes2/4, skill2 and broad rewind release gates unchanged.
