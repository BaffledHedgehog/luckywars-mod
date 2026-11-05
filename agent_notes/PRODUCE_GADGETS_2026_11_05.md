# Native produce gadgets - 5 November 2026

## Author rules

> капусту ничем, тыквинатор - МАГ (действующую тоже)

Kapusta is unjammed. Ordinary antimagic and the enhanced artifact deny Pumpkinator
activation and terminate its accepted actor. Targets within antimagic also resist
the magical explosion. TECH does not suppress Pumpkinator.

## Source and implementation

Read-only source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Kapusta: `lbc/function/other/kapusta/` and `lbc/function/projectiles/eggs.mcfunction`.
Pumpkinator: `lbc/function/other/pumpkinator/`, `universal_pos_razb_2.mcfunction`,
`raycastselector_rare.mcfunction`. Both dispatch each active tick through
`tick_not_timestopped.mcfunction`. Neither source supplies a lifetime.

Kapusta keeps eight samples/1.4 blocks per dispatch, owner-excluding contact,
WILD arrow6/radius3.5 and the actual damaging native firework. The primary creates
eight guaranteed inbound children; every explosion also has five independent
20% child chances. Children appear60 blocks away using the source upward direction
range, face the source body and retain its jitter. A fair bounded queue retains
accepted offspring in absent chunks; it never forces those chunks to load.
Persistent typed attachment data replaces tick NBT construction. The fireworks
preset is decoded once. Native display ownership participates in ordinary causal
damage, clone and Planetoid processing.

Pumpkinator uses a logical marker: lowered dispatch origin, both strict source
fills, 1% cactus10 explosion/upper pumpkin stem, then integer X/Z wandering.
Resident writes share existing causal terrain and BedWars protection. Marker
state/RNG and pending Kapusta spawns are typed checkpoint participants, including
partial progress. Existing map transforms move logical origins and pending spawns.
Source `minecraft:projectiles_double` explicitly includes marker/block_display,
so both families retain source x2/x4 behavior without cloning mobs under x2.

Two canonical definition entry names change to native Polymer identities; all
functions, counts, conditions, model/name/lore/style/components remain intact.
Public leaves: `lbc:ported/kapusta`, `lbc:ported/pumpkinator`.
Existing author RP models/translations suffice; source RP and removed templates
were not changed. Bucks stays deferred because its trades require TMT.

## Qualification

Exact test SHA256: `116c76e2ea24bd7ea25e07a1aa1bd7a5c868315f0db220c569793f8cd8cd3c2c`.
Exact production SHA256: `9a01e0573f2b0deb8d8622332e55c9344f522503b1fbf7b54ea3f118cf817cda`.
Evidence: `build/verification/produce-gadgets-2026-11-05/qualification.json`.
Eight isolated native matches: new behavior/replay in modes1 and3, plus existing
clone/vanity and BedWars protection/lifecycle regressions. Tests exercise actual
item input/hand consumption, native fireworks damage, WILD owner/allies, queued
offspring, source terrain, MAG denial/termination, TimeStop, canonical cloning and
the same native entity instances restored by the whole coordinator.

Only10 owned Java families/four resources overlay the qualified baseline archives.
10090 other inputs and275 author paths guarded; every ZIP entry
and retained binary references checked. Both build/libs archives and main JAR
updated through the stopped-server installer with rollback. No main stop/start,
push, physical-client/audio/peakTPS or disk-restart persistence claim.
Existing release gates and full IMBA/TMT-last policy remain unchanged.
