# Native mystical artifact closure — qualified, installed (6 November 2026)

Fifteen current artifact definitions now use the existing `skywars2:mystical_artifact`
identity and its Polymer tooltip hook. No extra item class, ability engine, tick
service, NBT poll or inventory scan was introduced. The existing Magic, Reactive,
Jump, Incoming and Skills services retain ownership. Their rules are unchanged.

| Family | Existing ready loot tables | Ability numbers |
| --- | --- | --- |
| Reactive | `lbc:ported/reactive_artifact_1` through `_6` | 9–14 |
| Jump | `lbc:ported/artifact_jump_speed`, `_lift`, `_combo` | 15–17 |
| Incoming | `lbc:ported/incoming_artifact_1` through `_6` | 18–23 |

The eight existing passive artifacts already used this identity. All 23 current
source outcomes now share it, with distinct full custom data. The original 23-entry
artifact pool and its weights/quality/conditions remain byte-for-byte unchanged.
Each of the 15 source edits replaces only the item ID; counts, functions, tags,
name, lore, tier and all other authored values are conserved. Public aliases were
already ready and remain the same. No new catalog, pool or game mode is admitted.
Legacy Balance artifacts keep their distinct original vanilla identities and rules.
Existing raw saved carriers remain recognized by the unchanged compatibility path.

The common deferred prototype reader now supplies the default artifact from its
canonical first source definition. Native `/give` receives its exact original
name/lore/tier/flags/model and the source command-block EPIC rarity. The same
registered item keeps its existing per-viewer Looper hook. All assets and RU/EN
keys already exist in the author resource pack; read-only checks pass. RP unchanged.

## Exact validation

Test `a212eae9` / production `c624b01c`. Full hashes, seven-match evidence and scope:
`build/verification/native-mystical-artifacts-2026-11-06/qualification.json`.
Four fresh Java families, 15 exact Gradle-processed resources, 10239 unchanged
source inputs and all 282 protected author paths; retained binary linkage passes.
Production still physically excludes MapAuthoring/MapDeletion classes.

Seven actual isolated native matches passed on this exact test archive:

- Mode 1: reactive-artifacts, jump-artifacts, incoming-artifacts, looper.
- Mode 3: reactive-artifacts, jump-artifacts, incoming-artifacts.

The established scenes exercise real reactive hits and effect targets, jump input
and native timed layers/expiry, incoming damage and Sand Amulet behavior, source
suppression, TimeStop, legacy variants and cleanup. The jump scene now uses all
23 actual ready loot outcomes, rather than synthesizing passive flags: it checks
the one canonical identity, exactly one ability description for the Looper viewer,
unresolved lore for the other viewer, authoritative Polymer round-trips, exact
server item-codec persistence and knowledge-hiding priority. Default item and
canonical source components must agree. Existing Looper menu/unlock/announcement
and TimeStop checks also pass. Pickup-stat snapshots cover native and old carriers.

An earlier candidate (54bcee50) exposed a mode1-only expectation in the jump
fixture: ordinary BedWars already prevents applying this targeted effect to an
allied mob. The test now verifies the real native alliance and expects that shared
BedWars protection, while retaining the original SkyWars effect and every other
assertion. No ability/target-policy code changed. Rejected evidence remains in the
stage; all seven matches were rerun on the final archive.

Fixtures stop normally and restore snapshots/auxiliary files; no child, retained
lock or temporary local map remains. This does not claim physical-client visuals,
peak-load TPS, disk restart persistence or broader rewind qualification.

## Release and resource limits

The qualified test JAR is installed to the stopped main server. Both build/libs
files match the qualified archives. No main restart and no push. Installer receipt:
`build/verification/native-mystical-artifacts-2026-11-06/main-install.json`.
One heavy operation at a time: Gradle 512 MiB/one worker, fixture 2 GiB only with
at least 3 GiB physical headroom, and one unpacked archive during packaging. Only
the confirmed own Serena JDT was stopped; unrelated user processes were preserved.
Full IMBA, TMT-last, custom/mixed/type item pools, mode, skill2 and broad rewind
release gates remain unchanged.
