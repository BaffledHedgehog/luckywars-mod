# Native IMBA pearl - 5 November 2026

The canonical source minecraft/ender_pearl__db2c64fce6b9 now uses
skywars2:imba_ender_pearl. Public lbc:ported/imba_ender_pearl references that
same definition. All source count, functions, model, name, style, lore and flags
are unchanged; the source Snowball carrier and its throw/debit behavior remain.

Source, read only: LBC/data/lbc/function/projectiles/specials,
specials_ender_pernul, specials_ender_pernul_rand and rand_motion_razbros.
The source creates seventeen pearls, not sixteen: one primary plus16 siblings.
All are emitted synchronously at the source snowball position. Primary velocity
uses floor(component*10000)*0.00025. Each sibling axis independently uses
(floor(primaryVelocity*10)+uniformInteger(-10,10))*0.1. Native gravity remains.

The existing special-projectile factory now has seven variants. It owns all
seventeen final native ThrownEnderpearl bodies, with no world snowball or pending
conversion from the typed input. The actual thrower replaces the source's nearest
player workaround. Native pearl ownership, player ledger, collision, teleport,
damage and chunk tickets remain native. Existing source throw upgrades and sole
used-hand item/stat/event/sound handling remain shared. No new tick service,
NBT polling or parallel implementation was introduced.

Existing native pearl MAG classification is retained; no new throw gate or policy
exception is added while the optional suppression question is pending. Pumpkin
and bucks are deferred; bucks requires the complete random_glack/TMT generator,
which the author explicitly placed last. Do not publish incomplete trader stock.

## Exact-archive proof

Test `eae198e599a7c546c07810db9e3e20ab529cfff2e07f20c3f0794f2cce80dd29`; production `5fe890960755d8bc5a99fbe18d11da244458e5aa65a78de9618e86a46c4932b3`.
Six isolated matches pass on the same test archive:
- mode1: native-fireball-items, special-fireballs, projectile-levelups, clone-skills;
- mode3: native-fireball-items, special-fireballs.

Native input checks all seven canonical throwables against an independent
original-carrier loot codec, including exact components/counts, ready references,
give defaults and Polymer roundtrip. Both actual hands emit final bodies before
returning, with one debit/stat and no intermediate snowball. Existing frozen and
creative controls remain. Dedicated pearl checks cover exact17 geometry, native
ledger, TimeStop saved/resumed velocity, x2*x4 (eight root copies plus16 original
siblings), same-body private service checkpoint links, real block contact with
native owner teleport/damage, and complete native ledger cleanup. The impact positive
control temporarily enables/restores vanilla FALL_DAMAGE: the initial map protection
window also suppresses native ender-pearl damage through IS_FALL. Source six-way
fireball regressions, shared source launch upgrades and general cloning pass.
Private link replay is not a new whole-world rewind qualification.

Rejected archives and failed proof remain separate. One fixture failure was an
edge-aligned test trajectory missing its block after native gravity; the target
was corrected to its center. An interrupted bootstrap exhausted Windows commit
limit (native mmap1455) before a match; its crash log and exact57-file plus15-file
auxiliary fixture recovery are retained. Failed runs are never marked qualified.

Four fresh Java families/two resources;10219 other source inputs and282 author
paths guarded. Every archive entry and retained binary reference checked.
Children shut down normally; exact native/auxiliary snapshots restored; temporary
mode3 maps removed. Main/both build/libs installed without main stop/start.
Evidence and receipts: build/verification/imba-pearl-2026-11-05/.
No author RP edits, extra indexer or push; idle Gradle stopped. Full IMBA/TMT-last,
mode, skill2 and broad-rewind gates unchanged. No physical-client, disk-restart
or peak-TPS claim.
