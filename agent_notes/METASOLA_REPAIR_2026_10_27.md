# Radiant and vector metasola repair - 27 October 2026

Radiant type5 now reproduces the original `5_tick1.mcfunction` placement:
`lbc:light_sphere_21x21`, offset(-10,-10,-10), integrity0.1 and strict updates.
The author's integrity1 was an uncertain recollection; the exact source says0.1.
The bundled decoded structure matches the original:2229fire,739lava and1977void_air
cells in21cubed. Geometry, selection probability,200tick activation,20in_fire
WILD damage and the host's0.2downward step are preserved.

The confirmed defect was incomplete strict semantics. `MapManager` set
`StructurePlaceSettings.knownShape` but wrote with flags258; native26.3
`PlaceCommand.placeTemplate` uses818 for strict. Missing known-shape and skip-on-place
write flags allowed neighbor-shape updates and `BaseFireBlock.onPlace` to remove
unsupported fire during the placement itself. They also started lava callbacks.
`placeLoadedTemplateStrict` now delegates to the existing resident-chunk, bounds,
rotation, native structure and lobby-overlay path with
`Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS`. Other callers keep
their existing flags. This preserves the template's intended destructive void-air
mask; it does not replace the source geometry with a new fire-fill algorithm.

Vector type3 keeps its original local sideways/up/forward direction and own-field
centre, with every force component multiplied by0.7:0.0518/0.056/0.056. WILD
targeting,20block radius,100tick activation and source feedback remain.

The old player path set `syncVelocity` every tick. Native tracking then sent an
absolute self-velocity packet; the client replaced its newer predicted jump/input
with stale server velocity. `PlayerForces.add` uses native additive impulse
transport for players. It updates the server velocity and sends only the added
delta, without setting `syncVelocity`. A `ClientboundExplodePacket` is used only
as the vanilla protocol's additive impulse carrier: zero radius/block count,
air terrain particle, empty block-particle list, disabled sound. No server
explosion is created. The exact client handler adds velocity; the air particle
provider returns null, and the empty list bypasses client explosion tracking.
No teleport, hidden vehicle or client mod is involved. Mob forces and ordinary
ghosting retain their prior paths; the existing server component cap10 remains.

Qualified artifact: `build/verification/skywars2-metasola-repair-fe61bfdb.jar`.
SHA256: `fe61bfdb1ae37c71cbd30bf232149e48255891c5aef5ddc857e55321a4d55330`.
Evidence: `build/verification/metasola-repair/`.

The final native run passed30recorded suite results in two actual mode1matches
(61.58seconds). Expanded Soulhunter fixtures verify the exact structure mask,
full strict unsupported fire/lava placement without new native callback ticks,
three independent actual integrity0.1 field samples, exact70percent vectors,
native packet encode/decode, grounded jump/input acceptance, preservation of
newer predicted jump motion, no absolute self-motion or teleport packets,
Greed and Time Stop guards. Existing six-field boundaries/host physics, Soulless
Academy, player rules, Time Stop contacts, effect layers, firearms, FOV and real
match-stop cleanup also pass. Physical client rendering and peak-load TPS were
not measured.

The first full run was rejected for two fixture assumptions. Native empty
sections normalize all-air reads and skip void-air writes, so only air states
are compared as air-equivalent; fire/lava remain exact. TimeStop.begin deliberately
zeros live motion, so pause stability is measured after begin. Production code
was unchanged between these runs. Rejected evidence is retained separately.

All508Java sources,1446classes and3216processed resources match the final build
(fabric metadata matches semantically plus Loom's expected nestedSGUI entry).
All175protected author paths and31live grimoire overrides are unchanged. Child
shutdown was normal and fixture snapshots restored exactly. No RP changes.

Automatic installation supersedes bulk-player4fa80764 and waits for running main
735d9905 to shut down normally. No forced stop/restart. After verified installation,
keep this artifact, preceding qualified4fa80764 and installed735d9905rollback;
retire five older hash/size/path-verified artifact copies.
