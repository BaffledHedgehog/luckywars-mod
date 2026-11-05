# Native throwable fireball inputs — 5 November 2026

Six existing source abilities now have native Polymer SnowballItem identities
and immediate input into their existing native factory. Flight/hits/landing
are not reimplemented. The source physical-launch policy is unchanged: the
caster's no-magic status does not reject throwing; released bodies retain their
existing type-based MAG behavior and ordinary explosions retain common TECH.

## Source and shared factory

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Source functions: `items/snowballs`, `projectiles/specials`,
`specials_fire_pernul`, `specials_dragon_pernul` and their spread helpers.
The existing `LbcSpecialFireballs` already owns all six factories, real native
flight, actor tracking, dragon-cloud inheritance, TNT landing payloads,
TimeStop, clone/checkpoint and accepted-actor lifetime. This batch closes native
item acquisition and removes the new input's old conversion delay.

Native item/ready-table suffixes:

- `throwable_small_fireball`, `throwable_large_fireball`, `throwable_dragon_fireball`;
- `imba_throwable_small_fireball`, `imba_throwable_large_fireball`, `imba_throwable_dragon_fireball`.

Item IDs use `skywars2:`; ready tables use `lbc:ported/`. Their existing canonical
definitions are shared, not copied. Only their item IDs change; all count ranges,
functions, lore/style/models/flags and original parent weights remain intact.
The large/dragon count ranges legitimately include zero. Native prototypes use
the shared deferred component reader, retaining16-stack vanilla carrier defaults
and original author assets. Dispenser configuration retains native ownerless
snowballs, which have no player-used source conversion in the original behavior.

`LbcSpecialFireballs.use` checks actual gameplay/role/respawn/frozen admission,
configures one unpublished vanilla Snowball for exact native throw motion and
shared SNOWBALL launch scalars, then delegates to `convert`. Only the final
native bodies enter the world before the use handler returns. The temporary
geometry object is never published, indexed or captured as a spell source.
There is no intermediate snowball spawn/destroy packet or pending conversion.
Exactly one used-hand item/stat, vanilla-position NEUTRAL throw sound and shoot
event are emitted; creative use retains the item. Variant/TMT scalar reads use
the existing allocation-free ItemData accessor instead of copying compound NBT.

The source factory remains unchanged: ordinary small/large/dragon produce one
native actor; IMBA small produces1+32 native TNT with fuse100 and source landing
flags; IMBA large has native power127/permanent invulnerability; IMBA dragon
produces1+16 native dragon fireballs. Source signed floor/scaling, offsets,
spread, default gravity and actual player authorship remain on those same actors.
Accepted actors continue after author forget/logout, as before. Existing raw
snowball tokens retain their compatibility pending adapter and TMT marker rule;
this does not enable or qualify the deferred TMT generator.

## Exact-archive proof

Test `82246c4e7e8ae301795364698a436b205eff6a175dfe2215a4ab73148fade377`; production `765939ed2d156e33358ba7c74b7ce26bd441806ef50ff625d887c95481b0edca`.
Six isolated matches pass on the exact test archive:

- mode1: `native-fireball-items`, `special-fireballs`, `projectile-levelups`, `clone-skills`;
- mode3: `native-fireball-items`, `special-fireballs`.

The new check compares24 seeded canonical outcomes per item against an
independent original-carrier codec table, including zero-count empty stacks and
every effective component. It checks give/default equivalence, public wrappers
and Polymer roundtrip. Actual native uses in both hands publish the complete
1/33/17 family synchronously with tickCount0, actual author/epoch and source
TNT fuse100, without a world snowball or pending job. Native debit/stat and the
untouched other hand, frozen rejection and creative conservation are tested.
Existing source tests separately exercise six raw aliases, exact geometry,
power/spread, real SmallFireball hits, WILD allied dragon cloud, protected roles,
full TNT landing payload and released-actor lifetime. Shared projectile upgrades
and cloning pass their existing native suites again.

The verifier correctly treats native loot's retained count0 stack as an empty
outcome, rather than asserting its apparent item ID; earlier rejected proof is
preserved. No failing archive is labeled qualified. Exactly six fresh
Java families/12 resources,10192 other inputs and282 author paths are guarded.
All archive entries and retained binary references are verified. Children exit
normally, exact fixture snapshots are restored and temporary BedWars maps removed.
Evidence: `build/verification/native-fireball-items-2026-11-05/qualification.json`.

Both build/libs are the exact qualified archives. Main SkyWars is stopped; the qualified 82246c4e archive is installed (receipt verified). The previous wait was a false positive: TerraFirmaGreg owns port8125. The installer now uses the target world session.lock, preserving the live-world guard without treating unrelated listeners as SkyWars.
No main stop/start, RP edit, extra indexer or push. Full IMBA/TMT-last/mode/skill2/broad-rewind gates
remain unchanged. No physical-client render, peak-TPS or disk-restart claim.
