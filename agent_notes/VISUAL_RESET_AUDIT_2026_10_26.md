# Manual visual reset audit - 26 October 2026

Snapshot of pre-fix sources; the later critical-owner section supersedes the
initial foreign-owner-only test assumption. Implemented behavior and executed
checks are in [the repair contract](MANUAL_VISUAL_ACK_REPAIR.md).

Read-only source audit by the delegated researcher; only this note was written. No production, test, build,
server or resource-pack files changed. Applicable guidance: root AGENTS.md
694-715 and agent_notes/AGENTS.md (preserve user statements; distinguish code,
native observations and rendered evidence). This does not establish client pixels.

## Confirmed gaps

1. **The load barrier is not an acknowledgement barrier.**
   [ManualVisualTracking.java](../src/main/java/me/skywars2/game/ManualVisualTracking.java)
   29-46 waits only for `hasClientLoaded()`, then permanently consumes pending.
   Exact native26.3 ServerGamePacketListenerImpl.java2261-2281 defines that as
   `!waitingForRespawn && clientLoadedTimeoutTimer <= 0`; the timer reaches zero
   without an ACK. `handleAcceptPlayerLoad`567-570 merely zeros it.
   Restart occurs in constructor290 and PERFORM_RESPAWN1934/1943. Native
   ServerPlayer.java1147-1153 sends a dimension Respawn without restarting that
   timer. Thus a live dimension trip can replay immediately before the new ACK;
   death respawn can replay after60ticks without ACK. This contradicts the
   documented guaranteed wait for acknowledgement. It does not by itself prove
   lost entities: normal packet ordering can still place a replay after Respawn.

2. **Bundled resets are invisible to the hook.**
   [ManualVisualResetMixin.java](../src/main/java/me/skywars2/mixin/ManualVisualResetMixin.java)
   17-21 examines only the outer packet. Exact native ClientPacketListener.java
   2450-2455 executes every subpacket in a ClientboundBundlePacket, including
   Respawn. ServerCommonPacketListenerImpl.java157-176 delegates send directly
   to Connection without recursively invoking the listener for bundle members.
   A Respawn inside a bundle therefore clears a dimension without queuing a
   reset. Native PlayerList.java418 and ServerPlayer.java1149 use direct sends;
   this audit has NOT established a current production producer of bundled resets.
   It is a concrete unsupported packet form, not an established incident cause.

3. **The previous causal explanation is overbroad.** Exact native
   ClientPacketListener.java1251-1282 allocates/replaces ClientLevel only when
   dimensionChanged is true. Same-dimension death respawn keeps the level and
   other entities, replacing only the player1300-1330. A missing post-death
   effect does not, by itself, show that item_display was discarded by Respawn.

## Viewer/lifecycle findings

BlackHoleVisuals.java32,123-153 keys slots by connection identity; unchanged
nearest holes retain their holders and membership. Attach154-167 registers the
manual holder, starts watching and ticks; ManualVisualTracking43 explicitly
stop/starts watching. Polymer0.18.2+26.3 ElementHolder.java90-110 confirms that
startWatching alone is idempotent, while120-134 removes subscription and sends
RemoveEntities. Re-pair is therefore substantive, not merely a local cache edit.
No repeat of the earlier ServerPlayer identity/Entity.equals viewer-key bug was
found in BlackHoleVisuals or NuclearVisuals.

BlackHoleVisuals.valid100-103 intentionally retires an effect whose owner died,
was removed, became observer or cannot use gameplay items. Viewer death is
separate from owner death; a test must retain a different living owner. Nuclear
main/fade holder creation shares registered createHolder189-194. ZoneBarrier
55 and103-110 registers the holder and reconciles viewers each pulse.
SignedGlowVisuals.java25-26,58-98 also uses connection identity and registers its
manual carrier, so it participates in reset replay. Its target/slot state is
separate from black holes. No `LbcZones` class exists in this source tree;
ZoneBarrier is the current border implementation.

## Existing VisualReloadChecks gaps

[VisualReloadChecks.java](../src/main/java/me/skywars2/game/VisualReloadChecks.java)
29-43 replaces the real Connection with a recorder, reporting memory/connected
and flattening bundles. It does not run ClientPacketListener or render a frame.
Its successful packet assertions support spawn/metadata emission only.

- Lines110-114 inject a loaded ACK before checking replay. They never wait out
  the native60tick timeout without ACK or inspect replay timing on a normal
  loaded dimension transition before ACK.
- Lines120-126 manually ACK both dimension legs before invoking tracking.tick;
  this masks the native dimension-transition barrier gap.
- The test captures a direct Respawn, never sends a bundle containing Respawn.
- Lines79-87 check that IDs and some metadata packets exist, but not a client's
  final entity table, wire encode/decode, metadata content, or later removal
  ordering. Packet sets alone cannot establish that the display survives.
- The fixture covers a surviving other owner and retained slots; it does not
  cover a fresh black hole created after viewer respawn, repeated deaths, a
  reset while an effect is released/reassigned, or reconnect to a new listener.
- Nuclear retiring/fade and SignedGlow carriers are not explicit IDs in the
  reset fixture; generic/manual membership is not equivalent coverage.

Exact native sources were read from
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.
Polymer sources were read from the local Gradle dependency cache's
`eu.pb4/polymer-virtual-entity/0.18.2+26.3/.../polymer-virtual-entity-0.18.2+26.3-sources.jar`.

