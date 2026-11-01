# FPV free camera, quiet carrier and peripheral HUD — 2 November 2026

User: camera must rotate freely; only the drone's turn rate is limited per tick.
Hide the Vex and its particles, remove controller/hands from the pilot's view,
and add an unobtrusive cyan technological interface only around screen edges.

## Native client cause and implementation

Local26.3 MouseHandler.turnPlayer rotates LocalPlayer. Camera.update reads its
camera entity's rotation; using SetCamera(Drone) therefore makes the Vex's capped
heading the camera heading. The controller now uses self-camera and a private
packet-only empty ItemDisplay seat. Mouse rotation remains local and immediate;
server-bound rotation only updates the desired heading. Existing flight code
still caps the entire 3D angular change at3 degrees per admitted tick. Physical
body position and orientation remain at the launch location for other viewers.

The seat is never added to a server world, ticked, serialized, or indexed for
combat; it owns no chunk ticket. Publish spawn, interpolation metadata, synthetic
passengers, self-camera and presentation in order. Its position compensates the
native passenger/player eye-height offsets so the self-camera remains at drone
eyes instead of jumping about1.2blocks higher on a Vex. One private position-sync
packet per active movement tick updates it; there is no recurring player teleport
or camera angle correction. Existing drone/visual/remote native chunk tracking
and explicit body ticket owners are unchanged.

Native LivingEntity.updateInvisibilityStatus clears constructor invisibility
when effects are empty. The drone now overrides this derived synchronization to
retain invisibility and remove effect particle metadata. This does not add a
potion or interfere with server damage/effect state. The visible authored drone
model remains available to other viewers and stays hidden from its own pilot.

GameRenderer's hand pass uses the client's local game mode, independently of
camera identity. An owner-only CHANGE_GAME_MODE event selects spectator rendering
while attached, hiding hands and hotbar. Do not mutate actual game mode, abilities,
inventory, PlayerInfo or team. In particular PlayerInfo retains the actual mode
(survival in the native fixture), so
the client does not gain spectator visibility of the invisible carrier. Native
abilities are resent immediately after the local mode event to avoid introducing
a client flight/FOV bonus. Server
input/use/inventory/ability gates remain the existing authoritative FPV gates.
On detach restore the actual current game mode and actual abilities, remove the
private seat, and restore self-camera. The one exit body-position update retains
momentum through the existing global TeleportMomentum contract.

## Lifecycle and checkpoint

Only active attachment adds native post effect `lbc:fpv_drone`. Release, collision,
electrical detonation, suppression, disconnect/forget and stop remove only this
effect; unrelated effects remain. Native death ends control immediately; genuine
replacement-body and join hooks remove orphaned FPV state. Saved post-effect IDs
must not activate an overlay on a new uncontrolled connection.

Checkpoint capture keeps authoritative drone/control/aim/body-ticket state.
Detach abandoned presentation without a body teleport. Recreate the transient
seat on publication using the current restored player's ID, self-camera and own
HUD; it is derived presentation and has no saved entity identity. Offline and
replacement player boundaries retain the existing canonical owner checks.

## Resource pack and performance

Apply [`updates/fpv_hud_2026_11_02`](../resourcepack_template/updates/fpv_hud_2026_11_02/README.md)
manually and reload F3+T. Only two new assets are supplied. Actual RP and already
applied/deleted templates remain untouched. Palette matches installed Magnum:
cyan94/193/199, pale180/212/225. Thin incomplete side brackets and fixed translucent
pixel clusters leave the central60% byte-identical at every height. No fictitious
telemetry, centre reticle, blur, dimming, texture asset or animated noise.

Two legal main-only post passes preserve all256 alpha bytes, including existing
black-hole/nuclear transport. One scene fetch per pixel, early exit across the
centre, four fixed decoration iterations on the sides; no history target. Sixty
offscreen RTX4080SUPER checks cover nine resolutions/aspects, final native blit,
exact central clearance and alpha preservation. The inspected preview is a
synthetic scene rendered with the actual shader, not an in-game screenshot.

Server work is active-controller-local. It does not scan inventories/worlds or
read NBT per flight tick. Empty-seat interpolation metadata is prepared once per
attachment. Actual physical-client feel and client FPS still require testing.

## Evidence

Local build/native/GPU manifests, logs, retained failures and qualification live
under `build/verification/fpv-control-2026-11-02/`. Initial compilation caught the
old getter spelling for the26.3 removal packet; corrected to `entityIds()`.
No main-server stop/restart or remote push is authorized by this work.

First native artifact `b2140adc` exposed two issues, both retained in its logs:
unpiloted flight re-encoded approximate-trig Euler angles every tick and drifted
after release; the older Vanity diagnostic cast the current fair SearchQueue to
ArrayDeque. Update orientation only while piloted. Adapt only the diagnostic
iteration/fixture-owned untyped-search removal to existing owner lanes; do not
change the actual ray scheduler or weaken opaque-work rejection.

Final exact test-server artifact: `test-b7f05f27.jar`, SHA-256
`b7f05f27eaa0972cfb05ab9a46a1dab25c315d4c9995cf86b3548066dbf6f06c`.
Production: `production-784cf4d5.jar`, SHA-256
`784cf4d530d5c832771ac1c4c8fa295e6cf7e1922faf75939c4cc4fde7f1c7d5`.
The installation here uses the test-server build. Shared entries match exactly;
production physically omits all8map editing implementation/check classes.

Three native groups passed in three actual mode1 matches: `fpv-drone`,
`vanity-batch`, `fpv-respawn`. These cover native listener/metadata/private seat
publication, exact eye alignment, immediate free-look target with3degree flight
steps, invisible/no-particle resynchronization, owner-only hand/HUD presentation,
authoritative body/inventory/abilities, Shift/forget/short-circuit cleanup,
two checkpoint death/respawn/offline replays, and actual lethal body damage while
attached plus genuine PlayerList replacement and20natural post-respawn ticks.
No manual lifecycle callback substitution or source world ticking was used.

9792 frozen inputs,2473compiled classes and7353processed resources exactly match
the selected test artifact;233author paths and31live grimoire overrides remain
unchanged. All children shut down normally, exact player/configuration/OP/level/
ticket snapshots restored, no retained locks, only allowlisted fixture25577.
Sixty offscreen GPU checks pass. The normal-stop installer receipt is
`build/verification/fpv-control-2026-11-02/main-install.json`; it never restarts
the main server and preserves its previous JAR for rollback.

Receipt confirms `b7f05f27` installed while main was stopped, retaining prior
`10b7c054`; `restarted:false`. No main stop/restart or push occurred.

Next migration candidate audit is retained in the same evidence directory as
`next-port-audit.md`: inventory skills4 Eternal Rock,8 Not RandChanted,11 Parallel
Streams are still missing from the native selectable catalog/consumers. Two
questions were sent in chat about suppressors and copying all versus first7
offhand modifiers. No answers or implementation are inferred from elapsed time.
