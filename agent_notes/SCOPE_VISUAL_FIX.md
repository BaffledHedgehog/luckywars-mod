# SLR/Magnum scope, SLR ammunition HUD and minigun beam - 23 October 2026

This patch implements the user's three weapon fixes. It does not reopen the
deferred old minigun duplication investigation or change its model animation,
damage, ammunition, cooldowns or inventory ownership.

## Rifle projection and scope overlays

The user allows FOV as a fallback to camera movement and retains the same
`lbc:zoom` effect for future crosshair/circular scope shading. Scope input remains
the previously selected left-click toggle, including offhand rifles and native
duplicate/mining-stream suppression. No client mod is required.

Local 26.3 sources are authoritative: `Camera.positionCameraAtEntity` only reads
`Attributes.CAMERA_DISTANCE` when the client selected a third-person camera.
There is no first-person camera-offset/FOV packet. The normal camera-distance
attribute does not provide an optical scope; moving a surrogate camera would
change the viewpoint relative to actual server shots. Mojang also documents
[camera_distance as third-person only](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-6).

`LbcRifleZoom` publishes a per-player immutable movement-speed snapshot for the
outgoing packet hook. `RifleZoomPacketMixin` changes only the walking-speed field
of scoped `ClientboundPlayerAbilitiesPacket` instances to -2 times the current
movement attribute. The exact vanilla `AbstractClientPlayer` formula then gives
`(movement / walking + 1) / 2 = 0.25`. This is a client FOV denominator, not a
negative authoritative speed: no player ability, attribute, saved data,
position or velocity is modified. Native ability flags and flying speed are
copied from each original packet; native resends retain the scoped projection.
Attribute changes refresh this packet, without sending a packet every tick
when the snapshot is unchanged. Exit restores current authoritative abilities,
not an old saved snapshot. Zero/nonfinite or unrepresentable movement values
leave the native packet unchanged instead of emitting nonfinite values.

The client's FOV Effects setting remains authoritative: at100% the angular FOV
factor is0.25, at0% the native mechanism is disabled. Flying keeps vanilla's1.1
factor. The fixture uses the exact client arithmetic; it does not claim GPU
rendering, real-client preferences or compatibility with arbitrary client mods.

Entering adds `lbc:zoom` and cancels `lbc:zoom_out`. Clicking out or unholding
the scoped stack restores native abilities immediately and adds `lbc:zoom_out`
for6 server ticks (0.3s at20TPS), then removes only that effect. Reentering cancels
the outgoing animation. Death, removal, dimension/role exit, disconnect and
actual match stop clear both rifle effects and release the scope snapshot.
Other post effects remain untouched. Active World Time Stop blocks new input;
the scope service still handles cleanup before the item-tick early return.

### Resource-pack handoff

The actual RP remains read-only. Its current zoom and zoom_out JSON both still
have `ZoomFactor=4`, and Duration0.25. Using that crop together with native FOV
would multiply magnification. The two template JSON preserve the actual passes,
directions and animation targets, changing only ZoomFactor to1 and Duration
to0.3. The user applies them or merges those two values into their newer custom
scope artwork. No PNG, existing shader source, language key or original RP file
was rewritten. See [template instructions](../resourcepack_template/README.md).

## SLR ammunition HUD

The existing E108 arrow row appears while holding SLR in either hand, including
flagged source stacks. SLR-only counting matches its real native ammunition
predicate: `minecraft:arrow`, including custom/infinite-arrow components; tipped
and spectral arrows are not usable SLR ammunition. Bow/crossbow broad ARROWS-tag
display remains unchanged and takes priority when mixed with SLR in two hands.
Counting uses the existing native counting-only inventory/crafting/cursor path
without mutating items. The established source ordering, camera-owner HUD,
completed-component cache and ignorance suppression remain intact.

## Laser Minigun display

Source `LbcLaserMinigun` created a virtual display with `ManualAttachment` and
ticked its holder, but never called `startWatching`. Installed Polymer
0.18.2+26.3 intentionally leaves ManualAttachment tracking-update methods empty,
so no viewer received any spawn/model/transform packets. The fix explicitly
enrolls nearby connected viewers and removes stale connection/world/range
watchers, both initially and during visual ticks. Frozen beams still update
viewers without aging. Holder destruction releases all viewers normally.

The actual read-only RP model `lbc:laser_minigun_ray` and texture were verified:
model bounds[7,7,0]..[9,9,8]; its cyan(51,220,255) alpha254 texture identifies the
unlit/fog-independent shader/bloom path. Native ItemDisplayRenderer's Y-pi
rotation makes the half-block model extend forward from the muzzle when scaled
by twice the hit distance. Those geometry, markers and NONE display context
remain unchanged. Source width starts0, then.20/.16/.12/.08/.04 and removal on
the sixth active tick. No particles substitute for the authored item display.

## Validation

`RifleZoomChecks` captures actual outgoing native ability packets and checks
projection arithmetic, unchanged authoritative state, current-state restore,
native resends, attribute refresh, input and six-tick overlay transitions.
`MinigunVisualChecks` captures actual outgoing virtual display spawn/metadata/
removal, model, orientation, native render geometry, fading, late viewer entry
and removal while paused. `UiWeaponBatchChecks` includes SLR inventory/crafting/
cursor counts, live updates, both hands, camera view and ignorance suppression.
The combined two-match runner is `tools/smoke_scope_visual.py`; final evidence,
installation status and its limits are appended after qualification.

## Final qualification

Artifact `build/verification/skywars2-scope-visual-ca582dcc.jar`, SHA256 `ca582dccd9435910597426eeed78d1ffcfbb9d41b70c12c47015394102961075`. Offline build plus two actual
isolated mode1 matches passed, 166 recorded suite results in
132.78s, including native rifle ability packets, minigun
spawn/metadata/removal and current SLR ammunition HUD. All prior154 recorded
results are retained. Exact source/class/resource byte checks and stopped test
server/lock release are recorded in ignored local scope-visual-final evidence.
Installed to main mods/skywars2.jar while stopped; rollback 3cb2c7d1 retained. The agent did not start the main server.

Retained attempt1 failed a fixture assertion that compared player.getSpeed()
to its old value after the fixture intentionally changed MOVEMENT_SPEED.
The corrected assertion captures the native changed input before zoom refresh
and checks that projection preserves it. Attempt2 exposed stop-probe interference from earlier dimension-freeze probes;
the final rifle stop probe uses a temporary native Stand-immune actor and keeps
the dimension frozen, preserving all pending-drop stop probes. Attempt3 exposed an overprecise comparison between the renderer's continuous
JOML trigonometry and Vec3's truncated sine lookup table at random ray angles.
The final geometry oracle uses analytic renderer direction at the same strict
tolerance. No production zoom/beam behavior or check requirement was weakened. Final logs contain only the documented pre-existing
fake-player Polymer packet-context diagnostic pair and no unexpected errors.

The native FOV formula and packets are tested, not real GPU output. No actual
disk restart/unload/disconnect, arbitrary client mods, real mouse sensitivity,
client-option migration or peak-capacity benchmark is claimed. The actual RP is
read-only; its required handoff is the supplied template and README.
<!-- scope-visual-qualification-end -->
