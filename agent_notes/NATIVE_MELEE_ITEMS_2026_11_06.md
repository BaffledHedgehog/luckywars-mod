# Fifteen native melee items — qualified, installed (6 November 2026)

Fifteen existing ready weapons now have canonical native item identities and
Polymer proxies. Existing ClassicMelee, Devil/Pending Swords, PendingCombat,
Hylian, HolyShield, Wands and MetalPipe services still own their abilities. No
extra ability engine, tick service, inventory scan or per-tick NBT parsing was
introduced. The shared source prototype reader runs at item registration.

| Existing ready loot | Native identity |
| --- | --- |
| `lbc:ported/antimagic_sword` | `skywars2:antimagic_sword` |
| `lbc:ported/chaos_sword` | `skywars2:chaos_sword` |
| `lbc:ported/devil_sword` | `skywars2:devil_sword` |
| `lbc:ported/ender_sword` | `skywars2:ender_sword` |
| `lbc:ported/epic_sword` | `skywars2:epic_sword` |
| `lbc:ported/feeding_sword` | `skywars2:feeding_sword` |
| `lbc:ported/fragment_sword` | `skywars2:fragment_sword` |
| `lbc:ported/gravity_sword` | `skywars2:gravity_sword` |
| `lbc:ported/guardian_sword` | `skywars2:guardian_sword` |
| `lbc:ported/holy_shield` | `skywars2:holy_shield` |
| `lbc:ported/hylian_shield` | `skywars2:hylian_shield` |
| `lbc:ported/hylian_sword` | `skywars2:hylian_sword` |
| `lbc:ported/metal_pipe_divider` | `skywars2:metal_pipe_divider` |
| `lbc:ported/piston_sword` | `skywars2:piston_sword` |
| `lbc:ported/vampire_fang` | `skywars2:vampire_fang` |

All 31 canonical source profiles are converted, including all 17 current Fragment
Sword states. The Fragment public wrapper retains its original uniform 17-entry
random selection; native /give uses the original state0 prototype. Every source
conversion changes only the ID. Source count, weights, quality, conditions,
functions, custom data, names, lore, tiers, enchants, banners and special attributes
are preserved. Existing Balance aliases remain distinct vanilla items. No new
public table or full-pool/game-mode admission was added. Old saved raw carriers
continue using the existing compatibility consumers.

## Shared native machinery

The existing deferred prototype factory now builds the actual vanilla carrier
properties before applying authored overrides: sword ToolMaterial/mining rules,
attack attributes, weapon durability, enchantability, material repairs, wooden
fuel and netherite fire resistance. Shields retain the vanilla ShieldItem subtype,
336 durability, banner defaults, wood repairs, offhand equipment, native directional
blocking/delay/bypass tag/block and break sounds. Carrot-based Piston/Divider use
the existing native wand input factory. No pig-riding behavior is inherited.

Three additive native tag overlays preserve the vanilla tag graph: swords,
piglin_loved and enchantable/durability. Existing ice_sword membership is retained.
Original source attribute overrides still take precedence over vanilla defaults.
The actual Piston item input reaches the existing synchronous terrain transaction;
existing Divider craft, both-hand use and timer owners are unchanged.

All models and RU/EN keys already exist in the author RP or vanilla assets, checked
read-only. No RP files or templates were changed.

## Exact qualification

Test `b1b42b54` / production `f4f01ae2`. Full archive hashes and sealed evidence:
`build/verification/native-melee-items-2026-11-06/qualification.json`.
Twelve fresh Java families, 34 exact Gradle-processed resources and 10216 unchanged
source inputs; all 282 protected author paths preserved. Retained binary linkage
passes; production physically excludes MapAuthoring/MapDeletion classes.

Twelve isolated actual native matches passed on the exact test archive:

- Mode 1: native-melee; classic-melee, devil-sword, pending-swords, pending-combat,
  passive-consumables, native-gear, metal-pipe.
- Mode 3: native-melee, classic-melee, pending-combat, native-gear.

The new oracle decodes each canonical table independently on its ORIGINAL vanilla
carrier and compares every component key/value and count with the native outcome.
All 31 profiles, native/default/public identities, declared Fragment random state,
sword/tool/shield machinery, relevant native tags, Polymer and server-codec
round-trips are checked. Existing feature scenes exercise actual attacks, sweep,
signed attributes/effects, reactive chaos/curse outcomes, exact complete Piston
rollback/25-block native input/container/ticks, full old/new Epic/Fragment cases,
normal reforges, Hylian pulses, both-hand native shield use/front/rear blocking,
Holy Shield effects, Divider native craft/input and full timer/TimeStop cleanup.
New native item counters are included in the existing scoped statistic snapshots.

Earlier candidate9a67c0f7 passed all seven feature scenes, but the new oracle
incorrectly expected a singleton Fragment wrapper. The fixture now accepts its
declared cooltype0..16 while comparing every other component, and independently
checks each of the 17 exact source profiles. Source selection and gameplay were
not changed. Rejected evidence remains in the stage; final matches were rerun.

Fixtures stop normally, restore snapshots/auxiliary files and leave no live child,
retained lock or temporary local map. Physical-client visuals, peak-load TPS, full
disk restart and the separate broad rewind gate are not qualified by these checks.

## Release and memory

The qualified test JAR is installed to the stopped main server; both build/libs
files match their qualified archives. Receipt: `build/verification/native-melee-items-2026-11-06/main-install.json`.
No main restart and no push. One heavy task at a time; Gradle512MiB/one worker,
fixture2GiB only above the 3GiB physical headroom gate, one unpacked archive at a
time. Only the confirmed own Serena JDT was stopped; unrelated processes preserved.
Full IMBA, TMT-last, custom/mixed/type pools, mode, skill2 and broad rewind gates
remain unchanged.
