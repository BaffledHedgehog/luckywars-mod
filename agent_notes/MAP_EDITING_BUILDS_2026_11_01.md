# Restricted map deletion and separate server artifacts

User,1 November2026: delete a failed authored map by ID and free its menu slot;
deletion may run only as operator `BaffledHedgehog` on this test server. Production
must have the map writer/deleter classes removed.

`/mapsave delete <id_name>` and alias `/mapdelete <id_name>` take the ID printed
after saving, e.g.65 or26b. They delete LOCAL authored records only. Bundled maps
are read-only. A selected or still-resident map must first be replaced by another
fully loaded map; active matches, map work and saves block deletion. No terrain
or live spawns are deleted. The complete map directory is atomically moved to
`world/data/luckywars/maps/.deleted/<id>_<uuid>`, outside the active catalog.
The menu entry disappears immediately and after restart; the old ID is reusable
by the next save. Recovery copies are deliberately retained.

Authorization is checked at command parsing AND inside the deletion implementation:
actual OP2, exact `GameProfile.name()` equal to `BaffledHedgehog`, test implementation
present, and the canonical process directory allowlisted. A privileged command
source cannot impersonate another player's actual OP/name. Display names and tags
do not grant access. The only directories are this installation
`E:/servers_mew/skywars2_last` and its isolated native verification fixture
`luckywars-mod/build/verification/key-reuse-server`. The old server and other
directories are denied. Writing also requires the test artifact and allowlisted
directory; writing remains available to other operators as previously requested.

MapEditing is a small shared interface and disabled implementation. It uses
reflective loading only behind the build/path guard, so production initializes
normally with no writer/deleter classes. Directory/build capability is cached
once per JVM, rather than touching the filesystem every tick. Production has
no pos/save/delete command nodes; the ordinary developer tool/gallery/practice
features remain available to authorized operators.

## Builds

- `gradlew.bat --offline jar` or `productionJar`: production artifact
  `build/libs/skywars2-0.1.0.jar`. Ordinary `build` remains production-only.
- `gradlew.bat --offline testServerJar`: explicit test artifact
  `build/libs/skywars2-0.1.0-test-server.jar`.

Production excludes every MapAuthoring*/MapDeletion* class, including nested
classes and the corresponding checks. The test artifact copies the final native
production archive and adds exactly those classes. Fabric metadata and every
Jar-in-Jar dependency are retained. `tools/check_map_editing_artifacts.py` compares
all shared bytes, checks absence/presence and validates archive integrity.
Use the test-server artifact for this development installation; the normal
production artifact intentionally disables authoring even here.

Deletion validates authoritative ID/manifest/directory correspondence, checks
all existing path ancestors, rejects symlink/junction escapes and unexpected
files, and never constructs a path from an unchecked name. Missing map.nbt is
allowed so a broken template record can still be removed; its manifest must match.
MapManager owns catalog unregistering and retains resident IDs across failed
replacements, so changing only a config selection cannot bypass live-map safety.

## Qualification

Evidence: build/verification/map-editing-2026-11-01/qualification.json,
artifact-split.json and scoped native receipts/logs.

Production000cc6e8 and test25dcef82 both pass native startup and actual matches.
Test: actual BaffledHedgehog identity with/without OP, another real operator,
source-permission impersonation, outside-directory denial, protected built-in/
selected maps, invalid paths/manifests, atomic SW/BW deletion, catalog removal,
restart persistence, recovery records and ID reuse. Native map saving and the
developer tools pass again behind the new interface. Production: writer/deleter
and their checks are genuinely absent, editing command nodes are absent, shared
developer initialization and an ordinary match run successfully.

Exactly8 classes differ between the artifacts;9809 shared archive entries are
byte-identical, including manifest/Fabric metadata and bundled SGUI dependency.
9785 frozen inputs,2462 compiled classes and7352 resources match the qualified
test artifact;226 author paths and31 live overrides remain exact. Four actual
mode1 matches completed. All children stopped normally, exact UUID/config/player/
OP/level/ticket snapshots restored, no locks. The6 owned fixture records were
archived and removed from the stopped fixture only. Earlier seven developer/
gallery/solo/loadout groups remain recorded against predecessor a5a8532a.

The test artifact is queued through the shared normal-stop installer for this
development server. Production is built separately and is not deployed here.
No main server stop/restart, RP/template change, peakTPS claim or push.
