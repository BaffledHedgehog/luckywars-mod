# Black-hole owner lifetime and manual visual replay

User report (26 October2026): old black holes remain invisible after death and
respawn. Newly created holes have not been checked (user); visibility to other
players is not established. User confirms the hole itself survives and keeps
working; Carpet player deaths did not break the local player's view. This
release repairs a confirmed visual-owner lifetime defect and replay handshake
gaps without changing black-hole gameplay lifetime or claiming client pixels.

## Confirmed stale-owner defect

`BlackHoleVisuals.Handle.owner` retained a pre-death `ServerPlayer`. The renderer
independently removed handles when that object died or was removed. UUID-based
producers can continue with the replacement player, then keep calling `update`
on the original nonnull handle. That handle was already unregistered, so its
visual stayed gone permanently despite functioning gameplay. Sheep holes do
not rebind their visual owner; arcanum rebinding can also be delayed by Time
Stop. The prior fixture used an unaffected foreign owner and missed this case.

The renderer now validates the match epoch; the producing mechanic owns the
hole's lifetime and explicitly removes its handle. Visual rendering no longer
interprets a transient owner object as an independent gameplay lifetime.
Disconnect cleanup matches owner UUID, including stale owner objects. Explicit
removal stays final: a later geometry update never revives a removed handle.
Existing source cancellation, shared-hole owner rebinding, epoch and stop
cleanup remain authoritative. No extra entities or new periodic scans are added.

## Corrected handshake

The prior tracking repair polled native `hasClientLoaded()`. Exact26.3 source
shows this also becomes true when the60tick timeout expires without an ACK.
Real dimension changes do not restart that timer, so the flag can already be
true immediately after a world reset. A reset could therefore be consumed before
the client acknowledged loading, with no replay left for the eventual ACK.
The old fixture manually acknowledged each transition before checking replay.
See [the source and Polymer audit](VISUAL_RESET_AUDIT_2026_10_26.md).

Pending resets now have an explicit not-ready/acknowledged state keyed by
connection identity. A new `ManualVisualLoadMixin` observes the actual native
`handleAcceptPlayerLoad` callback. Only that acknowledgement and native loaded
state permit replay. A newer reset revokes an earlier ACK; duplicate and idle
ACKs do not cause repeated respawns. Nested bundled Respawn packets also register
resets. Disconnected/replaced listener entries are removed through actual player
and connection identity, without building an all-player set on every waiting tick.

After ACK, currently subscribed live manual holders in the player's current
dimension are re-paired with full current spawn/metadata. IDs, shader slot colors,
scales, positions and effect ages are retained. Holders destroyed during loading
are skipped; newly created holders and changed metadata are read at replay time.
Unrelated viewers do not receive duplicate spawns. The registry covers black
holes, nuclear effects/fades, border, laser beams, Terra/Sauvojen/special-tool
manual models and enhanced glow carriers. Ordinary entity attachments continue
using native tracking.

One INFO line after each accepted pending reset records the player and number
of restored holders: `Manual visual load acknowledgement for ...: restored N holders`.
It distinguishes a replay from no remaining carriers if another report occurs.
Same-dimension vanilla respawn retains other client entities; actual dimension
changes recreate the client world. The earlier documentation's assertion that
every respawn clears the whole client entity world was too broad.

## Verification

`VisualReloadChecks` retains genuine lethal damage and native respawn, then
exhausts the native load timer without acknowledging and requires no replay.
It also retains a hole owned by the dying player with no visual-owner rebind,
checks that its original handle/slot survives player replacement, and updates
its center, radius and accretion-disc model through the real metadata send path.
It acknowledges through the real native callback and checks all five original
carriers, full metadata, exactly one spawn each, unchanged slots/scale and an
unaffected viewer. Direct/nested bundled resets, coalescing, revoked old ACKs,
duplicate ACKs, destroyed/late holders and changed item metadata are included.
Actual dimension roundtrips check the already-true native loaded flag before
ACK. Explicit producer removal, non-revival after later updates and cleanup by
replacement-player UUID are checked separately. Tests use native send paths and
memory connections, not a rendered client.

Runner: `tools/smoke_visual_replay.py --server-dir build/verification/key-reuse-server`.
Local qualification, artifact and install evidence: `build/verification/visual-replay/`.
No author resource-pack or template changes are required.

The first final-candidate run passed visual recovery and all gameplay suites,
then failed SLR stop-probe setup because the newly included arcanum stop probe
deliberately retains its owner's legacy `time_stopped` input lock until actual
stop. The runner now arms the SLR shot before that probe. Production guards and
assertions were preserved; first-run evidence remains separately recorded.

## Qualified release and installation

Build `1b68c82a0a9d0b1bd4655600913fa1f9cd5d2df73c7a048e792f8e1a2f4a9d5e`
passed 52 recorded results in two actual mode1 matches (52.13s). All 440 source
files, 1219 classes and 3140 resources match the tested release exactly. Native
source producer/lifetime, replay, renderer/gameplay and actual-stop suites pass.
The isolated process stopped normally and restored settings/profiles/scoreboard
and its original mod JAR before qualification. All175 protected author paths
and31 live grimoire overrides retain their original hashes/deletions.

Ready: `build/verification/skywars2-visual-replay-1b68c82a.jar`.
Immutable qualification and live `main-install.json`:
`build/verification/visual-replay/`. Existing installer11424 adopted the release
and waits for normal main shutdown/session.lock and port24566 release. Main disk
remains1a8a3825; no forced shutdown or restart. No RP/template update required.
Retain newest1b68c82a, precedingcbbb82ee and installed1a8a3825; obsoleteff96bdee
was removed only after verifying all retained hashes and the current request.
Cumulative cleanup326 JARs /11628428419 bytes. No rendered Minecraft pixels,
real-network reconnect, full main restart or peak-capacity TPS were observed.
