# Velocity protocol audit (Minecraft 26.3 merged sources)

Scope: server-to-client packet codecs and client receive path only. Movement gamerules, entity NBT, attribute bounds, and the server's incoming player move validation were left to the separate audit. No builds, server runs, or web sources were used.

## Findings

Vanilla entity velocity has a wire-format ceiling independent of server-side gamerules. `ClientboundSetEntityMotionPacket.STREAM_CODEC` serializes `movement` with `Vec3.LP_STREAM_CODEC`; `ClientboundAddEntityPacket` uses that same codec both when reading and writing its initial movement. `ServerEntity.sendChanges()` sends the motion packet for tracked delta changes and `syncVelocity`; its pairing path sends an entity's add packet. Thus clearing server movement checks does not remove the clientbound codec ceiling.

`Vec3.LP_STREAM_CODEC` delegates to `LpVec3.write/read`. The writer sanitizes each component: NaN becomes zero and values are clamped to `[-1.7179869183E10, +1.7179869183E10]`. The vector is encoded using a shared integer scale and 15-bit normalized components, so this is lossy quantized transport. If the largest absolute component is below `3.051944088384301E-5`, the writer emits the zero-vector marker. The source's declared `ABS_MAX_VALUE` is therefore the practical per-component cap for these velocity packets; larger finite server values are clipped before they reach an unmodified client. This is a protocol/codec cap, not a configurable server-side attribute or gamerule cap.

On receive, `ClientPacketListener.handleSetEntityMotion` looks up the entity and calls `entity.lerpMotion(packet.movement())`. Base `Entity.lerpMotion` calls `setDeltaMovement`; that setter accepts finite `Vec3`s and has no magnitude clamp. `ClientPacketListener.handleAddEntity` calls `recreateFromPacket`, which sets the packet's initial movement. There are specialized `lerpMotion` overrides for remote players, arrows, and minecarts; none add a general maximum-magnitude clamp in the inspected methods. `Entity.setDeltaMovement` rejects non-finite values, which is a finiteness check, not a finite-speed limit.

The separate `Vec3.STREAM_CODEC` reads/writes three raw doubles, but the two entity-velocity packet paths above do not use it. Replacing `LP_STREAM_CODEC` with raw doubles, or changing the packed representation/meaning, would change the existing packet wire format and would require matching client support. Do not invent an alternate encoding if stock-client compatibility is required.

`ClientboundSetHealthPacket` carries `float health`, `int food`, and `float saturation`; `ClientPacketListener.handleSetHealth` applies these to the local player's hurt/health and food state. It has no entity ID, vector field, or path into `setDeltaMovement`. Sending a health packet cannot transport or restore velocity. Floats can represent large magnitudes, but their semantic fields remain health/food/saturation and the vanilla client will interpret them as such.

Without a client mod, the stock motion packet can carry velocity only up to the `LpVec3` bound (with quantization). A server may still synchronize absolute entity positions using existing position packets, but that communicates position changes, not an uncapped delta-velocity value; the client will not reconstruct that value from health packets.

## Exact local source locations

The complete 26.3 merged sources are in the local [Minecraft merged sources JAR](<E:/servers_mew/skywars2_last/luckywars-mod/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar>). References below are archive entry paths and method/field names (the JAR is the source artifact; it was read without extraction):

- `net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket.java`: `STREAM_CODEC`, constructor from `Entity`.
- `net/minecraft/network/protocol/game/ClientboundAddEntityPacket.java`: `STREAM_CODEC`, `read`, `write`, `getMovement`.
- `net/minecraft/network/LpVec3.java`: `ABS_MAX_VALUE`, `ABS_MIN_VALUE`, `write`, `read`, `sanitize`, `pack`, `unpack`.
- `net/minecraft/world/phys/Vec3.java`: `STREAM_CODEC` (raw doubles), `LP_STREAM_CODEC` (packed motion).
- `net/minecraft/server/level/ServerEntity.java`: `sendChanges`, `sendPairingData`.
- `net/minecraft/client/multiplayer/ClientPacketListener.java`: `handleSetEntityMotion`, `handleAddEntity`, `handleSetHealth`.
- `net/minecraft/world/entity/Entity.java`: `lerpMotion`, `setDeltaMovement`, `recreateFromPacket`.
- `net/minecraft/network/protocol/game/ClientboundSetHealthPacket.java`: `STREAM_CODEC`, health/food/saturation fields.