No source-only finding above establishes the user's continuing pixel failure.
A useful next observation is the actual user's packet sequence across death:
outer/embedded Respawn, loaded ACK generation, stop/spawn/full metadata/remove
ordering, and a new post-respawn effect with a surviving foreign owner.


## Polymer send interception follow-up

Exact cached core/common/networking0.18.2+26.3 source archives were inspected.
Core `eu/pb4/polymer/core/mixin/other/ServerCommonPacketListenerImplMixin.java`
71-85 does modify the send packet at HEAD, cancel at HEAD when
`PacketPatcher.prevent` returns true, and invoke sendExtra at TAIL. A cancellation
can bypass an independent TAIL observer in principle. **It does not cancel or
replace direct Respawn in this version.**

`eu/pb4/polymer/core/impl/networking/PacketPatcher.java`63-136 handles equipment,
existing bundles and configuration feature packets; otherwise returns the same
packet. No core Java source references ClientboundRespawnPacket.
`prevent`149-193 handles status effects, attached entities, equipment, attributes,
block entities, recipes and animation; otherwise returns false.
`EntityAttachedPacket.java`20-21,32-34 gives an unattached Respawn a null entity,
therefore shouldSend is true. There is no memory-connection-only exemption here
that would explain the recorder passing and a vanilla network client failing.

PacketPatcher102-112 reconstructs an **existing** bundle by recursively replacing
and filtering members. It does not wrap direct Respawn in a newly created bundle.
The common send-listener mixin only handles resource-pack responses and exposes
Connection. The networking send-listener mixin handles custom payloads/rate
limits and exposes Connection. Neither wraps/cancels send.

Networking `eu/pb4/polymer/networking/mixin/PlayerListMixin.java` has a
placeNewPlayer hook which can send a forced Respawn directly via
`connection.send(...)` when its login handshake requests one. This bypasses any
ServerCommonPacketListenerImpl send observer, but it is an initial-login path,
not evidence of a death/respawn bypass. Native ordinary death and dimension
Respawn remain listener.send calls. No Polymer source-supported explanation for
TAIL failing on those direct death packets was found.


## Critical owner/renderer lifetime mismatch (new user evidence)

User reports that hole gameplay continues after the local player's death during
solo testing, while foreign Carpet player deaths do not break the local render.
This changes the priority: inspect effect ownership independently of viewer reset.

**Confirmed permanent orphan mechanism:** BlackHoleVisuals.Handle stores a
ServerPlayer reference. valid100-103 checks that exact object's life/removal,
dimension and gameplay role. tick108 removes a failing handle from the renderer's
`holes` list without notifying the producer. refresh subsequently destroys its
slots. update/move/rebind only mutate the handle; none re-register it.

LbcSheep.java122-124 resolves owners by UUID. tick290-299 resolves the current
player for source physics, but updateVisual265-269 only creates when h.visual is
null and otherwise updates the existing handle; it never rebinds. If renderer
prunes the old player handle while source survives into the replacement player's
life, Sheep continues physics against the replacement UUID owner and permanently
updates an unregistered, nonnull visual. A foreign viewer death does not alter
that owner reference, matching the reported asymmetry.

Qualification: Sheep itself removes source holes if its tick sees the owner dead
(validOwner123-124 and295-296). This is not evidence every ordinary delayed death
leaves Sheep gameplay alive. The orphan requires source survival across the gap,
such as replacement before its next active tick or source ticks being skipped.
There is an explicit skip: LbcItems.java1501-1507 ticks blackHoleVisuals during
map Time Stop then returns before Sheep and Void update. Thus renderer can prune
while the producer has no chance to end/rebind its effect.

Void has a related merged-source path. LbcVoidArcanum145 removes dead/invalid
members and keeps a hole if others remain, but does not rebind visual there.
The timeStopped return150 and paused-author/marker check170 can skip rebind.
updateVisual301-305 eventually rebinds but cannot resurrect a previously pruned
handle. Explicit removeOwner337-343 does immediately rebind a remaining member;
that makes this branch safer, but does not repair an earlier renderer prune.

Duplicating intentionally retains a ServerPlayer as source owner (Owned45), uses
valid(owner)68-74, removes invalid source plus visual561-565, and forget590-594
removes its own markers/jobs. Its normal owner-death source policy therefore
matches renderer retirement better; it still should not control shared renderer
policy for UUID-owned or merged effects. Visual updates501-502/581 also cannot
resurrect an orphaned handle.

LbcItems.forget1659-1697 calls source cleanup first (Void1662, Sheep1670,
Duplicating1696), then blackHoleVisuals.forget1697. Renderer forget currently
prunes by exact owner identity174. GameController.died578-592 calls items.clear,
not items.forget; clear1658 does not terminate these sources. Disconnect cleanup
is a distinct lifecycle. Explicit source.remove is already implemented by Sheep310,
Void331-335 and Duplicating495-496.

Minimal coherent policy: let producers own source lifetime; retain renderer epoch
cleanup plus explicit remove/stop, and avoid permanently unregistering active
handles solely because their cached ServerPlayer died/replaced/changed role.
Remove renderer's equivalent owner-only forget pruning if source cleanup is
already authoritative. Merely adding Sheep.rebind does not cover a prune during
source pause; merely changing ACK replay cannot recover a destroyed holder.
Add owner-death/replacement and merged contributor loss coverage with source
survival asserted, including Time Stop; existing VisualReloadChecks keeps its
foreign owner alive and cannot expose this defect. These are source findings,
not observed pixels or executed regression results.
