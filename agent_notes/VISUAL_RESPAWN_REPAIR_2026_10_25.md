## Superseded lifetime and acknowledgement details (26 October2026)

The user reports surviving gameplay with missing old-hole rendering. The first
repair below did not cover the dying visual owner: renderer pruning could
permanently unregister a live producer's handle using its stale ServerPlayer.
Native hasClientLoaded also permits timeout-only completion; the old test
acknowledged immediately. Same-dimension respawn does not recreate the whole
client level. See [the corrected owner/ACK contract and evidence](MANUAL_VISUAL_ACK_REPAIR.md)
and [exact native/Polymer source audit](VISUAL_RESET_AUDIT_2026_10_26.md).
Historical qualification, wording and the user's original quote below remain.

# Manual visual recovery after client world reset

Author report (user):

> заметил что после респауна игрока он перестает видеть черную дыру (а может и не только после респавна, и может это не только рендера дыры касается)

The exact local 26.3 native source shows that Entity.equals/hashCode use entity
ID. Respawn replaces ServerPlayer while retaining that ID and the existing
ServerGamePacketListenerImpl. BlackHoleVisuals and NuclearVisuals previously
used identity maps keyed by ServerPlayer, but ordinary HashSet membership to
prune disconnected viewers. That combination retained old-player rows while
creating another row for the replacement object on the same connection.

Both shader renderers now key their viewer slots by connection identity.
This keeps one set of shader slots across native player replacement and still
releases a disconnected connection normally. Native source references:
`net/minecraft/world/entity/Entity.java` and
`net/minecraft/server/players/PlayerList.java` in the exact Loom source archive.

Polymer 0.18.2+26.3 ElementHolder.startWatching is connection-based and does
nothing if already subscribed. ManualAttachment.updateTracking does nothing,
so a subscription alone cannot recreate virtual client entities after a client
world reset. These behaviors were inspected in the locally cached dependency
sources, including its player/network mixins.

ManualVisualTracking registers the mod's five manual-attachment creation sites:
black holes, nuclear main/fade carriers, zone border, minigun beams and Terra
terminal models. ManualVisualResetMixin observes actual outbound native
ClientboundRespawnPacket, covering both respawn and real dimension changes.
After the native client-loaded acknowledgement, the next game tick re-pairs
only subscribed, still-attached holders belonging to the current dimension.
Full native spawn and metadata are resent with the same IDs, slots, scales
and effect ages. Other viewers are untouched. Destroyed/expired holders are
skipped; reconnecting connections leave the pending set. Registry keys are weak,
and ordinary ticks return immediately when no reset is pending. Replay also
runs while game time is frozen without advancing spell/effect clocks.

EntityAttachment visuals continue using ordinary native entity tracking; their
gameplay/lifecycles were not replaced. No new world entities, chunk loads,
particle spam, effect lifetime or resource-pack changes were introduced.

VisualReloadChecks causes actual lethal void damage and sends native
PERFORM_RESPAWN. It checks replacement player identity/retained ID+listener,
waits for loaded acknowledgement, and captures complete replay packets for a
black hole, nuclear carrier, zone border and an additional manual holder.
A real dimension roundtrip with the same ServerPlayer tests world-reset
recovery without a player replacement. Native metadata and slot identities
persist, and no duplicate replay reaches the unaffected observer or idle tick.

Build71d12d26 passed24 recorded results in two real mode1 matches (43.27s),
including nuclear lifetime/fades, actual minigun visuals, scope presentation,
120 accuracy cases and match-stop cleanup. Exact371 sources/1043 classes/2837
resources match the installed artifact. Rendered client pixels, physical
reconnection, full process restart and peak main TPS were not directly observed.
Evidence: `build/verification/visual-respawn-final/`.

During verification the previously authorized installer installed ea7979f0
after normal main shutdown. Its receipt and artifact hashes were validated
before updating this task's installation baseline. The qualified71d12d26 JAR
was then installed with ea7979f0 retained as rollback. Main was not restarted.
